package com.sohai.platformer.screens

import box2dLight.PointLight
import box2dLight.RayHandler
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.sohai.platformer.Constants
import com.sohai.platformer.abilities.EboAbility
import com.sohai.platformer.abilities.LayaAbility
import com.sohai.platformer.abilities.ZephyrAbility
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.SmogSprite
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.entities.StormSentinel
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.levels.TmxLevel
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.physics.WorldContactListener
import com.sohai.platformer.rendering.CharacterAnimator
import com.sohai.platformer.rendering.CharacterAtlas
import com.sohai.platformer.rendering.ParallaxBackground
import com.sohai.platformer.rendering.ParallaxTheme
import com.sohai.platformer.rendering.ParticleSystem
import com.sohai.platformer.rendering.ScreenFade
import com.sohai.platformer.rendering.SpriteFactory
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Thin coordinator for an active level session. Owns the libGDX lifecycle
 * (render/resize/dispose) and wires three focused subsystems:
 *  - [LevelRunState]  — mutable session state + main update loop
 *  - [LevelRenderer]  — all ShapeRenderer/SpriteBatch drawing + particle helpers
 *  - [LevelTransitionController] — level-complete flow and screen transitions
 */
class GameScreen(
    private val level: Level,
    private val game: Game? = null,
    /** Optional saved checkpoint to resume from (world meters). Null = level spawn. */
    private val resumeCheckpoint: Vector2? = null,
    /** When true: stopwatch HUD active, no checkpoint autosaves, best time persisted. */
    private val isTimeTrial: Boolean = false
) : Screen {

    private companion object {
        const val CHECKPOINT_AUTOSAVE_FILE = "checkpoint_autosave.json"
        const val SAVE_SLOT_FILE           = "save_slot_1.json"
    }

    // ── Core rendering resources ──────────────────────────────────────────────
    private val camera: OrthographicCamera = OrthographicCamera()
    private val viewport: Viewport =
        FitViewport(Constants.VIRTUAL_WIDTH / Constants.PPM, Constants.VIRTUAL_HEIGHT / Constants.PPM, camera)
    private val shapeRenderer: ShapeRenderer
    private val spriteBatch: SpriteBatch = SpriteBatch()
    private val eboAtlas: CharacterAtlas  = SpriteFactory.createEbo()
    private val layaAtlas: CharacterAtlas = SpriteFactory.createLaya()
    private val eboAnimator: CharacterAnimator = CharacterAnimator(eboAtlas)
    private val layaAnimator: CharacterAnimator = CharacterAnimator(layaAtlas)
    private val rayHandler: RayHandler
    private val playerLight: PointLight
    private val footstepColor = Color(0.6f, 0.55f, 0.45f, 0.8f)

    // ── Physics world ─────────────────────────────────────────────────────────
    private val world: World

    // ── Gameplay objects ──────────────────────────────────────────────────────
    private val player: PlayerController
    private val eboAbility: EboAbility
    private val layaAbility: LayaAbility
    private val zephyrAbility: ZephyrAbility
    private val obstacleManager: ObstacleManager
    private val movingPlatforms = mutableListOf<MovingPlatform>()
    private val ecoTokens = mutableListOf<EcoToken>()
    private val snapshotPickups = mutableListOf<SnapshotPickup>()

    // ── HUD + effects ─────────────────────────────────────────────────────────
    private val hud: Hud
    private val parallaxBg: ParallaxBackground
    private val screenFade: ScreenFade
    private val particles = ParticleSystem(maxParticles = 200)
    private val achievementToast: AchievementToast

    // ── Subsystems ────────────────────────────────────────────────────────────
    private val renderer: LevelRenderer
    private val runState: LevelRunState
    private val transitionCtrl: LevelTransitionController

    // ── Overlay management (GameScreen owns overlay lifecycle) ────────────────
    private var isPaused = false
    private val pauseOverlay: PauseOverlay
    private var atlasOverlay: CloudAtlasOverlay? = null
    private var levelCompleteOverlay: LevelCompleteOverlay? = null
    private var gameOverOverlay: GameOverOverlay? = null

    // ── Boss ─────────────────────────────────────────────────────────────────
    private var sentinel: StormSentinel? = null

    // ── Body destruction queue ────────────────────────────────────────────────
    private val pendingBodyDestroy = mutableSetOf<Body>()

    private var isDisposed = false

    init {
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)

        world = World(Vector2(0f, Constants.GRAVITY), true)
        world.setContactListener(WorldContactListener())
        obstacleManager = ObstacleManager(world)
        shapeRenderer   = ShapeRenderer()

        RayHandler.setGammaCorrection(true)
        RayHandler.useDiffuseLight(true)
        rayHandler = RayHandler(world).apply { setAmbientLight(0.15f, 0.18f, 0.25f, 0.7f) }

        level.setup(world, obstacleManager, movingPlatforms)

        for (cp in level.getCheckpoints()) {
            obstacleManager.addCheckpointNormalized(
                cp.name,
                cp.x * Constants.PPM / Constants.VIRTUAL_WIDTH,
                cp.y * Constants.PPM / Constants.VIRTUAL_HEIGHT,
                18f / Constants.VIRTUAL_WIDTH
            )
        }

        eboAbility    = EboAbility(world)
        layaAbility   = LayaAbility(world)
        zephyrAbility = ZephyrAbility()
        player        = PlayerController(world, level.spawnX, level.spawnY, eboAbility)
        eboAbility.setPlayerController(player)
        layaAbility.setPlayerController(player)
        zephyrAbility.setPlayerController(player)

        resumeCheckpoint?.let { cp ->
            player.setSpawn(cp)
            player.body.setTransform(cp.x, cp.y + 0.2f, 0f)
            player.body.linearVelocity = Vector2.Zero
        }

        playerLight = PointLight(rayHandler, 128, Color(0.9f, 0.95f, 1f, 0.85f), 4f, 0f, 0f)
        playerLight.attachToBody(player.body)

        SoundManager.setVolume(SettingsManager.load().volSfx)

        ecoTokens.addAll(level.getEcoTokenPositions().map { EcoToken(world, it.x, it.y) })
        snapshotPickups.addAll(level.getSnapshotPickups(world))

        hud = Hud(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        hud.onSwapCharacter = { runState.switchCharacter() }
        hud.setTimeTrial(isTimeTrial)
        Gdx.input.inputProcessor = hud.stage

        achievementToast = AchievementToast(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)

        parallaxBg = ParallaxBackground(
            theme = when (level.id) {
                "level2" -> ParallaxTheme.WIND
                "level3" -> ParallaxTheme.ECO
                else     -> ParallaxTheme.ARID
            }
        )
        screenFade = ScreenFade(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        screenFade.fadeIn(speed = 1.5f)

        // ── Wire particle callbacks from player ───────────────────────────────
        // renderer is created below; player callbacks reference renderer via lambda closure.

        // Instantiate enemies from level definition (shared list: renderer draws, runState updates)
        val enemies = mutableListOf<Enemy>()
        if (level is TmxLevel) {
            for (def in level.getEnemyDefs()) {
                when (def.type) {
                    "smog_sprite" -> enemies.add(
                        SmogSprite.create(
                            world,
                            x = def.xPx / Constants.PPM,
                            y = def.yPx / Constants.PPM,
                            patrolLeftX = def.patrolLeftPx / Constants.PPM,
                            patrolRightX = def.patrolRightPx / Constants.PPM
                        )
                    )
                    else -> Gdx.app.error("GameScreen", "Unknown enemy type: ${def.type}")
                }
            }
            if (enemies.isNotEmpty()) {
                Gdx.app.log("GameScreen", "Spawned ${enemies.size} enemies for level ${level.id}")
            }
        }

        // Instantiate boss from level definition, if present
        if (level is TmxLevel) {
            val bdef = level.getBossDef()
            if (bdef != null) {
                when (bdef.type) {
                    "storm_sentinel" -> {
                        sentinel = StormSentinel(
                            world,
                            x          = bdef.xPx          / Constants.PPM,
                            y          = bdef.yPx          / Constants.PPM,
                            arenaLeft  = bdef.arenaLeftPx  / Constants.PPM,
                            arenaRight = bdef.arenaRightPx / Constants.PPM
                        )
                        Gdx.app.log("GameScreen", "Storm Sentinel spawned at (${bdef.xPx}, ${bdef.yPx}) px")
                    }
                    else -> Gdx.app.error("GameScreen", "Unknown boss type: ${bdef.type}")
                }
            }
        }

        renderer = LevelRenderer(
            shapeRenderer, spriteBatch, camera, parallaxBg, particles,
            eboAbility, layaAbility, zephyrAbility,
            obstacleManager, movingPlatforms, ecoTokens, snapshotPickups,
            enemies, player, eboAnimator, layaAnimator, footstepColor,
            sentinel = sentinel
        )

        player.onJump = {
            SoundManager.play("jump")
            renderer.spawnJumpPuff(player.body.position.x, player.body.position.y - 0.32f,
                runState.currentCharacter)
        }
        player.onFootstep = { fx, fy, _ -> renderer.spawnFootstep(fx, fy) }

        runState = LevelRunState(
            level, player, eboAbility, layaAbility, zephyrAbility,
            ecoTokens, snapshotPickups, obstacleManager, movingPlatforms,
            world, hud, game, particles, screenFade,
            eboAnimator, layaAnimator, camera, pendingBodyDestroy,
            renderer, enemies, CHECKPOINT_AUTOSAVE_FILE,
            isTimeTrial      = isTimeTrial,
            achievementToast = achievementToast,
            saveSlotFile     = SAVE_SLOT_FILE
        )

        // Wire boss sentinel into runState (callbacks set after runState exists)
        if (sentinel != null) {
            runState.sentinel = sentinel
            sentinel!!.onSpawnProjectile = { x, y, vx, vy ->
                runState.spawnProjectile(x, y, vx, vy)
            }
            sentinel!!.onDefeated = {
                Gdx.app.log("GameScreen", "Storm Sentinel defeated — level complete")
                runState.levelCompleted = true
                hud.showTransientMessage("Storm Sentinel defeated!", 2.5f)
                runState.tryUnlock("boss_defeated")
            }
        }

        runState.onAtlasCollected = { snap ->
            atlasOverlay = CloudAtlasOverlay(snap.entry) {
                atlasOverlay?.dispose()
                atlasOverlay = null
                Gdx.input.inputProcessor = hud.stage
            }
            Gdx.input.inputProcessor = atlasOverlay!!.stage
        }
        runState.onGameOverStart = {
            gameOverOverlay = GameOverOverlay(
                onRestart  = { if (game != null) { game.screen = GameScreen(level, game); dispose() } },
                onMainMenu = { if (game != null) { game.screen = MainMenuScreen(game); dispose() } }
            )
            Gdx.input.inputProcessor = gameOverOverlay!!.stage
        }
        transitionCtrl = LevelTransitionController(
            level, game, screenFade, ecoTokens,
            CHECKPOINT_AUTOSAVE_FILE,
            onInputChange    = { stage -> Gdx.input.inputProcessor = stage },
            onDispose        = { dispose() },
            isTimeTrial      = isTimeTrial,
            achievementToast = achievementToast,
            saveSlotFile     = SAVE_SLOT_FILE
        )

        pauseOverlay = PauseOverlay(
            onResume   = { setPaused(false) },
            onRestart  = { if (game != null) { game.screen = GameScreen(level, game); dispose() } },
            onMainMenu = { if (game != null) { game.screen = MainMenuScreen(game); dispose() } },
            // Toggle time trial: restart the level with the opposite mode
            onTimeTrial = {
                if (game != null) {
                    game.screen = GameScreen(level, game, isTimeTrial = !isTimeTrial)
                    dispose()
                }
            },
            isCurrentlyTimeTrial = isTimeTrial
        )

        hud.updateSpiritHealth(runState.spiritHealth)
        hud.showTransientMessage(level.name, 2.5f)

        // Start background music for this level
        MusicManager.play(level.musicTrack, fadeIn = true)
    }

    private fun setPaused(paused: Boolean) {
        isPaused = paused
        Gdx.input.inputProcessor = if (paused) pauseOverlay.stage else hud.stage
    }

    fun queueBodyDestroy(body: Body) { pendingBodyDestroy.add(body) }

    fun removeObstacle(id: String): Boolean = obstacleManager.remove(id)
    fun clearObstacles() = obstacleManager.clear()
    fun addRectObstacle(id: String, kind: ObstacleKind, xPx: Float, yPx: Float,
                        halfWidthPx: Float, halfHeightPx: Float, sensor: Boolean = false) {
        obstacleManager.addRectNormalized(id, kind,
            xPx / Constants.VIRTUAL_WIDTH, yPx / Constants.VIRTUAL_HEIGHT,
            halfWidthPx / Constants.VIRTUAL_WIDTH, halfHeightPx / Constants.VIRTUAL_HEIGHT, sensor)
    }
    fun addCheckpointObstacle(id: String, xPx: Float, yPx: Float, radiusPx: Float) {
        obstacleManager.addCheckpointNormalized(id,
            xPx / Constants.VIRTUAL_WIDTH, yPx / Constants.VIRTUAL_HEIGHT,
            radiusPx / Constants.VIRTUAL_WIDTH)
    }

    override fun show() {}

    override fun render(delta: Float) {
        val clampedDelta = delta.coerceAtMost(Constants.MAX_FRAME_DELTA)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) setPaused(!isPaused)

        if (Constants.SMOKE_MODE) {
            // Smoke mode: always tick update() so the auto-quit timer fires
            // regardless of overlays (Cloud Atlas snapshots), pause state, or
            // hitstop. Without this, the smoke autopilot collects a snapshot,
            // the atlas overlay opens, update halts, and the JVM hangs until
            // CI's 240s wall-clock timeout fires — losing the [smoke] log line.
            runState.update(clampedDelta)
        } else if (!isPaused && atlasOverlay == null) {
            if (runState.hitstopFrames > 0) runState.hitstopFrames--
            else runState.update(clampedDelta)
        }
        achievementToast.update(clampedDelta)

        // Drive music crossfade even while paused (audio should not glitch)
        MusicManager.update(clampedDelta)

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Layer 0–2: parallax + obstacles + VFX + particles
        renderer.renderWorld(runState.cleanseRatio, runState.currentCharacter, runState.projectiles)

        // Layer 2b: player sprite
        renderer.renderPlayer(runState.currentCharacter)

        // Layer 2c: portal labels (hub world only)
        if (level is com.sohai.platformer.levels.Level0_0) {
            renderer.renderPortalLabels()
        }

        // Layer 3: dynamic lighting
        rayHandler.setCombinedMatrix(
            camera.combined,
            camera.position.x, camera.position.y,
            camera.viewportWidth * camera.zoom,
            camera.viewportHeight * camera.zoom
        )
        rayHandler.updateAndRender()

        // Layer 4: HUD
        hud.stage.act(clampedDelta)
        hud.stage.draw()

        // Layer 4.5: achievement toast (above HUD, below pause/fade)
        achievementToast.render(spriteBatch)

        // Layer 5: screen fade overlay
        screenFade.render()

        // Layer 6: Cloud Atlas card overlay
        atlasOverlay?.render()

        // Layer 7: pause overlay
        if (isPaused) pauseOverlay.render()

        // Layer 8: level-complete card
        if (runState.levelCompleted && levelCompleteOverlay == null) {
            levelCompleteOverlay = transitionCtrl.startLevelComplete(
                runState.levelTimer, runState.score,
                onContinue = { runState.levelCompletionTimer = 0f }
            )
        }
        levelCompleteOverlay?.render()

        // Layer 9: game-over card
        gameOverOverlay?.render()

        // Transitions (end of render so dispose is never called mid-frame).
        // In smoke-test mode (cloudy.smokeMode=true) we suppress level-change
        // transitions so the autopilot can't hop into a new GameScreen and
        // reset its auto-quit timer. The smoke run stays in the level it was
        // launched into and exits cleanly when debugAutoQuitTimer fires.
        if (runState.isGameOver && runState.gameOverTimer <= 0f && game != null && !Constants.SMOKE_MODE) {
            game.screen = MainMenuScreen(game)
            dispose()
            return
        }
        if (runState.levelCompleted && !Constants.SMOKE_MODE) {
            runState.levelCompletionTimer -= clampedDelta
            if (runState.levelCompletionTimer <= 0f) transitionCtrl.goToNextLevel(runState.score)
        }
        val portalTarget = runState.pendingPortalTarget
        if (portalTarget != null && game != null && !Constants.SMOKE_MODE) {
            runState.pendingPortalTarget = null
            val targetLevel = LevelManager.getLevel(portalTarget)
            if (targetLevel != null) {
                game.screen = GameScreen(targetLevel, game)
                dispose()
                return
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        hud.resize(width, height)
        pauseOverlay.resize(width, height)
        atlasOverlay?.resize(width, height)
        levelCompleteOverlay?.resize(width, height)
        gameOverOverlay?.resize(width, height)
        achievementToast.resize(width, height)
    }

    override fun pause()  {}
    override fun resume() {}
    override fun hide()   {}

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        // Destroy boss body before world.dispose() to avoid stale-reference risk
        sentinel?.let { world.destroyBody(it.body) }
        sentinel = null

        snapshotPickups.forEach { world.destroyBody(it.body) }
        snapshotPickups.clear()
        achievementToast.dispose()
        atlasOverlay?.dispose();       atlasOverlay        = null
        levelCompleteOverlay?.dispose(); levelCompleteOverlay = null
        gameOverOverlay?.dispose();     gameOverOverlay      = null
        ecoTokens.forEach { world.destroyBody(it.body) }
        ecoTokens.clear()
        obstacleManager.clear()
        world.dispose()
        shapeRenderer.dispose()
        spriteBatch.dispose()
        eboAtlas.dispose()
        layaAtlas.dispose()
        rayHandler.dispose()
        hud.dispose()
        parallaxBg.dispose()
        screenFade.dispose()
        pauseOverlay.dispose()
    }
}

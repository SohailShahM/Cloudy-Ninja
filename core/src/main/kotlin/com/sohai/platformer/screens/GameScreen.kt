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
import com.sohai.platformer.entities.DriftHusk
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.entities.StormSentinel
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.input.InputManager
import com.sohai.platformer.input.RestartHoldTracker
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelEntityFactory
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.physics.WorldContactListener
import com.sohai.platformer.rendering.CharacterAnimator
import com.sohai.platformer.rendering.CharacterAtlas
import com.sohai.platformer.rendering.ParallaxBackground
import com.sohai.platformer.rendering.ParallaxTheme
import com.sohai.platformer.rendering.ParticleSystem
import com.sohai.platformer.rendering.ScreenFade
import com.sohai.platformer.rendering.SpriteFactory
import com.sohai.platformer.rendering.TileRenderer
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

    // ── Tile renderer ─────────────────────────────────────────────────────────
    private val tileRenderer: TileRenderer

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
    /** T-130: shown after the T-097 death animation completes. Re-created per death. */
    private var deathRecapOverlay: DeathRecapOverlay? = null
    // T-137: first-run tutorial overlay on Sky Sanctuary hub. Created only on
    // a fresh save (tutorialSeen=false) for Level0_0. Disposed + nulled on
    // dismiss; never recreated within this GameScreen lifetime.
    private var hubTutorialOverlay: HubTutorialOverlay? = null

    // ── Boss ─────────────────────────────────────────────────────────────────
    private var sentinel: StormSentinel? = null

    // ── Body destruction queue ────────────────────────────────────────────────
    private val pendingBodyDestroy = mutableSetOf<Body>()

    // ── T-133: quick-restart hotkey (hold rebindable `restart` key 0.5s) ─────
    private val restartHold = RestartHoldTracker(holdDurationSeconds = 0.5f)

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

        val settings = SettingsManager.load()
        MusicManager.setMusicVolume(settings.volMusic)
        SoundManager.setVolume(settings.volSfx)
        SoundManager.setUiVolume(settings.volUi)

        ecoTokens.addAll(level.getEcoTokenPositions().map { EcoToken(world, it.x, it.y) })
        // T-107: hidden eco-tokens are tagged isHidden=true so the renderer
        // can apply a golden tint and LevelRunState can route collection to
        // the cross-run persistence path. Skip spawning if this level's
        // hidden token was already collected in a previous session.
        val alreadyCollected = com.sohai.platformer.persist.SaveManager
            .loadGame(SAVE_SLOT_FILE)
            .collectedHiddenTokens
        if (level.id !in alreadyCollected) {
            ecoTokens.addAll(
                level.getHiddenEcoTokenPositions().map {
                    EcoToken(world, it.x, it.y, isHidden = true)
                }
            )
        }
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
        screenFade.fadeFromBlack(speed = 1.5f)

        // ── Wire particle callbacks from player ───────────────────────────────
        // renderer is created below; player callbacks reference renderer via lambda closure.

        // T-106: Instantiate enemies + drift husks + boss via the factory.
        // GameScreen still owns the Box2D world; factory just builds the entities.
        // New entity types plug in via LevelEntityFactory, not via more parallel
        // blocks here.
        val spawned = LevelEntityFactory.spawn(level, world)
        val enemies = spawned.enemies
        val driftHusks = spawned.driftHusks
        sentinel = spawned.boss

        tileRenderer = TileRenderer(spriteBatch, camera)

        val parallaxTheme = when (level.id) {
            "level2" -> ParallaxTheme.WIND
            "level3" -> ParallaxTheme.ECO
            else     -> ParallaxTheme.ARID
        }

        renderer = LevelRenderer(
            shapeRenderer, spriteBatch, camera, parallaxBg, particles,
            eboAbility, layaAbility, zephyrAbility,
            obstacleManager, movingPlatforms, ecoTokens, snapshotPickups,
            enemies, player, eboAnimator, layaAnimator, footstepColor,
            sentinel      = sentinel,
            tileRenderer  = tileRenderer,
            parallaxTheme = parallaxTheme,
            driftHusks    = driftHusks,
            // T-144: pass the level width so camera look-ahead can clamp
            // against the right edge — keeps the bias from revealing
            // out-of-bounds space at level extremes.
            levelWidthPx  = level.levelWidthPx
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
            saveSlotFile     = SAVE_SLOT_FILE,
            driftHusks       = driftHusks
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
                hud.showTransientMessage(Strings.get(StringKey.RUN_BOSS_DEFEATED), 2.5f)
                runState.fireBossDefeatedAchievements()
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
        // T-130: show the death-recap overlay after the death animation
        // finishes (or instantly on the reducedMotion/instant-respawn path).
        // Suppressed under SMOKE_MODE so the autopilot's frame budget is not
        // disrupted — the existing rapid-respawn loop continues unchanged.
        runState.onDeathRecap = { cause, t, stomps, tokens ->
            if (game != null && !Constants.SMOKE_MODE && deathRecapOverlay == null) {
                val s = SettingsManager.load()
                val overlay = DeathRecapOverlay(
                    onRetry = {
                        // Restart the level — matches the existing pause-overlay
                        // restart path. Dispose self at the end of the frame
                        // via the same pattern used by other transitions.
                        game.screen = GameScreen(level, game, isTimeTrial = isTimeTrial)
                        dispose()
                    },
                    onQuit = {
                        game.screen = MainMenuScreen(game)
                        dispose()
                    },
                    reducedMotion = s.reducedMotion,
                )
                overlay.show(DeathRecapOverlay.Snapshot(
                    cause          = cause,
                    timeIntoLevel  = t,
                    stompsThisRun  = stomps,
                    tokensThisRun  = tokens,
                ))
                deathRecapOverlay = overlay
                Gdx.input.inputProcessor = overlay.stage
            }
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
            isCurrentlyTimeTrial = isTimeTrial,
            // T-140: highlight the player's active character in the
            // pause-overlay ability-summary card.
            currentCharacter = runState.currentCharacter
        )

        hud.updateSpiritHealth(runState.spiritHealth)
        hud.showTransientMessage(level.name, 2.5f)

        // Start background music for this level
        MusicManager.play(level.musicTrack, fadeIn = true)

        // T-137: First-run hub tutorial overlay. Only constructed on Level0_0
        // when the slot has never seen it before. Dismissal flips
        // tutorialSeen=true and persists, so subsequent entries (this slot)
        // skip construction entirely. Smoke mode honors the same path — the
        // autopilot's first keypress within ~1s closes the overlay; rendering
        // continues underneath unchanged.
        if (level is com.sohai.platformer.levels.Level0_0) {
            val saved = com.sohai.platformer.persist.SaveManager.loadGame(SAVE_SLOT_FILE)
            if (com.sohai.platformer.levels.Level0_0.shouldShowFirstRunTutorial(saved)) {
                hubTutorialOverlay = HubTutorialOverlay(
                    onDismiss     = {
                        // Persist the dismissal so the overlay never reappears
                        // for this slot. Re-load to avoid clobbering writes
                        // that landed after our cached snapshot (e.g. an
                        // achievement unlock between overlay-open and dismiss).
                        val cur = com.sohai.platformer.persist.SaveManager.loadGame(SAVE_SLOT_FILE)
                        com.sohai.platformer.persist.SaveManager.saveGame(
                            cur.copy(tutorialSeen = true), SAVE_SLOT_FILE
                        )
                    },
                    reducedMotion = settings.reducedMotion
                )
            }
        }
    }

    private fun setPaused(paused: Boolean) {
        isPaused = paused
        // T-063: replay the 0.2s fade-in each time the overlay is shown.
        if (paused) pauseOverlay.resetFade()
        // T-117: dip music while the pause overlay is up so the menu reads cleanly,
        // restore on close. duck()/unduck() are idempotent — rapid toggles collapse.
        if (paused) MusicManager.duck() else MusicManager.unduck()
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

        // T-133: hold-restart timer. Ticked only while gameplay is active —
        // pause/overlay/smoke-mode all freeze the tracker (release-state
        // semantics: held=false resets heldSeconds to 0). The autopilot
        // never presses R (verified: LevelRunState.setDebugHeld only drives
        // left/right/jump/action), so this is safe under SMOKE_MODE too.
        val restartGameplayActive = !isPaused
            && atlasOverlay == null
            && levelCompleteOverlay == null
            && gameOverOverlay == null
            && !runState.isGameOver
            && !runState.levelCompleted
            && !Constants.SMOKE_MODE
        val restartHeldNow = restartGameplayActive && InputManager.isRestartHeld()
        if (restartHold.update(clampedDelta, restartHeldNow)) {
            // Threshold reached — restart the level. Match PauseOverlay.onRestart
            // (re-instantiate GameScreen on the same level, dispose this one).
            restartHold.reset()
            if (game != null) {
                game.screen = GameScreen(level, game, isTimeTrial = isTimeTrial)
                dispose()
                return
            }
        }

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

        // Layer 4.6: T-133 radial restart progress indicator. Drawn near the
        // top-right of the HUD (just under the score/timer block) only while
        // the player is actively holding the restart key. A short tap never
        // triggers a draw because heldSeconds returns to 0 the same frame the
        // key is released.
        if (restartHold.isHolding()) {
            renderRestartHoldIndicator(restartHold.progress())
        }

        // Layer 5: screen fade overlay
        screenFade.render()

        // Layer 6: Cloud Atlas card overlay
        atlasOverlay?.render()

        // Layer 7: pause overlay
        if (isPaused) pauseOverlay.render()

        // Layer 7b: first-run hub tutorial overlay (T-137).
        // Rendered above the pause overlay so a player who hits ESC the moment
        // they spawn still sees the tutorial; dispose + null once dismissed so
        // resize / dispose paths don't have to special-case it.
        hubTutorialOverlay?.let { overlay ->
            overlay.render()
            if (overlay.isDismissed) {
                overlay.dispose()
                hubTutorialOverlay = null
            }
        }

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

        // Layer 9b: death-recap card (T-130). Advances its own timer so
        // auto-dismiss fires even though gameplay update is gated by isPaused
        // checks elsewhere. Real-time delta keeps the 3s wall-clock honest.
        deathRecapOverlay?.let { overlay ->
            overlay.tick(clampedDelta)
            overlay.render()
        }

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

    /**
     * T-133: render a small radial progress arc near the top-right of the
     * HUD while the player holds the restart key. Uses screen-space pixel
     * coordinates (matches HUD's [Constants.VIRTUAL_WIDTH]×[Constants.VIRTUAL_HEIGHT]
     * virtual viewport) so it lines up visually with the score/timer block in
     * [Hud]. Implementation: filled background ring + foreground filled-arc
     * proportional to [progress] (0..1). No new texture assets.
     */
    private fun renderRestartHoldIndicator(progress: Float) {
        // Anchor near the top-right (mirrors HUD's topRight table padding
        // ~40px right, ~20px top — drop the ring just under the timer).
        val cx = Constants.VIRTUAL_WIDTH - 56f
        val cy = Constants.VIRTUAL_HEIGHT - 96f
        val outerR = 14f
        val innerR = 10f

        val width  = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        shapeRenderer.projectionMatrix.setToOrtho2D(
            0f, 0f,
            Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT
        )
        // Preserve any GL state we touch.
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Background ring: faint dark backdrop so the foreground arc reads
        // against the level.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.45f)
        shapeRenderer.circle(cx, cy, outerR, 24)
        shapeRenderer.color = Color(0.07f, 0.10f, 0.14f, 0.85f)
        shapeRenderer.circle(cx, cy, innerR, 24)

        // Foreground arc: bright green, swept clockwise from 12 o'clock by
        // (progress * 360°). libGDX's arc() takes start-degrees + sweep-degrees
        // measured CCW from +X axis; rotating start to 90° puts the 0% mark at
        // 12 o'clock, and negating sweep produces a clockwise fill.
        if (progress > 0f) {
            shapeRenderer.color = Color(0.3f, 1f, 0.55f, 0.95f)
            // Outer disk swept arc + inner punch-out reproduces a ring without
            // ShapeRenderer.Line aliasing.
            shapeRenderer.arc(cx, cy, outerR, 90f, -progress * 360f, 32)
            shapeRenderer.color = Color(0.07f, 0.10f, 0.14f, 1f)
            shapeRenderer.circle(cx, cy, innerR, 24)
        }
        shapeRenderer.end()

        // Restore identity-ish projection for subsequent renderers that rely
        // on the world-camera matrix (level fade overlay is next).
        shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, width, height)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        hud.resize(width, height)
        pauseOverlay.resize(width, height)
        atlasOverlay?.resize(width, height)
        levelCompleteOverlay?.resize(width, height)
        gameOverOverlay?.resize(width, height)
        deathRecapOverlay?.resize(width, height)
        hubTutorialOverlay?.resize(width, height)
        achievementToast.resize(width, height)
    }

    /**
     * T-112: libGDX fires [pause] when the desktop window loses focus (alt-tab,
     * minimise, another window stealing focus). Raise the existing T-063 pause
     * overlay so the game freezes until the player explicitly resumes. No-op if
     * we're already paused (an explicit ESC press, etc.) — avoids replaying the
     * fade animation when focus loss arrives while paused.
     *
     * The smoke-mode guard lives in [Main.pause]; this method is intentionally
     * unconditional so other auto-pause callers (if any are added later) get
     * the same behaviour.
     */
    override fun pause() {
        if (!isPaused) setPaused(true)
    }

    /**
     * T-112: libGDX fires [resume] when the window regains focus. We do
     * **not** auto-clear the pause overlay — the player must explicitly click
     * Resume or press ESC. [Main.resume] skips the super.resume() forward, but
     * this is also defensive in case a future caller invokes it directly.
     */
    override fun resume() {
        // Intentionally a no-op — overlay persists until explicit input.
    }
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
        deathRecapOverlay?.dispose();   deathRecapOverlay    = null
        hubTutorialOverlay?.dispose();  hubTutorialOverlay   = null
        ecoTokens.forEach { world.destroyBody(it.body) }
        ecoTokens.clear()
        obstacleManager.clear()
        tileRenderer.dispose()
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

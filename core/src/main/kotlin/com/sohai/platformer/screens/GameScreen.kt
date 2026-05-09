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
import com.sohai.platformer.rendering.CharacterAnimator
import com.sohai.platformer.rendering.CharacterAtlas
import com.sohai.platformer.rendering.SpriteFactory
import com.sohai.platformer.abilities.EboAbility
import com.sohai.platformer.abilities.LayaAbility
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.input.InputManager
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.physics.CleanseEventQueue
import com.sohai.platformer.physics.WorldContactListener
import com.sohai.platformer.rendering.ParallaxBackground
import com.sohai.platformer.rendering.ParticleSystem
import com.sohai.platformer.rendering.ScreenFade
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

class GameScreen(
    private val level: Level,
    private val game: Game? = null,
    /** Optional saved checkpoint to resume from, in world meters. Null = use level.spawnX/Y. */
    private val resumeCheckpoint: Vector2? = null
) : Screen {

    /** All hot-path colors hoisted out of render() to avoid per-frame allocations. */
    private companion object Palette {
        /** Filename used exclusively for checkpoint-based autosaves. Created when the
         *  player activates a checkpoint, deleted on level completion, and consumed on
         *  death so the player respawns at the last checkpoint instead of level start. */
        const val CHECKPOINT_AUTOSAVE_FILE = "checkpoint_autosave.json"

        // Abilities
        val DROPLET       = Color(0.3f, 0.6f, 1f, 0.7f)

        // Obstacles
        val HAZARD_CLEAN_BASE  = Color(0.25f, 0.65f, 0.3f,  1f)
        val HAZARD_CLEAN_GLEAM = Color(0.5f,  0.95f, 0.55f, 0.8f)
        val HAZARD_BASE        = Color(0.75f, 0.15f, 0.15f, 1f)
        val HAZARD_STRIPE      = Color(0.55f, 0.08f, 0.08f, 1f)
        val WALL_BASE          = Color(0.20f, 0.20f, 0.22f, 1f)
        val WALL_EDGE          = Color(0.35f, 0.35f, 0.38f, 1f)
        val EXIT_BASE          = Color(0.15f, 0.9f,  0.55f, 0.45f)
        val EXIT_EDGE          = Color(0.3f,  1f,    0.65f, 0.85f)
        val GROUND_BASE        = Color(0.40f, 0.42f, 0.45f, 1f)
        val GROUND_TOP         = Color(0.62f, 0.65f, 0.68f, 1f)

        // Moving platform
        val MP_BASE = Color(0.50f, 0.33f, 0.14f, 1f)
        val MP_TOP  = Color(0.75f, 0.55f, 0.30f, 1f)

        // Checkpoint
        val CP_GLOW_INACTIVE   = Color(0.15f, 0.15f, 0.65f, 0.3f)
        val CP_GLOW_ACTIVE     = Color(0.1f,  0.6f,  0.15f, 0.3f)
        val CP_BODY_INACTIVE   = Color(0.2f,  0.2f,  0.9f,  1f)
        val CP_BODY_ACTIVE     = Color(0.25f, 0.85f, 0.3f,  1f)

        // Snapshot star
        val SNAPSHOT_GLOW = Color(0.1f,  0.8f,  0.9f,  0.35f)
        val SNAPSHOT_BODY = Color(0.15f, 0.85f, 0.95f, 1f)

        // Tokens
        val TOKEN = Color(0.2f, 0.9f, 0.3f, 1f)

        // Collect sparkle colors (hoisted to avoid per-frame allocation)
        val SPARKLE_TOKEN    = Color(0.3f, 1f, 0.9f, 1f)   // cyan  — eco-token pickup
        val SPARKLE_SNAPSHOT = Color(1f, 0.9f, 0.2f, 1f)   // yellow — snapshot pickup

        // Wind trail (per-frame alpha; mutate `tmpWindCol`)
        val tmpWindCol = Color(1f, 1f, 1f, 1f)
    }

    private val camera: OrthographicCamera = OrthographicCamera()
    private val viewport: Viewport = FitViewport(Constants.VIRTUAL_WIDTH / Constants.PPM, Constants.VIRTUAL_HEIGHT / Constants.PPM, camera)

    private val world: World
    private val shapeRenderer: ShapeRenderer
    private val spriteBatch: SpriteBatch = SpriteBatch()
    private val eboAtlas: CharacterAtlas = SpriteFactory.createEbo()
    private val layaAtlas: CharacterAtlas = SpriteFactory.createLaya()
    private val eboAnimator: CharacterAnimator = CharacterAnimator(eboAtlas)
    private val layaAnimator: CharacterAnimator = CharacterAnimator(layaAtlas)
    private val rayHandler: RayHandler
    private val playerLight: PointLight
    private val player: PlayerController
    private val eboAbility: EboAbility
    private val layaAbility: LayaAbility
    private var currentCharacter = "Ebo"
    private var canSwitchCharacter = true
    private var switchCooldownTimer = 0f

    private val obstacleManager: ObstacleManager
    private val movingPlatforms = mutableListOf<MovingPlatform>()
    private val hud: Hud

    private val parallaxBg: ParallaxBackground
    private val screenFade: ScreenFade
    private val particles = ParticleSystem(maxParticles = 200)
    // Pre-allocated dust color reused for every footstep particle (T-011) — avoids per-step allocation.
    private val footstepColor = Color(0.6f, 0.55f, 0.45f, 0.8f)
    private var prevPlayerVy = 0f
    private var prevGrounded = false

    // Camera target tracking for dead-zone + forward-focus follow (Itay Keren style)
    private val camTarget = Vector2()
    private val camDeadZoneHalfW = 1.0f   // m — player can move 1m off-centre before camera follows
    private val camForwardOffset = 1.5f   // m — camera bias toward facing direction
    private val camLerpSpeed     = 5f     // higher = snappier, lower = smoother
    private var camInitialized   = false
    // Platform snap (§4.3): camera Y only tracks player when grounded or falling fast,
    // locking the view during jump arcs so the world doesn't bob with every jump.
    private val camVertSnapFallThreshold = -3f  // m/s — vy below this = "falling with intent"
    private var cameraTargetY = 0f              // last committed vertical camera target

    // Screen shake (intensity in meters)
    private var shakeIntensity = 0f
    private var shakeDuration = 0f
    private var shakeT = 0f
    // Hitstop (frame freeze on heavy impacts)
    private var hitstopFrames = 0

    // Fixed-timestep accumulator: physics steps run at a constant 1/60s rate
    // regardless of frame delta, keeping behavior consistent across hardware.
    private var physicsAccum = 0f

    private var isPaused = false
    private val pauseOverlay: PauseOverlay
    private var levelTimer = 0f
    private var levelCompletionTimer = 0f
    private var levelCompleted = false
    private val activatedCheckpoints = mutableSetOf<String>()
    private val ecoTokens = mutableListOf<EcoToken>()
    private var score = 0
    private var comboTimer = 0f
    private var comboMultiplier = 1
    private var cleanseRatio = 0f
    private var totalHazards = 0
    private var ecoRestoredAnnounced = false
    private val snapshotPickups = mutableListOf<SnapshotPickup>()
    private var atlasOverlay: CloudAtlasOverlay? = null
    private var levelCompleteOverlay: LevelCompleteOverlay? = null
    private var gameOverOverlay: GameOverOverlay? = null
    private var spiritHealth = 3
    private var isGameOver = false
    private var gameOverTimer = 0f
    private var isDisposed = false

    private val debugAutopilotEnabled = java.lang.Boolean.getBoolean("cloudy.autopilot")
    private val debugAutopilotSeconds = System.getProperty("cloudy.autopilotSeconds")?.toFloatOrNull()
        ?: Constants.AUTOPILOT_DEFAULT_SECONDS
    private val debugAutoQuitSeconds = System.getProperty("cloudy.autoquitSeconds")?.toFloatOrNull()
    private var debugAutopilotTimer = 0f
    private var debugAutoQuitTimer = debugAutoQuitSeconds

    // Autopilot state machine fields
    private var apLastX = 0f
    private var apStuckTimer = 0f          // time player hasn't moved right
    private var apJumpCooldown = 0f        // prevents continuous re-jump spam
    private var apPeriodicJumpTimer = 0f   // fires a preemptive jump every ~1.8s
    private var apAbilityTimer = 0f        // fires ability periodically

    /** Bodies queued for deletion next frame. Box2D forbids destroyBody during step()
     *  or contact callbacks. Add to this set from anywhere; it is drained once after
     *  world.step() each frame. */
    private val pendingBodyDestroy = mutableSetOf<Body>()

    fun queueBodyDestroy(body: Body) { pendingBodyDestroy.add(body) }

    private var perfLogTimer = 0f
    private var perfFrameCount = 0
    private var perfDeltaSum = 0f
    private var perfDeltaMax = 0f

    init {
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)

        world = World(Vector2(0f, Constants.GRAVITY), true)
        world.setContactListener(WorldContactListener())
        obstacleManager = ObstacleManager(world)

        shapeRenderer = ShapeRenderer()

        RayHandler.setGammaCorrection(true)
        RayHandler.useDiffuseLight(true)
        rayHandler = RayHandler(world).apply {
            setAmbientLight(0.15f, 0.18f, 0.25f, 0.7f)
        }

        level.setup(world, obstacleManager, movingPlatforms)

        val checkpoints = level.getCheckpoints()
        for (cp in checkpoints) {
            obstacleManager.addCheckpointNormalized(
                cp.name,
                cp.x * Constants.PPM / Constants.VIRTUAL_WIDTH,
                cp.y * Constants.PPM / Constants.VIRTUAL_HEIGHT,
                18f / Constants.VIRTUAL_WIDTH
            )
        }

        eboAbility = EboAbility(world)
        layaAbility = LayaAbility(world)

        player = PlayerController(world, level.spawnX, level.spawnY, eboAbility)
        eboAbility.setPlayerController(player)
        layaAbility.setPlayerController(player)

        // Restore from saved checkpoint if one was passed in (P0 fix)
        resumeCheckpoint?.let { cp ->
            player.setSpawn(cp)
            player.body.setTransform(cp.x, cp.y + 0.2f, 0f)
            player.body.linearVelocity = Vector2.Zero
        }

        playerLight = PointLight(rayHandler, 128, Color(0.9f, 0.95f, 1f, 0.85f), 4f, 0f, 0f)
        playerLight.attachToBody(player.body)

        player.onJump = {
            SoundManager.play("jump")
            spawnJumpPuff(player.body.position.x, player.body.position.y - 0.32f)
        }

        player.onFootstep = { fx, fy, _ ->
            spawnFootstep(fx, fy)
        }

        for (pos in level.getEcoTokenPositions()) {
            ecoTokens.add(EcoToken(world, pos.x, pos.y))
        }

        totalHazards = obstacleManager.rects().count { it.kind == ObstacleKind.HAZARD }
        snapshotPickups.addAll(level.getSnapshotPickups(world))

        hud = Hud(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        hud.onSwapCharacter = { switchCharacter() }
        Gdx.input.inputProcessor = hud.stage

        parallaxBg = ParallaxBackground()
        screenFade = ScreenFade(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        screenFade.fadeIn(speed = 1.5f)

        hud.updateSpiritHealth(spiritHealth)
        hud.showTransientMessage(level.name, 2.5f)

        pauseOverlay = PauseOverlay(
            onResume  = { setPaused(false) },
            onRestart = {
                if (game != null) { game.screen = GameScreen(level, game); dispose() }
            },
            onMainMenu = {
                if (game != null) { game.screen = MainMenuScreen(game); dispose() }
            }
        )

        if (debugAutopilotEnabled) {
            Gdx.app.log(
                "GameScreen",
                "Debug autopilot enabled for level=${level.id} (autopilotSeconds=$debugAutopilotSeconds, autoquitSeconds=$debugAutoQuitSeconds)"
            )
            InputManager.setDebugOverrideEnabled(true)
            InputManager.setDebugHeld(left = false, right = true, jump = false, action = false)
        }
    }

    private fun setPaused(paused: Boolean) {
        isPaused = paused
        Gdx.input.inputProcessor = if (paused) pauseOverlay.stage else hud.stage
    }

    fun removeObstacle(id: String): Boolean = obstacleManager.remove(id)

    fun clearObstacles() {
        obstacleManager.clear()
    }

    fun addRectObstacle(
        id: String,
        kind: ObstacleKind,
        xPx: Float,
        yPx: Float,
        halfWidthPx: Float,
        halfHeightPx: Float,
        sensor: Boolean = false
    ) {
        obstacleManager.addRectNormalized(
            id, kind,
            xPx / Constants.VIRTUAL_WIDTH,
            yPx / Constants.VIRTUAL_HEIGHT,
            halfWidthPx / Constants.VIRTUAL_WIDTH,
            halfHeightPx / Constants.VIRTUAL_HEIGHT,
            sensor
        )
    }

    fun addCheckpointObstacle(id: String, xPx: Float, yPx: Float, radiusPx: Float) {
        obstacleManager.addCheckpointNormalized(
            id,
            xPx / Constants.VIRTUAL_WIDTH,
            yPx / Constants.VIRTUAL_HEIGHT,
            radiusPx / Constants.VIRTUAL_WIDTH
        )
    }

    override fun show() {}

    override fun render(delta: Float) {
        val clampedDelta = delta.coerceAtMost(Constants.MAX_FRAME_DELTA)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            setPaused(!isPaused)
        }

        if (!isPaused && atlasOverlay == null) {
            if (hitstopFrames > 0) {
                hitstopFrames--
            } else {
                update(clampedDelta)
            }
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Single ShapeRenderer block: parallax background + all filled game objects.
        // Keeping one begin/end avoids repeated projection-matrix uploads via copyJni.
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Layer 0: parallax sky + terrain silhouettes
        parallaxBg.render(shapeRenderer, camera, cleanseRatio)

        // Layer 2: filled game objects

        shapeRenderer.color = Palette.DROPLET
        for (droplet in eboAbility.getActiveRaindrops()) {
            val pos = droplet.body.position
            shapeRenderer.circle(pos.x, pos.y, droplet.getRadius())
        }

        for (trail in layaAbility.getActiveWindTrails()) {
            val pos = trail.getCurrentPosition()
            val alpha = trail.getAlpha()
            Palette.tmpWindCol.set(1f, 1f, 1f, 0.6f * alpha)
            shapeRenderer.color = Palette.tmpWindCol
            shapeRenderer.circle(pos.x / Constants.PPM, pos.y / Constants.PPM, trail.getRadius())
        }

        for (rect in obstacleManager.rects()) {
            val ud = rect.fixture.userData as? String ?: ""
            val cx = rect.body.position.x
            val cy = rect.body.position.y
            val w  = rect.halfWidthPx / Constants.PPM
            val he = rect.halfHeightPx / Constants.PPM

            when {
                rect.kind == ObstacleKind.HAZARD && ud == "hazard_cleaned" -> {
                    shapeRenderer.color = Palette.HAZARD_CLEAN_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = Palette.HAZARD_CLEAN_GLEAM
                    shapeRenderer.rect(cx - w, cy + he - 0.04f, w * 2f, 0.04f)
                }
                rect.kind == ObstacleKind.HAZARD -> {
                    shapeRenderer.color = Palette.HAZARD_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = Palette.HAZARD_STRIPE
                    var sx = cx - w
                    while (sx < cx + w) {
                        shapeRenderer.rect(sx, cy - he, 0.05f, he * 2f)
                        sx += 0.2f
                    }
                }
                rect.kind == ObstacleKind.WALL -> {
                    shapeRenderer.color = Palette.WALL_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = Palette.WALL_EDGE
                    shapeRenderer.rect(cx - w, cy - he, 0.03f, he * 2f)
                }
                rect.kind == ObstacleKind.EXIT -> {
                    shapeRenderer.color = Palette.EXIT_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = Palette.EXIT_EDGE
                    shapeRenderer.rect(cx - w, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx + w - 0.05f, cy - he, 0.05f, he * 2f)
                }
                else -> {  // GROUND
                    shapeRenderer.color = Palette.GROUND_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = Palette.GROUND_TOP
                    shapeRenderer.rect(cx - w, cy + he - 0.05f, w * 2f, 0.05f)
                }
            }
        }

        // Moving platforms — earthy brown with top highlight
        for (mp in movingPlatforms) {
            val pos = mp.body.position
            val hw = 50f / Constants.PPM
            val hh = 10f / Constants.PPM
            shapeRenderer.color = Palette.MP_BASE
            shapeRenderer.rect(pos.x - hw, pos.y - hh, hw * 2f, hh * 2f)
            shapeRenderer.color = Palette.MP_TOP
            shapeRenderer.rect(pos.x - hw, pos.y + hh - 0.04f, hw * 2f, 0.04f)
        }

        // Checkpoints — pulsing beacon style
        for (cp in obstacleManager.checkpoints()) {
            val ud = cp.fixture.userData as? String ?: ""
            val activated = ud == "checkpoint_activated"
            val r = cp.radiusPx / Constants.PPM
            shapeRenderer.color = if (activated) Palette.CP_GLOW_ACTIVE else Palette.CP_GLOW_INACTIVE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r * 1.5f)
            shapeRenderer.color = if (activated) Palette.CP_BODY_ACTIVE else Palette.CP_BODY_INACTIVE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r)
            shapeRenderer.color = Color.WHITE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r * 0.35f)
        }

        // Eco-tokens with pulsing animation
        shapeRenderer.color = Palette.TOKEN
        for (token in ecoTokens) {
            if (!token.isCollected) {
                val p = token.body.position
                shapeRenderer.circle(p.x, p.y, token.getAnimatedRadius())
            }
        }

        // Cloud Atlas snapshots — 4-point diamond star
        for (snap in snapshotPickups) {
            if (!snap.isCollected) {
                val p = snap.body.position
                val r = snap.getAnimatedRadius()
                val ri = r * 0.45f
                shapeRenderer.color = Palette.SNAPSHOT_GLOW
                shapeRenderer.circle(p.x, p.y, r * 1.4f)
                shapeRenderer.color = Palette.SNAPSHOT_BODY
                shapeRenderer.triangle(p.x, p.y + r,  p.x + ri, p.y,      p.x, p.y - r)
                shapeRenderer.triangle(p.x, p.y + r,  p.x - ri, p.y,      p.x, p.y - r)
                // Horizontal bar to make it a 4-point star
                shapeRenderer.triangle(p.x - r, p.y,  p.x, p.y + ri,  p.x + r, p.y)
                shapeRenderer.triangle(p.x - r, p.y,  p.x, p.y - ri,  p.x + r, p.y)
            }
        }

        // Particles (dust, jump puffs, sparkles) — must render with same SR/begin block
        // since they need alpha; ShapeType.Filled supports it via OpenGL blend.
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        particles.render(shapeRenderer)

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // Layer 2b: player sprite (on top of terrain, behind lighting)
        val playerPos = player.body.position
        val flashVisible = !player.isFlashing || (player.deathFlashTimer * 8).toInt() % 2 == 0
        if (flashVisible) {
            val animator = if (currentCharacter == "Ebo") eboAnimator else layaAnimator
            val frame = animator.getCurrentFrame()
            val sw = SpriteFactory.SPRITE_W / Constants.PPM
            val sh = SpriteFactory.SPRITE_H / Constants.PPM
            val sx = playerPos.x - sw / 2f
            val sy = playerPos.y - 32f / Constants.PPM  // align bottom to physics box bottom
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            spriteBatch.projectionMatrix = camera.combined
            spriteBatch.begin()
            if (player.isFlashing) spriteBatch.setColor(1f, 0.35f, 0.35f, 0.85f)
            if (player.isFacingRight) {
                spriteBatch.draw(frame, sx, sy, sw, sh)
            } else {
                spriteBatch.draw(frame, sx + sw, sy, -sw, sh)
            }
            if (player.isFlashing) spriteBatch.setColor(Color.WHITE)
            spriteBatch.end()
            Gdx.gl.glDisable(GL20.GL_BLEND)
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

        // Layer 5: screen fade overlay (on top of everything)
        screenFade.render()

        // Layer 6: Cloud Atlas card overlay (pauses action while showing)
        atlasOverlay?.render()

        // Layer 7: pause overlay (drawn last so it blocks all interaction)
        if (isPaused) pauseOverlay.render()

        // Layer 8: level-complete card (shown above everything, including pause)
        levelCompleteOverlay?.render()

        // Layer 9: game-over card
        gameOverOverlay?.render()

        // Game over transition — always at the tail of render() so dispose() is never called
        // while bodies are still being accessed mid-frame (MovingPlatform, player, etc.)
        if (isGameOver && gameOverTimer <= 0f && game != null) {
            game.screen = MainMenuScreen(game)
            dispose()
            return
        }

        if (levelCompleted) {
            levelCompletionTimer -= clampedDelta
            if (levelCompletionTimer <= 0f) {
                goToNextLevel()
            }
        }
    }

    private fun update(delta: Float) {
        InputManager.update()
        screenFade.update(delta)

        perfLogTimer += delta
        perfFrameCount += 1
        perfDeltaSum += delta
        if (delta > perfDeltaMax) perfDeltaMax = delta
        if (perfLogTimer >= Constants.PERF_LOG_INTERVAL_SECONDS) {
            val avgDelta = if (perfFrameCount > 0) perfDeltaSum / perfFrameCount else 0f
            val fps = if (avgDelta > 0f) (1f / avgDelta) else 0f
            Gdx.app.log(
                "Perf",
                "fps=%.1f avgDelta=%.4f maxDelta=%.4f playerX=%.2f playerY=%.2f level=%s".format(
                    fps, avgDelta, perfDeltaMax,
                    player.body.position.x, player.body.position.y, level.id
                )
            )
            perfLogTimer = 0f; perfFrameCount = 0; perfDeltaSum = 0f; perfDeltaMax = 0f
        }

        if (debugAutopilotEnabled) {
            debugAutopilotTimer += delta

            val apActive = debugAutopilotTimer < debugAutopilotSeconds && !isGameOver

            if (apActive) {
                val playerX = player.body.position.x
                val onGround = player.isGrounded
                val touchWall = player.isTouchingWallLeft || player.isTouchingWallRight

                // Coyote-window edge detection: player just stepped off a ledge while
                // moving right — fire jump immediately so we still clear the gap.
                // prevGrounded is already tracked by the landing-dust system.
                val justLeftGround = !onGround && prevGrounded && player.body.linearVelocity.y >= -1.5f

                // Stuck detection: if X hasn't advanced > 0.05m in 0.35s, trigger jump
                if (playerX > apLastX + 0.05f) {
                    apLastX = playerX
                    apStuckTimer = 0f
                } else {
                    apStuckTimer += delta
                }
                val isStuck = apStuckTimer > 0.35f

                // Periodic preemptive jump — 0.8s keeps the player hopping so gaps
                // are cleared without relying solely on edge detection.
                apPeriodicJumpTimer -= delta
                val periodicJump = apPeriodicJumpTimer <= 0f && onGround
                if (periodicJump) apPeriodicJumpTimer = 0.8f

                // Ability fire every ~4s
                apAbilityTimer -= delta
                val fireAbility = apAbilityTimer <= 0f
                if (fireAbility) apAbilityTimer = 4.0f

                // Jump cooldown prevents spamming; coyote trigger skips cooldown check
                // because the window is only one frame wide.
                if (apJumpCooldown > 0f) apJumpCooldown -= delta
                val wantJump = justLeftGround ||
                    (apJumpCooldown <= 0f && (isStuck || touchWall || periodicJump))
                if (wantJump) {
                    apJumpCooldown = 0.4f
                    apStuckTimer = 0f
                    apLastX = playerX
                    InputManager.triggerDebugJumpJustPressed()
                }

                if (fireAbility) InputManager.triggerDebugActionJustPressed()

                InputManager.setDebugHeld(left = false, right = true, jump = false, action = false)
            } else {
                InputManager.setDebugHeld(left = false, right = false, jump = false, action = false)
            }

            if (debugAutoQuitTimer != null) {
                debugAutoQuitTimer = (debugAutoQuitTimer ?: 0f) - delta
                if ((debugAutoQuitTimer ?: 0f) <= 0f) {
                    Gdx.app.log("GameScreen", "Auto-quit timer reached; exiting.")
                    Gdx.app.exit()
                }
            }
        }

        player.update(delta)

        val vel = player.body.linearVelocity
        val onWall = player.isTouchingWallLeft || player.isTouchingWallRight
        eboAnimator.update(delta, player.isGrounded, vel.x, vel.y, onWall)
        layaAnimator.update(delta, player.isGrounded, vel.x, vel.y, onWall)

        hud.update(delta)

        if (!levelCompleted && !isGameOver) {
            levelTimer += delta
            hud.updateTimer(levelTimer)
        }

        // Eco-token progress bar (collected = initial total minus still-alive list)
        val totalEco = level.getEcoTokenPositions().size
        if (totalEco > 0) {
            val collected = totalEco - ecoTokens.size
            hud.updateProgress(collected.toFloat() / totalEco.toFloat())
        }

        hud.updateAbilityState(
            player.ability?.getCooldownRatio() ?: 0f,
            currentCharacter,
            player.ability?.getAbilityName() ?: ""
        )

        for (mp in movingPlatforms) mp.update(delta)

        // Fixed-timestep physics: run as many 1/60s steps as fit in this frame's
        // delta. Cap iterations to 5 to prevent the spiral-of-death after big
        // pauses (focus loss / GC spikes). MAX_FRAME_DELTA already clamps delta.
        physicsAccum += delta
        var stepsThisFrame = 0
        while (physicsAccum >= Constants.TIME_STEP && stepsThisFrame < 5) {
            world.step(Constants.TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS)
            physicsAccum -= Constants.TIME_STEP
            stepsThisFrame++
        }
        if (stepsThisFrame == 5) physicsAccum = 0f  // bail on the rest if we're really far behind

        // Drain dead WaterDroplets AFTER world.step() — avoids Box2D use-after-free.
        eboAbility.drainDeadDroplets { queueBodyDestroy(it) }

        // Note: pendingBodyDestroy is drained at the END of update() so anything
        // queued during this frame's logic (token/snapshot collection) is freed
        // before next frame's world.step().

        // Particles tick + landing detection
        particles.update(delta)
        val curVy = player.body.linearVelocity.y
        val curGrounded = player.isGrounded
        if (!prevGrounded && curGrounded && prevPlayerVy < -8f) {
            spawnLandingDust(player.body.position.x, player.body.position.y - 0.32f, prevPlayerVy)
            SoundManager.play("land")
            // Tiny shake scaled with how hard you landed
            val mag = ((-prevPlayerVy - 8f) / 20f).coerceIn(0f, 0.08f)
            if (mag > 0.02f) triggerShake(intensityMeters = mag, durationSec = 0.10f)
        }
        prevPlayerVy = curVy
        prevGrounded = curGrounded

        if (switchCooldownTimer > 0f) {
            switchCooldownTimer -= delta
            if (switchCooldownTimer <= 0f) canSwitchCharacter = true
        }

        val assistSettings = SettingsManager.load()
        val playerDied = !isGameOver && !assistSettings.assistInvincible &&
            (player.isDead || player.body.position.y < -10f / Constants.PPM)
        if (playerDied) {
            SoundManager.play("death")
            triggerShake(intensityMeters = 0.18f, durationSec = 0.25f)
            triggerHitstop(frames = 5)
            // Death sparkle burst
            spawnCollectSparkle(player.body.position.x, player.body.position.y, Color(1f, 0.3f, 0.3f, 0.95f))

            if (!assistSettings.assistInfiniteSpirits) {
                spiritHealth--
                hud.updateSpiritHealth(spiritHealth)
            }

            if (spiritHealth <= 0 && !assistSettings.assistInfiniteSpirits) {
                isGameOver = true
                gameOverTimer = 4f
                hud.showTransientMessage("Spirit Exhausted...", 2f)
                gameOverOverlay = GameOverOverlay(
                    onRestart  = { if (game != null) { game.screen = GameScreen(level, game); dispose() } },
                    onMainMenu = { if (game != null) { game.screen = MainMenuScreen(game); dispose() } }
                )
                Gdx.input.inputProcessor = gameOverOverlay!!.stage
            } else {
                hud.showTransientMessage("$currentCharacter fell  ($spiritHealth spirits left)", 1.2f)
            }
            // Restore from checkpoint autosave if one exists; otherwise respawn at
            // the level's default spawn position set during world setup.
            val cpSave = SaveManager.loadGame(CHECKPOINT_AUTOSAVE_FILE)
            val hasCpSave = SaveManager.listSaves().contains(CHECKPOINT_AUTOSAVE_FILE)
            if (hasCpSave && cpSave.checkpoint.levelName == level.id &&
                (cpSave.checkpoint.x != 0f || cpSave.checkpoint.y != 0f)
            ) {
                val cpPos = Vector2(cpSave.checkpoint.x, cpSave.checkpoint.y)
                player.setSpawn(cpPos)
                // collectedAtlasIds: union of current session and saved — never lose
                // items collected before death that were already persisted.
                score = cpSave.bestScores[level.id]?.coerceAtMost(score) ?: score
                hud.updateScore(score)
            }
            player.respawn()
        } else if (!isGameOver && assistSettings.assistInvincible &&
                   player.body.position.y < -10f / Constants.PPM) {
            // Invincible mode: still respawn if player falls off-screen, no spirit loss.
            player.respawn()
        }

        if (isGameOver) {
            gameOverTimer -= delta
            // Transition is handled in render() so dispose() is never called mid-frame.
        }

        // Camera: dead-zone follow + forward focus (Itay Keren / Celeste style),
        // with level-bounds clamping. Forward bias reveals what's ahead in the
        // direction of motion; dead zone prevents nervous bobbing on idle/small jumps.
        val halfW = viewport.worldWidth / 2f
        val halfH = viewport.worldHeight / 2f
        val levelW = level.levelWidthPx / Constants.PPM
        val playerX = player.body.position.x
        val playerY = player.body.position.y

        if (!camInitialized) {
            camTarget.set(playerX, playerY + 100f / Constants.PPM)
            cameraTargetY = playerY + 1.0f
            camInitialized = true
        }

        val biasX = if (player.isFacingRight) camForwardOffset else -camForwardOffset
        val desiredX = playerX + biasX
        val dx = desiredX - camTarget.x
        if (kotlin.math.abs(dx) > camDeadZoneHalfW) {
            // Pull target toward desired, but only past the dead-zone edge
            val excess = kotlin.math.abs(dx) - camDeadZoneHalfW
            camTarget.x += kotlin.math.sign(dx) * excess
        }
        // Vertical platform snap (§4.3): only update the committed target Y when the
        // player is grounded or falling fast (vy < threshold). During a jump arc the
        // target freezes so the camera doesn't bob up and down with every hop.
        val vy = player.body.linearVelocity.y
        if (player.isGrounded || vy < camVertSnapFallThreshold) {
            cameraTargetY = playerY + 1.0f
        }
        camTarget.y += (cameraTargetY - camTarget.y) * (camLerpSpeed * delta).coerceAtMost(1f)

        // Smooth horizontal lerp toward target so dead-zone exit doesn't snap.
        camera.position.x += (camTarget.x - camera.position.x) * (camLerpSpeed * delta).coerceAtMost(1f)
        camera.position.y = camTarget.y

        // Bounds clamp
        camera.position.x = camera.position.x.coerceIn(halfW, (levelW - halfW).coerceAtLeast(halfW))
        camera.position.y = camera.position.y.coerceAtLeast(halfH)

        // Screen shake decay + offset
        if (shakeDuration > 0f) {
            shakeT += delta
            shakeDuration -= delta
            val falloff = (shakeDuration / 0.2f).coerceIn(0f, 1f)
            val offX = com.badlogic.gdx.math.MathUtils.sin(shakeT * 60f) * shakeIntensity * falloff
            val offY = com.badlogic.gdx.math.MathUtils.cos(shakeT * 73f) * shakeIntensity * falloff
            camera.position.x += offX
            camera.position.y += offY
            if (shakeDuration <= 0f) {
                shakeIntensity = 0f
                shakeT = 0f
            }
        }
        camera.update()

        if (Gdx.input.isKeyJustPressed(Input.Keys.S) && canSwitchCharacter) switchCharacter()

        // Combo timer tick
        if (comboTimer > 0f) {
            comboTimer -= delta
            if (comboTimer <= 0f) comboMultiplier = 1
        }

        // Collect eco-tokens (destroy bodies via the deferred queue, never inline)
        val collected = ecoTokens.filter { it.isCollected }
        if (collected.isNotEmpty()) {
            if (comboTimer > 0f) {
                comboMultiplier = (comboMultiplier + 1).coerceAtMost(4)
            }
            score += collected.size * 10 * comboMultiplier
            comboTimer = 1.5f
            hud.updateScore(score)
            SoundManager.play("collect")
            if (comboMultiplier > 1) hud.showCombo(comboMultiplier)
            collected.forEach {
                // Capture position BEFORE queuing destroy
                spawnTokenSparkle(it.body.position.x, it.body.position.y)
                queueBodyDestroy(it.body)
            }
            ecoTokens.removeAll(collected.toSet())
        }

        // Animate remaining tokens
        for (token in ecoTokens) {
            if (!token.isCollected) token.update(delta)
        }

        // Cloud Atlas snapshots — detect collection, show overlay, persist
        val collectedSnap = snapshotPickups.firstOrNull { it.isCollected }
        if (collectedSnap != null && atlasOverlay == null) {
            SoundManager.play("collect")
            score += 25  // bonus score for a snapshot
            hud.updateScore(score)
            spawnSnapshotSparkle(collectedSnap.body.position.x, collectedSnap.body.position.y)
            snapshotPickups.remove(collectedSnap)
            queueBodyDestroy(collectedSnap.body)
            // Persist this entry to GameState.collectedAtlasIds (P0 fix)
            val existing = SaveManager.loadGame()
            if (collectedSnap.entry.id !in existing.collectedAtlasIds) {
                SaveManager.saveGame(
                    existing.copy(collectedAtlasIds = existing.collectedAtlasIds + collectedSnap.entry.id)
                )
            }
            atlasOverlay = CloudAtlasOverlay(collectedSnap.entry) {
                atlasOverlay?.dispose()
                atlasOverlay = null
                Gdx.input.inputProcessor = hud.stage
            }
            Gdx.input.inputProcessor = atlasOverlay!!.stage
        }
        for (snap in snapshotPickups) snap.update(delta)

        // Hazard cleanse events → particle burst + sound
        val cleanseEvents = CleanseEventQueue.drain()
        for (pos in cleanseEvents) {
            spawnCleanseburst(pos.x, pos.y)
            SoundManager.play("cleanse", pitch = com.badlogic.gdx.math.MathUtils.random(0.9f, 1.1f))
        }

        // World-state persistence: update cleanse ratio
        if (totalHazards > 0) {
            val cleansedCount = obstacleManager.rects().count {
                it.kind == ObstacleKind.HAZARD && it.fixture.userData == "hazard_cleaned"
            }
            cleanseRatio = cleansedCount.toFloat() / totalHazards.toFloat()
            if (cleanseRatio >= 1f && !ecoRestoredAnnounced) {
                ecoRestoredAnnounced = true
                hud.showTransientMessage("Eco-System Restored!", 2.5f)
            }
        }

        // Checkpoint activation → auto-save
        for (cp in obstacleManager.checkpoints()) {
            if (cp.fixture.userData == "checkpoint_activated" && cp.id !in activatedCheckpoints) {
                activatedCheckpoints.add(cp.id)
                SoundManager.play("checkpoint")
                // Checkpoints restore one spirit pip (up to max 3)
                if (spiritHealth < 3) {
                    spiritHealth = (spiritHealth + 1).coerceAtMost(3)
                    hud.updateSpiritHealth(spiritHealth)
                }
                // Merge with existing save to preserve completedLevels, bestScores, and
                // collectedAtlasIds gathered so far this session (including just-collected ones).
                val existing = SaveManager.loadGame()
                val cpState = existing.copy(
                    level = level.id,
                    characterName = currentCharacter,
                    checkpoint = com.sohai.platformer.persist.Checkpoint(
                        levelName = level.id,
                        x = player.spawnPos.x,
                        y = player.spawnPos.y
                    )
                )
                // Write to both the main slot and the dedicated autosave so the player
                // can resume from here if they quit, and respawn here on death.
                SaveManager.saveGame(cpState)
                SaveManager.saveGame(cpState, CHECKPOINT_AUTOSAVE_FILE)
            }
        }

        if (!levelCompleted && player.hasReachedExit) {
            Gdx.app.log("GameScreen", "Level complete via exit sensor, level=${level.id}")
            completeLevel()
        }

        // === END OF FRAME: drain deferred body destructions ===
        // Anything queued from contact handlers, collection logic, or ability
        // code is destroyed here, safely outside world.step() and outside any
        // body iteration above.
        if (pendingBodyDestroy.isNotEmpty()) {
            for (b in pendingBodyDestroy) world.destroyBody(b)
            pendingBodyDestroy.clear()
        }
    }

    private fun completeLevel() {
        levelCompleted = true
        levelCompletionTimer = 4f
        SoundManager.play("level_complete")
        screenFade.fadeOut(speed = 0.4f)

        val totalEco = level.getEcoTokenPositions().size
        val ecoCollected = totalEco - ecoTokens.size
        levelCompleteOverlay = LevelCompleteOverlay(
            levelName    = level.name,
            timeSeconds  = levelTimer,
            score        = score,
            ecoCollected = ecoCollected,
            ecoTotal     = totalEco,
            onContinue   = { levelCompletionTimer = 0f }
        )
        Gdx.input.inputProcessor = levelCompleteOverlay!!.stage

        // Persist level completion and best score
        val existing = SaveManager.loadGame()
        val newCompleted = existing.completedLevels + level.id
        val prevBest = existing.bestScores[level.id] ?: 0
        val newBest = existing.bestScores + (level.id to maxOf(prevBest, score))
        SaveManager.saveGame(existing.copy(completedLevels = newCompleted, bestScores = newBest))

        // Level is done — the checkpoint autosave is no longer valid; delete it so
        // that starting the next level (or replaying this one) begins clean.
        SaveManager.deleteSave(CHECKPOINT_AUTOSAVE_FILE)
    }

    private fun switchCharacter() {
        currentCharacter = if (currentCharacter == "Ebo") {
            player.changeAbility(layaAbility)
            "Laya"
        } else {
            player.changeAbility(eboAbility)
            "Ebo"
        }
        canSwitchCharacter = false
        switchCooldownTimer = 1.0f

        val burstColor = if (currentCharacter == "Ebo")
            Color(0.83f, 0.57f, 0.29f, 0.9f)
        else
            Color(0.29f, 0.50f, 0.66f, 0.9f)
        spawnCollectSparkle(player.body.position.x, player.body.position.y, burstColor)
        val abilityName = if (currentCharacter == "Ebo") "Seed Slam" else "Wind Dash"
        hud.showTransientMessage("$currentCharacter: $abilityName", 0.8f)
    }

    // === Screen shake / hitstop ============================================

    private fun triggerShake(intensityMeters: Float, durationSec: Float) {
        // Respect accessibility setting: the user can disable screen shake entirely.
        if (!SettingsManager.load().screenShake) return
        shakeIntensity = maxOf(shakeIntensity, intensityMeters)
        shakeDuration = maxOf(shakeDuration, durationSec)
    }

    private fun triggerHitstop(frames: Int) {
        hitstopFrames = maxOf(hitstopFrames, frames)
    }

    // === Particle helpers ==================================================

    /**
     * T-011: spawn one small dust particle at a foot position for a footstep.
     * Caller (PlayerController) supplies the already-offset L/R position.
     * No movement, no gravity, short lifespan — pure visual cue for walking cadence.
     */
    private fun spawnFootstep(x: Float, y: Float) {
        particles.spawn(
            x, y,
            vx = 0f, vy = 0f,
            radius = 0.05f,
            life = 0.2f,
            color = footstepColor,
            gravity = 0f
        )
    }

    private fun spawnJumpPuff(x: Float, y: Float) {
        val col = if (currentCharacter == "Ebo")
            Color(0.7f, 0.55f, 0.35f, 0.8f)
        else
            Color(0.9f, 0.95f, 1f, 0.8f)
        for (i in 0..2) {
            val ang = (com.badlogic.gdx.math.MathUtils.random() * 1.2f) + 0.2f  // 0.2 - 1.4 rad above horizontal
            val sign = if (com.badlogic.gdx.math.MathUtils.randomBoolean()) -1f else 1f
            val speed = com.badlogic.gdx.math.MathUtils.random(0.4f, 0.8f)
            particles.spawn(
                x + com.badlogic.gdx.math.MathUtils.random(-0.05f, 0.05f),
                y + com.badlogic.gdx.math.MathUtils.random(-0.02f, 0.04f),
                vx = sign * com.badlogic.gdx.math.MathUtils.cos(ang) * speed,
                vy = com.badlogic.gdx.math.MathUtils.sin(ang) * speed * 0.6f,
                radius = com.badlogic.gdx.math.MathUtils.random(0.04f, 0.08f),
                life = 0.18f,
                color = col,
                gravity = 0f
            )
        }
    }

    private fun spawnLandingDust(x: Float, y: Float, fallSpeed: Float) {
        val intensity = (-fallSpeed / 18f).coerceIn(0.5f, 1.5f)  // scale with how hard the landing was
        val count = (5 * intensity).toInt().coerceIn(4, 8)
        val col = Color(0.55f, 0.50f, 0.42f, 0.85f)  // tan dust
        for (i in 0 until count) {
            val sign = if (i % 2 == 0) -1f else 1f
            val outward = com.badlogic.gdx.math.MathUtils.random(0.6f, 1.4f) * intensity
            particles.spawn(
                x + com.badlogic.gdx.math.MathUtils.random(-0.08f, 0.08f),
                y,
                vx = sign * outward,
                vy = com.badlogic.gdx.math.MathUtils.random(0.1f, 0.5f),
                radius = com.badlogic.gdx.math.MathUtils.random(0.05f, 0.10f),
                life = com.badlogic.gdx.math.MathUtils.random(0.25f, 0.40f),
                color = col,
                gravity = 1f  // mild fall
            )
        }
    }

    private fun spawnCleanseburst(x: Float, y: Float) {
        // Green-blue water burst when a hazard is cleansed
        val col = Color(0.25f, 0.85f, 0.60f, 0.9f)
        for (i in 0 until 12) {
            val ang   = com.badlogic.gdx.math.MathUtils.random() * com.badlogic.gdx.math.MathUtils.PI2
            val speed = com.badlogic.gdx.math.MathUtils.random(1.0f, 2.8f)
            particles.spawn(
                x, y,
                vx = com.badlogic.gdx.math.MathUtils.cos(ang) * speed,
                vy = com.badlogic.gdx.math.MathUtils.sin(ang) * speed + 0.5f,
                radius = com.badlogic.gdx.math.MathUtils.random(0.06f, 0.12f),
                life   = com.badlogic.gdx.math.MathUtils.random(0.35f, 0.60f),
                color  = col,
                gravity = 2f
            )
        }
    }

    private fun spawnCollectSparkle(x: Float, y: Float, color: Color) {
        for (i in 0 until 8) {
            val ang = com.badlogic.gdx.math.MathUtils.random() * com.badlogic.gdx.math.MathUtils.PI2
            val speed = com.badlogic.gdx.math.MathUtils.random(0.8f, 2.0f)
            particles.spawn(
                x, y,
                vx = com.badlogic.gdx.math.MathUtils.cos(ang) * speed,
                vy = com.badlogic.gdx.math.MathUtils.sin(ang) * speed,
                radius = com.badlogic.gdx.math.MathUtils.random(0.05f, 0.09f),
                life = 0.4f,
                color = color,
                gravity = 1.5f
            )
        }
    }

    /**
     * T-019: Cyan sparkle burst on eco-token pickup.
     * 6–10 particles, upward-biased velocity, slight upward gravity (negative = lift),
     * short lifespan with alpha-fade. Reuses the existing 200-particle pool.
     */
    private fun spawnTokenSparkle(x: Float, y: Float) {
        val count = 6 + (Math.random() * 5).toInt()   // 6–10
        for (i in 0 until count) {
            particles.spawn(
                x + (Math.random() * 0.12 - 0.06).toFloat(),
                y + (Math.random() * 0.08).toFloat(),
                vx = (Math.random() * 1.0 - 0.5).toFloat(),
                vy = (0.8 + Math.random() * 0.4).toFloat(),
                radius = 0.05f,
                life = (0.35 + Math.random() * 0.10).toFloat(),
                color = Palette.SPARKLE_TOKEN,
                gravity = -2f
            )
        }
    }

    /**
     * T-019: Yellow sparkle burst on Cloud Atlas snapshot pickup.
     * Same spread as token sparkles but larger radius and longer lifespan
     * to distinguish the rarer, more important pickup.
     */
    private fun spawnSnapshotSparkle(x: Float, y: Float) {
        val count = 6 + (Math.random() * 5).toInt()   // 6–10
        for (i in 0 until count) {
            particles.spawn(
                x + (Math.random() * 0.12 - 0.06).toFloat(),
                y + (Math.random() * 0.08).toFloat(),
                vx = (Math.random() * 1.0 - 0.5).toFloat(),
                vy = (0.8 + Math.random() * 0.4).toFloat(),
                radius = 0.07f,
                life = 0.5f,
                color = Palette.SPARKLE_SNAPSHOT,
                gravity = -2f
            )
        }
    }

    private fun goToNextLevel() {
        val nextLevel = LevelManager.getNextLevel(level.id)
        if (nextLevel != null && game != null) {
            game.screen = GameScreen(nextLevel, game)
        } else if (game != null) {
            game.screen = VictoryScreen(game, score)
        }
        dispose()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        hud.resize(width, height)
        pauseOverlay.resize(width, height)
        atlasOverlay?.resize(width, height)
        levelCompleteOverlay?.resize(width, height)
        gameOverOverlay?.resize(width, height)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        snapshotPickups.forEach { world.destroyBody(it.body) }
        snapshotPickups.clear()
        atlasOverlay?.dispose()
        atlasOverlay = null
        levelCompleteOverlay?.dispose()
        levelCompleteOverlay = null
        gameOverOverlay?.dispose()
        gameOverOverlay = null
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

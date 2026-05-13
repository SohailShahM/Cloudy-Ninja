package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.abilities.EboAbility
import com.sohai.platformer.abilities.LayaAbility
import com.sohai.platformer.abilities.ZephyrAbility
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.entities.DeathCause
import com.sohai.platformer.entities.DriftHusk
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.Projectile
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.entities.StormSentinel
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.input.InputManager
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.Level0_0
import com.sohai.platformer.persist.Checkpoint
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.progression.AchievementInputs
import com.sohai.platformer.progression.AchievementPredicates
import com.sohai.platformer.progression.AchievementUnlocker
import com.sohai.platformer.physics.CleanseEventQueue
import com.sohai.platformer.rendering.CharacterAnimator
import com.sohai.platformer.rendering.ParticleSystem
import com.sohai.platformer.rendering.ScreenFade
import com.sohai.platformer.rendering.ScreenShake
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Owns all mutable per-session state (score, spirit health, combo, timers,
 * completion flags, camera target) plus the main [update] loop.
 *
 * Side effects that require GameScreen-level objects (overlays, input-processor
 * changes, body disposal) are routed through callbacks set during construction.
 */
class LevelRunState(
    private val level: Level,
    private val player: PlayerController,
    private val eboAbility: EboAbility,
    private val layaAbility: LayaAbility,
    private val zephyrAbility: ZephyrAbility,
    private val ecoTokens: MutableList<EcoToken>,
    private val snapshotPickups: MutableList<SnapshotPickup>,
    private val obstacleManager: ObstacleManager,
    private val movingPlatforms: MutableList<MovingPlatform>,
    private val world: World,
    private val hud: Hud,
    private val game: Game?,
    private val particles: ParticleSystem,
    private val screenFade: ScreenFade,
    private val eboAnimator: CharacterAnimator,
    private val layaAnimator: CharacterAnimator,
    private val camera: OrthographicCamera,
    private val pendingBodyDestroy: MutableSet<Body>,
    private val renderer: LevelRenderer,
    val enemies: MutableList<Enemy>,
    private val checkpointAutosaveFile: String,
    /** When true: timer counts up from 0, checkpoint autosaves are suppressed. */
    val isTimeTrial: Boolean = false,
    private val achievementToast: AchievementToast? = null,
    /** Save slot filename used for achievement persistence (must match the slot used in GameScreen). */
    private val saveSlotFile: String = "save_slot_1.json",
    /**
     * T-062: Drift Husks (drop-from-above enemies). Held parallel to [enemies]
     * because they require per-frame `setPlayerX(...)` to evaluate their
     * trigger band. Defeat is handled identically to SmogSprite.
     */
    val driftHusks: MutableList<DriftHusk> = mutableListOf()
) {

    // ── Game-session state ────────────────────────────────────────────────────

    var score = 0
    var comboTimer = 0f
    var comboMultiplier = 1
    var cleanseRatio = 0f
    var totalHazards = 0
    var ecoRestoredAnnounced = false
    var spiritHealth = 3
    var isGameOver = false
    var gameOverTimer = 0f
    var levelTimer = 0f
    var levelCompleted = false
    var levelCompletionTimer = 0f
    val activatedCheckpoints = mutableSetOf<String>()

    // ── Per-run stats (T-130: feeds the death-recap overlay) ──────────────────
    /** Stomps performed during the current run (resets when [LevelRunState] is rebuilt). */
    var stompsThisRun: Int = 0
        private set
    /** Eco-tokens collected during the current run (resets when [LevelRunState] is rebuilt). */
    var tokensThisRun: Int = 0
        private set
    /** Cause of the most recent death this run. Updated immediately on death detection. */
    var lastDeathCause: DeathCause = DeathCause.ENEMY
        private set

    // ── Boss ─────────────────────────────────────────────────────────────────

    /**
     * The level boss, if any. Set by [GameScreen] after construction.
     * Callbacks ([StormSentinel.onSpawnProjectile], [StormSentinel.onDefeated]) are
     * also wired by GameScreen so they can reference runState.levelCompleted.
     */
    var sentinel: StormSentinel? = null

    // ── Achievement tracking (fire-once flags per run) ────────────────────────

    private var achievFirstJumpFired    = false
    private var achievFirstCleanseFired = false
    private var achievEcoSweepFired     = false
    private var achievNoDeathFired      = false
    private var achievFirstEnemyFired   = false

    // ── Projectiles ──────────────────────────────────────────────────────────

    val projectiles = mutableListOf<Projectile>()

    // ── Character switching ───────────────────────────────────────────────────

    var currentCharacter = "Ebo"
        private set
    var canSwitchCharacter = true
    var switchCooldownTimer = 0f

    // ── Camera tracking ───────────────────────────────────────────────────────

    private val camTarget               = Vector2()
    private val camDeadZoneHalfW        = 1.0f
    private val camForwardOffset        = 1.5f
    private val camLerpSpeed            = 5f
    private var camInitialized          = false
    private val camVertSnapFallThreshold = -3f
    private var cameraTargetY           = 0f

    // ── Hitstop ───────────────────────────────────────────────────────────────
    // (Screen-shake state lives in the [ScreenShake] singleton since T-169 —
    // the local shakeIntensity/shakeDuration/shakeT fields and the
    // triggerShake() helper were removed when the two shake systems were
    // unified onto ScreenShake.trigger(...).)

    var hitstopFrames   = 0

    // ── Death animation (T-097) ───────────────────────────────────────────────
    // While [deathAnimT] is in (0, DEATH_ANIM_DURATION) the player is "dying":
    // its sprite fades out, the camera zooms out, and physics is frozen.
    // At completion we run the existing checkpoint-respawn logic and trigger a
    // brief screen-flash via [screenFade]. Gated behind reducedMotion + SMOKE_MODE
    // for byte-identical behaviour when accessibility is enabled or under CI.
    private companion object {
        const val DEATH_ANIM_DURATION = 0.5f
        const val DEATH_ZOOM_AMOUNT   = 0.2f      // camera.zoom goes 1.0 → 1.2
        const val DEATH_FLASH_SPEED   = 5f        // ScreenFade.fadeToBlack: 0→1 in 0.2s
    }
    private var deathAnimT: Float = 0f
    private var deathAnimActive = false

    // ── Physics accumulator ───────────────────────────────────────────────────

    var physicsAccum = 0f

    // ── Landing-dust helpers ──────────────────────────────────────────────────

    var prevPlayerVy  = 0f
    var prevGrounded  = false

    // ── Performance logging ───────────────────────────────────────────────────

    private var perfLogTimer    = 0f
    private var perfFrameCount  = 0
    private var perfDeltaSum    = 0f
    private var perfDeltaMax    = 0f

    // ── Debug autopilot ───────────────────────────────────────────────────────

    private val debugAutopilotEnabled  = java.lang.Boolean.getBoolean("cloudy.autopilot")
    private val debugAutopilotSeconds  = System.getProperty("cloudy.autopilotSeconds")?.toFloatOrNull()
        ?: Constants.AUTOPILOT_DEFAULT_SECONDS
    private val debugAutoQuitSeconds   = System.getProperty("cloudy.autoquitSeconds")?.toFloatOrNull()
    private var debugAutopilotTimer    = 0f
    private var debugAutoQuitTimer     = debugAutoQuitSeconds
    private var apLastX                = 0f
    private var apStuckTimer           = 0f
    private var apJumpCooldown         = 0f
    private var apPeriodicJumpTimer    = 0f
    private var apAbilityTimer         = 0f
    private val debugSmokeMode         = java.lang.Boolean.getBoolean("cloudy.smokeMode")
    private var smokeStartX            = Float.NaN
    private var smokeMaxX              = Float.NEGATIVE_INFINITY
    private var smokeFrameTimes        = FloatArray(7200)   // ~120s at 60Hz; we never need more
    private var smokeFrameIdx          = 0

    // ── Side-effect callbacks (set by GameScreen before first update) ─────────

    /** Called when a snapshot is picked up; GameScreen creates + shows the overlay. */
    var onAtlasCollected: ((SnapshotPickup) -> Unit)? = null
    /** Called when spirit health hits 0; GameScreen creates the game-over overlay. */
    var onGameOverStart: (() -> Unit)? = null
    /**
     * T-130: called once per death after the T-097 death animation finishes
     * (or immediately under reducedMotion / SMOKE_MODE) so [GameScreen] can
     * show the death-recap overlay. Receives the latest run-stat snapshot.
     * Not invoked on the final "game over" death — [onGameOverStart] handles
     * that path with its own overlay.
     */
    var onDeathRecap: ((cause: DeathCause, timeIntoLevel: Float, stompsThisRun: Int, tokensThisRun: Int) -> Unit)? = null
    /**
     * Set when a portal is activated; [GameScreen] reads this at end-of-frame and
     * performs the actual screen transition + dispose there (never mid-update).
     */
    var pendingPortalTarget: String? = null

    /** Called when a portal is activated in the hub world; GameScreen navigates to the target level. */
    var onPortalActivated: ((targetLevelId: String) -> Unit)? = null

    init {
        totalHazards = obstacleManager.rects().count { it.kind == ObstacleKind.HAZARD }

        if (debugAutopilotEnabled) {
            Gdx.app.log(
                "LevelRunState",
                "Autopilot ON — level=${level.id} ap=${debugAutopilotSeconds}s quit=${debugAutoQuitSeconds}s"
            )
            InputManager.setDebugOverrideEnabled(true)
            InputManager.setDebugHeld(left = false, right = true, jump = false, action = false)
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun triggerHitstop(frames: Int) {
        hitstopFrames = maxOf(hitstopFrames, frames)
    }

    fun switchCharacter() {
        currentCharacter = when (currentCharacter) {
            "Ebo"  -> { player.changeAbility(layaAbility);   "Laya"   }
            "Laya" -> { player.changeAbility(zephyrAbility); "Zephyr" }
            else   -> { player.changeAbility(eboAbility);    "Ebo"    }
        }
        canSwitchCharacter = false
        switchCooldownTimer = 1.0f
        val burstColor = when (currentCharacter) {
            "Ebo"  -> Color(0.83f, 0.57f, 0.29f, 0.9f)
            "Laya" -> Color(0.29f, 0.50f, 0.66f, 0.9f)
            else   -> Color(0.72f, 0.55f, 1.00f, 0.9f)
        }
        renderer.spawnCollectSparkle(player.body.position.x, player.body.position.y, burstColor)
        hud.showTransientMessage(Strings.format(StringKey.CHARACTER_ABILITY_SWAP, currentCharacter, player.ability?.getAbilityName() ?: ""), 0.8f)
    }

    /** Creates a new projectile at world coordinates (x, y) with velocity (vx, vy). */
    fun spawnProjectile(x: Float, y: Float, vx: Float, vy: Float) {
        projectiles.add(Projectile(world, x, y, vx, vy))
    }

    /**
     * Attempt to unlock an achievement by ID. No-ops if already unlocked.
     * Persists to [saveSlotFile] and shows the toast if [achievementToast] is set.
     *
     * T-128: thin facade over [AchievementUnlocker]. Retained as a public entry
     * point for [GameScreen.boss_defeated] callback only — other sites should
     * use [fireAchievements] so their predicate stays testable headless.
     */
    fun tryUnlock(achievementId: String) {
        AchievementUnlocker.tryUnlock(achievementId, saveSlotFile, achievementToast)
    }

    /**
     * T-128: evaluate-and-fire entry point used by callers outside this class
     * (currently [GameScreen]'s `boss_defeated` callback). Builds an
     * [AchievementInputs] with only the boss-defeated trigger set and runs
     * the same orchestrator as the private per-frame call sites.
     */
    fun fireBossDefeatedAchievements() {
        val state = SaveManager.loadGame(saveSlotFile)
        val inputs = AchievementInputs(
            totalStomps = state.totalStomps,
            atlasSize = state.collectedAtlasIds.size,
            completedLevels = state.completedLevels,
            collectedHiddenTokens = state.collectedHiddenTokens,
            unlockedAchievements = state.unlockedAchievements,
            bossDefeatedThisFrame = true,
        )
        for (id in AchievementPredicates.evaluate(inputs)) {
            AchievementUnlocker.tryUnlock(id, saveSlotFile, achievementToast)
        }
    }

    /**
     * T-128: pure-predicate evaluation helper. Builds [AchievementInputs] from
     * the current save state plus the supplied per-frame trigger overrides,
     * then fires every newly-met predicate via [AchievementUnlocker]. Keeps
     * call sites declarative ("here's what just happened") and the unlock
     * logic itself testable headless via [AchievementPredicates.evaluate].
     */
    private fun fireAchievements(
        jumpFiredThisFrame: Boolean = false,
        enemyDefeatedThisFrame: Boolean = false,
        cleanseEventThisFrame: Boolean = false,
        ecoSweepReachedThisFrame: Boolean = false,
        noDeathExitThisFrame: Boolean = false,
        totalStompsOverride: Int? = null,
        atlasSizeOverride: Int? = null,
        collectedHiddenTokensOverride: Set<String>? = null,
    ) {
        val state = SaveManager.loadGame(saveSlotFile)
        val inputs = AchievementInputs(
            totalStomps = totalStompsOverride ?: state.totalStomps,
            atlasSize = atlasSizeOverride ?: state.collectedAtlasIds.size,
            completedLevels = state.completedLevels,
            collectedHiddenTokens = collectedHiddenTokensOverride ?: state.collectedHiddenTokens,
            unlockedAchievements = state.unlockedAchievements,
            jumpFiredThisFrame = jumpFiredThisFrame,
            enemyDefeatedThisFrame = enemyDefeatedThisFrame,
            cleanseEventThisFrame = cleanseEventThisFrame,
            ecoSweepReachedThisFrame = ecoSweepReachedThisFrame,
            noDeathExitThisFrame = noDeathExitThisFrame,
        )
        for (id in AchievementPredicates.evaluate(inputs)) {
            AchievementUnlocker.tryUnlock(id, saveSlotFile, achievementToast)
        }
    }

    // ── Main update loop ──────────────────────────────────────────────────────

    fun update(delta: Float) {
        InputManager.update()
        screenFade.update(delta)

        // Perf logging
        perfLogTimer  += delta
        perfFrameCount++
        perfDeltaSum  += delta
        if (delta > perfDeltaMax) perfDeltaMax = delta
        if (perfLogTimer >= Constants.PERF_LOG_INTERVAL_SECONDS) {
            val avg = if (perfFrameCount > 0) perfDeltaSum / perfFrameCount else 0f
            Gdx.app.log("Perf",
                "fps=%.1f avgDelta=%.4f maxDelta=%.4f playerX=%.2f playerY=%.2f level=%s".format(
                    if (avg > 0f) 1f / avg else 0f, avg, perfDeltaMax,
                    player.body.position.x, player.body.position.y, level.id))
            perfLogTimer = 0f; perfFrameCount = 0; perfDeltaSum = 0f; perfDeltaMax = 0f
        }

        // Debug autopilot
        if (debugAutopilotEnabled) {
            debugAutopilotTimer += delta
            val apActive = debugAutopilotTimer < debugAutopilotSeconds && !isGameOver
            if (apActive) {
                val playerX  = player.body.position.x
                if (debugSmokeMode) {
                    if (smokeStartX.isNaN()) smokeStartX = playerX
                    if (playerX > smokeMaxX) smokeMaxX = playerX
                    if (smokeFrameIdx < smokeFrameTimes.size) {
                        smokeFrameTimes[smokeFrameIdx++] = delta * 1000f  // ms
                    }
                }
                val onGround = player.isGrounded
                val touchWall = player.isTouchingWallLeft || player.isTouchingWallRight
                val justLeftGround = !onGround && prevGrounded && player.body.linearVelocity.y >= -1.5f

                if (playerX > apLastX + 0.05f) { apLastX = playerX; apStuckTimer = 0f }
                else apStuckTimer += delta
                val isStuck = apStuckTimer > 0.35f

                apPeriodicJumpTimer -= delta
                val periodicJump = apPeriodicJumpTimer <= 0f && onGround
                if (periodicJump) apPeriodicJumpTimer = 0.8f

                apAbilityTimer -= delta
                val fireAbility = apAbilityTimer <= 0f
                if (fireAbility) apAbilityTimer = 4.0f

                if (apJumpCooldown > 0f) apJumpCooldown -= delta
                val wantJump = justLeftGround ||
                    (apJumpCooldown <= 0f && (isStuck || touchWall || periodicJump))
                if (wantJump) {
                    apJumpCooldown = 0.4f; apStuckTimer = 0f; apLastX = playerX
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
                    if (debugSmokeMode) {
                        val p99 = computeP99(smokeFrameTimes, smokeFrameIdx)
                        Gdx.app.log("smoke",
                            "level=${level.id} startX=%.3f maxX=%.3f deltaX=%.3f frameP99Ms=%.2f frames=%d"
                                .format(smokeStartX, smokeMaxX, smokeMaxX - smokeStartX, p99, smokeFrameIdx))
                    }
                    Gdx.app.log("LevelRunState", "Auto-quit.")
                    Gdx.app.exit()
                }
            }
        }

        // T-097: while the death animation is playing, freeze player physics
        // so the dead body cannot drift or jump. We skip player.update so
        // input/jump-handling does not run; velocity is zeroed in the death
        // block below anyway.
        if (!deathAnimActive) {
            player.update(delta)
        }

        // Achievement: first_jump — fires once when any jump is performed.
        // T-128: per-run flag preserved as a perf gate (avoid per-frame
        // SaveManager.loadGame). Predicate now lives in
        // [AchievementPredicates.firstJump].
        if (!achievFirstJumpFired && player.jumpFiredThisFrame) {
            achievFirstJumpFired = true
            fireAchievements(jumpFiredThisFrame = true)
        }

        val vel    = player.body.linearVelocity
        val onWall = player.isTouchingWallLeft || player.isTouchingWallRight
        eboAnimator.update(delta, player.isGrounded, vel.x, vel.y, onWall)
        layaAnimator.update(delta, player.isGrounded, vel.x, vel.y, onWall)

        hud.update(delta)

        if (!levelCompleted && !isGameOver) {
            levelTimer += delta
            hud.updateTimer(levelTimer)
            if (isTimeTrial) hud.updateStopwatch(levelTimer)
        }

        // HUD progress uses regular (non-hidden) eco-tokens only — hidden
        // tokens are tracked separately (T-107) and excluded from the
        // "collect all eco-tokens" milestone displayed on the HUD bar.
        val totalEco = level.getEcoTokenPositions().size
        if (totalEco > 0) {
            val regularRemaining = ecoTokens.count { !it.isHidden }
            val collected = totalEco - regularRemaining
            hud.updateProgress(collected.toFloat() / totalEco.toFloat())
        }

        hud.updateAbilityState(
            player.ability?.getCooldownRatio() ?: 0f,
            currentCharacter,
            player.ability?.getAbilityName() ?: ""
        )

        for (mp in movingPlatforms) mp.update(delta)

        // Update enemies; queue body-destroy for dead ones
        val deadEnemies = mutableListOf<Enemy>()
        for (enemy in enemies) {
            enemy.update(delta)
            if (enemy.isDead) deadEnemies.add(enemy)
        }
        for (dead in deadEnemies) {
            var stompTriggered = false
            var newTotalStomps = 0
            if (dead.wasStomped) {
                val pos = dead.body.position
                renderer.spawnStompSmokeBurst(pos.x, pos.y)
                SoundManager.play("land")

                // T-128: cumulative stomps still persists here (state mutation
                // is impure — pure predicates only consume the post-update
                // value). Predicate: AchievementPredicates.stomp10.
                val stompState = SaveManager.loadGame(saveSlotFile)
                newTotalStomps = stompState.totalStomps + 1
                SaveManager.saveGame(stompState.copy(totalStomps = newTotalStomps), saveSlotFile)
                stompTriggered = true
                // T-130: per-run counter for the death-recap overlay.
                stompsThisRun++
            }

            // Fire first_enemy + stomp_10 in a single evaluate pass.
            // T-128: per-run flag preserved as the perf gate (mirrors the
            // pre-refactor behavior — first_enemy only triggers an evaluate
            // call once per run).
            val fireFirstEnemy = !achievFirstEnemyFired
            if (fireFirstEnemy) achievFirstEnemyFired = true
            if (stompTriggered || fireFirstEnemy) {
                fireAchievements(
                    enemyDefeatedThisFrame = fireFirstEnemy || stompTriggered,
                    totalStompsOverride = if (stompTriggered) newTotalStomps else null,
                )
            }

            pendingBodyDestroy.add(dead.body)
            enemies.remove(dead)
        }

        // T-062: Drift Husks update -- mirrors the SmogSprite defeat flow
        // but also feeds the current player x into each husk so its trigger
        // band can fire deterministically.
        if (driftHusks.isNotEmpty()) {
            val playerX = player.body.position.x
            val deadHusks = mutableListOf<DriftHusk>()
            for (husk in driftHusks) {
                husk.setPlayerX(playerX)
                husk.update(delta)
                if (husk.isDead) deadHusks.add(husk)
            }
            for (dead in deadHusks) {
                var stompTriggered = false
                var newTotalStomps = 0
                if (dead.wasStomped) {
                    val pos = dead.body.position
                    renderer.spawnStompSmokeBurst(pos.x, pos.y)
                    SoundManager.play("land")

                    val stompState = SaveManager.loadGame(saveSlotFile)
                    newTotalStomps = stompState.totalStomps + 1
                    SaveManager.saveGame(stompState.copy(totalStomps = newTotalStomps), saveSlotFile)
                    stompTriggered = true
                    // T-130: per-run counter for the death-recap overlay.
                    stompsThisRun++
                }
                val fireFirstEnemy = !achievFirstEnemyFired
                if (fireFirstEnemy) achievFirstEnemyFired = true
                if (stompTriggered || fireFirstEnemy) {
                    fireAchievements(
                        enemyDefeatedThisFrame = fireFirstEnemy || stompTriggered,
                        totalStompsOverride = if (stompTriggered) newTotalStomps else null,
                    )
                }
                pendingBodyDestroy.add(dead.body)
                driftHusks.remove(dead)
            }
        }

        // Boss update (before physics step so attack spawns queue up correctly)
        sentinel?.update(delta)

        // Fixed-timestep physics
        physicsAccum += delta
        var stepsThisFrame = 0
        while (physicsAccum >= Constants.TIME_STEP && stepsThisFrame < 5) {
            world.step(Constants.TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS)
            physicsAccum -= Constants.TIME_STEP
            stepsThisFrame++
        }
        if (stepsThisFrame == 5) physicsAccum = 0f

        eboAbility.drainDeadDroplets { pendingBodyDestroy.add(it) }

        // Projectile update — queue body-destroy for expired or wall-hit projectiles
        val projIter = projectiles.iterator()
        while (projIter.hasNext()) {
            val proj = projIter.next()
            proj.update(delta)
            if (proj.isExpired) {
                pendingBodyDestroy.add(proj.body)
                projIter.remove()
            }
        }

        // Landing detection + dust particles
        particles.update(delta)
        val curVy       = player.body.linearVelocity.y
        val curGrounded = player.isGrounded
        if (!prevGrounded && curGrounded && prevPlayerVy < -4f) {
            renderer.spawnLandingDust(player.body.position.x, player.body.position.y - 0.32f, prevPlayerVy)
            SoundManager.play("land")
            val mag = ((-prevPlayerVy - 8f) / 20f).coerceIn(0f, 0.08f)
            if (mag > 0.02f) ScreenShake.trigger(mag, 0.10f)
        }
        prevPlayerVy = curVy
        prevGrounded = curGrounded

        if (switchCooldownTimer > 0f) {
            switchCooldownTimer -= delta
            if (switchCooldownTimer <= 0f) canSwitchCharacter = true
        }

        // Keyboard shortcut: swap key (default S, rebindable)
        if (InputManager.isSwapJustPressed() && canSwitchCharacter) {
            switchCharacter()
        }

        val assistSettings = SettingsManager.load()
        SoundManager.setVolume(assistSettings.volSfx)

        val fellOff = player.body.position.y < -10f / Constants.PPM
        val playerDied = !isGameOver && !assistSettings.assistInvincible &&
            (player.isDead || fellOff)

        // T-097: Death-animation gate.
        // When [animateDeath] is true we fade the player + zoom the camera over
        // [DEATH_ANIM_DURATION], then run the respawn flow. Otherwise we use
        // the original instant-respawn path (byte-identical to pre-T-097).
        val animateDeath = !assistSettings.reducedMotion && !Constants.SMOKE_MODE

        if (deathAnimActive) {
            // Drive the animation. Suppress player physics so the body cannot
            // drift past the killplane or into another hazard mid-fade.
            deathAnimT += delta
            player.body.linearVelocity = Vector2.Zero
            val t  = (deathAnimT / DEATH_ANIM_DURATION).coerceIn(0f, 1f)
            // Cubic ease-out: 1 - (1-t)^3 — matches the feel of libgdx's
            // Interpolation.pow3Out without pulling in another import.
            val ease = 1f - (1f - t) * (1f - t) * (1f - t)
            renderer.playerAlpha = 1f - ease
            camera.zoom          = 1f + DEATH_ZOOM_AMOUNT * ease

            if (deathAnimT >= DEATH_ANIM_DURATION) {
                // Animation complete: restore camera + sprite, do respawn, flash.
                deathAnimActive      = false
                deathAnimT           = 0f
                camera.zoom          = 1f
                renderer.playerAlpha = 1f
                performDeathRespawn(assistSettings)
                // Brief 0.2s screen flash (0 → 1 alpha at speed 5/s).
                screenFade.fadeToBlack(speed = DEATH_FLASH_SPEED)
                // T-130: surface the death-recap overlay AFTER the animation
                // completes. GameScreen owns the overlay lifecycle; it decides
                // whether to suppress under SMOKE_MODE.
                onDeathRecap?.invoke(lastDeathCause, levelTimer, stompsThisRun, tokensThisRun)
            }
        } else if (playerDied) {
            // T-130: determine cause-of-death once at the moment death is
            // detected. Fall-below-killplane overrides any contact-listener
            // tag (a hazard contact + a subsequent fall reads as FALL).
            lastDeathCause = if (fellOff) DeathCause.FALL else player.lastDeathCause

            // Instant feedback (sfx, shake, hitstop, sparkle, spirit-health
            // decrement) fires the moment death is detected, regardless of
            // whether we animate or respawn instantly.
            SoundManager.play("death")
            ScreenShake.trigger(0.18f, 0.25f)
            triggerHitstop(5)
            renderer.spawnCollectSparkle(player.body.position.x, player.body.position.y,
                Color(1f, 0.3f, 0.3f, 0.95f))

            if (!assistSettings.assistInfiniteSpirits) {
                spiritHealth--
                hud.updateSpiritHealth(spiritHealth)
            }

            if (spiritHealth <= 0 && !assistSettings.assistInfiniteSpirits) {
                isGameOver   = true
                gameOverTimer = 4f
                hud.showTransientMessage(Strings.get(StringKey.RUN_SPIRIT_EXHAUSTED), 2f)
                onGameOverStart?.invoke()
                // Game over: no respawn, no animation. (Existing semantics.)
            } else {
                hud.showTransientMessage(Strings.format(StringKey.SPIRIT_DEATH, currentCharacter, spiritHealth), 1.2f)
                if (animateDeath) {
                    // Begin the death-animation state machine; the respawn
                    // itself runs at completion above. Freeze gravity + velocity
                    // so the body stays put while the camera zooms and the
                    // sprite fades. [player.respawn] restores gravityScale to 1f.
                    deathAnimActive = true
                    deathAnimT      = 0f
                    player.body.linearVelocity = Vector2.Zero
                    player.body.gravityScale   = 0f
                } else {
                    // reducedMotion or SMOKE_MODE → original instant-respawn path.
                    performDeathRespawn(assistSettings)
                    // T-130: surface the recap on the instant-respawn path too.
                    // GameScreen suppresses under SMOKE_MODE so smoke autopilot
                    // is unaffected; reducedMotion players still see a recap.
                    onDeathRecap?.invoke(lastDeathCause, levelTimer, stompsThisRun, tokensThisRun)
                }
            }
        } else if (!isGameOver && assistSettings.assistInvincible &&
                   player.body.position.y < -10f / Constants.PPM) {
            player.respawn()
        }

        if (isGameOver) gameOverTimer -= delta

        // Combo timer
        if (comboTimer > 0f) {
            comboTimer -= delta
            if (comboTimer <= 0f) comboMultiplier = 1
        }

        // Eco-token collection
        val collected = ecoTokens.filter { it.isCollected }
        if (collected.isNotEmpty()) {
            if (comboTimer > 0f) comboMultiplier = (comboMultiplier + 1).coerceAtMost(4)
            score      += collected.size * 10 * comboMultiplier
            comboTimer  = 1.5f
            // T-130: per-run counter for the death-recap overlay (all eco-tokens
            // count, including hidden ones — the recap just shows "how much did
            // I collect before dying", not the eco_sweep gating).
            tokensThisRun += collected.size
            hud.updateScore(score)
            SoundManager.play("collect_token")
            if (comboMultiplier > 1) hud.showCombo(comboMultiplier)
            collected.forEach {
                renderer.spawnTokenSparkle(it.body.position.x, it.body.position.y)
                pendingBodyDestroy.add(it.body)
            }

            // T-107: route hidden ("golden") eco-tokens to the cross-run
            // persistence path and fire the `collector` achievement when all
            // 3 are collected (across runs — Set add is idempotent and
            // tryUnlock guards against double-firing).
            val collectedHidden = collected.filter { it.isHidden }
            var hiddenIdsAfterPickup: Set<String>? = null
            if (collectedHidden.isNotEmpty()) {
                val state = SaveManager.loadGame(saveSlotFile)
                val newIds = state.collectedHiddenTokens + level.id
                if (newIds != state.collectedHiddenTokens) {
                    SaveManager.saveGame(state.copy(collectedHiddenTokens = newIds), saveSlotFile)
                }
                hiddenIdsAfterPickup = newIds
            }

            ecoTokens.removeAll(collected.toSet())

            // Achievement: eco_sweep — all REGULAR tokens collected in this level
            // for the first time this run. Hidden tokens are tracked separately
            // (T-107) and excluded from eco_sweep + HUD progress so finding the
            // hidden token isn't required for the "all eco-tokens" milestone.
            // T-128: collector + eco_sweep fired together via evaluate(); each
            // predicate gates on the appropriate post-update threshold.
            val regularRemaining = ecoTokens.any { !it.isHidden }
            val ecoSweepFires =
                !achievEcoSweepFired && !regularRemaining && level.getEcoTokenPositions().isNotEmpty()
            if (ecoSweepFires) achievEcoSweepFired = true
            if (hiddenIdsAfterPickup != null || ecoSweepFires) {
                fireAchievements(
                    ecoSweepReachedThisFrame = ecoSweepFires,
                    collectedHiddenTokensOverride = hiddenIdsAfterPickup,
                )
            }
        }
        for (token in ecoTokens) { if (!token.isCollected) token.update(delta) }

        // Cloud Atlas snapshot collection
        val collectedSnap = snapshotPickups.firstOrNull { it.isCollected }
        if (collectedSnap != null) {
            SoundManager.play("collect_snapshot")
            score += 25
            hud.updateScore(score)
            renderer.spawnSnapshotSparkle(collectedSnap.body.position.x, collectedSnap.body.position.y)
            snapshotPickups.remove(collectedSnap)
            pendingBodyDestroy.add(collectedSnap.body)
            val existing = SaveManager.loadGame(saveSlotFile)
            if (collectedSnap.entry.id !in existing.collectedAtlasIds) {
                val updatedAtlas = existing.collectedAtlasIds + collectedSnap.entry.id
                SaveManager.saveGame(existing.copy(collectedAtlasIds = updatedAtlas), saveSlotFile)
                // T-128: atlas_half + atlas_full evaluated via pure predicates
                // (threshold checks live in AchievementPredicates).
                fireAchievements(atlasSizeOverride = updatedAtlas.size)
            }
            onAtlasCollected?.invoke(collectedSnap)
        }
        for (snap in snapshotPickups) snap.update(delta)

        // Hazard cleanse events
        val cleanseEvents = CleanseEventQueue.drain()
        for (pos in cleanseEvents) {
            renderer.spawnCleanseBurst(pos.x, pos.y)
            SoundManager.play("hazard_cleansed", pitch = MathUtils.random(0.9f, 1.1f))

            // Achievement: first_cleanse — first hazard cleansed with Seed Slam.
            // T-128: per-run flag preserved as perf gate; predicate lives in
            // [AchievementPredicates.firstCleanse].
            if (!achievFirstCleanseFired) {
                achievFirstCleanseFired = true
                fireAchievements(cleanseEventThisFrame = true)
            }
        }

        if (totalHazards > 0) {
            val cleansedCount = obstacleManager.rects().count {
                it.kind == ObstacleKind.HAZARD && it.fixture.userData == "hazard_cleaned"
            }
            cleanseRatio = cleansedCount.toFloat() / totalHazards.toFloat()
            if (cleanseRatio >= 1f && !ecoRestoredAnnounced) {
                ecoRestoredAnnounced = true
                hud.showTransientMessage(Strings.get(StringKey.RUN_ECOSYSTEM_RESTORED), 2.5f)
            }
        }

        hud.showActionHint = (level.id == "level0_4" && cleanseRatio == 0f)

        // Checkpoint activation → autosave
        for (cp in obstacleManager.checkpoints()) {
            if (cp.fixture.userData == "checkpoint_activated" && cp.id !in activatedCheckpoints) {
                activatedCheckpoints.add(cp.id)
                SoundManager.play("checkpoint")
                if (spiritHealth < 3) {
                    spiritHealth = (spiritHealth + 1).coerceAtMost(3)
                    hud.updateSpiritHealth(spiritHealth)
                }
                if (!isTimeTrial) {
                    val existing = SaveManager.loadGame()
                    val cpState  = existing.copy(
                        level         = level.id,
                        characterName = currentCharacter,
                        checkpoint    = Checkpoint(
                            levelName = level.id,
                            x         = player.spawnPos.x,
                            y         = player.spawnPos.y
                        )
                    )
                    SaveManager.saveGame(cpState)
                    SaveManager.saveGame(cpState, checkpointAutosaveFile)
                }
            }
        }

        if (!levelCompleted && player.hasReachedExit) {
            Gdx.app.log("LevelRunState", "Exit reached — level=${level.id}")
            levelCompleted         = true
            levelCompletionTimer   = 4f

            // Achievement: no_death_run — completed level with full spirit
            // health (never took damage). T-128: per-run flag preserved; the
            // condition `spiritHealth == 3` is the trigger gate (computed
            // here, not inside the predicate, because spiritHealth is a
            // per-frame state field, not a save-state field).
            if (!achievNoDeathFired && spiritHealth == 3) {
                achievNoDeathFired = true
                fireAchievements(noDeathExitThisFrame = true)
            }
        }

        // Portal activation (hub world only)
        val portalId = player.portalContact
        if (portalId != null && !levelCompleted && level is Level0_0) {
            val targetLevel = Level0_0.portalTargetLevel(portalId)
            val required    = Level0_0.portalUnlockRequirement(portalId)
            val state       = SaveManager.loadGame()
            val unlocked    = required.all { it in state.completedLevels }
            if (unlocked && targetLevel != null) {
                Gdx.app.log("LevelRunState", "Portal activated 🌀 $portalId -> $targetLevel")
                player.portalContact = null
                // Defer the actual screen swap to end-of-frame so we never call dispose()
                // from inside the physics/update step (causes a Box2D native crash).
                pendingPortalTarget = targetLevel
            } else {
                // Locked portal — just ignore the contact
                player.portalContact = null
            }
        }

        // Camera: dead-zone + forward-focus (Itay Keren style)
        val halfW   = camera.viewportWidth  / 2f
        val halfH   = camera.viewportHeight / 2f
        val levelW  = level.levelWidthPx / Constants.PPM
        val playerX = player.body.position.x
        val playerY = player.body.position.y

        if (!camInitialized) {
            camTarget.set(playerX, playerY + 100f / Constants.PPM)
            cameraTargetY = playerY + 1.0f
            camInitialized = true
        }

        val biasX    = if (player.isFacingRight) camForwardOffset else -camForwardOffset
        val desiredX = playerX + biasX
        val dx       = desiredX - camTarget.x
        if (kotlin.math.abs(dx) > camDeadZoneHalfW) {
            camTarget.x += kotlin.math.sign(dx) * (kotlin.math.abs(dx) - camDeadZoneHalfW)
        }
        if (player.isGrounded || player.body.linearVelocity.y < camVertSnapFallThreshold) {
            cameraTargetY = playerY + 1.0f
        }
        camTarget.y += (cameraTargetY - camTarget.y) * (camLerpSpeed * delta).coerceAtMost(1f)

        camera.position.x += (camTarget.x - camera.position.x) * (camLerpSpeed * delta).coerceAtMost(1f)
        camera.position.y  = camTarget.y

        camera.position.x = camera.position.x.coerceIn(halfW, (levelW - halfW).coerceAtLeast(halfW))
        camera.position.y = camera.position.y.coerceAtLeast(halfH)

        // Screen shake is owned by [ScreenShake] (T-169 consolidation) — it
        // ticks in LevelRenderer.renderWorld() and applies its offset to the
        // camera position around the projection-matrix calculation. No
        // shake-decay logic lives here any more; this method is responsible
        // only for the dead-zone + forward-focus tracking above.
        camera.update()

        // Drain deferred body destructions (safe — outside world.step and contact callbacks)
        if (pendingBodyDestroy.isNotEmpty()) {
            for (b in pendingBodyDestroy) world.destroyBody(b)
            pendingBodyDestroy.clear()
        }
    }

    /**
     * Loads the checkpoint autosave (if any) and respawns the player.
     * Extracted so both the instant-respawn path (reducedMotion / SMOKE_MODE)
     * and the post-animation path can share the exact same flow.
     */
    private fun performDeathRespawn(assistSettings: com.sohai.platformer.persist.Settings) {
        val cpSave    = SaveManager.loadGame(checkpointAutosaveFile)
        val hasCpSave = SaveManager.listSaves().contains(checkpointAutosaveFile)
        if (hasCpSave && cpSave.checkpoint.levelName == level.id &&
            (cpSave.checkpoint.x != 0f || cpSave.checkpoint.y != 0f)) {
            player.setSpawn(Vector2(cpSave.checkpoint.x, cpSave.checkpoint.y))
            score = cpSave.bestScores[level.id]?.coerceAtMost(score) ?: score
            hud.updateScore(score)
        }
        player.respawn()
    }

    private fun computeP99(times: FloatArray, count: Int): Float {
        if (count == 0) return 0f
        val sorted = times.copyOfRange(0, count).also { it.sort() }
        val idx = ((count - 1) * 0.99f).toInt()
        return sorted[idx]
    }
}

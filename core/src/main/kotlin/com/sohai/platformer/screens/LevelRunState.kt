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
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.Projectile
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.entities.StormSentinel
import com.sohai.platformer.input.InputManager
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.Level0_0
import com.sohai.platformer.persist.Checkpoint
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.progression.AchievementRegistry
import com.sohai.platformer.physics.CleanseEventQueue
import com.sohai.platformer.rendering.CharacterAnimator
import com.sohai.platformer.rendering.ParticleSystem
import com.sohai.platformer.rendering.ScreenFade
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
    private val saveSlotFile: String = "save_slot_1.json"
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

    // ── Screen shake / hitstop ────────────────────────────────────────────────

    var hitstopFrames   = 0
    private var shakeIntensity = 0f
    private var shakeDuration  = 0f
    private var shakeT         = 0f

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

    fun triggerShake(intensityMeters: Float, durationSec: Float) {
        val s = SettingsManager.load()
        if (!s.screenShake || s.reducedMotion) return
        shakeIntensity = maxOf(shakeIntensity, intensityMeters)
        shakeDuration  = maxOf(shakeDuration, durationSec)
    }

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
        hud.showTransientMessage("$currentCharacter: ${player.ability?.getAbilityName() ?: ""}", 0.8f)
    }

    /** Creates a new projectile at world coordinates (x, y) with velocity (vx, vy). */
    fun spawnProjectile(x: Float, y: Float, vx: Float, vy: Float) {
        projectiles.add(Projectile(world, x, y, vx, vy))
    }

    /**
     * Attempt to unlock an achievement by ID.  No-ops if already unlocked.
     * Persists to [saveSlotFile] and shows the toast if [achievementToast] is set.
     */
    fun tryUnlock(achievementId: String) {
        val state = SaveManager.loadGame(saveSlotFile)
        if (achievementId in state.unlockedAchievements) return
        val newState = state.copy(
            unlockedAchievements = state.unlockedAchievements + achievementId
        )
        SaveManager.saveGame(newState, saveSlotFile)
        val achievement = AchievementRegistry.get(achievementId) ?: return
        achievementToast?.show(achievement)
        Gdx.app.log("Achievement", "Unlocked: $achievementId — ${achievement.title}")
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

        player.update(delta)

        // Achievement: first_jump — fires once when any jump is performed
        if (!achievFirstJumpFired && player.jumpFiredThisFrame) {
            achievFirstJumpFired = true
            tryUnlock("first_jump")
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

        // Update enemies; queue body-destroy for dead ones
        val deadEnemies = mutableListOf<Enemy>()
        for (enemy in enemies) {
            enemy.update(delta)
            if (enemy.isDead) deadEnemies.add(enemy)
        }
        for (dead in deadEnemies) {
            if (dead.wasStomped) {
                val pos = dead.body.position
                renderer.spawnStompSmokeBurst(pos.x, pos.y)
                SoundManager.play("land")

                // Achievement: stomp_10 — track cumulative stomps across runs
                val stompState = SaveManager.loadGame(saveSlotFile)
                val newTotalStomps = stompState.totalStomps + 1
                SaveManager.saveGame(stompState.copy(totalStomps = newTotalStomps), saveSlotFile)
                if (newTotalStomps >= 10) tryUnlock("stomp_10")
            }

            // Achievement: first_enemy — first enemy defeated by any means
            if (!achievFirstEnemyFired) {
                achievFirstEnemyFired = true
                tryUnlock("first_enemy")
            }

            pendingBodyDestroy.add(dead.body)
            enemies.remove(dead)
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
            if (mag > 0.02f) triggerShake(mag, 0.10f)
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

        val playerDied = !isGameOver && !assistSettings.assistInvincible &&
            (player.isDead || player.body.position.y < -10f / Constants.PPM)
        if (playerDied) {
            SoundManager.play("death")
            triggerShake(0.18f, 0.25f)
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
                hud.showTransientMessage("Spirit Exhausted...", 2f)
                onGameOverStart?.invoke()
            } else {
                hud.showTransientMessage("$currentCharacter fell ($spiritHealth spirits left)", 1.2f)
            }

            val cpSave    = SaveManager.loadGame(checkpointAutosaveFile)
            val hasCpSave = SaveManager.listSaves().contains(checkpointAutosaveFile)
            if (hasCpSave && cpSave.checkpoint.levelName == level.id &&
                (cpSave.checkpoint.x != 0f || cpSave.checkpoint.y != 0f)) {
                player.setSpawn(Vector2(cpSave.checkpoint.x, cpSave.checkpoint.y))
                score = cpSave.bestScores[level.id]?.coerceAtMost(score) ?: score
                hud.updateScore(score)
            }
            player.respawn()
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
            hud.updateScore(score)
            SoundManager.play("collect_token")
            if (comboMultiplier > 1) hud.showCombo(comboMultiplier)
            collected.forEach {
                renderer.spawnTokenSparkle(it.body.position.x, it.body.position.y)
                pendingBodyDestroy.add(it.body)
            }
            ecoTokens.removeAll(collected.toSet())

            // Achievement: eco_sweep — all tokens collected in this level for the first time this run
            if (!achievEcoSweepFired && ecoTokens.isEmpty() && level.getEcoTokenPositions().isNotEmpty()) {
                achievEcoSweepFired = true
                tryUnlock("eco_sweep")
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
                // Achievements: atlas milestones
                if (updatedAtlas.size >= 6)  tryUnlock("atlas_half")
                if (updatedAtlas.size >= 12) tryUnlock("atlas_full")
            }
            onAtlasCollected?.invoke(collectedSnap)
        }
        for (snap in snapshotPickups) snap.update(delta)

        // Hazard cleanse events
        val cleanseEvents = CleanseEventQueue.drain()
        for (pos in cleanseEvents) {
            renderer.spawnCleanseBurst(pos.x, pos.y)
            SoundManager.play("hazard_cleansed", pitch = MathUtils.random(0.9f, 1.1f))

            // Achievement: first_cleanse — first hazard cleansed with Seed Slam
            if (!achievFirstCleanseFired) {
                achievFirstCleanseFired = true
                tryUnlock("first_cleanse")
            }
        }

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

            // Achievement: no_death_run — completed level with full spirit health (never took damage)
            if (!achievNoDeathFired && spiritHealth == 3) {
                achievNoDeathFired = true
                tryUnlock("no_death_run")
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

        // Screen shake
        if (shakeDuration > 0f) {
            shakeT         += delta
            shakeDuration  -= delta
            val falloff = (shakeDuration / 0.2f).coerceIn(0f, 1f)
            camera.position.x += MathUtils.sin(shakeT * 60f) * shakeIntensity * falloff
            camera.position.y += MathUtils.cos(shakeT * 73f) * shakeIntensity * falloff
            if (shakeDuration <= 0f) { shakeIntensity = 0f; shakeT = 0f }
        }
        camera.update()

        // Drain deferred body destructions (safe — outside world.step and contact callbacks)
        if (pendingBodyDestroy.isNotEmpty()) {
            for (b in pendingBodyDestroy) world.destroyBody(b)
            pendingBodyDestroy.clear()
        }
    }

    private fun computeP99(times: FloatArray, count: Int): Float {
        if (count == 0) return 0f
        val sorted = times.copyOfRange(0, count).also { it.sort() }
        val idx = ((count - 1) * 0.99f).toInt()
        return sorted[idx]
    }
}

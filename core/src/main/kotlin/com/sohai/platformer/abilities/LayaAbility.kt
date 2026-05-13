package com.sohai.platformer.abilities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.effects.WindTrail
import com.sohai.platformer.entities.PlayerController

/**
 * Laya's Atmospheric ability: Wind Dash.
 *
 * On activation, applies a forward+upward impulse for mobility. During the
 * initial rising phase (`windBoostActive`), gravity is reduced to 0.25× to
 * boost the dash height. After the player crosses the apex (vy goes from
 * positive to non-positive), the ability transitions into a **slow-descent
 * glide** (T-176): gravity is held at [WIND_DASH_GLIDE_GRAVITY_MULTIPLIER]
 * (~0.45×) so the player floats down like Crimson Desert's gliding cape.
 * The glide ends when ANY of three reset conditions fire:
 *   1. The player lands (foot sensor reports `isGrounded`).
 *   2. The player initiates a second action (jump, ability re-press, or wall
 *      contact) — handled by the per-frame reset checks below.
 *   3. The descent-cap timer ([WIND_DASH_GLIDE_MAX_DURATION], 3 s) expires —
 *      a safety net so the player can't float forever if the level geometry
 *      somehow keeps them airborne.
 *
 * Cap is intentional: see T-176 ticket. The reduced gravity ONLY applies to
 * Wind Dash's descent phase — normal-fall gravity for Laya outside this
 * ability and for other characters (Ebo, Zephyr) is unaffected.
 */
class LayaAbility(
    private val world: World,
    private var playerController: PlayerController? = null
) : CharacterAbility {
    private var cooldownTimer = 0f
    private val cooldownDuration = 1.2f // Slightly faster than Ebo for wind feel
    private var windBoostActive = false
    private var windBoostTimer = 0f
    private val windBoostDuration = 0.5f // Short burst
    private val activeWindTrails = mutableListOf<WindTrail>()

    /**
     * T-176: glide-state machine. `windDashActive` spans the entire dash
     * (from trigger to a reset condition); `apexCrossed` flips once vy goes
     * from + to ≤0; `glideActive` mirrors `apexCrossed && windDashActive` and
     * drives the per-frame gravity-scale write. `glideTimer` accumulates only
     * while glideActive and cuts the glide at [WIND_DASH_GLIDE_MAX_DURATION].
     */
    private var windDashActive = false
    private var apexCrossed = false
    private var glideActive = false
    private var glideTimer = 0f
    private var prevVy = 0f

    /** Test-only: snapshot of internal state so tests can drive the FSM headlessly. */
    internal fun stateForTest(): GlideState =
        GlideState(windDashActive, apexCrossed, glideActive, glideTimer)

    /** Test-only: bypass [executeWindDash] (which needs a Box2D body) to arm the glide FSM. */
    internal fun armForTest(prevVerticalVelocity: Float) {
        windDashActive = true
        apexCrossed = false
        glideActive = false
        glideTimer = 0f
        windBoostActive = false  // tests jump straight to the descent phase
        prevVy = prevVerticalVelocity
    }

    /** Test-only: clear all state. */
    internal fun resetForTest() {
        cooldownTimer = 0f
        windBoostActive = false
        windBoostTimer = 0f
        windDashActive = false
        apexCrossed = false
        glideActive = false
        glideTimer = 0f
        prevVy = 0f
    }

    fun setPlayerController(player: PlayerController) {
        playerController = player
    }

    override fun update(deltaTime: Float) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime
        }

        if (windBoostActive) {
            windBoostTimer -= deltaTime
            playerController?.body?.gravityScale = 0.25f
            if (windBoostTimer <= 0f) {
                windBoostActive = false
                // Do NOT reset gravity scale here — once the initial wind-boost
                // ends we fall through into the glide-state machine below,
                // which sets gravity scale per-frame.
            }
        }

        // ── Glide state machine (T-176) ──────────────────────────────────────
        if (windDashActive) {
            val pc = playerController
            val vy = pc?.body?.linearVelocity?.y ?: 0f

            // Apex detection: vy was positive last frame, non-positive now.
            // Once crossed it stays crossed for the remainder of this dash.
            if (!apexCrossed && prevVy > 0f && vy <= 0f) {
                apexCrossed = true
                glideActive = true
                glideTimer = 0f
            }

            if (glideActive) {
                pc?.body?.gravityScale = WIND_DASH_GLIDE_GRAVITY_MULTIPLIER
                pc?.isWindDashGliding = true
                glideTimer += deltaTime

                // Reset condition 1: landed.
                val landed = pc?.isGrounded == true
                // Reset condition 2: second action — a jump fired this frame
                // (any kind: ground / wall / air double-jump) is the
                // "player chose to break the glide" signal. Also reset if the
                // player makes wall contact (intent to wall-slide).
                val jumped = pc?.jumpFiredThisFrame == true
                val touchingWall = pc?.isTouchingWallLeft == true || pc?.isTouchingWallRight == true
                // Reset condition 3: descent-cap timer.
                val capped = glideTimer >= WIND_DASH_GLIDE_MAX_DURATION

                if (landed || jumped || touchingWall || capped) {
                    endGlide()
                }
            }

            prevVy = vy
        }

        // Update and cleanup dead wind trails
        for (i in activeWindTrails.indices.reversed()) {
            val trail = activeWindTrails[i]
            trail.update(deltaTime)
            if (!trail.isAlive) {
                activeWindTrails.removeAt(i)
            }
        }

    }

    private fun endGlide() {
        windDashActive = false
        apexCrossed = false
        glideActive = false
        glideTimer = 0f
        playerController?.body?.gravityScale = 1f
        playerController?.isWindDashGliding = false
    }

    override fun onActionPressed() {
        // Second-action reset (T-176): if a dash is in progress (rising OR
        // gliding) a re-press cancels it instead of trying to chain a new
        // dash. The cooldown still gates new dashes after the reset.
        if (windDashActive) {
            endGlide()
            return
        }
        // Only allow Wind Dash if cooldown is ready
        if (cooldownTimer <= 0f) {
            executeWindDash()
            cooldownTimer = cooldownDuration
        }
    }

    override fun onActionHeld() {
        // Wind Dash is a one-shot ability
    }

    override fun onActionReleased() {
        // Wind Dash is a one-shot ability
    }

    override fun getAbilityName(): String = "Wind Dash"

    override fun getCooldownRatio(): Float = (cooldownTimer / cooldownDuration).coerceIn(0f, 1f)

    private fun executeWindDash() {
        if (playerController == null) return

        val playerBody = playerController!!.body
        val playerPos = playerBody.position

        // Determine direction based on tracked facing direction
        val facingRight = playerController!!.isFacingRight
        val windDirection = if (facingRight) 1f else -1f

        // Apply forward impulse (wind boost)
        playerBody.applyLinearImpulse(
            Vector2(windDirection * 8f, 0f),
            playerPos,
            true
        )

        // Apply upward impulse for mobility (jump boost)
        playerBody.applyLinearImpulse(
            Vector2(0f, 3f),
            playerPos,
            true
        )

        // Activate wind boost state
        windBoostActive = true
        windBoostTimer = windBoostDuration

        // T-176: arm the glide FSM. The wind-boost phase keeps gravity at
        // 0.25× for the rise; once vy crosses zero the update() loop above
        // promotes us into the glide phase with gravity = 0.45×.
        windDashActive = true
        apexCrossed = false
        glideActive = false
        glideTimer = 0f
        prevVy = playerBody.linearVelocity.y

        SoundManager.play("ability_laya")
        spawnWindEffect(playerPos, windDirection)

        if (com.sohai.platformer.Constants.DEV_LOGS) {
            Gdx.app.log("LayaAbility", "Wind Dash at (${playerPos.x}, ${playerPos.y}) dir=${if (facingRight) "RIGHT" else "LEFT"}")
        }
    }

    private fun spawnWindEffect(epicenter: Vector2, windDirection: Float) {
        // Spawn 8 wind trails in a burst pattern
        val trailCount = 8

        for (i in 0 until trailCount) {
            val angle = (i / trailCount.toFloat()) * MathUtils.PI2
            val spreadX = MathUtils.cos(angle) * 1.5f
            val spreadY = MathUtils.sin(angle) * 1.5f

            val spawnX = (epicenter.x * 100f) + spreadX * 100f
            val spawnY = (epicenter.y * 100f) + spreadY * 100f

            // Wind trails move outward and slightly upward
            val vx = MathUtils.cos(angle) * 5f + windDirection * 3f
            val vy = MathUtils.sin(angle) * 3f + 1f

            activeWindTrails.add(WindTrail(spawnX, spawnY, vx, vy))
        }
    }

    fun isWindBoostActive(): Boolean = windBoostActive

    /** T-176: true while the slow-descent glide is biasing gravity for Laya. */
    fun isGlideActive(): Boolean = glideActive

    fun getActiveWindTrails(): List<WindTrail> = activeWindTrails

    /**
     * T-176: pure snapshot of the glide-state machine for Kotest. Mirrors the
     * private fields. Headless tests instantiate this directly to verify the
     * fall-distance ratio without needing a Box2D Body.
     */
    internal data class GlideState(
        val windDashActive: Boolean,
        val apexCrossed: Boolean,
        val glideActive: Boolean,
        val glideTimer: Float
    )

    companion object {
        /**
         * T-176: gravity multiplier applied to Laya during the descent half of
         * Wind Dash. Below 1.0 = slower fall; 0.45 was tuned to feel like
         * Crimson Desert's glide cape (per ticket spec, target range 0.4-0.5;
         * 0.45 sits in the middle and gives a fall distance ~45% of normal
         * over the same time window — see [PlayerControllerGravity] math in
         * `LayaWindDashGlideTest`).
         */
        const val WIND_DASH_GLIDE_GRAVITY_MULTIPLIER: Float = 0.45f

        /**
         * T-176: hard cap on glide duration (seconds). Three seconds at half
         * gravity covers ~22 m of vertical fall — well past any reasonable
         * level geometry. Prevents an infinite float bug if a reset condition
         * is missed (e.g. player ends up airborne in a no-wall void).
         */
        const val WIND_DASH_GLIDE_MAX_DURATION: Float = 3.0f

        /**
         * Pure-function helper: simulate vertical fall distance over [frames]
         * physics steps at fixed [deltaTime] starting at [initialVy], using a
         * constant effective gravity of `Constants.GRAVITY * gravityScale *
         * fallMultiplier`. Used by the T-176 test to verify the glide
         * multiplier produces ~55% of the normal-fall distance over the same
         * time window.
         *
         * Semi-implicit Euler (matches Box2D's integrator family closely
         * enough for the ratio test):
         *   v += g * dt
         *   y += v * dt
         */
        fun simulateFallDistance(
            initialVy: Float,
            gravityScale: Float,
            fallMultiplier: Float,
            frames: Int,
            deltaTime: Float
        ): Float {
            var v = initialVy
            var y = 0f
            val gEff = Constants.GRAVITY * gravityScale * fallMultiplier
            repeat(frames) {
                v += gEff * deltaTime
                y += v * deltaTime
            }
            return y
        }
    }
}

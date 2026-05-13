package com.sohai.platformer.rendering

import com.sohai.platformer.persist.SettingsManager

/**
 * Pure-math camera look-ahead helper (T-144).
 *
 * Smoothly biases a camera's horizontal position toward the direction the
 * player is moving so the player can see more of the level ahead of them.
 * The bias is a soft target the camera lerps toward each frame — there's no
 * hard snap on direction change, so quick A↔D oscillation produces a gentle
 * recenter instead of a whiplash camera.
 *
 * ## Lerp curve
 *
 * Per frame:
 *
 * ```
 *   targetOffset = sign(velocityX) * MAX_OFFSET_PX  (or 0 when velocity ≈ 0)
 *   currentOffset += (targetOffset - currentOffset) * LERP_FACTOR
 * ```
 *
 * `LERP_FACTOR = 0.15` matches the feel-good camera survey numbers used by
 * Celeste, Hollow Knight, and similar pixel platformers (see the GAME_PLAN
 * "feel polish" note). At 60 fps the camera reaches ~99% of the target after
 * roughly half a second, which keeps the offset legible without feeling
 * laggy.
 *
 * The offset is reported in **virtual pixels** to match the rest of the
 * camera-offset infra (T-116 ScreenShake uses world meters; this class
 * divides by PPM at the call site rather than inside the helper). The two
 * offsets are summed by the caller before render and reverted afterwards,
 * exactly like T-116.
 *
 * ## Settings gate
 *
 * `SettingsManager.load().cameraLookAhead == false` causes [update] to drive
 * the offset toward zero (so a mid-flight toggle gracefully recenters
 * instead of snap-cutting). [offsetPx] returns 0f when the setting is off
 * AND the residual offset has lerped to ~0, but always honours the lerp
 * during the transition.
 *
 * ## Velocity dead-zone
 *
 * Velocities with `|vx| < MIN_VELOCITY_FOR_OFFSET` are treated as "standing
 * still" — the target snaps to 0 so jitter near zero velocity doesn't shove
 * the camera one way or the other.
 *
 * ## Clamping
 *
 * This class does NOT know about level extents. The caller is expected to
 * clamp `camera.position.x + offsetPx` against `[halfW, levelW - halfW]`
 * before rendering — same pattern as the T-116 shake offset.
 *
 * ## Thread-safety
 *
 * Mutable instance state (`currentOffsetPx`). Touch only from the render
 * thread.
 */
class CameraLookAhead {

    /** Current smoothed offset in virtual pixels. Positive = right, negative = left. */
    private var currentOffsetPx: Float = 0f

    /**
     * Lerp the current offset toward the target dictated by [velocityX].
     *
     * @param velocityX horizontal velocity of the player in world units (m/s).
     *                  Sign drives the look-ahead direction; magnitude only
     *                  matters versus [MIN_VELOCITY_FOR_OFFSET] (no scaling).
     *
     * Honours `SettingsManager.load().cameraLookAhead`: when off, the target
     * is forced to 0 so the camera gracefully recenters.
     */
    fun update(velocityX: Float) {
        val enabled = SettingsManager.load().cameraLookAhead
        val targetPx = computeTargetOffset(velocityX, enabled)
        currentOffsetPx += (targetPx - currentOffsetPx) * LERP_FACTOR
    }

    /** Current offset in virtual pixels. Drives the camera's horizontal bias. */
    fun offsetPx(): Float = currentOffsetPx

    /** Test-only: reset state so each test starts clean. */
    internal fun resetForTest() {
        currentOffsetPx = 0f
    }

    /** Test-only: bypass settings to set the current offset directly. */
    internal fun setOffsetForTest(px: Float) {
        currentOffsetPx = px
    }

    companion object {
        /** Maximum horizontal bias in virtual pixels (±). Matches T-144 spec. */
        const val MAX_OFFSET_PX: Float = 48f

        /**
         * Lerp factor applied per frame. 0.15 = the T-144 spec value; chosen
         * to match Celeste/Hollow-Knight-class feel without overshoot.
         */
        const val LERP_FACTOR: Float = 0.15f

        /**
         * Below this absolute velocity (m/s) the player is treated as "not
         * moving" — the look-ahead target snaps to 0 so micro-jitter at the
         * apex of a jump or while wall-clinging doesn't bias the camera.
         *
         * Set well below the player's walk speed (`Constants.PLAYER_SPEED`,
         * a few m/s) so any deliberate input still triggers the offset.
         */
        const val MIN_VELOCITY_FOR_OFFSET: Float = 0.1f

        /**
         * Pure function: target offset given velocity and the settings gate.
         *
         * Extracted as a public-companion helper so Kotest can verify the
         * lerp math in isolation without instantiating the class or stubbing
         * `SettingsManager`. The instance [update] method passes the same
         * `enabled` argument it reads from settings — no behavioural drift.
         *
         * Contract:
         *   - `!enabled` → 0
         *   - `|vx| < MIN_VELOCITY_FOR_OFFSET` → 0
         *   - `vx > 0` → +MAX_OFFSET_PX
         *   - `vx < 0` → -MAX_OFFSET_PX
         */
        fun computeTargetOffset(velocityX: Float, enabled: Boolean): Float {
            if (!enabled) return 0f
            if (kotlin.math.abs(velocityX) < MIN_VELOCITY_FOR_OFFSET) return 0f
            return if (velocityX > 0f) MAX_OFFSET_PX else -MAX_OFFSET_PX
        }

        /**
         * Pure function: one lerp step. Same math as [update] but stateless
         * so tests can verify the curve verbatim.
         *
         * `lerpStep(curr, target) = curr + (target - curr) * LERP_FACTOR`
         */
        fun lerpStep(currentPx: Float, targetPx: Float): Float =
            currentPx + (targetPx - currentPx) * LERP_FACTOR
    }
}

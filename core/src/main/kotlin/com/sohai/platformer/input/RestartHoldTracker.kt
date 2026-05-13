package com.sohai.platformer.input

/**
 * T-133: pure-function hold-timer state machine for the quick-restart hotkey.
 *
 * The player must hold the rebindable `restart` key (default R) for
 * [holdDurationSeconds] (0.5s by default) before the level restarts. A tap
 * is a no-op — releasing the key before the threshold resets the timer to
 * zero so the next hold starts fresh.
 *
 * After the threshold fires once, the tracker latches into "consumed" state
 * and will not fire again until the player releases the key (drops back to
 * `held = false`). This prevents a single sustained hold from triggering
 * multiple restarts on consecutive frames.
 *
 * No Box2D / libGDX dependency — the caller (GameScreen) supplies the
 * `held` snapshot and `dt` per frame, so this class is trivially testable.
 */
class RestartHoldTracker(val holdDurationSeconds: Float = 0.5f) {

    /** Seconds the key has been continuously held so far. 0 when released. */
    var heldSeconds: Float = 0f
        private set

    /**
     * True once the threshold has fired for the current hold. Cleared back to
     * false the next frame the player releases the key. Prevents multi-fire
     * while the player keeps the key pressed past the threshold.
     */
    private var consumed: Boolean = false

    /**
     * Advance the tracker by [dt] using the current key state in [held]. Returns
     * true exactly once per hold — on the frame the cumulative held time first
     * reaches [holdDurationSeconds]. A second restart requires the player to
     * release the key first.
     */
    fun update(dt: Float, held: Boolean): Boolean {
        if (!held) {
            heldSeconds = 0f
            consumed = false
            return false
        }
        heldSeconds += dt
        if (!consumed && heldSeconds >= holdDurationSeconds) {
            consumed = true
            return true
        }
        return false
    }

    /**
     * Progress 0f..1f for rendering the radial indicator. Returns 0f when not
     * held. Capped at 1f once the threshold is met.
     */
    fun progress(): Float =
        if (holdDurationSeconds <= 0f) 0f
        else (heldSeconds / holdDurationSeconds).coerceIn(0f, 1f)

    /** Returns true if the player is actively holding (timer > 0). */
    fun isHolding(): Boolean = heldSeconds > 0f

    /** Force-clears the tracker (e.g. when the level restarts). */
    fun reset() {
        heldSeconds = 0f
        consumed = false
    }
}

package com.sohai.platformer.util

import kotlin.math.floor

/**
 * Formats an elapsed-seconds [Float] as a speedrun-style `MM:SS.mmm` string.
 *
 * Pure function — no GL / libGDX dependencies — so it is fully unit-testable
 * on the JVM and called once per frame by [com.sohai.platformer.screens.GameScreen]
 * when the T-142 speedrun-timer overlay is enabled.
 *
 * Edge cases:
 *  - Negative inputs are clamped to 0 — the in-game timer never goes negative
 *    but defending against numerical drift / a sentinel "not yet started"
 *    value keeps the HUD from rendering `-1:59.999`.
 *  - The milliseconds component is derived from `floor((seconds % 1) * 1000)`
 *    rather than from the *integer* truncation of seconds. This guarantees
 *    that an input of `59.9999` formats as `00:59.999` (and *not* the
 *    visually wrong `01:00.000`) — see [SpeedrunTimerFormatTest].
 *  - Inputs that overflow 60 minutes wrap by **carrying** into the minutes
 *    field: 3600.5s → `60:00.500`. No clamping is applied to the minutes
 *    field; a speedrunner who spends an hour in a level should still see
 *    a coherent timer instead of `00:00.500`.
 */
object SpeedrunTimerFormat {

    /**
     * Format [seconds] as `MM:SS.mmm`. See class kdoc for edge-case semantics.
     */
    fun format(seconds: Float): String {
        // Clamp to zero so transient negative drift never produces "-1:59.999".
        val clamped = if (seconds < 0f) 0.0 else seconds.toDouble()
        // Step in double-precision: at 60 FPS, the float-32 epsilon around
        // multi-minute timers can flip the millis digit. Doubles fix that
        // without forcing every caller to pass a Double.
        val totalMs = floor(clamped * 1000.0).toLong()
        val minutes = totalMs / 60_000L
        val seconds_ = (totalMs / 1000L) % 60L
        val millis  = totalMs % 1000L
        return "%02d:%02d.%03d".format(minutes, seconds_, millis)
    }
}

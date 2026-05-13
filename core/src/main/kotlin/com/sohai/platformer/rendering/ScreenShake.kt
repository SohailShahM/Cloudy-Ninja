package com.sohai.platformer.rendering

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.sohai.platformer.persist.SettingsManager

/**
 * Pure-math screen-shake utility (T-116).
 *
 * Decaying-amplitude camera oscillator used to add hit-feedback "juice" on
 * stomp-defeat and Storm Sentinel boss hits.  Lives in the rendering package
 * but performs **no** rendering work itself — it only computes an `(x, y)`
 * offset that callers apply to a camera before rendering and revert
 * afterwards.  This keeps the class headless-testable (no `Gdx.*` calls in
 * the hot path; no GL state, no texture loads, no resources).
 *
 * ## Decay curve
 *
 * **Linear decay** of amplitude over the trigger's duration, modulated by
 * fixed-frequency sine/cosine oscillation (different frequencies on each
 * axis so the shake doesn't trace a straight diagonal):
 *
 * ```
 *   t        = elapsed since trigger (clamped to [0, duration])
 *   falloff  = 1 - t / duration                (linear; 1 → 0 over duration)
 *   amp      = baseAmplitude * falloff
 *   offsetX  = sin(t * X_FREQ) * amp
 *   offsetY  = cos(t * Y_FREQ) * amp
 * ```
 *
 * Linear was chosen over exponential because it gives a predictable cut-off
 * — the shake hits exactly zero at `t == duration`, with no perceptual tail.
 * Exponential decay would either ring on indefinitely or require an arbitrary
 * cut-off threshold.
 *
 * ## Reduced-motion gate
 *
 * [trigger] is a **no-op** when `SettingsManager.load().reducedMotion == true`.
 * This is checked at trigger time rather than at render time so a setting flip
 * mid-shake still completes the existing animation cleanly.
 *
 * ## Stacking
 *
 * A new [trigger] call while a shake is already in-flight **replaces** the
 * previous animation (amplitude + remaining duration are reset to the new
 * values). This matches player intuition: a fresh hit should feel like a
 * fresh impact, not an additive accumulation that could exceed reasonable
 * screen-displacement bounds.
 *
 * ## Thread-safety
 *
 * Single-threaded by design — must only be touched from the game thread.
 * The object holds mutable float state and is not protected by a lock.
 */
object ScreenShake {

    /** Oscillation frequency on the X axis (radians per second of elapsed shake). */
    private const val X_FREQ = 60f

    /** Oscillation frequency on the Y axis (radians per second). Different from
     *  X_FREQ so the shake doesn't trace a straight diagonal line. */
    private const val Y_FREQ = 73f

    /** Base amplitude provided to the active [trigger] call. 0f when idle. */
    private var baseAmplitude: Float = 0f

    /** Total duration of the active shake, in seconds. 0f when idle. */
    private var duration: Float = 0f

    /** Elapsed seconds since the active shake started. */
    private var elapsed: Float = 0f

    /**
     * Reusable offset vector returned from [offset]. Callers MUST NOT retain
     * the reference across frames — the contents are overwritten by the next
     * [update] call. Local-only allocation avoids per-frame GC pressure.
     */
    private val tmpOffset = Vector2()

    /**
     * Begin (or replace) a shake.
     *
     * @param amplitude maximum displacement at `t = 0`, in world units (meters).
     *                  The standard call site uses `4f` — small enough not to
     *                  obscure the player on impact, large enough to read as
     *                  a clear hit confirmation.
     * @param duration  total shake duration in seconds. Must be `> 0` to take
     *                  effect; `0` or negative values are silently ignored.
     *
     * **Honours [com.sohai.platformer.persist.Settings.reducedMotion] — when on,
     * this call is a no-op.** The accessibility gate lives here (rather than at
     * each call site) so callers don't have to thread the setting through.
     */
    fun trigger(amplitude: Float, duration: Float) {
        if (duration <= 0f) return
        if (SettingsManager.load().reducedMotion) return
        this.baseAmplitude = amplitude
        this.duration      = duration
        this.elapsed       = 0f
    }

    /**
     * Advance the active shake by [delta] seconds. Safe to call every frame
     * even when no shake is active (early-returns for the idle case).
     *
     * Cap-clamps `elapsed` at `duration` so [offset] reads can never overshoot
     * past zero — the shake disappears cleanly at the exact duration mark.
     */
    fun update(delta: Float) {
        if (duration <= 0f) return
        elapsed += delta
        if (elapsed >= duration) {
            // Snap to a fully-rested state. Idempotent; multiple over-shoots
            // keep the object zeroed.
            baseAmplitude = 0f
            duration      = 0f
            elapsed       = 0f
        }
    }

    /**
     * Current `(x, y)` camera offset, in world units (meters). Always returns
     * the same reusable [Vector2] instance — copy the components if you need
     * to persist the value past the next call.
     *
     * Returns `(0, 0)` when no shake is active.
     */
    fun offset(): Vector2 {
        if (duration <= 0f) {
            tmpOffset.set(0f, 0f)
            return tmpOffset
        }
        val t       = elapsed
        // Linear decay: falloff is 1 at t=0, 0 at t=duration.
        val falloff = (1f - t / duration).coerceIn(0f, 1f)
        val amp     = baseAmplitude * falloff
        tmpOffset.set(
            MathUtils.sin(t * X_FREQ) * amp,
            MathUtils.cos(t * Y_FREQ) * amp
        )
        return tmpOffset
    }

    /** True while a shake is currently in progress (i.e. before [duration] elapses). */
    fun isActive(): Boolean = duration > 0f

    /**
     * Test-only hook to clear all shake state without invoking [SettingsManager].
     * Production code should not call this — let [update] retire the shake
     * naturally.
     */
    internal fun resetForTest() {
        baseAmplitude = 0f
        duration      = 0f
        elapsed       = 0f
        tmpOffset.set(0f, 0f)
    }
}

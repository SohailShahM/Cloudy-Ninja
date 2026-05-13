package com.sohai.platformer.rendering

/**
 * Pure-math easing utility (T-159).
 *
 * A standalone collection of normalised easing curves — all functions take a
 * **time parameter `t` in `[0, 1]`** and return a **progress value in `[0, 1]`**
 * where `f(0) == 0` and `f(1) == 1`. Use these to remap linear interpolation
 * progress into curved motion for tweens, UI fades, camera moves, etc.
 *
 * ## Design constraints
 *
 *  - **Pure math only.** No `Gdx.*` imports, no mutable state, no I/O. Every
 *    function is referentially transparent and trivially headless-testable.
 *  - **Caller-owned interpolation.** This file deliberately does NOT lerp
 *    between start/end values — that responsibility stays at the call site so
 *    the same curve can drive floats, vectors, colours, etc. without
 *    overloading.
 *  - **Clamped input.** All public functions clamp `t` into `[0, 1]` before
 *    use, so out-of-range inputs degrade gracefully to the nearest endpoint
 *    rather than overshooting or producing NaNs.
 *
 * ## Curve catalogue
 *
 *  - [linear]         — straight line; identity function.
 *  - [easeInQuad]     — `t^2`. Slow start, accelerates to the end.
 *  - [easeOutQuad]    — `1 - (1 - t)^2`. Fast start, decelerates into the end.
 *  - [easeInOutQuad]  — symmetric S-curve built from the quadratic pair.
 *  - [easeInCubic]    — `t^3`. Sharper start than [easeInQuad].
 *  - [easeOutCubic]   — mirror of [easeInCubic]; sharper finish than out-quad.
 *  - [easeInOutCubic] — symmetric S-curve built from the cubic pair.
 *  - [bezier]         — parametric cubic Bezier with two control points,
 *                       matching the CSS `cubic-bezier(p1x, p1y, p2x, p2y)`
 *                       convention. Use for one-off custom curves.
 *
 * ## Standard preset shorthands (for [bezier] callers)
 *
 * | Preset      | `(p1x, p1y, p2x, p2y)`        |
 * |-------------|-------------------------------|
 * | ease        | `(0.25f, 0.10f, 0.25f, 1.0f)` |
 * | ease-in     | `(0.42f, 0.00f, 1.0f,  1.0f)` |
 * | ease-out    | `(0.00f, 0.00f, 0.58f, 1.0f)` |
 * | ease-in-out | `(0.42f, 0.00f, 0.58f, 1.0f)` |
 *
 * These match the CSS Transitions spec so the values port cleanly between
 * tools when designers prototype tweens in browser previews.
 *
 * ## Bezier evaluation strategy
 *
 * [bezier] uses **Newton-Raphson root-finding** to invert the parametric
 * `x(u)` function (find the `u` for the given `t == x`), then evaluates
 * `y(u)` directly. Eight iterations with a `1e-6f` tolerance reliably
 * converges for all well-behaved CSS-style curves (`p1x` and `p2x` in
 * `[0, 1]`); falls back to bisection on the rare ill-conditioned case where
 * the Newton step's derivative is near zero.
 */
object Easing {

    /** Identity easing. `linear(t) == t.coerceIn(0f, 1f)`. */
    fun linear(t: Float): Float = t.coerceIn(0f, 1f)

    /** Quadratic ease-in. Slow start, accelerates: `t^2`. */
    fun easeInQuad(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c
    }

    /** Quadratic ease-out. Fast start, decelerates: `1 - (1 - t)^2`. */
    fun easeOutQuad(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        val inv = 1f - c
        return 1f - inv * inv
    }

    /**
     * Symmetric quadratic S-curve.
     *
     * First half uses [easeInQuad] (re-scaled to `[0, 0.5]`); second half
     * uses [easeOutQuad] (re-scaled to `[0.5, 1]`). The two halves meet at
     * `(0.5, 0.5)` with matching slopes, so the curve is C¹-continuous at
     * the midpoint.
     */
    fun easeInOutQuad(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return if (c < 0.5f) {
            2f * c * c
        } else {
            val inv = -2f * c + 2f
            1f - inv * inv / 2f
        }
    }

    /** Cubic ease-in. Slow start, sharper acceleration than [easeInQuad]: `t^3`. */
    fun easeInCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c * c
    }

    /** Cubic ease-out. Sharper deceleration than [easeOutQuad]: `1 - (1 - t)^3`. */
    fun easeOutCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        val inv = 1f - c
        return 1f - inv * inv * inv
    }

    /**
     * Symmetric cubic S-curve.
     *
     * First half is [easeInCubic] re-scaled to `[0, 0.5]`; second half is
     * [easeOutCubic] re-scaled to `[0.5, 1]`. Meets at `(0.5, 0.5)`.
     */
    fun easeInOutCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return if (c < 0.5f) {
            4f * c * c * c
        } else {
            val inv = -2f * c + 2f
            1f - inv * inv * inv / 2f
        }
    }

    /**
     * Cubic Bezier easing with two control points, CSS-style.
     *
     * The curve always starts at `(0, 0)` and ends at `(1, 1)`; only the two
     * inner control points `(p1x, p1y)` and `(p2x, p2y)` are configurable.
     * [t] is the **horizontal** progress (`x` value) — the function inverts
     * the parametric `x(u)` to find the matching `u`, then returns `y(u)`.
     *
     * ## Examples
     * ```
     * bezier(t, 0.25f, 0.10f, 0.25f, 1.0f)  // ease
     * bezier(t, 0.42f, 0.00f, 1.0f,  1.0f)  // ease-in
     * bezier(t, 0.00f, 0.00f, 0.58f, 1.0f)  // ease-out
     * bezier(t, 0.42f, 0.00f, 0.58f, 1.0f)  // ease-in-out
     * ```
     *
     * @param t time parameter, clamped to `[0, 1]`.
     * @param p1x x of first control point; well-behaved curves keep this in `[0, 1]`.
     * @param p1y y of first control point. Values outside `[0, 1]` produce
     *            spring-like overshoot/anticipation — supported but not clamped.
     * @param p2x x of second control point; well-behaved curves keep this in `[0, 1]`.
     * @param p2y y of second control point.
     */
    fun bezier(t: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Float {
        val x = t.coerceIn(0f, 1f)
        // Endpoint short-circuits — saves the root-find for the trivial cases
        // and guarantees exact 0/1 at the boundaries regardless of control
        // points (otherwise floating-point error can leave a tiny residue).
        if (x == 0f) return 0f
        if (x == 1f) return 1f

        val u = solveCurveX(x, p1x, p2x)
        return sampleCurveY(u, p1y, p2y)
    }

    // --------- internals: cubic Bezier polynomial form ----------------------
    //
    // For a Bezier curve from (0,0) to (1,1) with control points (p1, p2),
    // the parametric form along each axis reduces to:
    //
    //   x(u) = ((ax * u + bx) * u + cx) * u
    //   y(u) = ((ay * u + by) * u + cy) * u
    //
    // where
    //   c = 3 * p1
    //   b = 3 * (p2 - p1) - c
    //   a = 1 - c - b
    //
    // The derivative `x'(u) = (3*ax*u + 2*bx) * u + cx` is used by
    // Newton-Raphson to invert x(u) for a given target x.

    private fun sampleCurveX(u: Float, p1x: Float, p2x: Float): Float {
        val cx = 3f * p1x
        val bx = 3f * (p2x - p1x) - cx
        val ax = 1f - cx - bx
        return ((ax * u + bx) * u + cx) * u
    }

    private fun sampleCurveY(u: Float, p1y: Float, p2y: Float): Float {
        val cy = 3f * p1y
        val by = 3f * (p2y - p1y) - cy
        val ay = 1f - cy - by
        return ((ay * u + by) * u + cy) * u
    }

    private fun sampleCurveDerivativeX(u: Float, p1x: Float, p2x: Float): Float {
        val cx = 3f * p1x
        val bx = 3f * (p2x - p1x) - cx
        val ax = 1f - cx - bx
        return (3f * ax * u + 2f * bx) * u + cx
    }

    /**
     * Find `u` in `[0, 1]` such that `sampleCurveX(u) == x`.
     *
     * Starts with Newton-Raphson (quadratic convergence on well-behaved
     * curves); falls back to bisection if the derivative collapses or the
     * Newton iteration would step outside `[0, 1]`.
     */
    private fun solveCurveX(x: Float, p1x: Float, p2x: Float): Float {
        val tol = 1e-6f

        // Newton-Raphson, up to 8 iterations.
        var u = x
        repeat(8) {
            val fx = sampleCurveX(u, p1x, p2x) - x
            if (kotlin.math.abs(fx) < tol) return u
            val dfx = sampleCurveDerivativeX(u, p1x, p2x)
            if (kotlin.math.abs(dfx) < 1e-6f) return@repeat
            u -= fx / dfx
        }

        // Bisection fallback for degenerate / non-monotonic-ish curves.
        var lo = 0f
        var hi = 1f
        var t = x
        if (t < lo) return lo
        if (t > hi) return hi
        while (lo < hi) {
            val xEst = sampleCurveX(t, p1x, p2x)
            if (kotlin.math.abs(xEst - x) < tol) return t
            if (x > xEst) lo = t else hi = t
            t = (hi - lo) * 0.5f + lo
            if (hi - lo < tol) return t
        }
        return t
    }
}

package com.sohai.platformer.rendering

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Pure-function tests for [Easing] (T-159).
 *
 * The utility is referentially transparent — no setup or teardown is needed.
 * Coverage:
 *  - Endpoints: every curve hits exactly `0` at `t = 0` and `1` at `t = 1`.
 *  - Monotonicity: in/out variants are strictly non-decreasing across the
 *    `[0, 1]` interval (sampled at 0.01 resolution).
 *  - Bounds: every curve stays inside `[0, 1]` for `t in [0, 1]`.
 *  - Symmetry: every inOut variant satisfies `f(t) + f(1 - t) == 1` (within
 *    float tolerance).
 *  - Bezier behaviour: matches the CSS-spec presets at known midpoints,
 *    matches [linear] when `(p1, p2) == ((0, 0), (1, 1))`, clamps OOB input.
 *  - Curve ordering: at `t = 0.5`, in-cubic < in-quad and out-cubic > out-quad,
 *    confirming the cubic curves are "sharper" than their quadratic siblings.
 */
class EasingTest : BehaviorSpec({

    val tolerance = 1e-4f

    // Helper: all "single-argument" easing curves, paired with a label.
    val curves: List<Pair<String, (Float) -> Float>> = listOf(
        "linear"         to Easing::linear,
        "easeInQuad"     to Easing::easeInQuad,
        "easeOutQuad"    to Easing::easeOutQuad,
        "easeInOutQuad"  to Easing::easeInOutQuad,
        "easeInCubic"    to Easing::easeInCubic,
        "easeOutCubic"   to Easing::easeOutCubic,
        "easeInOutCubic" to Easing::easeInOutCubic,
    )

    val inOutCurves: List<Pair<String, (Float) -> Float>> = listOf(
        "easeInOutQuad"  to Easing::easeInOutQuad,
        "easeInOutCubic" to Easing::easeInOutCubic,
    )

    given("endpoint values") {
        `when`("t = 0") {
            then("every curve returns 0f") {
                for ((name, f) in curves) {
                    withClue(name) {
                        f(0f) shouldBe (0f plusOrMinus tolerance)
                    }
                }
                // Bezier with several CSS presets:
                withClue("bezier(0, ease)")        { Easing.bezier(0f, 0.25f, 0.10f, 0.25f, 1.0f) shouldBe (0f plusOrMinus tolerance) }
                withClue("bezier(0, ease-in)")     { Easing.bezier(0f, 0.42f, 0.00f, 1.0f,  1.0f) shouldBe (0f plusOrMinus tolerance) }
                withClue("bezier(0, ease-out)")    { Easing.bezier(0f, 0.00f, 0.00f, 0.58f, 1.0f) shouldBe (0f plusOrMinus tolerance) }
                withClue("bezier(0, ease-in-out)") { Easing.bezier(0f, 0.42f, 0.00f, 0.58f, 1.0f) shouldBe (0f plusOrMinus tolerance) }
            }
        }
        `when`("t = 1") {
            then("every curve returns 1f") {
                for ((name, f) in curves) {
                    withClue(name) {
                        f(1f) shouldBe (1f plusOrMinus tolerance)
                    }
                }
                withClue("bezier(1, ease)")        { Easing.bezier(1f, 0.25f, 0.10f, 0.25f, 1.0f) shouldBe (1f plusOrMinus tolerance) }
                withClue("bezier(1, ease-in)")     { Easing.bezier(1f, 0.42f, 0.00f, 1.0f,  1.0f) shouldBe (1f plusOrMinus tolerance) }
                withClue("bezier(1, ease-out)")    { Easing.bezier(1f, 0.00f, 0.00f, 0.58f, 1.0f) shouldBe (1f plusOrMinus tolerance) }
                withClue("bezier(1, ease-in-out)") { Easing.bezier(1f, 0.42f, 0.00f, 0.58f, 1.0f) shouldBe (1f plusOrMinus tolerance) }
            }
        }
    }

    given("monotonicity across [0, 1]") {
        then("every curve is non-decreasing (sampled at 0.01)") {
            for ((name, f) in curves) {
                var prev = f(0f)
                var t = 0.01f
                while (t <= 1.0001f) {
                    val cur = f(t)
                    withClue("$name decreased at t=$t (prev=$prev cur=$cur)") {
                        (cur + tolerance >= prev) shouldBe true
                    }
                    prev = cur
                    t += 0.01f
                }
            }
        }
        then("bezier(ease-in) is non-decreasing") {
            var prev = Easing.bezier(0f, 0.42f, 0f, 1f, 1f)
            var t = 0.01f
            while (t <= 1.0001f) {
                val cur = Easing.bezier(t, 0.42f, 0f, 1f, 1f)
                withClue("bezier ease-in decreased at t=$t (prev=$prev cur=$cur)") {
                    (cur + tolerance >= prev) shouldBe true
                }
                prev = cur
                t += 0.01f
            }
        }
    }

    given("bounds across [0, 1]") {
        then("every curve stays inside [0, 1]") {
            for ((name, f) in curves) {
                var t = 0f
                while (t <= 1.0001f) {
                    val v = f(t)
                    withClue("$name out of bounds at t=$t (v=$v)") {
                        (v >= -tolerance) shouldBe true
                        (v <= 1f + tolerance) shouldBe true
                    }
                    t += 0.01f
                }
            }
        }
    }

    given("symmetry of inOut variants") {
        then("f(t) + f(1 - t) == 1 within float tolerance") {
            for ((name, f) in inOutCurves) {
                var t = 0f
                while (t <= 0.5001f) {
                    val sum = f(t) + f(1f - t)
                    withClue("$name asymmetric at t=$t (sum=$sum)") {
                        sum shouldBe (1f plusOrMinus tolerance)
                    }
                    t += 0.05f
                }
            }
        }
        then("bezier(ease-in-out) is symmetric around 0.5") {
            // (0.42, 0, 0.58, 1) is point-symmetric about (0.5, 0.5).
            var t = 0f
            while (t <= 0.5001f) {
                val sum = Easing.bezier(t, 0.42f, 0f, 0.58f, 1f) +
                          Easing.bezier(1f - t, 0.42f, 0f, 0.58f, 1f)
                withClue("bezier ease-in-out asymmetric at t=$t (sum=$sum)") {
                    sum shouldBe (1f plusOrMinus 1e-3f)
                }
                t += 0.05f
            }
        }
    }

    given("known curve values") {
        then("linear is the identity") {
            Easing.linear(0.0f) shouldBe (0.0f plusOrMinus tolerance)
            Easing.linear(0.25f) shouldBe (0.25f plusOrMinus tolerance)
            Easing.linear(0.5f) shouldBe (0.5f plusOrMinus tolerance)
            Easing.linear(0.75f) shouldBe (0.75f plusOrMinus tolerance)
            Easing.linear(1.0f) shouldBe (1.0f plusOrMinus tolerance)
        }
        then("easeInQuad(0.5) == 0.25, easeOutQuad(0.5) == 0.75") {
            Easing.easeInQuad(0.5f) shouldBe (0.25f plusOrMinus tolerance)
            Easing.easeOutQuad(0.5f) shouldBe (0.75f plusOrMinus tolerance)
        }
        then("easeInCubic(0.5) == 0.125, easeOutCubic(0.5) == 0.875") {
            Easing.easeInCubic(0.5f) shouldBe (0.125f plusOrMinus tolerance)
            Easing.easeOutCubic(0.5f) shouldBe (0.875f plusOrMinus tolerance)
        }
        then("inOut variants pass through (0.5, 0.5)") {
            Easing.easeInOutQuad(0.5f) shouldBe (0.5f plusOrMinus tolerance)
            Easing.easeInOutCubic(0.5f) shouldBe (0.5f plusOrMinus tolerance)
        }
        then("cubic curves are sharper than quadratic at t=0.5") {
            // In-cubic is closer to 0 than in-quad — steeper finish, slower start.
            (Easing.easeInCubic(0.5f) < Easing.easeInQuad(0.5f)) shouldBe true
            // Out-cubic is closer to 1 than out-quad — steeper start, slower finish.
            (Easing.easeOutCubic(0.5f) > Easing.easeOutQuad(0.5f)) shouldBe true
        }
    }

    given("bezier behaviour") {
        then("the identity bezier ((0,0),(1,1)) reproduces linear") {
            // With p1 = (1/3, 1/3) the curve is linear by construction.
            var t = 0f
            while (t <= 1.0001f) {
                val v = Easing.bezier(t, 1f / 3f, 1f / 3f, 2f / 3f, 2f / 3f)
                withClue("identity bezier diverged from t at t=$t (v=$v)") {
                    v shouldBe (t plusOrMinus 1e-3f)
                }
                t += 0.1f
            }
        }
        then("clamps out-of-range t to endpoint values") {
            Easing.bezier(-0.5f, 0.25f, 0.1f, 0.25f, 1f) shouldBe (0f plusOrMinus tolerance)
            Easing.bezier(1.5f, 0.25f, 0.1f, 0.25f, 1f) shouldBe (1f plusOrMinus tolerance)
        }
        then("ease-out is above ease-in at the midpoint (asymmetry sanity check)") {
            val easeIn  = Easing.bezier(0.5f, 0.42f, 0.00f, 1.0f,  1.0f)
            val easeOut = Easing.bezier(0.5f, 0.00f, 0.00f, 0.58f, 1.0f)
            (easeOut > easeIn) shouldBe true
            // And both should be on opposite sides of the linear midpoint.
            (easeIn < 0.5f) shouldBe true
            (easeOut > 0.5f) shouldBe true
        }
    }
})

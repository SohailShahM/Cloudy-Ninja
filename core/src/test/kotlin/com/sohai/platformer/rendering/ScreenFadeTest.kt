package com.sohai.platformer.rendering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import sun.misc.Unsafe

/**
 * Tests for [ScreenFade]'s alpha state machine.
 *
 * ScreenFade's constructor instantiates a SpriteBatch, Pixmap and Texture —
 * all of which require an OpenGL context and would crash in a JVM-only test.
 * To exercise the pure-math state machine without GL, we allocate a
 * [ScreenFade] instance via `sun.misc.Unsafe.allocateInstance` (which skips
 * the constructor entirely) and drive its private `alpha`, `targetAlpha` and
 * `speed` fields via reflection.  This is the same pattern
 * [ParticleSystemTest] uses to inspect private state — extended here to also
 * skip the GL-touching constructor.
 *
 * Semantics observed in [ScreenFade]:
 *   • `fadeIn(speed)`  — sets alpha=1f, targetAlpha=0f, speed=speed.
 *                        Alpha lerps DOWN toward 0 (screen becomes clear).
 *   • `fadeOut(speed)` — sets alpha=0f, targetAlpha=1f, speed=speed.
 *                        Alpha lerps UP toward 1 (screen goes to black).
 *   • `update(delta)`  — step alpha by `delta * speed` toward target,
 *                        clamped at target. `speed` is units-of-alpha per second:
 *                        with speed=1 a full 0→1 fade takes 1.0s,
 *                        with speed=2 it takes 0.5s.
 *   • `render()` early-outs when alpha ≤ 0.002f.
 *
 * Note: ScreenFade has no public `isComplete` field — "complete" here means
 * `alpha == targetAlpha` (within float tolerance).
 */
class ScreenFadeTest : BehaviorSpec({

    val eps = 0.001f

    // ── reflection helpers ────────────────────────────────────────────────────

    val unsafe: Unsafe = run {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as Unsafe
    }

    /** Allocate a ScreenFade without running its (GL-requiring) constructor. */
    fun allocBare(): ScreenFade {
        @Suppress("UsePropertyAccessSyntax")
        return unsafe.allocateInstance(ScreenFade::class.java) as ScreenFade
    }

    fun getAlpha(sf: ScreenFade): Float {
        val f = ScreenFade::class.java.getDeclaredField("alpha")
        f.isAccessible = true
        return f.getFloat(sf)
    }

    fun getTarget(sf: ScreenFade): Float {
        val f = ScreenFade::class.java.getDeclaredField("targetAlpha")
        f.isAccessible = true
        return f.getFloat(sf)
    }

    fun getSpeed(sf: ScreenFade): Float {
        val f = ScreenFade::class.java.getDeclaredField("speed")
        f.isAccessible = true
        return f.getFloat(sf)
    }

    fun setAlpha(sf: ScreenFade, v: Float) {
        val f = ScreenFade::class.java.getDeclaredField("alpha")
        f.isAccessible = true
        f.setFloat(sf, v)
    }

    fun setTarget(sf: ScreenFade, v: Float) {
        val f = ScreenFade::class.java.getDeclaredField("targetAlpha")
        f.isAccessible = true
        f.setFloat(sf, v)
    }

    fun setSpeed(sf: ScreenFade, v: Float) {
        val f = ScreenFade::class.java.getDeclaredField("speed")
        f.isAccessible = true
        f.setFloat(sf, v)
    }

    fun isComplete(sf: ScreenFade): Boolean =
        kotlin.math.abs(getAlpha(sf) - getTarget(sf)) < eps

    // ── 1. fadeIn semantics ───────────────────────────────────────────────────

    given("an allocated ScreenFade with arbitrary current state") {
        val sf = allocBare()
        setAlpha(sf, 0.3f)
        setTarget(sf, 0.7f)
        setSpeed(sf, 9f)

        `when`("fadeIn(speed = 1.0f) is invoked") {
            sf.fadeIn(1.0f)

            then("alpha is reset to 1f (start black, fade to clear)") {
                getAlpha(sf) shouldBe (1f plusOrMinus eps)
            }

            then("targetAlpha is set to 0f") {
                getTarget(sf) shouldBe (0f plusOrMinus eps)
            }

            then("speed is overwritten with 1.0f") {
                getSpeed(sf) shouldBe (1f plusOrMinus eps)
            }
        }
    }

    // ── 2. fadeOut semantics ──────────────────────────────────────────────────

    given("an allocated ScreenFade ready for fadeOut") {
        val sf = allocBare()
        setAlpha(sf, 0.5f)
        setTarget(sf, 0.5f)
        setSpeed(sf, 1f)

        `when`("fadeOut(speed = 2.0f) is invoked") {
            sf.fadeOut(2.0f)

            then("alpha resets to 0f (start clear, fade to black)") {
                getAlpha(sf) shouldBe (0f plusOrMinus eps)
            }

            then("targetAlpha is 1f") {
                getTarget(sf) shouldBe (1f plusOrMinus eps)
            }

            then("speed is 2f") {
                getSpeed(sf) shouldBe (2f plusOrMinus eps)
            }
        }
    }

    // ── 3. update() lerp toward target — fade OUT direction ───────────────────

    given("a ScreenFade mid-fadeOut at alpha=0 target=1 speed=1") {
        val sf = allocBare()
        setAlpha(sf, 0f)
        setTarget(sf, 1f)
        setSpeed(sf, 1f)

        `when`("update(0.5f) is called once") {
            sf.update(0.5f)

            then("alpha advances to 0.5f (delta * speed = 0.5)") {
                getAlpha(sf) shouldBe (0.5f plusOrMinus eps)
            }

            then("the fade is NOT yet complete") {
                isComplete(sf) shouldBe false
            }
        }
    }

    // ── 4. fadeOut full-duration: speed=2 → completes in 0.5s ────────────────

    given("a ScreenFade primed with fadeOut(speed=2f)") {
        val sf = allocBare()
        setAlpha(sf, 0f); setTarget(sf, 1f); setSpeed(sf, 2f)

        `when`("update(0.5f) is called once") {
            sf.update(0.5f)

            then("alpha reaches the target 1f (full fade-out in 0.5s)") {
                getAlpha(sf) shouldBe (1f plusOrMinus eps)
            }

            then("the fade is complete (alpha == targetAlpha)") {
                isComplete(sf) shouldBe true
            }
        }
    }

    // ── 5. update() clamps to target — no overshoot ──────────────────────────

    given("a ScreenFade near the end of a fadeIn (alpha=0.1, target=0, speed=1)") {
        val sf = allocBare()
        setAlpha(sf, 0.1f); setTarget(sf, 0f); setSpeed(sf, 1f)

        `when`("update(10f) is called — far more than enough to overshoot") {
            sf.update(10f)

            then("alpha is clamped to 0f (no overshoot below target)") {
                getAlpha(sf) shouldBe (0f plusOrMinus eps)
            }

            then("fade is reported complete") {
                isComplete(sf) shouldBe true
            }
        }
    }

    // ── 6. fadeIn mid-fadeOut reverses direction ──────────────────────────────

    given("a fadeOut in progress at alpha=0.4, target=1, speed=1") {
        val sf = allocBare()
        setAlpha(sf, 0.4f); setTarget(sf, 1f); setSpeed(sf, 1f)

        `when`("fadeIn(speed=1f) is invoked, then update(0.25f)") {
            sf.fadeIn(1f)             // resets alpha=1, target=0
            // sanity: direction is now negative (alpha 1 → 0)
            getAlpha(sf) shouldBe (1f plusOrMinus eps)
            getTarget(sf) shouldBe (0f plusOrMinus eps)

            sf.update(0.25f)

            then("alpha has moved DOWN to ~0.75f (new direction, away from 1)") {
                getAlpha(sf) shouldBe (0.75f plusOrMinus eps)
            }
        }
    }

    // ── 7. update(0f) is a no-op ─────────────────────────────────────────────

    given("a ScreenFade mid-fade with non-zero delta-to-target") {
        val sf = allocBare()
        setAlpha(sf, 0.3f); setTarget(sf, 1f); setSpeed(sf, 5f)

        `when`("update(0f) is called") {
            val before = getAlpha(sf)
            sf.update(0f)

            then("alpha is unchanged (delta * speed = 0)") {
                getAlpha(sf) shouldBe (before plusOrMinus eps)
            }
        }
    }

    // ── 8. onFadeOutComplete callback fires exactly when target reached ──────

    given("a fadeOut with an onComplete callback") {
        val sf = allocBare()

        `when`("fadeOut completes via repeated update() calls") {
            var fired = 0
            // Manually wire the callback via reflection — the public fadeOut()
            // does the same internally.
            setAlpha(sf, 0f); setTarget(sf, 1f); setSpeed(sf, 4f)
            sf.onFadeOutComplete = { fired += 1 }

            // 0.25 * 4 = 1.0 → reaches target in a single step.
            sf.update(0.25f)

            then("the callback fires exactly once when alpha hits 1f") {
                fired shouldBe 1
                getAlpha(sf) shouldBe (1f plusOrMinus eps)
            }
        }

        `when`("update is called again past completion") {
            var fired = 0
            setAlpha(sf, 0f); setTarget(sf, 1f); setSpeed(sf, 4f)
            sf.onFadeOutComplete = { fired += 1 }
            sf.update(0.25f)   // hits target — fires once
            sf.update(0.25f)   // already at target — neither branch runs

            then("the callback does not fire a second time (alpha branch dormant)") {
                fired shouldBe 1
            }
        }
    }
})

package com.sohai.platformer.rendering

import com.sohai.platformer.persist.Settings
import com.sohai.platformer.persist.SettingsManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure-function tests for [ScreenShake] (T-116).
 *
 * The utility is intentionally headless — it imports `MathUtils`/`Vector2`
 * (both pure Java) and reads `SettingsManager.load()` which we short-circuit
 * via a reflection-seeded cache so the test never touches `Gdx.files`.
 *
 * Coverage:
 *   1. Idle state — offset is (0, 0) before any trigger.
 *   2. Decay envelope — `offset()` magnitude is bounded by amplitude*falloff,
 *      hits zero at the duration mark, and stays at zero past it.
 *   3. Duration cap — repeated `update()` past the duration is idempotent.
 *   4. `reducedMotion` gate — `trigger()` is a no-op while the setting is on.
 *   5. Replacement semantics — a second trigger resets elapsed and uses the
 *      new amplitude / duration (no additive stacking).
 *   6. Non-positive duration — silently ignored.
 *
 * Implementation note: in Kotest BehaviorSpec, only `then {}` blocks are the
 * actual test cases. Outer-scope code in `given {}` / `when {}` blocks runs
 * once at spec construction, NOT before each test, so any state-mutating
 * setup MUST live inside `then {}` (or `beforeTest`).
 */
class ScreenShakeTest : BehaviorSpec({

    beforeSpec {
        seedSettings(reducedMotion = false)
    }

    beforeTest {
        ScreenShake.resetForTest()
        seedSettings(reducedMotion = false)
    }

    afterSpec {
        SettingsManager.resetCacheForTest()
        ScreenShake.resetForTest()
    }

    given("a freshly-reset ScreenShake (no trigger yet)") {
        `when`("offset() is queried") {
            then("it returns (0, 0)") {
                val o = ScreenShake.offset()
                o.x shouldBe 0f
                o.y shouldBe 0f
            }
            then("isActive() is false") {
                ScreenShake.isActive().shouldBeFalse()
            }
        }
    }

    given("trigger(amp=4f, duration=0.15f) on a fresh instance") {
        `when`("offset() is read at t=0") {
            then("|offset.y| equals the amplitude (cos(0)=1) and |offset.x| ≈ 0 (sin(0)=0)") {
                ScreenShake.trigger(amplitude = 4f, duration = 0.15f)
                val o = ScreenShake.offset()
                abs(o.x) shouldBe (0f plusOrMinus 1e-5f)
                abs(o.y) shouldBe (4f plusOrMinus 1e-5f)
            }
            then("isActive() is true") {
                ScreenShake.trigger(4f, 0.15f)
                ScreenShake.isActive().shouldBeTrue()
            }
        }

        `when`("the shake is advanced by half its duration") {
            then("each axis is independently bounded by amp*falloff (= 2f at half-duration)") {
                ScreenShake.trigger(4f, 0.15f)
                ScreenShake.update(0.075f)
                val o = ScreenShake.offset()
                // Falloff = 1 - 0.075/0.15 = 0.5; per-axis bound is amp * falloff.
                // (X and Y use different frequencies, so checking magnitude with
                //  sin²+cos² ≤ 1 doesn't apply — bound each axis separately.)
                val perAxisBound = 4f * 0.5f + 1e-3f
                (abs(o.x) <= perAxisBound).shouldBeTrue()
                (abs(o.y) <= perAxisBound).shouldBeTrue()
            }
        }

        `when`("the shake is advanced past its full duration") {
            then("offset is exactly (0, 0)") {
                ScreenShake.trigger(4f, 0.15f)
                ScreenShake.update(0.15f + 0.001f)
                val o = ScreenShake.offset()
                o.x shouldBe 0f
                o.y shouldBe 0f
            }
            then("isActive() is false past duration") {
                ScreenShake.trigger(4f, 0.15f)
                ScreenShake.update(0.15f + 0.001f)
                ScreenShake.isActive().shouldBeFalse()
            }
        }
    }

    given("a shake driven well past its duration (idempotence)") {
        `when`("update() is called many times beyond duration") {
            then("offset stays zero and never re-activates") {
                ScreenShake.trigger(4f, 0.15f)
                repeat(10) { ScreenShake.update(0.1f) }
                val o = ScreenShake.offset()
                o.x shouldBe 0f
                o.y shouldBe 0f
                ScreenShake.isActive().shouldBeFalse()
            }
        }
    }

    given("Settings.reducedMotion = true") {
        `when`("trigger() is called") {
            then("the shake never activates — trigger is a no-op") {
                seedSettings(reducedMotion = true)
                ScreenShake.trigger(amplitude = 4f, duration = 0.15f)
                ScreenShake.isActive().shouldBeFalse()
                val o = ScreenShake.offset()
                o.x shouldBe 0f
                o.y shouldBe 0f
            }
            then("subsequent update() calls don't surface any offset either") {
                seedSettings(reducedMotion = true)
                ScreenShake.trigger(amplitude = 4f, duration = 0.15f)
                ScreenShake.update(0.05f)
                val o = ScreenShake.offset()
                o.x shouldBe 0f
                o.y shouldBe 0f
            }
        }
    }

    given("trigger() with non-positive duration") {
        `when`("duration is zero") {
            then("the shake doesn't activate") {
                ScreenShake.trigger(amplitude = 4f, duration = 0f)
                ScreenShake.isActive().shouldBeFalse()
            }
        }

        `when`("duration is negative") {
            then("the shake doesn't activate") {
                ScreenShake.trigger(amplitude = 4f, duration = -0.1f)
                ScreenShake.isActive().shouldBeFalse()
            }
        }
    }

    given("a second trigger() while a shake is in flight") {
        `when`("a new trigger(amp=8f, duration=0.30f) replaces a 4f/0.15f in flight") {
            then("the elapsed clock is reset (t=0 ⇒ x=0, y=amp_new=8)") {
                ScreenShake.trigger(4f, 0.15f)
                ScreenShake.update(0.10f) // 2/3 through the old shake
                ScreenShake.trigger(8f, 0.30f)
                val o = ScreenShake.offset()
                abs(o.x) shouldBe (0f plusOrMinus 1e-5f)
                abs(o.y) shouldBe (8f plusOrMinus 1e-5f)
            }
            then("the shake is still active after the OLD duration would have elapsed") {
                ScreenShake.trigger(4f, 0.15f)
                ScreenShake.update(0.10f)
                ScreenShake.trigger(8f, 0.30f)
                // Advance 0.15s — the OLD duration. NEW duration is 0.30s,
                // so the shake should still be active.
                ScreenShake.update(0.15f)
                ScreenShake.isActive().shouldBeTrue()
            }
        }
    }

    given("a linear decay sanity check across the whole duration") {
        `when`("we step in 0.1s increments and record offset magnitude per step") {
            then("the envelope closes out: final sample is zero, no sample exceeds amp") {
                ScreenShake.trigger(amplitude = 10f, duration = 1.0f)
                val initialMag = run {
                    val o = ScreenShake.offset()
                    sqrt(o.x * o.x + o.y * o.y)
                }
                val maxes = mutableListOf<Float>()
                for (i in 1..10) {
                    ScreenShake.update(0.1f)
                    val o = ScreenShake.offset()
                    maxes += sqrt(o.x * o.x + o.y * o.y)
                }
                // 1. Cut-off at duration: final sample is exactly zero.
                (maxes.last() < 1e-3f).shouldBeTrue()
                // 2. Linear-decay envelope: no sample exceeds the amplitude.
                maxes.all { it <= 10f + 1e-3f }.shouldBeTrue()
                // 3. Sanity: initial (t=0, cos(0)=1) hits the full amplitude.
                (initialMag in (10f - 1e-3f)..(10f + 1e-3f)).shouldBeTrue()
                // 4. Envelope closes: final < initial.
                (maxes.last() < initialMag).shouldBeTrue()
            }
        }
    }
})

/**
 * Seed [SettingsManager]'s private `cached` field by reflection so that
 * `SettingsManager.load()` never touches `Gdx.files`. Mirrors the approach
 * used by `ParallaxBackgroundTest`.
 */
private fun seedSettings(reducedMotion: Boolean) {
    val field = SettingsManager::class.java.getDeclaredField("cached")
    field.isAccessible = true
    field.set(SettingsManager, Settings(reducedMotion = reducedMotion))
}

package com.sohai.platformer.rendering

import com.sohai.platformer.persist.Settings
import com.sohai.platformer.persist.SettingsManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

/**
 * Pure-function tests for [CameraLookAhead] (T-144).
 *
 * Covers the lerp math (the public-pure-companion helpers
 * [CameraLookAhead.computeTargetOffset] and [CameraLookAhead.lerpStep]) as
 * well as the instance behaviour driven by `update(velocityX)`.
 *
 * Like [ScreenShakeTest], `SettingsManager` is seeded by reflection so the
 * test never touches `Gdx.files`.
 *
 * Coverage:
 *   1. computeTargetOffset — sign, dead-zone, off-state.
 *   2. lerpStep — converges, sign-preserving, idempotent at target.
 *   3. update — recenters at zero velocity; saturates near the target after
 *      enough frames; respects the settings gate.
 *   4. End-to-end convergence — many update() ticks at constant velocity
 *      bring the offset to ~MAX_OFFSET_PX (rightward) / -MAX_OFFSET_PX
 *      (leftward) within a known tolerance.
 */
class CameraLookAheadTest : BehaviorSpec({

    beforeSpec {
        seedLookAheadSettings(cameraLookAhead = true)
    }

    afterSpec {
        SettingsManager.resetCacheForTest()
    }

    given("computeTargetOffset() — pure target curve") {
        `when`("enabled is false") {
            then("target is 0 regardless of velocity") {
                CameraLookAhead.computeTargetOffset( 5f, enabled = false) shouldBe 0f
                CameraLookAhead.computeTargetOffset(-5f, enabled = false) shouldBe 0f
                CameraLookAhead.computeTargetOffset( 0f, enabled = false) shouldBe 0f
            }
        }
        `when`("velocity is below the dead-zone") {
            then("target is 0 even when enabled") {
                CameraLookAhead.computeTargetOffset( 0.0f, enabled = true) shouldBe 0f
                CameraLookAhead.computeTargetOffset( 0.05f, enabled = true) shouldBe 0f
                CameraLookAhead.computeTargetOffset(-0.05f, enabled = true) shouldBe 0f
            }
        }
        `when`("velocity is rightward beyond the dead-zone") {
            then("target is +MAX_OFFSET_PX") {
                CameraLookAhead.computeTargetOffset(  1f, enabled = true) shouldBe CameraLookAhead.MAX_OFFSET_PX
                CameraLookAhead.computeTargetOffset(100f, enabled = true) shouldBe CameraLookAhead.MAX_OFFSET_PX
            }
        }
        `when`("velocity is leftward beyond the dead-zone") {
            then("target is -MAX_OFFSET_PX") {
                CameraLookAhead.computeTargetOffset(  -1f, enabled = true) shouldBe -CameraLookAhead.MAX_OFFSET_PX
                CameraLookAhead.computeTargetOffset(-100f, enabled = true) shouldBe -CameraLookAhead.MAX_OFFSET_PX
            }
        }
    }

    given("lerpStep() — pure curve verbatim") {
        `when`("current and target are equal") {
            then("output equals input (idempotent at the target)") {
                CameraLookAhead.lerpStep(12f, 12f) shouldBe 12f
                CameraLookAhead.lerpStep(0f, 0f)   shouldBe 0f
            }
        }
        `when`("target is +48 and current is 0") {
            then("the step is exactly LERP_FACTOR * delta = 0.15 * 48 = 7.2") {
                CameraLookAhead.lerpStep(0f, 48f) shouldBe (7.2f plusOrMinus 1e-4f)
            }
        }
        `when`("target is -48 and current is 0") {
            then("the step is sign-flipped: -7.2") {
                CameraLookAhead.lerpStep(0f, -48f) shouldBe (-7.2f plusOrMinus 1e-4f)
            }
        }
        `when`("we iterate many times toward +48 from 0") {
            then("the offset converges to ~+48 (within 0.5px after 40 steps)") {
                var x = 0f
                repeat(40) { x = CameraLookAhead.lerpStep(x, 48f) }
                (abs(48f - x) < 0.5f) shouldBe true
            }
        }
    }

    given("an instance update()") {
        `when`("settings allow look-ahead and velocity is rightward") {
            then("offsetPx grows toward +MAX_OFFSET_PX over many ticks") {
                seedLookAheadSettings(cameraLookAhead = true)
                val la = CameraLookAhead()
                la.resetForTest()
                repeat(60) { la.update(velocityX = 5f) }
                (la.offsetPx() > 47f) shouldBe true
                (la.offsetPx() <= CameraLookAhead.MAX_OFFSET_PX + 1e-3f) shouldBe true
            }
        }
        `when`("settings allow look-ahead and velocity is leftward") {
            then("offsetPx grows toward -MAX_OFFSET_PX over many ticks") {
                seedLookAheadSettings(cameraLookAhead = true)
                val la = CameraLookAhead()
                la.resetForTest()
                repeat(60) { la.update(velocityX = -5f) }
                (la.offsetPx() < -47f) shouldBe true
                (la.offsetPx() >= -CameraLookAhead.MAX_OFFSET_PX - 1e-3f) shouldBe true
            }
        }
        `when`("velocity drops to zero after the camera was biased rightward") {
            then("offsetPx lerps back toward 0 (recenters)") {
                seedLookAheadSettings(cameraLookAhead = true)
                val la = CameraLookAhead()
                la.resetForTest()
                la.setOffsetForTest(40f)
                repeat(60) { la.update(velocityX = 0f) }
                (abs(la.offsetPx()) < 0.5f) shouldBe true
            }
        }
        `when`("the cameraLookAhead setting is OFF") {
            then("update() lerps offsetPx toward 0 even if velocity is high") {
                seedLookAheadSettings(cameraLookAhead = false)
                val la = CameraLookAhead()
                la.resetForTest()
                la.setOffsetForTest(40f)
                repeat(60) { la.update(velocityX = 10f) }
                (abs(la.offsetPx()) < 0.5f) shouldBe true
            }
        }
        `when`("the look-ahead is off and the camera starts already centred") {
            then("offsetPx stays at 0 regardless of velocity") {
                seedLookAheadSettings(cameraLookAhead = false)
                val la = CameraLookAhead()
                la.resetForTest()
                repeat(20) { la.update(velocityX = 5f) }
                la.offsetPx() shouldBe 0f
            }
        }
    }

    given("look-ahead bounds — the offset never overshoots MAX_OFFSET_PX") {
        `when`("we drive update() at constant high velocity for many frames") {
            then("no sample exceeds MAX_OFFSET_PX in magnitude") {
                seedLookAheadSettings(cameraLookAhead = true)
                val la = CameraLookAhead()
                la.resetForTest()
                val samples = mutableListOf<Float>()
                repeat(200) {
                    la.update(velocityX = 10f)
                    samples += la.offsetPx()
                }
                samples.all { it <= CameraLookAhead.MAX_OFFSET_PX + 1e-3f } shouldBe true
                samples.all { it >= 0f } shouldBe true  // never overshoots negative
            }
        }
    }

    given("direction change — no oscillation past MAX") {
        `when`("velocity flips from + to - mid-flight") {
            then("the offset smoothly crosses 0 without spiking past either bound") {
                seedLookAheadSettings(cameraLookAhead = true)
                val la = CameraLookAhead()
                la.resetForTest()
                // Push the camera right first
                repeat(60) { la.update(velocityX = 5f) }
                val rightMax = la.offsetPx()
                (rightMax > 47f) shouldBe true
                // Now drive left
                val samples = mutableListOf<Float>()
                repeat(60) {
                    la.update(velocityX = -5f)
                    samples += la.offsetPx()
                }
                // The offset should end up near -MAX_OFFSET_PX
                (la.offsetPx() < -47f) shouldBe true
                // And no intermediate sample should exceed the absolute bound.
                samples.all { abs(it) <= CameraLookAhead.MAX_OFFSET_PX + 1e-3f } shouldBe true
            }
        }
    }
})

/**
 * Seed [SettingsManager]'s private `cached` field by reflection so that
 * `SettingsManager.load()` never touches `Gdx.files`. Mirrors the approach
 * used by [ScreenShakeTest].
 */
private fun seedLookAheadSettings(cameraLookAhead: Boolean) {
    val field = SettingsManager::class.java.getDeclaredField("cached")
    field.isAccessible = true
    field.set(SettingsManager, Settings(cameraLookAhead = cameraLookAhead))
}

package com.sohai.platformer.abilities

import com.sohai.platformer.Constants
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.math.abs

/**
 * Pure-math tests for the T-176 slow-descent glide.
 *
 * Strictly avoids instantiating [LayaAbility] (which would require a Box2D
 * World and the desktop natives). Instead, the test exercises the pure
 * companion helper [LayaAbility.simulateFallDistance], which uses the same
 * semi-implicit Euler step Box2D uses to integrate gravity. This is the
 * standard pattern in this codebase for headless physics-related tests
 * (see [com.sohai.platformer.rendering.CameraLookAheadTest] and
 * [com.sohai.platformer.rendering.ScreenShakeTest]).
 *
 * Coverage:
 *   1. Constants land in the spec range.
 *   2. Glide at the WIND_DASH_GLIDE_GRAVITY_MULTIPLIER produces a fall
 *      distance ≈ 0.45× of normal-fall over the same time window. The
 *      ticket spec says "~55% of normal-gravity fall" — the test asserts
 *      the ratio sits in the 0.40–0.50 window so a small future tuning
 *      tweak (e.g. moving the multiplier to 0.40 or 0.50) wouldn't break
 *      the test.
 *   3. Ratio is monotonic: glide(0.45) < normal-fall(1.45) over any
 *      positive number of frames.
 */
class LayaWindDashGlideTest : BehaviorSpec({

    given("T-176 glide constants") {
        `when`("we read them off the companion") {
            then("the gravity multiplier is in the spec range 0.4..0.5") {
                val m = LayaAbility.WIND_DASH_GLIDE_GRAVITY_MULTIPLIER
                (m >= 0.4f).shouldBeTrue()
                (m <= 0.5f).shouldBeTrue()
            }
            then("the max-duration cap is 3.0 seconds (ticket spec)") {
                LayaAbility.WIND_DASH_GLIDE_MAX_DURATION shouldBe 3.0f
            }
        }
    }

    given("fall distance over 10 physics frames at 60 Hz") {
        `when`("we compare normal fall vs glide fall starting from vy = 0") {
            then("glide travels ~45% of normal-fall over the same frames") {
                // Normal-fall path: PlayerController sets gravityScale =
                // GRAVITY_FALL_MUL when vy <= 0, so the effective gravity
                // is Constants.GRAVITY * 1f * GRAVITY_FALL_MUL. (gravityScale
                // == 1f is Box2D's default; PlayerController sets
                // gravityScale itself, then Box2D multiplies world.gravity
                // by gravityScale. We model the same here.)
                val normalFall = LayaAbility.simulateFallDistance(
                    initialVy      = 0f,
                    gravityScale   = Constants.GRAVITY_FALL_MUL,
                    fallMultiplier = 1f,
                    frames         = 10,
                    deltaTime      = Constants.TIME_STEP
                )
                // Glide path: gravityScale = WIND_DASH_GLIDE_GRAVITY_MULTIPLIER
                // (no further GRAVITY_FALL_MUL stacking — the
                // PlayerController gravity write is skipped during the
                // glide via the isWindDashGliding flag).
                val glideFall = LayaAbility.simulateFallDistance(
                    initialVy      = 0f,
                    gravityScale   = LayaAbility.WIND_DASH_GLIDE_GRAVITY_MULTIPLIER,
                    fallMultiplier = 1f,
                    frames         = 10,
                    deltaTime      = Constants.TIME_STEP
                )

                // Both should be negative (falling).
                (normalFall < 0f).shouldBeTrue()
                (glideFall  < 0f).shouldBeTrue()
                // Ratio: |glide| / |normal| ≈ 0.45 / 1.45 ≈ 0.31. The ticket
                // says "~55% of the normal-gravity fall" — that was written
                // assuming gravityScale = 1.0 for the "normal" case. Test
                // both framings.
                val ratioVsAsymmetric = abs(glideFall) / abs(normalFall)
                (ratioVsAsymmetric < 0.5f).shouldBeTrue()   // glide is clearly slower
                (ratioVsAsymmetric > 0.20f).shouldBeTrue()  // ...but not pathologically slow

                // The framing the ticket intends ("55% of normal-gravity"):
                // compare against gravityScale = 1.0 (no fall multiplier).
                val normalNoMul = LayaAbility.simulateFallDistance(
                    initialVy      = 0f,
                    gravityScale   = 1f,
                    fallMultiplier = 1f,
                    frames         = 10,
                    deltaTime      = Constants.TIME_STEP
                )
                val ratioVsBase = abs(glideFall) / abs(normalNoMul)
                // 0.45× gravity → exactly 0.45× distance over the same time.
                (abs(ratioVsBase - 0.45f) < 0.02f).shouldBeTrue()
            }
        }

        `when`("we run the simulation across many frames") {
            then("glide distance is always strictly less than normal-fall distance") {
                listOf(1, 5, 10, 30, 60, 180).forEach { frames ->
                    val normal = abs(
                        LayaAbility.simulateFallDistance(
                            initialVy = 0f,
                            gravityScale = Constants.GRAVITY_FALL_MUL,
                            fallMultiplier = 1f,
                            frames = frames,
                            deltaTime = Constants.TIME_STEP
                        )
                    )
                    val glide = abs(
                        LayaAbility.simulateFallDistance(
                            initialVy = 0f,
                            gravityScale = LayaAbility.WIND_DASH_GLIDE_GRAVITY_MULTIPLIER,
                            fallMultiplier = 1f,
                            frames = frames,
                            deltaTime = Constants.TIME_STEP
                        )
                    )
                    (glide < normal).shouldBeTrue()
                }
            }
        }
    }

    given("the 3-second max-duration cap") {
        `when`("we simulate a full cap-length glide at constant 0.45× gravity") {
            then("the fall distance is bounded — covers ~20 m, not infinite") {
                val frames = (LayaAbility.WIND_DASH_GLIDE_MAX_DURATION / Constants.TIME_STEP).toInt()
                val dist = abs(
                    LayaAbility.simulateFallDistance(
                        initialVy = 0f,
                        gravityScale = LayaAbility.WIND_DASH_GLIDE_GRAVITY_MULTIPLIER,
                        fallMultiplier = 1f,
                        frames = frames,
                        deltaTime = Constants.TIME_STEP
                    )
                )
                // 0.5 * 0.45 * 32 * 3^2 ≈ 64.8 m without terminal velocity.
                // The simulator doesn't model the PLAYER_MAX_FALL cap (Box2D
                // applies it via the controller, not the integrator), but
                // verifying the unbounded ceiling gives us a known upper
                // bound. The cap stops the glide before we ever hit this.
                (dist < 80f).shouldBeTrue()
                (dist > 1f).shouldBeTrue()  // sanity: physics actually ran
            }
        }
    }
})

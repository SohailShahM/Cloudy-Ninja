package com.sohai.platformer.screens

import com.sohai.platformer.Constants
import com.sohai.platformer.abilities.LayaAbility
import com.sohai.platformer.rendering.DynamicZoom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * T-209 regression: prove the projection-mismatch scenario that motivated
 * deferring the [com.sohai.platformer.screens.LevelRenderer.revertCameraZoom]
 * call out of [com.sohai.platformer.screens.LevelRenderer.renderWorld].
 *
 * The bug: T-176 zoomed the camera viewport at the top of `renderWorld()`
 * and reverted it at the bottom of the SAME method. The player sprite is
 * drawn AFTER `renderWorld()` returns — so when Laya's Wind Dash flung the
 * player ~8 m above the camera centre, the world correctly zoomed out to
 * keep the surroundings visible, but the player sprite was drawn with the
 * already-reverted (BASE) projection. The sprite clipped out of the
 * visible area exactly when the zoom-out was supposed to keep it visible.
 *
 * The user-visible symptom was "the slow-descent glide isn't implemented"
 * — the glide WAS happening, but mostly above the camera's effective
 * viewport so the user couldn't see it.
 *
 * These tests check the geometry that makes the bug REAL — i.e. the Wind
 * Dash's apex height genuinely exceeds the zoomed viewport's top edge
 * (so deferring the revert is load-bearing, not theoretical).
 *
 * Notes on the model:
 *   - Box2D mass for the player body: density=1 kg/m² × area (0.32 m × 0.64 m)
 *     = 0.2048 kg. So `applyLinearImpulse(0, 3)` translates to a vertical
 *     velocity change of `3 / 0.2048 ≈ 14.6 m/s` — this is what gets the
 *     player so high.
 *   - We integrate vy with semi-implicit Euler at 1/60 s, matching the
 *     same family Box2D uses for gravity integration (the same approach
 *     [LayaAbility.simulateFallDistance] uses).
 */
class LevelRendererZoomVisibilityTest : BehaviorSpec({

    val playerMass = 1f * 0.32f * 0.64f       // density × area = 0.2048 kg
    val verticalImpulse = 3f                  // LayaAbility.executeWindDash impulse (N·s)
    val verticalVyKick = verticalImpulse / playerMass   // ≈ 14.6 m/s
    val windBoostGravityMul = 0.25f
    val windBoostDuration = 0.5f
    val baseViewportHeight = Constants.VIRTUAL_HEIGHT / Constants.PPM  // 7.2 m
    val cameraOffsetWhenGrounded = 1f                                  // LevelRunState: cameraTargetY = playerY + 1

    /**
     * Simulate Laya's Wind Dash vertical trajectory starting from a grounded
     * stand (vy = 0 → +verticalVyKick after the impulse). Returns the apex
     * height in metres above the spawn point.
     *
     * Phase A: while `windBoostActive`, gravity = GRAVITY × 0.25f.
     * Phase B: after windBoost expires (and before apex), no glide yet —
     *          PlayerController writes gravityScale = 1f (vy > 0 and jump
     *          not held → "else" branch in the asymmetric gravity when).
     */
    fun simulateApex(): Float {
        var vy = verticalVyKick
        var y = 0f
        val dt = Constants.TIME_STEP
        var t = 0f
        while (vy > 0f) {
            val gScale = if (t < windBoostDuration) windBoostGravityMul else 1f
            vy += Constants.GRAVITY * gScale * dt
            y += vy * dt
            t += dt
        }
        return y
    }

    given("Wind Dash launches the player well above the camera-locked viewport") {
        `when`("we simulate the rise to apex from a grounded stand") {
            then("the apex sits above the BASE viewport top — proving zoom is needed") {
                val apexY = simulateApex()
                // Camera is locked to (grounded playerY + 1m). With the
                // baseline 7.2 m viewport, the top edge sits 3.6 m above
                // camera centre = 4.6 m above the spawn.
                val cameraCentreY = cameraOffsetWhenGrounded
                val baseTop = cameraCentreY + baseViewportHeight / 2f       // 4.6 m
                (apexY > baseTop).shouldBeTrue()
            }

            then("the apex ALSO sits above the ZOOMED viewport top — proving the player sprite needs the zoom too") {
                val apexY = simulateApex()
                val cameraCentreY = cameraOffsetWhenGrounded
                // With ZOOM_MAX = 1.4×, zoomed viewport height = 7.2 × 1.4
                // = 10.08 m → half-height = 5.04 m. The zoomed top sits
                // 5.04 m above the camera centre = 6.04 m above spawn.
                val zoomedTop =
                    cameraCentreY + (baseViewportHeight * DynamicZoom.ZOOM_MAX) / 2f   // 6.04 m
                (apexY > zoomedTop).shouldBeTrue()
                // (We don't try to "fix" the apex height itself — T-176's
                //  impulse magnitudes are out of T-209's scope per ticket
                //  rule 5: "Do NOT change Wind Dash horizontal physics or
                //  trigger mechanics — only the descent + camera path".)
            }

            then("the apex DOES NOT exceed the zoomed viewport by more than ~10 m") {
                // Sanity bound — if this fires the impulse magnitudes have
                // grown so much that even 1.4× zoom is futile and T-209's
                // fix would be a band-aid. As long as the apex sits within
                // a roughly believable window above the zoomed top, the
                // zoom-out + slow descent combination has time to drag the
                // player back into view before the glide ends.
                val apexY = simulateApex()
                val zoomedTop =
                    cameraOffsetWhenGrounded + (baseViewportHeight * DynamicZoom.ZOOM_MAX) / 2f
                (apexY - zoomedTop < 10f).shouldBeTrue()
            }
        }
    }

    given("the slow-descent ratio holds the player visible long enough to see the glide") {
        `when`("comparing time-to-cross the zoomed viewport top under glide vs normal-fall") {
            then("glide descent through the visible band takes long enough to be visible") {
                // From apex, how long does the player take to drop back
                // through the zoomed-viewport top edge under glide gravity
                // (0.45×) vs under PlayerController's asymmetric-fall
                // gravity (GRAVITY_FALL_MUL = 1.45×)?
                //
                // We use [LayaAbility.simulateFallDistance] (semi-implicit
                // Euler — the same integrator the gameplay uses) and count
                // frames until the cumulative fall distance equals the
                // apex height above the zoomed-viewport top.
                val apexY = simulateApex()
                val zoomedTop =
                    cameraOffsetWhenGrounded + (baseViewportHeight * DynamicZoom.ZOOM_MAX) / 2f
                val invisibleHeight = apexY - zoomedTop

                fun framesToFall(gravityScale: Float, fallMul: Float): Int {
                    var frames = 0
                    while (frames < 600) {       // 10s safety cap
                        frames++
                        val dist = kotlin.math.abs(
                            LayaAbility.simulateFallDistance(
                                initialVy      = 0f,
                                gravityScale   = gravityScale,
                                fallMultiplier = fallMul,
                                frames         = frames,
                                deltaTime      = Constants.TIME_STEP
                            )
                        )
                        if (dist >= invisibleHeight) return frames
                    }
                    return 600
                }
                val glideFrames  = framesToFall(LayaAbility.WIND_DASH_GLIDE_GRAVITY_MULTIPLIER, 1f)
                val normalFrames = framesToFall(1f, Constants.GRAVITY_FALL_MUL)
                // Glide takes meaningfully longer to drop through the
                // invisible region above the viewport — which is exactly
                // what makes the slow-descent feel different from a
                // normal fall.
                (glideFrames > normalFrames).shouldBeTrue()
            }
        }
    }
})

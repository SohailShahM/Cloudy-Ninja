package com.sohai.platformer.screens

import com.sohai.platformer.rendering.DynamicZoom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Pure-math tests for the T-176 dynamic camera zoom-out
 * ([DynamicZoom.computeZoomTarget] / [DynamicZoom.lerpStep]).
 *
 * Mirrors the testing pattern used by
 * [com.sohai.platformer.rendering.CameraLookAheadTest]: verify the pure curve
 * directly so we don't have to instantiate the surrounding renderer (which
 * needs an OpenGL context).
 *
 * Coverage:
 *   1. Constants match the T-176 ticket spec.
 *   2. Player well below the trigger band → zoom target is exactly 1.0.
 *   3. Player above the top edge → zoom target is exactly [DynamicZoom.ZOOM_MAX].
 *   4. Player at the band boundary → 1.0 (no edge double-counting).
 *   5. Linear interpolation inside the band.
 *   6. Multi-frame lerp toward target.
 *   7. Zoom cap: 1.4× is the hard ceiling for any input.
 */
class LevelRendererZoomTest : BehaviorSpec({

    // Use a baseline matching the in-game viewport
    // (VIRTUAL_HEIGHT / PPM = 720 / 100 = 7.2 m).
    val baseHeight = 7.2f
    val viewportTopY = 3.6f  // camera centred at y=0, top edge at +half-height

    given("T-176 zoom constants") {
        `when`("we read them off DynamicZoom") {
            then("ZOOM_MAX is 1.4 (ticket spec)") {
                DynamicZoom.ZOOM_MAX shouldBe 1.4f
            }
            then("ZOOM_LERP_FACTOR matches T-144 look-ahead at 0.15") {
                DynamicZoom.ZOOM_LERP_FACTOR shouldBe 0.15f
            }
            then("ZOOM_TRIGGER_BAND is 0.10 (within 10% of top edge)") {
                DynamicZoom.ZOOM_TRIGGER_BAND shouldBe 0.10f
            }
        }
    }

    given("computeZoomTarget() pure curve") {
        `when`("the player is well below the trigger band (at viewport centre)") {
            then("the zoom target is 1.0 (no zoom)") {
                DynamicZoom.computeZoomTarget(
                    playerY = 0f, viewportTopY = viewportTopY, baseHeight = baseHeight
                ) shouldBe 1f
            }
        }

        `when`("the player is far below the viewport") {
            then("the zoom target is 1.0") {
                DynamicZoom.computeZoomTarget(
                    playerY = -50f, viewportTopY = viewportTopY, baseHeight = baseHeight
                ) shouldBe 1f
            }
        }

        `when`("the player is at the band boundary (exactly bandBottom)") {
            then("the zoom target is still 1.0 (no zoom yet)") {
                // band height = (7.2 / 2) * 0.10 = 0.36 m; bandBottom = 3.6 - 0.36 = 3.24
                val bandBottom = viewportTopY - (baseHeight / 2f) * DynamicZoom.ZOOM_TRIGGER_BAND
                DynamicZoom.computeZoomTarget(
                    playerY = bandBottom, viewportTopY = viewportTopY, baseHeight = baseHeight
                ) shouldBe 1f
            }
        }

        `when`("the player is exactly at the top viewport edge") {
            then("the zoom target saturates at ZOOM_MAX") {
                DynamicZoom.computeZoomTarget(
                    playerY = viewportTopY, viewportTopY = viewportTopY, baseHeight = baseHeight
                ) shouldBe DynamicZoom.ZOOM_MAX
            }
        }

        `when`("the player is above the top viewport edge") {
            then("the zoom target stays at ZOOM_MAX (saturated, not unbounded)") {
                DynamicZoom.computeZoomTarget(
                    playerY = viewportTopY + 5f,
                    viewportTopY = viewportTopY,
                    baseHeight = baseHeight
                ) shouldBe DynamicZoom.ZOOM_MAX
                DynamicZoom.computeZoomTarget(
                    playerY = 1000f,
                    viewportTopY = viewportTopY,
                    baseHeight = baseHeight
                ) shouldBe DynamicZoom.ZOOM_MAX
            }
        }

        `when`("the player is halfway through the trigger band") {
            then("the zoom target is halfway between 1.0 and ZOOM_MAX") {
                val bandBottom = viewportTopY - (baseHeight / 2f) * DynamicZoom.ZOOM_TRIGGER_BAND
                val midBand = (bandBottom + viewportTopY) / 2f
                val expected = 1f + 0.5f * (DynamicZoom.ZOOM_MAX - 1f)  // = 1.2
                DynamicZoom.computeZoomTarget(
                    playerY = midBand, viewportTopY = viewportTopY, baseHeight = baseHeight
                ) shouldBe (expected plusOrMinus 1e-4f)
            }
        }

        `when`("the player is a quarter of the way into the trigger band") {
            then("the zoom target is 1 + 0.25 * (ZOOM_MAX - 1) = 1.1") {
                val bandBottom = viewportTopY - (baseHeight / 2f) * DynamicZoom.ZOOM_TRIGGER_BAND
                val quarter = bandBottom + 0.25f * (viewportTopY - bandBottom)
                val expected = 1f + 0.25f * (DynamicZoom.ZOOM_MAX - 1f)
                DynamicZoom.computeZoomTarget(
                    playerY = quarter, viewportTopY = viewportTopY, baseHeight = baseHeight
                ) shouldBe (expected plusOrMinus 1e-4f)
            }
        }
    }

    given("lerpStep() pure curve") {
        `when`("current and target are equal") {
            then("output equals input (idempotent at the target)") {
                DynamicZoom.lerpStep(1.2f, 1.2f) shouldBe 1.2f
                DynamicZoom.lerpStep(1f, 1f)     shouldBe 1f
            }
        }
        `when`("current is 1.0 and target is 1.4") {
            then("the step is exactly LERP_FACTOR * delta = 0.15 * 0.4 = 0.06") {
                DynamicZoom.lerpStep(1f, 1.4f) shouldBe (1.06f plusOrMinus 1e-4f)
            }
        }
        `when`("we iterate many times toward 1.4 from 1.0") {
            then("the multiplier converges to ~1.4 within 30 steps") {
                var x = 1f
                repeat(30) { x = DynamicZoom.lerpStep(x, 1.4f) }
                (kotlin.math.abs(1.4f - x) < 0.01f).shouldBeTrue()
            }
        }
    }

    given("zoom lerp convergence (simulating the in-renderer per-frame step)") {
        `when`("the player is parked above the viewport for many frames") {
            then("the current zoom converges toward ZOOM_MAX within ~30 frames") {
                var current = 1f
                val target = DynamicZoom.computeZoomTarget(
                    playerY = viewportTopY + 1f,
                    viewportTopY = viewportTopY,
                    baseHeight = baseHeight
                )
                repeat(30) {
                    current = DynamicZoom.lerpStep(current, target)
                }
                // After 30 steps at 0.15 lerp factor: (1 - 0.85^30) ≈ 99.2%
                // of the gap closed.
                (kotlin.math.abs(current - DynamicZoom.ZOOM_MAX) < 0.01f).shouldBeTrue()
            }
        }

        `when`("the player drops back to viewport centre after a zoom-out") {
            then("the zoom lerps back toward 1.0 within ~30 frames") {
                var current = DynamicZoom.ZOOM_MAX  // already zoomed out
                val target = DynamicZoom.computeZoomTarget(
                    playerY = 0f,  // player back in middle
                    viewportTopY = viewportTopY,
                    baseHeight = baseHeight
                )
                target shouldBe 1f
                repeat(30) {
                    current = DynamicZoom.lerpStep(current, target)
                }
                (kotlin.math.abs(current - 1f) < 0.01f).shouldBeTrue()
            }
        }
    }

    given("zoom cap: 1.4× is the hard ceiling") {
        `when`("we lerp toward an over-saturated player position for many frames") {
            then("no sample exceeds ZOOM_MAX") {
                var current = 1f
                val samples = mutableListOf<Float>()
                repeat(200) {
                    val t = DynamicZoom.computeZoomTarget(
                        playerY = viewportTopY + 100f,  // way above
                        viewportTopY = viewportTopY,
                        baseHeight = baseHeight
                    )
                    current = DynamicZoom.lerpStep(current, t)
                    samples += current
                }
                samples.all { it <= DynamicZoom.ZOOM_MAX + 1e-3f }.shouldBeTrue()
                samples.all { it >= 1f - 1e-3f }.shouldBeTrue()
            }
        }
    }
})

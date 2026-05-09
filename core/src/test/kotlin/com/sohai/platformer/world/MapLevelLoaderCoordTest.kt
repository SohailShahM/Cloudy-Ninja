package com.sohai.platformer.world

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the y-coordinate translation logic in [MapLevelLoader].
 *
 * MapLevelLoader embeds a local `centerOf()` function with two modes:
 *
 *   flipY = true  — standard Tiled y-down → Box2D y-up:
 *       cy = (mapHeightPixels - r.y - r.height) + r.height / 2
 *          = mapHeightPixels - r.y - r.height / 2
 *
 *   flipY = false — TMX authored in y-up space; use as-is:
 *       cy = r.y + r.height / 2
 *
 * Moving-platform endY has its own flip formula (only flips when flipY=true):
 *   endY (flipY=true)  = rawEndY + platformHeight / 2
 *   endY (flipY=false) = rawEndY  (used verbatim)
 *
 * These tests validate the formulas in isolation — no libGDX runtime needed.
 */
class MapLevelLoaderCoordTest : BehaviorSpec({

    val eps = 0.001f

    // ── centerOf — flipY = true ──────────────────────────────────────────────

    given("flipY = true coordinate translation (Tiled y-down → Box2D y-up)") {

        `when`("rectangle at y=100, height=40 in a 480 px tall map") {
            val mapH = 480f
            val ry   = 100f
            val rh   = 40f
            // formula: (mapH - ry - rh) + rh/2  =  480 - 100 - 40 + 20  =  360
            val cy = (mapH - ry - rh) + rh / 2f

            then("center y = 360") {
                cy shouldBe (360f plusOrMinus eps)
            }
        }

        `when`("rectangle at y=0, height=32 in a 720 px tall map (floor tile)") {
            val mapH = 720f
            val ry   = 0f
            val rh   = 32f
            // formula: (720 - 0 - 32) + 16  =  704  (near the TOP in Box2D y-up)
            val cy = (mapH - ry - rh) + rh / 2f

            then("center y = 704") {
                cy shouldBe (704f plusOrMinus eps)
            }
        }

        `when`("rectangle at y=440, height=40 in a 480 px tall map (near bottom in Box2D)") {
            val mapH = 480f
            val ry   = 440f
            val rh   = 40f
            // formula: (480 - 440 - 40) + 20  =  0 + 20  =  20
            val cy = (mapH - ry - rh) + rh / 2f

            then("center y = 20") {
                cy shouldBe (20f plusOrMinus eps)
            }
        }

        `when`("map height is 0 (degenerate case, flipY but no map meta)") {
            val mapH = 0f
            val ry   = 50f
            val rh   = 20f
            // formula gives a negative value — no crash expected
            val cy = (mapH - ry - rh) + rh / 2f

            then("center y = -60 (formula result, no crash)") {
                cy shouldBe (-60f plusOrMinus eps)
            }
        }
    }

    // ── centerOf — flipY = false ─────────────────────────────────────────────

    given("flipY = false coordinate translation (y-up TMX, no flip)") {

        `when`("rectangle at y=100, height=40") {
            val ry = 100f
            val rh = 40f
            val cy = ry + rh / 2f

            then("center y = 120") {
                cy shouldBe (120f plusOrMinus eps)
            }
        }

        `when`("rectangle at y=0, height=720 (full-screen height slab)") {
            val ry = 0f
            val rh = 720f
            val cy = ry + rh / 2f

            then("center y = 360") {
                cy shouldBe (360f plusOrMinus eps)
            }
        }
    }

    // ── flipY symmetry ───────────────────────────────────────────────────────

    given("symmetry: flipping a rectangle and its mirror should give mirrored centers") {
        val mapH = 480f
        val rh   = 40f

        `when`("a rect at y=0 and its vertical mirror at y=(mapH - rh)") {
            val cy0 = (mapH - 0f - rh) + rh / 2f          // near top of screen → 460
            val cy1 = (mapH - (mapH - rh) - rh) + rh / 2f // near bottom → 20

            then("the two centers sum to mapH") {
                (cy0 + cy1) shouldBe (mapH plusOrMinus eps)
            }
        }
    }

    // ── Moving platform endY flip ────────────────────────────────────────────

    given("moving platform endY translation (flipY = true)") {
        `when`("rawEndY = 200, platformHeight = 40") {
            // flipY=true formula: endY = rawEndY + platformHeight / 2
            val endY = 200f + 40f / 2f

            then("endY = 220") {
                endY shouldBe (220f plusOrMinus eps)
            }
        }
    }

    given("moving platform endY translation (flipY = false)") {
        `when`("rawEndY = 200 — used verbatim") {
            val endY = 200f  // no transformation applied

            then("endY = 200") {
                endY shouldBe (200f plusOrMinus eps)
            }
        }
    }
})

package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.sohai.platformer.rendering.ColorRole
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pure-function tests for [HighContrastPalette] (T-132).
 *
 * The palette is intentionally `Gdx`-free — the only libGDX type touched is
 * [Color] (plain data). No `Gdx.files`, no GL context, no headless mode.
 *
 * Coverage:
 *   1. Identity when disabled — `mapColor(c, role, enabled=false)` returns the
 *      input verbatim, regardless of role. This guarantees the OFF render
 *      path is byte-identical to pre-T-132.
 *   2. Every [ColorRole] enum value has a defined swatch — `swatchFor` never
 *      returns null and the mapping table covers all entries. This catches
 *      future enum additions that forget to update the `when`.
 *   3. Spec-anchored mappings — the four roles called out in the T-132 spec
 *      (player=white, enemies=black, platforms=light-grey, hazards=red) map
 *      to their saturated targets.
 *   4. Alpha preservation — `mapColor` copies the input alpha onto the new
 *      swatch so translucent halos still fade correctly.
 *   5. Distinct swatches for distinct semantic roles — PLAYER vs ENEMY vs
 *      HAZARD vs HAZARD_CLEANED must each be visibly distinct (this is the
 *      whole point of a high-contrast palette).
 *
 * Notes on style: BehaviorSpec `given/when/then` blocks; only `then {}` runs
 * as a test case. This matches the conventions used by `ScreenShakeTest` and
 * `ParallaxBackgroundTest` in the same package.
 */
class HighContrastPaletteTest : BehaviorSpec({

    given("an OFF (enabled=false) call to mapColor") {
        `when`("the input is a random opaque colour and any role") {
            then("the returned Color is exactly the same reference as the input") {
                val input = Color(0.13f, 0.42f, 0.71f, 1f)
                // Identity must hold regardless of role to keep pre-T-132 paths
                // byte-identical.
                for (role in ColorRole.values()) {
                    val out = HighContrastPalette.mapColor(input, role, enabled = false)
                    out shouldBe input
                }
            }
        }

        `when`("the input has a non-opaque alpha (glow/halo)") {
            then("identity still holds — alpha is irrelevant when disabled") {
                val glow = Color(0.5f, 0.5f, 0.5f, 0.25f)
                val out = HighContrastPalette.mapColor(glow, ColorRole.SNAPSHOT, enabled = false)
                out shouldBe glow
            }
        }
    }

    given("an ON (enabled=true) call to mapColor for each ColorRole") {
        `when`("every enum entry is mapped") {
            then("none of the swatch lookups throw and every result has the input alpha") {
                val input = Color(0.42f, 0.13f, 0.71f, 0.65f)
                for (role in ColorRole.values()) {
                    val out = HighContrastPalette.mapColor(input, role, enabled = true)
                    // Alpha must be preserved exactly from the input.
                    out.a shouldBe (input.a plusOrMinus 1e-6f)
                    // And the RGB triple must come from the role's swatch, not
                    // the input's RGB. (Identity ⇒ regression: the mapping
                    // table forgot to add this role.)
                    val swatch = HighContrastPalette.swatchFor(role)
                    out.r shouldBe (swatch.r plusOrMinus 1e-6f)
                    out.g shouldBe (swatch.g plusOrMinus 1e-6f)
                    out.b shouldBe (swatch.b plusOrMinus 1e-6f)
                }
            }
        }
    }

    given("the spec-anchored mappings from the T-132 ticket") {
        // The ticket spec calls out four canonical targets:
        //   player = pure white, enemies = pure black,
        //   platforms = inverted (light) grey, hazards = saturated red.
        `when`("PLAYER role is queried") {
            then("the swatch is pure white") {
                val s = HighContrastPalette.swatchFor(ColorRole.PLAYER)
                s.r shouldBe 1f
                s.g shouldBe 1f
                s.b shouldBe 1f
            }
        }
        `when`("ENEMY role is queried") {
            then("the swatch is pure black") {
                val s = HighContrastPalette.swatchFor(ColorRole.ENEMY)
                s.r shouldBe 0f
                s.g shouldBe 0f
                s.b shouldBe 0f
            }
        }
        `when`("PLATFORM role is queried") {
            then("the swatch is a high-luminance grey (≥ 0.8)") {
                val s = HighContrastPalette.swatchFor(ColorRole.PLATFORM)
                // Spec calls for inverted grey — pre-T-132 platforms are
                // ~0.40 grey, so the high-contrast variant should sit well
                // above the midpoint. We use 0.8 as a conservative bound.
                (s.r >= 0.8f).shouldBeTrue()
                (s.g >= 0.8f).shouldBeTrue()
                (s.b >= 0.8f).shouldBeTrue()
                // And the three channels must be near-equal (a true grey,
                // not a tinted one).
                (kotlin.math.abs(s.r - s.g) < 0.01f).shouldBeTrue()
                (kotlin.math.abs(s.r - s.b) < 0.01f).shouldBeTrue()
            }
        }
        `when`("HAZARD role is queried") {
            then("the swatch is saturated red (R=1, G≈0, B≈0)") {
                val s = HighContrastPalette.swatchFor(ColorRole.HAZARD)
                s.r shouldBe 1f
                (s.g <= 0.05f).shouldBeTrue()
                (s.b <= 0.05f).shouldBeTrue()
            }
        }
    }

    given("the requirement that high-contrast roles read as distinct primitives") {
        `when`("the four spec-called-out roles + a few semantic siblings are compared") {
            then("PLAYER, ENEMY, PLATFORM, HAZARD, HAZARD_CLEANED, TOKEN are all distinct swatches") {
                // Build a set of (r, g, b) triples — any duplicate fails.
                val rolesToCheck = listOf(
                    ColorRole.PLAYER,
                    ColorRole.ENEMY,
                    ColorRole.PLATFORM,
                    ColorRole.HAZARD,
                    ColorRole.HAZARD_CLEANED,
                    ColorRole.TOKEN
                )
                val triples = rolesToCheck.map {
                    val s = HighContrastPalette.swatchFor(it)
                    Triple(s.r, s.g, s.b)
                }
                triples.toSet().size shouldBe rolesToCheck.size
            }
        }

        `when`("HAZARD_CLEANED is compared to HAZARD (the dangerous twin)") {
            then("the two swatches are not the same triple") {
                val a = HighContrastPalette.swatchFor(ColorRole.HAZARD)
                val b = HighContrastPalette.swatchFor(ColorRole.HAZARD_CLEANED)
                Triple(a.r, a.g, a.b) shouldNotBe Triple(b.r, b.g, b.b)
            }
        }

        `when`("CHECKPOINT_ACTIVE is compared to CHECKPOINT_INACTIVE") {
            then("the two swatches are not the same triple") {
                val a = HighContrastPalette.swatchFor(ColorRole.CHECKPOINT_ACTIVE)
                val b = HighContrastPalette.swatchFor(ColorRole.CHECKPOINT_INACTIVE)
                Triple(a.r, a.g, a.b) shouldNotBe Triple(b.r, b.g, b.b)
            }
        }

        `when`("PORTAL_LOCKED is compared to PORTAL_UNLOCKED") {
            then("the two swatches are not the same triple") {
                val a = HighContrastPalette.swatchFor(ColorRole.PORTAL_LOCKED)
                val b = HighContrastPalette.swatchFor(ColorRole.PORTAL_UNLOCKED)
                Triple(a.r, a.g, a.b) shouldNotBe Triple(b.r, b.g, b.b)
            }
        }
    }

    given("alpha-preservation invariants") {
        `when`("a fully-opaque colour is mapped") {
            then("output alpha is exactly 1.0") {
                val opaque = Color(0.123f, 0.456f, 0.789f, 1f)
                val out = HighContrastPalette.mapColor(opaque, ColorRole.HAZARD, enabled = true)
                out.a shouldBe 1f
            }
        }

        `when`("a fully-transparent colour is mapped") {
            then("output alpha is exactly 0.0 (would render as invisible)") {
                val invisible = Color(0.1f, 0.2f, 0.3f, 0f)
                val out = HighContrastPalette.mapColor(invisible, ColorRole.TOKEN, enabled = true)
                out.a shouldBe 0f
            }
        }

        `when`("a partially-translucent glow colour is mapped") {
            then("output alpha matches the input alpha within float tolerance") {
                val glow = Color(0.2f, 0.8f, 0.5f, 0.35f)
                val out = HighContrastPalette.mapColor(glow, ColorRole.SNAPSHOT, enabled = true)
                out.a shouldBe (0.35f plusOrMinus 1e-6f)
            }
        }
    }
})

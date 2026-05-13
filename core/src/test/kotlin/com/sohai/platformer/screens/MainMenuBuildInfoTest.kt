package com.sohai.platformer.screens

import com.badlogic.gdx.graphics.Color
import com.sohai.platformer.Constants
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import sun.misc.Unsafe

/**
 * T-100: Game version + build info label on MainMenu.
 *
 * `MainMenuScreen`'s real constructor builds a libGDX `Stage`, which would
 * crash in a JVM-only test (no GL context). Following the established pattern
 * ([MainMenuAchievementProgressTest], [CreditsScreenTest], [ScreenFadeTest]),
 * we allocate a bare instance via `sun.misc.Unsafe.allocateInstance` for the
 * one structural assertion, but the bulk of the test exercises the pure
 * helpers on the companion object so no GL dependencies are pulled in.
 *
 * Coverage:
 *
 *  1. `MENU_BUILD_INFO` template — non-blank, contains `{0}` + `{1}`, renders
 *     to the canonical `v0.1.0 · 2026-05-12` form when fed Constants.
 *  2. `buildInfoText` companion helper formats version + date via i18n.
 *  3. `buildInfoColor` returns the exact dim-grey RGBA tuple the ticket
 *     specifies — `(0.5, 0.5, 0.5, 0.6)`.
 *  4. `buildInfoLabelPosition` anchors the label 8px from the bottom-right
 *     corner across a representative range of stage widths.
 *  5. `BUILD_INFO_PADDING` is exactly 8px (ticket constraint).
 *  6. Bare `MainMenuScreen` allocation works (no GL).
 */
class MainMenuBuildInfoTest : BehaviorSpec({

    val unsafe: Unsafe = run {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as Unsafe
    }

    /** Allocate a MainMenuScreen without running its (GL-requiring) constructor. */
    fun allocBare(): MainMenuScreen {
        @Suppress("UsePropertyAccessSyntax")
        return unsafe.allocateInstance(MainMenuScreen::class.java) as MainMenuScreen
    }

    // ── 1. MENU_BUILD_INFO i18n template ──────────────────────────────────────

    given("the T-100 i18n key MENU_BUILD_INFO") {
        `when`("it is looked up") {
            val template = Strings.get(StringKey.MENU_BUILD_INFO)
            then("the template is non-blank and includes both {0} and {1}") {
                template.shouldNotBeBlank()
                template shouldContain "{0}"
                template shouldContain "{1}"
            }
        }
        `when`("formatted with the live Constants values") {
            val rendered = Strings.format(
                StringKey.MENU_BUILD_INFO,
                Constants.BUILD_VERSION,
                Constants.BUILD_DATE
            )
            then("the rendered text starts with 'v' (no separate prefix arg in template)") {
                rendered shouldContain "v${Constants.BUILD_VERSION}"
            }
            then("the rendered text includes both the version and the date") {
                rendered shouldContain Constants.BUILD_VERSION
                rendered shouldContain Constants.BUILD_DATE
            }
            then("the rendered text matches the canonical alpha form 'v0.1.0 · 2026-05-12'") {
                // Pinned: the ticket explicitly lists `v0.1.0 · 2026-05-12`
                // as the alpha pre-launch value. If a future ticket bumps
                // either, edit Constants.kt — this assertion guarantees the
                // current alpha output.
                rendered shouldBe "v0.1.0 · 2026-05-12"
            }
        }
    }

    // ── 2. buildInfoText helper ──────────────────────────────────────────────

    given("the buildInfoText pure helper") {
        `when`("called with arbitrary version + date") {
            then("it formats via MENU_BUILD_INFO with both args substituted") {
                MainMenuScreen.buildInfoText("9.8.7", "2099-12-31") shouldBe
                    Strings.format(StringKey.MENU_BUILD_INFO, "9.8.7", "2099-12-31")
            }
        }
        `when`("called with the live Constants values") {
            then("it matches Strings.format(MENU_BUILD_INFO, …)") {
                MainMenuScreen.buildInfoText(Constants.BUILD_VERSION, Constants.BUILD_DATE) shouldBe
                    Strings.format(StringKey.MENU_BUILD_INFO, Constants.BUILD_VERSION, Constants.BUILD_DATE)
            }
        }
    }

    // ── 3. buildInfoColor helper ─────────────────────────────────────────────

    given("the buildInfoColor pure helper") {
        `when`("called") {
            val color = MainMenuScreen.buildInfoColor()
            then("the color is dim grey (0.5, 0.5, 0.5, 0.6) per ticket spec") {
                color shouldBe Color(0.5f, 0.5f, 0.5f, 0.6f)
            }
        }
    }

    // ── 4. buildInfoLabelPosition helper ─────────────────────────────────────

    given("the buildInfoLabelPosition pure helper") {
        `when`("the stage is 1920px wide and the label measures 120px") {
            val (x, y) = MainMenuScreen.buildInfoLabelPosition(1920f, 120f)
            then("x anchors the label right-edge 8px from the stage right edge") {
                // x is the LEFT edge → right edge = x + labelWidth = stage - 8
                (x + 120f) shouldBe (1920f - MainMenuScreen.BUILD_INFO_PADDING)
            }
            then("y is exactly 8px (the bottom padding)") {
                y shouldBe MainMenuScreen.BUILD_INFO_PADDING
            }
        }
        `when`("the stage is 1280px wide and the label measures 100px") {
            val (x, y) = MainMenuScreen.buildInfoLabelPosition(1280f, 100f)
            then("x positions correctly for the smaller stage") {
                x shouldBe (1280f - 100f - 8f)
                y shouldBe 8f
            }
        }
        `when`("the stage is 3840px wide (4K) and the label measures 140px") {
            val (x, y) = MainMenuScreen.buildInfoLabelPosition(3840f, 140f)
            then("the corner anchor still resolves to stage_w - label_w - 8") {
                x shouldBe (3840f - 140f - 8f)
                y shouldBe 8f
            }
        }
    }

    // ── 5. BUILD_INFO_PADDING constant ───────────────────────────────────────

    given("BUILD_INFO_PADDING") {
        then("is exactly 8px per ticket spec") {
            MainMenuScreen.BUILD_INFO_PADDING shouldBe 8f
        }
    }

    // ── 6. Constants pinned for alpha ────────────────────────────────────────

    given("the alpha pre-launch build constants") {
        then("BUILD_VERSION is 0.1.0 (manually maintained — bump for each cut release)") {
            Constants.BUILD_VERSION shouldBe "0.1.0"
        }
        then("BUILD_DATE is 2026-05-12 (matches the alpha-cut spec)") {
            Constants.BUILD_DATE shouldBe "2026-05-12"
        }
    }

    // ── 7. Bare-instance allocation works (no GL needed) ─────────────────────

    given("the MainMenuScreen class") {
        `when`("an instance is allocated via Unsafe (no constructor)") {
            val screen: Any = allocBare()
            then("the instance is non-null and of the right type") {
                (screen is MainMenuScreen) shouldBe true
            }
        }
    }
})

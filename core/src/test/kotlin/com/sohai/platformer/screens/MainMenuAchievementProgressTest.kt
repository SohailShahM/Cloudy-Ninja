package com.sohai.platformer.screens

import com.badlogic.gdx.graphics.Color
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.progression.AchievementRegistry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import sun.misc.Unsafe

/**
 * T-099: Achievement progress counter on MainMenu — verifies the three
 * branches of the new label:
 *
 *  1. **0/12** — no save data → grey "Achievements: 0/12 unlocked".
 *  2. **5/12** — partial progress → grey "Achievements: 5/12 unlocked".
 *  3. **12/12** — all unlocked → gold "Achievements: All 12 unlocked!" branch
 *     using `MENU_ACHIEVEMENT_PROGRESS_COMPLETE`.
 *
 * `MainMenuScreen`'s constructor builds a libGDX `Stage`, which would crash
 * in a JVM-only test (no GL context). Following the established pattern
 * ([CreditsScreenTest], [ScreenFadeTest]), we allocate a bare instance via
 * `sun.misc.Unsafe.allocateInstance` to assert structural facts when needed,
 * but the bulk of the test exercises the pure helpers on the companion
 * object so no GL dependencies are pulled in.
 */
class MainMenuAchievementProgressTest : BehaviorSpec({

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

    val total = AchievementRegistry.ALL.size

    // ── 1. New StringKeys resolve ────────────────────────────────────────────

    given("the T-099 i18n keys") {
        `when`("MENU_ACHIEVEMENT_PROGRESS is looked up") {
            then("the template includes both {0} (count) and {1} (total) placeholders") {
                val template = Strings.get(StringKey.MENU_ACHIEVEMENT_PROGRESS)
                template.shouldNotBeBlank()
                template shouldContain "{0}"
                template shouldContain "{1}"
            }
        }
        `when`("MENU_ACHIEVEMENT_PROGRESS_COMPLETE is looked up") {
            then("the template includes the {0} (total) placeholder and is non-blank") {
                val template = Strings.get(StringKey.MENU_ACHIEVEMENT_PROGRESS_COMPLETE)
                template.shouldNotBeBlank()
                template shouldContain "{0}"
            }
        }
    }

    // ── 2. Branch coverage for achievementProgressTextAndColor ───────────────

    given("an empty profile with 0 unlocked achievements") {
        `when`("achievementProgressTextAndColor(0, total) is called") {
            val (text, color) = MainMenuScreen.achievementProgressTextAndColor(0, total)
            then("the rendered text matches the partial-progress template (uses MENU_ACHIEVEMENT_PROGRESS)") {
                text shouldBe Strings.format(StringKey.MENU_ACHIEVEMENT_PROGRESS, 0, total)
                text shouldContain "0/$total"
            }
            then("the color is light grey (0.75, 0.75, 0.75, 1)") {
                color shouldBe Color(0.75f, 0.75f, 0.75f, 1f)
            }
        }
    }

    given("a partially-progressed profile with 5 unlocked achievements") {
        `when`("achievementProgressTextAndColor(5, total) is called") {
            val (text, color) = MainMenuScreen.achievementProgressTextAndColor(5, total)
            then("the rendered text shows 5/total via MENU_ACHIEVEMENT_PROGRESS") {
                text shouldBe Strings.format(StringKey.MENU_ACHIEVEMENT_PROGRESS, 5, total)
                text shouldContain "5/$total"
            }
            then("the color is light grey (not gold)") {
                color shouldBe Color(0.75f, 0.75f, 0.75f, 1f)
            }
        }
    }

    given("a fully-completed profile with all achievements unlocked") {
        `when`("achievementProgressTextAndColor(total, total) is called") {
            val (text, color) = MainMenuScreen.achievementProgressTextAndColor(total, total)
            then("the rendered text uses the MENU_ACHIEVEMENT_PROGRESS_COMPLETE template") {
                text shouldBe Strings.format(StringKey.MENU_ACHIEVEMENT_PROGRESS_COMPLETE, total)
            }
            then("the color is gold (1f, 0.85f, 0.1f, 1f) per the ticket spec") {
                color shouldBe Color(1f, 0.85f, 0.1f, 1f)
            }
        }
    }

    given("an over-completed count (defensive: count > total)") {
        `when`("achievementProgressTextAndColor(total + 3, total) is called") {
            val (_, color) = MainMenuScreen.achievementProgressTextAndColor(total + 3, total)
            then("the gold branch still fires (count >= total)") {
                color shouldBe Color(1f, 0.85f, 0.1f, 1f)
            }
        }
    }

    given("a zero-total edge case (registry somehow empty)") {
        `when`("achievementProgressTextAndColor(0, 0) is called") {
            val (_, color) = MainMenuScreen.achievementProgressTextAndColor(0, 0)
            then("the partial-progress branch fires (avoids a gold 0/0 label)") {
                color shouldBe Color(0.75f, 0.75f, 0.75f, 1f)
            }
        }
    }

    // ── 3. Max-across-slots aggregation ──────────────────────────────────────

    given("the per-slot unlocked-count aggregator") {
        `when`("called with an empty list") {
            then("it returns 0") {
                MainMenuScreen.maxUnlockedAcrossSlotCounts(emptyList()) shouldBe 0
            }
        }
        `when`("called with three slots all 0") {
            then("it returns 0") {
                MainMenuScreen.maxUnlockedAcrossSlotCounts(listOf(0, 0, 0)) shouldBe 0
            }
        }
        `when`("called with slots [3, 7, 2]") {
            then("it returns the max (7)") {
                MainMenuScreen.maxUnlockedAcrossSlotCounts(listOf(3, 7, 2)) shouldBe 7
            }
        }
        `when`("called with all slots full") {
            then("it returns total") {
                MainMenuScreen.maxUnlockedAcrossSlotCounts(listOf(total, total, total)) shouldBe total
            }
        }
    }

    // ── 4. Registry size is the source of truth (not hardcoded 12) ───────────

    given("AchievementRegistry.ALL") {
        `when`("the achievement total is read") {
            then("the registry's size is what drives the label total") {
                // Sanity: ticket originally assumed 12, T-107 bumped to 13;
                // the label code already reads AchievementRegistry.ALL.size
                // dynamically (verified in MainMenuScreen.kt:251), so this
                // assertion just tracks the registry's current cardinality.
                AchievementRegistry.ALL.size shouldBe 13
            }
        }
    }

    // ── 5. Bare-instance allocation works (no GL needed) ─────────────────────

    given("the MainMenuScreen class") {
        `when`("an instance is allocated via Unsafe (no constructor)") {
            val screen: Any = allocBare()
            then("the instance is non-null and of the right type") {
                (screen is MainMenuScreen) shouldBe true
            }
        }
    }
})

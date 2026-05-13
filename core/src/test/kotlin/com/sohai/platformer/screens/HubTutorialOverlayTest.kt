package com.sohai.platformer.screens

import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.levels.Level0_0
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveMigrations
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * T-137: First-run hub tutorial overlay tests.
 *
 * The overlay's own `init`/`render` paths require a libGDX GL context (Stage,
 * ShapeRenderer, FontManager) so they're verified end-to-end by the smoke CI
 * autopilot — the same dispatch surface as PauseOverlay / CloudAtlasOverlay,
 * which also rely on smoke CI to catch GL-context regressions.
 *
 * This unit test pins the pure, JVM-only contracts that the production
 * codepath relies on:
 *
 *  1. The four new StringKeys resolve to non-blank English copy and the body
 *     of each hint references the actual control mentioned in the ticket
 *     (A/D + SPACE for movement, Q for swap, portal for entry). If a future
 *     i18n edit accidentally drops a control hint, this fires.
 *  2. [GameState.tutorialSeen] is a new additive field defaulting to `false`
 *     so legacy saves (which omit the field entirely) deserialize with the
 *     overlay armed — i.e. existing players will see the tutorial once on
 *     their next launch. Conscious trade-off, documented in the ticket PR.
 *  3. [Level0_0.shouldShowFirstRunTutorial] returns `true` for fresh saves
 *     and `false` once `tutorialSeen` flips to `true`. This is the gate the
 *     [GameScreen] init block reads to decide whether to construct the
 *     overlay at all — keeping subsequent hub entries free of any overlay
 *     allocation.
 *  4. Migrating a pre-T-137 save (no `tutorialSeen` field) through the
 *     T-113 migration scaffold produces a `GameState` with
 *     `tutorialSeen == false`, exercising the additive-field contract end
 *     to end (parse → migrate → decode → field default).
 */
class HubTutorialOverlayTest : BehaviorSpec({

    // ── 1. i18n keys resolve to copy that names the actual controls ─────────

    given("the T-137 tutorial i18n keys") {
        `when`("TUTORIAL_TITLE is looked up") {
            then("the title is non-blank") {
                Strings.get(StringKey.TUTORIAL_TITLE).shouldNotBeBlank()
            }
        }
        `when`("TUTORIAL_HINT_MOVE is looked up") {
            then("the hint names A/D and SPACE per the ticket spec") {
                val s = Strings.get(StringKey.TUTORIAL_HINT_MOVE)
                s shouldContain "A/D"
                s shouldContain "SPACE"
            }
        }
        `when`("TUTORIAL_HINT_SWAP is looked up") {
            then("the hint names the swap key (Q) per the ticket spec") {
                // Spec text: "Swap character with Q to use water-cycle abilities"
                // Q is the post-T-121 default; the literal text is what new
                // players see today, irrespective of any rebind. T-121 will
                // migrate the default keybind separately.
                val s = Strings.get(StringKey.TUTORIAL_HINT_SWAP)
                s shouldContain "Q"
                s shouldContain "Swap"
            }
        }
        `when`("TUTORIAL_HINT_PORTAL is looked up") {
            then("the hint references a portal") {
                Strings.get(StringKey.TUTORIAL_HINT_PORTAL) shouldContain "portal"
            }
        }
        `when`("TUTORIAL_DISMISS_HINT is looked up") {
            then("the dismissal copy is non-blank") {
                Strings.get(StringKey.TUTORIAL_DISMISS_HINT).shouldNotBeBlank()
            }
        }
    }

    // ── 2. GameState.tutorialSeen default contract ──────────────────────────

    given("a fresh, default-constructed GameState") {
        `when`("inspected") {
            then("tutorialSeen defaults to false — overlay armed for new saves") {
                GameState().tutorialSeen shouldBe false
            }
            then("saveFormatVersion is still CURRENT_VERSION — no schema bump") {
                // T-137 must NOT bump saveFormatVersion. The new field is
                // additive per the T-113 contract; kotlinx-serialization
                // supplies the default for missing keys at load time, so
                // legacy saves still decode unchanged.
                GameState().saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
        }
    }

    given("a GameState with tutorialSeen explicitly set") {
        `when`("copied with tutorialSeen=true") {
            val mutated = GameState().copy(tutorialSeen = true)
            then("the new instance carries the flag") {
                mutated.tutorialSeen shouldBe true
            }
            then("other fields round-trip unchanged") {
                mutated.level shouldBe GameState().level
                mutated.totalDeaths shouldBe GameState().totalDeaths
            }
        }
    }

    // ── 3. Level0_0.shouldShowFirstRunTutorial gate ──────────────────────────

    given("Level0_0.shouldShowFirstRunTutorial(state)") {
        `when`("called on a fresh, default save") {
            then("returns true — fresh saves see the tutorial") {
                Level0_0.shouldShowFirstRunTutorial(GameState()) shouldBe true
            }
        }
        `when`("called on a save where tutorialSeen=true") {
            then("returns false — subsequent hub entries skip the overlay") {
                Level0_0.shouldShowFirstRunTutorial(
                    GameState().copy(tutorialSeen = true)
                ) shouldBe false
            }
        }
    }

    // ── 4. Pre-T-137 saves migrate cleanly via the T-113 scaffold ──────────

    given("a legacy save written before tutorialSeen existed") {
        // This is the back-compat scenario for existing players (one-off CC
        // contributors who already have a save_slot_1.json on disk). The
        // migration chain must not throw, and the decoded state must read
        // tutorialSeen=false so they see the overlay exactly once on next
        // launch.
        val legacyJson = """
            {
              "saveFormatVersion": 1,
              "level": "level0_0",
              "characterName": "Ebo",
              "totalDeaths": 2
            }
        """.trimIndent()

        `when`("routed through the production migration chain") {
            val migrated = SaveMigrations.migrate(legacyJson)
            then("the load succeeds and the new field defaults to false") {
                migrated.tutorialSeen shouldBe false
            }
            then("untouched fields round-trip unchanged") {
                migrated.level shouldBe "level0_0"
                migrated.characterName shouldBe "Ebo"
                migrated.totalDeaths shouldBe 2
            }
            then("the migrated save remains at CURRENT_VERSION") {
                migrated.saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
        }
    }
})

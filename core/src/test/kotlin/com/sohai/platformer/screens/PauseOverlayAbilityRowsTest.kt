package com.sohai.platformer.screens

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * T-140: per-character ability tooltip in the pause overlay.
 *
 * Coverage:
 *  1. The three new StringKeys (`PAUSE_ABILITY_EBO/LAYA/ZEPHYR`) exist and
 *     contain `{0}` so the action-key substitutes cleanly.
 *  2. [PauseOverlay.actionKeyName] resolves a custom keybind to the libGDX
 *     `Input.Keys.toString` name, and falls back to "E" when the binding is
 *     missing.
 *  3. [PauseOverlay.abilityRowText] formats each of the three rows with a
 *     non-default keybind (covers the "keys reflect current bindings"
 *     "done when" clause for T-140).
 *  4. [PauseOverlay.abilityRowColor] returns [PauseOverlay.HIGHLIGHT_COLOR]
 *     for the matching character and [PauseOverlay.DIM_COLOR] for the others.
 *     This is the "current character is highlighted" assertion compared
 *     against the character field exposed by [LevelRunState.currentCharacter].
 *  5. [PauseOverlay.abilityRows] returns the rows in the canonical
 *     Ebo / Laya / Zephyr order with the highlight applied exactly once.
 *
 * No libGDX GL context required — all assertions are against the pure helpers
 * on [PauseOverlay.Companion]. Matches the existing test pattern established
 * by `MainMenuBuildInfoTest`.
 */
class PauseOverlayAbilityRowsTest : BehaviorSpec({

    // ── 1. StringKey templates ────────────────────────────────────────────────

    given("the T-140 pause-ability StringKey templates") {
        `when`("each template is looked up") {
            then("Ebo template starts with 'Ebo' and contains {0}") {
                val t = Strings.get(StringKey.PAUSE_ABILITY_EBO)
                t shouldContain "Ebo"
                t shouldContain "Seed Slam"
                t shouldContain "{0}"
            }
            then("Laya template starts with 'Laya' and contains {0}") {
                val t = Strings.get(StringKey.PAUSE_ABILITY_LAYA)
                t shouldContain "Laya"
                t shouldContain "Wind Dash"
                t shouldContain "{0}"
            }
            then("Zephyr template starts with 'Zephyr' and contains {0}") {
                val t = Strings.get(StringKey.PAUSE_ABILITY_ZEPHYR)
                t shouldContain "Zephyr"
                t shouldContain "Cloud Float"
                t shouldContain "{0}"
            }
        }
    }

    // ── 2. actionKeyName ──────────────────────────────────────────────────────

    given("actionKeyName") {
        `when`("the binding map has action → Input.Keys.F (a custom key)") {
            val name = PauseOverlay.actionKeyName(mapOf("action" to Input.Keys.F))
            then("the name resolves via Input.Keys.toString and is non-blank") {
                name shouldBe Input.Keys.toString(Input.Keys.F)
            }
        }
        `when`("the binding map omits the action entry") {
            val name = PauseOverlay.actionKeyName(emptyMap())
            then("the helper falls back to the default 'E' (Input.Keys.E label)") {
                name shouldBe Input.Keys.toString(Input.Keys.E)
            }
        }
        `when`("the binding map has action → Input.Keys.SPACE") {
            val name = PauseOverlay.actionKeyName(mapOf("action" to Input.Keys.SPACE))
            then("the name matches Input.Keys.toString(SPACE)") {
                name shouldBe Input.Keys.toString(Input.Keys.SPACE)
            }
        }
    }

    // ── 3. abilityRowText (keybind reflection) ────────────────────────────────

    given("abilityRowText with a custom keybind name 'F'") {
        val customKey = "F"
        `when`("formatting Ebo's row") {
            val text = PauseOverlay.abilityRowText("Ebo", customKey)
            then("the text contains 'Ebo', 'Seed Slam', and the custom key 'F'") {
                text shouldContain "Ebo"
                text shouldContain "Seed Slam"
                text shouldContain "F"
            }
        }
        `when`("formatting Laya's row") {
            val text = PauseOverlay.abilityRowText("Laya", customKey)
            then("the text contains 'Laya', 'Wind Dash', and the custom key 'F'") {
                text shouldContain "Laya"
                text shouldContain "Wind Dash"
                text shouldContain "F"
            }
        }
        `when`("formatting Zephyr's row") {
            val text = PauseOverlay.abilityRowText("Zephyr", customKey)
            then("the text contains 'Zephyr', 'Cloud Float', and the custom key 'F'") {
                text shouldContain "Zephyr"
                text shouldContain "Cloud Float"
                text shouldContain "F"
            }
        }
    }

    // ── 4. abilityRowColor (highlight comparison vs LevelRunState char) ──────

    given("abilityRowColor — current character is highlighted") {
        `when`("currentCharacter equals the row character (e.g. 'Laya')") {
            // The character-selection field on [LevelRunState] is a String;
            // the spec calls for comparison against that exact field, so we
            // assert the helper does a String-equality check yielding the
            // existing toast accent color.
            val color = PauseOverlay.abilityRowColor("Laya", currentCharacter = "Laya")
            then("the color is HIGHLIGHT_COLOR (achievement-toast gold)") {
                color shouldBe PauseOverlay.HIGHLIGHT_COLOR
            }
            then("HIGHLIGHT_COLOR matches the toast accent (1, 0.92, 0.3, 1)") {
                PauseOverlay.HIGHLIGHT_COLOR shouldBe Color(1f, 0.92f, 0.3f, 1f)
            }
        }
        `when`("currentCharacter differs from the row character") {
            val color = PauseOverlay.abilityRowColor("Ebo", currentCharacter = "Zephyr")
            then("the color is DIM_COLOR (dimmed non-selected row)") {
                color shouldBe PauseOverlay.DIM_COLOR
            }
        }
    }

    // ── 5. abilityRows — full card composition ────────────────────────────────

    given("abilityRows — full 3-row card") {
        `when`("currentCharacter is 'Zephyr' and the action key is 'E'") {
            val rows = PauseOverlay.abilityRows(currentCharacter = "Zephyr", keyName = "E")
            then("there are exactly 3 rows in Ebo / Laya / Zephyr order") {
                rows.map { it.character } shouldContainExactly listOf("Ebo", "Laya", "Zephyr")
            }
            then("only the Zephyr row uses HIGHLIGHT_COLOR; the others use DIM_COLOR") {
                rows.single { it.character == "Zephyr" }.color shouldBe PauseOverlay.HIGHLIGHT_COLOR
                rows.single { it.character == "Ebo"    }.color shouldBe PauseOverlay.DIM_COLOR
                rows.single { it.character == "Laya"   }.color shouldBe PauseOverlay.DIM_COLOR
            }
            then("each row's text contains the action-key name") {
                rows.forEach { it.text shouldContain "E" }
            }
        }
        `when`("currentCharacter is 'Ebo' (the LevelRunState default) and key is 'E'") {
            val rows = PauseOverlay.abilityRows(currentCharacter = "Ebo", keyName = "E")
            then("the Ebo row is highlighted exactly once") {
                rows.count { it.color == PauseOverlay.HIGHLIGHT_COLOR } shouldBe 1
                rows.first { it.color == PauseOverlay.HIGHLIGHT_COLOR }.character shouldBe "Ebo"
            }
        }
        `when`("currentCharacter is an unrecognised string") {
            // Defensive: spec says highlight the current character; if the
            // field somehow holds an unexpected value (shouldn't happen — the
            // game only sets Ebo/Laya/Zephyr) no row is highlighted.
            val rows = PauseOverlay.abilityRows(currentCharacter = "Unknown", keyName = "E")
            then("no row is highlighted (all DIM_COLOR)") {
                rows.count { it.color == PauseOverlay.HIGHLIGHT_COLOR } shouldBe 0
                rows.all { it.color == PauseOverlay.DIM_COLOR } shouldBe true
            }
        }
    }
})

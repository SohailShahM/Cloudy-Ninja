package com.sohai.platformer.screens

import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import sun.misc.Unsafe

/**
 * T-119: Save-slot delete confirmation modal — verifies the four new
 * `MENU_DELETE_SLOT_CONFIRM_*` i18n keys resolve, the title template
 * substitutes the 1-based slot number, the body warning text is non-blank,
 * and the [MainMenuScreen] companion exposes the structural constants the
 * production code uses to look up modal actors by name.
 *
 * `MainMenuScreen`'s constructor builds a libGDX `Stage`, which would crash
 * in a JVM-only test (no GL context). Following the established pattern
 * ([MainMenuAchievementProgressTest], [CreditsScreenTest]), we allocate a
 * bare instance via `sun.misc.Unsafe.allocateInstance` only to assert
 * structural facts; the bulk of the test exercises the pure helpers on the
 * companion object so no GL or VisUI skin dependency is pulled in.
 */
class MainMenuDeleteModalTest : BehaviorSpec({

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

    // ── 1. The four new StringKeys resolve to non-blank English text ─────────

    given("the T-119 delete-modal i18n keys") {
        `when`("MENU_DELETE_SLOT_CONFIRM_TITLE is looked up") {
            then("the template is non-blank and includes the {0} slot placeholder") {
                val template = Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_TITLE)
                template.shouldNotBeBlank()
                template shouldContain "{0}"
            }
        }
        `when`("MENU_DELETE_SLOT_CONFIRM_BODY is looked up") {
            then("the warning copy mentions the irreversibility of the action") {
                val body = Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_BODY)
                body.shouldNotBeBlank()
                // The whole point of the modal is to scare the player away
                // from an accidental tap — pin the "cannot be undone" wording
                // so future copy edits don't accidentally soften it.
                body shouldContain "undone"
            }
        }
        `when`("MENU_DELETE_SLOT_CONFIRM_DELETE is looked up") {
            then("the button label is non-blank") {
                Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_DELETE).shouldNotBeBlank()
            }
        }
        `when`("MENU_DELETE_SLOT_CONFIRM_CANCEL is looked up") {
            then("the button label is non-blank") {
                Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_CANCEL).shouldNotBeBlank()
            }
        }
    }

    // ── 2. deleteModalTitle substitutes the 1-based slot number ──────────────

    given("the deleteModalTitle helper") {
        `when`("called with slot 1") {
            val text = MainMenuScreen.deleteModalTitle(1)
            then("the rendered title contains '1' and no unresolved placeholders") {
                text shouldContain "1"
                // Unresolved {0} would be a regression — sanity check.
                (text.contains("{0}")) shouldBe false
            }
        }
        `when`("called with slot 2") {
            val text = MainMenuScreen.deleteModalTitle(2)
            then("the rendered title contains '2'") {
                text shouldContain "2"
            }
            then("the rendered text matches the formatted template") {
                text shouldBe Strings.format(StringKey.MENU_DELETE_SLOT_CONFIRM_TITLE, 2)
            }
        }
        `when`("called with slot 3") {
            val text = MainMenuScreen.deleteModalTitle(3)
            then("the rendered title contains '3'") {
                text shouldContain "3"
            }
        }
    }

    // ── 3. deleteModalBody is a stable warning string ────────────────────────

    given("the deleteModalBody helper") {
        `when`("called") {
            val body = MainMenuScreen.deleteModalBody()
            then("it matches the MENU_DELETE_SLOT_CONFIRM_BODY i18n entry exactly") {
                body shouldBe Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_BODY)
            }
        }
    }

    // ── 4. Companion exposes the actor-name constants the screen uses ────────
    //
    // These constants are the contract between the screen code (which tags
    // the modal's Cancel/Delete buttons via Actor.name) and the screen's own
    // focus-management code (which looks them up via Group.findActor). If the
    // names drift, default-focus and Esc-cancel both silently break.

    given("the MainMenuScreen companion") {
        `when`("the modal actor-name constants are read") {
            then("Cancel button name is 'delete_modal_cancel'") {
                MainMenuScreen.DELETE_MODAL_CANCEL_NAME shouldBe "delete_modal_cancel"
            }
            then("Confirm (Delete) button name is 'delete_modal_confirm'") {
                MainMenuScreen.DELETE_MODAL_CONFIRM_NAME shouldBe "delete_modal_confirm"
            }
            then("Modal root group name is 'delete_modal_root'") {
                MainMenuScreen.DELETE_MODAL_ROOT_NAME shouldBe "delete_modal_root"
            }
            then("Cancel and Confirm names are distinct (so findActor never aliases)") {
                (MainMenuScreen.DELETE_MODAL_CANCEL_NAME ==
                    MainMenuScreen.DELETE_MODAL_CONFIRM_NAME) shouldBe false
            }
        }
    }

    // ── 5. Default English wording on the buttons (anti-regression) ──────────

    given("the default English delete-modal button labels") {
        `when`("Cancel and Delete labels are compared") {
            val cancel = Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_CANCEL)
            val confirm = Strings.get(StringKey.MENU_DELETE_SLOT_CONFIRM_DELETE)
            then("they are distinct strings (no accidental same-label confusion)") {
                (cancel == confirm) shouldBe false
            }
            then("Cancel says 'Cancel' (matches ticket spec verbatim)") {
                cancel shouldBe "Cancel"
            }
            then("Delete says 'Delete' (matches ticket spec verbatim)") {
                confirm shouldBe "Delete"
            }
        }
    }

    // ── 6. Bare-instance allocation works (no GL needed) ─────────────────────
    //
    // Smoke check that future tests can keep using the same pattern to assert
    // structural facts on the screen without booting the GL stage.

    given("the MainMenuScreen class") {
        `when`("an instance is allocated via Unsafe (no constructor)") {
            val screen: Any = allocBare()
            then("the instance is non-null and of the right type") {
                (screen is MainMenuScreen) shouldBe true
            }
        }
    }
})

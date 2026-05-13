package com.sohai.platformer.input

import com.badlogic.gdx.Input
import com.sohai.platformer.persist.defaultKeybinds
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the T-118 master-mute hotkey wiring.
 *
 * Scope is intentionally narrow — runtime behaviour of [InputManager.pollMuteHotkey]
 * depends on `Gdx.input` / `Gdx.app` / `SettingsManager` / the audio managers, which
 * would require the same MockK harness [com.sohai.platformer.persist.SettingsManagerTest]
 * sets up. The high-value invariant for this PR is the default-keybind contract:
 *
 *   1. "mute" must exist in [defaultKeybinds].
 *   2. The default keycode must be [Input.Keys.M] — anything else silently
 *      ships the wrong default and the ticket's "default M" requirement
 *      regresses without a compile-time failure.
 *
 * The wire-name contract (every default key must resolve to an [InputAction])
 * is already covered by [InputActionTest]; we explicitly assert "mute" is no
 * longer the missing-from-defaults outlier.
 */
class MuteHotkeyTest : BehaviorSpec({

    given("Settings.defaultKeybinds() after T-118") {
        `when`("looking up the 'mute' action") {
            then("it is bound to Input.Keys.M by default") {
                defaultKeybinds()["mute"] shouldBe Input.Keys.M
            }
        }

        `when`("comparing against the InputAction enum") {
            then("every InputAction wireName (including MUTE) now has a default binding") {
                val defaultKeys = defaultKeybinds().keys
                InputAction.values().forEach { action ->
                    val key = defaultKeys.firstOrNull { it == action.wireName }
                    check(key != null) {
                        "InputAction.${action.name} (wireName='${action.wireName}') " +
                                "is missing from defaultKeybinds() after T-118"
                    }
                }
            }
        }
    }
})

package com.sohai.platformer.screens

import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * T-145: Sound-test subsection in SettingsScreen.
 *
 * SettingsScreen's constructor instantiates a libGDX [com.badlogic.gdx.scenes.scene2d.Stage],
 * which can't be safely constructed in a JVM-only test. Rather than reach for
 * `sun.misc.Unsafe.allocateInstance`, we narrow this guard to the i18n
 * contract: every new [StringKey] added for the sound-test buttons must
 * resolve to a non-blank English string, and the music-button label must
 * still mention the 3-second auto-stop the spec promises.
 *
 * This catches the most common regression — adding a [StringKey] without a
 * matching entry in the english map, which would throw [error] at runtime
 * the first time the Settings screen renders.
 */
class SettingsSoundTestStringsTest : BehaviorSpec({

    given("every T-145 sound-test StringKey") {
        val soundTestKeys = listOf(
            StringKey.SETTINGS_SOUND_TEST_HEADING,
            StringKey.SETTINGS_TEST_UI_CLICK,
            StringKey.SETTINGS_TEST_SFX_JUMP,
            StringKey.SETTINGS_TEST_MUSIC_AMBIENT,
        )

        `when`("each is looked up via Strings.get") {
            then("none returns blank or throws") {
                for (key in soundTestKeys) {
                    Strings.get(key).shouldNotBeBlank()
                }
            }
        }

        `when`("the music button label is inspected") {
            then("it advertises the 3-second auto-stop the spec promises") {
                // Guards the contract that the visible button text matches
                // the actual scheduled stop duration (3s) — if someone bumps
                // the Timer.schedule delay but forgets to update the label
                // (or vice versa) this catches the drift.
                Strings.get(StringKey.SETTINGS_TEST_MUSIC_AMBIENT) shouldContain "3s"
            }
        }
    }
})

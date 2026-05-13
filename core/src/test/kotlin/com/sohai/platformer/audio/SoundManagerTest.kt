package com.sohai.platformer.audio

import com.badlogic.gdx.Application
import com.badlogic.gdx.Audio
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * Tests for [SoundManager] — the SFX registry + per-bus (game / UI) volume bus.
 *
 * Isolation strategy:
 *  - [SoundManager] is an `object`, so its `sounds` map persists across tests.
 *    Every scenario calls [SoundManager.dispose] (which clears the map and
 *    resets enabled-state to whatever was last set) and re-runs `init()` so
 *    each test starts from a known state.
 *  - libGDX globals (`Gdx.app`, `Gdx.audio`, `Gdx.files`) are mocked in
 *    [beforeSpec]. `Gdx.files.internal(path)` returns a relaxed [FileHandle]
 *    whose `exists()` is true for every key in the production map, so `init()`
 *    happily loads every entry. `Gdx.audio.newSound(handle)` returns a fresh
 *    relaxed [Sound] mock per path, recorded in [soundMocks] so the test body
 *    can verify capture args.
 *  - No live audio backend, no disk I/O, no temp dirs.
 *
 * The production [SoundManager] singleton is NOT modified.
 *
 * Contract notes (verified by the tests below):
 *  - `play(name)` resolves to `Sound.play(volume, pitch, pan=0f)` — the 3-arg
 *    libGDX overload, not the 1-arg one.
 *  - Unknown ids call `Gdx.app.error(...)` and do not throw.
 *  - `setVolume(v)` clamps via `coerceIn(0f, 1f)` — so 2f -> 1f, -0.5f -> 0f.
 *  - Volume changes apply on the NEXT `play`, not retroactively.
 *  - `volume` and `uiVolume` are separate buses.
 */
class SoundManagerTest : BehaviorSpec({

    // Snapshot prior globals so we can restore them.
    val prevApp: Application? = Gdx.app
    val prevAudio: Audio? = Gdx.audio
    val prevFiles: Files? = Gdx.files

    // Per-spec registry of path -> Sound mock so tests can fetch the same mock
    // that init() handed to SoundManager.
    val soundMocks = mutableMapOf<String, Sound>()

    // The full canonical + legacy key->path map mirrored from SoundManager.
    val allPaths = mapOf(
        "jump"              to "audio/sfx/jump.wav",
        "land"              to "audio/sfx/land.wav",
        "collect_token"     to "audio/sfx/collect_token.wav",
        "collect_snapshot"  to "audio/sfx/collect_snapshot.wav",
        "death"             to "audio/sfx/death.wav",
        "checkpoint"        to "audio/sfx/checkpoint.wav",
        "level_complete"    to "audio/sfx/level_complete.wav",
        "hazard_cleansed"   to "audio/sfx/hazard_cleansed.wav",
        "collect"           to "sounds/collect.wav",
        "cleanse"           to "sounds/cleanse.wav",
        "ability_ebo"       to "sounds/ability_ebo.wav",
        "ability_laya"      to "sounds/ability_laya.wav"
    )

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)

        // Path -> handle cache so audio.newSound(handle) can map back to the
        // path the handle was minted for.
        val pathToHandle = mutableMapOf<String, FileHandle>()

        // Files mock: internal(path) -> a relaxed FileHandle with exists()=true.
        val filesMock = mockk<Files>(relaxed = true)
        every { filesMock.internal(any<String>()) } answers {
            val p = firstArg<String>()
            pathToHandle.getOrPut(p) {
                mockk<FileHandle>(relaxed = true).also {
                    every { it.exists() } returns true
                }
            }
        }
        Gdx.files = filesMock

        // Audio mock: newSound(handle) -> a fresh relaxed Sound mock, recorded
        // by path so the test body can verify(exactly = ...) { mockFor(id) }.
        val audioMock = mockk<Audio>(relaxed = true)
        every { audioMock.newSound(any<FileHandle>()) } answers {
            val handle = firstArg<FileHandle>()
            val path = pathToHandle.entries.firstOrNull { it.value === handle }?.key
                ?: "<unknown>"
            val s = mockk<Sound>(relaxed = true)
            soundMocks[path] = s
            s
        }
        Gdx.audio = audioMock
    }

    afterSpec {
        // Drop everything the manager loaded and restore globals.
        SoundManager.dispose()
        Gdx.app = prevApp
        Gdx.audio = prevAudio
        Gdx.files = prevFiles
        soundMocks.clear()
    }

    /** Wipe + re-init so each scenario starts with a freshly-loaded registry. */
    fun resetManager() {
        SoundManager.dispose()
        soundMocks.clear()
        SoundManager.setEnabled(true)
        SoundManager.setVolume(0.8f)        // production default
        SoundManager.setUiVolume(1f)        // production default
        SoundManager.setMasterVolume(1f)    // T-105 production default
        SoundManager.setMuted(false)        // T-105 production default
        SoundManager.init()
    }

    /** Convenience: the Sound mock that init() registered for [name]. */
    fun mockFor(name: String): Sound =
        soundMocks[allPaths.getValue(name)]
            ?: error("No mock recorded for $name — init() did not load it.")

    given("an initialised SoundManager at the default volume (0.8)") {
        resetManager()

        `when`("play(\"jump\") is called") {
            val volSlot = slot<Float>()
            val pitchSlot = slot<Float>()
            val panSlot = slot<Float>()

            SoundManager.play("jump")

            then("the registered Sound for jump receives play(0.8, 1.0, 0.0)") {
                verify(exactly = 1) {
                    mockFor("jump").play(
                        capture(volSlot),
                        capture(pitchSlot),
                        capture(panSlot)
                    )
                }
                volSlot.captured shouldBe 0.8f
                pitchSlot.captured shouldBe 1f
                panSlot.captured shouldBe 0f
            }
        }
    }

    given("a volume change before playing") {
        resetManager()

        `when`("setVolume(0.25) then play(\"land\")") {
            SoundManager.setVolume(0.25f)
            SoundManager.play("land")

            then("Sound.play receives the new volume (0.25)") {
                verify(exactly = 1) {
                    mockFor("land").play(0.25f, 1f, 0f)
                }
            }
        }
    }

    given("a play call that has already happened") {
        resetManager()

        `when`("setVolume changes AFTER the play call") {
            // First play at the default volume (0.8).
            SoundManager.play("checkpoint")
            // Now change the volume. The earlier call must NOT be affected.
            SoundManager.setVolume(0.1f)

            then("the prior Sound.play call was made with the OLD volume") {
                verify(exactly = 1) {
                    mockFor("checkpoint").play(0.8f, 1f, 0f)
                }
                // And explicitly: the new volume was NOT used for that call.
                verify(exactly = 0) {
                    mockFor("checkpoint").play(0.1f, any(), any())
                }
            }

            and("a subsequent play uses the new volume") {
                SoundManager.play("checkpoint")
                verify(exactly = 1) {
                    mockFor("checkpoint").play(0.1f, 1f, 0f)
                }
            }
        }
    }

    given("an unknown sound id") {
        resetManager()

        `when`("play(\"definitely_not_a_real_sound\") is called") {
            // Should not throw.
            SoundManager.play("definitely_not_a_real_sound")

            then("Gdx.app.error is invoked and no exception escapes") {
                verify(atLeast = 1) {
                    Gdx.app.error("SoundManager", match<String> {
                        it.contains("unknown sound") &&
                            it.contains("definitely_not_a_real_sound")
                    })
                }
            }
            then("no registered Sound mock was triggered") {
                soundMocks.values.forEach { sound ->
                    verify(exactly = 0) { sound.play(any(), any(), any()) }
                }
            }
        }
    }

    given("the volume bus pinned to zero") {
        resetManager()

        `when`("setVolume(0f) then play(\"jump\")") {
            SoundManager.setVolume(0f)
            SoundManager.play("jump")

            then("Sound.play is still invoked (silent, not skipped) at volume 0") {
                verify(exactly = 1) {
                    mockFor("jump").play(0f, 1f, 0f)
                }
            }
        }
    }

    given("the volume bus pinned to one") {
        resetManager()

        `when`("setVolume(1f) then play(\"death\")") {
            SoundManager.setVolume(1f)
            SoundManager.play("death")

            then("Sound.play is invoked at volume 1.0") {
                verify(exactly = 1) {
                    mockFor("death").play(1f, 1f, 0f)
                }
            }
        }
    }

    given("setVolume called with out-of-range values") {
        resetManager()

        `when`("setVolume(2f) then play") {
            SoundManager.setVolume(2f)
            SoundManager.play("collect_token")

            then("the value clamps to 1.0 via coerceIn(0f, 1f)") {
                verify(exactly = 1) {
                    mockFor("collect_token").play(1f, 1f, 0f)
                }
            }
        }

        `when`("setVolume(-0.5f) then play") {
            SoundManager.setVolume(-0.5f)
            SoundManager.play("collect_snapshot")

            then("the value clamps to 0.0 via coerceIn(0f, 1f)") {
                verify(exactly = 1) {
                    mockFor("collect_snapshot").play(0f, 1f, 0f)
                }
            }
        }
    }

    given("two distinct sound ids") {
        resetManager()

        `when`("play(\"jump\") and play(\"land\") are both called") {
            SoundManager.play("jump")
            SoundManager.play("land")

            then("they resolve to different Sound mocks") {
                val jump = mockFor("jump")
                val land = mockFor("land")
                (jump === land) shouldBe false
            }
            then("each mock recorded exactly one play at the current volume") {
                verify(exactly = 1) { mockFor("jump").play(0.8f, 1f, 0f) }
                verify(exactly = 1) { mockFor("land").play(0.8f, 1f, 0f) }
            }
        }
    }

    given("a separate UI volume bus") {
        resetManager()

        `when`("setUiVolume(0.3f) and playUi vs play diverge") {
            SoundManager.setVolume(0.9f)
            SoundManager.setUiVolume(0.3f)

            SoundManager.play("jump")        // uses game bus -> 0.9
            SoundManager.playUi("checkpoint") // uses UI bus   -> 0.3

            then("play uses the game volume and playUi uses the UI volume") {
                verify(exactly = 1) { mockFor("jump").play(0.9f, 1f, 0f) }
                verify(exactly = 1) { mockFor("checkpoint").play(0.3f, 1f, 0f) }
            }
        }
    }

    given("the manager disabled via setEnabled(false)") {
        resetManager()

        `when`("setEnabled(false) then play(\"jump\")") {
            SoundManager.setEnabled(false)
            SoundManager.play("jump")

            then("no Sound.play is invoked on the registered mock") {
                verify(exactly = 0) {
                    mockFor("jump").play(any(), any(), any())
                }
            }
        }
    }

    given("a play call with a custom pitch") {
        resetManager()

        `when`("play(\"jump\", pitch = 1.5f) is called") {
            SoundManager.play("jump", 1.5f)

            then("Sound.play receives the supplied pitch (and pan stays 0)") {
                verify(exactly = 1) {
                    mockFor("jump").play(0.8f, 1.5f, 0f)
                }
            }
        }
    }

    // ── T-105: master volume + mute toggle ───────────────────────────────────

    given("setMasterVolume() multiplies on top of the per-bus volume (T-105)") {
        resetManager()

        `when`("master = 0.5 and play(\"jump\") at bus volume 0.8") {
            SoundManager.setMasterVolume(0.5f)
            SoundManager.play("jump")

            then("Sound.play receives master * bus = 0.5 * 0.8 = 0.4") {
                verify(exactly = 1) { mockFor("jump").play(0.4f, 1f, 0f) }
            }
        }
    }

    given("setMasterVolume() applies to the UI bus too (T-105)") {
        resetManager()

        `when`("master = 0.25 and playUi(\"checkpoint\") at uiVolume 1.0") {
            SoundManager.setMasterVolume(0.25f)
            SoundManager.playUi("checkpoint")

            then("Sound.play receives master * ui = 0.25 * 1 = 0.25") {
                verify(exactly = 1) { mockFor("checkpoint").play(0.25f, 1f, 0f) }
            }
        }
    }

    given("setMasterVolume() clamps out-of-range values (T-105)") {
        resetManager()

        `when`("setMasterVolume(2f) then play") {
            SoundManager.setMasterVolume(2f)
            SoundManager.play("jump")

            then("the value clamps to 1.0 → effective = 0.8") {
                verify(exactly = 1) { mockFor("jump").play(0.8f, 1f, 0f) }
            }
        }

        `when`("setMasterVolume(-0.5f) then play") {
            SoundManager.setMasterVolume(-0.5f)
            SoundManager.play("land")

            then("the value clamps to 0 → effective = 0") {
                verify(exactly = 1) { mockFor("land").play(0f, 1f, 0f) }
            }
        }
    }

    given("setMuted(true) gates the output regardless of master/bus (T-105)") {
        resetManager()

        `when`("setMuted(true) then play(\"jump\")") {
            // Confirm the slider values are preserved (not zeroed) by mute.
            SoundManager.setMasterVolume(0.7f)
            SoundManager.setVolume(0.5f)
            SoundManager.setMuted(true)
            SoundManager.play("jump")

            then("Sound.play receives 0 (gated by mute)") {
                verify(exactly = 1) { mockFor("jump").play(0f, 1f, 0f) }
            }
        }

        `when`("setMuted(false) restores the prior master * bus value") {
            // resetManager wipes registry; we re-mute, play, unmute, play again.
            resetManager()
            SoundManager.setMasterVolume(0.7f)
            SoundManager.setVolume(0.5f)
            SoundManager.setMuted(true)
            SoundManager.play("jump")
            // Now unmute and replay — slider values were preserved, so the next
            // play() should use the prior master * bus.
            SoundManager.setMuted(false)
            SoundManager.play("jump")

            then("the unmuted play uses master * bus = 0.7 * 0.5 = 0.35") {
                verify(exactly = 1) { mockFor("jump").play(0.35f, 1f, 0f) }
                // And the muted play recorded a 0 — both calls were captured.
                verify(exactly = 1) { mockFor("jump").play(0f, 1f, 0f) }
            }
        }
    }

    given("setMuted(true) gates the UI bus too (T-105)") {
        resetManager()

        `when`("setMuted(true) then playUi(\"checkpoint\")") {
            SoundManager.setUiVolume(0.9f)
            SoundManager.setMuted(true)
            SoundManager.playUi("checkpoint")

            then("Sound.play receives 0 for the UI bus too") {
                verify(exactly = 1) { mockFor("checkpoint").play(0f, 1f, 0f) }
            }
        }
    }
})

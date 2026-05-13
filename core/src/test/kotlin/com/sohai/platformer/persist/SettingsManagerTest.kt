package com.sohai.platformer.persist

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID

/**
 * Tests for [SettingsManager] round-trip persistence and field defaults.
 *
 * Isolation strategy mirrors [SaveManagerTest]:
 *  - [SettingsManager] uses [Gdx.files] and [Gdx.app]. We mock both:
 *      * `Gdx.app`   — relaxed mock so `log()` / `error()` are no-ops.
 *      * `Gdx.files` — `local(path)` returns a real [FileHandle] wrapping a
 *        per-spec temp directory so writeString / readString / exists / delete
 *        all hit a real (but isolated) filesystem.
 *  - The in-memory cache is reset before every scenario via
 *    [SettingsManager.resetCacheForTest] so test ordering does not bleed.
 *
 * The T-105 coverage focus is on `volMaster` and `muted`:
 *  - default values
 *  - round-trip through save/load
 *  - back-compat with legacy settings.json files lacking the new fields
 */
class SettingsManagerTest : BehaviorSpec({

    val tmpRoot: File = File(
        System.getProperty("java.io.tmpdir"),
        "cloudy_settingsmgr_${UUID.randomUUID()}"
    ).apply { mkdirs() }

    val prevApp: Application? = Gdx.app
    val prevFiles: Files? = Gdx.files

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)
        val filesMock = mockk<Files>(relaxed = true)
        every { filesMock.local(any<String>()) } answers {
            val rel = firstArg<String>()
            val abs = File(tmpRoot, rel).absoluteFile
            FileHandle(abs)
        }
        Gdx.files = filesMock
    }

    afterSpec {
        Gdx.app = prevApp
        Gdx.files = prevFiles
        tmpRoot.deleteRecursively()
    }

    /** Wipe settings.json + the in-memory cache so each scenario starts clean. */
    fun resetWorld() {
        SettingsManager.resetCacheForTest()
        File(tmpRoot, "settings.json").delete()
        File(tmpRoot, "settings.json.tmp").delete()
    }

    given("a fresh load with no settings.json on disk") {

        `when`("load() is called") {
            resetWorld()
            val s = SettingsManager.load()

            then("volMaster defaults to 1.0 (T-105)") {
                s.volMaster shouldBe 1.0f
            }
            then("muted defaults to false (T-105)") {
                s.muted shouldBe false
            }
            then("the existing per-bus defaults are unchanged (T-035 back-compat)") {
                s.volMusic shouldBe 0.7f
                s.volSfx shouldBe 0.9f
                s.volUi shouldBe 0.9f
            }
        }
    }

    given("a save/load round-trip for the T-105 fields") {

        `when`("save() writes a modified Settings and load() re-reads") {
            resetWorld()
            val written = Settings(volMaster = 0.42f, muted = true)
            SettingsManager.save(written)
            SettingsManager.resetCacheForTest()                 // force a real re-read from disk
            val read = SettingsManager.load()

            then("the master volume round-trips exactly") {
                read.volMaster shouldBe 0.42f
            }
            then("the mute flag round-trips exactly") {
                read.muted shouldBe true
            }
        }

        `when`("update { copy(volMaster, muted) } is used") {
            resetWorld()
            // Seed defaults first by loading once.
            SettingsManager.load()
            SettingsManager.update { it.copy(volMaster = 0.25f, muted = true) }
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("both fields persist") {
                read.volMaster shouldBe 0.25f
                read.muted shouldBe true
            }
        }
    }

    // ----------------------------------------------------------------------
    // T-121: swap-keybind default change S → Q, with one-shot migration for
    // players who never opened the Controls panel. Six cases per the ticket.
    // ----------------------------------------------------------------------

    given("T-121 case A: fresh install (no settings.json on disk)") {

        `when`("load() is called") {
            resetWorld()
            val s = SettingsManager.load()

            then("the default swap binding is Q") {
                s.keybinds["swap"] shouldBe Input.Keys.Q
            }
            then("keybindsCustomized defaults to false") {
                s.keybindsCustomized shouldBe false
            }
        }
    }

    given("T-121 case B: legacy save with swap=S and no customized flag") {

        `when`("load() runs the migration") {
            resetWorld()
            // Legacy file: pre-T-121, has swap=S, lacks keybindsCustomized.
            // We build the JSON from Input.Keys.* constants rather than
            // hard-coded keycodes so the test stays correct if libGDX ever
            // renumbers (unlikely, but cheap insurance).
            val legacy = """{"keybinds":{"left":${Input.Keys.A},"right":${Input.Keys.D},"jump":${Input.Keys.SPACE},"action":${Input.Keys.E},"swap":${Input.Keys.S},"restart":${Input.Keys.R},"mute":${Input.Keys.M}}}"""
            File(tmpRoot, "settings.json").writeText(legacy)
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("swap is auto-upgraded to Q") {
                read.keybinds["swap"] shouldBe Input.Keys.Q
            }
            then("keybindsCustomized stays false — they haven't customized, we just migrated the default") {
                read.keybindsCustomized shouldBe false
            }
            then("other keybinds are untouched by the migration") {
                read.keybinds["jump"] shouldBe Input.Keys.SPACE
                read.keybinds["left"] shouldBe Input.Keys.A
            }
        }
    }

    given("T-121 case C: user opened Controls and explicitly kept swap=S") {

        `when`("load() reads a customized save with swap=S") {
            resetWorld()
            val customized =
                """{"keybinds":{"left":${Input.Keys.A},"right":${Input.Keys.D},"jump":${Input.Keys.SPACE},"action":${Input.Keys.E},"swap":${Input.Keys.S},"restart":${Input.Keys.R},"mute":${Input.Keys.M}},"keybindsCustomized":true}"""
            File(tmpRoot, "settings.json").writeText(customized)
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("their swap=S is respected") {
                read.keybinds["swap"] shouldBe Input.Keys.S
            }
            then("the customized flag is preserved") {
                read.keybindsCustomized shouldBe true
            }
        }
    }

    given("T-121 case D: idempotent reload after a migrated session") {

        `when`("we save the migrated state and reload twice") {
            resetWorld()
            val legacy = """{"keybinds":{"swap":${Input.Keys.S}}}"""
            File(tmpRoot, "settings.json").writeText(legacy)
            SettingsManager.resetCacheForTest()
            val firstLoad = SettingsManager.load()
            firstLoad.keybinds["swap"] shouldBe Input.Keys.Q
            firstLoad.keybindsCustomized shouldBe false

            // Persist the migrated state, then re-read from disk.
            SettingsManager.save(firstLoad)
            SettingsManager.resetCacheForTest()
            val secondLoad = SettingsManager.load()

            then("swap stays Q after a save/reload cycle") {
                secondLoad.keybinds["swap"] shouldBe Input.Keys.Q
            }
            then("keybindsCustomized stays false — migration is a no-op once swap≠S") {
                secondLoad.keybindsCustomized shouldBe false
            }
        }
    }

    given("T-121 case E: update() flips keybindsCustomized when keybinds map changes") {

        `when`("update() rebinds jump to K") {
            resetWorld()
            val initial = SettingsManager.load()
            initial.keybindsCustomized shouldBe false

            SettingsManager.update {
                it.copy(keybinds = it.keybinds + ("jump" to Input.Keys.K))
            }
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("the rebind persists") {
                read.keybinds["jump"] shouldBe Input.Keys.K
            }
            then("keybindsCustomized flips to true") {
                read.keybindsCustomized shouldBe true
            }
        }
    }

    given("T-121 case F: update() does NOT flip the flag on non-keybind changes") {

        `when`("update() mutates only volMaster") {
            resetWorld()
            val initial = SettingsManager.load()
            initial.keybindsCustomized shouldBe false

            SettingsManager.update { it.copy(volMaster = 0.5f) }
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("volMaster persists") {
                read.volMaster shouldBe 0.5f
            }
            then("keybindsCustomized stays false") {
                read.keybindsCustomized shouldBe false
            }
        }
    }

    // ----------------------------------------------------------------------
    // T-142: Speedrun-timer toggle round-trip. New additive boolean — must
    // default to false (so smoke CI / pre-T-142 saves are unchanged) and
    // must persist exactly through save → load.
    // ----------------------------------------------------------------------

    given("T-142: speedrun-timer default + round-trip") {

        `when`("load() reads a fresh install") {
            resetWorld()
            val s = SettingsManager.load()

            then("speedrunTimer defaults to false") {
                s.speedrunTimer shouldBe false
            }
        }

        `when`("update { copy(speedrunTimer = true) } is used") {
            resetWorld()
            SettingsManager.load() // seed defaults
            SettingsManager.update { it.copy(speedrunTimer = true) }
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("the flag persists across save/reload") {
                read.speedrunTimer shouldBe true
            }
            then("keybindsCustomized stays false — non-keybind change") {
                read.keybindsCustomized shouldBe false
            }
        }

        `when`("a legacy settings.json without speedrunTimer is loaded") {
            resetWorld()
            // Lacks the speedrunTimer field — deserialisation must fall back
            // to the default `false` rather than throw.
            val legacy = """{"volMaster":0.8}"""
            File(tmpRoot, "settings.json").writeText(legacy)
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("speedrunTimer falls back to the default false") {
                read.speedrunTimer shouldBe false
            }
        }
    }

    // ----------------------------------------------------------------------
    // T-143: Reset-to-defaults. The footer button in SettingsScreen routes
    // through [SettingsManager.reset]. It must:
    //   (a) write a default Settings() to disk so reload sees defaults
    //   (b) leave keybindsCustomized = false even when the prior state had
    //       customized keybinds — the whole reason reset() bypasses update()
    //       rather than reusing it
    //   (c) be idempotent (back-to-back resets keep the same default state)
    //   (d) NOT touch save data (we don't test save files here, but the
    //       implementation only writes settings.json — see (a))
    // ----------------------------------------------------------------------

    given("T-143 case A: reset() from a heavily customized state") {

        `when`("reset() is called after a user customized audio + keybinds") {
            resetWorld()
            // Seed defaults, then mutate broadly: a keybind change (which
            // also flips keybindsCustomized to true via update()) plus
            // several non-keybind fields across categories.
            SettingsManager.load()
            SettingsManager.update {
                it.copy(
                    volMaster = 0.42f,
                    muted = true,
                    showFps = true,
                    speedrunTimer = true,
                    reducedMotion = true,
                    highContrast = true,
                    screenShake = false,
                    keybinds = it.keybinds + ("jump" to Input.Keys.K)
                )
            }
            // Sanity: the customization detector should have flipped.
            SettingsManager.resetCacheForTest()
            SettingsManager.load().keybindsCustomized shouldBe true

            // Now reset.
            SettingsManager.reset()
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("audio fields are restored to defaults") {
                read.volMaster shouldBe 1.0f
                read.muted shouldBe false
                read.volMusic shouldBe 0.7f
                read.volSfx shouldBe 0.9f
                read.volUi shouldBe 0.9f
                read.volAmbient shouldBe 0.6f
            }
            then("display + accessibility fields are restored to defaults") {
                read.showFps shouldBe false
                read.speedrunTimer shouldBe false
                read.reducedMotion shouldBe false
                read.highContrast shouldBe false
                read.screenShake shouldBe true
                read.cameraLookAhead shouldBe true
            }
            then("keybinds are restored to factory defaults") {
                read.keybinds["jump"] shouldBe Input.Keys.SPACE
                read.keybinds["swap"] shouldBe Input.Keys.Q
            }
            then("keybindsCustomized stays FALSE — the key invariant: reset must NOT route through update()") {
                read.keybindsCustomized shouldBe false
            }
        }
    }

    given("T-143 case B: reset() is idempotent") {

        `when`("reset() is called twice in a row") {
            resetWorld()
            SettingsManager.load()
            SettingsManager.update { it.copy(volMaster = 0.1f) }

            val first  = SettingsManager.reset()
            val second = SettingsManager.reset()

            then("both returned states equal a fresh Settings()") {
                first  shouldBe Settings()
                second shouldBe Settings()
            }

            // Confirm the on-disk state matches after the second call too.
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("the persisted state still equals defaults after two resets") {
                read shouldBe Settings()
            }
        }
    }

    given("T-143 case C: reset() persists across a cache flush") {

        `when`("reset() is followed by an in-memory cache flush and reload") {
            resetWorld()
            SettingsManager.load()
            SettingsManager.update { it.copy(volSfx = 0.05f, fullscreen = true) }
            SettingsManager.reset()

            // Force the next load() to hit disk.
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("the reload sees defaults (file was actually written, not just cached)") {
                read.volSfx shouldBe 0.9f
                read.fullscreen shouldBe false
            }
        }
    }

    // ----------------------------------------------------------------------
    // T-208: ambient-light brightness multiplier. New additive Float — must
    // default to 1.0 (so smoke CI / pre-T-208 saves render byte-identically),
    // persist exactly through save → load, and not flip the keybindsCustomized
    // detector (it's a non-keybind change).
    // ----------------------------------------------------------------------

    given("T-208: brightness default + round-trip") {

        `when`("load() reads a fresh install") {
            resetWorld()
            val s = SettingsManager.load()

            then("brightness defaults to 1.0 (= calibrated T-207 ambient)") {
                s.brightness shouldBe 1.0f
            }
        }

        `when`("update { copy(brightness = 0.5f) } is used") {
            resetWorld()
            SettingsManager.load() // seed defaults
            SettingsManager.update { it.copy(brightness = 0.5f) }
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("the multiplier persists across save/reload") {
                read.brightness shouldBe 0.5f
            }
            then("keybindsCustomized stays false — non-keybind change (T-121 detector intact)") {
                read.keybindsCustomized shouldBe false
            }
        }

        `when`("the slider is dragged to the 2.0 maximum") {
            resetWorld()
            SettingsManager.load()
            SettingsManager.update { it.copy(brightness = 2.0f) }
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("the maximum value persists exactly (no clamping in persistence)") {
                read.brightness shouldBe 2.0f
            }
        }

        `when`("a legacy settings.json without brightness is loaded") {
            resetWorld()
            // Lacks the brightness field — deserialisation must fall back to
            // the default 1.0f (= T-207 calibrated look) rather than throw.
            val legacy = """{"volMaster":0.8}"""
            File(tmpRoot, "settings.json").writeText(legacy)
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("brightness falls back to the default 1.0") {
                read.brightness shouldBe 1.0f
            }
        }
    }

    given("a legacy settings.json predating T-105 (no volMaster, no muted)") {

        `when`("load() deserialises the legacy file") {
            resetWorld()
            // Hand-write a legacy settings.json that lacks volMaster + muted.
            // ignoreUnknownKeys + serialisation defaults guarantee the missing
            // fields take their default values without error.
            val legacy = """{"volMusic":0.5,"volSfx":0.8}"""
            File(tmpRoot, "settings.json").writeText(legacy)
            SettingsManager.resetCacheForTest()
            val read = SettingsManager.load()

            then("volMaster falls back to the default 1.0 (T-113 back-compat)") {
                read.volMaster shouldBe 1.0f
            }
            then("muted falls back to the default false (T-113 back-compat)") {
                read.muted shouldBe false
            }
            then("the legacy fields survived deserialisation") {
                read.volMusic shouldBe 0.5f
                read.volSfx shouldBe 0.8f
            }
        }
    }
})

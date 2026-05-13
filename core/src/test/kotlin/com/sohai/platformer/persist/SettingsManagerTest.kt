package com.sohai.platformer.persist

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
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

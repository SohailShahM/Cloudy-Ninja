package com.sohai.platformer.audio

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID

/**
 * Tests for the T-134 addition of the `ambient_menu` procedural track.
 *
 * Two surfaces are exercised:
 *   1. [ProceduralMusicGenerator.generateOne] writes a non-empty WAV at the
 *      expected on-disk path when invoked with `"ambient_menu"`.
 *   2. [MusicManager.PRELOAD_TRACKS] — the splash preload (T-104) — includes
 *      `"ambient_menu"`, so the new track is generated + warmed alongside the
 *      pre-existing three.
 *
 * Isolation strategy mirrors [com.sohai.platformer.persist.SaveManagerTest]:
 * we stub `Gdx.files.local(path)` to return a real [FileHandle] under a
 * per-spec temp directory so the generator's `writeBytes` hits a real (but
 * isolated) filesystem. `Gdx.app` is relaxed-mocked so the generator's
 * `log()` call is a no-op.
 */
class ProceduralMusicGeneratorTest : BehaviorSpec({

    val tmpRoot: File = File(
        System.getProperty("java.io.tmpdir"),
        "cloudy_musicgen_${UUID.randomUUID()}"
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

    given("MusicManager.PRELOAD_TRACKS — splash preload (T-104) list") {

        `when`("the production preload list is inspected") {

            then("ambient_menu is included so the splash generates + warms it") {
                MusicManager.PRELOAD_TRACKS shouldContain "ambient_menu"
            }

            then("the three pre-existing tracks remain in the list") {
                MusicManager.PRELOAD_TRACKS shouldContain "ambient_arid"
                MusicManager.PRELOAD_TRACKS shouldContain "ambient_wind"
                MusicManager.PRELOAD_TRACKS shouldContain "ambient_eco"
            }

            then("the list has exactly four entries — no duplicates / stale ids") {
                MusicManager.PRELOAD_TRACKS.size shouldBe 4
                MusicManager.PRELOAD_TRACKS.toSet().size shouldBe 4
            }
        }
    }

    given("ProceduralMusicGenerator.generateOne(\"ambient_menu\")") {

        `when`("invoked with no pre-existing file on disk") {
            // Use a clean sub-temp so generateOne is forced to write a fresh file.
            val cleanTmp = File(
                System.getProperty("java.io.tmpdir"),
                "cloudy_musicgen_clean_${UUID.randomUUID()}"
            ).apply { mkdirs() }

            // Re-stub Gdx.files.local to point under cleanTmp for this when-block only.
            val filesMockClean = mockk<Files>(relaxed = true)
            every { filesMockClean.local(any<String>()) } answers {
                val rel = firstArg<String>()
                val abs = File(cleanTmp, rel).absoluteFile
                FileHandle(abs)
            }
            val prev = Gdx.files
            Gdx.files = filesMockClean

            try {
                ProceduralMusicGenerator.generateOne("ambient_menu")

                val written = File(cleanTmp, "audio/music/ambient_menu.wav")

                then("a WAV file is written at audio/music/ambient_menu.wav") {
                    written.exists() shouldBe true
                }

                then("the WAV is non-empty (60s 22.05 kHz 16-bit mono => ~2.5 MB)") {
                    // 22050 samples/s * 60s * 2 bytes/sample = 2_646_000 byte data
                    // chunk + 44-byte header. Lower-bound at 1 MB to allow for
                    // future generator tweaks while still catching truncation /
                    // header-only outputs.
                    written.length().toInt() shouldBeGreaterThan 1_000_000
                }

                then("the file starts with a valid RIFF/WAVE header") {
                    val head = written.readBytes().copyOfRange(0, 12)
                    String(head, 0, 4, Charsets.US_ASCII) shouldBe "RIFF"
                    String(head, 8, 4, Charsets.US_ASCII) shouldBe "WAVE"
                }
            } finally {
                Gdx.files = prev
                cleanTmp.deleteRecursively()
            }
        }

        `when`("invoked with an unknown track id") {
            val cleanTmp = File(
                System.getProperty("java.io.tmpdir"),
                "cloudy_musicgen_unknown_${UUID.randomUUID()}"
            ).apply { mkdirs() }

            val filesMockClean = mockk<Files>(relaxed = true)
            every { filesMockClean.local(any<String>()) } answers {
                val rel = firstArg<String>()
                val abs = File(cleanTmp, rel).absoluteFile
                FileHandle(abs)
            }
            val prev = Gdx.files
            Gdx.files = filesMockClean

            try {
                ProceduralMusicGenerator.generateOne("definitely_not_a_real_track")

                then("no WAV is written (unknown ids are ignored, not synthesised)") {
                    val unwanted = File(cleanTmp, "audio/music/definitely_not_a_real_track.wav")
                    unwanted.exists() shouldBe false
                }
            } finally {
                Gdx.files = prev
                cleanTmp.deleteRecursively()
            }
        }
    }
})

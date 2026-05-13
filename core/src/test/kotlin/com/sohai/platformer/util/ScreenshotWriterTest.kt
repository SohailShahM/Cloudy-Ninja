package com.sohai.platformer.util

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * T-139: Unit tests for [ScreenshotWriter] — the pure-function path plus the
 * [ScreenshotWriter.write] seam.
 *
 * Tests intentionally avoid constructing a real [Pixmap] (which needs the
 * libGDX native lib loaded) and a real GL backbuffer; instead they:
 *  - exercise the pure filename + directory functions directly
 *  - mock [Pixmap] / [FileHandle] and inject a fake [ScreenshotWriter.WriteFn]
 *    seam to cover the success + failure paths of [ScreenshotWriter.write]
 *
 * The framebuffer-capture branch in [ScreenshotWriter.captureAndWrite] is
 * intentionally NOT exercised here — it requires a GL context which neither
 * unit tests nor the smoke matrix has. Manual dev verification covers it,
 * matching the [com.sohai.platformer.persist.CrashReporter.writeCrashFile]
 * precedent (T-115).
 */
class ScreenshotWriterTest : BehaviorSpec({

    val prevApp: Application? = Gdx.app

    beforeSpec {
        // Some tests probe Gdx.app.error to verify failure logging — give them
        // a relaxed mock and restore the prior global in afterSpec.
        Gdx.app = mockk<Application>(relaxed = true)
    }

    afterSpec {
        Gdx.app = prevApp
    }

    given("the screenshot filename generator") {
        `when`("given a normal level id and epoch zero") {
            val name = ScreenshotWriter.screenshotFileName(
                levelId = "level1",
                timestampMillis = 0L,
            )
            then("it follows the victory-{levelId}-yyyyMMdd-HHmmss.png shape") {
                name shouldMatch Regex("^victory-level1-\\d{8}-\\d{6}\\.png$")
            }
        }

        `when`("given a level id containing path separators or spaces") {
            val name = ScreenshotWriter.screenshotFileName(
                levelId = "world/2 boss",
                timestampMillis = 0L,
            )
            then("non-alphanumeric chars are replaced with underscores") {
                // Slash and space must NOT survive — they'd produce a malformed path.
                name.shouldContain("world_2_boss")
                name.shouldMatch(Regex("^victory-world_2_boss-\\d{8}-\\d{6}\\.png$"))
            }
        }

        `when`("given an empty level id") {
            val name = ScreenshotWriter.screenshotFileName(
                levelId = "",
                timestampMillis = 0L,
            )
            then("it falls back to 'unknown' rather than producing a double-dash name") {
                name.shouldContain("victory-unknown-")
            }
        }
    }

    given("the screenshot directory resolver") {
        `when`("given an explicit user-home path") {
            val dir = ScreenshotWriter.screenshotDir(userHome = "/tmp/fake-home")
            then("it nests under .cloudy-ninja/screenshots") {
                val path = dir.path.replace('\\', '/')
                path shouldContain ".cloudy-ninja/screenshots"
                path shouldContain "fake-home"
            }
        }
    }

    given("ScreenshotWriter.write happy path with a working WriteFn") {
        val pixmap = mockk<Pixmap>(relaxed = true)
        val file = mockk<FileHandle>(relaxed = true)
        var captured: Pair<FileHandle, Pixmap>? = null
        val writeFn = ScreenshotWriter.WriteFn { f, p -> captured = f to p }

        `when`("write is invoked") {
            val result = ScreenshotWriter.write(pixmap, file, writeFn)

            then("it returns true") {
                result.shouldBeTrue()
            }
            then("the WriteFn was handed the same file + pixmap") {
                captured?.first shouldBe file
                captured?.second shouldBe pixmap
            }
            then("the pixmap is disposed exactly once (success path)") {
                verify(exactly = 1) { pixmap.dispose() }
                confirmVerified(pixmap)
            }
        }
    }

    given("ScreenshotWriter.write failure path when WriteFn throws") {
        val pixmap = mockk<Pixmap>(relaxed = true)
        val file = mockk<FileHandle>(relaxed = true)
        every { file.path() } returns "/bogus/path/screenshot.png"
        val writeFn = ScreenshotWriter.WriteFn { _, _ ->
            throw java.io.IOException("disk full")
        }

        `when`("write is invoked") {
            val result = ScreenshotWriter.write(pixmap, file, writeFn)

            then("it returns false rather than throwing") {
                result.shouldBeFalse()
            }
            then("Gdx.app.error is called with a diagnostic message") {
                verify(atLeast = 1) {
                    Gdx.app.error("ScreenshotWriter", match<String> {
                        it.contains("disk full")
                    })
                }
            }
            then("the pixmap is still disposed even on failure") {
                verify(exactly = 1) { pixmap.dispose() }
            }
        }
    }

    given("ScreenshotWriter.write when pixmap.dispose() itself throws") {
        val pixmap = mockk<Pixmap>(relaxed = true)
        every { pixmap.dispose() } throws RuntimeException("native already freed")
        val file = mockk<FileHandle>(relaxed = true)
        val writeFn = ScreenshotWriter.WriteFn { _, _ -> /* succeed */ }

        `when`("write is invoked") {
            val result = ScreenshotWriter.write(pixmap, file, writeFn)

            then("the swallowed dispose error does not flip the success result") {
                // Write succeeded; dispose-throw is logged as info but does NOT
                // propagate or downgrade the boolean.
                result.shouldBeTrue()
            }
        }
    }

    given("the public toast wording") {
        then("it points at ~/.cloudy-ninja/screenshots/ verbatim per ticket spec") {
            ScreenshotWriter.TOAST_TEXT shouldBe
                "Screenshot saved to ~/.cloudy-ninja/screenshots/"
        }
    }
})

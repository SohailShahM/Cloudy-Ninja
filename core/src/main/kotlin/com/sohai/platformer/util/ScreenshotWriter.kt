package com.sohai.platformer.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.ScreenUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * T-139: Victory-screen screenshot dumper.
 *
 * Captures the current framebuffer to PNG at
 * `<userHome>/.cloudy-ninja/screenshots/victory-{levelId}-{yyyyMMdd-HHmmss}.png`.
 *
 * Reuses the [com.sohai.platformer.persist.CrashReporter] directory-creation
 * idiom (`<userHome>/.cloudy-ninja/<subdir>/`, `mkdirs()` on demand, never throw
 * — the on-victory toast must not fail the screen).
 *
 * **Smoke mode:** Callers MUST short-circuit on `Constants.SMOKE_MODE` BEFORE
 * invoking [captureAndWrite]. That avoids touching the framebuffer on smoke CI
 * (where there isn't necessarily a usable GL context) and prevents writing to
 * the runner's home directory.
 *
 * The public API exposes two entry points:
 *  - [write] — pure-ish wrapper around [PixmapIO.writePNG]. Always disposes
 *    [pixmap]. Returns `true` on success, `false` if the write threw.
 *  - [captureAndWrite] — convenience for the VictoryScreen call site:
 *    snapshots the back buffer via [ScreenUtils.getFrameBufferPixmap] and
 *    forwards to [write]. Lives behind the same try/catch contract.
 *
 * A seam-injection [WriteFn] is exposed for tests so we can exercise the
 * success and failure paths without spinning up a real GL context. Production
 * callers leave the default (`PixmapIO::writePNG`).
 */
object ScreenshotWriter {

    /** Functional seam matching `PixmapIO.writePNG(FileHandle, Pixmap)`. */
    fun interface WriteFn {
        fun write(file: FileHandle, pixmap: Pixmap)
    }

    private val defaultWriteFn: WriteFn = WriteFn { file, pixmap ->
        PixmapIO.writePNG(file, pixmap)
    }

    /** Stable timestamp format used in the filename. Matches CrashReporter. */
    private const val FILENAME_TIMESTAMP = "yyyyMMdd-HHmmss"

    /** Subdirectory under `user.home` where victory screenshots land. */
    private const val SCREENSHOT_SUBDIR = ".cloudy-ninja/screenshots"

    /** Toast wording displayed on VictoryScreen — kept here so tests can assert it verbatim. */
    const val TOAST_TEXT = "Screenshot saved to ~/.cloudy-ninja/screenshots/"

    /**
     * Build the screenshot filename. Pure.
     *
     * Format: `victory-{levelId}-{yyyyMMdd-HHmmss}.png` in the JVM's default
     * time zone (matches what a player sees on their wall clock).
     *
     * The [levelId] is sanitized to `[A-Za-z0-9_-]` so a future level whose id
     * contains a path separator or whitespace can't produce a malformed name.
     */
    fun screenshotFileName(
        levelId: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ): String {
        val fmt = SimpleDateFormat(FILENAME_TIMESTAMP, Locale.ROOT)
        val safeLevelId = sanitizeLevelId(levelId)
        return "victory-$safeLevelId-${fmt.format(Date(timestampMillis))}.png"
    }

    /** Resolve the screenshot directory under the JVM's user-home. Does not create it. */
    fun screenshotDir(userHome: String = System.getProperty("user.home")): File =
        File(userHome, SCREENSHOT_SUBDIR)

    private fun sanitizeLevelId(levelId: String): String {
        if (levelId.isEmpty()) return "unknown"
        val sb = StringBuilder(levelId.length)
        for (c in levelId) {
            sb.append(
                if (c.isLetterOrDigit() || c == '-' || c == '_') c else '_'
            )
        }
        return sb.toString()
    }

    /**
     * Write [pixmap] to [file] as PNG. Always disposes [pixmap] on the way out
     * (success or failure). Returns `true` on success, `false` if the write
     * threw — failure is logged via `Gdx.app.error` so the call site doesn't
     * have to.
     *
     * Visible for tests via [writeFn] injection.
     */
    fun write(
        pixmap: Pixmap,
        file: FileHandle,
        writeFn: WriteFn = defaultWriteFn,
    ): Boolean {
        return try {
            writeFn.write(file, pixmap)
            true
        } catch (t: Throwable) {
            // Don't crash the victory screen if the disk is full / read-only /
            // sandboxed — just log and let the caller skip the toast.
            try {
                Gdx.app?.error("ScreenshotWriter", "Failed to write PNG: ${t.message}")
            } catch (_: Throwable) {
                // Gdx.app may be null in tests; swallow.
            }
            false
        } finally {
            try {
                pixmap.dispose()
            } catch (_: Throwable) {
                // Tolerate double-dispose or mock-stub disposal.
            }
        }
    }

    /**
     * VictoryScreen entry point: snapshot the current framebuffer and write a
     * PNG into the user-home screenshots dir. Returns `true` if the file
     * landed on disk. Never throws.
     *
     * The caller is responsible for checking `Constants.SMOKE_MODE` BEFORE
     * invoking this — we don't want to allocate a pixmap on smoke CI runners.
     */
    fun captureAndWrite(
        levelId: String,
        timestampMillis: Long = System.currentTimeMillis(),
        userHome: String = System.getProperty("user.home"),
    ): Boolean {
        // Mirror CrashReporter's directory-creation idiom.
        val dir = screenshotDir(userHome)
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                Gdx.app?.error("ScreenshotWriter", "Could not create dir: ${dir.absolutePath}")
                return false
            }
        } catch (t: Throwable) {
            Gdx.app?.error("ScreenshotWriter", "mkdirs failed: ${t.message}")
            return false
        }

        @Suppress("DEPRECATION") // libGDX nightly deprecated this; the replacement
        // (Pixmap.createFromFrameBuffer) isn't available in our pinned 1.x.
        val pixmap: Pixmap = try {
            ScreenUtils.getFrameBufferPixmap(
                0,
                0,
                Gdx.graphics.backBufferWidth,
                Gdx.graphics.backBufferHeight,
            )
        } catch (t: Throwable) {
            Gdx.app?.error("ScreenshotWriter", "Framebuffer capture failed: ${t.message}")
            return false
        }

        val target = FileHandle(File(dir, screenshotFileName(levelId, timestampMillis)))
        return write(pixmap, target)
    }
}

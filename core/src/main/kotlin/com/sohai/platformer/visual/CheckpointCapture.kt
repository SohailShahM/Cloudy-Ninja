package com.sohai.platformer.visual

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.ScreenUtils
import com.sohai.platformer.Constants
import java.io.File

/**
 * T-A10: Visual checkpoint capture system V0.
 *
 * Smoke autopilot calls [capture] at known game states (main-menu loaded,
 * level1 start, mid-jump, after-death, etc.) and the PNGs land in
 * `build/visual-checkpoints/<sanitized-name>.png`. Claude reads them back via
 * the multimodal Read tool to visually verify the game still looks right —
 * catching regressions (title scrim missing, ambient too dark) that crash-only
 * smoke CI is blind to.
 *
 * **Gated:** the actual GL probe + disk write only happens when
 * `Constants.CAPTURE_CHECKPOINTS` is true (pass
 * `-Dcloudy.captureCheckpoints=true`). When the flag is false this object is
 * a single boolean check per call — zero allocations, zero GL work, zero IO.
 *
 * **Filenames are deterministic:** same checkpoint name → same file, so each
 * run overwrites the previous capture. No timestamps in filenames — that's
 * the [com.sohai.platformer.util.ScreenshotWriter] (T-139/T-147) contract,
 * not this one.
 *
 * **Output dir:** `build/visual-checkpoints/` (build-time artifact, NOT
 * `~/.cloudy-ninja/` which belongs to T-139/T-147's user-facing screenshots).
 * Already covered by `/build/` in `.gitignore`.
 *
 * **Failure handling:** every IO / GL exception is caught and logged via
 * `Gdx.app.error`. A capture failure must never crash the smoke autopilot.
 * Pixmap dispose happens in `finally`.
 */
object CheckpointCapture {

    /**
     * Matches any character that's NOT in `[A-Za-z0-9_-]`. The replacement
     * collapses each run of unsafe characters to a single underscore so a
     * name like "Level 1 / start!" becomes a single-token-per-word path
     * instead of "Level_1___start_".
     *
     * Visible as `internal` for the test.
     */
    internal val UNSAFE_NAME = Regex("[^A-Za-z0-9_-]+")

    /** Build-relative output dir. Created on demand by [capture]. */
    internal const val OUT_DIR_PATH = "build/visual-checkpoints"

    /**
     * Capture the current back buffer to `build/visual-checkpoints/<name>.png`.
     *
     * No-op (single boolean check) unless `cloudy.captureCheckpoints=true` is
     * passed at JVM startup. Must be called AFTER the relevant frame has been
     * rendered — calling mid-render before flush will write a partial frame.
     * The smoke autopilot's call sites use a `pendingCapture` one-frame-delayed
     * pattern (set in `show()` / state-change; consumed at start of the next
     * `render()` after the actual scene was drawn).
     */
    fun capture(name: String) {
        if (!Constants.CAPTURE_CHECKPOINTS) return  // cheap no-op when disabled

        val sanitized = sanitize(name)
        // Guard the framebuffer probe + write — we never want a capture to
        // crash the smoke autopilot. ScreenUtils.getFrameBufferPixmap requires
        // a live GL context; in the unlikely event Gdx.graphics is null (e.g.
        // headless test harness flips the flag on) we still want to swallow.
        var pixmap: Pixmap? = null
        try {
            @Suppress("DEPRECATION")  // mirrors ScreenshotWriter — pinned 1.x libGDX.
            pixmap = ScreenUtils.getFrameBufferPixmap(
                0, 0,
                Gdx.graphics.backBufferWidth,
                Gdx.graphics.backBufferHeight,
            )
            val dir = File(OUT_DIR_PATH)
            if (!dir.exists() && !dir.mkdirs()) {
                Gdx.app?.error("CheckpointCapture", "mkdirs failed for ${dir.absolutePath}")
                return
            }
            val out = File(dir, "$sanitized.png")
            PixmapIO.writePNG(FileHandle(out), pixmap)
            Gdx.app?.log("CheckpointCapture", "captured \"$name\" -> ${out.absolutePath}")
        } catch (t: Throwable) {
            try {
                Gdx.app?.error("CheckpointCapture", "Failed to capture \"$name\": ${t.message}")
            } catch (_: Throwable) {
                // Gdx.app may be null in tests; swallow.
            }
        } finally {
            try {
                pixmap?.dispose()
            } catch (_: Throwable) {
                // Tolerate double-dispose or mock disposal.
            }
        }
    }

    /**
     * Pure helper: sanitize a checkpoint name to a safe filename stem.
     *
     * - Empty input falls back to `"unnamed"`.
     * - Runs of non-`[A-Za-z0-9_-]` characters collapse to a single `_`.
     * - Already-safe names (alphanumeric + `-` + `_`) pass through unchanged.
     *
     * Exposed so tests can verify the contract without spinning up GL.
     */
    fun sanitize(name: String): String {
        val source = if (name.isBlank()) "unnamed" else name
        return UNSAFE_NAME.replace(source, "_")
    }
}

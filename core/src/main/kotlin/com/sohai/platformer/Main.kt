package com.sohai.platformer

import com.badlogic.gdx.Game
import com.kotcrab.vis.ui.VisUI
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.ProceduralMusicGenerator
import com.sohai.platformer.audio.ProceduralSoundGenerator
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.persist.CrashReporter
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.rendering.DisplayScale
import com.sohai.platformer.screens.SplashScreen

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms. */
class Main : Game() {
    override fun create() {
        // T-115: Install the crash dumper before any other init runs, so any
        // exception thrown during the rest of create() still produces a file.
        installCrashHandler()

        // Compute font/sprite DPI scale from the actual physical window size.
        // Must happen before any FontManager or SpriteFactory calls.
        DisplayScale.init()

        VisUI.load(VisUI.SkinScale.X2)

        val smokeLevel = System.getProperty("cloudy.smokeLevel")
        if (smokeLevel != null) {
            // Smoke path: do all preload work synchronously here so the smoke
            // matrix doesn't pay a per-job splash tax. Skip the splash entirely.
            ProceduralSoundGenerator.generateAll()
            ProceduralMusicGenerator.generateAll()
            SoundManager.init()

            val level = com.sohai.platformer.levels.LevelManager.getLevel(smokeLevel)
            if (level != null) {
                setScreen(com.sohai.platformer.screens.GameScreen(level, this))
            } else {
                com.badlogic.gdx.Gdx.app.error("Main", "Unknown smoke level: $smokeLevel")
                com.badlogic.gdx.Gdx.app.exit()
            }
        } else {
            // T-104: cold-start splash drives preload one frame at a time so the
            // user sees a real progress bar. SplashScreen itself fast-skips the
            // 1-second minimum-duration gate when Constants.SMOKE_MODE is true.
            setScreen(SplashScreen(this))
        }
    }

    override fun resize(width: Int, height: Int) {
        // Re-compute scale and regenerate font cache whenever the window is resized
        // (e.g. user changes resolution from Settings, or enters/leaves fullscreen).
        DisplayScale.init()
        FontManager.clearSharedCache()
        super.resize(width, height)   // forwards to current screen
    }

    override fun dispose() {
        super.dispose()
        MusicManager.dispose()
        SoundManager.dispose()
        FontManager.disposeShared()
        VisUI.dispose()
    }

    /**
     * T-112: libGDX fires [pause] when the desktop window loses focus (alt-tab,
     * minimise, another window stealing focus). Forward to the active screen so
     * a [GameScreen] can raise its pause overlay (see [GameScreen.pause]).
     *
     * Smoke mode (`Constants.SMOKE_MODE`) skips auto-pause entirely — the smoke
     * autopilot must never lose its render tick to an unexpected overlay.
     */
    override fun pause() {
        if (Constants.SMOKE_MODE) return
        // Default Game.pause() forwards to the current screen's pause().
        super.pause()
    }

    /**
     * T-112: libGDX fires [resume] when the window regains focus. We
     * intentionally do **not** call `super.resume()` — the pause overlay must
     * stay up until the player explicitly dismisses it, even if the OS thinks
     * the window came back. The overlay's own input handlers drive un-pause.
     */
    override fun resume() {
        // Intentionally no super.resume() — overlay persists across focus regain.
    }

    /**
     * T-115: Wire `Thread.setDefaultUncaughtExceptionHandler` to dump a crash report
     * to `<userHome>/.cloudy-ninja/crashes/crash-{yyyyMMdd-HHmmss}.log`.
     *
     * In smoke mode (`Constants.SMOKE_MODE` is true), we log to stderr only — CI must
     * not write to the runner's home directory and we want the smoke matrix output to
     * stay self-contained.
     */
    private fun installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val osInfo = CrashReporter.currentOsInfo()
                val jdkInfo = CrashReporter.currentJdkInfo()
                val slotMetadata = collectSlotMetadata()
                val report = CrashReporter.format(
                    throwable = throwable,
                    gameVersion = Constants.BUILD_VERSION,
                    osInfo = osInfo,
                    jdkInfo = jdkInfo,
                    slotMetadata = slotMetadata,
                )

                if (Constants.SMOKE_MODE) {
                    // Smoke path: stderr only, no file write.
                    System.err.println("=== Cloudy Ninja crash (smoke mode, file write skipped) ===")
                    System.err.println("Thread: ${thread.name}")
                    System.err.println(report)
                } else {
                    val written = CrashReporter.writeCrashFile(report)
                    if (written != null) {
                        System.err.println("Cloudy Ninja crashed. Crash report: ${written.absolutePath}")
                    } else {
                        // Fall back to stderr if we couldn't create the dir / write the file.
                        System.err.println("=== Cloudy Ninja crash (file write failed) ===")
                        System.err.println("Thread: ${thread.name}")
                        System.err.println(report)
                    }
                }
            } catch (inner: Throwable) {
                // Never let the handler itself throw — that would mask the real crash.
                System.err.println("CrashReporter handler failed: ${inner.message}")
                throwable.printStackTrace(System.err)
            }
        }
    }

    /**
     * Snapshot save-slot metadata (slot index + completedLevels count) for the crash log.
     *
     * **PII:** Only the index and count cross the boundary. We never embed save contents
     * (character names, etc.) in the crash report.
     */
    private fun collectSlotMetadata(): List<CrashReporter.SlotMetadata> {
        // Mirrors `MainMenuScreen.SLOT_FILES` / `StatsScreen.STATS_SLOT_FILES`.
        val slotFiles = listOf("save_slot_0.json", "save_slot_1.json", "save_slot_2.json")
        return slotFiles.mapIndexed { index, filename ->
            val count = try {
                if (SaveManager.hasSave(filename)) {
                    SaveManager.loadGame(filename).completedLevels.size
                } else null
            } catch (t: Throwable) {
                null
            }
            CrashReporter.SlotMetadata(slotIndex = index, completedLevelCount = count)
        }
    }
}

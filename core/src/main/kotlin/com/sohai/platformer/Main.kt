package com.sohai.platformer

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.kotcrab.vis.ui.VisUI
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.ProceduralMusicGenerator
import com.sohai.platformer.audio.ProceduralSoundGenerator
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.input.GlobalInputRouter
import com.sohai.platformer.input.InputManager
import com.sohai.platformer.persist.CrashReporter
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.rendering.DisplayScale
import com.sohai.platformer.screens.SplashScreen
import com.sohai.platformer.util.ScreenshotWriter

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

        // T-171 (Phase A): install the global input router and register the
        // F12 + M-key handlers BEFORE the first setScreen() call. libGDX's
        // setScreen invokes screen.show(), and the screen will set
        // Gdx.input.inputProcessor (legacy path for unmigrated screens, or
        // router.pushScreen for MainMenu). If we installed AFTER setScreen,
        // we'd clobber GameScreen's hud.stage in the smoke path. Migrated
        // screens (MainMenu) re-install the router in their own show(), so
        // pre-installing here doesn't conflict with them.
        GlobalInputRouter.install()
        GlobalInputRouter.register(object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.F12) {
                    captureF12Screenshot()
                }
                // Don't consume — other globals (and other future router
                // adapters) should still see F12 if they care.
                return false
            }
        })
        GlobalInputRouter.register(object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                // InputManager.isMuteKey reads the cached (rebindable) keybind
                // so a user changing the mapping via Settings + reloadKeybinds()
                // takes effect immediately without re-registering this adapter.
                if (InputManager.isMuteKey(keycode)) {
                    InputManager.performMuteToggle()
                }
                return false
            }
        })

        val smokeLevel = System.getProperty("cloudy.smokeLevel")
        if (smokeLevel != null) {
            // Smoke path: do all preload work synchronously here so the smoke
            // matrix doesn't pay a per-job splash tax. Skip the splash entirely.
            ProceduralSoundGenerator.generateAll()
            ProceduralMusicGenerator.generateAll()
            SoundManager.init()
            // T-129: open the audio gate immediately for smoke runs. The
            // splash screen would normally do this on first user gesture, but
            // smoke bypasses the splash entirely, so we release here so the
            // smoke autopilot's GameScreen can call MusicManager.play() as it
            // did before T-129.
            MusicManager.releaseAudioGate()

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

    /**
     * Global per-frame hook for hotkeys that must work regardless of which
     * [com.badlogic.gdx.Screen] currently owns `Gdx.input.inputProcessor`.
     *
     * **T-171 (Phase A):** the F12 and M-key hotkeys are now ALSO wired
     * through [GlobalInputRouter] via [InputAdapter]s registered in [create].
     * The polling here is the legacy fallback for screens that haven't been
     * migrated to the router yet — each poll is gated on
     * `!GlobalInputRouter.isActive()` so migrated screens (currently just
     * MainMenuScreen) don't double-fire. Phase B (T-172) deletes this hook
     * once every screen cooperates with the router.
     *
     *  - **T-147 F12 screenshot:** capture the current framebuffer to a PNG
     *    under `~/.cloudy-ninja/screenshots/`. Smoke mode skipped inside
     *    [ScreenshotWriter.captureManual] AND short-circuited here so we
     *    never even probe the keypress under SMOKE_MODE.
     *  - **T-118 M-key mute:** flip [com.sohai.platformer.persist.Settings.muted],
     *    propagate to [MusicManager] / [SoundManager], log a `[MUTED]` line.
     *    The mute flag is the same one the Audio "Mute all" checkbox drives
     *    (T-105), so the keybind and the checkbox stay in lock-step.
     */
    override fun render() {
        // T-171 (Phase A): gate the legacy F12 poll on `!GlobalInputRouter.isActive()`.
        // When a migrated screen owns the router (MainMenu only in Phase A),
        // the router's F12 InputAdapter fires via libGDX's normal event loop;
        // we must NOT poll here too or F12 captures twice in one frame.
        // Unmigrated screens leave the router inactive, isActive() returns
        // false, and the legacy poll fires as before.
        if (!GlobalInputRouter.isActive() &&
            !Constants.SMOKE_MODE &&
            Gdx.input.isKeyJustPressed(Input.Keys.F12)
        ) {
            captureF12Screenshot()
        }
        // T-118: master-mute hotkey (default M). Toast is log-only — no global
        // Stage exists at the [Main] level; a visual toast would require the
        // same cross-Stage refactor T-147 also opted out of.
        //
        // T-171 (Phase A): InputManager.pollMuteHotkey() itself self-gates on
        // GlobalInputRouter.isActive(), so it's safe to keep this unconditional
        // call. The router's M-key adapter handles the action on migrated
        // screens; the poll handles it on the rest.
        InputManager.pollMuteHotkey()
        super.render()
    }

    /**
     * T-147 / T-171: capture the current framebuffer to a PNG via
     * [ScreenshotWriter]. Extracted as a helper so both trigger paths — the
     * legacy [render] poll and the [GlobalInputRouter]-registered keyDown
     * adapter wired up in [create] — share one implementation. Single source
     * of truth for the action; two trigger paths.
     *
     * Smoke mode early-outs because [ScreenshotWriter.captureManual] itself
     * skips under SMOKE_MODE and we don't even want to probe the GL state on
     * smoke runs. The router-driven path won't hit this in smoke (smoke skips
     * the splash, jumps straight to GameScreen, which is unmigrated and
     * leaves the router inactive) but the guard is defensive and free.
     *
     * All failures are swallowed and logged — a screenshot attempt must never
     * crash the render loop.
     */
    private fun captureF12Screenshot() {
        if (Constants.SMOKE_MODE) return
        try {
            val screenName = screen?.javaClass?.simpleName ?: "unknown"
            val ok = ScreenshotWriter.captureManual(screenName)
            if (ok) {
                Gdx.app?.log("T-147", "${ScreenshotWriter.TOAST_TEXT} ($screenName)")
            }
            // On failure ScreenshotWriter already logged via Gdx.app.error.
        } catch (t: Throwable) {
            // Defensive: never let a screenshot attempt take down the render loop.
            Gdx.app?.error("T-147", "F12 screenshot handler crashed: ${t.message}")
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

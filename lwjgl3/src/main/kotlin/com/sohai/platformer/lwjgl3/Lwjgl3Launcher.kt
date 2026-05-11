@file:JvmName("Lwjgl3Launcher")

package com.sohai.platformer.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.sohai.platformer.Constants
import com.sohai.platformer.Main
import com.sohai.platformer.persist.Settings
import kotlinx.serialization.json.Json

/** Launches the desktop (LWJGL3) application. */
fun main() {
    if (StartupHelper.startNewJvmIfRequired()) return

    // Read saved display prefs BEFORE Gdx is initialised so the OS window is
    // created at the correct size / fullscreen mode immediately (no flash).
    val savedSettings = readPreLaunchSettings()

    Lwjgl3Application(Main(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("Cloudy Ninja")
        useVsync(true)
        setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)

        if (savedSettings.fullscreen) {
            setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
        } else {
            val w = savedSettings.displayWidth.coerceAtLeast(Constants.VIRTUAL_WIDTH.toInt())
            val h = savedSettings.displayHeight.coerceAtLeast(Constants.VIRTUAL_HEIGHT.toInt())
            setWindowedMode(w, h)
        }

        setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
    })
}

/**
 * Reads `settings.json` from the working directory (where libGDX local files live
 * on desktop) without requiring Gdx to be initialised.  Falls back to defaults on
 * any error so a missing / corrupt file never prevents launch.
 */
private fun readPreLaunchSettings(): Settings {
    return try {
        val file = java.io.File("settings.json")
        if (file.exists()) {
            Json { ignoreUnknownKeys = true }.decodeFromString<Settings>(file.readText())
        } else {
            Settings()
        }
    } catch (e: Exception) {
        System.err.println("[Lwjgl3Launcher] Could not read settings.json: ${e.message} — using defaults")
        Settings()
    }
}

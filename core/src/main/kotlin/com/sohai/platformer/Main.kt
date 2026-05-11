package com.sohai.platformer

import com.badlogic.gdx.Game
import com.kotcrab.vis.ui.VisUI
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.ProceduralMusicGenerator
import com.sohai.platformer.audio.ProceduralSoundGenerator
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.rendering.DisplayScale
import com.sohai.platformer.screens.MainMenuScreen

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms. */
class Main : Game() {
    override fun create() {
        // Compute font/sprite DPI scale from the actual physical window size.
        // Must happen before any FontManager or SpriteFactory calls.
        DisplayScale.init()

        VisUI.load(VisUI.SkinScale.X2)
        ProceduralSoundGenerator.generateAll()
        ProceduralMusicGenerator.generateAll()
        SoundManager.init()
        val smokeLevel = System.getProperty("cloudy.smokeLevel")
        if (smokeLevel != null) {
            val level = com.sohai.platformer.levels.LevelManager.getLevel(smokeLevel)
            if (level != null) {
                setScreen(com.sohai.platformer.screens.GameScreen(level, this))
            } else {
                com.badlogic.gdx.Gdx.app.error("Main", "Unknown smoke level: $smokeLevel")
                com.badlogic.gdx.Gdx.app.exit()
            }
        } else {
            setScreen(MainMenuScreen(this))
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
}

package com.sohai.platformer

import com.badlogic.gdx.Game
import com.kotcrab.vis.ui.VisUI
import com.sohai.platformer.audio.ProceduralSoundGenerator
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.screens.MainMenuScreen

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms. */
class Main : Game() {
    override fun create() {
        VisUI.load(VisUI.SkinScale.X2)
        ProceduralSoundGenerator.generateAll()
        SoundManager.init()
        setScreen(MainMenuScreen(this))
    }

    override fun dispose() {
        super.dispose()
        SoundManager.dispose()
        VisUI.dispose()
    }
}

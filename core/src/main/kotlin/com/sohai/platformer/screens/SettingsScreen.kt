package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.persist.SettingsManager

class SettingsScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private var settings = SettingsManager.load()

    private val titleFont   = FontManager.getShared(32)
    private val sectionFont = FontManager.getShared(20)

    init {
        Gdx.input.inputProcessor = stage

        val skin = VisUI.getSkin()
        val titleStyle   = Label.LabelStyle(titleFont,   com.badlogic.gdx.graphics.Color(0.3f, 1f, 0.55f, 1f))
        val sectionStyle = Label.LabelStyle(sectionFont, com.badlogic.gdx.graphics.Color(0.7f, 0.85f, 0.75f, 1f))

        val root = VisTable()
        root.setFillParent(true)
        root.top().padTop(40f)

        root.add(Label("SETTINGS", titleStyle)).padBottom(24f).row()

        val inner = VisTable()
        inner.top().left().pad(10f)

        // ── Audio ──────────────────────────────────────────────────────────
        inner.add(Label("Audio", sectionStyle)).left().padBottom(8f).row()

        inner.add(VisLabel("Music Volume")).left().padRight(16f)
        val sliderMusic = Slider(0f, 1f, 0.05f, false, skin)
        sliderMusic.value = settings.volMusic
        sliderMusic.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volMusic = sliderMusic.value) }
            }
        })
        inner.add(sliderMusic).width(260f).row()

        inner.add(VisLabel("SFX Volume")).left().padRight(16f)
        val sliderSfx = Slider(0f, 1f, 0.05f, false, skin)
        sliderSfx.value = settings.volSfx
        sliderSfx.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volSfx = sliderSfx.value) }
                SoundManager.setVolume(sliderSfx.value)
            }
        })
        inner.add(sliderSfx).width(260f).padBottom(16f).row()

        // ── Visual / Feel ─────────────────────────────────────────────────
        inner.add(Label("Visual / Feel", sectionStyle)).left().padBottom(8f).row()

        val chkShake = CheckBox(" Screen Shake", skin)
        chkShake.isChecked = settings.screenShake
        chkShake.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(screenShake = chkShake.isChecked) }
            }
        })
        inner.add(chkShake).left().row()

        val chkFlash = CheckBox(" Death Flash", skin)
        chkFlash.isChecked = settings.deathFlash
        chkFlash.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(deathFlash = chkFlash.isChecked) }
            }
        })
        inner.add(chkFlash).left().row()

        val chkFps = CheckBox(" Show FPS (console)", skin)
        chkFps.isChecked = settings.showFps
        chkFps.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(showFps = chkFps.isChecked) }
            }
        })
        inner.add(chkFps).left().padBottom(16f).row()

        // ── Assist Mode ───────────────────────────────────────────────────
        inner.add(Label("Assist Mode", sectionStyle)).left().padBottom(8f).row()
        inner.add(VisLabel("Accessibility options — relax the challenge as needed.")).left().padBottom(6f).row()

        val chkInfiniteSpirits = CheckBox(" Infinite Spirits (no game over)", skin)
        chkInfiniteSpirits.isChecked = settings.assistInfiniteSpirits
        chkInfiniteSpirits.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistInfiniteSpirits = chkInfiniteSpirits.isChecked) }
            }
        })
        inner.add(chkInfiniteSpirits).left().row()

        val chkInvincible = CheckBox(" Invincible (no damage)", skin)
        chkInvincible.isChecked = settings.assistInvincible
        chkInvincible.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistInvincible = chkInvincible.isChecked) }
            }
        })
        inner.add(chkInvincible).left().padBottom(16f).row()

        inner.add(VisLabel("Slow Speed")).left().padRight(16f)
        val sliderSpeed = Slider(0.25f, 1f, 0.05f, false, skin)
        sliderSpeed.value = settings.assistSlowSpeed
        sliderSpeed.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistSlowSpeed = sliderSpeed.value) }
            }
        })
        inner.add(sliderSpeed).width(260f).row()

        // Scroll pane so the content isn't clipped on small windows
        val scroll = VisScrollPane(inner)
        scroll.setFlickScroll(false)
        root.add(scroll).expand().fill().padTop(10f).padBottom(10f).row()

        // Back button
        val btnBack = VisTextButton("Back")
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                dispose()
            }
        })
        root.add(btnBack).size(200f, 55f).padBottom(30f).row()

        stage.addActor(root)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.10f, 0.14f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
    override fun show() {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
    }
}

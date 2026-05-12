package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.input.InputManager
import com.sohai.platformer.persist.ColorBlindMode
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.persist.defaultKeybinds
import com.sohai.platformer.rendering.DisplayScale

class SettingsScreen(
    private val game: Game,
    private val currentState: GameState = GameState()
) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private var settings = SettingsManager.load()

    private val titleFont   = FontManager.getShared(32)
    private val sectionFont = FontManager.getShared(20)
    private val bodyFont    = FontManager.getShared(16)

    /** Toast feedback label shown after save/load/delete actions */
    private val toastLabel = Label("", Label.LabelStyle(bodyFont, Color(0.3f, 1f, 0.55f, 1f)))
    private var toastTimer = 0f

    companion object {
        private const val SAVE_SLOT = "save_slot_0.json"
        private const val TOAST_DURATION = 2f
    }

    init {
        Gdx.input.inputProcessor = stage

        val skin = VisUI.getSkin()
        val titleStyle   = Label.LabelStyle(titleFont,   Color(0.3f, 1f, 0.55f, 1f))
        val sectionStyle = Label.LabelStyle(sectionFont, Color(0.7f, 0.85f, 0.75f, 1f))
        val bodyStyle    = Label.LabelStyle(bodyFont,    Color(0.82f, 0.88f, 0.82f, 1f))

        val root = VisTable()
        root.setFillParent(true)
        root.top().padTop(40f)

        root.add(Label("SETTINGS", titleStyle)).padBottom(24f).row()

        val inner = VisTable()
        inner.top().left().pad(10f)

        // ── Display ───────────────────────────────────────────────────────
        inner.add(Label("Display", sectionStyle)).left().padBottom(8f).row()

        // Resolution presets  (width × height — physical pixels)
        data class ResPreset(val label: String, val w: Int, val h: Int) {
            override fun toString() = label
        }
        val resPresets = listOf(
            ResPreset("1280 × 720  (HD)",          1280,  720),
            ResPreset("1920 × 1080  (Full HD)",     1920, 1080),
            ResPreset("2560 × 1440  (2K / QHD)",   2560, 1440),
            ResPreset("3840 × 2160  (4K / UHD)",   3840, 2160)
        )

        inner.add(Label("Resolution", bodyStyle)).left().padRight(16f)
        val resBox = SelectBox<ResPreset>(skin)
        val resItems = GdxArray<ResPreset>()
        resPresets.forEach { resItems.add(it) }
        resBox.items = resItems
        // Pre-select the item matching the saved resolution
        val savedPreset = resPresets.firstOrNull {
            it.w == settings.displayWidth && it.h == settings.displayHeight
        } ?: resPresets[0]
        resBox.selected = savedPreset
        resBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val p = resBox.selected ?: return
                settings = SettingsManager.update { it.copy(displayWidth = p.w, displayHeight = p.h) }
                if (!settings.fullscreen) applyDisplaySettings()
            }
        })
        inner.add(resBox).width(300f).row()

        val chkFullscreen = CheckBox(" Fullscreen", skin)
        chkFullscreen.isChecked = settings.fullscreen
        chkFullscreen.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(fullscreen = chkFullscreen.isChecked) }
                applyDisplaySettings()
            }
        })
        inner.add(chkFullscreen).left().padBottom(4f).row()

        inner.add(Label("Sprites sharpen fully at next launch.", bodyStyle)).left()
            .padBottom(16f).row()

        // ── Audio ──────────────────────────────────────────────────────────
        inner.add(Label("Audio", sectionStyle)).left().padBottom(8f).row()

        inner.add(Label("Music", bodyStyle)).left().padRight(16f)
        val sliderMusic = Slider(0f, 1f, 0.05f, false, skin)
        sliderMusic.value = settings.volMusic
        sliderMusic.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volMusic = sliderMusic.value) }
                MusicManager.setMusicVolume(sliderMusic.value)
            }
        })
        inner.add(sliderMusic).width(260f).row()

        inner.add(Label("SFX", bodyStyle)).left().padRight(16f)
        val sliderSfx = Slider(0f, 1f, 0.05f, false, skin)
        sliderSfx.value = settings.volSfx
        sliderSfx.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volSfx = sliderSfx.value) }
                SoundManager.setVolume(sliderSfx.value)
            }
        })
        inner.add(sliderSfx).width(260f).row()

        inner.add(Label("UI", bodyStyle)).left().padRight(16f)
        val sliderUi = Slider(0f, 1f, 0.05f, false, skin)
        sliderUi.value = settings.volUi
        sliderUi.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volUi = sliderUi.value) }
                SoundManager.setUiVolume(sliderUi.value)
            }
        })
        inner.add(sliderUi).width(260f).padBottom(16f).row()

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

        // ── Accessibility ─────────────────────────────────────────────────
        inner.add(Label("Accessibility", sectionStyle)).left().padBottom(8f).row()

        inner.add(Label("Color-blind mode", bodyStyle)).left().padRight(16f)
        val cbBox = SelectBox<ColorBlindMode>(skin)
        val cbItems = GdxArray<ColorBlindMode>()
        ColorBlindMode.values().forEach { cbItems.add(it) }
        cbBox.items = cbItems
        cbBox.selected = settings.colorBlindMode
        cbBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val m = cbBox.selected ?: return
                settings = SettingsManager.update { it.copy(colorBlindMode = m) }
            }
        })
        inner.add(cbBox).width(300f).padBottom(16f).row()

        // ── Assist Mode ───────────────────────────────────────────────────
        inner.add(Label("Assist Mode", sectionStyle)).left().padBottom(8f).row()
        inner.add(Label("Accessibility options — relax the challenge as needed.", bodyStyle)).left().padBottom(6f).row()

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

        inner.add(Label("Slow Speed", bodyStyle)).left().padRight(16f)
        val sliderSpeed = Slider(0.25f, 1f, 0.05f, false, skin)
        sliderSpeed.value = settings.assistSlowSpeed
        sliderSpeed.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistSlowSpeed = sliderSpeed.value) }
            }
        })
        inner.add(sliderSpeed).width(260f).padBottom(16f).row()

        // ── Controls (Key Rebinding) ──────────────────────────────────────
        inner.add(Label("Controls", sectionStyle)).left().padTop(20f).padBottom(8f).row()

        val actionNames = listOf("left", "right", "jump", "action", "swap")
        val displayNames = mapOf(
            "left" to "Move Left",
            "right" to "Move Right",
            "jump" to "Jump",
            "action" to "Action",
            "swap" to "Swap Character"
        )

        // Track buttons so we can update their text after rebind
        val keybindButtons = mutableMapOf<String, VisTextButton>()

        for (action in actionNames) {
            val row = VisTable()
            row.add(Label(displayNames[action] ?: action, bodyStyle)).left().width(180f).padRight(16f)

            val currentKey = settings.keybinds[action] ?: defaultKeybinds()[action] ?: -1
            val btn = VisTextButton(Input.Keys.toString(currentKey))
            keybindButtons[action] = btn

            btn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    // Enter capture mode: change button text and listen for next key
                    btn.setText("Press a key...")

                    // Add a stage listener that captures the next keyDown
                    stage.addListener(object : InputListener() {
                        override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                            // Update settings with new keybind
                            val newBinds = settings.keybinds.toMutableMap()
                            newBinds[action] = keycode
                            settings = SettingsManager.update { it.copy(keybinds = newBinds) }
                            InputManager.reloadKeybinds()

                            // Update button text to show the new key
                            btn.setText(Input.Keys.toString(keycode))

                            // Remove this listener (capture complete)
                            stage.removeListener(this)
                            return true
                        }
                    })
                }
            })

            row.add(btn).width(160f).height(36f)
            inner.add(row).left().padBottom(4f).row()
        }

        // "Reset to Defaults" button
        val btnResetKeys = VisTextButton("Reset to Defaults")
        btnResetKeys.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(keybinds = defaultKeybinds()) }
                InputManager.reloadKeybinds()
                // Refresh all button labels
                for (action in actionNames) {
                    val keycode = settings.keybinds[action] ?: -1
                    keybindButtons[action]?.setText(Input.Keys.toString(keycode))
                }
                showToast("Controls reset!")
            }
        })
        inner.add(btnResetKeys).left().padTop(8f).padBottom(16f).row()

        // ── Save / Load / Delete ──────────────────────────────────────────
        inner.add(Label("Save Data", sectionStyle)).left().padTop(20f).padBottom(8f).row()

        val saveRow = VisTable()
        val btnSave = VisTextButton("Save")
        btnSave.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SaveManager.saveGame(currentState, SAVE_SLOT)
                showToast("Saved!")
            }
        })
        val btnLoad = VisTextButton("Load")
        btnLoad.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SaveManager.loadGame(SAVE_SLOT)
                showToast("Loaded!")
            }
        })
        val btnDelete = VisTextButton("Delete")
        btnDelete.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SaveManager.deleteSave(SAVE_SLOT)
                showToast("Deleted!")
            }
        })
        saveRow.add(btnSave).size(120f, 48f).padRight(12f)
        saveRow.add(btnLoad).size(120f, 48f).padRight(12f)
        saveRow.add(btnDelete).size(120f, 48f)
        inner.add(saveRow).left().padBottom(8f).row()
        inner.add(toastLabel).left().padBottom(8f).row()

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

    /**
     * Applies the current [settings] display configuration at runtime.
     * libGDX calls [resize] on all active screens after the mode change so
     * viewports adapt automatically.  Fonts are regenerated on next use
     * (shared cache is cleared).  Sprites use the new scale on the NEXT
     * GameScreen construction.
     */
    private fun applyDisplaySettings() {
        if (settings.fullscreen) {
            val mode = Gdx.graphics.displayMode
            Gdx.graphics.setFullscreenMode(mode)
        } else {
            val w = settings.displayWidth.coerceAtLeast(1280)
            val h = settings.displayHeight.coerceAtLeast(720)
            Gdx.graphics.setWindowedMode(w, h)
        }
        // Fonts must be regenerated at the new physical scale
        DisplayScale.init()
        FontManager.clearSharedCache()
        showToast("Display updated — sprites sharpen at next launch")
    }

    private fun showToast(message: String) {
        toastLabel.setText(message)
        toastLabel.color = Color(0.3f, 1f, 0.55f, 1f)
        toastTimer = TOAST_DURATION
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.07f, 0.10f, 0.14f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (toastTimer > 0f) {
            toastTimer -= delta
            val alpha = (toastTimer / TOAST_DURATION).coerceIn(0f, 1f)
            toastLabel.color.a = alpha
            if (toastTimer <= 0f) {
                toastLabel.setText("")
                toastTimer = 0f
            }
        }

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

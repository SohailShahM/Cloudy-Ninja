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
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
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

    // T-145: Sound-test music auto-stop. A 3s [Timer.Task] is scheduled when the
    // player presses "Play Music (ambient_arid 3s)" so the music preview doesn't
    // keep looping after they move on. We hold a reference so [hide]/[dispose]
    // can cancel a still-pending stop if the player leaves Settings early — see
    // the cancel calls below.
    private var musicTestStopTask: Timer.Task? = null

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

        root.add(Label(Strings.get(StringKey.SETTINGS_TITLE), titleStyle)).padBottom(24f).row()

        val inner = VisTable()
        inner.top().left().pad(10f)

        // Helper: render a visually distinct section header with an underline.
        fun sectionHeader(text: String, topPad: Float = 0f) {
            inner.add(Label(text, sectionStyle))
                .left().padTop(topPad).padBottom(4f).row()
            inner.add(Separator()).left().fillX().width(560f).padBottom(10f).row()
        }

        // ══════════════════════════════════════════════════════════════════
        // SECTION 1 — DISPLAY
        // ══════════════════════════════════════════════════════════════════
        sectionHeader(Strings.get(StringKey.SETTINGS_DISPLAY))

        // Resolution presets  (width × height — physical pixels)
        data class ResPreset(val label: String, val w: Int, val h: Int) {
            override fun toString() = label
        }
        val resPresets = listOf(
            ResPreset(Strings.get(StringKey.SETTINGS_RES_720),  1280,  720),
            ResPreset(Strings.get(StringKey.SETTINGS_RES_1080), 1920, 1080),
            ResPreset(Strings.get(StringKey.SETTINGS_RES_1440), 2560, 1440),
            ResPreset(Strings.get(StringKey.SETTINGS_RES_2160), 3840, 2160)
        )

        inner.add(Label(Strings.get(StringKey.SETTINGS_RESOLUTION), bodyStyle)).left().padRight(16f)
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

        val chkFullscreen = CheckBox(Strings.get(StringKey.SETTINGS_FULLSCREEN), skin)
        chkFullscreen.isChecked = settings.fullscreen
        chkFullscreen.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(fullscreen = chkFullscreen.isChecked) }
                applyDisplaySettings()
            }
        })
        inner.add(chkFullscreen).left().padBottom(4f).row()

        inner.add(Label(Strings.get(StringKey.SETTINGS_SPRITES_HINT), bodyStyle)).left()
            .padBottom(8f).row()

        val chkFps = CheckBox(Strings.get(StringKey.SETTINGS_SHOW_FPS), skin)
        chkFps.isChecked = settings.showFps
        chkFps.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(showFps = chkFps.isChecked) }
            }
        })
        inner.add(chkFps).left().padBottom(4f).row()

        // T-144: Camera look-ahead toggle. Default ON — players who prefer
        // a static, perfectly-centred camera can switch it off here.
        // Independent of the screen-shake toggle in Accessibility; both
        // offsets coexist when both are on.
        val chkCameraLookAhead = CheckBox(Strings.get(StringKey.SETTINGS_CAMERA_LOOK_AHEAD), skin)
        chkCameraLookAhead.isChecked = settings.cameraLookAhead
        chkCameraLookAhead.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(cameraLookAhead = chkCameraLookAhead.isChecked) }
            }
        })
        inner.add(chkCameraLookAhead).left().padBottom(20f).row()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 2 — AUDIO
        // ══════════════════════════════════════════════════════════════════
        sectionHeader(Strings.get(StringKey.SETTINGS_AUDIO), topPad = 12f)

        // T-105: Master volume slider + mute toggle. Sits ABOVE the existing
        // Music/SFX/UI sliders. Effective volume for each bus becomes
        // `volMaster * volBus` and is gated to 0 by [muted] without altering
        // the slider position so unmute restores. T-118 reuses the same flag
        // for the M-keybind transient mute.
        inner.add(Label(Strings.get(StringKey.SETTINGS_MASTER), bodyStyle)).left().padRight(16f)
        val masterRow = VisTable()
        val sliderMaster = Slider(0f, 1f, 0.05f, false, skin)
        sliderMaster.value = settings.volMaster
        val chkMute = CheckBox(Strings.get(StringKey.SETTINGS_MUTE), skin)
        chkMute.isChecked = settings.muted
        sliderMaster.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volMaster = sliderMaster.value) }
                MusicManager.setMasterVolume(sliderMaster.value)
                SoundManager.setMasterVolume(sliderMaster.value)
            }
        })
        chkMute.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val m = chkMute.isChecked
                settings = SettingsManager.update { it.copy(muted = m) }
                MusicManager.setMuted(m)
                SoundManager.setMuted(m)
            }
        })
        masterRow.add(sliderMaster).width(260f).padRight(16f)
        masterRow.add(chkMute).left()
        inner.add(masterRow).left().row()

        inner.add(Label(Strings.get(StringKey.SETTINGS_MUSIC), bodyStyle)).left().padRight(16f)
        val sliderMusic = Slider(0f, 1f, 0.05f, false, skin)
        sliderMusic.value = settings.volMusic
        sliderMusic.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volMusic = sliderMusic.value) }
                MusicManager.setMusicVolume(sliderMusic.value)
            }
        })
        inner.add(sliderMusic).width(260f).row()

        inner.add(Label(Strings.get(StringKey.SETTINGS_SFX), bodyStyle)).left().padRight(16f)
        val sliderSfx = Slider(0f, 1f, 0.05f, false, skin)
        sliderSfx.value = settings.volSfx
        sliderSfx.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volSfx = sliderSfx.value) }
                SoundManager.setVolume(sliderSfx.value)
            }
        })
        inner.add(sliderSfx).width(260f).row()

        inner.add(Label(Strings.get(StringKey.SETTINGS_UI_VOLUME), bodyStyle)).left().padRight(16f)
        val sliderUi = Slider(0f, 1f, 0.05f, false, skin)
        sliderUi.value = settings.volUi
        sliderUi.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(volUi = sliderUi.value) }
                SoundManager.setUiVolume(sliderUi.value)
            }
        })
        inner.add(sliderUi).width(260f).padBottom(12f).row()

        // ── Sound Test (T-145) ────────────────────────────────────────────
        // Three one-shot buttons under the per-bus sliders. Each plays a
        // single sample through the relevant manager at the *current* slider
        // values so the player can verify their volume choices without
        // entering gameplay. If [Settings.muted] is true (mute checkbox above
        // or M-key from T-118) all three correctly play silently — that's
        // expected; the mute checkbox is the visible feedback.
        //
        // - UI Click: routes through [SoundManager.playUi] using the UI bus.
        //   The "ui_click" id has no registered sample (T-035 plumbed the bus
        //   but no UI click asset was added); [SoundManager.playUi] handles
        //   unknown ids gracefully with a log line, so the button is still a
        //   valid bus check.
        // - SFX jump: plays the canonical "jump" sample on the SFX bus.
        // - Music ambient_arid: starts the loop (fading in) and schedules a
        //   3-second stop. The task is tracked on [musicTestStopTask] so
        //   leaving Settings cancels it (see [hide]/[dispose]) — otherwise a
        //   [Timer.Task] would survive the screen transition and stop music
        //   the player started afterwards.
        inner.add(Label(Strings.get(StringKey.SETTINGS_SOUND_TEST_HEADING), bodyStyle))
            .left().padBottom(6f).row()

        val soundTestRow = VisTable()
        val btnTestUi = VisTextButton(Strings.get(StringKey.SETTINGS_TEST_UI_CLICK))
        btnTestUi.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SoundManager.playUi("ui_click")
            }
        })
        val btnTestSfx = VisTextButton(Strings.get(StringKey.SETTINGS_TEST_SFX_JUMP))
        btnTestSfx.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SoundManager.play("jump")
            }
        })
        val btnTestMusic = VisTextButton(Strings.get(StringKey.SETTINGS_TEST_MUSIC_AMBIENT))
        btnTestMusic.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                // Cancel any prior pending stop before starting a new preview
                // so two quick presses don't race and cut the second sample
                // short at the first task's 3s mark.
                musicTestStopTask?.cancel()
                MusicManager.play("ambient_arid", fadeIn = true)
                musicTestStopTask = Timer.schedule(
                    object : Timer.Task() {
                        override fun run() { MusicManager.stop() }
                    },
                    3f
                )
            }
        })
        soundTestRow.add(btnTestUi).padRight(8f)
        soundTestRow.add(btnTestSfx).padRight(8f)
        soundTestRow.add(btnTestMusic)
        inner.add(soundTestRow).left().padBottom(20f).row()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 3 — CONTROLS
        // ══════════════════════════════════════════════════════════════════
        sectionHeader(Strings.get(StringKey.SETTINGS_CONTROLS), topPad = 12f)

        val actionNames = listOf("left", "right", "jump", "action", "swap", "restart", "mute")
        val displayNames = mapOf(
            "left" to Strings.get(StringKey.SETTINGS_MOVE_LEFT),
            "right" to Strings.get(StringKey.SETTINGS_MOVE_RIGHT),
            "jump" to Strings.get(StringKey.SETTINGS_JUMP),
            "action" to Strings.get(StringKey.SETTINGS_ACTION),
            "swap" to Strings.get(StringKey.SETTINGS_SWAP_CHARACTER),
            // T-133: rebindable quick-restart hotkey (held 0.5s in-game).
            "restart" to Strings.get(StringKey.SETTINGS_RESTART_LEVEL),
            // T-118: master mute hotkey (default M). Toggles Settings.muted —
            // the same flag the Audio "Mute all" checkbox drives, so the two
            // controls stay in lock-step automatically.
            "mute" to Strings.get(StringKey.SETTINGS_MUTE_TOGGLE)
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
                    btn.setText(Strings.get(StringKey.SETTINGS_PRESS_KEY))

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
        val btnResetKeys = VisTextButton(Strings.get(StringKey.SETTINGS_RESET_DEFAULTS))
        btnResetKeys.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(keybinds = defaultKeybinds()) }
                InputManager.reloadKeybinds()
                // Refresh all button labels
                for (action in actionNames) {
                    val keycode = settings.keybinds[action] ?: -1
                    keybindButtons[action]?.setText(Input.Keys.toString(keycode))
                }
                showToast(Strings.get(StringKey.SETTINGS_CONTROLS_RESET))
            }
        })
        inner.add(btnResetKeys).left().padTop(8f).padBottom(20f).row()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 4 — ACCESSIBILITY
        // ══════════════════════════════════════════════════════════════════
        sectionHeader(Strings.get(StringKey.SETTINGS_ACCESSIBILITY), topPad = 12f)

        inner.add(Label(Strings.get(StringKey.SETTINGS_COLORBLIND_MODE), bodyStyle)).left().padRight(16f)
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
        inner.add(cbBox).width(300f).padBottom(8f).row()

        val chkReducedMotion = CheckBox(Strings.get(StringKey.SETTINGS_REDUCED_MOTION), skin)
        chkReducedMotion.isChecked = settings.reducedMotion
        chkReducedMotion.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(reducedMotion = chkReducedMotion.isChecked) }
            }
        })
        inner.add(chkReducedMotion).left().padBottom(8f).row()

        // T-132: high-contrast mode toggle. Independent from color-blind mode
        // — both can be on; the high-contrast palette overrides at the same
        // role when both apply (see HighContrastPalette docs).
        val chkHighContrast = CheckBox(Strings.get(StringKey.SETTINGS_HIGH_CONTRAST), skin)
        chkHighContrast.isChecked = settings.highContrast
        chkHighContrast.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(highContrast = chkHighContrast.isChecked) }
            }
        })
        inner.add(chkHighContrast).left().padBottom(8f).row()

        val chkShake = CheckBox(Strings.get(StringKey.SETTINGS_SCREEN_SHAKE), skin)
        chkShake.isChecked = settings.screenShake
        chkShake.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(screenShake = chkShake.isChecked) }
            }
        })
        inner.add(chkShake).left().row()

        val chkFlash = CheckBox(Strings.get(StringKey.SETTINGS_DEATH_FLASH), skin)
        chkFlash.isChecked = settings.deathFlash
        chkFlash.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(deathFlash = chkFlash.isChecked) }
            }
        })
        inner.add(chkFlash).left().padBottom(12f).row()

        // Assist Mode lives inside Accessibility — relaxes the challenge.
        inner.add(Label(Strings.get(StringKey.SETTINGS_ASSIST_MODE_HINT), bodyStyle)).left().padBottom(6f).row()

        val chkInfiniteSpirits = CheckBox(Strings.get(StringKey.SETTINGS_ASSIST_INFINITE_SPIRITS), skin)
        chkInfiniteSpirits.isChecked = settings.assistInfiniteSpirits
        chkInfiniteSpirits.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistInfiniteSpirits = chkInfiniteSpirits.isChecked) }
            }
        })
        inner.add(chkInfiniteSpirits).left().row()

        val chkInvincible = CheckBox(Strings.get(StringKey.SETTINGS_ASSIST_INVINCIBLE), skin)
        chkInvincible.isChecked = settings.assistInvincible
        chkInvincible.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistInvincible = chkInvincible.isChecked) }
            }
        })
        inner.add(chkInvincible).left().padBottom(16f).row()

        inner.add(Label(Strings.get(StringKey.SETTINGS_ASSIST_SLOW_SPEED), bodyStyle)).left().padRight(16f)
        val sliderSpeed = Slider(0.25f, 1f, 0.05f, false, skin)
        sliderSpeed.value = settings.assistSlowSpeed
        sliderSpeed.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings = SettingsManager.update { it.copy(assistSlowSpeed = sliderSpeed.value) }
            }
        })
        inner.add(sliderSpeed).width(260f).padBottom(20f).row()

        // ── Save / Load / Delete ──────────────────────────────────────────
        // Per the categorized-layout brief, save controls sit at the bottom
        // without their own section header — they're a footer action group.
        val saveRow = VisTable()
        val btnSave = VisTextButton(Strings.get(StringKey.SETTINGS_SAVE))
        btnSave.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SaveManager.saveGame(currentState, SAVE_SLOT)
                showToast(Strings.get(StringKey.SETTINGS_TOAST_SAVED))
            }
        })
        val btnLoad = VisTextButton(Strings.get(StringKey.SETTINGS_LOAD))
        btnLoad.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SaveManager.loadGame(SAVE_SLOT)
                showToast(Strings.get(StringKey.SETTINGS_TOAST_LOADED))
            }
        })
        val btnDelete = VisTextButton(Strings.get(StringKey.SETTINGS_DELETE))
        btnDelete.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                SaveManager.deleteSave(SAVE_SLOT)
                showToast(Strings.get(StringKey.SETTINGS_TOAST_DELETED))
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

        // Footer row — Credits (T-101) + Back
        val footer = VisTable()
        val btnCredits = VisTextButton(Strings.get(StringKey.SETTINGS_CREDITS))
        btnCredits.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = CreditsScreen(game)
                dispose()
            }
        })
        val btnBack = VisTextButton(Strings.get(StringKey.SETTINGS_BACK))
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                dispose()
            }
        })
        footer.add(btnCredits).size(200f, 55f).padRight(16f)
        footer.add(btnBack).size(200f, 55f)
        root.add(footer).padBottom(30f).row()

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
        showToast(Strings.get(StringKey.SETTINGS_TOAST_DISPLAY_UPDATED))
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

    /**
     * T-145: cancel any still-pending sound-test music stop when the player
     * leaves Settings. Without this a [Timer.Task] scheduled by the Music
     * test button would survive the screen transition and stop music the
     * player started afterwards (e.g. via Level Select → game start in
     * that 3-second window).
     */
    override fun hide() {
        musicTestStopTask?.cancel()
        musicTestStopTask = null
    }

    override fun dispose() {
        musicTestStopTask?.cancel()
        musicTestStopTask = null
        stage.dispose()
    }
}

package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.persist.SettingsManager

class PauseOverlay(
    private val onResume: () -> Unit,
    private val onRestart: () -> Unit,
    private val onMainMenu: () -> Unit,
    private val onTimeTrial: (() -> Unit)? = null,
    private val isCurrentlyTimeTrial: Boolean = false
) : Disposable {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    val stage = Stage(viewport)
    private val shapeRenderer = ShapeRenderer()
    // Use shared cache — disposed once at app shutdown, not per overlay instance.
    private val titleFont = FontManager.getShared(32)
    private val bodyFont = FontManager.getShared(20)
    private val hintFont = FontManager.getShared(14)

    // T-063 polish: fade-in state. Manual delta-based lerp keeps the
    // ShapeRenderer backdrop and Scene2D menu in lockstep with one variable.
    // 0 = invisible, 1 = fully visible. Targets reached at FADE_IN_SECONDS.
    private var fadeT = 0f
    private val FADE_IN_SECONDS = 0.2f
    private val DIM_TARGET_ALPHA = 0.55f

    init {
        val titleStyle = Label.LabelStyle(titleFont, Color.WHITE)

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label(Strings.get(StringKey.PAUSE_TITLE), titleStyle)).padBottom(30f).row()

        val btnResume = VisTextButton(Strings.get(StringKey.PAUSE_RESUME))
        btnResume.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onResume() }
        })
        table.add(btnResume).size(220f, 55f).padBottom(14f).row()

        val btnRestart = VisTextButton(Strings.get(StringKey.PAUSE_RESTART))
        btnRestart.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onRestart() }
        })
        table.add(btnRestart).size(220f, 55f).padBottom(14f).row()

        val btnMenu = VisTextButton(Strings.get(StringKey.PAUSE_MAIN_MENU))
        btnMenu.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onMainMenu() }
        })
        table.add(btnMenu).size(220f, 55f).row()

        // Time Trial button (always shown; label reflects current state)
        val trialLabel = if (isCurrentlyTimeTrial) Strings.get(StringKey.PAUSE_EXIT_TIME_TRIAL) else Strings.get(StringKey.PAUSE_ENTER_TIME_TRIAL)
        val btnTrial = VisTextButton(trialLabel)
        btnTrial.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onTimeTrial?.invoke() }
        })
        table.add(btnTrial).size(220f, 55f).padTop(20f).row()

        stage.addActor(table)

        // T-063 polish: "Press Esc to resume" hint, bottom-right of overlay.
        // Honor a rebound pause key if Settings.keybinds["pause"] is set
        // (currently no such default; ESC is hardcoded in GameScreen). The
        // lookup is read-only — future task can add a real keybind entry.
        val hintStyle = Label.LabelStyle(hintFont, Color(0.6f, 0.6f, 0.6f, 0.8f))
        val hintTable = VisTable()
        hintTable.setFillParent(true)
        hintTable.bottom().right()
        hintTable.add(Label("Press ${pauseKeyName()} to resume", hintStyle)).pad(12f)
        stage.addActor(hintTable)
    }

    /** Display name of the current pause key, falling back to "Esc". */
    private fun pauseKeyName(): String {
        val code = SettingsManager.load().keybinds["pause"] ?: -1
        return if (code > 0) Input.Keys.toString(code) else Strings.get(StringKey.PAUSE_KEY_ESC)
    }

    /**
     * T-063 polish: Reset the fade animation. Called by the host screen when
     * the pause overlay is about to be shown again, so the 0→1 fade plays
     * from scratch every time the player pauses. Un-pause is instant (no
     * fade-out) which matches the responsive feel expected on resume.
     */
    fun resetFade() { fadeT = 0f }

    fun render() {
        // Advance fade-in (clamped to 1). Use real-time delta so the curve is
        // independent of the gameplay tick rate (gameplay is paused anyway).
        val dt = Gdx.graphics.deltaTime
        fadeT = (fadeT + dt / FADE_IN_SECONDS).coerceAtMost(1f)
        // Simple smooth-step curve (matches Interpolation.fade feel).
        val k = fadeT * fadeT * (3f - 2f * fadeT)

        // Semi-transparent dark backdrop dim, alpha = 0..0.55.
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0f, 0f, 0f, DIM_TARGET_ALPHA * k)
        shapeRenderer.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // Apply the same curve to the Scene2D content (menu + hint).
        stage.root.color.a = k
        stage.act()
        stage.draw()
        // Restore for next-frame safety (in case stage is queried elsewhere).
        stage.root.color.a = 1f
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        shapeRenderer.dispose()
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }
}

package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
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

    init {
        val titleStyle = Label.LabelStyle(titleFont, Color.WHITE)

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label("PAUSED", titleStyle)).padBottom(30f).row()

        val btnResume = VisTextButton("Resume")
        btnResume.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onResume() }
        })
        table.add(btnResume).size(220f, 55f).padBottom(14f).row()

        val btnRestart = VisTextButton("Restart Level")
        btnRestart.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onRestart() }
        })
        table.add(btnRestart).size(220f, 55f).padBottom(14f).row()

        val btnMenu = VisTextButton("Main Menu")
        btnMenu.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onMainMenu() }
        })
        table.add(btnMenu).size(220f, 55f).row()

        // Time Trial button (always shown; label reflects current state)
        val trialLabel = if (isCurrentlyTimeTrial) "Exit Time Trial" else "▶ Time Trial"
        val btnTrial = VisTextButton(trialLabel)
        btnTrial.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onTimeTrial?.invoke() }
        })
        table.add(btnTrial).size(220f, 55f).padTop(20f).row()

        stage.addActor(table)
    }

    fun render() {
        // Semi-transparent dark overlay
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f)
        shapeRenderer.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        stage.act()
        stage.draw()
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

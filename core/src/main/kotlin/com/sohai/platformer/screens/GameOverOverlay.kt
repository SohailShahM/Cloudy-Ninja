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
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings

class GameOverOverlay(
    private val onRestart: () -> Unit,
    private val onMainMenu: () -> Unit
) : Disposable {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    val stage = Stage(viewport)
    private val shapeRenderer = ShapeRenderer()

    private val titleFont = FontManager.getShared(40)
    private val bodyFont  = FontManager.getShared(20)

    init {
        val titleStyle = Label.LabelStyle(titleFont, Color(0.9f, 0.25f, 0.2f, 1f))
        val subStyle   = Label.LabelStyle(bodyFont,  Color(0.85f, 0.75f, 0.75f, 1f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label(Strings.get(StringKey.GAME_OVER_TITLE), titleStyle)).padBottom(10f).row()
        table.add(Label(Strings.get(StringKey.GAME_OVER_SUBTITLE), subStyle)).padBottom(36f).row()

        val btnRestart = VisTextButton(Strings.get(StringKey.GAME_OVER_TRY_AGAIN))
        btnRestart.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onRestart() }
        })
        table.add(btnRestart).size(240f, 60f).padBottom(16f).row()

        val btnMenu = VisTextButton(Strings.get(StringKey.GAME_OVER_MAIN_MENU))
        btnMenu.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onMainMenu() }
        })
        table.add(btnMenu).size(240f, 60f).row()

        stage.addActor(table)
    }

    fun render() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Full-screen blood-red tint
        shapeRenderer.setColor(0.12f, 0f, 0f, 0.76f)
        shapeRenderer.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)

        // Card
        val cardW = 420f; val cardH = 280f
        val cardX = (Constants.VIRTUAL_WIDTH  - cardW) / 2f
        val cardY = (Constants.VIRTUAL_HEIGHT - cardH) / 2f
        shapeRenderer.setColor(0.10f, 0.04f, 0.04f, 0.93f)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)

        // Top accent stripe (red)
        shapeRenderer.setColor(0.8f, 0.15f, 0.15f, 0.9f)
        shapeRenderer.rect(cardX, cardY + cardH - 5f, cardW, 5f)

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
    }
}

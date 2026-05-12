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

class LevelCompleteOverlay(
    private val levelName: String,
    private val timeSeconds: Float,
    private val score: Int,
    private val ecoCollected: Int,
    private val ecoTotal: Int,
    private val onContinue: () -> Unit
) : Disposable {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    val stage = Stage(viewport)
    private val shapeRenderer = ShapeRenderer()

    private val titleFont = FontManager.getShared(40)
    private val statFont  = FontManager.getShared(24)
    private val bodyFont  = FontManager.getShared(20)

    init {
        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.55f, 1f))
        val subStyle   = Label.LabelStyle(bodyFont,  Color(0.8f, 0.9f, 0.8f, 1f))
        val statStyle  = Label.LabelStyle(statFont,  Color.WHITE)

        val mins = (timeSeconds / 60).toInt()
        val secs = timeSeconds % 60f
        val timeStr = "%d:%05.2f".format(mins, secs)
        val ecoStr  = Strings.format(StringKey.ECO_FRACTION, ecoCollected, ecoTotal)

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label(Strings.get(StringKey.LEVEL_COMPLETE_TITLE), titleStyle)).padBottom(8f).row()
        table.add(Label(levelName, subStyle)).padBottom(32f).row()

        val statsTable = VisTable()
        statsTable.add(Label(Strings.get(StringKey.LEVEL_COMPLETE_TIME), statStyle)).left().padRight(40f)
        statsTable.add(Label(timeStr,   statStyle)).right().row()
        statsTable.add(Label(Strings.get(StringKey.LEVEL_COMPLETE_SCORE), statStyle)).left().padRight(40f)
        statsTable.add(Label("$score",  statStyle)).right().row()
        statsTable.add(Label(Strings.get(StringKey.LEVEL_COMPLETE_ECO_TOKENS), statStyle)).left().padRight(40f)
        statsTable.add(Label(ecoStr,    statStyle)).right().row()

        table.add(statsTable).padBottom(36f).row()

        val btnContinue = VisTextButton(Strings.get(StringKey.LEVEL_COMPLETE_CONTINUE))
        btnContinue.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onContinue() }
        })
        table.add(btnContinue).size(240f, 60f).row()

        stage.addActor(table)
    }

    fun render() {
        // Dark tinted overlay so gameplay is still vaguely visible behind the panel
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Full-screen dim
        shapeRenderer.setColor(0f, 0.05f, 0.02f, 0.72f)
        shapeRenderer.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)

        // Card background
        val cardW = 480f; val cardH = 340f
        val cardX = (Constants.VIRTUAL_WIDTH  - cardW) / 2f
        val cardY = (Constants.VIRTUAL_HEIGHT - cardH) / 2f
        shapeRenderer.setColor(0.06f, 0.14f, 0.10f, 0.92f)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)

        // Card top accent stripe (green)
        shapeRenderer.setColor(0.25f, 0.9f, 0.5f, 0.8f)
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

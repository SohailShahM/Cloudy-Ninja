package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.levels.LevelManager

class VictoryScreen(
    private val game: Game,
    private val finalScore: Int,
    private val bestTrialTime: Float? = null,
    private val isNewTimeBest: Boolean = false,
    private val priorBestTime: Float? = null
) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont = FontManager.getShared(36)
    private val bodyFont = FontManager.getShared(22)

    init {
        Gdx.input.inputProcessor = stage

        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.5f, 1f))
        val bodyStyle  = Label.LabelStyle(bodyFont, Color.WHITE)
        val scoreStyle = Label.LabelStyle(bodyFont, Color(0.3f, 1f, 0.4f, 1f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label("MISSION COMPLETE!", titleStyle)).padBottom(20f).row()
        table.add(Label("The ecosystem has been restored.", bodyStyle)).padBottom(12f).row()
        table.add(Label("Final Score: $finalScore", scoreStyle)).padBottom(40f).row()

        if (bestTrialTime != null) {
            val mins  = (bestTrialTime / 60f).toInt()
            val secs  = (bestTrialTime % 60f).toInt()
            val tenth = ((bestTrialTime % 1f) * 10f).toInt()
            val timeStr = "%d:%02d.%d".format(mins, secs, tenth)
            val timeColor = if (isNewTimeBest) Color(0.1f, 0.95f, 0.85f, 1f) else Color(0.75f, 0.75f, 1f, 1f)
            table.add(Label("Trial Time: $timeStr", Label.LabelStyle(bodyFont, timeColor)))
                .padBottom(8f).row()
            if (priorBestTime != null) {
                val delta = bestTrialTime - priorBestTime
                if (delta != 0f) {
                    val absDelta = if (delta < 0f) -delta else delta
                    val deltaStr = if (delta < 0f) "−%.2fs under best".format(absDelta)
                                   else "+%.2fs slower".format(absDelta)
                    val deltaColor = if (delta < 0f) Color(0.3f, 1f, 0.5f, 1f) else Color(0.75f, 0.75f, 0.75f, 1f)
                    table.add(Label(deltaStr, Label.LabelStyle(bodyFont, deltaColor)))
                        .padBottom(8f).row()
                }
            }
            if (isNewTimeBest) {
                val bestStyle = Label.LabelStyle(bodyFont, Color(1f, 0.85f, 0.1f, 1f))
                table.add(Label("★ NEW BEST! ★", bestStyle)).padBottom(32f).row()
            }
        }

        val btnMenu = VisTextButton("Main Menu")
        btnMenu.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@VictoryScreen.dispose()
            }
        })
        table.add(btnMenu).size(220f, 60f).padBottom(14f).row()

        val btnReplay = VisTextButton("Play Again")
        btnReplay.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val level1 = LevelManager.getLevel("level1") ?: return
                game.screen = GameScreen(level1, game)
                this@VictoryScreen.dispose()
            }
        })
        table.add(btnReplay).size(220f, 60f)

        stage.addActor(table)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
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
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }
}

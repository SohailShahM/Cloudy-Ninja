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
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.SaveManager

class LevelSelectScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont = FontManager.getShared(30)
    private val bodyFont = FontManager.getShared(18)

    init {
        Gdx.input.inputProcessor = stage

        val save = SaveManager.loadGame()
        val completed = save.completedLevels
        val bestScores = save.bestScores

        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.85f, 1f))
        val infoStyle  = Label.LabelStyle(bodyFont,  Color(0.7f, 0.7f, 0.7f, 1f))
        val bestStyle  = Label.LabelStyle(bodyFont,  Color(0.3f, 1f, 0.4f,  1f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label(Strings.get(StringKey.LEVEL_SELECT_TITLE), titleStyle)).padBottom(36f).row()

        val allLevels = LevelManager.getAllLevels()
        // Level 1 is always unlocked; subsequent levels unlock when previous is completed
        for ((idx, level) in allLevels.withIndex()) {
            val unlocked = idx == 0 || allLevels[idx - 1].id in completed
            val isCompleted = level.id in completed
            val best = bestScores[level.id]

            val innerTable = VisTable()
            innerTable.pad(12f)

            // World number + name
            val nameColor = when {
                !unlocked   -> Color(0.4f, 0.4f, 0.4f, 1f)
                isCompleted -> Color(0.3f, 1f, 0.65f, 1f)
                else        -> Color.WHITE
            }
            val nameStyle = Label.LabelStyle(bodyFont, nameColor)
            val badge = when {
                !unlocked   -> "[X] "
                isCompleted -> "[+] "
                else        -> "[ ] "
            }
            innerTable.add(Label(Strings.format(StringKey.WORLD_PORTAL, badge, idx + 1, level.name), nameStyle)).left().row()

            if (best != null) {
                innerTable.add(Label(Strings.format(StringKey.BEST_SCORE_VALUE, best), bestStyle)).left().padTop(2f).row()
            } else if (unlocked) {
                innerTable.add(Label(Strings.get(StringKey.LEVEL_SELECT_NOT_CLEARED), infoStyle)).left().padTop(2f).row()
            } else {
                innerTable.add(Label(Strings.format(StringKey.COMPLETE_WORLD_FIRST, idx), infoStyle)).left().padTop(2f).row()
            }

            val btnPlay = VisTextButton(if (unlocked) Strings.get(StringKey.LEVEL_SELECT_BTN_PLAY) else Strings.get(StringKey.LEVEL_SELECT_BTN_LOCKED))
            btnPlay.isDisabled = !unlocked
            val capturedLevel = level
            btnPlay.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (!unlocked) return
                    game.screen = GameScreen(capturedLevel, game)
                    this@LevelSelectScreen.dispose()
                }
            })

            val row = VisTable()
            row.add(innerTable).expandX().left()
            row.add(btnPlay).size(120f, 50f).right()

            table.add(row).fillX().padBottom(14f).width(600f).row()
        }

        table.add(Label("", infoStyle)).padBottom(20f).row()

        val btnBack = VisTextButton(Strings.get(StringKey.LEVEL_SELECT_BACK))
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@LevelSelectScreen.dispose()
            }
        })
        table.add(btnBack).size(180f, 52f)

        stage.addActor(table)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = viewport.update(width, height, true)
    override fun show() {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }
}

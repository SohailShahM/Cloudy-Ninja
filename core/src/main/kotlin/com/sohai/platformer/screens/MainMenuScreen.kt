package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.levels.Level1
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.SaveManager

class MainMenuScreen(private val game: Game) : Screen {
    private val stage: Stage = Stage(ScreenViewport())

    init {
        Gdx.input.inputProcessor = stage

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        val title = VisLabel("Cloudy Ninja")
        table.add(title).padBottom(60f).row()

        val newGameButton = VisTextButton("New Game")
        newGameButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                startNewGame()
            }
        })
        table.add(newGameButton).width(220f).height(55f).padBottom(16f).row()

        val saveList = SaveManager.listSaves()
        if (saveList.isNotEmpty()) {
            val continueButton = VisTextButton("Continue")
            continueButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val savedState = SaveManager.loadGame()
                    val cp = savedState.checkpoint
                    val hasValidCheckpoint = cp.levelName == savedState.level && (cp.x != 0f || cp.y != 0f)
                    val resume = if (hasValidCheckpoint) com.badlogic.gdx.math.Vector2(cp.x, cp.y) else null
                    openGameAtLevel(savedState.level, resume)
                }
            })
            table.add(continueButton).width(220f).height(55f).padBottom(16f).row()
        }

        val levelSelectButton = VisTextButton("Level Select")
        levelSelectButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = LevelSelectScreen(game)
                dispose()
            }
        })
        table.add(levelSelectButton).width(220f).height(55f).padBottom(16f).row()

        val atlasButton = VisTextButton("Cloud Atlas")
        atlasButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = CloudAtlasScreen(game)
                dispose()
            }
        })
        table.add(atlasButton).width(220f).height(55f).padBottom(16f).row()

        val settingsButton = VisTextButton("Settings")
        settingsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.log("Menu", "Settings not yet implemented")
            }
        })
        table.add(settingsButton).width(220f).height(55f).padBottom(16f).row()

        val quitButton = VisTextButton("Quit")
        quitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })
        table.add(quitButton).width(220f).height(55f).row()

        stage.addActor(table)
    }

    private fun startNewGame() {
        openGameAtLevel("level1")
    }

    private fun openGameAtLevel(levelId: String, resumeCheckpoint: com.badlogic.gdx.math.Vector2? = null) {
        val level = LevelManager.getLevel(levelId) ?: Level1()
        game.screen = GameScreen(level, game, resumeCheckpoint)
        dispose()
    }

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
    }
}

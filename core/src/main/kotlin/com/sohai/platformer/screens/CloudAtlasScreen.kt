package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.atlas.CloudAtlasEntry
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.persist.SaveManager

/**
 * Browses every Cloud Atlas snapshot the player has collected.
 * Shows locked silhouettes for entries not yet found, full text for collected ones.
 */
class CloudAtlasScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont    = FontManager.getShared(32)
    private val entryFont    = FontManager.getShared(20)
    private val bodyFont     = FontManager.getShared(16)

    private var selectedEntry: CloudAtlasEntry? = null
    private lateinit var detailContainer: VisTable

    init {
        Gdx.input.inputProcessor = stage

        val collected = SaveManager.loadGame().collectedAtlasIds

        val rootStyle      = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.85f, 1f))
        val countStyle     = Label.LabelStyle(bodyFont,  Color(0.7f, 0.9f, 1f, 1f))
        val unlockedStyle  = Label.LabelStyle(entryFont, Color.WHITE)
        val lockedStyle    = Label.LabelStyle(entryFont, Color(0.45f, 0.45f, 0.45f, 1f))
        val infoStyle      = Label.LabelStyle(bodyFont,  Color(0.6f, 0.6f, 0.6f, 1f))
        val bodyStyle      = Label.LabelStyle(bodyFont,  Color.WHITE)
        val subtitleStyle  = Label.LabelStyle(bodyFont,  Color(0.7f, 0.9f, 1f, 1f))

        val root = VisTable()
        root.setFillParent(true)
        root.top().pad(40f)

        root.add(Label("CLOUD ATLAS", rootStyle)).colspan(2).padBottom(8f).row()
        root.add(Label("${collected.size} / ${CloudAtlasLibrary.entries.size} snapshots discovered", countStyle))
            .colspan(2).padBottom(28f).row()

        // Left: list of entries
        val list = VisTable()
        list.top().left()
        for (entry in CloudAtlasLibrary.entries.values) {
            val isUnlocked = entry.id in collected
            val style = if (isUnlocked) unlockedStyle else lockedStyle
            val label = if (isUnlocked) "${entry.title}" else "??? (locked)"
            val btn = VisTextButton(label)
            btn.label.style = style
            btn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (isUnlocked) {
                        selectedEntry = entry
                        rebuildDetailPane()
                    }
                }
            })
            list.add(btn).left().fillX().width(360f).padBottom(8f).row()
        }
        val listPane = ScrollPane(list)
        listPane.setScrollingDisabled(true, false)
        root.add(listPane).top().left().width(380f).height(420f).padRight(20f)

        // Right: selected entry detail (rebuilt on selection)
        detailContainer = VisTable()
        detailContainer.top().left()
        rebuildDetailPane()
        root.add(detailContainer).top().left().width(680f).height(420f).row()

        // Bottom: back button
        val btnBack = VisTextButton("Back to Menu")
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@CloudAtlasScreen.dispose()
            }
        })
        root.add(btnBack).colspan(2).padTop(30f).size(220f, 52f)

        stage.addActor(root)
    }

    private fun rebuildDetailPane() {
        detailContainer.clearChildren()
        val entry = selectedEntry
        val titleStyle    = Label.LabelStyle(entryFont, Color(0.3f, 1f, 0.85f, 1f))
        val subtitleStyle = Label.LabelStyle(bodyFont,  Color(0.7f, 0.9f, 1f, 1f))
        val bodyStyle     = Label.LabelStyle(bodyFont,  Color.WHITE)
        val infoStyle     = Label.LabelStyle(bodyFont,  Color(0.6f, 0.6f, 0.6f, 1f))

        if (entry == null) {
            detailContainer.add(Label("Select a snapshot to read.", infoStyle)).left().pad(20f)
            return
        }
        detailContainer.add(Label(entry.title, titleStyle)).left().padBottom(6f).row()
        detailContainer.add(Label(entry.subtitle, subtitleStyle)).left().padBottom(12f).row()
        detailContainer.add(Label("Discovered by: ${entry.character}", infoStyle)).left().padBottom(20f).row()
        val body = Label(entry.body, bodyStyle)
        body.wrap = true
        detailContainer.add(body).left().width(660f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.screen = MainMenuScreen(game)
            dispose()
            return
        }
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

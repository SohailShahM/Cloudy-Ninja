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
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager

private val STATS_SLOT_FILES = arrayOf("save_0.json", "save_1.json", "save_2.json")
private const val TOTAL_ACHIEVEMENTS = 12
private const val STATS_CARD_CONTENT_WIDTH = 1120f

class StatsScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont = FontManager.getShared(32)
    private val bodyFont = FontManager.getShared(18)

    init {
        Gdx.input.inputProcessor = stage

        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.85f, 1f))
        val sectionStyle = Label.LabelStyle(bodyFont, Color(0.8f, 0.95f, 1f, 1f))
        val bodyStyle = Label.LabelStyle(bodyFont, Color.WHITE)
        val mutedStyle = Label.LabelStyle(bodyFont, Color(0.6f, 0.6f, 0.6f, 1f))

        val root = VisTable()
        root.setFillParent(true)
        root.top().padTop(36f).padLeft(30f).padRight(30f)

        root.add(Label("STATS", titleStyle)).padBottom(20f).row()

        val slotList = VisTable()
        slotList.top().left()
        slotList.defaults().left().expandX().fillX().padBottom(14f)
        for (slotIndex in STATS_SLOT_FILES.indices) {
            slotList.add(buildSlotCard(slotIndex, sectionStyle, bodyStyle, mutedStyle)).row()
        }

        val scroll = VisScrollPane(slotList)
        scroll.setFlickScroll(false)
        root.add(scroll).expand().fill().row()

        val backButton = VisTextButton("Back")
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@StatsScreen.dispose()
            }
        })
        root.add(backButton).size(220f, 52f).padTop(18f).padBottom(26f).row()

        stage.addActor(root)
    }

    private fun buildSlotCard(
        slotIndex: Int,
        sectionStyle: Label.LabelStyle,
        bodyStyle: Label.LabelStyle,
        mutedStyle: Label.LabelStyle
    ): VisTable {
        val file = STATS_SLOT_FILES[slotIndex]
        val hasSave = SaveManager.hasSave(file)
        val state = if (hasSave) SaveManager.loadGame(file) else null

        val card = VisTable()
        card.background("window")
        card.pad(12f)
        card.left().top()

        card.add(Label("Slot ${slotIndex + 1}", sectionStyle)).left().padBottom(8f).row()
        if (state == null) {
            card.add(Label("— Empty —", mutedStyle)).left().row()
            return card
        }

        card.add(Label("Total deaths: ${state.totalDeaths}", bodyStyle)).left().padBottom(4f).row()

        val completedOrdered = LevelManager.getAllLevels()
            .map { it.id }
            .filter { it in state.completedLevels }
        val completedDisplay = if (completedOrdered.isEmpty()) "—" else completedOrdered.joinToString(", ") { id ->
            LevelManager.getLevel(id)?.name ?: id
        }
        card.add(Label("Levels completed: ${state.completedLevels.size}", bodyStyle)).left().padBottom(2f).row()
        val completedLabel = Label(completedDisplay, bodyStyle)
        completedLabel.wrap = true
        card.add(completedLabel).left().width(STATS_CARD_CONTENT_WIDTH).padBottom(6f).row()

        val ecoCollected = completedOrdered.sumOf { id ->
            LevelManager.getLevel(id)?.getEcoTokenPositions()?.size ?: 0
        }
        card.add(Label("Eco-tokens collected: $ecoCollected", bodyStyle)).left().padBottom(6f).row()

        val bestTimesDisplay = if (state.bestTimes.isEmpty()) {
            "—"
        } else {
            state.bestTimes.entries
                .sortedBy { it.key }
                .joinToString(", ") { (levelId, timeSec) ->
                    "${LevelManager.getLevel(levelId)?.name ?: levelId}: ${"%.2f".format(timeSec)}s"
                }
        }
        val bestTimesLabel = Label("Best times: $bestTimesDisplay", bodyStyle)
        bestTimesLabel.wrap = true
        card.add(bestTimesLabel).left().width(STATS_CARD_CONTENT_WIDTH).padBottom(6f).row()

        val unlockedAchievements = readUnlockedAchievements(state)
        if (unlockedAchievements == null) {
            card.add(Label("Achievements unlocked: —", bodyStyle)).left().padBottom(2f).row()
            card.add(Label("—", mutedStyle)).left().row()
        } else {
            val list = if (unlockedAchievements.isEmpty()) "—" else unlockedAchievements.sorted().joinToString(", ")
            card.add(Label("Achievements unlocked: ${unlockedAchievements.size}/$TOTAL_ACHIEVEMENTS", bodyStyle)).left().padBottom(2f).row()
            val achievementsLabel = Label(list, bodyStyle)
            achievementsLabel.wrap = true
            card.add(achievementsLabel).left().width(STATS_CARD_CONTENT_WIDTH).row()
        }

        return card
    }

    @Suppress("UNCHECKED_CAST")
    private fun readUnlockedAchievements(state: GameState): Set<String>? {
        return runCatching {
            val getter = state.javaClass.methods.firstOrNull { it.name == "getUnlockedAchievements" }
            getter?.invoke(state) as? Set<String>
        }.getOrNull()
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
    }
}

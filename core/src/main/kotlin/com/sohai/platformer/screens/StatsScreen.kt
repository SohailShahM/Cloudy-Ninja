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
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager

internal val STATS_SLOT_FILES = arrayOf("save_slot_0.json", "save_slot_1.json", "save_slot_2.json")
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

        root.add(Label(Strings.get(StringKey.STATS_TITLE), titleStyle)).padBottom(20f).row()

        val slotList = VisTable()
        slotList.top().left()
        slotList.defaults().left().expandX().fillX().padBottom(14f)
        for (slotIndex in STATS_SLOT_FILES.indices) {
            slotList.add(buildSlotCard(slotIndex, sectionStyle, bodyStyle, mutedStyle)).row()
        }

        val scroll = VisScrollPane(slotList)
        scroll.setFlickScroll(false)
        root.add(scroll).expand().fill().row()

        val backButton = VisTextButton(Strings.get(StringKey.STATS_BACK))
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

        card.add(Label(Strings.format(StringKey.SLOT_LABEL, slotIndex + 1), sectionStyle)).left().padBottom(8f).row()
        if (state == null) {
            card.add(Label(Strings.get(StringKey.STATS_EMPTY), mutedStyle)).left().row()
            return card
        }

        card.add(Label(Strings.format(StringKey.TOTAL_DEATHS, state.totalDeaths), bodyStyle)).left().padBottom(4f).row()

        val completedOrdered = LevelManager.getAllLevels()
            .map { it.id }
            .filter { it in state.completedLevels }
        val completedDisplay = if (completedOrdered.isEmpty()) Strings.get(StringKey.STATS_DASH) else completedOrdered.joinToString(", ") { id ->
            LevelManager.getLevel(id)?.name ?: id
        }
        card.add(Label(Strings.format(StringKey.LEVELS_COMPLETED, state.completedLevels.size), bodyStyle)).left().padBottom(2f).row()
        val completedLabel = Label(completedDisplay, bodyStyle)
        completedLabel.wrap = true
        card.add(completedLabel).left().width(STATS_CARD_CONTENT_WIDTH).padBottom(6f).row()

        val ecoCollected = completedOrdered.sumOf { id ->
            LevelManager.getLevel(id)?.getEcoTokenPositions()?.size ?: 0
        }
        card.add(Label(Strings.format(StringKey.ECO_TOKENS_COLLECTED, ecoCollected), bodyStyle)).left().padBottom(6f).row()

        if (state.bestTimes.isEmpty()) {
            card.add(Label(Strings.get(StringKey.STATS_BEST_TIMES_EMPTY), mutedStyle))
                .left().padBottom(6f).row()
        } else {
            card.add(Label(Strings.get(StringKey.STATS_BEST_TIMES_HEADER), sectionStyle)).left().padBottom(2f).row()
            // Iterate levels in canonical LevelManager order; skip levels with no recorded time.
            for (level in LevelManager.getAllLevels()) {
                val timeSec = state.bestTimes[level.id] ?: continue
                val line = Strings.format(StringKey.BEST_TIME_LINE, level.name, formatBestTime(timeSec))
                card.add(Label(line, bodyStyle)).left().padBottom(2f).row()
            }
            // Spacer below the section
            card.add(Label("", bodyStyle)).left().padBottom(4f).row()
        }

        val unlockedAchievements = readUnlockedAchievements(state)
        if (unlockedAchievements == null) {
            card.add(Label(Strings.get(StringKey.STATS_ACHIEVEMENTS_MISSING), bodyStyle)).left().padBottom(2f).row()
            card.add(Label(Strings.get(StringKey.STATS_ACHIEVEMENTS_NONE), mutedStyle)).left().row()
        } else {
            val list = if (unlockedAchievements.isEmpty()) Strings.get(StringKey.STATS_DASH) else unlockedAchievements.sorted().joinToString(", ")
            card.add(Label(Strings.format(StringKey.ACHIEVEMENTS_UNLOCKED, unlockedAchievements.size, TOTAL_ACHIEVEMENTS), bodyStyle)).left().padBottom(2f).row()
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

    /**
     * Format a best-time value (stored in seconds as a Float) as MM:SS.mmm,
     * e.g. 83.45f -> "01:23.450". Negative or NaN inputs clamp to "00:00.000".
     */
    private fun formatBestTime(timeSec: Float): String {
        if (timeSec.isNaN() || timeSec <= 0f) return "00:00.000"
        val totalMillis = (timeSec * 1000f).toLong()
        val minutes = (totalMillis / 60_000L)
        val seconds = (totalMillis / 1000L) % 60L
        val millis  = totalMillis % 1000L
        return "${minutes.toString().padStart(2, '0')}:" +
               "${seconds.toString().padStart(2, '0')}." +
               millis.toString().padStart(3, '0')
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

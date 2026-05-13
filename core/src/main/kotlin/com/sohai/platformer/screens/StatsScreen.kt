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
import com.sohai.platformer.input.GlobalInputRouter
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager

internal val STATS_SLOT_FILES = arrayOf("save_slot_0.json", "save_slot_1.json", "save_slot_2.json")
private const val TOTAL_ACHIEVEMENTS = 12
private const val STATS_CARD_CONTENT_WIDTH = 1120f

/**
 * T-135: Campaign level IDs whose per-level token completion is shown on
 * StatsScreen. Mirrors the set referenced by the `all_clear` achievement in
 * [LevelTransitionController]. Tutorial rooms (level0_0..level0_4) are
 * excluded — they have no eco-tokens and aren't part of the completionist
 * progression surface.
 */
internal val STATS_CAMPAIGN_LEVEL_IDS = listOf("level1", "level2", "level3")

class StatsScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont = FontManager.getShared(32)
    private val bodyFont = FontManager.getShared(18)

    init {
        // T-172 (Phase B): input wiring moved to show()/hide() via the
        // GlobalInputRouter so this screen no longer clobbers the router.

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

        // T-135: per-campaign-level eco-token completion (regular + hidden combined)
        // plus a hidden-only discovery row. Token totals come from the level
        // registry (`getEcoTokenPositions` + `getHiddenEcoTokenPositions`) — not
        // hardcoded. Per-level "regular collected" is derived from
        // `completedLevels` (consistent with the aggregate line above);
        // per-level "hidden collected" is derived from `collectedHiddenTokens`
        // (T-107).
        card.add(Label(Strings.get(StringKey.STATS_LEVEL_TOKENS_HEADER), sectionStyle))
            .left().padBottom(2f).row()
        for (levelId in STATS_CAMPAIGN_LEVEL_IDS) {
            val level = LevelManager.getLevel(levelId) ?: continue
            val progress = computeLevelTokenProgress(
                level = level,
                completed = level.id in state.completedLevels,
                hiddenCollected = level.id in state.collectedHiddenTokens
            )
            val line = Strings.format(
                StringKey.STATS_LEVEL_TOKEN_PROGRESS,
                level.name,
                progress.collected,
                progress.total,
                progress.percent
            )
            card.add(Label(line, bodyStyle)).left().padBottom(2f).row()
        }
        val hiddenSummary = computeHiddenTokenSummary(state.collectedHiddenTokens)
        card.add(
            Label(
                Strings.format(
                    StringKey.STATS_HIDDEN_TOKEN_PROGRESS,
                    hiddenSummary.collected,
                    hiddenSummary.total
                ),
                mutedStyle
            )
        ).left().padBottom(6f).row()

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
            // T-108: replaced comma-joined id string with count + "View All →" link
            // that opens the new AchievementsScreen on the same slot.
            card.add(
                Label(
                    Strings.format(
                        StringKey.STATS_ACHIEVEMENT_COUNT,
                        unlockedAchievements.size,
                        TOTAL_ACHIEVEMENTS
                    ),
                    bodyStyle
                )
            ).left().padBottom(4f).row()

            val viewAllBtn = VisTextButton(Strings.get(StringKey.ACHIEVEMENTS_VIEW_ALL_BUTTON))
            viewAllBtn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.screen = AchievementsScreen(game, slotIndex)
                    this@StatsScreen.dispose()
                }
            })
            card.add(viewAllBtn).left().width(180f).height(40f).row()
        }

        return card
    }

    companion object {
        /**
         * T-135: per-level token-progress row data. [collected] / [total] = raw
         * counts (regular + hidden combined). [percent] is integer-floored
         * (e.g. 9/10 → 90, 1/3 → 33). Empty saves and levels with zero tokens
         * yield 0/0/0 — callers display "0/0 tokens (0%)" in that case.
         */
        data class LevelTokenProgress(val collected: Int, val total: Int, val percent: Int)

        /**
         * Pure helper for the StatsScreen per-level row. Reads token totals
         * from the level registry — never hardcoded — and derives "collected"
         * from existing save fields only (no new save state required).
         *
         *  - Regular: assume all collected iff [completed]. Matches the
         *    aggregate "Eco-tokens collected: N" line which has used this
         *    derivation since before T-107.
         *  - Hidden: 1× hidden total iff [hiddenCollected] (per-level binary
         *    flag from `GameState.collectedHiddenTokens`). The campaign
         *    currently has exactly 1 hidden per level (T-107), but multiplying
         *    by `hiddenTotal` keeps the helper correct if that ever grows.
         */
        fun computeLevelTokenProgress(
            level: Level,
            completed: Boolean,
            hiddenCollected: Boolean
        ): LevelTokenProgress {
            val regularTotal = level.getEcoTokenPositions().size
            val hiddenTotal = level.getHiddenEcoTokenPositions().size
            val total = regularTotal + hiddenTotal
            val collected =
                (if (completed) regularTotal else 0) +
                (if (hiddenCollected) hiddenTotal else 0)
            val percent = if (total == 0) 0 else (collected * 100 / total)
            return LevelTokenProgress(collected, total, percent)
        }

        /** T-135: bonus "Hidden: N/M found" summary across the campaign. */
        data class HiddenTokenSummary(val collected: Int, val total: Int)

        /**
         * Counts hidden tokens across the campaign levels named in
         * [STATS_CAMPAIGN_LEVEL_IDS]. `total` is the registry-derived count of
         * hidden tokens summed across those levels; `collected` is the count
         * of campaign level IDs in [collectedHiddenTokens] that map to a level
         * with at least one hidden token defined. Unknown IDs in the save set
         * are ignored to keep the display consistent with the visible total.
         */
        fun computeHiddenTokenSummary(collectedHiddenTokens: Set<String>): HiddenTokenSummary {
            var total = 0
            var collected = 0
            for (levelId in STATS_CAMPAIGN_LEVEL_IDS) {
                val level = LevelManager.getLevel(levelId) ?: continue
                val hiddenCount = level.getHiddenEcoTokenPositions().size
                total += hiddenCount
                if (hiddenCount > 0 && levelId in collectedHiddenTokens) collected += hiddenCount
            }
            return HiddenTokenSummary(collected, total)
        }
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
    /** T-172 (Phase B): wire input via the router on show. */
    override fun show() {
        GlobalInputRouter.install()
        GlobalInputRouter.pushScreen(stage)
    }
    override fun pause() {}
    override fun resume() {}
    /** T-172 (Phase B): pop our stage off the router on screen exit. */
    override fun hide() {
        GlobalInputRouter.popScreen(stage)
    }

    override fun dispose() {
        // T-172 (Phase B): defensive pop covers dispose() reached without hide().
        GlobalInputRouter.popScreen(stage)
        stage.dispose()
    }
}

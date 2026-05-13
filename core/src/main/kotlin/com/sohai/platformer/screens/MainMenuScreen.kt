package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.progression.AchievementRegistry

/** Filenames for the three save slots (index 0-2). */
private val SLOT_FILES = arrayOf("save_slot_0.json", "save_slot_1.json", "save_slot_2.json")

/**
 * Total number of Cloud Atlas entries — used for atlas % calculation.
 * Falls back to 1 to avoid division-by-zero if the library is somehow empty.
 */
private val TOTAL_ATLAS_ENTRIES: Int
    get() = CloudAtlasLibrary.entries.size.takeIf { it > 0 } ?: 1

class MainMenuScreen(private val game: Game) : Screen {

    private val stage: Stage = Stage(ScreenViewport())

    /**
     * Tracks which slot is currently waiting for delete confirmation.
     * -1 means no slot is pending confirmation.
     */
    private var pendingDeleteSlot: Int = -1

    /** Mutable references to the per-slot delete buttons so we can update their labels. */
    private val deleteButtons = arrayOfNulls<VisTextButton>(3)

    /**
     * Bottom-right build-info label (T-100). Kept as a stage-level actor (not
     * inside the centered root table) so it pins to the corner regardless of
     * the table layout, and gets repositioned in [resize].
     */
    private var buildInfoLabel: Label? = null

    init {
        Gdx.input.inputProcessor = stage
        buildUi()
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private fun buildUi() {
        stage.clear()

        val root = VisTable()
        root.setFillParent(true)
        root.center()

        // Title
        val title = VisLabel(Strings.get(StringKey.MAIN_TITLE))
        root.add(title).padBottom(40f).row()

        // --- Three slot cards ---
        val slotsRow = VisTable()
        slotsRow.defaults().padLeft(12f).padRight(12f)

        for (slotIndex in 0..2) {
            val card = buildSlotCard(slotIndex)
            slotsRow.add(card).top().width(220f).padBottom(8f)
        }
        root.add(slotsRow).row()

        // --- Achievement progress counter (T-099) ---
        root.add(buildAchievementProgressLabel()).pad(12f).row()

        // --- Bottom navigation buttons ---
        root.add(buildNavButtons()).padTop(36f).row()

        stage.addActor(root)

        // --- Build/version label, pinned to bottom-right corner (T-100) ---
        val label = buildBuildInfoLabel()
        buildInfoLabel = label
        stage.addActor(label)
        repositionBuildInfoLabel()
    }

    /**
     * Builds the tiny build-info label shown in the bottom-right of the menu
     * (T-100). Reads version + date from [Constants] (manually maintained
     * during the alpha pre-launch window) and renders via the
     * `MENU_BUILD_INFO` i18n template `v{0} · {1}` in dim grey at font-size 11.
     */
    private fun buildBuildInfoLabel(): Label {
        val text  = Strings.format(StringKey.MENU_BUILD_INFO, Constants.BUILD_VERSION, Constants.BUILD_DATE)
        val font  = FontManager.getShared(11)
        val color = Color(0.5f, 0.5f, 0.5f, 0.6f)
        val label = Label(text, Label.LabelStyle(font, color))
        label.pack()
        return label
    }

    /**
     * Pins [buildInfoLabel] to 8px from the bottom-right of the stage. Called
     * from [buildUi] and [resize] so the corner-anchor survives window
     * resizes.
     */
    private fun repositionBuildInfoLabel() {
        val label = buildInfoLabel ?: return
        // Keep label width as packed so right-alignment math stays correct.
        label.width = label.prefWidth
        val sw = stage.viewport.worldWidth.takeIf { it > 0f }
            ?: (label.prefWidth + BUILD_INFO_PADDING)
        val (x, y) = buildInfoLabelPosition(sw, label.prefWidth)
        label.setPosition(x, y)
    }

    /**
     * Builds a single save-slot card (VisTable) for [slotIndex] (0, 1, or 2).
     * The card shows slot summary info and Load/New Game + Delete buttons.
     */
    private fun buildSlotCard(slotIndex: Int): VisTable {
        val filename = SLOT_FILES[slotIndex]
        val hasSave  = SaveManager.hasSave(filename)
        val state    = if (hasSave) SaveManager.loadGame(filename) else null

        val card = VisTable()
        card.background("window")   // VisUI "window" drawable gives a bordered panel look
        card.pad(14f)
        card.top()

        // --- Header ---
        val header = VisLabel(Strings.format(StringKey.SLOT_LABEL, slotIndex + 1))
        card.add(header).padBottom(10f).row()

        if (state != null) {
            addSaveInfo(card, state)
        } else {
            val emptyLabel = VisLabel(Strings.get(StringKey.MAIN_SLOT_EMPTY))
            card.add(emptyLabel).padBottom(10f).row()
        }

        // --- Action buttons ---
        if (state != null) {
            // Load button
            val loadBtn = VisTextButton(Strings.get(StringKey.MAIN_BTN_LOAD))
            loadBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    loadSlot(slotIndex, state)
                }
            })
            card.add(loadBtn).width(160f).height(40f).padBottom(8f).row()

            // Delete button (two-click confirm)
            val deleteBtn = VisTextButton(Strings.get(StringKey.MAIN_BTN_DELETE))
            deleteButtons[slotIndex] = deleteBtn
            deleteBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    handleDeleteClick(slotIndex, filename)
                }
            })
            card.add(deleteBtn).width(160f).height(40f).row()
        } else {
            // New Game button for empty slot
            val newBtn = VisTextButton(Strings.get(StringKey.MAIN_BTN_NEW_GAME))
            newBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    startNewGameInSlot(slotIndex)
                }
            })
            card.add(newBtn).width(160f).height(40f).row()
        }

        return card
    }

    /** Adds the detailed save summary rows to [card]. */
    private fun addSaveInfo(card: VisTable, state: GameState) {
        val dimStyle = Label.LabelStyle(
            com.badlogic.gdx.graphics.g2d.BitmapFont(),
            Color(0.75f, 0.85f, 1f, 1f)
        )

        // Level name (use LevelManager to get the human-readable name if possible)
        val levelObj  = LevelManager.getLevel(state.level)
        val levelName = levelObj?.name ?: state.level
        card.add(VisLabel(levelName)).padBottom(4f).row()

        // Atlas %
        val atlasPct = (state.collectedAtlasIds.size * 100) / TOTAL_ATLAS_ENTRIES
        card.add(VisLabel(Strings.format(StringKey.ATLAS_PCT, atlasPct))).padBottom(4f).row()

        // Total deaths
        card.add(VisLabel(Strings.format(StringKey.DEATHS_COUNT, state.totalDeaths))).padBottom(4f).row()

        // Last played date (only shown when non-empty)
        if (state.lastPlayed.isNotEmpty()) {
            card.add(VisLabel(Strings.format(StringKey.LAST_PLAYED, state.lastPlayed))).padBottom(4f).row()
        }
    }

    /**
     * Builds the achievement progress label shown below the slot row (T-099).
     *
     * Reads all 3 save slots, takes the **max** count across them, and renders
     * `Achievements: {count}/{total} unlocked` in light grey at font size 14.
     * If every achievement is unlocked, the gold "complete" variant is used
     * instead. `total` comes from `AchievementRegistry.ALL.size` so the label
     * stays correct if achievements are added/removed later.
     */
    private fun buildAchievementProgressLabel(): Label {
        val count = maxUnlockedAcrossSlots()
        val total = AchievementRegistry.ALL.size
        val (text, color) = achievementProgressTextAndColor(count, total)
        val font = FontManager.getShared(14)
        return Label(text, Label.LabelStyle(font, color))
    }

    /** Builds the row of bottom-navigation buttons (Level Select, Cloud Atlas, Settings, Quit). */
    private fun buildNavButtons(): VisTable {
        val nav = VisTable()
        nav.defaults().width(200f).height(50f).padLeft(8f).padRight(8f)

        val levelSelectButton = VisTextButton(Strings.get(StringKey.MAIN_BTN_LEVEL_SELECT))
        levelSelectButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = LevelSelectScreen(game)
                dispose()
            }
        })
        nav.add(levelSelectButton)

        val atlasButton = VisTextButton(Strings.get(StringKey.MAIN_BTN_CLOUD_ATLAS))
        atlasButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = CloudAtlasScreen(game)
                dispose()
            }
        })
        nav.add(atlasButton)

        val achievementsButton = VisTextButton(Strings.get(StringKey.MENU_ACHIEVEMENTS))
        achievementsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = AchievementsScreen(game)
                dispose()
            }
        })
        nav.add(achievementsButton)

        val statsButton = VisTextButton(Strings.get(StringKey.MAIN_BTN_STATS))
        statsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = StatsScreen(game)
                dispose()
            }
        })
        nav.add(statsButton)

        val settingsButton = VisTextButton(Strings.get(StringKey.MAIN_BTN_SETTINGS))
        settingsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = SettingsScreen(game)
                dispose()
            }
        })
        nav.add(settingsButton)

        val quitButton = VisTextButton(Strings.get(StringKey.MAIN_BTN_QUIT))
        quitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })
        nav.add(quitButton)

        return nav
    }

    // -------------------------------------------------------------------------
    // Slot actions
    // -------------------------------------------------------------------------

    /** Load an existing save and open GameScreen. */
    private fun loadSlot(slotIndex: Int, state: GameState) {
        pendingDeleteSlot = -1
        val cp = state.checkpoint
        val hasValidCheckpoint = cp.levelName == state.level && (cp.x != 0f || cp.y != 0f)
        val resume = if (hasValidCheckpoint) Vector2(cp.x, cp.y) else null
        openGameAtLevel(state.level, resume)
    }

    /** Start a fresh game in the given slot, saving an initial state. */
    private fun startNewGameInSlot(slotIndex: Int) {
        pendingDeleteSlot = -1
        val filename = SLOT_FILES[slotIndex]
        val initialState = GameState(
            level       = "level0_0",
            lastPlayed  = todayIso()
        )
        SaveManager.saveGame(initialState, filename)
        openGameAtLevel("level0_0")
    }

    /**
     * Two-click delete: first click sets pending confirmation and relabels the button;
     * second click (or click on any other delete) confirms deletion and rebuilds the UI.
     */
    private fun handleDeleteClick(slotIndex: Int, filename: String) {
        if (pendingDeleteSlot == slotIndex) {
            // Second click — confirmed
            SaveManager.deleteSave(filename)
            pendingDeleteSlot = -1
            // Rebuild UI to reflect the now-empty slot
            buildUi()
        } else {
            // First click — ask for confirmation
            // Reset any previously pending slot
            if (pendingDeleteSlot != -1) {
                deleteButtons[pendingDeleteSlot]?.setText(Strings.get(StringKey.MAIN_BTN_DELETE))
            }
            pendingDeleteSlot = slotIndex
            deleteButtons[slotIndex]?.setText(Strings.get(StringKey.MAIN_BTN_DELETE_CONFIRM))
        }
    }

    private fun openGameAtLevel(levelId: String, resumeCheckpoint: Vector2? = null) {
        val level = LevelManager.getLevel(levelId)
            ?: LevelManager.getLevel("level0_0")
            ?: LevelManager.getAllLevels().firstOrNull()
            ?: return
        game.screen = GameScreen(level, game, resumeCheckpoint)
        dispose()
    }

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        // Re-anchor the bottom-right build label after the new viewport size
        // is applied (T-100).
        repositionBuildInfoLabel()
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Returns today's date as a simple "yyyy-MM-dd" string without requiring
     * java.time (which needs core library desugaring on older Android).
     * Uses java.util.Calendar which is available on all platforms.
     */
    private fun todayIso(): String {
        val cal = java.util.Calendar.getInstance()
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    /**
     * Returns the **max** number of unlocked achievements across the three
     * save slots (T-099). Empty / unparseable slots contribute 0.
     */
    private fun maxUnlockedAcrossSlots(): Int =
        SLOT_FILES.maxOf { filename ->
            if (SaveManager.hasSave(filename)) {
                SaveManager.loadGame(filename).unlockedAchievements.size
            } else {
                0
            }
        }

    companion object {
        /**
         * Padding (in stage units / px) from the right + bottom edges of the
         * stage to the build-info label (T-100). 8px per ticket spec.
         */
        const val BUILD_INFO_PADDING = 8f

        /**
         * Pure helper for T-099: given a per-slot unlocked-count list (typically
         * size 3), returns the max. Empty input returns 0. Exposed so headless
         * tests can verify the max-across-slots aggregation without booting GL.
         */
        fun maxUnlockedAcrossSlotCounts(perSlotCounts: List<Int>): Int =
            if (perSlotCounts.isEmpty()) 0 else perSlotCounts.max()

        /**
         * Pure helper for T-099: picks the appropriate template+color for the
         * achievement progress label, mirroring [buildAchievementProgressLabel]
         * but without requiring a [BitmapFont]. Returns the rendered text and
         * its display color.
         */
        fun achievementProgressTextAndColor(count: Int, total: Int): Pair<String, Color> =
            if (count >= total && total > 0) {
                Strings.format(StringKey.MENU_ACHIEVEMENT_PROGRESS_COMPLETE, total) to
                    Color(1f, 0.85f, 0.1f, 1f)
            } else {
                Strings.format(StringKey.MENU_ACHIEVEMENT_PROGRESS, count, total) to
                    Color(0.75f, 0.75f, 0.75f, 1f)
            }

        /**
         * Pure helper for T-100: returns the rendered text of the build-info
         * label using the `MENU_BUILD_INFO` template. Exposed so headless tests
         * can verify the format without booting GL.
         */
        fun buildInfoText(version: String, date: String): String =
            Strings.format(StringKey.MENU_BUILD_INFO, version, date)

        /**
         * Pure helper for T-100: the display color of the build-info label —
         * dim grey `(0.5, 0.5, 0.5, 0.6)` per ticket spec.
         */
        fun buildInfoColor(): Color = Color(0.5f, 0.5f, 0.5f, 0.6f)

        /**
         * Pure helper for T-100: returns the (x, y) bottom-left position of
         * the build-info label given the stage width and the label's measured
         * width. y is just [BUILD_INFO_PADDING]; x anchors the label's right
         * edge 8px from the stage's right edge.
         */
        fun buildInfoLabelPosition(stageWidth: Float, labelWidth: Float): Pair<Float, Float> =
            (stageWidth - labelWidth - BUILD_INFO_PADDING) to BUILD_INFO_PADDING
    }
}

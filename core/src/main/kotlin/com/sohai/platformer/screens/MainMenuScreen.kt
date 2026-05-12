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
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager

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

        // --- Bottom navigation buttons ---
        root.add(buildNavButtons()).padTop(36f).row()

        stage.addActor(root)
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
}

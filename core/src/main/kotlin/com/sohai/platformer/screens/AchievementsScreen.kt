package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.input.GlobalInputRouter
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.progression.Achievement
import com.sohai.platformer.progression.AchievementRegistry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * T-108: AchievementsScreen
 *
 * Per-slot scrollable list of all achievements from [AchievementRegistry.ALL],
 * each rendered with a 32×32 icon, title, description, and an unlocked/locked
 * status label. Unlocked entries appear first (stable order); locked entries
 * are dimmed (icon at 50% alpha, muted text colors).
 *
 * Slot UX: top-row tabs let the user switch between save slots 1/2/3. The
 * active slot is highlighted; clicking a tab rebuilds the list. The initial
 * slot is supplied by the caller (defaults to 0 for MainMenu entry).
 *
 * Icon caching follows the same lazy-load pattern used in [AchievementToast]:
 * a private `HashMap<String, Texture>` keyed by achievement id, populated on
 * first row build, and disposed alongside the stage in [dispose].
 */
class AchievementsScreen(
    private val game: Game,
    initialSlotIndex: Int = 0
) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)

    private val titleFont = FontManager.getShared(32)
    private val rowTitleFont = FontManager.getShared(18)
    private val rowBodyFont = FontManager.getShared(14)

    /**
     * Lazy icon cache. Mirrors [AchievementToast]'s shape: keyed by achievement
     * id, populated on first render of each row, all entries disposed in
     * [dispose]. Missing files log a warning and leave the slot blank.
     */
    private val iconCache: MutableMap<String, Texture> = HashMap()

    private var currentSlotIndex: Int = initialSlotIndex.coerceIn(0, STATS_SLOT_FILES.size - 1)

    /** Root table — rebuilt whenever the active slot tab changes. */
    private val root = VisTable()

    init {
        // T-172 (Phase B): input wiring moved to show()/hide() via
        // GlobalInputRouter so this screen no longer clobbers the router's mux.
        root.setFillParent(true)
        stage.addActor(root)
        rebuild()
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private fun rebuild() {
        root.clear()
        root.top().padTop(36f).padLeft(30f).padRight(30f)

        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.85f, 1f))
        root.add(
            Label(Strings.format(StringKey.ACHIEVEMENTS_SCREEN_TITLE, currentSlotIndex + 1), titleStyle)
        ).padBottom(16f).row()

        // Slot tabs
        root.add(buildSlotTabs()).padBottom(16f).row()

        // List of achievement rows
        val list = VisTable()
        list.top().left()
        list.defaults().left().expandX().fillX().padBottom(10f)

        val slotState = loadSlotState(currentSlotIndex)
        val unlocked = slotState.unlockedAchievements
        val timestamps = slotState.achievementTimestamps
        val sorted = sortByUnlockedThenRegistration(AchievementRegistry.ALL, unlocked)
        for (achievement in sorted) {
            val isUnlocked = achievement.id in unlocked
            val unlockedAtMs = if (isUnlocked) timestamps[achievement.id] else null
            list.add(buildAchievementRow(achievement, isUnlocked, unlockedAtMs)).row()
        }

        val scroll = VisScrollPane(list)
        scroll.setFlickScroll(false)
        scroll.setFadeScrollBars(false)
        root.add(scroll).expand().fill().row()

        // Back button — bottom-center
        val backButton = VisTextButton(Strings.get(StringKey.ACHIEVEMENTS_BACK_BUTTON))
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@AchievementsScreen.dispose()
            }
        })
        root.add(backButton).size(220f, 52f).padTop(18f).padBottom(26f).center().row()
    }

    /** Builds the row of slot-selector tabs (1/2/3). */
    private fun buildSlotTabs(): VisTable {
        val tabs = VisTable()
        tabs.defaults().padLeft(6f).padRight(6f)
        for (slotIndex in STATS_SLOT_FILES.indices) {
            val label = if (slotIndex == currentSlotIndex) {
                "[" + Strings.format(StringKey.SLOT_LABEL, slotIndex + 1) + "]"
            } else {
                Strings.format(StringKey.SLOT_LABEL, slotIndex + 1)
            }
            val btn = VisTextButton(label)
            btn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (slotIndex != currentSlotIndex) {
                        currentSlotIndex = slotIndex
                        rebuild()
                    }
                }
            })
            tabs.add(btn).width(140f).height(40f)
        }
        return tabs
    }

    /**
     * Builds a single achievement row:
     * `[ICON 32×32]   TITLE (bold)  /  description (italic-ish)  /  status`.
     *
     * Locked rows render the icon at 50% alpha and use muted grey text.
     *
     * T-146: when [isUnlocked] is true, a "Unlocked: YYYY-MM-DD" line is appended
     * under the status row. [unlockedAtMs] = epoch ms in UTC; rendered in the
     * user's local timezone as ISO date. A null value (legacy unlock with no
     * recorded timestamp) renders the fallback "Unlocked: ?".
     */
    private fun buildAchievementRow(
        achievement: Achievement,
        isUnlocked: Boolean,
        unlockedAtMs: Long?
    ): VisTable {
        val row = VisTable()
        row.background("window")
        row.pad(10f)
        row.left()

        // Icon column
        val iconImage = VisImage()
        val tex = loadIcon(achievement)
        if (tex != null) {
            iconImage.drawable = TextureRegionDrawable(tex)
        }
        // Dim the icon when locked.
        iconImage.color = if (isUnlocked) Color(1f, 1f, 1f, 1f) else Color(1f, 1f, 1f, 0.5f)
        row.add(iconImage).size(ICON_SIZE, ICON_SIZE).padRight(14f).top()

        // Text column (title + description + status)
        val textCol = VisTable()
        textCol.left().top()

        val titleColor = if (isUnlocked) Color(1f, 0.92f, 0.3f, 1f) else Color(0.7f, 0.7f, 0.7f, 1f)
        val descColor = if (isUnlocked) Color(0.9f, 0.95f, 1f, 1f) else Color(0.55f, 0.55f, 0.55f, 1f)
        val titleStyle = Label.LabelStyle(rowTitleFont, titleColor)
        val descStyle = Label.LabelStyle(rowBodyFont, descColor)

        val titleLbl = Label(achievement.title, titleStyle)
        textCol.add(titleLbl).left().expandX().fillX().padBottom(2f).row()

        val descLbl = Label(achievement.desc, descStyle)
        descLbl.wrap = true
        textCol.add(descLbl).left().expandX().fillX().padBottom(4f).row()

        val statusKey =
            if (isUnlocked) StringKey.ACHIEVEMENT_UNLOCKED_LABEL else StringKey.ACHIEVEMENT_LOCKED_LABEL
        val statusColor =
            if (isUnlocked) Color(0.45f, 0.95f, 0.45f, 1f) else Color(0.6f, 0.6f, 0.6f, 1f)
        val statusStyle = Label.LabelStyle(rowBodyFont, statusColor)
        textCol.add(Label(Strings.get(statusKey), statusStyle)).left().row()

        // T-146: unlock-timestamp line under each unlocked row. Locked rows
        // get no timestamp line at all.
        if (isUnlocked) {
            val tsText = if (unlockedAtMs != null) {
                Strings.format(StringKey.ACHIEVEMENT_UNLOCKED_AT, formatUnlockDate(unlockedAtMs))
            } else {
                Strings.get(StringKey.ACHIEVEMENT_UNLOCKED_AT_UNKNOWN)
            }
            val tsStyle = Label.LabelStyle(rowBodyFont, Color(0.7f, 0.85f, 0.95f, 1f))
            textCol.add(Label(tsText, tsStyle)).left().padTop(2f).row()
        }

        row.add(textCol).left().expandX().fillX()
        return row
    }

    /**
     * T-146: Format an unlock-timestamp (epoch ms, UTC) as an ISO date
     * (`YYYY-MM-DD`) in the user's local timezone. Stable, locale-independent,
     * unambiguous — matches the ticket constraint.
     */
    private fun formatUnlockDate(epochMs: Long): String =
        DATE_FORMATTER.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

    // -------------------------------------------------------------------------
    // Data helpers
    // -------------------------------------------------------------------------

    /**
     * Lightweight projection of the slot save used by [rebuild] — just the
     * unlocked set + per-id timestamp map. Empty defaults if the slot has no
     * save on disk. Loading once per rebuild instead of twice (once for the
     * set, once for the map) keeps disk reads at parity with the pre-T-146
     * behavior.
     */
    private data class SlotSummary(
        val unlockedAchievements: Set<String>,
        val achievementTimestamps: Map<String, Long>
    )

    private fun loadSlotState(slotIndex: Int): SlotSummary {
        val file = STATS_SLOT_FILES[slotIndex]
        if (!SaveManager.hasSave(file)) {
            return SlotSummary(emptySet(), emptyMap())
        }
        val state = SaveManager.loadGame(file)
        return SlotSummary(state.unlockedAchievements, state.achievementTimestamps)
    }

    /**
     * Stable sort: unlocked first, then locked, each group preserving the
     * original [AchievementRegistry.ALL] registration order.
     */
    private fun sortByUnlockedThenRegistration(
        all: List<Achievement>,
        unlocked: Set<String>
    ): List<Achievement> {
        val groupA = all.filter { it.id in unlocked }
        val groupB = all.filter { it.id !in unlocked }
        return groupA + groupB
    }

    /**
     * Lazy-load the icon texture for [achievement]. Cached by id so the same
     * texture is reused across rebuilds (slot switches). Missing files log a
     * warning and return null (slot left blank), matching [AchievementToast].
     */
    private fun loadIcon(achievement: Achievement): Texture? {
        val cached = iconCache[achievement.id]
        if (cached != null) return cached
        val handle = Gdx.files.internal(achievement.iconPath)
        if (!handle.exists()) {
            Gdx.app.log("AchievementsScreen", "Icon not found: ${achievement.iconPath}")
            return null
        }
        val t = Texture(handle)
        iconCache[achievement.id] = t
        return t
    }

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = viewport.update(width, height, true)
    /**
     * T-172 (Phase B): re-install the router (any unmigrated sibling we came
     * from will have clobbered Gdx.input.inputProcessor) and push our stage so
     * scene2d events route here while F12/M-key globals remain wired.
     */
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
        // T-172 (Phase B): defensive pop in case dispose() is reached without
        // a preceding hide() (e.g. an exception path).
        GlobalInputRouter.popScreen(stage)
        iconCache.values.forEach { it.dispose() }
        iconCache.clear()
        stage.dispose()
    }

    companion object {
        private const val ICON_SIZE = 32f

        /**
         * T-146: ISO-format date (YYYY-MM-DD), no time component. Locale-
         * independent pattern so the value is unambiguous across regions.
         */
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

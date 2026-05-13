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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.atlas.CloudAtlasEntry
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.input.GlobalInputRouter
import com.sohai.platformer.persist.SaveManager

/**
 * Browses every Cloud Atlas snapshot the player has collected.
 * Shows locked silhouettes for entries not yet found, full text for collected ones.
 *
 * T-141: a small `VisTextField` at the top filters the visible list by
 * case-insensitive substring match against title + body summary. A clear
 * button (✕) resets the filter; the filter is transient (not persisted).
 */
class CloudAtlasScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont    = FontManager.getShared(32)
    private val entryFont    = FontManager.getShared(20)
    private val bodyFont     = FontManager.getShared(16)

    private var selectedEntry: CloudAtlasEntry? = null
    private var detailContainer: VisTable = VisTable()

    /** Container for the filterable list of entry rows; rebuilt on each filter change. */
    private val listTable: VisTable = VisTable()

    /** Current filter text (lowercased on apply). Transient — never persisted. */
    private var filterText: String = ""

    /** Pre-cached so we don't re-read SaveManager on every keystroke. */
    private val collectedIds: Set<String> = SaveManager.loadGame().collectedAtlasIds.toSet()

    init {
        // T-172 (Phase B): input wiring moved to show()/hide() via the
        // GlobalInputRouter so this screen no longer clobbers the router.

        val rootStyle      = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.85f, 1f))
        val countStyle     = Label.LabelStyle(bodyFont,  Color(0.7f, 0.9f, 1f, 1f))

        val root = VisTable()
        root.setFillParent(true)
        root.top().pad(40f)

        root.add(Label(Strings.get(StringKey.ATLAS_TITLE), rootStyle)).colspan(2).padBottom(8f).row()
        root.add(Label(
            Strings.format(StringKey.ATLAS_SNAPSHOTS_DISCOVERED, collectedIds.size, CloudAtlasLibrary.entries.size),
            countStyle
        )).colspan(2).padBottom(16f).row()

        // T-141: search row — VisTextField + clear button, right-aligned.
        val searchRow = VisTable()
        val searchField = VisTextField("")
        searchField.messageText = Strings.get(StringKey.ATLAS_SEARCH_PLACEHOLDER)
        searchField.setTextFieldListener { tf, _ ->
            filterText = tf.text
            rebuildListPane()
        }
        val btnClear = VisTextButton(Strings.get(StringKey.ATLAS_SEARCH_CLEAR))
        btnClear.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                searchField.text = ""
                filterText = ""
                rebuildListPane()
            }
        })
        searchRow.left()
        searchRow.add(searchField).width(300f).padRight(8f)
        searchRow.add(btnClear).width(80f)
        root.add(searchRow).colspan(2).left().padBottom(12f).row()

        // Left: list of entries (rebuilt by rebuildListPane).
        listTable.top().left()
        rebuildListPane()
        val listPane = ScrollPane(listTable)
        listPane.setScrollingDisabled(true, false)
        root.add(listPane).top().left().width(380f).height(420f).padRight(20f)

        // Right: selected entry detail (rebuilt on selection).
        detailContainer = VisTable()
        detailContainer.top().left()
        rebuildDetailPane()
        root.add(detailContainer).top().left().width(680f).height(420f).row()

        // Bottom: back button.
        val btnBack = VisTextButton(Strings.get(StringKey.ATLAS_BACK))
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@CloudAtlasScreen.dispose()
            }
        })
        root.add(btnBack).colspan(2).padTop(30f).size(220f, 52f)

        stage.addActor(root)
    }

    /** Rebuild the visible list of entries given [filterText]. */
    private fun rebuildListPane() {
        listTable.clearChildren()

        val unlockedStyle = Label.LabelStyle(entryFont, Color.WHITE)
        val lockedStyle   = Label.LabelStyle(entryFont, Color(0.45f, 0.45f, 0.45f, 1f))
        val emptyStyle    = Label.LabelStyle(bodyFont,  Color(0.6f, 0.6f, 0.6f, 1f))

        val filtered = filterEntries(CloudAtlasLibrary.entries.values, filterText)
        if (filtered.isEmpty()) {
            listTable.add(Label(Strings.get(StringKey.ATLAS_SEARCH_NO_RESULTS), emptyStyle))
                .left().pad(20f).row()
            return
        }

        for (entry in filtered) {
            val isUnlocked = entry.id in collectedIds
            val style = if (isUnlocked) unlockedStyle else lockedStyle
            val label = if (isUnlocked) entry.title else Strings.get(StringKey.ATLAS_LOCKED)
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
            listTable.add(btn).left().fillX().width(360f).padBottom(8f).row()
        }
    }

    private fun rebuildDetailPane() {
        detailContainer.clearChildren()
        val entry = selectedEntry
        val titleStyle    = Label.LabelStyle(entryFont, Color(0.3f, 1f, 0.85f, 1f))
        val subtitleStyle = Label.LabelStyle(bodyFont,  Color(0.7f, 0.9f, 1f, 1f))
        val bodyStyle     = Label.LabelStyle(bodyFont,  Color.WHITE)
        val infoStyle     = Label.LabelStyle(bodyFont,  Color(0.6f, 0.6f, 0.6f, 1f))

        if (entry == null) {
            detailContainer.add(Label(Strings.get(StringKey.ATLAS_SELECT_HINT), infoStyle)).left().pad(20f)
            return
        }
        detailContainer.add(Label(entry.title, titleStyle)).left().padBottom(6f).row()
        detailContainer.add(Label(entry.subtitle, subtitleStyle)).left().padBottom(12f).row()
        detailContainer.add(Label(Strings.format(StringKey.ATLAS_DISCOVERED_BY, entry.character), infoStyle)).left().padBottom(20f).row()
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
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }

    companion object {
        /**
         * Pure substring-match filter (case-insensitive) over title + body.
         * Subtitle and character are intentionally NOT searched — the ticket
         * scopes the match to title + summary text. Blank input returns all.
         *
         * Order is preserved relative to [source] iteration order.
         */
        fun filterEntries(source: Iterable<CloudAtlasEntry>, query: String): List<CloudAtlasEntry> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return source.toList()
            return source.filter { entry ->
                entry.title.lowercase().contains(q) || entry.body.lowercase().contains(q)
            }
        }
    }
}

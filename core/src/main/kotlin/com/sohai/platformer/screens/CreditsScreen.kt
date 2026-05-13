package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.input.GlobalInputRouter

/**
 * T-101: CreditsScreen.
 *
 * Scrollable credits page reachable from [SettingsScreen]'s footer. Renders a
 * sequence of sections (Game / Code Assistants / Art / Audio / Engine /
 * Climate Sources / Thanks), each with a [FontManager.getShared] (22) header
 * and [FontManager.getShared] (14) body lines.
 *
 * All text is sourced from [Strings] via [StringKey] — no hardcoded copy. Asset
 * URLs (when they appear in the rendered string) are baked into the i18n value
 * so the credits screen itself stays string-free.
 *
 * Back button sits bottom-center and returns to [SettingsScreen].
 */
class CreditsScreen(private val game: Game) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)

    private val titleFont = FontManager.getShared(32)
    private val sectionFont = FontManager.getShared(22)
    private val bodyFont = FontManager.getShared(14)

    init {
        // T-172 (Phase B): input wiring moved to show()/hide() via the
        // GlobalInputRouter so this screen no longer clobbers the router.
        buildUi()
    }

    private fun buildUi() {
        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.85f, 1f))
        val sectionStyle = Label.LabelStyle(sectionFont, Color(0.7f, 0.85f, 0.75f, 1f))
        val bodyStyle = Label.LabelStyle(bodyFont, Color(0.85f, 0.9f, 0.85f, 1f))
        val licenseStyle = Label.LabelStyle(bodyFont, Color(0.55f, 0.7f, 0.6f, 1f))

        val root = VisTable()
        root.setFillParent(true)
        root.top().padTop(36f).padLeft(30f).padRight(30f)

        // ── Title ───────────────────────────────────────────────────────────────
        root.add(Label(Strings.get(StringKey.CREDITS_TITLE), titleStyle))
            .padBottom(18f).row()

        // ── Scrollable content ──────────────────────────────────────────────────
        val content = VisTable()
        content.top().left()
        content.defaults().left().expandX().fillX()

        fun section(headerKey: StringKey, topPad: Float = 14f) {
            content.add(Label(Strings.get(headerKey), sectionStyle))
                .left().padTop(topPad).padBottom(4f).row()
            content.add(Separator()).left().fillX().width(640f).padBottom(8f).row()
        }

        fun body(key: StringKey, style: Label.LabelStyle = bodyStyle) {
            content.add(Label(Strings.get(key), style)).left().padBottom(3f).row()
        }

        // GAME
        section(StringKey.CREDITS_SECTION_GAME, topPad = 0f)
        body(StringKey.CREDITS_GAME_AUTHOR)
        body(StringKey.CREDITS_GAME_ROLE)
        body(StringKey.CREDITS_GAME_YEAR)

        // CODE ASSISTANTS
        section(StringKey.CREDITS_SECTION_CODE_ASSISTANTS)
        body(StringKey.CREDITS_CODE_CLAUDE)
        body(StringKey.CREDITS_CODE_COPILOT)
        body(StringKey.CREDITS_CODE_ANTIGRAVITY)
        body(StringKey.CREDITS_CODE_NOTEBOOKLM)

        // ART
        section(StringKey.CREDITS_SECTION_ART)
        body(StringKey.CREDITS_ART_KENNEY)
        body(StringKey.CREDITS_ART_PIXEL_LINE)
        body(StringKey.CREDITS_ART_PIXEL_REDUX)
        body(StringKey.CREDITS_ART_FOREST_TILESET)
        body(StringKey.CREDITS_ART_BLUEGRASS)
        body(StringKey.CREDITS_ART_KENNEY_LICENSE, licenseStyle)
        body(StringKey.CREDITS_ART_RESEARCH_NOTE, licenseStyle)

        // AUDIO
        section(StringKey.CREDITS_SECTION_AUDIO)
        body(StringKey.CREDITS_AUDIO_PROCEDURAL)
        body(StringKey.CREDITS_AUDIO_KENNEY_SFX)
        body(StringKey.CREDITS_AUDIO_RESEARCH_NOTE, licenseStyle)

        // ENGINE
        section(StringKey.CREDITS_SECTION_ENGINE)
        body(StringKey.CREDITS_ENGINE_LIBGDX)
        body(StringKey.CREDITS_ENGINE_BOX2D)
        body(StringKey.CREDITS_ENGINE_KOTLIN)
        body(StringKey.CREDITS_ENGINE_VISUI)
        body(StringKey.CREDITS_ENGINE_KOTEST)
        body(StringKey.CREDITS_ENGINE_GRADLE)

        // CLIMATE SOURCES
        section(StringKey.CREDITS_SECTION_CLIMATE_SOURCES)
        body(StringKey.CREDITS_CLIMATE_NOAA)
        body(StringKey.CREDITS_CLIMATE_NASA_EO)
        body(StringKey.CREDITS_CLIMATE_NASA_CLIMATE)
        body(StringKey.CREDITS_CLIMATE_NSIDC)
        body(StringKey.CREDITS_CLIMATE_USGS)
        body(StringKey.CREDITS_CLIMATE_IPCC)
        body(StringKey.CREDITS_CLIMATE_ARXIV)
        body(StringKey.CREDITS_CLIMATE_NOTE, licenseStyle)

        // THANKS
        section(StringKey.CREDITS_SECTION_THANKS)
        body(StringKey.CREDITS_THANKS_OPEN_SOURCE)
        body(StringKey.CREDITS_THANKS_PLAYERS)

        val scroll = VisScrollPane(content)
        scroll.setFlickScroll(false)
        scroll.setFadeScrollBars(false)
        root.add(scroll).expand().fill().padBottom(10f).row()

        // ── Back button — bottom-center ─────────────────────────────────────────
        val btnBack = VisTextButton(Strings.get(StringKey.CREDITS_BACK))
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = SettingsScreen(game)
                this@CreditsScreen.dispose()
            }
        })
        root.add(btnBack).size(220f, 52f).padBottom(26f).center().row()

        stage.addActor(root)
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

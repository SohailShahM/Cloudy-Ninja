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
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.input.GlobalInputRouter
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.util.ScreenshotWriter

class VictoryScreen(
    private val game: Game,
    private val finalScore: Int,
    private val bestTrialTime: Float? = null,
    private val isNewTimeBest: Boolean = false,
    private val priorBestTime: Float? = null,
    /**
     * T-139: id of the level that was just cleared. Used to compose the
     * screenshot filename `victory-{levelId}-{yyyyMMdd-HHmmss}.png`. Defaults
     * to `"final"` when the caller doesn't supply one (e.g. legacy tests).
     */
    private val clearedLevelId: String = "final"
) : Screen {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    private val stage = Stage(viewport)
    private val titleFont = FontManager.getShared(36)
    private val bodyFont = FontManager.getShared(22)
    private val toastFont = FontManager.getShared(16)

    /**
     * T-139: Small label anchored bottom-center that appears once the victory
     * screenshot lands on disk. Hidden if the write failed or smoke mode
     * short-circuited.
     */
    private val screenshotToast: Label = Label(
        ScreenshotWriter.TOAST_TEXT,
        Label.LabelStyle(toastFont, Color(0.85f, 0.85f, 0.85f, 1f))
    ).apply { isVisible = false }

    /** T-139: ensures we only attempt the capture once per VictoryScreen lifetime. */
    private var screenshotAttempted = false

    init {
        // T-172 (Phase B): input wiring moved to show()/hide() via the
        // GlobalInputRouter so this screen no longer clobbers the router.

        val titleStyle = Label.LabelStyle(titleFont, Color(0.3f, 1f, 0.5f, 1f))
        val bodyStyle  = Label.LabelStyle(bodyFont, Color.WHITE)
        val scoreStyle = Label.LabelStyle(bodyFont, Color(0.3f, 1f, 0.4f, 1f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        table.add(Label(Strings.get(StringKey.VICTORY_TITLE), titleStyle)).padBottom(20f).row()
        table.add(Label(Strings.get(StringKey.VICTORY_SUBTITLE), bodyStyle)).padBottom(12f).row()
        table.add(Label(Strings.format(StringKey.VICTORY_FINAL_SCORE, finalScore), scoreStyle)).padBottom(40f).row()

        if (bestTrialTime != null) {
            val mins  = (bestTrialTime / 60f).toInt()
            val secs  = (bestTrialTime % 60f).toInt()
            val tenth = ((bestTrialTime % 1f) * 10f).toInt()
            val timeStr = "%d:%02d.%d".format(mins, secs, tenth)
            val timeColor = if (isNewTimeBest) Color(0.1f, 0.95f, 0.85f, 1f) else Color(0.75f, 0.75f, 1f, 1f)
            table.add(Label(Strings.format(StringKey.VICTORY_TRIAL_TIME, timeStr), Label.LabelStyle(bodyFont, timeColor)))
                .padBottom(8f).row()
            if (priorBestTime != null) {
                val delta = bestTrialTime - priorBestTime
                if (delta != 0f) {
                    val absDelta = if (delta < 0f) -delta else delta
                    val deltaStr = if (delta < 0f) Strings.get(StringKey.VICTORY_DELTA_UNDER).format(absDelta)
                                   else Strings.get(StringKey.VICTORY_DELTA_OVER).format(absDelta)
                    val deltaColor = if (delta < 0f) Color(0.3f, 1f, 0.5f, 1f) else Color(0.75f, 0.75f, 0.75f, 1f)
                    table.add(Label(deltaStr, Label.LabelStyle(bodyFont, deltaColor)))
                        .padBottom(8f).row()
                }
            }
            if (isNewTimeBest) {
                val bestStyle = Label.LabelStyle(bodyFont, Color(1f, 0.85f, 0.1f, 1f))
                table.add(Label(Strings.get(StringKey.VICTORY_NEW_BEST), bestStyle)).padBottom(32f).row()
            }
        }

        val btnMenu = VisTextButton(Strings.get(StringKey.VICTORY_MAIN_MENU))
        btnMenu.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = MainMenuScreen(game)
                this@VictoryScreen.dispose()
            }
        })
        table.add(btnMenu).size(220f, 60f).padBottom(14f).row()

        val btnReplay = VisTextButton(Strings.get(StringKey.VICTORY_PLAY_AGAIN))
        btnReplay.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val level1 = LevelManager.getLevel("level1") ?: return
                game.screen = GameScreen(level1, game)
                this@VictoryScreen.dispose()
            }
        })
        table.add(btnReplay).size(220f, 60f)

        stage.addActor(table)

        // T-139: bottom-center toast — populated in show() once the screenshot
        // write returns success. Anchored manually rather than nested in the
        // center table so it doesn't shift the menu/replay buttons.
        screenshotToast.setPosition(
            (Constants.VIRTUAL_WIDTH - 360f) / 2f,
            20f
        )
        screenshotToast.setSize(360f, 24f)
        stage.addActor(screenshotToast)
    }

    /**
     * T-139: capture the framebuffer once on first VictoryScreen entry and
     * show the toast if the write succeeded. Smoke mode short-circuits
     * BEFORE pixmap allocation so CI never writes to the runner's home dir.
     */
    private fun maybeCaptureVictoryScreenshot() {
        if (screenshotAttempted) return
        screenshotAttempted = true
        if (Constants.SMOKE_MODE) return
        val ok = ScreenshotWriter.captureAndWrite(clearedLevelId)
        screenshotToast.isVisible = ok
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()

        // T-139: capture after stage.draw() so the framebuffer holds the
        // rendered victory screen, not the cleared backdrop. Runs exactly once.
        maybeCaptureVictoryScreenshot()
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
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
}

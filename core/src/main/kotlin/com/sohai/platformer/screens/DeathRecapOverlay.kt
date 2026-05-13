package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.entities.DeathCause
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings

/**
 * T-130: Small post-death recap overlay.
 *
 * Shown by [LevelRunState] after the T-097 death animation completes
 * (instant-show under reducedMotion / SMOKE_MODE). Displays:
 *  - cause of death (enemy / hazard / fall / boss attack)
 *  - time into the level
 *  - stomps this run
 *  - eco-tokens this run
 *
 * Plus two buttons — **Retry?** (restart level) and **Quit to menu** (return
 * to MainMenu). Auto-dismisses to the retry path after [AUTO_DISMISS_SECONDS]
 * so the loop continues to flow rapidly if the player just keeps moving.
 *
 * State machine: `idle → showing → dismissed`. `show()` flips idle→showing
 * and resets the timer; `tick()` advances the timer; `triggerRetry()` /
 * `triggerQuit()` flip showing→dismissed and call the appropriate host
 * callback exactly once.
 *
 * Respects [com.sohai.platformer.persist.Settings.reducedMotion] via the
 * `reducedMotion` constructor flag — no fade animation when on, just instant
 * show/hide.
 */
class DeathRecapOverlay(
    private val onRetry: () -> Unit,
    private val onQuit: () -> Unit,
    private val reducedMotion: Boolean = false,
) : Disposable {

    enum class State { IDLE, SHOWING, DISMISSED }

    companion object {
        /** Wall-clock seconds before auto-dismiss fires the Retry path. */
        const val AUTO_DISMISS_SECONDS = 3f
        /** 0→1 fade-in duration. Bypassed when reducedMotion is on. */
        const val FADE_IN_SECONDS = 0.2f
    }

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    val stage = Stage(viewport)
    private val shapeRenderer = ShapeRenderer()

    private val titleFont = FontManager.getShared(32)
    private val bodyFont  = FontManager.getShared(18)

    // ── State machine ────────────────────────────────────────────────────────

    var state: State = State.IDLE
        private set

    /** Time spent in the SHOWING state, in seconds. */
    var elapsed: Float = 0f
        private set

    /** Snapshot of the run stats at the moment show() was called. */
    var snapshot: Snapshot? = null
        private set

    private val titleLabel: Label
    private val causeLabel: Label
    private val timeLabel: Label
    private val stompsLabel: Label
    private val tokensLabel: Label

    data class Snapshot(
        val cause: DeathCause,
        val timeIntoLevel: Float,
        val stompsThisRun: Int,
        val tokensThisRun: Int,
    )

    init {
        val titleStyle = Label.LabelStyle(titleFont, Color(0.95f, 0.55f, 0.25f, 1f))
        val bodyStyle  = Label.LabelStyle(bodyFont,  Color(0.92f, 0.92f, 0.92f, 1f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()

        titleLabel  = Label(Strings.get(StringKey.DEATH_RECAP_TITLE), titleStyle)
        causeLabel  = Label("", bodyStyle)
        timeLabel   = Label("", bodyStyle)
        stompsLabel = Label("", bodyStyle)
        tokensLabel = Label("", bodyStyle)

        table.add(titleLabel).padBottom(18f).row()
        table.add(causeLabel).padBottom(6f).row()
        table.add(timeLabel).padBottom(6f).row()
        table.add(stompsLabel).padBottom(6f).row()
        table.add(tokensLabel).padBottom(24f).row()

        val btnRetry = VisTextButton(Strings.get(StringKey.DEATH_RECAP_RETRY))
        btnRetry.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { triggerRetry() }
        })
        table.add(btnRetry).size(220f, 50f).padBottom(10f).row()

        val btnQuit = VisTextButton(Strings.get(StringKey.DEATH_RECAP_QUIT))
        btnQuit.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { triggerQuit() }
        })
        table.add(btnQuit).size(220f, 50f).row()

        stage.addActor(table)
    }

    /**
     * Public API — IDLE → SHOWING. Captures the run stats snapshot and resets
     * the auto-dismiss timer. No-op if already showing or dismissed.
     */
    fun show(snapshot: Snapshot) {
        if (state != State.IDLE) return
        this.snapshot = snapshot
        elapsed = 0f
        state = State.SHOWING
        causeLabel.setText(Strings.format(StringKey.DEATH_RECAP_CAUSE, causeText(snapshot.cause)))
        timeLabel.setText(Strings.format(StringKey.DEATH_RECAP_TIME, formatTime(snapshot.timeIntoLevel)))
        stompsLabel.setText(Strings.format(StringKey.DEATH_RECAP_STOMPS, snapshot.stompsThisRun))
        tokensLabel.setText(Strings.format(StringKey.DEATH_RECAP_TOKENS, snapshot.tokensThisRun))
    }

    /**
     * Advance the auto-dismiss timer. When [elapsed] crosses
     * [AUTO_DISMISS_SECONDS] this fires the Retry path exactly once. Safe to
     * call every frame; no-op when not in SHOWING.
     */
    fun tick(delta: Float) {
        if (state != State.SHOWING) return
        elapsed += delta
        if (elapsed >= AUTO_DISMISS_SECONDS) {
            triggerRetry()
        }
    }

    /** Public API — SHOWING → DISMISSED via Retry. Idempotent. */
    fun triggerRetry() {
        if (state != State.SHOWING) return
        state = State.DISMISSED
        onRetry()
    }

    /** Public API — SHOWING → DISMISSED via Quit. Idempotent. */
    fun triggerQuit() {
        if (state != State.SHOWING) return
        state = State.DISMISSED
        onQuit()
    }

    /** Whether the overlay is currently visible (state == SHOWING). */
    val isShowing: Boolean get() = state == State.SHOWING

    fun render() {
        if (state != State.SHOWING) return

        val k = if (reducedMotion) {
            // Instant show — no fade-in.
            1f
        } else {
            // 0→1 smooth-step fade-in. Real-time delta so the curve plays at the
            // same wall-clock rate regardless of gameplay tick rate.
            val t = (elapsed / FADE_IN_SECONDS).coerceIn(0f, 1f)
            t * t * (3f - 2f * t)
        }

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Dim full-screen backdrop.
        shapeRenderer.setColor(0f, 0f, 0f, 0.55f * k)
        shapeRenderer.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)

        // Centered card.
        val cardW = 380f
        val cardH = 320f
        val cardX = (Constants.VIRTUAL_WIDTH  - cardW) / 2f
        val cardY = (Constants.VIRTUAL_HEIGHT - cardH) / 2f
        shapeRenderer.setColor(0.10f, 0.08f, 0.10f, 0.92f * k)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)
        // Accent stripe at the top of the card.
        shapeRenderer.setColor(0.95f, 0.55f, 0.25f, 0.9f * k)
        shapeRenderer.rect(cardX, cardY + cardH - 4f, cardW, 4f)

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        stage.root.color.a = k
        stage.act()
        stage.draw()
        stage.root.color.a = 1f
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        shapeRenderer.dispose()
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }

    private fun causeText(cause: DeathCause): String = Strings.get(when (cause) {
        DeathCause.ENEMY       -> StringKey.DEATH_CAUSE_ENEMY
        DeathCause.HAZARD      -> StringKey.DEATH_CAUSE_HAZARD
        DeathCause.FALL        -> StringKey.DEATH_CAUSE_FALL
        DeathCause.BOSS_ATTACK -> StringKey.DEATH_CAUSE_BOSS_ATTACK
    })

    private fun formatTime(seconds: Float): String {
        val total = seconds.coerceAtLeast(0f).toInt()
        val m = total / 60
        val s = total % 60
        return "%d:%02d".format(m, s)
    }
}

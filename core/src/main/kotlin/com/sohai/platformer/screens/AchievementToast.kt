package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.sohai.platformer.FontManager
import com.sohai.platformer.progression.Achievement

/**
 * Slides in from the top-right corner, holds for [HOLD_DURATION] seconds,
 * then fades out.  Internal queue ensures toasts never overlap.
 *
 * Lifecycle: create once and hand to [GameScreen]; call [update] and [render]
 * every frame (batch must NOT be in begin() — this class owns its own Stage).
 */
class AchievementToast(
    private val viewportWidth: Float,
    private val viewportHeight: Float
) : Disposable {

    companion object {
        private const val SLIDE_DURATION = 0.35f   // seconds to slide in / slide out
        private const val HOLD_DURATION  = 2.4f    // seconds to hold at rest
        private const val FADE_DURATION  = 0.4f    // seconds to fade out
        private const val TOAST_WIDTH    = 280f    // virtual pixels
        private const val TOAST_HEIGHT   = 64f
        private const val PAD_RIGHT      = 16f     // gap from right edge (virtual px)
        private const val PAD_TOP        = 16f     // gap from top edge (virtual px)
        private const val TOTAL_DURATION = SLIDE_DURATION + HOLD_DURATION + FADE_DURATION
    }

    // ── State machine ─────────────────────────────────────────────────────────

    private enum class Phase { IDLE, SLIDE_IN, HOLD, FADE_OUT }

    private data class Queued(val achievement: Achievement)

    private val queue = ArrayDeque<Queued>()
    private var phase = Phase.IDLE
    private var phaseTimer = 0f
    private var currentAchievement: Achievement? = null

    // ── Rendering resources ───────────────────────────────────────────────────

    private val viewport = FitViewport(viewportWidth, viewportHeight)
    private val stage = Stage(viewport)

    // Background panel texture (semi-transparent dark rectangle)
    private val bgTexture: Texture
    private val bgImage: VisImage

    // Labels using shared FreeType fonts (NOT VisUI baked skin fonts — per LEARNINGS.md T-044)
    private val titleFont = FontManager.getShared(20)
    private val descFont  = FontManager.getShared(16)
    private val titleLabel: VisLabel
    private val descLabel: VisLabel

    // Container table — moved off-screen initially
    private val toastTable: VisTable

    init {
        // Create a 1x1 dark semi-transparent texture for the background panel
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(0.08f, 0.08f, 0.12f, 0.88f))
        pixmap.fill()
        bgTexture = Texture(pixmap)
        pixmap.dispose()

        bgImage = VisImage(bgTexture)

        val titleStyle = Label.LabelStyle(titleFont, Color(1f, 0.92f, 0.3f, 1f))   // gold
        val descStyle  = Label.LabelStyle(descFont,  Color(0.85f, 0.85f, 0.85f, 1f))

        titleLabel = VisLabel("", titleStyle)
        titleLabel.setWrap(false)
        descLabel  = VisLabel("", descStyle)
        descLabel.setWrap(false)

        // Stack: background image behind the text table
        val stack = com.badlogic.gdx.scenes.scene2d.ui.Stack()

        bgImage.setSize(TOAST_WIDTH, TOAST_HEIGHT)

        val textTable = VisTable()
        textTable.pad(8f)
        textTable.add(titleLabel).left().expandX().fillX().row()
        textTable.add(descLabel).left().expandX().fillX()

        stack.add(bgImage)
        stack.add(textTable)

        toastTable = VisTable()
        toastTable.add(stack).width(TOAST_WIDTH).height(TOAST_HEIGHT)

        // Anchor to top-right; start fully off-screen to the right
        toastTable.setSize(TOAST_WIDTH, TOAST_HEIGHT)
        toastTable.setPosition(viewportWidth, viewportHeight - PAD_TOP - TOAST_HEIGHT)

        stage.addActor(toastTable)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Queue an achievement toast. If a toast is already showing it will display
     * after the current one finishes, so they never overlap.
     */
    fun show(achievement: Achievement) {
        queue.addLast(Queued(achievement))
    }

    fun update(delta: Float) {
        // Start the next queued toast when idle
        if (phase == Phase.IDLE && queue.isNotEmpty()) {
            val next = queue.removeFirst()
            currentAchievement = next.achievement
            titleLabel.setText(next.achievement.title)
            descLabel.setText(next.achievement.desc)
            toastTable.color.a = 1f
            phase = Phase.SLIDE_IN
            phaseTimer = 0f
            Gdx.app.log("AchievementToast", "Showing: ${next.achievement.id}")
        }

        when (phase) {
            Phase.IDLE -> return

            Phase.SLIDE_IN -> {
                phaseTimer += delta
                val t = (phaseTimer / SLIDE_DURATION).coerceIn(0f, 1f)
                // Ease: smoothstep
                val ease = t * t * (3f - 2f * t)
                val restX = viewportWidth - PAD_RIGHT - TOAST_WIDTH
                val offX  = viewportWidth   // fully off-screen right
                toastTable.setPosition(offX + (restX - offX) * ease,
                    viewportHeight - PAD_TOP - TOAST_HEIGHT)
                if (phaseTimer >= SLIDE_DURATION) {
                    toastTable.setPosition(restX, viewportHeight - PAD_TOP - TOAST_HEIGHT)
                    phase = Phase.HOLD
                    phaseTimer = 0f
                }
            }

            Phase.HOLD -> {
                phaseTimer += delta
                if (phaseTimer >= HOLD_DURATION) {
                    phase = Phase.FADE_OUT
                    phaseTimer = 0f
                }
            }

            Phase.FADE_OUT -> {
                phaseTimer += delta
                val t = (phaseTimer / FADE_DURATION).coerceIn(0f, 1f)
                toastTable.color.a = 1f - t
                if (phaseTimer >= FADE_DURATION) {
                    toastTable.color.a = 0f
                    toastTable.setPosition(viewportWidth, viewportHeight - PAD_TOP - TOAST_HEIGHT)
                    phase = Phase.IDLE
                    phaseTimer = 0f
                    currentAchievement = null
                }
            }
        }

        stage.act(delta)
    }

    /**
     * Render the toast. Call after the HUD layer (Layer 4) and before the pause
     * overlay (Layer 7) — i.e. at Layer 4.5 in GameScreen.render().
     * The [batch] should NOT be in begin() state when this is called.
     */
    fun render(batch: SpriteBatch) {
        if (phase == Phase.IDLE && queue.isEmpty()) return
        // Update viewport to match the current window size so the toast scales correctly
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, false)
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height, false)
    }

    override fun dispose() {
        bgTexture.dispose()
        stage.dispose()
        // titleFont/descFont are shared — do NOT dispose them
    }
}

package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings

/**
 * T-137: First-run tutorial overlay shown on the player's first entry into the
 * Sky Sanctuary hub (Level0_0). Three hint cards introduce movement, character
 * swap, and portal entry. Dismissed by any key (or touch on mobile) — the host
 * screen flips [isDismissed] to true and the GameState `tutorialSeen` flag is
 * persisted so the overlay never appears again for that save slot.
 *
 * Modeled after [PauseOverlay] / [CloudAtlasOverlay]: ShapeRenderer dim
 * backdrop, Scene2D Stage for the modal content, FontManager.getShared() for
 * fonts (no per-instance dispose).
 *
 * Reduced-motion behavior: when [reducedMotion] is true the fade-in animation
 * is skipped and the overlay snaps to fully visible on first frame.
 *
 * Smoke-CI behavior: the dismiss path reads `Gdx.input.isKeyJustPressed(ANY_KEY)`
 * via a small fan-out plus `Gdx.input.justTouched()`, so the autopilot's first
 * synthetic keypress closes the overlay and unblocks player movement.
 */
class HubTutorialOverlay(
    private val onDismiss: () -> Unit,
    private val reducedMotion: Boolean = false
) : Disposable {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    val stage = Stage(viewport)
    private val sr = ShapeRenderer()

    private val titleFont = FontManager.getShared(32)
    private val bodyFont  = FontManager.getShared(20)
    private val hintFont  = FontManager.getShared(14)

    // Match PauseOverlay's fade timings so the visual language stays consistent.
    private var fadeT = if (reducedMotion) 1f else 0f
    private val FADE_IN_SECONDS = 0.2f
    private val DIM_TARGET_ALPHA = 0.55f

    /**
     * Once true, [render] is a no-op past dismissal and the host screen should
     * dispose this overlay + persist `tutorialSeen=true`. Single-shot — set by
     * a key press / touch on any frame after the first render tick (so the
     * autopilot's first input always lands).
     */
    var isDismissed = false
        private set

    /**
     * One frame of grace so the overlay actually renders before [dismiss] can
     * fire. Without this, an input from the same frame the overlay opens (e.g.
     * the SPACE press that closed the previous menu) would close this one
     * immediately and the player never sees the hints.
     */
    private var inputArmedAfterFrames = 0
    private val INPUT_ARM_FRAMES = 1

    init {
        val titleStyle = Label.LabelStyle(titleFont, Color(0.95f, 0.95f, 1f, 1f))
        val bodyStyle  = Label.LabelStyle(bodyFont,  Color.WHITE)
        val hintStyle  = Label.LabelStyle(hintFont,  Color(0.6f, 0.6f, 0.6f, 0.9f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()
        table.pad(60f)

        table.add(Label(Strings.get(StringKey.TUTORIAL_TITLE), titleStyle)).padBottom(28f).row()

        // Three hint cards — each is a single wrapped Label row. No new assets,
        // no per-card decorations beyond vertical spacing (constraint: "no new
        // assets, pause-overlay-style modal").
        val hintKeys = listOf(
            StringKey.TUTORIAL_HINT_MOVE,
            StringKey.TUTORIAL_HINT_SWAP,
            StringKey.TUTORIAL_HINT_PORTAL
        )
        for (key in hintKeys) {
            val lbl = Label(Strings.get(key), bodyStyle)
            lbl.wrap = true
            table.add(lbl).width(640f).padBottom(18f).row()
        }

        table.add(Label(Strings.get(StringKey.TUTORIAL_DISMISS_HINT), hintStyle))
            .padTop(20f)

        stage.addActor(table)
    }

    /**
     * Public hook for external callers (e.g. tests) to force-dismiss without
     * synthesizing keyboard input. Idempotent.
     */
    fun dismiss() {
        if (isDismissed) return
        isDismissed = true
        onDismiss()
    }

    /**
     * Returns true if a real player input arrived this frame. Split out so
     * tests can stub the input layer if needed and so the dismissal logic is
     * easy to read.
     *
     * Smoke CI relies on this: the autopilot synthesizes movement key presses
     * (A/D/SPACE) within the first second, all of which match `isKeyJustPressed`
     * for "any key pressed this frame". Touch is included for mobile parity.
     */
    private fun anyInputJustPressed(): Boolean {
        // isKeyJustPressed(Input.Keys.ANY_KEY) is the libGDX-blessed way to
        // detect "any key down this frame". Touch is checked separately because
        // ANY_KEY does not capture pointer input.
        return Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.justTouched()
    }

    fun render() {
        if (isDismissed) return

        // Advance fade-in. Real-time delta — Sky Sanctuary's render loop ticks
        // even with the overlay up (the player can still move underneath),
        // but the visual fade is independent of the game tick rate. When
        // reducedMotion is set, fadeT is pinned to 1 so the smooth-step below
        // collapses to a no-op.
        if (!reducedMotion) {
            val dt = Gdx.graphics.deltaTime
            fadeT = (fadeT + dt / FADE_IN_SECONDS).coerceAtMost(1f)
        }
        val k = fadeT * fadeT * (3f - 2f * fadeT)

        // Dim backdrop
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        sr.projectionMatrix = stage.camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.setColor(0f, 0f, 0f, DIM_TARGET_ALPHA * k)
        sr.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        sr.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        stage.root.color.a = k
        stage.act()
        stage.draw()
        stage.root.color.a = 1f

        // Arm input on the second-rendered frame so the visual lands first.
        if (inputArmedAfterFrames < INPUT_ARM_FRAMES) {
            inputArmedAfterFrames++
            return
        }
        if (anyInputJustPressed()) {
            dismiss()
        }
    }

    fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    override fun dispose() {
        stage.dispose()
        sr.dispose()
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }
}

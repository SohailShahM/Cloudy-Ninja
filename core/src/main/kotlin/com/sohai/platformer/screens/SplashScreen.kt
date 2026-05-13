package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.ProceduralMusicGenerator
import com.sohai.platformer.audio.ProceduralSoundGenerator
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings

/**
 * Cold-start splash screen (T-104).
 *
 * Shows a horizontal progress bar that tracks real asset-preload work. The
 * preload is executed incrementally across [PreloadStep] entries — one step
 * per frame — so the bar fill mirrors actual progress rather than a fake
 * animation.
 *
 * ## Transition gate
 *
 * The splash transitions to [MainMenuScreen] only when **both** of the
 * following are true:
 *   1. [progress] has reached `1f` (every preload step has run).
 *   2. The elapsed wall-clock time since splash creation is ≥ [MIN_DURATION_S].
 *
 * The minimum duration prevents flash-frames on fast machines where preload
 * completes in <16 ms.
 *
 * ## Smoke mode bypass
 *
 * In smoke-mode runs (`-Dcloudy.smokeMode=true`) the splash is never
 * instantiated — [com.sohai.platformer.Main.create] short-circuits straight
 * to the requested smoke-level [GameScreen]. The 1-second minimum is also
 * skipped when [smokeBypass] is true (used by tests).
 *
 * ## Preload steps
 *
 * The work surfaced through the bar is the same work the previous
 * `Main.create()` performed synchronously before showing the menu:
 *
 *  - **fonts** — prime the shared font cache (1 step).
 *  - **music** — procedural WAV generation + warm the [MusicManager]
 *    pre-decoder (3 steps, one per track).
 *  - **sfx** — procedural SFX generation + [SoundManager.init] (1 step).
 *
 * Total of 5 logical steps. This number is intentionally not exposed as a
 * constant — the bar tracks `completedSteps / totalSteps`, computed from the
 * length of the runtime step list, so adding/removing steps doesn't break the
 * UI.
 *
 * The preload functions are themselves idempotent (each writes-if-missing
 * for generators, no-ops on second [SoundManager.init]), so smoke runs that
 * skip the splash and the next cold start that does run the splash both
 * converge to the same on-disk state.
 */
class SplashScreen(
    private val game: Game,
    /**
     * If true, the 1-second minimum-duration gate is removed and the splash
     * transitions as soon as preload reports done. Used by tests; production
     * callers should leave this at the default.
     */
    private val smokeBypass: Boolean = Constants.SMOKE_MODE,
) : Screen {

    companion object {
        /** Minimum wall-clock seconds the splash must be visible. */
        const val MIN_DURATION_S: Float = 1.0f
    }

    // ── Preload step model ───────────────────────────────────────────────────

    /** One unit of preload work, executed on a single render frame. */
    internal class PreloadStep(val label: String, val run: () -> Unit)

    /**
     * Preload steps, in execution order. Public to the package so tests can
     * inspect length and drive [tick] until completion without GL.
     */
    internal val steps: MutableList<PreloadStep> = mutableListOf(
        PreloadStep("Generating music: ambient_arid")  {
            ProceduralMusicGenerator.generateOne("ambient_arid")
        },
        PreloadStep("Generating music: ambient_wind")  {
            ProceduralMusicGenerator.generateOne("ambient_wind")
        },
        PreloadStep("Generating music: ambient_eco")   {
            ProceduralMusicGenerator.generateOne("ambient_eco")
        },
        PreloadStep("Generating sound effects")        {
            ProceduralSoundGenerator.generateAll()
        },
        PreloadStep("Loading sound effects")           {
            SoundManager.init()
            MusicManager.preloadAll()
        },
    )

    /** Index of the next [PreloadStep] to run. When == [steps].size, preload is complete. */
    internal var nextStep: Int = 0

    /** Wall-clock seconds since the splash was constructed. */
    internal var elapsed: Float = 0f

    /** True once [transition] has been invoked — set so we never transition twice. */
    internal var transitioned: Boolean = false

    /** Most-recent status string (drawn under the bar). */
    internal var currentLabel: String = "Initialising…"

    /**
     * Factory for the screen we transition to once preload + timer gate both
     * pass. Default constructs a [MainMenuScreen]; tests override this so
     * they can verify the transition without instantiating GL-bound screens.
     */
    internal var nextScreenFactory: () -> Screen = { MainMenuScreen(game) }

    // ── GL state (lazily-constructed; left null in test path) ─────────────────

    private var viewport: FitViewport? = null
    private var stage: Stage? = null
    private var shapes: ShapeRenderer? = null
    private var label: Label? = null

    init {
        // Build GL resources only if a real Gdx graphics context exists. Tests
        // allocate this screen via sun.misc.Unsafe.allocateInstance and never
        // invoke the constructor, so this block runs only in production.
        if (Gdx.graphics != null && Gdx.gl != null) {
            val vp = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
            viewport = vp
            stage = Stage(vp)
            shapes = ShapeRenderer()

            val titleFont = FontManager.getShared(48)
            val style = Label.LabelStyle(titleFont, Color(0.85f, 0.95f, 1f, 1f))
            val l = Label(Strings.get(StringKey.MAIN_TITLE), style)
            l.setPosition(
                Constants.VIRTUAL_WIDTH / 2f - 140f,
                Constants.VIRTUAL_HEIGHT / 2f + 60f,
            )
            stage?.addActor(l)
            label = l

            Gdx.input.inputProcessor = stage
        }
    }

    // ── Public testable surface ──────────────────────────────────────────────

    /**
     * Progress fraction in [0f, 1f] — `completedSteps / totalSteps`.
     * If [steps] is empty, returns 1f (preload trivially complete).
     */
    val progress: Float
        get() = if (steps.isEmpty()) 1f else nextStep.toFloat() / steps.size

    /** True once every preload step has run. */
    val preloadDone: Boolean
        get() = nextStep >= steps.size

    /**
     * True iff [transition] should be called this frame: preload is done AND
     * (smoke-bypass OR elapsed ≥ [MIN_DURATION_S]).
     */
    val shouldTransition: Boolean
        get() = preloadDone && (smokeBypass || elapsed >= MIN_DURATION_S)

    /**
     * Drive the splash forward by [delta] seconds.
     *
     * Runs at most one preload step per call, updates [elapsed], and refreshes
     * [currentLabel]. Used directly by [render] in production and by tests to
     * step the state machine without a GL context.
     */
    internal fun tick(delta: Float) {
        elapsed += delta
        if (nextStep < steps.size) {
            val step = steps[nextStep]
            currentLabel = step.label
            try {
                step.run()
            } catch (t: Throwable) {
                Gdx.app?.error("SplashScreen", "Preload step failed: ${step.label}", t)
            }
            nextStep += 1
            if (nextStep >= steps.size) {
                currentLabel = "Ready"
            }
        }
    }

    // ── libGDX Screen lifecycle ──────────────────────────────────────────────

    override fun show() = Unit

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.07f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        tick(delta)

        // Title text via Stage.
        val s = stage
        if (s != null) {
            s.viewport.apply()
            s.act(delta)
            s.draw()
        }

        // Progress bar via ShapeRenderer in virtual coords.
        val sr = shapes
        val vp = viewport
        if (sr != null && vp != null) {
            sr.projectionMatrix = vp.camera.combined
            val barX = Constants.VIRTUAL_WIDTH / 2f - 200f
            val barY = Constants.VIRTUAL_HEIGHT / 2f - 30f
            val barW = 400f
            val barH = 18f

            sr.begin(ShapeRenderer.ShapeType.Filled)
            // Track
            sr.color = Color(0.15f, 0.18f, 0.22f, 1f)
            sr.rect(barX, barY, barW, barH)
            // Fill — clamped to [0, barW]
            sr.color = Color(0.30f, 0.85f, 0.95f, 1f)
            sr.rect(barX, barY, barW * progress.coerceIn(0f, 1f), barH)
            sr.end()

            sr.begin(ShapeRenderer.ShapeType.Line)
            sr.color = Color(0.85f, 0.95f, 1f, 1f)
            sr.rect(barX, barY, barW, barH)
            sr.end()
        }

        if (shouldTransition && !transitioned) {
            transition()
        }
    }

    /** Move the [Game] over to the [nextScreenFactory] target exactly once. */
    internal fun transition() {
        if (transitioned) return
        transitioned = true
        game.setScreen(nextScreenFactory())
    }

    override fun resize(width: Int, height: Int) {
        viewport?.update(width, height, true)
    }

    override fun pause() = Unit
    override fun resume() = Unit
    override fun hide() = Unit

    override fun dispose() {
        stage?.dispose()
        shapes?.dispose()
    }
}

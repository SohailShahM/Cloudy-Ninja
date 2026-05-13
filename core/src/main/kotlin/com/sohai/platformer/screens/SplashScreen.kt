package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
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
import com.sohai.platformer.input.GlobalInputRouter

/**
 * Cold-start splash screen (T-104, T-129).
 *
 * Shows a horizontal progress bar that tracks real asset-preload work. The
 * preload is executed incrementally across [PreloadStep] entries — one step
 * per frame — so the bar fill mirrors actual progress rather than a fake
 * animation.
 *
 * ## Transition gate
 *
 * The splash transitions to [MainMenuScreen] only when **all** of the
 * following are true:
 *   1. [progress] has reached `1f` (every preload step has run).
 *   2. The elapsed wall-clock time since splash creation is ≥ [MIN_DURATION_S].
 *   3. (T-129) The player has pressed any key or clicked — **OR** [smokeBypass]
 *      is true.
 *
 * The minimum duration prevents flash-frames on fast machines where preload
 * completes in <16 ms. The user-gesture gate (T-129) pre-bakes the contract
 * a future HTML5/WebGL build (T-123 Option 2) will hard-require: browsers
 * refuse to start an `AudioContext` until the page has received a user
 * gesture. Routing every desktop and web cold-start through the same gate
 * means the web port doesn't need a runtime fork. When the gate fires we
 * call [MusicManager.releaseAudioGate] so the menu / first level can begin
 * playing music.
 *
 * ## Hint label
 *
 * After gates (1) and (2) are met, a small "Press any key to continue" hint
 * appears under the progress bar. The hint is intentionally hidden during
 * preload — showing it while the bar is still moving would prompt the
 * player to bash keys before audio assets are actually generated.
 *
 * ## Smoke mode bypass
 *
 * In smoke-mode runs (`-Dcloudy.smokeMode=true`) the splash is never
 * instantiated — [com.sohai.platformer.Main.create] short-circuits straight
 * to the requested smoke-level [GameScreen] and releases the audio gate
 * itself. The 1-second minimum and the user-input gate are also skipped
 * when [smokeBypass] is true (used by tests).
 *
 * ## Preload steps
 *
 * The work surfaced through the bar is the same work the previous
 * `Main.create()` performed synchronously before showing the menu:
 *
 *  - **fonts** — prime the shared font cache (1 step).
 *  - **music** — procedural WAV generation + warm the [MusicManager]
 *    pre-decoder (4 steps, one per track — T-134 added `ambient_menu`).
 *  - **sfx** — procedural SFX generation + [SoundManager.init] (1 step).
 *
 * Total of 6 logical steps. This number is intentionally not exposed as a
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
        PreloadStep("Generating music: ambient_menu")  {
            ProceduralMusicGenerator.generateOne("ambient_menu")
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
     * T-129: True once the player has pressed any key or clicked. Flips the
     * third leg of the transition gate. In [smokeBypass] mode this stays
     * false but [shouldTransition] short-circuits past it.
     */
    internal var inputReceived: Boolean = false

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

    /** T-129: hint shown under the bar once preload + timer gates are met. */
    private var hintLabel: Label? = null

    /**
     * T-172 (Phase B): the any-key/any-touch gate InputAdapter built once in
     * [init] and pushed/popped through [GlobalInputRouter] in [show] / [hide].
     * Kept as a field so [hide] can pop the exact instance that [show] pushed.
     */
    private var inputGate: InputProcessor? = null

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

            // T-129: hint label sits under the progress bar. We add it now
            // but keep it invisible (color.a = 0f) until preload + the timer
            // gate are met — see render() below.
            val hintFont = FontManager.getShared(24)
            val hintStyle = Label.LabelStyle(hintFont, Color(0.85f, 0.95f, 1f, 0f))
            val hint = Label(Strings.get(StringKey.SPLASH_PRESS_ANY_KEY), hintStyle)
            hint.setPosition(
                Constants.VIRTUAL_WIDTH / 2f - 140f,
                Constants.VIRTUAL_HEIGHT / 2f - 80f,
            )
            stage?.addActor(hint)
            hintLabel = hint

            // T-129: build an InputAdapter alongside the Stage that fires on any
            // key/touch so the press-any-key gate flips. T-172 (Phase B): the
            // adapter is now pushed onto the [GlobalInputRouter] in [show] (and
            // popped in [hide]) so it cooperates with the F12 / M-key globals
            // instead of clobbering them with a private InputMultiplexer.
            val gate = object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean {
                    onUserInput()
                    return false
                }
                override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
                    onUserInput()
                    return false
                }
            }
            inputGate = gate
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
     * True iff both pre-input gates have cleared: preload is done AND
     * (smoke-bypass OR elapsed ≥ [MIN_DURATION_S]).
     *
     * The hint label is shown only while [readyForInput] is true and
     * [inputReceived] is still false. Splitting this from [shouldTransition]
     * lets the render loop draw the hint at exactly the right moment.
     */
    val readyForInput: Boolean
        get() = preloadDone && (smokeBypass || elapsed >= MIN_DURATION_S)

    /**
     * True iff [transition] should be called this frame: preload is done,
     * the timer gate has cleared, AND (T-129) a user gesture has been
     * received OR [smokeBypass] is on.
     */
    val shouldTransition: Boolean
        get() = readyForInput && (smokeBypass || inputReceived)

    /**
     * True iff the "Press any key to continue" hint should be visible this
     * frame: both pre-input gates met and no input received yet. False in
     * smoke-bypass (the splash is exited before the hint would draw).
     */
    val showsHint: Boolean
        get() = readyForInput && !inputReceived && !smokeBypass

    /**
     * Handle a user-input event from the InputAdapter installed in [init].
     * Sets [inputReceived] and opens the [MusicManager] audio gate so the
     * MainMenu / first level may begin playing music.
     *
     * Idempotent — repeat events are no-ops.
     */
    internal fun onUserInput() {
        if (inputReceived) return
        // Only count input as "gate cleared" once preload + timer have
        // already passed. Otherwise an over-eager keystroke during the 1s
        // minimum or during preload would skip ahead.
        if (!readyForInput) return
        inputReceived = true
        MusicManager.releaseAudioGate()
    }

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

    /**
     * T-172 (Phase B): install the router and push both the stage and the
     * any-key/any-touch gate so the press-any-key flow + scene2d focus both
     * coexist with the F12/M-key globals registered in [com.sohai.platformer.Main.create].
     *
     * Order matters: pushScreen prepends, so to preserve the pre-T-172 dispatch
     * order of the legacy `InputMultiplexer(gate, stage)` (gate fires first),
     * we push the stage first and then the gate — that lands the gate at
     * index 0 and the stage at index 1.
     */
    override fun show() {
        GlobalInputRouter.install()
        val s = stage
        if (s != null) GlobalInputRouter.pushScreen(s)
        val g = inputGate
        if (g != null) GlobalInputRouter.pushScreen(g)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.07f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        tick(delta)

        // T-129: toggle the hint's visibility every frame so it appears
        // precisely at the moment both gates clear. Alpha 0 / 1 lets the
        // label sit in the Stage without reflowing layout.
        hintLabel?.let { hl ->
            val a = if (showsHint) 1f else 0f
            if (hl.color.a != a) hl.color.a = a
        }

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

    /**
     * Move the [Game] over to the [nextScreenFactory] target exactly once.
     *
     * T-129: also ensures [MusicManager.releaseAudioGate] has been called so
     * the next screen can begin playing music. In the smoke-bypass path
     * [onUserInput] never fires; this guarantees the gate is open before
     * MainMenu / GameScreen runs.
     */
    internal fun transition() {
        if (transitioned) return
        transitioned = true
        MusicManager.releaseAudioGate()
        game.setScreen(nextScreenFactory())
    }

    override fun resize(width: Int, height: Int) {
        viewport?.update(width, height, true)
    }

    override fun pause() = Unit
    override fun resume() = Unit
    /**
     * T-172 (Phase B): pop both pushed processors so the next screen owns the
     * router. Pop the gate first (it was pushed last), then the stage.
     */
    override fun hide() {
        val g = inputGate
        if (g != null) GlobalInputRouter.popScreen(g)
        val s = stage
        if (s != null) GlobalInputRouter.popScreen(s)
    }

    override fun dispose() {
        // T-172 (Phase B): defensive pop covers dispose() reached without hide().
        val g = inputGate
        if (g != null) GlobalInputRouter.popScreen(g)
        val s = stage
        if (s != null) GlobalInputRouter.popScreen(s)
        stage?.dispose()
        shapes?.dispose()
    }
}

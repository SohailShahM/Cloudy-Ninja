package com.sohai.platformer.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import sun.misc.Unsafe

/**
 * Tests for [SplashScreen] (T-104).
 *
 * [SplashScreen]'s constructor instantiates a [com.badlogic.gdx.utils.viewport.FitViewport],
 * [com.badlogic.gdx.scenes.scene2d.Stage] and [com.badlogic.gdx.graphics.glutils.ShapeRenderer]
 * when a real Gdx graphics context exists — all GL-bound and unavailable in
 * headless JUnit. Following the established pattern (see
 * [com.sohai.platformer.rendering.ScreenFadeTest]), we allocate the screen
 * via `sun.misc.Unsafe.allocateInstance` so neither the constructor nor any
 * property initializer runs, then drive the pure-Kotlin state machine via
 * a small reflection helper that sets the private fields.
 *
 * What is verified here:
 *  - [SplashScreen.progress] reaches `1f` once every stub step has run.
 *  - The transition gate ([SplashScreen.shouldTransition]) fires only when
 *    **both** the preload-done flag is set AND elapsed time ≥
 *    [SplashScreen.MIN_DURATION_S].
 *  - Smoke-bypass mode (`smokeBypass = true`) skips the 1-second wait.
 *  - [SplashScreen.transition] only invokes [Game.setScreen] once even when
 *    invoked repeatedly past the gate (idempotence).
 *  - tick() with no remaining steps still advances `elapsed` and can clear
 *    the timer gate.
 */
class SplashScreenTest : BehaviorSpec({

    // ── Gdx.app shim (so the catch{} log inside tick() doesn't NPE) ──────────

    val prevApp: Application? = Gdx.app

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)
    }

    afterSpec {
        Gdx.app = prevApp
    }

    // ── Reflection helpers ───────────────────────────────────────────────────

    val unsafe: Unsafe = run {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as Unsafe
    }

    /** Allocate a [SplashScreen] without running the constructor or any initializers. */
    fun allocBare(): SplashScreen {
        return unsafe.allocateInstance(SplashScreen::class.java) as SplashScreen
    }

    fun setField(target: Any, name: String, value: Any?) {
        val f = SplashScreen::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    /**
     * Install [n] no-op preload steps on [s] and clear [nextStep] / [elapsed].
     * Each step increments [counter][0] so we can verify side-effects ran.
     */
    fun primeWith(s: SplashScreen, n: Int, smokeBypass: Boolean, counter: IntArray) {
        val steps = mutableListOf<SplashScreen.PreloadStep>()
        repeat(n) { idx ->
            steps.add(SplashScreen.PreloadStep("stub-$idx") { counter[0] += 1 })
        }
        setField(s, "steps", steps)
        setField(s, "nextStep", 0)
        setField(s, "elapsed", 0f)
        setField(s, "transitioned", false)
        setField(s, "smokeBypass", smokeBypass)
        setField(s, "currentLabel", "Initialising…")
    }

    /** Inject a [Game] mock so [SplashScreen.transition] can call setScreen. */
    fun setGame(s: SplashScreen, game: Game) {
        setField(s, "game", game)
    }

    fun readElapsed(s: SplashScreen): Float {
        val f = SplashScreen::class.java.getDeclaredField("elapsed")
        f.isAccessible = true
        return f.getFloat(s)
    }

    // ── 1. progress mirrors completed-step count ─────────────────────────────

    given("a SplashScreen primed with 5 stub steps") {
        val s = allocBare()
        val counter = IntArray(1)
        primeWith(s, 5, smokeBypass = true, counter = counter)

        `when`("progress is read before any tick() call") {
            then("progress is 0f") {
                s.progress shouldBe (0f plusOrMinus 0.001f)
            }
            then("preloadDone is false") {
                s.preloadDone.shouldBeFalse()
            }
        }

        `when`("tick(0.1f) is called five times") {
            repeat(5) { s.tick(0.1f) }

            then("every stub step ran exactly once (counter == 5)") {
                counter[0] shouldBe 5
            }
            then("progress reaches 1f") {
                s.progress shouldBe (1f plusOrMinus 0.001f)
            }
            then("preloadDone is true") {
                s.preloadDone.shouldBeTrue()
            }
        }
    }

    // ── 2. preloadDone alone is NOT enough — timer gate must also be met ─────

    given("a SplashScreen NOT in smoke-bypass with 2 steps") {
        val s = allocBare()
        val counter = IntArray(1)
        primeWith(s, 2, smokeBypass = false, counter = counter)

        `when`("both steps run but elapsed is < MIN_DURATION_S") {
            s.tick(0.05f)   // elapsed = 0.05, runs step 0
            s.tick(0.05f)   // elapsed = 0.10, runs step 1

            then("preloadDone is true") {
                s.preloadDone.shouldBeTrue()
            }
            then("shouldTransition is FALSE — timer gate not met") {
                s.shouldTransition.shouldBeFalse()
            }
        }

        `when`("subsequent ticks accumulate past MIN_DURATION_S without running new steps") {
            // Keep ticking; nextStep is already at steps.size so tick() does nothing
            // except advance elapsed.
            repeat(20) { s.tick(0.1f) }   // +2.0s

            then("shouldTransition becomes TRUE once timer ≥ MIN_DURATION_S") {
                s.shouldTransition.shouldBeTrue()
            }
            then("no extra step invocations happened (still 2)") {
                counter[0] shouldBe 2
            }
        }
    }

    // ── 3. smoke-bypass skips the timer gate ─────────────────────────────────

    given("a SplashScreen in smoke-bypass mode with 3 steps") {
        val s = allocBare()
        val counter = IntArray(1)
        primeWith(s, 3, smokeBypass = true, counter = counter)

        `when`("all steps run in well under MIN_DURATION_S") {
            s.tick(0.001f)
            s.tick(0.001f)
            s.tick(0.001f)

            then("elapsed is far below MIN_DURATION_S (sanity)") {
                (readElapsed(s) < SplashScreen.MIN_DURATION_S).shouldBeTrue()
            }
            then("shouldTransition is TRUE — smoke-bypass skips the timer gate") {
                s.shouldTransition.shouldBeTrue()
            }
        }
    }

    // ── 4. transition() is invoked at most once (idempotence) ────────────────

    given("a SplashScreen ready to transition") {
        val s = allocBare()
        val counter = IntArray(1)
        primeWith(s, 1, smokeBypass = true, counter = counter)

        val gameMock = mockk<Game>(relaxed = true)
        val captured = slot<Screen>()
        every { gameMock.setScreen(capture(captured)) } answers { /* no-op */ }
        setGame(s, gameMock)
        // Replace the next-screen factory with a stub so we don't construct
        // a real MainMenuScreen (which needs a GL context).
        val stubNext = mockk<Screen>(relaxed = true)
        setField(s, "nextScreenFactory", { stubNext })

        `when`("tick runs the lone step and transition() is invoked twice") {
            s.tick(0.5f)         // runs step 0, preload done, smoke-bypass on
            s.shouldTransition.shouldBeTrue()

            s.transition()       // first: should flip transitioned + setScreen
            s.transition()       // second: must NOT invoke setScreen again

            then("setScreen was invoked exactly once") {
                verify(exactly = 1) { gameMock.setScreen(any<Screen>()) }
            }
            then("a screen was captured by the setScreen mock") {
                captured.isCaptured.shouldBeTrue()
            }
        }
    }

    // ── 5. tick() past completion still advances elapsed (clears timer gate) ──

    given("a SplashScreen whose preload has already finished") {
        val s = allocBare()
        val counter = IntArray(1)
        primeWith(s, 1, smokeBypass = false, counter = counter)
        s.tick(0.1f)   // runs the lone step

        `when`("tick is invoked again with no work remaining") {
            val before = counter[0]
            s.tick(0.5f)
            s.tick(0.5f)

            then("no extra step ran") {
                counter[0] shouldBe before
            }
            then("elapsed advanced — eventually clears the timer gate") {
                s.shouldTransition.shouldBeTrue()
            }
        }
    }

    // ── 6. empty step list → progress is immediately 1f and preloadDone true ─

    given("a SplashScreen with zero preload steps") {
        val s = allocBare()
        val counter = IntArray(1)
        primeWith(s, 0, smokeBypass = true, counter = counter)

        `when`("progress is read before any tick() runs") {
            then("progress is 1f (trivial completion)") {
                s.progress shouldBe (1f plusOrMinus 0.001f)
            }
            then("preloadDone is true") {
                s.preloadDone.shouldBeTrue()
            }
            then("shouldTransition is TRUE — smoke-bypass, preload-done") {
                s.shouldTransition.shouldBeTrue()
            }
        }
    }
})

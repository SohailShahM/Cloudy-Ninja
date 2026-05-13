package com.sohai.platformer.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

/**
 * Tests for the T-171 [GlobalInputRouter] scaffold.
 *
 * The router is a thin wrapper around [com.badlogic.gdx.InputMultiplexer] that
 * exposes pushScreen / popScreen / register semantics + an [isActive] probe.
 * The high-value invariants verified here:
 *
 *  1. `install()` writes the router's mux to `Gdx.input.inputProcessor`.
 *  2. `pushScreen()` adds at the **front** (screen wins priority); `register()`
 *     adds at the **end** (global handlers see leftovers).
 *  3. `popScreen()` removes a specific processor.
 *  4. `isActive()` reports true after install, false when another processor is
 *     installed (the legacy `Gdx.input.inputProcessor = stage` pattern that
 *     unmigrated screens still use during Phase A).
 *  5. **Ordering invariant** (the load-bearing one for the whole design): a
 *     `pushScreen()`'d stage receives `keyDown` before a `register()`'d global.
 *     If this regresses, every global hotkey would steal events from screens.
 *
 * `Gdx.input` is mocked because the router writes to it directly and reads it
 * back in `isActive()`. The mock uses a [slot] to capture the value that
 * `install()` writes, so subsequent `Gdx.input.inputProcessor` reads return
 * the same instance — mirroring what real libGDX would do.
 */
class GlobalInputRouterTest : BehaviorSpec({

    val prevInput = try { Gdx.input } catch (t: Throwable) { null }

    beforeSpec {
        // A slot-backed mock: install() writes to inputProcessor, and reads
        // return whatever was last written. This is what real libGDX does;
        // a relaxed mock without the slot wiring would return null on read
        // and the isActive() invariant couldn't be exercised.
        val processorSlot = slot<InputProcessor>()
        var currentProcessor: InputProcessor? = null
        Gdx.input = mockk(relaxed = true) {
            every { inputProcessor = capture(processorSlot) } answers {
                currentProcessor = processorSlot.captured
            }
            every { inputProcessor } answers { currentProcessor }
        }
    }

    afterSpec {
        // Restore prior Gdx.input (typically null in headless test runs).
        Gdx.input = prevInput
    }

    beforeEach {
        GlobalInputRouter.resetForTest()
    }

    given("a fresh GlobalInputRouter") {
        `when`("install() is called") {
            GlobalInputRouter.install()

            then("Gdx.input.inputProcessor identifies as the router") {
                GlobalInputRouter.isActive().shouldBeTrue()
            }
        }

        `when`("a different processor is installed directly (legacy path)") {
            GlobalInputRouter.install()
            // Simulate an unmigrated screen doing `Gdx.input.inputProcessor = stage`.
            Gdx.input.inputProcessor = InputAdapter()

            then("isActive() returns false so polling fallbacks fire") {
                GlobalInputRouter.isActive().shouldBeFalse()
            }
        }
    }

    given("processor ordering inside the multiplexer") {
        `when`("a global is registered and a screen is pushed") {
            val callOrder = mutableListOf<String>()
            val global = object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean {
                    callOrder += "global"
                    return false // don't consume
                }
            }
            val screen = object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean {
                    callOrder += "screen"
                    return false // don't consume so the global still fires
                }
            }

            GlobalInputRouter.install()
            GlobalInputRouter.register(global)
            GlobalInputRouter.pushScreen(screen)

            // Drive the mux directly. We can't use Gdx.input here because the
            // mock doesn't dispatch — we feed the captured processor instead.
            val mux = Gdx.input.inputProcessor!!
            mux.keyDown(Input.Keys.F12)

            then("screen receives keyDown BEFORE global (push goes to front, register to end)") {
                callOrder shouldBe listOf("screen", "global")
            }
        }

        `when`("popScreen() removes a specific stage") {
            val callOrder = mutableListOf<String>()
            val screenA = object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean {
                    callOrder += "A"; return false
                }
            }
            val screenB = object : InputAdapter() {
                override fun keyDown(keycode: Int): Boolean {
                    callOrder += "B"; return false
                }
            }

            GlobalInputRouter.install()
            GlobalInputRouter.pushScreen(screenA)
            GlobalInputRouter.pushScreen(screenB) // now front: B, A
            GlobalInputRouter.popScreen(screenA)

            val mux = Gdx.input.inputProcessor!!
            mux.keyDown(Input.Keys.M)

            then("only the un-popped screen receives the event") {
                callOrder shouldBe listOf("B")
            }
        }
    }
})

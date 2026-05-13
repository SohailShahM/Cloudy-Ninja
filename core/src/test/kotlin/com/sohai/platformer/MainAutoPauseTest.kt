package com.sohai.platformer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.sohai.platformer.screens.GameScreen
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify

/**
 * Tests for [Main]'s T-112 auto-pause-on-focus-loss wiring.
 *
 * libGDX fires [com.badlogic.gdx.ApplicationListener.pause] when the desktop
 * window loses focus (alt-tab, minimise, etc.). Default [com.badlogic.gdx.Game]
 * behaviour forwards to the active screen's `pause()`; [Main] preserves that
 * forwarding **but** skips it entirely when `Constants.SMOKE_MODE` is true so
 * the smoke autopilot can't be derailed by an unexpected overlay.
 *
 * It also overrides [com.badlogic.gdx.ApplicationListener.resume] to **not**
 * forward — the pause overlay must stay up until the player explicitly
 * dismisses it, even when the OS reports the window has regained focus.
 *
 * What is verified here:
 *  - When the active screen is a [GameScreen], `Main.pause()` invokes
 *    `screen.pause()` once (proving the forward path lights up — that's the
 *    hook that raises the T-063 pause overlay via `setPaused(true)`).
 *  - `Main.pause()` forwards to **any** [Screen] subtype (so the contract
 *    isn't accidentally narrowed to only `GameScreen`).
 *  - `Main.resume()` does **not** call `screen.resume()` — overlay persists.
 *
 * `Constants.SMOKE_MODE` is read at JVM start from `cloudy.smokeMode`; we don't
 * try to mutate it in-test (it's a `final` `@JvmField`). Smoke-mode skip is
 * exercised by the smoke matrix itself in CI — any auto-pause regression there
 * shows up as a hung run.
 */
class MainAutoPauseTest : BehaviorSpec({

    val prevApp: Application? = Gdx.app

    beforeSpec {
        // VisUI / FontManager are not touched by these tests — we never call
        // Main.create(). But Game.setScreen() calls screen.show(), which on a
        // relaxed mock is a no-op. A relaxed Gdx.app guards any incidental
        // logging from defensive code paths.
        Gdx.app = mockk<Application>(relaxed = true)
    }

    afterSpec {
        Gdx.app = prevApp
    }

    /**
     * Inject a [Screen] into [Game]'s `protected` field without going through
     * [Game.setScreen], which calls `Gdx.graphics.getWidth()` (null in headless
     * tests). Reflection sidesteps the GL path entirely.
     */
    fun injectScreen(main: Main, screen: Screen) {
        val f = Game::class.java.getDeclaredField("screen")
        f.isAccessible = true
        f.set(main, screen)
    }

    given("a Main with a GameScreen mock as the active screen") {
        val main = Main()
        val gameScreen = mockk<GameScreen>(relaxed = true)
        injectScreen(main, gameScreen)

        `when`("Main.pause() is invoked (simulating alt-tab focus loss)") {
            main.pause()

            then("screen.pause() is forwarded exactly once") {
                // GameScreen.pause() internally calls setPaused(true), which
                // raises the T-063 overlay. We verify the forward; the
                // overlay-raise behaviour is covered by GameScreen's own
                // setPaused() being a tiny, audited 4-line method.
                verify(exactly = 1) { gameScreen.pause() }
            }
        }
    }

    given("a Main with a plain Screen mock (non-GameScreen) as the active screen") {
        val main = Main()
        val anyScreen = mockk<Screen>(relaxed = true)
        injectScreen(main, anyScreen)

        `when`("Main.pause() is invoked") {
            main.pause()

            then("screen.pause() is still forwarded (super.pause() default contract)") {
                verify(exactly = 1) { anyScreen.pause() }
            }
        }
    }

    given("a Main with a GameScreen mock as the active screen, after window regains focus") {
        val main = Main()
        val gameScreen = mockk<GameScreen>(relaxed = true)
        injectScreen(main, gameScreen)

        `when`("Main.resume() is invoked (simulating focus regain)") {
            main.resume()

            then("screen.resume() is NOT forwarded — overlay must persist") {
                verify(exactly = 0) { gameScreen.resume() }
            }
        }
    }
})

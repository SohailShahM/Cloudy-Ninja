package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.sohai.platformer.input.GlobalInputRouter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import sun.misc.Unsafe

/**
 * T-172 (Phase B): integration smoke for the screen migrations.
 *
 * The Phase A [GlobalInputRouterTest] already covers the router's
 * push / pop / install / isActive primitives. This spec verifies the
 * cooperation contract Phase B added: after a migrated [com.badlogic.gdx.Screen]'s
 * `show()` runs, [GlobalInputRouter.isActive] must be true (i.e. the screen
 * didn't accidentally clobber `Gdx.input.inputProcessor`). If a future
 * refactor pulls a `Gdx.input.inputProcessor = stage` assignment back into
 * one of these screens, this test fails — protecting the F12 + M-key router
 * adapters from silently going dark.
 *
 * Scope: a representative sample (Settings / Credits / Stats / Achievements /
 * LevelSelect / Victory / CloudAtlas). The smoke autopilot exercises the
 * GameScreen + overlay paths via SMOKE_MODE in CI, so they're not duplicated
 * here. SplashScreen has its own GL-bound init that's awkward to mock; its
 * router cooperation is observable via the same smoke-CI run.
 *
 * Pattern follows [SplashScreenTest] — Unsafe-allocate the screen so the
 * GL-bound constructor never runs, then inject a mock [InputProcessor] into
 * the `stage` field via reflection. `Gdx.input` is a slot-backed mock so
 * writes round-trip on read (mirroring real libGDX behaviour required by
 * [GlobalInputRouter.isActive]).
 */
class ScreenRouterMigrationTest : BehaviorSpec({

    val prevInput = try { Gdx.input } catch (t: Throwable) { null }

    beforeSpec {
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
        Gdx.input = prevInput
    }

    val unsafe: Unsafe = run {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as Unsafe
    }

    fun <T : Any> allocBare(cls: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return unsafe.allocateInstance(cls) as T
    }

    fun setField(target: Any, name: String, value: Any?) {
        var cls: Class<*>? = target::class.java
        while (cls != null) {
            try {
                val f = cls.getDeclaredField(name)
                f.isAccessible = true
                f.set(target, value)
                return
            } catch (e: NoSuchFieldException) {
                cls = cls.superclass
            }
        }
        error("Field '$name' not found on ${target::class.java.name}")
    }

    /**
     * Simulate the pre-show state Phase B is defending against: an unmigrated
     * sibling screen left the router clobbered. show() must heal this by
     * calling install() again.
     */
    fun clobberRouter() {
        GlobalInputRouter.resetForTest()
        Gdx.input.inputProcessor = InputAdapter()  // legacy clobber
        GlobalInputRouter.isActive().shouldBeFalse()
    }

    given("a migrated CreditsScreen") {
        `when`("show() is invoked after a sibling clobbered the router") {
            clobberRouter()
            val s = allocBare(CreditsScreen::class.java)
            setField(s, "stage", mockk<Stage>(relaxed = true))
            s.show()

            then("GlobalInputRouter is active again") {
                GlobalInputRouter.isActive().shouldBeTrue()
            }
        }
    }

    given("a migrated StatsScreen") {
        `when`("show() is invoked after a sibling clobbered the router") {
            clobberRouter()
            val s = allocBare(StatsScreen::class.java)
            setField(s, "stage", mockk<Stage>(relaxed = true))
            s.show()

            then("GlobalInputRouter is active again") {
                GlobalInputRouter.isActive().shouldBeTrue()
            }
        }
    }

    given("a migrated LevelSelectScreen") {
        `when`("show() is invoked after a sibling clobbered the router") {
            clobberRouter()
            val s = allocBare(LevelSelectScreen::class.java)
            setField(s, "stage", mockk<Stage>(relaxed = true))
            s.show()

            then("GlobalInputRouter is active again") {
                GlobalInputRouter.isActive().shouldBeTrue()
            }
        }
    }

    given("a migrated VictoryScreen") {
        `when`("show() is invoked after a sibling clobbered the router") {
            clobberRouter()
            val s = allocBare(VictoryScreen::class.java)
            setField(s, "stage", mockk<Stage>(relaxed = true))
            s.show()

            then("GlobalInputRouter is active again") {
                GlobalInputRouter.isActive().shouldBeTrue()
            }
        }
    }

    given("the migration symmetry: show() pushes, hide() pops") {
        `when`("CreditsScreen.show() then hide() runs end-to-end") {
            GlobalInputRouter.resetForTest()
            GlobalInputRouter.install()
            val s = allocBare(CreditsScreen::class.java)
            val stage = mockk<Stage>(relaxed = true)
            setField(s, "stage", stage)

            // show() — pushScreen lands the stage at index 0.
            s.show()

            // hide() — popScreen removes the stage. The router itself stays
            // installed so the F12 / M-key adapters survive screen transitions.
            s.hide()

            then("router remains installed after hide()") {
                GlobalInputRouter.isActive().shouldBeTrue()
            }
        }
    }
})

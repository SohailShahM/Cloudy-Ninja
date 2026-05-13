package com.sohai.platformer.input

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Pure-function tests for [RestartHoldTracker] (T-133).
 *
 * The tracker has no libGDX dependency, so this spec exercises it directly
 * without any reflection seeding or MockK setup. Coverage required by the
 * T-133 brief:
 *   1. Hold-timer increments per update tick.
 *   2. Releasing before 0.5s aborts — timer resets, no fire.
 *   3. >=0.5s of continuous hold fires exactly once.
 *   4. A second restart requires a fresh hold (key must release first).
 *   5. Progress reads 0..1 monotonically while held, and 0 on release.
 */
class RestartHoldTrackerTest : BehaviorSpec({

    given("a freshly-constructed tracker") {
        `when`("update(0.1f, held=false) is called") {
            then("heldSeconds stays 0 and no fire") {
                val t = RestartHoldTracker()
                t.update(0.1f, held = false).shouldBeFalse()
                t.heldSeconds shouldBe 0f
                t.isHolding().shouldBeFalse()
                t.progress() shouldBe 0f
            }
        }
    }

    given("a tracker accumulating short hold ticks") {
        `when`("two 0.1s ticks while held") {
            then("heldSeconds advances by dt; no fire yet (< 0.5s)") {
                val t = RestartHoldTracker()
                t.update(0.1f, held = true).shouldBeFalse()
                t.update(0.1f, held = true).shouldBeFalse()
                t.heldSeconds shouldBe (0.2f plusOrMinus 1e-5f)
                t.isHolding().shouldBeTrue()
                // 0.2 / 0.5 = 0.4
                t.progress() shouldBe (0.4f plusOrMinus 1e-5f)
            }
        }
    }

    given("a hold released before the 0.5s threshold") {
        `when`("0.4s held then released") {
            then("the timer resets to 0 and never fires") {
                val t = RestartHoldTracker()
                t.update(0.2f, held = true).shouldBeFalse()
                t.update(0.2f, held = true).shouldBeFalse()
                t.heldSeconds shouldBe (0.4f plusOrMinus 1e-5f)

                // Release — must abort
                t.update(0.016f, held = false).shouldBeFalse()
                t.heldSeconds shouldBe 0f
                t.isHolding().shouldBeFalse()
                t.progress() shouldBe 0f
            }
        }
    }

    given("a continuous hold past the 0.5s threshold") {
        `when`("the tracker is ticked across 0.5s in a single update") {
            then("it fires exactly once") {
                val t = RestartHoldTracker()
                t.update(0.5f, held = true).shouldBeTrue()
                t.progress() shouldBe 1f
            }
        }

        `when`("the tracker reaches 0.5s in a sequence of ticks") {
            then("it fires on the tick that crosses the threshold (not before)") {
                val t = RestartHoldTracker()
                t.update(0.2f, held = true).shouldBeFalse() // 0.2
                t.update(0.2f, held = true).shouldBeFalse() // 0.4
                t.update(0.1f, held = true).shouldBeTrue()  // 0.5 — fire
            }
        }

        `when`("the player keeps holding past the threshold") {
            then("further updates do NOT re-fire (latched)") {
                val t = RestartHoldTracker()
                t.update(0.5f, held = true).shouldBeTrue()
                t.update(0.1f, held = true).shouldBeFalse()
                t.update(0.5f, held = true).shouldBeFalse()
                t.update(1.0f, held = true).shouldBeFalse()
            }
        }
    }

    given("a tracker that already fired once") {
        `when`("the key is released and then held again past the threshold") {
            then("a second restart fires (fresh hold required)") {
                val t = RestartHoldTracker()
                t.update(0.5f, held = true).shouldBeTrue()  // first fire
                t.update(0.1f, held = true).shouldBeFalse() // still held, no re-fire
                t.update(0.05f, held = false).shouldBeFalse() // release
                t.heldSeconds shouldBe 0f
                t.update(0.4f, held = true).shouldBeFalse() // re-hold, not yet
                t.update(0.1f, held = true).shouldBeTrue()  // second fire
            }
        }
    }

    given("progress() over a partial hold") {
        `when`("0.25s of hold has accumulated (half-way)") {
            then("progress() reports 0.5") {
                val t = RestartHoldTracker()
                t.update(0.25f, held = true).shouldBeFalse()
                t.progress() shouldBe (0.5f plusOrMinus 1e-5f)
            }
        }
    }

    given("reset()") {
        `when`("a hold is in flight and reset() is called") {
            then("heldSeconds returns to 0 and progress() reports 0") {
                val t = RestartHoldTracker()
                t.update(0.3f, held = true)
                t.heldSeconds shouldBe (0.3f plusOrMinus 1e-5f)
                t.reset()
                t.heldSeconds shouldBe 0f
                t.progress() shouldBe 0f
                t.isHolding().shouldBeFalse()
            }
        }
    }

    given("a custom hold duration") {
        `when`("threshold is 1.0s and 0.5s elapses") {
            then("no fire yet — progress reports 0.5") {
                val t = RestartHoldTracker(holdDurationSeconds = 1.0f)
                t.update(0.5f, held = true).shouldBeFalse()
                t.progress() shouldBe (0.5f plusOrMinus 1e-5f)
                t.update(0.5f, held = true).shouldBeTrue() // reaches 1.0s
            }
        }
    }
})

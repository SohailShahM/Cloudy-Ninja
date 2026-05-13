package com.sohai.platformer.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * T-142: pure-function tests for [SpeedrunTimerFormat.format].
 *
 * The formatter is called once per frame from the gameplay HUD when the
 * speedrun-timer toggle is on. Correctness of the millisecond column and
 * minute carry-over is load-bearing — a `01:00.000` flash at the 59.999s
 * mark would be a visible glitch to speedrunners.
 */
class SpeedrunTimerFormatTest : BehaviorSpec({

    given("zero") {
        `when`("format(0f)") {
            then("renders 00:00.000") {
                SpeedrunTimerFormat.format(0f) shouldBe "00:00.000"
            }
        }
    }

    given("a sub-second time") {
        `when`("format(0.5f)") {
            then("renders 00:00.500") {
                SpeedrunTimerFormat.format(0.5f) shouldBe "00:00.500"
            }
        }
    }

    given("a multi-second time with fractional component") {
        `when`("format(12.345f)") {
            then("renders 00:12.345") {
                SpeedrunTimerFormat.format(12.345f) shouldBe "00:12.345"
            }
        }
    }

    given("just under one minute (millis-rounding edge)") {
        // Guard against the visible 01:00.000-at-59.999 glitch that motivates
        // the floor-based millis derivation in [SpeedrunTimerFormat].
        `when`("format(59.9999f)") {
            val out = SpeedrunTimerFormat.format(59.9999f)
            then("the minutes column stays at 00 (no carry)") {
                out.substring(0, 2) shouldBe "00"
            }
            then("the seconds column stays at 59 (no carry)") {
                out.substring(3, 5) shouldBe "59"
            }
        }
    }

    given("exactly one minute") {
        `when`("format(60f)") {
            then("renders 01:00.000 (carry into minutes)") {
                SpeedrunTimerFormat.format(60f) shouldBe "01:00.000"
            }
        }
    }

    given("over one minute") {
        `when`("format(125.250f)") {
            then("renders 02:05.250") {
                SpeedrunTimerFormat.format(125.250f) shouldBe "02:05.250"
            }
        }
    }

    given("a negative input (defensive clamp)") {
        `when`("format(-3.5f)") {
            then("renders 00:00.000 — never a negative readout") {
                SpeedrunTimerFormat.format(-3.5f) shouldBe "00:00.000"
            }
        }
    }

    given("an overflow past 60 minutes (no minute clamp)") {
        `when`("format(3600.5f)") {
            then("renders 60:00.500 — minutes column carries cleanly") {
                SpeedrunTimerFormat.format(3600.5f) shouldBe "60:00.500"
            }
        }
    }
})

package com.sohai.platformer.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PreloadTimingLogTest : BehaviorSpec({

    given("a freshly-constructed PreloadTimingLog") {
        `when`("summary() is called with no entries recorded") {
            val log = PreloadTimingLog()

            then("the result is the empty string") {
                log.summary() shouldBe ""
            }
        }
    }

    given("a PreloadTimingLog with one recorded step") {
        `when`("record(\"fonts\", 42) then summary()") {
            val log = PreloadTimingLog()
            log.record("fonts", 42L)

            then("summary returns the expected single-line string") {
                log.summary() shouldBe "fonts -> 42 ms"
            }
        }
    }

    given("a PreloadTimingLog with multiple distinct steps") {
        `when`("steps are recorded in a specific order") {
            val log = PreloadTimingLog()
            log.record("fonts", 12L)
            log.record("music", 340L)
            log.record("sfx", 88L)

            then("summary preserves insertion order") {
                log.summary() shouldBe
                    """
                    fonts -> 12 ms
                    music -> 340 ms
                    sfx -> 88 ms
                    """.trimIndent()
            }
        }
    }

    given("a PreloadTimingLog with repeated step names") {
        `when`("the same step name is recorded twice with different durations") {
            val log = PreloadTimingLog()
            log.record("music", 100L)
            log.record("sfx", 50L)
            log.record("music", 200L)

            then("all entries are retained in insertion order") {
                log.summary() shouldBe
                    """
                    music -> 100 ms
                    sfx -> 50 ms
                    music -> 200 ms
                    """.trimIndent()
            }
        }
    }
})

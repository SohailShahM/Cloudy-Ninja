package com.sohai.platformer.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class GameRandomTest : BehaviorSpec({

    given("a seeded GameRandom") {
        `when`("setSeed(42) is called and nextFloat() is drawn twice") {
            GameRandom.setSeed(42L)
            val first = GameRandom.nextFloat()
            val second = GameRandom.nextFloat()

            then("repeating the same seed produces the same pair") {
                GameRandom.setSeed(42L)
                GameRandom.nextFloat() shouldBe first
                GameRandom.nextFloat() shouldBe second
            }
        }

        `when`("range(1f, 10f) is called 1000 times") {
            GameRandom.setSeed(7L)
            val values = List(1000) { GameRandom.range(1f, 10f) }

            then("every value is in [1, 10]") {
                values.forEach { v ->
                    (v >= 1f) shouldBe true
                    (v <= 10f) shouldBe true
                }
            }
        }
    }

    given("two independent RandomXS128 instances wrapped by separate GameRandom-equivalent seeds") {
        `when`("both are seeded with 99 and 100 floats are drawn") {
            // Prove seed-control by using two fresh RandomXS128 instances directly
            // (GameRandom is a singleton so we exercise it sequentially instead)
            GameRandom.setSeed(99L)
            val seqA = List(100) { GameRandom.nextFloat() }

            GameRandom.setSeed(99L)
            val seqB = List(100) { GameRandom.nextFloat() }

            then("both sequences are identical") {
                seqA shouldBe seqB
            }
        }

        `when`("seeded with different values") {
            GameRandom.setSeed(1L)
            val seqX = List(10) { GameRandom.nextFloat() }

            GameRandom.setSeed(2L)
            val seqY = List(10) { GameRandom.nextFloat() }

            then("sequences differ") {
                (seqX == seqY) shouldBe false
            }
        }
    }
})

package com.sohai.platformer.screens

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelCheckpoint
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.world.ObstacleManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * T-135: Coverage for the pure StatsScreen helpers that compute per-level
 * eco-token completion rows. Exercises the percentage math (incl. floor
 * rounding and divide-by-zero) and the empty-save default. Also asserts the
 * cross-level [computeHiddenTokenSummary] reads totals from the live
 * [LevelManager] registry — guarding against accidental hardcoded campaign
 * totals.
 */
class StatsLevelTokenProgressTest : BehaviorSpec({

    /** Minimal in-memory Level subclass for percentage-math cases. */
    class FakeLevel(
        override val id: String,
        override val name: String,
        private val regular: List<Vector2>,
        private val hidden: List<Vector2>
    ) : Level() {
        override val spawnX: Float = 0f
        override val spawnY: Float = 0f
        override fun setup(world: World, obstacleManager: ObstacleManager, movingPlatforms: MutableList<MovingPlatform>) {
            // no-op for tests
        }
        override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()
        override fun getEcoTokenPositions(): List<Vector2> = regular
        override fun getHiddenEcoTokenPositions(): List<Vector2> = hidden
    }

    fun fakeLevel(regularCount: Int, hiddenCount: Int): FakeLevel = FakeLevel(
        id = "fake",
        name = "Fake",
        regular = List(regularCount) { Vector2(it.toFloat(), 0f) },
        hidden = List(hiddenCount) { Vector2(it.toFloat(), 1f) }
    )

    given("computeLevelTokenProgress") {
        `when`("the save is empty (level not completed, hidden not found)") {
            then("returns 0/total/0%") {
                val level = fakeLevel(regularCount = 9, hiddenCount = 1)
                val result = StatsScreen.computeLevelTokenProgress(
                    level = level,
                    completed = false,
                    hiddenCollected = false
                )
                result.collected shouldBe 0
                result.total shouldBe 10
                result.percent shouldBe 0
            }
        }

        `when`("level completed but hidden not found") {
            then("counts regular tokens only and floors the percent") {
                // 9/10 -> 90%
                val level = fakeLevel(regularCount = 9, hiddenCount = 1)
                val result = StatsScreen.computeLevelTokenProgress(
                    level = level,
                    completed = true,
                    hiddenCollected = false
                )
                result.collected shouldBe 9
                result.total shouldBe 10
                result.percent shouldBe 90
            }
        }

        `when`("level completed and hidden found") {
            then("counts everything and yields 100%") {
                val level = fakeLevel(regularCount = 9, hiddenCount = 1)
                val result = StatsScreen.computeLevelTokenProgress(
                    level = level,
                    completed = true,
                    hiddenCollected = true
                )
                result.collected shouldBe 10
                result.total shouldBe 10
                result.percent shouldBe 100
            }
        }

        `when`("only hidden found (rare but possible mid-completion edge)") {
            then("counts hidden only — example spec '2/3 found' lower bound") {
                val level = fakeLevel(regularCount = 5, hiddenCount = 1)
                val result = StatsScreen.computeLevelTokenProgress(
                    level = level,
                    completed = false,
                    hiddenCollected = true
                )
                result.collected shouldBe 1
                result.total shouldBe 6
                // 1/6 -> 16% (floor)
                result.percent shouldBe 16
            }
        }

        `when`("the level has zero tokens") {
            then("returns 0/0/0% without divide-by-zero") {
                val level = fakeLevel(regularCount = 0, hiddenCount = 0)
                val result = StatsScreen.computeLevelTokenProgress(
                    level = level,
                    completed = true,
                    hiddenCollected = true
                )
                result.collected shouldBe 0
                result.total shouldBe 0
                result.percent shouldBe 0
            }
        }

        `when`("percentage requires floor rounding (not banker's)") {
            then("1/3 -> 33% (not 34%) and 2/3 -> 66% (not 67%)") {
                val level = fakeLevel(regularCount = 2, hiddenCount = 1)
                val oneOfThree = StatsScreen.computeLevelTokenProgress(
                    level = level, completed = false, hiddenCollected = true
                )
                oneOfThree.percent shouldBe 33

                val twoOfThree = StatsScreen.computeLevelTokenProgress(
                    level = level, completed = true, hiddenCollected = false
                )
                twoOfThree.percent shouldBe 66
            }
        }
    }

    given("computeHiddenTokenSummary") {
        // Pull live totals from the registry so the test fails loudly if a
        // future level adds another hidden token without anyone updating the
        // display contract.
        val campaignHiddenTotal = STATS_CAMPAIGN_LEVEL_IDS.sumOf { id ->
            LevelManager.getLevel(id)?.getHiddenEcoTokenPositions()?.size ?: 0
        }

        `when`("the save is empty (no hidden collected)") {
            then("returns 0 / registry-total") {
                val summary = StatsScreen.computeHiddenTokenSummary(emptySet())
                summary.collected shouldBe 0
                summary.total shouldBe campaignHiddenTotal
                // Sanity: T-107 ships 3 hidden tokens (one per campaign level).
                summary.total shouldBe 3
            }
        }

        `when`("two of three hidden tokens are collected") {
            then("returns 2/3 (matches the spec example 'Hidden: 2/3 found')") {
                val summary = StatsScreen.computeHiddenTokenSummary(setOf("level1", "level2"))
                summary.collected shouldBe 2
                summary.total shouldBe 3
            }
        }

        `when`("all hidden tokens are collected") {
            then("returns 3/3") {
                val summary = StatsScreen.computeHiddenTokenSummary(
                    setOf("level1", "level2", "level3")
                )
                summary.collected shouldBe 3
                summary.total shouldBe 3
            }
        }

        `when`("the save contains an unknown level id") {
            then("ignores it and still totals the campaign correctly") {
                val summary = StatsScreen.computeHiddenTokenSummary(
                    setOf("level1", "level_does_not_exist")
                )
                summary.collected shouldBe 1
                summary.total shouldBe 3
            }
        }
    }

    given("STATS_CAMPAIGN_LEVEL_IDS") {
        `when`("inspected") {
            then("matches the campaign levels with hidden tokens defined") {
                STATS_CAMPAIGN_LEVEL_IDS shouldBe listOf("level1", "level2", "level3")
                // Each campaign id must resolve through LevelManager — guard
                // against a level rename divorcing this list from the registry.
                for (id in STATS_CAMPAIGN_LEVEL_IDS) {
                    (LevelManager.getLevel(id) != null) shouldBe true
                }
            }
        }
    }
})

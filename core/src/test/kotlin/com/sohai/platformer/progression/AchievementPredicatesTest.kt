package com.sohai.platformer.progression

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Pure-function tests for [AchievementPredicates]. T-128 — these predicates
 * are the testable refactor of the inline `tryUnlock(...)` sites previously
 * embedded in `LevelRunState` / `LevelTransitionController` / `GameScreen`.
 *
 * Every predicate gets three cases:
 *  - **met** — the predicate fires for the right input.
 *  - **unmet** — the predicate stays silent when its trigger or threshold is missing.
 *  - **already-unlocked** — even when met, [AchievementPredicates.evaluate]
 *    excludes IDs already in [AchievementInputs.unlockedAchievements].
 *
 * Behavior must match the pre-refactor inline conditions byte-for-byte. If a
 * test here fails after touching the source, it's a behavior regression, not
 * a test bug.
 */
class AchievementPredicatesTest : BehaviorSpec({

    given("first_jump predicate") {
        `when`("jumpFiredThisFrame is true") {
            val inputs = AchievementInputs(jumpFiredThisFrame = true)
            then("predicate fires AND evaluate returns first_jump") {
                AchievementPredicates.firstJump(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "first_jump"
            }
        }
        `when`("jumpFiredThisFrame is false") {
            val inputs = AchievementInputs(jumpFiredThisFrame = false)
            then("predicate does not fire") {
                AchievementPredicates.firstJump(inputs) shouldBe false
                AchievementPredicates.evaluate(inputs) shouldNotContain "first_jump"
            }
        }
        `when`("first_jump is already unlocked") {
            val inputs = AchievementInputs(
                jumpFiredThisFrame = true,
                unlockedAchievements = setOf("first_jump"),
            )
            then("evaluate skips it even though the predicate would fire") {
                AchievementPredicates.firstJump(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldNotContain "first_jump"
            }
        }
    }

    given("first_cleanse predicate") {
        `when`("cleanseEventThisFrame is true") {
            val inputs = AchievementInputs(cleanseEventThisFrame = true)
            then("predicate fires") {
                AchievementPredicates.firstCleanse(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "first_cleanse"
            }
        }
        `when`("cleanseEventThisFrame is false") {
            then("predicate does not fire") {
                AchievementPredicates.firstCleanse(AchievementInputs()) shouldBe false
            }
        }
        `when`("first_cleanse is already unlocked") {
            val inputs = AchievementInputs(
                cleanseEventThisFrame = true,
                unlockedAchievements = setOf("first_cleanse"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "first_cleanse"
            }
        }
    }

    given("first_enemy predicate") {
        `when`("enemyDefeatedThisFrame is true") {
            val inputs = AchievementInputs(enemyDefeatedThisFrame = true)
            then("predicate fires") {
                AchievementPredicates.firstEnemy(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "first_enemy"
            }
        }
        `when`("enemyDefeatedThisFrame is false") {
            then("predicate does not fire") {
                AchievementPredicates.firstEnemy(AchievementInputs()) shouldBe false
            }
        }
        `when`("first_enemy is already unlocked") {
            val inputs = AchievementInputs(
                enemyDefeatedThisFrame = true,
                unlockedAchievements = setOf("first_enemy"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "first_enemy"
            }
        }
    }

    given("stomp_10 predicate") {
        `when`("enemy defeated AND totalStomps >= 10") {
            val inputs = AchievementInputs(enemyDefeatedThisFrame = true, totalStomps = 10)
            then("predicate fires") {
                AchievementPredicates.stomp10(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "stomp_10"
            }
        }
        `when`("enemy defeated but totalStomps < 10") {
            val inputs = AchievementInputs(enemyDefeatedThisFrame = true, totalStomps = 9)
            then("predicate does not fire") {
                AchievementPredicates.stomp10(inputs) shouldBe false
                AchievementPredicates.evaluate(inputs) shouldNotContain "stomp_10"
            }
        }
        `when`("totalStomps >= 10 but no enemy defeated this frame") {
            // Old behavior: stomp_10 only fired immediately after a stomp event.
            // The pure predicate preserves that gate so post-load enemy
            // counts can't double-fire mid-session.
            val inputs = AchievementInputs(enemyDefeatedThisFrame = false, totalStomps = 25)
            then("predicate does not fire") {
                AchievementPredicates.stomp10(inputs) shouldBe false
            }
        }
        `when`("threshold met but stomp_10 already unlocked") {
            val inputs = AchievementInputs(
                enemyDefeatedThisFrame = true,
                totalStomps = 100,
                unlockedAchievements = setOf("stomp_10"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "stomp_10"
            }
        }
        `when`("totalStomps == 11 (above threshold)") {
            val inputs = AchievementInputs(enemyDefeatedThisFrame = true, totalStomps = 11)
            then("predicate still fires") {
                AchievementPredicates.stomp10(inputs) shouldBe true
            }
        }
    }

    given("eco_sweep predicate") {
        `when`("ecoSweepReachedThisFrame is true") {
            val inputs = AchievementInputs(ecoSweepReachedThisFrame = true)
            then("predicate fires") {
                AchievementPredicates.ecoSweep(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "eco_sweep"
            }
        }
        `when`("ecoSweepReachedThisFrame is false") {
            then("predicate does not fire") {
                AchievementPredicates.ecoSweep(AchievementInputs()) shouldBe false
            }
        }
        `when`("eco_sweep already unlocked") {
            val inputs = AchievementInputs(
                ecoSweepReachedThisFrame = true,
                unlockedAchievements = setOf("eco_sweep"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "eco_sweep"
            }
        }
    }

    given("no_death_run predicate") {
        `when`("noDeathExitThisFrame is true") {
            val inputs = AchievementInputs(noDeathExitThisFrame = true)
            then("predicate fires") {
                AchievementPredicates.noDeathRun(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "no_death_run"
            }
        }
        `when`("noDeathExitThisFrame is false") {
            then("predicate does not fire") {
                AchievementPredicates.noDeathRun(AchievementInputs()) shouldBe false
            }
        }
        `when`("no_death_run already unlocked") {
            val inputs = AchievementInputs(
                noDeathExitThisFrame = true,
                unlockedAchievements = setOf("no_death_run"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "no_death_run"
            }
        }
    }

    given("atlas_half predicate") {
        `when`("atlasSize >= 6") {
            val inputs = AchievementInputs(atlasSize = 6)
            then("predicate fires") {
                AchievementPredicates.atlasHalf(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "atlas_half"
            }
        }
        `when`("atlasSize == 5 (just under threshold)") {
            val inputs = AchievementInputs(atlasSize = 5)
            then("predicate does not fire") {
                AchievementPredicates.atlasHalf(inputs) shouldBe false
                AchievementPredicates.evaluate(inputs) shouldNotContain "atlas_half"
            }
        }
        `when`("atlasSize == 0") {
            then("predicate does not fire") {
                AchievementPredicates.atlasHalf(AchievementInputs()) shouldBe false
            }
        }
        `when`("atlas_half already unlocked at size 12") {
            val inputs = AchievementInputs(
                atlasSize = 12,
                unlockedAchievements = setOf("atlas_half"),
            )
            then("evaluate skips atlas_half (but atlas_full still fires)") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "atlas_half"
                AchievementPredicates.evaluate(inputs) shouldContain "atlas_full"
            }
        }
    }

    given("atlas_full predicate") {
        `when`("atlasSize >= 12") {
            val inputs = AchievementInputs(atlasSize = 12)
            then("predicate fires") {
                AchievementPredicates.atlasFull(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "atlas_full"
            }
        }
        `when`("atlasSize == 11 (just under threshold)") {
            val inputs = AchievementInputs(atlasSize = 11)
            then("predicate does not fire") {
                AchievementPredicates.atlasFull(inputs) shouldBe false
            }
        }
        `when`("atlas_full already unlocked") {
            val inputs = AchievementInputs(
                atlasSize = 50,
                unlockedAchievements = setOf("atlas_full"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "atlas_full"
            }
        }
    }

    given("collector predicate") {
        `when`("collectedHiddenTokens.size >= 3") {
            val inputs = AchievementInputs(
                collectedHiddenTokens = setOf("level1", "level2", "level3"),
            )
            then("predicate fires") {
                AchievementPredicates.collector(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "collector"
            }
        }
        `when`("collectedHiddenTokens.size == 2") {
            val inputs = AchievementInputs(
                collectedHiddenTokens = setOf("level1", "level2"),
            )
            then("predicate does not fire") {
                AchievementPredicates.collector(inputs) shouldBe false
                AchievementPredicates.evaluate(inputs) shouldNotContain "collector"
            }
        }
        `when`("empty hidden-token set") {
            then("predicate does not fire") {
                AchievementPredicates.collector(AchievementInputs()) shouldBe false
            }
        }
        `when`("collector already unlocked") {
            val inputs = AchievementInputs(
                collectedHiddenTokens = setOf("level1", "level2", "level3"),
                unlockedAchievements = setOf("collector"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "collector"
            }
        }
    }

    given("speed_demon predicate") {
        `when`("time trial completed under 120s") {
            val inputs = AchievementInputs(
                timeTrialCompletedThisFrame = true,
                levelTimer = 119.9f,
            )
            then("predicate fires") {
                AchievementPredicates.speedDemon(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "speed_demon"
            }
        }
        `when`("time trial completed at exactly 120s") {
            // Strictly less-than per pre-refactor inline check `levelTimer < 120f`.
            val inputs = AchievementInputs(
                timeTrialCompletedThisFrame = true,
                levelTimer = 120f,
            )
            then("predicate does not fire") {
                AchievementPredicates.speedDemon(inputs) shouldBe false
                AchievementPredicates.evaluate(inputs) shouldNotContain "speed_demon"
            }
        }
        `when`("not a time trial run but levelTimer < 120s") {
            val inputs = AchievementInputs(
                timeTrialCompletedThisFrame = false,
                levelTimer = 30f,
            )
            then("predicate does not fire") {
                AchievementPredicates.speedDemon(inputs) shouldBe false
            }
        }
        `when`("speed_demon already unlocked") {
            val inputs = AchievementInputs(
                timeTrialCompletedThisFrame = true,
                levelTimer = 30f,
                unlockedAchievements = setOf("speed_demon"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "speed_demon"
            }
        }
    }

    given("world_1_clear predicate") {
        `when`("level completion and levelId == level1") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                levelId = "level1",
            )
            then("predicate fires") {
                AchievementPredicates.world1Clear(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "world_1_clear"
            }
        }
        `when`("level completion but levelId != level1") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                levelId = "level2",
            )
            then("predicate does not fire") {
                AchievementPredicates.world1Clear(inputs) shouldBe false
                AchievementPredicates.evaluate(inputs) shouldNotContain "world_1_clear"
            }
        }
        `when`("levelId == level1 but no level completion") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = false,
                levelId = "level1",
            )
            then("predicate does not fire") {
                AchievementPredicates.world1Clear(inputs) shouldBe false
            }
        }
        `when`("world_1_clear already unlocked") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                levelId = "level1",
                unlockedAchievements = setOf("world_1_clear"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "world_1_clear"
            }
        }
    }

    given("all_clear predicate") {
        `when`("level completion and all campaign levels completed") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                completedLevels = setOf("level1", "level2", "level3"),
            )
            then("predicate fires") {
                AchievementPredicates.allClear(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "all_clear"
            }
        }
        `when`("level completion but only 2 of 3 campaign levels completed") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                completedLevels = setOf("level1", "level2"),
            )
            then("predicate does not fire") {
                AchievementPredicates.allClear(inputs) shouldBe false
            }
        }
        `when`("all campaign levels completed but no completion event") {
            // Old code only ran the check after a completion event. The pure
            // predicate keeps that gate so we can't fire all_clear mid-stats.
            val inputs = AchievementInputs(
                levelCompletedThisFrame = false,
                completedLevels = setOf("level1", "level2", "level3"),
            )
            then("predicate does not fire") {
                AchievementPredicates.allClear(inputs) shouldBe false
            }
        }
        `when`("completed has extra non-campaign levels but missing level3") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                completedLevels = setOf("level1", "level2", "level0_0", "tutorial"),
            )
            then("predicate does not fire") {
                AchievementPredicates.allClear(inputs) shouldBe false
            }
        }
        `when`("all_clear already unlocked") {
            val inputs = AchievementInputs(
                levelCompletedThisFrame = true,
                completedLevels = setOf("level1", "level2", "level3"),
                unlockedAchievements = setOf("all_clear"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "all_clear"
            }
        }
    }

    given("boss_defeated predicate") {
        `when`("bossDefeatedThisFrame is true") {
            val inputs = AchievementInputs(bossDefeatedThisFrame = true)
            then("predicate fires") {
                AchievementPredicates.bossDefeated(inputs) shouldBe true
                AchievementPredicates.evaluate(inputs) shouldContain "boss_defeated"
            }
        }
        `when`("bossDefeatedThisFrame is false") {
            then("predicate does not fire") {
                AchievementPredicates.bossDefeated(AchievementInputs()) shouldBe false
            }
        }
        `when`("boss_defeated already unlocked") {
            val inputs = AchievementInputs(
                bossDefeatedThisFrame = true,
                unlockedAchievements = setOf("boss_defeated"),
            )
            then("evaluate skips it") {
                AchievementPredicates.evaluate(inputs) shouldNotContain "boss_defeated"
            }
        }
    }

    given("evaluate orchestrator") {

        `when`("no trigger flags are set") {
            val inputs = AchievementInputs()
            then("evaluate returns an empty list") {
                AchievementPredicates.evaluate(inputs).shouldBeEmpty()
            }
        }

        `when`("threshold-only fields are set but no trigger flags") {
            // atlas_half/atlas_full/collector are the only achievements that
            // fire on threshold without an explicit per-frame trigger flag.
            // Once you have 12 snapshots saved, atlas_full fires whenever
            // evaluate runs. This mirrors the pre-refactor behavior: those
            // unlocks fire inside the same call that updates the save.
            val inputs = AchievementInputs(
                atlasSize = 12,
                collectedHiddenTokens = setOf("level1", "level2", "level3"),
            )
            val fired = AchievementPredicates.evaluate(inputs)
            then("threshold-gated achievements fire") {
                fired.shouldContainAll(listOf("atlas_half", "atlas_full", "collector"))
            }
            then("trigger-gated achievements stay silent") {
                fired shouldNotContain "first_jump"
                fired shouldNotContain "first_enemy"
                fired shouldNotContain "stomp_10"
                fired shouldNotContain "boss_defeated"
            }
        }

        `when`("multiple predicates fire in one evaluation") {
            // Stomping the 10th enemy on the same frame as the first cleanse
            // should fire stomp_10 + first_enemy + first_cleanse together —
            // matches the pre-refactor multi-tryUnlock pattern at the call site.
            val inputs = AchievementInputs(
                enemyDefeatedThisFrame = true,
                cleanseEventThisFrame = true,
                totalStomps = 10,
            )
            val fired = AchievementPredicates.evaluate(inputs)
            then("all three fire") {
                fired shouldContainAll listOf("first_enemy", "stomp_10", "first_cleanse")
            }
        }

        `when`("a fully-unlocked save state hits every threshold and trigger") {
            // Sanity: evaluate is idempotent. Already-unlocked achievements
            // never re-fire even when their predicates would otherwise return true.
            val inputs = AchievementInputs(
                jumpFiredThisFrame = true,
                cleanseEventThisFrame = true,
                enemyDefeatedThisFrame = true,
                ecoSweepReachedThisFrame = true,
                noDeathExitThisFrame = true,
                bossDefeatedThisFrame = true,
                timeTrialCompletedThisFrame = true,
                levelCompletedThisFrame = true,
                levelId = "level1",
                levelTimer = 30f,
                totalStomps = 50,
                atlasSize = 12,
                collectedHiddenTokens = setOf("level1", "level2", "level3"),
                completedLevels = setOf("level1", "level2", "level3"),
                unlockedAchievements = setOf(
                    "first_jump", "first_cleanse", "eco_sweep", "no_death_run",
                    "speed_demon", "atlas_half", "atlas_full", "first_enemy",
                    "stomp_10", "boss_defeated", "world_1_clear", "all_clear",
                    "collector",
                ),
            )
            then("evaluate returns nothing") {
                AchievementPredicates.evaluate(inputs).shouldBeEmpty()
            }
        }

        `when`("every predicate id is present in AchievementRegistry") {
            // Drift guard: any new achievement added to PREDICATES must also
            // exist in AchievementRegistry so the toast + persistence layer
            // can look it up. This catches typo'd ids early.
            val registryIds = AchievementRegistry.ALL.map { it.id }.toSet()
            then("every predicate id resolves to a registry entry") {
                AchievementPredicates.PREDICATES.keys.forEach { id ->
                    (id in registryIds) shouldBe true
                }
            }
            then("every registry id has a matching predicate (full coverage)") {
                registryIds.forEach { id ->
                    (id in AchievementPredicates.PREDICATES.keys) shouldBe true
                }
            }
        }
    }
})

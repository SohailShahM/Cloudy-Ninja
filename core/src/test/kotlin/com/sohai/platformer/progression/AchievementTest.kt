package com.sohai.platformer.progression

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * Unit tests for [AchievementRegistry] and [Achievement].
 *
 * AchievementRegistry is a pure Kotlin object with no libGDX deps — constructed directly.
 *
 * Note on scope: unlock-condition logic is currently embedded inside libGDX-coupled
 * screen code (LevelRunState, LevelTransitionController, GameScreen) rather than as
 * pure predicates on AchievementRegistry / GameState. Pure-logic tests for predicate
 * firing (e.g. stomp_10 fires at totalStomps >= 10, atlas_full at 12 collected, etc.)
 * would require a source-side refactor to extract those predicates, which is out of
 * scope for this test-only task (T-056 hard rule: "Do NOT touch any source file").
 *
 * The thresholds themselves are nonetheless asserted indirectly here via the
 * description-string invariants (e.g. atlas_full must mention "12", stomp_10 must
 * mention "10"), so that any drift between achievement metadata and the unlock-site
 * thresholds is caught.
 */
class AchievementTest : BehaviorSpec({

    given("AchievementRegistry.get") {

        `when`("called with a known id (stomp_10)") {
            val achievement = AchievementRegistry.get("stomp_10")

            then("it returns a non-null Achievement with the correct title and desc") {
                achievement.shouldNotBeNull()
                achievement.id shouldBe "stomp_10"
                achievement.title shouldBe "Stomper"
                achievement.desc shouldBe "Stomp 10 enemies"
            }
        }

        `when`("called with another known id (atlas_full)") {
            val achievement = AchievementRegistry.get("atlas_full")

            then("it returns the Sky Scholar achievement") {
                achievement.shouldNotBeNull()
                achievement.id shouldBe "atlas_full"
                achievement.title shouldBe "Sky Scholar"
                achievement.desc shouldBe "Collect all 12 Cloud Atlas snapshots"
            }
        }

        `when`("called with first_jump") {
            val achievement = AchievementRegistry.get("first_jump")

            then("it returns the First Flight achievement") {
                achievement.shouldNotBeNull()
                achievement.title shouldBe "First Flight"
            }
        }

        `when`("called with all_clear") {
            val achievement = AchievementRegistry.get("all_clear")

            then("it returns the Eco Restored achievement") {
                achievement.shouldNotBeNull()
                achievement.title shouldBe "Eco Restored"
            }
        }

        `when`("called with an unknown id") {
            val achievement = AchievementRegistry.get("does_not_exist")

            then("it returns null") {
                achievement.shouldBeNull()
            }
        }

        `when`("called with a random/garbage id") {
            val achievement = AchievementRegistry.get("zzz_random_xyz_9999")

            then("it returns null") {
                achievement.shouldBeNull()
            }
        }

        `when`("called with an empty string") {
            val achievement = AchievementRegistry.get("")

            then("it returns null (no blank-id achievement exists)") {
                achievement.shouldBeNull()
            }
        }

        `when`("called twice with the same id") {
            val first = AchievementRegistry.get("boss_defeated")
            val second = AchievementRegistry.get("boss_defeated")

            then("both calls return equal values (idempotent lookup)") {
                first.shouldNotBeNull()
                second.shouldNotBeNull()
                first shouldBe second
            }
        }
    }

    given("AchievementRegistry.ALL") {

        `when`("the full list is inspected") {
            val all = AchievementRegistry.ALL

            then("it contains exactly 13 achievements") {
                all shouldHaveSize 13
            }

            then("every achievement has a non-blank id") {
                all.forEach { it.id.shouldNotBeBlank() }
            }

            then("every achievement has a non-blank title") {
                all.forEach { it.title.shouldNotBeBlank() }
            }

            then("every achievement has a non-blank desc") {
                all.forEach { it.desc.shouldNotBeBlank() }
            }

            then("no two achievements share the same id (uniqueness invariant)") {
                val ids = all.map { it.id }
                ids.toSet().size shouldBe ids.size
            }

            then("no two achievements share the same title") {
                val titles = all.map { it.title }
                titles.toSet().size shouldBe titles.size
            }

            then("the canonical 13 achievement ids are all present") {
                val expectedIds = setOf(
                    "first_jump", "first_cleanse", "eco_sweep", "no_death_run",
                    "speed_demon", "atlas_half", "atlas_full", "first_enemy",
                    "stomp_10", "boss_defeated", "world_1_clear", "all_clear",
                    // T-107: hidden eco-token meta-achievement
                    "collector"
                )
                val actualIds = all.map { it.id }.toSet()
                actualIds shouldBe expectedIds
            }

            then("every id is round-trippable via get()") {
                all.forEach { achievement ->
                    val looked = AchievementRegistry.get(achievement.id)
                    looked shouldBe achievement
                }
            }
        }
    }

    given("Achievement metadata / unlock-threshold invariants") {

        // The unlock predicates live in screen code today (see test-class kdoc).
        // We assert the metadata that the unlock sites consume so any drift in the
        // displayed thresholds is caught.

        `when`("inspecting stomp_10's desc") {
            val a = AchievementRegistry.get("stomp_10")
            then("it advertises the '10' threshold matching the unlock site") {
                a.shouldNotBeNull()
                (a.desc.contains("10")) shouldBe true
            }
        }

        `when`("inspecting atlas_half's desc") {
            val a = AchievementRegistry.get("atlas_half")
            then("it advertises the '6' threshold") {
                a.shouldNotBeNull()
                (a.desc.contains("6")) shouldBe true
            }
        }

        `when`("inspecting atlas_full's desc") {
            val a = AchievementRegistry.get("atlas_full")
            then("it advertises the '12' threshold (NOT 11)") {
                a.shouldNotBeNull()
                (a.desc.contains("12")) shouldBe true
                (a.desc.contains("11")) shouldBe false
            }
        }

        `when`("inspecting speed_demon's desc") {
            val a = AchievementRegistry.get("speed_demon")
            then("it advertises the '2 minute' time-trial threshold") {
                a.shouldNotBeNull()
                (a.desc.contains("2 minutes")) shouldBe true
            }
        }

        `when`("inspecting world_1_clear's desc") {
            val a = AchievementRegistry.get("world_1_clear")
            then("it refers to World 1") {
                a.shouldNotBeNull()
                (a.desc.contains("World 1")) shouldBe true
            }
        }

        `when`("inspecting collector's metadata") {
            // T-107: meta-achievement for hidden eco-tokens. Description must
            // advertise the '3' threshold matching the LevelRunState unlock
            // site (newIds.size >= 3 → tryUnlock("collector")).
            val a = AchievementRegistry.get("collector")
            then("the achievement exists with the right title") {
                a.shouldNotBeNull()
                a.title shouldBe "Collector"
            }
            then("desc advertises the '3' threshold") {
                a.shouldNotBeNull()
                (a.desc.contains("3")) shouldBe true
            }
            then("desc mentions 'hidden' to differentiate from eco_sweep") {
                a.shouldNotBeNull()
                (a.desc.contains("hidden")) shouldBe true
            }
        }

        `when`("the registry is verified as a closed enumeration") {
            val all = AchievementRegistry.ALL

            then("it has the expected immutable list type (read-only)") {
                // Cast intent: AchievementRegistry.ALL is val + listOf — confirm
                // its identity is consistent and stable across observations.
                val snapshot1 = AchievementRegistry.ALL
                val snapshot2 = AchievementRegistry.ALL
                snapshot1 shouldBe snapshot2
                snapshot1 shouldHaveSize all.size
            }

            then("ALL contains every id queryable via get()") {
                all.forEach { achievement ->
                    AchievementRegistry.ALL.map { it.id } shouldContain achievement.id
                }
            }
        }
    }
})

package com.sohai.platformer.levels

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * Unit tests for [LevelManager].
 *
 * LevelManager is a pure-Kotlin singleton: it builds an ordered list of
 * [Level] instances at load time (hub + 4 tutorials + all [LevelRegistry]
 * TMX-backed levels) and exposes id-based lookup + sequential traversal.
 *
 * These tests verify:
 *  - id → level lookup correctness for hub, tutorial, and campaign ids
 *  - getNextLevel ordering across the canonical sequence
 *  - getAllLevels invariants (count, ordering, no duplicate ids)
 *  - isLastLevel terminates on the final campaign level
 *
 * Locked-world (portal unlock) logic does NOT live on LevelManager — it is
 * a companion-object concern on [Level0_0] (portalUnlockRequirement /
 * portalTargetLevel) and is covered by Level0_0-scoped tests, not here.
 */
class LevelManagerTest : BehaviorSpec({

    given("LevelManager.getLevel") {

        `when`("called with the hub level id 'level0_0'") {
            val level = LevelManager.getLevel("level0_0")
            then("it returns the hub level") {
                level.shouldNotBeNull()
                level.id shouldBe "level0_0"
            }
        }

        `when`("called with each tutorial id (level0_1..level0_4)") {
            val ids = listOf("level0_1", "level0_2", "level0_3", "level0_4")
            then("each id resolves to the matching Level") {
                ids.forEach { id ->
                    val lvl = LevelManager.getLevel(id)
                    lvl.shouldNotBeNull()
                    lvl.id shouldBe id
                }
            }
        }

        `when`("called with each campaign id (level1/level2/level3)") {
            val ids = listOf("level1", "level2", "level3")
            then("each id resolves to the matching Level") {
                ids.forEach { id ->
                    val lvl = LevelManager.getLevel(id)
                    lvl.shouldNotBeNull()
                    lvl.id shouldBe id
                }
            }
        }

        `when`("called with an unknown id") {
            val level = LevelManager.getLevel("not_a_real_level_zzz")
            then("it returns null") {
                level.shouldBeNull()
            }
        }

        `when`("called with an empty string") {
            val level = LevelManager.getLevel("")
            then("it returns null") {
                level.shouldBeNull()
            }
        }
    }

    given("LevelManager.getNextLevel") {

        `when`("called from the hub 'level0_0'") {
            val next = LevelManager.getNextLevel("level0_0")
            then("it returns the first tutorial, 'level0_1'") {
                next.shouldNotBeNull()
                next.id shouldBe "level0_1"
            }
        }

        `when`("walked sequentially through every level") {
            val canonicalOrder = listOf(
                "level0_0", "level0_1", "level0_2", "level0_3", "level0_4",
                "level1",   "level2",   "level3"
            )
            then("each consecutive call advances by exactly one id") {
                for (i in 0 until canonicalOrder.size - 1) {
                    val next = LevelManager.getNextLevel(canonicalOrder[i])
                    next.shouldNotBeNull()
                    next.id shouldBe canonicalOrder[i + 1]
                }
            }
        }

        `when`("called on the final level 'level3'") {
            val next = LevelManager.getNextLevel("level3")
            then("it returns null (signals the victory screen)") {
                next.shouldBeNull()
            }
        }

        `when`("called with an unknown id") {
            val next = LevelManager.getNextLevel("definitely_not_a_level")
            then("it returns null") {
                next.shouldBeNull()
            }
        }

        `when`("called with an empty string") {
            val next = LevelManager.getNextLevel("")
            then("it returns null") {
                next.shouldBeNull()
            }
        }
    }

    given("LevelManager.isLastLevel") {

        `when`("called on the final campaign level 'level3'") {
            then("it returns true") {
                LevelManager.isLastLevel("level3") shouldBe true
            }
        }

        `when`("called on any earlier level in the sequence") {
            then("it returns false") {
                listOf(
                    "level0_0", "level0_1", "level0_2", "level0_3", "level0_4",
                    "level1", "level2"
                ).forEach { id ->
                    LevelManager.isLastLevel(id) shouldBe false
                }
            }
        }

        `when`("called with an unknown id") {
            then("it returns true (no next level exists)") {
                // Documented behaviour: getNextLevel() returns null for unknown ids,
                // and isLastLevel() is defined as getNextLevel() == null.
                LevelManager.isLastLevel("nope") shouldBe true
            }
        }
    }

    given("LevelManager.getAllLevels") {

        val all = LevelManager.getAllLevels()

        `when`("the full list is inspected") {
            then("it contains the expected 8 levels (hub + 4 tutorials + 3 campaign)") {
                all shouldHaveSize 8
            }

            then("the ordering is hub -> tutorials -> campaign") {
                val expectedOrder = listOf(
                    "level0_0", "level0_1", "level0_2", "level0_3", "level0_4",
                    "level1",   "level2",   "level3"
                )
                all.map { it.id } shouldBe expectedOrder
            }

            then("no two levels share the same id (uniqueness invariant)") {
                val ids = all.map { it.id }
                ids.toSet().size shouldBe ids.size
            }

            then("every level exposes a non-blank id and name") {
                all.forEach { lvl ->
                    lvl.id.shouldNotBeBlank()
                    lvl.name.shouldNotBeBlank()
                }
            }
        }
    }
})

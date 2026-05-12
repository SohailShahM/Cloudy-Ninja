package com.sohai.platformer.levels

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for [Level0_0]'s companion-object portal logic.
 *
 * Locked-world / portal-unlock logic lives on the companion, not on
 * [LevelManager] — [Level0_0.Companion.portalUnlockRequirement] returns
 * the set of `completedLevels` entries gating each portal, and
 * [Level0_0.Companion.portalTargetLevel] maps each portal id to the
 * levelId the player should navigate to.
 *
 * These tests are pure logic (no libGDX runtime). They lock down:
 *  - portal id → unlock-requirement set for each of the four portals
 *  - the "always unlocked" semantics of an empty requirement set
 *  - the captured-actual fall-closed default for unknown portal ids
 *  - portal id → target level mapping
 *  - idempotence (repeated calls return equal results)
 *
 * Hub-room construction / Box2D setup is covered elsewhere (smoke CI) —
 * we only exercise pure companion functions here.
 */
class Level0_0Test : BehaviorSpec({

    given("Level0_0.portalUnlockRequirement") {

        `when`("called with 'portal_world0'") {
            val req = Level0_0.portalUnlockRequirement("portal_world0")
            then("it returns an empty set (always unlocked)") {
                req shouldBe emptySet()
            }
        }

        `when`("called with 'portal_world1'") {
            val req = Level0_0.portalUnlockRequirement("portal_world1")
            then("it requires the tutorial finale 'level0_4'") {
                req shouldBe setOf("level0_4")
            }
        }

        `when`("called with 'portal_world2'") {
            val req = Level0_0.portalUnlockRequirement("portal_world2")
            then("it requires World 1 'level1' to be completed") {
                req shouldBe setOf("level1")
            }
        }

        `when`("called with 'portal_world3'") {
            val req = Level0_0.portalUnlockRequirement("portal_world3")
            then("it requires World 2 'level2' to be completed") {
                req shouldBe setOf("level2")
            }
        }

        `when`("called with an unknown portal id") {
            val req = Level0_0.portalUnlockRequirement("portal_does_not_exist")
            then("it returns the unsatisfiable sentinel set (fail-closed default)") {
                // Actual behaviour: setOf("__impossible__"). The point of this default
                // is that GameState.completedLevels can never contain that token, so
                // unknown portals stay locked rather than accidentally opening.
                req shouldBe setOf("__impossible__")
                req.shouldHaveSize(1)
            }
        }

        `when`("called with an empty string") {
            val req = Level0_0.portalUnlockRequirement("")
            then("it falls through to the unknown-id default") {
                req shouldBe setOf("__impossible__")
            }
        }

        `when`("called twice with the same portal id (every defined portal + a bogus id)") {
            then("both calls return equal sets (idempotent / pure)") {
                listOf(
                    "portal_world0", "portal_world1", "portal_world2", "portal_world3",
                    "bogus"
                ).forEach { id ->
                    Level0_0.portalUnlockRequirement(id) shouldBe
                        Level0_0.portalUnlockRequirement(id)
                }
            }
        }
    }

    given("Level0_0.portalTargetLevel") {

        `when`("called with 'portal_world0'") {
            then("it returns the first tutorial level 'level0_1'") {
                Level0_0.portalTargetLevel("portal_world0") shouldBe "level0_1"
            }
        }

        `when`("called with 'portal_world1'") {
            then("it returns the World 1 entry 'level1'") {
                Level0_0.portalTargetLevel("portal_world1") shouldBe "level1"
            }
        }

        `when`("called with 'portal_world2'") {
            then("it returns the World 2 entry 'level2'") {
                Level0_0.portalTargetLevel("portal_world2") shouldBe "level2"
            }
        }

        `when`("called with 'portal_world3'") {
            then("it returns the World 3 entry 'level3'") {
                Level0_0.portalTargetLevel("portal_world3") shouldBe "level3"
            }
        }

        `when`("called with an unknown portal id") {
            val target = Level0_0.portalTargetLevel("portal_does_not_exist")
            then("it returns null (no navigation target)") {
                target.shouldBeNull()
            }
        }

        `when`("called with an empty string") {
            then("it returns null") {
                Level0_0.portalTargetLevel("").shouldBeNull()
            }
        }

        `when`("called twice with the same portal id (every defined portal + a bogus id)") {
            then("both calls return equal values (idempotent / pure)") {
                listOf(
                    "portal_world0", "portal_world1", "portal_world2", "portal_world3",
                    "bogus"
                ).forEach { id ->
                    Level0_0.portalTargetLevel(id) shouldBe Level0_0.portalTargetLevel(id)
                }
            }
        }
    }

    given("Level0_0.PORTALS list") {

        val portals = Level0_0.PORTALS

        `when`("inspected") {
            then("it exposes the four canonical portals in declared order") {
                portals.map { it.userData } shouldBe listOf(
                    "portal_world0", "portal_world1", "portal_world2", "portal_world3"
                )
            }

            then("each portal userData resolves to a concrete target level") {
                portals.forEach { p ->
                    Level0_0.portalTargetLevel(p.userData).shouldNotBeNull()
                }
            }

            then("each portal userData has a defined unlock requirement (never the fail-closed sentinel)") {
                portals.forEach { p ->
                    Level0_0.portalUnlockRequirement(p.userData) shouldNotBe
                        setOf("__impossible__")
                }
            }
        }
    }
})

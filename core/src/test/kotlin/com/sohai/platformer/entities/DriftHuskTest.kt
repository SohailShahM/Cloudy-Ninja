package com.sohai.platformer.entities

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * Pure-logic tests for [DriftHusk] (T-062).
 *
 * Follows the same pattern as [SmogSpriteTest]:
 *   1. Mock the Box2D `Body` so no native lib is needed.
 *   2. Reach the private `DriftHusk(body, originX, originY, triggerX)`
 *      constructor via reflection, bypassing `create()` (which calls
 *      `world.createBody`, `PolygonShape()`, etc.).
 *
 * Only the FLOATING → DROPPING → COOLDOWN → FLOATING state machine and the
 * inherited damage/death logic are exercised; rendering is excluded (needs
 * a live `ShapeRenderer` and OpenGL context).
 */
class DriftHuskTest : BehaviorSpec({

    val eps = 0.0001f

    /** Build a DriftHusk bypassing the private ctor via reflection. */
    fun newHusk(
        body: Body,
        originX: Float = 5f,
        originY: Float = 6f,
        triggerX: Float = 5f,
    ): DriftHusk {
        val ctor = DriftHusk::class.java.getDeclaredConstructor(
            Body::class.java,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
        )
        ctor.isAccessible = true
        return ctor.newInstance(body, originX, originY, triggerX)
    }

    /**
     * Build a mocked Body whose `position` reflects a mutable Vector2 (so
     * tests can pretend the physics step moved the husk) and whose
     * `linearVelocity` setter writes through to a captured slot.
     */
    fun mockBodyAt(x: Float, y: Float = 0f): Pair<Body, Vector2> {
        val pos = Vector2(x, y)
        val body = mockk<Body>(relaxed = true)
        every { body.position } returns pos
        return body to pos
    }

    // ── construction ─────────────────────────────────────────────────────────

    given("a freshly constructed DriftHusk") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)

        `when`("inspecting initial state") {
            then("starts in FLOATING state") { husk.state shouldBe DriftHusk.State.FLOATING }
            then("starts at full HP = 2") { husk.hp shouldBe 2 }
            then("is not dead") { husk.isDead shouldBe false }
            then("wasStomped defaults to false") { husk.wasStomped shouldBe false }
            then("hitTerrain defaults to false") { husk.hitTerrain shouldBe false }
        }
    }

    // ── FLOATING -- no movement until trigger ────────────────────────────────

    given("a husk in FLOATING with no player x set yet") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)

        `when`("update() is called once") {
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            husk.update(1f / 60f)

            then("velocity is zeroed (no drift)") {
                capV.captured.x shouldBe (0f plusOrMinus eps)
                capV.captured.y shouldBe (0f plusOrMinus eps)
            }
            then("state remains FLOATING") { husk.state shouldBe DriftHusk.State.FLOATING }
        }
    }

    given("a husk in FLOATING with a player far outside the trigger band") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)

        `when`("the player is far away and update() is called") {
            husk.setPlayerX(0f) // way outside the 0.6m trigger band around triggerX=5
            husk.update(1f / 60f)

            then("state stays FLOATING") { husk.state shouldBe DriftHusk.State.FLOATING }
        }
    }

    // ── trigger ──────────────────────────────────────────────────────────────

    given("a husk in FLOATING with the player inside the trigger band") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)

        `when`("the player crosses triggerX exactly") {
            husk.setPlayerX(5f)
            husk.update(1f / 60f)

            then("state transitions to DROPPING") {
                husk.state shouldBe DriftHusk.State.DROPPING
            }
        }
    }

    given("a husk already in DROPPING state") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)
        // Transition to DROPPING
        husk.setPlayerX(5f)
        husk.update(1f / 60f)
        husk.state shouldBe DriftHusk.State.DROPPING

        `when`("the player remains inside the band on the next update") {
            husk.setPlayerX(5f)
            husk.update(1f / 60f)

            then("state stays DROPPING (no re-trigger)") {
                husk.state shouldBe DriftHusk.State.DROPPING
            }
        }
    }

    // ── gravity accumulation ─────────────────────────────────────────────────

    given("a husk in DROPPING") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)
        // Trigger drop
        husk.setPlayerX(5f)
        // First update transitions FLOATING -> DROPPING (no gravity applied yet)
        husk.update(0f)
        val capV = slot<Vector2>()
        every { body.linearVelocity = capture(capV) } answers { }

        `when`("a second update with delta=1/60s runs") {
            husk.update(1f / 60f)
            val firstFrameVy = capV.captured.y

            then("downward velocity is non-zero (gravity has kicked in)") {
                firstFrameVy shouldBe ((-DriftHusk.GRAVITY / 60f) plusOrMinus eps)
            }

            `when`("a third update with delta=1/60s runs") {
                husk.update(1f / 60f)
                then("downward velocity has grown (accelerating fall)") {
                    val secondFrameVy = capV.captured.y
                    (secondFrameVy < firstFrameVy) shouldBe true
                    secondFrameVy shouldBe ((-DriftHusk.GRAVITY * 2f / 60f) plusOrMinus eps)
                }
            }
        }
    }

    // ── terrain hit → COOLDOWN ───────────────────────────────────────────────

    given("a husk in DROPPING that hits terrain") {
        val (body, _) = mockBodyAt(5f, 4f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)
        husk.setPlayerX(5f)
        husk.update(1f / 60f)
        husk.state shouldBe DriftHusk.State.DROPPING

        `when`("the contact listener flips hitTerrain=true and update runs") {
            husk.hitTerrain = true
            husk.update(1f / 60f)

            then("state transitions to COOLDOWN") {
                husk.state shouldBe DriftHusk.State.COOLDOWN
            }
            then("hitTerrain is drained back to false") {
                husk.hitTerrain shouldBe false
            }
        }
    }

    // ── COOLDOWN → FLOATING after 4s, respawning at the origin ───────────────

    given("a husk in COOLDOWN") {
        val (body, _) = mockBodyAt(5f, 4f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)
        husk.setPlayerX(5f)
        husk.update(1f / 60f)        // enter DROPPING
        husk.hitTerrain = true
        husk.update(1f / 60f)        // enter COOLDOWN
        husk.state shouldBe DriftHusk.State.COOLDOWN

        `when`("less than 4 seconds have elapsed") {
            husk.update(2f)
            then("state stays COOLDOWN") {
                husk.state shouldBe DriftHusk.State.COOLDOWN
            }
        }

        `when`("a total of 4+ seconds have elapsed") {
            // We previously ticked 2s above on the same husk; one more big tick
            // takes total cooldown elapsed past 4s.
            husk.update(2.1f)
            then("state returns to FLOATING") {
                husk.state shouldBe DriftHusk.State.FLOATING
            }
            then("body.setTransform was called with the original (originX, originY)") {
                verify { body.setTransform(5f, 6f, any()) }
            }
        }
    }

    // ── damage / death ───────────────────────────────────────────────────────

    given("a fresh DriftHusk (HP = 2)") {

        `when`("takeDamage(1) is called once") {
            val (body, _) = mockBodyAt(0f)
            val husk = newHusk(body)
            husk.takeDamage(1)

            then("HP drops to 1") { husk.hp shouldBe 1 }
            then("is not yet dead") { husk.isDead shouldBe false }
        }

        `when`("takeDamage(1) is called twice (two seed-slam hits)") {
            val (body, _) = mockBodyAt(0f)
            val husk = newHusk(body)
            husk.takeDamage(1)
            husk.takeDamage(1)

            then("HP reaches 0") { husk.hp shouldBe 0 }
            then("isDead flag flips at HP 0") { husk.isDead shouldBe true }
        }

        `when`("takeDamage(5) overkills in one hit") {
            val (body, _) = mockBodyAt(0f)
            val husk = newHusk(body)
            husk.takeDamage(5)

            then("HP clamps to 0 (no negative HP)") { husk.hp shouldBe 0 }
            then("isDead is true") { husk.isDead shouldBe true }
        }
    }

    given("a DriftHusk that has just been killed") {
        val (body, _) = mockBodyAt(5f, 6f)
        val husk = newHusk(body, originX = 5f, originY = 6f, triggerX = 5f)
        husk.takeDamage(2)

        `when`("update() is called after death") {
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            husk.update(1f / 60f)

            then("velocity is zeroed (no movement post-death)") {
                capV.captured.x shouldBe (0f plusOrMinus eps)
                capV.captured.y shouldBe (0f plusOrMinus eps)
            }
        }
    }

    // ── stomp flag (set externally by WorldContactListener) ──────────────────

    given("the wasStomped flag") {
        `when`("set true (simulating a player stomp from the contact listener)") {
            val (body, _) = mockBodyAt(0f)
            val husk = newHusk(body)
            husk.wasStomped = true

            then("flag flips correctly") { husk.wasStomped shouldBe true }
        }
    }
})

package com.sohai.platformer.entities

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

/**
 * Pure-logic tests for [SmogSprite].
 *
 * `SmogSprite`'s `create()` factory builds a real Box2D body + fixture, which
 * requires the libgdx Box2D native library (not available in unit-test JVMs
 * — see PlayerControllerMovementTest for the same constraint). To exercise
 * the patrol/AI logic without natives we:
 *
 *   1. mock the Box2D `Body` (a pure-Java interface surface — same trick
 *      used in [com.sohai.platformer.physics.WorldContactListenerTest]).
 *   2. reach the private `SmogSprite(body, leftX, rightX, speed)` constructor
 *      via reflection so we never go through `create()` (which calls
 *      `world.createBody`, `PolygonShape()`, etc.).
 *
 * Only the patrol AI and inherited damage/death logic are tested; rendering
 * is excluded (needs a live `ShapeRenderer` and OpenGL context).
 */
class SmogSpriteTest : BehaviorSpec({

    val eps = 0.0001f

    /** Build a SmogSprite bypassing the private ctor via reflection. */
    fun newSprite(
        body: Body,
        leftX: Float = 0f,
        rightX: Float = 4f,
        speed: Float = 2f,
    ): SmogSprite {
        val ctor = SmogSprite::class.java.getDeclaredConstructor(
            Body::class.java,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
        )
        ctor.isAccessible = true
        return ctor.newInstance(body, leftX, rightX, speed)
    }

    /**
     * Build a mocked Body whose `position` reflects a mutable Vector2 and
     * whose `linearVelocity` setter writes through to a captured slot so
     * tests can assert on the most recent value. The same Vector2 is
     * returned by reference so production code can mutate it (it does not).
     */
    fun mockBodyAt(x: Float, y: Float = 0f): Pair<Body, Vector2> {
        val pos = Vector2(x, y)
        val body = mockk<Body>(relaxed = true)
        every { body.position } returns pos
        return body to pos
    }

    // ── construction ─────────────────────────────────────────────────────────

    given("a freshly constructed SmogSprite with non-overlapping waypoints") {
        val (body, _) = mockBodyAt(1f)
        val sprite = newSprite(body, leftX = 0f, rightX = 4f, speed = 2f)

        `when`("inspecting initial state") {
            then("starts at full HP = 2") { sprite.hp shouldBe 2 }
            then("is not dead") { sprite.isDead shouldBe false }
            then("wasStomped defaults to false") { sprite.wasStomped shouldBe false }
        }
    }

    given("a SmogSprite constructed with leftX == rightX (degenerate waypoints)") {
        // Source has no guard against this — verify no crash and the sprite
        // immediately oscillates direction without moving.
        val (body, _) = mockBodyAt(2f)
        val sprite = newSprite(body, leftX = 2f, rightX = 2f, speed = 2f)

        `when`("update is called once") {
            sprite.update(1f / 60f)

            then("no crash; sprite is still alive") { sprite.isDead shouldBe false }
        }
    }

    // ── patrol direction ─────────────────────────────────────────────────────

    given("a SmogSprite patrolling between x=0 and x=4 at speed 2") {

        `when`("starting at the midpoint (x=2) and updating once") {
            val (body, _) = mockBodyAt(2f)
            val sprite = newSprite(body, leftX = 0f, rightX = 4f, speed = 2f)
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            sprite.update(1f / 60f)

            then("velocity is set to +speed in x (moving right)") {
                capV.captured.x shouldBe (2f plusOrMinus eps)
                capV.captured.y shouldBe (0f plusOrMinus eps)
            }
        }

        `when`("the sprite reaches the right waypoint (x=4)") {
            val (body, _) = mockBodyAt(4f)
            val sprite = newSprite(body, leftX = 0f, rightX = 4f, speed = 2f)
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            sprite.update(1f / 60f)

            then("direction reverses → velocity is -speed (moving left)") {
                capV.captured.x shouldBe (-2f plusOrMinus eps)
            }
        }

        `when`("the sprite has just reversed and reaches the left waypoint (x=0)") {
            // Force initial movingRight = false via two updates:
            // first update at x=4 flips to left, then jump body to x=0
            // and update again to flip back.
            val pos = Vector2(4f, 0f)
            val body = mockk<Body>(relaxed = true)
            every { body.position } returns pos
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            val sprite = newSprite(body, leftX = 0f, rightX = 4f, speed = 2f)
            sprite.update(1f / 60f)        // at x=4 → flip to moving left
            capV.captured.x shouldBe (-2f plusOrMinus eps)

            pos.x = 0f                     // simulate physics step
            sprite.update(1f / 60f)        // at x=0 → flip to moving right

            then("direction reverses → velocity is +speed again") {
                capV.captured.x shouldBe (2f plusOrMinus eps)
            }
        }

        `when`("the sprite is strictly inside the patrol band") {
            val (body, _) = mockBodyAt(1f) // 0 < 1 < 4, no reversal expected
            val sprite = newSprite(body, leftX = 0f, rightX = 4f, speed = 2f)
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            sprite.update(1f / 60f)

            then("keeps moving in the initial direction (+speed)") {
                capV.captured.x shouldBe (2f plusOrMinus eps)
            }
        }
    }

    // ── damage / death ───────────────────────────────────────────────────────

    given("a fresh SmogSprite (HP = 2)") {

        `when`("takeDamage(1) is called once") {
            val (body, _) = mockBodyAt(0f)
            val sprite = newSprite(body)
            sprite.takeDamage(1)

            then("HP drops to 1") { sprite.hp shouldBe 1 }
            then("is not yet dead") { sprite.isDead shouldBe false }
        }

        `when`("takeDamage(1) is called twice (two seed-slam hits)") {
            val (body, _) = mockBodyAt(0f)
            val sprite = newSprite(body)
            sprite.takeDamage(1)
            sprite.takeDamage(1)

            then("HP reaches 0") { sprite.hp shouldBe 0 }
            then("isDead is true") { sprite.isDead shouldBe true }
        }

        `when`("takeDamage(5) overkills in one hit") {
            val (body, _) = mockBodyAt(0f)
            val sprite = newSprite(body)
            sprite.takeDamage(5)

            then("HP clamps to 0 (no negative HP)") { sprite.hp shouldBe 0 }
            then("isDead is true") { sprite.isDead shouldBe true }
        }

        `when`("takeDamage is called on an already-dead sprite") {
            val (body, _) = mockBodyAt(0f)
            val sprite = newSprite(body)
            sprite.takeDamage(2) // kill
            val hpAfterKill = sprite.hp
            sprite.takeDamage(99) // should be a no-op

            then("HP does not go further negative") { sprite.hp shouldBe hpAfterKill }
            then("still dead") { sprite.isDead shouldBe true }
        }
    }

    given("a SmogSprite that has just been killed") {
        val (body, _) = mockBodyAt(2f)
        val sprite = newSprite(body, leftX = 0f, rightX = 4f, speed = 2f)
        sprite.takeDamage(2)

        `when`("update() is called after death") {
            val capV = slot<Vector2>()
            every { body.linearVelocity = capture(capV) } answers { }

            sprite.update(1f / 60f)

            then("velocity is zeroed (no patrol motion)") {
                capV.captured.x shouldBe (0f plusOrMinus eps)
                capV.captured.y shouldBe (0f plusOrMinus eps)
            }
        }
    }

    // ── stomp flag (set externally by WorldContactListener) ──────────────────

    given("the wasStomped flag") {
        `when`("set true (simulating a player stomp from the contact listener)") {
            val (body, _) = mockBodyAt(0f)
            val sprite = newSprite(body)
            sprite.wasStomped = true

            then("flag flips correctly") { sprite.wasStomped shouldBe true }
        }
    }
})

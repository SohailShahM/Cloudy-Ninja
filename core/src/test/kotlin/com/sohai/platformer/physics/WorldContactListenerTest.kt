package com.sohai.platformer.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.Fixture
import com.sohai.platformer.entities.PlayerController
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

/**
 * Tests for [WorldContactListener].
 *
 * Each Box2D fixture exposes a `userData` string that the contact listener
 * uses to drive gameplay state transitions on the player. Because building a
 * real Box2D `World` + `PlayerController` is heavy (and would tug in native
 * libs at unit-test time), we mock the Box2D contract surface with MockK and
 * assert that the listener calls the expected method on the player mock.
 */
class WorldContactListenerTest : DescribeSpec({

    /** Build a [Contact] whose two fixtures have the given user-data values. */
    fun mockContact(
        udA: Any?,
        udB: Any?,
        playerOnA: PlayerController? = null,
        playerOnB: PlayerController? = null,
    ): Contact {
        val bodyA = mockk<Body>(relaxed = true)
        val bodyB = mockk<Body>(relaxed = true)
        every { bodyA.userData } returns playerOnA
        every { bodyB.userData } returns playerOnB
        every { bodyA.position } returns Vector2(0f, 0f)
        every { bodyB.position } returns Vector2(0f, 0f)

        val fixA = mockk<Fixture>(relaxed = true)
        val fixB = mockk<Fixture>(relaxed = true)
        every { fixA.userData } returns udA
        every { fixB.userData } returns udB
        every { fixA.body } returns bodyA
        every { fixB.body } returns bodyB

        val contact = mockk<Contact>(relaxed = true)
        every { contact.fixtureA } returns fixA
        every { contact.fixtureB } returns fixB
        return contact
    }

    /** Convenience: a mocked PlayerController whose contact callbacks are no-ops. */
    fun mockPlayer(): PlayerController {
        val player = mockk<PlayerController>(relaxed = true)
        every { player.onGroundContact(any()) } just Runs
        every { player.onWallLeftContact(any()) } just Runs
        every { player.onWallRightContact(any()) } just Runs
        every { player.setSpawn(any()) } just Runs
        every { player.isFlashing } returns false
        return player
    }

    describe("WorldContactListener") {
        val listener = WorldContactListener()

        // ---------------- player_foot ----------------
        describe("player_foot fixture") {
            it("forwards beginContact to PlayerController.onGroundContact(true)") {
                val player = mockPlayer()
                val contact = mockContact(udA = "player_foot", udB = "ground", playerOnA = player)
                listener.beginContact(contact)
                verify(exactly = 1) { player.onGroundContact(true) }
            }

            it("forwards endContact to PlayerController.onGroundContact(false)") {
                val player = mockPlayer()
                val contact = mockContact(udA = "ground", udB = "player_foot", playerOnB = player)
                listener.endContact(contact)
                verify(exactly = 1) { player.onGroundContact(false) }
            }

            it("ignores contacts with irrelevant userData (no ground call)") {
                val player = mockPlayer()
                // Neither fixture is "player_foot" — should not invoke onGroundContact.
                val contact = mockContact(udA = "ground", udB = "ground", playerOnA = player, playerOnB = player)
                listener.beginContact(contact)
                listener.endContact(contact)
                verify(exactly = 0) { player.onGroundContact(any()) }
            }
        }

        // ---------------- player_wall_left ----------------
        describe("player_wall_left fixture") {
            it("forwards beginContact to PlayerController.onWallLeftContact(true)") {
                val player = mockPlayer()
                val contact = mockContact(udA = "player_wall_left", udB = "ground", playerOnA = player)
                listener.beginContact(contact)
                verify(exactly = 1) { player.onWallLeftContact(true) }
            }

            it("forwards endContact to PlayerController.onWallLeftContact(false)") {
                val player = mockPlayer()
                val contact = mockContact(udA = "ground", udB = "player_wall_left", playerOnB = player)
                listener.endContact(contact)
                verify(exactly = 1) { player.onWallLeftContact(false) }
            }

            it("ignores contacts that don't carry player_wall_left userData") {
                val player = mockPlayer()
                val contact = mockContact(udA = "ground", udB = "hazard", playerOnA = player)
                listener.beginContact(contact)
                verify(exactly = 0) { player.onWallLeftContact(any()) }
            }
        }

        // ---------------- player_wall_right ----------------
        describe("player_wall_right fixture") {
            it("forwards beginContact to PlayerController.onWallRightContact(true)") {
                val player = mockPlayer()
                val contact = mockContact(udA = "player_wall_right", udB = "ground", playerOnA = player)
                listener.beginContact(contact)
                verify(exactly = 1) { player.onWallRightContact(true) }
            }

            it("forwards endContact to PlayerController.onWallRightContact(false)") {
                val player = mockPlayer()
                val contact = mockContact(udA = "ground", udB = "player_wall_right", playerOnB = player)
                listener.endContact(contact)
                verify(exactly = 1) { player.onWallRightContact(false) }
            }

            it("ignores contacts that don't carry player_wall_right userData") {
                val player = mockPlayer()
                val contact = mockContact(udA = "ground", udB = "hazard", playerOnA = player)
                listener.beginContact(contact)
                verify(exactly = 0) { player.onWallRightContact(any()) }
            }
        }

        // ---------------- hazard ----------------
        describe("hazard fixture") {
            it("flips PlayerController.isDead to true when player touches hazard") {
                val player = mockPlayer()
                every { player.isDead = any() } just Runs
                val contact = mockContact(udA = "hazard", udB = "player_foot", playerOnB = player)
                listener.beginContact(contact)
                verify(exactly = 1) { player.isDead = true }
            }

            it("does NOT kill a flashing (post-respawn invincible) player") {
                val player = mockPlayer()
                every { player.isFlashing } returns true
                every { player.isDead = any() } just Runs
                val contact = mockContact(udA = "hazard", udB = "player_foot", playerOnB = player)
                listener.beginContact(contact)
                verify(exactly = 0) { player.isDead = true }
            }

            it("does nothing on a hazard-less contact (negative case)") {
                val player = mockPlayer()
                every { player.isDead = any() } just Runs
                val contact = mockContact(udA = "ground", udB = "player_foot", playerOnB = player)
                listener.beginContact(contact)
                verify(exactly = 0) { player.isDead = true }
            }
        }

        // ---------------- ground ----------------
        // "ground" is a passive collider — the listener never branches on it
        // alone. Verify we don't accidentally trigger any player callbacks
        // when only "ground" appears on both fixtures.
        describe("ground fixture") {
            it("triggers no player state changes by itself (positive: ground+player_foot still routes through player_foot branch)") {
                val player = mockPlayer()
                every { player.isDead = any() } just Runs
                val contact = mockContact(udA = "player_foot", udB = "ground", playerOnA = player)
                listener.beginContact(contact)
                // Only the foot branch should fire; no death, no wall calls.
                verify(exactly = 1) { player.onGroundContact(true) }
                verify(exactly = 0) { player.onWallLeftContact(any()) }
                verify(exactly = 0) { player.onWallRightContact(any()) }
                verify(exactly = 0) { player.isDead = true }
            }

            it("ground-on-ground contact fires no player callbacks (negative)") {
                val player = mockPlayer()
                every { player.isDead = any() } just Runs
                val contact = mockContact(udA = "ground", udB = "ground")
                listener.beginContact(contact)
                listener.endContact(contact)
                verify(exactly = 0) { player.onGroundContact(any()) }
                verify(exactly = 0) { player.onWallLeftContact(any()) }
                verify(exactly = 0) { player.onWallRightContact(any()) }
                verify(exactly = 0) { player.isDead = true }
            }
        }
    }
})

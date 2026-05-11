package com.sohai.platformer.physics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.effects.WaterDroplet
import com.sohai.platformer.physics.CleanseEventQueue
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.SnapshotPickup

class WorldContactListener : ContactListener {
    override fun beginContact(contact: Contact) {
        val fixA = contact.fixtureA
        val fixB = contact.fixtureB

        handleContact(fixA, fixB, true)
    }

    override fun endContact(contact: Contact) {
        val fixA = contact.fixtureA
        val fixB = contact.fixtureB

        handleContact(fixA, fixB, false)
    }

    private fun handleContact(fixA: Fixture, fixB: Fixture, begin: Boolean) {
        val udA = fixA.userData
        val udB = fixB.userData

        // Droplet hitting hazard -> cleanse hazard and destroy droplet
        if (begin) {
            when {
                udA is WaterDroplet && udB == "hazard" -> {
                    fixB.userData = "hazard_cleaned"
                    val pos = fixB.body.position
                    CleanseEventQueue.push(pos.x, pos.y)
                    udA.destroy()
                }
                udB is WaterDroplet && udA == "hazard" -> {
                    fixA.userData = "hazard_cleaned"
                    val pos = fixA.body.position
                    CleanseEventQueue.push(pos.x, pos.y)
                    udB.destroy()
                }
            }
        }

        // Droplet hitting enemy -> damage enemy and destroy droplet
        if (begin) {
            when {
                udA is WaterDroplet && udB == "enemy" -> {
                    val enemy = fixB.body.userData as? Enemy
                    enemy?.takeDamage(1)
                    udA.destroy()
                }
                udB is WaterDroplet && udA == "enemy" -> {
                    val enemy = fixA.body.userData as? Enemy
                    enemy?.takeDamage(1)
                    udB.destroy()
                }
            }
        }

        // Player touching enemy -> kill player (same as hazard)
        if (begin && (udA == "enemy" || udB == "enemy")) {
            val playerFixture = if (udA == "enemy") fixB else fixA
            val player = playerFixture.body.userData as? PlayerController
            if (player != null && !player.isFlashing) {
                player.isDead = true
            }
        }

        // Checkpoint activation: player touches checkpoint -> save spawn.
        // Allow re-touching activated checkpoints to refresh spawn.
        if (begin) {
            val checkpointA = (udA == "checkpoint" || udA == "checkpoint_activated")
            val checkpointB = (udB == "checkpoint" || udB == "checkpoint_activated")

            when {
                checkpointA && fixB.body.userData is PlayerController -> {
                    val player = fixB.body.userData as PlayerController
                    player.setSpawn(fixA.body.position)
                    fixA.userData = "checkpoint_activated"
                }
                checkpointB && fixA.body.userData is PlayerController -> {
                    val player = fixA.body.userData as PlayerController
                    player.setSpawn(fixB.body.position)
                    fixB.userData = "checkpoint_activated"
                }
            }
        }

        // EcoToken collection
        if (begin) {
            when {
                udA is EcoToken && udB == "player_body" -> udA.collect()
                udB is EcoToken && udA == "player_body" -> udB.collect()
            }
        }

        // SnapshotPickup (Cloud Atlas) collection
        if (begin) {
            when {
                udA is SnapshotPickup && udB == "player_body" -> udA.collect()
                udB is SnapshotPickup && udA == "player_body" -> udB.collect()
            }
        }

        // Exit sensor
        if ((udA == "exit" || udB == "exit") && begin) {
            val playerFixture = if (udA == "exit") fixB else fixA
            (playerFixture.body.userData as? PlayerController)?.hasReachedExit = true
        }

        // Player hazard detection (player death) — skip while flashing (post-respawn invincibility)
        if ((udA == "hazard" || udB == "hazard") && begin) {
            val playerFixture = if (udA == "hazard") fixB else fixA
            val player = playerFixture.body.userData as? PlayerController
            if (player != null && !player.isFlashing) {
                player.isDead = true
            }
        }

        // Ground detection — moving-platform carry is now handled by Box2D
        // friction (see PlayerController), so we no longer track platform
        // contacts in user code.
        if (udA == "player_foot" || udB == "player_foot") {
            val playerFixture = if (udA == "player_foot") fixA else fixB
            val otherFixture  = if (udA == "player_foot") fixB else fixA
            val player = playerFixture.body.userData as? PlayerController
            player?.onGroundContact(begin)

            // Defensive logging: trace every moving-platform contact so we can
            // detect missed contact-end events (the suspected cause of the stale
            // body crash — T-017).
            val otherUserData = otherFixture.userData
            if (otherUserData == "moving_platform" || otherFixture.body.userData is MovingPlatform) {
                val platform = otherFixture.body.userData as? MovingPlatform
                val platformBodyId = platform?.body?.hashCode() ?: otherFixture.body.hashCode()
                if (begin) {
                    Gdx.app.log(
                        "ContactListener",
                        "Platform contact BEGIN — platformBody=0x${platformBodyId.toString(16)}"
                    )
                } else {
                    Gdx.app.log(
                        "ContactListener",
                        "Platform contact END   — platformBody=0x${platformBodyId.toString(16)}"
                    )
                    // Sanity check: warn if the platform body no longer appears valid.
                    // A body in a destroyed or inactive state is a red flag for stale
                    // references — exactly the scenario that triggers the JNI crash.
                    if (platform != null) {
                        try {
                            val stillActive = platform.body.isActive
                            if (!stillActive) {
                                Gdx.app.error(
                                    "ContactListener",
                                    "WARN: platform contact END — body 0x${platformBodyId.toString(16)} is INACTIVE (stale reference risk)"
                                )
                            }
                        } catch (e: Exception) {
                            Gdx.app.error(
                                "ContactListener",
                                "WARN: platform contact END — body 0x${platformBodyId.toString(16)} threw on isActive check: ${e.message}"
                            )
                        }
                    }
                }
            }
        }

        // Left wall detection
        if (udA == "player_wall_left" || udB == "player_wall_left") {
            val playerFixture = if (udA == "player_wall_left") fixA else fixB
            val player = playerFixture.body.userData as? PlayerController
            player?.onWallLeftContact(begin)
        }

        // Right wall detection
        if (udA == "player_wall_right" || udB == "player_wall_right") {
            val playerFixture = if (udA == "player_wall_right") fixA else fixB
            val player = playerFixture.body.userData as? PlayerController
            player?.onWallRightContact(begin)
        }
    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) {}
    override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
}

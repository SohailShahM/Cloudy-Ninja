package com.sohai.platformer.physics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.effects.WaterDroplet
import com.sohai.platformer.physics.CleanseEventQueue
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.Projectile
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.entities.StormSentinel
import com.sohai.platformer.rendering.ScreenShake
import com.sohai.platformer.Constants

/**
 * Tunable constants for screen-shake feedback (T-116). Kept here so the
 * contact callbacks read as plain magic-number-free trigger calls.
 */
private const val SHAKE_AMPLITUDE = 4f
private const val SHAKE_DURATION  = 0.15f

class WorldContactListener : ContactListener {
    override fun beginContact(contact: Contact) {
        val fixA = contact.fixtureA
        val fixB = contact.fixtureB

        handleContact(fixA, fixB, true, contact)
    }

    override fun endContact(contact: Contact) {
        val fixA = contact.fixtureA
        val fixB = contact.fixtureB

        handleContact(fixA, fixB, false, contact)
    }

    private fun handleContact(fixA: Fixture, fixB: Fixture, begin: Boolean, contact: Contact) {
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

        // Droplet hitting Storm Sentinel boss -> damage boss and destroy droplet
        if (begin) {
            when {
                udA is WaterDroplet && udB == "boss_sentinel" -> {
                    (fixB.body.userData as? StormSentinel)?.takeDamage()
                    // T-116: boss hit-confirm — small kick on every damage tick.
                    // ScreenShake.trigger() honours reducedMotion internally.
                    ScreenShake.trigger(SHAKE_AMPLITUDE, SHAKE_DURATION)
                    udA.destroy()
                }
                udB is WaterDroplet && udA == "boss_sentinel" -> {
                    (fixA.body.userData as? StormSentinel)?.takeDamage()
                    ScreenShake.trigger(SHAKE_AMPLITUDE, SHAKE_DURATION)
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

        // Player touching enemy -> stomp (from above) or kill player (lateral)
        if (begin && (udA == "enemy" || udB == "enemy")) {
            val enemyFixture  = if (udA == "enemy") fixA else fixB
            val playerFixture = if (udA == "enemy") fixB else fixA
            val player = playerFixture.body.userData as? PlayerController
            val enemy  = enemyFixture.body.userData as? Enemy

            if (player != null && enemy != null && !enemy.isDead) {
                val playerVy = player.body.linearVelocity.y
                // Check contact normal to determine if player is above the enemy.
                // worldManifold.normal points from A to B; we need "up" relative
                // to the enemy, so normalise direction based on fixture order.
                val normal = contact.worldManifold.normal
                val normalYTowardPlayer = if (udA == "enemy") normal.y else -normal.y

                val isStomp = playerVy < -3f && normalYTowardPlayer > 0.5f

                if (isStomp) {
                    // Stomp: defeat enemy instantly + bounce the player
                    enemy.takeDamage(enemy.hp)
                    enemy.wasStomped = true
                    player.body.setLinearVelocity(player.body.linearVelocity.x, 5f)
                    // T-116: stomp-defeat kick — pairs with T-098 hit-flash for
                    // full hit-feedback. trigger() honours reducedMotion internally.
                    ScreenShake.trigger(SHAKE_AMPLITUDE, SHAKE_DURATION)
                } else if (!player.isFlashing) {
                    // Lateral contact: kill the player
                    player.isDead = true
                }
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

        // Portal sensors (hub world) — userData starts with "portal_"
        val portalStrA = udA as? String
        val portalStrB = udB as? String
        val portalA = portalStrA?.startsWith("portal_") == true
        val portalB = portalStrB?.startsWith("portal_") == true
        if (portalA || portalB) {
            val portalId      = if (portalA) portalStrA else portalStrB
            val playerFixture = if (portalA) fixB else fixA
            val player = playerFixture.body.userData as? PlayerController
            if (player != null) {
                player.portalContact = if (begin) portalId else null
            }
        }

        // Player hazard detection (player death) — skip while flashing (post-respawn invincibility)
        if ((udA == "hazard" || udB == "hazard") && begin) {
            val playerFixture = if (udA == "hazard") fixB else fixA
            val player = playerFixture.body.userData as? PlayerController
            if (player != null && !player.isFlashing) {
                player.isDead = true
            }
        }

        // Projectile contacts — kill player or mark wall-hit for deferred destroy
        if (begin) {
            val projA = fixA.body.userData as? Projectile
            val projB = fixB.body.userData as? Projectile
            when {
                projA != null && fixB.body.userData is PlayerController -> {
                    val player = fixB.body.userData as PlayerController
                    if (!player.isFlashing) player.isDead = true
                    projA.hitWall = true  // expire on contact
                }
                projB != null && fixA.body.userData is PlayerController -> {
                    val player = fixA.body.userData as PlayerController
                    if (!player.isFlashing) player.isDead = true
                    projB.hitWall = true
                }
                projA != null && (udB == "ground" || udB == "hazard" || udB == "hazard_cleaned"
                    || fixB.filterData.categoryBits == Constants.BIT_GROUND
                    || fixB.filterData.categoryBits == Constants.BIT_WALL) -> {
                    projA.hitWall = true
                }
                projB != null && (udA == "ground" || udA == "hazard" || udA == "hazard_cleaned"
                    || fixA.filterData.categoryBits == Constants.BIT_GROUND
                    || fixA.filterData.categoryBits == Constants.BIT_WALL) -> {
                    projB.hitWall = true
                }
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

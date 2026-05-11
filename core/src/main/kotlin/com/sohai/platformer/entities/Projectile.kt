package com.sohai.platformer.entities

import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants

/**
 * A kinematic projectile hazard (e.g. lightning bolt) that moves at constant
 * velocity and kills the player on contact. Used by Storm Sentinel boss and
 * Storm Node static emitters.
 *
 * Body destruction is **never** done inline — the owning [LevelRunState] queues
 * expired projectiles into `pendingBodyDestroy` after the physics step.
 */
class Projectile(
    world: World,
    x: Float,
    y: Float,
    val vx: Float,
    val vy: Float,
    private val lifetime: Float = 3f
) {
    val body: Body
    var age = 0f
        private set
    var hitWall = false

    val isExpired: Boolean get() = age >= lifetime || hitWall

    companion object {
        /** World-space radius of the projectile circle fixture. */
        const val RADIUS = 4f / Constants.PPM  // ~4 px
    }

    init {
        val bdef = BodyDef().apply {
            type = BodyDef.BodyType.KinematicBody
            position.set(x, y)
        }
        body = world.createBody(bdef)
        body.userData = this

        val shape = CircleShape().apply {
            radius = RADIUS
        }

        val fdef = FixtureDef().apply {
            this.shape = shape
            isSensor = false
            filter.categoryBits = Constants.BIT_HAZARD
            filter.maskBits = (Constants.BIT_PLAYER.toInt() or Constants.BIT_GROUND.toInt() or Constants.BIT_WALL.toInt()).toShort()
        }
        body.createFixture(fdef).userData = "projectile"

        shape.dispose()

        // Kinematic bodies use setLinearVelocity — constant, no forces needed.
        body.setLinearVelocity(vx, vy)
    }

    /** Advance age; mark expired when lifetime runs out. */
    fun update(delta: Float) {
        age += delta
    }
}

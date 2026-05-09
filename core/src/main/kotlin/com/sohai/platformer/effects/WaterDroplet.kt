package com.sohai.platformer.effects

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.Gdx
import com.sohai.platformer.Constants

/**
 * A water droplet that falls and interacts with the world.
 * Destroyed after a short lifetime or on contact with hazards.
 */
class WaterDroplet(
    world: World,
    x: Float,
    y: Float,
    val vx: Float,
    val vy: Float,
    private val onDestroy: () -> Unit
) {
    val body: Body
    private var lifetime = 5f // 5 seconds before auto-cleanup
    var isAlive = true

    init {
        val bdef = BodyDef()
        bdef.type = BodyDef.BodyType.DynamicBody
        bdef.position.set(x / Constants.PPM, y / Constants.PPM)
        bdef.linearDamping = 0.5f

        body = world.createBody(bdef)
        // Store a reference to this droplet on the body/fixture so contact listener can find it
        body.userData = this

        // Small circle for droplet
        val shape = CircleShape()
        shape.radius = 0.2f

        val fdef = FixtureDef()
        fdef.shape = shape
        fdef.density = 0.1f
        fdef.friction = 0.3f
        fdef.restitution = 0.3f
        fdef.filter.categoryBits = Constants.BIT_DROPLET
        fdef.filter.maskBits = (Constants.BIT_GROUND.toInt() or Constants.BIT_WALL.toInt() or Constants.BIT_HAZARD.toInt()).toShort()

        // Attach the droplet instance as fixture userData so contacts can reference the object directly
        body.createFixture(fdef).userData = this
        body.linearVelocity = Vector2(vx, vy)

        shape.dispose()
    }

    fun update(deltaTime: Float) {
        lifetime -= deltaTime
        if (lifetime <= 0f) {
            destroy()
        }
    }

    fun destroy() {
        isAlive = false
        onDestroy()
    }

    fun getRadius(): Float = 0.2f
}


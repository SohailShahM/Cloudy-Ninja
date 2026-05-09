package com.sohai.platformer.entities

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants

class MovingPlatform(
    world: World,
    startX: Float,
    startY: Float,
    private val endX: Float,
    private val endY: Float,
    private val speed: Float
) {
    val body: Body
    private val startPos = Vector2(startX / Constants.PPM, startY / Constants.PPM)
    private val endPos = Vector2(endX / Constants.PPM, endY / Constants.PPM)
    private var movingToEnd = true

    init {
        val bdef = BodyDef()
        bdef.type = BodyDef.BodyType.KinematicBody
        bdef.position.set(startPos)
        body = world.createBody(bdef)
        body.userData = this  // back-reference for contact listener

        val shape = PolygonShape()
        shape.setAsBox(50f / Constants.PPM, 10f / Constants.PPM)
        val fdef = FixtureDef()
        fdef.shape = shape
        fdef.filter.categoryBits = Constants.BIT_GROUND
        fdef.filter.maskBits = Constants.BIT_PLAYER
        fdef.friction = 1f
        body.createFixture(fdef).userData = "moving_platform"

        shape.dispose()
    }

    fun update(deltaTime: Float) {
        val target = if (movingToEnd) endPos else startPos
        val pos = body.position
        
        val dist = pos.dst(target)
        if (dist < 0.1f) {
            movingToEnd = !movingToEnd
        }
        
        // Calculate velocity toward target
        val dir = Vector2(target).sub(pos).nor()
        body.linearVelocity = dir.scl(speed)
    }
}

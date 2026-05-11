package com.sohai.platformer.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants

/**
 * Smog Sprite -- a ground-patrolling enemy that moves between two x-waypoints
 * at a constant speed (~2 m/s). Reverses direction at waypoints.
 *
 * - Kills the player on lateral body contact (uses ENEMY category bits which
 *   the contact listener treats as hazardous).
 * - Defeated by 2 Seed Slam water droplet hits ([takeDamage]).
 * - Rendered as a dark-grey ShapeRenderer oval.
 *
 * Box2D setup: kinematic body so it patrols reliably without being pushed
 * by the player or terrain. Category = BIT_ENEMY, masks GROUND | PLAYER | DROPLET.
 */
class SmogSprite private constructor(
    body: Body,
    private val patrolLeftX: Float,
    private val patrolRightX: Float,
    private val speed: Float
) : Enemy(body, hp = 2) {

    /** True when moving toward patrolRightX. */
    private var movingRight: Boolean = true

    override fun update(delta: Float) {
        if (isDead) {
            body.linearVelocity = Vector2.Zero
            return
        }

        val posX = body.position.x

        // Reverse at waypoints
        if (movingRight && posX >= patrolRightX) {
            movingRight = false
        } else if (!movingRight && posX <= patrolLeftX) {
            movingRight = true
        }

        val vx = if (movingRight) speed else -speed
        body.linearVelocity = Vector2(vx, 0f)
    }

    override fun draw(renderer: ShapeRenderer) {
        if (isDead) return

        val px = body.position.x
        val py = body.position.y

        // Dark-grey oval body
        renderer.color = BODY_COLOR
        renderer.ellipse(px - HALF_W, py - HALF_H, HALF_W * 2f, HALF_H * 2f)

        // Small red "eyes" -- direction indicator
        val eyeOffsetX = if (movingRight) 0.06f else -0.06f
        renderer.color = EYE_COLOR
        renderer.circle(px + eyeOffsetX, py + 0.03f, 0.025f)
    }

    companion object {
        // Visual constants (hoisted to avoid per-frame allocation)
        private val BODY_COLOR   = Color(0.30f, 0.30f, 0.32f, 0.90f)
        private val EYE_COLOR    = Color(0.85f, 0.20f, 0.20f, 1.00f)
        private const val HALF_W = 0.15f   // metres (oval half-width)
        private const val HALF_H = 0.12f   // metres (oval half-height)

        /**
         * Factory method that creates the Box2D body and returns a ready
         * [SmogSprite]. The body uses a kinematic type with category
         * [Constants.BIT_ENEMY].
         *
         * @param world        The Box2D world to create the body in.
         * @param x            Spawn X position in world metres.
         * @param y            Spawn Y position in world metres.
         * @param patrolLeftX  Left waypoint in world metres.
         * @param patrolRightX Right waypoint in world metres.
         * @param speed        Patrol speed in m/s (default 2 m/s).
         */
        fun create(
            world: World,
            x: Float,
            y: Float,
            patrolLeftX: Float,
            patrolRightX: Float,
            speed: Float = 2f
        ): SmogSprite {
            val bdef = BodyDef().apply {
                type = BodyDef.BodyType.KinematicBody
                position.set(x, y)
            }
            val body = world.createBody(bdef)

            // Solid collision box (slightly smaller than visual)
            val solidShape = PolygonShape()
            solidShape.setAsBox(0.14f, 0.10f)
            val solidFdef = FixtureDef().apply {
                shape = solidShape
                friction = 0f
                restitution = 0f
                filter.categoryBits = Constants.BIT_ENEMY
                filter.maskBits = (Constants.BIT_GROUND.toInt() or Constants.BIT_PLAYER.toInt() or Constants.BIT_DROPLET.toInt()).toShort()
            }
            body.createFixture(solidFdef).userData = "enemy"

            solidShape.dispose()

            val sprite = SmogSprite(body, patrolLeftX, patrolRightX, speed)
            body.userData = sprite
            return sprite
        }
    }
}

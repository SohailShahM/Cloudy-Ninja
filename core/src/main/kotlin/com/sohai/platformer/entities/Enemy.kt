package com.sohai.platformer.entities

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World

/**
 * Abstract base class for all enemies in Cloudy Ninja.
 *
 * Enemies are polluted/corrupted entities defeated by the player's cleansing
 * abilities (Seed Slam droplets, stomp, Wind Dash). Each concrete subclass
 * defines its own movement AI and visual representation.
 *
 * Box2D bodies must be destroyed via the deferred destroy queue in
 * [com.sohai.platformer.screens.LevelRunState] -- never during a world step
 * or contact callback.
 */
abstract class Enemy(
    val body: Body,
    var hp: Int
) {
    var isDead: Boolean = false

    /**
     * Per-frame movement AI update. Called from [LevelRunState.update].
     */
    abstract fun update(delta: Float)

    /**
     * Draw the enemy using ShapeRenderer. Called from [LevelRenderer.renderWorld]
     * inside an open Filled block.
     */
    abstract fun draw(renderer: ShapeRenderer)

    /**
     * Apply damage to this enemy. When HP reaches 0 the enemy is marked dead
     * and its body should be queued for deferred destruction.
     */
    open fun takeDamage(amount: Int) {
        if (isDead) return
        hp -= amount
        if (hp <= 0) {
            hp = 0
            isDead = true
        }
    }

    /**
     * Remove this enemy's physics body from the world.
     * Only call from the deferred body-destroy drain (never during world.step).
     */
    fun destroy(world: World) {
        world.destroyBody(body)
    }
}

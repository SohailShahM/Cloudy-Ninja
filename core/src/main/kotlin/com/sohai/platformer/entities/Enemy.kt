package com.sohai.platformer.entities

import com.badlogic.gdx.graphics.Color
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
    /** Set by WorldContactListener when this enemy is defeated via a player stomp. */
    var wasStomped: Boolean = false

    /**
     * T-098: Seconds of "hit-flash" remaining. Set to [HIT_FLASH_SECONDS] each
     * time the enemy survives a [takeDamage] hit; decremented (clamped to >= 0)
     * by [tickHitFlash], which subclasses call from [update]. Renderers can lerp
     * the enemy's base body colour toward white by `clamp(timer / HIT_FLASH_SECONDS)`
     * for a brief on-hit feedback frame. Defeat path is unchanged -- the timer
     * is NOT set when the hit also kills the enemy.
     */
    var hitFlashTimer: Float = 0f

    /**
     * Per-frame movement AI update. Called from [LevelRunState.update].
     */
    abstract fun update(delta: Float)

    /**
     * T-098: Decrement [hitFlashTimer] by [delta] (clamped to >= 0). Subclasses
     * call this from their [update] override. Cheap no-op when not flashing.
     */
    protected fun tickHitFlash(delta: Float) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= delta
            if (hitFlashTimer < 0f) hitFlashTimer = 0f
        }
    }

    /**
     * T-098: Lerp factor in [0, 1] for the hit-flash white tint. Equals
     * `hitFlashTimer / HIT_FLASH_SECONDS` clamped to [0, 1]. Subclass [draw]
     * implementations call [applyHitFlash] to obtain the tinted body colour.
     */
    protected val hitFlashRatio: Float
        get() = (hitFlashTimer / HIT_FLASH_SECONDS).coerceIn(0f, 1f)

    /**
     * T-098: Write `lerp(base, white, hitFlashRatio)` into [out] and return it.
     * Caller owns [out]; this avoids per-frame Color allocation in the draw path.
     * When [hitFlashTimer] is 0f the result is byte-identical to [base].
     */
    protected fun applyHitFlash(base: Color, out: Color): Color {
        val t = hitFlashRatio
        if (t == 0f) {
            out.set(base)
        } else {
            out.r = base.r + (1f - base.r) * t
            out.g = base.g + (1f - base.g) * t
            out.b = base.b + (1f - base.b) * t
            out.a = base.a + (1f - base.a) * t
        }
        return out
    }

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
            // T-098: Don't paint a hit-flash on the frame the enemy dies --
            // the defeat path (smoke burst, body destroy) owns that frame.
            return
        }
        // T-098: Survived the hit -- arm the brief white-tint feedback flash.
        hitFlashTimer = HIT_FLASH_SECONDS
    }

    /**
     * Remove this enemy's physics body from the world.
     * Only call from the deferred body-destroy drain (never during world.step).
     */
    fun destroy(world: World) {
        world.destroyBody(body)
    }

    companion object {
        /** T-098: Duration in seconds of the on-hit white-tint flash. */
        const val HIT_FLASH_SECONDS: Float = 0.2f
    }
}

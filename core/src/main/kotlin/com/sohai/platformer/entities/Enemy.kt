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
     * T-170: Half-width (metres) of the high-contrast silhouette rectangle.
     * Subclasses override to tighten/loosen the cover relative to their
     * actual body footprint. Default matches the pre-T-170 generous box
     * (0.36m wide) that the LevelRenderer overlay used.
     */
    protected open val hcHalfWidth: Float
        get() = 0.18f

    /**
     * T-170: Half-height (metres) of the high-contrast silhouette rectangle.
     * Default matches the pre-T-170 generous box (0.32m tall) that the
     * LevelRenderer overlay used.
     */
    protected open val hcHalfHeight: Float
        get() = 0.16f

    /**
     * T-170: scratch Color reused per [drawHighContrast] call to avoid
     * allocation on the hit-flash lerp. Separate from any subclass scratch
     * used in [draw] so high-contrast doesn't clobber base-draw state.
     */
    private val hcTmp: Color = Color()

    /**
     * T-170: Draw the entity's high-contrast silhouette using [color] as the
     * base. Called from [LevelRenderer.renderWorld] inside the same open
     * Filled block as [draw], only when the high-contrast accessibility flag
     * is on. The default implementation paints a [hcHalfWidth] x [hcHalfHeight]
     * rectangle centred at the body position.
     *
     * Composes the T-098 hit-flash on top so the flash remains visible in
     * high-contrast mode: the base [color] is lerped toward white by the
     * current [hitFlashRatio]. Outside the flash window this is byte-identical
     * to painting [color] directly.
     *
     * No-op when [isDead]. Subclasses with different silhouette shapes (e.g.
     * StormSentinel's circle) override this method directly.
     */
    open fun drawHighContrast(renderer: ShapeRenderer, color: Color) {
        if (isDead) return
        val p = body.position
        renderer.color = applyHitFlash(color, hcTmp)
        renderer.rect(p.x - hcHalfWidth, p.y - hcHalfHeight, hcHalfWidth * 2f, hcHalfHeight * 2f)
    }

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

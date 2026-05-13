package com.sohai.platformer.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants

/**
 * Drift Husk -- a drop-from-above enemy. Floats stationary above its
 * [triggerX] coordinate. When the player crosses within [TRIGGER_BAND_WIDTH]
 * metres of triggerX, the husk switches to a gravity-driven drop. On hitting
 * terrain it enters a [COOLDOWN_SECONDS] cooldown, then respawns at its
 * original floating position.
 *
 * - Defeated by 2 Seed Slam droplet hits ([takeDamage]) -- same HP as
 *   SmogSprite.
 * - Can be stomped from above (the `wasStomped` flag is set externally by
 *   [com.sohai.platformer.physics.WorldContactListener], identical to
 *   SmogSprite).
 * - Pure deterministic state machine: FLOATING → DROPPING → COOLDOWN →
 *   FLOATING. No randomness.
 *
 * Box2D setup: kinematic body so the husk is not pushed by the player.
 * Gravity is simulated manually inside [update] by accumulating a downward
 * velocity and writing it to `body.linearVelocity` each frame; the husk's
 * position is integrated by the Box2D world step like any other kinematic
 * body. Category = BIT_ENEMY, masks GROUND | PLAYER | DROPLET.
 */
class DriftHusk private constructor(
    body: Body,
    private val originX: Float,
    private val originY: Float,
    private val triggerX: Float
) : Enemy(body, hp = 2) {

    /** Husk lifecycle states. */
    enum class State { FLOATING, DROPPING, COOLDOWN }

    /** Current state of the husk (visible to tests). */
    var state: State = State.FLOATING
        private set

    /** Accumulated downward speed (m/s) while in [State.DROPPING]. */
    private var dropSpeed: Float = 0f

    /** Seconds remaining in [State.COOLDOWN] before respawn. */
    private var cooldownTimer: Float = 0f

    /**
     * Latest player X position, fed in by [LevelRunState] each frame via
     * [setPlayerX] before [update] is called. Allows the husk to evaluate
     * its trigger band without taking a hard dependency on PlayerController.
     */
    private var lastPlayerX: Float = Float.NaN

    /**
     * Set by [com.sohai.platformer.physics.WorldContactListener] when the
     * husk's solid fixture touches a non-player fixture while DROPPING.
     * Drained by [update] which transitions the husk into COOLDOWN.
     */
    var hitTerrain: Boolean = false

    /** T-098: scratch Color reused per draw call to avoid allocation on the hit-flash lerp. */
    private val tmpColor: Color = Color()

    /**
     * Feed the current player x-coordinate into the husk so its trigger
     * logic can evaluate it on the next [update]. Called per-frame from
     * [com.sohai.platformer.screens.LevelRunState].
     */
    fun setPlayerX(x: Float) {
        lastPlayerX = x
    }

    override fun update(delta: Float) {
        // T-098: tick the on-hit white-flash timer every frame (cheap no-op when 0).
        tickHitFlash(delta)

        if (isDead) {
            body.linearVelocity = Vector2.Zero
            return
        }

        when (state) {
            State.FLOATING -> {
                // Stay perfectly still; gravity-on-kinematic does nothing
                // unless we set a velocity ourselves.
                body.linearVelocity = Vector2.Zero
                // Trigger check
                val px = lastPlayerX
                if (!px.isNaN() && MathUtils.isEqual(px, triggerX, TRIGGER_BAND_WIDTH)) {
                    state = State.DROPPING
                    dropSpeed = 0f
                }
            }
            State.DROPPING -> {
                // Manual gravity accumulation
                dropSpeed += GRAVITY * delta
                body.linearVelocity = Vector2(0f, -dropSpeed)
                // Terrain-hit handling -- WorldContactListener sets this flag
                if (hitTerrain) {
                    hitTerrain = false
                    state = State.COOLDOWN
                    cooldownTimer = COOLDOWN_SECONDS
                    body.linearVelocity = Vector2.Zero
                    dropSpeed = 0f
                }
            }
            State.COOLDOWN -> {
                body.linearVelocity = Vector2.Zero
                cooldownTimer -= delta
                if (cooldownTimer <= 0f) {
                    // Respawn at the original floating position
                    body.setTransform(originX, originY, body.angle)
                    state = State.FLOATING
                    cooldownTimer = 0f
                }
            }
        }
    }

    override fun draw(renderer: ShapeRenderer) {
        if (isDead) return
        if (state == State.COOLDOWN) return  // invisible while respawning

        val px = body.position.x
        val py = body.position.y

        // Faint trailing wisp (drawn first so the body overlays it)
        renderer.color = WISP_COLOR
        renderer.ellipse(px - HALF_W * 0.85f, py + 0.05f, HALF_W * 1.7f, HALF_H * 0.7f)

        // Floating purple oval body (T-098: tinted toward white for [hitFlashTimer] frames after a hit).
        renderer.color = applyHitFlash(BODY_COLOR, tmpColor)
        renderer.ellipse(px - HALF_W, py - HALF_H, HALF_W * 2f, HALF_H * 2f)
    }

    companion object {
        // Tuning constants
        const val TRIGGER_BAND_WIDTH: Float = 0.6f       // metres each side of triggerX
        const val GRAVITY: Float            = 12f        // m/s^2 (gentler than Box2D's ~10)
        const val COOLDOWN_SECONDS: Float   = 4f         // respawn delay after wall-hit

        // Visual constants (hoisted to avoid per-frame allocation)
        private val BODY_COLOR = Color(0.60f, 0.30f, 0.80f, 1.00f)
        private val WISP_COLOR = Color(0.75f, 0.55f, 0.95f, 0.35f)
        private const val HALF_W = 0.16f   // metres (oval half-width)
        private const val HALF_H = 0.14f   // metres (oval half-height)

        /**
         * Factory method that creates the Box2D body and returns a ready
         * [DriftHusk]. The body uses a kinematic type with category
         * [Constants.BIT_ENEMY].
         *
         * @param world     The Box2D world to create the body in.
         * @param x         Spawn X position in world metres (the floating origin).
         * @param y         Spawn Y position in world metres (the floating origin).
         * @param triggerX  Player x-coordinate that triggers the drop.
         */
        fun create(
            world: World,
            x: Float,
            y: Float,
            triggerX: Float
        ): DriftHusk {
            val bdef = BodyDef().apply {
                type = BodyDef.BodyType.KinematicBody
                position.set(x, y)
            }
            val body = world.createBody(bdef)

            val solidShape = PolygonShape()
            solidShape.setAsBox(0.15f, 0.12f)
            val solidFdef = FixtureDef().apply {
                shape = solidShape
                friction = 0f
                restitution = 0f
                filter.categoryBits = Constants.BIT_ENEMY
                filter.maskBits = (Constants.BIT_GROUND.toInt() or Constants.BIT_PLAYER.toInt() or Constants.BIT_DROPLET.toInt()).toShort()
            }
            body.createFixture(solidFdef).userData = "enemy"

            solidShape.dispose()

            val husk = DriftHusk(body, x, y, triggerX)
            body.userData = husk
            return husk
        }
    }
}

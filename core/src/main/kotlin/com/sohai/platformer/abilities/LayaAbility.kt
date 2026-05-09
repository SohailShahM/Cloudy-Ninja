package com.sohai.platformer.abilities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.effects.WindTrail
import com.sohai.platformer.entities.PlayerController

/**
 * Laya's Atmospheric ability: Wind Dash.
 * On activation, applies a forward impulse and boosts jump height for mobility.
 */
class LayaAbility(
    private val world: World,
    private var playerController: PlayerController? = null
) : CharacterAbility {
    private var cooldownTimer = 0f
    private val cooldownDuration = 1.2f // Slightly faster than Ebo for wind feel
    private var windBoostActive = false
    private var windBoostTimer = 0f
    private val windBoostDuration = 0.5f // Short burst
    private val activeWindTrails = mutableListOf<WindTrail>()

    fun setPlayerController(player: PlayerController) {
        playerController = player
    }

    override fun update(deltaTime: Float) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime
        }

        if (windBoostActive) {
            windBoostTimer -= deltaTime
            playerController?.body?.gravityScale = 0.25f
            if (windBoostTimer <= 0f) {
                windBoostActive = false
                playerController?.body?.gravityScale = 1f
            }
        }

        // Update and cleanup dead wind trails
        for (i in activeWindTrails.indices.reversed()) {
            val trail = activeWindTrails[i]
            trail.update(deltaTime)
            if (!trail.isAlive) {
                activeWindTrails.removeAt(i)
            }
        }

    }

    override fun onActionPressed() {
        // Only allow Wind Dash if cooldown is ready
        if (cooldownTimer <= 0f) {
            executeWindDash()
            cooldownTimer = cooldownDuration
        }
    }

    override fun onActionHeld() {
        // Wind Dash is a one-shot ability
    }

    override fun onActionReleased() {
        // Wind Dash is a one-shot ability
    }

    override fun getAbilityName(): String = "Wind Dash"

    override fun getCooldownRatio(): Float = (cooldownTimer / cooldownDuration).coerceIn(0f, 1f)

    private fun executeWindDash() {
        if (playerController == null) return

        val playerBody = playerController!!.body
        val playerPos = playerBody.position

        // Determine direction based on tracked facing direction
        val facingRight = playerController!!.isFacingRight
        val windDirection = if (facingRight) 1f else -1f

        // Apply forward impulse (wind boost)
        playerBody.applyLinearImpulse(
            Vector2(windDirection * 8f, 0f),
            playerPos,
            true
        )

        // Apply upward impulse for mobility (jump boost)
        playerBody.applyLinearImpulse(
            Vector2(0f, 3f),
            playerPos,
            true
        )

        // Activate wind boost state
        windBoostActive = true
        windBoostTimer = windBoostDuration

        SoundManager.play("ability_laya")
        spawnWindEffect(playerPos, windDirection)

        Gdx.app.log("LayaAbility", "Wind Dash at (${playerPos.x}, ${playerPos.y}) dir=${if (facingRight) "RIGHT" else "LEFT"}")
    }

    private fun spawnWindEffect(epicenter: Vector2, windDirection: Float) {
        // Spawn 8 wind trails in a burst pattern
        val trailCount = 8

        for (i in 0 until trailCount) {
            val angle = (i / trailCount.toFloat()) * MathUtils.PI2
            val spreadX = MathUtils.cos(angle) * 1.5f
            val spreadY = MathUtils.sin(angle) * 1.5f

            val spawnX = (epicenter.x * 100f) + spreadX * 100f
            val spawnY = (epicenter.y * 100f) + spreadY * 100f

            // Wind trails move outward and slightly upward
            val vx = MathUtils.cos(angle) * 5f + windDirection * 3f
            val vy = MathUtils.sin(angle) * 3f + 1f

            activeWindTrails.add(WindTrail(spawnX, spawnY, vx, vy))
        }
    }

    fun isWindBoostActive(): Boolean = windBoostActive
    fun getActiveWindTrails(): List<WindTrail> = activeWindTrails
}

package com.sohai.platformer.abilities

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.effects.WindTrail
import com.sohai.platformer.entities.PlayerController

/**
 * Zephyr's ability: Lightweight Float.
 *
 * On action press, reduces the player's gravity scale to 0.2f for [floatDuration]
 * seconds, creating a brief floaty glide. After the float window expires the
 * gravity scale is restored to 1f and the ability enters a [cooldownDuration]
 * cooldown before it can be used again.
 *
 * Visual feedback: a radial burst of [WindTrail] particles at activation.
 */
class ZephyrAbility(
    private var playerController: PlayerController? = null
) : CharacterAbility {

    private var cooldownTimer = 0f
    private val cooldownDuration = 3f

    private var floatTimer = 0f
    private val floatDuration = 1.5f

    val isFloating get() = floatTimer > 0f

    private val activeWindTrails = mutableListOf<WindTrail>()

    fun setPlayerController(player: PlayerController) {
        playerController = player
    }

    override fun update(deltaTime: Float) {
        if (cooldownTimer > 0f) cooldownTimer -= deltaTime

        if (floatTimer > 0f) {
            floatTimer -= deltaTime
            playerController?.body?.gravityScale = 0.2f
            if (floatTimer <= 0f) {
                floatTimer = 0f
                playerController?.body?.gravityScale = 1f
            }
        }

        for (i in activeWindTrails.indices.reversed()) {
            activeWindTrails[i].update(deltaTime)
            if (!activeWindTrails[i].isAlive) activeWindTrails.removeAt(i)
        }
    }

    override fun onActionPressed() {
        if (cooldownTimer > 0f) return
        floatTimer = floatDuration
        cooldownTimer = cooldownDuration
        SoundManager.play("ability_laya")   // wind SFX shared with Laya until a Zephyr asset exists
        playerController?.body?.position?.let { spawnFloatEffect(it) }
    }

    override fun onActionHeld() {}
    override fun onActionReleased() {}

    override fun getAbilityName(): String = "Float"

    override fun getCooldownRatio(): Float = (cooldownTimer / cooldownDuration).coerceIn(0f, 1f)

    private fun spawnFloatEffect(pos: Vector2) {
        // Radial burst of 12 wind trails — wider spread than Laya's directional dash
        for (i in 0 until 12) {
            val angle = (i / 12f) * MathUtils.PI2
            val spreadX = MathUtils.cos(angle) * 120f   // pixels
            val spreadY = MathUtils.sin(angle) * 120f
            activeWindTrails.add(
                WindTrail(
                    pos.x * 100f + spreadX,
                    pos.y * 100f + spreadY,
                    vx = MathUtils.cos(angle) * 2.5f,
                    vy = MathUtils.sin(angle) * 2.5f + 0.5f
                )
            )
        }
    }

    fun getActiveWindTrails(): List<WindTrail> = activeWindTrails
}

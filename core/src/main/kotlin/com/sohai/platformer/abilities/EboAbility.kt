package com.sohai.platformer.abilities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.util.GameRandom
import com.sohai.platformer.effects.WaterDroplet
import com.sohai.platformer.entities.PlayerController

/**
 * Ebo's Precipitation ability: Seed Slam.
 * On activation, creates a rain effect that cleanses hazards and creates water platforms.
 */
class EboAbility(
    private val world: World,
    private var playerController: PlayerController? = null
) : CharacterAbility {
    private var cooldownTimer = 0f
    private val cooldownDuration = 1.5f // Time between Seed Slams
    private val activeRaindrops = mutableListOf<WaterDroplet>()

    fun setPlayerController(player: PlayerController) {
        playerController = player
    }

    override fun update(deltaTime: Float) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime
        }

        // Only tick lifetime; do NOT call world.destroyBody() here.
        // Bodies are destroyed safely after world.step() via drainDeadDroplets().
        for (drop in activeRaindrops) {
            drop.update(deltaTime)
        }
    }

    /**
     * Call this AFTER world.step() each frame.
     * Removes dead droplets and hands their bodies to [destroyFn] for
     * deferred destruction (never destroys during the physics step).
     */
    fun drainDeadDroplets(destroyFn: (com.badlogic.gdx.physics.box2d.Body) -> Unit) {
        for (i in activeRaindrops.indices.reversed()) {
            if (!activeRaindrops[i].isAlive) {
                destroyFn(activeRaindrops[i].body)
                activeRaindrops.removeAt(i)
            }
        }
    }

    override fun onActionPressed() {
        // Only allow Seed Slam if cooldown is ready
        if (cooldownTimer <= 0f) {
            executeSeedSlam()
            cooldownTimer = cooldownDuration
        }
    }

    override fun onActionHeld() {
        // Seed Slam is a one-shot ability; holding doesn't do anything extra
    }

    override fun onActionReleased() {
        // Seed Slam is a one-shot ability; release doesn't do anything
    }

    override fun getAbilityName(): String = "Seed Slam"

    override fun getCooldownRatio(): Float = (cooldownTimer / cooldownDuration).coerceIn(0f, 1f)

    private fun executeSeedSlam() {
        if (playerController == null) return

        val playerBody = playerController!!.body
        val playerPos = playerBody.position

        // Don't fire when the player is below the playable floor
        if (playerPos.y < -0.2f) return

        // Apply a downward impulse (heavy rain effect)
        playerBody.applyLinearImpulse(
            Vector2(0f, -5f),
            playerPos,
            true
        )

        SoundManager.play("ability_ebo")
        spawnRainEffect(playerPos)

        Gdx.app.log("EboAbility", "Seed Slam at (${playerPos.x}, ${playerPos.y})")
    }

    private fun spawnRainEffect(epicenter: Vector2) {
        // 14 droplets in a tight downward cone (±45° from vertical), spawned near player feet
        val dropletCount = 14
        val baseAngle = -MathUtils.HALF_PI  // straight down
        val halfSpread = 45f * MathUtils.degreesToRadians

        val feetY = epicenter.y - 0.32f  // bottom of player hitbox

        for (i in 0 until dropletCount) {
            val t = i.toFloat() / (dropletCount - 1)
            val angle = baseAngle + (t - 0.5f) * halfSpread * 2f

            // Spawn clustered near player feet with small jitter (pixels)
            val dist = GameRandom.range(15f, 40f)
            val spawnX = epicenter.x * 100f + MathUtils.cos(angle) * dist
            val spawnY = feetY * 100f + MathUtils.sin(angle) * dist

            val speed = GameRandom.range(6f, 10f)
            val vx = MathUtils.cos(angle) * speed
            val vy = MathUtils.sin(angle) * speed  // negative (downward)

            activeRaindrops.add(WaterDroplet(world, spawnX, spawnY, vx, vy) {})
        }
    }

    fun getActiveRaindrops(): List<WaterDroplet> = activeRaindrops
}

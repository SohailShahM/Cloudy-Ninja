package com.sohai.platformer.effects

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

/**
 * A wind trail effect that visualizes Laya's Wind Dash.
 * These are purely visual (no physics body) and fade over time.
 */
class WindTrail(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float
) {
    private var lifetime = 0.8f // Shorter than water droplets for snappy feel
    var isAlive = true

    fun update(deltaTime: Float) {
        lifetime -= deltaTime
        if (lifetime <= 0f) {
            destroy()
        }
    }

    fun destroy() {
        isAlive = false
    }

    fun getAlpha(): Float {
        // Fade out over lifetime
        return lifetime / 0.8f
    }

    fun getCurrentPosition(): Vector2 {
        // Simple linear movement (no physics)
        val elapsed = 0.8f - lifetime
        return Vector2(x + vx * elapsed, y + vy * elapsed)
    }

    fun getRadius(): Float = 0.15f
}


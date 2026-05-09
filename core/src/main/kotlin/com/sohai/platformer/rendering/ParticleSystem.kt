package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Pool-backed particle system for cheap visual juice.
 * Coordinates are world-meters (rendered with the game camera matrix).
 *
 * Capacity 200 is plenty for our scale; adjust if heavy combat is added.
 *
 * NOTE: render() must be called inside an existing
 *       shapeRenderer.begin(Filled) / end() block — it does NOT manage state.
 */
class ParticleSystem(maxParticles: Int = 200) {

    class Particle {
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var radius = 0f
        var lifeMax = 0f
        var lifeLeft = 0f
        val color = Color()
        var gravity = 0f       // m/s² downward (positive = falls); 0 = floats
        var alive = false
    }

    private val pool: Array<Particle> = Array(maxParticles) { Particle() }
    private var nextIdx = 0

    fun spawn(
        x: Float, y: Float,
        vx: Float, vy: Float,
        radius: Float,
        life: Float,
        color: Color,
        gravity: Float = 0f
    ) {
        // Find a dead particle, or overwrite the oldest live one.
        var p: Particle? = null
        for (i in pool.indices) {
            val idx = (nextIdx + i) % pool.size
            if (!pool[idx].alive) { p = pool[idx]; nextIdx = (idx + 1) % pool.size; break }
        }
        if (p == null) {
            p = pool[nextIdx]
            nextIdx = (nextIdx + 1) % pool.size
        }
        p.x = x; p.y = y; p.vx = vx; p.vy = vy
        p.radius = radius
        p.lifeMax = life; p.lifeLeft = life
        p.color.set(color)
        p.gravity = gravity
        p.alive = true
    }

    fun update(dt: Float) {
        for (p in pool) {
            if (!p.alive) continue
            p.lifeLeft -= dt
            if (p.lifeLeft <= 0f) { p.alive = false; continue }
            p.vy -= p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
    }

    private val tmpColor = Color()

    fun render(sr: ShapeRenderer) {
        for (p in pool) {
            if (!p.alive) continue
            val a = (p.lifeLeft / p.lifeMax).coerceIn(0f, 1f)
            tmpColor.set(p.color.r, p.color.g, p.color.b, p.color.a * a)
            sr.color = tmpColor
            sr.circle(p.x, p.y, p.radius * a)
        }
    }

    fun clear() {
        for (p in pool) p.alive = false
    }
}

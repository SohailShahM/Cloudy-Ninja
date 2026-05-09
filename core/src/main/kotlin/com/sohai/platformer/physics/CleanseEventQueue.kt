package com.sohai.platformer.physics

import com.badlogic.gdx.math.Vector2
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe queue: WorldContactListener pushes a world-position whenever a
 * hazard is cleansed; GameScreen drains it each frame to spawn particle bursts.
 */
object CleanseEventQueue {
    private val queue = ConcurrentLinkedQueue<Vector2>()

    fun push(x: Float, y: Float) { queue.add(Vector2(x, y)) }

    /** Returns all pending events and clears the queue. */
    fun drain(): List<Vector2> {
        val out = mutableListOf<Vector2>()
        while (true) out.add(queue.poll() ?: break)
        return out
    }
}

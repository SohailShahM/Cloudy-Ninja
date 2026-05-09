package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for [ParticleSystem] pool eviction behaviour.
 *
 * `Color` is a plain data class (r, g, b, a floats) that requires no Gdx
 * initialisation, so ParticleSystem can be instantiated and driven entirely
 * in-process without a libGDX application context.  The `render()` method
 * (which needs an OpenGL ShapeRenderer) is intentionally excluded from this
 * suite — it is covered by visual smoke-testing during desktop play.
 */
class ParticleSystemTest : BehaviorSpec({

    val white = Color(1f, 1f, 1f, 1f)

    /** Spawn [n] particles into [sys] with a fixed lifetime of [life] seconds. */
    fun spawnN(sys: ParticleSystem, n: Int, life: Float = 1f) {
        repeat(n) { i ->
            sys.spawn(i.toFloat(), 0f, 0f, 0f, 1f, life, white)
        }
    }

    /** Count how many particles in the pool report alive = true via reflection. */
    fun aliveCount(sys: ParticleSystem): Int {
        // Access the private pool via reflection so the test doesn't require
        // adding a public accessor to the production class.
        val poolField = ParticleSystem::class.java.getDeclaredField("pool")
        poolField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val pool = poolField.get(sys) as Array<ParticleSystem.Particle>
        return pool.count { it.alive }
    }

    // -------------------------------------------------------------------------
    // 1. Spawning up to capacity — all succeed
    // -------------------------------------------------------------------------
    given("a ParticleSystem with capacity 10") {
        val capacity = 10
        val sys = ParticleSystem(maxParticles = capacity)

        `when`("${capacity} particles are spawned (exactly at capacity)") {
            spawnN(sys, capacity, life = 5f)

            then("all ${capacity} slots are alive") {
                aliveCount(sys) shouldBe capacity
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. Spawning beyond capacity — pool stays at max (oldest overwritten,
    //    no crash, no exception)
    // -------------------------------------------------------------------------
    given("a ParticleSystem with capacity 10 that is already full") {
        val capacity = 10
        val sys = ParticleSystem(maxParticles = capacity)
        spawnN(sys, capacity, life = 5f)

        `when`("5 more particles are spawned beyond the capacity") {
            // ParticleSystem overwrites the oldest slot when the pool is full
            // (ring-buffer eviction). There must be no exception and the alive
            // count must remain exactly at capacity.
            val extraSpawns = 5
            spawnN(sys, extraSpawns, life = 5f)

            then("alive count stays at capacity (${capacity}) — no crash, no overflow") {
                aliveCount(sys) shouldBe capacity
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. update(dt) past lifeMax marks particles dead
    // -------------------------------------------------------------------------
    given("a ParticleSystem with 3 particles having lifeMax = 0.5s") {
        val sys = ParticleSystem(maxParticles = 10)
        val lifeMax = 0.5f
        repeat(3) { i ->
            sys.spawn(i.toFloat(), 0f, 0f, 0f, 1f, lifeMax, white)
        }

        `when`("update is called with dt > lifeMax (dt = 0.6s)") {
            sys.update(0.6f)

            then("all 3 particles are marked dead (alive = false)") {
                aliveCount(sys) shouldBe 0
            }
        }
    }

    given("a ParticleSystem with 3 particles having lifeMax = 1.0s") {
        val sys = ParticleSystem(maxParticles = 10)
        repeat(3) { i ->
            sys.spawn(i.toFloat(), 0f, 0f, 0f, 1f, 1.0f, white)
        }

        `when`("update is called with dt < lifeMax (dt = 0.4s)") {
            sys.update(0.4f)

            then("all 3 particles remain alive") {
                aliveCount(sys) shouldBe 3
            }
        }

        `when`("update is called again so cumulative dt exceeds lifeMax (second dt = 0.7s)") {
            sys.update(0.7f)  // 0.4 + 0.7 = 1.1s > 1.0s

            then("all 3 particles are now dead") {
                aliveCount(sys) shouldBe 0
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. Dead slots are reused by the next spawn
    // -------------------------------------------------------------------------
    given("a ParticleSystem at capacity 5 where all particles have just died") {
        val capacity = 5
        val sys = ParticleSystem(maxParticles = capacity)

        // Fill the pool with short-lived particles.
        spawnN(sys, capacity, life = 0.1f)
        aliveCount(sys) shouldBe capacity   // sanity check

        // Age them past their lifespan.
        sys.update(0.2f)
        aliveCount(sys) shouldBe 0          // all dead

        `when`("a new particle is spawned into the now-empty pool") {
            sys.spawn(99f, 99f, 1f, 1f, 2f, 5f, white)

            then("alive count is 1 — the dead slot was reused") {
                aliveCount(sys) shouldBe 1
            }

            then("the reused slot has the new position (x = 99)") {
                val poolField = ParticleSystem::class.java.getDeclaredField("pool")
                poolField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val pool = poolField.get(sys) as Array<ParticleSystem.Particle>
                val respawned = pool.firstOrNull { it.alive }
                respawned shouldNotBe null
                respawned!!.x shouldBe 99f
                respawned.y shouldBe 99f
            }
        }
    }
})

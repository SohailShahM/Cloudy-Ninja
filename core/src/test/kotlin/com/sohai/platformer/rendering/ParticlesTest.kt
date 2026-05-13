package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

/**
 * Pure-math tests for [ParticleEmitter] + [Particle] (T-158).
 *
 * The utility imports only [com.badlogic.gdx.graphics.Color] — a plain
 * (r, g, b, a) value class — so the suite needs no Gdx context, no MockK
 * setup, no reflection. Every test exercises observable behaviour through
 * the public API.
 *
 * Coverage:
 *   1. Spawn count — emitter holds exactly N particles after spawn(N, ...).
 *   2. Spawn no-ops — non-positive count/ttl and empty palette spawn nothing.
 *   3. Palette cycling — particles cycle through the palette modulo its size.
 *   4. Palette decoupling — mutating the caller's palette after spawn does
 *      not bleed into already-spawned particles (defensive copy).
 *   5. Initial velocity layout — count=4 yields the cardinal-direction fan.
 *   6. Velocity decay under gravity — vy decreases by gravity*delta per tick.
 *   7. Position integration — x/y advance by v*delta per tick.
 *   8. TTL pruning — particles drop out the moment ttl <= 0.
 *   9. TTL partial decay — particles survive a tick smaller than their ttl.
 *  10. Multi-spawn — two consecutive bursts coexist in the same emitter.
 *  11. clear() drops every live particle.
 *  12. Negative gravity (e.g. "rising smoke") accelerates particles upward.
 *  13. Zero-delta update is a no-op (no spurious pruning, no position drift).
 */
class ParticlesTest : BehaviorSpec({

    val red   = Color(1f, 0f, 0f, 1f)
    val green = Color(0f, 1f, 0f, 1f)
    val blue  = Color(0f, 0f, 1f, 1f)

    // -------------------------------------------------------------------------
    // 1. Spawn count
    // -------------------------------------------------------------------------
    given("a fresh ParticleEmitter") {
        `when`("spawn(count=8, ttl=1f) is called with a single-colour palette") {
            then("particles list has exactly 8 entries") {
                val emitter = ParticleEmitter()
                emitter.spawn(x = 0f, y = 0f, count = 8, ttl = 1f, palette = listOf(red))
                emitter.particles shouldHaveSize 8
            }
            then("every particle carries the requested ttl") {
                val emitter = ParticleEmitter()
                emitter.spawn(x = 0f, y = 0f, count = 8, ttl = 0.75f, palette = listOf(red))
                emitter.particles.forEach { it.ttl shouldBe 0.75f }
            }
            then("every particle starts at the spawn origin") {
                val emitter = ParticleEmitter()
                emitter.spawn(x = 5f, y = 7f, count = 4, ttl = 1f, palette = listOf(red))
                emitter.particles.forEach {
                    it.x shouldBe 5f
                    it.y shouldBe 7f
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. Spawn no-ops
    // -------------------------------------------------------------------------
    given("an emitter with no live particles") {
        `when`("spawn() is called with count = 0") {
            then("no particles are added") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 0, ttl = 1f, palette = listOf(red))
                emitter.particles shouldHaveSize 0
            }
        }
        `when`("spawn() is called with negative count") {
            then("no particles are added") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = -3, ttl = 1f, palette = listOf(red))
                emitter.particles shouldHaveSize 0
            }
        }
        `when`("spawn() is called with ttl = 0") {
            then("no particles are added (a 0-ttl particle would be pruned next tick anyway)") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 0f, palette = listOf(red))
                emitter.particles shouldHaveSize 0
            }
        }
        `when`("spawn() is called with an empty palette") {
            then("no particles are added") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 1f, palette = emptyList())
                emitter.particles shouldHaveSize 0
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. Palette cycling
    // -------------------------------------------------------------------------
    given("a 3-colour palette and spawn(count=6)") {
        `when`("the 6 particles are inspected in spawn order") {
            then("colours cycle r, g, b, r, g, b (palette[i mod 3])") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 6, ttl = 1f, palette = listOf(red, green, blue))
                val colours = emitter.particles.map { it.color }
                // Compare component-wise — Color equality requires identical floats.
                colours[0].r shouldBe 1f; colours[0].g shouldBe 0f; colours[0].b shouldBe 0f
                colours[1].r shouldBe 0f; colours[1].g shouldBe 1f; colours[1].b shouldBe 0f
                colours[2].r shouldBe 0f; colours[2].g shouldBe 0f; colours[2].b shouldBe 1f
                colours[3].r shouldBe 1f; colours[3].g shouldBe 0f; colours[3].b shouldBe 0f
                colours[4].r shouldBe 0f; colours[4].g shouldBe 1f; colours[4].b shouldBe 0f
                colours[5].r shouldBe 0f; colours[5].g shouldBe 0f; colours[5].b shouldBe 1f
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. Palette decoupling — mutating after spawn doesn't leak in
    // -------------------------------------------------------------------------
    given("a palette whose colour entry is mutated after the spawn") {
        `when`("the caller mutates the original Color instance post-spawn") {
            then("the particle's colour keeps its pre-mutation values") {
                val emitter = ParticleEmitter()
                val mutableEntry = Color(0.25f, 0.5f, 0.75f, 1f)
                emitter.spawn(0f, 0f, count = 1, ttl = 1f, palette = listOf(mutableEntry))
                // Mutate the caller-owned entry after the fact.
                mutableEntry.set(0f, 0f, 0f, 0f)
                val stored = emitter.particles.single().color
                stored.r shouldBe 0.25f
                stored.g shouldBe 0.5f
                stored.b shouldBe 0.75f
                stored.a shouldBe 1f
            }
        }
    }

    // -------------------------------------------------------------------------
    // 5. Initial velocity layout for count = 4 (cardinal-direction fan)
    // -------------------------------------------------------------------------
    given("spawn(count=4) — angles 0, 90°, 180°, 270°") {
        `when`("the four velocities are inspected") {
            then("they point right, up, left, down at the burst speed") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 1f, palette = listOf(red))
                val s = ParticleEmitter.BURST_SPEED
                val ps = emitter.particles
                // i=0: angle=0 → ( s,  0)
                ps[0].vx shouldBe (s plusOrMinus 1e-5f)
                abs(ps[0].vy) shouldBe (0f plusOrMinus 1e-5f)
                // i=1: angle=pi/2 → ( 0,  s)
                abs(ps[1].vx) shouldBe (0f plusOrMinus 1e-5f)
                ps[1].vy shouldBe (s plusOrMinus 1e-5f)
                // i=2: angle=pi   → (-s,  0)
                ps[2].vx shouldBe (-s plusOrMinus 1e-5f)
                abs(ps[2].vy) shouldBe (0f plusOrMinus 1e-5f)
                // i=3: angle=3pi/2 → ( 0, -s)
                abs(ps[3].vx) shouldBe (0f plusOrMinus 1e-5f)
                ps[3].vy shouldBe (-s plusOrMinus 1e-5f)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 6. Velocity decay under gravity
    // -------------------------------------------------------------------------
    given("an emitter with one particle that has gravity = 10 m/s²") {
        `when`("update(delta = 0.1s) is called") {
            then("vy has decreased by 10 * 0.1 = 1.0 m/s") {
                val emitter = ParticleEmitter()
                // count=4 yields a known vy for i=1 (= BURST_SPEED).
                emitter.spawn(0f, 0f, count = 4, ttl = 1f, palette = listOf(red), gravity = 10f)
                val vy0 = emitter.particles[1].vy
                emitter.update(0.1f)
                val vy1 = emitter.particles[1].vy
                // Gravity convention: vy -= gravity * delta, so vy1 = vy0 - 1.
                (vy1 - (vy0 - 1f)) shouldBe (0f plusOrMinus 1e-5f)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 7. Position integration
    // -------------------------------------------------------------------------
    given("a particle moving right at BURST_SPEED with no gravity") {
        `when`("update(delta = 0.5s) is called") {
            then("its x position has advanced by BURST_SPEED * 0.5") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 5f, palette = listOf(red))
                // i=0 → vx = BURST_SPEED, vy = 0
                val p = emitter.particles[0]
                val x0 = p.x
                emitter.update(0.5f)
                val expected = x0 + ParticleEmitter.BURST_SPEED * 0.5f
                p.x shouldBe (expected plusOrMinus 1e-4f)
                abs(p.y) shouldBe (0f plusOrMinus 1e-5f)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 8. TTL pruning — particles drop out the moment ttl <= 0
    // -------------------------------------------------------------------------
    given("an emitter with 5 particles, ttl = 0.5s") {
        `when`("update(0.6s) is called (well past ttl)") {
            then("the particles list is empty (all pruned)") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 5, ttl = 0.5f, palette = listOf(red))
                emitter.particles shouldHaveSize 5
                emitter.update(0.6f)
                emitter.particles shouldHaveSize 0
            }
        }
        `when`("update(0.5s) is called (exactly at ttl)") {
            then("the particles are pruned (ttl reaches zero, pruning is <= 0)") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 5, ttl = 0.5f, palette = listOf(red))
                emitter.update(0.5f)
                emitter.particles shouldHaveSize 0
            }
        }
    }

    // -------------------------------------------------------------------------
    // 9. TTL partial decay — particles survive a tick smaller than their ttl
    // -------------------------------------------------------------------------
    given("an emitter with 3 particles, ttl = 1.0s") {
        `when`("update(0.3s) is called") {
            then("all 3 remain alive with ttl reduced to 0.7s") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 3, ttl = 1.0f, palette = listOf(red))
                emitter.update(0.3f)
                emitter.particles shouldHaveSize 3
                emitter.particles.forEach { it.ttl shouldBe (0.7f plusOrMinus 1e-5f) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 10. Multi-spawn — two consecutive bursts coexist in the same emitter
    // -------------------------------------------------------------------------
    given("an emitter that has had two bursts spawned into it") {
        `when`("spawn(count=4) then spawn(count=3) are called back-to-back") {
            then("particles list contains 7 entries in spawn order") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 1f, palette = listOf(red))
                emitter.spawn(10f, 10f, count = 3, ttl = 1f, palette = listOf(green))
                emitter.particles shouldHaveSize 7
                // Burst 1 — origin (0, 0).
                emitter.particles.take(4).forEach {
                    it.x shouldBe 0f
                    it.y shouldBe 0f
                    it.color.g shouldBe 0f  // red palette → g = 0
                }
                // Burst 2 — origin (10, 10).
                emitter.particles.drop(4).forEach {
                    it.x shouldBe 10f
                    it.y shouldBe 10f
                    it.color.g shouldBe 1f  // green palette → g = 1
                }
            }
        }
        `when`("burst 1 has short ttl and burst 2 has long ttl, and update past burst 1's ttl is called") {
            then("only the long-lived burst remains") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 0.2f, palette = listOf(red))
                emitter.spawn(10f, 10f, count = 3, ttl = 2f, palette = listOf(green))
                emitter.update(0.3f)
                emitter.particles shouldHaveSize 3
                emitter.particles.forEach { it.color.g shouldBe 1f }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 11. clear() drops every live particle
    // -------------------------------------------------------------------------
    given("an emitter with live particles") {
        `when`("clear() is called") {
            then("the particles list becomes empty") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 10, ttl = 5f, palette = listOf(red))
                emitter.particles shouldHaveSize 10
                emitter.clear()
                emitter.particles shouldHaveSize 0
            }
        }
    }

    // -------------------------------------------------------------------------
    // 12. Negative gravity (rising smoke)
    // -------------------------------------------------------------------------
    given("an emitter with one particle and gravity = -5 m/s² (rising)") {
        `when`("update(0.1s) is called") {
            then("vy has INCREASED by 5 * 0.1 = 0.5 m/s (negative gravity pulls up)") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 4, ttl = 1f, palette = listOf(red), gravity = -5f)
                val vy0 = emitter.particles[1].vy
                emitter.update(0.1f)
                val vy1 = emitter.particles[1].vy
                (vy1 - (vy0 + 0.5f)) shouldBe (0f plusOrMinus 1e-5f)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 13. Zero-delta update is a no-op
    // -------------------------------------------------------------------------
    given("an emitter with 5 particles") {
        `when`("update(0f) is called") {
            then("ttls are unchanged and positions are unchanged") {
                val emitter = ParticleEmitter()
                emitter.spawn(3f, 4f, count = 5, ttl = 1f, palette = listOf(red))
                val ttlsBefore = emitter.particles.map { it.ttl }
                val xsBefore   = emitter.particles.map { it.x }
                val ysBefore   = emitter.particles.map { it.y }
                emitter.update(0f)
                emitter.particles shouldHaveSize 5
                emitter.particles.map { it.ttl } shouldBe ttlsBefore
                emitter.particles.map { it.x }   shouldBe xsBefore
                emitter.particles.map { it.y }   shouldBe ysBefore
            }
            then("the particles list reference returned by the accessor is still readable") {
                val emitter = ParticleEmitter()
                emitter.spawn(0f, 0f, count = 3, ttl = 1f, palette = listOf(red))
                val view = emitter.particles
                emitter.update(0f)
                // Read after update — must not throw, must reflect current state.
                (view.size == 3).shouldBeTrue()
            }
        }
    }
})

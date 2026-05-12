package com.sohai.platformer.entities

import com.badlogic.gdx.physics.box2d.Body
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.objenesis.ObjenesisStd

/**
 * Pure-logic tests for [Projectile].
 *
 * Projectile's primary constructor calls into Box2D's `CircleShape()` (which
 * triggers a `Shape.<clinit>` that loads `libgdx-box2d.dll` — not available
 * in unit-test JVMs; see PlayerControllerMovementTest for the same constraint).
 * To exercise the age / expiry / hitWall logic without natives we bypass the
 * constructor entirely with **Objenesis** (already on the test classpath as
 * a transitive of MockK) and inject the private fields via reflection.
 *
 * This means rendering, fixture filter wiring, and the Box2D body setup are
 * NOT covered here — those are exercised via the desktop run + `lwjgl3:test`.
 * The portion under test is the time-based expiry contract:
 *
 *   isExpired ≡ (age ≥ lifetime) ∨ hitWall
 *
 * which is what `LevelRunState` consults to queue body destruction.
 */
class ProjectileTest : BehaviorSpec({

    val eps = 0.0001f
    val objenesis = ObjenesisStd()

    /**
     * Build a Projectile bypassing its native-loading constructor.
     * The `body` field is set to a relaxed MockK Body — production code that
     * touches `projectile.body` from tests will go through the mock.
     */
    fun newProjectile(
        vx: Float = 1f,
        vy: Float = 0f,
        lifetime: Float = 3f,
    ): Projectile {
        val p = objenesis.newInstance(Projectile::class.java)

        fun setField(name: String, value: Any?) {
            val f = Projectile::class.java.getDeclaredField(name)
            f.isAccessible = true
            f.set(p, value)
        }

        setField("body", mockk<Body>(relaxed = true))
        setField("vx", vx)
        setField("vy", vy)
        setField("lifetime", lifetime)
        setField("age", 0f)
        setField("hitWall", false)
        return p
    }

    // ── construction / initial state ─────────────────────────────────────────

    given("a freshly constructed Projectile (lifetime = 3s)") {
        val p = newProjectile(vx = 5f, vy = -2f, lifetime = 3f)

        `when`("inspecting initial state") {
            then("age is 0") { p.age shouldBe (0f plusOrMinus eps) }
            then("hitWall is false") { p.hitWall shouldBe false }
            then("isExpired is false") { p.isExpired shouldBe false }
            then("velocity components are preserved on the instance") {
                p.vx shouldBe (5f plusOrMinus eps)
                p.vy shouldBe (-2f plusOrMinus eps)
            }
        }
    }

    // ── lifetime / age ───────────────────────────────────────────────────────

    given("a Projectile with lifetime = 1s") {

        `when`("update is called with dt < lifetime") {
            val p = newProjectile(lifetime = 1f)
            p.update(0.4f)

            then("age advances by dt") { p.age shouldBe (0.4f plusOrMinus eps) }
            then("isExpired is still false") { p.isExpired shouldBe false }
        }

        `when`("update accumulates past the lifetime") {
            val p = newProjectile(lifetime = 1f)
            p.update(0.4f)
            p.update(0.4f)
            p.update(0.4f) // total = 1.2 > 1

            then("age is the sum of all dt values") {
                p.age shouldBe (1.2f plusOrMinus eps)
            }
            then("isExpired flips to true") { p.isExpired shouldBe true }
        }

        `when`("update is called with dt exactly equal to lifetime") {
            val p = newProjectile(lifetime = 1f)
            p.update(1f)

            then("isExpired is true (lifetime boundary is inclusive: age ≥ lifetime)") {
                p.isExpired shouldBe true
            }
        }

        `when`("many tiny updates accumulate past the lifetime") {
            val p = newProjectile(lifetime = 1f)
            // 110 × 0.01 = ~1.1f — safely above lifetime even with float drift.
            repeat(110) { p.update(0.01f) }

            then("isExpired is true once the lifetime boundary is crossed") {
                p.isExpired shouldBe true
            }
        }
    }

    // ── wall-hit shortcut ────────────────────────────────────────────────────

    given("a young Projectile (age << lifetime)") {

        `when`("hitWall is flipped to true (simulating a contact-listener event)") {
            val p = newProjectile(lifetime = 5f)
            p.update(0.1f) // age = 0.1, well below lifetime
            p.isExpired shouldBe false

            p.hitWall = true

            then("isExpired flips to true immediately, regardless of age") {
                p.isExpired shouldBe true
            }
        }

        `when`("hitWall is left false but age stays below lifetime") {
            val p = newProjectile(lifetime = 5f)
            p.update(0.1f)

            then("isExpired remains false") {
                p.isExpired shouldBe false
            }
        }
    }

    // ── independence between projectiles ─────────────────────────────────────

    given("two Projectiles updated independently") {
        val a = newProjectile(lifetime = 2f)
        val b = newProjectile(lifetime = 2f)

        `when`("only A is advanced") {
            a.update(0.5f)

            then("A's age moves but B's age stays at 0") {
                a.age shouldBe (0.5f plusOrMinus eps)
                b.age shouldBe (0f plusOrMinus eps)
            }
            then("B.isExpired is still false") { b.isExpired shouldBe false }
        }

        `when`("B's hitWall is flipped") {
            b.hitWall = true

            then("B is expired but A is not") {
                b.isExpired shouldBe true
                a.isExpired shouldBe false
            }
        }
    }

    // ── constants ────────────────────────────────────────────────────────────

    given("the Projectile.RADIUS constant") {
        `when`("compared to the spec value (4 px / PPM)") {
            then("matches 4 / 100 = 0.04 metres") {
                Projectile.RADIUS shouldBe (0.04f plusOrMinus eps)
            }
        }
    }
})

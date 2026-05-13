package com.sohai.platformer.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.sohai.platformer.util.GameRandom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.objenesis.ObjenesisStd

/**
 * T-170: tests for [Enemy.drawHighContrast] / [StormSentinel.drawHighContrast]
 * silhouette rendering.
 *
 * Mirrors the reflection + mocked-Body pattern used in [SmogSpriteTest],
 * [DriftHuskTest], [EnemyHitFlashTest], and [StormSentinelTest] so no libgdx
 * Box2D natives or OpenGL context are required.
 *
 * Each test confirms:
 *   1. The method does not crash (smoke-level coverage).
 *   2. The renderer.color and a single rect/circle call are issued at the
 *      entity's body position.
 *   3. The hit-flash lerp composes through the new draw path (positive
 *      side-effect of T-170 — hit-flash now visible in high-contrast mode).
 */
class HighContrastSilhouetteTest : BehaviorSpec({

    val eps = 0.0001f

    fun newSprite(body: Body): SmogSprite {
        val ctor = SmogSprite::class.java.getDeclaredConstructor(
            Body::class.java,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
        )
        ctor.isAccessible = true
        return ctor.newInstance(body, 0f, 4f, 2f)
    }

    fun newHusk(body: Body): DriftHusk {
        val ctor = DriftHusk::class.java.getDeclaredConstructor(
            Body::class.java,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
        )
        ctor.isAccessible = true
        return ctor.newInstance(body, 5f, 6f, 5f)
    }

    fun mockBodyAt(x: Float, y: Float = 0f): Body {
        val body = mockk<Body>(relaxed = true)
        every { body.position } returns Vector2(x, y)
        return body
    }

    /** Capture the colour passed to renderer.color across multiple writes. */
    fun mockRenderer(): Pair<ShapeRenderer, () -> Color?> {
        val sr = mockk<ShapeRenderer>(relaxed = true)
        var current: Color? = null
        every { sr.color = any() } answers { current = Color(firstArg<Color>()) }
        return sr to { current }
    }

    // ── SmogSprite ───────────────────────────────────────────────────────────

    given("a fresh SmogSprite at (2, 1) in high-contrast mode") {
        `when`("drawHighContrast is called with pure black") {
            val sprite = newSprite(mockBodyAt(2f, 1f))
            val (sr, lastColor) = mockRenderer()
            sprite.drawHighContrast(sr, Color.BLACK)

            then("no crash; a rect is drawn at the body position with the 0.18x0.16 half-extents") {
                verify(exactly = 1) {
                    sr.rect(
                        match { v -> kotlin.math.abs(v - (2f - 0.18f)) < eps },
                        match { v -> kotlin.math.abs(v - (1f - 0.16f)) < eps },
                        match { v -> kotlin.math.abs(v - 0.36f) < eps },
                        match { v -> kotlin.math.abs(v - 0.32f) < eps },
                    )
                }
            }
            then("the renderer colour is the base black (no hit-flash → no lerp)") {
                val c = lastColor() ?: error("colour not set")
                c.r shouldBe (0f plusOrMinus eps)
                c.g shouldBe (0f plusOrMinus eps)
                c.b shouldBe (0f plusOrMinus eps)
            }
        }
    }

    given("a SmogSprite that is mid hit-flash (timer = HIT_FLASH_SECONDS)") {
        `when`("drawHighContrast is called with pure black") {
            val sprite = newSprite(mockBodyAt(0f, 0f))
            sprite.takeDamage(1) // arms timer to HIT_FLASH_SECONDS
            val (sr, lastColor) = mockRenderer()
            sprite.drawHighContrast(sr, Color.BLACK)

            then("the silhouette is fully lerped toward white (positive side-effect: hit-flash now visible)") {
                val c = lastColor() ?: error("colour not set")
                c.r shouldBe (1f plusOrMinus eps)
                c.g shouldBe (1f plusOrMinus eps)
                c.b shouldBe (1f plusOrMinus eps)
            }
        }
    }

    given("a dead SmogSprite") {
        `when`("drawHighContrast is called") {
            val sprite = newSprite(mockBodyAt(0f, 0f))
            sprite.takeDamage(99) // kill outright
            val (sr, _) = mockRenderer()
            sprite.drawHighContrast(sr, Color.BLACK)

            then("no rect is issued (isDead short-circuits)") {
                verify(exactly = 0) { sr.rect(any(), any(), any(), any()) }
            }
        }
    }

    // ── DriftHusk ────────────────────────────────────────────────────────────

    given("a fresh DriftHusk in high-contrast mode") {
        `when`("drawHighContrast is called") {
            val husk = newHusk(mockBodyAt(5f, 6f))
            val (sr, _) = mockRenderer()
            husk.drawHighContrast(sr, Color.BLACK)

            then("no crash; a single rect is issued") {
                verify(exactly = 1) { sr.rect(any(), any(), any(), any()) }
            }
        }
    }

    given("a dead DriftHusk") {
        `when`("drawHighContrast is called") {
            val husk = newHusk(mockBodyAt(5f, 6f))
            husk.takeDamage(99)
            val (sr, _) = mockRenderer()
            husk.drawHighContrast(sr, Color.BLACK)

            then("no rect is issued") {
                verify(exactly = 0) { sr.rect(any(), any(), any(), any()) }
            }
        }
    }

    // ── StormSentinel ────────────────────────────────────────────────────────

    val objenesis = ObjenesisStd()

    fun setField(target: Any, name: String, value: Any?) {
        val f = StormSentinel::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    fun newSentinel(): StormSentinel {
        GameRandom.setSeed(1234L)
        val boss = objenesis.newInstance(StormSentinel::class.java)
        setField(boss, "x", 3f)
        setField(boss, "y", 4f)
        setField(boss, "arenaLeft", 0f)
        setField(boss, "arenaRight", 10f)
        setField(boss, "hp", 3)
        setField(boss, "isDead", false)
        setField(boss, "phase", StormSentinel.Phase.REST)
        setField(boss, "phaseTimer", StormSentinel.REST_DURATION)
        setField(boss, "attackIndex", 0)
        setField(boss, "hitFlashTimer", 0f)
        setField(boss, "sweepGoesRight", true)
        setField(boss, "_lightningWarnings", mutableListOf<Float>())
        setField(boss, "sweepWarningX", 0f)
        setField(boss, "sweepWarningDir", 1)
        setField(boss, "hcTmp", Color())
        return boss
    }

    given("a fresh StormSentinel in high-contrast mode") {
        `when`("drawHighContrast is called with pure black") {
            val boss = newSentinel()
            val (sr, lastColor) = mockRenderer()
            boss.drawHighContrast(sr, Color.BLACK)

            then("a single 0.5m circle is issued at the boss position (BODY_RADIUS=0.45 fully covered)") {
                verify(exactly = 1) {
                    sr.circle(
                        match { v -> kotlin.math.abs(v - 3f) < eps },
                        match { v -> kotlin.math.abs(v - 4f) < eps },
                        match { v -> kotlin.math.abs(v - 0.5f) < eps },
                    )
                }
            }
            then("the colour is the base black (no hit-flash → no lerp)") {
                val c = lastColor() ?: error("colour not set")
                c.r shouldBe (0f plusOrMinus eps)
                c.g shouldBe (0f plusOrMinus eps)
                c.b shouldBe (0f plusOrMinus eps)
            }
        }
    }

    given("a StormSentinel mid hit-flash") {
        `when`("drawHighContrast is called with pure black") {
            val boss = newSentinel()
            setField(boss, "hitFlashTimer", StormSentinel.HIT_FLASH_DURATION)
            val (sr, lastColor) = mockRenderer()
            boss.drawHighContrast(sr, Color.BLACK)

            then("the silhouette colour is fully lerped toward white (hit-flash now visible in HC)") {
                val c = lastColor() ?: error("colour not set")
                c.r shouldBe (1f plusOrMinus eps)
                c.g shouldBe (1f plusOrMinus eps)
                c.b shouldBe (1f plusOrMinus eps)
            }
        }
    }

    given("a dead StormSentinel") {
        `when`("drawHighContrast is called") {
            val boss = newSentinel()
            setField(boss, "isDead", true)
            val (sr, _) = mockRenderer()
            boss.drawHighContrast(sr, Color.BLACK)

            then("no circle is issued") {
                verify(exactly = 0) { sr.circle(any(), any(), any()) }
            }
        }
    }
})

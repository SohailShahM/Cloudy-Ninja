package com.sohai.platformer.entities

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * T-098: tests for the on-hit white-flash timer on [Enemy] subclasses.
 *
 * Exercises the timer set/decrement/clamp behaviour and the defeat-path
 * exclusion (the killing blow does NOT arm the flash; that frame belongs to
 * the defeat VFX). Uses the same reflection + mocked-Body pattern as
 * [SmogSpriteTest] and [DriftHuskTest] so no libgdx Box2D natives are needed.
 */
class EnemyHitFlashTest : BehaviorSpec({

    val eps = 0.0001f

    fun newSprite(
        body: Body,
        leftX: Float = 0f,
        rightX: Float = 4f,
        speed: Float = 2f,
    ): SmogSprite {
        val ctor = SmogSprite::class.java.getDeclaredConstructor(
            Body::class.java,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
        )
        ctor.isAccessible = true
        return ctor.newInstance(body, leftX, rightX, speed)
    }

    fun newHusk(
        body: Body,
        originX: Float = 5f,
        originY: Float = 6f,
        triggerX: Float = 5f,
    ): DriftHusk {
        val ctor = DriftHusk::class.java.getDeclaredConstructor(
            Body::class.java,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
            java.lang.Float.TYPE,
        )
        ctor.isAccessible = true
        return ctor.newInstance(body, originX, originY, triggerX)
    }

    /** Mocked Body with a mutable position and a sink for linearVelocity writes. */
    fun mockBodyAt(x: Float, y: Float = 0f): Body {
        val pos = Vector2(x, y)
        val body = mockk<Body>(relaxed = true)
        every { body.position } returns pos
        return body
    }

    // ── HIT_FLASH_SECONDS contract ───────────────────────────────────────────

    given("the Enemy.HIT_FLASH_SECONDS constant") {
        then("matches the ticket spec (200ms)") {
            Enemy.HIT_FLASH_SECONDS shouldBe (0.2f plusOrMinus eps)
        }
    }

    // ── initial state ────────────────────────────────────────────────────────

    given("a fresh SmogSprite") {
        val sprite = newSprite(mockBodyAt(0f))
        then("hitFlashTimer starts at 0") {
            sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
        }
    }

    given("a fresh DriftHusk") {
        val husk = newHusk(mockBodyAt(5f, 6f))
        then("hitFlashTimer starts at 0") {
            husk.hitFlashTimer shouldBe (0f plusOrMinus eps)
        }
    }

    // ── set on damage (survived hit) ─────────────────────────────────────────

    given("a SmogSprite (HP=2) that takes a non-killing hit") {
        val sprite = newSprite(mockBodyAt(0f))
        sprite.takeDamage(1)
        then("hitFlashTimer is armed to HIT_FLASH_SECONDS") {
            sprite.hitFlashTimer shouldBe (Enemy.HIT_FLASH_SECONDS plusOrMinus eps)
        }
        then("still alive (defeat path NOT taken)") { sprite.isDead shouldBe false }
    }

    given("a DriftHusk (HP=2) that takes a non-killing hit") {
        val husk = newHusk(mockBodyAt(5f, 6f))
        husk.takeDamage(1)
        then("hitFlashTimer is armed to HIT_FLASH_SECONDS") {
            husk.hitFlashTimer shouldBe (Enemy.HIT_FLASH_SECONDS plusOrMinus eps)
        }
        then("still alive (defeat path NOT taken)") { husk.isDead shouldBe false }
    }

    // ── decrement via update(delta) ──────────────────────────────────────────

    given("a SmogSprite armed with a hit-flash") {
        `when`("update(1/60s) is called once") {
            val sprite = newSprite(mockBodyAt(2f))
            sprite.takeDamage(1)
            val before = sprite.hitFlashTimer
            sprite.update(1f / 60f)
            then("hitFlashTimer drops by exactly delta") {
                sprite.hitFlashTimer shouldBe ((before - 1f / 60f) plusOrMinus eps)
            }
        }
    }

    given("a DriftHusk armed with a hit-flash") {
        `when`("update(1/60s) is called once") {
            val husk = newHusk(mockBodyAt(5f, 6f))
            husk.takeDamage(1)
            val before = husk.hitFlashTimer
            husk.update(1f / 60f)
            then("hitFlashTimer drops by exactly delta") {
                husk.hitFlashTimer shouldBe ((before - 1f / 60f) plusOrMinus eps)
            }
        }
    }

    // ── clamp to >= 0, even on a giant delta ─────────────────────────────────

    given("a SmogSprite armed with a hit-flash") {
        `when`("a huge delta (1s) overshoots the timer") {
            val sprite = newSprite(mockBodyAt(2f))
            sprite.takeDamage(1)
            sprite.update(1f)
            then("hitFlashTimer is clamped to 0 (never negative)") {
                sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
            }
        }

        `when`("multiple updates run after the flash has already decayed") {
            val sprite = newSprite(mockBodyAt(2f))
            sprite.takeDamage(1)
            sprite.update(1f)              // burns down to 0
            sprite.update(1f / 60f)
            sprite.update(1f / 60f)
            then("hitFlashTimer stays at 0 (does not go negative)") {
                sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
            }
        }
    }

    given("a DriftHusk armed with a hit-flash") {
        `when`("a huge delta (1s) overshoots the timer") {
            val husk = newHusk(mockBodyAt(5f, 6f))
            husk.takeDamage(1)
            husk.update(1f)
            then("hitFlashTimer is clamped to 0 (never negative)") {
                husk.hitFlashTimer shouldBe (0f plusOrMinus eps)
            }
        }
    }

    // ── decay over exactly HIT_FLASH_SECONDS lands at 0 ──────────────────────

    given("a SmogSprite armed with a hit-flash") {
        `when`("update is called with delta == HIT_FLASH_SECONDS") {
            val sprite = newSprite(mockBodyAt(2f))
            sprite.takeDamage(1)
            sprite.update(Enemy.HIT_FLASH_SECONDS)
            then("hitFlashTimer is exactly 0") {
                sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
            }
        }
    }

    // ── defeat path: killing blow does NOT arm the flash ─────────────────────

    given("a SmogSprite (HP=2) that takes a one-shot lethal hit") {
        val sprite = newSprite(mockBodyAt(0f))
        sprite.takeDamage(5) // overkill
        then("isDead is true") { sprite.isDead shouldBe true }
        then("hitFlashTimer is NOT armed (defeat path owns this frame)") {
            sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
        }
    }

    given("a SmogSprite (HP=2) that takes two non-killing hits then the killing blow") {
        val sprite = newSprite(mockBodyAt(0f))
        sprite.takeDamage(1)                  // arm flash
        sprite.hitFlashTimer shouldBe (Enemy.HIT_FLASH_SECONDS plusOrMinus eps)
        sprite.update(Enemy.HIT_FLASH_SECONDS) // burn it down
        sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
        sprite.takeDamage(1)                  // killing blow
        then("isDead is true") { sprite.isDead shouldBe true }
        then("hitFlashTimer stays 0 (killing blow does NOT re-arm)") {
            sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
        }
    }

    given("a DriftHusk (HP=2) that takes a one-shot lethal hit") {
        val husk = newHusk(mockBodyAt(5f, 6f))
        husk.takeDamage(99)
        then("isDead is true") { husk.isDead shouldBe true }
        then("hitFlashTimer is NOT armed (defeat path owns this frame)") {
            husk.hitFlashTimer shouldBe (0f plusOrMinus eps)
        }
    }

    // ── unchanged-when-not-hit invariant ─────────────────────────────────────

    given("a SmogSprite that never takes damage") {
        `when`("update runs many times") {
            val sprite = newSprite(mockBodyAt(2f))
            repeat(10) { sprite.update(1f / 60f) }
            then("hitFlashTimer stays at 0") {
                sprite.hitFlashTimer shouldBe (0f plusOrMinus eps)
            }
        }
    }

    given("a DriftHusk that never takes damage") {
        `when`("update runs many times in FLOATING state") {
            val husk = newHusk(mockBodyAt(5f, 6f))
            repeat(10) { husk.update(1f / 60f) }
            then("hitFlashTimer stays at 0") {
                husk.hitFlashTimer shouldBe (0f plusOrMinus eps)
            }
        }
    }

    // ── re-arming: a second non-killing hit refreshes the timer ──────────────

    given("a SmogSprite whose flash has partially decayed") {
        val sprite = newSprite(mockBodyAt(0f))
        sprite.hp = 5 // tweak HP so we can take multiple non-killing hits
        sprite.takeDamage(1)
        sprite.update(0.1f) // burn half the flash
        sprite.hitFlashTimer shouldBe (0.1f plusOrMinus eps)

        `when`("a second non-killing hit lands") {
            sprite.takeDamage(1)
            then("hitFlashTimer is reset to HIT_FLASH_SECONDS") {
                sprite.hitFlashTimer shouldBe (Enemy.HIT_FLASH_SECONDS plusOrMinus eps)
            }
        }
    }
})

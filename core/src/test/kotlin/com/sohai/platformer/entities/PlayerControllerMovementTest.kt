package com.sohai.platformer.entities

import com.sohai.platformer.Constants
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Test suite for PlayerController movement mechanics.
 *
 * **Box2D-backed cases (skipped):**
 * - Left/right movement velocity setting
 * - Facing direction tracking
 * - Direction changes mid-air
 *
 * These need Box2D natives (libgdx-box2d) which aren't available in the
 * core unit-test classpath. To run with natives: `./gradlew lwjgl3:test`.
 *
 * **Pure-math cases (active):**
 * The T-175 coast-damping baseline is governed by two scalar multipliers
 * applied each frame when no horizontal input is held — Constants.GROUND_COAST_DAMPING
 * (grounded) and Constants.AIR_COAST_DAMPING (airborne). We can verify the
 * tuning goal — "stop within ~5 frames at 60Hz from top speed" — without
 * spinning up a Box2D world.
 */
class PlayerControllerMovementTest : BehaviorSpec({
    xgiven("a PlayerController with movement input") {
        // Tests skipped - requires Box2D natives (libgdx-box2d.so not available in unit test env)
    }

    given("T-175 ground-coast damping tuning") {
        `when`("velocity starts at PLAYER_SPEED and no input is held") {
            then("velocity drops below 0.5 m/s within 5 frames at 60Hz") {
                var v = Constants.PLAYER_SPEED.toDouble()
                for (frame in 1..5) {
                    v *= Constants.GROUND_COAST_DAMPING
                }
                // After 5 frames, |v| must be < 0.5 m/s — i.e. the player has
                // effectively come to rest by the time the player can perceive
                // it (~83ms).
                kotlin.math.abs(v) shouldBeLessThan 0.5
            }
            then("velocity is still positive on frame 1 (no instant snap)") {
                // We don't want a robotic instant-kill — the dampener should be
                // a smooth bleed, not a hard zero. Confirms GROUND_COAST_DAMPING
                // is well above 0 (i.e. some coast remains, just briefly).
                val v1 = Constants.PLAYER_SPEED.toDouble() * Constants.GROUND_COAST_DAMPING
                (v1 > 1.0) shouldBe true
            }
        }
    }

    given("T-175 air-coast damping tuning") {
        `when`("airborne velocity starts at PLAYER_SPEED and no input is held") {
            then("velocity drops below 0.5 m/s within 5 frames at 60Hz") {
                var v = Constants.PLAYER_SPEED.toDouble()
                for (frame in 1..5) {
                    v *= Constants.AIR_COAST_DAMPING
                }
                kotlin.math.abs(v) shouldBeLessThan 0.5
            }
        }
    }
})

package com.sohai.platformer.entities

import com.sohai.platformer.rendering.SheetAnimState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * T-186: unit tests for [PlayerController.computeAnimState] — the pure
 * state-resolution helper that maps the player's physics + status into the
 * [SheetAnimState] played by the sprite-sheet renderer.
 *
 * These run as plain JVM tests (no libgdx, no Box2D natives) because the
 * helper takes primitive inputs — see the kdoc on `computeAnimState` for the
 * rationale.
 */
class PlayerControllerStateTest : BehaviorSpec({

    fun state(
        isDead: Boolean = false,
        isFlashing: Boolean = false,
        isAbilityAttackActive: Boolean = false,
        isGrounded: Boolean = true,
        velocityX: Float = 0f,
        velocityY: Float = 0f,
    ): SheetAnimState = PlayerController.computeAnimState(
        isDead = isDead,
        isFlashing = isFlashing,
        isAbilityAttackActive = isAbilityAttackActive,
        isGrounded = isGrounded,
        velocityX = velocityX,
        velocityY = velocityY,
    )

    given("a grounded player with no input") {
        `when`("velocity is zero") {
            then("returns IDLE") {
                state(isGrounded = true, velocityX = 0f, velocityY = 0f) shouldBe SheetAnimState.IDLE
            }
        }
        `when`("horizontal speed is below the RUN threshold") {
            then("still returns IDLE") {
                // RUN_VX_THRESHOLD = 0.5f
                state(isGrounded = true, velocityX = 0.3f) shouldBe SheetAnimState.IDLE
                state(isGrounded = true, velocityX = -0.4f) shouldBe SheetAnimState.IDLE
            }
        }
    }

    given("a grounded player moving horizontally") {
        `when`("vx exceeds the RUN threshold (rightward)") {
            then("returns RUN") {
                state(isGrounded = true, velocityX = 2f) shouldBe SheetAnimState.RUN
            }
        }
        `when`("vx exceeds the RUN threshold (leftward)") {
            then("returns RUN regardless of sign") {
                state(isGrounded = true, velocityX = -2f) shouldBe SheetAnimState.RUN
            }
        }
    }

    given("an airborne player") {
        `when`("vy is well above the JUMP threshold") {
            then("returns JUMP") {
                state(isGrounded = false, velocityY = 5f) shouldBe SheetAnimState.JUMP
            }
        }
        `when`("vy is negative (descending)") {
            then("returns FALL") {
                state(isGrounded = false, velocityY = -3f) shouldBe SheetAnimState.FALL
            }
        }
        `when`("vy is near zero (apex hang)") {
            then("returns FALL (no separate apex state)") {
                state(isGrounded = false, velocityY = 0f) shouldBe SheetAnimState.FALL
            }
        }
        `when`("the player is airborne AND moving horizontally") {
            then("FALL/JUMP wins over RUN (airborne takes priority)") {
                state(isGrounded = false, velocityX = 4f, velocityY = -1f) shouldBe SheetAnimState.FALL
                state(isGrounded = false, velocityX = 4f, velocityY = 3f) shouldBe SheetAnimState.JUMP
            }
        }
    }

    given("the ability is active (Seed Slam for Ebo)") {
        `when`("the player is grounded and idle") {
            then("returns ATTACK1 (ability overrides IDLE)") {
                state(isAbilityAttackActive = true) shouldBe SheetAnimState.ATTACK1
            }
        }
        `when`("the player is also moving horizontally") {
            then("ATTACK1 overrides RUN") {
                state(isAbilityAttackActive = true, velocityX = 3f) shouldBe SheetAnimState.ATTACK1
            }
        }
        `when`("the player is also airborne and jumping") {
            then("ATTACK1 overrides JUMP") {
                state(
                    isAbilityAttackActive = true,
                    isGrounded = false,
                    velocityY = 5f,
                ) shouldBe SheetAnimState.ATTACK1
            }
        }
    }

    given("the player is taking a hit (flashing)") {
        `when`("the player is also running") {
            then("TAKE_HIT overrides RUN") {
                state(isFlashing = true, velocityX = 4f) shouldBe SheetAnimState.TAKE_HIT
            }
        }
        `when`("an ability is active at the same time") {
            then("TAKE_HIT overrides ATTACK1") {
                state(isFlashing = true, isAbilityAttackActive = true) shouldBe SheetAnimState.TAKE_HIT
            }
        }
    }

    given("the player is dying") {
        `when`("any other state would otherwise apply") {
            then("DEATH wins — it's the top of the priority ladder") {
                state(
                    isDead = true,
                    isFlashing = true,
                    isAbilityAttackActive = true,
                    isGrounded = false,
                    velocityX = 5f,
                    velocityY = -4f,
                ) shouldBe SheetAnimState.DEATH
            }
        }
    }

    given("transition-priority spot checks") {
        then("DEATH > TAKE_HIT") {
            state(isDead = true, isFlashing = true) shouldBe SheetAnimState.DEATH
        }
        then("TAKE_HIT > ATTACK1") {
            state(isFlashing = true, isAbilityAttackActive = true) shouldBe SheetAnimState.TAKE_HIT
        }
        then("ATTACK1 > JUMP/FALL/RUN/IDLE") {
            state(
                isAbilityAttackActive = true,
                isGrounded = false,
                velocityY = 5f,
            ) shouldBe SheetAnimState.ATTACK1
        }
        then("JUMP > FALL when vy is positive") {
            state(isGrounded = false, velocityY = 1.5f) shouldBe SheetAnimState.JUMP
        }
        then("FALL > RUN when airborne, even if vx is large") {
            state(isGrounded = false, velocityX = 5f, velocityY = -2f) shouldBe SheetAnimState.FALL
        }
        then("RUN > IDLE when grounded and moving") {
            state(velocityX = 3f) shouldBe SheetAnimState.RUN
        }
    }
})

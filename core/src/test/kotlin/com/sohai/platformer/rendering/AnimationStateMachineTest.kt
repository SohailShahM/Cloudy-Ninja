package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

/**
 * Tests for the T-180 [AnimationStateMachine] scaffold.
 *
 * Builds tiny synthetic [SheetCharacterAtlas] instances directly via the constructor —
 * no factory, no PNGs. Each frame is a distinct mock [TextureRegion] so the test can
 * assert by identity which frame `currentFrame(delta)` returns.
 */
class AnimationStateMachineTest : BehaviorSpec({

    /** Build a strip of `n` distinct mock regions. */
    fun strip(n: Int): Array<TextureRegion> {
        val a = Array<TextureRegion>(n)
        repeat(n) { a.add(mockk(relaxed = true)) }
        return a
    }

    /** Atlas with deterministic frame counts mirroring MH1 (no attack3). */
    fun mh1LikeAtlas() = SheetCharacterAtlas(
        idle    = strip(8),
        run     = strip(8),
        jump    = strip(2),
        fall    = strip(2),
        attack1 = strip(6),
        attack2 = strip(6),
        attack3 = null,
        takeHit = strip(4),
        death   = strip(6),
    )

    /** Atlas with MH3 counts (attack3 present, jump = 3 frames). */
    fun mh3LikeAtlas() = SheetCharacterAtlas(
        idle    = strip(10),
        run     = strip(8),
        jump    = strip(3),
        fall    = strip(3),
        attack1 = strip(7),
        attack2 = strip(6),
        attack3 = strip(9),
        takeHit = strip(3),
        death   = strip(11),
    )

    given("a fresh AnimationStateMachine") {
        val atlas = mh1LikeAtlas()
        val sm = AnimationStateMachine(atlas)

        `when`("inspecting initial state") {
            then("currentState is IDLE") {
                sm.currentState shouldBe SheetAnimState.IDLE
            }
            then("elapsedInState is 0") {
                sm.elapsedInState shouldBe 0f
            }
            then("currentFrame(0) returns idle[0]") {
                (sm.currentFrame(0f) === atlas.idle[0]) shouldBe true
            }
        }
    }

    given("IDLE state at 6 FPS (default) advancing 0.5 s") {
        val atlas = mh1LikeAtlas()
        val sm = AnimationStateMachine(atlas)

        `when`("currentFrame(0.5f) is called") {
            val frame = sm.currentFrame(0.5f)
            then("the frame index is floor(0.5 * 6) = 3, i.e. idle[3]") {
                (frame === atlas.idle[3]) shouldBe true
            }
        }
    }

    given("IDLE state and elapsed time exceeding frames.size / fps") {
        val atlas = mh1LikeAtlas()
        val sm = AnimationStateMachine(atlas)

        `when`("a delta of 2.0 s is supplied (2.0 * 6 = 12 >= 8 frames)") {
            val frame = sm.currentFrame(2f)
            then("the index wraps modulo 8 → 12 % 8 = 4 → idle[4]") {
                (frame === atlas.idle[4]) shouldBe true
            }
        }

        `when`("a further delta brings total elapsed to 4.0 s (24 raw idx, 24 % 8 = 0)") {
            val frame2 = sm.currentFrame(2f)
            then("the index wraps back to idle[0]") {
                (frame2 === atlas.idle[0]) shouldBe true
            }
        }
    }

    given("state transition") {
        val atlas = mh1LikeAtlas()
        val sm = AnimationStateMachine(atlas)
        sm.currentFrame(1f)             // accumulate ~1 s of IDLE elapsed
        val elapsedBefore = sm.elapsedInState

        `when`("setState(RUN) is called") {
            sm.setState(SheetAnimState.RUN)

            then("currentState is RUN") {
                sm.currentState shouldBe SheetAnimState.RUN
            }
            then("elapsedInState is reset to 0") {
                sm.elapsedInState shouldBe 0f
            }
            then("the prior IDLE elapsed was non-zero (sanity check)") {
                (elapsedBefore > 0f) shouldBe true
            }
            then("currentFrame(0) of the new state is run[0]") {
                (sm.currentFrame(0f) === atlas.run[0]) shouldBe true
            }
        }
    }

    given("setState to the same state") {
        val atlas = mh1LikeAtlas()
        val sm = AnimationStateMachine(atlas)
        sm.currentFrame(0.5f)
        val elapsedBefore = sm.elapsedInState

        `when`("setState(IDLE) is called while already IDLE") {
            sm.setState(SheetAnimState.IDLE)

            then("elapsedInState is NOT reset (animation keeps playing)") {
                sm.elapsedInState shouldBe elapsedBefore
            }
        }
    }

    given("per-state FPS map") {
        val atlas = mh1LikeAtlas()
        // Default map: RUN → 12 fps, ATTACK1 → 15 fps.
        val sm = AnimationStateMachine(atlas)

        `when`("RUN state and 0.25 s of elapsed") {
            sm.setState(SheetAnimState.RUN)
            val frame = sm.currentFrame(0.25f)

            then("the index is floor(0.25 * 12) = 3 → run[3]") {
                (frame === atlas.run[3]) shouldBe true
            }
        }

        `when`("ATTACK1 state and 0.2 s of elapsed (separately, fresh SM)") {
            val sm2 = AnimationStateMachine(atlas)
            sm2.setState(SheetAnimState.ATTACK1)
            val frame = sm2.currentFrame(0.2f)

            then("the index is floor(0.2 * 15) = 3 → attack1[3]") {
                (frame === atlas.attack1[3]) shouldBe true
            }
        }
    }

    given("a custom per-state FPS override") {
        val atlas = mh1LikeAtlas()
        val sm = AnimationStateMachine(
            atlas = atlas,
            framesPerSecond = 1f,
            stateFps = mapOf(SheetAnimState.RUN to 4f), // override only RUN
        )

        `when`("currentFrame(0.5 s) is called in RUN") {
            sm.setState(SheetAnimState.RUN)
            val frame = sm.currentFrame(0.5f)
            then("the index uses 4 fps (override), not the 12 default — 0.5*4 = 2 → run[2]") {
                (frame === atlas.run[2]) shouldBe true
            }
        }

        `when`("currentFrame(0.5 s) is called in IDLE (no override → falls back to 1 fps)") {
            val sm2 = AnimationStateMachine(
                atlas = atlas,
                framesPerSecond = 1f,
                stateFps = mapOf(SheetAnimState.RUN to 4f),
            )
            sm2.setState(SheetAnimState.IDLE)
            val frame = sm2.currentFrame(0.5f)
            then("0.5 * 1 = 0 → idle[0]") {
                (frame === atlas.idle[0]) shouldBe true
            }
        }
    }

    given("a null-optional state (attack3 on MH1)") {
        val atlas = mh1LikeAtlas()    // attack3 is null
        val sm = AnimationStateMachine(atlas)

        `when`("setState(ATTACK3) then currentFrame(0)") {
            sm.setState(SheetAnimState.ATTACK3)
            val frame = sm.currentFrame(0f)

            then("the state machine falls back to idle[0] rather than throwing") {
                (frame === atlas.idle[0]) shouldBe true
            }
        }
    }

    given("MH3-style atlas — attack3 is populated and 9 frames long") {
        val atlas = mh3LikeAtlas()
        val sm = AnimationStateMachine(atlas)

        `when`("ATTACK3 advanced by 0.4 s at 15 fps (0.4*15 = 6)") {
            sm.setState(SheetAnimState.ATTACK3)
            val frame = sm.currentFrame(0.4f)
            then("the result is attack3[6]") {
                (frame === atlas.attack3!![6]) shouldBe true
            }
        }
    }
})

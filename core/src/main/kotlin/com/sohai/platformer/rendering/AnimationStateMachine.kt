package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

/**
 * Animation state ids used by the T-046 sprite-sheet renderer (T-180 scaffold).
 *
 * Distinct from the existing [AnimState] enum (IDLE/WALK/JUMP/FALL/WALL_SLIDE) used by
 * [CharacterAnimator] — that drives the procedural Pixmap path. This enum models the full
 * LuizMelo Martial Hero state set including combat states (attack1/2/3, take-hit, death),
 * which the procedural path does not. T-186 will be the first ticket to wire this
 * scaffold into the live game.
 */
enum class SheetAnimState { IDLE, RUN, JUMP, FALL, ATTACK1, ATTACK2, ATTACK3, TAKE_HIT, DEATH }

/**
 * Frame-accurate animation state machine over a [SheetCharacterAtlas] (T-180 scaffold).
 *
 * Holds a current state + elapsed time, advances the frame index on each [currentFrame]
 * call by `(elapsed * fps).toInt() % frameCount`. Calling [setState] with a different
 * state resets elapsed time to 0 so the new animation starts from frame 0; calling
 * [setState] with the *same* state is a no-op and the animation keeps playing.
 *
 * Per-state FPS:
 *  - IDLE      6  (gentle breathing loop, MH1's 8-frame idle cycles every 8/6 ≈ 1.33 s)
 *  - RUN      12  (8-frame run cycle ≈ 0.67 s — matches MH1/2/3 run feel)
 *  - JUMP     10  (2–3 frames; brief ascent — MH3's 3-frame "Going Up" reads at ≈0.3 s)
 *  - FALL     10  (2–3 frames; brief descent transition)
 *  - ATTACK1  15  (snappier than locomotion — 6/15 ≈ 0.4 s for MH1)
 *  - ATTACK2  15
 *  - ATTACK3  15
 *  - TAKE_HIT 12  (3–4 hit-flash frames)
 *  - DEATH    10  (6–11 frames; deliberate, not jittery)
 *
 * Defaults are tuned for the LuizMelo Martial Hero packs at 48 px; ticket-T-186 onward
 * can override via the [framesPerSecond] fallback or by passing a custom per-state map.
 */
class AnimationStateMachine(
    private val atlas: SheetCharacterAtlas,
    private val framesPerSecond: Float = 12f,
    private val stateFps: Map<SheetAnimState, Float> = DEFAULT_STATE_FPS,
) {

    var currentState: SheetAnimState = SheetAnimState.IDLE
        private set

    /** Time elapsed since the current state was entered. */
    var elapsedInState: Float = 0f
        private set

    /**
     * Switch to [state]. If [state] equals [currentState] this is a no-op (the animation
     * keeps playing); otherwise [elapsedInState] is reset to 0 so the new animation
     * starts from frame 0.
     */
    fun setState(state: SheetAnimState) {
        if (state != currentState) {
            currentState = state
            elapsedInState = 0f
        }
    }

    /**
     * Advance the state-machine clock by [delta] seconds and return the [TextureRegion]
     * to draw for the current frame. The frame index wraps modulo `frames.size` so
     * looping animations replay indefinitely.
     *
     * If the requested state has no frames (a null optional like [SheetCharacterAtlas.attack3]
     * on MH1/MH2) the state machine falls back to the IDLE strip — never throws.
     */
    fun currentFrame(delta: Float): TextureRegion {
        elapsedInState += delta
        val frames = framesFor(currentState) ?: atlas.idle
        val fps = stateFps[currentState] ?: framesPerSecond
        val idx = ((elapsedInState * fps).toInt()).mod(frames.size)
        return frames[idx]
    }

    private fun framesFor(state: SheetAnimState): Array<TextureRegion>? = when (state) {
        SheetAnimState.IDLE     -> atlas.idle
        SheetAnimState.RUN      -> atlas.run
        SheetAnimState.JUMP     -> atlas.jump
        SheetAnimState.FALL     -> atlas.fall
        SheetAnimState.ATTACK1  -> atlas.attack1
        SheetAnimState.ATTACK2  -> atlas.attack2
        SheetAnimState.ATTACK3  -> atlas.attack3
        SheetAnimState.TAKE_HIT -> atlas.takeHit
        SheetAnimState.DEATH    -> atlas.death
    }

    companion object {
        /** Per-state FPS defaults. See class kdoc for the rationale. */
        val DEFAULT_STATE_FPS: Map<SheetAnimState, Float> = mapOf(
            SheetAnimState.IDLE     to 6f,
            SheetAnimState.RUN      to 12f,
            SheetAnimState.JUMP     to 10f,
            SheetAnimState.FALL     to 10f,
            SheetAnimState.ATTACK1  to 15f,
            SheetAnimState.ATTACK2  to 15f,
            SheetAnimState.ATTACK3  to 15f,
            SheetAnimState.TAKE_HIT to 12f,
            SheetAnimState.DEATH    to 10f,
        )
    }
}

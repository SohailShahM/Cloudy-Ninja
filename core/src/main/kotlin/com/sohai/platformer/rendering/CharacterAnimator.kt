package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.g2d.TextureRegion

enum class AnimState { IDLE, WALK, JUMP, FALL, WALL_SLIDE }

class CharacterAnimator(val atlas: CharacterAtlas) {

    private var stateTime = 0f
    var state = AnimState.IDLE
        private set

    fun update(dt: Float, grounded: Boolean, velX: Float, velY: Float, onWall: Boolean) {
        val next = when {
            onWall && !grounded && velY <= 0f         -> AnimState.WALL_SLIDE
            !grounded && velY > 1f                    -> AnimState.JUMP
            !grounded                                 -> AnimState.FALL
            grounded && kotlin.math.abs(velX) > 0.5f -> AnimState.WALK
            else                                      -> AnimState.IDLE
        }
        if (next != state) stateTime = 0f
        state = next
        stateTime += dt
    }

    fun getCurrentFrame(): TextureRegion = when (state) {
        AnimState.IDLE       -> atlas.idle[(stateTime / 0.25f).toInt() % atlas.idle.size]
        AnimState.WALK       -> atlas.walk[(stateTime / 0.13f).toInt() % atlas.walk.size]
        AnimState.JUMP       -> atlas.jump
        AnimState.FALL       -> atlas.fall
        AnimState.WALL_SLIDE -> atlas.wallSlide
    }
}

package com.sohai.platformer.entities

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants

/**
 * A collectible eco-token placed in the world.
 * The player walks through the sensor to collect it; score is tracked in GameScreen.
 *
 * @param isHidden T-107: hidden ("golden") token. Rendered with a golden tint
 *                 and tracked separately in
 *                 [com.sohai.platformer.persist.GameState.collectedHiddenTokens].
 */
class EcoToken(
    world: World,
    xMeters: Float,
    yMeters: Float,
    val isHidden: Boolean = false
) {
    val body: Body
    var isCollected = false
    private var animTime = 0f

    init {
        val bdef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(xMeters, yMeters)
        }
        body = world.createBody(bdef)
        body.userData = this

        val shape = CircleShape().apply { radius = 0.25f }
        val fdef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
            filter.categoryBits = Constants.BIT_ECOTOKEN
            filter.maskBits = Constants.BIT_PLAYER
        }
        body.createFixture(fdef).userData = this
        shape.dispose()
    }

    fun collect() {
        isCollected = true
    }

    fun update(delta: Float) {
        animTime += delta
    }

    fun getRadius() = 0.25f
    fun getAnimatedRadius() = 0.25f + MathUtils.sin(animTime * 4f) * 0.04f
}

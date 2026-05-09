package com.sohai.platformer.entities

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants
import com.sohai.platformer.atlas.CloudAtlasEntry

/**
 * A collectible Cloud Atlas "Snapshot" placed in the world.
 * When the player touches it, [isCollected] is set to true and
 * GameScreen shows the full educational card overlay.
 */
class SnapshotPickup(
    world: World,
    xMeters: Float,
    yMeters: Float,
    val entry: CloudAtlasEntry
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

        val shape = CircleShape().apply { radius = 0.30f }
        val fdef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
            filter.categoryBits = Constants.BIT_ECOTOKEN   // reuse bit — same mask as eco-tokens
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

    /** Pulsing outer radius for the star-ring render */
    fun getAnimatedRadius() = 0.30f + MathUtils.sin(animTime * 3f) * 0.05f
}

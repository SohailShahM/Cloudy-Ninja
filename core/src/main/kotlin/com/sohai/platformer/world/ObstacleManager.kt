package com.sohai.platformer.world

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants

enum class ObstacleKind {
    GROUND,
    WALL,
    HAZARD,
    CHECKPOINT,
    EXIT
}

sealed interface ManagedObstacle {
    val id: String
    val kind: ObstacleKind
    val body: Body
    val fixture: Fixture
}

data class RectObstacle(
    override val id: String,
    override val kind: ObstacleKind,
    override val body: Body,
    override val fixture: Fixture,
    val halfWidthPx: Float,
    val halfHeightPx: Float
) : ManagedObstacle

data class CheckpointObstacle(
    override val id: String,
    override val kind: ObstacleKind,
    override val body: Body,
    override val fixture: Fixture,
    val radiusPx: Float
) : ManagedObstacle

class ObstacleManager(private val world: World) {
    private val obstacles = linkedMapOf<String, ManagedObstacle>()

    fun addRectNormalized(
        id: String,
        kind: ObstacleKind,
        xRatio: Float,
        yRatio: Float,
        halfWidthRatio: Float,
        halfHeightRatio: Float,
        sensor: Boolean = false
    ): RectObstacle {
        remove(id)

        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(
                ratioToWorldX(xRatio),
                ratioToWorldY(yRatio)
            )
        }
        val body = world.createBody(bodyDef)

        val shape = PolygonShape().apply {
            setAsBox(
                ratioToWorldX(halfWidthRatio),
                ratioToWorldY(halfHeightRatio)
            )
        }

        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = sensor
            filter.categoryBits = when (kind) {
                ObstacleKind.GROUND -> Constants.BIT_GROUND
                ObstacleKind.WALL -> Constants.BIT_WALL
                ObstacleKind.HAZARD -> Constants.BIT_HAZARD
                ObstacleKind.CHECKPOINT -> Constants.BIT_CHECKPOINT
                ObstacleKind.EXIT -> Constants.BIT_EXIT
            }
            filter.maskBits = when (kind) {
                ObstacleKind.GROUND -> (Constants.BIT_PLAYER.toInt() or Constants.BIT_HAZARD.toInt() or Constants.BIT_DROPLET.toInt()).toShort()
                ObstacleKind.WALL -> (Constants.BIT_PLAYER.toInt() or Constants.BIT_DROPLET.toInt()).toShort()
                ObstacleKind.HAZARD -> (Constants.BIT_PLAYER.toInt() or Constants.BIT_DROPLET.toInt()).toShort()
                ObstacleKind.CHECKPOINT -> Constants.BIT_PLAYER
                ObstacleKind.EXIT -> Constants.BIT_PLAYER
            }
            isSensor = if (kind == ObstacleKind.EXIT || kind == ObstacleKind.CHECKPOINT) true else sensor
            friction = if (kind == ObstacleKind.WALL) 0f else 0.5f
        }

        val fixture = body.createFixture(fixtureDef)
        fixture.userData = when (kind) {
            ObstacleKind.GROUND -> "ground"
            ObstacleKind.WALL -> "wall"
            ObstacleKind.HAZARD -> "hazard"
            ObstacleKind.CHECKPOINT -> "checkpoint"
            ObstacleKind.EXIT -> "exit"
        }

        shape.dispose()

        return RectObstacle(id, kind, body, fixture, ratioToWorldX(halfWidthRatio) * Constants.PPM, ratioToWorldY(halfHeightRatio) * Constants.PPM)
            .also { obstacles[id] = it }
    }

    fun addCheckpointNormalized(
        id: String,
        xRatio: Float,
        yRatio: Float,
        radiusRatio: Float
    ): CheckpointObstacle {
        remove(id)

        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(ratioToWorldX(xRatio), ratioToWorldY(yRatio))
        }
        val body = world.createBody(bodyDef)

        val shape = CircleShape().apply {
            radius = ratioToWorldX(radiusRatio)
        }

        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
            filter.categoryBits = Constants.BIT_CHECKPOINT
            filter.maskBits = Constants.BIT_PLAYER
        }

        val fixture = body.createFixture(fixtureDef)
        fixture.userData = "checkpoint"
        shape.dispose()

        return CheckpointObstacle(id, ObstacleKind.CHECKPOINT, body, fixture, ratioToWorldX(radiusRatio) * Constants.PPM)
            .also { obstacles[id] = it }
    }

    fun remove(id: String): Boolean {
        val existing = obstacles.remove(id) ?: return false
        world.destroyBody(existing.body)
        return true
    }

    fun clear() {
        obstacles.values.forEach { world.destroyBody(it.body) }
        obstacles.clear()
    }

    fun rects(): List<RectObstacle> = obstacles.values.filterIsInstance<RectObstacle>()

    fun checkpoints(): List<CheckpointObstacle> = obstacles.values.filterIsInstance<CheckpointObstacle>()


    private fun ratioToWorldX(ratio: Float): Float = (ratio * Constants.VIRTUAL_WIDTH) / Constants.PPM

    private fun ratioToWorldY(ratio: Float): Float = (ratio * Constants.VIRTUAL_HEIGHT) / Constants.PPM
}


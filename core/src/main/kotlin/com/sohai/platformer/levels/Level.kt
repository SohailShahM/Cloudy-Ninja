package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.ObstacleManager

/**
 * Static placement of a checkpoint inside a level definition.
 * NOT serialized — the persisted Checkpoint lives in `persist/GameState.kt`.
 */
data class LevelCheckpoint(
    val name: String,
    val x: Float,
    val y: Float,
    val levelId: String
)

/**
 * Abstract level definition. Each level knows how to set itself up in the world.
 */
abstract class Level {
    abstract val id: String
    abstract val name: String
    abstract val spawnX: Float
    abstract val spawnY: Float
    /** Approximate level width in virtual pixels, used to clamp the camera. */
    open val levelWidthPx: Float = 1280f
    /** Music track base name (loaded from `audio/music/{musicTrack}.wav`). */
    open val musicTrack: String = "ambient_arid"

    abstract fun setup(world: World, obstacleManager: ObstacleManager, movingPlatforms: MutableList<MovingPlatform>)
    abstract fun getCheckpoints(): List<LevelCheckpoint>

    /** Eco-token spawn positions in world meters (x, y). */
    open fun getEcoTokenPositions(): List<Vector2> = emptyList()

    /** Cloud Atlas snapshot pickups placed in this level. Default: none. */
    open fun getSnapshotPickups(world: World): List<SnapshotPickup> = emptyList()

    /**
     * Places a full-height exit sensor at [exitXPx] virtual pixels from the left.
     * Call this at the end of each level's setup().
     */
    protected fun addExitSensor(obstacleManager: ObstacleManager, exitXPx: Float) {
        obstacleManager.addRectNormalized(
            "level_exit",
            com.sohai.platformer.world.ObstacleKind.EXIT,
            (exitXPx + 30f) / 1280f,
            0.5f,
            30f / 1280f,
            0.5f
        )
    }
}


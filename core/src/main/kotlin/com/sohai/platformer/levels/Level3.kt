package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.world.MapLevelLoader
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Level 3 — "Stormy Heights"
 * Loads geometry from maps/level3.tmx (y-up virtual-pixel coordinates).
 * Combines wall-jump shaft climbing with fast moving platforms and precision jumps.
 */
class Level3 : Level() {
    override val id = "level3"
    override val name = "Stormy Heights"
    override val spawnX = 80f  // pixels — PlayerController divides by PPM
    override val spawnY = 80f
    override val levelWidthPx = 2200f

    override fun setup(world: World, obstacleManager: ObstacleManager, movingPlatforms: MutableList<MovingPlatform>) {
        MapLevelLoader.load("maps/level3.tmx", obstacleManager, movingPlatforms, world, flipY = true)
        obstacleManager.addRectNormalized("pit_floor", ObstacleKind.GROUND,
            0.5f, -80f / Constants.VIRTUAL_HEIGHT, 0.5f, 20f / Constants.VIRTUAL_HEIGHT)
        addExitSensor(obstacleManager, 2050f)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = listOf(
        LevelCheckpoint("cp1",  6.0f, 0.8f, id),
        LevelCheckpoint("cp2", 11.0f, 0.8f, id),
        LevelCheckpoint("cp3", 17.0f, 0.8f, id)
    )

    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        Vector2(350f / Constants.PPM,  120f / Constants.PPM),
        Vector2(470f / Constants.PPM,  230f / Constants.PPM),
        Vector2(600f / Constants.PPM,  330f / Constants.PPM),
        Vector2(730f / Constants.PPM,  490f / Constants.PPM),  // sky shortcut reward
        Vector2(900f / Constants.PPM,  310f / Constants.PPM),
        Vector2(1110f / Constants.PPM, 220f / Constants.PPM),
        Vector2(1340f / Constants.PPM, 130f / Constants.PPM),
        Vector2(1490f / Constants.PPM, 420f / Constants.PPM),  // high platform
        Vector2(1710f / Constants.PPM, 300f / Constants.PPM),
        Vector2(1960f / Constants.PPM, 200f / Constants.PPM)   // final approach
    )
}

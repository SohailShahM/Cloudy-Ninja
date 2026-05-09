package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.MapLevelLoader
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Level 2 — "Winds of Change"
 * Loads geometry from maps/level2.tmx (y-up virtual-pixel coordinates).
 * Mastering Laya's Wind Dash is essential to cross the air gaps.
 */
class Level2 : Level() {
    override val id = "level2"
    override val name = "Winds of Change"
    override val spawnX = 80f  // pixels — PlayerController divides by PPM
    override val spawnY = 80f
    override val levelWidthPx = 2100f

    override fun setup(world: World, obstacleManager: ObstacleManager, movingPlatforms: MutableList<MovingPlatform>) {
        MapLevelLoader.load("maps/level2.tmx", obstacleManager, movingPlatforms, world, flipY = true)
        obstacleManager.addRectNormalized("pit_floor", ObstacleKind.GROUND,
            0.5f, -80f / Constants.VIRTUAL_HEIGHT, 0.5f, 20f / Constants.VIRTUAL_HEIGHT)
        addExitSensor(obstacleManager, 1950f)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = listOf(
        LevelCheckpoint("cp1", 7.0f,  0.8f, id),
        LevelCheckpoint("cp2", 14.0f, 0.8f, id)
    )

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = listOfNotNull(
        CloudAtlasLibrary.get("temperature_inversion")?.let {
            SnapshotPickup(world, 600f / Constants.PPM, 200f / Constants.PPM, it)
        },
        CloudAtlasLibrary.get("albedo_effect")?.let {
            SnapshotPickup(world, 1500f / Constants.PPM, 300f / Constants.PPM, it)
        }
    )

    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        Vector2(340f / Constants.PPM,  100f / Constants.PPM),
        Vector2(540f / Constants.PPM,  170f / Constants.PPM),
        Vector2(730f / Constants.PPM,  250f / Constants.PPM),
        Vector2(950f / Constants.PPM,  310f / Constants.PPM),
        Vector2(620f / Constants.PPM,  420f / Constants.PPM),
        Vector2(820f / Constants.PPM,  470f / Constants.PPM),
        Vector2(1240f / Constants.PPM, 200f / Constants.PPM),
        Vector2(1550f / Constants.PPM, 180f / Constants.PPM),
        Vector2(1720f / Constants.PPM, 230f / Constants.PPM),
        Vector2(1920f / Constants.PPM, 200f / Constants.PPM)
    )
}

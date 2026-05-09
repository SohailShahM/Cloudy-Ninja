package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.MapLevelLoader
import com.sohai.platformer.world.ObstacleManager

/**
 * Level 1 — "The First Rain"
 * Loads geometry from maps/level1.tmx (y-up virtual-pixel coordinates).
 * Introduces Ebo's Seed Slam and wall-jump mechanics.
 */
class Level1 : Level() {
    override val id = "level1"
    override val name = "The First Rain"
    // Spawn above the start platform (start_ground covers x=0..360, y=0..40)
    override val spawnX = 80f  // pixels — PlayerController divides by PPM
    override val spawnY = 80f
    override val levelWidthPx = 2100f

    override fun setup(world: World, obstacleManager: ObstacleManager, movingPlatforms: MutableList<MovingPlatform>) {
        MapLevelLoader.load("maps/level1.tmx", obstacleManager, movingPlatforms, world, flipY = true)
        // Pit safety net so the player lands somewhere before the death threshold
        obstacleManager.addRectNormalized("pit_floor", com.sohai.platformer.world.ObstacleKind.GROUND,
            0.5f, -80f / Constants.VIRTUAL_HEIGHT, 0.5f, 20f / Constants.VIRTUAL_HEIGHT)
        addExitSensor(obstacleManager, 1950f)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = listOf(
        LevelCheckpoint("cp1", 7.0f,  0.8f, id),
        LevelCheckpoint("cp2", 14.0f, 0.8f, id)
    )

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = listOfNotNull(
        CloudAtlasLibrary.get("silver_iodide")?.let {
            SnapshotPickup(world, 450f / Constants.PPM, 100f / Constants.PPM, it)
        },
        CloudAtlasLibrary.get("water_cycle")?.let {
            SnapshotPickup(world, 1200f / Constants.PPM, 400f / Constants.PPM, it)
        }
    )

    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        Vector2(250f / Constants.PPM,  60f / Constants.PPM),
        Vector2(560f / Constants.PPM, 130f / Constants.PPM),
        Vector2(700f / Constants.PPM, 200f / Constants.PPM),
        Vector2(900f / Constants.PPM, 310f / Constants.PPM),
        Vector2(1150f / Constants.PPM, 360f / Constants.PPM),
        Vector2(1400f / Constants.PPM, 450f / Constants.PPM),
        Vector2(1610f / Constants.PPM, 450f / Constants.PPM),
        Vector2(1800f / Constants.PPM, 410f / Constants.PPM)
    )
}

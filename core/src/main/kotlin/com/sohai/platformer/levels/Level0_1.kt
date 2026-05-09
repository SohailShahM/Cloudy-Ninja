package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * World 0, Room 1 — "First Step"
 *
 * Single-screen tutorial room (no scrolling). Teaches walk + ground jump.
 * Layout (virtual pixels, 1280×720):
 *
 *   LEFT GROUND  |   2-tile gap (128 px)   |  RIGHT GROUND  [EXIT]
 *   x=0..600     |   x=600..728            |  x=728..1280
 *   y=0..40      |                         |  y=0..40
 *
 * One eco-token sits on the far side of the gap — reachable only by jumping.
 * No text, no UI hints; the token and the gap are the entire lesson.
 *
 * On exit: transitions to Level0_2 if registered, falls back to level1.
 */
class Level0_1 : Level() {
    override val id = "level0_1"
    override val name = "First Step"

    // Spawn on the left platform, safely above ground
    override val spawnX = 80f   // pixels; PlayerController divides by PPM
    override val spawnY = 80f

    // Single screen — no horizontal scrolling
    override val levelWidthPx = Constants.VIRTUAL_WIDTH   // 1280f

    override fun setup(
        world: World,
        obstacleManager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>
    ) {
        val vw = Constants.VIRTUAL_WIDTH
        val vh = Constants.VIRTUAL_HEIGHT

        // ── Floor ──────────────────────────────────────────────────────────────
        // Left ground platform: x=0..600 px, y=0..40 px
        // Center (300, 20), half-extents (300, 20)
        obstacleManager.addRectNormalized(
            "ground_left", ObstacleKind.GROUND,
            xRatio        = 300f / vw,
            yRatio        = 20f  / vh,
            halfWidthRatio  = 300f / vw,
            halfHeightRatio = 20f  / vh
        )

        // Right ground platform: x=728..1280 px (gap ends at 728), y=0..40 px
        // Center (1004, 20), half-extents (276, 20)
        obstacleManager.addRectNormalized(
            "ground_right", ObstacleKind.GROUND,
            xRatio        = 1004f / vw,
            yRatio        = 20f   / vh,
            halfWidthRatio  = 276f / vw,
            halfHeightRatio = 20f  / vh
        )

        // ── Boundary walls ─────────────────────────────────────────────────────
        // Left wall: x=-10..0 px, full height
        obstacleManager.addRectNormalized(
            "wall_left", ObstacleKind.WALL,
            xRatio        = -5f  / vw,
            yRatio        = 0.5f,
            halfWidthRatio  = 5f / vw,
            halfHeightRatio = 0.5f
        )

        // Right wall: x=1280..1290 px, full height
        obstacleManager.addRectNormalized(
            "wall_right", ObstacleKind.WALL,
            xRatio        = 1285f / vw,
            yRatio        = 0.5f,
            halfWidthRatio  = 5f  / vw,
            halfHeightRatio = 0.5f
        )

        // Pit safety net: catches the player if they fall into the gap
        obstacleManager.addRectNormalized(
            "pit_floor", ObstacleKind.GROUND,
            xRatio        = 0.5f,
            yRatio        = -80f / vh,
            halfWidthRatio  = 0.5f,
            halfHeightRatio = 20f / vh
        )

        // ── Level exit sensor ──────────────────────────────────────────────────
        // Placed near the right edge so the player must cross the whole right platform
        addExitSensor(obstacleManager, 1180f)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = emptyList()

    /**
     * Single eco-token on the far side of the gap (right platform).
     * x ≈ 860 px (2 tile-widths from the gap edge), y ≈ 80 px above ground.
     */
    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        Vector2(860f / Constants.PPM, 80f / Constants.PPM)
    )
}

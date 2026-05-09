package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * World 0, Room 3 — "Wall Climb"
 *
 * Single-screen tutorial room. Teaches wall-jumping.
 *
 * Layout (virtual pixels, 1280×720, y=0 is bottom):
 *
 *   ─── wall_shaft_left ──────────────────────── wall_shaft_right ───
 *       x=540..580                                   x=700..740
 *       y=0..720                                     y=0..720
 *
 *                 ╔══════════ SHAFT ═══════════╗
 *                 ║   x=580..700  (120 px)     ║
 *                 ║                            ║
 *                 ║   [EXIT SENSOR y=680]       ║  ← player must reach here
 *                 ║                            ║
 *                 ║   wall-jump up ↑↑↑          ║
 *                 ║                            ║
 *                 ║   [eco-token y=360]         ║  ← mid-shaft reward
 *                 ║                            ║
 *                 ╚════════════════════════════╝
 *         [SPAWN x=640, y=80]
 *         ───── spawn_floor x=560..720, y=0..40 ─────
 *
 * Physics:
 *   PLAYER_WALL_JUMP_IMPULSE_Y = 11 m/s, effective rise gravity = 16 m/s².
 *   Height per wall-bounce across 1.2 m shaft ≈ 156 px → ~4 bounces clears 600 px.
 *
 * No death: safety net at y = -110 px catches any fall so the player can retry
 * without a game-over screen. Falling back to the spawn floor is the natural reset.
 *
 * On exit: transitions to "level0_4" if registered, falls back to "level1".
 */
class Level0_3 : Level() {
    override val id = "level0_3"
    override val name = "Wall Climb"

    override val spawnX = 640f   // center of shaft, in virtual pixels
    override val spawnY = 80f    // above the spawn floor

    override val levelWidthPx = Constants.VIRTUAL_WIDTH   // single screen

    override fun setup(
        world: World,
        obstacleManager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>
    ) {
        val vw = Constants.VIRTUAL_WIDTH   // 1280f
        val vh = Constants.VIRTUAL_HEIGHT  // 720f

        // ── SPAWN FLOOR (chimney base) ────────────────────────────────────────
        // x=560..720, y=0..40 → center=(640,20), half=(80,20)
        obstacleManager.addRectNormalized(
            "spawn_floor", ObstacleKind.GROUND,
            xRatio          = 640f / vw,
            yRatio          = 20f  / vh,
            halfWidthRatio  = 80f  / vw,
            halfHeightRatio = 20f  / vh
        )

        // ── LEFT SHAFT WALL ───────────────────────────────────────────────────
        // x=540..580 (inner face at x=580), y=0..720
        // center=(560,360), half=(20,360)
        obstacleManager.addRectNormalized(
            "wall_shaft_left", ObstacleKind.WALL,
            xRatio          = 560f / vw,
            yRatio          = 360f / vh,
            halfWidthRatio  = 20f  / vw,
            halfHeightRatio = 360f / vh
        )

        // ── RIGHT SHAFT WALL ──────────────────────────────────────────────────
        // x=700..740 (inner face at x=700), y=0..720
        // center=(720,360), half=(20,360)
        obstacleManager.addRectNormalized(
            "wall_shaft_right", ObstacleKind.WALL,
            xRatio          = 720f / vw,
            yRatio          = 360f / vh,
            halfWidthRatio  = 20f  / vw,
            halfHeightRatio = 360f / vh
        )

        // ── OUTER BOUNDARY WALLS ──────────────────────────────────────────────
        // Prevent the player from escaping left or right outside the shaft area.
        obstacleManager.addRectNormalized(
            "wall_left", ObstacleKind.WALL,
            xRatio          = -5f  / vw,
            yRatio          = 0.5f,
            halfWidthRatio  = 5f   / vw,
            halfHeightRatio = 0.5f
        )
        obstacleManager.addRectNormalized(
            "wall_right", ObstacleKind.WALL,
            xRatio          = 1285f / vw,
            yRatio          = 0.5f,
            halfWidthRatio  = 5f    / vw,
            halfHeightRatio = 0.5f
        )

        // ── SAFETY NET ────────────────────────────────────────────────────────
        // Full-width floor far below the screen — catches any fall without death
        // so the player can try again without a game-over screen.
        obstacleManager.addRectNormalized(
            "pit_floor", ObstacleKind.GROUND,
            xRatio          = 0.5f,
            yRatio          = -110f / vh,
            halfWidthRatio  = 0.5f,
            halfHeightRatio = 20f   / vh
        )

        // ── EXIT SENSOR ───────────────────────────────────────────────────────
        // Horizontal sensor spanning the shaft at y=680 px — triggered when the
        // player wall-jumps up to this height. Uses addRectNormalized with EXIT
        // kind directly (instead of the vertical addExitSensor helper) because
        // this exit is at the top of the shaft, not the right side of the level.
        obstacleManager.addRectNormalized(
            "level_exit", ObstacleKind.EXIT,
            xRatio          = 640f / vw,
            yRatio          = 680f / vh,
            halfWidthRatio  = 80f  / vw,
            halfHeightRatio = 30f  / vh
        )
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = emptyList()

    /**
     * Single eco-token at mid-shaft height — requires at least two wall jumps to reach.
     * Rewards the player for making progress even before they exit.
     */
    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        Vector2(640f / Constants.PPM, 360f / Constants.PPM)
    )
}

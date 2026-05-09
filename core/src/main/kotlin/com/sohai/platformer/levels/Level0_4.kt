package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * World 0, Room 4 — "First Cleanse"
 *
 * Single-screen tutorial room. Teaches Ebo's Seed Slam ability.
 *
 * Layout (virtual pixels, 1280×720, y=0 is bottom):
 *
 *   LEFT FLOOR     HAZARD STRIP (single body)     RIGHT FLOOR
 *   x=0..280       x=280..1100                    x=1100..1280
 *   y=0..40        y=0..40                        y=0..40
 *
 *   [SPAWN x=80]   [HAZARD — kills on contact]    [ECO-TOKEN x=1160] [EXIT x=1180]
 *
 * Teaching flow:
 *   1. Player walks right and dies on the hazard strip.
 *   2. HUD action button pulses, directing attention to Seed Slam.
 *   3. Player stands at the edge of the left floor and triggers Seed Slam.
 *   4. One WaterDroplet hit marks the ENTIRE hazard body as "hazard_cleaned".
 *   5. Player walks across the now-safe (green) strip and exits.
 *
 * The hazard strip is 820 px wide — wider than the maximum single-jump horizontal
 * travel (~738 px at PLAYER_SPEED=9 m/s), so the player cannot bypass it by jumping.
 *
 * On exit: transitions to "level1" (first campaign level) via the standard
 * LevelManager sequence and GameScreen screenFade.
 */
class Level0_4 : Level() {
    override val id = "level0_4"
    override val name = "First Cleanse"

    override val spawnX = 80f    // pixels; PlayerController divides by PPM
    override val spawnY = 80f

    override val levelWidthPx = Constants.VIRTUAL_WIDTH   // single screen

    override fun setup(
        world: World,
        obstacleManager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>
    ) {
        val vw = Constants.VIRTUAL_WIDTH   // 1280f
        val vh = Constants.VIRTUAL_HEIGHT  // 720f

        // ── LEFT FLOOR (spawn platform) ──────────────────────────────────────
        // x=0..280 px, y=0..40 px  →  center=(140, 20), half=(140, 20)
        obstacleManager.addRectNormalized(
            "ground_left", ObstacleKind.GROUND,
            xRatio          = 140f / vw,
            yRatio          = 20f  / vh,
            halfWidthRatio  = 140f / vw,
            halfHeightRatio = 20f  / vh
        )

        // ── HAZARD STRIP ──────────────────────────────────────────────────────
        // Single wide body: x=280..1100 px, y=0..40 px
        // center=(690, 20), half=(410, 20)
        //
        // One WaterDroplet hit marks the ENTIRE body as "hazard_cleaned" — the
        // contact listener operates per-fixture, not per-pixel. Once cleansed,
        // the strip is safe to walk across (no player death) and is rendered green.
        obstacleManager.addRectNormalized(
            "hazard_strip", ObstacleKind.HAZARD,
            xRatio          = 690f / vw,
            yRatio          = 20f  / vh,
            halfWidthRatio  = 410f / vw,
            halfHeightRatio = 20f  / vh
        )

        // ── RIGHT FLOOR (exit platform) ───────────────────────────────────────
        // x=1100..1280 px, y=0..40 px  →  center=(1190, 20), half=(90, 20)
        obstacleManager.addRectNormalized(
            "ground_right", ObstacleKind.GROUND,
            xRatio          = 1190f / vw,
            yRatio          = 20f   / vh,
            halfWidthRatio  = 90f   / vw,
            halfHeightRatio = 20f   / vh
        )

        // ── BOUNDARY WALLS ────────────────────────────────────────────────────
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
        // Catches any fall without triggering death so the player can retry.
        obstacleManager.addRectNormalized(
            "pit_floor", ObstacleKind.GROUND,
            xRatio          = 0.5f,
            yRatio          = -110f / vh,
            halfWidthRatio  = 0.5f,
            halfHeightRatio = 20f   / vh
        )

        // ── EXIT SENSOR ───────────────────────────────────────────────────────
        // Standard right-edge exit — transitions to level1.
        addExitSensor(obstacleManager, 1180f)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = emptyList()

    /**
     * Single eco-token on the right platform — reward for completing the cleanse.
     * Placed after the hazard strip so it's only reachable once the strip is safe.
     */
    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        Vector2(1160f / Constants.PPM, 80f / Constants.PPM)
    )
}

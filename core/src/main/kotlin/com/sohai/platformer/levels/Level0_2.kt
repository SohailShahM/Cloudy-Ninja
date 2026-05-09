package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * World 0, Room 2 — "Long Fall"
 *
 * Single-screen tutorial room (no scrolling). Teaches two advanced mechanics:
 *
 * 1. VARIABLE JUMP HEIGHT — a low-ceiling passage (~1.5 tiles of clearance above the
 *    floor) that only fits if the player releases jump early. A full held jump overshoots
 *    and bonks the ceiling; a short hop (release before apex) slips through cleanly.
 *
 * 2. COYOTE TIME — a ledge that juts out over a gap. The player must walk off the edge
 *    and then jump while briefly airborne (within COYOTE_TIME ~0.10 s) to reach the next
 *    platform. No alternate path exists; the gap is too wide to cross any other way.
 *
 * Layout (virtual pixels, 1280×720, y=0 is bottom):
 *
 *   [SPAWN]                                           [EXIT]
 *   ──────── left_floor ────┐   ┌─ coyote_ledge ─┐   ┌───── right_floor ───────
 *   x=0..380                │   │  x=480..760     │   │ x=960..1280
 *   y=0..40                 GAP │  y=0..40        GAP │ y=0..40
 *                           │   │                 │   │
 *        ════════ ceiling ══════ (y=136..160, x=80..380)
 *        (variable-jump passage above left_floor)
 *
 * Safety net sits far below (y=-120..-100 px) to catch any fall without death.
 *
 * On exit: transitions to "level0_3" if registered, falls back to "level1".
 */
class Level0_2 : Level() {
    override val id = "level0_2"
    override val name = "Long Fall"

    // Spawn on the left platform, safely above ground
    override val spawnX = 80f    // pixels; PlayerController divides by PPM
    override val spawnY = 80f

    // Single screen — no horizontal scrolling
    override val levelWidthPx = Constants.VIRTUAL_WIDTH   // 1280f

    override fun setup(
        world: World,
        obstacleManager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>
    ) {
        val vw = Constants.VIRTUAL_WIDTH   // 1280f
        val vh = Constants.VIRTUAL_HEIGHT  // 720f

        // ── LEFT FLOOR (spawn platform) ──────────────────────────────────────
        // x=0..380 px, y=0..40 px  → center=(190,20), half=(190,20)
        obstacleManager.addRectNormalized(
            "ground_left", ObstacleKind.GROUND,
            xRatio          = 190f / vw,
            yRatio          = 20f  / vh,
            halfWidthRatio  = 190f / vw,
            halfHeightRatio = 20f  / vh
        )

        // ── LOW CEILING (variable-jump passage) ──────────────────────────────
        // Bottom of ceiling at y=136 px.
        //   Floor top  = 40 px
        //   Player height = 64 px → player top at rest = 40+64 = 104 px
        //   Clearance = 136-104 = 32 px  (~0.5 tile)
        //
        // Full jump peak (JUMP_IMPULSE=13 m/s):
        //   y_peak ≈ 13²/(2×32) ≈ 2.64 m = 264 px  → player top hits ~104+264 = 368 px → BONK
        //
        // Short hop (release early → vel cut by JUMP_CUT_MUL=0.4):
        //   v_cut ≈ 13×0.4 = 5.2 m/s → peak ≈ 5.2²/(2×32×1.45) ≈ 0.29 m = 29 px → stays well below 136 px
        //
        // Ceiling slab: x=80..380 px, y=136..160 px
        //   center=(230, 148), half=(150, 12)
        obstacleManager.addRectNormalized(
            "ceiling_passage", ObstacleKind.GROUND,
            xRatio          = 230f / vw,
            yRatio          = 148f / vh,
            halfWidthRatio  = 150f / vw,
            halfHeightRatio = 12f  / vh
        )

        // ── COYOTE LEDGE (mid platform with open right edge) ─────────────────
        // x=480..760 px, y=0..40 px  → center=(620, 20), half=(140, 20)
        // Player must walk rightward off the edge at x=760 and then jump in the
        // air to bridge the gap. Gap = x=760..960 = 200 px = 2 m.
        // At PLAYER_SPEED=9 m/s the coyote window of 0.10 s drifts the player
        // 90 px into the gap before jumping — remaining 110 px is cleared in ~0.12 s.
        obstacleManager.addRectNormalized(
            "coyote_ledge", ObstacleKind.GROUND,
            xRatio          = 620f / vw,
            yRatio          = 20f  / vh,
            halfWidthRatio  = 140f / vw,
            halfHeightRatio = 20f  / vh
        )

        // ── RIGHT FLOOR (exit platform) ───────────────────────────────────────
        // x=960..1280 px, y=0..40 px  → center=(1120, 20), half=(160, 20)
        obstacleManager.addRectNormalized(
            "ground_right", ObstacleKind.GROUND,
            xRatio          = 1120f / vw,
            yRatio          = 20f   / vh,
            halfWidthRatio  = 160f  / vw,
            halfHeightRatio = 20f   / vh
        )

        // ── BOUNDARY WALLS ────────────────────────────────────────────────────
        // Left wall: x=-10..0 px, full height — prevents backtracking into void
        obstacleManager.addRectNormalized(
            "wall_left", ObstacleKind.WALL,
            xRatio          = -5f  / vw,
            yRatio          = 0.5f,
            halfWidthRatio  = 5f   / vw,
            halfHeightRatio = 0.5f
        )

        // Right wall: x=1280..1290 px, full height
        obstacleManager.addRectNormalized(
            "wall_right", ObstacleKind.WALL,
            xRatio          = 1285f / vw,
            yRatio          = 0.5f,
            halfWidthRatio  = 5f    / vw,
            halfHeightRatio = 0.5f
        )

        // ── SAFETY NET ────────────────────────────────────────────────────────
        // Spans the full width well below the screen — catches any fall so the
        // player bounces back up rather than dying, letting them retry naturally.
        // y center = -110 px → yRatio negative, same trick as Level0_1
        obstacleManager.addRectNormalized(
            "pit_floor", ObstacleKind.GROUND,
            xRatio          = 0.5f,
            yRatio          = -110f / vh,
            halfWidthRatio  = 0.5f,
            halfHeightRatio = 20f   / vh
        )

        // ── LEVEL EXIT SENSOR ─────────────────────────────────────────────────
        // Near the right edge — player must cross the full right platform
        addExitSensor(obstacleManager, 1180f)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = emptyList()

    /**
     * Two eco-tokens — one rewards the successful short hop (placed just past the
     * ceiling passage), one rewards the coyote-time landing on the right floor.
     */
    override fun getEcoTokenPositions(): List<Vector2> = listOf(
        // Past the low ceiling passage, rewarding the short hop
        Vector2(420f / Constants.PPM, 80f / Constants.PPM),
        // On the right floor, rewarding the coyote-time jump
        Vector2(1020f / Constants.PPM, 80f / Constants.PPM)
    )
}

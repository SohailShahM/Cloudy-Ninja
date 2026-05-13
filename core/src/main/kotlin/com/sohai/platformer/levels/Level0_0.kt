package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Level 0-0 — Sky Sanctuary (Hub World)
 *
 * Single-screen hub room (~1280px wide). The player spawns center-left and
 * can walk to one of four portal doors (sensor bodies) to enter a world.
 *
 * Layout (virtual pixels, 1280x720, y=0 is bottom):
 *
 *   GROUND: full width x=0..1280, y=0..40
 *
 *   Four portal doors evenly spaced:
 *     portal_world0 at x=220   (World 0 — Tutorial)
 *     portal_world1 at x=480   (World 1 — The First Rain)
 *     portal_world2 at x=740   (World 2 — Winds of Change)
 *     portal_world3 at x=1000  (World 3 — Stormy Heights)
 *
 *   Each portal is a sensor body 60px wide x 100px tall, sitting on the ground.
 *
 *   Decorative stone pillars at x=100 and x=1180 (walls, non-functional boundary).
 *
 * Portal activation is handled in LevelRunState: when the player contacts a
 * portal sensor, check GameState.completedLevels to determine if the world is
 * unlocked, then navigate to the first level of that world.
 *
 * There is no exit sensor — the player leaves via portals only.
 */
class Level0_0 : Level() {
    override val id = "level0_0"
    override val name = "Sky Sanctuary"

    // Spawn center-left, above ground
    override val spawnX = 140f   // pixels; PlayerController divides by PPM
    override val spawnY = 80f

    // Single screen — no horizontal scrolling
    override val levelWidthPx = Constants.VIRTUAL_WIDTH   // 1280f

    override val musicTrack: String = "ambient_arid"

    /** Portal definitions: (userData, centerX px, worldIndex). */
    data class PortalDef(
        val userData: String,
        val centerXPx: Float,
        val label: String
    )

    companion object {
        val PORTALS = listOf(
            PortalDef("portal_world0", 220f,  "Tutorial"),
            PortalDef("portal_world1", 480f,  "World 1"),
            PortalDef("portal_world2", 740f,  "World 2"),
            PortalDef("portal_world3", 1000f, "World 3")
        )

        /** Portal body dimensions in virtual pixels. */
        const val PORTAL_HALF_W_PX = 30f
        const val PORTAL_HALF_H_PX = 50f
        /** Portal base Y center — sits on top of the ground (ground top = 40px). */
        const val PORTAL_CENTER_Y_PX = 90f  // 40 + 50 = 90, so bottom of portal at y=40

        /**
         * Maps a portal userData string to the level id the player should navigate to.
         * Returns null if the portal id is not recognized.
         */
        fun portalTargetLevel(portalId: String): String? = when (portalId) {
            "portal_world0" -> "level0_1"
            "portal_world1" -> "level1"
            "portal_world2" -> "level2"
            "portal_world3" -> "level3"
            else -> null
        }

        /**
         * Returns the set of completedLevels entries required to unlock a portal.
         * An empty set means the portal is always unlocked.
         */
        fun portalUnlockRequirement(portalId: String): Set<String> = when (portalId) {
            "portal_world0" -> emptySet()                 // always unlocked
            "portal_world1" -> setOf("level0_4")          // must complete tutorial
            "portal_world2" -> setOf("level1")            // must complete World 1
            "portal_world3" -> setOf("level2")            // must complete World 2
            else -> setOf("__impossible__")
        }

        /**
         * T-137: Returns true if [HubTutorialOverlay] should be shown for a
         * player whose save reads [state]. Pure function — no I/O — so the
         * gate is unit-testable without touching libGDX. The host screen
         * (`GameScreen`) calls this on Level0_0 construction; on a true
         * result, it builds the overlay and persists `tutorialSeen=true`
         * once the player dismisses it.
         */
        fun shouldShowFirstRunTutorial(state: GameState): Boolean = !state.tutorialSeen
    }

    override fun setup(
        world: World,
        obstacleManager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>
    ) {
        val vw = Constants.VIRTUAL_WIDTH   // 1280f
        val vh = Constants.VIRTUAL_HEIGHT  // 720f

        // -- Full-width ground platform: x=0..1280, y=0..40 --
        obstacleManager.addRectNormalized(
            "ground", ObstacleKind.GROUND,
            xRatio          = 0.5f,
            yRatio          = 20f / vh,
            halfWidthRatio  = 0.5f,
            halfHeightRatio = 20f / vh
        )

        // -- Boundary walls --
        obstacleManager.addRectNormalized(
            "wall_left", ObstacleKind.WALL,
            xRatio          = -5f / vw,
            yRatio          = 0.5f,
            halfWidthRatio  = 5f / vw,
            halfHeightRatio = 0.5f
        )
        obstacleManager.addRectNormalized(
            "wall_right", ObstacleKind.WALL,
            xRatio          = 1285f / vw,
            yRatio          = 0.5f,
            halfWidthRatio  = 5f / vw,
            halfHeightRatio = 0.5f
        )

        // -- Decorative stone pillars --
        obstacleManager.addRectNormalized(
            "pillar_left", ObstacleKind.WALL,
            xRatio          = 60f / vw,
            yRatio          = 120f / vh,
            halfWidthRatio  = 15f / vw,
            halfHeightRatio = 80f / vh
        )
        obstacleManager.addRectNormalized(
            "pillar_right", ObstacleKind.WALL,
            xRatio          = 1220f / vw,
            yRatio          = 120f / vh,
            halfWidthRatio  = 15f / vw,
            halfHeightRatio = 80f / vh
        )

        // -- Safety net below ground --
        obstacleManager.addRectNormalized(
            "pit_floor", ObstacleKind.GROUND,
            xRatio          = 0.5f,
            yRatio          = -80f / vh,
            halfWidthRatio  = 0.5f,
            halfHeightRatio = 20f / vh
        )

        // -- Portal sensor bodies --
        for (portal in PORTALS) {
            obstacleManager.addRectNormalized(
                portal.userData, ObstacleKind.EXIT,
                xRatio          = portal.centerXPx / vw,
                yRatio          = PORTAL_CENTER_Y_PX / vh,
                halfWidthRatio  = PORTAL_HALF_W_PX / vw,
                halfHeightRatio = PORTAL_HALF_H_PX / vh,
                sensor          = true
            )
            // Override the fixture userData from "exit" to portal-specific id
            // so WorldContactListener can distinguish portals from regular exits.
            val rect = obstacleManager.rects().last { it.id == portal.userData }
            rect.fixture.userData = portal.userData
        }
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> = emptyList()

    override fun getEcoTokenPositions(): List<Vector2> = emptyList()
}

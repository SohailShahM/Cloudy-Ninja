package com.sohai.platformer.rendering

import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.world.ObstacleKind

/**
 * Central registry for [TilesetPack] instances.
 *
 * Usage:
 *   - Call [register] once at app startup for each available pack.
 *   - Renderers call [current] to obtain the active pack (determined by
 *     [com.sohai.platformer.persist.Settings.tilesetPackId]).
 *   - Future art-style UI calls [register] and updates Settings to swap packs.
 *
 * The Kenney pixel-platformer pack is registered in the [defaults] block below
 * and is always present.  All tile indices are 0-based into the 20-column
 * `tilemap_packed.png` sheet (18×18 px tiles, 20 cols × 9 rows = 180 tiles).
 *
 * Tile-index mapping rationale (verified against
 * `assets/tilesets/kenney_pixel_platformer/Tiled/tilemap-example-a.tmx`):
 *
 *   The TMX uses firstgid=28 for tileset-tiles, so Kenney-index = CSV-value - 28.
 *   Ground platforms in the example use:
 *     - CSV 50 (→ Kenney 22): grass-top center tile  (row 1, col 2 in 20-col sheet)
 *     - CSV 150 (→ Kenney 122): dirt body/fill tile  (row 6, col 2)
 *   Spike hazards:
 *     - CSV 97 (→ Kenney 69): spike tile             (row 3, col 9)
 *   Stone/brick wall:
 *     - CSV 136 (→ Kenney 108): stone block          (row 5, col 8)
 *
 *   CHECKPOINT and EXIT kinds are intentionally absent from the tileMap — the
 *   renderer falls back to the existing ShapeRenderer path for those.
 */
object TilesetRegistry {

    private val packs = mutableMapOf<String, TilesetPack>()

    /** The id of the fallback pack, always present. */
    const val KENNEY_ID = "kenney_pixel_platformer"

    init {
        defaults()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Register a [TilesetPack].  Re-registering the same id replaces the entry. */
    fun register(pack: TilesetPack) {
        packs[pack.id] = pack
    }

    /** Returns the registered pack for [id], or null if not found. */
    fun get(id: String): TilesetPack? = packs[id]

    /**
     * Returns the active pack: reads [com.sohai.platformer.persist.Settings.tilesetPackId]
     * and looks it up; falls back to the Kenney pack if the id is unknown or
     * Settings cannot be loaded.
     */
    val current: TilesetPack
        get() {
            val id = try {
                SettingsManager.load().tilesetPackId
            } catch (_: Exception) {
                KENNEY_ID
            }
            return packs[id] ?: packs[KENNEY_ID]!!
        }

    // ── Default registrations ─────────────────────────────────────────────────

    private fun defaults() {
        // ── Kenney pixel-platformer (CC0) ─────────────────────────────────────
        // Sheet: tilemap_packed.png, 20 cols × 9 rows, 18×18 px per tile.
        // All tile indices below are 0-based (Kenney index = Tiled CSV value − 28).
        //
        // Tile 22  (row 1, col 2)  = grass-top center     → GROUND top row
        // Tile 122 (row 6, col 2)  = dirt interior fill   → GROUND body rows
        // Tile 108 (row 5, col 8)  = stone/brick block    → WALL
        // Tile 69  (row 3, col 9)  = spike tile           → HAZARD
        //
        // CHECKPOINT and EXIT are absent — those fall back to ShapeRenderer.

        val groundMapping = TileMapping(topTileIndex = 22, fillTileIndex = 122)
        val wallMapping   = uniformMapping(108)
        val hazardMapping = uniformMapping(69)

        val allThemes = ParallaxTheme.entries

        val tileMap = buildMap<Pair<ObstacleKind, ParallaxTheme>, TileMapping> {
            for (theme in allThemes) {
                // Kenney v1 uses the same indices for all three themes.
                // Per-theme overrides can be added when a dedicated ARID/ECO tileset lands.
                put(ObstacleKind.GROUND to theme, groundMapping)
                put(ObstacleKind.WALL   to theme, wallMapping)
                put(ObstacleKind.HAZARD to theme, hazardMapping)
                // CHECKPOINT + EXIT intentionally omitted → ShapeRenderer fallback
            }
        }

        register(
            TilesetPack(
                id          = KENNEY_ID,
                displayName = "Kenney Pixel Platformer",
                tileWidth   = 18,
                tileHeight  = 18,
                atlasPath   = "tilesets/kenney_pixel_platformer/Tilemap/tilemap_packed.png",
                tileMap     = tileMap
            )
        )
    }
}

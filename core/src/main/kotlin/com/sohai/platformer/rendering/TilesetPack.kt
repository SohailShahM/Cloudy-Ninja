package com.sohai.platformer.rendering

import com.sohai.platformer.world.ObstacleKind

/**
 * Pure-data description of a tileset pack.
 *
 * Holds the metadata and tile-index mappings needed to render [ObstacleKind]
 * variants per [ParallaxTheme].  No textures are loaded here — texture loading
 * is deferred to [TileRenderer].
 *
 * @param id          Stable identifier used by [TilesetRegistry] and persisted
 *                    in [com.sohai.platformer.persist.Settings.tilesetPackId].
 * @param displayName Human-readable name for a future art-style picker UI.
 * @param tileWidth   Source tile width in pixels (e.g. 18 for Kenney pixel-platformer).
 * @param tileHeight  Source tile height in pixels.
 * @param atlasPath   Path to the packed sprite sheet relative to the libGDX
 *                    internal assets root (e.g. "tilesets/kenney_pixel_platformer/Tilemap/tilemap_packed.png").
 * @param tileMap     Maps (ObstacleKind, ParallaxTheme) → [TileMapping] describing
 *                    which 0-based tile indices to use.  A missing key means the
 *                    caller falls back to the ShapeRenderer path.
 */
data class TilesetPack(
    val id: String,
    val displayName: String,
    val tileWidth: Int,
    val tileHeight: Int,
    val atlasPath: String,
    val tileMap: Map<Pair<ObstacleKind, ParallaxTheme>, TileMapping>
)

/**
 * Describes the tile indices to use when rendering a single [ObstacleKind].
 *
 * For [ObstacleKind.GROUND] the renderer uses [topTileIndex] on the topmost
 * row of a rectangle and [fillTileIndex] for every row below, producing the
 * classic grass-on-dirt look.  For all other kinds only [fillTileIndex] is used.
 *
 * Tile indices are 0-based into the packed atlas, reading left-to-right,
 * top-to-bottom.  Given a sheet with [TilesetPack.tileWidth] columns the pixel
 * coordinates are:
 *   x = (index % columns) * tileWidth
 *   y = (index / columns) * tileHeight
 */
data class TileMapping(
    /** Tile index for the top row of a GROUND obstacle (grass cap). */
    val topTileIndex: Int,
    /** Tile index for interior / fill rows, and the sole tile for non-GROUND kinds. */
    val fillTileIndex: Int
)

/** Convenience: build a [TileMapping] where top and fill share the same index. */
fun uniformMapping(index: Int) = TileMapping(topTileIndex = index, fillTileIndex = index)

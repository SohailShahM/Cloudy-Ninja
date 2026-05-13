package com.sohai.platformer.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.sohai.platformer.Constants
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.RectObstacle

/**
 * Renders terrain obstacles as tiled sprites using the active [TilesetPack].
 *
 * Texture loading is lazy — the atlas is not opened until the first [renderObstacle]
 * call that actually needs it.  If the atlas file is missing at that point, the
 * renderer logs a warning and falls back gracefully (returns `false` so the caller
 * keeps the ShapeRenderer path).
 *
 * **Coordinate convention:** world units (meters), y-up, matching the game camera.
 *
 * **Destination tile size:** each tile is rendered at [TILE_DEST_M] world-meters
 * square (default 0.32 m = 32 px at PPM=100), scaling 18 source pixels → 32
 * destination pixels (≈1.78×).  This gives a chunky pixel-art feel at typical
 * viewport zoom.  Right and top remainders smaller than one full tile are dropped.
 * Obstacles smaller than one full tile on an axis are rendered as a single
 * tile scaled down to fit (see T-174); this prevents thin colliders like the
 * Level0_2 low-ceiling slab from being invisible to the player.
 *
 * @param spriteBatch  Shared batch — caller is responsible for begin/end.
 *                     [renderObstacle] opens its own begin/end block.
 * @param camera       Used to set the projection matrix on the batch.
 */
class TileRenderer(
    private val spriteBatch: SpriteBatch,
    private val camera: OrthographicCamera
) {

    companion object {
        /** Destination size of each tile in world-meters. */
        const val TILE_DEST_M = 0.32f
    }

    // Lazy-loaded atlas and sliced regions.  Null until first successful load.
    private var texture: Texture? = null
    private var regions: Array<Array<TextureRegion>>? = null
    private var loadAttempted = false
    private var loadedPackId: String? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Renders [rect] as tiled sprites for the given [theme].
     *
     * Opens a [SpriteBatch] begin/end block internally.
     *
     * **Thin-obstacle handling (T-174):** when an obstacle is smaller than
     * [TILE_DEST_M] on either axis (e.g. a 24 px tall ceiling slab or a 20 px
     * wide wall column), we still render at least one tile and scale it to fit
     * the obstacle's exact width/height. Previously these obstacles returned
     * `false` with `cols == 0 || rows == 0`, but the [LevelRenderer]
     * ShapeRenderer fallback skips kinds that *have* a tile mapping regardless
     * of whether tiles actually drew — so thin GROUND/WALL bodies rendered
     * nothing at all. Players felt "invisible barriers" near spawn (e.g. the
     * Level0_2 low-ceiling tutorial slab directly above the spawn point) where
     * the collider existed but no sprite was drawn.
     *
     * @return `true` if the obstacle was rendered via tiles; `false` if there
     *         is no tile mapping for this kind/theme (caller should use
     *         ShapeRenderer fallback) or if the atlas could not be loaded.
     */
    fun renderObstacle(rect: RectObstacle, theme: ParallaxTheme): Boolean {
        val pack = TilesetRegistry.current
        val mapping = pack.tileMap[rect.kind to theme] ?: return false

        val regs = ensureTexture(pack) ?: return false

        val cx = rect.body.position.x
        val cy = rect.body.position.y
        val w  = rect.halfWidthPx  / Constants.PPM
        val h  = rect.halfHeightPx / Constants.PPM

        val fullW = w * 2f
        val fullH = h * 2f
        val left   = cx - w
        val bottom = cy - h

        // T-174: clamp to at least 1×1 so thin obstacles (height<32px or
        // width<32px in virtual pixels) still get a sprite. We later scale the
        // tile to (tileW, tileH) below so it covers the obstacle exactly
        // rather than overflowing.
        val cols = (fullW / TILE_DEST_M).toInt().coerceAtLeast(1)
        val rows = (fullH / TILE_DEST_M).toInt().coerceAtLeast(1)

        // Effective per-tile size: shrinks to fit when the obstacle is smaller
        // than a full TILE_DEST_M on either axis; otherwise equal to TILE_DEST_M
        // (preserving the original chunky-pixel look for normal-sized rects).
        val tileW = if (fullW < TILE_DEST_M) fullW else TILE_DEST_M
        val tileH = if (fullH < TILE_DEST_M) fullH else TILE_DEST_M

        val columns = pack.tileWidth.let { tw ->
            val textureWidth = texture?.width ?: return false
            textureWidth / tw
        }

        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.begin()

        for (row in 0 until rows) {
            val tileY = bottom + row * tileH
            val isTopRow = (row == rows - 1)

            for (col in 0 until cols) {
                val tileX = left + col * tileW

                val tileIndex = when {
                    rect.kind == ObstacleKind.GROUND && isTopRow -> mapping.topTileIndex
                    else -> mapping.fillTileIndex
                }

                val tileRow = tileIndex / columns
                val tileCol = tileIndex % columns

                val region = regs[tileRow][tileCol]
                spriteBatch.draw(region, tileX, tileY, tileW, tileH)
            }
        }

        spriteBatch.end()
        return true
    }

    /** Releases the loaded atlas texture.  Call from the owning screen's `dispose()`. */
    fun dispose() {
        texture?.dispose()
        texture = null
        regions = null
        loadAttempted = false
        loadedPackId = null
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns the sliced [TextureRegion] grid for [pack], loading on first use.
     * Returns null if the atlas file is missing or loading fails.
     */
    private fun ensureTexture(pack: TilesetPack): Array<Array<TextureRegion>>? {
        // Re-load if the active pack has changed since last load.
        if (loadedPackId != pack.id) {
            texture?.dispose()
            texture = null
            regions = null
            loadAttempted = false
            loadedPackId = null
        }

        if (loadAttempted) return regions

        loadAttempted = true
        loadedPackId = pack.id

        val file = Gdx.files.internal(pack.atlasPath)
        if (!file.exists()) {
            Gdx.app.error("TileRenderer", "Atlas not found: ${pack.atlasPath} — falling back to ShapeRenderer")
            return null
        }

        return try {
            val tex = Texture(file)
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            texture = tex

            val cols = tex.width  / pack.tileWidth
            val rows = tex.height / pack.tileHeight

            val grid = Array(rows) { r ->
                Array(cols) { c ->
                    TextureRegion(tex,
                        c * pack.tileWidth,
                        r * pack.tileHeight,
                        pack.tileWidth,
                        pack.tileHeight
                    )
                }
            }
            regions = grid
            grid
        } catch (e: Exception) {
            Gdx.app.error("TileRenderer", "Failed to load atlas '${pack.atlasPath}': ${e.message}")
            null
        }
    }
}

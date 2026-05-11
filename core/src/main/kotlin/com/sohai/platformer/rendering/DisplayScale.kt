package com.sohai.platformer.rendering

import com.badlogic.gdx.Gdx
import com.sohai.platformer.Constants

/**
 * Computes the DPI scale between the current physical display and the game's
 * virtual resolution (1280 × 720).  Initialise once in [com.sohai.platformer.Main.create]
 * and again in [com.sohai.platformer.Main.resize] whenever the window size changes.
 *
 * ## Why this matters
 * libGDX's [com.badlogic.gdx.utils.viewport.FitViewport] already upscales the
 * world correctly — but [com.sohai.platformer.FontManager] bakes TrueType glyphs
 * into a bitmap at a fixed pixel size, and [SpriteFactory] draws procedural sprites
 * onto a fixed-size Pixmap.  At 4 K (×3 scale) these bitmaps are GPU-upsampled and
 * look soft.  Using [fontScale] / [spriteScale] to generate them at the native
 * resolution produces crisp output at any display size.
 *
 * ## Scale values
 * | Display  | fontScale | spriteScale |
 * |----------|-----------|-------------|
 * | 720 p    | 1.0       | 1           |
 * | 1080 p   | 1.5       | 1           |
 * | 1440 p   | 2.0       | 2           |
 * | 4 K      | 3.0       | 3           |
 *
 * spriteScale intentionally floors to the nearest integer so sprites remain
 * crisp nearest-neighbour pixel-art at exact multiples.
 */
object DisplayScale {

    /**
     * Floating-point scale applied to font virtual sizes before the TrueType
     * generator rasterises them.  A font requested at size 22 is generated at
     * `round(22 × fontScale)` physical pixels, then rendered at virtual size 22
     * inside the 1280 × 720 viewport — producing a perfectly sharp glyph atlas.
     */
    var fontScale: Float = 1f
        private set

    /**
     * Integer scale applied when upscaling procedural sprite Pixmaps.
     * A 32 × 80 sprite at spriteScale = 3 becomes a 96 × 240 Pixmap rendered
     * at the same 0.32 × 0.80 m world size — exact 1 : 1 physical pixels at 4 K.
     */
    var spriteScale: Int = 1
        private set

    /**
     * Call this from [com.sohai.platformer.Main.create] and again from
     * [com.sohai.platformer.Main.resize] to keep scale values current.
     */
    fun init() {
        val physW = Gdx.graphics.width.toFloat()
        val physH = Gdx.graphics.height.toFloat()

        // Mirror FitViewport: scale is the smaller of the two axis ratios
        val scaleW = physW / Constants.VIRTUAL_WIDTH
        val scaleH = physH / Constants.VIRTUAL_HEIGHT
        val scale  = minOf(scaleW, scaleH).coerceAtLeast(1f)

        fontScale   = scale
        spriteScale = scale.toInt().coerceAtLeast(1)

        Gdx.app.log(
            "DisplayScale",
            "init — physical=${physW.toInt()}×${physH.toInt()}  " +
            "fontScale=%.2f  spriteScale=$spriteScale".format(fontScale)
        )
    }
}

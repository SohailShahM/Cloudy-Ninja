package com.sohai.platformer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

/**
 * Generates BitmapFonts from a TTF at assets/fonts/main.ttf.
 * Falls back to the libGDX default font if the file is absent so the
 * game still runs without a custom font asset.
 *
 * Drop any .ttf into assets/fonts/ and rename it to main.ttf to activate it.
 *
 * NOTE: Most callers should use [getShared] which caches and never disposes —
 * this prevents the per-screen-transition cost of regenerating font atlases.
 * Only call [create] for fonts that need custom colors or one-off use, and
 * remember to dispose them yourself.
 */
object FontManager {
    private const val FONT_PATH = "fonts/main.ttf"

    fun create(size: Int, color: com.badlogic.gdx.graphics.Color = com.badlogic.gdx.graphics.Color.WHITE): BitmapFont {
        if (!Gdx.files.internal(FONT_PATH).exists()) {
            Gdx.app.log("FontManager", "No font at $FONT_PATH — using default BitmapFont")
            return BitmapFont().apply { data.setScale(size / 15f) }
        }
        val generator = FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH))
        val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            this.size = size
            this.color = color
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear
            mono = false
        }
        val font = generator.generateFont(param)
        generator.dispose()
        return font
    }

    // ----- Shared cache -----------------------------------------------------
    // Single shared instance per font size, color = white. Lifetime = app.
    // Saves regenerating FreeType atlases on every screen transition.

    private val sharedCache = mutableMapOf<Int, BitmapFont>()

    /** Get a cached default-color font of the given size. Do NOT dispose the result. */
    fun getShared(size: Int): BitmapFont = sharedCache.getOrPut(size) { create(size) }

    /** Dispose the shared cache. Call exactly once at app shutdown. */
    fun disposeShared() {
        sharedCache.values.forEach { it.dispose() }
        sharedCache.clear()
    }
}

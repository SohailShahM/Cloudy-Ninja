package com.sohai.platformer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.sohai.platformer.rendering.DisplayScale
import kotlin.math.roundToInt

/**
 * Generates BitmapFonts from a TTF at assets/fonts/main.ttf.
 * Falls back to the libGDX default font if the file is absent so the
 * game still runs without a custom font asset.
 *
 * Drop any .ttf into assets/fonts/ and rename it to main.ttf to activate it.
 *
 * ## DPI-aware sizing
 * The `size` parameter is always a **virtual pixel size** (relative to the 1280 × 720
 * virtual resolution).  Internally, [create] multiplies it by [DisplayScale.fontScale]
 * so the generated glyph atlas is baked at the native physical pixel density.  A font
 * requested at virtual size 22 on a 4 K display (scale = 3) is rasterised at 66 px,
 * then rendered at virtual size 22 inside the FitViewport — producing a perfectly sharp
 * glyph with no GPU upsampling artefacts.
 *
 * NOTE: Most callers should use [getShared] which caches and never disposes.
 * Call [clearSharedCache] after a display resolution change so stale atlases
 * are replaced on next use.
 *
 * ## Headless testability seam (T-109)
 * The native/GPU-dependent font-loading codepath is encapsulated by [FontLoader].
 * The default implementation ([DefaultFontLoader]) is wired to `Gdx.files` +
 * `FreeTypeFontGenerator` and is used at runtime. Tests may inject a no-op
 * loader via [setLoaderForTesting] to exercise [create] without a live GL
 * context or `gdx-freetype` JNI natives on the classpath.
 */
object FontManager {
    internal const val FONT_PATH = "fonts/main.ttf"

    /**
     * Loads (or fallback-generates) a [BitmapFont] at a baked physical pixel size.
     *
     * Implementations may touch `Gdx.files`, `Gdx.graphics`, the freetype natives,
     * and GL resources — all of which are unavailable in pure-JVM unit tests.
     * Tests should inject a no-op via [setLoaderForTesting].
     */
    internal interface FontLoader {
        /**
         * @param physicalSize Pre-scaled physical pixel size to rasterise at.
         * @param color        Tint colour baked into the glyph atlas.
         */
        fun load(physicalSize: Int, color: Color): BitmapFont
    }

    /**
     * Production loader: uses `Gdx.files.internal(FONT_PATH)` + `FreeTypeFontGenerator`,
     * falling back to the default [BitmapFont] when the TTF is absent.
     * Behaviour is byte-for-byte identical to the pre-T-109 inline codepath.
     */
    internal object DefaultFontLoader : FontLoader {
        override fun load(physicalSize: Int, color: Color): BitmapFont {
            if (!Gdx.files.internal(FONT_PATH).exists()) {
                Gdx.app.log("FontManager", "No font at $FONT_PATH — using default BitmapFont (scale=${DisplayScale.fontScale})")
                return BitmapFont().apply { data.setScale(physicalSize / 15f) }
            }
            val generator = FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH))
            val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size = physicalSize
                this.color = color
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
                mono = false
            }
            val font = generator.generateFont(param)
            generator.dispose()
            return font
        }
    }

    // Mutable so tests can swap. `internal` keeps it module-private at the source
    // level; the JVM-level access is unchanged from the prior inline call site.
    @Volatile
    private var loader: FontLoader = DefaultFontLoader

    /**
     * Inject a test-only [FontLoader]. Pass `null` to restore [DefaultFontLoader].
     * Package-internal so production code can't accidentally rewire the loader.
     */
    internal fun setLoaderForTesting(replacement: FontLoader?) {
        loader = replacement ?: DefaultFontLoader
    }

    /**
     * Create a font at the given **virtual** size.
     * The physical raster size = `round(size × DisplayScale.fontScale)`.
     *
     * @param size  Virtual pixel size (at 1280 × 720).
     * @param color Tint colour baked into the glyph atlas.
     */
    fun create(size: Int, color: Color = Color.WHITE): BitmapFont {
        val physicalSize = (size * DisplayScale.fontScale).roundToInt().coerceAtLeast(size)
        return loader.load(physicalSize, color)
    }

    // ----- Shared cache -------------------------------------------------------
    // Single shared instance per virtual size, color = white.  Lifetime = until
    // the next clearSharedCache() call (triggered on resolution change).

    private val sharedCache = mutableMapOf<Int, BitmapFont>()

    /** Get (or lazily create) a cached default-colour font. Do NOT dispose the result. */
    fun getShared(size: Int): BitmapFont = sharedCache.getOrPut(size) { create(size) }

    /**
     * Dispose all cached fonts and clear the cache.
     * Call after a display resolution change so the next [getShared] call
     * regenerates atlases at the new physical size.
     */
    fun clearSharedCache() {
        sharedCache.values.forEach { it.dispose() }
        sharedCache.clear()
        Gdx.app.log("FontManager", "Shared font cache cleared (scale=${DisplayScale.fontScale})")
    }

    /** Dispose the shared cache. Call exactly once at app shutdown. */
    fun disposeShared() = clearSharedCache()
}

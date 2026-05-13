package com.sohai.platformer.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable

/**
 * Texture-caching loader + frame-strip slicer for the T-046 sprite-sheet rendering path
 * (T-180 scaffold).
 *
 * This sits parallel to the existing procedural [SpriteFactory] (which generates Pixmaps
 * for the current ShapeRenderer-fallback hero). T-186 will be the first ticket to wire
 * this factory into [com.sohai.platformer.screens.LevelRenderer]; this scaffold ticket
 * intentionally does not modify any existing rendering code.
 *
 * Usage:
 * ```
 * val frames: Array<TextureRegion> = SpriteSheetFactory.loadFrameStrip(
 *     "sprites/luizmelo/martial-hero-1/Idle.png",
 *     frameWidth = 48,
 *     frameHeight = 48,
 * )
 * // ... use frames in a CharacterAtlas / Animation ...
 * SpriteSheetFactory.dispose() // at shutdown
 * ```
 *
 * Lifecycle: [Texture] instances loaded via [texture] are cached and the same instance is
 * returned for repeat calls with the same path. [dispose] disposes every cached texture
 * and clears the cache. The T-186 wire-up will own the call site for [dispose] (currently
 * unowned — see T-180 scaffold rules).
 */
object SpriteSheetFactory : Disposable {

    /** path → Texture cache. */
    private val textureCache = mutableMapOf<String, Texture>()

    /**
     * Texture-construction seam (T-180). The default closes over libGDX's
     * `Gdx.files.internal(path)` + `Texture(FileHandle)`. Tests inject a synthetic loader
     * via [setLoaderForTesting] so no real PNG / GL context is needed.
     */
    internal fun interface TextureLoader {
        fun load(internalPath: String): Texture
    }

    private val defaultLoader = TextureLoader { path -> Texture(Gdx.files.internal(path)) }
    private var loader: TextureLoader = defaultLoader

    /** Test-only: swap in a fake [TextureLoader]. Pass `null` to restore the default. */
    internal fun setLoaderForTesting(testLoader: TextureLoader?) {
        loader = testLoader ?: defaultLoader
    }

    /**
     * Internal-path-relative load. e.g. `"sprites/luizmelo/martial-hero-1/Idle.png"`.
     *
     * Repeat calls with the same [internalPath] return the **same** [Texture] instance.
     * Different paths produce distinct textures.
     */
    fun texture(internalPath: String): Texture =
        textureCache.getOrPut(internalPath) { loader.load(internalPath) }

    /**
     * Slices a horizontal-strip sprite sheet into [TextureRegion]s left-to-right.
     *
     * For a sheet `frameCount * frameWidth` px wide and `frameHeight` px tall, returns
     * `frameCount` regions of size [frameWidth] × [frameHeight] at x = 0, frameWidth,
     * 2·frameWidth, … The sheet width must be an integer multiple of [frameWidth].
     *
     * Returns a libGDX `Array` (not `kotlin.Array`) — matches the type used by
     * libGDX's `Animation` constructor.
     */
    fun stripFrames(texture: Texture, frameWidth: Int, frameHeight: Int): Array<TextureRegion> {
        val count = texture.width / frameWidth
        val out = Array<TextureRegion>(count)
        for (i in 0 until count) {
            out.add(TextureRegion(texture, i * frameWidth, 0, frameWidth, frameHeight))
        }
        return out
    }

    /** Convenience: [texture] + [stripFrames] in one call. */
    fun loadFrameStrip(internalPath: String, frameWidth: Int, frameHeight: Int): Array<TextureRegion> =
        stripFrames(texture(internalPath), frameWidth, frameHeight)

    /** Visible for tests. */
    internal fun cacheSize(): Int = textureCache.size

    /**
     * Disposes every cached [Texture] and clears the cache.
     *
     * The T-186 wire-up will hook this into the [com.sohai.platformer.Main] lifecycle.
     * Until then, this scaffold is dormant and the dispose hook is not load-bearing.
     */
    override fun dispose() {
        textureCache.values.forEach { it.dispose() }
        textureCache.clear()
    }
}

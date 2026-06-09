package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Texture
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Tests for the T-180 [SpriteSheetFactory] scaffold.
 *
 * Isolation strategy:
 *  - [SpriteSheetFactory] is an `object`, so its cache persists across tests; every
 *    scenario calls [SpriteSheetFactory.dispose] to clear it (dispose is also exercised
 *    explicitly in the final scenario).
 *  - The `Texture(FileHandle)` constructor needs a live GL context, so we inject a fake
 *    `TextureLoader` via [SpriteSheetFactory.setLoaderForTesting] that hands back relaxed
 *    mockk-mocks. No PNG files, no GL context, no `Gdx.files` mocking required.
 *  - `stripFrames` is tested directly against a mock texture with a stubbed `width` —
 *    the slicing math is pure arithmetic.
 */
class SpriteSheetFactoryTest : BehaviorSpec({

    afterSpec {
        SpriteSheetFactory.dispose()
        SpriteSheetFactory.setLoaderForTesting(null)
    }

    /** Wipe state at the start of each scenario; afterTest would clear between `then`s. */
    fun resetFactory() {
        SpriteSheetFactory.dispose()
    }

    given("a fake texture loader returning distinct mocks per path") {
        resetFactory()
        val produced = mutableMapOf<String, Texture>()
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { path ->
                produced.getOrPut(path) { mockk<Texture>(relaxed = true) }
            }
        )

        `when`("texture(samePath) is called twice") {
            then("both calls return the SAME Texture instance (cache hit) and the cache holds one entry") {
                val a = SpriteSheetFactory.texture("sprites/luizmelo/martial-hero-1/Idle.png")
                val b = SpriteSheetFactory.texture("sprites/luizmelo/martial-hero-1/Idle.png")
                (a === b) shouldBe true
                SpriteSheetFactory.cacheSize() shouldBe 1
            }
        }
    }

    given("a fake loader handing back a single tracked mock texture") {
        resetFactory()
        val loaded = mockk<Texture>(relaxed = true)
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { _ -> loaded }
        )

        `when`("texture(path) loads the sheet") {
            then("the pixel-art Nearest min/mag filter is forced on the loaded texture") {
                SpriteSheetFactory.texture("sprites/luizmelo/martial-hero-1/Idle.png")
                verify(exactly = 1) {
                    loaded.setFilter(
                        Texture.TextureFilter.Nearest,
                        Texture.TextureFilter.Nearest,
                    )
                }
            }
        }
    }

    given("two distinct sprite paths") {
        resetFactory()
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { _ -> mockk<Texture>(relaxed = true) }
        )

        `when`("texture is called for each distinct path") {
            then("the two results are different Texture instances (cache miss) and cache size is 2") {
                val a = SpriteSheetFactory.texture("sprites/luizmelo/martial-hero-1/Idle.png")
                val b = SpriteSheetFactory.texture("sprites/luizmelo/martial-hero-1/Run.png")
                (a === b) shouldBe false
                SpriteSheetFactory.cacheSize() shouldBe 2
            }
        }
    }

    given("a 384×48 horizontal strip with 48×48 frames (MH1 Idle / Run convention)") {
        val tex = mockk<Texture>(relaxed = true)
        every { tex.width } returns 384
        every { tex.height } returns 48

        `when`("stripFrames is called with frameWidth=48, frameHeight=48") {
            val frames = SpriteSheetFactory.stripFrames(tex, 48, 48)

            then("the strip yields 8 frames (384 / 48)") {
                frames.size shouldBe 8
            }
            then("frame N starts at x = N * 48 and is 48×48") {
                for (i in 0 until frames.size) {
                    frames[i].regionX shouldBe i * 48
                    frames[i].regionY shouldBe 0
                    frames[i].regionWidth shouldBe 48
                    frames[i].regionHeight shouldBe 48
                }
            }
        }
    }

    given("a 144×48 strip with 48×48 frames (MH3 Going Up convention — 3 frames)") {
        val tex = mockk<Texture>(relaxed = true)
        every { tex.width } returns 144
        every { tex.height } returns 48

        `when`("stripFrames is called") {
            val frames = SpriteSheetFactory.stripFrames(tex, 48, 48)

            then("the strip yields exactly 3 frames") {
                frames.size shouldBe 3
            }
        }
    }

    given("a populated cache and a tracking loader") {
        resetFactory()
        val produced = mutableListOf<Texture>()
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { _ ->
                mockk<Texture>(relaxed = true).also { produced += it }
            }
        )

        `when`("dispose() is called on a populated cache") {
            then("the cache is emptied and every cached Texture had dispose() invoked once") {
                SpriteSheetFactory.texture("a.png")
                SpriteSheetFactory.texture("b.png")
                SpriteSheetFactory.texture("c.png")
                SpriteSheetFactory.cacheSize() shouldBe 3

                SpriteSheetFactory.dispose()

                SpriteSheetFactory.cacheSize() shouldBe 0
                produced.forEach { tex ->
                    verify(exactly = 1) { tex.dispose() }
                }
            }
        }
    }

    given("loadFrameStrip end-to-end (load + slice)") {
        resetFactory()
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { _ ->
                mockk<Texture>(relaxed = true).also {
                    every { it.width } returns 288   // MH1 Attack1 = 6 frames × 48
                    every { it.height } returns 48
                }
            }
        )

        `when`("loadFrameStrip is called with a 288-px-wide sheet") {
            val frames = SpriteSheetFactory.loadFrameStrip(
                "sprites/luizmelo/martial-hero-1/Attack1.png", 48, 48,
            )

            then("the result has 6 frames") {
                frames.size shouldBe 6
            }
        }
    }
})

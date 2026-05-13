package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Texture
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

/**
 * Tests for the T-180 [SheetCharacterAtlas.loadLuizMelo] companion-loader scaffold.
 *
 * Each scenario installs a fake [SpriteSheetFactory.TextureLoader] that returns relaxed
 * mock textures whose `width` is set from a path → width map matching the post-T-181
 * downsample inventory (48 px per frame). The loader thus never opens a real PNG and
 * never needs `Gdx.files`.
 */
class SheetCharacterAtlasTest : BehaviorSpec({

    afterSpec {
        SpriteSheetFactory.dispose()
        SpriteSheetFactory.setLoaderForTesting(null)
    }

    /** Inventory-authoritative sheet widths in pixels (48 px / frame × frame count). */
    val sheetWidths: Map<String, Int> = mapOf(
        // MH1: idle 8, run 8, jump 2, fall 2, attack1 6, attack2 6, take-hit 4, death 6
        "sprites/luizmelo/martial-hero-1/Idle.png"     to 384,
        "sprites/luizmelo/martial-hero-1/Run.png"      to 384,
        "sprites/luizmelo/martial-hero-1/Jump.png"     to 96,
        "sprites/luizmelo/martial-hero-1/Fall.png"     to 96,
        "sprites/luizmelo/martial-hero-1/Attack1.png"  to 288,
        "sprites/luizmelo/martial-hero-1/Attack2.png"  to 288,
        "sprites/luizmelo/martial-hero-1/Take Hit.png" to 192,
        "sprites/luizmelo/martial-hero-1/Death.png"    to 288,
        // MH2: idle 4, run 8, jump 2, fall 2, attack1 4, attack2 4, take-hit 3, death 7
        "sprites/luizmelo/martial-hero-2/Idle.png"     to 192,
        "sprites/luizmelo/martial-hero-2/Run.png"      to 384,
        "sprites/luizmelo/martial-hero-2/Jump.png"     to 96,
        "sprites/luizmelo/martial-hero-2/Fall.png"     to 96,
        "sprites/luizmelo/martial-hero-2/Attack1.png"  to 192,
        "sprites/luizmelo/martial-hero-2/Attack2.png"  to 192,
        "sprites/luizmelo/martial-hero-2/Take Hit.png" to 144,
        "sprites/luizmelo/martial-hero-2/Death.png"    to 336,
        // MH3: idle 10, run 8, going-up 3, going-down 3, attack1 7, attack2 6, attack3 9, take-hit 3, death 11
        "sprites/luizmelo/martial-hero-3/Idle.png"       to 480,
        "sprites/luizmelo/martial-hero-3/Run.png"        to 384,
        "sprites/luizmelo/martial-hero-3/Going Up.png"   to 144,
        "sprites/luizmelo/martial-hero-3/Going Down.png" to 144,
        "sprites/luizmelo/martial-hero-3/Attack1.png"    to 336,
        "sprites/luizmelo/martial-hero-3/Attack2.png"    to 288,
        "sprites/luizmelo/martial-hero-3/Attack3.png"    to 432,
        "sprites/luizmelo/martial-hero-3/Take Hit.png"   to 144,
        "sprites/luizmelo/martial-hero-3/Death.png"      to 528,
    )

    fun installInventoryLoader() {
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { path ->
                val w = sheetWidths[path]
                    ?: error("Test inventory has no width for path '$path'")
                mockk<Texture>(relaxed = true).also {
                    every { it.width } returns w
                    every { it.height } returns 48
                }
            }
        )
    }

    given("LuizMelo Martial Hero 1 pack root") {
        installInventoryLoader()

        `when`("loadLuizMelo('sprites/luizmelo/martial-hero-1') runs") {
            val atlas = SheetCharacterAtlas.loadLuizMelo("sprites/luizmelo/martial-hero-1")

            then("attack3 is null (MH1 has no third attack sheet)") {
                atlas.attack3 shouldBe null
            }
            then("attack2 is non-null (MH1 has Attack2.png)") {
                atlas.attack2 shouldNotBe null
            }
            then("frame counts match the inventory") {
                atlas.idle.size shouldBe 8
                atlas.run.size shouldBe 8
                atlas.jump.size shouldBe 2
                atlas.fall.size shouldBe 2
                atlas.attack1.size shouldBe 6
                atlas.attack2!!.size shouldBe 6
                atlas.takeHit.size shouldBe 4
                atlas.death.size shouldBe 6
            }
        }
        SpriteSheetFactory.dispose()
    }

    given("LuizMelo Martial Hero 2 pack root") {
        installInventoryLoader()

        `when`("loadLuizMelo('sprites/luizmelo/martial-hero-2') runs") {
            val atlas = SheetCharacterAtlas.loadLuizMelo("sprites/luizmelo/martial-hero-2")

            then("attack3 is null (MH2 has no third attack sheet)") {
                atlas.attack3 shouldBe null
            }
            then("frame counts match the inventory (note MH2 idle is only 4 frames)") {
                atlas.idle.size shouldBe 4
                atlas.run.size shouldBe 8
                atlas.attack1.size shouldBe 4
                atlas.takeHit.size shouldBe 3
                atlas.death.size shouldBe 7
            }
        }
        SpriteSheetFactory.dispose()
    }

    given("LuizMelo Martial Hero 3 pack root (Going Up / Going Down convention)") {
        installInventoryLoader()

        `when`("loadLuizMelo('sprites/luizmelo/martial-hero-3') runs") {
            val atlas = SheetCharacterAtlas.loadLuizMelo("sprites/luizmelo/martial-hero-3")

            then("attack3 is non-null (MH3 ships a third attack sheet)") {
                atlas.attack3 shouldNotBe null
            }
            then("jump.size == 3 (MH3 'Going Up' has 3 frames per inventory)") {
                atlas.jump.size shouldBe 3
            }
            then("fall.size == 3 (MH3 'Going Down' has 3 frames per inventory)") {
                atlas.fall.size shouldBe 3
            }
            then("attack3 has 9 frames (per inventory)") {
                atlas.attack3!!.size shouldBe 9
            }
            then("idle has 10 frames, death has 11 (MH3 deluxe counts)") {
                atlas.idle.size shouldBe 10
                atlas.death.size shouldBe 11
            }
        }
        SpriteSheetFactory.dispose()
    }

    given("an unknown pack root") {
        installInventoryLoader()

        `when`("loadLuizMelo('sprites/luizmelo/martial-hero-99') is called") {
            then("the loader rejects with a clear error") {
                var threw: Throwable? = null
                try {
                    SheetCharacterAtlas.loadLuizMelo("sprites/luizmelo/martial-hero-99")
                } catch (t: Throwable) {
                    threw = t
                }
                (threw != null) shouldBe true
                (threw!!.message?.contains("Unknown LuizMelo pack root") == true) shouldBe true
            }
        }
        SpriteSheetFactory.dispose()
    }

    given("a loader that produces a wrong sheet width (inventory drift simulation)") {
        // Override Idle.png width so it produces only 4 frames instead of inventory's 8.
        SpriteSheetFactory.setLoaderForTesting(
            SpriteSheetFactory.TextureLoader { path ->
                val w = if (path.endsWith("/Idle.png")) 192 else (sheetWidths[path] ?: 96)
                mockk<Texture>(relaxed = true).also {
                    every { it.width } returns w
                    every { it.height } returns 48
                }
            }
        )

        `when`("loadLuizMelo runs against the drifted Idle.png") {
            then("an integration-time check throws with an inventory-mismatch message") {
                var threw: Throwable? = null
                try {
                    SheetCharacterAtlas.loadLuizMelo("sprites/luizmelo/martial-hero-1")
                } catch (t: Throwable) {
                    threw = t
                }
                (threw != null) shouldBe true
                (threw!!.message?.contains("Inventory mismatch") == true) shouldBe true
            }
        }
        SpriteSheetFactory.dispose()
    }
})

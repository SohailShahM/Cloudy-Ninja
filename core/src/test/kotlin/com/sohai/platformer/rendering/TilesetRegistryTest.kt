package com.sohai.platformer.rendering

import com.sohai.platformer.world.ObstacleKind
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for [TilesetRegistry] and [TilesetPack] — pure-data layer, no libGDX
 * or texture loading required.
 */
class TilesetRegistryTest : BehaviorSpec({

    // -------------------------------------------------------------------------
    // Helper: build a minimal mock TilesetPack for injection
    // -------------------------------------------------------------------------
    fun mockPack(id: String, displayName: String = "Mock Pack"): TilesetPack {
        val mapping = TileMapping(topTileIndex = 0, fillTileIndex = 1)
        val tileMap = buildMap<Pair<ObstacleKind, ParallaxTheme>, TileMapping> {
            for (theme in ParallaxTheme.entries) {
                put(ObstacleKind.GROUND to theme, mapping)
                put(ObstacleKind.WALL   to theme, mapping)
            }
        }
        return TilesetPack(
            id          = id,
            displayName = displayName,
            tileWidth   = 16,
            tileHeight  = 16,
            atlasPath   = "tilesets/mock_$id.png",
            tileMap     = tileMap
        )
    }

    // -------------------------------------------------------------------------
    // 1. Kenney pack is registered by default
    // -------------------------------------------------------------------------
    given("the TilesetRegistry with default registrations") {
        `when`("the Kenney pack id is looked up") {
            val pack = TilesetRegistry.get(TilesetRegistry.KENNEY_ID)

            then("it is present") {
                pack shouldNotBe null
            }
            then("its id matches KENNEY_ID") {
                pack!!.id shouldBe TilesetRegistry.KENNEY_ID
            }
            then("it covers all three themes for GROUND") {
                val p = pack!!
                for (theme in ParallaxTheme.entries) {
                    p.tileMap[ObstacleKind.GROUND to theme] shouldNotBe null
                }
            }
            then("it covers all three themes for WALL") {
                val p = pack!!
                for (theme in ParallaxTheme.entries) {
                    p.tileMap[ObstacleKind.WALL to theme] shouldNotBe null
                }
            }
            then("it covers all three themes for HAZARD") {
                val p = pack!!
                for (theme in ParallaxTheme.entries) {
                    p.tileMap[ObstacleKind.HAZARD to theme] shouldNotBe null
                }
            }
            then("CHECKPOINT has no tile mapping (ShapeRenderer fallback)") {
                val p = pack!!
                for (theme in ParallaxTheme.entries) {
                    p.tileMap[ObstacleKind.CHECKPOINT to theme] shouldBe null
                }
            }
            then("EXIT has no tile mapping (ShapeRenderer fallback)") {
                val p = pack!!
                for (theme in ParallaxTheme.entries) {
                    p.tileMap[ObstacleKind.EXIT to theme] shouldBe null
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. register() + get() round-trip
    // -------------------------------------------------------------------------
    given("a mock TilesetPack registered under id 'test_pack'") {
        val mock = mockPack("test_pack")
        TilesetRegistry.register(mock)

        `when`("it is looked up by id") {
            val retrieved = TilesetRegistry.get("test_pack")

            then("the same instance is returned") {
                retrieved shouldBe mock
            }
        }
        `when`("a non-existent id is looked up") {
            val missing = TilesetRegistry.get("does_not_exist")

            then("null is returned") {
                missing shouldBe null
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. TileMapping: topTileIndex / fillTileIndex are stored correctly
    // -------------------------------------------------------------------------
    given("a TileMapping with distinct top and fill indices") {
        val mapping = TileMapping(topTileIndex = 42, fillTileIndex = 99)

        then("topTileIndex is 42") {
            mapping.topTileIndex shouldBe 42
        }
        then("fillTileIndex is 99") {
            mapping.fillTileIndex shouldBe 99
        }
    }

    // -------------------------------------------------------------------------
    // 4. uniformMapping helper produces equal top and fill
    // -------------------------------------------------------------------------
    given("uniformMapping(77)") {
        val m = uniformMapping(77)

        then("top equals fill equals 77") {
            m.topTileIndex shouldBe 77
            m.fillTileIndex shouldBe 77
        }
    }

    // -------------------------------------------------------------------------
    // 5. Kenney pack ground mapping: top tile != fill tile (grass-on-dirt look)
    // -------------------------------------------------------------------------
    given("the Kenney pack's GROUND mapping for ARID theme") {
        val pack    = TilesetRegistry.get(TilesetRegistry.KENNEY_ID)!!
        val mapping = pack.tileMap[ObstacleKind.GROUND to ParallaxTheme.ARID]!!

        then("top tile index differs from fill tile index (grass cap vs dirt body)") {
            mapping.topTileIndex shouldNotBe mapping.fillTileIndex
        }
        then("top tile index is 22 (Kenney grass-top center)") {
            mapping.topTileIndex shouldBe 22
        }
        then("fill tile index is 122 (Kenney dirt fill)") {
            mapping.fillTileIndex shouldBe 122
        }
    }
})

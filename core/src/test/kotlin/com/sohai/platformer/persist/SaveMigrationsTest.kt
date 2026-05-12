package com.sohai.platformer.persist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Tests for the [SaveMigrations] scaffold (T-113).
 *
 * The production migration chain ([SaveMigrations.STEPS]) is empty at v1
 * introduction — v1 is the baseline, so there's nothing for the chain to do
 * in production. To exercise the codepath itself, every test uses
 * [SaveMigrations.migrateWith] with an explicit, test-only step list.
 *
 * The contract being verified:
 *  - Missing `saveFormatVersion` is treated as the current version (v1).
 *    Existing player saves written before T-113 land MUST still load.
 *  - When a save declares a lower version, the matching step fires.
 *  - When a save declares the current version, the chain is an identity.
 *  - The decoded [GameState] always carries [SaveMigrations.CURRENT_VERSION].
 *  - A malformed version field fails loudly rather than silently corrupting.
 */
class SaveMigrationsTest : BehaviorSpec({

    given("a save written before saveFormatVersion existed (no version field)") {
        // This is the existing-player-saves back-compat scenario from T-113:
        // every save on disk today omits saveFormatVersion. Loading must
        // succeed and the in-memory state must look like a fresh v1 save.
        val legacyJson = """
            {
              "level": "level1_2",
              "characterName": "Ebo",
              "checkpoint": { "levelName": "level1_2", "x": 50.0, "y": 12.0 },
              "stats": { "seedSlamsUsed": 3, "windDashesUsed": 1, "checkpointsReached": 2, "timeSpent": 88.0 },
              "completedLevels": ["level0_0"],
              "totalDeaths": 4,
              "lastPlayed": "2026-04-01"
            }
        """.trimIndent()

        `when`("routed through the production migrate() pipeline") {
            val migrated = SaveMigrations.migrate(legacyJson)

            then("the load succeeds and known fields parse correctly") {
                migrated.level shouldBe "level1_2"
                migrated.characterName shouldBe "Ebo"
                migrated.totalDeaths shouldBe 4
                migrated.lastPlayed shouldBe "2026-04-01"
            }
            then("the decoded state carries CURRENT_VERSION") {
                migrated.saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
            then("absent collections fall back to their declared defaults") {
                migrated.unlockedAchievements shouldBe emptySet()
                migrated.collectedAtlasIds shouldBe emptySet()
                migrated.bestScores shouldBe emptyMap()
                migrated.bestTimes shouldBe emptyMap()
            }
        }
    }

    given("a save that explicitly declares saveFormatVersion = 1") {
        // The default forward case: a save written by current-version code.
        val currentJson = """
            {
              "saveFormatVersion": 1,
              "level": "level2_0",
              "characterName": "Laya",
              "totalStomps": 7
            }
        """.trimIndent()

        `when`("routed through the production migrate() pipeline") {
            val migrated = SaveMigrations.migrate(currentJson)

            then("fields round-trip unchanged") {
                migrated.level shouldBe "level2_0"
                migrated.characterName shouldBe "Laya"
                migrated.totalStomps shouldBe 7
            }
            then("saveFormatVersion remains at CURRENT_VERSION") {
                migrated.saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
        }
    }

    given("a save that declares an older version (saveFormatVersion = 0)") {
        // Scaffold-firing proof: register a fake v0 → v1 step that mutates
        // a known field, then verify the mutation lands in the decoded
        // GameState. This is the migration-codepath contract — v1 in
        // production is a pure no-op, but a hypothetical v0 save must be
        // upgradeable by the chain.
        val v0Json = """
            {
              "saveFormatVersion": 0,
              "level": "old_level_name",
              "characterName": "Ebo",
              "totalDeaths": 12
            }
        """.trimIndent()

        val v0ToV1: SaveMigrations.MigrationStep = SaveMigrations.MigrationStep(
            fromVersion = 0,
            transform = { obj: JsonObject ->
                // Fake migration: rename `level` from `old_level_name` to
                // `level0_0` and bump totalDeaths by 1. Real migrations will
                // look like this — touch specific fields, leave the rest.
                val mutated = obj.toMutableMap()
                if (obj["level"] is JsonPrimitive &&
                    (obj["level"] as JsonPrimitive).content == "old_level_name"
                ) {
                    mutated["level"] = JsonPrimitive("level0_0")
                }
                val oldDeaths = (obj["totalDeaths"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                mutated["totalDeaths"] = JsonPrimitive(oldDeaths + 1)
                JsonObject(mutated)
            }
        )

        `when`("routed through migrateWith() using the fake v0 → v1 step") {
            val migrated = SaveMigrations.migrateWith(v0Json, listOf(v0ToV1))

            then("the v0 → v1 step fired and rewrote `level`") {
                migrated.level shouldBe "level0_0"
            }
            then("the v0 → v1 step fired and bumped totalDeaths") {
                migrated.totalDeaths shouldBe 13
            }
            then("the decoded state carries CURRENT_VERSION, not 0") {
                migrated.saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
            then("untouched fields round-trip unchanged") {
                migrated.characterName shouldBe "Ebo"
            }
        }

        `when`("the chain is empty but the save claims an older version") {
            then("migrateWith() fails loudly rather than silently skipping") {
                shouldThrow<IllegalStateException> {
                    SaveMigrations.migrateWith(v0Json, emptyList())
                }
            }
        }
    }

    given("a save with a malformed saveFormatVersion field") {
        val malformedJson = """
            {
              "saveFormatVersion": "not-a-number",
              "level": "level0_0"
            }
        """.trimIndent()

        `when`("routed through migrate()") {
            then("the load fails loudly rather than coercing to a default") {
                shouldThrow<IllegalStateException> {
                    SaveMigrations.migrate(malformedJson)
                }
            }
        }
    }

    given("applyChain() called directly on a JsonObject") {
        // Direct test of the chain primitive — proves applyChain stamps the
        // current version onto the result even when no steps run.
        val root = buildJsonObject {
            put("level", JsonPrimitive("level0_0"))
            put("characterName", JsonPrimitive("Ebo"))
        }

        `when`("the chain is empty and the input omits saveFormatVersion") {
            val out: JsonObject = SaveMigrations.applyChain(root, emptyList())

            then("the result includes saveFormatVersion stamped to CURRENT_VERSION") {
                val v = out["saveFormatVersion"] as JsonPrimitive
                v.content.toInt() shouldBe SaveMigrations.CURRENT_VERSION
            }
            then("the rest of the JSON is preserved") {
                (out["level"] as JsonPrimitive).content shouldBe "level0_0"
                (out["characterName"] as JsonPrimitive).content shouldBe "Ebo"
            }
        }
    }

    given("CURRENT_VERSION") {
        `when`("inspected") {
            then("matches the default value of GameState.saveFormatVersion") {
                // GameState's default value MUST equal CURRENT_VERSION so
                // writes always emit the current version (T-113 contract).
                GameState().saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
            then("is at least 1 (v1 is the baseline)") {
                (SaveMigrations.CURRENT_VERSION >= 1) shouldBe true
            }
        }
    }
})

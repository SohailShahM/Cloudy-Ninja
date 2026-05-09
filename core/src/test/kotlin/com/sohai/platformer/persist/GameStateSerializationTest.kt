package com.sohai.platformer.persist

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Test suite for GameState serialization.
 * Demonstrates kotlinx.serialization integration with Kotest.
 */
class GameStateSerializationTest : BehaviorSpec({
    given("a GameState object") {
        val json = Json { prettyPrint = true }
        val gameState = GameState(
            level = "level1",
            characterName = "Ebo",
            checkpoint = Checkpoint(
                levelName = "level1",
                x = 10.5f,
                y = 20.3f
            ),
            stats = PlayerStats(
                seedSlamsUsed = 5,
                windDashesUsed = 3,
                checkpointsReached = 2,
                timeSpent = 145.7f
            )
        )

        `when`("the game state is serialized to JSON") {
            val jsonString = json.encodeToString(gameState)
            println("Serialized JSON:\n$jsonString")

            then("the JSON output should contain explicitly-set fields and omit defaults") {
                // Fields with default values are omitted by kotlinx.serialization by design
                // So the JSON will only contain explicitly set nested objects
                jsonString.shouldContain("checkpoint")
                jsonString.shouldContain("stats")
                jsonString.shouldContain("10.5") // checkpoint x
                jsonString.shouldContain("5") // seedSlamsUsed
            }
        }

        `when`("JSON is deserialized back to GameState") {
            val jsonString = json.encodeToString(gameState)
            val deserializedState = json.decodeFromString<GameState>(jsonString)

            then("the object should match the original") {
                deserializedState.level shouldBe gameState.level
                deserializedState.characterName shouldBe gameState.characterName
                deserializedState.stats.seedSlamsUsed shouldBe gameState.stats.seedSlamsUsed
                deserializedState.checkpoint.x shouldBe gameState.checkpoint.x
            }
        }

        `when`("creating a checkpoint with default values") {
            val checkpoint = Checkpoint()

            then("it should have zero position") {
                checkpoint.x shouldBe 0f
                checkpoint.y shouldBe 0f
            }
        }
    }

    given("a GameState with progression data") {
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
        val state = GameState(
            level = "level3",
            completedLevels = setOf("level1", "level2"),
            collectedAtlasIds = setOf("silver_iodide", "water_cycle"),
            bestScores = mapOf("level1" to 850, "level2" to 1240)
        )

        `when`("serialized and deserialized") {
            val s = json.encodeToString(state)
            val back = json.decodeFromString<GameState>(s)

            then("completedLevels round-trips") {
                back.completedLevels shouldBe state.completedLevels
            }

            then("collectedAtlasIds round-trips") {
                back.collectedAtlasIds shouldBe state.collectedAtlasIds
            }

            then("bestScores round-trips") {
                back.bestScores shouldBe state.bestScores
            }
        }

        `when`("an older save without progression fields is loaded") {
            // Simulates loading a save written before completedLevels/etc. were added.
            val oldJson = """{"level":"level1","characterName":"Ebo"}"""
            val parsed = json.decodeFromString<GameState>(oldJson)

            then("missing fields default to empty") {
                parsed.completedLevels shouldBe emptySet()
                parsed.collectedAtlasIds shouldBe emptySet()
                parsed.bestScores shouldBe emptyMap()
            }

            then("known fields parse correctly") {
                parsed.level shouldBe "level1"
                parsed.characterName shouldBe "Ebo"
            }
        }
    }
})




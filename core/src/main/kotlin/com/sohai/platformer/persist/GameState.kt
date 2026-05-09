package com.sohai.platformer.persist

import kotlinx.serialization.Serializable

/**
 * Represents the saveable game state for Cloudy Ninja.
 * Uses kotlinx.serialization for JSON export/import.
 */
@Serializable
data class GameState(
    val level: String = "level1",
    val characterName: String = "Ebo",
    val checkpoint: Checkpoint = Checkpoint(),
    val stats: PlayerStats = PlayerStats(),
    /** IDs of levels the player has completed at least once */
    val completedLevels: Set<String> = emptySet(),
    /** IDs of Cloud Atlas entries collected across all runs */
    val collectedAtlasIds: Set<String> = emptySet(),
    /** Best score per level */
    val bestScores: Map<String, Int> = emptyMap()
)

@Serializable
data class Checkpoint(
    val levelName: String = "level1",
    val x: Float = 0f,
    val y: Float = 0f
)

@Serializable
data class PlayerStats(
    val seedSlamsUsed: Int = 0,
    val windDashesUsed: Int = 0,
    val checkpointsReached: Int = 0,
    val timeSpent: Float = 0f
)


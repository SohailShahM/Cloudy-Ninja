package com.sohai.platformer.persist

import kotlinx.serialization.Serializable

/**
 * Represents the saveable game state for Cloudy Ninja.
 * Uses kotlinx.serialization for JSON export/import.
 */
@Serializable
data class GameState(
    /**
     * Save schema version. Bumped only when a non-additive schema change is made.
     * Existing saves written before this field was introduced (T-113) are missing
     * the key entirely; [SaveMigrations] treats those as v1 (the current version
     * at introduction time). Writes always emit the current version.
     */
    val saveFormatVersion: Int = SaveMigrations.CURRENT_VERSION,
    val level: String = "level0_0",
    val characterName: String = "Ebo",
    val checkpoint: Checkpoint = Checkpoint(),
    val stats: PlayerStats = PlayerStats(),
    /** IDs of levels the player has completed at least once */
    val completedLevels: Set<String> = emptySet(),
    /** IDs of Cloud Atlas entries collected across all runs */
    val collectedAtlasIds: Set<String> = emptySet(),
    /** Best score per level */
    val bestScores: Map<String, Int> = emptyMap(),
    /** Best completion time per level in seconds (lower = better).
     *  Only written during time trial runs; null means never completed in trial. */
    val bestTimes: Map<String, Float> = emptyMap(),
    /** Cumulative deaths across all sessions for this slot */
    val totalDeaths: Int = 0,
    /** ISO-8601 date string of the last save ("yyyy-MM-dd"), empty if never saved */
    val lastPlayed: String = "",
    /** IDs of achievements unlocked across all runs */
    val unlockedAchievements: Set<String> = emptySet(),
    /** Total enemy stomps across all sessions for this slot (used for stomp_10 achievement) */
    val totalStomps: Int = 0
)

@Serializable
data class Checkpoint(
    val levelName: String = "level0_0",
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


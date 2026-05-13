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
    val totalStomps: Int = 0,
    /**
     * T-107: IDs of hidden ("golden") eco-tokens collected across all sessions.
     * Token id is the level id (one hidden token per campaign level: level1,
     * level2, level3). Collecting all 3 unlocks the `collector` achievement.
     * Additive field per T-113 migration scaffold: legacy saves load with an
     * empty set (default value, no migration required).
     */
    val collectedHiddenTokens: Set<String> = emptySet(),
    /**
     * T-146: Per-achievement unlock timestamps (epoch milliseconds, UTC).
     * Recorded by [com.sohai.platformer.progression.AchievementUnlocker.tryUnlock]
     * the moment a new achievement is unlocked. AchievementsScreen renders these
     * as "Unlocked: YYYY-MM-DD" (in user's local timezone) under each unlocked row.
     *
     * Legacy unlocks (in `unlockedAchievements` but absent from this map) render
     * "Unlocked: ?" — additive field per T-113 migration scaffold: pre-T-146 saves
     * load with an empty map (default value, no migration required).
     *
     * Keyed by achievement id (matches [com.sohai.platformer.progression.AchievementRegistry]);
     * value is `System.currentTimeMillis()` captured at unlock.
     */
    val achievementTimestamps: Map<String, Long> = emptyMap(),
    /**
     * T-137: Whether the player has seen the first-run hub tutorial overlay.
     * Shown once on first entry to Level0_0 (Sky Sanctuary), then dismissed
     * for the lifetime of the save slot. Reset only by deleting the save.
     *
     * Additive field per T-113 migration scaffold: legacy saves load with
     * `false` (the default), so existing pre-T-137 players will see the
     * overlay once on their next launch — that's the conscious trade-off
     * for not bumping `saveFormatVersion`. See the T-137 PR for rationale.
     */
    val tutorialSeen: Boolean = false
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


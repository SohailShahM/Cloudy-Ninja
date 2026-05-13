package com.sohai.platformer.progression

import com.badlogic.gdx.Gdx
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.screens.AchievementToast

/**
 * Consolidated achievement-unlock helper. Previously duplicated as private
 * `tryUnlock(...)` methods in `LevelRunState` and `LevelTransitionController`.
 *
 * This is the **impure** side of the unlock pipeline — it persists to
 * [SaveManager] and shows the [AchievementToast]. The corresponding **pure**
 * side lives in [AchievementPredicates].
 *
 * Typical call site:
 * ```
 * val inputs = AchievementInputs(..., enemyDefeatedThisFrame = true, totalStomps = newStomps)
 * for (id in AchievementPredicates.evaluate(inputs)) {
 *     AchievementUnlocker.tryUnlock(id, saveSlotFile, achievementToast)
 * }
 * ```
 *
 * T-128 — extracted from the two duplicate private helpers; preserves their
 * exact persistence + toast behavior:
 *  1. Load save from [saveSlotFile].
 *  2. No-op if [achievementId] is already in `unlockedAchievements`.
 *  3. Save with the id added.
 *  4. Show toast (if [achievementToast] is non-null) and log.
 */
object AchievementUnlocker {

    /**
     * Attempt to unlock an achievement by ID. No-ops if already unlocked.
     * Persists to [saveSlotFile] and shows the toast if [achievementToast]
     * is non-null.
     */
    fun tryUnlock(
        achievementId: String,
        saveSlotFile: String,
        achievementToast: AchievementToast?
    ) {
        val state = SaveManager.loadGame(saveSlotFile)
        if (achievementId in state.unlockedAchievements) return
        val newState = state.copy(
            unlockedAchievements = state.unlockedAchievements + achievementId
        )
        SaveManager.saveGame(newState, saveSlotFile)
        val achievement = AchievementRegistry.get(achievementId) ?: return
        achievementToast?.show(achievement)
        Gdx.app.log("Achievement", "Unlocked: $achievementId — ${achievement.title}")
    }
}

package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.Checkpoint
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.progression.AchievementRegistry
import com.sohai.platformer.rendering.ScreenFade

/**
 * Owns level-completion and level-transition logic: persists scores, shows the
 * [LevelCompleteOverlay], and navigates to the next [GameScreen] (or
 * [VictoryScreen] if this was the last level).
 */
class LevelTransitionController(
    private val level: Level,
    private val game: Game?,
    private val screenFade: ScreenFade,
    private val ecoTokens: List<EcoToken>,
    private val checkpointAutosaveFile: String,
    private val onInputChange: (Stage) -> Unit,
    private val onDispose: () -> Unit,
    private val isTimeTrial: Boolean = false,
    private val onBestTime: ((time: Float, isNewBest: Boolean) -> Unit)? = null,
    private val achievementToast: AchievementToast? = null,
    private val saveSlotFile: String = "save_slot_1.json"
) {

    private var lastTrialTime = 0f
    private var trialIsNewBest = false
    private var lastPrevTime: Float? = null

    /**
     * Unlock an achievement by ID.  No-ops if already unlocked.
     * Shows the toast if [achievementToast] is wired.
     */
    private fun tryUnlock(achievementId: String) {
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

    /** Shows the level-complete UI and persists the score. Returns the overlay. */
    fun startLevelComplete(
        levelTimer: Float,
        score: Int,
        onContinue: () -> Unit
    ): LevelCompleteOverlay {
        SoundManager.play("level_complete")
        screenFade.fadeOut(speed = 0.4f)

        val totalEco    = level.getEcoTokenPositions().size
        val ecoCollected = totalEco - ecoTokens.size
        val overlay = LevelCompleteOverlay(
            levelName    = level.name,
            timeSeconds  = levelTimer,
            score        = score,
            ecoCollected = ecoCollected,
            ecoTotal     = totalEco,
            onContinue   = onContinue
        )
        onInputChange(overlay.stage)

        // Persist completion and best score
        val existing     = SaveManager.loadGame()
        val newCompleted = existing.completedLevels + level.id
        val prevBest     = existing.bestScores[level.id] ?: 0
        val newBestScores = existing.bestScores + (level.id to maxOf(prevBest, score))
        if (isTimeTrial) {
            lastTrialTime  = levelTimer
            val prevTime   = existing.bestTimes[level.id]
            lastPrevTime   = prevTime
            trialIsNewBest = prevTime == null || levelTimer < prevTime
            val newBestTimes = if (trialIsNewBest) existing.bestTimes + (level.id to levelTimer)
                               else existing.bestTimes
            SaveManager.saveGame(existing.copy(
                completedLevels = newCompleted,
                bestScores      = newBestScores,
                bestTimes       = newBestTimes
            ))
            onBestTime?.invoke(lastTrialTime, trialIsNewBest)
            // no autosave to delete — time trial never writes checkpoints

            // Achievement: speed_demon — time trial completed under 2 minutes
            if (levelTimer < 120f) tryUnlock("speed_demon")
        } else {
            SaveManager.saveGame(existing.copy(
                completedLevels = newCompleted,
                bestScores      = newBestScores
            ))
            SaveManager.deleteSave(checkpointAutosaveFile)
        }

        // World/campaign clear achievements (checked after save so newCompleted is persisted)
        if (level.id == "level1") tryUnlock("world_1_clear")
        val allCampaignLevels = setOf("level1", "level2", "level3")
        if (allCampaignLevels.all { it in newCompleted }) tryUnlock("all_clear")

        return overlay
    }

    /** Navigates to the next level or [VictoryScreen] if this was the last one. */
    fun goToNextLevel(score: Int) {
        val nextLevel = LevelManager.getNextLevel(level.id)
        if (nextLevel != null && game != null) {
            game.screen = GameScreen(nextLevel, game)
        } else if (game != null) {
            game.screen = VictoryScreen(
                game, score,
                bestTrialTime  = if (isTimeTrial) lastTrialTime else null,
                isNewTimeBest  = trialIsNewBest,
                priorBestTime  = if (isTimeTrial) lastPrevTime else null
            )
        }
        onDispose()
    }
}

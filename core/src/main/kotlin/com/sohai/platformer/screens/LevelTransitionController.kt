package com.sohai.platformer.screens

import com.badlogic.gdx.Game
import com.badlogic.gdx.scenes.scene2d.Stage
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.levels.Level
import com.sohai.platformer.levels.LevelManager
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.progression.AchievementInputs
import com.sohai.platformer.progression.AchievementPredicates
import com.sohai.platformer.progression.AchievementUnlocker
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
     * T-128: fire any newly-met level-completion achievements via the shared
     * [AchievementPredicates] orchestrator + [AchievementUnlocker] impure
     * helper. Previously this controller had its own private `tryUnlock`
     * duplicating [LevelRunState.tryUnlock]; the consolidation removes that
     * duplication.
     */
    private fun fireCompletionAchievements(
        completedLevels: Set<String>,
        levelId: String,
        levelTimer: Float,
        timeTrialCompleted: Boolean,
    ) {
        val state = SaveManager.loadGame(saveSlotFile)
        val inputs = AchievementInputs(
            atlasSize = state.collectedAtlasIds.size,
            completedLevels = completedLevels,
            collectedHiddenTokens = state.collectedHiddenTokens,
            totalStomps = state.totalStomps,
            unlockedAchievements = state.unlockedAchievements,
            timeTrialCompletedThisFrame = timeTrialCompleted,
            levelCompletedThisFrame = true,
            levelTimer = levelTimer,
            levelId = levelId,
        )
        for (id in AchievementPredicates.evaluate(inputs)) {
            AchievementUnlocker.tryUnlock(id, saveSlotFile, achievementToast)
        }
    }

    /** Shows the level-complete UI and persists the score. Returns the overlay. */
    fun startLevelComplete(
        levelTimer: Float,
        score: Int,
        onContinue: () -> Unit
    ): LevelCompleteOverlay {
        SoundManager.play("level_complete")
        screenFade.fadeToBlack(speed = 0.4f)

        // T-107: hidden tokens are excluded from the level-complete overlay
        // counter to keep the displayed total aligned with the visible-from-
        // the-start "regular" eco-tokens. The hidden-token achievement
        // (`collector`) is tracked separately across runs.
        val totalEco    = level.getEcoTokenPositions().size
        val regularRemaining = ecoTokens.count { !it.isHidden }
        val ecoCollected = totalEco - regularRemaining
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
        } else {
            SaveManager.saveGame(existing.copy(
                completedLevels = newCompleted,
                bestScores      = newBestScores
            ))
            SaveManager.deleteSave(checkpointAutosaveFile)
        }

        // T-128: world_1_clear + all_clear + speed_demon evaluated via pure
        // predicates. The orchestrator runs every level-completion predicate
        // in one pass; speed_demon's `levelTimer < 120f` threshold lives in
        // [AchievementPredicates.speedDemon].
        fireCompletionAchievements(
            completedLevels = newCompleted,
            levelId = level.id,
            levelTimer = levelTimer,
            timeTrialCompleted = isTimeTrial,
        )

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
                bestTrialTime   = if (isTimeTrial) lastTrialTime else null,
                isNewTimeBest   = trialIsNewBest,
                priorBestTime   = if (isTimeTrial) lastPrevTime else null,
                clearedLevelId  = level.id
            )
        }
        onDispose()
    }
}

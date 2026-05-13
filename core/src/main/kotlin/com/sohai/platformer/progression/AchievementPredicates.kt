package com.sohai.platformer.progression

/**
 * Pure-function snapshot of the inputs every achievement-unlock predicate may
 * read. Built once per "fire achievements" call site by [LevelRunState],
 * [LevelTransitionController] and [GameScreen], then handed to
 * [AchievementPredicates.evaluate].
 *
 * All fields default to a no-op value (false / empty / 0) so a caller only sets
 * the inputs relevant to its trigger site (e.g. the stomp-handler sets
 * [enemyDefeatedThisFrame] + [totalStomps] but leaves [snapshotPickedUpThisFrame]
 * at its default).
 *
 * **Predicate evaluation is pure** — no `Gdx.*` access, no save-manager I/O.
 * That makes every predicate testable headless. The save-manager + toast side
 * effects continue to live in the call site's `tryUnlock(...)` helper.
 *
 * T-128 — extracted from the inline `tryUnlock(...)` sites previously embedded
 * in [LevelRunState] / [LevelTransitionController] / [GameScreen]. **Behavior
 * must remain identical**; the predicates encode the same conditions the
 * previous inline sites used, including any per-run gating (e.g. eco_sweep
 * fires only when a level has any eco-tokens AND none remain ungathered).
 */
data class AchievementInputs(
    // ── Cross-run save-state fields (post-update snapshot — i.e. *after* the
    //    trigger event has been applied). The call site reads
    //    `state.copy(...) -> newTotalStomps` etc. and passes the post-update
    //    value here. This keeps the predicates pure and easy to test.

    /** Total stomps across all runs *after* the trigger has been applied. */
    val totalStomps: Int = 0,
    /** Cloud Atlas snapshot ids collected across all runs (post-pickup). */
    val atlasSize: Int = 0,
    /** Level ids completed across all runs (post-completion). */
    val completedLevels: Set<String> = emptySet(),
    /** Hidden eco-token ids collected across all runs (post-pickup). */
    val collectedHiddenTokens: Set<String> = emptySet(),
    /** Achievements already unlocked across all runs (pre-evaluation). */
    val unlockedAchievements: Set<String> = emptySet(),

    // ── Per-trigger event flags. Each call site flips exactly the flags that
    //    correspond to the event it just observed.

    /** First-jump trigger. True iff the player jumped this frame. */
    val jumpFiredThisFrame: Boolean = false,
    /** Enemy-defeat trigger (smog sprite OR drift husk). */
    val enemyDefeatedThisFrame: Boolean = false,
    /** Cleanse-event trigger (Seed Slam hazard cleanse). */
    val cleanseEventThisFrame: Boolean = false,
    /** Eco-sweep trigger (level had eco-tokens, all regular tokens now gone). */
    val ecoSweepReachedThisFrame: Boolean = false,
    /** No-death exit trigger (level exit reached with full spirit health). */
    val noDeathExitThisFrame: Boolean = false,
    /** Storm Sentinel defeated trigger. */
    val bossDefeatedThisFrame: Boolean = false,

    // ── Level-completion context (LevelTransitionController call sites).

    /** Time trial run completed this frame (drives speed_demon). */
    val timeTrialCompletedThisFrame: Boolean = false,
    /** Any non-time-trial level completed this frame (drives world_1_clear / all_clear). */
    val levelCompletedThisFrame: Boolean = false,
    /** Time on the level-complete clock (seconds). */
    val levelTimer: Float = 0f,
    /** Level id being acted on (used by world_1_clear). */
    val levelId: String = "",
)

/**
 * Registry of pure achievement-unlock predicates plus an [evaluate] orchestrator
 * that returns the newly-firing achievement IDs for the given [AchievementInputs].
 *
 * Each predicate is `(AchievementInputs) -> Boolean`. The orchestrator excludes
 * any predicate whose id is already in [AchievementInputs.unlockedAchievements]
 * so callers can pass the returned list directly into their `tryUnlock(id)`
 * helper without re-checking the unlocked set.
 *
 * **Behavior contract:** every predicate corresponds 1:1 to a previous inline
 * `tryUnlock("...")` site. Match the trigger flag to the site:
 *
 * | Achievement       | Trigger flag                       | Threshold check |
 * |-------------------|------------------------------------|-----------------|
 * | first_jump        | jumpFiredThisFrame                 | —               |
 * | first_cleanse     | cleanseEventThisFrame              | —               |
 * | first_enemy       | enemyDefeatedThisFrame             | —               |
 * | stomp_10          | enemyDefeatedThisFrame             | totalStomps >= 10 |
 * | eco_sweep         | ecoSweepReachedThisFrame           | —               |
 * | no_death_run      | noDeathExitThisFrame               | —               |
 * | atlas_half        | (atlasSize >= 6)                   | atlasSize >= 6  |
 * | atlas_full        | (atlasSize >= 12)                  | atlasSize >= 12 |
 * | collector         | (collectedHiddenTokens.size >= 3)  | size >= 3       |
 * | speed_demon       | timeTrialCompletedThisFrame        | levelTimer < 120f |
 * | world_1_clear     | levelCompletedThisFrame            | levelId == "level1" |
 * | all_clear         | levelCompletedThisFrame            | all campaign levels in completedLevels |
 * | boss_defeated     | bossDefeatedThisFrame              | —               |
 */
object AchievementPredicates {

    /** Canonical set of the three campaign levels that all_clear gates on. */
    val CAMPAIGN_LEVELS: Set<String> = setOf("level1", "level2", "level3")

    /** stomp_10 unlock threshold. */
    const val STOMP_10_THRESHOLD: Int = 10
    /** atlas_half unlock threshold. */
    const val ATLAS_HALF_THRESHOLD: Int = 6
    /** atlas_full unlock threshold. */
    const val ATLAS_FULL_THRESHOLD: Int = 12
    /** collector unlock threshold (hidden eco-token ids). */
    const val COLLECTOR_THRESHOLD: Int = 3
    /** speed_demon unlock threshold (seconds; strictly less-than). */
    const val SPEED_DEMON_MAX_SECONDS: Float = 120f

    // ── Per-achievement predicates ────────────────────────────────────────────

    fun firstJump(i: AchievementInputs): Boolean = i.jumpFiredThisFrame

    fun firstCleanse(i: AchievementInputs): Boolean = i.cleanseEventThisFrame

    fun firstEnemy(i: AchievementInputs): Boolean = i.enemyDefeatedThisFrame

    fun stomp10(i: AchievementInputs): Boolean =
        i.enemyDefeatedThisFrame && i.totalStomps >= STOMP_10_THRESHOLD

    fun ecoSweep(i: AchievementInputs): Boolean = i.ecoSweepReachedThisFrame

    fun noDeathRun(i: AchievementInputs): Boolean = i.noDeathExitThisFrame

    fun atlasHalf(i: AchievementInputs): Boolean = i.atlasSize >= ATLAS_HALF_THRESHOLD

    fun atlasFull(i: AchievementInputs): Boolean = i.atlasSize >= ATLAS_FULL_THRESHOLD

    fun collector(i: AchievementInputs): Boolean =
        i.collectedHiddenTokens.size >= COLLECTOR_THRESHOLD

    fun speedDemon(i: AchievementInputs): Boolean =
        i.timeTrialCompletedThisFrame && i.levelTimer < SPEED_DEMON_MAX_SECONDS

    fun world1Clear(i: AchievementInputs): Boolean =
        i.levelCompletedThisFrame && i.levelId == "level1"

    fun allClear(i: AchievementInputs): Boolean =
        i.levelCompletedThisFrame && CAMPAIGN_LEVELS.all { it in i.completedLevels }

    fun bossDefeated(i: AchievementInputs): Boolean = i.bossDefeatedThisFrame

    /**
     * Predicate map keyed by achievement id. Preserves insertion order so
     * [evaluate] returns IDs in a stable, registry-aligned order.
     */
    val PREDICATES: Map<String, (AchievementInputs) -> Boolean> = linkedMapOf(
        "first_jump"     to ::firstJump,
        "first_cleanse"  to ::firstCleanse,
        "first_enemy"    to ::firstEnemy,
        "stomp_10"       to ::stomp10,
        "eco_sweep"      to ::ecoSweep,
        "no_death_run"   to ::noDeathRun,
        "atlas_half"     to ::atlasHalf,
        "atlas_full"     to ::atlasFull,
        "collector"      to ::collector,
        "speed_demon"    to ::speedDemon,
        "world_1_clear"  to ::world1Clear,
        "all_clear"      to ::allClear,
        "boss_defeated"  to ::bossDefeated,
    )

    /**
     * Pure orchestrator. Evaluates every predicate against [inputs] and
     * returns the IDs that fired AND are not already in [inputs.unlockedAchievements].
     *
     * Order of returned IDs follows [PREDICATES] iteration order, which mirrors
     * [AchievementRegistry.ALL] (with the T-107 `collector` slot moved next to
     * the eco-related predicates — registry-displayed order is unaffected).
     */
    fun evaluate(inputs: AchievementInputs): List<String> {
        val fired = mutableListOf<String>()
        for ((id, pred) in PREDICATES) {
            if (id in inputs.unlockedAchievements) continue
            if (pred(inputs)) fired.add(id)
        }
        return fired
    }
}

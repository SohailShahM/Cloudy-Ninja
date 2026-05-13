package com.sohai.platformer.entities

/**
 * T-130: Why the player most recently died.
 *
 * Set on [PlayerController.lastDeathCause] by whichever code path flips
 * [PlayerController.isDead] true; read by the death-recap overlay to label the
 * cause of death. Kept deliberately small (4 buckets) — finer-grained tags
 * (specific enemy archetype, specific hazard) live in the per-source-of-truth
 * stats, not here.
 */
enum class DeathCause {
    /** Lateral contact with an [Enemy] (Smog Sprite, Drift Husk, …). */
    ENEMY,

    /** Touched a fixture tagged "hazard" (spikes, lethal obstacles). */
    HAZARD,

    /** Fell below the world's kill-plane (y < -10/PPM). */
    FALL,

    /** Hit by a boss-spawned projectile or direct boss attack. */
    BOSS_ATTACK,
}

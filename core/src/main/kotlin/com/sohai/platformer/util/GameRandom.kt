package com.sohai.platformer.util

import com.badlogic.gdx.math.RandomXS128

/**
 * Seeded RNG singleton for gameplay-affecting random calls.
 *
 * Wraps libGDX's [RandomXS128] so that any code that needs deterministic
 * replay behaviour (T-A3) can be re-seeded via [setSeed]. At session start
 * the wrapper is seeded from [System.nanoTime], matching the previous
 * unseeeded-[com.badlogic.gdx.math.MathUtils] behaviour. Once a replay
 * seed is injected the sequence becomes fully deterministic.
 *
 * Usage mirrors the [com.badlogic.gdx.math.MathUtils] helpers it replaces:
 *   - [nextFloat] → `MathUtils.random()` (0 inclusive, 1 exclusive)
 *   - [range]     → `MathUtils.random(min, max)`
 *   - [rangeInt]  → `MathUtils.random(min, max)` for Int
 *   - [bool]      → `MathUtils.randomBoolean()`
 */
object GameRandom {
    private var rng = RandomXS128(System.nanoTime())

    /** Replace the RNG seed. Call at level-start when recording, or with the
     *  stored seed when playing back a replay. */
    fun setSeed(seed: Long) {
        rng = RandomXS128(seed)
    }

    /** Returns a float in [0, 1). */
    fun nextFloat(): Float = rng.nextFloat()

    /** Returns a float in [min, max], matching `MathUtils.random(min, max)`. */
    fun range(min: Float, max: Float): Float = min + rng.nextFloat() * (max - min)

    /** Returns an int in [min, max] inclusive, matching `MathUtils.random(min, max)`. */
    fun rangeInt(min: Int, max: Int): Int = min + (rng.nextInt(max - min + 1))

    /** Returns a random boolean, matching `MathUtils.randomBoolean()`. */
    fun bool(): Boolean = rng.nextBoolean()
}

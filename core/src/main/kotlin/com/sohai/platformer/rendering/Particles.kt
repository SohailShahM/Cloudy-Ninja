package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color

/**
 * Pure-data particle-system foundation (T-158).
 *
 * This file is a minimal, headless particle utility. It deliberately performs
 * **no rendering** and contains **no `Gdx.*` calls** in its hot path — the only
 * libGDX dependency is [com.badlogic.gdx.graphics.Color], which is itself a
 * plain (r, g, b, a) value class with no OpenGL state. Render glue is the
 * caller's responsibility (a future ticket will wire it into a screen).
 *
 * ## Why a second particle utility?
 *
 * The repo already ships [ParticleSystem] — a pool-backed system with a
 * `render(ShapeRenderer)` method that draws filled circles. That class owns
 * a render path and is wired into `LevelRunState` for stomp-defeat smoke
 * bursts, cleanse bursts, and collect sparkles.
 *
 * This file ([ParticleEmitter] + [Particle]) is a **clean-room foundation**
 * for future effects that prefer a data-only API:
 *   - `particles: List<Particle>` accessor (no reflection-poking in tests),
 *   - palette-driven `spawn(x, y, count, ttl, palette)` helper,
 *   - linear velocity + gravity + ttl decay, no rendering, no pooling.
 *
 * Pairs the same "pure-math utility lives in `rendering/`, caller renders it"
 * shape as [ScreenShake]. Future tickets can graduate this into the in-game
 * render path or replace [ParticleSystem] entirely; this PR adds the
 * foundation only and does not wire any caller.
 *
 * ## Update model
 *
 * Each [update] tick advances every live particle by `delta` seconds:
 *
 * ```
 *   ttl -= delta                        (particles with ttl <= 0 are pruned)
 *   vy  += gravity * delta              (positive gravity accelerates downward)
 *   x   += vx * delta
 *   y   += vy * delta
 * ```
 *
 * Gravity convention: **positive `gravity` pulls particles in the `-y`
 * direction** (i.e. downward in world-space, where `+y` is up). A value of
 * `0f` makes particles float in a straight line — the default for the
 * emitter is `0f` so a caller that doesn't want gravity gets predictable
 * behaviour without setting a flag.
 *
 * Pruning runs in-place on the backing list during [update]; the public
 * [particles] accessor returns an unmodifiable view so callers can iterate
 * without fear of ConcurrentModificationException from internal mutation.
 *
 * ## Thread-safety
 *
 * Single-threaded. Like [ScreenShake], must only be touched from the game
 * thread. No locking on the internal list.
 */

/**
 * A single particle. Mutable so that [ParticleEmitter.update] can advance
 * its position and ttl in-place without allocating a new instance per frame.
 *
 * @property x        world-x position (meters)
 * @property y        world-y position (meters)
 * @property vx       horizontal velocity (m/s)
 * @property vy       vertical velocity (m/s). +y is up.
 * @property color    rgba colour for this particle. Held by reference — the
 *                    emitter copies the palette entry on spawn so a caller
 *                    mutating their palette later won't retroactively recolour
 *                    live particles.
 * @property ttl      seconds remaining before this particle is pruned. Starts
 *                    at the spawn-time `ttl` and decays linearly via [update].
 * @property gravity  downward acceleration (m/s²). Defaults to `0f` (floats).
 */
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var ttl: Float,
    var gravity: Float = 0f,
)

/**
 * Emitter that owns a list of live [Particle]s. Has no rendering surface —
 * read [particles] and draw from caller code.
 *
 * The class is intentionally simple: no pooling, no capacity cap, no
 * spatial partitioning. Future tickets can layer those on top once a real
 * call-site exists and load-shapes are known.
 */
class ParticleEmitter {

    /**
     * Internal mutable list, advanced and pruned in-place by [update].
     * Exposed externally as an unmodifiable view via [particles].
     */
    private val _particles: MutableList<Particle> = mutableListOf()

    /**
     * Read-only view of the live particles. Iteration order is spawn order
     * (oldest first); callers MUST NOT cast this back to a mutable list — the
     * underlying list is mutated during [update].
     *
     * Returns a `List<Particle>` (not a `Sequence`) so callers can take
     * `.size` cheaply for HUD / debug overlays.
     */
    val particles: List<Particle> get() = _particles

    /**
     * Advance every live particle by [delta] seconds and prune any whose
     * `ttl` has fallen to zero or below.
     *
     * Safe to call with `delta = 0` (no-op for live particles; doesn't prune
     * anything since none of them tick down). Negative `delta` is permitted
     * but not meaningful — particles will gain ttl and rewind — so the caller
     * is responsible for clamping `delta >= 0` upstream.
     *
     * Implementation detail: we use a removing iterator rather than two
     * passes (filter-then-update) so we don't allocate a new list per frame.
     */
    fun update(delta: Float) {
        val it = _particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.ttl -= delta
            if (p.ttl <= 0f) {
                it.remove()
                continue
            }
            p.vy -= p.gravity * delta
            p.x  += p.vx * delta
            p.y  += p.vy * delta
        }
    }

    /**
     * Spawn [count] particles in a radial fan from `(x, y)`, cycling through
     * [palette] for per-particle colour.
     *
     * Velocity layout: angles are evenly spaced around the full circle so
     * `count = 8` gives a clean 8-spoke burst at 45° intervals. Speed is a
     * fixed [BURST_SPEED] for now — predictable for tests, easy to extend
     * with a `speed` parameter later if a caller needs it.
     *
     * The [palette] is indexed by `particleIndex mod palette.size` so a
     * 3-colour palette spawning 9 particles yields 3 particles of each
     * colour. Each particle gets a fresh [Color] copy so subsequent palette
     * mutations don't leak into already-spawned particles.
     *
     * @param x       spawn origin x (world meters)
     * @param y       spawn origin y (world meters)
     * @param count   number of particles to add. Must be `> 0`; non-positive
     *                values are silently ignored. No upper cap — the caller
     *                is trusted to keep counts reasonable.
     * @param ttl     lifetime for every particle in this burst (seconds).
     *                Non-positive values are silently ignored.
     * @param palette colours to cycle through. Must be non-empty; an empty
     *                palette is silently ignored (no spawn).
     * @param gravity optional downward acceleration applied to every particle
     *                in the burst. Defaults to `0f` (no gravity).
     */
    fun spawn(
        x: Float,
        y: Float,
        count: Int,
        ttl: Float,
        palette: List<Color>,
        gravity: Float = 0f,
    ) {
        if (count <= 0) return
        if (ttl <= 0f) return
        if (palette.isEmpty()) return

        val twoPi = (2.0 * Math.PI).toFloat()
        for (i in 0 until count) {
            val angle = twoPi * i.toFloat() / count.toFloat()
            val vx = BURST_SPEED * kotlin.math.cos(angle)
            val vy = BURST_SPEED * kotlin.math.sin(angle)
            _particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    // Defensive copy — the caller's palette entry stays under
                    // their control and our particle keeps its own colour.
                    color = Color(palette[i % palette.size]),
                    ttl = ttl,
                    gravity = gravity,
                )
            )
        }
    }

    /**
     * Drop every live particle immediately. Useful for level resets / death
     * transitions so a fresh level doesn't start with stale visual debris.
     */
    fun clear() {
        _particles.clear()
    }

    companion object {
        /**
         * Speed (m/s) used for every particle spawned by [spawn]. Picked to
         * match the "small visual juice" footprint used by the existing
         * stomp-smoke burst — large enough to read, small enough not to
         * outrun the camera. Tunable from a future ticket; lives as a
         * `const` for now so tests can assert exact numeric outcomes.
         */
        const val BURST_SPEED: Float = 3f
    }
}

package com.sohai.platformer.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants

/**
 * Storm Sentinel — the Level 3 boss.
 *
 * A floating storm-cloud entity that attacks with downward lightning bolts and
 * a horizontal energy sweep. Defeated by landing 3 of Ebo's Seed Slam water
 * droplets on its body.
 *
 * ## Physics
 * StaticBody with a sensor fixture so water droplets pass through and register
 * a contact event without physical deflection. Projectiles from the boss use
 * the existing [Projectile] / [com.sohai.platformer.entities.Projectile] class
 * and are spawned via [onSpawnProjectile] (never inside a world step or contact
 * callback).
 *
 *   - Fixture categoryBits = [Constants.BIT_HAZARD]   (matches droplet mask)
 *   - Fixture maskBits     = [Constants.BIT_DROPLET]   (only droplets activate contact)
 *   - Fixture userData     = `"boss_sentinel"`          (WorldContactListener key)
 *   - Body userData        = `this`
 *
 * ## Phase loop (repeats until defeated)
 * ```
 *   REST → LIGHTNING_TELEGRAPH → LIGHTNING → REST
 *   REST → SWEEP_TELEGRAPH     → SWEEP     → REST   (alternates each cycle)
 * ```
 *
 * @param world       The live Box2D world.
 * @param x           Spawn centre X in world metres.
 * @param y           Spawn centre Y in world metres.
 * @param arenaLeft   Left boundary of the boss arena (world metres).
 * @param arenaRight  Right boundary of the boss arena (world metres).
 */
class StormSentinel(
    world: World,
    val x: Float,
    val y: Float,
    val arenaLeft: Float,
    val arenaRight: Float
) {

    // ── State machine ─────────────────────────────────────────────────────────

    enum class Phase {
        REST,
        LIGHTNING_TELEGRAPH, LIGHTNING,
        SWEEP_TELEGRAPH, SWEEP
    }

    var hp = 3
        private set
    var isDead = false
        private set

    var phase = Phase.REST
        private set

    private var phaseTimer   = REST_DURATION
    private var attackIndex  = 0   // incremented each cycle; even=lightning, odd=sweep
    private var hitFlashTimer = 0f
    private var sweepGoesRight = true

    // ── Telegraph data (read by LevelRenderer for warning indicators) ─────────

    /** X positions (world metres) where lightning will strike. Populated during LIGHTNING_TELEGRAPH. */
    val lightningWarnings: List<Float> get() = _lightningWarnings
    private val _lightningWarnings = mutableListOf<Float>()

    /** X position of the incoming sweep (world metres). */
    var sweepWarningX = 0f
        private set
    /** +1 = sweeping rightward, -1 = sweeping leftward. */
    var sweepWarningDir = 1
        private set

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /**
     * Called whenever the boss fires a projectile.
     * Parameters: world-space (x, y, vx, vy).
     * Must be set before [update] is called.
     *
     * GameScreen wires this to [com.sohai.platformer.screens.LevelRunState.spawnProjectile].
     */
    var onSpawnProjectile: ((x: Float, y: Float, vx: Float, vy: Float) -> Unit)? = null

    /** Called when HP reaches 0. GameScreen sets `runState.levelCompleted = true`. */
    var onDefeated: (() -> Unit)? = null

    // ── Box2D body ────────────────────────────────────────────────────────────

    val body: Body

    init {
        val bdef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(x, y)
        }
        body = world.createBody(bdef)
        body.userData = this

        val shape = CircleShape().apply { radius = BODY_RADIUS }
        val fdef = FixtureDef().apply {
            this.shape   = shape
            isSensor     = true
            filter.categoryBits = Constants.BIT_HAZARD
            filter.maskBits     = Constants.BIT_DROPLET
        }
        body.createFixture(fdef).userData = "boss_sentinel"
        shape.dispose()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Register one hit from a water droplet.
     * Idempotent once dead; triggers [onDefeated] on final hit.
     */
    fun takeDamage() {
        if (isDead) return
        hp--
        hitFlashTimer = HIT_FLASH_DURATION
        if (hp <= 0) {
            hp = 0
            isDead = true
            onDefeated?.invoke()
        }
    }

    /** True while the boss should render in hit-flash (white) colour. */
    fun isFlashing(): Boolean = hitFlashTimer > 0f

    fun update(delta: Float) {
        if (isDead) return
        if (hitFlashTimer > 0f) hitFlashTimer -= delta
        phaseTimer -= delta
        if (phaseTimer <= 0f) advancePhase()
    }

    // ── Phase transitions ─────────────────────────────────────────────────────

    private fun advancePhase() {
        when (phase) {
            Phase.REST -> {
                if (attackIndex % 2 == 0) startLightningTelegraph()
                else                       startSweepTelegraph()
                attackIndex++
            }
            Phase.LIGHTNING_TELEGRAPH -> {
                phase      = Phase.LIGHTNING
                phaseTimer = LIGHTNING_DURATION
                fireLightning()
            }
            Phase.LIGHTNING -> {
                phase      = Phase.REST
                phaseTimer = REST_DURATION
                _lightningWarnings.clear()
            }
            Phase.SWEEP_TELEGRAPH -> {
                phase      = Phase.SWEEP
                phaseTimer = SWEEP_DURATION
                fireSweep()
            }
            Phase.SWEEP -> {
                phase      = Phase.REST
                phaseTimer = REST_DURATION
            }
        }
    }

    private fun startLightningTelegraph() {
        phase      = Phase.LIGHTNING_TELEGRAPH
        phaseTimer = LIGHTNING_TELEGRAPH_DURATION
        _lightningWarnings.clear()
        val margin = 0.8f
        repeat(LIGHTNING_COUNT) {
            _lightningWarnings.add(MathUtils.random(arenaLeft + margin, arenaRight - margin))
        }
    }

    private fun startSweepTelegraph() {
        phase          = Phase.SWEEP_TELEGRAPH
        phaseTimer     = SWEEP_TELEGRAPH_DURATION
        sweepGoesRight = MathUtils.randomBoolean()
        sweepWarningDir = if (sweepGoesRight) 1 else -1
        sweepWarningX   = if (sweepGoesRight) arenaLeft else arenaRight
    }

    private fun fireLightning() {
        for (wx in _lightningWarnings) {
            onSpawnProjectile?.invoke(wx, y, 0f, -LIGHTNING_SPEED)
        }
    }

    private fun fireSweep() {
        val vx     = if (sweepGoesRight) SWEEP_SPEED else -SWEEP_SPEED
        val startX = if (sweepGoesRight) arenaLeft   else arenaRight
        onSpawnProjectile?.invoke(startX, SWEEP_Y_METRES, vx, 0f)
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Draw the boss and all active telegraph indicators.
     * Called from [com.sohai.platformer.screens.LevelRenderer.renderWorld] inside
     * an open ShapeRenderer Filled block.
     */
    fun draw(renderer: ShapeRenderer) {
        if (isDead) return

        val bodyCol  = if (isFlashing()) FLASH_COLOR else BODY_COLOR
        val innerCol = if (isFlashing()) FLASH_COLOR else INNER_COLOR

        // Main cloud blob
        renderer.color = bodyCol
        renderer.circle(x, y, BODY_RADIUS)

        // Bright inner core
        renderer.color = innerCol
        renderer.circle(x, y, BODY_RADIUS * 0.42f)

        // HP pip row below the body
        for (i in 0 until MAX_HP) {
            renderer.color = if (i < hp) HP_FULL_COLOR else HP_EMPTY_COLOR
            renderer.circle(x + (i - 1) * 0.24f, y - BODY_RADIUS - 0.14f, 0.07f)
        }

        // Phase-specific telegraph overlays
        when (phase) {
            Phase.LIGHTNING_TELEGRAPH -> drawLightningTelegraph(renderer)
            Phase.SWEEP_TELEGRAPH     -> drawSweepTelegraph(renderer)
            else                      -> Unit
        }
    }

    private fun drawLightningTelegraph(renderer: ShapeRenderer) {
        // Expanding yellow ring on the boss body
        val ratio = (1f - phaseTimer / LIGHTNING_TELEGRAPH_DURATION).coerceIn(0f, 1f)
        renderer.color = TELEGRAPH_LIGHTNING_COLOR
        renderer.circle(x, y, BODY_RADIUS + 0.10f + ratio * 0.25f)

        // Target cross-hairs on the arena floor
        renderer.color = LIGHTNING_WARN_COLOR
        for (wx in _lightningWarnings) {
            val groundSurface = 0.40f   // arena floor top = 40 px / 100 PPM
            renderer.circle(wx, groundSurface + 0.05f, 0.12f)
        }
    }

    private fun drawSweepTelegraph(renderer: ShapeRenderer) {
        // Orange halo on the boss body
        renderer.color = TELEGRAPH_SWEEP_COLOR
        renderer.circle(x, y, BODY_RADIUS + 0.15f)

        // Arrow marker on the side the sweep starts from
        renderer.color = SWEEP_WARN_COLOR
        renderer.circle(sweepWarningX, SWEEP_Y_METRES, 0.14f)
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** World-space radius for both the visual circle and the sensor hitbox. */
        const val BODY_RADIUS = 0.45f   // metres ≈ 45 px

        // Timing (seconds)
        const val REST_DURATION               = 2.5f
        const val LIGHTNING_TELEGRAPH_DURATION = 1.5f
        const val LIGHTNING_DURATION           = 0.3f
        const val SWEEP_TELEGRAPH_DURATION     = 1.2f
        const val SWEEP_DURATION               = 0.3f
        const val HIT_FLASH_DURATION           = 0.3f

        // Attack parameters
        const val LIGHTNING_COUNT  = 3
        const val LIGHTNING_SPEED  = 14f      // m/s (downward)
        const val SWEEP_SPEED      = 7f       // m/s (horizontal)
        /** World-space Y for the sweep projectile — just above a standing player (~190 px). */
        const val SWEEP_Y_METRES   = 1.9f

        private const val MAX_HP = 3

        // Colours (allocated once, not per frame)
        private val BODY_COLOR              = Color(0.28f, 0.12f, 0.52f, 1.00f)
        private val INNER_COLOR             = Color(0.68f, 0.42f, 1.00f, 1.00f)
        private val FLASH_COLOR             = Color(1.00f, 1.00f, 1.00f, 1.00f)
        private val HP_FULL_COLOR           = Color(0.80f, 0.38f, 1.00f, 1.00f)
        private val HP_EMPTY_COLOR          = Color(0.28f, 0.22f, 0.33f, 0.60f)
        private val TELEGRAPH_LIGHTNING_COLOR = Color(1.00f, 0.88f, 0.18f, 0.55f)
        private val LIGHTNING_WARN_COLOR    = Color(1.00f, 0.88f, 0.18f, 0.80f)
        private val TELEGRAPH_SWEEP_COLOR   = Color(0.90f, 0.32f, 0.08f, 0.55f)
        private val SWEEP_WARN_COLOR        = Color(0.90f, 0.32f, 0.08f, 0.85f)
    }
}

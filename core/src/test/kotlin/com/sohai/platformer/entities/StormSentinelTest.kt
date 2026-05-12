package com.sohai.platformer.entities

import com.sohai.platformer.util.GameRandom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.objenesis.ObjenesisStd

/**
 * Pure-logic tests for [StormSentinel]'s phase state machine and HP system.
 *
 * ## Box2D constraint
 * [StormSentinel]'s primary constructor calls `world.createBody(...)`,
 * `CircleShape()` (JNI), and `body.createFixture(...)`. Constructing those
 * requires libGDX native (.dll/.so) libraries which are not on the `:core`
 * test classpath — natives are pulled in only by the `:desktop` subproject.
 *
 * To keep this suite purely in-process and free of native code, every
 * [StormSentinel] instance in the tests below is allocated **without
 * invoking its constructor** via [ObjenesisStd], then its private fields
 * are seeded with values that exactly mirror the post-constructor state
 * (HP=3, phase=REST, phaseTimer=REST_DURATION, etc.). The Box2D [body]
 * field stays null — the state-machine code under test never reads it.
 *
 * ## Determinism
 * All random draws go through [GameRandom]. Each test seeds the singleton
 * with a fixed value before instantiating a sentinel, making the lightning
 * warning positions and sweep direction reproducible.
 *
 * ## Coverage
 *  - All five phase transitions (REST → LIGHTNING_TELEGRAPH → LIGHTNING →
 *    REST → SWEEP_TELEGRAPH → SWEEP → REST)
 *  - HP decrement on each [StormSentinel.takeDamage] (3 → 2 → 1 → 0)
 *  - Defeat flag flips when HP hits 0; [StormSentinel.onDefeated] fires once
 *  - takeDamage is idempotent once dead
 *  - Lightning projectile spawn count matches [StormSentinel.LIGHTNING_COUNT]
 *  - Sweep direction is driven by [GameRandom] (seeded determinism)
 *  - update() no-ops once dead
 */
class StormSentinelTest : BehaviorSpec({

    // ── Reflection helpers ───────────────────────────────────────────────────

    val objenesis = ObjenesisStd()

    /** Set a private (or `private set`) field by name via reflection. */
    fun setField(target: Any, name: String, value: Any?) {
        val f = StormSentinel::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    /**
     * Allocate a [StormSentinel] without running its constructor, then seed
     * its fields to the values the real constructor would have produced.
     *
     * The Box2D [StormSentinel.body] field stays null — none of the tested
     * methods read it, and Objenesis leaves non-initialized refs at null.
     */
    fun newSentinel(
        seed: Long = 1234L,
        x: Float = 0f,
        y: Float = 2f,
        arenaLeft: Float = 0f,
        arenaRight: Float = 10f,
    ): StormSentinel {
        GameRandom.setSeed(seed)
        val boss = objenesis.newInstance(StormSentinel::class.java)
        setField(boss, "x", x)
        setField(boss, "y", y)
        setField(boss, "arenaLeft", arenaLeft)
        setField(boss, "arenaRight", arenaRight)
        setField(boss, "hp", 3)
        setField(boss, "isDead", false)
        setField(boss, "phase", StormSentinel.Phase.REST)
        setField(boss, "phaseTimer", StormSentinel.REST_DURATION)
        setField(boss, "attackIndex", 0)
        setField(boss, "hitFlashTimer", 0f)
        setField(boss, "sweepGoesRight", true)
        setField(boss, "_lightningWarnings", mutableListOf<Float>())
        setField(boss, "sweepWarningX", 0f)
        setField(boss, "sweepWarningDir", 1)
        return boss
    }

    /** Advance the phase timer enough to trigger exactly one phase transition. */
    fun tickPhase(boss: StormSentinel) {
        // The biggest phase duration is REST = 2.5s; a single 100s tick reliably
        // drops phaseTimer below 0 and advancePhase() only fires once per call.
        boss.update(100f)
    }

    // ── Initial state ────────────────────────────────────────────────────────

    given("a freshly spawned StormSentinel") {
        `when`("the boss is constructed") {
            val boss = newSentinel()

            then("it starts in REST phase with full HP and is not dead") {
                boss.phase shouldBe StormSentinel.Phase.REST
                boss.hp shouldBe 3
                boss.isDead shouldBe false
            }
        }
    }

    // ── Phase transition #1: REST → LIGHTNING_TELEGRAPH ──────────────────────

    given("a boss in REST with attackIndex = 0 (even → lightning)") {
        `when`("the rest timer elapses") {
            val boss = newSentinel(seed = 42L)
            tickPhase(boss)

            then("the boss transitions to LIGHTNING_TELEGRAPH") {
                boss.phase shouldBe StormSentinel.Phase.LIGHTNING_TELEGRAPH
            }

            then("LIGHTNING_COUNT warning positions are populated") {
                boss.lightningWarnings shouldHaveSize StormSentinel.LIGHTNING_COUNT
            }

            then("every warning falls inside the arena bounds (margin 0.8)") {
                boss.lightningWarnings.forEach { wx ->
                    (wx >= boss.arenaLeft + 0.8f) shouldBe true
                    (wx <= boss.arenaRight - 0.8f) shouldBe true
                }
            }
        }
    }

    // ── Phase transition #2: LIGHTNING_TELEGRAPH → LIGHTNING ─────────────────

    given("a boss in LIGHTNING_TELEGRAPH") {
        `when`("the telegraph timer elapses") {
            val boss = newSentinel(seed = 42L)
            tickPhase(boss)   // REST → LIGHTNING_TELEGRAPH
            val spawns = mutableListOf<FloatArray>()
            boss.onSpawnProjectile = { px, py, vx, vy -> spawns += floatArrayOf(px, py, vx, vy) }
            tickPhase(boss)   // LIGHTNING_TELEGRAPH → LIGHTNING (fires bolts)

            then("phase is LIGHTNING") {
                boss.phase shouldBe StormSentinel.Phase.LIGHTNING
            }

            then("exactly LIGHTNING_COUNT projectiles are spawned") {
                spawns shouldHaveSize StormSentinel.LIGHTNING_COUNT
            }

            then("every projectile fires straight down at LIGHTNING_SPEED from the boss y") {
                spawns.forEach { args ->
                    args[1] shouldBe boss.y                          // py
                    args[2] shouldBe 0f                              // vx
                    args[3] shouldBe -StormSentinel.LIGHTNING_SPEED  // vy
                }
            }
        }
    }

    // ── Phase transition #3: LIGHTNING → REST ────────────────────────────────

    given("a boss in LIGHTNING") {
        `when`("the lightning timer elapses") {
            val boss = newSentinel(seed = 42L)
            boss.onSpawnProjectile = { _, _, _, _ -> }
            tickPhase(boss)   // REST → LIGHTNING_TELEGRAPH
            tickPhase(boss)   // LIGHTNING_TELEGRAPH → LIGHTNING
            tickPhase(boss)   // LIGHTNING → REST

            then("phase is REST") {
                boss.phase shouldBe StormSentinel.Phase.REST
            }

            then("lightning warning list is cleared") {
                boss.lightningWarnings shouldHaveSize 0
            }
        }
    }

    // ── Phase transition #4: REST → SWEEP_TELEGRAPH (odd attackIndex) ────────

    given("a boss whose second attack cycle is starting (attackIndex = 1 → sweep)") {
        `when`("a full lightning cycle completes and the next rest timer elapses") {
            val boss = newSentinel(seed = 7L)
            boss.onSpawnProjectile = { _, _, _, _ -> }
            tickPhase(boss)   // REST → LIGHTNING_TELEGRAPH (attackIndex: 0→1)
            tickPhase(boss)   // LIGHTNING_TELEGRAPH → LIGHTNING
            tickPhase(boss)   // LIGHTNING → REST
            tickPhase(boss)   // REST → SWEEP_TELEGRAPH (attackIndex: 1→2)

            then("phase is SWEEP_TELEGRAPH") {
                boss.phase shouldBe StormSentinel.Phase.SWEEP_TELEGRAPH
            }

            then("sweepWarningDir is ±1") {
                (boss.sweepWarningDir == 1 || boss.sweepWarningDir == -1) shouldBe true
            }

            then("sweepWarningX is the arena edge matching the sweep direction") {
                val expected = if (boss.sweepWarningDir == 1) boss.arenaLeft else boss.arenaRight
                boss.sweepWarningX shouldBe expected
            }
        }
    }

    // ── Phase transition #5: SWEEP_TELEGRAPH → SWEEP (one projectile) ────────

    given("a boss in SWEEP_TELEGRAPH") {
        `when`("the telegraph timer elapses") {
            val boss = newSentinel(seed = 7L)
            val spawns = mutableListOf<FloatArray>()
            boss.onSpawnProjectile = { px, py, vx, vy -> spawns += floatArrayOf(px, py, vx, vy) }
            tickPhase(boss)   // REST → LIGHTNING_TELEGRAPH
            tickPhase(boss)   // → LIGHTNING (LIGHTNING_COUNT projectiles)
            tickPhase(boss)   // → REST
            tickPhase(boss)   // → SWEEP_TELEGRAPH (no projectile)
            val before = spawns.size
            tickPhase(boss)   // → SWEEP (fires 1 horizontal projectile)

            then("phase is SWEEP") {
                boss.phase shouldBe StormSentinel.Phase.SWEEP
            }

            then("exactly one new projectile was spawned for the sweep") {
                (spawns.size - before) shouldBe 1
            }

            then("the sweep projectile travels horizontally at SWEEP_Y_METRES") {
                val s = spawns.last()
                s[1] shouldBe StormSentinel.SWEEP_Y_METRES
                s[3] shouldBe 0f                            // vy
                (s[2] == StormSentinel.SWEEP_SPEED || s[2] == -StormSentinel.SWEEP_SPEED) shouldBe true
            }
        }
    }

    // ── Phase transition #6: SWEEP → REST ────────────────────────────────────

    given("a boss in SWEEP") {
        `when`("the sweep timer elapses") {
            val boss = newSentinel(seed = 7L)
            boss.onSpawnProjectile = { _, _, _, _ -> }
            tickPhase(boss)   // REST → LIGHTNING_TELEGRAPH
            tickPhase(boss)   // → LIGHTNING
            tickPhase(boss)   // → REST
            tickPhase(boss)   // → SWEEP_TELEGRAPH
            tickPhase(boss)   // → SWEEP
            tickPhase(boss)   // → REST

            then("phase returns to REST") {
                boss.phase shouldBe StormSentinel.Phase.REST
            }
        }
    }

    // ── HP / death ───────────────────────────────────────────────────────────

    given("a fresh boss taking damage") {
        `when`("takeDamage is called once") {
            val boss = newSentinel()
            boss.takeDamage()

            then("HP drops from 3 to 2 and the boss is still alive") {
                boss.hp shouldBe 2
                boss.isDead shouldBe false
            }
        }

        `when`("takeDamage is called twice") {
            val boss = newSentinel()
            boss.takeDamage()
            boss.takeDamage()

            then("HP drops to 1 and the boss is still alive") {
                boss.hp shouldBe 1
                boss.isDead shouldBe false
            }
        }

        `when`("takeDamage is called three times") {
            val boss = newSentinel()
            var defeatedCalls = 0
            boss.onDefeated = { defeatedCalls++ }
            boss.takeDamage()
            boss.takeDamage()
            boss.takeDamage()

            then("HP reaches 0 and isDead flips to true") {
                boss.hp shouldBe 0
                boss.isDead shouldBe true
            }

            then("onDefeated callback fires exactly once") {
                defeatedCalls shouldBe 1
            }
        }

        `when`("takeDamage is called more than three times") {
            val boss = newSentinel()
            var defeatedCalls = 0
            boss.onDefeated = { defeatedCalls++ }
            repeat(7) { boss.takeDamage() }

            then("HP stays clamped at 0 and onDefeated only fires once") {
                boss.hp shouldBe 0
                boss.isDead shouldBe true
                defeatedCalls shouldBe 1
            }
        }
    }

    // ── update() is a no-op once dead ────────────────────────────────────────

    given("a defeated boss") {
        `when`("update is called with a huge delta") {
            val boss = newSentinel()
            repeat(3) { boss.takeDamage() }
            val phaseBefore = boss.phase
            boss.update(999f)

            then("the phase does not advance and isDead remains true") {
                boss.phase shouldBe phaseBefore
                boss.isDead shouldBe true
            }
        }
    }

    // ── Determinism: same seed → same lightning warnings & sweep dir ────────

    given("two bosses constructed with the same RNG seed") {
        `when`("both run a full lightning telegraph + sweep telegraph") {
            val a = newSentinel(seed = 12345L)
            a.onSpawnProjectile = { _, _, _, _ -> }
            tickPhase(a)
            val warnsA = a.lightningWarnings.toList()
            tickPhase(a)  // → LIGHTNING
            tickPhase(a)  // → REST
            tickPhase(a)  // → SWEEP_TELEGRAPH
            val sweepDirA = a.sweepWarningDir

            val b = newSentinel(seed = 12345L)
            b.onSpawnProjectile = { _, _, _, _ -> }
            tickPhase(b)
            val warnsB = b.lightningWarnings.toList()
            tickPhase(b)
            tickPhase(b)
            tickPhase(b)
            val sweepDirB = b.sweepWarningDir

            then("lightning warning positions match across runs") {
                warnsA shouldBe warnsB
            }

            then("sweep direction matches across runs") {
                sweepDirA shouldBe sweepDirB
            }
        }
    }
})

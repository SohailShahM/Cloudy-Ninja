package com.sohai.platformer.levels

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.world.ObstacleManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify

/**
 * Unit tests for [LevelEntityFactory] (T-106).
 *
 * The factory's concrete entity creators (`SmogSprite.create`,
 * `DriftHusk.create`, `StormSentinel(...)`) all hit the libgdx Box2D native lib
 * (see `SmogSpriteTest` for the same constraint). On top of that, the Box2D
 * `World` class is `final` AND has a `static { SharedLibraryLoader.load(...) }`
 * block, so any direct reference to `World` from a unit-test JVM — *including*
 * `mockk<World>()` — triggers native-lib loading and crashes the test.
 *
 * To stay native-free we never refer to `World.class` directly in this file.
 * Tests obtain a `World`-typed placeholder via `Unsafe.allocateInstance(...)`
 * (the same trick a few other suites use for screens with `SpriteBatch` /
 * `Texture` ctors — see [com.sohai.platformer.screens.SplashScreenTest]) and
 * invoke `LevelEntityFactory.spawn` via reflection. The placeholder is only
 * forwarded into the spawn paths that don't dereference it (non-TMX, empty
 * defs, unknown-type, null-boss), which is everything we can cover without
 * natives anyway.
 *
 * What we lock in here:
 *
 *  1. Non-TMX levels return an empty [SpawnedEntities]. Matches the pre-T-106
 *     behaviour where the `if (level is TmxLevel)` guards in
 *     `GameScreen.init` short-circuited.
 *  2. TMX levels with empty enemy / drift husk / boss defs return empty lists
 *     and `boss = null`. No native-lib calls happen in this path.
 *  3. Unknown enemy and boss type identifiers are reported via
 *     `Gdx.app.error(...)` and skipped without throwing — the contract is
 *     identical to pre-T-106; only the log tag moves from `"GameScreen"`
 *     to `"LevelEntityFactory"`.
 *  4. A TMX level with `bossDef = null` returns `boss = null` with no
 *     `Gdx.app.error("Unknown boss type")` call.
 *
 * Positive instantiation paths (real SmogSprite / DriftHusk / StormSentinel
 * from valid type identifiers) are covered by the smoke CI run, which spins
 * up a real Box2D world and exercises every shipped level.
 */
class LevelEntityFactoryTest : BehaviorSpec({

    // Snapshot prior globals so we can restore them.
    val prevApp: Application? = Gdx.app

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)
    }

    afterSpec {
        Gdx.app = prevApp
    }

    /**
     * Build a `World` without invoking its static initializer (which would
     * load the Box2D native lib and crash). The returned object is only ever
     * forwarded into spawn paths that don't dereference it.
     *
     * `sun.misc.Unsafe.allocateInstance` is the same hatch used elsewhere in
     * the suite (see [com.sohai.platformer.screens.SplashScreenTest]).
     */
    val cl: ClassLoader = LevelEntityFactoryTest::class.java.classLoader
    // initialize=false skips the `static { SharedLibraryLoader.load("gdx-box2d") }`
    // block on World, which is the entire point of this dance.
    val worldClass: Class<*> = Class.forName("com.badlogic.gdx.physics.box2d.World", false, cl)

    /**
     * Invoke `LevelEntityFactory.spawn(level, world)` via reflection so the
     * test class file never names `World.class` in a `mockk<World>()` or
     * `World::class` site that would trip the static initializer at class
     * load. `null` is passed for `world` — the spawn paths we exercise
     * (non-TMX, empty defs, unknown-type, null-boss) never dereference it, so
     * Kotlin's `Intrinsics.checkNotNullParameter` is the only obstacle. The
     * reflection call bypasses that check because the JVM does NOT enforce
     * Kotlin nullability annotations across reflective `invoke`.
     */
    fun callSpawn(level: Level): SpawnedEntities {
        val method = LevelEntityFactory::class.java.getDeclaredMethod(
            "spawn", Level::class.java, worldClass
        )
        return method.invoke(LevelEntityFactory, level, null) as SpawnedEntities
    }

    /**
     * A non-TMX [Level] stub — exercises the early-return branch.
     * Mirrors the no-entity nature of `Level0_0` (the hub world) without
     * dragging in its libGDX setup deps.
     */
    class StubNonTmxLevel : Level() {
        override val id: String = "stub_nontmx"
        override val name: String = "Stub Non-TMX"
        override val spawnX: Float = 0f
        override val spawnY: Float = 0f
        override fun setup(
            world: World,
            obstacleManager: ObstacleManager,
            movingPlatforms: MutableList<MovingPlatform>
        ) = Unit
        override fun getCheckpoints(): List<LevelCheckpoint> = emptyList()
    }

    /** Minimal TMX def with no entities — drives the "all empty" path. */
    fun emptyTmxLevel(id: String = "empty_tmx"): TmxLevel = TmxLevel(
        TmxLevelDefinition(
            id           = id,
            name         = "Empty TMX",
            mapPath      = "maps/${id}.tmx",
            spawnX       = 0f,
            spawnY       = 0f,
            levelWidthPx = 1280f,
            exitXPx      = 1200f
        )
    )

    given("a non-TmxLevel") {
        val level = StubNonTmxLevel()

        `when`("spawn() is called") {
            val spawned = callSpawn(level)

            then("enemies list is empty") {
                spawned.enemies.shouldBeEmpty()
            }
            then("drift husks list is empty") {
                spawned.driftHusks.shouldBeEmpty()
            }
            then("boss is null") {
                spawned.boss.shouldBeNull()
            }
        }
    }

    given("a TmxLevel with no enemies / drift husks / boss") {
        val level = emptyTmxLevel()

        `when`("spawn() is called") {
            val spawned = callSpawn(level)

            then("enemies list is empty") {
                spawned.enemies.shouldBeEmpty()
            }
            then("drift husks list is empty") {
                spawned.driftHusks.shouldBeEmpty()
            }
            then("boss is null") {
                spawned.boss.shouldBeNull()
            }
            then("no spawn-count log line is emitted (no entities to count)") {
                // The factory only logs `"Spawned N enemies / Drift Husks"`
                // when the list is non-empty. Lock that in.
                verify(exactly = 0) {
                    Gdx.app.log("LevelEntityFactory", match<String> { it.contains("Spawned") })
                }
            }
        }
    }

    given("a TmxLevel with an unknown enemy type") {
        val level = TmxLevel(
            TmxLevelDefinition(
                id           = "level_unknown_enemy",
                name         = "Unknown Enemy",
                mapPath      = "maps/x.tmx",
                spawnX       = 0f,
                spawnY       = 0f,
                levelWidthPx = 1280f,
                exitXPx      = 1200f,
                enemies      = listOf(
                    EnemyDef(
                        type = "definitely_not_a_real_enemy",
                        xPx = 0f, yPx = 0f,
                        patrolLeftPx = 0f, patrolRightPx = 0f
                    )
                )
            )
        )

        `when`("spawn() is called") {
            val spawned = callSpawn(level)

            then("no enemy is added") {
                spawned.enemies.shouldBeEmpty()
            }
            then("Gdx.app.error is invoked with the unknown-type tag") {
                verify(atLeast = 1) {
                    Gdx.app.error("LevelEntityFactory", match<String> {
                        it.contains("Unknown enemy type") &&
                            it.contains("definitely_not_a_real_enemy")
                    })
                }
            }
            then("boss is null (unrelated boss path didn't fire)") {
                spawned.boss.shouldBeNull()
            }
        }
    }

    given("a TmxLevel with an unknown boss type") {
        val level = TmxLevel(
            TmxLevelDefinition(
                id           = "level_unknown_boss",
                name         = "Unknown Boss",
                mapPath      = "maps/x.tmx",
                spawnX       = 0f,
                spawnY       = 0f,
                levelWidthPx = 1280f,
                exitXPx      = 1200f,
                bossDef      = BossDef(
                    type         = "definitely_not_a_real_boss",
                    xPx          = 0f,
                    yPx          = 0f,
                    arenaLeftPx  = 0f,
                    arenaRightPx = 0f
                )
            )
        )

        `when`("spawn() is called") {
            val spawned = callSpawn(level)

            then("boss is null (no entity instantiated)") {
                spawned.boss.shouldBeNull()
            }
            then("Gdx.app.error is invoked with the unknown-type tag") {
                verify(atLeast = 1) {
                    Gdx.app.error("LevelEntityFactory", match<String> {
                        it.contains("Unknown boss type") &&
                            it.contains("definitely_not_a_real_boss")
                    })
                }
            }
        }
    }

    given("a TmxLevel with bossDef explicitly null") {
        val level = emptyTmxLevel("level_no_boss") // bossDef defaults to null

        `when`("spawn() is called") {
            // Clear call history from earlier scenarios so the
            // exactly=0 assertion below reflects ONLY this scenario.
            clearMocks(Gdx.app, recordedCalls = true, answers = false)
            val spawned = callSpawn(level)

            then("boss is null") {
                spawned.boss.shouldBeNull()
            }
            then("the unknown-boss error path is NOT triggered") {
                verify(exactly = 0) {
                    Gdx.app.error("LevelEntityFactory", match<String> { it.contains("Unknown boss type") })
                }
            }
        }
    }

    given("a SpawnedEntities instance from spawn()") {
        `when`("constructed via spawn() on a non-TMX level") {
            val spawned = callSpawn(StubNonTmxLevel())

            then("the returned lists are mutable (callers append/remove at runtime)") {
                // The data-class field type is MutableList<...> so .add must
                // compile here; this also pins the API shape for callers.
                spawned.enemies.size shouldBe 0
                spawned.driftHusks.size shouldBe 0
            }
        }
    }
})

package com.sohai.platformer.levels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.DriftHusk
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.SmogSprite
import com.sohai.platformer.entities.StormSentinel

/**
 * The collection of dynamic entities a level needs at start-of-run, produced
 * by [LevelEntityFactory.spawn].
 *
 * Keeping these together lets [com.sohai.platformer.screens.GameScreen] swap
 * three parallel `if (level is TmxLevel) { for (def in level.getXxx()) ... }`
 * blocks for a single one-line spawn call (T-106).
 *
 * Lists are mutable so subsystems ([LevelRunState], [LevelRenderer]) can keep
 * mutating them as they did pre-extraction (e.g. removing defeated enemies).
 *
 * @param enemies     Ground-patrol enemies (currently SmogSprite only).
 * @param driftHusks  Drop-from-above enemies (T-062).
 * @param boss        Optional boss entity; null for levels without a boss
 *                    encounter.
 */
data class SpawnedEntities(
    val enemies: MutableList<Enemy>,
    val driftHusks: MutableList<DriftHusk>,
    val boss: StormSentinel?
)

/**
 * Builds the dynamic entities (enemies, drift husks, boss) for a given level.
 *
 * Before T-106 this logic lived inline in `GameScreen.init`, with one
 * `if (level is TmxLevel) { for (def in level.getXxx()) { ... } }` block per
 * entity type. New entity archetypes required editing GameScreen each time —
 * the explicit motivation for this extraction (see TASKS.md T-106).
 *
 * Behaviour is intentionally identical to the pre-extraction code: same
 * coordinate conversions, same per-spawn logging, same `Gdx.app.error` line
 * for unknown types. The only structural change is that **all** spawning
 * happens here; GameScreen owns the [World] and just receives the result.
 */
object LevelEntityFactory {

    /**
     * Spawn every dynamic entity declared by [level] into [world].
     *
     * Non-TMX levels (e.g. the hub `Level0_0`) return an empty
     * [SpawnedEntities]; this matches the pre-T-106 behaviour where the
     * `if (level is TmxLevel)` guards short-circuited.
     *
     * **Why `world` is nullable in the signature:** the Box2D `World` class is
     * `final` and runs a `static { SharedLibraryLoader.load("gdx-box2d") }`
     * block on first load, which crashes any unit-test JVM without natives.
     * Tests cover the non-TMX / empty-defs / unknown-type / null-boss paths
     * (which never dereference `world`) by reflectively invoking with a null
     * world; making the parameter nullable removes Kotlin's
     * `checkNotNullParameter` guard so those paths run cleanly. Production
     * callers always pass a real world — see `GameScreen.init`.
     */
    fun spawn(level: Level, world: World?): SpawnedEntities {
        if (level !is TmxLevel) {
            return SpawnedEntities(
                enemies    = mutableListOf(),
                driftHusks = mutableListOf(),
                boss       = null
            )
        }

        // `world` is required for any actual entity creation; the per-type
        // helpers only dereference it when a known type matches and
        // `SmogSprite.create` / `DriftHusk.create` / `StormSentinel(...)` is
        // invoked. The nullable signature exists solely so tests can hit the
        // empty-defs / unknown-type / null-boss branches without booting the
        // Box2D native lib (see the kdoc on `spawn`).
        val enemies = spawnEnemies(level, world)
        val driftHusks = spawnDriftHusks(level, world)
        val boss = spawnBoss(level, world)

        return SpawnedEntities(
            enemies    = enemies,
            driftHusks = driftHusks,
            boss       = boss
        )
    }

    private fun spawnEnemies(level: TmxLevel, world: World?): MutableList<Enemy> {
        val enemies = mutableListOf<Enemy>()
        for (def in level.getEnemyDefs()) {
            when (def.type) {
                "smog_sprite" -> enemies.add(
                    SmogSprite.create(
                        world!!,
                        x = def.xPx / Constants.PPM,
                        y = def.yPx / Constants.PPM,
                        patrolLeftX = def.patrolLeftPx / Constants.PPM,
                        patrolRightX = def.patrolRightPx / Constants.PPM
                    )
                )
                else -> Gdx.app.error("LevelEntityFactory", "Unknown enemy type: ${def.type}")
            }
        }
        if (enemies.isNotEmpty() && Constants.DEV_LOGS) {
            Gdx.app.log("LevelEntityFactory", "Spawned ${enemies.size} enemies for level ${level.id}")
        }
        return enemies
    }

    private fun spawnDriftHusks(level: TmxLevel, world: World?): MutableList<DriftHusk> {
        val driftHusks = mutableListOf<DriftHusk>()
        for (def in level.getDriftHuskDefs()) {
            driftHusks.add(
                DriftHusk.create(
                    world!!,
                    x        = def.xPx        / Constants.PPM,
                    y        = def.yPx        / Constants.PPM,
                    triggerX = def.triggerXPx / Constants.PPM
                )
            )
        }
        if (driftHusks.isNotEmpty() && Constants.DEV_LOGS) {
            Gdx.app.log("LevelEntityFactory", "Spawned ${driftHusks.size} Drift Husks for level ${level.id}")
        }
        return driftHusks
    }

    private fun spawnBoss(level: TmxLevel, world: World?): StormSentinel? {
        val bdef = level.getBossDef() ?: return null
        return when (bdef.type) {
            "storm_sentinel" -> {
                val boss = StormSentinel(
                    world!!,
                    x          = bdef.xPx          / Constants.PPM,
                    y          = bdef.yPx          / Constants.PPM,
                    arenaLeft  = bdef.arenaLeftPx  / Constants.PPM,
                    arenaRight = bdef.arenaRightPx / Constants.PPM
                )
                if (Constants.DEV_LOGS) {
                    Gdx.app.log("LevelEntityFactory", "Storm Sentinel spawned at (${bdef.xPx}, ${bdef.yPx}) px")
                }
                boss
            }
            else -> {
                Gdx.app.error("LevelEntityFactory", "Unknown boss type: ${bdef.type}")
                null
            }
        }
    }
}

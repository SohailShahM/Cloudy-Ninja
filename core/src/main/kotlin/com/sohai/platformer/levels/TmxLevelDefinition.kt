package com.sohai.platformer.levels

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.world.MapLevelLoader
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Data-driven definition for a standard TMX-backed level.
 *
 * All parameters that differ across Level1/Level2/Level3 live here;
 * the shared body-creation logic is handled by [TmxLevel].
 *
 * To add a new level, register a new [TmxLevelDefinition] in
 * [LevelRegistry.ALL] — no new class required.
 *
 * @param id            Unique level id (e.g. "level1").
 * @param name          Human-readable level name shown in the UI.
 * @param mapPath       Asset path for the .tmx file (e.g. "maps/level1.tmx").
 * @param spawnX        Player spawn position X in virtual pixels.
 * @param spawnY        Player spawn position Y in virtual pixels.
 * @param levelWidthPx  Approximate level width in virtual pixels (used to clamp camera).
 * @param exitXPx       X position of the level-exit sensor in virtual pixels.
 * @param ecoTokens     Eco-token positions in virtual pixels (converted to meters internally).
 * @param snapshots     Pairs of (CloudAtlasLibrary id, x px, y px) for snapshot pickups.
 * @param checkpoints   Static checkpoint definitions for this level.
 * @param enemies       Enemy placement definitions for this level.
 * @param bossDef       Optional boss definition; null for levels with no boss encounter.
 */
data class TmxLevelDefinition(
    val id: String,
    val name: String,
    val mapPath: String,
    val spawnX: Float,
    val spawnY: Float,
    val levelWidthPx: Float,
    val exitXPx: Float,
    val ecoTokens: List<Vector2> = emptyList(),
    val snapshots: List<SnapshotDef> = emptyList(),
    val checkpoints: List<LevelCheckpoint> = emptyList(),
    val enemies: List<EnemyDef> = emptyList(),
    /** Optional boss encounter definition. Null = no boss in this level. */
    val bossDef: BossDef? = null,
    /** Music track base name (loaded from `audio/music/{musicTrack}.wav`). */
    val musicTrack: String = "ambient_arid"
)

/**
 * Lightweight description of an enemy placement inside a level.
 *
 * Positions are in virtual pixels; [TmxLevel] / [LevelRunState] converts to
 * world metres by dividing by [Constants.PPM].
 *
 * @param type         Enemy type identifier (e.g. "smog_sprite").
 * @param xPx          Spawn X in virtual pixels.
 * @param yPx          Spawn Y in virtual pixels.
 * @param patrolLeftPx Left patrol waypoint X in virtual pixels.
 * @param patrolRightPx Right patrol waypoint X in virtual pixels.
 */
data class EnemyDef(
    val type: String,
    val xPx: Float,
    val yPx: Float,
    val patrolLeftPx: Float,
    val patrolRightPx: Float
)

/**
 * Lightweight description of a boss encounter inside a level.
 *
 * Positions are in virtual pixels; [TmxLevel] / [GameScreen] converts to
 * world metres by dividing by [Constants.PPM].
 *
 * @param type          Boss type identifier (e.g. "storm_sentinel").
 * @param xPx           Boss spawn centre X in virtual pixels.
 * @param yPx           Boss spawn centre Y in virtual pixels.
 * @param arenaLeftPx   Left boundary of the boss arena in virtual pixels.
 * @param arenaRightPx  Right boundary of the boss arena in virtual pixels.
 */
data class BossDef(
    val type: String,
    val xPx: Float,
    val yPx: Float,
    val arenaLeftPx: Float,
    val arenaRightPx: Float
)

/**
 * Lightweight description of a snapshot pickup inside a level.
 *
 * @param atlasId  Key used with [CloudAtlasLibrary.get] to look up the entry.
 * @param xPx      X position in virtual pixels.
 * @param yPx      Y position in virtual pixels.
 */
data class SnapshotDef(
    val atlasId: String,
    val xPx: Float,
    val yPx: Float
)

// ---------------------------------------------------------------------------
// TmxLevel — concrete Level backed by a TmxLevelDefinition
// ---------------------------------------------------------------------------

/**
 * A [Level] that derives all its configuration from a [TmxLevelDefinition].
 *
 * The shared body-creation pattern (load TMX, add pit floor, add exit sensor)
 * lives here once, eliminating the near-identical duplication that existed
 * across Level1/Level2/Level3.
 */
class TmxLevel(private val def: TmxLevelDefinition) : Level() {
    override val id: String         get() = def.id
    override val name: String       get() = def.name
    override val spawnX: Float      get() = def.spawnX
    override val spawnY: Float      get() = def.spawnY
    override val levelWidthPx: Float get() = def.levelWidthPx
    override val musicTrack: String  get() = def.musicTrack

    override fun setup(
        world: World,
        obstacleManager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>
    ) {
        // flipY=true: libGDX's TmxMapLoader already flips rectangle Y to y-up internally.
        // MapLevelLoader's own flip cancels that out and correctly maps Tiled y-down
        // coordinates onto the Box2D y-up world (ground at y=0 → bottom of physics world).
        MapLevelLoader.load(def.mapPath, obstacleManager, movingPlatforms, world, flipY = true)

        // Pit safety net so the player lands somewhere before the death threshold
        obstacleManager.addRectNormalized(
            "pit_floor", ObstacleKind.GROUND,
            0.5f,
            -80f / Constants.VIRTUAL_HEIGHT,
            0.5f,
            20f / Constants.VIRTUAL_HEIGHT
        )

        addExitSensor(obstacleManager, def.exitXPx)
    }

    override fun getCheckpoints(): List<LevelCheckpoint> = def.checkpoints

    override fun getEcoTokenPositions(): List<Vector2> =
        def.ecoTokens.map { Vector2(it.x / Constants.PPM, it.y / Constants.PPM) }

    fun getEnemyDefs(): List<EnemyDef> = def.enemies

    fun getBossDef(): BossDef? = def.bossDef

    override fun getSnapshotPickups(world: World): List<SnapshotPickup> =
        def.snapshots.mapNotNull { snap ->
            CloudAtlasLibrary.get(snap.atlasId)?.let { entry ->
                SnapshotPickup(world, snap.xPx / Constants.PPM, snap.yPx / Constants.PPM, entry)
            }
        }
}

// ---------------------------------------------------------------------------
// LevelRegistry — single source of truth for all TMX levels
// ---------------------------------------------------------------------------

/**
 * Registry of every standard TMX-backed level.
 *
 * **Adding a new level:** append one [TmxLevelDefinition] to [ALL].
 * No new Kotlin class is required.
 *
 * Eco-token positions and snapshot positions are stored in virtual pixels
 * here; [TmxLevel] divides by [Constants.PPM] when exposing them.
 *
 * [Level0_1] is *not* in this registry — it hand-builds its geometry
 * procedurally (no TMX file) and is registered separately in [LevelManager].
 */
object LevelRegistry {
    val ALL: List<TmxLevelDefinition> = listOf(

        // ── Level 1 — "The First Rain" ──────────────────────────────────────
        // Introduces Ebo's Seed Slam and wall-jump mechanics.
        TmxLevelDefinition(
            id           = "level1",
            name         = "The First Rain",
            mapPath      = "maps/level1.tmx",
            spawnX       = 80f,
            spawnY       = 80f,
            levelWidthPx = 2100f,
            exitXPx      = 1950f,
            checkpoints  = listOf(
                LevelCheckpoint("cp1",  7.0f,  0.8f, "level1"),
                LevelCheckpoint("cp2", 14.0f,  0.8f, "level1")
            ),
            snapshots = listOf(
                SnapshotDef("silver_iodide",  450f,  100f),
                SnapshotDef("water_cycle",   1200f,  400f)
            ),
            ecoTokens = listOf(
                Vector2( 250f,  60f),
                Vector2( 560f, 130f),
                Vector2( 700f, 200f),
                Vector2( 900f, 310f),
                Vector2(1150f, 360f),
                Vector2(1400f, 450f),
                Vector2(1610f, 450f),
                Vector2(1800f, 410f)
            ),
            enemies = listOf(
                // Three Smog Sprites patrolling ground sections of Level 1
                EnemyDef("smog_sprite", 400f, 60f,  300f,  550f),
                EnemyDef("smog_sprite", 950f, 60f,  850f, 1100f),
                EnemyDef("smog_sprite", 1500f, 60f, 1350f, 1650f)
            ),
            musicTrack = "ambient_arid"
        ),

        // ── Level 2 — "Winds of Change" ─────────────────────────────────────
        // Mastering Laya's Wind Dash is essential to cross the air gaps.
        TmxLevelDefinition(
            id           = "level2",
            name         = "Winds of Change",
            mapPath      = "maps/level2.tmx",
            spawnX       = 80f,
            spawnY       = 80f,
            levelWidthPx = 2100f,
            exitXPx      = 1950f,
            checkpoints  = listOf(
                LevelCheckpoint("cp1",  7.0f,  0.8f, "level2"),
                LevelCheckpoint("cp2", 14.0f,  0.8f, "level2")
            ),
            snapshots = listOf(
                SnapshotDef("temperature_inversion",  600f, 200f),
                SnapshotDef("albedo_effect",         1500f, 300f)
            ),
            ecoTokens = listOf(
                Vector2( 340f,  100f),
                Vector2( 540f,  170f),
                Vector2( 730f,  250f),
                Vector2( 950f,  310f),
                Vector2( 620f,  420f),
                Vector2( 820f,  470f),
                Vector2(1240f,  200f),
                Vector2(1550f,  180f),
                Vector2(1720f,  230f),
                Vector2(1920f,  200f)
            ),
            musicTrack = "ambient_wind"
        ),

        // ── Level 3 — "Stormy Heights" ───────────────────────────────────────
        // Kishōtenketsu layout (T-025): Ki = ground run, Shō = ascending steps,
        // Ten = wall-jump shaft + sky sequence, Ketsu = moving-platform gauntlet.
        // Boss arena (T-034): Storm Sentinel at x=2200–2820, defeated = level end.
        TmxLevelDefinition(
            id           = "level3",
            name         = "Stormy Heights",
            mapPath      = "maps/level3.tmx",
            spawnX       = 80f,
            spawnY       = 80f,
            levelWidthPx = 2840f,
            exitXPx      = 2780f,
            checkpoints  = listOf(
                // cp1: Shō entry, first step platform
                LevelCheckpoint("cp1",  6.0f,  0.8f, "level3"),
                // cp2: top of wall-jump shaft, sky_landing platform (y=410px)
                LevelCheckpoint("cp2", 13.8f,  4.5f, "level3"),
                // cp3: sky3 platform before Ketsu gauntlet (y=320px)
                LevelCheckpoint("cp3", 19.3f,  3.5f, "level3"),
                // cp4: boss arena entrance (just past the boss gap)
                LevelCheckpoint("cp4", 23.0f,  0.8f, "level3")
            ),
            snapshots = listOf(
                // Above the mid combat platform — collected while fighting the boss
                SnapshotDef("storm_system", 2540f, 380f)
            ),
            ecoTokens = listOf(
                // Ki zone — flat ground run
                Vector2( 170f,  60f),
                Vector2( 400f,  60f),
                // Shō zone — ascending step platforms
                Vector2( 620f,  60f),
                Vector2( 790f, 145f),
                Vector2( 980f, 205f),
                Vector2(1110f, 265f),
                // Ten zone — sky sequence after shaft
                Vector2(1390f, 475f),
                Vector2(1585f, 395f),
                Vector2(1745f, 315f),
                // Ketsu zone — exit island reward
                Vector2(2120f, 225f),
                // Boss arena — reward tokens on the combat platforms
                Vector2(2310f, 265f),
                Vector2(2550f, 370f),
                Vector2(2710f, 265f)
            ),
            bossDef = BossDef(
                type         = "storm_sentinel",
                xPx          = 2520f,
                yPx          = 150f,
                arenaLeftPx  = 2200f,
                arenaRightPx = 2820f
            ),
            musicTrack = "ambient_eco"
        )
    )

    /** Look up a definition by id. */
    fun get(id: String): TmxLevelDefinition? = ALL.firstOrNull { it.id == id }

    /** Instantiate a [TmxLevel] from its definition. */
    fun buildLevel(id: String): Level? = get(id)?.let { TmxLevel(it) }
}

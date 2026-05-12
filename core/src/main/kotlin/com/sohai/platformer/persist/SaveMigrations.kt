package com.sohai.platformer.persist

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Save-format migration chain.
 *
 * The pipeline is intentionally small and explicit:
 *
 *   raw save JSON  ──► detect version  ──► apply step(v → v+1) zero or more times
 *                  ──► decode into [GameState] at [CURRENT_VERSION]
 *
 * Each [MigrationStep] is a pure function `(JsonObject) -> JsonObject` that
 * upgrades a save from `fromVersion` to `fromVersion + 1`. v1 is the first
 * versioned format and has **no** migration step ahead of it; saves written
 * before [GameState.saveFormatVersion] existed are treated as v1 (T-113).
 *
 * The scaffold is the contract here; v1 itself is the identity case. Real
 * migrations will be appended to [STEPS] when a future ticket bumps
 * [CURRENT_VERSION].
 *
 * Test seam: [migrateWith] takes an explicit step list so tests can prove the
 * chain fires (e.g. a fake v0 → v1 step). Production [migrate] always uses
 * [STEPS]. No reflection, no service-loader magic.
 */
object SaveMigrations {

    /** Current save schema version. Bump when you append a new step to [STEPS]. */
    const val CURRENT_VERSION: Int = 1

    /**
     * A single forward-only schema migration. Transforms a save at
     * [fromVersion] into the equivalent save at [fromVersion] + 1.
     *
     * Implementations must be pure (no I/O, no side effects) and must not
     * assume any field is present beyond what the source version guarantees.
     */
    data class MigrationStep(
        val fromVersion: Int,
        val transform: (JsonObject) -> JsonObject
    )

    /**
     * The production migration chain. Empty at v1 introduction (T-113) — v1
     * is the baseline and needs no rewrite. Future migrations are appended
     * here in ascending [MigrationStep.fromVersion] order.
     */
    val STEPS: List<MigrationStep> = emptyList()

    /** Lenient parser so missing keys and unknown future keys are tolerated. */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Read [rawJson], run it through the production migration chain, and
     * decode to a [GameState] at [CURRENT_VERSION].
     */
    fun migrate(rawJson: String): GameState = migrateWith(rawJson, STEPS)

    /**
     * Test-friendly variant: route [rawJson] through [steps] instead of the
     * production [STEPS]. Used by `SaveMigrationsTest` with a fake v0 → v1
     * step to prove the chain actually fires.
     */
    fun migrateWith(rawJson: String, steps: List<MigrationStep>): GameState {
        val root = json.parseToJsonElement(rawJson)
        require(root is JsonObject) {
            "Save root must be a JSON object, got ${root::class.simpleName}"
        }
        val migrated = applyChain(root, steps)
        return json.decodeFromJsonElement(GameState.serializer(), migrated)
    }

    /**
     * Walk [steps] from the save's current version up to [CURRENT_VERSION],
     * applying every step whose [MigrationStep.fromVersion] matches the
     * running version. Missing version field is treated as v1.
     *
     * Exposed for tests so they can verify intermediate JSON shapes.
     */
    fun applyChain(root: JsonObject, steps: List<MigrationStep>): JsonObject {
        var current = root
        var version = detectVersion(current)

        // Index steps by their fromVersion for O(1) lookup. Multiple steps
        // sharing a fromVersion is a programmer error — keep the first to
        // make this deterministic and fail loudly in tests.
        val byFromVersion: Map<Int, MigrationStep> = steps.associateBy { it.fromVersion }

        while (version < CURRENT_VERSION) {
            val step = byFromVersion[version]
                ?: error(
                    "No SaveMigrations.MigrationStep registered for v$version → v${version + 1}; " +
                        "cannot migrate save up to v$CURRENT_VERSION"
                )
            current = step.transform(current)
            version += 1
        }

        // Stamp the (possibly migrated) save with the current version so the
        // decoded GameState carries the right value even when the raw input
        // omitted it or used an older value.
        return JsonObject(current.toMutableMap().also { it["saveFormatVersion"] = JsonPrimitive(CURRENT_VERSION) })
    }

    /**
     * Returns the saveFormatVersion encoded in [root], or [CURRENT_VERSION]
     * (currently v1) when the field is absent. Saves written before T-113
     * are pre-versioning and are treated as v1 by definition.
     */
    fun detectVersion(root: JsonObject): Int {
        val versionElement: JsonElement = root["saveFormatVersion"] ?: return CURRENT_VERSION
        return (versionElement as? JsonPrimitive)?.intOrNull
            ?: error("saveFormatVersion must be an integer, got: ${versionElement.jsonPrimitive}")
    }
}

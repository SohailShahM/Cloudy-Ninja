# Save Format Evolution Plan

Strategy document for evolving Cloudy Ninja's save data safely after the alpha. This is **strategy-only** — no code changes are proposed here. When a future ticket needs to bump the save schema, this plan tells the implementer which lever to pull and which traps to avoid.

Authoritative source files referenced throughout:

- [`GameState.kt`](../core/src/main/kotlin/com/sohai/platformer/persist/GameState.kt) — the `@Serializable` schema.
- [`SaveManager.kt`](../core/src/main/kotlin/com/sohai/platformer/persist/SaveManager.kt) — atomic-write protocol + load cache.
- [`SaveMigrations.kt`](../core/src/main/kotlin/com/sohai/platformer/persist/SaveMigrations.kt) — the forward-only migration chain (T-113 scaffold).
- [`Settings.kt`](../core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt) — cross-slot user settings (separate file, separate evolution rules).

---

## 1. Current state

The save format is **v1**, established by T-113 as the first explicitly versioned schema. Everything written before T-113 is treated as v1 by definition (saves missing the `saveFormatVersion` key fall through `detectVersion()` and are stamped with `CURRENT_VERSION` on load).

Key invariants in `SaveMigrations`:

- `CURRENT_VERSION = 1`.
- `STEPS: List<MigrationStep>` is **empty** — v1 is the baseline, no rewrite step exists.
- `migrate(rawJson: String): GameState` always routes through the chain, even at v1, so the migration code path is exercised on every load.
- `detectVersion()` returns `CURRENT_VERSION` (not `0`) when the key is absent. Pre-T-113 saves are therefore opted into v1 silently.
- `applyChain()` always re-stamps `saveFormatVersion` with `CURRENT_VERSION` after migration, so a save's on-disk version always equals what the decoded `GameState` carries.

Kotlinx-serialization is configured with `ignoreUnknownKeys = true` in both `SaveManager` and `SaveMigrations`, which is what makes the additive-field pattern (Section 2) safe: a newer save written on a player's machine, opened by an older build, drops fields it doesn't recognise instead of crashing — and the next save from the older build re-writes the file without them. This is a graceful-degradation property, not a guarantee of round-trip preservation. We have explicitly accepted that trade-off for alpha.

The save root must be a JSON object (`require(root is JsonObject)` in `migrateWith`); anything else throws on load and the catch in `SaveManager.loadGame` returns a fresh `GameState()`. **Loading never crashes the game.** Corrupt or malformed saves yield a default save, logged but not surfaced to the player.

## 2. Additive fields added during alpha

All of the following fields landed without bumping `CURRENT_VERSION`. Each defaults to an empty value that is semantically equivalent to "the player never did this thing", which is the test for whether an additive field is safe (Section 3).

| Field | Ticket | Default | Why additive is safe |
| --- | --- | --- | --- |
| `collectedHiddenTokens: Set<String>` | T-107 | `emptySet()` | Pre-T-107 saves never collected hidden tokens; empty set is the correct historical answer. |
| `achievementTimestamps: Map<String, Long>` | T-146 | `emptyMap()` | Legacy unlocks in `unlockedAchievements` render `"Unlocked: ?"` for the missing timestamp — UI explicitly handles the legacy case. |
| `tutorialSeen: Boolean` | T-137 (planned) | `false` | Pre-T-137 saves never saw the new overlay; showing it once on the next hub entry is the desired behaviour. |
| Future `Settings` additions (e.g. accessibility flags) | various | `false` / `OFF` / sensible default | `Settings` is a separate file but follows the same rule: every new field must deserialise from a pre-existing `settings.json` to a value that preserves prior behaviour. T-132 `highContrast`, T-130 `reducedMotion`, and `colorBlindMode` all follow this. |

The pattern is documented inline in `GameState.kt` for each field (see the KDoc on `collectedHiddenTokens` and `achievementTimestamps`). New additive fields **must** carry a similar comment naming the ticket and stating the legacy-load behaviour.

## 3. When to bump to v2

Stay on v1 (additive only) for:

- Adding a new field with a default that means "never happened" or "feature off".
- Adding a new entry to an existing `Set<String>` or `Map<K, V>` (e.g. a new achievement id, a new level id in `bestScores`).
- Adding a new value to a `Serializable` enum **at the end** of the declaration list (kotlinx-serialization tolerates unknown enum values on read when paired with `ignoreUnknownKeys` only via custom serializers; the safer route is to add a new enum value, default any field that uses it to an existing value, and never persist the new value into older code paths).

Bump to v2 when any of the following is true:

1. **Breaking rename.** A field's JSON key changes (e.g. `collectedAtlasIds` → `cloudAtlasIds`). Old saves still carry the old key; v1 → v2 must rewrite the key.
2. **Semantic change.** Same key, same type, different meaning (e.g. `bestTimes` flips from seconds to milliseconds, or `totalDeaths` starts excluding tutorial-room deaths). Without a migration, every old save silently displays wrong numbers.
3. **Removed field becomes ambiguous.** Dropping a field is fine if `ignoreUnknownKeys` can swallow it on load *and* the absence carries no information. If absence is meaningful (e.g. "this save was written before we tracked X, so we don't know X"), encode that into a migration that maps the old shape to whatever sentinel v2 uses.
4. **Type narrowing or widening.** `Int` → `Long`, `Float` → `Double`, `Set<String>` → `Map<String, SomeStruct>`. Kotlinx-serialization will refuse to decode mismatched shapes; bump and migrate.
5. **Nested-structure refactor.** Splitting `PlayerStats` into per-world buckets, or moving fields between top-level and `checkpoint`. Even if the data is preserved, the JSON shape changes.

Rule of thumb: if a player on the *current* build saves, then upgrades to the *next* build, and the next build needs to read the old save and get the right answer without `ignoreUnknownKeys` silently dropping data, you need a migration step.

## 4. Migration patterns library

Every step is a `MigrationStep(fromVersion, transform: (JsonObject) -> JsonObject)`. Patterns we expect to use:

### 4a. Rename a field

Read the old key, write the new key, remove the old key. Pure `JsonObject` manipulation — no `GameState` decode in the middle. Example shape (illustrative, do not copy into code now):

```
v1 → v2: rename "collectedAtlasIds" to "cloudAtlasIds"
- read root["collectedAtlasIds"] (may be null on saves that never had it)
- emit root + ("cloudAtlasIds" to that value) - "collectedAtlasIds"
- if null, emit root - "collectedAtlasIds" with default supplied by the GameState constructor
```

### 4b. Derive a new field from existing data

When v2 introduces a field that can be reconstructed from v1 data, the migration computes it once at upgrade time rather than waiting for gameplay to repopulate it. Example: a hypothetical `totalLevelsCompleted: Int` derived from `completedLevels.size`. Migration sets `totalLevelsCompleted = completedLevels.size`; from v2 onward the runtime maintains both.

### 4c. Soft-cleanup of orphaned data

If a future patch removes a level or an achievement, the migration scrubs orphaned ids out of `completedLevels`, `bestScores`, `bestTimes`, `unlockedAchievements`, `achievementTimestamps`, and `collectedHiddenTokens`. The scrub list lives inside the migration step (as a hardcoded constant), not in a runtime registry — the migration must be reproducible from the JSON alone, forever, with no runtime state.

### 4d. Default-fill on widening

When a field's type widens (e.g. `Float` → a struct `{ value: Float, recordedAt: Long }`), the migration wraps the old scalar into the new shape with a sentinel `recordedAt = 0L` meaning "unknown". The decoded `GameState` then carries an honest "we don't know when this happened" rather than fabricating a timestamp.

### 4e. Multi-step chains

`applyChain()` runs steps in order. A save at v1 going to v4 fires v1→v2, then v2→v3, then v3→v4. Each step sees the output of the previous step. **Do not write a v1→v4 short-circuit step.** Players who skip versions are handled by the chain naturally; a custom shortcut doubles maintenance and creates a second code path that can drift.

## 5. Anti-patterns

These have all bitten real codebases doing the same thing we're doing. Avoid.

### 5a. No reflection-dependent migrations

Every step is `(JsonObject) -> JsonObject`. Do **not** decode to `GameState`, mutate the Kotlin object, and re-encode. If a future Kotlin field is removed or renamed, an old migration step that decodes to `GameState` breaks — and breaks *retroactively*, for every save in the field that still needs to walk through it. The whole point of `SaveMigrations` being JSON-shaped is that v1→v2 written today must still run unchanged in three years against v17 of `GameState`. JSON shape is the stable interface; `GameState` is not.

The KDoc on `SaveMigrations` says "no reflection, no service-loader magic" — that's the same rule from a different angle.

### 5b. No reading player content into telemetry

If we ever wire saves to telemetry/crash reports (see [`CrashReporter.kt`](../core/src/main/kotlin/com/sohai/platformer/persist/CrashReporter.kt)), do not ship raw save JSON. The save contains `characterName` (player-chosen string) and could in future contain other player-authored content. Telemetry pipelines should emit schema version + opaque counters (`completedLevels.size`, `totalDeaths`, `saveFormatVersion`), not the JSON body.

### 5c. Don't bump the version without a step

`CURRENT_VERSION` and `STEPS.size` move together. The chain in `applyChain()` errors loudly with "No SaveMigrations.MigrationStep registered for vN → vN+1" when they desync — that error is the contract working as intended. Don't suppress it by skipping the bump or by adding an empty step; either of those papers over a real schema change without a migration.

### 5d. Don't rely on `ignoreUnknownKeys` to delete data

`ignoreUnknownKeys = true` is a forward-compatibility shim, not a deletion mechanism. If a field is removed, write a v→v+1 step that removes it from the JSON. Otherwise the next save round-trip rewrites the file without the field anyway (since the runtime doesn't know about it), but the *intermediate* on-disk state has stale data that future code might attempt to read or that future migrations might collide with on the same key name.

### 5e. Don't change a default to do migration work

Tempting shortcut: rename a field, then make the new field's Kotlin default read from the old field. This works exactly once and traps the dependency in `GameState`'s constructor forever. The migration chain is the right place.

## 6. Atomic-write protocol — preserved across migrations

The T-136 atomic-write sequence in `SaveManager.saveGame` is independent of the schema and **must not** be touched by any migration:

1. Serialise `GameState` to JSON.
2. Write to `<filename>.tmp`, then `fd.sync()` (data + metadata).
3. `Files.move(tmpPath, finalPath, ATOMIC_MOVE, REPLACE_EXISTING)`.
4. Fallback to non-atomic `REPLACE_EXISTING` only on `AtomicMoveNotSupportedException` (FAT, some SMB shares).

Crash semantics, copied verbatim from the KDoc and re-asserted here because future migration work must not regress them:

- Crash during step 2 → temp file may be partial; original target untouched and still loadable.
- Crash between step 2 and step 3 → original target untouched.
- Crash during step 3 → either the old or the new file is at the target path; both are valid JSON, so the load path always succeeds.

**Implication for migrations:** migrations run on *load*, not on *save*. A migrated save is not written back until gameplay triggers a normal save. That means a player can load an old save, quit before the autosave point, and the file on disk stays at the old version. That is fine and intentional — the next load re-migrates from the same JSON, and the next save writes the current version. Do not add a "rewrite on load" step to force version bumps eagerly; it would mean every save load performs disk I/O, defeats the in-memory cache in `SaveManager`, and bypasses the atomic-write protocol's "the player is doing a thing they expect to persist" semantics.

`SaveManager.cache` is keyed by filename and stores the *post-migration* `GameState`. Saves following a load therefore write the current schema version even if the on-disk file is still old. The cache is invalidated on save and delete, which keeps it coherent with disk.

The test seams `crashAfterTempWriteHook` and `crashDuringWriteHook` in `SaveManager` exist precisely to prove the atomic-write protocol holds under failure. Any future change to the save pipeline (including any migration-related plumbing) must keep those seams intact and keep the corresponding tests passing.

---

## TL;DR for the next implementer

1. Adding a field with a sensible default? Just add it. Document the legacy-load behaviour in KDoc with the ticket id. Don't touch `CURRENT_VERSION`.
2. Renaming / semantically changing / removing-with-meaning a field? Bump `CURRENT_VERSION`, append a `MigrationStep(fromVersion = oldVersion, transform = ...)` to `SaveMigrations.STEPS`, write a test using `migrateWith(rawJson, listOf(yourStep))` that proves the chain fires.
3. Never decode to `GameState` inside a migration step.
4. Never touch `SaveManager.saveGame`'s atomic-write sequence as part of a migration.

# Determinism audit (T-A2)

Generated: 2026-05-11

## Purpose
This document inventories every source of non-determinism in `core/src/main/kotlin/com/sohai/platformer/`. Replay-based regression testing (T-A3, future) requires that gameplay-affecting non-determinism is eliminated; cosmetic non-determinism can stay.

## Summary

| Category | Count | Action |
|---|---|---|
| Hot-path gameplay random | 2 | Needs seeded RNG wrapper |
| Cosmetic random (particle/audio) | 18 | Safe to leave |
| Map/Set iteration in hot paths | 1 | Replace with LinkedHashSet or sort before drain |
| Map/Set iteration in non-hot paths | 4 | No action needed |
| Variable timestep | 0 | Fixed timestep confirmed (1/60 s) |
| File listing order | 1 | N/A for replay (save-slot UI only) |
| System time in gameplay | 0 | No hits |

## Findings

| Site (file:line) | Category | Deterministic? | Needs fix? | Notes |
|---|---|---|---|---|
| `entities/StormSentinel.kt:183` | Gameplay | No | Fixed via GameRandom (this commit) | `MathUtils.random(...)` picks X positions for lightning strikes inside `startLightningTelegraph()`. Called every time the boss enters its lightning phase — affects projectile spawn positions directly. |
| `entities/StormSentinel.kt:190` | Gameplay | No | Fixed via GameRandom (this commit) | `MathUtils.randomBoolean()` picks sweep direction inside `startSweepTelegraph()`. Affects sweep beam path and player dodge requirement. |
| `abilities/EboAbility.kt:108` | Gameplay | No | Fixed via GameRandom (this commit) | `MathUtils.random(15f, 40f)` for raindrop spawn jitter. Called on Ebo ability use (hot-path per activation). Affects Box2D body positions, which feed physics — replay-breaking. |
| `abilities/EboAbility.kt:112` | Gameplay | No | Fixed via GameRandom (this commit) | `MathUtils.random(6f, 10f)` for raindrop speed. Same activation path as line 108; affects Box2D velocity, directly gameplay. |
| `screens/LevelRunState.kt:456` | Cosmetic | No | No | `MathUtils.random(0.9f, 1.1f)` pitch variation on hazard-cleansed sound. Audio only; no gameplay state affected. |
| `screens/LevelRenderer.kt:344–352` | Cosmetic | No | No | `MathUtils.random` × 6 in `spawnJumpPuff()`. Visual particles only. |
| `screens/LevelRenderer.kt:366–372` | Cosmetic | No | No | `MathUtils.random` × 5 in `spawnLandingDust()`. Visual particles only. |
| `screens/LevelRenderer.kt:382–389` | Cosmetic | No | No | `MathUtils.random` × 4 in `spawnCleanseBurst()`. Visual particles only. |
| `screens/LevelRenderer.kt:398–404` | Cosmetic | No | No | `MathUtils.random` × 4 in `spawnCollectSparkle()`. Visual particles only. |
| `screens/LevelRenderer.kt:413–421` | Cosmetic | No | No | `Math.random` × 6 in `spawnTokenSparkle()`. Uses Java `Math.random()` rather than libGDX — second distinct unseeded RNG. Visual only. |
| `screens/LevelRenderer.kt:429–435` | Cosmetic | No | No | `Math.random` × 6 in `spawnSnapshotSparkle()`. Same Java RNG. Visual only. |
| `screens/LevelRenderer.kt:446–456` | Cosmetic | No | No | `Math.random` + `MathUtils.random` × 6 in `spawnStompSmokeBurst()`. Visual only. |
| `screens/GameScreen.kt:116` | Hot-path iteration | Depends | **Yes** | `pendingBodyDestroy = mutableSetOf<Body>()` — a `LinkedHashSet` in Kotlin, so insertion-ordered. Body insertion happens inside Box2D contact callbacks (order is engine-determined but consistent). Iteration order for `world.destroyBody()` calls is likely deterministic in practice but worth pinning to `LinkedHashSet` explicitly to make it contract-guaranteed. |
| `screens/LevelRunState.kt:84` | Setup | Yes | No | `activatedCheckpoints = mutableSetOf<String>()` — `LinkedHashSet`, insertion-ordered by checkpoint activation sequence. Never iterated in a gameplay-affecting way; used for save state. |
| `audio/SoundManager.kt:22` | Setup | Yes | No | `sounds = mutableMapOf<String, Sound>()` — `LinkedHashMap`, insertion-ordered. Iterated only for bulk dispose. |
| `FontManager.kt:63` | Setup | Yes | No | `sharedCache = mutableMapOf<Int, BitmapFont>()` — `LinkedHashMap`. Font cache lookup by key; order irrelevant. |
| `persist/SaveManager.kt:25` | Setup | Yes | No | `cache = mutableMapOf<String, GameState>()` — `LinkedHashMap`. Iterated only for save/load; no gameplay effect. |
| `persist/SaveManager.kt:84` | UI / Setup | No | No | `saveDir.list()` — filesystem directory listing. Order is OS-dependent but this is the save-slot UI, not called during gameplay. No replay impact. |
| `screens/LevelRunState.kt:327` | Physics | Yes | No | `world.step(Constants.TIME_STEP, …)` — uses the fixed constant `1/60f` with an accumulator loop (max 5 sub-steps, remainder discarded). Timestep is deterministic given identical input sequence. |

## Recommended fix order for T-A3 (record/replay)

1. **Introduce a seeded `GameRng` singleton** (wrapping `java.util.Random` or libGDX `RandomXS128`) that is re-seeded at level-start with a stored seed. Replace the four gameplay-path `MathUtils.random` calls (EboAbility lines 108/112, StormSentinel lines 183/190) with `GameRng` calls. Store the seed in the replay header so runs are reproducible.
2. **Replace cosmetic `Math.random()` calls** in `LevelRenderer.kt` (lines 413–421, 429–435, 446) with either `MathUtils.random` (same libGDX RNG) or the `GameRng` — keeps both RNGs in sync and removes the stray Java `Math.random()` dependency. Low priority since these are cosmetic, but cleaner for a single-RNG policy.
3. **Explicitly declare `pendingBodyDestroy` as `LinkedHashSet<Body>`** (`GameScreen.kt:116`) to make the destruction order a guaranteed contract rather than an implementation detail of Kotlin's `mutableSetOf`.

## Sites accepted as non-deterministic (cosmetic only)

- `LevelRenderer.spawnJumpPuff` — particle visuals, no gameplay state
- `LevelRenderer.spawnLandingDust` — particle visuals, no gameplay state
- `LevelRenderer.spawnCleanseBurst` — particle visuals, no gameplay state
- `LevelRenderer.spawnCollectSparkle` — particle visuals, no gameplay state
- `LevelRenderer.spawnTokenSparkle` — particle visuals, no gameplay state
- `LevelRenderer.spawnSnapshotSparkle` — particle visuals, no gameplay state
- `LevelRenderer.spawnStompSmokeBurst` — particle visuals, no gameplay state
- `LevelRunState.kt:456` — audio pitch variation on hazard-cleansed SFX
- `SaveManager.listSaves()` — save-slot UI listing, never called during active gameplay

# TASKS.md — Multi-Agent Task Board

Coordination file for parallel agents working on Cloudy Ninja.

**Required reading before claiming any task:**
1. [AGENTS.md](AGENTS.md) — architecture, conventions, module layout
2. [GDD_ADDENDUM.md](GDD_ADDENDUM.md) — technical spec, calibration numbers, sprint plan, P0 bug history
3. [GAME_PLAN.md](GAME_PLAN.md) — high-level roadmap, content themes, educational goals

Each task below cites a `GDD ref:` (section number in `GDD_ADDENDUM.md`) when applicable — read that section before starting.

## Workflow

1. **Pick** a task from `## Todo` whose `Depends on` tasks are all `Done`.
2. **Claim** it: move the task block to `## In Progress`, fill in `Agent` and `Branch`, then commit + push to `main`:
   ```
   git add TASKS.md && git commit -m "claim T-XXX" && git push
   ```
3. **Work** on your branch in a worktree: `git worktree add ../cn-T-XXX -b claude/T-XXX-short-desc`
4. **Finish**: merge branch to `main`, then move the task to `## Done` with a one-line outcome and PR/commit hash.
5. **Conflicts**: if `git push` rejects your claim because someone else claimed it first, pull, pick a different task.

**Rules:**
- One task = one branch = one worktree. Don't bundle.
- Don't claim a task whose dependencies aren't `Done`.
- Keep claim-commits tiny (only `TASKS.md`) so conflicts are rare.
- If you abandon a task, move it back to `Todo` and clear the `Agent`/`Branch` fields.

---

## Todo

### T-002 — Add Kotest specs for PlayerController state machine
- **Status:** In Progress
- **Agent:** team-agent-E (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **Files:** `core/src/test/kotlin/com/sohai/platformer/entities/`
- **Goal:** Coyote-time edge cases, jump-buffer expiration, ability-swap mid-air, ground/air state transitions.

### T-003 — Save/load UI in Settings menu
- **Status:** In Progress
- **Agent:** team-agent-F (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/SettingsScreen.kt`, `persist/SaveManager.kt`
- **Goal:** Save/Load/Delete buttons in SettingsScreen wired to SaveManager; toast confirmation.

### T-004 — Checkpoint restart via serialized GameState
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-003
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`, `persist/GameState.kt`
- **Goal:** On player death, restore from the last `GameState` checkpoint instead of reloading the level from scratch. Capture checkpoint state when the player crosses a checkpoint trigger.
- **Done when:** Dying after a checkpoint respawns the player at the checkpoint with eco-tokens/snapshots collected up to that point preserved.

### T-006 — World 0 Room 1 "First Step" tutorial
- **Status:** In Progress
- **Agent:** team-agent-G (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §7.1 (table row 0-1)
- **Files:** new `core/src/main/kotlin/com/sohai/platformer/levels/Level0_1.kt`, `levels/LevelManager.kt`, possibly a new `.tmx` in `assets/maps/`
- **Goal:** Single-screen room teaching walk + ground jump. One eco-token on the far side of a 2-tile gap that requires exactly one jump. No text — environment teaches.
- **Done when:** Player can launch `level0_1` from `LevelManager`, jump the gap, collect the token, and trigger the level-exit sensor that hands off to Room 2 (or returns to main menu while T-007 is unbuilt).

### T-007 — World 0 Room 2 "Long Fall" tutorial
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-006
- **GDD ref:** §7.1 (table row 0-2)
- **Files:** new `levels/Level0_2.kt`, `LevelManager.kt`
- **Goal:** Vertical drop teaching variable jump height (must release jump early to fit through a low ceiling) and coyote time (a ledge requiring the player to walk off then jump).
- **Done when:** Both teaching beats are physically only solvable using the intended mechanic; level chains from Room 1.

### T-008 — World 0 Room 3 "Wall Climb" tutorial
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-007
- **GDD ref:** §7.1 (table row 0-3)
- **Files:** new `levels/Level0_3.kt`, `LevelManager.kt`
- **Goal:** Vertical shaft, no other path. Player must wall-jump up. If they fall, restart the room (no death penalty — this is teaching).
- **Done when:** Wall-jump is the only solution, falling cleanly resets the room without a game-over screen.

### T-009 — World 0 Room 4 "First Cleanse" tutorial
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-008
- **GDD ref:** §7.1 (table row 0-4) and §7.2 prologue beat
- **Files:** new `levels/Level0_4.kt`, `LevelManager.kt`, `Hud.kt` (action button hint)
- **Goal:** Hazard blocks the only path. Action button gets a pulsing-ring hint. After Seed Slam cleanses, the hazard becomes ground. Final transition uses `screenFade` into `level1`.
- **Done when:** Cleanse is the only solution; HUD action button visibly hints; finishing fades into the existing `level1`.

### T-012 — Camera vertical platform snap
- **Status:** In Progress
- **Agent:** team-agent-H (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §4.3
- **Files:** wherever the camera controller lives (likely `screens/GameScreen.kt` per §4.4 sketch)
- **Goal:** Only follow vertical position when the player is grounded *or* falling past a y-threshold. Locks the camera during jump arcs so the world doesn't bob with every jump.
- **Done when:** Standing-jumping in level1 keeps the camera still; falling off a high platform still pans down to keep the player on screen.

### T-013 — Generate 8 base SFX via ProceduralSoundGenerator
- **Status:** In Progress
- **Agent:** team-agent-I (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §5.4 (priority-order list, items 1–8)
- **Files:** `audio/ProceduralSoundGenerator.kt`, `audio/SoundManager.kt`, new files in `assets/audio/sfx/`
- **Goal:** Use the existing `ProceduralSoundGenerator` to synthesize wav files for: jump, land, collect_token, collect_snapshot, death, checkpoint, level_complete, hazard_cleansed. Output to `assets/audio/sfx/`. SoundManager loads them at startup and exposes `play(name)`.
- **Done when:** All 8 sounds exist on disk, `SoundManager.play("jump")` makes a recognizable noise, no allocations in hot path.

### T-014 — Wire SoundManager into gameplay events
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-013
- **GDD ref:** §5.1, §5.2 (bus layout + ducking), §5.4
- **Files:** `entities/PlayerController.kt`, `screens/GameScreen.kt`, `screens/Hud.kt` (where ability fires), `physics/WorldContactListener.kt`
- **Goal:** Trigger appropriate SoundManager calls from: jump (PlayerController), land (contact begin with `vy_prev < -8`), collect_token / collect_snapshot (pickup), death (player.die), checkpoint (sensor), level_complete (exit sensor), Ebo seed slam, hazard cleansed.
- **Done when:** Every gameplay event has audio feedback; `Settings.sfxVolume` controls level; mute works.

### T-015 — Three save slots UI
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-003
- **GDD ref:** §6.2
- **Files:** `screens/MainMenuScreen.kt`, `persist/SaveManager.kt`
- **Goal:** Replace single Continue/New Game with 3 slot cards on main menu. Each card shows: level reached, % atlas collected, total deaths, last-played timestamp. Empty slot shows "New Game".
- **Done when:** Players can save to any of 3 slots, see at-a-glance status, load any slot, delete a slot with confirmation.

### T-016 — Refactor levels to data-driven registry
- **Status:** In Progress
- **Agent:** team-agent-J (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §10.4
- **Files:** `levels/Level1.kt`, `Level2.kt`, `Level3.kt`, `LevelManager.kt`, new `levels/TmxLevelDefinition.kt`
- **Goal:** Replace the three near-identical `LevelN` classes with a single registry of `TmxLevelDefinition(id, name, mapPath, spawnX, spawnY, levelWidthPx, exitX, ecoTokens, snapshots)`. `LevelManager` reads from the registry.
- **Done when:** All three existing levels still play identically; adding a 4th level requires only a new registry entry (no new class).

### T-017 — Investigate intermittent native Box2D crash
- **Status:** In Progress
- **Agent:** team-agent-K (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §0 "KNOWN ISSUE: Intermittent native Box2D crash"
- **Files:** `entities/PlayerController.kt`, `physics/WorldContactListener.kt`, `screens/GameScreen.kt`
- **Goal:** Reproduce the `EXCEPTION_ACCESS_VIOLATION` in `gdx-box2d64.dll` from `Body.jniGetPosition`. Follow the investigation path in §0: disable platform-carry to isolate, add logging in contact begin/end to verify `platformContacts` map invariants, hunt for stale body references.
- **Done when:** Either a reproducer is documented in a new `BUGS.md` with stack trace and minimal repro steps, OR a fix is shipped with a regression test.

### T-019 — Collect sparkles on eco-token and snapshot pickup
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §3.6
- **Files:** `screens/GameScreen.kt`, `rendering/ParticleSystem.kt`, `entities/EcoToken.kt`, `entities/SnapshotPickup.kt`
- **Goal:** When player collects an eco-token or atlas snapshot, burst 6–10 small particles upward: additive blend feel (bright cyan/yellow), slight upward initial velocity (~1 m/s), 0.4 s lifespan, alpha-fade. Reuse the existing 200-particle pool.
- **Done when:** Collecting any token or snapshot triggers visible sparkle burst; pool cap is respected; compile and tests pass.

### T-020 — Apply Celeste-calibrated movement constants + asymmetric gravity
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §2.1, §2.2
- **Files:** `Constants.kt`, `entities/PlayerController.kt`
- **Goal:** Apply the full constant table from GDD §2.1 (PLAYER_RUN_ACCEL, PLAYER_RUN_DECEL, PLAYER_AIR_ACCEL_MUL, PLAYER_JUMP_HOLD_GRAVITY_MUL, PLAYER_APEX_VEL_THRESHOLD, PLAYER_JUMP_CUT_MUL, GRAVITY_FALL_MUL, PLAYER_MAX_FALL, PLAYER_FAST_FALL, PLAYER_WALL_JUMP_IMPULSE_X/Y, PLAYER_WALL_SLIDE_SPEED). Add the asymmetric gravity + apex-hang `gravityScale` code from §2.2 into `PlayerController.update()`. Add terminal velocity cap.
- **Done when:** All constants from §2.1 exist in `Constants.kt`; asymmetric gravity and terminal velocity are applied each frame; existing tests pass; compile clean.

### T-021 — Split GameScreen into focused subsystems
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §10.1
- **Files:** `screens/GameScreen.kt` (636 LOC → ~300), new `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`, `screens/LevelTransitionController.kt`
- **Goal:** Extract from `GameScreen`: (1) `LevelRunState` — score, combo, spirit health, completion flags; (2) `LevelRenderer` — the entire `shapeRenderer.begin/end` block; (3) `LevelTransitionController` — goToNextLevel / dispose chain. `GameScreen` becomes a thin coordinator. No behaviour change.
- **Done when:** `GameScreen.kt` is ≤ 350 LOC; game compiles and existing tests pass.

### T-022 — Particle pool eviction tests
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §11 (table row "Particle pool eviction")
- **Files:** new `core/src/test/kotlin/com/sohai/platformer/rendering/ParticleSystemTest.kt`
- **Goal:** Test that spawning more than 200 particles (pool capacity) doesn't crash or allocate new objects — the overflow is silently dropped. Test that `update()` correctly marks particles dead after their lifespan, freeing slots for reuse.
- **Done when:** `./gradlew :core:test` passes with new specs covering overflow, lifespan expiry, and slot reuse.

### T-023 — Zephyr third-character ability skeleton
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN §4.3 ("Third Character: Zephyr — Air Elemental")
- **Files:** new `abilities/ZephyrAbility.kt`, `screens/GameScreen.kt` (add Zephyr to character roster)
- **Goal:** Create `ZephyrAbility` implementing `CharacterAbility`. Ability: lightweight float — on action press, reduce gravity scale to 0.2f for 1.5 s (float), then cooldown 3 s. Spawns wind-trail effects (reuse `WindTrail`). Wire into `GameScreen` character roster (cycle Ebo → Laya → Zephyr → Ebo). Zephyr renders as a light-purple circle.
- **Done when:** Player can cycle to Zephyr, float ability works, compile and tests pass.

### T-024 — Time trial mode
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN §4.1
- **Files:** `screens/GameScreen.kt`, `screens/Hud.kt`, `persist/GameState.kt`, `persist/SaveManager.kt`
- **Goal:** Add a time trial mode toggled from the pause menu. In time trial mode: timer counts up from 0, no checkpoint saves, HUD shows a prominent stopwatch, on level complete the time is saved to `GameState.bestScores[levelId]` if it beats the previous best. Show "New Best!" on the VictoryScreen.
- **Done when:** Time trial can be started from pause, timer displays and counts, best time persists across sessions, compile clean.

### T-025 — Level 3 pacing rebalance per Kishōtenketsu
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §8 ("Audit: level3 currently jumps to wall-jump + fast moving platforms in first 30%")
- **Files:** `levels/Level3.kt`
- **Goal:** Restructure Level 3 so the first 30% is ground-only movement (no wall-jump required), the wall-jump shaft appears at ~50%, and the moving-platform gauntlet is reserved for the final 15% "Ketsu" section. Apply the Ki/Shō/Ten/Ketsu template from §8.
- **Done when:** Level3 loads and plays through end-to-end; first section requires no wall-jump; compile clean.

### T-026 — Per-level parallax background theming
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN §2.1–2.3 (level themes: water cycle, wind/weather, eco-restoration)
- **Files:** `rendering/ParallaxBackground.kt`, `screens/GameScreen.kt`
- **Goal:** Give each level a distinct parallax colour palette: Level 1 = warm browns/greens (parched expanse); Level 2 = cool blues/whites (wind and weather); Level 3 = lush greens/teals (eco-restoration). `ParallaxBackground` should accept a theme parameter (or colour set) rather than hardcoded colours.
- **Done when:** Each level has visually distinct parallax layers; switching levels changes the background; compile clean.

### T-027 — CloudAtlasLibrary.get unit tests
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §11 (table row "CloudAtlasLibrary.get")
- **Files:** new `core/src/test/kotlin/com/sohai/platformer/atlas/CloudAtlasLibraryTest.kt`
- **Goal:** Test `CloudAtlasLibrary`: known ID returns the correct `CloudAtlasEntry`; unknown ID returns null (or throws, per implementation); all entries have non-blank `title`, `subtitle`, `character`, and `id` fields; no two entries share the same `id`.
- **Done when:** `./gradlew :core:test` passes with new specs.

### T-028 — Android lint + build verification
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN success metrics ("Both Android and Desktop run without errors")
- **Files:** `android/` module, `.github/workflows/` (if CI exists)
- **Goal:** Run `./gradlew android:lint` and fix any errors (warnings are acceptable). Document any outstanding warnings in `BUGS.md`. If a GitHub Actions workflow doesn't exist yet, create a minimal one at `.github/workflows/ci.yml` that runs `./gradlew :core:compileKotlin :core:test android:lint` on push to main.
- **Done when:** `./gradlew android:lint` exits 0 (or only warnings); CI workflow file exists and is valid YAML.

---

## In Progress

### T-005 — Kotest specs for WorldContactListener
- **Status:** In Progress
- **Agent:** team-agent-A (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **Files:** `core/src/test/kotlin/com/sohai/platformer/physics/`
- **Goal:** Test that fixture `userData` strings translate to correct gameplay state on `beginContact`/`endContact`.

### T-010 — Corner correction in PlayerController
- **Status:** In Progress
- **Agent:** team-agent-B (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §2.3
- **Files:** `entities/PlayerController.kt`, `Constants.kt`
- **Goal:** Raycast-based corner nudge when rising into clipped overhead obstacles (≤ `CORNER_CORRECT_PX`).

### T-011 — Footstep particles
- **Status:** In Progress
- **Agent:** team-agent-C (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §3.5
- **Files:** `entities/PlayerController.kt`, `rendering/ParticleSystem.kt`
- **Goal:** Alternate L/R foot particle every ~12 cm of grounded horizontal travel.

### T-018 — Tests for MapLevelLoader coordinate flipping
- **Status:** In Progress
- **Agent:** team-agent-D (parallel team)
- **Branch:** auto (worktree)
- **Started:** 2026-05-09
- **GDD ref:** §11, §15
- **Files:** new `core/src/test/kotlin/com/sohai/platformer/world/MapLevelLoaderTest.kt`
- **Goal:** Cover libGDX `flipY = true` y-coordinate translation.

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** In Progress
- **Agent:** <your-handle>
- **Branch:** claude/T-XXX-short-desc
- **Started:** YYYY-MM-DD
- **Depends on:** ...
- **Files:** ...
- **Goal:** ...
- **Done when:** ...
- **Progress notes:** (optional, append as you go)
-->

---

## Done

### T-001 — Migrate Hud.kt buttons to VisUI
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** HUD `Label`/`Table`/`Image` widgets migrated to `VisLabel`/`VisTable`/`VisImage`. Buttons were already VisUI. Compile clean, behavior unchanged.
- **Commit/PR:** 3fd1a91

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** Done
- **Completed:** YYYY-MM-DD
- **Outcome:** one-line summary of what shipped
- **Commit/PR:** <hash or PR link>
-->

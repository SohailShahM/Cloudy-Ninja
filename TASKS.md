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

### T-009 — World 0 Room 4 "First Cleanse" tutorial
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-008 ✅
- **GDD ref:** §7.1 (table row 0-4) and §7.2 prologue beat
- **Files:** new `levels/Level0_4.kt`, `LevelManager.kt`, `Hud.kt` (action button hint)
- **Goal:** Hazard blocks the only path. Action button gets a pulsing-ring hint. After Seed Slam cleanses, the hazard becomes ground. Final transition uses `screenFade` into `level1`.
- **Done when:** Cleanse is the only solution; HUD action button visibly hints; finishing fades into the existing `level1`.

### T-021 — Split GameScreen into focused subsystems
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §10.1
- **Files:** `screens/GameScreen.kt` (1179 LOC → ~300), new `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`, `screens/LevelTransitionController.kt`
- **Goal:** Extract from `GameScreen`: (1) `LevelRunState` — score, combo, spirit health, completion flags; (2) `LevelRenderer` — the entire `shapeRenderer.begin/end` block; (3) `LevelTransitionController` — goToNextLevel / dispose chain. `GameScreen` becomes a thin coordinator. No behaviour change.
- **Done when:** `GameScreen.kt` is ≤ 350 LOC; game compiles and existing tests pass.

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

---

## In Progress

_(none — all claimed tasks completed)_

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

### T-002 — Add Kotest specs for PlayerController state machine
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** 4 test files added: `PlayerControllerJumpTest`, `PlayerControllerMovementTest`, `PlayerControllerStateTest`, `PlayerControllerWallAndAbilityTest`. Covers coyote-time, jump-buffer, ability-swap, wall contacts.
- **Commit/PR:** merged via worktree

### T-003 — Save/load UI in Settings menu
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Save/Load/Delete buttons wired to SaveManager in SettingsScreen with toast confirmation.
- **Commit/PR:** 190d96b

### T-004 — Checkpoint restart via serialized GameState
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Player respawns at last checkpoint on death; `resumeCheckpoint` passed into GameScreen constructor.
- **Commit/PR:** 4617e9e

### T-005 — Kotest specs for WorldContactListener
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `WorldContactListenerTest` covers player_foot, player_wall_left/right, hazard kill, flashing invincibility.
- **Commit/PR:** 3db00d0

### T-006 — World 0 Room 1 "First Step" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_1.kt` added; single-screen room with one jump gap and one eco-token. Registered in LevelManager.
- **Commit/PR:** 2449d75

### T-007 — World 0 Room 2 "Long Fall" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_2.kt` added; teaches variable jump height (low ceiling) and coyote time (walk-off ledge). Two eco-tokens.
- **Commit/PR:** d4e37f2

### T-008 — World 0 Room 3 "Wall Climb" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_3.kt` added; 120 px wide chimney shaft, ~4 wall-jumps needed, safety net at bottom, horizontal EXIT sensor at top. Registered in LevelManager.
- **Commit/PR:** this branch

### T-010 — Corner correction in PlayerController
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Raycast-based corner nudge when rising into clipped overhead obstacles (≤ CORNER_CORRECT_M).
- **Commit/PR:** d09d0b1

### T-011 — Footstep particles
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Alternate L/R dust particles every 12 cm of grounded horizontal travel via `onFootstep` callback.
- **Commit/PR:** ed58182

### T-012 — Camera vertical platform snap
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `cameraTargetY` only updates when grounded or falling past threshold — no Y bobbing during jump arcs.
- **Commit/PR:** 1e22d03

### T-013 — Generate 8 base SFX via ProceduralSoundGenerator
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** jump, land, collect_token, collect_snapshot, death, checkpoint, level_complete, hazard_cleansed WAV files generated and placed in `assets/audio/sfx/`.
- **Commit/PR:** e38ce48

### T-014 — Wire SoundManager into gameplay events
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** All 8 canonical sounds wired into jump, land, collect, death, checkpoint, level_complete, and hazard_cleansed events.
- **Commit/PR:** 34b2cd3

### T-015 — Three save slots UI
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Main menu replaced Continue/New Game with 3 slot cards showing level, progress, deaths, last-played. Wired to SaveManager.
- **Commit/PR:** 890a472

### T-016 — Refactor levels to data-driven registry
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Level1/2/3 classes removed; single `TmxLevelDefinition` registry in `LevelRegistry.ALL`. Adding a level = one registry entry.
- **Commit/PR:** d7c77f4

### T-017 — Investigate intermittent native Box2D crash
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Root cause documented in BUGS.md; defensive fixes: friction-based platform carry (no stale body refs), contact-begin/end logging, isPlatformBodyValid guard, deferred body-destroy queue.
- **Commit/PR:** 2a80160

### T-018 — Tests for MapLevelLoader coordinate flipping
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `MapLevelLoaderCoordTest.kt` added; BehaviorSpec covers flipY=true/false centerOf formula, symmetry invariant, and moving-platform endY translation — pure math tests, no libGDX runtime needed.
- **Commit/PR:** this branch

### T-019 — Collect sparkles on eco-token and snapshot pickup
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** 6–10 particle burst spawned at collection site; additive cyan/yellow colors; reuses 200-particle pool.
- **Commit/PR:** 896d7d4

### T-020 — Apply Celeste-calibrated movement constants + asymmetric gravity
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Full constant table from GDD §2.1 in Constants.kt; asymmetric gravity + apex-hang gravityScale applied per frame; terminal velocity capped.
- **Commit/PR:** dccb872

### T-022 — Particle pool eviction tests
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `ParticleSystemTest.kt` covers overflow (silent drop), lifespan expiry, slot reuse.
- **Commit/PR:** 609ac4b

### T-023 — Zephyr third-character ability skeleton
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `ZephyrAbility.kt` implements Float (gravity 0.2f for 1.5 s, cooldown 3 s); radial WindTrail burst at activation; GameScreen cycles Ebo → Laya → Zephyr → Ebo; Zephyr renders as light-purple tinted sprite.
- **Commit/PR:** this branch

### T-026 — Per-level parallax background theming
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `ParallaxTheme` enum (ARID/WIND/ECO) added to `ParallaxBackground`; Level 1 = warm browns/golds, Level 2 = slate blues/whites, Level 3 = deep-to-bright greens. GameScreen selects theme by level ID.
- **Commit/PR:** this branch

### T-027 — CloudAtlasLibrary.get unit tests
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `CloudAtlasLibraryTest.kt` covers known-ID lookup, unknown-ID null, non-blank fields, and unique IDs.
- **Commit/PR:** fc297c3

### T-028 — Android lint + build verification
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `.github/workflows/ci.yml` created; runs `:core:compileKotlin`, `:core:test`, and `android:lint` on push/PR to main; uploads lint and test reports as artifacts.
- **Commit/PR:** this branch

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** Done
- **Completed:** YYYY-MM-DD
- **Outcome:** one-line summary of what shipped
- **Commit/PR:** <hash or PR link>
-->

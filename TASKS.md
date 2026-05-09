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
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **Files:** `core/src/test/kotlin/com/sohai/platformer/entities/`
- **Goal:** Existing tests cover jump/movement/wall basics. Add Kotest specs for: coyote-time edge cases, jump-buffer expiration, ability-swap mid-air, and ground/air state transitions across collision events.
- **Done when:** `./gradlew :core:test` passes with new specs, coverage of `PlayerController.update()` branches is meaningfully higher.

### T-003 — Save/load UI in Settings menu
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/SettingsScreen.kt`, `persist/SaveManager.kt`
- **Goal:** Add Save / Load / Delete buttons to `SettingsScreen` wired to `SaveManager`. Use slot 0 for now; show a toast/label confirming each action.
- **Done when:** Player can save mid-level, return to main menu, load, and resume at the saved level + character.

### T-004 — Checkpoint restart via serialized GameState
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-003
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`, `persist/GameState.kt`
- **Goal:** On player death, restore from the last `GameState` checkpoint instead of reloading the level from scratch. Capture checkpoint state when the player crosses a checkpoint trigger.
- **Done when:** Dying after a checkpoint respawns the player at the checkpoint with eco-tokens/snapshots collected up to that point preserved.

### T-005 — Kotest specs for WorldContactListener
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **Files:** `core/src/test/kotlin/com/sohai/platformer/physics/`
- **Goal:** Test that fixture `userData` strings (`ground`, `hazard`, `player_foot`, `player_wall_left`, `player_wall_right`) translate to the correct gameplay state on `beginContact` / `endContact`.
- **Done when:** New `WorldContactListenerTest.kt` covers each userData string with at least one positive and one negative case.

### T-006 — World 0 Room 1 "First Step" tutorial
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
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

### T-010 — Corner correction in PlayerController
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §2.3
- **Files:** `entities/PlayerController.kt`, `Constants.kt` (already has `CORNER_CORRECT_PX` per §2.1)
- **Goal:** When the player's head clips an overhead obstacle by ≤ `CORNER_CORRECT_PX` (~6 cm) while `vy > 0`, nudge them horizontally past the corner instead of killing vertical velocity. Implement via raycast above the head when rising.
- **Done when:** Manual playtest of level3 wall-jump shaft no longer "stubs" the player on overhead corners; existing PlayerController tests still pass.

### T-011 — Footstep particles
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §3.5
- **Files:** `entities/PlayerController.kt`, `rendering/ParticleSystem.kt`
- **Goal:** Every ~12 cm of horizontal travel while grounded, alternate L/R foot and spawn one small circle particle (lifespan 0.2 s, no movement). Distinguishes movement direction visually.
- **Done when:** Walking left vs right is visibly distinguishable from particles alone; cap respects existing 200-particle pool.

### T-012 — Camera vertical platform snap
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §4.3
- **Files:** wherever the camera controller lives (likely `screens/GameScreen.kt` per §4.4 sketch)
- **Goal:** Only follow vertical position when the player is grounded *or* falling past a y-threshold. Locks the camera during jump arcs so the world doesn't bob with every jump.
- **Done when:** Standing-jumping in level1 keeps the camera still; falling off a high platform still pans down to keep the player on screen.

### T-013 — Generate 8 base SFX via ProceduralSoundGenerator
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
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
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §10.4
- **Files:** `levels/Level1.kt`, `Level2.kt`, `Level3.kt`, `LevelManager.kt`, new `levels/TmxLevelDefinition.kt`
- **Goal:** Replace the three near-identical `LevelN` classes with a single registry of `TmxLevelDefinition(id, name, mapPath, spawnX, spawnY, levelWidthPx, exitX, ecoTokens, snapshots)`. `LevelManager` reads from the registry.
- **Done when:** All three existing levels still play identically; adding a 4th level requires only a new registry entry (no new class).

### T-017 — Investigate intermittent native Box2D crash
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §0 "KNOWN ISSUE: Intermittent native Box2D crash"
- **Files:** `entities/PlayerController.kt`, `physics/WorldContactListener.kt`, `screens/GameScreen.kt`
- **Goal:** Reproduce the `EXCEPTION_ACCESS_VIOLATION` in `gdx-box2d64.dll` from `Body.jniGetPosition`. Follow the investigation path in §0: disable platform-carry to isolate, add logging in contact begin/end to verify `platformContacts` map invariants, hunt for stale body references.
- **Done when:** Either a reproducer is documented in a new `BUGS.md` with stack trace and minimal repro steps, OR a fix is shipped with a regression test.

### T-018 — Tests for MapLevelLoader coordinate flipping
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §11 (table row "MapLevelLoader coordinate flipping"), §15 ("the dramatic playerY=-0.27 vs 0.73 bug history")
- **Files:** new `core/src/test/kotlin/com/sohai/platformer/world/MapLevelLoaderTest.kt`
- **Goal:** Cover the libGDX `flipY = true` translation that has historically caused regressions. Use synthetic `RectangleMapObject` fixtures.
- **Done when:** Tests catch sign-flip and origin-shift mistakes in y-coordinate handling.

---

## In Progress

_No tasks currently in progress._

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

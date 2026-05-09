# TASKS.md — Multi-Agent Task Board

Coordination file for parallel agents working on Cloudy Ninja. Read [AGENTS.md](AGENTS.md) first for project context.

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

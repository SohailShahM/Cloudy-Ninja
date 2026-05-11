# T-037 — Achievement system + toast notifications (Claude Code)

**Target tool:** Claude Code (in a terminal)
**Ticket tier:** M — multi-file, cross-cutting
**Autonomous:** yes (auto-merge eligible)

## Launch procedure

1. Open a fresh Claude Code session in the repo: `claude` in the repo root
2. Paste the prompt body below
3. Claude reads `START_HERE.md`, claims the ticket, dispatches Sonnet sub-agents for the parallel mechanical work (12 achievement conditions)
4. When the PR opens, auto-merge fires on CI green

## Prompt body (paste this into a fresh Claude Code session)

```
Read START_HERE.md and work on T-037 from TASKS.md. Your identity is `claude-code-sonnet`.

This ticket is cross-cutting (state + UI + persistence + game-event hooks). Plan once, then dispatch sub-agents in parallel for the mechanical pieces — specifically, the 12 achievement conditions can be split into 3 sub-agent batches of 4 each.

Read these first:
- START_HERE.md (your entry point — identity, claim protocol, hard limits)
- AGENTS.md (architecture — note the GameScreen subsystem split in T-021: state goes in LevelRunState, drawing in LevelRenderer, transitions in LevelTransitionController)
- LEARNINGS.md (gotchas — especially: persistence calls do NOT belong in render/update hot paths)
- GDD_ADDENDUM.md §22 ("Achievement System Spec") for the 12 achievement definitions

Move T-037 from `## Todo` to `## In Progress` in TASKS.md when you claim it. Branch: `claude/T-037-achievements`.

Open a PR titled "T-037: achievement system + toast notifications" when done. CI smoke test (T-A1) must pass. The PR will auto-merge on green.

If you hit determinism-sensitive code paths (recording achievement times, etc.), check DETERMINISM.md first.

If you hit ambiguity you can't resolve, append to QUESTIONS.md and release the claim rather than guessing.
```

## What Claude will do (rough plan)

1. Add `progression/Achievement.kt` (data class) + `progression/AchievementRegistry.kt` (the 12 entries)
2. Add `unlockedAchievements: Set<String>` to `GameState`
3. Add `screens/AchievementToast.kt` (slides in from top-right, 2.4 s hold, fades out)
4. Hook unlock checks in `LevelRunState.update()` (first_jump, first_cleanse, eco_sweep, no_death_run)
5. Hook unlock checks in `LevelTransitionController` (speed_demon, world clears)
6. Render toast in `GameScreen` above Layer 4 (HUD)

## Done when
- At least 6 achievements can be unlocked during normal play
- Toast appears + dismisses cleanly + doesn't overlap
- Unlocked set persists across sessions
- Compiles clean, smoke test passes

# T-038 — Ghost replay in time trials (Claude Code, supervised)

**Target tool:** Claude Code (in a terminal)
**Ticket tier:** M — determinism-sensitive
**Autonomous:** **NO** — you must supervise this one

## Why this needs supervision

Ghost replay requires that recorded inputs produce identical end-states on replay. That makes it determinism-sensitive — see `DETERMINISM.md` for sites that currently break replay (`MathUtils.random` in boss + Ebo ability, etc.). T-038 must either avoid those sites or coordinate a fix with T-A3.

Pure autonomous flow is risky here. Sit with the session.

## Launch procedure

1. Open a Claude Code session in the repo
2. Paste the prompt body below
3. **Stay in the terminal** — Claude will likely ask determinism-related questions you need to answer
4. After the PR opens, manually review the diff before letting auto-merge fire (or disable auto-merge for this one PR if you'd rather merge by hand)

## Prompt body

```
Read START_HERE.md and work on T-038 from TASKS.md. Your identity is `claude-code-sonnet`.

This ticket is determinism-sensitive. BEFORE writing any code:
1. Read DETERMINISM.md fully
2. Identify which sites listed there would affect a ghost replay of a time-trial run
3. Decide: (a) avoid those sites entirely (e.g. record only player kinematic state, not derived random effects), (b) seed the recordings with the RNG state at start, or (c) document that ghost replays may drift from the original run by ≤X cm and ship anyway
4. Post your chosen approach in QUESTIONS.md as a decision log before starting code, so I (the user) can confirm before you build

Then proceed per the ticket Goal + Done-when in TASKS.md. Branch: `claude/T-038-ghost-replay`.

If your approach requires changes outside the ticket's Files: list (e.g. seeding RNG in PlayerController), stop and post to QUESTIONS.md.

Open a PR titled "T-038: ghost replay in time trials" when done. CI must pass. I'll manually review before merging.
```

## What to look for in the PR

- Does the ghost recording capture enough state? Position + facing + character + frame index minimum.
- Does it serialize cleanly through `SaveManager`?
- Does the replay actually run on a subsequent time-trial start?
- Most importantly: **does the ghost stay in sync with the level geometry across multiple plays of the same level?** Test by:
  1. Run a time trial on Level 1 → save best time + ghost
  2. Run another time trial on Level 1 → the ghost should retrace your previous run
  3. If the ghost drifts immediately, the recording is broken; if it drifts after 20+ seconds, that's the determinism issue

## Done when
- New best time saves a ghost JSON
- Subsequent time-trial runs show the ghost moving through the level
- Ghost doesn't interfere with gameplay (no collisions, render only)
- Compiles clean, smoke test passes
- You (human) have reviewed and approved the PR before auto-merge fires

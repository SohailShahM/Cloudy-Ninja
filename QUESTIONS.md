# QUESTIONS.md

Append-only log of questions autonomous agents post when they hit ambiguity. The user (or a planning session) answers them; once answered, the agent who posted can resume — or the ticket gets re-routed.

Format per entry:
- Date + agent identity + ticket
- Question
- Status: `open` | `answered` | `resolved`
- Answer (filled in by user or planner)

---

_(no questions yet — autonomous agents append here as they encounter ambiguity)_

### 2026-05-12 — antigravity — (no ticket)
- **Question:** I was asked to pick an unclaimed task from TASKS.md whose Tool: matches my identity (`antigravity`). However, there are no tasks currently available. The only task involving me is `T-046`, but its dependency `T-031` is not yet `Done`. Please route a new task or resolve dependencies.
- **Status:** `resolved`
- **Answer:** Resolved 2026-05-12. Queue is now populated for `antigravity`: **T-049** (climate-source compilation — priority; unblocks T-045), **T-047** (audio asset research), **T-048** (itch.io listing style guide). T-046b is already claimed and in progress. T-031 has also since merged (PR #14), so any future T-046-family follow-up that depended on it is now unblocked. Next pickup: re-read `TASKS.md ## Todo` on `main`.

### 2026-05-12 — claude-code-sub-agent — T-120
- **Question:** The i18n coverage audit (full report at `research/i18n-coverage.md`) found that ~98% of widget construction sites already route through `Strings.get(...)` / `Strings.format(...)`. Three **high-confidence** leftovers are worth keying:
  1. `screens/VictoryScreen.kt:61` — `"−%.2fs under best"` (full English phrase shown after a time-trial PB)
  2. `screens/VictoryScreen.kt:62` — `"+%.2fs slower"` (sibling of #1)
  3. `screens/LevelRenderer.kt:500` — `"[Locked]"` (label drawn above locked portals in Level0_0)

  Seven **lower-confidence** candidates exist (HUD/Victory/LevelComplete time-format templates like `"%d:%02d.%d"`, plus a stats `"00:00.000"` sentinel and an ISO date format). These are numeric format strings that could matter for non-English locales (decimal separator, M/D/H ordering) but aren't strictly English copy.

  **Ask:** Should I spawn ONE `copilot-agent` follow-up ticket covering just the three high-confidence cases (#1–3), defer the numeric format templates until a real second locale lands, or wire all ten in a single sweep?
- **Status:** `open`
- **Answer:** _(pending)_

# LEARNINGS.md

Append-only log of non-obvious gotchas. **Read before claiming a task in a related area.**

Format per entry:
- Date + ticket number
- Symptom (what broke)
- Cause (root cause, not just first explanation)
- Fix (what worked)
- Cost (time / tokens wasted before discovery, so future agents weight risk correctly)

---

## 2026-05-11 — flipY trap in TMX level loading (T-043)

**Symptom:** Players spawn-die immediately on any campaign level (Level1, Level2, Level3). Player Y reads ~-0.27m at rest.

**Cause:** libGDX's `TmxMapLoader` already flips rectangle Y coordinates internally (`r.y = mapHeight - tiledY - height`). `MapLevelLoader.load(..., flipY=false)` then trusted that value as-is, placing the ground rectangle at the **top** of the camera viewport. Player spawns at y=0.8m, has no ground beneath it, falls past the pit safety net.

**Fix:** `TmxLevel.setup()` calls `MapLevelLoader.load(..., flipY=true)`. The second flip cancels libGDX's internal flip; ground ends up at y≈0 (bottom of world) where it belongs.

**Cost:** ~30 min, ~15k tokens of investigation. Would have been instant if AGENTS.md flagged it.

**Now documented in:** AGENTS.md "Levels" section.

---

## 2026-05-11 — Box2D native crash on portal transition (T-043)

**Symptom:** `EXCEPTION_ACCESS_VIOLATION` in `gdx-box2d64.dll` when the player walked into a hub-world portal.

**Cause:** Portal contact handler called `game.screen = GameScreen(...)` + `dispose()` mid-render, while the Box2D step and ShapeRenderer were still iterating bodies/fixtures of the world being torn down.

**Fix:** Defer screen transitions to end-of-frame via `LevelRunState.pendingPortalTarget: String?`. `GameScreen.render()` checks this field after all other rendering is complete and only then performs `dispose()` + screen swap.

**Cost:** ~25 min to diagnose (libGDX docs are quiet on this lifecycle constraint).

**Lesson:** Never call `dispose()` on a Screen from inside its own `render()` call. Always defer.

---

## 2026-05-11 — SaveManager per-frame disk read spam (T-043)

**Symptom:** Frame rate hitches in hub world; `[SaveManager] Loaded from save_slot_1.json` log line spammed 60× per second.

**Cause:** `LevelRenderer.renderWorld()` called `SaveManager.loadGame().completedLevels` for each portal obstacle every frame to color it locked/unlocked. Every call hit disk.

**Fix:** Added `private val cache = mutableMapOf<String, GameState>()` to `SaveManager`. `loadGame()` returns from cache on subsequent calls; `saveGame()` updates the cache; `deleteSave()` evicts.

**Cost:** ~10 min once spotted in logs.

**Lesson:** If you see ANY function from `persist/` being called inside a `render*()` or `update()` loop, that's a perf bug. Persist calls belong in event handlers (save on transition, load on init), not hot paths.

---

## 2026-05-11 — Headless rendering caveats for AI testing (T-A1)

**Symptom:** Originally planned to run smoke tests headless via libGDX's `HeadlessApplication`. Quickly realized `ShapeRenderer`, `SpriteBatch`, `FreeTypeFontGenerator`, and `MusicManager.play()` all hard-crash without a GL context (`Gdx.gl == null`).

**Solution:** Skip true headless. Use `xvfb-run` in CI to provide a virtual framebuffer; the game runs as if windowed, just into a virtual display the test harness throws away. Far cheaper than gating every render call with `if (!Constants.HEADLESS)`.

**Cost:** ~20 min of analysis before pivoting.

**Lesson:** "Headless libGDX" is a trap for games that rely on rendering systems. Use `xvfb-run` instead.

## 2026-05-11 — Smoke autopilot hopping levels via portals (T-A1)

**Symptom:** AI smoke workflow stuck on 5 of 8 levels for 9+ minutes per job; same 3 levels always passed (level0_3, level2, level3), same 5 always hung (level0_0, level0_1, level0_2, level0_4, level1). Eventually killed by GitHub's 6-hour job timeout.

**Cause:** The smoke autopilot walked the player into a portal sensor (hub world) or exit sensor (tutorials/campaign). That triggered `GameScreen.render()` to deferred-construct a new `GameScreen`, which built a fresh `LevelRunState` with `debugAutoQuitTimer` reset to 10s. The smoke autoquit could never fire because the autopilot kept hopping levels and resetting the timer.

The 3 levels that *did* pass were ones where BasicAutopilot can't reach an exit: level0_3 (vertical wall-climb shaft), level2 (needs Laya wind dash), level3 (boss arena). They hit autoquit cleanly.

**Fix:** Added `Constants.SMOKE_MODE` flag bound to `-Dcloudy.smokeMode=true`. `GameScreen.render()` now suppresses portal-transition, level-complete-transition, and game-over→MainMenu transitions when this flag is on. Smoke runs stay in the level they were launched into for the full 10s autoquit window.

**Belt-and-suspenders:** Added `timeout 90` wrapper around the smoke shell command in `.github/workflows/ai-smoke.yml`. If the JVM ever hangs again for any reason, the shell forces exit at 90s. Exit code 124 = timeout fired, which counts as a CI failure.

**Cost:** ~30 min of confused CI debugging across 3+ runs. Looked at it from queue/concurrency angles first; the smoke-mode-transition theory landed only after I noticed the pass/fail split correlated with "can the autopilot reach an exit in this level."

**Lesson:** When designing an autonomous test runner, think about every state-machine path the bot's inputs could trigger. If the system being tested can swap out from under the bot (screen change, level reload, etc.), the test harness needs to either survive that or actively suppress it.

## 2026-05-11 — Smoke autoquit blocked by Cloud Atlas overlay (T-A1, follow-up)

**Symptom:** Smoke runs hung at 240s CI timeout. Game-side log showed normal startup + autopilot firing + `[SaveManager] Saved`, then silence. No `Auto-quit.` log line. No `[smoke]` summary line.

**Cause:** `GameScreen.render()` gates `runState.update(delta)` on `!isPaused && atlasOverlay == null`. The smoke autopilot drives the player right, hits a Cloud Atlas snapshot pickup (e.g. `silver_iodide` at x=4.5m on Level 1, reachable in ~1 second), the overlay opens, update() stops being called, and the autoquit timer in `LevelRunState` never decrements. The JVM is technically alive — it's still rendering the overlay — but the smoke-test exit path can't fire.

**Fix:** In smoke mode, bypass the gate entirely. `GameScreen.render()` now does:
```kotlin
if (Constants.SMOKE_MODE) {
    runState.update(clampedDelta)  // unconditional — keeps autoquit ticking
} else if (!isPaused && atlasOverlay == null) {
    ...
}
```

**Cost:** Two failed CI runs (one at 90s timeout, one at 240s timeout) before identifying the overlay as the blocker. About ~25k tokens of debugging.

**Lesson:** When building a smoke-test harness that relies on a game-internal timer to exit cleanly, audit EVERY code path that can pause/gate the game's update loop. Overlays, pause menus, hitstop, game-over modal — all of them break the exit timer. Either bypass them in test mode, or move the test-exit timer somewhere that ticks unconditionally (e.g. directly inside `render(delta)` not gated by any state).

## 2026-05-11 — Antigravity skips PR-open + TASKS.md updates (T-046a)

**Symptom:** Antigravity (Gemini 3.1 Pro backend) was asked in the launch prompt to: (1) write `art-research/tileset-candidates.md`, (2) move T-046a from Todo→Done in TASKS.md, (3) open a PR titled "T-046a: tileset research". It did (1) flawlessly in ~5 minutes — high-quality 12-candidate comparison table — but skipped (2) and (3). The branch `antigravity/T-046a-tileset-research` was pushed; the PR had to be opened manually by another agent.

**Cause (theory):** Antigravity's agent loop appears to consider the task "done" once the requested file content is committed and pushed. Workflow housekeeping steps phrased as a numbered list inside the prompt aren't treated as deliverables of equal weight — only as a checklist that the agent self-marks complete without verifying. Could also be a permissions issue (Antigravity bot might not have PR-open scope on the repo), but the branch push worked so write scope clearly exists.

**Workaround:** When prompting Antigravity, structure follow-through actions as SEPARATE explicit deliverables, not list items. E.g.:
> Deliverable 1: write the file.
> Deliverable 2: separately, you must also open a PR — confirm with the user before exiting.

Or simpler — accept that Antigravity is great at the *artifact* and let a different agent (or human / planner-Claude) handle the workflow wrap-up. Update `prompts/T-XXX-antigravity.md` templates to explicitly note "PR-open is the user's job after Antigravity finishes."

**Cost:** ~5 min of manual cleanup. Not painful but worth automating away for future Antigravity tickets. Time-to-output (5 min for full research) is genuinely impressive — this tool is the right fit for content-generation tickets.

**Lesson:** Different agent platforms have different "definition of done." Copilot agent goes all the way through to opening a PR. Claude Code agent commits + pushes + opens PR. Antigravity stops at commit. Update tool runbooks in `START_HERE.md` to flag this so future routing decisions reflect actual end-to-end behavior.

## 2026-05-12 — Antigravity doesn't catch art-style/perspective mismatches (T-046a)

**Symptom:** Antigravity (Gemini 3.1 Pro backend) ran T-046a and produced a 12-candidate comparison in ~5 min. One ARID-theme candidate — "Whispers of Avalon Desert" on OpenGameArt — was rated `4/5 theme fit`. Visual review revealed it's a **top-down RPG-perspective** tileset (skull dunes, palm oasis viewed from above). Cloudy Ninja is a **side-scrolling platformer**. Geometry incompatible.

**Cause:** Antigravity's research loop reads asset metadata (title, license, file count, description text) but doesn't perform visual analysis on preview images. Camera-perspective and visual-style compatibility are invisible to text-only analysis. The agent confidently rates "theme fit" based on description ("desert tileset") without checking whether the geometry will slot into a side-scroller.

**Workaround:** Future Antigravity art-research prompts must explicitly require: *"For each candidate, examine the preview image and verify the intended camera perspective (top-down vs side-scrolling). Reject any candidate whose perspective doesn't match Cloudy Ninja's side-scrolling 2-D platformer style."* Alternatively, accept that art research always needs a human visual-review pass before acting on it.

**Cost:** ~5 min of user visual-review to catch (1 of 4 top recommendations needed re-research). The other three candidates — Kenney pixel-platformer, Pixel Art Forest, Bluegrass — were correct.

**Lesson:** Research-bot tools excel at *breadth* but miss *style/perspective compatibility*. Always do a quick visual pass before acting on art-research output. Bake this into `prompts/T-046a-antigravity.md` and any future art-research prompts.

## 2026-05-12 — GitHub Actions spending limit hit on private repo (PR #13, #14)

**Symptom:** All CI jobs on PRs #13 and #14 returned `failure` in 4–10 seconds with zero step output. Initial diagnosis suggested concurrency cancellation, but annotation on the smoke-job check-run revealed the real cause:

> "The job was not started because recent account payments have failed or your spending limit needs to be increased. Please check the 'Billing & plans' section in your settings"

**Cause:** A private GitHub repo's Actions minutes were exhausted within one work session. The heavy multi-AI workflow burned through the free-tier allocation:
- ~15+ PRs opened
- Each PR runs 9 CI jobs (1 lint + 8 smoke matrix)
- Several PRs needed re-runs due to bug-fixing cycle (T-A1 alone went through 8 layers of fixes = 8 × 9 = 72 jobs)
- Each smoke job runs 30s–4 min

Personal GitHub Pro (via Edu) gives 3000 minutes/month on private repos. We hit that in one day.

**Fix paths (cheapest first):**
1. **Raise spending limit** in https://github.com/settings/billing/spending_limit. Default is $0 — set to $5 to enable overage billing (extremely unlikely to actually trigger charges for a project this size).
2. **Wait for monthly reset** — billing anniversary.
3. **Make repo public** — free unlimited Actions minutes. The original "Option A" we considered when transferring out of MashxLabz org.

**Workaround during the outage:** admin-merge bypasses required-CI checks (we already use it for `required_conversation_resolution`). Risk: changes land without smoke validation, so the autonomous flow effectively becomes "trust the developer / sub-agent" until CI returns.

**Lesson:** Multi-AI autonomous flows on private repos can exhaust Actions minutes faster than expected — especially during a bug-fix cycle where each fix re-runs the full matrix. **Public repos are the right default for indie game projects** unless there's a compelling reason to stay private (the trade-off we considered when transferring from MashxLabz org). Worth re-evaluating that decision once active development slows down.

**Cost:** Detection was ~10 min of confused log-staring. The actual fix is one click in GitHub settings — once the user is back.

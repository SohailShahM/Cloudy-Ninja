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

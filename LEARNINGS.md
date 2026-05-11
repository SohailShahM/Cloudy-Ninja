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

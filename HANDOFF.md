# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-14 by Claude Opus — visual feedback loop closed end-to-end + thin-static-platform sprite-sink resolved. Closed-loop "automate testing AND adjusting" pipeline operational; 28+ PRs merged across the two sessions; 5 new LEARNINGS entries.

## 🚨 Awaiting user action on return

### PR #161 — T-186 Ebo MH1 sprite integration
Still **OPEN** from two sessions ago. The fixes that landed since then (T-209 Wind Dash visibility, T-A14 autotuner infrastructure, thin-platform visual extension) make the visual context cleaner for verifying Ebo against the anime-pixel MH1 sprite. Merge when ready; expect to re-run the autotuner against MH1's sprite shape afterward.

### Live game state (verified working as of this handoff)
1. **Brightness** ambient `(0.39, 0.44, 0.53)` + Settings → Accessibility → Brightness slider (0.0–2.0)
2. **MainMenu title scrim** (T-173) — title legible against dark backdrop
3. **Invisible barriers** (T-174) — fixed; `TileRenderer` was dropping sub-32-px obstacle tiles
4. **Snappy movement** (T-175 Celeste-like ground damping)
5. **Laya Wind Dash slow-descent + camera zoom** (T-176 + T-209 fix) — now visibly triggers in-game
6. **Sprite foot offset 0.3f** — Ebo/Laya/Zephyr all correctly seated on thick static surfaces
7. **Thin static platforms** dynamically extend visual rect downward (only platforms <0.30m total) so character feet don't dangle in air. Moving platforms unchanged (collision matches visual).
8. **Visual checkpoint capture + autotuner pipeline** operational

## How to drive the visual review loop (next session)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Smoke + capture (PowerShell needs --% before -D flags):
.\gradlew.bat --% :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 -Dcloudy.captureCheckpoints=true
```

PNGs land at `assets/build/visual-checkpoints/` (yes — `assets/build/`, lwjgl3 task's CWD is `assets/`). Then Read each PNG via the multimodal Read tool. Run `python scripts/foot_offset_autotuner.py` to recalibrate Ebo's foot offset against a fresh capture (V0 is Ebo-only).

**For manual play screenshots:** F12 anywhere — writes to `~/.cloudy-ninja/screenshots/manual-{ScreenName}-{ts}.png`. (Y-flip applied as of 2026-05-14.)

## What landed in the latest pass

### Visual feedback loop (closed)
- **T-A10** Visual checkpoint capture system (6 named PNGs)
- **T-A13** Session-start visual review workflow doc (`docs/SESSION_START_VISUAL_REVIEW.md`)
- **T-A14 V0** Foot-offset autotuner (Pillow + numpy script, Ebo-only)
- **CheckpointCapture Y-flip + timing fixes** (inline)
- **ScreenshotWriter Y-flip fix** (T-139/T-147 same bug)

### Sprite/platform calibration (closed)
- **T-209** Laya slow-descent + camera zoom root-cause fix (zoom-revert was happening before player render — sprite clipped out of view)
- **T-210** Box2D bounds diagnostic doc — confirmed math is identical static-vs-platform; float-vs-sink is purely surface-thickness vs sprite-foot-offset mismatch
- **Moving-platform extension experiment + revert** (overshot; user clarified the issue was thin STATIC platforms)
- **Thin-static-platform dynamic visual extension** — platforms <0.30m total height get `extraDown = max(0, 0.30 - height)` added to their visual rect. Collision unchanged. **User confirmed this looks correct in-game.**

### Closed PRs (won't re-land)
- **PR #169 (T-A16 visual regression CI)** closed due to merge conflict on `scripts/requirements-visual.txt` after T-A14 added the same file. **T-A17 spec'd as the rebased redo** (drops the duplicate requirements addition).

### Specs filed for next session
- **T-A17** Re-land visual regression CI (rebased)
- **T-A18** Foot-offset autotuner V1 — tighter character detection (V0 heuristic was greedy; reported gap was window-bottom rather than sprite-feet)
- **T-200-T-205** T-046 gap-fill assets (boss sprite, dash/cast/wall-slide anims, lightning VFX, Cloud Atlas UI flourishes)
- **T-182-T-185** Claude Design batch (MainMenu polish, itch.io landing, Cloud Atlas card, pitch deck)
- **T-187/T-188/T-189-T-193** Character + biome + enemy integrations (held on PR #161 verification first)
- **T-168** Pre-alpha visual font verification (still pending; Inter swap was clean per captured PNG, but human eyeball pass on all surfaces is the gate)

## Source-side quirks pinned this session

1. **`ScreenUtils.getFrameBufferPixmap` returns bottom-up framebuffer** — explicit `flipY` required before PNG write. Applied to CheckpointCapture + ScreenshotWriter. See `LEARNINGS.md` 2026-05-14 entry.
2. **PowerShell needs `--%` stop-token** before `-D...` flags or Gradle sees the flags as task names.
3. **Smoke autopilot won't auto-Play MainMenu** — pass `-Dcloudy.smokeLevel=level1` to enter a level directly.
4. **lwjgl3 :run task CWD is `assets/`** — relative paths land under `assets/build/...`, not `<repo>/build/...`.
5. **Visual captures must wait past screen-fade-from-black (~1.5s)** — `level1-start` checkpoint queues from `render()` gated by `runState.levelTimer >= 1.5f` (not `show()`).
6. **Box2D rest-position math IS identical for static tiles vs moving platforms** (T-210 finding). Float-vs-sink illusions come from sprite-foot-offset interacting with surface visual thickness, not from physics.
7. **For thin platforms (<0.30m total height)**: extend visual rect downward by `max(0, sinkHide - height)` where `sinkHide` = sprite foot offset. Collision unchanged; rendering does the cover-up.

## Architectural smells status

- ✅ Dual screen-shake systems (T-169 closed)
- ✅ Silhouette overlay hack (T-170 closed)
- ✅ Per-screen input clobbering (T-171 + T-172 closed)
- 🟡 **Sprite-vs-surface-thickness coupling** — currently dynamic-extension is a per-render-path patch (procedural ground does it; TileRenderer doesn't yet; MovingPlatform reverted). If a third thin-render surface appears, extract the pattern into a `SurfaceVisualExtender` helper. Tracked informally in this session's LEARNINGS entry; not yet ticketed.
- 🟡 **Two screenshot capture paths** — `CheckpointCapture` (T-A10) and `ScreenshotWriter` (T-139/T-147) both need the same Y-flip + framebuffer-capture code. Future refactor: extract to a `PixmapUtils.captureFlippedFramebuffer()` shared helper. Cosmetic; not blocking.

## Working-tree hygiene (refreshed gitignore this session)

`assets/saves/checkpoint_autosave.json` and `assets/audio/sfx/achievement_unlock.wav` were churning each play session. Now ignored. Combined with prior T-194 patterns (`save_slot_*.json`, `ambient_*.wav`), the working tree should be clean post-play.

## Repo state: public + proprietary-licensed (unchanged)

Admin-merge default. Direct push works for docs/TASKS/LEARNINGS via admin bypass. Used ~15× this session.

## At end of your session

1. Bump "Last updated" + summary
2. Update "Awaiting user action" — remove what's done, add new gates
3. Capture new gotchas in `LEARNINGS.md` and reference here
4. Commit + push to main (direct push via admin bypass for docs-only)

# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-14 by Claude Opus — closed-loop visual-test build-out session. **23 PRs merged + 6 specs + closed-loop visual auto-tune pipeline now operational.**

## 🚨 Awaiting user action on return

### Visual review — try the new build

1. `git pull origin main`
2. `./gradlew :lwjgl3:run` — should see:
   - **Brightness:** ambient now `(0.39, 0.44, 0.53)` after the dial-down — should read as dim-daytime, not cave-dark
   - **Settings → Accessibility → Brightness slider** (T-208) — live in-game tuner, range 0.0–2.0, default 1.0
   - **Sprite foot alignment:** the autotuner (T-A14) calibrated `SPRITE_FOOT_OFFSET_EBO = 0.030f` based on `level1-start.png` pixel analysis. The 0.3 → 0.03 jump should make Ebo's feet sit on tile/platform tops cleanly. **Laya + Zephyr still at 0.3f** — V0 was Ebo-only.
   - **Laya Wind Dash slow-descent (T-209 fix):** the slow-fall + camera zoom-out should now actually trigger in-game. T-176 shipped this logic but a projection mismatch made the player invisible during the dash. T-209 split the zoom apply/revert so the player sprite shares the world-space zoomed projection.
3. F12 anytime captures a manual screenshot to `~/.cloudy-ninja/screenshots/`. (If you take one, paste the path; I can Read it visually.)

### PR #161 — T-186 Ebo MH1 sprite integration

Still **OPEN** from the prior session. Needs your visual sign-off. With T-209 + T-A14 now landed, Ebo's MH1 sprite path will:
- Render the actual itch.io anime-pixel art instead of procedural
- Use a separate sprite-world-size (0.80×0.80m) — different from the 0.32×0.80m procedural
- Need ITS OWN foot-offset calibration once merged (T-A14 V0 calibrated for procedural Ebo only)

Merge it when you're ready; expect to re-run the autotuner against MH1's sprite shape afterward.

## What landed this session

### User-reported bugs investigated
- **T-126** Calibri legal-blocker → Inter (CC0) shipped earlier session
- **T-173** MainMenu title scrim (visually confirmed working via captured PNG)
- **T-174** Invisible barriers in levels: `TileRenderer.kt` was dropping sub-32px obstacle tiles — root-cause fix shipped
- **T-175** Snappy ground movement (Celeste-like baseline)
- **T-176** Laya Wind Dash slow-descent — code shipped, but didn't visibly trigger
- **T-209 (this session)** Found why T-176 didn't trigger: zoom apply/revert ordering put the player sprite outside the zoomed projection. Fixed by splitting revert into a separate call from `GameScreen.render()`.

### Auto-test/auto-tune pipeline (user mandate: "automate the testing AND adjusting")
- **T-A10** Visual checkpoint capture system — 6 named PNGs land in `assets/build/visual-checkpoints/` when `cloudy.captureCheckpoints=true`
- **T-A13** Session-start visual review workflow doc (`docs/SESSION_START_VISUAL_REVIEW.md`)
- **T-A14 V0** Foot-offset autotuner (Pillow + numpy script). Calibrated `SPRITE_FOOT_OFFSET_EBO` from 0.3 → 0.030 based on the captured PNG.
- **T-210 (this session)** Box2D bounds investigation — `research/box2d-bounds-investigation.md`. Diagnosed that math is identical static-vs-moving-platform; the float-vs-sink illusion was purely SPRITE_FOOT_OFFSET miscalibration.

### CheckpointCapture bugfixes (inline edits this session)
- **Y-flip**: GL framebuffer is bottom-up; PNG is top-down. Original captures came out upside-down (HUD text was "dewS" / "noitcA"). Fixed via manual row-reverse copy.
- **Level-start timing**: capture was firing during screen-fade-from-black → PNG was all-black. Moved the queue logic from `show()` into `render()` gated by `runState.levelTimer >= 1.5f` so it fires post-fade.

### Other this-session shipments
- **T-208** Brightness slider in Settings (live multiplier; range 0.0–2.0; persists)
- **T-194** gitignore for save slots + procedural audio gens
- **T-172** GlobalInputRouter Phase B (closed architectural smell #3 — all 14 Screen files migrated, polling fallbacks deleted)
- **T-180** SpriteSheetFactory + SheetCharacterAtlas + AnimationStateMachine scaffold
- **T-198** Dev log gating behind `cloudy.devLogs`
- Plus prior-section work: T-181 manual-download CC0 pack placement + 48-px downsample

### Specs filed for next session
- **T-A17** Re-land visual-regression CI (PR #169 closed due to merge conflict with T-A14's identical requirements file; redo without that file)
- **T-A18** Foot-offset autotuner V1 (tighter character detection; V0 heuristic was greedy)
- **T-200-T-205** T-046 gap-fill: Storm Sentinel boss sprite, per-character dash/cast/wall-slide anims, lightning VFX, Cloud Atlas UI flourishes
- **T-182-T-185** Claude Design batch (MainMenu polish, itch.io landing, Cloud Atlas card, pitch deck) — user-driven Phase A
- **T-187/T-188/T-189-T-193** Character + biome + enemy integrations (held on PR #161 visual verification first)

## How to use the visual review pipeline (next session)

```powershell
# 1. Generate fresh PNGs (background; auto-quits in ~30s):
.\gradlew.bat --% :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 -Dcloudy.captureCheckpoints=true

# 2. List + Read them:
ls assets/build/visual-checkpoints/
# Claude: use Read tool on each .png

# 3. If foot offset needs retune (Ebo only in V0):
python scripts/foot_offset_autotuner.py
# script edits SpriteFactory.kt; commit + push
```

Important quirks:
- **PowerShell needs `--%`** before `-D...` flags (else PowerShell mangles them into Gradle task names)
- **Need `-Dcloudy.smokeLevel=level1`** — otherwise autopilot hangs at MainMenu (no auto-Play behavior)
- **Output path is `assets/build/visual-checkpoints/`** (not `build/...`) because the lwjgl3 run task's CWD is `assets/`

## Repo state: public + proprietary-licensed (unchanged)

Admin-merge default. Direct push works for docs/TASKS/LEARNINGS via admin bypass — used ~10× this session.

## Source-side quirks pinned this session

1. **Smoke autopilot only triggers gameplay (Level entry)** — MainMenu has no autopilot. Use `cloudy.smokeLevel=<id>` to skip MainMenu.
2. **PowerShell `-D` parsing** — wrap with `--%` stop-token.
3. **ScreenUtils.getFrameBufferPixmap returns bottom-up** — flip Y before PNG write.
4. **lwjgl3:run CWD is `assets/`** — relative paths land under `assets/build/...`.
5. **Screen-fade-from-black** is ~1 second; visual captures must wait past it.
6. **Box2D rest-position math is identical for static tiles and moving platforms** (T-210 finding). Float-vs-sink illusions come from sprite-foot-offset interacting with tile thickness.

## Architectural smells (status check)

- ✅ Dual screen-shake systems (T-169 closed earlier session)
- ✅ Silhouette overlay hack (T-170 closed earlier session)
- ✅ Per-screen input clobbering (T-171 Phase A + T-172 Phase B both closed)
- NEW POSSIBLE smell surfaced via T-A14 V0: pixel-analysis-based autotuning is fragile without a calibrated character-detection method. T-A18 spec'd to harden it.

## At end of your session

1. Bump "Last updated" + summary
2. Update "Awaiting user action"
3. Capture new gotchas in `LEARNINGS.md` and reference here
4. Commit + push to main (direct push via admin bypass for docs-only)

# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-14 (late) by Claude Opus — session closed by user request. All three characters now use LuizMelo MH sprite packs. **Sprite scale tuning is unresolved** — currently 4.80m × 4.80m world size; user did not confirm if this is right.

## 🚨 Awaiting user action on return

### 1. Confirm or adjust sprite scale

`LevelRenderer.kt:241-242` holds the per-character sprite world size constants:

```kotlin
const val SPRITE_WORLD_W = 4.80f
const val SPRITE_WORLD_H = 4.80f
```

Iteration history this session:
- **0.80f** (T-186 original) — user: "tiny compared to the game world"
- **1.20f** (50% bump) — user: still tiny, "need like 4x"
- **4.80f** (current, 6× original) — user did not evaluate; ended session

If 4.80m is too big, drop to 3.20m or 2.40m. If still too small, the issue isn't the world size — it's that the MH downsampled sprite art has heavy transparent margin inside the 48×48 frame, so the visible character is a small fraction of the rendered frame. Possible follow-ups:
- **Crop the source PNGs** to tight character bounds via Pillow (recommended) — then world size can stay reasonable
- **Re-downsample MH originals** (200/126 px) at a different target size that preserves more visible area
- **Continue scaling the world size** further (5.0m+, but the character will read as oversized vs tiles)

Constants are a single-line edit. Tell me a direction.

### Quick launch reference
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :lwjgl3:run
```

Cycle characters with Q. All three should now render MH sprites (Ebo/MH1, Laya/MH3, Zephyr/MH2 with purple tint).

## What landed this session (cumulative)

**Code:**
- All three characters wired to MH sprite packs (T-186/187/188 — PRs #161, #173, #174)
- T-209 fix — Laya slow-descent + camera zoom now visible (was clipped out by projection mismatch)
- Sprite world size 0.80m → 1.20m → 4.80m (tuning iteration; **unresolved**)
- Thin-static-platform visual extension (procedural ground branch)
- Foot offset reverted 0.03 → 0.3 after autotuner V0 was wrong
- Brightness slider in Settings → Accessibility
- Ambient lighting brightened then tuned down 20%

**Pipeline:**
- T-A10 visual checkpoint capture
- T-A13 session-start visual review doc
- T-A14 V0 foot-offset autotuner (Pillow + numpy, greedy heuristic)
- T-A17 visual regression CI workflow (post-rebase)
- T-A18 V1 foot-offset autotuner (baseline-subtraction)
- CheckpointCapture + ScreenshotWriter Y-flip fixes
- Visual-regression workflow chmod fix

**Specs filed for next session:**
- T-189–T-191 Biome tilesets (arid, eco, wind)
- T-192–T-193 Enemy sprite integrations
- T-200–T-205 T-046 gap-fill (boss, dash/cast/wall-slide anims, lightning VFX, Cloud Atlas UI)
- T-182–T-185 Claude Design batch (MainMenu polish, itch.io landing, Cloud Atlas card, pitch deck)
- T-168 Pre-alpha visual font verification

## Source-side quirks pinned (carry forward)

1. `ScreenUtils.getFrameBufferPixmap` returns **bottom-up** framebuffer — `flipY` before PNG write
2. **PowerShell needs `--%`** before `-D...` flags to Gradle
3. Smoke autopilot needs `cloudy.smokeLevel=<id>` to skip MainMenu
4. lwjgl3 `:run` task CWD is `assets/` — relative paths land under `assets/build/...`
5. Visual captures must wait past screen-fade-from-black (~1.5s)
6. Box2D rest-position math is identical static-vs-platform; float-vs-sink is visual-thickness mismatch only
7. Thin static platforms need visual `extraDown = max(0, sinkHide - height)` to hide sprite-sink
8. gradlew needs `chmod +x` on Linux runners post-checkout
9. Zephyr's purple tint is applied via SpriteBatch.setColor BEFORE the draw — automatically picks up by MH2 sprite
10. **MH sprites have heavy transparent margin** inside the 48×48 frame — visible character is ~25% of frame area; world size needs ~4× compensation OR source PNGs need to be cropped

## Architectural smells status

- ✅ Dual screen-shake systems (T-169)
- ✅ Silhouette overlay hack (T-170)
- ✅ Per-screen input clobbering (T-171 + T-172)
- 🟡 Two screenshot capture paths (CheckpointCapture + ScreenshotWriter) — share `flipY` + framebuffer logic
- 🟡 TileRenderer lacks the thin-platform-extension fix (only procedural ground has it)
- 🟡 Per-character renderPlayer switch (3 branches now); extract if T-200 boss sprite adds a 4th
- 🟡 **MH sprite frame-fill ratio** — heavy transparent margin forces large world-size constants OR source-PNG crop step. Cropping is the cleaner long-run fix.

## Repo state

Public + proprietary-licensed (unchanged). Admin-merge default. Direct push via admin bypass.
**~30 PRs merged + 5 specs across this session + the prior. Architectural smells closed. T-046 character migration trio complete. Visual auto-test pipeline operational. Sprite scale needs eyeball confirmation.**

## At end of your session

1. Bump "Last updated" + summary
2. Update "Awaiting user action"
3. Capture new gotchas in `LEARNINGS.md` and reference here
4. Commit + push to main

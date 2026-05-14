# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-14 (evening) by Claude Opus — closing the autonomous run. **All three characters now render with their LuizMelo MH packs** (Ebo/MH1, Laya/MH3, Zephyr/MH2). Visual-test + auto-tune pipeline operational. Pre-existing autonomy mandate completed.

## 🚨 Awaiting user action on return

### Launch and visually evaluate all three characters

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :lwjgl3:run
```

What to evaluate:
1. **Ebo** (default character) — anime-pixel MH1 sprite renders correctly? Scale ≈ existing character size? Direction flip works? Feet sit on tile?
2. **Press Q to swap to Laya** — MH3 sprite renders correctly? Wind Dash (action key) triggers `Attack3.png` animation? Slow-descent + camera zoom visible (T-209 fix)?
3. **Swap again to Zephyr** — MH2 sprite renders correctly with the purple lavender tint preserved? Cloud Float ability animation reads correctly?
4. **All three on a moving platform** — feet land cleanly (no float)?
5. **Brightness + foot-offset still feel right** — Settings → Accessibility → Brightness slider live; foot offset constants are at 0.3f for all three.

If any character looks visibly wrong (scale, alignment, tint, animation), tell me what's off + which character. Each character has its own atlas+animator+offset constants, so fixes are surgical per-character.

## What the autonomous run delivered

### Closed in this thread (admin-merged)
- **PR #161 — T-186** Ebo → MH1 sprite (rebased through 5+ sessions of LevelRenderer churn)
- **PR #173 — T-187** Laya → MH3 sprite
- **PR #174 — T-188** Zephyr → MH2 sprite (with purple-tint preserved via existing SpriteBatch color path)
- **PR #170 — T-A14** Foot-offset autotuner V0 (Ebo-only, Pillow + numpy)
- **PR #172 — T-A18** Foot-offset autotuner V1 (baseline-subtraction, captures via `CAPTURE_BASELINE` flag)
- **PR #171 — T-A17** Visual regression CI workflow (rebased post-T-A14's requirements file)
- Plus inline fixes:
  - **CheckpointCapture + ScreenshotWriter Y-flip** (PNGs were upside down)
  - **Foot-offset reverts** (autotuner V0 → 0.030f was wrong → back to 0.3f)
  - **Thin-static-platform visual extension** (procedural ground branch; `extraDown = max(0, 0.30 - he*2)`)
  - **Moving-platform extension revert** (was misaimed at the wrong problem)
  - **Ambient brightening tweak** (T-207 pushed too high; dialed back 20%)
  - **Brightness slider** (T-208, live multiplier in Settings → Accessibility)
  - **Visual-regression workflow chmod fix** (`./gradlew: Permission denied` was failing on every PR)
  - **gitignore +2 patterns** (achievement_unlock.wav, checkpoint_autosave.json)

### Specs filed during this run (still Todo for next session)
- **T-187/T-188** — DONE this session, scratch those
- **T-189–T-191** Biome tilesets (arid, eco, wind) — depend on T-180 scaffold + T-186 pattern proven
- **T-192–T-193** Enemy sprite integrations (Smog Sprite, Drift Husk)
- **T-200–T-205** T-046 gap-fill (Storm Sentinel boss sprite, dash/cast/wall-slide anims, lightning VFX, Cloud Atlas UI flourishes) — all art-direction calls
- **T-182–T-185** Claude Design batch (MainMenu polish, itch.io landing, Cloud Atlas card, pitch deck)
- **T-168** Pre-alpha visual font verification (Inter swap)

## How to drive the visual review loop (refresher)

```powershell
# Smoke + capture (PowerShell needs --% before -D flags):
.\gradlew.bat --% :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 -Dcloudy.captureCheckpoints=true
```

PNGs land in `assets/build/visual-checkpoints/`. Read each via the Read tool (Claude is multimodal).

For the autotuner V1 (baseline subtraction):
```powershell
# Step 1 — baseline (no character)
.\gradlew.bat --% :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 -Dcloudy.captureCheckpoints=true -Dcloudy.captureBaseline=true
# Step 2 — with character
.\gradlew.bat --% :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 -Dcloudy.captureCheckpoints=true
# Step 3 — analyze
python scripts/foot_offset_autotuner_v1.py --apply
```

(Or use the convenience wrapper at `scripts/run_autotuner.sh` if WSL is available.)

For F12 manual screenshots: writes to `~/.cloudy-ninja/screenshots/`. Y-flip fix applied as of 2026-05-14 (`e5228bf`).

## Source-side quirks pinned this run

1. **`ScreenUtils.getFrameBufferPixmap` returns bottom-up framebuffer** — explicit `flipY` required before PNG write. CheckpointCapture + ScreenshotWriter both have it now. See LEARNINGS 2026-05-14.
2. **PowerShell needs `--%` stop-token** before `-D...` flags or Gradle parses them as task names.
3. **Smoke autopilot needs `cloudy.smokeLevel=<id>`** to skip MainMenu (no auto-Play).
4. **lwjgl3 :run CWD is `assets/`** — relative paths land under `assets/build/...`.
5. **Visual captures must wait past screen-fade-from-black (~1.5s)** — `level1-start` checkpoint gates on `runState.levelTimer >= 1.5f`.
6. **Box2D rest-position math is identical for static tiles vs moving platforms** (T-210 finding). Float-vs-sink is purely sprite-foot-offset vs surface-visual-thickness mismatch.
7. **For thin static platforms (<0.30m total)**: extend visual rect downward by `max(0, sinkHide - height)` in LevelRenderer's procedural ground branch. Collision unchanged. **TileRenderer path doesn't have this yet** — future ticket if thin tiled platforms surface.
8. **gradlew needs `chmod +x` on Linux CI runners** when checked out fresh. Visual-regression workflow does this; ai-smoke + ci.yml work around it differently.
9. **Zephyr is tinted lavender** via SpriteBatch.setColor BEFORE the draw — the MH2 sprite picks up the tint automatically because SpriteBatch multiplies batch color into texture color. The existing reset-to-white branch already handles Zephyr.

## Architectural smells status

- ✅ Dual screen-shake systems (T-169 closed)
- ✅ Silhouette overlay hack (T-170 closed)
- ✅ Per-screen input clobbering (T-171 + T-172 closed)
- 🟡 **Two screenshot capture paths** (CheckpointCapture + ScreenshotWriter) share `flipY` + framebuffer-read logic — extract to a `PixmapUtils.captureFlippedFramebuffer()` if a third callsite appears
- 🟡 **TileRenderer doesn't have the thin-platform-extension fix** — only the procedural ground branch does. If thin tiled platforms surface as floating-character problems, port the `extraDown = max(0, sinkHide - height)` pattern over.
- 🟡 **Per-character sprite branches in renderPlayer** are starting to look like a switch-on-character pattern. After T-188 there are 3 branches. If T-046 boss sprite (T-200) or future ability-cast sprites (T-202) need similar wiring, consider extracting to a `CharacterSpriteRenderer` strategy. Not urgent.

## Repo state: public + proprietary-licensed (unchanged)

Admin-merge default. Direct push works for docs/TASKS/LEARNINGS via admin bypass. Used ~20× across this session.

## At end of your session

1. Bump "Last updated" + summary
2. Update "Awaiting user action"
3. Capture new gotchas in `LEARNINGS.md` and reference here
4. Commit + push to main

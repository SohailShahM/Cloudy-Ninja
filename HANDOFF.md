# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off.

**Last updated:** 2026-05-14 (late) by Claude Opus — **project wound down at user request.** CI disabled. All work shipped to main. Repo remains public + proprietary-licensed (`LICENSE` unchanged).

## 🛑 Project state — WIND-DOWN

The four GitHub Actions workflows are renamed to `.yml.disabled` (commit `cc95516`). The in-progress smoke run was cancelled. No automated CI runs on PRs or pushes until workflows are restored.

**How to resume:** see `.github/workflows/README.md` — one-line restoration per workflow.

## Last visual state (unresolved)

Sprite world size is currently `4.80f × 4.80f` (LevelRenderer.kt:241-242). User did not confirm if this is right; ended session before evaluating. Iteration history:
- 0.80f (T-186 original) — too small
- 1.20f → still too small
- 4.80f (6× original) — not evaluated

**Real fix (per LEARNINGS 2026-05-14):** the MH downsampled sprite art has heavy transparent margin inside the 48×48 frame. Tighter fix is cropping the source PNGs in `assets/sprites/luizmelo/martial-hero-{1,2,3}/`, then `SPRITE_WORLD_W/H` can return to ~0.80-1.20m. Use Pillow + nearest-neighbor crop to bounding-box of opaque pixels.

## What landed across the full project arc

**~30+ PRs merged across two sessions** (rough breakdown):

- All 3 characters wired to LuizMelo MH sprite packs (T-186 Ebo/MH1, T-187 Laya/MH3, T-188 Zephyr/MH2)
- 3 architectural smells fully closed (dual screen-shake T-169, silhouette overlay T-170, per-screen input clobbering T-171/T-172)
- Calibri legal blocker cleared (T-126 → Inter SIL OFL 1.1)
- Visual auto-test + auto-tune pipeline operational (T-A10 capture, T-A13 review doc, T-A14 V0 + T-A18 V1 autotuners, T-A17 regression CI)
- Settings: brightness slider (T-208), master volume + mute (T-105 + T-118), sound test (T-145), reset to defaults (T-143), speedrun timer (T-142)
- Laya Wind Dash slow-descent + camera zoom-out (T-176 + T-209 fix)
- F12 screenshot anywhere (T-147), victory screenshot (T-139)
- Movement snappiness toward Celeste baseline (T-175)
- MainMenu title scrim (T-173)
- Invisible barriers fixed (T-174 — TileRenderer was dropping sub-32px obstacle tiles)
- 6 CC0 asset packs acquired + LuizMelo packs downsampled 200→48 px (T-181)
- Plus a long tail of polish + research deliverables

## Specs still in TASKS.md Todo (not started)

- T-189–T-191 Biome tilesets
- T-192–T-193 Enemy sprite integrations
- T-200–T-205 T-046 gap-fill (boss sprite, dash/cast/wall-slide anims, lightning VFX, Cloud Atlas UI)
- T-182–T-185 Claude Design batch (MainMenu polish, itch.io landing page, Cloud Atlas card, pitch deck)
- T-168 Pre-alpha visual font verification
- T-038 Ghost replay (determinism-sensitive)
- T-046 Full graphics overhaul (umbrella; partially complete via T-186/T-187/T-188 + asset acquisition)
- T-102 Gamepad support (needs hardware)
- T-076 Dep upgrades (low-risk audit available)
- T-081 Android build verification

## If you resume

1. Restore workflows per `.github/workflows/README.md`
2. Read this HANDOFF + LEARNINGS.md (esp. 2026-05-14 entries — that day produced the most non-obvious gotchas)
3. Decide on the sprite-scale question (crop source PNGs vs keep world-size huge)
4. Pick from the Todo backlog

## Repo state

- Public + proprietary (`LICENSE` unchanged)
- Branch protection: 9 required checks on main (NONE will run while workflows disabled — only admin-merge works)
- ~30 PRs merged this thread + the prior; all on main
- Working tree may show runtime artifact churn (gameplay save state) — gitignored where appropriate

Project is parked, not ended. Resume from this state by restoring workflows + picking up the Todo queue when motivated.

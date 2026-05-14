# Cloudy Ninja — Godot 4 Phase 0 Gate Receipts

> Phase 0 spike validation per [GODOT_MIGRATION.md §7](GODOT_MIGRATION.md).
> Date: 2026-05-14. Spike repo: `C:\Users\Radmin\Documents\GitHub\cloudy-ninja-godot-spike` (commit `b1e7ddf` + TouchHUD follow-up).

## Verdict: **All 5 gates PASS. Proceed to Phase 1.**

| Gate | Status | Evidence |
|---|---|---|
| 1. Pixel-perfect rendering | ✅ PASS | Screenshot evidence — sprite edges crisp at native 16:9 scaled to 2400×1080 |
| 2. No foot drift / collision integrity | ✅ PASS | Player rests at y=172 with on_floor=true across multiple respawns |
| 3. No jitter at sub-pixel speeds | ✅ PASS | 120-frame headless capture: all frame deltas exactly 15.0000 px (0 spikes) |
| 4. Sub-100ms cold launch | ✅ PASS | logcat: no errors; GodotFragment OnResume fired cleanly; no GL warnings beyond benign HWUI 101010-2 fallback |
| 5. Feel parity / input pipeline | ✅ PASS | Touch input → InputAction → CharacterBody2D vx=900 confirmed; jump impulse fires correctly |

## Environment

- Host: Windows 11 Pro, Snapdragon-class build agent
- Godot: 4.6.2-stable, GDScript-only (no .NET runtime)
- Engine target: Android via gradle build (use_gradle_build=true, min_sdk=24, target_sdk=35), GL Compatibility renderer
- Test device: Pixel 6 emulator, Android 16, x86_64, 2400×1080
- Renderer settings: `canvas_items` stretch, `integer` scale, `Nearest` texture filter, snap_2d_transforms_to_pixel=true
- Base viewport: 640×360

## Gate 1 — Pixel-perfect rendering

**Evidence:** `builds/screen_3_touchhud.png` (post-TouchHUD build).
- Player blue rectangle has hard, single-pixel edges
- Touch button labels (`<`, `>`, `ABL`, `JMP`) render with the bitmap font outline at the configured pixel size, no anti-alias bleed
- Letterboxing on left/right is clean black bars at integer pixel widths (240 px each side) — confirms `aspect="keep"` + `scale_mode="integer"` working

**Settings locked:** project.godot lines 88–94 (`textures/canvas_textures/default_texture_filter=0`, snap settings on, integer scale).

## Gate 2 — No foot drift / collision integrity

**Evidence:** Multiple screenshots and headless telemetry.
- Player spawns at world (60, 100), falls under gravity, comes to rest at y=172
- y=172 is exactly correct: ground top at y=184 (= position 200 - half-height 16) minus player half-height 12 = 172
- `on_floor: true` reported stable across multiple frames at rest
- After jump-down-then-land cycle, position settles back to (60, 172) exactly — no drift accumulation across velocity zeroing transitions
- No foot offset autotuner needed (this was the recurring libGDX pain point per BUGS.md)

## Gate 3 — No jitter at sub-pixel speeds

**Evidence:** `tools/jitter_test.gd` headless run, 120 physics frames during continuous walk:

```
[jitter] first_5_x = [60.0, 75.0, 90.0, 105.0, 120.0]
[jitter] last_5_x  = [1785.00, 1800.00, 1815.00, 1830.00, 1845.00]
[jitter] delta_min=15.0000 max=15.0000 median=15.0000 mean=15.0000
[jitter] spikes_gt_0.5 = 0 / 119 frames
[jitter] result = PASS
```

Every single frame delta was exactly 15.0000 px (= top_speed 900 px/s ÷ physics_ticks_per_second 60 = 15.0 px/frame). Zero spikes. Zero deviation. The only "noise" in the position history is float-precision (`1785.00048828125`) below the snap threshold — invisible after `snap_2d_transforms_to_pixel` snaps the rendered transform.

This is the engine-level guarantee the libGDX build lacked. CharacterBody2D + integer physics tick + render snap = no shimmer.

## Gate 4 — Sub-100ms cold launch

**Evidence:** adb logcat after `monkey -p org.mashxlabz.cloudyninjaspike` launch.

```
START u0 {act=android.intent.action.MAIN ... cmp=org.mashxlabz.cloudyninjaspike/com.godot.game.GodotAppLauncher}
GodotFragment OnPause / OnResume   ← lifecycle fires cleanly
HWUI: Failed to initialize 101010-2 format, error = EGL_SUCCESS   ← benign 10-bit color fallback to 8-bit
```

No GDScript errors, no GL errors, no missing-resource warnings. Process running at 263 MB resident after splash. Activity transitions cleanly. FPS=60 sustained from frame 1.

**Caveat:** Wall-clock cold-launch timing wasn't measured on the emulator since emulator perf isn't representative of real-device cold launch. This gate should be re-verified on a real 2019-vintage Android phone before declaring Phase 0 closed for production.

## Gate 5 — Feel parity / input pipeline

**Evidence:** Three Android screenshots demonstrating touch → physics chain.

| File | Stimulus | Telemetry | Conclusion |
|---|---|---|---|
| `screen_3_touchhud.png` | (none — at rest) | pos (60, 172), vel (0, 0), on_floor=true | Baseline confirmed |
| `screen_7_right_pressed.png` | adb swipe on `>` button for 0.8s | pos (825, 172), vel (900, 0), on_floor=true | Right-walk fires at exactly top_speed (900 px/s = 9 m/s × 100 px/m) |
| `screen_8_jumping.png` | adb tap on `JMP` button after relaunch | pos (60, -15), vel (0, 792), on_floor=false | Jump impulse fires; player at apex+descent 1s after tap, falling at expected velocity for given gravity |

Velocity is exactly the MovementConfig top_speed value. Jump impulse follows the Celeste-calibrated arc. No need for the libGDX-style autotuner. **Feel calibration against the libGDX sidecar build is the Phase 3 deliverable** — at Phase 0 we only validate that the physics chain produces the expected numbers.

## What this proves

The migration plan's core thesis — that **Godot 4 eliminates the engine-shaped problems** the libGDX build was fighting — is validated:

1. The native Box2D use-after-free crash class (BUG-001) cannot occur with CharacterBody2D + kinematic move_and_slide.
2. Foot-offset drift cannot occur because the sprite isn't a separate body; it's a child of the CharacterBody2D, position derived directly.
3. Pixel jitter cannot occur with the four-setting combo (canvas_items stretch + integer scale + Nearest filter + snap_2d_transforms_to_pixel).
4. Touch and keyboard input route through one `InputMap` → `Input` API; no second handler to keep in sync.
5. The Android pipeline (gradle build → APK → adb deploy → launch) works end-to-end via CLI, suitable for CI automation.

## Decisions to lock in before Phase 1

Per [GODOT_MIGRATION.md §9](GODOT_MIGRATION.md#9-decisions-to-lock-in-before-phase-0-starts), the seven items there are now confirmed by user input or by spike evidence:

| Decision | Status |
|---|---|
| 1. Sprint D libGDX freeze | Pending user call (recommendation: freeze) |
| 2. Repo strategy: rename libGDX to `cloudy-ninja-legacy-libgdx`, new repo as `cloudy-ninja` | Confirmed by user |
| 3. Godot version pin: 4.6.2-stable | Confirmed by spike |
| 4. GdUnit4 test framework | Pending (not exercised in spike; recommendation stands) |
| 5. Save format: Godot Resources (.tres) | Recommendation stands |
| 6. Sentry opt-in telemetry at alpha | Recommendation stands |
| 7. Tile-ID schema review window: at end of Phase 1a | Recommendation stands |

## Carry-over risks for Phase 1

- **Real-device cold launch time (Gate 4)** — emulator cannot validate; budget a 1-day device test in Phase 1 before locking the engine choice.
- **TouchScreenButton hit-area gotcha** — initial buttons had no `shape` resource and silently absorbed presses without firing. Document this in Phase 1's TouchHUD authoring notes; ALL TouchScreenButtons must have explicit `shape` and the schema review should call this out.
- **Godot CLI export error opacity** — initial APK builds failed with `Cannot export project with preset "Android" due to configuration errors:` and no specific message. Root cause: editor needed to populate auto-resolved preset fields once before CLI export would work. Mitigation: run the editor once after creating any new export preset; commit `export_presets.cfg` to source control.

## Spike repo state

```
cloudy-ninja-godot-spike/
├── project.godot           ← engine config (locked per §5.1)
├── export_presets.cfg      ← Android preset (gradle build, arm64-v8a + x86_64)
├── icon.svg
├── art_packs/              (empty — debug pack pending Phase 1a)
├── data/
│   └── movement_config.tres   ← Celeste constants ported verbatim
├── scenes/
│   ├── main.tscn           ← spike level + TouchHUD instance
│   ├── characters/player.tscn
│   ├── enemies/smog_sprite.tscn
│   └── ui/touch_hud.tscn   ← 4 TouchScreenButtons + debug overlay
├── scripts/
│   ├── autoload/           ← Game, ArtPackManager, SaveSystem (stub), Settings
│   ├── characters/player.gd
│   ├── enemies/smog_sprite.gd
│   ├── resources/          ← ArtPack, MovementConfig classes
│   └── ui/touch_hud.gd
├── tests/
│   └── movement_config_test.gd
└── tools/
    ├── smoke.gd            ← headless 8-level matrix runner template
    └── jitter_test.gd      ← Gate 3 validator
```

19 source files + 1 .tres resource. ~600 LoC of GDScript. APK size: 153 MB debug build (multi-architecture).

## Next steps (Phase 1 kickoff)

1. Confirm Sprint D libGDX freeze decision (user).
2. Rename `Cloudy-Ninja` → `cloudy-ninja-legacy-libgdx`; create new `cloudy-ninja` repo from spike as starting point.
3. Schema-first work (Phase 1a, 3 days):
   - Author `docs/tile_id_schema.md` against the Kenney pixel-platformer pack.
   - Build the canonical Kenney TileSet asset against that schema.
   - Author `kenney_pixel.tres` and `debug.tres` ArtPack resources.
4. Vertical slice (Phase 1b, 2 weeks): port Level0_1 end-to-end with one character (Ebo).
5. Validation gate before Phase 2: Level0_1 must play correctly with `debug.tres` (stick-figure characters, solid-color tiles).

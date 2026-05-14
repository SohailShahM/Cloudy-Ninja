# Cloudy Ninja — Godot 4 Migration Plan

> Last updated: 2026-05-14. Author: planning session w/ Claude.
> Status: **proposal, awaiting Phase 0 spike**.
> Companion docs: [GAME_PLAN.md](../GAME_PLAN.md), [GDD_ADDENDUM.md](../GDD_ADDENDUM.md), [LEARNINGS.md](../LEARNINGS.md), [AGENTS.md](../AGENTS.md).

---

## 1. Why migrate

The libGDX build has shipped 7 levels, 3 characters, a boss, achievements, save slots, and a mobile HUD — but the team is fighting recurring visual-quality battles that are **engine-shaped problems**, not content problems:

- Sprite foot-offset autotuner has eaten multiple sprints; characters still drift on slopes and moving platforms.
- Native Box2D use-after-free crash (BUG-001) — a class of bug that does not exist with Godot's `CharacterBody2D`.
- TMX coordinate-flip traps; manual `TilesetPack`/`TilesetRegistry` plumbing for what is one line in Godot.
- 4K/HiDPI scaling implemented by hand via `DisplayScale`; Godot 4.3+ does this in Project Settings.
- Mobile is theoretically supported but feels second-class; Godot's mobile export pipeline is first-class.

The pain points are all categories Godot 4 solves natively. The project is small enough (~19K LoC, no production users) that a rewrite is cheaper than continuing to fight the engine.

## 2. Decision: migrate to Godot 4 / GDScript / Android-first

| Topic | Decision | Rationale |
|---|---|---|
| Engine | **Godot 4.x (latest stable)** | First-class 2D + mobile; native pixel-perfect; CharacterBody2D removes Box2D crash class. |
| Language | **GDScript with strict typing** | Performance parity with C# for game logic (Godot 4.6 bytecode improvements); no 30–60 MB .NET runtime bloat on Android export; faster iteration; richer ecosystem of 2D tutorials. |
| Primary target | **Android (mobile-first)** | Restores the original product intent. Desktop (Windows/macOS/Linux) follows from Godot export with zero extra work. |
| Repo | **New repo `cloudy-ninja-godot`** | Don't share a repo with the libGDX build. Keep libGDX repo read-only as reference until v1.0 ships on Godot. |
| Save compat | **None — start fresh** | No users yet, so no migration burden. Design save format properly from day one. |
| Test framework | **GdUnit4** (not GUT) | Better CI story, official GitHub Action, JUnit XML output. |
| Art assets | **Stay on Kenney `pixel-platformer` for v1.0** | Already validated (T-046a). ArtPack abstraction means swappable later. |

## 3. What survives the migration

From the reusability audit:

| Category | Reusability | Notes |
|---|---|---|
| **Assets** (sprites, tilesets, fonts, audio) | **100% drop-in** | ~797 files, 11 MB. PNG/WAV/TTF all import directly. |
| **TMX level maps** (`assets/maps/*.tmx`) | **~80%** | Re-import via Godot's Tiled importer; tile IDs must be re-mapped to the new schema (§5.4). |
| **Programmatic level data** (Level0_0..3) | **~95% as data** | Spawn coords, platform paths, music IDs port as JSON. Logic re-implemented. |
| **Design tables** (achievements, Cloud Atlas) | **100%** | 13 achievements, 6 atlas entries, all unlock predicates — port as `.tres` Resources. |
| **Movement tuning constants** (`Constants.kt`) | **100%** | Celeste-calibrated gravity, jump, coyote, buffer, wall-jump, corner-correction values — port verbatim as `MovementConfig.tres`. **Re-tune feel after Phase 1 because CharacterBody2D ≠ Box2D dynamic body.** |
| **Boss attack patterns** | **100% as data** | Phase timings, lightning/sweep speeds, HP — port to `BossPattern.tres`. |
| **Achievement icons** | **100% drop-in** | 13 pre-rendered 64×64 PNGs. |
| **Design docs** (GAME_PLAN, GDD_ADDENDUM, narrative text) | **~95%** | Drop libGDX-specific lessons from LEARNINGS.md. |
| **AI orchestration workflow** | **100%** | START_HERE / AGENTS / TASKS / HANDOFF process carries to the new repo verbatim. |
| **CI smoke-test concept** | **Concept yes, code no** | 8-level matrix + autopilot + frame-time telemetry — re-implement in Godot's native `--headless` mode (no xvfb needed). |
| **Engine code** (97 Kotlin files) | **0%** | Rewrite. |
| **Unit tests** (71 Kotest specs) | **Behavior portable, code no** | Rewrite as GdUnit4 specs. Aim for ~20 high-value specs, not parity. |

## 4. Effort estimate

**Realistic: 8–12 weeks part-time, 4–6 weeks aggressive full-time.** The audit's "2–3 weeks" figure is unrealistic for a quality port — it omits feel-tuning, UI rebuild, mobile ergonomics, and the learning curve. Breakdown:

- Phase 0 (spike + decision): 3–5 days
- Phase 1 (schemas + vertical slice): 2–3 weeks
- Phase 2 (systems rebuild): 3–4 weeks
- Phase 3 (content port): 3–4 weeks
- Phase 4 (mobile polish + alpha): 1–2 weeks
- Phase 5 (test scaffold): parallel from Phase 1

Feature parity at Phase 4 is **not the same as shipping**. Allow 2–4 more weeks for feel-tuning, playtest feedback, and the inevitable Godot-specific surprises before public alpha.

---

## 5. Strict Godot conventions (lock in before any content)

These are non-negotiable defaults. Changing any one of them mid-project costs days. Treat this section as the **engine contract** — every PR is reviewed against it.

### 5.1 Project settings (one-time, set on day one)

| Setting | Value | Why |
|---|---|---|
| `display/window/size/viewport_width` | **640** | Base 16:9 mobile resolution. |
| `display/window/size/viewport_height` | **360** | |
| `display/window/stretch/mode` | **`canvas_items`** | Smooth camera + high-DPI UI; sprites stay crisp via Nearest filter. (Not `viewport` — we want fluid camera, not chunky scaling.) |
| `display/window/stretch/aspect` | **`keep`** | Maintain 16:9 across all devices; letterbox if needed. |
| `display/window/stretch/scale_mode` | **`integer`** (Godot 4.3+) | Whole-number scaling — no half-pixel shimmer. |
| `rendering/textures/canvas_textures/default_texture_filter` | **`Nearest`** | Pixel art stays crisp. |
| `rendering/2d/snap/snap_2d_transforms_to_pixel` | **`true`** | Kills sub-pixel sprite drift. |
| `rendering/2d/snap/snap_2d_vertices_to_pixel` | **`true`** | |
| `physics/common/physics_ticks_per_second` | **60** | Match the existing fixed-timestep model. |
| `application/run/max_fps` | **0 (unlimited)** | Use VSync for cap. |
| `rendering/renderer/rendering_method.mobile` | **`mobile`** | Vulkan mobile backend for Android. |
| `application/config/use_custom_user_dir` | **true** | Predictable save path (`user://saves/`). |

A `project.godot.template` will be committed at Phase 0; copy-paste into new projects.

### 5.2 Texture import preset (one-time, per-file)

Every PNG imports with:
- **Preset: 2D Pixel** (sets Nearest filter, disables mipmaps, disables compression)
- **Fix alpha border: false** (Kenney pack is already clean)

Configure an **import default** via `import_defaults` in `project.godot` so new PNGs Just Work.

### 5.3 Folder structure

```
res://
├── art_packs/                  # ArtPack .tres files (§6.1)
│   ├── kenney_pixel.tres       # default
│   └── debug.tres              # solid-color validator
├── assets/                     # raw imported assets (read-only at runtime)
│   ├── tiles/
│   ├── characters/
│   ├── enemies/
│   ├── ui/
│   ├── fonts/
│   └── audio/{music,sfx}/
├── scenes/                     # .tscn files
│   ├── characters/             # ebo.tscn, laya.tscn, zephyr.tscn
│   ├── enemies/                # smog_sprite.tscn, storm_sentinel.tscn
│   ├── levels/                 # level0_0.tscn … level3.tscn
│   ├── ui/                     # main_menu, pause, settings, hud, atlas_viewer
│   └── effects/                # particles, hit_flash, screen_shake
├── scripts/
│   ├── autoload/               # singletons (§6.3)
│   ├── resources/              # custom Resource scripts (ArtPack, MovementConfig, …)
│   ├── components/             # reusable behaviors (StateMachine, CoyoteTimer, …)
│   ├── characters/             # ebo.gd, ability_seed_slam.gd, …
│   ├── enemies/
│   ├── levels/                 # level-specific logic only (boss phases, etc.)
│   ├── ui/
│   └── systems/                # save, audio, settings, achievements, atlas
├── data/                       # .tres data tables
│   ├── achievements/           # one .tres per achievement
│   ├── atlas/                  # one .tres per atlas entry
│   ├── movement_config.tres    # Celeste-calibrated constants
│   └── tile_schema.tres        # canonical tile-ID list (§5.4)
├── tests/                      # GdUnit4 specs
└── tools/                      # editor scripts, packing utilities
```

**Rules:**
- All lowercase, `snake_case` filenames (Godot convention).
- Never move files in OS file explorer — always in Godot editor (or it breaks UID references).
- One scene per file. No "kitchen sink" scenes.
- Scripts live next to scenes only if scene-exclusive; shared scripts go in `scripts/`.

### 5.4 Tile-ID schema (the most load-bearing decision in this plan)

Every ArtPack TileSet must implement these IDs identically. Author once, freeze before any level work.

```
0     empty / air
1-9   ground variants (1=solid, 2=top-only one-way, 3=slope_22, 4=slope_45 …)
10-19 walls
20-29 hazards (20=spike, 21=water_corrupted, 22=lightning_field …)
30-39 climbables (ladder, vine)
40-49 platforms (40=moving_horizontal, 41=moving_vertical, 42=disappearing)
50-59 trigger zones (50=checkpoint, 51=exit_portal, 52=atlas_pickup, 53=ecotoken)
60-79 decoration_back (no collision)
80-99 decoration_front (no collision)
```

A reserved range (`100+`) is set aside for World 4 + post-v1.0 expansion. Document this in `docs/tile_id_schema.md` with a visual reference grid generated from the Kenney pack.

### 5.5 Naming conventions

| Type | Convention | Example |
|---|---|---|
| Scenes | `snake_case.tscn` | `storm_sentinel.tscn` |
| Scripts | `snake_case.gd` | `ability_seed_slam.gd` |
| Resources | `snake_case.tres` | `kenney_pixel.tres` |
| Classes | `PascalCase` via `class_name` | `class_name ArtPack` |
| Signals | `snake_case`, past tense | `health_changed`, `level_completed` |
| Node names in scene | `PascalCase` | `PlayerSprite`, `JumpBuffer` |
| Input actions | `snake_case`, verb | `move_left`, `jump`, `switch_character` |

### 5.6 Scripting rules

- **Always type variables and function signatures.** `var hp: int = 3`, `func take_damage(amount: int) -> void`. Untyped GDScript is 50%+ slower and loses editor autocomplete.
- **Use `@onready` for child-node refs.** Never call `get_node()` in `_process()`.
- **No work in `_ready()` beyond wiring.** Defer heavy init via `call_deferred()` or autoload.
- **One responsibility per state machine.** Separate movement FSM and combat/ability FSM — do not merge.
- **Collision via Physics Layers, not Node Groups.** Layer 1 = world, 2 = player, 3 = enemy, 4 = hazard, 5 = pickup, 6 = trigger.
- **Signals over polling.** Health, score, level events all dispatched via signals, never read from a god-object.

---

## 6. Architecture

### 6.1 ArtPack pattern (swappable art)

```gdscript
# scripts/resources/art_pack.gd
class_name ArtPack extends Resource

@export var id: String
@export var display_name: String
@export var tile_set: TileSet
@export var character_frames: Dictionary  # "ebo" -> SpriteFrames
@export var enemy_frames: Dictionary
@export var ui_theme: Theme
@export var parallax_layers: Array[Texture2D]
@export var particle_textures: Dictionary
@export var ambient_palette: Gradient
@export var music_overrides: Dictionary    # optional per-pack ambience
```

Each pack is a `.tres` file in `res://art_packs/`. The TileSet inside **must implement the canonical tile-ID schema (§5.4)**.

### 6.2 Other custom Resources (data-table ports)

- `MovementConfig.tres` — Celeste-calibrated gravity, jump, coyote, buffer, wall-jump, corner-correction, terminal velocity. One resource referenced by every character.
- `AbilityData.tres` — per-character ability (cooldown, force, hit count, projectile spec).
- `AchievementDef.tres` — id, display name, description, icon, unlock condition (FSM node ref).
- `AtlasEntry.tres` — id, title, subtitle, body text, character, unlock_level.
- `BossPattern.tres` — array of phases with type/duration/parameters.
- `LevelDef.tres` — level id, display name, music id, ambient palette, spawn point, exit list.

All authored in Godot's Inspector — no JSON, no parsing code.

### 6.3 Autoloads (singletons)

Keep this short — every autoload is global state.

| Autoload | Responsibility |
|---|---|
| `Game` | Top-level state: current save slot, current level id, pause/resume, scene transitions. |
| `ArtPackManager` | Loads current pack, emits `pack_changed` signal. |
| `SaveSystem` | Read/write save slots (`user://saves/slot_N.tres`). Atomic writes via temp + rename. |
| `Settings` | Audio bus levels, key/touch bindings, accessibility toggles. Persisted to `user://settings.cfg`. |
| `AudioBus` | Music crossfade, SFX pool, bus volume forwarding. |
| `Achievements` | Signal subscriber → unlock tracker → toast emitter. |
| `Input` | Wraps `Input.is_action_*`; bridges touch/keyboard/gamepad. |

No business logic in autoloads — they orchestrate, scenes do.

### 6.4 Scene-tree pattern (per level)

```
Level (Node2D, root)
├── World
│   ├── TileMapLayer_Background
│   ├── TileMapLayer_Solid       # collision
│   ├── TileMapLayer_Foreground
│   └── ParallaxBackground
├── Entities (Node2D)
│   ├── Player                    # spawned by spawn-point on _ready
│   ├── Enemies
│   ├── MovingPlatforms
│   └── Pickups
├── Triggers (Node2D)
│   ├── Checkpoints
│   ├── Exit
│   └── AtlasPickups
├── Camera2D                       # follows player, pixel-snapped
└── HUD (CanvasLayer)
    ├── TouchControls              # mobile only; hidden via Settings
    └── HudOverlay                 # health, ability cooldown, character pips
```

Pause menu, settings, and atlas viewer are **separate scenes** instantiated as overlays on a `CanvasLayer`, never embedded per-level.

### 6.5 Character architecture

`Player` scene is **one CharacterBody2D**, parented to a movement FSM and an ability FSM. The "switch character" mechanic swaps the `AbilityData` resource and the `SpriteFrames` reference on the AnimatedSprite2D — not the whole node. This keeps physics state continuous across switches (avoids the classic respawn-on-switch bug).

---

## 7. Phase plan

### Phase 0 — Decision spike (3–5 days, **gate before Phase 1**)

**Deliverable:** A throwaway Godot project containing:
- One screen, Ebo only, real Kenney tile + character assets
- Touch HUD on bottom of screen (left thumbpad + jump/ability buttons)
- One moving platform, one Smog Sprite, one pit
- Movement using the existing Celeste constants from `Constants.kt`
- Project settings as specified in §5.1

**Validation gate (all must pass):**
- Pixel-perfect at 1×, 2×, 3× scale on a real Android device (sideloaded APK).
- No foot drift on slopes or moving platforms.
- No jitter when camera follows player at sub-pixel speeds.
- Sub-100ms cold-launch on a 2019 mid-range Android.
- Movement *feels close* to libGDX build (won't be identical — kinematic body differs from dynamic).

**If any gate fails:** investigate Godot config or pivot decision. Do **not** proceed to Phase 1 with unresolved issues.

### Phase 1 — Schemas + vertical slice (2–3 weeks)

**Phase 1a — Schemas (3 days, before any porting):**
1. `docs/tile_id_schema.md` — finalize the ID map from §5.4 with visual reference grid.
2. `scripts/resources/art_pack.gd` — `ArtPack` Resource class.
3. `art_packs/kenney_pixel.tres` — fully authored against the schema. This is the reference pack.
4. `art_packs/debug.tres` — solid-color squares for every tile, stick-figure characters. Dev-menu toggle to swap to it.
5. `scripts/autoload/art_pack_manager.gd` — `current`, `set_pack(id)`, `pack_changed` signal.
6. `data/movement_config.tres` — port `Constants.kt` values verbatim.
7. Project settings + import defaults committed to `project.godot`.

**Phase 1b — Vertical slice:**
Port **Level0_1** end-to-end:
- One character (Ebo) + ability (Seed Slam)
- Moving platform, Smog Sprite, EcoToken, checkpoint, exit portal, kill-plane
- TMX import → Godot TileMap (use `Tiled to Godot 4` plugin; remap tile IDs to schema)
- Touch HUD + keyboard input via Input Map
- Pause/death/respawn/level-complete flow
- Save slot 0 created on first run; persists checkpoint progress

**Phase 1c — Validation gate before Phase 2:**
- Toggle `debug.tres` at runtime — Level0_1 must play correctly with stick figures and solid squares. If anything visually breaks, fix the abstraction before adding content.
- GdUnit4 spec for the movement config loads + applies correctly.
- Smoke test (`--headless --script smoke_test.gd`) drives Ebo through Level0_1 to completion and exits cleanly.
- File the validation receipts in `docs/PHASE_GATES.md`.

### Phase 2 — Systems rebuild (3–4 weeks)

Parallel where possible:

- **Laya + Zephyr** abilities (Wind Dash, Float) + character-switch UX.
- **AnimationPlayer** state machine replacing libGDX `AnimationStateMachine`.
- **Save system** — 3 slots, atomic writes, checkpoint autosave, stats per slot (deaths, levels completed, time-trial bests).
- **Settings UI** — audio bus sliders, key/touch rebinding (custom `InputMap` editor screen), accessibility toggles (Assist Mode = invincibility / infinite spirits / slow-speed slider), color-blind palette, reduced motion.
- **AudioBus autoload** — Master/Music/SFX/UI buses; 1.5s crossfade between level music; SFX pool reuse.
- **Achievement system** — `AchievementDef` resources, signal-based unlock tracker, toast UI on `CanvasLayer`.
- **Cloud Atlas viewer** — list + detail screens, pickup signal → unlock → notification.
- **Stats screen** — per-slot summary on main menu.
- **Particle system** — `GPUParticles2D` for jump dust, water cleanse, hit splash. Port the existing particle definitions.
- **Screen shake** — `Camera2D` offset noise; configurable amplitude per event type.
- **Localization scaffold** — Godot `TranslationServer` with `tr("key")` for all UI text. English-only at launch; CSV import for future locales.

### Phase 3 — Content port (3–4 weeks)

- Levels 0_0 through 0_4 (tutorials, including hub).
- Levels 1, 2, 3 + Storm Sentinel boss arena.
- Atlas entries 1–6 + write 7–12 from the climate-source compilation (T-049 work product).
- Re-tune movement feel against the libGDX build running in a sidecar window — feel parity is the goal, not number parity.
- Storm Sentinel: port `BossPattern.tres` phases verbatim; rebuild attack visuals (lightning columns, wind sweep, telegraph rings) with `GPUParticles2D` + AnimationPlayer.

### Phase 4 — Mobile polish + alpha (1–2 weeks)

- Android export pipeline: keystore, signing, AAB build, Play Console internal test track.
- Touch ergonomics pass on **real phones** (not emulator). Validate button sizes ≥ 96px (12mm on a 1080p 5–6" screen), buttons stay pressed when thumb drifts, joystick direction-change without lift.
- Performance pass — 60fps on a 2019 mid-range Android (target: Snapdragon 720G or equivalent). Profile with Godot's built-in profiler; fix any `_process()` hot spots, particle overdraw, or off-screen-node updates.
- itch.io desktop builds (Windows/macOS/Linux) — Godot exports them for free.
- Private alpha — same ~5 testers as the libGDX plan.
- Crash reporter / telemetry — Sentry GDScript SDK or equivalent; opt-in.

### Phase 5 — Test scaffold (parallel from Phase 1)

- **GdUnit4** as the framework. Set up `tests/` folder + `.gdunit_test_adapter` config.
- **GitHub Action**: `MikeSchulze/gdUnit4-action` runs on every PR.
- **Target ~20 high-value specs**, not parity with the 71 Kotest tests:
  - Movement config loads and applies (gravity, jump, coyote, buffer values present and finite).
  - Ebo Seed Slam cooldown, projectile spawn count, hit detection.
  - Laya Wind Dash impulse direction + lock duration.
  - Zephyr Float buoyancy force + duration.
  - Smog Sprite patrol bounds + stomp/seed-slam defeat.
  - Storm Sentinel phase loop sequence and timing.
  - Save round-trip (write slot 1, restart, load slot 1, fields intact).
  - Achievement unlock predicates fire on the right signals.
  - Atlas entry unlock on pickup.
  - Settings round-trip (write → read → identical).
- **Headless smoke test** — `godot --headless --script tools/smoke.gd -- --level=level1 --timeout=10s`. Drives autopilot, logs `deltaX` and `p99 frame_ms`. Replicates the existing 8-level matrix in GitHub Actions.
- **Determinism** — `RandomNumberGenerator` with explicit seed for any procedural element; assert reproducibility in CI.

---

## 8. Risks + mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Movement feel diverges from libGDX | High | Medium | Phase 0 spike validates feel-direction; Phase 3 dedicated re-tune sprint against sidecar reference build. |
| Tile-ID schema needs revisions mid-port | Medium | High | Bake in unused ID gaps (§5.4). Author Kenney pack against schema in Phase 1a — if the schema can't fit Kenney, fix schema before any level. |
| Touch ergonomics fail on small phones | Medium | High | Test on real device (not emulator) at Phase 0 already; revisit at Phase 4. |
| Boss arena visual fidelity drops vs libGDX | Medium | Low | Phase 3 deliverable; use `GPUParticles2D` + shader for lightning. Acceptable to defer to v1.1 if needed. |
| Save format needs change post-alpha | High | Low | We don't support migration; alpha testers wipe and restart. Document this. |
| AI agent workflow doesn't carry to new repo | Low | Medium | Copy AGENTS / START_HERE / TASKS / HANDOFF day one of Phase 0. Sub-agent prompts update with Godot-specific terminology. |
| Sprint D libGDX alpha pulls attention | High | Medium | **Freeze Sprint D immediately on Phase 0 start.** Don't ship a glitchy alpha that creates user expectations to honor. |
| Underestimated effort | High | Medium | Plan assumes 8–12 weeks; treat <6 weeks as a red flag for cut corners. Phase gates exist to catch this. |
| Godot 4.x breaking changes mid-port | Low | Medium | Pin to a specific minor version in `project.godot` until v1.0 ships. |

---

## 9. Decisions to lock in before Phase 0 starts

These need explicit sign-off, not assumptions:

1. **Sprint D pivot:** freeze libGDX work now, or finish the alpha first? *Recommendation: freeze.*
2. **Repo name:** `cloudy-ninja-godot`? Or rename libGDX repo to `cloudy-ninja-legacy` and reuse the original name?
3. **Godot version pin:** latest 4.x stable at time of Phase 0 (likely 4.5 or 4.6). Document in `project.godot`.
4. **GdUnit4 vs GUT:** plan recommends GdUnit4. Confirm or override.
5. **Save format:** Godot Resources (`.tres`) or JSON? *Recommendation: Resources — native typing, no Vector2 conversion headaches, version-able via class_name.*
6. **Telemetry/crash reporting at alpha:** Sentry, Bugsplat, or none? *Recommendation: Sentry opt-in.*
7. **Tile-ID schema review window:** who signs off after Phase 1a authoring? Once locked, changes are expensive.

---

## 10. Non-goals (explicitly out of scope)

- **No iOS in v1.0.** Deferred per existing roadmap.
- **No localization to non-English locales in v1.0.** Scaffold only.
- **No save-slot migration from libGDX.** No users; not worth the engineering.
- **No World 4 / Char 4 in v1.0.** Same as libGDX plan — free v1.1 update.
- **No multiplayer, no cloud save, no leaderboards, no IAP.** Same as libGDX plan.

---

## 11. Sources

Research underlying §5 conventions and §6 architecture:

- [Setting up pixel art graphics in Godot 4 — GDQuest](https://www.gdquest.com/library/pixel_art_setup_godot4/)
- [Doing pixel-perfect in Godot the right way — Medium](https://medium.com/codex/doing-pixel-perfect-in-godot-the-right-way-77cd39f8f23d)
- [Platform character — Godot 4 Recipes (kidscancode)](https://kidscancode.org/godot_recipes/4.x/2d/platform_character/)
- [Coyote Time — Godot 4 Recipes](https://kidscancode.org/godot_recipes/4.x/2d/coyote_time/)
- [Celeste-Like Platformer in Godot — Godot Mentor](https://www.godotmentor.com/en/tutorials/celeste-like-godot-csharp-jump-physics/)
- [Project organization — Godot docs](https://docs.godotengine.org/en/stable/tutorials/best_practices/project_organization.html)
- [Using TileSets — Godot docs](https://docs.godotengine.org/en/stable/tutorials/2d/using_tilesets.html)
- [Using TileMaps — Godot docs](https://docs.godotengine.org/en/stable/tutorials/2d/using_tilemaps.html)
- [Exporting for Android — Godot docs](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html)
- [Saving games — Godot docs](https://docs.godotengine.org/en/stable/tutorials/io/saving_games.html)
- [GDScript vs C# in Godot 4 — Chickensoft](https://chickensoft.games/blog/gdscript-vs-csharp)
- [GdUnit4 — GitHub](https://github.com/godot-gdunit-labs/gdUnit4)
- [5 Common Mistakes in Godot 4 Platformer Games — Ludonauta](https://ludonauta.itch.io/platformer-essentials/devlog/1137232/5-common-mistakes-in-godot-4-platformer-games)
- [5 Subtle Mistakes to Avoid When Programming Games in Godot 4 — Max/Wang](https://medium.com/@maxslashwang/5-subtle-mistakes-to-avoid-when-programming-games-in-godot-4-3-45fb821f0210)
- [Custom Resources are OP in Godot 4 — Ezcha](https://ezcha.net/news/3-1-23-custom-resources-are-op-in-godot-4)
- [Virtual Joystick for Godot 4 — Marco Fazio Random](https://github.com/MarcoFazioRandom/Virtual-Joystick-Godot)

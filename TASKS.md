# TASKS.md — Multi-Agent Task Board

Coordination file for parallel agents working on Cloudy Ninja.

**Required reading before claiming any task:**
1. [AGENTS.md](AGENTS.md) — architecture, conventions, module layout
2. [GDD_ADDENDUM.md](GDD_ADDENDUM.md) — technical spec, calibration numbers, sprint plan, P0 bug history
3. [GAME_PLAN.md](GAME_PLAN.md) — high-level roadmap, content themes, educational goals

Each task below cites a `GDD ref:` (section number in `GDD_ADDENDUM.md`) when applicable — read that section before starting.

## Workflow

1. **Pick** a task from `## Todo` whose `Depends on` tasks are all `Done`.
2. **Claim** it: move the task block to `## In Progress`, fill in `Agent` and `Branch`, then commit + push to `main`:
   ```
   git add TASKS.md && git commit -m "claim T-XXX" && git push
   ```
3. **Work** on your branch in a worktree: `git worktree add ../cn-T-XXX -b claude/T-XXX-short-desc`
4. **Finish**: merge branch to `main`, then move the task to `## Done` with a one-line outcome and PR/commit hash.
5. **Conflicts**: if `git push` rejects your claim because someone else claimed it first, pull, pick a different task.

**Rules:**
- One task = one branch = one worktree. Don't bundle.
- Don't claim a task whose dependencies aren't `Done`.
- Keep claim-commits tiny (only `TASKS.md`) so conflicts are rare.
- If you abandon a task, move it back to `Todo` and clear the `Agent`/`Branch` fields.

---

## Todo

<!-- ═══════════════════════════════════════════════════════════════
     SPRINT C — "Content & Combat"  (GDD_ADDENDUM §16)
     Priority order: P1 first (enemies, music), then P2, then P3.
     P1 tasks have no blocking dependencies and can run in parallel.
═══════════════════════════════════════════════════════════════ -->

### T-029 — Enemy framework + Smog Sprite patroller  [P1]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `Enemy.kt` abstract base + `SmogSprite.kt` patroller (2-hit Seed Slam defeat, patrol AI). 3 Smog Sprites placed in Level 1 via `EnemyDef`. LevelRunState updates+destroys dead enemies. LevelRenderer draws dark-grey ovals.
- **Commit/PR:** 303f07b
- **Depends on:** _none_
- **GDD ref:** §17 ("Enemy Design Spec")
- **Files:** `entities/Enemy.kt` (new), `entities/SmogSprite.kt` (new), `levels/TmxLevelDefinition.kt`, `levels/LevelRegistry.kt`, `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`
- **Goal:** Add abstract `Enemy` base class (update/draw/takeDamage) and `SmogSprite` concrete patroller. SmogSprite patrols between two x-waypoints, kills player on lateral contact, and can be defeated by Seed Slam droplets (2 hits). Add `enemies: List<EnemyDef>` to `TmxLevelDefinition`; populate Level 1 with 3 Smog Sprites. `LevelRunState` updates all enemies and queues body-destroy on defeat. `LevelRenderer` draws them as dark-grey ShapeRenderer ovals.
- **Done when:** Smog Sprites patrol Level 1, kill the player on contact, die to 2 Seed Slam hits, compile clean, no crash.

### T-030 — Background music system + 3 ambient tracks  [P1]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `MusicManager` with 1.5 s crossfade; `ProceduralMusicGenerator` writes 3 ambient WAVs. `musicTrack` field on TmxLevelDefinition. GameScreen wires play+update.
- **Commit/PR:** 1008c5e
- **Depends on:** _none_
- **GDD ref:** §18 ("Music System Spec")
- **Files:** `audio/MusicManager.kt` (new), `audio/ProceduralMusicGenerator.kt` (new), `screens/GameScreen.kt`, `levels/TmxLevelDefinition.kt`
- **Goal:** Add `MusicManager` singleton with crossfade (1.5 s) between tracks and separate `volMusic` knob. Add `ProceduralMusicGenerator` that writes 60-second looping WAVs (`ambient_arid`, `ambient_wind`, `ambient_eco`) to `assets/audio/music/` on first run. Add `musicTrack: String` field to `TmxLevelDefinition`. `GameScreen.init` calls `MusicManager.play(level.musicTrack, fadeIn = true)`; `GameScreen.render` calls `MusicManager.update(delta)`. Tutorial rooms play `ambient_arid` by default.
- **Done when:** Music plays and crossfades between levels, volume knob works, compile clean.

### T-040 — Projectile / lightning hazard entity  [P1]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `Projectile.kt` kinematic body (HAZARD category), auto-expires on lifetime or wall-hit. LevelRunState holds `projectiles` list and drains expired. LevelRenderer draws orange circles.
- **Commit/PR:** ff0f000
- **Depends on:** _none_
- **GDD ref:** §17.3 ("Projectile entity")
- **Files:** `entities/Projectile.kt` (new), `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`
- **Goal:** Add `Projectile(world, x, y, vx, vy, lifetime)` with a kinematic Box2D body (category=HAZARD_BITS). `LevelRunState` holds `val projectiles = mutableListOf<Projectile>()`, updates each frame, queues body-destroy on expiry or wall-hit. `LevelRenderer` draws projectiles as small orange circles. Expose `LevelRunState.spawnProjectile(x, y, vx, vy)` for boss use. No spawner placed in levels yet — that comes with T-034.
- **Done when:** Projectiles move, kill the player on contact, auto-expire, compile clean.

### T-032 — Stomp-defeat mechanic  [P1]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** WorldContactListener detects player landing on enemy from above (vy < -3 m/s). Enemy marked stomped, player bounced +5 m/s upward. Smoke burst + land SFX on defeat.
- **Commit/PR:** d688fd5
- **Depends on:** T-029
- **GDD ref:** §17.2 ("Stomp mechanic")
- **Files:** `physics/WorldContactListener.kt`, `entities/Enemy.kt`, `screens/LevelRunState.kt`
- **Goal:** In `WorldContactListener.beginContact`, detect player landing on enemy from above (player `vy < -3 m/s`, contact normal pointing up). Mark enemy for defeat + bounce player upward (+5 m/s). `LevelRunState` processes defeat the same way as droplet-hit. Play `land` SFX + smoke burst particle. Stomp must NOT trigger player death even though the enemy fixture is normally lethal.
- **Done when:** Jumping on a Smog Sprite defeats it and bounces player; lateral contact still kills player. Compile clean.

### T-031 — Tile-based terrain rendering  [P2]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §21 ("Tile Rendering Spec")
- **Files:** `rendering/TileRenderer.kt` (new), `screens/LevelRenderer.kt`, `assets/tilesets/` (3 new PNG atlases)
- **Goal:** Create three 128×64 px tileset PNGs (`tiles_arid.png`, `tiles_wind.png`, `tiles_eco.png`) — each with at least 2 tiles: solid-interior and grass/rock-top. Add `TileRenderer` that tile-fills each `ObstacleRect` using `SpriteBatch` instead of the current `ShapeRenderer` solid-rect pass. `LevelRenderer` selects the tileset by `ParallaxTheme`. Remove the ShapeRenderer obstacle-rect draw loop after verifying tile coverage is complete.
- **Done when:** All three levels show tiled terrain instead of solid grey/red rectangles; no visual gaps; compile clean.

### T-033 — Hub world: Sky Sanctuary (Level 0-0)  [P2]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `Level0_0.kt` hub room with 4 portal doors. Portal contact triggers world navigation. Locked worlds show greyed portal. Main menu "Play" → Hub. LevelRunState handles portal activation callback.
- **Commit/PR:** 20bdb0c
- **Depends on:** _none_
- **GDD ref:** §19 ("Hub World Spec")
- **Files:** `levels/Level0_0.kt` (new), `levels/LevelManager.kt`, `screens/MainMenuScreen.kt`, `screens/LevelRunState.kt`
- **Goal:** Add `Level0_0` — single-screen hub room with 4 portal sensor doors (one per world). Portals activate on player contact and navigate to the first level of that world. Locked worlds show a greyed portal (check `GameState.completedLevels`). Register Level0_0 as index 0 in `LevelManager`. Main menu "Play" button goes to `GameScreen(Level0_0)` instead of directly to Level 1. World 0 portal always unlocked; World 1 portal unlocked if World 0 completed.
- **Done when:** Hub loads, player can walk through portals into each world's first level, locked worlds show visually distinct portals. Compile clean.

### T-034 — Boss encounter: Storm Sentinel  [P2]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `StormSentinel` entity with 3 HP and REST/LIGHTNING_TELEGRAPH/LIGHTNING/SWEEP_TELEGRAPH/SWEEP state machine. Level 3 extended to 2840 px with boss arena (boss_floor + 3 combat platforms). BossDef data class + getBossDef() in TmxLevel. Storm_system Atlas entry (6th card). Contact listener handles droplet-on-boss_sentinel. GameScreen instantiates + wires sentinel; defeat sets levelCompleted=true.
- **Commit/PR:** 1f1157c
- **Agent:** claude
- **Branch:** claude/T-034-storm-sentinel
- **Started:** 2026-05-11
- **Depends on:** T-029, T-040
- **GDD ref:** §20 ("Boss Design Spec")
- **Files:** `entities/StormSentinel.kt` (new), `assets/maps/level3.tmx`, `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`, `screens/GameScreen.kt`, `levels/TmxLevelDefinition.kt`, `physics/WorldContactListener.kt`, `atlas/CloudAtlasEntry.kt`
- **Goal:** Extend `level3.tmx` with a 640 px boss arena past the current exit. Add `StormSentinel` — a static sensor entity with 3-phase attack cycle (lightning columns → wind sweep → rest/Seed-Slam window). 3 Seed Slam hits defeat the boss; defeat triggers level exit + unlocks `storm_system` Cloud Atlas entry. Move the Level 3 exit sensor inside the boss room. `LevelRunState` holds an optional `sentinel: StormSentinel?` and updates it if non-null.
- **Done when:** Player reaches boss arena in Level 3, boss cycles attacks, can be defeated in 3 hits, defeat triggers level complete. Compile clean.

### T-035 — Audio bus sliders: music / sfx / ui  [P2]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-030
- **GDD ref:** §18.4 ("Audio bus sliders")
- **Files:** `persist/Settings.kt`, `screens/SettingsScreen.kt`, `audio/MusicManager.kt`, `audio/SoundManager.kt`
- **Goal:** Add `volMusic: Float = 0.7f` and `volUi: Float = 0.9f` to `Settings` (existing `volSfx` stays). Replace the single SFX slider in `SettingsScreen` with three VisUI sliders labelled "Music", "SFX", "UI". On slider change: call `MusicManager.setMusicVolume(v)` and `SoundManager.setVolume(v)` respectively. Persist immediately via `SettingsManager.save()`. `GameScreen.init` applies all three volumes.
- **Done when:** Three sliders visible in Settings, all three volumes respond in real-time, persist across sessions. Compile clean.

### T-036 — Key rebinding UI in Settings  [P2]
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `keybinds: Map<String,Int>` added to Settings. SettingsScreen "Controls" panel with 5 rebindable actions. InputManager reads from keybinds. Persists across sessions.
- **Commit/PR:** 4c9e74e
- **Depends on:** _none_
- **GDD ref:** GDD_ADDENDUM §16 gap analysis
- **Files:** `persist/Settings.kt`, `screens/SettingsScreen.kt`, `input/InputManager.kt`
- **Goal:** Add `keybinds: Map<String, Int> = defaultKeybinds()` to `Settings` where keys are action names (`"left"`, `"right"`, `"jump"`, `"action"`, `"swap"`) and values are `Input.Keys.*` ints. Add a "Controls" section in `SettingsScreen` — for each action, show a VisTextButton displaying the current key name; clicking it enters "press a key" mode and records the next key press. `InputManager` reads keybinds from `SettingsManager.load().keybinds` on each poll instead of hardcoded constants.
- **Done when:** Player can rebind all 5 actions in Settings, new bindings work in gameplay, persist across sessions. Compile clean.

### T-037 — Achievement system + toast notifications  [P3]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §22 ("Achievement System Spec")
- **Files:** `progression/Achievement.kt` (new), `progression/AchievementRegistry.kt` (new), `screens/AchievementToast.kt` (new), `persist/GameState.kt`, `screens/LevelRunState.kt`, `screens/GameScreen.kt`, `screens/StatsScreen.kt` (see T-041)
- **Goal:** Implement the 12 achievements from GDD §22.1. Add `unlockedAchievements: Set<String>` to `GameState`. Add `AchievementToast` — slides in from top-right, holds 2.4 s, fades out, never overlaps. `LevelRunState.update()` checks unlock conditions for in-game achievements (first_jump, first_cleanse, eco_sweep, no_death_run). `LevelTransitionController` checks speed_demon and world clear achievements. `GameScreen` renders toast above Layer 4 (HUD).
- **Done when:** At least 6 achievements can be unlocked during normal play; toast appears and dismisses cleanly; unlocked set persists. Compile clean.

### T-038 — Ghost replay in time trials  [P3]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §23 ("Ghost Replay Spec")
- **Files:** `persist/GhostRecording.kt` (new), `persist/SaveManager.kt`, `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`
- **Goal:** During a time-trial run, `LevelRunState` records one `GhostFrame(x, y, facingRight, character)` every 3 rendered frames. On new best time, serialize to `saves/ghost_{levelId}.json` via a new `SaveManager.saveGhost/loadGhost` pair. On subsequent time-trial runs for the same level, load the ghost and advance a `ghostFrameIndex` each frame. `LevelRenderer` draws the ghost as a 35%-alpha tinted circle/sprite at the ghost position.
- **Done when:** Setting a new best saves a ghost; next run shows the ghost moving through the level; ghost does not interfere with gameplay. Compile clean.

### T-041 — Stats screen on main menu  [P3]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GDD_ADDENDUM §16 gap analysis
- **Files:** `screens/StatsScreen.kt` (new), `screens/MainMenuScreen.kt`
- **Goal:** Add a "Stats" button to `MainMenuScreen` that opens `StatsScreen`. Stats screen shows per-slot: total deaths, levels completed (count + list), eco-tokens collected (running total from completed runs), best times per level, achievements unlocked (count/12 + list). All data read from `SaveManager.loadGame()` + `AchievementRegistry`. Back button returns to main menu.
- **Done when:** Stats screen opens from main menu, displays accurate data for the active slot, back button works. Compile clean.

### T-046 — Full graphics overhaul: pixel-art sprites + tilesets  [P3]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-031
- **GDD ref:** _to be written in GDD_ADDENDUM_
- **Files:** `assets/tilesets/` (PNG atlases), `assets/sprites/` (character sprite sheets), `rendering/SpriteFactory.kt`, `rendering/CharacterAtlas.kt`, `rendering/TileRenderer.kt` (from T-031)
- **Goal:** Replace all procedurally-generated geometry with hand-drawn (or tool-generated) pixel-art assets. Minimum deliverable: (a) 3 character sprite sheets (Ebo/Laya/Zephyr) at 64×64 per frame — idle, run (4f), jump, fall, wall-slide; (b) 3 tileset PNGs (tiles_arid/tiles_wind/tiles_eco) replacing ShapeRenderer ground/wall rectangles — solid interior + grass/rock top tile variants (completes T-031); (c) enemy sprite (Smog Sprite oval → proper sprite); (d) boss sprite (Storm Sentinel box → animated sprite). All assets at 32×32 base scaled by `DisplayScale.spriteScale` at load time. Remove ShapeRenderer primitive draw paths after verifying visual coverage.
- **Done when:** Game renders no ShapeRenderer primitives for terrain or characters. All visual elements use TextureRegion. T-031 is a blocker (tile-fill infrastructure). Compile and run clean.

### T-045 — Cloud Atlas expansion to 12 entries  [P3]
- **Status:** Todo
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-034
- **GDD ref:** GAME_PLAN §2 (educational goals), GDD_ADDENDUM §22 (atlas_full achievement)
- **Files:** `atlas/CloudAtlasLibrary.kt`, `levels/TmxLevelDefinition.kt` (level1/2/3 snapshot lists)
- **Goal:** Expand `CloudAtlasLibrary.ALL` from 5 to 12 entries, each with a real educational fact about the water cycle or climate systems. Distribute new snapshots across levels (2–3 per level, including the boss-room `storm_system` from T-034). Update LevelRegistry snapshot lists. Entries should cover: water_cycle, silver_iodide, temperature_inversion, albedo_effect, transpiration, groundwater_recharge, carbon_sequestration, storm_system, biodiversity_index, soil_microbiome, ocean_acidification, cloud_seeding.
- **Done when:** 12 entries in registry, all reachable in gameplay, atlas screen displays all 12 cards with correct text. Compile clean.


---

## In Progress

_(none — all claimed tasks completed)_

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** In Progress
- **Agent:** <your-handle>
- **Branch:** claude/T-XXX-short-desc
- **Started:** YYYY-MM-DD
- **Depends on:** ...
- **Files:** ...
- **Goal:** ...
- **Done when:** ...
- **Progress notes:** (optional, append as you go)
-->

---

## Done

### T-044 — Polish: HUD transparency + Settings font scaling + visual geometry
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** (1) All 5 HUD buttons set to `color.a = BTN_ALPHA = 0.55f` so the player character is visible through them. (2) `SettingsScreen` body labels migrated from `VisLabel` (baked VisUI skin font, non-scaling) to `Label` with `FontManager.getShared(16)` (DisplayScale-aware, sharp at 4K). (3) `ParallaxBackground` upgraded: 3-band sky gradient, new midground hill layer (scrollFactor=0.28), stars in corrupted sky (fade by cleanseRatio=0.5), mountain peak highlight caps, pine-crown triangles above tree trunks. (4) `LevelRenderer` upgraded: grass tufts along ground top surface (deterministic sin-based height variation), triangular spike shapes on hazard tiles, bottom shadow strip on ground blocks, underside shadow on moving platforms.
- **Commit/PR:** (this session — claude/T-034-storm-sentinel branch)
- **Depends on:** T-042

### T-043 — Bug fixes: SaveManager per-frame spam + Box2D portal crash + spawn-death flipY
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** (1) `SaveManager` in-memory cache (`private val cache`) prevents per-frame disk reads — `LevelRenderer.renderWorld()` was calling `loadGame()` on every frame for portal color checks. (2) Portal transition deferred to end-of-frame via `pendingPortalTarget: String?` field in `LevelRunState`; `GameScreen.render()` handles the actual `game.screen = GameScreen(...)` + `dispose()` after all rendering is done, eliminating Box2D native crash (`EXCEPTION_ACCESS_VIOLATION` in `gdx-box2d64.dll`). (3) `TmxLevel.setup()` changed from `flipY=false` to `flipY=true` — libGDX's TmxMapLoader already flips rectangle Y internally so the second flip corrects it; without this fix ground was placed at the top of the screen and all campaign-level players spawn-died immediately.
- **Commit/PR:** (this session — claude/T-034-storm-sentinel branch)
- **Depends on:** _none_

### T-042 — 4K / HiDPI display scaling
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `DisplayScale` singleton computes `fontScale = min(physW/1280, physH/720)` and `spriteScale = max(1, floor(fontScale))` at startup and after mode changes. `FontManager.create(size)` multiplies by `fontScale` so text renders at exactly `size` virtual pixels regardless of physical resolution. Resolution presets (HD/FHD/2K/4K) and fullscreen toggle added to `SettingsScreen`; `applyDisplaySettings()` calls `DisplayScale.init() + FontManager.clearSharedCache()` after mode switch. Game tested at 2560×1440 (fontScale=2.0).
- **Commit/PR:** (this session — claude/T-034-storm-sentinel branch)
- **Depends on:** _none_
- **Files:** `rendering/DisplayScale.kt` (new), `FontManager.kt`, `screens/SettingsScreen.kt`

### T-001 — Migrate Hud.kt buttons to VisUI
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** HUD `Label`/`Table`/`Image` widgets migrated to `VisLabel`/`VisTable`/`VisImage`. Buttons were already VisUI. Compile clean, behavior unchanged.
- **Commit/PR:** 3fd1a91

### T-002 — Add Kotest specs for PlayerController state machine
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** 4 test files added: `PlayerControllerJumpTest`, `PlayerControllerMovementTest`, `PlayerControllerStateTest`, `PlayerControllerWallAndAbilityTest`. Covers coyote-time, jump-buffer, ability-swap, wall contacts.
- **Commit/PR:** merged via worktree

### T-003 — Save/load UI in Settings menu
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Save/Load/Delete buttons wired to SaveManager in SettingsScreen with toast confirmation.
- **Commit/PR:** 190d96b

### T-004 — Checkpoint restart via serialized GameState
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Player respawns at last checkpoint on death; `resumeCheckpoint` passed into GameScreen constructor.
- **Commit/PR:** 4617e9e

### T-005 — Kotest specs for WorldContactListener
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `WorldContactListenerTest` covers player_foot, player_wall_left/right, hazard kill, flashing invincibility.
- **Commit/PR:** 3db00d0

### T-006 — World 0 Room 1 "First Step" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_1.kt` added; single-screen room with one jump gap and one eco-token. Registered in LevelManager.
- **Commit/PR:** 2449d75

### T-007 — World 0 Room 2 "Long Fall" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_2.kt` added; teaches variable jump height (low ceiling) and coyote time (walk-off ledge). Two eco-tokens.
- **Commit/PR:** d4e37f2

### T-008 — World 0 Room 3 "Wall Climb" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_3.kt` added; 120 px wide chimney shaft, ~4 wall-jumps needed, safety net at bottom, horizontal EXIT sensor at top. Registered in LevelManager.
- **Commit/PR:** this branch

### T-010 — Corner correction in PlayerController
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Raycast-based corner nudge when rising into clipped overhead obstacles (≤ CORNER_CORRECT_M).
- **Commit/PR:** d09d0b1

### T-011 — Footstep particles
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Alternate L/R dust particles every 12 cm of grounded horizontal travel via `onFootstep` callback.
- **Commit/PR:** ed58182

### T-012 — Camera vertical platform snap
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `cameraTargetY` only updates when grounded or falling past threshold — no Y bobbing during jump arcs.
- **Commit/PR:** 1e22d03

### T-013 — Generate 8 base SFX via ProceduralSoundGenerator
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** jump, land, collect_token, collect_snapshot, death, checkpoint, level_complete, hazard_cleansed WAV files generated and placed in `assets/audio/sfx/`.
- **Commit/PR:** e38ce48

### T-014 — Wire SoundManager into gameplay events
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** All 8 canonical sounds wired into jump, land, collect, death, checkpoint, level_complete, and hazard_cleansed events.
- **Commit/PR:** 34b2cd3

### T-015 — Three save slots UI
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Main menu replaced Continue/New Game with 3 slot cards showing level, progress, deaths, last-played. Wired to SaveManager.
- **Commit/PR:** 890a472

### T-016 — Refactor levels to data-driven registry
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Level1/2/3 classes removed; single `TmxLevelDefinition` registry in `LevelRegistry.ALL`. Adding a level = one registry entry.
- **Commit/PR:** d7c77f4

### T-017 — Investigate intermittent native Box2D crash
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Root cause documented in BUGS.md; defensive fixes: friction-based platform carry (no stale body refs), contact-begin/end logging, isPlatformBodyValid guard, deferred body-destroy queue.
- **Commit/PR:** 2a80160

### T-018 — Tests for MapLevelLoader coordinate flipping
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `MapLevelLoaderCoordTest.kt` added; BehaviorSpec covers flipY=true/false centerOf formula, symmetry invariant, and moving-platform endY translation — pure math tests, no libGDX runtime needed.
- **Commit/PR:** this branch

### T-019 — Collect sparkles on eco-token and snapshot pickup
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** 6–10 particle burst spawned at collection site; additive cyan/yellow colors; reuses 200-particle pool.
- **Commit/PR:** 896d7d4

### T-020 — Apply Celeste-calibrated movement constants + asymmetric gravity
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** Full constant table from GDD §2.1 in Constants.kt; asymmetric gravity + apex-hang gravityScale applied per frame; terminal velocity capped.
- **Commit/PR:** dccb872

### T-022 — Particle pool eviction tests
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `ParticleSystemTest.kt` covers overflow (silent drop), lifespan expiry, slot reuse.
- **Commit/PR:** 609ac4b

### T-023 — Zephyr third-character ability skeleton
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `ZephyrAbility.kt` implements Float (gravity 0.2f for 1.5 s, cooldown 3 s); radial WindTrail burst at activation; GameScreen cycles Ebo → Laya → Zephyr → Ebo; Zephyr renders as light-purple tinted sprite.
- **Commit/PR:** this branch

### T-026 — Per-level parallax background theming
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `ParallaxTheme` enum (ARID/WIND/ECO) added to `ParallaxBackground`; Level 1 = warm browns/golds, Level 2 = slate blues/whites, Level 3 = deep-to-bright greens. GameScreen selects theme by level ID.
- **Commit/PR:** this branch

### T-027 — CloudAtlasLibrary.get unit tests
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `CloudAtlasLibraryTest.kt` covers known-ID lookup, unknown-ID null, non-blank fields, and unique IDs.
- **Commit/PR:** fc297c3

### T-021 — Split GameScreen into focused subsystems
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** GameScreen 1214 → 349 LOC; `LevelRunState.kt` (~320L) owns all state + update loop; `LevelRenderer.kt` (~290L) owns all drawing + particle helpers + Palette; `LevelTransitionController.kt` (~85L) owns level-complete + goToNextLevel. No behaviour change.
- **Commit/PR:** this branch

### T-009 — World 0 Room 4 "First Cleanse" tutorial
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `Level0_4.kt` added; 820 px hazard strip blocks passage; Hud.showActionHint pulses action button at 1.5 Hz; GameScreen enables hint while cleanseRatio==0 in level0_4; Level0_4 registered in LevelManager before campaign levels.
- **Commit/PR:** this branch

### T-024 — Time trial mode
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `GameState.bestTimes` map added; pause menu "▶ Time Trial" / "Exit Time Trial" button restarts level with `isTimeTrial=true`; stopwatch (cyan, top-centre) visible in trial mode; checkpoint autosaves suppressed in trial; best time saved to `GameState.bestTimes` on completion; VictoryScreen shows trial time + "★ NEW BEST! ★" banner. Wired through `GameScreen → LevelRunState + LevelTransitionController`.
- **Commit/PR:** this branch

### T-025 — Level 3 pacing rebalance per Kishōtenketsu
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `assets/maps/level3.tmx` rewritten with Ki/Shō/Ten/Ketsu zones; wall-jump shaft moved to ~55%; moving-platform gauntlet in final 15%. LevelRegistry level3 checkpoints and eco-tokens updated to match new layout. *(Note: the flipY fix shipped later in T-043 — `flipY=true` is now correct.)*
- **Commit/PR:** this branch

### T-028 — Android lint + build verification
- **Status:** Done
- **Completed:** 2026-05-09
- **Outcome:** `.github/workflows/ci.yml` created; runs `:core:compileKotlin`, `:core:test`, and `android:lint` on push/PR to main; uploads lint and test reports as artifacts.
- **Commit/PR:** this branch

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** Done
- **Completed:** YYYY-MM-DD
- **Outcome:** one-line summary of what shipped
- **Commit/PR:** <hash or PR link>
-->

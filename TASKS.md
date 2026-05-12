# TASKS.md — Multi-Agent Task Board

Coordination file for parallel AI agents working on Cloudy Ninja.

**REQUIRED READING — every agent, every session:**
1. **[START_HERE.md](START_HERE.md)** — entry point: identity, capability gates, claim protocol, routing
2. [AGENTS.md](AGENTS.md) — architecture, conventions, module layout
3. [LEARNINGS.md](LEARNINGS.md) — gotchas from previous sessions (read before claiming)
4. [GDD_ADDENDUM.md](GDD_ADDENDUM.md) — technical reference (read sections relevant to your ticket)
5. [GAME_PLAN.md](GAME_PLAN.md) — vision and roadmap

## Strict routing model

Every ticket has a `Tool:` field tagged by the planner (Claude Code Opus). **AIs do NOT self-route.** If your identity (from `START_HERE.md` §1) does not match the ticket's `Tool:` field, **do not claim that ticket** — even if it's unclaimed and looks easy. Wrong-tool execution is the primary failure mode of multi-AI systems.

If you need a task and nothing is tagged for your identity, append to `QUESTIONS.md` and stop. The planner will route something to you.

## Workflow

1. **Pick** a task from `## Todo` whose `Tool:` matches your identity AND whose `Depends on` tasks are all `Done`.
2. **Claim** it: move the task block to `## In Progress`, fill in `Agent`, `Branch`, `Started`, then commit + push to `main`:
   ```
   git add TASKS.md && git commit -m "claim T-XXX" && git push
   ```
3. **Work** on your branch in a worktree: `git worktree add ../cn-T-XXX -b <identity-prefix>/T-XXX-short-desc`
   - Branch prefix per identity: `claude/...`, `copilot/...`, `antigravity/...`, etc.
4. **Finish**: open a PR; CI smoke test (T-A1) must pass; merge to `main`; move the task to `## Done` with a one-line outcome and PR/commit hash.
5. **If you hit a non-obvious gotcha:** append to `LEARNINGS.md` so the next agent doesn't repeat it.
6. **If you hit ambiguity you can't resolve:** append to `QUESTIONS.md` and release the claim.

**Rules:**
- One task = one branch = one worktree. Don't bundle.
- Don't claim a task whose dependencies aren't `Done`.
- Don't claim a task whose `Tool:` doesn't match your identity.
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
- **Tool:** `human-then-claude-code-sonnet`  *(user downloads Kenney `pixel-platformer` zip from https://kenney.nl/assets/pixel-platformer and extracts to `assets/tilesets/kenney_pixel_platformer/`; then Claude wires `TileRenderer`)*
- **Tier:** M
- **Autonomous-eligible:** no  *(blocks on user asset download; Kenney's site has an optional donation gate that automation shouldn't bypass)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §21 ("Tile Rendering Spec")
- **Art decision (resolved 2026-05-12 via T-046a):**
  - **Base pack:** Kenney `pixel-platformer` (CC0, ~350 files, side-scroller perspective) — provides terrain, characters, enemies, pickups, hazards
  - **ECO accent:** [OpenGameArt Pixel Art Forest](https://opengameart.org/content/pixel-art-forest-tilesets) (CC0) for vines, foliage
  - **ARID/WIND:** use Kenney's sandy/sky tiles within the base pack (no separate tilesets needed)
- **Files:** `rendering/TileRenderer.kt` (new), `screens/LevelRenderer.kt`, `assets/tilesets/kenney_pixel_platformer/` (user-supplied), `assets/tilesets/eco_accents/` (optional)
- **Goal:** Wire Kenney's `pixel-platformer` tileset (and ECO accent) into a new `TileRenderer` that tile-fills each `ObstacleRect` using `SpriteBatch` instead of the current `ShapeRenderer` solid-rect pass. `LevelRenderer` selects tiles by `ParallaxTheme` (ARID/WIND uses Kenney base, ECO mixes in forest accents). Remove the ShapeRenderer obstacle-rect draw loop after verifying tile coverage is complete.
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
- **Tool:** `copilot-agent`  *(autonomous from GitHub Issue)*
- **Tier:** S
- **Autonomous-eligible:** yes
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


### T-038 — Ghost replay in time trials  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`  *(determinism-sensitive — read DETERMINISM.md first; not autonomous)*
- **Tier:** M
- **Autonomous-eligible:** no  *(per START_HERE.md §7: determinism-sensitive work needs human review)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** §23 ("Ghost Replay Spec")
- **Files:** `persist/GhostRecording.kt` (new), `persist/SaveManager.kt`, `screens/LevelRunState.kt`, `screens/LevelRenderer.kt`
- **Goal:** During a time-trial run, `LevelRunState` records one `GhostFrame(x, y, facingRight, character)` every 3 rendered frames. On new best time, serialize to `saves/ghost_{levelId}.json` via a new `SaveManager.saveGhost/loadGhost` pair. On subsequent time-trial runs for the same level, load the ghost and advance a `ghostFrameIndex` each frame. `LevelRenderer` draws the ghost as a 35%-alpha tinted circle/sprite at the ghost position.
- **Done when:** Setting a new best saves a ghost; next run shows the ghost moving through the level; ghost does not interfere with gameplay. Compile clean.



### T-046 — Full graphics overhaul: pixel-art sprites + tilesets  [P3]
- **Status:** Todo
- **Tool:** `human-then-antigravity-then-claude-code-sonnet`  *(human picks style/source; Antigravity automates asset pipeline; Claude wires sprites into renderer)*
- **Tier:** L
- **Autonomous-eligible:** no  *(art style + commissioning decisions require user input)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-031
- **GDD ref:** _to be written in GDD_ADDENDUM_
- **Files:** `assets/tilesets/` (PNG atlases), `assets/sprites/` (character sprite sheets), `rendering/SpriteFactory.kt`, `rendering/CharacterAtlas.kt`, `rendering/TileRenderer.kt` (from T-031)
- **Goal:** Replace all procedurally-generated geometry with hand-drawn (or tool-generated) pixel-art assets. Minimum deliverable: (a) 3 character sprite sheets (Ebo/Laya/Zephyr) at 64×64 per frame — idle, run (4f), jump, fall, wall-slide; (b) 3 tileset PNGs (tiles_arid/tiles_wind/tiles_eco) replacing ShapeRenderer ground/wall rectangles — solid interior + grass/rock top tile variants (completes T-031); (c) enemy sprite (Smog Sprite oval → proper sprite); (d) boss sprite (Storm Sentinel box → animated sprite). All assets at 32×32 base scaled by `DisplayScale.spriteScale` at load time. Remove ShapeRenderer primitive draw paths after verifying visual coverage.
- **Done when:** Game renders no ShapeRenderer primitives for terrain or characters. All visual elements use TextureRegion. T-031 is a blocker (tile-fill infrastructure). Compile and run clean.

### T-045 — Cloud Atlas expansion to 12 entries  [P3]
- **Status:** Todo
- **Tool:** `notebooklm-then-copilot-agent`  *(see START_HERE.md §8 for NotebookLM workflow — user uploads climate sources, NotebookLM drafts 12 grounded entries, Copilot wires them into `CloudAtlasLibrary.kt`)*
- **Tier:** S
- **Autonomous-eligible:** yes-with-review  *(NotebookLM output should be skim-reviewed for accuracy before wiring)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-034
- **GDD ref:** GAME_PLAN §2 (educational goals), GDD_ADDENDUM §22 (atlas_full achievement)
- **Files:** `atlas/CloudAtlasLibrary.kt`, `levels/TmxLevelDefinition.kt` (level1/2/3 snapshot lists)
- **Goal:** Expand `CloudAtlasLibrary.ALL` from 5 to 12 entries, each with a real educational fact about the water cycle or climate systems. Distribute new snapshots across levels (2–3 per level, including the boss-room `storm_system` from T-034). Update LevelRegistry snapshot lists. Entries should cover: water_cycle, silver_iodide, temperature_inversion, albedo_effect, transpiration, groundwater_recharge, carbon_sequestration, storm_system, biodiversity_index, soil_microbiome, ocean_acidification, cloud_seeding.
- **Done when:** 12 entries in registry, all reachable in gameplay, atlas screen displays all 12 cards with correct text. Compile clean.


---

## Backlog — AI testing v2 (planned, after MVP T-A1/T-A2 lands)

MVP (T-A1) catches the bug class that just shipped (spawn-death, crashes, perf regressions). The tickets below add coverage for failure modes the MVP cannot catch. Build them only when a specific bug demands the work — do not pre-spend.

### T-A3 — Input record & replay
- Determinism prerequisite: every site flagged as "needs work" in T-A2's DETERMINISM.md must be fixed (seeded RNG wrapper, sorted Map iteration, fixed timestep)
- `InputRecorder` writes JSON: `{levelId, seed, gameVersion, frames[], endStateChecksum}`
- `ReplayAgent` deterministically replays a recording; CI asserts checksum match within tolerance
- Use case: pinning a known-good run of a tricky level as a regression; reproducing flaky bug reports
- Estimated tokens: ~80k (Sonnet sub-agent), $0.55

### T-A4 — Reactive `SensorAgent` (replaces hand-tuned waypoints)
- Box2D raycast-based local sensing: wall ahead → jump; gap ahead → pre-jump; hazard ahead → ability; stuck → swap character
- Zero per-level config; generalizes to new levels automatically
- Use case: when `BasicAutopilot` gets stuck on a future level we add. Defer until that happens.
- Estimated tokens: ~60k (Sonnet sub-agent), $0.40

### T-A7 — Menu/UI smoke agent + UI invariants
- `MenuSmokeAgent` taps through every Scene2D screen reachable from MainMenu
- `UiInvariantChecker` runs per screen: no UI overflow, every button has a click handler, every Label uses `FontManager` font (catches T-044 Settings-font-style bugs), no overlapping interactive elements, contrast ΔE > 20
- Use case: catching menu/settings regressions that the gameplay-level smoke (T-A1) cannot see. Build when we ship the first UI regression.
- Estimated tokens: ~70k (Sonnet sub-agent), $0.45

### T-A8 — Windowed CI lane (visual regression)
- Separate nightly workflow that runs windowed (real GL via `xvfb-run`)
- Screenshots framebuffer at fixed replay frames, diffs against golden PNGs in `tests/golden/`
- Defer until T-046 (full art overhaul) is done — visual diffs against procedural geometry are too noisy to maintain
- Estimated tokens: ~60k

### T-A9 — Boss combat sub-agent
- Recognize Storm Sentinel lightning telegraph, dodge, attack on REST phase
- Already decided **not worth doing** (6h+ of tuning, flaky CI risk). Documented here only so a future contributor doesn't redo the analysis.
- Replacement coverage (in T-A1): "boss entered IDLE_COMBAT within 5s of player entering arena" invariant.

**v2 total if all built:** ~$2.85 in Sonnet sub-agent tokens.

---

## In Progress

_(none — all claimed tasks completed)_


<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** In Progress
- **Agent:** <your-identity-from-START_HERE.md-section-1>
- **Tool:** <pre-tagged by planner — do not change>
- **Tier:** S | M | L
- **Autonomous-eligible:** yes | no
- **Branch:** <identity-prefix>/T-XXX-short-desc
- **Started:** YYYY-MM-DD
- **Depends on:** ...
- **Files:** ...
- **Goal:** ...
- **Done when:** ...
- **Progress notes:** (optional, append as you go)
-->

---

## Done

### T-037 — Achievement system + toast notifications
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** 12 achievements from GDD §22 implemented. `progression/Achievement.kt` + `AchievementRegistry.kt` + `screens/AchievementToast.kt` (FitViewport+Stage toast with smoothstep slide-in, 2.4s hold, fade-out, internal queue prevents overlap). `GameState` gained `unlockedAchievements: Set<String>` + `totalStomps: Int` (defaults keep saves backward-compatible). 11 unlock hooks wired across `LevelRunState` + `LevelTransitionController` + `GameScreen.sentinel.onDefeated`. Toast renders at Layer 4.5 (above HUD, below pause). `FontManager.getShared()` used per T-044 lesson. **Implemented by Claude Sonnet sub-agent in ~7 min; auto-merged via PR #7.**
- **Commit/PR:** PR #7 (squashed merge `9b27015`)
- **Tool:** `claude-code-sonnet`

### T-041 — Stats screen on main menu
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** New `screens/StatsScreen.kt` shows per-slot stats: total deaths, completed levels, achievements unlocked (count/12 + list). Reads `SaveManager.loadGame(slot)` for each of 3 slots. Scrollable card layout. Stats button added to `MainMenuScreen` between Atlas and Settings. **Implemented end-to-end by Copilot coding agent autonomously from GitHub Issue #2; auto-merged via PR #3.**
- **Commit/PR:** PR #3 (squashed merge `4d592bc`)
- **Tool:** `copilot-agent`

### T-A1 — AI smoke test: per-level autopilot run via CI
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** Headless smoke test running on every PR. `LevelRunState` emits a structured `[smoke]` log line on auto-quit when `cloudy.smokeMode=true`; `Constants.SMOKE_MODE` flag suppresses screen transitions + atlas-overlay gate so the autoquit always fires; `Main.kt` `cloudy.smokeLevel` bypasses menu→GameScreen. `.github/workflows/ai-smoke.yml` runs an 8-level matrix via `xvfb-run`, parses the log line, fails the build on `deltaX<0.3` (spawn-death) or `frameP99>80ms` (perf regression) or crashed process. All 9 required CI checks (1 lint + 8 smoke) gate `main` branch merge. **PR #1 validated the system end-to-end: 8 bug layers peeled (desktop.ini, gradlew chmod, threshold tuning, queue saturation, level-hopping, overlay-blocked-update, cold-runner timeout) before green run merged.** Each layer documented in `LEARNINGS.md`.
- **Commit/PR:** PR #1 (squashed merge `3468df1`)
- **Tool:** `claude-code-opus`

### T-A2 — Determinism audit (`DETERMINISM.md`)
- **Status:** Done
- **Completed:** 2026-05-11
- **Outcome:** `DETERMINISM.md` written at repo root. Catalogs every non-deterministic site in `core/src/main/kotlin/com/sohai/platformer/`. **Findings:** 4 gameplay-breaking sites flagged for future T-A3 (StormSentinel:183,190 — random lightning + sweep params; EboAbility:108,112 — raindrop spawn jitter + speed). 18 cosmetic sites safe to leave (particle randomization, audio pitch variation). 0 surprises: `world.step` uses fixed 1/60 s accumulator. No code changes — audit was the deliverable.
- **Commit/PR:** 0f3aff0
- **Tool:** `claude-code-sonnet` (sub-agent dispatched from `claude-code-opus`)

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

### T-046a — Tileset research: find pixel-art tilesets for 3 themes
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Antigravity (Gemini 3.1 Pro) researched 12 candidate tilesets (4 per theme: ARID/WIND/ECO) from OpenGameArt and Kenney.nl, compiled into `art-research/tileset-candidates.md` with name, source URL, license, file count, theme fit, art quality (1-5), and character-sprite notes. **Decision (post-visual-review):** Kenney `pixel-platformer` (CC0, ~350 files, side-scroller perspective) as base + OpenGameArt Pixel Art Forest (CC0) for ECO accents; ARID/WIND use Kenney's sandy/sky tiles within base pack. One Antigravity recommendation rejected post-review (Whispers of Avalon Desert — top-down RPG perspective; flagged in LEARNINGS.md as research-tool blindspot). **Antigravity time-to-output: ~5 min** for research; ~5 min of human visual review.
- **Commit/PR:** PR #10 (merged) + decision recorded in `GAME_PLAN.md` and T-031 unblocked.
- **Tool:** `antigravity`

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** Done
- **Completed:** YYYY-MM-DD
- **Outcome:** one-line summary of what shipped
- **Commit/PR:** <hash or PR link>
-->

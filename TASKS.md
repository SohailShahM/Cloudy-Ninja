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
- **Updated dependency (2026-05-12):** T-045 now depends on **T-049** (climate-source compilation). The climate.gov URLs in the original prompt are dead (site archived to noaa.gov). T-049 produces a `research/climate-sources/` folder with verified-live URLs + downloaded PDFs, ready to feed NotebookLM in one step.




### T-061 — AI smoke: per-character autopilot matrix  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`  *(touches CI workflow — supervise / not fully autonomous)*
- **Tier:** S
- **Autonomous-eligible:** no  *(CI changes warrant a human review per START_HERE.md §7)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-A1
- **GDD ref:** T-A1 outcome
- **Files:** `.github/workflows/ai-smoke.yml`, `core/src/main/kotlin/com/sohai/platformer/Main.kt` (cloudy.smokeCharacter property)
- **Goal:** Extend smoke matrix from 9 (level × default-character) to 27 (level × {ebo, laya, zephyr}). Add `cloudy.smokeCharacter` system property to `Main.kt` that pre-selects the starting character before `GameScreen` init. Update workflow to include a `character` axis.
- **Done when:** All 27 smoke jobs pass on a PR; CI duration acceptable (parallel jobs, should still finish under 6 min).

- **Done when:** Fade-in visible at 60 FPS without input lag, hint visible, smoke CI passes.


### T-076 — Execute low-risk dependency upgrades (from T-051 audit)  [P2]
- **Status:** Todo
- **Tool:** `antigravity`
- **Tier:** M  *(real code work — bumps versions, must keep CI green)*
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** `antigravity/T-076-dep-upgrades-low-risk`
- **Depends on:** T-051
- **GDD ref:** `research/dependency-audit.md` (the T-051 deliverable)
- **Files:** `build.gradle.kts`, `core/build.gradle.kts`, `android/build.gradle.kts`, plus any Kotlin source files that need adjustment for breaking changes.
- **Goal:** Read T-051's `research/dependency-audit.md` and execute every upgrade tagged **upgrade-risk: LOW** (and only LOW). For each upgrade, open it as a **separate PR** so failures can be reverted independently. For each PR: (1) bump the version in the right gradle file, (2) fix any compilation errors the bump introduces, (3) run `./gradlew :core:test` locally + ensure CI passes, (4) include a one-paragraph PR description citing the audit entry. **Do not do MEDIUM or HIGH risk upgrades** — those need a separate decision.
- **Done when:** All LOW-risk upgrades from the audit are merged as individual PRs, each with green CI. If any LOW upgrade unexpectedly breaks something, halt that PR and post to QUESTIONS.md — do not chain into a multi-version cascade.
- **Constraints:** One upgrade = one PR. Don't combine. If the audit lists 5 LOW upgrades, that's 5 PRs. Don't bypass CI. Don't touch source code beyond what each version bump strictly requires.

### T-081 — Android build verification + smoke  [P3]
- **Status:** Todo
- **Tool:** `antigravity`
- **Tier:** M
- **Autonomous-eligible:** yes-with-review  *(if a real fix is needed, surface it)*
- **Agent:** _unclaimed_
- **Branch:** `antigravity/T-081-android-build`
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (mobile-platform support)
- **Files:** `android/build.gradle.kts`, `android/src/...` (only if a real fix is required), `.github/workflows/ci.yml` (add an `assembleDebug` step)
- **Goal:** Verify the `android` module still builds an APK in 2026 toolchain. (1) Run `./gradlew android:assembleDebug` locally — if it fails, document each error in a `BUILD-LOG.md` and only fix issues that are *obviously* trivial (deprecated API name swaps, AGP version mismatches). (2) Add a CI step that runs `assembleDebug` on every PR so we catch future regressions early. (3) Surface any *non-trivial* failures (e.g. a manifest schema change, a Box2D-Android ABI mismatch) in `QUESTIONS.md` — do not improvise architectural decisions.
- **Done when:** Either: (A) Android APK builds locally + CI step passes + a one-line "Mobile build OK as of 2026-05-12" note in LEARNINGS.md. Or: (B) `BUILD-LOG.md` + `QUESTIONS.md` entry documents the blocker; ticket marked blocked. Either outcome is a success — clarity is the goal.
- **Constraints:** Do NOT attempt risky toolchain bumps (AGP > 1 minor, Kotlin > 1 minor, Java target changes). Do NOT publish APK to anywhere. Do NOT change signing config.


### T-098 — Enemy hit-flash on takeDamage (200ms white tint)  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-029, T-062
- **GDD ref:** GAME_PLAN.md (combat juice)
- **Files:** `entities/Enemy.kt` (abstract base — add `hitFlashTimer`), `entities/SmogSprite.kt` + `entities/DriftHusk.kt` (set timer in takeDamage), `screens/LevelRenderer.kt` (lerp color toward white when timer > 0)
- **Goal:** Add `hitFlashTimer: Float = 0f` to `Enemy`. `takeDamage()` sets it to 0.2f. `update(delta)` decrements. `LevelRenderer` reads `enemy.hitFlashTimer` and lerps base color toward white `(1, 1, 1, 1)` by `clamp(timer / 0.2f)`. Don't touch defeat path; just hit-feedback frames.
- **Done when:** Visible hit-flash on Seed-Slamming SmogSprite + DriftHusk; unchanged when not hit; smoke CI passes.

### T-102 — Controller (gamepad) input support  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes-with-review  *(input is hard to verify in CI; manual smoke recommended)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-036
- **GDD ref:** GAME_PLAN.md (broader platform support; pre-Steam-controller compatibility)
- **Files:** `input/InputManager.kt`, `input/GamepadMapping.kt` (new), `persist/Settings.kt` (add `gamepadEnabled: Boolean = true`)
- **Goal:** Add gamepad via libGDX's `Controllers` API. Map: left stick / D-pad → move; A / cross → jump; X / square → action; Y / triangle → swap; Start → pause. Reads from `Settings.gamepadEnabled` (default true; opt-out toggle in Accessibility). Both keyboard and gamepad work simultaneously. Detect plug/unplug at runtime.
- **Done when:** Verified locally with at least one gamepad (Xbox or DualShock 4); keyboard parallel works; smoke CI passes (no controller plugged = no-op).
- **Constraints:** `gdx-controllers` extension required — verify it's already on the classpath before adding any new dep. If a new dep is needed, stop and post to QUESTIONS.md.

### T-105 — Master volume slider in Settings (above per-bus)  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-035, T-094, T-095
- **GDD ref:** GAME_PLAN.md (audio UX — single mute/volume target most users reach for first)
- **Files:** `persist/Settings.kt`, `screens/SettingsScreen.kt`, `audio/MusicManager.kt`, `audio/SoundManager.kt`
- **Goal:** Add `volMaster: Float = 1.0f` to `Settings`. In the Audio section, render this as the FIRST slider (above the existing Music/SFX/UI sliders). Effective volume becomes `volMaster * volBus` — both managers multiply through. Add a small "mute" toggle next to the master slider (clamp to 0).
- **Done when:** Master slider visible above per-bus; dragging it scales all three buses; mute toggle works; persists; smoke CI passes.

<!-- ═══════════════════════════════════════════════════════════════
     SPRINT D — "Alpha launch readiness"
     T-109..T-120 batch (planned 2026-05-12 by claude-code-opus).
     Three tickets clear HANDOFF.md "Source-side quirks" (T-109/T-110/T-111).
     The remainder address alpha-launch gaps: pause-on-focus-loss, save
     migration scaffolding, itch.io deploy, crash reporting, juice
     (shake/duck), mute shortcut, slot-delete confirm, i18n audit.
═══════════════════════════════════════════════════════════════ -->

### T-111 — SoundManager unknown-id: log → error  [P3]
- **Status:** Todo
- **Tool:** `copilot-agent`  *(autonomous from GitHub Issue)*
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** HANDOFF.md source-side quirk #1 — `SoundManager` unknown-id uses `Gdx.app.log` not `Gdx.app.error`; IS an error state
- **Files:** `core/src/main/kotlin/com/sohai/platformer/audio/SoundManager.kt`, `core/src/test/kotlin/com/sohai/platformer/audio/SoundManagerTest.kt`
- **Goal:** In the unknown-sound-id branch of `SoundManager.play()` (search for `Gdx.app.log` referring to unknown ids), switch to `Gdx.app.error(...)`. Update the existing test to assert error-level logging instead of info.
- **Done when:** Unknown sound id triggers an error-level log; existing tests pass; smoke CI passes.

### T-118 — Master mute keyboard shortcut (M key)  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-105  *(needs `volMaster` to exist)*
- **GDD ref:** GAME_PLAN.md (alpha streamer/recorder friendliness)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/input/InputManager.kt`, `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/SettingsScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** Add a `keybind("mute")` (default `Input.Keys.M`) in `Settings.defaultKeybinds()` and the Controls section. Pressing M toggles a transient mute — internally clamps `volMaster` effective value to 0 without overwriting the slider position. Toggling off restores. Briefly flash a `[MUTED]` toast (1.5s) on toggle-on.
- **Done when:** M-key mutes/unmutes from any screen; master slider position is preserved; rebindable in Settings; persists across sessions; smoke CI passes.



<!-- ═══════════════════════════════════════════════════════════════
     SPRINT D — "Follow-ups + Alpha legal/discovery"
     T-121..T-125 batch (planned 2026-05-12 by claude-code-opus,
     mid-autonomous-run).
     T-121 derives from T-073's keyboard research finding.
     T-122 derives from T-120's i18n audit hits.
     T-123 is the HTML5/web-demo viability spike (Sprint D discovery).
     T-124 turns T-075's Steam tag research into a concrete itch.io page.
     T-125 is the alpha-blocking asset attribution audit (legal).
═══════════════════════════════════════════════════════════════ -->

### T-121 — Default `swap` keybind: S → Q (T-073 follow-up)  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-118  *(Settings.kt + keybind table contention with mute-key ticket)*
- **GDD ref:** `research/keyboard-layout-conventions.md` (T-073 deliverable) — every Layout-A platformer surveyed (Hades, Dead Cells, Risk of Rain 2, Ori, Terraria, The Messenger) reserves S for downward movement; Q is the strongest "cycle/switch" precedent.
- **Files:** `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt`, `core/src/main/kotlin/com/sohai/platformer/persist/SettingsManager.kt`, `core/src/test/kotlin/com/sohai/platformer/persist/SettingsManagerTest.kt`
- **Goal:** Change `defaultKeybinds()` for `"swap"` from `Input.Keys.S` to `Input.Keys.Q`. In `SettingsManager.load()` add a tiny migration: if a loaded `Settings` has `swap = Input.Keys.S` AND the user has never opened the Controls panel (track a new `keybindsCustomized: Boolean = false` flag), upgrade to `Q`. If the user HAS opened Controls, respect their saved binding even if it's still S. Add `keybindsCustomized` setter to fire on any rebind in `SettingsScreen`.
- **Done when:** Fresh installs default to Q for swap; players who already touched Controls keep their bindings; players who never touched Controls auto-upgrade on next launch; test covers all three cases.
- **Constraints:** Save-data-adjacent. Existing saves without `keybindsCustomized` field treated as `false` (i.e., legacy user gets the upgrade — they wouldn't be filing bug reports about S working). Cite T-073 research in the PR description.

### T-122 — Wire 3 high-confidence i18n strings to StringKey (T-120 follow-up)  [P3]
- **Status:** Todo
- **Tool:** `copilot-agent`  *(Copilot dogfood — single-purpose, file-isolated)*
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-098, T-101  *(LevelRenderer + Strings contention)*
- **GDD ref:** `research/i18n-coverage.md` (T-120 deliverable) — 3 high-confidence hardcoded English-phrase hits
- **Files:** `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/VictoryScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/LevelRenderer.kt`
- **Goal:** Add 3 new `StringKey` entries and replace the literals at:
  - `VictoryScreen.kt:61` — `"−%.2fs under best"` → `StringKey.VICTORY_DELTA_UNDER` (format arg: seconds)
  - `VictoryScreen.kt:62` — `"+%.2fs slower"` → `StringKey.VICTORY_DELTA_OVER`
  - `LevelRenderer.kt:500` — `"[Locked]"` → `StringKey.HUB_PORTAL_LOCKED`
  Use `Strings.format(key, ...)` consistently with the existing 130+ keys pattern.
- **Done when:** The 3 literals are gone from source; smoke CI passes; existing tests pass.
- **Constraints:** Only these 3 strings. Do NOT broaden scope to the 7 numeric format templates from the audit — those are deferred until a second locale lands.


<!-- ═══════════════════════════════════════════════════════════════
     ALPHA BLOCKERS — surfaced mid-autonomous-run by completed audits.
     T-126 is HUMAN-REQUIRED (Calibri license violation). T-127 is
     derived from T-123's HTML5 spike finding (dead deps).
═══════════════════════════════════════════════════════════════ -->

### T-126 — Replace Calibri Regular font (ALPHA-BLOCKING LEGAL)  [P1]
- **Status:** Todo
- **Tool:** `human-then-claude-code-sonnet`  *(user picks the replacement font and confirms visual regression is acceptable; sub-agent wires it in)*
- **Tier:** M
- **Autonomous-eligible:** **NO**  *(font swap affects every UI screen; visual eyeballing required; smoke CI does NOT validate font readability)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_  *(but **blocks alpha launch**)*
- **GDD ref:** `research/asset-attribution-audit.md` (T-125 deliverable) §HIGH-1; `QUESTIONS.md` T-125-Q1
- **Files:** `assets/fonts/main.ttf` (replace), `core/src/main/kotlin/com/sohai/platformer/FontManager.kt` (path + sizing tweaks if metrics differ), `NOTICE.md` (add attribution for new font)
- **Goal:** `assets/fonts/main.ttf` is currently **Microsoft Calibri Regular** — proprietary, redistribution-forbidden, and the repo being public counts as redistribution. Replace with a SIL OFL 1.1-licensed font (T-125 recommends **Inter**, alt: Atkinson Hyperlegible for accessibility). Verify rendering legibility across MainMenu, SettingsScreen, AchievementsScreen, CreditsScreen, StatsScreen, VictoryScreen, GameScreen HUD, and CloudAtlasScreen. Update `NOTICE.md` with the new font's SIL OFL attribution.
- **Done when:** No Microsoft-proprietary font in repo; all text renders legibly at all `FontManager` sizes (currently used: 11, 14, 22 — verify others); NOTICE.md attribution complete; smoke CI passes; user visually confirms across the 8+ screens.
- **Constraints:** Save-data-adjacent? No (no font baked into save files). UI-visual? Yes — VISUAL REGRESSION RISK IS HIGH. Do not flip-flop fonts late in alpha; pick once and stick. After this lands, append a LEARNINGS.md entry: "always vet bundled fonts against their TTF name-table license string before shipping."

### T-127 — Remove dead gradle deps (ashley, gdx-ai) — T-123 follow-up  [P3]
- **Status:** Todo
- **Tool:** `copilot-agent`  *(single gradle file, single-purpose; or `claude-code-sonnet` if Copilot busy)*
- **Tier:** S
- **Autonomous-eligible:** yes-with-verification  *(grep twice, remove, run full test suite)*
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** `research/html5-web-demo-viability.md` (T-123 deliverable) — flagged `ashley` (Entity-Component-System) and `gdx-ai` (AI utility lib) as declared in `core/build.gradle` but never imported in `core/src/main/kotlin/`
- **Files:** `core/build.gradle` (or `core/build.gradle.kts`)
- **Goal:** Confirm via grep (TWICE — `Grep "ashley" core/src/main/kotlin/` and `Grep "com.badlogic.gdx.ai" core/src/main/kotlin/` separately) that neither package is imported. If confirmed zero hits, remove the `ashley` and `gdx-ai` dependency lines from `core/build.gradle`. If ANY hit found (even in comments), abort and surface to QUESTIONS.md. Run `./gradlew :core:test` after removal — full suite must pass green.
- **Done when:** Deps removed; `./gradlew :core:build` clean; smoke CI passes; PR description cites T-123's finding.
- **Constraints:** Verify twice before removing. If Copilot picks this up, the grep step must be the first commit on the branch — don't bundle with the removal.


<!-- ═══════════════════════════════════════════════════════════════
     SPRINT D wave 5 — alpha polish + code health + a11y
     T-128..T-135 batch (planned 2026-05-13 by claude-code-opus,
     mid-autonomous-run).
     T-128 is the re-spec of the predicates-refactor spawn-task chip
     whose initial agent died silently.
═══════════════════════════════════════════════════════════════ -->


### T-130 — Death recap overlay  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-097, T-128  *(LevelRunState contention with predicates refactor)*
- **GDD ref:** GAME_PLAN.md (player engagement — "what just happened" feedback after death)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/DeathRecapOverlay.kt` (new), `core/src/main/kotlin/com/sohai/platformer/screens/LevelRunState.kt`, `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** When the player dies (after T-097 death animation completes), show a small overlay with: cause-of-death (enemy contact / lethal hazard / fall / boss attack), time-into-level, stomps-this-run, tokens-this-run, "Retry?" button + "Quit to menu" button. Auto-fade after 3s or on Retry/Quit input.
- **Done when:** Death triggers overlay; stats are accurate per-run; Retry restarts level; Quit returns to MainMenu; smoke CI passes (autopilot dies sometimes — verify it doesn't lock up on the overlay).
- **Constraints:** Read cause-of-death from existing LevelRunState death-cause field if one exists; otherwise add a `DeathCause` enum. Don't broaden scope — no leaderboards, no telemetry.


### T-133 — Quick-restart hotkey (R) in-game  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-036  *(adds new keybind to existing system)*
- **GDD ref:** GAME_PLAN.md (player flow — speedrunners + casual retry experience)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt`, `core/src/main/kotlin/com/sohai/platformer/input/InputManager.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`
- **Goal:** Add `keybind("restart")` default `Input.Keys.R`. Holding R for 0.5s (NOT a tap — prevent accidental restarts) triggers level restart. Visual feedback: small radial progress indicator near HUD while held. Releasing before 0.5s cancels. Rebindable in Settings → Controls.
- **Done when:** Hold-R-to-restart works at 0.5s; tap-R is a no-op; rebindable; persists; smoke CI passes (autopilot won't hold R — confirmed safe).
- **Constraints:** Hold-not-tap is deliberate — accidental R presses are common. The 0.5s threshold is the standard for "are you sure?" patterns in indie games.

### T-134 — MainMenu background music  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-030, T-129  *(audio gate must be in place before MainMenu plays music)*
- **GDD ref:** GAME_PLAN.md (atmosphere — silent MainMenu undersells the game)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/MainMenuScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/audio/MusicManager.kt`, `core/src/main/kotlin/com/sohai/platformer/audio/ProceduralMusicGenerator.kt`
- **Goal:** Add a 4th procedural track `ambient_menu` — softer than `ambient_arid`, more harmonic. Generate via existing `ProceduralMusicGenerator` pattern (60s loop). `MainMenuScreen` plays it on enter; transitions to level-specific track on Play. Crossfade pattern from T-030 applies.
- **Done when:** MainMenu has background music; volume responds to Music slider; ducks correctly on Settings open (per T-117 pattern if applicable); smoke CI passes.
- **Constraints:** Procedural only (no new audio file assets). Keep under 30 lines added to the generator.



<!-- ═══════════════════════════════════════════════════════════════
     SPRINT D wave 6 — defensive saves + onboarding + polish
     T-136..T-141 batch (planned 2026-05-13 by claude-code-opus,
     mid-autonomous-run).
═══════════════════════════════════════════════════════════════ -->

### T-136 — Atomic save writes (write-to-temp-then-rename)  [P2]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-113  *(builds on save format scaffold)*
- **GDD ref:** GAME_PLAN.md (alpha durability — a crash mid-save must not corrupt the player's saved file)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/persist/SaveManager.kt`, `core/src/test/kotlin/com/sohai/platformer/persist/SaveManagerTest.kt`
- **Goal:** Rewrite the save-write path to use atomic semantics: write JSON to `<slot>.tmp`, fsync, then atomic-rename to `<slot>`. If any step fails, the original `<slot>` file remains untouched and loadable. Cross-platform: use `java.nio.file.Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)` with a fallback for filesystems that don't support `ATOMIC_MOVE`.
- **Done when:** Mid-write crash (simulated by deliberately throwing between temp write and rename) leaves original save intact; test covers the crash-mid-write path; smoke CI passes.
- **Constraints:** **Save-data-adjacent.** Existing saves must still load. Don't change the schema. Don't add a new field.

### T-137 — First-run tutorial overlay on Sky Sanctuary entry  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-033, T-091
- **GDD ref:** GAME_PLAN.md (onboarding — new players don't know about Seed Slam, character swap, or hub portals)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/levels/Level0_0.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/HubTutorialOverlay.kt` (new), `core/src/main/kotlin/com/sohai/platformer/persist/GameState.kt` (add `tutorialSeen: Boolean = false` additive field, migrate via T-113 scaffold), `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** First time a player enters the Sky Sanctuary hub (Level0_0), show a small overlay with 3 hint cards: (1) "Move with A/D, Jump with SPACE", (2) "Swap character with Q to use water-cycle abilities", (3) "Walk into a portal to enter a world". Player dismisses with any key. Sets `tutorialSeen = true`. Never shown again unless save is reset.
- **Done when:** Fresh save shows overlay on first hub entry; subsequent entries do NOT show it; new keys via Strings.kt; persists; smoke CI passes (autopilot dismisses by key — confirm doesn't lock).
- **Constraints:** No new assets. Use the existing pause-overlay-style modal pattern. Respect reducedMotion (no animations).

### T-138 — SFX on achievement unlock (audio feedback)  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-037, T-128  *(achievement unlock pipeline + post-refactor toast trigger site)*
- **GDD ref:** GAME_PLAN.md (player engagement — currently achievement toasts are silent)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/AchievementToast.kt`, `core/src/main/kotlin/com/sohai/platformer/audio/SoundManager.kt`, `core/src/main/kotlin/com/sohai/platformer/audio/ProceduralSoundGenerator.kt` (or equivalent)
- **Goal:** Generate a procedural "achievement unlock" SFX (short 0.3s C-major arpeggio chime via existing procedural-audio pattern). Play once when `AchievementToast.show(id)` fires. Volume scales with `volSfx` (or new `volUi` if that bus exists). Respect `enabled` flag in SoundManager.
- **Done when:** Achievement unlock plays the chime; volume responds to sfx slider; tests cover playback gating; smoke CI passes.
- **Constraints:** Procedural only — no new audio asset files. Don't double-fire on multi-achievement unlocks in same frame (debounce 200ms).

### T-139 — Screenshot-on-victory (PNG to user dir)  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (player sharing — Twitch/Discord screenshots boost organic discovery)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/VictoryScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/util/ScreenshotWriter.kt` (new), `core/src/test/kotlin/com/sohai/platformer/util/ScreenshotWriterTest.kt` (new)
- **Goal:** On VictoryScreen entry, capture the current framebuffer to PNG at `<userHome>/.cloudy-ninja/screenshots/victory-{levelId}-{yyyyMMdd-HHmmss}.png`. Small toast: "Screenshot saved to ~/.cloudy-ninja/screenshots/". Skip in SMOKE_MODE. Tolerate file-write failure (log error, no crash).
- **Done when:** Beating any level produces a PNG in the documented dir; toast visible; smoke CI does not write screenshots; smoke CI passes.
- **Constraints:** Don't read user's filesystem outside the documented dir. Don't add a setting toggle yet (default-on; can ticket later). Use `Pixmap` + `PixmapIO.writePNG()` — already in libGDX.

### T-140 — Per-character ability tooltip in pause overlay  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** T-063, T-128  *(pause overlay + achievement predicates refactor — GameScreen contention)*
- **GDD ref:** GAME_PLAN.md (player onboarding — currently characters swap freely but pause overlay doesn't say what they do)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt` (pause overlay render path), `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** In the pause overlay, below the existing resume/quit buttons, show a 3-row card listing: `Ebo — Seed Slam (action key)`, `Laya — Wind Dash (action key)`, `Zephyr — Cloud Float (action key)`. Highlight the currently-selected character with the existing toast accent color. Pull binding labels from `Settings.keybinds["action"]` (T-036 keybind system).
- **Done when:** Pause overlay shows the 3-character ability summary; current character highlighted; keys reflect current bindings; smoke CI passes.
- **Constraints:** Pause-overlay only. Don't add a new screen. New StringKey entries via `Strings.kt`.

### T-141 — Cloud Atlas search/filter  [P3]
- **Status:** Todo
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** _unclaimed_
- **Branch:** _none_
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (atlas accessibility — with 6→12 entries planned the list needs a filter)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/CloudAtlasScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** Add a small `VisTextField` at the top of the CloudAtlas screen. Filtering is substring-match against entry title (case-insensitive) + entry summary text. Clear button (✕) resets filter. If 0 results: show `No entries match` message. Filter is transient (doesn't persist).
- **Done when:** Typing in the field narrows the visible entries; clear button works; smoke CI passes (autopilot doesn't enter the atlas — verify).
- **Constraints:** Atlas screen only. Don't change the registry. Don't add fuzzy matching.



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

### T-132 — High-contrast mode toggle (a11y)  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-132-high-contrast-mode
- **Started:** 2026-05-13
- **Depends on:** T-057  *(builds on existing color-blind palette infrastructure)*
- **GDD ref:** GAME_PLAN.md (a11y completeness — beyond color-blind palette)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/SettingsScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/rendering/HighContrastPalette.kt` (new), `core/src/main/kotlin/com/sohai/platformer/screens/LevelRenderer.kt`, `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** Add `highContrast: Boolean = false` to `Settings`. Toggle in Accessibility section. When on, all gameplay colors flip to maximum-contrast variants (player = pure white, enemies = pure black, platforms = inverted grey, hazards = saturated red). Separate from color-blind palette (player can have both on). Renders via a thin `HighContrastPalette` wrapper that intercepts color lookups in `LevelRenderer`.
- **Done when:** Toggle visible in Settings → Accessibility; ON: all rendered colors map through high-contrast palette; OFF: rendering identical to pre-T-132; persists; smoke CI passes.
- **Constraints:** Don't touch UI screens (MainMenu/Settings rendering) — only gameplay rendering. Don't replace the existing T-057 color-blind palette path; coexist.

### T-135 — Per-level eco-token completion % in StatsScreen  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-135-stats-token-completion
- **Started:** 2026-05-13
- **Depends on:** T-107  *(uses hidden eco-token state)*
- **GDD ref:** GAME_PLAN.md (completionist engagement — visible progress per level)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/StatsScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** Add a per-level row to StatsScreen showing eco-token collection % (regular + hidden combined). Format: `Level 1: 8/10 tokens (80%)`. Show all 3 campaign levels. Hidden token discovery rate also appears as a small bonus row: `Hidden: 2/3 found`.
- **Done when:** StatsScreen displays per-level completion; numbers reflect actual save data; smoke CI passes.
- **Constraints:** StatsScreen-only. Don't add any new save fields — read from existing T-107 + per-level token state.

### T-131 — README badges (build status + license + Kotlin version)  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`  *(or copilot-agent — single-file README edit)*
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-131-readme-badges
- **Started:** 2026-05-13
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (community readiness — alpha launch presentation polish)
- **Files:** `README.md`
- **Goal:** Add badges to top of README: (1) CI build status — `github.com/SohailShahM/Cloudy-Ninja/actions/workflows/ci.yml/badge.svg`, (2) AI smoke status — same path with `ai-smoke.yml`, (3) license — proprietary badge linked to `LICENSE`, (4) Kotlin version — derived from gradle, (5) libGDX version. Use shields.io for static text badges; native GH badges for workflow status. Group above first heading.
- **Done when:** Badges render; all link targets correct; no broken images; CI passes.
- **Constraints:** README-only. Don't restructure existing sections; just add the badge block at the very top.

### T-129 — Audio gate on first user input  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-129-audio-gate-first-input
- **Started:** 2026-05-13
- **Depends on:** T-104  *(splash now precedes MainMenu)*
- **GDD ref:** GAME_PLAN.md — desktop usually doesn't need this, but **future HTML5 web demo (T-123 Option 2)** will hard-require a user-gesture before audio can play. Pre-bake the gate now so the web port doesn't need a runtime fork.
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/SplashScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/Main.kt`, `core/src/main/kotlin/com/sohai/platformer/audio/MusicManager.kt`
- **Goal:** Splash screen waits for first keypress OR mouse-click before transitioning to MainMenu (in addition to the existing 1s minimum + preload gate). When the gate fires, MusicManager.play() is allowed to start. Smoke mode bypasses this gate (already configured in T-104).
- **Done when:** On desktop, splash shows a small "Press any key to continue" hint after the 1s minimum + preload complete. Key/click advances to MainMenu. Smoke CI passes unchanged.
- **Constraints:** Don't break smoke. Don't add network calls. Hint text via Strings.kt new key `SPLASH_PRESS_ANY_KEY`.

### T-128 — Refactor achievement unlock predicates to pure functions  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`  *(re-spec from a 2026-05-13 spawn-task chip whose dispatched agent died silently)*
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-128-predicates-refactor
- **Started:** 2026-05-13
- **Depends on:** T-107  *(AchievementRegistry + LevelRunState contention — wait for T-107 to land first)*
- **GDD ref:** HANDOFF.md source-side quirk #4 — predicate-firing tests need a source refactor
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/LevelRunState.kt` (12+ inline `tryUnlock` sites), `core/src/main/kotlin/com/sohai/platformer/screens/LevelTransitionController.kt` (3 sites + duplicate `tryUnlock` helper), `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt` (1 site at L305 for `boss_defeated`), `core/src/main/kotlin/com/sohai/platformer/progression/AchievementRegistry.kt` (or new `AchievementPredicates.kt`), new test file
- **Goal:** Extract each achievement's unlock condition into a pure function `(AchievementInputs) -> Boolean`. `AchievementInputs` data class carries `totalStomps`, `atlasSize`, `completedLevels`, `levelTimer`, `levelId`, `noDeathRun`, `unlockedAchievements: Set<String>`, `collectedHiddenTokens: Set<String>` (from T-107), etc. Add `evaluate(inputs, currentlyUnlocked): List<String>` orchestrator returning newly-firing achievement IDs. Rewrite call sites to build inputs and loop; keep `tryUnlock` itself (it's the impure toast+save side). Consolidate the two duplicate `tryUnlock` helpers.
- **Done when:** Each predicate is testable headless without `Gdx.*`; behavior identical (no achievement fires earlier or later than before); test file covers each predicate with met/unmet/already-unlocked cases.
- **Constraints:** **BEHAVIOR MUST STAY IDENTICAL.** Don't fix bugs as part of the refactor — surface them in LEARNINGS.md and QUESTIONS.md instead. Don't refactor T-107's `collector` predicate beyond what fits this pattern.

### T-106 — Extract `LevelEntityFactory` from GameScreen  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-106-level-entity-factory
- **Started:** 2026-05-13
- **Depends on:** _none_
- **GDD ref:** Code health — `GameScreen.kt` grew by 30+ lines per new entity type (T-029 SmogSprite, T-034 StormSentinel, T-062 DriftHusk all added parallel instantiation blocks)
- **Files:** `screens/GameScreen.kt`, `levels/LevelEntityFactory.kt` (new)
- **Goal:** Extract the entity-instantiation logic out of `GameScreen.init` (currently lines ~194–250 with three parallel `if (level is TmxLevel) { for (def in level.getXxx()) { … } }` blocks for enemies + boss + drift husks) into a new `LevelEntityFactory.spawn(level, world): SpawnedEntities` data class. `GameScreen` becomes a one-liner: `val spawned = LevelEntityFactory.spawn(level, world)`. Future enemy types (T-046's sprite work, future ticket additions) plug in via the factory, not via more parallel blocks in `GameScreen`.
- **Done when:** `GameScreen.init` is shorter; entity behavior unchanged; smoke CI passes; future entity additions need no GameScreen edit.

### T-112 — Auto-pause on window focus loss  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-112-auto-pause-focus
- **Started:** 2026-05-13
- **Depends on:** T-104  *(both touch `Main.kt` — sequential)*
- **GDD ref:** GAME_PLAN.md (alpha polish — players will alt-tab during testing)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/Main.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`
- **Goal:** Implement libGDX's `ApplicationListener.pause()` in `Main` (or the active `Game` subclass) to forward to the active screen if it's a `GameScreen`. `GameScreen.pause()` activates the existing pause overlay (T-063). On `resume()`, the overlay stays up — player must explicitly resume. Respect `SMOKE_MODE` — skip auto-pause in smoke.
- **Done when:** Alt-tab while in-game triggers the pause overlay; resume keeps overlay up until input; smoke CI passes (no auto-pause in smoke mode).

### T-107 — Hidden eco-token in each campaign level + "Collector" achievement  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-107-hidden-eco-tokens
- **Started:** 2026-05-13
- **Depends on:** T-019, T-037
- **GDD ref:** GAME_PLAN.md (collectibles + replay-value loop)
- **Files:** `levels/TmxLevelDefinition.kt` (add 1 hidden eco-token per level1/2/3 in an off-path location), `progression/AchievementRegistry.kt` (add `collector` achievement: "Find all 3 hidden eco-tokens"), `screens/LevelRunState.kt` (track hidden-token collection separately + emit unlock)
- **Goal:** Add 1 visually-distinct "hidden" eco-token to each of level1/2/3, placed in an out-of-the-way spot (e.g. behind a wall jump, in a ceiling alcove). Collecting all 3 across runs unlocks a new `collector` achievement. Hidden tokens render with a slight golden tint to distinguish from regular ones.
- **Done when:** Each campaign level has 1 hidden token; collecting all 3 unlocks `collector`; smoke CI passes (autopilot is unlikely to find them — that's fine, they're hidden).

### T-117 — Audio ducking on pause overlay  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-117-audio-duck-pause
- **Started:** 2026-05-13
- **Depends on:** T-104  *(MusicManager contention)*
- **GDD ref:** GAME_PLAN.md (alpha audio polish)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/audio/MusicManager.kt`, `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`
- **Goal:** Add `MusicManager.duck(amount: Float = 0.3f, fadeMs: Int = 250)` and `MusicManager.unduck(fadeMs: Int = 250)`. While ducked, effective volume is `volMusic * amount`. `GameScreen` calls `duck()` on pause-overlay open, `unduck()` on close. Stacks idempotently — multiple `duck()` calls collapse, one `unduck()` restores.
- **Done when:** Pause open ↔ music dips audibly; close ↔ music restores; rapid open/close doesn't desync the fade; existing `MusicManagerTest` passes; new test covers duck/unduck math; smoke CI passes.

### T-119 — Save slot delete confirmation modal  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-119-slot-delete-confirm
- **Started:** 2026-05-13
- **Depends on:** T-099, T-100  *(MainMenuScreen contention)*
- **GDD ref:** GAME_PLAN.md (alpha safety — accidental slot deletion is a top-of-funnel rage bug)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/MainMenuScreen.kt`, `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt`
- **Goal:** Tapping Delete on a save slot now opens a modal: `Delete slot {N}? This cannot be undone.` with `Cancel` / `Delete`. Default-focus is `Cancel`. `Esc` cancels. Only `Delete` fires `SaveManager.deleteSlot()`. Empty slots hide (or disable) the delete affordance to match existing slot-card style.
- **Done when:** No path deletes a slot without the confirm modal; Cancel preserves the slot; smoke CI passes (verify autopilot doesn't open the modal — that path stays unaffected).

### T-115 — In-game crash report dumper  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-115-crash-reporter
- **Started:** 2026-05-13
- **Depends on:** T-104  *(both touch `Main.kt` — sequential)*
- **GDD ref:** GAME_PLAN.md (alpha bug-reporting — players need something to attach)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/Main.kt`, `core/src/main/kotlin/com/sohai/platformer/persist/CrashReporter.kt` (new), `core/src/test/kotlin/com/sohai/platformer/persist/CrashReporterTest.kt` (new)
- **Goal:** Wire `Thread.setDefaultUncaughtExceptionHandler` in `Main.create()` to write a crash file at `<userHome>/.cloudy-ninja/crashes/crash-{yyyyMMdd-HHmmss}.log` containing: timestamp, OS + JDK + game version (`BUILD_VERSION` from T-100), full stack trace, save-slot metadata (slot indices + completed-level counts, **no PII**). After writing, re-throw or exit per libGDX convention. `CrashReporter` is a pure object; `Main` calls into it. Respect `SMOKE_MODE` — no-op in smoke.
- **Done when:** A deliberately-thrown exception in dev produces a crash file in the documented path; smoke CI doesn't write crash files; tests cover the pure-function formatter path.
- **Constraints:** Don't read save *contents* into the crash log (PII risk). Only metadata.

### T-110 — ScreenFade semantics: rename or doc  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-110-screenfade-rename
- **Started:** 2026-05-13
- **Depends on:** _none_
- **GDD ref:** HANDOFF.md source-side quirk #3 — `fadeIn` / `fadeOut` semantics are intuitively reversed (`fadeIn` makes screen clear)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/rendering/ScreenFade.kt` + all callers (find via `Grep "fadeIn|fadeOut" --type kt`)
- **Goal:** Pick ONE and apply consistently: **(A)** rename `fadeIn` → `fadeFromBlack` and `fadeOut` → `fadeToBlack`, update all callers + tests; **(B)** add a KDoc paragraph above each function explaining the reversed-from-intuition semantics. Default to (A) unless caller count exceeds 20.
- **Done when:** No caller is left ambiguous; existing `ScreenFadeTest` passes (renamed if (A) chosen); smoke CI passes.

### T-116 — Screen shake on stomp + boss hit  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** `claude/T-116-screen-shake`
- **Started:** 2026-05-13
- **Depends on:** T-098  *(both touch `LevelRenderer.kt` — sequential)*
- **GDD ref:** GAME_PLAN.md (combat juice — T-098 hit-flash pairs with shake for full hit-feedback)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/screens/LevelRenderer.kt`, `core/src/main/kotlin/com/sohai/platformer/physics/WorldContactListener.kt`, `core/src/main/kotlin/com/sohai/platformer/rendering/ScreenShake.kt` (new), `core/src/test/kotlin/com/sohai/platformer/rendering/ScreenShakeTest.kt` (new)
- **Goal:** Add `ScreenShake` utility (decaying amplitude over time, `update(delta)` + `offset(): Vector2`). On stomp-defeat in `WorldContactListener` and Storm Sentinel hit-confirm, call `ScreenShake.trigger(amplitude=4f, duration=0.15f)`. `LevelRenderer` applies the offset to the camera before rendering each frame. **Respect `Settings.reducedMotion`** — when on, `trigger()` is a no-op.
- **Done when:** Visible shake on stomp + boss hit; no shake when `reducedMotion` is on; shake never exceeds duration; pure-function tests on the decay curve pass; smoke CI passes.

### T-100 — Game version + build info on MainMenu (bottom corner)  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-100-version-label
- **Started:** 2026-05-13
- **Depends on:** T-091
- **GDD ref:** GAME_PLAN.md (release readiness — let players report exact version)
- **Files:** `screens/MainMenuScreen.kt`, `Constants.kt` (add `BUILD_VERSION` + `BUILD_DATE`), `i18n/Strings.kt`
- **Goal:** Add `BUILD_VERSION` and `BUILD_DATE` constants in `Constants.kt` (manually maintained for alpha). MainMenu shows a tiny right-bottom label: `v{0} · {1}` (e.g. `v0.1.0 · 2026-05-12`). Style: `FontManager.getShared(11)`, dim grey `(0.5f, 0.5f, 0.5f, 0.6f)`, 8px from corner. Add `StringKey.MENU_BUILD_INFO`.
- **Done when:** Label visible on MainMenu; reads from constants; smoke CI passes.

### T-099 — Achievement progress counter on MainMenu  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-099-menu-achievement-progress
- **Started:** 2026-05-13
- **Depends on:** T-037, T-091
- **GDD ref:** GAME_PLAN.md (player engagement signals)
- **Files:** `screens/MainMenuScreen.kt`, `i18n/Strings.kt`
- **Goal:** Below the slot cards on MainMenu, render `Achievements: {0}/12 unlocked` showing the **max** count across the 3 save slots. Style: `FontManager.getShared(14)`, light grey, 12px padded. Use `Strings.format(StringKey.MENU_ACHIEVEMENT_PROGRESS, count, total)` — new key. If all 12 unlocked: `MENU_ACHIEVEMENT_PROGRESS_COMPLETE` rendered in gold `(1f, 0.85f, 0.1f, 1f)`.
- **Done when:** Counter visible reflecting save data; both states verified; smoke CI passes.

### T-098 — Enemy hit-flash on takeDamage (200ms white tint)  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent *(re-dispatched 2026-05-13 — prior agent died silently)*
- **Branch:** `claude/T-098-enemy-hit-flash`
- **Started:** 2026-05-12
- **Depends on:** T-029, T-062
- **GDD ref:** GAME_PLAN.md (combat juice)
- **Files:** `entities/Enemy.kt` (abstract base — add `hitFlashTimer`), `entities/SmogSprite.kt` + `entities/DriftHusk.kt` (set timer in takeDamage), `screens/LevelRenderer.kt` (lerp color toward white when timer > 0)
- **Goal:** Add `hitFlashTimer: Float = 0f` to `Enemy`. `takeDamage()` sets it to 0.2f. `update(delta)` decrements. `LevelRenderer` reads `enemy.hitFlashTimer` and lerps base color toward white `(1, 1, 1, 1)` by `clamp(timer / 0.2f)`. Don't touch defeat path; just hit-feedback frames.
- **Done when:** Visible hit-flash on Seed-Slamming SmogSprite + DriftHusk; unchanged when not hit; smoke CI passes.

### T-123 — HTML5 / web-demo viability spike  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent`  *(research-only, but technical — needs codebase-aware judgement)*
- **Tier:** S
- **Autonomous-eligible:** yes-with-review  *(go/no-go memo informs whether to invest in a real port)*
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-123-html5-spike
- **Started:** 2026-05-13
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (alpha discovery — itch.io HTML5 embed dramatically increases play-through rate vs. download)
- **Files:** `research/html5-web-demo-viability.md` (new)
- **Goal:** Investigate whether Cloudy-Ninja can be ported to an HTML5 / GWT (libGDX-Teavm) target. Assess: (a) does our current libGDX version support GWT/Teavm backends? (b) which deps are GWT-hostile (Box2D-native, Kotlin stdlib reflection, GSON/Jackson)? (c) what's the rough effort estimate (S/M/L/XL)? (d) which game systems would need refactor or graceful-degrade (save serialization, audio formats, font baking)? Produce a go/no-go memo with options: (1) ship desktop-only alpha + web demo deferred; (2) cut a stripped web demo (1-2 levels, no save); (3) full web port.
- **Done when:** Memo exists with the 4 questions answered, a clear recommendation (one of the 3 options), and a rough effort estimate.
- **Constraints:** **Research-only.** Do NOT add a Teavm/GWT module, do NOT touch gradle build files, do NOT modify any deps. Read-only investigation. Use `WebSearch` for libGDX-Teavm / libGDX-html status circa 2025.

### T-124 — itch.io page draft + Tag Wizard order (T-075 follow-up)  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent`  *(re-routed 2026-05-13 from claude-code-sonnet — parent dispatched as sub-agent)*
- **Tier:** S
- **Autonomous-eligible:** yes-with-review  *(marketing copy benefits from user voice — surface the draft for editing)*
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-124-itch-page-draft
- **Started:** 2026-05-13
- **Depends on:** T-075, T-077  *(uses Steam-tag research + presskit scaffold)*
- **GDD ref:** `marketing/steam-tags-research.md` + `marketing/presskit/` — Sprint D launch needs an itch.io page
- **Files:** `marketing/itch-page-draft.md` (new)
- **Goal:** Draft the full itch.io page content: short description (160 chars), long description (~500 words, plain markdown, no autolinks), feature list (5-8 bullets), system requirements, controls reference (refer to T-073 default mapping), genre + tag list (primary tags first per T-075 — `Pixel Graphics`, `Platformer`, `2D`, `Nature`, then stretch). Also draft the Tag Wizard order (Steam-style — itch uses a similar discovery mechanism). Embed placeholders for screenshots + a future trailer.
- **Done when:** Markdown draft exists; tag order matches T-075's primary→stretch recommendation; no accidental Steam-specific terminology bleed-through; ready for the user to copy-paste into itch.io's CMS.
- **Constraints:** Marketing copy — do not invent feature claims. Every bullet must reflect what's actually shipped (cross-check with TASKS.md `## Done`). No promised features in "coming soon" section.

### T-125 — Asset attribution audit (alpha-blocking legal)  [P2]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent`  *(re-routed 2026-05-13 from claude-code-sonnet — dispatched as sub-agent for autonomous run)*
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-125-asset-attribution-audit
- **Started:** 2026-05-13
- **Depends on:** _none_
- **GDD ref:** `LICENSE` + `NOTICE.md` — alpha launch must not ship with an under-attributed or mis-licensed asset
- **Files:** `research/asset-attribution-audit.md` (new), append entries to `QUESTIONS.md` if any gaps found
- **Goal:** Walk every file under `assets/` (and `core/src/main/resources/` if any). For each: identify the source, license (CC0, CC-BY, custom), author/attribution string, and compare to what `NOTICE.md` actually declares. Flag (a) assets used but not credited, (b) assets credited but not used (stale entries), (c) any license that requires more than NOTICE.md provides (e.g. CC-BY needs visible in-game credit, not just NOTICE). Cross-reference Kenney CC0 declarations + any audio files generated by `ProceduralMusicGenerator` (those are own-IP).
- **Done when:** Audit report exists; mismatches are documented; if any (a) or (c) gaps exist, a high-priority `QUESTIONS.md` entry is filed BEFORE the alpha can ship.
- **Constraints:** **Research-only.** Do NOT modify `NOTICE.md` or `LICENSE` in this PR — surface gaps for human resolution. Do NOT modify any asset files.

### T-073 — User research: pixel-platformer default keyboard layouts  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent` *(re-routed 2026-05-12 from antigravity — autonomous-run velocity)*
- **Tier:** S  *(research-only, no code)*
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-073-keyboard-layout-research
- **Started:** 2026-05-12
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (default input scheme)
- **Files:** `research/keyboard-layout-conventions.md` (new)
- **Goal:** Catalog default keyboard bindings across 10–15 popular indie pixel platformers (Celeste, Hollow Knight, Hyper Light Drifter, Hades, Dead Cells, Risk of Rain, Stardew Valley, Owlboy, Shovel Knight, etc.). Capture for each: jump key, action key, dash key, alt-action key, pause key, inventory key, accessibility-mode key (if any). Synthesize a most-common default + a recommended Cloudy-Ninja default that maximizes "feels familiar to platformer players."
- **Done when:** `research/keyboard-layout-conventions.md` exists with a comparison table + a recommended default mapping for Cloudy Ninja's 5 actions (left/right/jump/action/swap), plus a 2-sentence rationale per binding.

### T-075 — Steam tags + keyword research  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent` *(re-routed 2026-05-12 from antigravity — autonomous-run velocity)*
- **Tier:** S  *(research-only, no code)*
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-075-steam-tags-research
- **Started:** 2026-05-12
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (launch/visibility plan)
- **Files:** `marketing/steam-tags-research.md` (new)
- **Goal:** Cloudy Ninja's eventual Steam listing needs the right tag combination. Survey Steam's top-rated 2D pixel-art platformers with eco/climate/accessibility angles. Cross-reference Steam's official tag taxonomy. Identify: (a) tag combinations correlating with discovery success, (b) tag conflicts that *hurt* visibility, (c) the 3-5 must-have tags for our pitch, (d) 5-8 "stretch" tags that broaden audience without diluting positioning.
- **Done when:** `marketing/steam-tags-research.md` exists with a recommended primary tag set, a stretch tag set, and rationale citing 3+ comparable games per tag.

### T-114 — itch.io deploy workflow (butler)  [P2]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** M
- **Autonomous-eligible:** yes-with-review  *(secret-handling needs sanity check)*
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-114-itch-deploy
- **Started:** 2026-05-12
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (Sprint D — public alpha to itch.io)
- **Files:** `.github/workflows/itch-deploy.yml` (new), `scripts/deploy-itch.sh` (new), `docs/itch-deploy.md` (new)
- **Goal:** Manual-trigger (`workflow_dispatch`) workflow that (1) builds the desktop JAR (`./gradlew desktop:dist`), (2) downloads butler, (3) uploads via `butler push <jar> sohailshahm/cloudy-ninja:<channel> --userversion <tag>`. Reads `ITCH_API_KEY` from repo secrets. Inputs: `channel` (default `desktop`), `version-tag` (optional, defaults to short SHA). `docs/itch-deploy.md` explains itch.io page setup + API key creation.
- **Done when:** Workflow file exists; runs cleanly on dispatch with a valid secret; docs explain setup.
- **Constraints:** **Do NOT add the `ITCH_API_KEY` secret yourself** — note in PR description that the user runs `gh secret set ITCH_API_KEY` before the workflow can fire. Do NOT change signing config. Do NOT publish a build from the PR itself.

### T-109 — FontManager testability seam  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-109-fontmanager-seam
- **Started:** 2026-05-12
- **Depends on:** _none_
- **GDD ref:** HANDOFF.md source-side quirk #2 — `FontManager.create()` is unreachable headlessly; needs a factory seam for end-to-end testability
- **Files:** `core/src/main/kotlin/com/sohai/platformer/FontManager.kt`, `core/src/test/kotlin/com/sohai/platformer/FontManagerTest.kt`
- **Goal:** Extract the font-loading codepath inside `FontManager.create()` into a small delegate (e.g. `interface FontLoader { fun load(handle: FileHandle): BitmapFont }`) with a default `Gdx.files`-backed implementation. Expose a package-private setter so tests can inject a no-op loader without mocking `Gdx.files`. Runtime behavior unchanged.
- **Done when:** Existing `FontManagerTest` no longer needs to mock `Gdx.files` for the loader path; the `create()` codepath becomes reachable in headless tests; no behavioral regression in-game; smoke CI passes.

### T-101 — Credits screen  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-101-credits-screen
- **Started:** 2026-05-12
- **Depends on:** T-031, T-046a
- **GDD ref:** GAME_PLAN.md (legal compliance + community goodwill)
- **Files:** `screens/CreditsScreen.kt` (new), `screens/SettingsScreen.kt` (add a "Credits" button in the footer row), `i18n/Strings.kt` (credit-related keys)
- **Goal:** Scrollable Credits screen reachable from Settings. Sections: **Game** (Sohail Shah, design + code, 2026); **Code assistants** (Claude Code/Anthropic, GitHub Copilot, Antigravity/Gemini/Google, NotebookLM); **Art** (Kenney pixel-platformer, CC0, kenney.nl + entries from `art-research/tileset-candidates.md`); **Audio** (procedural via T-013/T-030 + candidates in `art-research/audio-candidates.md`); **Engine** (libGDX, Box2D, Kotlin, VisUI, Kotest); **Climate sources** (NOAA, NASA Earth Observatory, IPCC etc. — see `research/climate-sources/INDEX.md`). Section header `FontManager.getShared(22)`, body `getShared(14)`. Back button bottom-center.
- **Done when:** Screen reachable, all sections render, no asset URLs hardcoded (in `Strings.kt`), smoke CI passes.

### T-113 — Save format version field + migration scaffold  [P2]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent`
- **Tier:** M
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-113-save-format-version
- **Started:** 2026-05-12
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (alpha launch — once players have saves, schema changes must be migration-safe)
- **Files:** `core/src/main/kotlin/com/sohai/platformer/persist/GameState.kt`, `core/src/main/kotlin/com/sohai/platformer/persist/SaveManager.kt`, `core/src/main/kotlin/com/sohai/platformer/persist/SaveMigrations.kt` (new), `core/src/test/kotlin/com/sohai/platformer/persist/SaveMigrationsTest.kt` (new)
- **Goal:** Add `saveFormatVersion: Int = 1` to `GameState`. In `SaveManager.loadGame()`, route the deserialized JSON through `SaveMigrations.migrate(json): GameState` before returning. `SaveMigrations` is a chain of `(version, JsonValue) -> JsonValue` steps; v1 is an identity no-op. Existing saves without a version field are treated as v1. Persist writes always use the current version.
- **Done when:** Saves carry a version field; loads route through the migration chain; existing saves remain loadable; new test covers the migration scaffold with a fake v0→v1 step; smoke CI passes.
- **Constraints:** Don't change the save schema beyond adding the version field. The scaffold is what matters, not migrations themselves.

### T-120 — Localization coverage audit  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sub-agent` *(re-routed 2026-05-12 from antigravity — autonomous-run velocity)*
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent
- **Branch:** claude/T-120-i18n-coverage-audit
- **Started:** 2026-05-12
- **Depends on:** T-091  *(i18n format API)*
- **GDD ref:** HANDOFF.md ("i18n scaffolding (T-059) + Strings.format API (T-091); 130+ keys") — verify nothing leaked
- **Files:** `research/i18n-coverage.md` (new), entries in `QUESTIONS.md` if hardcoded user-facing strings found
- **Goal:** Scan all `*.kt` files under `core/src/main/kotlin/com/sohai/platformer/screens/`, `entities/`, and other UI-adjacent paths for string literals that look user-facing (`Label("...")`, `TextButton("...")`, `setText("...")`; log messages excluded). For each hit record: file, line, literal, recommended `StringKey` name. Group by category (settings/menu/gameplay/achievements). Produce a punch-list — **do NOT modify code**. If high-confidence cases are found, file ONE summary QUESTIONS.md entry asking whether to wire each as a follow-up Copilot ticket.
- **Done when:** `research/i18n-coverage.md` exists with a table of all candidate strings + recommended keys; QUESTIONS.md gets one summary entry if any are found; no source-code changes in this PR.
- **Constraints:** Research-only. Do NOT add new `StringKey` entries. Do NOT modify Kotlin source files. Do NOT auto-fix.

### T-104 — Splash / asset-preload progress bar  [P3]
- **Status:** In Progress
- **Tool:** `claude-code-sonnet`
- **Tier:** S
- **Autonomous-eligible:** yes
- **Agent:** claude-code-sub-agent *(re-dispatched 2026-05-13 — prior agent died silently)*
- **Branch:** claude/T-104-splash-preload
- **Started:** 2026-05-12
- **Depends on:** _none_
- **GDD ref:** GAME_PLAN.md (cold-start UX)
- **Files:** `screens/SplashScreen.kt` (new), `Main.kt` (start with SplashScreen instead of MainMenu), `audio/MusicManager.kt` (load tracks lazily via AssetManager hook if not already)
- **Goal:** On first launch, show a 1-second minimum splash with a horizontal progress bar tracking asset preload (atlas pack, music tracks via `ProceduralMusicGenerator`, 8 SFX). Transition to `MainMenuScreen` when preload completes AND the 1s minimum has elapsed. The minimum prevents flash-frames on fast machines.
- **Done when:** Splash visible on every cold start; progress bar reflects real preload; transition happens once both gates are met; smoke CI passes (smoke mode should fast-skip the splash via existing `cloudy.smokeMode` system property).

<!--
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

### T-050 — Press/journalist outreach list
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Generated curated outreach list of 30 journalists, creators, and advocates across S, M, and L tiers with tailored hooks.
- **Commit/PR:** Merged PR for T-050
- **Tool:** ntigravity

### T-052 — Indie game festival + showcase eligibility research
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Catalogued 15 indie game festivals with focus on eco/wholesome events; prioritized top 5 including Wholesome Direct and Games for Change.
- **Commit/PR:** PR #29
- **Tool:** ntigravity

### T-053 — Eco-themed games design comparison study
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Generated comparison of 6 eco-themed games (Terra Nil, ABZU, Endling, Alba, Flower, Sable) with 4 actionable mechanical recommendations for Cloudy Ninja.
- **Commit/PR:** PR #28
- **Tool:** ntigravity

### T-051 — Dependency upgrade audit
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Completed upgrade audit for libGDX, Kotlin, VisUI, Kotest, and kotlinx.serialization; identified Vulkan backend additions and font-scaling breaking changes.
- **Commit/PR:** PR #27
- **Tool:** ntigravity

### T-054 — Kotest specs for StormSentinel boss state machine
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `core/src/test/kotlin/com/sohai/platformer/entities/StormSentinelTest.kt` — Kotest BehaviorSpec, 24 tests, 0.317s. Covers all 5 phase transitions (REST → LIGHTNING_TELEGRAPH → LIGHTNING → REST → SWEEP_TELEGRAPH → SWEEP → REST), HP decrement 3→2→1→0 via `takeDamage`, `isDead` flag, `onDefeated` callback firing exactly once, post-death idempotency, `update` no-op when dead, projectile spawn count + trajectory for both attacks, seeded `GameRandom` determinism. Box2D natives bypassed via `ObjenesisStd` + private-field reflection — pattern reusable for future entity tests.
- **Commit/PR:** PR #32 (squashed merge `e13cc09`)
- **Tool:** `claude-code-sub-agent`

### T-055 — Kotest specs for Enemy + SmogSprite + Projectile
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Two new files (`SmogSpriteTest.kt`, `ProjectileTest.kt`) — 34 tests total. SmogSprite (18): patrol reversal at both waypoints, mid-band motion, equal-waypoint degenerate ctor, two-hit Seed-Slam defeat (HP 2→1→0), overkill clamping, post-death idempotency + velocity zeroing, `wasStomped` flag. Projectile (16): `isExpired ≡ age ≥ lifetime ∨ hitWall` contract, per-frame age, lifetime-boundary inclusivity, wall-hit short-circuit, multi-projectile independence, `RADIUS` invariant, sub-frame precision. `EnemyTest` skipped — all abstract-base behavior covered transitively. MockK + Objenesis reflection (same pattern as T-054).
- **Commit/PR:** PR #33 (squashed merge `053e152`)
- **Tool:** `claude-code-sub-agent`

### T-056 — Kotest specs for AchievementRegistry + unlock conditions
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `core/src/test/kotlin/com/sohai/platformer/progression/AchievementTest.kt` — Kotest BehaviorSpec, 23 tests, 0.26s. Covers `AchievementRegistry.get()` (known/unknown/empty/idempotent for 5 ids), `ALL`-list invariants (size 12, non-blank id/title/desc, unique ids + titles, canonical id set, round-trip), threshold-string drift checks (`stomp_10`→"10", `atlas_half`→"6", `atlas_full`→"12" not "11"). **Predicate-firing tests skipped** — unlock logic is inlined in screen code, not pure functions; flagged as follow-up via spawn_task chip.
- **Commit/PR:** PR #30 (squashed merge `9e5a4a9`)
- **Tool:** `claude-code-sub-agent`

### T-057 — Color-blind palette toggle
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 4-mode toggle (`OFF` / `DEUTERANOPIA` / `PROTANOPIA` / `TRITANOPIA`) added to Settings + new SettingsScreen accessibility section. `LevelRenderer` palette split into mode-sensitive `Palette` value class + unchanged `SharedPalette` companion. Color choices grounded in Brettel/Viénot/Mollon (1997) dichromat simulation framework + IBM Design Language guidance — red-green ↔ blue-orange for deuteranopia/protanopia, blue-yellow ↔ magenta-cyan for tritanopia. `OFF` is byte-identical to pre-change render. Save-compat via kotlinx-serialization default-value (same pattern as `tilesetPackId`).
- **Commit/PR:** PR #35 (squashed merge `3a6b146`)
- **Tool:** `claude-code-sub-agent`

### T-060 — Best-times row in StatsScreen
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Per-slot best-times section in `StatsScreen` — header + one `MM:SS.mmm` line per recorded level in canonical level order (via `LevelManager.getAllLevels()`, which covers full sequence including tutorial rooms — `LevelRegistry.ALL` would have missed those). Empty state renders a muted "(no times recorded)". `Label` + `FontManager.getShared` per the T-044 lesson. Sub-agent caught spec drift: `bestTimes` is actually `Map<String, Float>` (seconds, written by `LevelTransitionController`), not `Map<String, Long>` (ms) as the brief claimed — matched the codebase rather than fabricate.
- **Commit/PR:** PR #34 (squashed merge `f6e34a8`)
- **Tool:** `claude-code-sub-agent`

### T-058 — Reduced-motion mode toggle
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `reducedMotion: Boolean = false` added to Settings + checkbox in the existing Accessibility section. Three dampenings: (1) `LevelRunState.triggerShake()` early-returns when flag is set (covers landing-thud + death-burst sites); (2) new private `LevelRenderer.clampBurstCount(default)` invoked at every particle-spawn site (`spawnJumpPuff`, `spawnLandingDust`, `spawnCleanseBurst`, `spawnCollectSparkle`, `spawnTokenSparkle`, `spawnSnapshotSparkle`, `spawnStompSmokeBurst`) — returns 1 when flag set, else default; (3) `ParallaxBackground.render()` forces `effectiveScroll = 1f` per layer, so the background tracks camera 1:1 and appears static. Flag = false is byte-identical to pre-change.
- **Commit/PR:** PR #38 (squashed merge `4493baf`)
- **Tool:** `claude-code-sub-agent`

### T-064 — Victory screen best-time delta indicator
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** New `priorBestTime: Float?` param on `VictoryScreen`. When non-null and delta ≠ 0, renders one line between Trial Time and the NEW BEST banner: `−2.31s under best` (green) or `+0.42s slower` (light grey). Scope expanded by 4 lines in `LevelTransitionController` to capture `prevTime` before SaveManager overwrites it — caught by sub-agent's BLOCKER analysis (original ticket forbade caller-side changes, which made the work impossible). Orchestrator did the small plumbing inline rather than re-dispatch. Default `null` preserves all existing callers.
- **Commit/PR:** PR #36 (squashed merge `246191e`)
- **Tool:** `claude-code-opus` (inline after sub-agent flagged scope blocker)

### T-062 — Second enemy type: Drift Husk (drop-from-above)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** New `entities/DriftHusk.kt` (168 lines) — kinematic-body enemy with state machine `FLOATING → DROPPING → COOLDOWN → FLOATING`. Trigger band 0.6m, 4s respawn, 2-hit Seed-Slam defeat, stomp-from-above sets `wasStomped`. Two placed in Level 2 at (820, 600) and (1620, 600). Renderer adds floating-purple-oval block with trailing wisp. `entities/DriftHuskTest.kt` (245 lines, **25 tests**) covers full state machine via reflection (same pattern as SmogSpriteTest). Sub-agent correctly flagged that `GameScreen.kt` wiring was out of file scope; orchestrator added it inline in a follow-up commit (3-line parallel block + named-arg passthrough). All defaults preserve behavior on non-Level-2 levels.
- **Commit/PR:** PR #40 (squashed merge `c752577`)
- **Tool:** `claude-code-sub-agent` + `claude-code-opus` (follow-up wiring)

### T-069 — Settings screen: categorized layout
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `screens/SettingsScreen.kt` reorganized into 4 sections: Display (resolution, fullscreen, FPS) / Audio (music/SFX/UI sliders) / Controls (5 keybinds + reset) / Accessibility (color-blind mode, reduced motion, screen shake, death flash, assist mode group). Pattern: section headers + `Separator` divider (rejected `VisTabbedPane` since it isn't used elsewhere). Save/Load/Delete moved to footer below sections (no header, per pattern). No widget added/removed/renamed/rewired; `Settings.kt` not touched. Judgment calls: "Show FPS" → Display (output toggle, not comfort); screen shake + death flash → Accessibility (gates same comfort-vs-juice trade-off as Reduce Motion).
- **Commit/PR:** PR #39 (squashed merge `6707ca5`)
- **Tool:** `claude-code-sub-agent`

### T-063 — Pause menu visual polish + fade-in
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `screens/PauseOverlay.kt` + `screens/GameScreen.kt`: 0.2s fade-in driven by manual smoothstep lerp (`t²(3-2t)`, matches `Interpolation.fade` feel) — one `fadeT` variable drives both ShapeRenderer backdrop alpha and `stage.root.color.a` in lockstep. 55% black backdrop dim (`Color(0, 0, 0, 0.55)`) filled rect across viewport. "Press Esc to resume" hint at bottom-right (`FontManager.getShared(14)`, light grey `(0.6, 0.6, 0.6, 0.8)`, 12px padded). Hint reads `SettingsManager.load().keybinds["pause"]` for the display name, falls back to `"Esc"`. **Follow-up flagged:** no `"pause"` default in `Settings.keybinds` today (ESC hardcoded in `GameScreen`); sub-agent left a code comment for a future ticket to add it. No fade-out on un-pause — instant snap-back matches expected responsiveness.
- **Commit/PR:** PR #41 (squashed merge `483fc95`)
- **Tool:** `claude-code-sub-agent`

### T-059 — String extraction scaffolding (i18n prep)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** New `core/src/main/kotlin/com/sohai/platformer/i18n/Strings.kt` (catalog of **104 keys** + `Strings.get(key)` resolver, English defaults only). Sweep across **13 screen files** replacing every static user-facing literal with `Strings.get(StringKey.X)`. Grouped by area: `MAIN_*`, `SETTINGS_*` (subgrouped), `ATLAS_*`, `GAME_OVER_*`, `HUD_*`, `LEVEL_COMPLETE_*`, `LEVEL_SELECT_*`, `PAUSE_*`, `STATS_*`, `VICTORY_*`, `RUN_*`. **Deliberately skipped**: `$`-interpolated literals (would freeze English word order for future locales — needs a `Strings.format(key, *args)` API as a follow-up), all `Gdx.app.log(...)` (developer-facing), printf-style format placeholders. No file outside `screens/` + `i18n/` touched. English byte-identical to pre-change (smoke CI verifies).
- **Commit/PR:** PR #42 (squashed merge `87f5097`)
- **Tool:** `claude-code-sub-agent`
- **Follow-up:** `Strings.format(key, *args)` API for interpolated/compositional strings.

### T-088 — Kotest specs for SaveManager round-trip + back-compat
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 23 tests in `core/src/test/kotlin/com/sohai/platformer/persist/SaveManagerTest.kt`. Strict round-trip equality for every `GameState` field (incl. nested `Checkpoint`, `PlayerStats`, all Sets/Maps, Float times). Back-compat: legacy JSON without `unlockedAchievements`/`totalStomps`/`collectedAtlasIds`/`bestScores`/`bestTimes` deserializes to safe defaults. Forward-compat: unknown future keys ignored. Isolation via MockK on `Gdx.app`/`Gdx.files.local` + per-spec tmpdir under `${java.io.tmpdir}/cloudy_savemgr_<uuid>/` + UUID-suffixed slot filenames; `afterSpec` cleans up. **No source-side test hooks added.**
- **Commit/PR:** PR #44 (squashed merge `8f531e8`)
- **Tool:** `claude-code-sub-agent`

### T-089 — Kotest specs for ParallaxBackground theme + reducedMotion
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 23 then-blocks in `core/src/test/kotlin/com/sohai/platformer/rendering/ParallaxBackgroundTest.kt`. Pairwise theme-palette distinctness (ARID/WIND/ECO), strict-increasing scrollFactor ordering, parallax law `Δ(baseX) = Δcam · (1 − scrollFactor)` verified by driving a real `OrthographicCamera` + MockK `ShapeRenderer`, T-058 reduced-motion invariant (camera-translation-invariant x-coords), star-fade by `cleanseRatio` follows `((0.5−t)·2)·0.9` with hard cutoff at `t ≥ 0.5`. Identifies per-layer geometry by `Color.equals` filter, not positional indexing — robust to viewport culling. Only `ShapeRenderer` mocked; cameras + Settings are real (latter seeded via reflection).
- **Commit/PR:** PR #47 (squashed merge `964273d`)
- **Tool:** `claude-code-sub-agent`

### T-090 — Kotest specs for LevelManager
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 17 tests in `core/src/test/kotlin/com/sohai/platformer/levels/LevelManagerTest.kt`. Covers `getLevel(id)` (returns expected for canonical ids, null otherwise), `getNextLevel(id)` (advances through `level0_0 → level0_1..4 → level1 → level2 → level3 → null`), `getAllLevels()` (returns 8 in canonical order, no duplicates). Sub-agent correctly mapped locked-world / portal-unlock logic to `Level0_0`'s companion (not `LevelManager`) and flagged for follow-up — see T-092.
- **Commit/PR:** PR #43 (squashed merge `a98b817`)
- **Tool:** `claude-code-sub-agent`

### T-092 — Kotest specs for Level0_0 portal-unlock companion
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 17 tests covering `Level0_0.portalUnlockRequirement(portalId)` and `Level0_0.portalTargetLevel(portalId)`. Spawned via `mcp__ccd_session__spawn_task` chip after T-090's sub-agent flagged this as a discrete follow-up.
- **Commit/PR:** PR #46 (squashed merge `faaf1a3`)
- **Tool:** `claude-code-sub-agent` (spawned-task chip from session)

### T-091 — `Strings.format(key, *args)` API for compositional strings
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Closes the i18n gap T-059 deliberately left. Added `Strings.format(key, vararg args: Any)` using a simple `{N}` regex substitution (chose this over `java.text.MessageFormat` to avoid locale-sensitive number/date quirks). **26 new keys**, **28 interpolation sites** swept across **10 screens** (`CloudAtlasOverlay`, `CloudAtlasScreen`, `Hud`, `LevelCompleteOverlay`, `LevelRunState`, `LevelSelectScreen`, `MainMenuScreen`, `PauseOverlay`, `StatsScreen`, `VictoryScreen`). New keys include `SLOT_LABEL`, `WORLD_PORTAL`, `SPIRIT_DEATH`, `COMBO_MULTIPLIER`, `ATLAS_PCT`, etc. **Byte-identical English output.** Deliberately left as literal: bare `"$score"` Labels (no surrounding copy), data values like `"${entry.title}"`, `Gdx.app.log` strings, printf templates using Kotlin's `.format()`.
- **Commit/PR:** PR #48 (squashed merge `8c6486b`)
- **Tool:** `claude-code-sub-agent`

### T-079 — CI duration optimization (smoke matrix)
- **Status:** Done (v2 — v1 reverted via PR #61)
- **Completed:** 2026-05-12
- **Outcome:** **v1** (PR #61) tried matrix-packing 8 jobs → 3 jobs; real CI data showed projected savings didn't materialize (warm gradle daemon only saved ~30s/level, not ~3min/level as projected) AND wall time doubled (~5min → ~12min). Closed without merging. **v2** (PR #62) re-applied just the wins that actually paid: gradle cache key rename (`gradle-${os}-…` → `${os}-gradle-…`) + `setup-java cache: gradle` on ai-smoke + conditional `android:lint` via git-diff path filter + `maxParallelForks = 4` on `:core:test` + **`concurrency: cancel-in-progress`** on both workflows. The big win: **gate job + doc-PR skip filter** — PRs that only touch `*.md`, `prompts/`, `marketing/`, `research/`, `.github/ISSUE_TEMPLATE/`, etc. skip the entire 8-job smoke matrix. Validated empirically by PR #63 (doc-only): gate ran 6s, all 8 smoke jobs reported as `skipped`, total wall ~2m31s vs ~5min code-PR baseline. Code PRs unchanged (~5min wall, 8 parallel jobs). Re-routed from `antigravity` to `claude-code-sub-agent` (AGV was quiet; pre-condition for going private).
- **Commit/PR:** PR #62 (squashed merge `fe307de`), validated by PR #63 (squashed merge `9a1342d`)
- **Tool:** `claude-code-sub-agent` (re-routed from `antigravity`)
- **Lesson learned:** sub-agent projections can be optimistic — verify with real CI data before merging. The v1 → v2 iteration was the right move.

### T-093 — Kotest specs for FontManager (scaling + shared cache)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 22 tests in `core/src/test/kotlin/com/sohai/platformer/FontManagerTest.kt`. Mocked `BitmapFont` via MockK (uses Objenesis under the hood; no GL needed). Pre-populates `FontManager`'s private `sharedCache` map via reflection — covers cache identity (`===`), per-size distinctness, `clearSharedCache` + `disposeShared` (verified `dispose()` called on each cached mock). `fontScale` contract verified as a **pure-math mirror** of line 40 (`(size * fontScale).roundToInt().coerceAtLeast(size)`) at fontScale = 1.0/2.0/3.0 because `FontManager.create()` is unreachable headlessly (both branches touch GL).
- **Commit/PR:** PR #52 (squashed merge `0134f2a`)
- **Tool:** `claude-code-sub-agent`
- **Follow-up flagged:** expose a factory seam (`var fontFactory: (Int, Color) -> BitmapFont`) so `create()` becomes unit-testable end-to-end.

### T-094 — Kotest specs for MusicManager (crossfade timing + volume)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 35 cases across 16 `when` blocks. Covers 1.5s crossfade timing (midpoint within `EPS=1e-3`), volume application out-of-fade, `play(sameTrack)` no-op, unknown-track logged-not-crashing, volume-0 silent, multi-`play()` only-last-wins. Mocked `Music` instances with backed `volume` getter/setter for round-trip assertions; `Gdx.app`/`Gdx.files`/`Gdx.audio` mocked in `beforeSpec`.
- **Commit/PR:** PR #50 (squashed merge `d0a734f`)
- **Tool:** `claude-code-sub-agent`
- **Source quirks pinned:** (1) `setMusicVolume()` silently skips during crossfades — silent UX failure if user drags slider mid-fade. (2) `stop()` is immediate despite doc claiming a fade. Spawn-task chip generated for fixing.

### T-103 — MusicManager: fix two quirks flagged by T-094 (silent-skip volume mid-crossfade; stop() doc)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Quirk 1 (behavior): `setMusicVolume()` now applies the new master volume during crossfade and fade-in, scaled by each track's current fade weight (`volMusic * (1-t)` outgoing, `volMusic * t` incoming) — slider drags between levels are heard immediately rather than dropped for 1.5s. Quirk 2 (doc only): rewrote `stop()` KDoc to match its synchronous-stop implementation. Updated the `setMusicVolume is called mid-crossfade` case in `MusicManagerTest` (which under T-094 pinned the silent-skip bug) to assert the new immediate-apply semantics; added a parallel mid-fade-in case.
- **Commit/PR:** PR #54 (squashed merge `3c6dcd6`)
- **Tool:** `claude-code-opus`

### T-078 — Procedural achievement icon generator (Kotlin tool)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `tools/IconGenerator.kt` (standalone `fun main()`, package `com.sohai.platformer.tools`) + 12 16×16 PNGs at `assets/icons/achievements/*.png`. Wired via `core/build.gradle.kts` test source set + `:core:generateAchievementIcons` JavaExec task. Pure-Kotlin + JDK std lib (`BufferedImage`, `Graphics2D`, `ImageIO`); no new deps. **JDK-portable** — digits drawn pixel-by-pixel in knock-out style instead of `drawString` (font rasterization varies by JDK). Companion `IconGeneratorCoverageTest` asserts: (1) 12 IDs match `AchievementRegistry.ALL` in order, (2) every render is 16×16, (3) two renders byte-identical (idempotence). Palette: dark-blue-grey BG, off-white silhouette, gold accent for special achievements, red for X-eyes/gems, green for seeds. Re-routed from `antigravity` (critical for unblocking T-066).
- **Commit/PR:** PR #57 (squashed merge `85ac2e7`)
- **Tool:** `claude-code-sub-agent` (re-routed from `antigravity`)

### T-080 — GitHub repo infrastructure (Issue/PR/Discussion templates)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 7 files under `.github/`: 3 Issue Forms (`bug-report.yml` 13 fields incl. required Severity + Platform; `feature-request.yml` 6 fields incl. required user story; `accessibility-issue.yml` 7 fields incl. required Area + WCAG criterion), `config.yml` (blank-issues off, contact link to Discussions), `PULL_REQUEST_TEMPLATE.md` (Summary + Closes + Test plan with Claude-footer delete-if-human note), and 2 Discussion templates (`announcements.yml` with pre-seeded "What's new / How to get it / Known issues" skeleton; `help.yml` with required "Where are you stuck?" dropdown). All YAML validated via `python yaml.safe_load`. Re-routed from `antigravity` (critical for public-alpha community readiness). Did NOT toggle GitHub features or modify CODEOWNERS/branch protection per scope.
- **Commit/PR:** PR #55 (squashed merge `5277fb9`)
- **Tool:** `claude-code-sub-agent` (re-routed from `antigravity`)

### T-097 — Death animation (alpha fade + camera zoom-out + screen-flash)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 0.5s animation on death. `LevelRunState` owns the timer (`deathAnimT`, `deathAnimActive` + companion constants); `LevelRenderer` exposes `var playerAlpha: Float` that `renderPlayer` multiplies into the sprite tint (renderer stays stateless about timing). Camera zoom driven on the already-injected `OrthographicCamera`. Cubic ease-out `1 - (1-t)³` drives both fade and zoom. 0.2s black flash via `ScreenFade.fadeOut(speed=5f)` on completion. Two guards: `!settings.reducedMotion` (T-058 invariant — snap instantly when on) AND `!Constants.SMOKE_MODE` (preserves smoke-test `deltaX`/`frameP99` determinism). `player.respawn()` restores `gravityScale = 1f` after the freeze. Both `reducedMotion=true` and `smokeMode=true` are byte-identical to pre-change behavior.
- **Commit/PR:** PR #56 (squashed merge `e622135`)
- **Tool:** `claude-code-sub-agent`

### T-077 — presskit() scaffold for Cloudy Ninja
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 14 files under `marketing/presskit/`: `index.html` (hero / about / 8 features / 6-image grid / trailer placeholder / awards placeholder / GitHub-only contact / factsheet / footer), `presskit.css` (60 lines, system fonts, 960px container, 768px mobile breakpoint, muted-green `#2e7d4a` accent), `data.xml` (presskit() schema — title, tagline, description, history, genre, platforms, releaseDate=TBA, price, features×8, trailers, images, logo/icon, awards, quotes, credits, contact, social, projects), `contacts.csv` (UTF-8 with BOM, **30 contacts parsed from T-050's outreach list**, public channels only), `_gen_placeholders.kt` (Kotlin script for placeholder regeneration), 6 screenshots (1920×1080), `cover.png` (1920×1080), `logo.png` (512×512 transparent), `icon.png` (256×256). Pure-Kotlin placeholder generation via JDK awt/imageio — no Pillow/ImageMagick. **Deployable to itch.io or GitHub Pages with zero further structural edits.** Re-routed from `antigravity` (critical for Sprint D launch outreach).
- **Commit/PR:** PR #58 (squashed merge `0445012`)
- **Tool:** `claude-code-sub-agent` (re-routed from `antigravity`)

### T-066 — Achievement icons wire-up
- **Status:** Done (toast surface; atlas-list surface flagged as follow-up)
- **Completed:** 2026-05-12
- **Outcome:** `Achievement.kt` gets new `iconPath: String = "icons/achievements/$id.png"` field — default uses `id` interpolation so `AchievementRegistry`'s 12 existing entries pick it up unchanged. `AchievementToast.kt` lazy-loads icons via `HashMap<String, Texture>` keyed by id, populates from `applyIcon()` when a toast promotes off `IDLE`. Missing files tolerated (warning log, no crash). Rendered via `VisImage` + `TextureRegionDrawable` at 32×32 with 8px right-pad before title/desc column. Cached textures disposed in `dispose()`. T-056's `AchievementTest` continues to pass via the default. **Scope deviation:** the brief listed `screens/AchievementsScreen.kt` as a second surface, but that file doesn't exist — achievements live as a comma-joined string in `StatsScreen`. Spawn-task chip generated for building the proper `AchievementsScreen`.
- **Commit/PR:** PR #59 (squashed merge `8ed30f2`)
- **Tool:** `claude-code-sub-agent`

### T-108 — AchievementsScreen with per-row icon layout
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Closes the surface T-066 deferred. New `screens/AchievementsScreen.kt` — scrollable per-slot list with 32×32 icon + bold gold title + italic desc + locked/unlocked status per row. Sort: unlocked first, then by registration order within each group. Lazy texture cache mirrors T-066 `AchievementToast` (HashMap by id, lazy populate, dispose-all). **Slot UX:** top-row tabs (1/2/3, active wrapped in brackets); `initialSlotIndex` constructor param lets MainMenu open on slot 0 while StatsScreen's "View All →" deep-links to its own slot. Locked vs unlocked styling: gold/green/full-alpha vs muted-grey/50%-alpha icon. **MainMenu button** added between Atlas and Stats via `Strings.get(StringKey.MENU_ACHIEVEMENTS)`. **StatsScreen refactor** drops comma-joined ids → `Achievements: N/12 unlocked` count + `[View All →]` button; all other StatsScreen features preserved untouched (T-060 best-times, deaths, completed levels, eco-tokens, slot-empty handling, `formatBestTime`).
- **Commit/PR:** PR #60 (squashed merge `baf46c3`)
- **Tool:** `claude-code-sub-agent`

### T-095 — Kotest specs for SoundManager (per-bus volume)
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 15 tests. Covers default-volume play, volume change applied to next-play (not retroactively), unknown-id soft-fail, volume = 0/1 boundaries, clamping (`setVolume(2f)` → 1, `setVolume(-0.5f)` → 0 via `coerceIn`), multi-sound distinct mocks, separate game vs UI bus, `setEnabled(false)` suppression, custom pitch. `Gdx.app`/`Gdx.audio`/`Gdx.files` MockK-mocked, per-test reset via `dispose() + init()`.
- **Commit/PR:** PR #51 (squashed merge `e88b18c`)
- **Tool:** `claude-code-sub-agent`
- **Contract surprise pinned:** unknown-id is logged at `log` level, not `error` (likely worth upgrading); `play()` uses the 3-arg `Sound.play(volume, pitch, pan)` overload.

### T-096 — Kotest specs for ScreenFade state machine
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** 16 tests across 8 `given` blocks. Reverse-engineered actual semantics (worth documenting): `fadeIn(speed)` sets `alpha=1, targetAlpha=0` (alpha lerps **down**, screen clears); `fadeOut(speed)` sets `alpha=0, targetAlpha=1` (alpha lerps **up**, screen blacks). `speed` is units-of-alpha/second, not 1/duration. No public `isComplete` — completion is `alpha == targetAlpha`. `onFadeOutComplete` fires exactly once. Test isolation via `sun.misc.Unsafe.allocateInstance` to bypass the GL-required constructor + private-field reflection.
- **Commit/PR:** PR #53 (squashed merge `cec4bf6`)
- **Tool:** `claude-code-sub-agent`
- **Naming note:** `fadeIn`/`fadeOut` semantics are intuitively reversed from common scene-transition convention — worth a doc comment or rename.

### T-049 — Climate-source compilation for NotebookLM
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Compiled 36 authoritative climate sources (PDFs and live URLs) across 12 topics; under 47MB total.
- **Commit/PR:** PR #26
- **Tool:** ntigravity

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

### T-031 — Tile-based terrain rendering
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Art-style abstraction layer (`TilesetPack` data class + `TilesetRegistry` singleton + `Settings.tilesetPackId`) lets future packs slot in via one `register(...)` call. `TileRenderer` lazy-loads the Kenney `pixel-platformer` atlas, slices 18×18 TextureRegions, and tile-fills `ObstacleRect`s at 0.32 m per tile. `LevelRenderer` runs a tile pass before the ShapeRenderer fallback (which handles unmapped kinds — CHECKPOINT/EXIT/cleaned-HAZARD). Kotest covers registry round-trips and the specific Kenney tile indices. Default `tilesetPackId = "kenney_pixel_platformer"` keeps saves backward-compatible. AI smoke green.
- **Commit/PR:** PR #14 (squashed merge `79108a8`)
- **Tool:** `claude-code-sonnet`

### T-046a — Tileset research: find pixel-art tilesets for 3 themes
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Antigravity (Gemini 3.1 Pro) researched 12 candidate tilesets (4 per theme: ARID/WIND/ECO) from OpenGameArt and Kenney.nl, compiled into `art-research/tileset-candidates.md` with name, source URL, license, file count, theme fit, art quality (1-5), and character-sprite notes. **Decision (post-visual-review):** Kenney `pixel-platformer` (CC0, ~350 files, side-scroller perspective) as base + OpenGameArt Pixel Art Forest (CC0) for ECO accents; ARID/WIND use Kenney's sandy/sky tiles within base pack. One Antigravity recommendation rejected post-review (Whispers of Avalon Desert — top-down RPG perspective; flagged in LEARNINGS.md as research-tool blindspot). **Antigravity time-to-output: ~5 min** for research; ~5 min of human visual review.
- **Commit/PR:** PR #10 (merged) + decision recorded in `GAME_PLAN.md` and T-031 unblocked.
- **Tool:** `antigravity`

### T-046b — Character sprite-sheet research
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** Created `art-research/character-sprite-candidates.md` with side-scrolling CC0/CC-BY sprite sheet candidates for Ebo, Laya, and Zephyr, prioritizing Kenney base palette variants and structurally compatible OpenGameArt sprites.
- **Commit/PR:** PR #22 (merged) `26d3d8d`

### T-047 — Audio asset research: CC0 music + SFX supplements
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `art-research/audio-candidates.md` with 32 verified candidates — 19 music tracks across 5 themes (ARID 6, WIND 6, ECO 7, MENU 6, BOSS 7) + 13 SFX entries (footsteps, UI, jumps, pickups, Kenney Interface Sounds 100-clip pack). Every URL WebFetched + license confirmed CC0/CC-BY. All BOSS tracks CC0 (no attribution at climactic moments); three ECO entries have seamless-loop guarantees; menu reaches into Free Music Archive (HoliznaCC0, John Bartmann). Recommended-picks block names a top track per theme + three top SFX, justified for libGDX + existing procedural ambient context. Re-routed from `antigravity` → `claude-code-sub-agent` (parallel with T-049).
- **Commit/PR:** PR #24 (squashed merge `aba3a75`)
- **Tool:** `claude-code-sub-agent` (re-routed from `antigravity`)

### T-048 — Marketing research: itch.io listing style guide
- **Status:** Done
- **Completed:** 2026-05-12
- **Outcome:** `marketing/itch-listing-style-guide.md` analyzing 12 itch.io listings across three buckets — precision platformers (Celeste, Sheepy, Frogfall, SELF, Öoo), Metroidvanias (Pseudoregalia, Lone Fungus, Alwa's Awakening, Vapor Trails, Anodyne), eco/nature (A Short Hike, Terra Nil). Synthesis: 5 headline patterns, screenshot rules (lead with GIF; protagonist in hero shot; 5–7 gallery assets), 60–75s trailer beat sheet, 3 differentiator surfaces for Cloudy Ninja (split-frame corrupted/restored cover, mid-air character-switch GIF, accessibility callout above sysreqs). **Notable finding:** trailer embeds are nearly absent from top-rated indie pages — autoplaying GIFs dominate; influences our launch asset priorities. Re-routed from `antigravity` → `claude-code-sub-agent` (parallel with T-049).
- **Commit/PR:** PR #25 (squashed merge `d60d2f4`)
- **Tool:** `claude-code-sub-agent` (re-routed from `antigravity`)

<!--
Template for moving a task here:

### T-XXX — <title>
- **Status:** Done
- **Completed:** YYYY-MM-DD
- **Outcome:** one-line summary of what shipped
- **Commit/PR:** <hash or PR link>
-->

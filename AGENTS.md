# AGENTS.md

## Multi-agent coordination
Open tasks live in [TASKS.md](TASKS.md). Before starting work, claim a task there (move it to `In Progress`, fill in your agent name + branch, push to `main`). Work in a git worktree on the branch you claimed. When done, merge to `main` and move the task to `Done`.

Required reading before claiming a task:
1. This file (architecture, conventions, module layout)
2. [GDD_ADDENDUM.md](GDD_ADDENDUM.md) (technical spec, calibration numbers, sprint plans, P0 bug history, feature specs)
3. [GAME_PLAN.md](GAME_PLAN.md) (high-level roadmap, content themes, educational goals)

---

## Project snapshot (May 2026, post Sprint C)
- **Engine:** libGDX 1.14.0 + Box2D, Kotlin, multi-module Gradle
- **Modules:** `core` (shared gameplay), `lwjgl3` (desktop launcher), `android` (Android launcher) — see `settings.gradle`
- **Resolution:** 1280×720 virtual, PPM = 100, y-up world coords; 4K/HiDPI supported via `DisplayScale`
- **Levels shipped:** 7 (4 tutorial rooms in World 0, 3 campaign in Worlds 1–3); Level 3 has boss arena
- **Characters:** 3 (Ebo / Laya / Zephyr) — switch with Swap button or `S` keyboard
- **Tests:** 9 specs covering player movement, persistence, contacts, particles, atlas, TMX coords
- **Audio:** 8 SFX + 3 looping ambient music tracks (procedurally generated); crossfade between levels
- **Controls:** two-thumb mobile UI (HUD buttons, semi-transparent), keyboard alt (WASD/arrows + space + E + S); fully rebindable in Settings

---

## Module / package layout

```
core/src/main/kotlin/com/sohai/platformer/
├── Main.kt                  # Game entry point → MainMenuScreen
├── Constants.kt             # Tuning hub: physics, jump windows, collision bits, virtual size
├── FontManager.kt           # Shared FreeType font cache (getShared(size) — DO NOT dispose)
├── abilities/
│   ├── CharacterAbility.kt  # Interface: onActionPressed/Held/Released, update, getCooldownRatio
│   ├── EboAbility.kt        # Seed Slam — spawns water droplets, cleanses hazards
│   ├── LayaAbility.kt       # Wind Dash — forward+up impulse, brief gravity reduction
│   └── ZephyrAbility.kt     # Float — 0.2× gravity for 1.5 s on cooldown
├── atlas/
│   └── CloudAtlasLibrary.kt # Registry of educational snapshot entries (id → entry)
├── audio/
│   ├── SoundManager.kt      # SFX playback singleton (8 canonical sounds)
│   └── ProceduralSoundGenerator.kt  # Generates WAVs at first run if assets missing
├── effects/
│   ├── WaterDroplet.kt      # Box2D droplet body w/ lifetime — managed by EboAbility
│   └── WindTrail.kt         # Visual-only fading particle — Laya feedback
├── entities/
│   ├── PlayerController.kt  # Movement, coyote/buffer, wall-jump, corner-correct, ability hooks
│   ├── EcoToken.kt          # Floating collectible
│   ├── SnapshotPickup.kt    # Cloud Atlas pickup (educational reward)
│   └── MovingPlatform.kt    # Kinematic platform; player carry via friction
├── input/
│   └── InputManager.kt      # Single input gate: keyboard + HUD button flags + debug overrides
├── levels/
│   ├── Level.kt             # Abstract base — id, spawn, setup, getCheckpoints, etc.
│   ├── LevelManager.kt      # Static registry, ordered traversal, getNextLevel
│   ├── LevelRegistry.kt     # Aliased re-export of TmxLevelDefinition.LevelRegistry
│   ├── TmxLevelDefinition.kt # Data class + TmxLevel concrete + LevelRegistry.ALL
│   ├── Level0_1.kt … 0_4.kt # Hand-built tutorial rooms (no TMX)
│   └── (Level1/2/3 are data-driven via LevelRegistry)
├── persist/
│   ├── GameState.kt         # @Serializable save model: completedLevels, bestScores, bestTimes
│   ├── SaveManager.kt       # Atomic JSON read/write, listSaves, deleteSave
│   ├── Settings.kt          # @Serializable settings: volume, screenShake, assist flags
│   └── SettingsManager.kt   # Settings load/save (separate file from GameState)
├── physics/
│   ├── WorldContactListener.kt  # Maps fixture userData → gameplay state
│   └── CleanseEventQueue.kt    # Buffers hazard-cleanse events for the renderer
├── rendering/
│   ├── CharacterAnimator.kt    # Sprite state machine (idle/run/jump/fall/wall)
│   ├── CharacterAtlas.kt       # Texture region holder per character
│   ├── SpriteFactory.kt        # createEbo / createLaya / createZephyr atlas builders
│   ├── ParallaxBackground.kt   # 3-layer parallax (mountains/midground hills/trees), 3-band sky gradient, stars in corrupted sky; ParallaxTheme = ARID/WIND/ECO
│   ├── ParticleSystem.kt       # 200-particle pool, additive blend, gravity per particle
│   └── ScreenFade.kt           # Async fade-in/out with completion callback
├── screens/
│   ├── MainMenuScreen.kt       # Title + 3 save slots + settings/atlas/quit
│   ├── LevelSelectScreen.kt    # Pick level, locked indicator
│   ├── GameScreen.kt           # Lifecycle coordinator (~350 LOC) — NO update logic, NO drawing
│   ├── LevelRunState.kt        # All mutable session state + main update() loop
│   ├── LevelRenderer.kt        # All ShapeRenderer/SpriteBatch drawing + Palette + particle helpers
│   ├── LevelTransitionController.kt  # Level-complete persistence + next-screen navigation
│   ├── Hud.kt                  # On-screen buttons + status/score/timer/stopwatch labels
│   ├── PauseOverlay.kt, GameOverOverlay.kt, LevelCompleteOverlay.kt, CloudAtlasOverlay.kt
│   ├── SettingsScreen.kt, CloudAtlasScreen.kt, VictoryScreen.kt
│   └── ...
└── world/
    ├── ObstacleManager.kt      # Owns all static-obstacle Box2D bodies (rect / checkpoint / exit)
    ├── ObstacleKind.kt         # Enum: GROUND, WALL, HAZARD, CHECKPOINT, EXIT
    └── MapLevelLoader.kt       # Loads .tmx into ObstacleManager + MovingPlatforms (flipY = true for TmxLevels — see Levels section)
```

---

## Core architecture

### Game lifecycle (top-down)
1. `Main.kt` → `MainMenuScreen` (title, slot cards, settings/atlas/quit)
2. Slot card → `GameScreen(level, game, resumeCheckpoint?, isTimeTrial?)`
3. `GameScreen.init` builds the world + the three subsystems below, then renders/updates them
4. Level-complete → `LevelTransitionController.goToNextLevel` → next `GameScreen` or `VictoryScreen`

### GameScreen subsystem split (T-021)
`GameScreen.kt` is a thin coordinator (~350 LOC). It owns the libGDX lifecycle (render/resize/dispose) and wires three focused subsystems:

- **`LevelRunState`** — owns all mutable session state (score, spirit health, combo timers, level timer, completion flags, camera tracking, screen shake, hitstop, debug autopilot) **and** the main `update(delta)` loop. Side effects that need GameScreen-level objects (atlas overlay, game-over overlay) are routed via callbacks (`onAtlasCollected`, `onGameOverStart`).
- **`LevelRenderer`** — owns *all* ShapeRenderer/SpriteBatch drawing, the `Palette` color constants, and helper methods to spawn particles (footsteps, jump puff, landing dust, sparkles, cleanse bursts). GameScreen calls `renderer.renderWorld(...)` and `renderer.renderPlayer(...)`.
- **`LevelTransitionController`** — owns level-complete persistence and screen transitions. `startLevelComplete` returns the overlay; `goToNextLevel` advances to the next `GameScreen` or `VictoryScreen`.

When adding gameplay logic, decide which subsystem owns it. Drawing → Renderer. State → RunState. Inter-screen flow → TransitionController. **Do not** add update/draw code back into `GameScreen.kt` — keep it a coordinator.

### Player movement
`PlayerController.kt` owns:
- Direct horizontal velocity (target speed × input axis); friction-based slowdown
- Coyote time (0.10 s), jump buffer (0.10 s), variable jump cut (0.4× on release)
- Asymmetric gravity (apex hang at 0.5×, fall at 1.45×) — set per-frame via `body.gravityScale`
- Wall slide cap, wall-jump impulse, wall-jump lock-out (0.13 s)
- Corner-correction raycast (≤ 6 cm) when rising into clipped overhead
- Footstep callback every ~12 cm of grounded horizontal travel
- Optional `CharacterAbility` swap via `changeAbility()`

`Constants.kt` is the single tuning hub. Change values there before scattering literals.

### Abilities
All abilities implement `CharacterAbility`. Swap by character (Ebo / Laya / Zephyr) via the Swap button — `LevelRunState.switchCharacter()` cycles them and triggers a colored sparkle burst. `PlayerController.update()` invokes `onActionPressed/Held/Released` and `update(dt)` automatically.

### Levels
- **Tutorial rooms (World 0):** hand-built — `Level0_1.kt` through `Level0_4.kt` extend `Level` directly and override `setup()` to build geometry programmatically.
- **Campaign (Worlds 1–3):** data-driven — `LevelRegistry.ALL` is a list of `TmxLevelDefinition(id, name, mapPath, spawnX, spawnY, levelWidthPx, exitXPx, ecoTokens, snapshots, checkpoints)`. Adding a campaign level = appending one entry. The shared `TmxLevel` class loads the `.tmx` via `MapLevelLoader` with `flipY = true`. **Why `flipY=true`:** libGDX's `TmxMapLoader` already flips rectangle Y coords internally (`r.y = mapHeight - tiledY - height`), producing y-up values. `MapLevelLoader`'s own flip with `flipY=true` undoes that, correctly placing objects so that Tiled Y=0 (ground) maps to Box2D Y≈0 (bottom of world). Without the second flip, ground ends up at the top of the screen and players spawn-die immediately.

### Persistence
- `GameState` (per-slot) — completedLevels, collectedAtlasIds, bestScores, bestTimes, totalDeaths, lastPlayed
- `Settings` (shared across slots) — volume, keybinds (placeholder), screen-shake, assist-mode flags
- Atomic write: temp file → copy → delete (defends against mid-write crashes)
- Three save slots wired through MainMenuScreen
- Checkpoint autosave: separate file (`checkpoint_autosave.json`) written on checkpoint touch; suppressed in time trial mode

### Audio
- `SoundManager` (singleton) plays the 8 canonical SFX: `jump`, `land`, `collect_token`, `collect_snapshot`, `death`, `checkpoint`, `level_complete`, `hazard_cleansed`
- `ProceduralSoundGenerator` writes WAVs to `assets/audio/sfx/` if missing on first run
- `MusicManager` (singleton, T-030) plays looping background music with 1.5 s crossfade; `volMusic` knob in Settings. Three procedurally-generated 60-second WAVs: `ambient_arid`, `ambient_wind`, `ambient_eco`

### Visual systems
- `ParallaxBackground` — 3 themes (ARID/WIND/ECO); 3-layer scrolling (mountains 0.15×, midground hills 0.28×, trees 0.4×); 3-band sky gradient with star field in corrupted sky; mountain peak caps + pine-crown on trees. Sky lerps from corrupted → cleansed palette as `cleanseRatio` rises.
- `LevelRenderer` — ground tiles get grass tufts (top) + shadow strip (bottom); hazard tiles get triangular spike shapes; moving platforms get an underside shadow. Palette constants live in the companion object — never scatter raw `Color(...)` into geometry code.
- `ParticleSystem` — 200-particle pool, owned by GameScreen, mutated by Renderer helpers
- Box2DLights `RayHandler` — ambient + a single PointLight attached to the player body
- `ScreenFade` — owned by GameScreen, used at level start (fade-in) and on level-complete (fade-out)

### Debug autopilot
`LevelRunState` has a built-in autopilot for unattended smoke tests. Activate with Gradle project properties:

```powershell
# Windows — set JAVA_HOME first if needed
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

.\gradlew :lwjgl3:run -PcloudyAutopilot=true -PcloudyAutopilotSeconds=30 -PcloudyAutoquitSeconds=35
```

| Property | Default | Meaning |
|---|---|---|
| `-PcloudyAutopilot=true` | _(disabled)_ | Enable the autopilot |
| `-PcloudyAutopilotSeconds=N` | 3 s | Seconds the bot drives before releasing input |
| `-PcloudyAutoquitSeconds=N` | _(none)_ | Seconds until the process auto-exits (useful in CI) |

**What it does:** Holds right, jumps periodically (~every 0.8 s), fires ability every 4 s, and adds extra jumps when stuck or wall-sliding. Designed for unattended runs — the player is **not** expected to touch the keyboard while autopilot is active. Normal human play ignores all autopilot code paths.

---

## Editing conventions

- Keep all gameplay code in `core/`. Launchers stay thin.
- **Constants live in `Constants.kt`.** Tune one at a time, playtest, write down the feel difference.
- **Hot-path renders are in `LevelRenderer.kt`.** Don't allocate Color/Vector2 inside its draw methods — use the `Palette` companion or pre-allocated temporaries.
- **Fonts are shared.** Use `FontManager.getShared(size)`; DO NOT dispose shared fonts in screen `dispose()` — they live for the app lifetime.
- When adding a level: append one `TmxLevelDefinition` to `LevelRegistry.ALL` (campaign) or write a new `Level0_X.kt` (tutorial). LevelManager picks them up by registration order.
- When adding a character: implement `CharacterAbility`, instantiate in `GameScreen.init`, add to the `LevelRunState.switchCharacter()` rotation, give it a distinct sparkle color.
- Preserve fixture `userData` strings (`ground`, `hazard`, `player_foot`, `player_wall_left/right`, `checkpoint_activated`, `hazard_cleaned`) — they're how `WorldContactListener` and gameplay code communicate.
- `Hud` only flips flags on `InputManager`. Gameplay code never reads platform input APIs directly.
- TMX files are **y-up** (Cloudy Ninja convention). Load with `flipY = false`. `MapLevelLoader.load(...)`.
- **Body destruction is queued.** Use `pendingBodyDestroy.add(body)` in update code — never call `world.destroyBody` from a contact callback or mid-`world.step`.

---

## Build / run workflow

- Desktop run: `./gradlew lwjgl3:run`
- Desktop jar: `./gradlew lwjgl3:jar` (variants: `jarMac`, `jarLinux`, `jarWin`, `dist`)
- Tests: `./gradlew :core:test`
- Compile-only check: `./gradlew :core:compileKotlin`
- Android lint: `./gradlew android:lint`
- Android device launch: `./gradlew android:run` (requires `local.properties` / `ANDROID_SDK_ROOT`)
- Full build: `./gradlew build`
- CI: `.github/workflows/ci.yml` runs compile + test + android lint on push/PR to `main`

**Before declaring a feature done:** run `./gradlew :core:compileKotlin && ./gradlew :core:test`. Where feasible, also run `./gradlew lwjgl3:run` and confirm no `[Perf]` log shows fps < 100 or maxDelta > 0.05.

---

## Integration points

- Root `build.gradle` regenerates `assets/assets.txt` from `assets/` — do not hand-edit
- `android/AndroidLauncher.kt` initializes `Main()` with immersive mode
- `lwjgl3/Lwjgl3Launcher.kt` starts the desktop app; `StartupHelper.java` handles JVM relaunch quirks
- `settings.gradle` applies the Foojay resolver convention plugin (auto-downloads JDK)

### Library tier
| Library | Use |
|---|---|
| kotlinx.serialization 1.7.3 | All `@Serializable` save data |
| VisUI 1.5.4 | Buttons, labels, tables across all screens |
| MockK 1.13.14 | Mocking inside Kotest specs |
| Kotest 5.8.1 | Behavior/Description specs (`*Test.kt`) |
| Box2DLights | Player point light, ambient illumination |

---

## Formatting / generated files

- Follow `.editorconfig`: 4 spaces for Kotlin/Java/Groovy, 2 for Gradle, UTF-8, LF
- Ignore generated outputs: `build/`, `core/bin/`, `lwjgl3/bin/`, `android/bin/`, `assets/assets.txt`

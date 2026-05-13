# Cloudy Ninja — Architecture

A high-level map of the codebase: what the modules are, what subsystems
live where, and the small handful of patterns that recur everywhere.

This document is descriptive, not prescriptive. For conventions ("where do
I add a new thing?") see [`AGENTS.md`](../AGENTS.md). For tuning constants
and physics knobs see [`core/src/main/kotlin/com/sohai/platformer/Constants.kt`](../core/src/main/kotlin/com/sohai/platformer/Constants.kt).

---

## 1. Module layout

Cloudy Ninja is a standard libGDX multi-module Gradle project. Three
modules, wired through [`settings.gradle`](../settings.gradle).

```
Cloudy-Ninja/
├── core/      # All shared gameplay code (Kotlin). Engine-agnostic.
├── lwjgl3/    # Desktop launcher (Windows/macOS/Linux JAR).
├── android/   # Android launcher (APK).
└── assets/    # Shared art, audio, fonts, TMX maps, atlas JSON.
```

### `core/`
Pure Kotlin. All gameplay, rendering, persistence, levels, screens, and
audio code lives here. No JVM-launcher or Android dependencies — compiles
identically against either runtime.

- Source: [`core/src/main/kotlin/com/sohai/platformer/`](../core/src/main/kotlin/com/sohai/platformer/)
- Tests: [`core/src/test/kotlin/`](../core/src/test/kotlin/) — Kotest + MockK,
  ~600+ specs, 4 parallel forks (see [`core/build.gradle`](../core/build.gradle))
- Deps: libGDX (gdx, gdx-box2d, gdx-freetype), Box2DLights,
  kotlinx.serialization, VisUI, gdx-controllers. `ashley` and `gdx-ai` are
  declared but unused (tracked as T-127).

### `lwjgl3/`
Thin desktop launcher. [`Lwjgl3Launcher.kt`](../lwjgl3/src/main/kotlin/com/sohai/platformer/lwjgl3/Lwjgl3Launcher.kt)
constructs `Main()` inside an `Lwjgl3Application`;
[`StartupHelper.java`](../lwjgl3/src/main/java/com/sohai/platformer/lwjgl3/StartupHelper.java)
handles macOS `-XstartOnFirstThread` relaunch. `:lwjgl3:dist` is an alias
for `:lwjgl3:jar` and produces the release fat-JAR for itch.io.

### `android/`
Thin Android launcher.
[`AndroidLauncher.kt`](../android/src/main/kotlin/com/sohai/platformer/android/AndroidLauncher.kt)
extends `AndroidApplication`, enables immersive mode, starts `Main()`.

Asset manifest [`assets/assets.txt`](../assets/assets.txt) is regenerated
by the root [`build.gradle`](../build.gradle) — do not hand-edit.

---

## 2. Subsystem map

The `core/` package tree is organized by subsystem. Each directory below
maps to one Kotlin package under `com.sohai.platformer.*`.

```
core/src/main/kotlin/com/sohai/platformer/
│
├── Main.kt              ──►  libGDX Game entry. Picks Splash or MainMenu.
├── Constants.kt         ──►  Single tuning hub (gravity, jump windows,
│                              collision bits, virtual size, BUILD_VERSION).
├── FontManager.kt       ──►  Shared FreeType font cache (do NOT dispose).
│
├── rendering/           Visual systems
│   ├── ParallaxBackground       3 themes × 3 layers + sky gradient
│   ├── ParticleSystem           200-particle pool, additive blend
│   ├── ScreenFade               Async fade-from/to-black with callback
│   ├── ScreenShake              T-116 stomp/boss-hit (linear decay)
│   ├── DisplayScale             4K/HiDPI font + sprite scale
│   ├── TilesetPack              Pure-data tileset description
│   ├── TilesetRegistry          Active-pack lookup, driven by Settings
│   ├── TileRenderer             Sprite-batched terrain tiles
│   ├── HighContrastPalette      Color-blind palette toggle (T-057)
│   ├── CharacterAnimator        Idle/run/jump/fall/wall state machine
│   ├── CharacterAtlas           Per-character TextureRegion holder
│   └── SpriteFactory            createEbo / createLaya / createZephyr
│
├── physics/             Box2D contact + event glue
│   ├── WorldContactListener     Fixture userData → gameplay state
│   └── CleanseEventQueue        Buffered hazard-cleanse events
│
├── input/               Single input gate
│   ├── InputManager             Keyboard + HUD flags + debug overrides
│   └── RestartHoldTracker       Hold-to-restart timing
│
├── audio/               SFX + music
│   ├── SoundManager             8 SFX (jump, land, collect_token, …)
│   ├── ProceduralSoundGenerator Writes WAVs on first run if missing
│   ├── MusicManager             Looping ambient + 1.5 s crossfade
│   └── ProceduralMusicGenerator 60-second ambient WAVs
│
├── persist/             JSON save/load
│   ├── GameState                Per-slot save model (@Serializable)
│   ├── SaveManager              Atomic read/write, listSaves, deleteSave
│   ├── SaveMigrations           Versioned schema migration (T-113)
│   ├── Settings                 Shared settings model (@Serializable)
│   └── CrashReporter            Uncaught-exception dumper (T-115)
│
├── progression/         Achievements
│   ├── Achievement              @Serializable record
│   ├── AchievementRegistry      13 achievements + lookup
│   ├── AchievementPredicates    Pure unlock predicates
│   └── AchievementUnlocker      Evaluates predicates, fires toast
│
├── levels/              Level definitions and registry
│   ├── Level                    Abstract base
│   ├── LevelManager             Ordered registry traversal
│   ├── TmxLevelDefinition       Data class + concrete TmxLevel + ALL list
│   ├── Level0_0 … Level0_4      Hand-built tutorial rooms (no TMX)
│   └── LevelEntityFactory       Builds tokens/snapshots/enemies/platforms
│                                 from a level def — extracted from GameScreen
│
├── world/               Static world geometry
│   ├── ObstacleManager          Owns all static Box2D bodies + ObstacleKind
│   └── MapLevelLoader           .tmx → ObstacleManager (flipY=true; see §3)
│
├── entities/            Dynamic gameplay actors
│   ├── PlayerController         Movement, coyote/buffer, wall-jump
│   ├── DeathCause               Sealed cause enum (hazard/enemy/fall…)
│   ├── EcoToken                 Floating collectible
│   ├── SnapshotPickup           Cloud Atlas pickup
│   ├── MovingPlatform           Kinematic platform; friction-carry
│   ├── Enemy                    Abstract base; deferred body destruction
│   ├── SmogSprite               Ground-patrolling 2-hit enemy
│   ├── DriftHusk                Level 2 air-drifter (T-062)
│   ├── Projectile               Kinematic hazard (lightning bolt, …)
│   └── StormSentinel            Level 3 boss
│
├── abilities/           Per-character ability strategy
│   ├── CharacterAbility         Interface (onPressed/Held/Released)
│   ├── EboAbility               Seed Slam (cleanses hazards)
│   ├── LayaAbility              Wind Dash (impulse + gravity dip)
│   └── ZephyrAbility            Float (0.2× gravity, 1.5 s)
│
├── effects/             Ability VFX (separated from gameplay)
│   ├── WaterDroplet             Box2D droplet body w/ lifetime
│   └── WindTrail                Visual-only fading particle
│
├── screens/             libGDX Screens + overlays
│   ├── SplashScreen             Cold-start preload + progress bar (T-104)
│   ├── MainMenuScreen           Title + 3 save slots
│   ├── LevelSelectScreen        Locked indicator + best times
│   ├── SettingsScreen           Volume / keybinds / accessibility
│   ├── AchievementsScreen       Per-row icons + descriptions
│   ├── StatsScreen              Lifetime stats per slot
│   ├── CreditsScreen            Settings → Credits (T-101)
│   ├── CloudAtlasScreen         Browse collected snapshots
│   ├── VictoryScreen            End-of-campaign + delta indicators
│   ├── GameScreen               Lifecycle coordinator (~350 LOC)
│   ├── LevelRunState            Mutable session state + update loop
│   ├── LevelRenderer            ALL ShapeRenderer/SpriteBatch draws
│   ├── LevelTransitionController Persistence + next-screen navigation
│   ├── Hud                      Two-thumb buttons + status/score labels
│   ├── PauseOverlay             0.2 s fade-in; auto-pauses on alt-tab
│   ├── GameOverOverlay          Restart / quit
│   ├── LevelCompleteOverlay     Score + best-time row
│   ├── DeathRecapOverlay        Cause-of-death summary
│   ├── CloudAtlasOverlay        In-game pickup celebration
│   └── AchievementToast         Slide-in unlock notification
│
├── atlas/               Cloud Atlas data
│   └── CloudAtlasEntry          Entry record + CloudAtlasLibrary registry
│
├── i18n/                Localization
│   └── Strings                  130+ key → English string map
│
└── util/
    └── GameRandom               Seeded RandomXS128 singleton (T-A3)
```

---

## 3. Key patterns

A small number of patterns recur across the codebase. Knowing them makes
the rest of the code read like variations on a theme.

### Three-way GameScreen split (T-021)

`GameScreen` is a thin coordinator (~350 LOC). It owns libGDX lifecycle
(`render`/`resize`/`dispose`) and wires three focused subsystems. Adding
gameplay logic means picking which of the three owns it:

```
                  ┌────────────────────────────────────┐
                  │            GameScreen              │
                  │  (libGDX lifecycle coordinator)    │
                  └──────┬──────────┬──────────────┬───┘
                         │          │              │
              ┌──────────▼─────┐ ┌──▼──────────┐ ┌─▼─────────────────────────┐
              │ LevelRunState  │ │LevelRenderer│ │LevelTransitionController  │
              │                │ │             │ │                           │
              │ mutable state, │ │ ALL drawing │ │ level-complete persistence│
              │ update(delta)  │ │ + Palette   │ │ + next-screen navigation  │
              │ side effects   │ │ + particle  │ │                           │
              │ via callbacks  │ │   helpers   │ │                           │
              └────────────────┘ └─────────────┘ └───────────────────────────┘
```

- [`LevelRunState`](../core/src/main/kotlin/com/sohai/platformer/screens/LevelRunState.kt)
  owns score, spirit health, combo timers, completion flags, camera
  tracking, screen shake (lightning + boss-defeat), hitstop, and debug
  autopilot. Side effects that need `GameScreen`-level objects (atlas
  overlay, game-over overlay) flow back via `onAtlasCollected`,
  `onGameOverStart`, etc.
- [`LevelRenderer`](../core/src/main/kotlin/com/sohai/platformer/screens/LevelRenderer.kt)
  owns *all* `ShapeRenderer`/`SpriteBatch` calls, the `Palette` color
  constants in its companion object, and helpers that spawn particles
  (footsteps, jump puff, landing dust, sparkles, cleanse bursts).
- [`LevelTransitionController`](../core/src/main/kotlin/com/sohai/platformer/screens/LevelTransitionController.kt)
  owns level-complete persistence and screen transitions
  (`startLevelComplete`, `goToNextLevel`).

**Rule:** drawing → `LevelRenderer`. Mutable state → `LevelRunState`.
Inter-screen flow → `LevelTransitionController`. Do not push update or
draw code back into `GameScreen`.

### Save persistence: SaveManager + SaveMigrations + Settings

```
   write path                           read path
   ──────────                           ─────────
   GameState ──serialize──► JSON        JSON ──deserialize──► GameState
        │                                    │
        ▼                                    ▼
   SaveManager.write(slot)            SaveManager.read(slot)
        │ atomic                            │
        │  tmp → copy → delete              ▼
        ▼                              SaveMigrations.apply(rawJson)
   .cloudy-ninja/slot-N.json                │
                                            ▼
                                       GameState (current schema)
```

- [`GameState`](../core/src/main/kotlin/com/sohai/platformer/persist/GameState.kt)
  is the `@Serializable` per-slot model (completed levels, atlas IDs, best
  scores, best times, total deaths, last-played timestamp).
- [`SaveManager`](../core/src/main/kotlin/com/sohai/platformer/persist/SaveManager.kt)
  writes atomically (temp file → copy → delete) so a mid-write crash
  cannot corrupt the slot. Three slots; plus a separate
  `checkpoint_autosave.json` written on checkpoint touch (suppressed in
  time-trial mode). API is `deleteSave(filename)`, not `deleteSlot`.
- [`SaveMigrations`](../core/src/main/kotlin/com/sohai/platformer/persist/SaveMigrations.kt)
  (T-113) is the version-bump scaffold — runs on load before
  deserialization to upgrade old schemas.
- [`Settings`](../core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt)
  is a separate `@Serializable` model shared across slots (volume buses,
  keybinds, screen-shake, assist-mode flags) managed by `SettingsManager`.

### Achievements: registry + predicates + unlocker

```
   gameplay event ──► AchievementUnlocker.check(context)
                              │
                              ▼
                   for each Achievement in AchievementRegistry.ALL:
                       AchievementPredicates.eval(achievement, context)
                              │
                       if newly true ─► persist + AchievementToast
```

- [`AchievementRegistry`](../core/src/main/kotlin/com/sohai/platformer/progression/AchievementRegistry.kt)
  enumerates 13 achievements (id, title, description).
- [`AchievementPredicates`](../core/src/main/kotlin/com/sohai/platformer/progression/AchievementPredicates.kt)
  holds the unlock conditions as pure functions.
- [`AchievementUnlocker`](../core/src/main/kotlin/com/sohai/platformer/progression/AchievementUnlocker.kt)
  is invoked from `LevelRunState` and `LevelTransitionController` at the
  right moments, writes unlocks back to `GameState`, and fires the toast.

### Entity-factory pattern (T-106)

[`LevelEntityFactory`](../core/src/main/kotlin/com/sohai/platformer/levels/LevelEntityFactory.kt)
extracts entity construction out of `GameScreen.init`. Given a
`TmxLevelDefinition` (or hand-built `Level`), it builds the `EcoToken`,
`SnapshotPickup`, `Enemy`, and `MovingPlatform` instances that populate
the room. `GameScreen` only needs to call the factory and store the lists
— no per-entity construction boilerplate.

### Box2D + level loading: the `flipY` rule

```
     Tiled (.tmx)            libGDX TmxMapLoader        MapLevelLoader
     y-down                  flips rects internally     flipY = true
     ─────────               ──────────────────────     ───────────────────
     Y = 0 at top  ────►     r.y = mapH - tiledY - h    flips again
                             (now y-up)                  ─────────────►
                                                        Y = 0 at ground
                                                        Box2D y-up world
```

TMX files are loaded by [`MapLevelLoader`](../core/src/main/kotlin/com/sohai/platformer/world/MapLevelLoader.kt)
with `flipY = true`. libGDX's `TmxMapLoader` already flips rectangle Y
coords internally, so the second flip cancels that out and puts ground
at Box2D Y ≈ 0. Without `flipY = true`, ground ends up at the top of the
screen and the player spawn-dies immediately.

### Single input gate

```
   keyboard / touch / HUD button / autopilot
                       │
                       ▼
                  InputManager  (flags + axes)
                       │
                       ▼
              PlayerController, LevelRunState
```

- [`Hud`](../core/src/main/kotlin/com/sohai/platformer/screens/Hud.kt) only
  flips flags on [`InputManager`](../core/src/main/kotlin/com/sohai/platformer/input/InputManager.kt).
- Gameplay code never reads platform input APIs directly.
- The autopilot path (`-PcloudyAutopilot=true`) also writes through
  `InputManager`, so the rest of the codebase cannot tell the difference
  between human and bot input.

### Ability strategy + character swap

All three abilities implement [`CharacterAbility`](../core/src/main/kotlin/com/sohai/platformer/abilities/CharacterAbility.kt).
`LevelRunState.switchCharacter()` cycles them on the Swap button (S key,
or HUD button) and triggers a colored sparkle burst.
`PlayerController.update()` invokes `onActionPressed`, `onActionHeld`,
`onActionReleased`, and `update(dt)` on the active ability each frame.
Adding a fourth character is: implement the interface, instantiate in
`GameScreen.init`, register in the swap rotation, give it a sparkle color.

### Deferred Box2D body destruction

Bodies cannot be destroyed inside a contact callback or mid-`world.step`.
The convention everywhere: push onto `pendingBodyDestroy` and let the
update loop drain it after the step. `Enemy` exposes this directly;
`LevelRunState` drains the queue once per frame.

### Fixture userData as a communication bus

`WorldContactListener` reads fixture `userData` strings to route contact
events. Preserved tags: `ground`, `hazard`, `player_foot`,
`player_wall_left`, `player_wall_right`, `checkpoint_activated`,
`hazard_cleaned`. New entities should use these where possible — the
contact listener does not branch on body type, only on userData.

### Shared `FontManager`

[`FontManager.getShared(size)`](../core/src/main/kotlin/com/sohai/platformer/FontManager.kt)
returns a cached `BitmapFont` keyed by size. **Never dispose** the
returned font in a screen's `dispose()` — shared fonts live for the app
lifetime. T-109 added a `FontLoader` interface so tests can inject a
no-op loader without `Gdx.files`.

### Seeded RNG for determinism

All randomness flows through [`GameRandom`](../core/src/main/kotlin/com/sohai/platformer/util/GameRandom.kt),
a `RandomXS128` singleton seeded per-run (T-A3). This is the seed
ghost-replay (T-038) will eventually anchor to.

### Two screen-shake systems (post-T-116)

There are currently two camera-shake implementations that coexist and
sum on overlapping frames:

- `LevelRunState.triggerShake()` — pre-existing; lightning hits and
  boss-defeat; sin/cos offset.
- [`rendering/ScreenShake`](../core/src/main/kotlin/com/sohai/platformer/rendering/ScreenShake.kt)
  — T-116; stomps and boss-hits; linear decay.

Future camera work should be aware that the two offsets add together.

---

## 4. Cross-cutting concerns

- **Constants live in [`Constants.kt`](../core/src/main/kotlin/com/sohai/platformer/Constants.kt)**
  — gravity, jump windows, collision bits, virtual size, `BUILD_VERSION`,
  `BUILD_DATE`. Tune one at a time; never scatter literals.
- **Hot-path allocations** — `LevelRenderer` runs every frame; don't
  allocate `Color` or `Vector2` in draw methods. Use the `Palette`
  companion or pre-allocated temporaries. Same for `LevelRunState.update`.
- **Tests bypass GL constructors** — Box2D entities and screens needing
  real `SpriteBatch`/`Texture` are built via reflection (`ObjenesisStd` /
  `sun.misc.Unsafe.allocateInstance`); MockK mocks libGDX statics in
  `beforeSpec`/`afterSpec`.
- **CI / smoke** — [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
  runs compile + test + android lint on push/PR to `main`. Smoke uses the
  autopilot (`-PcloudyAutopilot=true`); `CrashReporter` is a no-op in
  smoke mode.

---

## 5. Where to read next

- Conventions and the routing table: [`AGENTS.md`](../AGENTS.md),
  [`START_HERE.md`](../START_HERE.md)
- Technical reference: [`GDD_ADDENDUM.md`](../GDD_ADDENDUM.md)
- Roadmap and vision: [`GAME_PLAN.md`](../GAME_PLAN.md)
- Gotchas other agents have hit: [`LEARNINGS.md`](../LEARNINGS.md)
- Open ticket queue: [`TASKS.md`](../TASKS.md)
- Session-to-session continuity: [`HANDOFF.md`](../HANDOFF.md)

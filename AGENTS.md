# AGENTS.md

## Multi-agent coordination
Open tasks live in [TASKS.md](TASKS.md). Before starting work, claim a task there (move it to `In Progress`, fill in your agent name + branch, push to `main`). Work in a git worktree on the branch you claimed. When done, merge to `main` and move the task to `Done`.

## Project snapshot
- This is a multi-module **libGDX** game generated with gdx-liftoff.
- The current design is a momentum-based 2D physics platformer built on Box2D, focused on climate-change education, the water cycle, and eco-restoration.
- Controls are intended for a two-thumb mobile UI: movement on the left, context-sensitive action on the right.
- Shared gameplay code lives in `core/`; platform launchers live in `lwjgl3/` and `android/`.
- `settings.gradle` includes exactly these modules: `core`, `lwjgl3`, `android`.

## Core architecture
- `core/src/main/kotlin/com/sohai/platformer/Main.kt` is the shared entry point and immediately opens `GameScreen`.
- `GameScreen.kt` owns the Box2D world, level setup, camera, HUD, and the main update loop; manages effect rendering and character switching.
- `InputManager.kt` is the single input gate: keyboard, touch zones, and UI buttons all feed through it; supports left/right movement, jump, and action (ability) input.
- `PlayerController.kt` owns movement, coyote time, jump buffering, wall jumps, and wall sliding; accepts an optional `CharacterAbility` to enable character-specific actions; can swap abilities on the fly via `changeAbility()`.
- `PlayerController.kt` is the shared physics base for the character roster (`Ebo`, `Laya`, and later assist characters); differentiation comes through context-sensitive action abilities.
- `abilities/CharacterAbility.kt` is an interface for character abilities; implement `onActionPressed()`, `onActionHeld()`, `onActionReleased()`, and `update(deltaTime)`.
- `abilities/EboAbility.kt` implements Ebo's Seed Slam: cooldown-based ability that spawns rain droplets, applies downward impulse, and manages droplet lifecycle.
- `abilities/LayaAbility.kt` implements Laya's Wind Dash: cooldown-based mobility ability that applies forward and upward impulses for fast traversal; spawns wind trail effects on activation.
- `effects/WaterDroplet.kt` represents a single water droplet with physics body, visual radius, and lifetime; managed by abilities.
- `effects/WindTrail.kt` represents a wind trail particle (visual-only, no physics) that fades over time; spawned by Laya's Wind Dash for feedback.
- `MovingPlatform.kt` defines the kinematic moving platform used in `GameScreen.createTestEnvironment()`; mirror that pattern when adding moving ground.
- `WorldContactListener.kt` turns Box2D fixture `userData` strings into gameplay state (`ground`, `hazard`, `player_foot`, `player_wall_left`, `player_wall_right`).
- `Constants.kt` is the tuning hub for physics, jump timing, and collision bits; prefer changing values there before scattering literals.

## Editing conventions
- Keep gameplay logic in `core/`; launcher modules should stay thin wrappers.
- Keep the current gray-box momentum test level in `GameScreen.createTestEnvironment()` as the tuning ground before layering art, world-state persistence, or hub-world flow on top.
- When adding character-specific abilities, preserve the shared base controller and branch only the action logic (for example, rain/Seed Slam for `Ebo` and wind-based mobility for `Laya`). Each ability is a separate `.kt` file in `abilities/` that implements `CharacterAbility`.
- To add a new character: create a new ability class (e.g., `LayaAbility.kt`), instantiate it in `GameScreen`, wire it to the `PlayerController`, and set any deferred references via setter methods.
- Ability input is routed through `InputManager.isActionPressed()` and `InputManager.isActionJustPressed()` (mapped to keyboard `E` or the HUD **ACTION** button); the `PlayerController.update()` method calls ability callbacks automatically.
- Characters are visually differentiated in `GameScreen.render()` by rendering the player body as a colored circle: Ebo is brown (earth/nature), Laya is white/light-blue (wind/air). Each ability spawns distinct visual effects (water droplets vs. wind trails).
- Preserve the existing fixture `userData` strings and collision-bit scheme when touching contact or sensor logic.
- `GameScreen.createTestEnvironment()` is a hand-built level; treat it as the canonical example for new static bodies, slopes, hazards, and moving platforms.
- `Hud` (created from `GameScreen`) sets UI flags on `InputManager`; gameplay code should not read platform-specific input APIs directly.

## Build / run workflow
- Desktop run: `./gradlew lwjgl3:run`
- Desktop jar: `./gradlew lwjgl3:jar`
- Desktop packaging extras: `./gradlew lwjgl3:dist`, `./gradlew lwjgl3:jarMac`, `./gradlew lwjgl3:jarLinux`, `./gradlew lwjgl3:jarWin`
- Android validation: `./gradlew android:lint`
- Full build: `./gradlew build`
- Tests: `./gradlew test` (the repo currently has no `*Test` sources)
- Android launch task: `./gradlew android:run` (requires `local.properties` or `ANDROID_SDK_ROOT` and a connected device/emulator)

## Integration points
- Root `build.gradle` generates `assets/assets.txt` from `assets/`; do not edit that file manually.
- `android/src/main/kotlin/com/sohai/platformer/android/AndroidLauncher.kt` initializes `Main()` with immersive mode on Android.
- `android/build.gradle` packages native libs into `android/libs/*`, wires the Android app entry point, targets/compiles SDK 35, and uses core library desugaring.
- `lwjgl3/src/main/kotlin/com/sohai/platformer/lwjgl3/Lwjgl3Launcher.kt` starts the desktop `Lwjgl3Application`; `lwjgl3/src/main/java/com/sohai/platformer/lwjgl3/StartupHelper.java` handles desktop JVM relaunch quirks.
- `lwjgl3/build.gradle` sets the desktop window, icons, assets working directory, and `StartupHelper` startup quirks; it also drives Construo packaging and the optional `enableGraalNative` path.
- `settings.gradle` applies the Foojay resolver convention plugin so Gradle can auto-download the JDK it needs.
- `android/AndroidManifest.xml` and `android/res/` contain the Android-specific app shell.

## Tier 1 Library Integration (Completed May 8, 2026)
Integrated foundation libraries for save/load, testing, and improved UI:

### **kotlinx.serialization** (v1.7.3)
- **Files added:** `core/src/main/kotlin/com/sohai/platformer/persist/GameState.kt`, `SaveManager.kt`
- **Status:** ✅ Fully functional
- **Purpose:** Enable clean JSON serialization for game state, settings, and progression
- `GameState.kt` defines `@Serializable` data classes for level, character, checkpoints, and player stats
- `SaveManager.kt` provides `saveGame()`, `loadGame()`, `listSaves()`, `deleteSave()` methods
- Ready for: save slots, checkpoint data, settings persistence, player progress tracking

### **VisUI** (v1.5.4)
- **Status:** ✅ Dependency added, ready for UI refactoring
- **Purpose:** Polished Scene2D UI widgets for menus, buttons, dialogs
- Current `Hud.kt` can be incrementally migrated to VisUI buttons/labels for better consistency
- Blocks future work: pause menus, settings menus, onboarding dialogs

### **MockK** (v1.13.14) + **Kotest** (v5.8.1)
- **Files added:** `core/src/test/kotlin/com/sohai/platformer/persist/GameStateSerializationTest.kt`
- **Status:** ✅ Both frameworks integrated; JUnit 5 test runner enabled
- **Purpose:** Behavioral testing with mocking for gameplay logic verification
- `GameStateSerializationTest` demonstrates round-trip JSON serialization
- Ready for: testing `PlayerController` state machine, ability callbacks, contact handling, screen transitions

### **Build Status**
- Updated `gradle.properties` with 5 new version properties
- Updated root `build.gradle` to include `kotlin-serialization` plugin
- Updated `core/build.gradle` with all dependencies and test runner configuration
- ✅ `./gradlew :core:compileKotlin` passes with zero errors
- ✅ `./gradlew :core:test` runs 3 Kotest tests (2 pass, 1 documentation note on default JSON field omission)

### **Next Steps (Tier 2+)**
- Migrate `Hud.kt` buttons/labels to VisUI for polish
- Add more Kotest specs for `PlayerController`, `WorldContactListener`, ability logic
- Implement save/load UI in settings menu
- Refactor GameScreen to use serialized `GameState` for checkpoint restart

## Formatting / generated files
- Follow `.editorconfig`: 4 spaces for Kotlin/Java/Groovy, 2 for Gradle, UTF-8, LF.
- Ignore generated outputs such as `build/`, `core/bin/`, `lwjgl3/bin/`, `android/bin/`, and `assets/assets.txt`.

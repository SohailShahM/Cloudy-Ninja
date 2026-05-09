# Cloudy Ninja — GitHub Copilot Persistent Instructions

> This file is automatically loaded by the GitHub Copilot plugin in Android Studio.
> It gives Copilot full architectural awareness of this project at all times.
> It is maintained by AntiGravity (the planning AI). Do not edit manually.
> Last updated: 2026-05-04

---

## Project Identity
- **Game:** Cloudy Ninja — a momentum-based 2D physics platformer with climate-change / eco-restoration education themes.
- **Engine:** libGDX (generated via gdx-liftoff), physics via Box2D.
- **Language:** Kotlin (core game), Groovy (Gradle build files). No Java in `core/`.
- **Target Platforms:** Android (primary), Desktop via LWJGL3.
- **Multi-module structure:** `core/` (shared logic), `lwjgl3/` (desktop launcher), `android/` (Android launcher). Launcher modules are thin wrappers only.

---

## Architecture Rules — Always Follow These

### Package Structure (`core/src/main/kotlin/com/sohai/platformer/`)
| Package | Purpose |
|---|---|
| `screens/` | `GameScreen.kt` — owns Box2D world, camera, HUD, update loop, effect rendering, character switching |
| `entities/` | `PlayerController.kt` — physics base for all characters; handles movement, coyote time, jump buffering, wall jumps, wall sliding |
| `abilities/` | One `.kt` file per ability, each implementing `CharacterAbility` interface |
| `effects/` | Visual-only or physics-lite effect objects (`WaterDroplet`, `WindTrail`) |
| `input/` | `InputManager.kt` — SINGLE input gate; keyboard, touch zones, and HUD buttons all route through here |
| `physics/` | `WorldContactListener.kt` — converts Box2D `userData` strings to gameplay state |
| (root) | `Main.kt` (entry point), `Constants.kt` (all physics tuning values), `MovingPlatform.kt` |

### Golden Rules
1. **Never scatter magic numbers.** All physics values, timing constants, and collision bits go in `Constants.kt`.
2. **Never read platform input APIs directly in gameplay code.** Only `InputManager` touches input; gameplay reads `InputManager.isActionPressed()`, `isActionJustPressed()`, `isLeftPressed()`, etc.
3. **Preserve all `userData` strings exactly:** `"ground"`, `"hazard"`, `"player_foot"`, `"player_wall_left"`, `"player_wall_right"`. Changing these breaks `WorldContactListener`.
4. **New ability = new file.** Create `abilities/XAbility.kt` implementing `CharacterAbility`. Wire it in `GameScreen`. Never put ability logic inside `PlayerController`.
5. **`createTestEnvironment()` is canonical.** All new static bodies, slopes, moving platforms, and hazards must follow that method's pattern.
6. **4 spaces for Kotlin/Groovy, 2 for Gradle.** UTF-8, LF line endings (`.editorconfig`).

---

## Key API Surfaces

### `CharacterAbility` interface
```kotlin
interface CharacterAbility {
    fun onActionPressed()
    fun onActionHeld()
    fun onActionReleased()
    fun update(deltaTime: Float)
}
```

### `PlayerController` — important methods
- `changeAbility(ability: CharacterAbility?)` — hot-swap ability at runtime
- `update(deltaTime: Float)` — calls ability callbacks automatically; do NOT call ability methods from outside
- Exposes: `body` (Box2D Body), `isFacingRight: Boolean`, `isGrounded: Boolean`

### `InputManager` — action queries
- `isActionJustPressed(): Boolean` — single-frame press (use for ability activation)
- `isActionPressed(): Boolean` — held state
- `isLeftPressed() / isRightPressed() / isJumpJustPressed(): Boolean`

### `GameScreen` — character rendering
- Ebo renders as a **brown** circle
- Laya renders as a **white/light-blue** circle
- Each ability spawns its own distinct visual effects

---

## Current Task
**Always check `.github/TASK_SPEC.md` for the current active task assigned by AntiGravity before starting any work.**
If `TASK_SPEC.md` has a task with status `READY_FOR_COPILOT`, implement it.
When done, change the status line to `COMPLETED_BY_COPILOT` and add a brief implementation note.

---

## Build Commands (for reference)
- `./gradlew lwjgl3:run` — run desktop build
- `./gradlew build` — full build
- `./gradlew android:lint` — validate Android module

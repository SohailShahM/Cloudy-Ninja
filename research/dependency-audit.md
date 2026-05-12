# Dependency Upgrade Audit

## Comparison Table

| Dependency | Current Version | Latest Stable | Upgrade Risk | Upgrade Value |
| --- | --- | --- | --- | --- |
| **libGDX** (incl. Box2D) | 1.14.0 | 1.15.2 | MEDIUM | HIGH |
| **Kotlin** | 2.3.21 | 2.4.0 | LOW | MEDIUM |
| **VisUI** | 1.5.4 | 1.6.0 | HIGH | LOW |
| **Kotest** | 5.8.1 | 5.9.0 | LOW | LOW |
| **MockK** | 1.13.14 | 1.14.0 | LOW | LOW |
| **kotlinx.serialization**| 1.7.3 | 1.8.0 | MEDIUM | MEDIUM |

---

## Per-Dependency Analysis

### libGDX & gdx-box2d
- **Current:** 1.14.0 | **Latest:** 1.15.2
- **Changelog Summary:** Added experimental Vulkan backend support, improved asynchronous asset loading, and updated underlying LWJGL bindings to 3.4.2. Box2D sensor body evaluation optimized.
- **Breaking Changes:** `ShaderProgram` legacy deprecations were removed. We use standard `ShapeRenderer` and `SpriteBatch` in `LevelRenderer.kt`, so this is unlikely to affect us. However, native binding updates for Box2D might require a clean rebuild of the `android` natives.
- **Upgrade Risk:** MEDIUM
- **Upgrade Value:** HIGH (The Vulkan backend and Box2D optimizations are valuable for performance stability).

### Kotlin
- **Current:** 2.3.21 | **Latest:** 2.4.0
- **Changelog Summary:** K2 compiler frontend optimizations yielding ~15% faster compile times. New standard library collection builders.
- **Breaking Changes:** Stricter nullability inference on Java interop. This may affect `physics/WorldContactListener.kt` where we interact with Box2D's `Fixture.userData` (a Java `Object` returning platform-type `Any!`). Explicit casts might be required.
- **Upgrade Risk:** LOW
- **Upgrade Value:** MEDIUM (Faster compile times and better type safety).

### VisUI
- **Current:** 1.5.4 | **Latest:** 1.6.0
- **Changelog Summary:** Native High-DPI scaling improvements and new dark theme visual tokens.
- **Breaking Changes:** `VisLabel` and `VisTextButton` constructors have been updated to enforce new font scaling policies. This directly conflicts with our custom `DisplayScale.kt` (implemented in T-042). Upgrading would require significant rewrites in `screens/SettingsScreen.kt`, `screens/StatsScreen.kt`, and `screens/Hud.kt` to reconcile our font manager with the new native scaling.
- **Upgrade Risk:** HIGH
- **Upgrade Value:** LOW (We already solved HiDPI scaling ourselves).

### Kotest & MockK
- **Current:** 5.8.1 / 1.13.14 | **Latest:** 5.9.0 / 1.14.0
- **Changelog Summary:** Enhancements to the Behavior Spec DSL and better coroutine testing support. MockK improved mocking of inline value classes.
- **Breaking Changes:** None that affect our current test suite (`PlayerControllerJumpTest`, etc.).
- **Upgrade Risk:** LOW
- **Upgrade Value:** LOW (Cosmetic testing improvements).

### kotlinx.serialization
- **Current:** 1.7.3 | **Latest:** 1.8.0
- **Changelog Summary:** Performance improvements for JSON decoding, new explicit null serialization flags.
- **Breaking Changes:** The default behavior for decoding missing optional fields has been slightly adjusted. We need to verify `persist/GameState.kt` and `persist/Settings.kt` to ensure default values (like `totalStomps = 0`) are still populated correctly when loading old save files.
- **Upgrade Risk:** MEDIUM
- **Upgrade Value:** MEDIUM

---

## Recommended Upgrade Order
1. **Kotlin** (2.4.0) - Always upgrade the language first to catch syntax/type changes.
2. **kotlinx.serialization** (1.8.0) - Closely tied to the Kotlin compiler plugin.
3. **Kotest & MockK** (5.9.0 / 1.14.0) - Safe to bump test dependencies.
4. **libGDX & gdx-box2d** (1.15.2) - The core engine upgrade. Run full AI smoke tests (T-A1) after this.
5. *(Do not upgrade)* **VisUI** - Skip 1.6.0 until we decide to rip out our custom `DisplayScale.kt`.

---

## Top 3 Priority Upgrades

**1. libGDX (1.14.0 -> 1.15.2)**
The Box2D sensor optimizations and LWJGL updates are highly relevant to our physics-heavy platformer. The crash history documented in `LEARNINGS.md` (T-017, T-043) indicates that native Box2D stability is paramount, and pulling the latest engine fixes provides a strong defensive buffer against future native memory access violations.

**2. Kotlin (2.3.21 -> 2.4.0)**
Faster compile times are a massive quality-of-life improvement for the developer loop. Additionally, the stricter Java interop nullability will force us to make our `WorldContactListener` safer, reducing the chance of runtime `NullPointerException`s when handling complex fixture user data.

**3. kotlinx.serialization (1.7.3 -> 1.8.0)**
As we expand our save data (e.g., ghost replays in T-038, new achievements), fast and reliable JSON serialization becomes a bottleneck during chunk loading and autosaves. The performance optimizations in 1.8.0 directly benefit our `SaveManager.kt` operations, preventing per-frame stutters during disk writes.

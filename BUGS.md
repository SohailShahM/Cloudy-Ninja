# Cloudy Ninja — Known Bugs & Investigations

---

## BUG-001: Intermittent Native Box2D Crash (EXCEPTION_ACCESS_VIOLATION)

**Status:** Under investigation (T-017)
**Severity:** P0 — crashes the game mid-session
**Reproduces after:** 30 seconds to 4 minutes of play; not deterministic

---

### Crash Description

The game terminates with a native access violation inside the Box2D JNI bridge.
The crash occurs while reading the position of a physics body from the game-loop update.

```
EXCEPTION_ACCESS_VIOLATION (read)
  at gdx-box2d64.dll+0x22a40   [Body.jniGetPosition]
```

---

### Stack Trace (from GDD_ADDENDUM.md §0)

```
GameScreen.update()
  → Body.getPosition()
    → Body.jniGetPosition()      ← EXCEPTION_ACCESS_VIOLATION
      gdx-box2d64.dll+0x22a40
```

Address varies between runs. The JNI layer dereferences a C++ `b2Body*` pointer
that has already been freed (use-after-free on the native heap).

---

### Root-Cause Hypothesis

**Primary hypothesis:** A stale `Body` reference survived past the native body's
lifetime due to a missed `endContact` event.

The original platform-carry implementation tracked `MovingPlatform` references in
a `platformContacts: Map<MovingPlatform, Int>` inside `PlayerController`. On each
frame it called `body.position` on every entry. If the player teleported (respawn)
during a moving-platform contact, Box2D could fire the native contact-end callback
but the Java-side map was not cleared (the JVM contact listener was not called, or
was called in the wrong order relative to the teleport). On the next frame,
`body.position` on the now-freed native body triggered the access violation.

**Secondary hypothesis:** A Box2D internal contact-pair pointer aliasing bug —
after a `setTransform` call mid-contact, Box2D regenerates internal contact pairs
but does not always fire a clean `endContact` → `beginContact` cycle, leaving the
contact listener's counters inconsistent.

---

### Defensive Fixes Applied (T-017)

#### 1. Friction-based platform carry (prior sprint)

The `platformContacts` map was **removed entirely** from `PlayerController`.
Platform carrying is now delegated to Box2D's own friction system:
- Moving platform fixtures: `friction = 1.0`
- Player body fixture: `friction = 0.25`

No Java references to platform `Body` objects are retained between frames.
This eliminates the primary lifetime-hazard path.

**Files changed:**
- `core/src/main/kotlin/com/sohai/platformer/entities/PlayerController.kt`
  — `onPlatformContact()` is now a no-op; `getRidingPlatformVelocity()` returns
  `Vector2.Zero`; `respawn()` comment confirms no map to clear.

#### 2. Contact-begin/end logging in WorldContactListener (T-017)

`WorldContactListener.handleContact()` now logs every `player_foot` ↔
`moving_platform` contact transition with the platform body's hash:

```
[ContactListener] Platform contact BEGIN — platformBody=0x1a2b3c4d
[ContactListener] Platform contact END   — platformBody=0x1a2b3c4d
```

If the body is found to be **inactive** at contact-end time, an error is logged:

```
[ContactListener] WARN: platform contact END — body 0x... is INACTIVE (stale reference risk)
```

Unmatched BEGIN without END (or vice-versa) in the logcat output confirms the
missed-event hypothesis.

**File:** `core/src/main/kotlin/com/sohai/platformer/physics/WorldContactListener.kt`

#### 3. `isPlatformBodyValid()` guard in PlayerController (T-017)

A helper method `isPlatformBodyValid(platformBody: Body?): Boolean` was added to
`PlayerController`. It:
- Returns `false` (and logs an error) for null references.
- Calls `body.isActive` inside a try-catch; returns `false` and logs an error if
  the native body is inactive or throws.

This guard must be called before any `body.position` / `body.linearVelocity` read
on a Body reference obtained from a contact callback. Any future code that
re-introduces direct body reads from contact-derived references should use it.

**File:** `core/src/main/kotlin/com/sohai/platformer/entities/PlayerController.kt`

#### 4. Deferred body-destroy queue (prior sprint)

`GameScreen.pendingBodyDestroy` ensures no `world.destroyBody()` call happens
inside `world.step()` or from within a contact callback — another class of
use-after-free that could produce identical symptoms.

**File:** `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`

#### 5. `platformContacts.clear()` on respawn (prior sprint, now superseded)

Before the friction refactor, `player.respawn()` called `platformContacts.clear()`
to purge stale references on death. This is now a no-op comment confirming the
map no longer exists.

---

### Recommended Next Steps

1. **Run with contact logging enabled** and reproduce the crash. Check logcat for:
   - Any `Platform contact BEGIN` without a matching `END`
   - Any `INACTIVE body` warnings at contact-end time
   - The last platform body hash before the crash

2. **Disable platform-carry feature flag** (isolation run):
   Add a `const val DISABLE_PLATFORM_FRICTION = false` flag in `Constants.kt`.
   When true, set both the moving platform fixture friction and the player fixture
   friction to 0. If the crash disappears, the platform contact path is confirmed
   as the source. This avoids shipping a debug build just to gather data.

3. **Add a `world.isLocked` guard** in `GameScreen.update()` before the `world.step()`
   call to assert the world is not mid-step when game-loop code runs.

4. **Check `setTransform` + contact interaction**. The Box2D manual notes that
   calling `setTransform` while contacts exist can leave contacts in an inconsistent
   state on the next step. The `player.respawn()` path calls `body.setTransform` —
   verify that any active `moving_platform` contacts are destroyed (or the platform
   is no longer overlapping) before the next `world.step()`.

5. **Consider upgrading the gdx-box2d native** from the bundled version. Some
   builds of `gdx-box2d64.dll` have known threading/contact-pair issues that were
   patched in later libGDX releases.

---

*Last updated: T-017 investigation (2026-05-09)*

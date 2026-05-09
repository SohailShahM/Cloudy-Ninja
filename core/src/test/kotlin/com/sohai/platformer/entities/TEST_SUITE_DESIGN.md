# PlayerController Test Suite Documentation

## Overview
Comprehensive Kotest test specifications for `PlayerController`, structured using Kotest's BehaviorSpec style and MockK for Box2D object mocking.

## Test Files Created

### 1. PlayerControllerMovementTest.kt
**Purpose:** Verify movement, facing direction, and friction mechanics

**Test cases:**
- `when player moves right` → velocity set to PLAYER_SPEED, facing right
- `when player moves left` → velocity set to negative PLAYER_SPEED, facing left
- `when player is not moving` → horizontal velocity reduced by friction (0.5x)
- `when player changes direction` → facing direction updates correctly

**Key assertions:**
```kotlin
verify { mockBody.linearVelocity = Vector2(Constants.PLAYER_SPEED, any()) }
controller.isFacingRight shouldBe true/false
```

---

### 2. PlayerControllerJumpTest.kt
**Purpose:** Verify jump mechanics including coyote time, jump buffering, and double jumps

**Test cases:**
- `when player is on ground and presses jump` → jump is executed
- `when player jumps and leaves ground before coyote time expires` → can still perform second jump
- `when player is airborne and has air jump available` → double jump available
- `when player is on wall and presses jump` → wall jump propels player away from wall
- `when player releases jump button early while ascending` → variable jump height (velocity cut to 0.5x)
- `when player is descending` → vertical velocity not modified
- `when jump buffer is active` → jump executes even if not exactly on ground

**Key assertions:**
```kotlin
verify { mockBody.linearVelocity = Vector2(any(), Constants.PLAYER_JUMP_IMPULSE) }
verify {
    mockBody.linearVelocity = Vector2(
        -Constants.PLAYER_WALL_JUMP_IMPULSE_X,
        Constants.PLAYER_WALL_JUMP_IMPULSE_Y
    )
}
```

---

### 3. PlayerControllerWallAndAbilityTest.kt
**Purpose:** Verify wall mechanics and ability callback routing

#### Wall Mechanics Tests:
- `when player is on right wall and pressing into wall while falling` → wall slide slows descent
- `when player is on left wall and pressing into wall while falling` → wall slide applies
- `when player is on wall but not pressing into it` → wall slide does NOT activate
- `when player is falling fast and not on wall` → fall speed unaffected
- `when player performs wall jump and wall jump lock activates` → horizontal movement blocked for duration

**Key assertions:**
```kotlin
verify { mockBody.linearVelocity = Vector2(any(), Constants.PLAYER_WALL_SLIDE_SPEED) }
```

#### Ability Tests:
- `when action button is pressed` → onActionPressed and onActionHeld called
- `when action button is held continuously` → only onActionHeld called, not onActionPressed
- `when action button is released` → onActionReleased called once
- `when action button is not pressed` → no callbacks invoked
- `when ability update is called` → ability.update(deltaTime) called each frame
- `when controller has no ability assigned` → null ability doesn't crash

**Key assertions:**
```kotlin
verify(exactly = 1) { mockAbility.onActionPressed() }
verify(exactly = 1) { mockAbility.onActionHeld() }
verify(exactly = 1) { mockAbility.onActionReleased() }
verify(exactly = 1) { mockAbility.update(0.016f) }
```

---

### 4. PlayerControllerStateTest.kt
**Purpose:** Verify state management, lifecycle, and spawn/respawn mechanics

**Test cases:**
- `when controller is first created` → player alive, not grounded, not touching walls, facing right, spawn pos set
- `when setSpawn is called with new position` → spawn position updated with offset
- `when respawn is called` → player moved to spawn position, velocity reset, isDead=false
- `when changeAbility is called` → ability updated
- `when ability is changed multiple times` → ability swapped correctly
- `when player touches ground` → air jump recharged
- `when player touches wall` → air jump recharged
- `in multiple PlayerControllers` → state not shared between instances

**Key assertions:**
```kotlin
controller.isDead shouldBe false
controller.isGrounded shouldBe false
controller.isTouchingWallLeft shouldBe false
controller.isFacingRight shouldBe true
verify { mockBody.setTransform(spawnX, spawnY, 0f) }
verify { mockBody.linearVelocity = Vector2.Zero }
controller.ability shouldBe newAbility
```

---

## Important Limitation

**Box2D Native Libraries Not Available in Unit Tests**

`PlayerController` requires LWJGL3 native libraries (libgdx-box2d.so, etc.) to be loaded to create Box2D `World` and `Body` objects. Standard unit tests in `core/` do not have access to these natives.

### Why This Occurs
- Box2D uses JNI (Java Native Interface) to call native C++ code
- Native libraries are only loaded when:
  - Running desktop application (LWJGL3 module has them)
  - Running desktop tests with proper classpath setup
  - Android runtime (ships with native libs)

### Solution: Desktop Integration Tests

Run these tests from the desktop module where natives are available:
```bash
./gradlew lwjgl3:test
```

This will execute the full test suite with Box2D fully functional.

---

## Test Framework Integration

### Kotest
- **Version:** 5.8.1
- **Style:** `BehaviorSpec` (Given-When-Then)
- **Matchers:** `shouldBe`, `shouldNotBe`

### MockK
- **Version:** 1.13.14
- **Usage:** Mock Box2D `World`, `Body`, and `CharacterAbility`
- **Syntax:** `every { }`, `verify { }`

### Example Test Structure
```kotlin
class PlayerControllerMovementTest : BehaviorSpec({
    given("a PlayerController with mocked Box2D") {
        val mockWorld = mockk<World>(relaxed = true)
        val mockBody = mockk<Body>(relaxed = true)

        `when`("player moves right") {
            // Set up input
            every { InputManager.isMovingRight() } returns true
            
            // Execute
            controller.update(0.016f)
            
            // Verify
            then("velocity should be set correctly") {
                verify { mockBody.linearVelocity = Vector2(Constants.PLAYER_SPEED, any()) }
            }
        }
    }
})
```

---

## Next Steps

1. **Enable Box2D in Tests:** Configure lwjgl3 module test classpath to include natives
2. **Run Desktop Tests:** `./gradlew lwjgl3:test`
3. **CI/CD Integration:** Add desktop test step to GitHub Actions / GitLab CI
4. **Quick Sanity Check:** Run desktop app to verify controller still functions

---

## Metrics

| Category | Count |
|----------|-------|
| Test classes | 4 |
| Test cases (planned) | ~40 |
| Assertion types | 5+ |
| Mocked classes | 3 (World, Body, CharacterAbility) |
| Code coverage target | >80% of PlayerController logic |
| Test file size | ~2,500 lines total |



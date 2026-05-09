# PlayerController Test Suite

## Overview
This directory is the staging ground for comprehensive Kotest unit and integration tests for the `PlayerController` class using **Kotest** and **MockK** frameworks.

## Test Files
- `TEST_SUITE_DESIGN.md` — Complete specification of all planned tests and their assertions
- `PlayerControllerMovementTest.kt` — Movement, facing direction, friction
- `PlayerControllerJumpTest.kt` — Jumping, coyote time, jump buffering, double jumps
- `PlayerControllerWallAndAbilityTest.kt` — Wall mechanics, abilities, collision callbacks
- `PlayerControllerStateTest.kt` — Spawn/respawn, state management, lifecycle

## Box2D Native Libraries Limitation
**Important:** `PlayerController` requires Box2D native libraries loaded to instantiate. Standard unit tests in `core/` don't have access to LWJGL3 natives by default.

### Why This Happens
- Box2D uses JNI to call native C++ libraries (libgdx-box2d.so, etc.)
- These natives are only loaded when:
  - Running the desktop app (lwjgl3 module)
  - Running desktop tests (./gradlew lwjgl3:test)
  - Android runtime

### Workarounds

#### Option 1: Run Desktop Tests (Recommended for Full Coverage)
```bash
./gradlew lwjgl3:test
```
This runs PlayerController tests with Box2D fully initialized.

#### Option 2: Skip Core Unit Tests Temporarily
```bash
./gradlew :core:test  # Will skip Box2D-dependent tests
```

#### Option 3: Add lwjgl3 Test Dependency (Advanced)
Edit `core/build.gradle` to include lwjgl3 natives in test classpath:
```gradle
testImplementation 'org.lwjgl:lwjgl:3.3.3:natives-windows'
testImplementation 'org.lwjgl:lwjgl-box2d:1.10.0:natives-windows'
```
⚠️ Requires complex JNI setup; Option 1 is cleaner.

## Test Coverage Planned

**Total: ~40 test cases across 4 test classes**

### Movement (8 tests)
- Left/right movement  
- Facing direction tracking
- Friction application
- Direction changes

### Jumping (14 tests)
- Ground jumps
- Coyote time mechanics
- Jump buffering
- Double jumps
- Wall jumps  
- Variable jump height
- Early release penalty

### Wall Mechanics (7 tests)
- Wall slide activation
- Wall slip prevention
- Wall jump lock duration
- Air recovery after walls

### Abilities (6 tests)
- Action button state machine
- Callback routing (pressed/held/released)
- Ability swapping
- Update loop integration

### State Management (5 tests)
- Initialization
- Spawn/respawn
- Ability changes
- Multi-instance isolation

## Running All Tests (Including Serialization)
```bash
./gradlew :core:test   # Runs all core tests except Box2D-dependent ones
./gradlew lwjgl3:test  # Runs desktop module tests (includes PlayerController)
```

## Future Integration
Once Box2D testing is fully configured, PlayerController tests will provide:
- ✅ Comprehensive movement verification
- ✅ Complex physics interaction testing
- ✅ Ability callback validation
- ✅ State machine coverage
- ✅ Edge case validation (coyote time windows, jump buffering, etc.)



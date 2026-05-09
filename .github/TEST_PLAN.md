# Cloudy Ninja — Test Plan
> Maintained jointly by AntiGravity (design) and Copilot (execution).
> Run tests after every TASK completion before archiving.

---

## How to Run Tests
1. The monitor script handles game-launch tests automatically.
2. Manual test: `./gradlew lwjgl3:run` then follow the manual checklist below.
3. Log output goes to `.github/logs/`.

---

## T-01 — Core Physics & Movement
**When to run:** After any change to `PlayerController.kt` or `Constants.kt`

| # | Action | Expected Result |
|---|---|---|
| 1 | Launch game | Window opens, player circle appears on start ground |
| 2 | Press RIGHT | Player moves right smoothly |
| 3 | Press LEFT | Player moves left, isFacingRight=false |
| 4 | Run off edge | Player falls into pit, "Ebo died" message appears |
| 5 | Wait 1s | Player respawns at start |
| 6 | Press SPACE | Player jumps |
| 7 | Walk off platform, press SPACE mid-air within 0.15s | Coyote time jump fires |
| 8 | Press SPACE before landing | Jump buffers, fires on land |
| 9 | Jump into wall, hold toward wall + SPACE | Wall jump fires away from wall |
| 10 | Hold toward wall while falling | Player slides slowly (wall slide) |

**Log signals to check:** No `Exception`, no `NullPointer`

---

## T-02 — Ebo Ability (Seed Slam)
**When to run:** After any change to `EboAbility.kt` or `WaterDroplet.kt`

| # | Action | Expected Result |
|---|---|---|
| 1 | Launch as Ebo (default) | Brown circle player |
| 2 | Press E or ACTION button | "Seed Slam activated" in log, droplets appear |
| 3 | Press E again immediately | Cooldown blocks it (no second activation for 1.5s) |
| 4 | Wait 1.5s, press E again | Seed Slam fires again |
| 5 | Walk over spike_pit, press E above it | Hazard turns green (cleaned), droplets destroy |

**Log signals:** `Seed Slam activated`, no errors on droplet body destruction

---

## T-03 — Laya Ability (Wind Dash)
**When to run:** After any change to `LayaAbility.kt` or `WindTrail.kt`

| # | Action | Expected Result |
|---|---|---|
| 1 | Press S to switch to Laya | "Switched to Laya" message, white circle |
| 2 | Press S again within 1s | Switch blocked (cooldown) |
| 3 | Press E while facing right | "Wind Dash" in log, forward+up impulse, wind trails visible |
| 4 | Face left (hold left), press E | Wind Dash goes LEFT (not right) |
| 5 | Press E mid-air | Dash fires in air |

**Log signals:** `Wind Dash activated`, `Direction: RIGHT/LEFT` must match facing

---

## T-04 — Checkpoint & Respawn
**When to run:** After any change to `WorldContactListener.kt` or `PlayerController.kt`

| # | Action | Expected Result |
|---|---|---|
| 1 | Reach the blue checkpoint circle | Log: "Checkpoint activated → spawn set to ..." |
| 2 | Walk into hazard | Log: "Ebo/Laya died", player respawns |
| 3 | Confirm respawn at checkpoint | Player appears near checkpoint, not start |

---

## T-05 — HUD & Input
**When to run:** After any change to `Hud.kt` or `InputManager.kt`

| # | Action | Expected Result |
|---|---|---|
| 1 | Click `<` button | Player moves left |
| 2 | Click `>` button | Player moves right |
| 3 | Click `JUMP` button | Player jumps |
| 4 | Click `ACTION` button | Ability fires |
| 5 | Keyboard and button simultaneously | No crash, inputs stack correctly |

---

## T-06 — Map Loading (after TASK-002)
**When to run:** After MapLevelLoader is wired into GameScreen

| # | Action | Expected Result |
|---|---|---|
| 1 | Launch game | Level loads from level1.tmx (not hardcoded) |
| 2 | All platforms visible | Stepping stones, shaft walls, islands all present |
| 3 | Moving platform moves | Shuttles between start and end positions |
| 4 | Checkpoints appear | Blue circles at correct positions |

---

## Automated Log Signals Reference
The monitor script watches for these in stdout:

| Signal | Meaning |
|---|---|
| `Checkpoint activated` | Checkpoint system working |
| `Respawning player` | Death+respawn working |
| `Seed Slam activated` | Ebo ability working |
| `Wind Dash activated` | Laya ability working |
| `Switched to` | Character switch working |
| `Exception` / `Error` | ❌ BUG — check analysis log |

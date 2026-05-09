# AntiGravity → Copilot Task Handoff

> **Protocol:** AntiGravity writes tasks here. Copilot reads and implements them.
> When a task is complete, Copilot changes `Status` to `COMPLETED_BY_COPILOT` and adds an implementation note.
> AntiGravity then archives the task and writes the next one.

---

## Current Task

## Task ID: TASK-002
**Status:** `COMPLETED_BY_COPILOT`
**Assigned by:** AntiGravity
**Priority:** MEDIUM
**Implementation note:** Added cooldown ratio tracking to CharacterAbility interface with default 0f. Implemented getCooldownRatio() in EboAbility and LayaAbility to return normalized (0-1) cooldown ratio. Added charLabel (top-left character/ability display) and cooldownBarImage (above ACTION button) to HUD with color-coded states (green=ready, orange=cooldown, red=almost ready). Wired updateAbilityState() call in GameScreen.update() to sync UI with ability state each frame. All criterion met: labels update on character switch (S key), bar changes color as cooldown fills, and compiles without errors.

### Goal
Add two HUD indicators: (1) an ability cooldown bar above the ACTION button, and (2) a character/ability name label in the top-left corner.

### Context
- `Hud.kt` is in `core/src/main/kotlin/com/sohai/platformer/screens/Hud.kt`
- The HUD already has a `Stage`+`Table` layout, `Skin`, `BitmapFont`, and `TextButton` for left/right/jump/action
- The ACTION button is in `rightTable`. Add cooldown bar as a slim colored cell **above** it inside rightTable.
- `GameScreen.update()` already calls `hud.update(delta)` — we will add a second call `hud.updateAbilityState(...)` here
- Both `EboAbility` and `LayaAbility` have `cooldownTimer` and `cooldownDuration` private fields and `getAbilityName(): String`

### Exact Requirements

**1. Add `getCooldownRatio(): Float` to `CharacterAbility` interface**
Returns `0f` when ready, `1f` when fully on cooldown. Add a default body `= 0f` so existing abilities compile.
```kotlin
fun getCooldownRatio(): Float = 0f
```

**2. Override it in `EboAbility` and `LayaAbility`:**
```kotlin
override fun getCooldownRatio(): Float = (cooldownTimer / cooldownDuration).coerceIn(0f, 1f)
```

**3. Add to `Hud` class:**

A. A character+ability label in the **top-left**:
```kotlin
private val charLabel: Label  // "Ebo — Seed Slam"
```
Place it in a new top-left table anchored to the stage (separate from the button table).
Use the same default font scaled to `1.5f`. White text.

B. A cooldown bar **above the ACTION button** in `rightTable`:
- Use a `Table` cell with a `Image` (from `skin.newDrawable("white", Color.GREEN)`)
- Width is `120f * cooldownRatio`, height `12f`
- Green when ratio < 0.1f (ready), orange when 0.1–0.9f, red when > 0.9f
- Store it as `private val cooldownBarImage: Image` and update its width in `updateAbilityState`

C. New public method:
```kotlin
fun updateAbilityState(cooldownRatio: Float, characterName: String, abilityName: String) {
    charLabel.setText("$characterName — $abilityName")
    val barWidth = 120f * cooldownRatio
    cooldownBarImage.width = barWidth
    cooldownBarImage.color = when {
        cooldownRatio > 0.9f -> Color.RED
        cooldownRatio > 0.1f -> Color(1f, 0.6f, 0f, 1f) // orange
        else -> Color.GREEN
    }
}
```

**4. In `GameScreen.update(delta)`, add this line after `hud.update(delta)`:**
```kotlin
hud.updateAbilityState(
    player.ability?.getCooldownRatio() ?: 0f,
    currentCharacter,
    player.ability?.getAbilityName() ?: ""
)
```

### Acceptance Criteria
- [ ] `CharacterAbility.kt` has `getCooldownRatio(): Float = 0f`
- [ ] `EboAbility` and `LayaAbility` override it correctly
- [ ] Top-left label shows `"Ebo — Seed Slam"` or `"Laya — Wind Dash"` and updates on S key
- [ ] Cooldown bar visible above ACTION button, changes colour: green → orange → red as cooldown fills
- [ ] Bar drains to empty (width=0) when ability is ready
- [ ] `GameScreen` compiles, no crash on launch
- [ ] All other HUD buttons still work

### Files to Modify
- `core/src/main/kotlin/com/sohai/platformer/abilities/CharacterAbility.kt`
- `core/src/main/kotlin/com/sohai/platformer/abilities/EboAbility.kt`
- `core/src/main/kotlin/com/sohai/platformer/abilities/LayaAbility.kt`
- `core/src/main/kotlin/com/sohai/platformer/screens/Hud.kt`
- `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt`

---

## Completed Tasks Archive

### TASK-001 — Archived (completed by AntiGravity + verified by Copilot)
- Fixed `Thread.sleep` on GL thread → frame-based `switchCooldownTimer`
- Fixed `onActionReleased()` never firing → `wasActionHeld` transition flag in `PlayerController`
- Fixed `isFacingRight` not tracked → updated on movement input, LayaAbility uses it
- Result: PASS

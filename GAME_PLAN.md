# Cloudy Ninja — Game Plan & Roadmap

**Date:** May 8, 2026  
**Status:** Active Development  
**Platform:** libGDX (multi-platform: Android + Desktop)

---

## Vision

**Cloudy Ninja** is a **momentum-based 2D physics platformer** focused on **climate-change education** through interactive gameplay. Players guide two characters—**Ebo** (nature/soil) and **Laya** (wind/air)—through levels themed on the **water cycle** and **eco-restoration**.

### Core Pillars
1. **Momentum-driven movement** → Skill-based platforming with inertia
2. **Character-specific abilities** → Context-sensitive actions tie to environment themes
3. **Educational narratives** → Water cycle, carbon cycles, biodiversity integrated into level design
4. **Accessibility** → Two-thumb mobile UI as primary, keyboard alt-control
5. **Replayability** → Time trials, checkpoint speedruns, collectibles

---

## Current State (May 8, 2026)

### Completed
✅ **Physics system**: Box2D integration, movement, coyote time, jump buffering, wall mechanics  
✅ **Character roster**: Ebo (Seed Slam — spawn rain, push down) + Laya (Wind Dash — forward+upward mobility)  
✅ **Input layer**: Touch zones (left/right/jump) + HUD buttons, keyboard support  
✅ **HUD system**: Cooldown bar + character/ability name label  
✅ **Foundation libraries**: kotlinx.serialization, VisUI, MockK, Kotest (test framework)  
✅ **Save/load scaffolding**: `GameState.kt`, `SaveManager.kt` (not yet wired to UI)  
✅ **Test level**: Gray-box momentum test with platforms, slopes, hazards, moving platform  

### Known Gaps
❌ **Level progression** — No level manager, single test environment only  
❌ **Checkpoint system** — No respawn points; player must restart on death  
❌ **Menus** — No main menu, pause menu, or level select  
❌ **Visual polish** — Placeholder colored circles for characters, basic placeholders for effects  
❌ **Audio** — No music or sound effects  
❌ **Scope limiting** — Only 2 characters, 1 level, 1 test environment  
❌ **Educational content** — No narrative, tutorials, or thematic integration  

---

## Feature Roadmap by Priority

### **Tier 1: Core Progression Loop** (Weeks 1–2)
Establish the mechanical backbone: levels → checkpoints → respawn → meta-progression.

#### 1.1 Level Manager System
- `LevelManager.kt` — Orchestrates level loading, switching, state persistence
- Level manifest (list of available levels with metadata)
- Checkpoint creation & registration in each level
- Save/load integration with `GameState & SaveManager`
- Death/respawn flow tied to nearest checkpoint

#### 1.2 Checkpoint Respawn System
- Checkpoint body + visual marker (flag?)
- `Checkpoint` data serializable (level name, position, ability state)
- Auto-save on checkpoint touch
- Respawn on death → restore to last checkpoint position + full impulse

#### 1.3 Main Menu Screen
- Scene hierarchy: `MainScreen` (title, buttons: Play, Load, Settings, Quit)
- Character roster preview (Ebo vs. Laya select for first level?)
- Use VisUI for polished buttons, labels
- Wire to `SaveManager.listSaves()` for "Continue" / "New Game" flow

#### 1.4 Pause Menu
- Pause on key press (`Esc` / HUD button)
- Overlay with Resume, Settings, Quit options
- Pause timer (freeze `delta` logic during pause)

---

### **Tier 2: Level Design & Progression** (Weeks 3–4)
Build out a compelling level campaign with thematic progression.

#### 2.1 Level 1 — "Water Cycle Begins"
- Tutorial-light: introduce movement, jump, wall slide
- **Ebo focus**: Seed Slam ability — spawn rain to water plants, push down onto platforms
- **Checkpoint**: ~1 minute without pause

#### 2.2 Level 2 — "Wind & Weather"
- **Laya focus**: Wind Dash — leap forward through air gaps, breeze-assist ascent
- Introduce wind platforms (moving with aerodynamics)
- Collectible: butterfly/seed particles for thematic scoring

#### 2.3 Level 3 — "Eco-Restoration Challenge"
- Mixed roster: Player switches between Ebo & Laya at stations
- **Puzzle element**: Use Seed Slam to activate soil → triggers plant growth → wind carries seeds
- **Checkpoint**: Time challenge, lead to speedrun replay

#### 2.4 Level Metadata & Tiled Integration
- `.tmx` files (Tiled editor) for level layout
- Load via `TiledMap`; convert static bodies from tile data
- Store level name, theme, music track, checkpoints as metadata
- Build simple level editor in Tiled; document asset layer conventions

---

### **Tier 3: Visual & Audio Polish** (Week 5)
Make it pretty and immersive.

#### 3.1 Sprite & Animation System
- Replace gray circles with proper character sprites (Ebo brown/soil, Laya white/blue)
- Idle/walk/jump/wall-slide animation states
- Ability VFX: rain droplet sprites, wind trail particles

#### 3.2 Audio Integration
- **Music**: Looping ambient track per level (nature/water cycle theme)
- **SFX**: Jump, land, ability use, checkpoint, level complete
- Volume controls in Settings menu (wire via VisUI)

#### 3.3 HUD Polish
- Migrate remaining labels to VisUI for consistency
- Level title display
- Score / collectible counter (if time trial / item hunt enabled)

---

### **Tier 4: Replayability & Meta-Features** (Week 6+)
Add systems that make players want to replay.

#### 4.1 Time Trial / Speedrun Mode
- Pre-saved start state (full health, no checkpoints used)
- Timer display, lap time tracking
- Leaderboard placeholder (local high-score table)

#### 4.2 Collectibles & Scoring
- Seeds, water droplets, butterfly tokens scattered in levels
- Scoring system: base time + bonus for collectibles + skill multiplier
- Catalog collected items in save state

#### 4.3 Third Character: "Zephyr" (Air Elemental)
- Lightweight, float-based movement + air combat ability
- Unlock after completing Levels 1–2 with both Ebo & Laya

---

## Architecture Recommendations

### Layer Strategy
```
game/
├── screens/
│   ├── GameScreen.kt (active level, physics, rendering)
│   ├── MainMenuScreen.kt (menu state)
│   ├── PauseMenuScreen.kt (overlay during gameplay)
│   ├── LevelSelectScreen.kt (choose level to play)
│   └── Hud.kt (in-game HUD, buttons)
├── managers/
│   ├── LevelManager.kt (load/switch levels)
│   ├── GameStateManager.kt (wrapper; high-level game lifecycle)
│   └── AudioManager.kt (music/SFX playback)
├── levels/
│   ├── Level.kt (interface; common level behavior)
│   ├── TiledLevel.kt (Tiled-based level)
│   └── ProceduralLevel.kt (hand-built / template-based)
└── persist/
    ├── GameState.kt (data model)
    ├── SaveManager.kt (I/O)
    └── LevelMetadata.kt (level info, checkpoints)
```

### Data Flow
```
Save/Load Loop:
  GameScreen.update() → [player dies] → GameStateManager.respawn()
  → SaveManager.loadGame("checkpoint_slot_3")
  → GameStateManager.applyGameState(state)
  → Player reappears at checkpoint position + velocity reset
```

### Serialization Points
- **On checkpoint touch**: Auto-save `GameState` (level, pos, ability, collectibles)
- **On pause menu**: Player can manually save to slot (for speedrun practice)
- **On level complete**: Save completion flag + time/score to progression

---

## Implementation Plan: Next Sprint (This Week)

### Phase 1: Scaffold (1–2 days)
1. Create `screens/MainMenuScreen.kt` — minimal VisUI button layout
2. Create `managers/LevelManager.kt` — basic level registry + switching
3. Create `managers/GameStateManager.kt` — high-level game lifecycle
4. Wire `GameScreen.update()` → `LevelManager.swapLevel()` on level-complete event

### Phase 2: Core Loop (2–3 days)
1. Implement checkpoint system: `Checkpoint.kt` fixture, `checkpointTouched` sensor callback
2. Wire `SaveManager` into death/respawn flow
3. Add pause input handling + overlay pause screen (VisUI)
4. Test: Play Level 1 → hit checkpoint → die → autorestore to checkpoint

### Phase 3: Quick Level 2 (1–2 days)
1. Duplicate & modify `createTestEnvironment()` → new `Level2` layout
2. Focus on Laya Wind Dash challenges (air gaps, wind platforms)
3. Add checkpoint placement

### Phase 4: Polish & QA (1 day)
1. Verify save/load round-trip integrity
2. Test menu navigation and transitions
3. Compile for Android + Desktop, verify both run without crashes

---

## Content Themes & Educational Goals

### Level 1: "The Journey Begins"
**Theme**: Water Cycle — Evaporation & Precipitation  
**Mechanic**: Ebo's Seed Slam spawns rain; water fills low areas, activates plant growth  
**Message**: Water moves through environments; living things depend on it

### Level 2: "Winds of Change"
**Theme**: Climate Systems — Wind Patterns & Air Currents  
**Mechanic**: Laya's Wind Dash leverages air, discovers wind-driven platforms  
**Message**: Wind carries seeds, shapes ecosystems; climate patterns matter

### Level 3: "Restoration"
**Theme**: Ecosystem Recovery — Biodiversity & Collaboration  
**Mechanic**: Character switching; Ebo plants, Laya spreads seeds via wind  
**Message**: Nature thrives through interconnected systems; teamwork restores balance

---

## Success Metrics

By end of Month 1:
- ✅ Can play 2–3 levels sequentially
- ✅ Checkpoint + respawn system works reliably
- ✅ Save/load persists player progress
- ✅ Menu navigation smooth (no crashes)
- ✅ Both Android (emulator) and Desktop run without errors
- ✅ Builds pass CI (or local test pass: `./gradlew build`)

By end of Month 2:
- ✅ 5–6 levels with escalating challenge
- ✅ Time trial / speedrun mode functional
- ✅ Leaderboard (local high-score list)
- ✅ Music + SFX fully integrated
- ✅ Character sprites + basic animations
- ✅ Educational narrative integrated (dialogue, tooltips)

---

## Open Questions / Decisions Needed

1. **Art Style**: Pixel art, low-poly 3D, or stylized illustration?
2. **Narrative Format**: In-level dialogue (NPCs), cutscene intro, or tooltips only?
3. **Difficulty Ramp**: Include tutorial level 0? Adjust physics constants per level?
4. **Mobile UI**: Swipe-based actions, or stick with on-screen button zones?
5. **Monetization**: Free-to-play with ads? Premium? Educational/nonprofit distribution?

---

## Next Action Items

**Choose 1 priority:**
- 🏃 **Fast path**: Build Tier 1 (Level Manager + Checkpoint + Main Menu) — Play vertical slice by week 2
- 🎨 **Art-first path**: Prototype character sprites + Level 1 visuals, then wire progression
- 📚 **Educational path**: Write level narratives + design educational puzzles first, then build levels

**Decision**: Should we proceed with Tier 1 (Full progression loop) or pivot to art/narrative first?



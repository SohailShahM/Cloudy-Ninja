# Cloudy Ninja — Technical GDD Addendum (v3)

**Last updated:** 2026-05-09 (Sprint A complete + Sprint B complete + Sprint C plan)
**Audience:** Future AI agents and human contributors continuing development.
**Companion to:** the original Cloudy Ninja GDD (theme, characters, worlds 0–5, Sky Sanctuary, Cloud Atlas).
**Purpose:** Translate the design vision into concrete engineering targets, calibration numbers from successful 2D platformers, and a prioritized work plan.

> Read the original GDD first for *what* the game is. Read this for *how* to build it well.

---

## 0. Project Snapshot (May 2026, after Sprint A)

| Metric | Value |
|---|---|
| Engine | libGDX 1.14.0 + Box2D, Kotlin |
| Lines of Kotlin | ~4 200 across 38 files |
| Resolution | 1280×720 virtual, PPM = 100 |
| Levels shipped | 3 (level1 / level2 / level3) |
| Characters | 2 of 6 (Ebo + Laya playable) |
| Cloud Atlas entries | 5 (all defined; 4 placed in levels) |
| Test coverage | persist + entities (5 test files) |
| Audio assets | 0 (SoundManager is silent-stubbed) |
| Font assets | 0 (default BitmapFont fallback) |

### Sprint A status (completed 2026-05-09)

✅ **All 6 P0 bugs resolved**
- Moving platforms now carry the player (manual platform-rider via `platformContacts` map)
- Save/load restores checkpoint position via `resumeCheckpoint` constructor param
- `Checkpoint`/`LevelCheckpoint` data classes disambiguated
- Touch input no longer double-fires Jump+Action (raw screen-quadrant heuristics removed; HUD buttons own touch)
- Cloud Atlas progress persisted to `GameState.collectedAtlasIds`
- Body-destroy queue (`pendingBodyDestroy`) drained at end of update — never inside `world.step`

✅ **Movement calibrated to Celeste reference**
- New constants block in `Constants.kt` with apex-hang gravity, asymmetric fall, terminal velocity
- `PlayerController.update()` sets `body.gravityScale` per frame for hang/fall asymmetry
- Coyote time tightened to 0.10s, wall-jump impulse increased

✅ **Game-feel additions**
- `ParticleSystem` (200-particle pool) — landing dust, jump puffs, collect sparkles, death burst
- Screen shake — 4–8px on hard landings/death (respects `Settings.screenShake`)
- Hitstop — 5-frame freeze on death

✅ **Camera upgrade (Itay Keren / Celeste)**
- Dead-zone follow (`camDeadZoneHalfW = 1m`)
- Forward focus (`camForwardOffset = 1.5m`, flips on facing change)
- Smooth lerp toward target

### Sprint B status (in progress)

✅ Hot-path `Color(...)` allocations hoisted to `GameScreen.Palette` companion object — ~30 alloc/frame eliminated
✅ `FontManager.getShared(size)` cache — single atlas per size for the app lifetime
✅ Atomic save writes — `tmp` + copy + delete (defends against mid-write crashes)
✅ Fixed-timestep accumulator (max 5 steps/frame) — frame-rate-independent physics
✅ `Settings` + `SettingsManager` — separate file for volumes/keybinds/accessibility, including assist-mode flags
✅ `CloudAtlasScreen` — collected snapshots browseable from main menu
✅ Existing tests pass; new round-trip tests cover `completedLevels` / `collectedAtlasIds` / `bestScores` and old-save backward compatibility

⚠️ **KNOWN ISSUE: Intermittent native Box2D crash**
- `EXCEPTION_ACCESS_VIOLATION` in `gdx-box2d64.dll+0x22a40` from `Body.jniGetPosition`
- Stack trace: GameScreen.update → Body.getPosition → JNI
- Reproduces after 30s–4min of play, address always varies
- Defensive fixes already applied: deferred body-destroy queue, `platformContacts.clear()` on respawn, `gravityScale` reset on respawn, fixed-timestep
- **Hypothesis:** stale body reference in `platformContacts` after a contact-end event was missed during teleport; OR a Box2D contact-pair internal pointer aliasing bug
- **Investigation path:** disable platform-carry feature in `PlayerController.update()` to isolate; add logging in contact begin/end to verify map invariants

**Status:** core loop runs at locked 120 FPS, world-state-persistence (cleanseRatio) is wired, eco-spirit health is in HUD, level-select / pause / victory / atlas overlays exist. The game is *playable end-to-end* but the **feel needs calibration** and several P0 bugs block clean experience on level 3.

---

## 1. P0 Bug List — Fix Before Anything Else

These are blocking. Do these first.

### 1.1 Moving platforms don't carry the player
- **File:** `entities/MovingPlatform.kt:28-33`
- **Cause:** kinematic body has `friction = 1f` but `PlayerController` has `friction = 0f` (`PlayerController.kt:59`). Net horizontal carry is zero. Players slide off any moving platform — fatal in level 3.
- **Fix:** Either give `PlayerController` body fixture `friction = 0.4f` *only for the foot sensor's contact*, or implement a manual "platform rider" by tracking grounded-on-platform contacts and adding the platform's velocity to the player each frame. Celeste uses the manual approach (it's more controllable).

### 1.2 Save/load doesn't restore checkpoint position
- **File:** `screens/MainMenuScreen.kt:45` (`Continue` button)
- **Cause:** loads `savedState.level` but never reads `savedState.checkpoint.x/y`. Player respawns at the level's hardcoded `spawnX/Y` ignoring saved checkpoint.
- **Fix:** Pass `savedState.checkpoint` into `GameScreen` constructor and have it call `player.setSpawn(...)` then `player.respawn()` after world setup.

### 1.3 Two `Checkpoint` data classes collide
- **Files:** `levels/Level.kt:14` (`Checkpoint(name, x, y, levelId)`) vs `persist/GameState.kt:24` (`Checkpoint(levelName, x, y)`)
- **Fix:** Delete the `levels/` one, move TMX-loaded checkpoint definitions to a non-`@Serializable` POJO. Save format only needs `persist/Checkpoint`.

### 1.4 Touch input double-fires Jump + Action
- **File:** `input/InputManager.kt:107`
- **Cause:** `isJumpPressed` returns true for *any* right-half tap, but the on-screen Action button also occupies the right half; a single Action tap fires both Jump and Action.
- **Fix:** Only treat raw screen touches as Jump if `uiActionPressed == false` and the touch y is *above* the action button's row. Or remove the screen-half heuristic entirely now that on-screen buttons exist.

### 1.5 Cloud Atlas progress not persisted
- **File:** `screens/GameScreen.kt:524-530`
- **Cause:** atlas overlay opens but `GameState.collectedAtlasIds` is never written.
- **Fix:** When a snapshot is collected, append its `entry.id` to the loaded `GameState`, then `SaveManager.saveGame(...)`.

### 1.6 Body destruction is convention-only
- **File:** `physics/WorldContactListener.kt`
- **Cause:** there's no deferred-destroy queue. Today, destroy calls happen safely from `update()`, but any future contact-handler that calls `world.destroyBody` *will* crash mid-step.
- **Fix:** Add `private val pendingDestroy = mutableSetOf<Body>()` in `GameScreen`, drain it after `world.step(...)`. Document the rule.

---

## 2. Movement Calibration — Target Numbers

The current constants (`Constants.kt`) are too weak to feel snappy. These are calibrated against **Celeste's `Player.cs`** (open source, 320×180 internal resolution), translated to our 1280×720 (4× upscale → multiply px by 4) and PPM=100.

### 2.1 Recommended `Constants.kt` updates

```kotlin
// === HORIZONTAL ===
const val PLAYER_SPEED         = 9f        // was 8 — Celeste 90 px/s × 4 / 100 = 3.6 m/s reads slow at 1280p; 9 m/s feels right
const val PLAYER_RUN_ACCEL     = 40f       // m/s² — accelerate to top speed over ~0.22s (was instant)
const val PLAYER_RUN_DECEL     = 16f       // friction when no input — was hardcoded *0.5 lerp
const val PLAYER_AIR_ACCEL_MUL = 0.65f     // air control 65% of ground (Celeste-ish)

// === JUMP ===
const val PLAYER_JUMP_IMPULSE          = 13f      // was 12 — slight bump for hang
const val PLAYER_JUMP_HOLD_GRAVITY_MUL = 0.5f     // half-gravity while jump held + rising (apex hang)
const val PLAYER_APEX_VEL_THRESHOLD    = 4f       // |vy| below this near apex → bonus hang
const val PLAYER_JUMP_CUT_MUL          = 0.4f     // was 0.5 — sharper variable jump
const val GRAVITY_FALL_MUL             = 1.45f    // asymmetric: gravity 45% stronger when falling
const val PLAYER_MAX_FALL              = 18f      // m/s terminal (was uncapped)
const val PLAYER_FAST_FALL             = 28f      // when down held

// === WALL ===
const val PLAYER_WALL_JUMP_IMPULSE_X = 7.5f       // was 6 — needs more push to clear shaft
const val PLAYER_WALL_JUMP_IMPULSE_Y = 11f        // was 10
const val PLAYER_WALL_SLIDE_SPEED    = -2.5f      // was -2 — slightly faster, less floaty
const val WALL_JUMP_LOCK_TIME        = 0.13f      // was 0.15 — earlier reclaim of horizontal control

// === FORGIVENESS WINDOWS (Celeste matches these) ===
const val COYOTE_TIME       = 0.10f   // was 0.15 — too generous, made level3 trivial
const val JUMP_BUFFER_TIME  = 0.10f   // unchanged — 6 frames @ 60 fps
const val CORNER_CORRECT_PX = 0.06f   // 6cm worth — nudge through clipped jump corners

// === GLOBAL ===
const val GRAVITY = -32f   // was -25 — combined with halved-on-rise gives Celeste-like arc
```

**Tuning protocol:** change one constant at a time, playtest a level segment, write down the feel difference. Do not bulk-change.

### 2.2 Add to `PlayerController.update`

```kotlin
// Asymmetric gravity & apex hang
val vy = body.linearVelocity.y
when {
    vy > 0 && InputManager.isJumpHeld() && Math.abs(vy) < Constants.PLAYER_APEX_VEL_THRESHOLD ->
        body.gravityScale = Constants.PLAYER_JUMP_HOLD_GRAVITY_MUL
    vy <= 0 ->
        body.gravityScale = Constants.GRAVITY_FALL_MUL
    else ->
        body.gravityScale = 1f
}

// Terminal velocity
val cap = if (InputManager.isDownHeld()) Constants.PLAYER_FAST_FALL else Constants.PLAYER_MAX_FALL
if (vy < -cap) body.linearVelocity = Vector2(body.linearVelocity.x, -cap)
```

### 2.3 Corner correction (must-have for tight platforming)

When the player jumps and clips the corner of an overhead obstacle by ≤ `CORNER_CORRECT_PX`, nudge them through instead of stopping vertical velocity. Celeste does 2 px (out of 8 px tile = 25%); we do 6 cm (out of 32 cm tile = ~19%). Implement by raycasting up from the head when `vy > 0` and `vy_prev > 0`.

---

## 3. Game Feel Polish

These are the cheap-but-massive-impact additions. Order from highest ROI:

### 3.1 Landing dust (~ 1 day)

- Trigger: `groundContactCount` flips 0→1 *and* `prev_vy < -8 m/s` (i.e. fell hard, not stepped off a platform).
- Spawn 4–8 small ShapeRenderer circles at foot position.
- Lifespan 0.25 s, outward velocity 1–2 m/s, no gravity, alpha-fade.
- Reuse a `MutableList<DustParticle>` with object pooling (cap 100).

### 3.2 Jump puff (~ 0.5 day)

- 3 small puffs at feet on every jump (ground or wall).
- Lifespan 0.15 s, outward velocity 0.5 m/s.
- Different color per character (Ebo: brown; Laya: white-blue).

### 3.3 Hitstop on death (~ 0.25 day)

- 4-frame freeze when `player.isDead = true`. Skip `world.step` for those frames; render the player flashing red at the impact location.
- Then trigger respawn.

### 3.4 Screen shake (~ 0.5 day)

- Add `ScreenShake` component holding `intensity, duration, decay`. In `GameScreen.update`, decay; in render, offset `camera.position` by `(Math.sin(t*60)*intensity, Math.cos(t*73)*intensity)`.
- Trigger 4 px / 0.2 s on death; 2 px / 0.1 s on hazard cleanse; 1 px / 0.05 s on coin pickup.

### 3.5 Footstep particles (~ 0.5 day)

- Every ~12 cm of horizontal travel while grounded, alternating L/R foot.
- Single circle, lifespan 0.2 s, no movement. Distinguishes movement direction visually.

### 3.6 Snapshot / token sparkles on collect (~ 0.5 day)

- 6–10 particles, additive blend, slight upward gravity, 0.4 s.
- The current 4-point cyan star pickup is good — augment with sparkles on collect.

---

## 4. Camera Upgrade

Current camera (`GameScreen.kt:484`) is a simple lerp-less follow with horizontal clamp. Itay Keren's "Scroll Back" GDC talk gives the playbook:

### 4.1 Add a dead zone

Don't move the camera until the player crosses a small inner rectangle (e.g. 200×120 px). Inside the rectangle the player can move without dragging the camera. This stops nervous bobbing during small jumps and idle walking.

### 4.2 Forward focus

When the player is moving right, place the camera so the player sits at the *left third* of the screen — revealing what's ahead in the direction of motion. Flip on direction change with a 0.3 s lerp.

### 4.3 Platform snap (vertical)

Only follow vertical position when the player is grounded *or* falling past a y-threshold. Locks the camera during jump arcs so the world doesn't bob with every jump. Matches Sonic and SMB behavior.

### 4.4 Camera triggers

Optional: per-room `CameraOffset` triggers (Celeste-style) — a TMX object type that, when the player enters its rect, lerps the camera to a hand-authored offset. Use for set-pieces (boss reveal, sky-bridge wide shot).

```kotlin
// Sketch
class CameraController {
    private val target = Vector2()
    private val deadZoneHalfW = 1.0f  // m
    private val deadZoneHalfH = 0.6f
    private val forwardOffset = 1.5f  // m bias toward facing direction
    private val lerpSpeed = 4f        // higher = snappier

    fun update(dt: Float, playerPos: Vector2, facingRight: Boolean) {
        val biasX = if (facingRight) forwardOffset else -forwardOffset
        val tx = playerPos.x + biasX
        // Dead-zone clamp
        if (Math.abs(tx - target.x) > deadZoneHalfW) {
            target.x += Math.signum(tx - target.x) * (Math.abs(tx - target.x) - deadZoneHalfW)
        }
        // Vertical: only follow on landings & long falls
        target.y = MathUtils.lerp(target.y, playerPos.y, lerpSpeed * dt)
    }
}
```

---

## 5. Audio Architecture

Currently 0 audio assets. When adding sounds, plan the bus layout up-front so the mix stays clean.

### 5.1 Bus layout (5 buses)

| Bus | Examples | Default vol |
|---|---|---|
| Music | level theme, hub theme | -18 LUFS |
| SFX | jump, land, collect, ability, death | -9 dBFS peaks |
| Footsteps | walk loop, pitch-randomized | -12 dBFS |
| Ambient | wind, rain, smog hum | -22 dBFS |
| UI | menu clicks, atlas card flip | -12 dBFS, never ducked |

### 5.2 Ducking

When SFX peaks above -12 dBFS, duck Music by 4 dB with 50 ms attack / 300 ms release. This is the "punch" filter — every coin pickup gets crisp. Implement either with FMOD/Wwise or a simple gain-envelope on the music source after each SFX `play()`.

### 5.3 Style

GDD has eco-restoration theme. **Hybrid orchestral-synth** (à la Celeste — Lena Raine) reads as "alive and growing" better than chiptune. Reserve chiptune for diegetic moments (Ebo's ancestral memory, World 0 prologue title).

### 5.4 Required sound list (priority order)

1. `jump.wav` — punchy 0.1 s blip
2. `land.wav` — soft thud, randomized 3 variants
3. `collect_token.wav` — pretty bell, 0.2 s
4. `collect_snapshot.wav` — same but with reverb tail and "glow" feel (separate sample)
5. `death.wav` — short 0.3 s break/shatter
6. `checkpoint.wav` — ascending arpeggio, 0.5 s
7. `level_complete.wav` — celebratory stinger, 1.5 s
8. `ability_ebo_seed_slam.wav` — heavy "thunk" + rain hiss tail
9. `ability_laya_wind_dash.wav` — whoosh, 0.4 s
10. `hazard_cleansed.wav` — bubbly "sizzle"
11. `footstep_grass.wav` × 4 variants, pitch-randomized at runtime
12. Music tracks: 1 per world, looping, ~2 minute loop is fine

---

## 6. Save System Hardening

### 6.1 Atomic writes

`SaveManager.saveGame` currently writes directly with `writeString(jsonString, false)`. A crash mid-write corrupts the save.

```kotlin
fun saveGame(state: GameState, filename: String = DEFAULT_SAVE_FILE) {
    val tmp = Gdx.files.local("$SAVE_DIR/$filename.tmp")
    val final = Gdx.files.local("$SAVE_DIR/$filename")
    tmp.writeString(json.encodeToString(state), false)
    // libGDX has no rename — manual copy + delete:
    final.delete()
    tmp.copyTo(final)
    tmp.delete()
}
```

### 6.2 Three save slots

Indie standard. UI: 3 cards on main menu showing per-slot summary (level reached, % atlas collected, total deaths, last-played timestamp). Lets families share, defends against single-save corruption.

### 6.3 What to actually persist

- `currentLevel`, `currentCharacter`
- `checkpoint: Checkpoint`
- `completedLevels: Set<String>`
- `bestScores: Map<String, Int>`
- `collectedAtlasIds: Set<String>` ← **wire this up (P0 #1.5)**
- `totalDeaths: Int`, `totalPlayTimeSec: Float`
- `settingsRef: String` (settings live in a separate file)

### 6.4 Settings file

Keep separate from save state — one `settings.json` shared across slots: volume per bus, key bindings, screen-shake on/off, accessibility (assist-mode flags: invincibility, speed-down, infinite jumps — Celeste-inspired).

---

## 7. Onboarding — Build World 0 (Prologue)

The current "level1" jumps straight into core mechanics. Per GDD, **World 0 / Sky Sanctuary Fall** is the tutorial. Build it as 4 short rooms, each teaching exactly one concept (Kishōtenketsu).

### 7.1 World 0 room plan

| Room | Teaches | Mechanic intro pattern |
|---|---|---|
| 0-1 "First Step" | Walk left/right + ground jump | Single-screen room. Eco-token unreachable without a single jump over a 2-tile gap. |
| 0-2 "Long Fall" | Variable jump height + coyote time | Vertical drop with a single platform; landing requires releasing jump early to fit. Coyote: a ledge that requires walking off-the-edge then jumping. |
| 0-3 "Wall Climb" | Wall jump | Vertical shaft, no other path. Auto-fail-safe: if player falls, return to room start, no death. |
| 0-4 "First Cleanse" | Ebo's Seed Slam | Hazard blocks the only path. Seed Slam button highlighted on HUD with pulsing ring. After cleanse, hazard becomes ground (rule of 3). |

Each room is **one screen**, no scrolling. No text — environment teaches via the only-possible-solution and pulsing-glow hints.

### 7.2 Prologue narrative beat

The Sky Sanctuary tower is under attack by the Great Haze. The 4 rooms are platform sections of the falling tower. Final room = Ebo lands in The Parched Expanse (level1). Use `screenFade` between rooms as the "falling" visual.

---

## 8. Level Pacing Template (apply to existing & new)

Borrow the **Kishōtenketsu** structure for every level:

1. **Ki (Introduce, 0–25%)** — start zone; safe demo of one new mechanic. Eco-tokens are easy.
2. **Shō (Develop, 25–60%)** — same mechanic with light pressure (a pit, a hazard). 1–2 hidden tokens.
3. **Ten (Twist, 60–85%)** — combine with another mechanic (wall-jump + moving platform), or invert (the safe object becomes the threat).
4. **Ketsu (Conclude, 85–100%)** — set-piece room: 3–5 mechanics layered, biggest token cluster, then the exit sensor. Should feel like a final exam.

**Rule of 3:** every new mechanic appears at least 3 times in escalating risk before being combined with another.

**Audit:** level3 currently jumps to wall-jump + fast moving platforms in the first 30 % — too aggressive. Move the wall-jump shaft to ~50 % and add a ground-only opening sequence first.

---

## 9. Particle / VFX System (concrete spec)

Build a pool-backed particle system. Don't use libGDX's `ParticleEffect` — overkill and slow for ShapeRenderer-style art.

```kotlin
package com.sohai.platformer.rendering

class Particle {
    val pos = Vector2()
    val vel = Vector2()
    var radius = 0f
    var lifeMax = 0f
    var lifeLeft = 0f
    var color = Color()
    var gravity = 0f
    var alive = false
}

class ParticleSystem(maxParticles: Int = 200) {
    private val pool = Array(maxParticles) { Particle() }

    fun spawn(x: Float, y: Float, vx: Float, vy: Float, r: Float, life: Float, col: Color, g: Float = 0f) {
        val p = pool.firstOrNull { !it.alive } ?: return
        p.pos.set(x, y); p.vel.set(vx, vy); p.radius = r
        p.lifeMax = life; p.lifeLeft = life; p.color.set(col); p.gravity = g; p.alive = true
    }

    fun update(dt: Float) {
        for (p in pool) if (p.alive) {
            p.lifeLeft -= dt
            if (p.lifeLeft <= 0f) { p.alive = false; continue }
            p.vel.y += p.gravity * dt
            p.pos.mulAdd(p.vel, dt)
        }
    }

    fun render(sr: ShapeRenderer) {
        for (p in pool) if (p.alive) {
            val a = p.lifeLeft / p.lifeMax
            sr.color = Color(p.color.r, p.color.g, p.color.b, p.color.a * a)
            sr.circle(p.pos.x, p.pos.y, p.radius * a)
        }
    }
}
```

Capacity 200 is plenty for our scale. Run inside `shapeRenderer.begin/end` block in GameScreen.

---

## 10. Architecture Refactors

Defer until P0 fixes are done, but plan for these.

### 10.1 Split GameScreen (636 LOC → ~300)

GameScreen reaches into 14 subsystems. Extract:
- `LevelRunState` — score, combo, spirit health, completion flags
- `LevelRenderer` — the entire `shapeRenderer.begin/end` block
- `LevelTransitionController` — goToNextLevel / dispose chain
- `InputRouter` — manages which Stage owns the input processor (currently scattered)

### 10.2 Global font cache

Every screen creates 2–3 fonts (`FontManager.create(N)`). Each call generates a fresh FreeType atlas → wasted GPU memory + alloc time on transitions. Build:

```kotlin
object FontCache {
    private val cache = mutableMapOf<Int, BitmapFont>()
    fun get(size: Int): BitmapFont = cache.getOrPut(size) { FontManager.create(size) }
    fun disposeAll() { cache.values.forEach { it.dispose() }; cache.clear() }
}
```

Replace all `FontManager.create(N)` calls with `FontCache.get(N)`. Dispose only at app exit.

### 10.3 Hoist hot-path Color allocations

`GameScreen.render` allocates ~30 `Color(...)` per frame inside loops. Move to `companion object` constants:

```kotlin
companion object {
    private val COLOR_GROUND       = Color(0.40f, 0.42f, 0.45f, 1f)
    private val COLOR_GROUND_TOP   = Color(0.62f, 0.65f, 0.68f, 1f)
    private val COLOR_HAZARD_BASE  = Color(0.75f, 0.15f, 0.15f, 1f)
    // ... etc.
}
```

### 10.4 Levels via data, not classes

`Level1`/`Level2`/`Level3` are 90 % identical. Replace with a registry of `TmxLevelDefinition(id, name, mapPath, spawnX, spawnY, levelWidthPx, exitX, ecoTokens, snapshots)`.

### 10.5 Box2D fixed-timestep accumulator

Currently `world.step(1/60f, ...)` is called regardless of `delta`. Replace with:

```kotlin
private var accum = 0f
fun update(dt: Float) {
    accum += dt
    while (accum >= TIME_STEP) {
        world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
        accum -= TIME_STEP
    }
}
```

Frame-independent physics. Critical if game ever runs on hardware that drops below 60 fps.

---

## 11. Testing Strategy

Currently 0 tests. Set up `core/src/test/kotlin/...` and JUnit 5. **Don't aim for 80 % coverage** — aim for the high-leverage units:

| Test | Why | Fakes needed |
|---|---|---|
| `SaveManager` round-trip | Catches save-format breaks | None (use temp dir) |
| `MapLevelLoader` coordinate flipping | Recurring source of bugs | Synthetic `RectangleMapObject` |
| `PlayerController` jump state machine | Coyote/buffer/wall-jump correctness | Fake World + InputManager driver |
| `LevelManager.getNextLevel` | Sequencing | None |
| `CloudAtlasLibrary.get` | Lookup | None |
| `Particle` pool eviction | Capacity bug class | None |

Run tests via `./gradlew core:test`. Wire into pre-commit hook.

---

## 12. Ship Plan — 2-Week Sprints

### Sprint A (week 1) — "Make it Feel Right"
- Day 1–2: Fix all P0 bugs (§1)
- Day 3: Apply movement constants (§2.1) + asymmetric gravity (§2.2)
- Day 4: Corner correction (§2.3) + camera dead zone & forward focus (§4.1, §4.2)
- Day 5: Particle system (§9) + landing dust + jump puff
- Day 6: Hitstop + screen shake (§3.3, §3.4)
- Day 7: Playtest pass, tune numbers

### Sprint B (week 2) — "Make it Stick"
- Day 8: Save system hardening (§6.1, §6.3) + atlas persistence
- Day 9: Build World 0 (4 tutorial rooms — §7.1)
- Day 10: Audio bus layout + record/source 12 priority sounds (§5.4)
- Day 11: Refactor GameScreen split + FontCache (§10.1, §10.2)
- Day 12: Hot-path Color hoisting + fixed-timestep (§10.3, §10.5)
- Day 13: Add 6 priority unit tests (§11)
- Day 14: Polish pass — particle counts, sound mixing, transition timing

After these two sprints, the game's first three worlds should *feel* indie-quality and the codebase should be in good shape to add World 4 and the assist characters per the original GDD.

---

## 13. Reference Numbers Table (quick-grab card)

| What | Value | Source |
|---|---|---|
| Coyote time | 100 ms | Celeste |
| Jump buffer | 100 ms | Celeste, Hollow Knight |
| Variable jump cut multiplier | 0.4 | Celeste 0.4–0.5 |
| Apex hang gravity multiplier | 0.5 | Celeste |
| Falling gravity multiplier | 1.4–1.5 | Hollow Knight, Mario games |
| Corner correction | ≤ 25 % of tile width | Celeste 2 of 8 px |
| Dust particles per landing | 4–8 | Standard |
| Particle lifespan | 0.15–0.5 s | Vlambeer |
| Hitstop on heavy impact | 4–6 frames | Vlambeer |
| Screen shake on death | 4 px / 0.2 s decay | Standard |
| Stick deadzone | radial, 0.20–0.25 inner, 0.95 outer | Sutphin |
| Music duck on SFX | 4 dB / 50 ms attack / 300 ms release | Standard |
| Save autosave triggers | room transition, item pickup, settings | Celeste |
| Save slots | 3 named | Celeste, Hollow Knight, Ori |
| Music LUFS | -18 LUFS short-term | Game audio standard |

---

## 14. References (for AI agents continuing the work)

- Maddy Thorson — *Celeste Forgiveness*: https://maddythorson.medium.com/celeste-forgiveness-31e4a40399f1
- Maddy Thorson — *Celeste & TowerFall Physics*: https://maddythorson.medium.com/celeste-and-towerfall-physics-d24bd2ae0fc5
- Itay Keren — *Scroll Back* (GDC 2015): https://www.gamedeveloper.com/design/scroll-back-the-theory-and-practice-of-cameras-in-side-scrollers
- Vlambeer — *Art of Screenshake*: https://www.youtube.com/watch?v=AJdEqssNZ-U
- Mark Brown — *Game Maker's Toolkit* on Kishōtenketsu and rule-of-three
- Celeste source (NoelFB/Celeste, `Player.cs`): https://github.com/NoelFB/Celeste
- Josh Sutphin — *Doing Thumbstick Dead Zones Right*: https://www.gamedeveloper.com/business/doing-thumbstick-dead-zones-right
- libGDX wiki — Box2D, ShapeRenderer, Stage/Scene2D
- Original Cloudy Ninja GDD — for theme, characters, worlds, Sky Sanctuary, Cloud Atlas

---

## 15. AI Agent Operating Notes

If you (an AI agent) are continuing this project:

- **Read this addendum and the original GDD before making non-trivial changes.**
- Constants live in `core/.../Constants.kt`. Tune one at a time.
- Hot-path renders are in `LevelRenderer.kt` (NOT GameScreen — that was refactored). Don't allocate inside `renderWorld()`.
- `Hud`, `PauseOverlay`, `LevelSelectScreen`, `VictoryScreen`, `CloudAtlasOverlay` all call `FontManager.getShared(size)` — this is the correct pattern. Do NOT dispose shared fonts in screen `dispose()`.
- The default BitmapFont fallback can't render emoji or non-ASCII. Use ASCII glyphs (`[+]`, `[X]`, `***`) until a TTF is shipped in `assets/fonts/main.ttf`.
- TMX files use y-up coordinates (`flipY = false` in `MapLevelLoader.load`). The old `flipY = true` bug was fixed in T-025 (May 2026).
- **Before declaring a feature done, run** `./gradlew :core:compileKotlin` and `./gradlew :core:test` and confirm no regressions.

Welcome aboard. Cloudy Ninja's mission is too important to ship feeling mushy. Make it snap.

---

## 16. Sprint C Plan — "Content & Combat" (Weeks 5–6)

Sprint A fixed the core feel. Sprint B added persistence, world structure, and UX polish. Sprint C adds the missing gameplay depth: enemies, music, hub world, and the visual/UX gaps that stop this from feeling like a released indie title.

### Gap analysis vs. polished 2D platformers (Celeste, Hollow Knight, Ori, Rayman)

| Gap | Impact | Sprint C priority |
|---|---|---|
| No enemies — all challenge is geometry only | High — limits mechanical variety | **P1** |
| No background music | High — biggest atmospheric hole | **P1** |
| Solid-color rectangle terrain (no tile sprites) | Medium — poor visual identity | **P2** |
| No hub world (Sky Sanctuary from GDD) | Medium — no sense of world structure | **P2** |
| No boss fights | Medium — no climactic moments | **P2** |
| No per-bus audio sliders (music/sfx/ui) | Low-medium — expected by players | **P2** |
| No key rebinding | Low-medium — accessibility | **P2** |
| No achievement / meta-progression | Low — replay incentive | **P3** |
| Ghost replay in time trials | Low — nice speedrun feature | **P3** |
| Cloud Atlas only 5 entries | Low — educational content thin | **P3** |
| No stats screen | Low — meta-progression visibility | **P3** |

### Sprint C task list (see TASKS.md for full specs)

**P1 — Do first:**
- T-029: Enemy framework + Patroller (patrolling enemy that responds to abilities)
- T-030: Music system + 3 procedural ambient tracks
- T-032: Stomp-defeat mechanic (jump on enemy from above)
- T-040: Projectile/lightning hazard entity

**P2 — Do after P1:**
- T-031: Tile-based terrain rendering
- T-033: Hub world — Sky Sanctuary (Level 0-0)
- T-034: Boss encounter — Storm Sentinel (Level 3 finale)
- T-035: Audio bus sliders (music / sfx / ui — separate from existing volSfx)
- T-036: Key rebinding UI in Settings

**P3 — Polish pass:**
- T-037: Achievement system + toast notifications
- T-038: Ghost replay in time trials
- T-041: Stats screen on main menu
- T-045: Cloud Atlas expansion to 12 entries

---

## 17. Enemy Design Spec (T-029, T-032, T-040)

### Philosophy
Every enemy in Cloudy Ninja must *teach* an ability while also threatening the player. The game's eco-restoration theme means enemies are polluted/corrupted entities — defeated by the character's cleansing ability, not conventional violence. Three archetype slots:

| Slot | Archetype | Teaches | Defeated by |
|---|---|---|---|
| Ground Patroller | Smog Sprite | Dodging, timing stomp window | Seed Slam droplets or stomp |
| Air Floater | Haze Wisp | Precision movement, Laya timing | Wind Dash pass-through |
| Projectile Shooter | Storm Node | Projectile awareness | (static, destroyed by 3 Seed Slams) |

### 17.1 Smog Sprite (first enemy — T-029)

```
entities/Enemy.kt        — abstract base class
entities/SmogSprite.kt   — first concrete enemy
```

**Behaviour:**
- Patrols between two x-waypoints (read from TMX `enemy_patrol` objectgroup, or set in LevelRegistry)
- On reaching a wall or waypoint edge, flips direction
- Kills player on lateral body contact
- Stomped (player vy < -3 m/s, contact from above) → defeated; player gets a bounce impulse +5 m/s upward
- Hit by Seed Slam droplet (fixture userData == "water_droplet") → defeated after 2 hits
- On defeat: spawn 6-particle smoke burst (grey), play `hazard_cleansed` SFX, queue body destroy

**Box2D setup:**
- Dynamic body, `linearDamping = 0f`
- Fixture: 0.3 × 0.3 m box sensor (detection) + 0.28 × 0.26 m box solid (collision)
- Category: `ENEMY_BITS = 0x0010`, Mask: `GROUND | PLAYER`
- Set horizontal velocity in `update()` — do NOT use Box2D forces (too springy)

**Data definition in LevelRegistry:**
```kotlin
data class EnemyDef(val type: String, val x: Float, val y: Float,
                    val patrolLeft: Float, val patrolRight: Float)
// Add: val enemies: List<EnemyDef> = emptyList() to TmxLevelDefinition
```

**TMX alternative:** parse `objectgroup name="enemies"` in `MapLevelLoader`; look for objects with `type="smog_sprite"` and `property patrol_left/right`.

### 17.2 Stomp mechanic (T-032)

In `WorldContactListener.beginContact`:
```kotlin
// Detect player stomping enemy
if (aIsPlayer && bIsEnemy && playerVelocityY < -3f) {
    CleanseEventQueue.push(enemy.body.position.copy())  // reuse cleanse queue for defeat events
    enemy.takeDamage(fatal = true)
    player.body.linearVelocity = Vector2(player.body.linearVelocity.x, 5f)  // bounce
}
```

### 17.3 Projectile entity (T-040)

```kotlin
class Projectile(world: World, x: Float, y: Float,
                 val vx: Float, val vy: Float, val lifetime: Float = 3f) {
    val body: Body  // kinematic, sensor=false, category=HAZARD_BITS
    var age = 0f
    val isExpired get() = age >= lifetime || hitWall
}
```

`GameScreen` / `LevelRunState` updates each `Projectile` per frame and queues body-destroy when expired. Rendered as a small orange ShapeRenderer circle. Used by Storm Sentinel boss and Storm Node static emitters.

---

## 18. Music System Spec (T-030, T-035)

### 18.1 MusicManager architecture

```kotlin
object MusicManager {
    private var current: Music? = null
    private var next: Music?    = null
    private var fadeTimer       = 0f
    private var fadeDuration    = 1.5f
    private var volMusic        = 0.7f  // separate from SFX

    fun play(track: String, fadeIn: Boolean = true) { … }
    fun update(delta: Float) {  // crossfade logic
        if (next != null) {
            fadeTimer += delta
            val t = (fadeTimer / fadeDuration).coerceIn(0f, 1f)
            current?.volume = volMusic * (1f - t)
            next?.volume = volMusic * t
            if (t >= 1f) { current?.stop(); current = next; next = null }
        }
    }
    fun setMusicVolume(vol: Float) { volMusic = vol; current?.volume = vol }
}
```

`MusicManager.update(delta)` is called from `GameScreen.render` each frame (after `runState.update`).

### 18.2 Per-level track selection

Add `musicTrack: String` field to `TmxLevelDefinition` (default `"ambient_arid"`). `GameScreen.init` calls `MusicManager.play(level.musicTrack)`.

| Level ID | Track | Mood |
|---|---|---|
| level0_* | `ambient_prologue` | Calm, sparse — player learning |
| level1 | `ambient_arid` | Dry, hopeful undertones |
| level2 | `ambient_wind` | Airy, flowing, slightly tense |
| level3 | `ambient_eco` | Dense, forest-alive, builds in second loop |

### 18.3 Procedural track generation

`ProceduralSoundGenerator` already exists. Extend it (or add `ProceduralMusicGenerator`) to produce 60-second looping WAV files:
- Layered sine/triangle oscillators at harmonically related frequencies
- Amplitude-modulated at 0.5–2 Hz for "breathing" texture
- Output: `assets/audio/music/{trackId}.wav`

### 18.4 Audio bus sliders (T-035)

Extend `Settings.kt`:
```kotlin
val volMusic: Float = 0.7f
val volSfx:   Float = 0.8f   // already exists
val volUi:    Float = 0.9f
```

`SettingsScreen` gets three VisUI sliders (Music / SFX / UI). On change:
```kotlin
MusicManager.setMusicVolume(settings.volMusic)
SoundManager.setVolume(settings.volSfx)
// UI sounds TBD — placeholder for now
```

---

## 19. Hub World Spec (T-033)

### 19.1 Sky Sanctuary — Level0_0

Per the original GDD, the Sky Sanctuary is the prologue home. In gameplay terms it's a **hub world** — a single-screen room with portal doors to each unlocked World.

```kotlin
class Level0_0 : Level() {
    override val id = "level0_0"
    override val name = "Sky Sanctuary"
    override val spawnX = 640f; override val spawnY = 160f
    override val levelWidthPx = 1280f  // one-screen hub
}
```

**Layout (1280×720 screen):**
- Wide central platform (0–1280, y=0..40) — the sanctuary floor
- Tall stone pillars at x=200 and x=1080 — decorative only
- Four portal doors (sensor bodies) at x=280, 480, 680, 880 — one per world
- Each door has a `name` in ObstacleManager: `portal_world0`, `portal_world1`, etc.

**Portal activation:**
- `WorldContactListener` adds `"portal_activated"` userData to portal fixtures
- `LevelRunState` detects portal contact → triggers callback to GameScreen → GameScreen loads first level of that world
- Locked worlds: portal renders as greyed-out, ignores contact. Unlocked = check `GameState.completedLevels` for last level of previous world.

**Entry from main menu:** `LevelManager` places `Level0_0` as index 0 (before Level0_1). Main menu "Play" → `GameScreen(Level0_0)`. First-time players start in hub, walk right to portal_world0 to enter the tutorial.

---

## 20. Boss Design Spec (T-034)

### Storm Sentinel — Level 3 Finale

A static storm-entity that occupies a dedicated arena room appended to Level 3's Ketsu zone. Not a full movement-AI boss — a pattern-based hazard machine (Shovel Knight "sub-boss" style).

**Arena layout:**
- 640×720 room at x=2200..2840 (extends Level 3 map 640 px)
- Boss body: 80×80 px static sensor at center-top (x=2520, y=580–660)
- Platforms at y=160, y=300 (player fight platforms)

**Attack pattern (3-phase cycling, 8-second period):**
1. **Lightning column** (1.5 s telegraph, then 3 `Projectile` objects drop vertically at random x)
2. **Wind sweep** (sweep 5 projectiles horizontally at y=160 — player must jump)
3. **Rest** (2 s safe window — Seed Slam window)

**Defeat condition:**
- Boss takes 3 Seed Slam hits (droplet contact). Each hit: flash red, spawn burst, play `hazard_cleansed` SFX.
- On defeat: 30-particle burst, `level_complete` SFX, auto-trigger level exit.
- Defeat also unlocks a Cloud Atlas snapshot entry (`storm_system`).

**Implementation notes:**
- `entities/StormSentinel.kt` — not a `MovingPlatform` or `Enemy` subclass; manages its own attack timer and `List<Projectile>`
- `LevelRunState` updates sentinel each frame alongside moving platforms
- Level 3 exit sensor is placed INSIDE the boss room (only reachable after sentinel is destroyed)

---

## 21. Tile Rendering Spec (T-031)

Current terrain uses ShapeRenderer colored rectangles (GROUND = grey gradient, HAZARD = red). This is fast but visually flat. Replace with a lightweight tile-sprite overlay that preserves the Box2D geometry unchanged.

### 21.1 Tileset structure

```
assets/tilesets/
├── tiles_arid.png   — 64×64 px atlas: rock, dirt, grass-top (3 tiles × variants)
├── tiles_wind.png   — 64×64 px atlas: slate, cloud-stone, ice-edge
└── tiles_eco.png    — 64×64 px atlas: mossy-stone, roots, leaf-top
```

Each tileset: 3 columns × 2 rows (6 tiles):
- [0,0] solid interior, [1,0] top-face, [2,0] corner-cap
- [0,1] hazard interior, [1,1] hazard-top, [2,1] hazard-surface

### 21.2 TileRenderer class

```kotlin
class TileRenderer(private val batch: SpriteBatch, private val tilesetTexture: Texture) {
    private val tileSize = 16f / Constants.PPM  // 16 px tiles → 0.16 m in world space
    fun renderObstacles(obstacles: List<ObstacleRect>, camera: OrthographicCamera) {
        batch.projectionMatrix = camera.combined
        batch.begin()
        for (obs in obstacles) {
            val region = selectRegion(obs.kind)  // map GROUND→[1,0], HAZARD→[0,1] etc.
            // tile-fill the obstacle bounds
            val x0 = obs.centerX - obs.halfW; val y0 = obs.centerY - obs.halfH
            for (ty in 0 until ceil(obs.halfH*2 / tileSize).toInt()) {
                for (tx in 0 until ceil(obs.halfW*2 / tileSize).toInt()) {
                    batch.draw(region, x0 + tx*tileSize, y0 + ty*tileSize, tileSize, tileSize)
                }
            }
        }
        batch.end()
    }
}
```

`LevelRenderer` calls `tileRenderer.renderObstacles(...)` INSTEAD OF the current `shapeRenderer` rect loop. The ShapeRenderer rect code is removed.

### 21.3 Tileset selection

`LevelRenderer` receives the `ParallaxTheme` from `GameScreen`. Map theme → tileset:
```kotlin
val tilesetTexture = when (theme) {
    ARID -> Texture("tilesets/tiles_arid.png")
    WIND -> Texture("tilesets/tiles_wind.png")
    ECO  -> Texture("tilesets/tiles_eco.png")
}
```

---

## 22. Achievement System Spec (T-037)

### 22.1 Achievement registry

```kotlin
@Serializable
data class Achievement(val id: String, val title: String, val desc: String)

object AchievementRegistry {
    val ALL = listOf(
        Achievement("first_jump",      "First Flight",    "Complete your first jump"),
        Achievement("first_cleanse",   "Seed Planter",    "Cleanse your first hazard with Seed Slam"),
        Achievement("eco_sweep",       "Eco Champion",    "Collect all eco-tokens in any one level"),
        Achievement("no_death_run",    "Ghost Walker",    "Complete a level without dying"),
        Achievement("speed_demon",     "Speed Demon",     "Complete any level under 2 minutes in Time Trial"),
        Achievement("atlas_half",      "Cloud Watcher",   "Collect 6 Cloud Atlas snapshots"),
        Achievement("atlas_full",      "Sky Scholar",     "Collect all 12 Cloud Atlas snapshots"),
        Achievement("first_enemy",     "Cleanse Warrior", "Defeat your first Smog Sprite"),
        Achievement("stomp_10",        "Stomper",         "Stomp 10 enemies"),
        Achievement("boss_defeated",   "Storm Breaker",   "Defeat the Storm Sentinel"),
        Achievement("world_1_clear",   "The First Rain",  "Complete World 1"),
        Achievement("all_clear",       "Eco Restored",    "Complete all worlds")
    )
}
```

### 22.2 Persistence

Add to `GameState`:
```kotlin
val unlockedAchievements: Set<String> = emptySet()
```

### 22.3 Toast notification

`AchievementToast` — overlaid above HUD, auto-dismissed after 3 s:
```kotlin
class AchievementToast {
    fun show(achievement: Achievement)  // fades in, holds 2s, fades out
    fun update(delta: Float)
    fun render(batch: SpriteBatch)
}
```

`LevelRunState` holds a reference; `GameScreen` renders it above Layer 4 (HUD) but below Layer 7 (pause).

### 22.4 Unlock triggers

Unlocks are checked in `LevelRunState.update()` after relevant events:
- `first_jump` → on first `player.onJump` callback
- `eco_sweep` → when `ecoTokens.isEmpty()` for the first time
- `no_death_run` → when `levelCompleted && spiritHealth == 3` (never took damage)
- `speed_demon` → in `LevelTransitionController` when `isTimeTrial && levelTimer < 120f`

---

## 23. Ghost Replay Spec (T-038)

### Design intent
After setting a time-trial best, the player's next run sees a translucent "ghost" of their record run. Creates natural speedrun tension without online infrastructure.

### 23.1 Recording

During a time-trial run, `LevelRunState` records a snapshot every 3 frames (≈50 ms at 60 fps):
```kotlin
data class GhostFrame(val x: Float, val y: Float, val facingRight: Boolean, val character: String)
```

On run completion (new best), serialize the frame list to `saves/ghost_{levelId}.json`. Max 4000 frames (200 s at 50 ms cadence) → ~200 KB; acceptable.

### 23.2 Playback

Next time-trial run: load ghost frames into `GhostPlayer`. Each frame, `GhostPlayer.currentFrame` advances by `elapsed / frameDelta`. `LevelRenderer` draws the ghost as a translucent (alpha=0.35) version of the player circle/sprite at that position, in the opposite character color.

### 23.3 Data model extension

```kotlin
// persist/GhostRecording.kt
@Serializable
data class GhostRecording(
    val levelId: String,
    val timeSeconds: Float,
    val frames: List<GhostFrame>
)
```

`SaveManager` gains `saveGhost(rec, levelId)` and `loadGhost(levelId): GhostRecording?`.

---

## 24. Reference numbers update (Sprint C additions)

| What | Value | Source |
|---|---|---|
| Enemy patrol speed | 2.5 m/s | Celeste: Crumble-block guardian speed |
| Stomp bounce impulse | 5 m/s upward | Mario 64, Hollow Knight stomp |
| Projectile speed | 6 m/s horizontal, 8 m/s vertical | Shovel Knight |
| Boss hit window | 2 s per phase rest | Shovel Knight sub-boss pattern |
| Ghost recording interval | 3 frames @ 60 fps = 50 ms | Trackmania ghost standard |
| Achievement toast duration | 3 s (0.3 s fade in, 2.4 s hold, 0.3 s fade out) | Hollow Knight, Ori |
| Music crossfade | 1.5 s | Celeste room transitions |
| Music volume LUFS | -18 LUFS | Game audio standard (see §5.3) |
| Tile size | 16 px (0.16 m at PPM=100) | Standard pixel-art platformer |

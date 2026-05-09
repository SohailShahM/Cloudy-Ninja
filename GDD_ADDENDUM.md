# Cloudy Ninja — Technical GDD Addendum (v2)

**Last updated:** 2026-05-09 (Sprint A complete + Sprint B partial)
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
- Hot-path renders are in `screens/GameScreen.kt`. Don't allocate inside `render()`.
- `Hud`, `PauseOverlay`, `LevelSelectScreen`, `VictoryScreen`, `CloudAtlasOverlay` all create their own fonts — replace with `FontCache.get(...)` once §10.2 lands.
- The default BitmapFont fallback can't render emoji or non-ASCII. Use ASCII glyphs (`[+]`, `[X]`, `***`) until a TTF is shipped in `assets/fonts/main.ttf`.
- Tile coordinates from `.tmx` files use libGDX's `flipY = true` translation. See `MapLevelLoader.kt` and the dramatic playerY=-0.27 vs 0.73 bug history (resolved May 2026).
- The smart-approve PreToolUse hook is configured at `~/.claude/settings.json` — bash compound commands like `cd <path> && git status` auto-approve if all sub-commands are in the allow list.
- **Before declaring a feature done, run** `./gradlew core:compileKotlin` *and* `./gradlew lwjgl3:run` and confirm no `[Perf]` log shows fps < 100 or maxDelta > 0.05.

Welcome aboard. Cloudy Ninja's mission is too important to ship feeling mushy. Make it snap.

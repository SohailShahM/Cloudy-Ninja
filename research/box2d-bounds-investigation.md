# Box2D Body Bounds Investigation (T-210)

**Status:** Diagnostic — root cause identified, no in-scope code fix; recommended follow-up listed below.

**Symptom (user play-test):**
- On **moving platforms**: character appears to **float midway above** the platform surface.
- On **regular tiles**: character appears to **sit slightly below** the ground surface.

## TL;DR

The Box2D rest-position math is **identical** for both surface types. The player body settles with `body.y = surface_top + 0.32m` on either a static tile or a moving platform — there is no physics asymmetry, no extra "lift" on platforms, and no offset bug in `MovingPlatform.kt`.

The perceived inconsistency is a **rendering interaction** between three pre-existing constants:

| Constant                          | Value | Where                                          |
| --------------------------------- | ----- | ---------------------------------------------- |
| Player main-fixture half-height   | 0.32m | `PlayerController.kt:84,115`                   |
| `SPRITE_FOOT_OFFSET_*`            | 0.30m | `SpriteFactory.kt:41-43`                       |
| MovingPlatform half-height        | 0.10m | `MovingPlatform.kt:28`                         |

The 0.30m foot-offset pushes the sprite's visible feet **30 cm below the collision surface, regardless of surface type**. Whether the user perceives this as "sinking" or "floating" depends on whether there is opaque texture below the surface line:

- Thick tile (≥ 0.30m tall): the 30 cm of feet sink **into** opaque tile texture → reads as "slightly below ground."
- Thin platform (0.20m tall): the 30 cm of feet pass **through and below** the entire platform into empty air → reads as "feet dangling under the platform" → human eye interprets the visible torso (37 cm above platform top) as the character "floating midway above."

## Static-tile rest math

1. `addRectNormalized` in `ObstacleManager.kt:47-108` creates a `StaticBody` with a `PolygonShape.setAsBox(hw, hh)` centred at the TMX rectangle's centre. Top edge = `ground.y + hh`. Category `BIT_GROUND`, mask `BIT_PLAYER | BIT_HAZARD | BIT_DROPLET`.
2. Player main fixture (`PlayerController.kt:115-138`): `setAsBox(0.16, 0.32)`, category `BIT_PLAYER`, mask includes `BIT_GROUND`. **Solid-solid contact** (both non-sensor, both polygons; dynamic-vs-static).
3. Rest equation: `body.y - 0.32 = ground_top` → **`body.y = ground_top + 0.32m`**.
4. Sprite render in `LevelRenderer.kt:699`: `sy = body.y - 32/PPM - 0.30 = body.y - 0.62m`. Sprite is 0.80m tall, so it spans `body.y - 0.62` to `body.y + 0.18`.
5. Visible feet (pixel rows 75-80 of the 80-px sprite; see `SpriteFactory.kt:118-119`) sit at world y `body.y - 0.62`, which equals **`ground_top - 0.30m`** → 30 cm below the collision top.

For typical TMX ground rects rendered by `TileRenderer.kt` (tile size 0.32m), the visual top of the drawn tiles equals the collision top **only when the rect's full height is a clean multiple of 0.32m**. Smaller remainders are dropped (see `TileRenderer.kt:25`), but this affects only very thin/irregular slabs and does not change the rest-position math.

## Moving-platform rest math

1. `MovingPlatform.kt:20-37`: `KinematicBody` with `PolygonShape.setAsBox(0.50, 0.10)`. Category `BIT_GROUND`, mask `BIT_PLAYER`. Top edge = `platform.y + 0.10`. Friction 1.0.
2. Box2D contact rule: **Dynamic ↔ Kinematic** polygon-polygon contacts ARE solid (only Kinematic ↔ Kinematic and Kinematic ↔ Static are pass-through). So the player main fixture lands on the platform exactly as it lands on a tile.
3. Rest equation: `body.y - 0.32 = platform.y + 0.10` → **`body.y = platform.y + 0.42m`**.
4. Visible feet at `body.y - 0.62 = platform.y - 0.20` → **30 cm below the platform's top edge**, identical offset to the static-tile case.
5. The MovingPlatform's visual rect (drawn by `LevelRenderer.kt:518-528` via `ShapeRenderer`) exactly matches its collision rect — top at `pos.y + 0.10`, bottom at `pos.y - 0.10`. No visual/collision skew.
6. `MovingPlatform.update` (lines 39-51) only writes `body.linearVelocity` toward the next waypoint — no `setTransform` of the player, no Y-axis lift, no carry hack. The player is carried purely by Box2D tangential friction (see `PlayerController.kt:188-206`).

## Why the perceptions differ

The 0.30m gap between "physics rest position" and "visible feet" is the same in both cases. What changes is the **height of the opaque surface below the collision line**:

| Surface          | Surface height | Feet below collision top | Feet relative to surface bottom |
| ---------------- | -------------- | ------------------------ | ------------------------------- |
| Static ground    | usually ≥ 0.64m| 0.30m (inside opaque)    | 0.34m above surface bottom      |
| Moving platform  | 0.20m          | 0.30m                    | **0.10m below surface bottom**  |

On a moving platform the lower 30 cm of feet are visually outside the platform entirely — dangling in air. The brain reads the torso (37 cm above platform top) as the "actual character height" and the dangling feet as artefact. The platform appears to be attached at knee/mid-leg height.

Quick numeric check on visible figure:
- Sprite center y = `body.y - 0.22`.
- On moving platform: sprite center = `platform_top + 0.20` → midpoint of figure is 20 cm above platform top, with platform spanning 20 cm centred 30 cm below the feet.
- Torso top (pixel y=23 from top, so 57 px from bottom = 0.57m from sprite bottom): `body.y - 0.62 + 0.57 = body.y - 0.05 = platform_top + 0.37m`.

## Investigated and ruled out

| Hypothesis                                                  | Verdict        | Evidence                                                              |
| ----------------------------------------------------------- | -------------- | --------------------------------------------------------------------- |
| Velocity-matching carry lifts the player off the platform   | **Ruled out**  | `getRidingPlatformVelocity` returns `Vector2.Zero` (`PlayerController.kt:224`); no `body.setTransform` or `applyLinearImpulse` on platform contact. |
| Foot sensor acts as collision arbiter on moving platforms   | **Ruled out**  | Foot sensor has `isSensor = true` (`PlayerController.kt:145`); sensors never produce solid contact response. The player main fixture is what rests. |
| MovingPlatform collision shape offset from visual shape     | **Ruled out**  | `LevelRenderer.kt:519-528` draws the rect at `pos ± (0.50, 0.10)`, exactly matching the collision `setAsBox(0.50, 0.10)`. |
| PlayerController applies extra "lift" on moving-platform contact | **Ruled out** | Only contact-side state mutation is `movingPlatformContactCount++` (`PlayerController.kt:215-221`), which gates a coast-damping branch, not position. |
| `WorldContactListener` performs Y-axis position adjustment  | **Ruled out**  | Lines 236-288 only call `onGroundContact` / `onMovingPlatformFootContact` counters and log; no transform writes. |
| Per-frame Y nudge in `LevelRunState` while on a platform    | **Ruled out**  | Only call site is `movingPlatforms[i].update(delta)` at line 467; no player-Y math is conditioned on platform contact. |

## Resolution options

### Option 1 — Retune `SPRITE_FOOT_OFFSET` toward 0
- **Change:** Drop all three `SPRITE_FOOT_OFFSET_*` from 0.30 → ~0.00 (or whatever matches the sprite's actual transparent bottom margin, which is ~0 based on `SpriteFactory.kt`).
- **Effect:** Sprite feet align with the collision-top on BOTH surfaces. The previous "floating above" feel on tiles (the symptom that led to introducing the offset in the first place) returns unless the underlying cause is something else (e.g. tile visual offset).
- **Tradeoff:** Resolves the moving-platform "float" entirely. Risks reintroducing the tile-floating that 0.30m was tuned to fix. Owned by T-A14 / autotuner, not this ticket.

### Option 2 — Thicken `MovingPlatform`
- **Change:** Increase platform half-height from 0.10m → ~0.30m so the 30 cm foot-overshoot stays inside opaque pixels.
- **Effect:** Visual parity with static tiles; "feet sinking" perception on both. Gameplay-relevant: platforms become 0.60m tall instead of 0.20m, changing their visual footprint and possibly their relationship to designed-in jump arcs and authored gaps in level TMX files.
- **Tradeoff:** Violates hard rule #3 (don't change moving-platform geometry — gameplay-feel-tuned). Out of scope for T-210.

### Option 3 — Render-only Y adjustment for sprite-on-platform
- **Change:** In `LevelRenderer.renderPlayer`, when `player.isOnMovingPlatform` is true, override `sy` to align sprite bottom with platform top instead of the foot-offset formula.
- **Effect:** Feet visually plant on the platform. Has zero physics impact (it's a render-only nudge).
- **Tradeoff:** Adds a special case to the renderer. Hard rule #4 says we don't touch `SPRITE_FOOT_OFFSET`, but a per-frame conditional Y override is a distinct mechanism. Risk: sprite "snaps" up by 30 cm at the moment the foot sensor begins contact, creating a visible jitter at the contact boundary unless smoothed. Smoothing adds complexity.

## Recommendation

**Option 1, scheduled under T-A14 (sprite-foot-offset autotuner).** The 0.30m foot offset is the **only knob that explains the discrepancy**, and the root cause of needing 0.30m at all is likely a separate misalignment (transparent margin in sprite, or tile renderer visual-vs-collision skew on irregular ground rects) that should be diagnosed independently. The autotuner's job is exactly this: find the value of `SPRITE_FOOT_OFFSET_*` that makes the visible feet sit on the visible surface across all surface types simultaneously. Right now, 0.30m is over-tuned for thick tiles and reveals the asymmetry on thin platforms.

**Why not Option 3 (render-only):** introducing a per-frame conditional Y override would mask the symptom on platforms while leaving the tile-sinking unfixed, and would couple the renderer to physics-contact state in a way that creates a jitter risk and a future maintenance trap.

## Files referenced

- `core/src/main/kotlin/com/sohai/platformer/entities/PlayerController.kt` (body, fixtures, contact counters, moving-platform riding doc-block at L188-206)
- `core/src/main/kotlin/com/sohai/platformer/entities/MovingPlatform.kt` (KinematicBody, `setAsBox(0.50, 0.10)`, velocity-only motion)
- `core/src/main/kotlin/com/sohai/platformer/physics/WorldContactListener.kt` (L236-288 — platform contact handling, counter-only, no Y math)
- `core/src/main/kotlin/com/sohai/platformer/world/ObstacleManager.kt` (StaticBody ground rects, L47-108)
- `core/src/main/kotlin/com/sohai/platformer/world/MapLevelLoader.kt` (TMX → `addRectNormalized` pipeline)
- `core/src/main/kotlin/com/sohai/platformer/rendering/SpriteFactory.kt` (sprite layout L114-119, `SPRITE_FOOT_OFFSET_*` L41-43)
- `core/src/main/kotlin/com/sohai/platformer/rendering/TileRenderer.kt` (tile draw — top remainders dropped, L25)
- `core/src/main/kotlin/com/sohai/platformer/screens/LevelRenderer.kt` (player sprite L678-718; moving-platform rect L517-529)
- `core/src/main/kotlin/com/sohai/platformer/screens/LevelRunState.kt` (no per-frame player-Y adjustment on platform contact)

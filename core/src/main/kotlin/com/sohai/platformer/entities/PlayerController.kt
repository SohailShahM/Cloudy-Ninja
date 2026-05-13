package com.sohai.platformer.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.sohai.platformer.Constants
import com.sohai.platformer.abilities.CharacterAbility
import com.sohai.platformer.input.InputManager

class PlayerController(world: World, x: Float, y: Float, var ability: CharacterAbility? = null) {
    val body: Body
    private var groundContactCount = 0
    private var wallRightContactCount = 0
    private var wallLeftContactCount = 0
    val isGrounded get() = groundContactCount > 0
    val isTouchingWallRight get() = wallRightContactCount > 0
    val isTouchingWallLeft get() = wallLeftContactCount > 0
    var isDead = false
    /**
     * T-130: cause-of-death tag set by whatever code path flips [isDead]. Read
     * by [com.sohai.platformer.screens.LevelRunState] at the moment death is
     * processed so the death-recap overlay can show "enemy / hazard / fall /
     * boss attack". Defaults to [DeathCause.ENEMY] (most common path); the
     * fall-below-killplane check in LevelRunState overrides to [DeathCause.FALL]
     * before reading. Cleared on respawn.
     */
    var lastDeathCause: DeathCause = DeathCause.ENEMY
    var hasReachedExit = false
    /** Set by WorldContactListener when the player contacts a portal sensor (e.g. "portal_world0"). Null when not touching any portal. */
    var portalContact: String? = null
    var onJump: (() -> Unit)? = null
    /** Set to true for one frame whenever a jump fires (any type). Read by LevelRunState for achievement checks. Reset at the start of each update(). */
    var jumpFiredThisFrame: Boolean = false
    /**
     * Invoked when a footstep should spawn a particle.
     * Args: (x, y, isLeftFoot). Coordinates are world meters at the foot position,
     * already offset slightly to the appropriate side of the player.
     */
    var onFootstep: ((Float, Float, Boolean) -> Unit)? = null
    var deathFlashTimer = 0f
    val isFlashing get() = deathFlashTimer > 0f
    // Stored spawn position (in world meters) used for respawn
    val spawnPos = Vector2()

    fun changeAbility(newAbility: CharacterAbility) {
        ability = newAbility
    }

    var isFacingRight = true
        private set

    private var coyoteTimeCounter = 0f
    private var jumpBufferCounter = 0f
    private var wallJumpLockCounter = 0f
    private var wasActionHeld = false
    private var airJumpAvailable = true
    private var prevVy = 0f

    // Player half-extents in meters (mirror of init values below; keep in sync)
    private val halfWidth = 32f / Constants.PPM / 2f
    private val halfHeight = 64f / Constants.PPM / 2f

    // Reusable scratch vectors / state for corner-correction raycasts
    private val rcFrom = Vector2()
    private val rcTo = Vector2()
    private var rcHit = false

    // Footstep particle accumulator (T-011)
    private var distanceWalkedSinceLastFootstep = 0f
    private var lastFootWasLeft = false
    companion object {
        private const val FOOTSTEP_INTERVAL_M = 0.12f
        private const val FOOTSTEP_OFFSET_X = 0.10f   // ~half-width of player body in meters
        private const val FOOTSTEP_OFFSET_Y = 0.32f   // foot position below body center
    }


    init {
        val def = BodyDef()
        def.type = BodyDef.BodyType.DynamicBody
        def.position.set(x / Constants.PPM, y / Constants.PPM)
        def.fixedRotation = true // Player shouldn't tip over

        body = world.createBody(def)
        body.userData = this

        // initialize spawn position to the starting body position
        spawnPos.set(body.position)
        // Main body (Capsule/Box)
        val shape = PolygonShape()
        val width = 32f / Constants.PPM / 2f
        val height = 64f / Constants.PPM / 2f
        shape.setAsBox(width, height)

        val fixtureDef = FixtureDef()
        fixtureDef.shape = shape
        fixtureDef.density = 1f
        // Small friction so Box2D can carry the player on moving platforms.
        // Combined with platform friction = 1, ground friction = 0.5, this gives
        // ~0.4 effective friction on platforms (decent ride) and ~0.31 on ground
        // (still slidey enough that movement-input override dominates).
        fixtureDef.friction = 0.25f
        fixtureDef.filter.categoryBits = Constants.BIT_PLAYER
        // Include hazards so active hazard fixtures can trigger death contacts.
        fixtureDef.filter.maskBits = (
            Constants.BIT_GROUND.toInt() or
            Constants.BIT_WALL.toInt() or
            Constants.BIT_HAZARD.toInt() or
            Constants.BIT_CHECKPOINT.toInt() or
            Constants.BIT_EXIT.toInt() or
            Constants.BIT_ENEMY.toInt() or
            Constants.BIT_ECOTOKEN.toInt()
        ).toShort()

        body.createFixture(fixtureDef).userData = "player_body"

        // Foot sensor
        val footShape = PolygonShape()
        footShape.setAsBox(width * 0.8f, 0.1f, Vector2(0f, -height), 0f)
        val footDef = FixtureDef()
        footDef.shape = footShape
        footDef.isSensor = true
        footDef.filter.categoryBits = Constants.BIT_PLAYER
        footDef.filter.maskBits = Constants.BIT_GROUND
        body.createFixture(footDef).userData = "player_foot"

        // Right wall sensor
        val rightSensor = PolygonShape()
        rightSensor.setAsBox(0.1f, height * 0.8f, Vector2(width, 0f), 0f)
        val rightDef = FixtureDef()
        rightDef.shape = rightSensor
        rightDef.isSensor = true
        rightDef.filter.categoryBits = Constants.BIT_PLAYER
        rightDef.filter.maskBits = Constants.BIT_WALL
        body.createFixture(rightDef).userData = "player_wall_right"

        // Left wall sensor
        val leftSensor = PolygonShape()
        leftSensor.setAsBox(0.1f, height * 0.8f, Vector2(-width, 0f), 0f)
        val leftDef = FixtureDef()
        leftDef.shape = leftSensor
        leftDef.isSensor = true
        leftDef.filter.categoryBits = Constants.BIT_PLAYER
        leftDef.filter.maskBits = Constants.BIT_WALL
        body.createFixture(leftDef).userData = "player_wall_left"

        shape.dispose()
        footShape.dispose()
        rightSensor.dispose()
        leftSensor.dispose()
    }

    /**
     * Set player's spawn (checkpoint) position in world meters.
     */
    fun setSpawn(pos: Vector2) {
        // Offset spawn slightly higher so player doesn't spawn inside the ground
        spawnPos.set(pos.x, pos.y + 0.2f)
    }

    fun onGroundContact(begin: Boolean) {
        groundContactCount = if (begin) groundContactCount + 1 else (groundContactCount - 1).coerceAtLeast(0)
    }

    // -------------------------------------------------------------------------
    // Moving-platform riding
    //
    // Implementation note: we previously tracked active MovingPlatform contacts
    // in a Map<MovingPlatform, Int> via the contact listener and applied the
    // platform's velocity each frame. That introduced an intermittent native
    // Box2D access-violation when a stale platform reference outlived a contact
    // (likely tied to teleport-respawn and contact end events). We now use a
    // simpler model: the WorldContactListener temporarily applies friction to
    // the player when the foot sensor touches a moving-platform fixture, so
    // Box2D itself carries the player. No Java references to platform bodies
    // are retained, which eliminates the lifetime hazard.
    //
    // Kept as no-ops for source compatibility with the contact listener API.
    // -------------------------------------------------------------------------

    fun onPlatformContact(platform: MovingPlatform, begin: Boolean) {
        // Intentionally empty — see note above.
    }

    /** Always returns Vector2.Zero now; physics friction handles the carry. */
    fun getRidingPlatformVelocity(): Vector2 = Vector2.Zero

    /**
     * Defensive guard: returns true only if [platformBody] is safe to read from.
     *
     * Before the friction-based carry refactor, `platformContacts` stored raw
     * Box2D Body references that could become stale after a teleport-respawn (the
     * contact-end event was missed, leaving a destroyed body in the map). Reading
     * `body.position` on such a body caused EXCEPTION_ACCESS_VIOLATION inside
     * `Body.jniGetPosition` (T-017).
     *
     * This helper is kept here as a reference for any future code that re-introduces
     * direct Body position reads from contact-derived references. Always call it
     * before touching position/velocity on a body obtained outside the player's own
     * init block.
     */
    fun isPlatformBodyValid(platformBody: Body?): Boolean {
        if (platformBody == null) {
            Gdx.app.error("PlayerController", "isPlatformBodyValid: null body — stale reference detected")
            return false
        }
        return try {
            // isActive() is a cheap JNI call; it will throw or return false if the
            // native proxy has been freed, letting us catch the crash early in Java.
            val active = platformBody.isActive
            if (!active) {
                Gdx.app.error(
                    "PlayerController",
                    "isPlatformBodyValid: body 0x${platformBody.hashCode().toString(16)} is INACTIVE — skipping position read"
                )
            }
            active
        } catch (e: Exception) {
            Gdx.app.error(
                "PlayerController",
                "isPlatformBodyValid: body 0x${platformBody.hashCode().toString(16)} threw on isActive: ${e.message}"
            )
            false
        }
    }

    fun onWallRightContact(begin: Boolean) {
        wallRightContactCount = if (begin) wallRightContactCount + 1 else (wallRightContactCount - 1).coerceAtLeast(0)
    }

    fun onWallLeftContact(begin: Boolean) {
        wallLeftContactCount = if (begin) wallLeftContactCount + 1 else (wallLeftContactCount - 1).coerceAtLeast(0)
    }

    /**
     * Respawn the player at the last saved spawn position and clear death state.
     */
    fun respawn() {
        body.setTransform(spawnPos.x, spawnPos.y, 0f)
        body.linearVelocity = Vector2.Zero
        body.gravityScale = 1f  // reset any apex-hang scaling that was active at death
        isDead = false
        hasReachedExit = false
        portalContact = null
        airJumpAvailable = true
        deathFlashTimer = 1.2f  // blink for 1.2s after respawn
        groundContactCount = 0
        wallRightContactCount = 0
        wallLeftContactCount = 0
        // (No platformContacts to clear — friction-based carry has no Java state.)
    }
    fun update(deltaTime: Float) {
        jumpFiredThisFrame = false   // reset each frame; set below when a jump actually fires
        val vel = body.linearVelocity

        // Asymmetric gravity & apex hang (GDD §2.2) — placed first, before any
        // velocity writes, so gravity scale is set for this physics step.
        //   - rising while jump held & near apex: half gravity (apex hang)
        //   - falling (vy <= 0): 1.45× gravity for snappy descent
        //   - fast-fall (down held, airborne): 2.5× gravity for quick drop
        //   - otherwise (rising, jump released): normal gravity (variable jump)
        val vy = body.linearVelocity.y
        val fastFalling = !isGrounded && InputManager.isDownPressed()
        when {
            fastFalling -> body.gravityScale = 2.5f
            vy > 0 && InputManager.isJumpHeld() && Math.abs(vy) < Constants.PLAYER_APEX_VEL_THRESHOLD ->
                body.gravityScale = Constants.PLAYER_JUMP_HOLD_GRAVITY_MUL
            vy <= 0 -> body.gravityScale = Constants.GRAVITY_FALL_MUL
            else    -> body.gravityScale = 1f
        }

        // Terminal velocity cap (GDD §2.2) — after gravity scale is set.
        val cap = if (InputManager.isDownPressed()) Constants.PLAYER_FAST_FALL else Constants.PLAYER_MAX_FALL
        if (body.linearVelocity.y < -cap) {
            body.linearVelocity = Vector2(body.linearVelocity.x, -cap)
        }

        if (isGrounded || isTouchingWallLeft || isTouchingWallRight) {
            airJumpAvailable = true
        }

        // Update timers
        if (wallJumpLockCounter > 0f) wallJumpLockCounter -= deltaTime
        if (deathFlashTimer > 0f) deathFlashTimer -= deltaTime


        if (isGrounded) {
            coyoteTimeCounter = Constants.COYOTE_TIME
        } else {
            coyoteTimeCounter -= deltaTime
        }

        if (InputManager.isJumpPressed()) {
            jumpBufferCounter = Constants.JUMP_BUFFER_TIME
        } else {
            jumpBufferCounter -= deltaTime
        }

        // Movement.
        //
        // For moving-platform riding we let Box2D friction handle the carry
        // (see PlayerController class header). To avoid stomping that friction
        // when the player is idle and grounded, we leave the velocity alone in
        // that case — Box2D-applied friction gradually pulls the player toward
        // the platform's velocity instead.
        if (wallJumpLockCounter <= 0f) {
            if (InputManager.isMovingLeft()) {
                body.linearVelocity = Vector2(-Constants.PLAYER_SPEED, vel.y)
                isFacingRight = false
            } else if (InputManager.isMovingRight()) {
                body.linearVelocity = Vector2(Constants.PLAYER_SPEED, vel.y)
                isFacingRight = true
            } else if (!isGrounded) {
                // Air: standard horizontal friction
                body.linearVelocity = Vector2(vel.x * 0.5f, vel.y)
            }
            // else: grounded + idle → let Box2D friction pull us along
        }

        // Jumping
        if (jumpBufferCounter > 0f) {
            if (coyoteTimeCounter > 0f) {
                body.linearVelocity = Vector2(body.linearVelocity.x, Constants.PLAYER_JUMP_IMPULSE)
                jumpBufferCounter = 0f
                coyoteTimeCounter = 0f
                jumpFiredThisFrame = true
                onJump?.invoke()
            } else if (isTouchingWallRight) {
                body.linearVelocity = Vector2(-Constants.PLAYER_WALL_JUMP_IMPULSE_X, Constants.PLAYER_WALL_JUMP_IMPULSE_Y)
                jumpBufferCounter = 0f
                wallJumpLockCounter = Constants.WALL_JUMP_LOCK_TIME
                jumpFiredThisFrame = true
                onJump?.invoke()
            } else if (isTouchingWallLeft) {
                body.linearVelocity = Vector2(Constants.PLAYER_WALL_JUMP_IMPULSE_X, Constants.PLAYER_WALL_JUMP_IMPULSE_Y)
                jumpBufferCounter = 0f
                wallJumpLockCounter = Constants.WALL_JUMP_LOCK_TIME
                jumpFiredThisFrame = true
                onJump?.invoke()
            } else if (airJumpAvailable) {
                body.linearVelocity = Vector2(body.linearVelocity.x, Constants.PLAYER_JUMP_IMPULSE)
                jumpBufferCounter = 0f
                airJumpAvailable = false
                jumpFiredThisFrame = true
                onJump?.invoke()
            }
        }

        // Corner correction: when rising into an overhead obstacle that only
        // clips the head by ≤ CORNER_CORRECT_M, nudge horizontally past the
        // corner instead of letting the body kill vertical velocity.
        // Only triggered while genuinely rising (this frame and last frame),
        // to avoid interfering with deliberate ceiling contacts.
        if (body.linearVelocity.y > 0f && prevVy > 0f) {
            tryCornerCorrect()
        }

        // Variable jump height — release-cut
        if (!InputManager.isJumpHeld() && body.linearVelocity.y > 0) {
            body.linearVelocity = Vector2(body.linearVelocity.x, body.linearVelocity.y * Constants.PLAYER_JUMP_CUT_MUL)
        }

        // Fast-fall: pressing down while airborne gives an immediate downward
        // kick so the response feels instant (gravity scale handles the rest).
        val downHeld = !isGrounded && InputManager.isDownPressed()
        if (downHeld && body.linearVelocity.y > -2f) {
            body.applyLinearImpulse(Vector2(0f, -8f), body.position, true)
        }

        // Wall sliding
        val touchingWall = (isTouchingWallLeft && InputManager.isMovingLeft()) ||
                           (isTouchingWallRight && InputManager.isMovingRight())

        if (touchingWall && !isGrounded && body.linearVelocity.y < 0) {
            // Slide down slower
            if (body.linearVelocity.y < Constants.PLAYER_WALL_SLIDE_SPEED) {
                body.linearVelocity = Vector2(body.linearVelocity.x, Constants.PLAYER_WALL_SLIDE_SPEED)
            }
        }

        // Footstep particles (T-011): accumulate horizontal distance while
        // grounded; every FOOTSTEP_INTERVAL_M, spawn an alternating L/R foot puff.
        if (isGrounded) {
            val vx = body.linearVelocity.x
            if (vx != 0f) {
                distanceWalkedSinceLastFootstep += Math.abs(vx) * deltaTime
                if (distanceWalkedSinceLastFootstep >= FOOTSTEP_INTERVAL_M) {
                    distanceWalkedSinceLastFootstep = 0f
                    lastFootWasLeft = !lastFootWasLeft
                    val px = body.position.x + (if (lastFootWasLeft) -FOOTSTEP_OFFSET_X else FOOTSTEP_OFFSET_X)
                    val py = body.position.y - FOOTSTEP_OFFSET_Y
                    onFootstep?.invoke(px, py, lastFootWasLeft)
                }
            }
        } else {
            // Reset accumulator so the first step after landing isn't an instant puff.
            distanceWalkedSinceLastFootstep = 0f
        }

        // Update ability if one is assigned
        ability?.update(deltaTime)

        // Handle ability input
        val actionJustPressed = InputManager.isActionJustPressed()
        val actionHeld = InputManager.isActionPressed()

        if (actionJustPressed) {
            ability?.onActionPressed()
        }
        if (actionHeld) {
            ability?.onActionHeld()
        }
        if (wasActionHeld && !actionHeld) {
            ability?.onActionReleased()
        }
        wasActionHeld = actionHeld

        prevVy = body.linearVelocity.y
    }

    /**
     * If the player is rising and the head clips an overhead obstacle by
     * ≤ CORNER_CORRECT_M, snap the body horizontally to a clear column so
     * the jump continues instead of stalling on the corner.
     *
     * Implementation: cast a short ray straight up from the head over a
     * distance equal to CORNER_CORRECT_M. If it hits, scan small horizontal
     * offsets (left and right) and snap to the nearest one whose upward ray
     * is clear. Only ground/wall fixtures are considered as ceilings.
     */
    private fun tryCornerCorrect() {
        val world = body.world ?: return
        val pos = body.position
        // A small probe distance above the body's top edge.
        val probe = Constants.CORNER_CORRECT_M

        // Cast straight up from the centerline of the head.
        if (!ceilingHit(world, pos.x, pos.y + halfHeight, probe)) return

        // Scan horizontal offsets in 1cm steps up to the player's half-width
        // (max sideways nudge). Try alternating left/right at increasing
        // distance and pick the first clear column.
        val maxNudge = halfWidth // do not move so far that we'd skip over a wall edge
        val step = 0.01f
        var d = step
        while (d <= maxNudge + 1e-4f) {
            // Try right
            val rx = pos.x + d
            if (!ceilingHit(world, rx, pos.y + halfHeight, probe) &&
                !ceilingHit(world, rx + halfWidth * 0.9f, pos.y + halfHeight, probe) &&
                !ceilingHit(world, rx - halfWidth * 0.9f, pos.y + halfHeight, probe)) {
                body.setTransform(rx, pos.y, body.angle)
                return
            }
            // Try left
            val lx = pos.x - d
            if (!ceilingHit(world, lx, pos.y + halfHeight, probe) &&
                !ceilingHit(world, lx + halfWidth * 0.9f, pos.y + halfHeight, probe) &&
                !ceilingHit(world, lx - halfWidth * 0.9f, pos.y + halfHeight, probe)) {
                body.setTransform(lx, pos.y, body.angle)
                return
            }
            d += step
        }
        // No clear column within nudge range — leave the player alone; normal
        // collision response will absorb the upward velocity.
    }

    /**
     * Returns true if a vertical upward ray from (x, y) over [distance] meters
     * hits a non-sensor ground/wall fixture.
     */
    /**
     * T-170: Draw the player's high-contrast silhouette rectangle using
     * [color] as the base. Called from [com.sohai.platformer.screens.LevelRenderer.renderPlayer]
     * inside an open Filled ShapeRenderer block, only when the high-contrast
     * accessibility flag is on.
     *
     * Bounds: 0.28m wide x 0.42m tall, centred horizontally on the body and
     * vertically offset to match the sprite's body+legs footprint (the same
     * geometry the pre-T-170 LevelRenderer overlay used at lines 665-673).
     *
     * The caller is responsible for pre-multiplying the death-fade
     * `playerAlpha` into [color]'s alpha channel so the silhouette fades out
     * along with the sprite during the death animation.
     */
    fun drawHighContrast(renderer: ShapeRenderer, color: Color) {
        val p = body.position
        renderer.color = color
        // 28x42 px at PPM=100 → 0.28x0.42 m. Match the sprite's body
        // footprint: body centre is at p.y; sprite extends from sy = p.y - 0.32
        // up by ~0.64 (the 64px sprite). The silhouette covers the body + a
        // bit of leg, centred on the same horizontal axis as the sprite.
        renderer.rect(p.x - 0.14f, p.y - 0.20f, 0.28f, 0.42f)
    }

    private fun ceilingHit(world: World, x: Float, y: Float, distance: Float): Boolean {
        rcHit = false
        rcFrom.set(x, y)
        rcTo.set(x, y + distance)
        world.rayCast({ fixture, _, _, _ ->
            if (fixture.isSensor) return@rayCast -1f
            val cat = fixture.filterData.categoryBits.toInt()
            val solid = (cat and (Constants.BIT_GROUND.toInt() or Constants.BIT_WALL.toInt())) != 0
            if (solid) {
                rcHit = true
                0f // stop at first solid hit
            } else {
                -1f // ignore
            }
        }, rcFrom, rcTo)
        return rcHit
    }
}


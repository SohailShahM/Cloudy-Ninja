package com.sohai.platformer

object Constants {
    // ─── Build identity (T-100) ──────────────────────────────────────────────
    // Manually maintained for the alpha pre-launch window. Bump these by hand
    // when cutting a release; do NOT try to auto-derive from git/gradle yet
    // (the build script doesn't expose either, and we want repeatable headless
    // smoke output).
    const val BUILD_VERSION = "0.1.0"
    const val BUILD_DATE    = "2026-05-12"

    // Virtual resolution for camera/HUD viewport.
    const val VIRTUAL_WIDTH = 1280f
    const val VIRTUAL_HEIGHT = 720f

    // Pixels Per Meter - used to convert physics world units to screen pixels
    const val PPM = 100f

    // Physics constants
    const val GRAVITY = -32f                      // Was -25; combined with halved-on-rise gravity gives Celeste-style arc
    const val TIME_STEP = 1 / 60f
    const val VELOCITY_ITERATIONS = 6
    const val POSITION_ITERATIONS = 2

    // Frame-time safety clamp (helps avoid extreme jitter after window focus loss / GC spikes)
    const val MAX_FRAME_DELTA = 1 / 20f

    // Player horizontal movement
    //
    // T-175 — Movement responsiveness baseline (Celeste-like snap-to-zero).
    // The controller writes velocity directly when a horizontal input is held
    // (acceleration is effectively instantaneous — top speed reached in 1 frame).
    // When no input is held, we multiply v.x by GROUND_COAST_DAMPING (grounded)
    // or AIR_COAST_DAMPING (airborne) each frame to bleed off coast.
    //
    // GROUND_COAST_DAMPING = 0.55 → velocity drops to ~5% of top speed in 5
    // frames at 60Hz (9 m/s × 0.55^5 ≈ 0.45 m/s), i.e. effectively stopped
    // within ~80ms. Increase toward 1.0 for more slide; decrease toward 0 for
    // an even harder stop (0 = instant kill, which feels robotic).
    //
    // AIR_COAST_DAMPING = 0.5 preserves the pre-T-175 air-idle behaviour
    // (jumps cut their horizontal speed quickly when no input is held). Raise
    // toward 0.9 if you want jumps to carry more momentum into a release.
    const val PLAYER_SPEED                = 9f    // Top horizontal speed (m/s). UNCHANGED by T-175.
    const val PLAYER_RUN_ACCEL            = 40f   // m/s² — currently UNUSED (controller writes velocity directly). Kept for future acceleration-based tuning.
    const val PLAYER_RUN_DECEL            = 16f   // m/s² — currently UNUSED (replaced by GROUND_COAST_DAMPING multiplier). Kept for future acceleration-based tuning.
    const val PLAYER_AIR_ACCEL_MUL        = 0.65f // Air control = 65% of ground (Celeste-ish). Currently UNUSED.
    const val GROUND_COAST_DAMPING        = 0.55f // T-175: velocity multiplier per frame when grounded + no horizontal input. Lower = snappier stop.
    const val AIR_COAST_DAMPING           = 0.5f  // T-175: velocity multiplier per frame when airborne + no horizontal input. Higher = more air momentum.

    // Player jump
    const val PLAYER_JUMP_IMPULSE          = 13f    // Was 12
    const val PLAYER_JUMP_HOLD_GRAVITY_MUL = 0.5f   // Half-gravity while jump held + rising → apex hang
    const val PLAYER_APEX_VEL_THRESHOLD    = 4f     // |vy| below this near apex → bonus hang
    const val PLAYER_JUMP_CUT_MUL          = 0.4f   // Was 0.5; sharper variable jump
    const val GRAVITY_FALL_MUL             = 1.45f  // Asymmetric: falling gravity 45% stronger
    const val PLAYER_MAX_FALL              = 18f    // m/s terminal velocity (was uncapped)
    const val PLAYER_FAST_FALL             = 28f    // m/s terminal when down held

    // Player wall
    const val PLAYER_WALL_JUMP_IMPULSE_X = 7.5f   // Was 6; needs more push to clear shafts
    const val PLAYER_WALL_JUMP_IMPULSE_Y = 11f    // Was 10
    const val PLAYER_WALL_SLIDE_SPEED    = -2.5f  // Was -2; less floaty

    // Forgiveness windows (matches Celeste public source)
    const val COYOTE_TIME       = 0.10f   // Was 0.15; too generous made level3 trivial
    const val JUMP_BUFFER_TIME  = 0.10f
    const val WALL_JUMP_LOCK_TIME = 0.13f // Was 0.15; earlier reclaim of horizontal control
    const val CORNER_CORRECT_M  = 0.06f   // Nudge through clipped jump corners (~6 cm)

    // Collision bits (powers of 2)
    const val BIT_PLAYER: Short = 2
    const val BIT_GROUND: Short = 4
    const val BIT_WALL: Short = 8
    const val BIT_HAZARD: Short = 16
    const val BIT_CHECKPOINT: Short = 32
    const val BIT_DROPLET: Short = 64
    const val BIT_EXIT: Short = 128
    const val BIT_ENEMY: Short = 256.toShort()
    const val BIT_ECOTOKEN: Short = 512.toShort()

    // Debug / profiling
    const val PERF_LOG_INTERVAL_SECONDS = 1f
    const val AUTOPILOT_DEFAULT_SECONDS = 3f

    // Smoke-test mode: when true, the game stays in whichever level it was
    // launched into and refuses screen transitions (portal contact + level-
    // complete). This prevents the smoke autopilot from hopping levels and
    // resetting the auto-quit timer in a fresh GameScreen instance — which
    // was hanging CI on every level reachable by horizontal walking.
    @JvmField val SMOKE_MODE: Boolean = java.lang.Boolean.getBoolean("cloudy.smokeMode")
}

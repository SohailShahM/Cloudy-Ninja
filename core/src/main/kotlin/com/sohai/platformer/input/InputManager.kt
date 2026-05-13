package com.sohai.platformer.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controllers
import com.sohai.platformer.audio.MusicManager
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.persist.defaultKeybinds

object InputManager {
    // --- Cached keybinds (loaded once, refreshed via reloadKeybinds()) ---
    private var keybinds: Map<String, Int> = defaultKeybinds()

    /** Reload keybinds from persisted settings. Call after the user changes a binding. */
    fun reloadKeybinds() {
        keybinds = SettingsManager.load().keybinds
    }

    private fun keyFor(action: String): Int = keybinds[action] ?: defaultKeybinds()[action] ?: -1

    /**
     * T-171 (Phase A): true iff [keycode] is currently bound to the master-
     * mute action. Exposed so the [GlobalInputRouter] M-key adapter wired up
     * in [com.sohai.platformer.Main.create] can stay rebind-aware without
     * reaching into the private [keybinds] cache. Re-reads via [keyFor] so a
     * user-changed binding takes effect immediately after
     * [reloadKeybinds] without re-registering the adapter.
     */
    fun isMuteKey(keycode: Int): Boolean = keycode == keyFor("mute")
    // --- On-screen button state (set by HUD) ---
    var uiLeftPressed = false
    var uiRightPressed = false
    var uiJumpPressed = false
    var uiActionPressed = false
    var uiActionJustPressed = false

    // --- Controller state (refreshed once per frame by update()) ---
    private var ctrlLeft = false
    private var ctrlRight = false
    private var ctrlJumpHeld = false
    private var ctrlJumpJustPressed = false
    private var ctrlActionHeld = false
    private var ctrlActionJustPressed = false
    private var prevCtrlJump = false
    private var prevCtrlAction = false

    // --- Debug override (for autopilot/repro on desktop) ---
    private var debugOverrideEnabled = false
    private var debugLeftHeld = false
    private var debugRightHeld = false
    private var debugJumpHeld = false
    private var debugActionHeld = false
    private var debugJumpJustPressed = false
    private var debugActionJustPressed = false

    fun setDebugOverrideEnabled(enabled: Boolean) {
        debugOverrideEnabled = enabled
        if (!enabled) {
            debugLeftHeld = false; debugRightHeld = false
            debugJumpHeld = false; debugActionHeld = false
            debugJumpJustPressed = false; debugActionJustPressed = false
        }
    }

    fun setDebugHeld(left: Boolean, right: Boolean, jump: Boolean, action: Boolean) {
        debugLeftHeld = left; debugRightHeld = right
        debugJumpHeld = jump; debugActionHeld = action
    }

    fun triggerDebugJumpJustPressed() { debugJumpJustPressed = true }
    fun triggerDebugActionJustPressed() { debugActionJustPressed = true }

    /**
     * Must be called once per frame (from GameScreen.update) before any
     * isXxx() query. Snapshots controller state so just-pressed flags are
     * stable across multiple reads in the same frame.
     */
    fun update() {
        val ctrl = Controllers.getControllers().firstOrNull()
        if (ctrl == null) {
            ctrlLeft = false; ctrlRight = false
            ctrlJumpHeld = false; ctrlJumpJustPressed = false
            ctrlActionHeld = false; ctrlActionJustPressed = false
            prevCtrlJump = false; prevCtrlAction = false
            return
        }

        val mapping = ctrl.mapping
        val axisX = ctrl.getAxis(mapping.axisLeftX)
        ctrlLeft  = axisX < -0.3f || ctrl.getButton(mapping.buttonDpadLeft)
        ctrlRight = axisX >  0.3f || ctrl.getButton(mapping.buttonDpadRight)

        val jumpNow   = ctrl.getButton(mapping.buttonA)
        val actionNow = ctrl.getButton(mapping.buttonX)

        ctrlJumpHeld        = jumpNow
        ctrlJumpJustPressed = jumpNow && !prevCtrlJump
        ctrlActionHeld        = actionNow
        ctrlActionJustPressed = actionNow && !prevCtrlAction

        prevCtrlJump   = jumpNow
        prevCtrlAction = actionNow
    }

    // NOTE: Raw screen-quadrant touch heuristics were removed (P0 bug fix).
    // They double-fired with the on-screen Vis UI buttons (a single tap on the
    // on-screen Action button also registered as Jump because both interpret
    // right-half touches). All touch input now flows exclusively through the
    // HUD's button listeners → uiLeftPressed/uiRightPressed/uiJumpPressed/uiActionPressed.

    fun isMovingLeft(): Boolean {
        if (debugOverrideEnabled && debugLeftHeld) return true
        if (uiLeftPressed || ctrlLeft) return true
        if (Gdx.input.isKeyPressed(keyFor("left")) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) return true
        return false
    }

    fun isMovingRight(): Boolean {
        if (debugOverrideEnabled && debugRightHeld) return true
        if (uiRightPressed || ctrlRight) return true
        if (Gdx.input.isKeyPressed(keyFor("right")) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) return true
        return false
    }

    fun isJumpPressed(): Boolean {
        if (debugOverrideEnabled && debugJumpJustPressed) { debugJumpJustPressed = false; return true }
        if (uiJumpPressed || ctrlJumpJustPressed) return true
        if (Gdx.input.isKeyJustPressed(keyFor("jump")) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) return true
        return false
    }

    fun isJumpHeld(): Boolean {
        if (debugOverrideEnabled && debugJumpHeld) return true
        if (uiJumpPressed || ctrlJumpHeld) return true
        if (Gdx.input.isKeyPressed(keyFor("jump")) ||
            Gdx.input.isKeyPressed(Input.Keys.W) ||
            Gdx.input.isKeyPressed(Input.Keys.UP)) return true
        return false
    }

    fun isActionPressed(): Boolean {
        if (debugOverrideEnabled && debugActionHeld) return true
        if (uiActionPressed || ctrlActionHeld) return true
        if (Gdx.input.isKeyPressed(keyFor("action"))) return true
        return false
    }

    fun isDownPressed(): Boolean {
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) return true
        val ctrl = Controllers.getControllers().firstOrNull()
        if (ctrl != null) {
            val axisY = ctrl.getAxis(ctrl.mapping.axisLeftY)
            if (axisY > 0.5f) return true
            if (ctrl.getButton(ctrl.mapping.buttonDpadDown)) return true
        }
        return false
    }

    fun isSwapJustPressed(): Boolean {
        if (Gdx.input.isKeyJustPressed(keyFor("swap"))) return true
        return false
    }

    fun isActionJustPressed(): Boolean {
        if (debugOverrideEnabled && debugActionJustPressed) { debugActionJustPressed = false; return true }
        if (ctrlActionJustPressed) return true
        if (uiActionJustPressed) { uiActionJustPressed = false; return true }
        if (Gdx.input.isKeyJustPressed(keyFor("action"))) return true
        return false
    }

    /**
     * T-133: returns true while the rebindable `restart` key (default R) is
     * physically held. Used by [com.sohai.platformer.screens.GameScreen] to
     * drive a 0.5s hold-to-restart gesture with a radial progress indicator
     * — tap-R is a no-op by design.
     *
     * Intentionally bypasses the debug-override path: the smoke autopilot
     * never asserts a restart, so we keep the raw keyboard probe simple and
     * autopilot-safe. (Verified: `LevelRunState` only calls `setDebugHeld`
     * for left/right/jump/action; it never touches restart.)
     */
    fun isRestartHeld(): Boolean {
        return Gdx.input.isKeyPressed(keyFor("restart"))
    }

    /**
     * T-118: poll the master-mute hotkey (default M, rebindable in Settings →
     * Controls). On a fresh key edge, invokes [performMuteToggle].
     *
     * **T-171 (Phase A):** gated on `!GlobalInputRouter.isActive()` so the
     * router's M-key adapter handles the action whenever a migrated screen
     * (e.g. [com.sohai.platformer.screens.MainMenuScreen]) owns the input
     * processor. Unmigrated screens leave the router inactive, the gate
     * passes, and this polling path remains the legacy fallback. Phase B
     * (T-172) deletes the polling path once every screen cooperates.
     *
     * Returns `true` if the toggle fired this frame and the new state is
     * "muted" (so the caller can flash a `[MUTED]` toast on the active screen).
     * Returns `false` on no edge, on toggle-off, or when the router is
     * active — quiet by design.
     *
     * Designed to be called from a single global hook ([com.sohai.platformer.Main.render])
     * so the hotkey works from any screen without each Screen subclass
     * having to opt in.
     */
    fun pollMuteHotkey(): Boolean {
        if (GlobalInputRouter.isActive()) return false
        if (!Gdx.input.isKeyJustPressed(keyFor("mute"))) return false
        return performMuteToggle()
    }

    /**
     * T-118 / T-171 (Phase A): the action half of the master-mute hotkey,
     * extracted from [pollMuteHotkey] so it can be invoked from **both** the
     * legacy polling path (when no screen has cooperated with the router) AND
     * the [GlobalInputRouter]-registered keyDown adapter wired up in
     * [com.sohai.platformer.Main.create]. Single source of truth for the
     * toggle + persist + propagate + log sequence; two trigger paths.
     *
     * Flips [com.sohai.platformer.persist.Settings.muted], persists via
     * [SettingsManager.update], and propagates the new state to
     * [MusicManager] / [SoundManager] so the existing T-105 output gate
     * engages immediately. The master-volume slider value is **not** touched —
     * mute is a separate flag that gates output to 0 without overwriting
     * [com.sohai.platformer.persist.Settings.volMaster].
     *
     * Returns `true` if the new state is "muted".
     */
    fun performMuteToggle(): Boolean {
        val next = SettingsManager.update { it.copy(muted = !it.muted) }
        MusicManager.setMuted(next.muted)
        SoundManager.setMuted(next.muted)
        if (next.muted) {
            Gdx.app.log("T-118", "[MUTED]")
        } else {
            Gdx.app.log("T-118", "[UNMUTED]")
        }
        return next.muted
    }
}

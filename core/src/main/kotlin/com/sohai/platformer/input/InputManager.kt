package com.sohai.platformer.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controllers

object InputManager {
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
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) return true
        return false
    }

    fun isMovingRight(): Boolean {
        if (debugOverrideEnabled && debugRightHeld) return true
        if (uiRightPressed || ctrlRight) return true
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) return true
        return false
    }

    fun isJumpPressed(): Boolean {
        if (debugOverrideEnabled && debugJumpJustPressed) { debugJumpJustPressed = false; return true }
        if (uiJumpPressed || ctrlJumpJustPressed) return true
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) return true
        return false
    }

    fun isJumpHeld(): Boolean {
        if (debugOverrideEnabled && debugJumpHeld) return true
        if (uiJumpPressed || ctrlJumpHeld) return true
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyPressed(Input.Keys.W) ||
            Gdx.input.isKeyPressed(Input.Keys.UP)) return true
        return false
    }

    fun isActionPressed(): Boolean {
        if (debugOverrideEnabled && debugActionHeld) return true
        if (uiActionPressed || ctrlActionHeld) return true
        if (Gdx.input.isKeyPressed(Input.Keys.E)) return true
        return false
    }

    fun isDownPressed(): Boolean {
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) return true
        val ctrl = Controllers.getControllers().firstOrNull()
        if (ctrl != null) {
            val axisY = ctrl.getAxis(ctrl.mapping.axisLeftY)
            if (axisY > 0.5f) return true
            if (ctrl.getButton(ctrl.mapping.buttonDpadDown)) return true
        }
        return false
    }

    fun isActionJustPressed(): Boolean {
        if (debugOverrideEnabled && debugActionJustPressed) { debugActionJustPressed = false; return true }
        if (ctrlActionJustPressed) return true
        if (uiActionJustPressed) { uiActionJustPressed = false; return true }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) return true
        return false
    }
}

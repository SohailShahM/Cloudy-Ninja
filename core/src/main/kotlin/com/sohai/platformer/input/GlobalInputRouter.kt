package com.sohai.platformer.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor

/**
 * T-171 (Phase A): single root [InputMultiplexer] for screen-local input and
 * global hotkeys.
 *
 * **Why this exists.** Every [com.badlogic.gdx.Screen] in this codebase does
 * `Gdx.input.inputProcessor = stage` in its `show()` / `init`, which clobbers
 * whatever root multiplexer the previous screen left behind. Today T-147's F12
 * screenshot and T-118's M-key mute both ship as `Gdx.input.isKeyJustPressed`
 * polls inside `Main.render()` / `InputManager` precisely because installing
 * a real multiplexer was too risky as long as every screen reassigns the input
 * processor on every show.
 *
 * Phase A is the conservative first step. It introduces this router, gates the
 * existing F12 + M-key polls on [isActive] (so they remain the legacy fallback
 * for unmigrated screens), and migrates **only** [com.sohai.platformer.screens.MainMenuScreen]
 * to the new pushScreen/popScreen API as a proof. Phase B (T-172) does the
 * bulk-screen migration and deletes the polling fallbacks once every screen
 * cooperates with the router.
 *
 * ### API contract
 *
 *  - [install] sets `Gdx.input.inputProcessor` to this router's internal mux.
 *    Idempotent — safe to call from any screen's `show()` that wants the
 *    router active. Screens that haven't been migrated continue to clobber
 *    `Gdx.input.inputProcessor` directly; [isActive] returns false on those
 *    screens, which is the contract the polling-fallback gates rely on.
 *  - [register] appends a global [InputProcessor] (e.g. an F12 / M-key
 *    [com.badlogic.gdx.InputAdapter]) at the **end** of the mux. Screen
 *    processors fire first; globals only see events the screen didn't consume.
 *  - [pushScreen] inserts a screen processor at the **front** (highest
 *    priority). Call from `Screen.show()`.
 *  - [popScreen] removes a specific processor. Call from `Screen.hide()`.
 *  - [isActive] returns true iff `Gdx.input.inputProcessor` is still the
 *    router's mux. Returns false the moment any legacy screen does
 *    `Gdx.input.inputProcessor = stage`. The polling fallbacks in
 *    [com.sohai.platformer.Main.render] and
 *    [com.sohai.platformer.input.InputManager.pollMuteHotkey] gate on
 *    `!isActive()` so they don't double-fire when the router is also wired
 *    to handle the same key.
 */
object GlobalInputRouter {
    private val mux = InputMultiplexer()

    /**
     * Install the router as `Gdx.input.inputProcessor`. Idempotent — safe to
     * call repeatedly. Migrated screens call this from `show()` so the router
     * comes back online after an unmigrated sibling clobbered it.
     */
    fun install() {
        Gdx.input.inputProcessor = mux
    }

    /**
     * Register a global handler. Globals go to the **end** of the multiplexer
     * so screen processors get the event first (and may consume it by
     * returning `true`).
     */
    fun register(global: InputProcessor) {
        mux.addProcessor(mux.processors.size, global)
    }

    /**
     * Push a screen [InputProcessor] (typically a [com.badlogic.gdx.scenes.scene2d.Stage])
     * to the **front** of the multiplexer (highest priority). Call from
     * `Screen.show()`.
     */
    fun pushScreen(stage: InputProcessor) {
        mux.addProcessor(0, stage)
    }

    /**
     * Remove [stage] from the multiplexer. Call from `Screen.hide()`. Safe to
     * call with a processor that was never added (no-op).
     */
    fun popScreen(stage: InputProcessor) {
        mux.removeProcessor(stage)
    }

    /**
     * True when the router's mux is currently the active
     * `Gdx.input.inputProcessor`. Used by the legacy polling fallbacks
     * (F12 screenshot, M-key mute) to skip themselves on screens that have
     * migrated to the router — preventing double-fire.
     */
    fun isActive(): Boolean = Gdx.input.inputProcessor === mux

    /**
     * Test-only: clear every processor. The router is a global singleton, so
     * cross-spec ordering can otherwise leak state.
     */
    internal fun resetForTest() {
        val procs = mux.processors.toList()
        procs.forEach { mux.removeProcessor(it) }
    }
}

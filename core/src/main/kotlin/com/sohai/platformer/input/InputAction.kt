package com.sohai.platformer.input

/**
 * Typed handle for a player-facing input action (T-161 groundwork).
 *
 * Today the input pipeline keys off raw strings — [com.sohai.platformer.input.InputManager]
 * calls `keybinds["jump"]`, [com.sohai.platformer.persist.defaultKeybinds] returns a
 * `Map<String, Int>`, and the Controls UI threads the same string literals through.
 * This enum is the first step toward replacing those stringly-typed lookups with
 * a compile-checked identifier. Future tickets (rebind UI, controller mapping,
 * the planned `mute` hotkey from T-118) can migrate call sites to
 * `InputAction.JUMP.wireName` and eventually drop the raw strings entirely.
 *
 * **Wire compatibility:** [wireName] is the persisted/serialised key used in
 * `settings.json` (`"keybinds": { "jump": 62, ... }`). Renaming an existing
 * value here would silently invalidate every player's saved bindings, so the
 * strings are pinned and a unit test asserts they match
 * [com.sohai.platformer.persist.defaultKeybinds] 1:1.
 *
 * **Additive only:** this PR does not re-wire any production caller. New code
 * may opt in via `InputAction.JUMP.wireName`; existing string literals keep
 * working unchanged.
 */
enum class InputAction(val wireName: String) {
    LEFT("left"),
    RIGHT("right"),
    JUMP("jump"),
    ACTION("action"),
    SWAP("swap"),
    RESTART("restart"),

    /**
     * Reserved for the master-mute hotkey landing in T-118. Not present in
     * [com.sohai.platformer.persist.defaultKeybinds] yet — the test only asserts
     * 1:1 coverage for keys that DO exist in the map today, so this entry is
     * allowed to lead the defaults rather than follow them.
     */
    MUTE("mute"),
}

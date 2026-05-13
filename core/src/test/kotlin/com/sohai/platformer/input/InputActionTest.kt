package com.sohai.platformer.input

import com.sohai.platformer.persist.defaultKeybinds
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Groundwork tests for [InputAction] (T-161).
 *
 * Asserts the invariants future tickets will rely on when they start migrating
 * call sites away from raw string lookups:
 *   1. Every enum entry has a unique wireName (no two actions collide on the
 *      same persisted key).
 *   2. Every key currently in [defaultKeybinds] is covered by exactly one
 *      enum entry with a matching wireName — i.e. the enum is a strict
 *      superset of today's bindings. This catches the obvious regressions
 *      (typo in a wireName, renaming a key in `Settings.kt` without updating
 *      the enum, etc.) without forcing this PR to also add new bindings to
 *      `defaultKeybinds` — MUTE is reserved for T-118.
 */
class InputActionTest : BehaviorSpec({

    given("the InputAction enum") {
        `when`("collecting every wireName") {
            then("each is unique (no duplicate persisted keys)") {
                val wireNames = InputAction.values().map { it.wireName }
                wireNames.size shouldBe wireNames.toSet().size
            }

            then("the enum exposes the expected seven actions") {
                InputAction.values().map { it.wireName } shouldContainExactlyInAnyOrder listOf(
                    "left", "right", "jump", "action", "swap", "restart", "mute"
                )
            }
        }
    }

    given("the wire contract with Settings.defaultKeybinds()") {
        `when`("comparing wireNames to the keys currently persisted") {
            then("every defaultKeybinds key maps to exactly one InputAction with the same wireName") {
                val defaultKeys = defaultKeybinds().keys
                val byWireName = InputAction.values().associateBy { it.wireName }

                // Every key in defaults must resolve to an enum entry.
                defaultKeys.forEach { key ->
                    val action = byWireName[key]
                    check(action != null) {
                        "defaultKeybinds() key '$key' has no matching InputAction.wireName"
                    }
                    action.wireName shouldBe key
                }
            }

            then("only MUTE is allowed to be absent from defaultKeybinds (reserved for T-118)") {
                val defaultKeys = defaultKeybinds().keys
                val missingFromDefaults = InputAction.values()
                    .map { it.wireName }
                    .filterNot { it in defaultKeys }
                missingFromDefaults shouldBe listOf("mute")
            }
        }
    }
})

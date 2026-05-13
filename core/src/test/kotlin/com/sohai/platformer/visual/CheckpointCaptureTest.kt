package com.sohai.platformer.visual

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

/**
 * T-A10: Unit tests for [CheckpointCapture] — the pure [sanitize] helper only.
 *
 * The GL / disk-write path inside [CheckpointCapture.capture] needs a live
 * libGDX `Gdx.graphics` back buffer, which neither this test JVM nor the
 * smoke matrix has. That path is exercised indirectly by the
 * `cloudy.captureCheckpoints=true` smoke run (CI checks the artifact dir for
 * PNGs). Here we only cover the deterministic-filename contract.
 */
class CheckpointCaptureTest : BehaviorSpec({

    given("CheckpointCapture.sanitize") {

        `when`("given an already-safe name (alphanumeric + hyphen + underscore)") {
            val result = CheckpointCapture.sanitize("MainMenu-Loaded_42")
            then("it passes through unchanged") {
                result shouldBe "MainMenu-Loaded_42"
            }
        }

        `when`("given a name with spaces, slashes, and punctuation") {
            val result = CheckpointCapture.sanitize("Level 1 / start!")
            then("non-safe runs collapse to a single underscore each") {
                // " / " becomes a single "_"; trailing "!" becomes "_".
                result shouldBe "Level_1_start_"
            }
        }

        `when`("given a blank name") {
            val result = CheckpointCapture.sanitize("")
            then("it falls back to the 'unnamed' sentinel") {
                result shouldBe "unnamed"
            }
        }

        `when`("given a whitespace-only name") {
            val result = CheckpointCapture.sanitize("   ")
            then("it also falls back to 'unnamed' (isBlank() guard)") {
                result shouldBe "unnamed"
            }
        }

        `when`("given a name with consecutive unsafe characters") {
            val result = CheckpointCapture.sanitize("level1 // mid jump !!!")
            then("each run collapses to one underscore (regex with +)") {
                // " // " → "_", " mid jump " stays mostly the same with
                // single-underscore replacements, " !!!" → "_".
                result shouldBe "level1_mid_jump_"
            }
        }

        `when`("given a name with leading and trailing unsafe characters") {
            val result = CheckpointCapture.sanitize("///level1///")
            then("leading and trailing unsafe runs become underscores too") {
                result shouldMatch Regex("^_level1_$")
            }
        }
    }
})

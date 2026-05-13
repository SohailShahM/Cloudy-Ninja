package com.sohai.platformer.rendering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * T-206 — verifies the per-character foot-offset tuning knob is present and
 * defaults to `0f` so behavior is unchanged until the user/orchestrator tunes
 * each value via single-line edits.
 *
 * These constants are read by [com.sohai.platformer.screens.LevelRenderer.renderPlayer]
 * to push a character sprite down by the configured number of world meters,
 * compensating for transparent pixel rows at the bottom of sprite frames that
 * would otherwise make the visible character appear to float above the ground.
 *
 * The rendering math itself isn't tested here — `renderPlayer` requires live
 * GL state. This test simply pins the public knob's existence and default.
 */
class SpriteFactoryTest : BehaviorSpec({

    given("per-character spriteFootOffset constants") {
        then("SPRITE_FOOT_OFFSET_EBO is set to the current tuning value") {
            SpriteFactory.SPRITE_FOOT_OFFSET_EBO shouldBe 0.4f
        }
        then("SPRITE_FOOT_OFFSET_LAYA is set to the current tuning value") {
            SpriteFactory.SPRITE_FOOT_OFFSET_LAYA shouldBe 0.4f
        }
        then("SPRITE_FOOT_OFFSET_ZEPHYR is set to the current tuning value") {
            SpriteFactory.SPRITE_FOOT_OFFSET_ZEPHYR shouldBe 0.4f
        }
    }
})

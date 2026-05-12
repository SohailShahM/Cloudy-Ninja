package com.sohai.platformer.screens

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

/**
 * Regression guard for T-034.
 *
 * PR #3 (Copilot T-041) introduced STATS_SLOT_FILES with the wrong pattern
 * ("save_0.json" instead of "save_slot_0.json"), causing the stats screen to
 * always show "Empty" for every slot.  This test locks down the correct
 * filename convention so a future edit can't quietly reintroduce the bug.
 */
class StatsScreenSlotFilesTest : BehaviorSpec({
    given("STATS_SLOT_FILES") {
        `when`("inspected") {
            then("should contain exactly 3 entries") {
                STATS_SLOT_FILES shouldHaveSize 3
            }

            then("each entry must match the save_slot_N.json convention") {
                STATS_SLOT_FILES.forEachIndexed { index, filename ->
                    filename shouldMatch Regex("^save_slot_$index\\.json$")
                }
            }

            then("entries are save_slot_0, save_slot_1, save_slot_2 in order") {
                STATS_SLOT_FILES[0] shouldBe "save_slot_0.json"
                STATS_SLOT_FILES[1] shouldBe "save_slot_1.json"
                STATS_SLOT_FILES[2] shouldBe "save_slot_2.json"
            }
        }
    }
})

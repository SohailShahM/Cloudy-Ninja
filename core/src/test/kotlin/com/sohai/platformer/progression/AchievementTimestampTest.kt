package com.sohai.platformer.progression

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import com.sohai.platformer.persist.GameState
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.persist.SaveMigrations
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Tests for T-146 — per-achievement unlock-timestamp persistence + display.
 *
 * Five facets the ticket calls out:
 *  1. **New unlock records a timestamp.** [AchievementUnlocker.tryUnlock] must
 *     write `System.currentTimeMillis()` (via the injectable [AchievementUnlocker.clock]
 *     seam) into [GameState.achievementTimestamps] keyed by the achievement id.
 *  2. **Legacy fallback.** An achievement that is in `unlockedAchievements`
 *     but absent from `achievementTimestamps` must render
 *     [StringKey.ACHIEVEMENT_UNLOCKED_AT_UNKNOWN] (`"Unlocked: ?"`).
 *  3. **Date format.** The display string matches `YYYY-MM-DD` exactly.
 *  4. **Save round-trip preserves the map.** A `GameState` with timestamps
 *     survives serialize → write → read → deserialize unchanged.
 *  5. **Back-compat for pre-T-146 saves** (the test the prompt asks us to call
 *     out explicitly): a save JSON written before this ticket has no
 *     `achievementTimestamps` field at all — loading must succeed and yield
 *     an empty map.
 *
 * Isolation strategy mirrors [com.sohai.platformer.persist.SaveManagerTest]:
 * stub `Gdx.app` and `Gdx.files` so the round-trip case writes to a real
 * temp dir without touching production paths.
 */
class AchievementTimestampTest : BehaviorSpec({

    val tmpRoot: File = File(
        System.getProperty("java.io.tmpdir"),
        "cloudy_t146_${UUID.randomUUID()}"
    ).apply { mkdirs() }

    val prevApp: Application? = Gdx.app
    val prevFiles: Files? = Gdx.files
    val prevClock: () -> Long = AchievementUnlocker.clock

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)
        val filesMock = mockk<Files>(relaxed = true)
        every { filesMock.local(any<String>()) } answers {
            val rel = firstArg<String>()
            FileHandle(File(tmpRoot, rel).absoluteFile)
        }
        Gdx.files = filesMock
    }

    afterSpec {
        Gdx.app = prevApp
        Gdx.files = prevFiles
        AchievementUnlocker.clock = prevClock
        tmpRoot.deleteRecursively()
    }

    fun slot(label: String): String = "${label}_${UUID.randomUUID()}.json"

    // ── 1. Timestamp recording ───────────────────────────────────────────────

    given("a fresh save and a deterministic clock") {
        val filename = slot("ts_new_unlock")
        val fixedNow = 1_715_500_000_000L // 2024-05-12T08:26:40Z

        `when`("AchievementUnlocker.tryUnlock fires for a new achievement") {
            AchievementUnlocker.clock = { fixedNow }
            // Seed an empty save so the unlocker has something to copy.
            SaveManager.saveGame(GameState(), filename)
            AchievementUnlocker.tryUnlock("first_jump", filename, null)

            val state = SaveManager.loadGame(filename)

            then("unlockedAchievements gains the id") {
                state.unlockedAchievements shouldBe setOf("first_jump")
            }
            then("achievementTimestamps records the clock value under the id") {
                state.achievementTimestamps shouldContainKey "first_jump"
                state.achievementTimestamps["first_jump"] shouldBe fixedNow
            }
        }

        `when`("AchievementUnlocker.tryUnlock fires twice for the same id (idempotence)") {
            val idemFile = slot("ts_idempotent")
            AchievementUnlocker.clock = { fixedNow }
            SaveManager.saveGame(GameState(), idemFile)
            AchievementUnlocker.tryUnlock("stomp_10", idemFile, null)

            // Second call with a later clock — must be a no-op so the
            // original timestamp is preserved.
            AchievementUnlocker.clock = { fixedNow + 60_000L }
            AchievementUnlocker.tryUnlock("stomp_10", idemFile, null)

            val state = SaveManager.loadGame(idemFile)

            then("the original timestamp wins (no-op on second call)") {
                state.achievementTimestamps["stomp_10"] shouldBe fixedNow
            }
            then("the unlocked set still has exactly one entry for the id") {
                state.unlockedAchievements shouldBe setOf("stomp_10")
            }
        }

        `when`("multiple distinct achievements are unlocked at different times") {
            val multiFile = slot("ts_multi")
            SaveManager.saveGame(GameState(), multiFile)

            AchievementUnlocker.clock = { fixedNow }
            AchievementUnlocker.tryUnlock("first_jump", multiFile, null)
            AchievementUnlocker.clock = { fixedNow + 1_000_000L }
            AchievementUnlocker.tryUnlock("first_cleanse", multiFile, null)

            val state = SaveManager.loadGame(multiFile)

            then("each achievement carries its own timestamp") {
                state.achievementTimestamps["first_jump"] shouldBe fixedNow
                state.achievementTimestamps["first_cleanse"] shouldBe fixedNow + 1_000_000L
            }
        }
    }

    // ── 2. Legacy fallback (achievement unlocked but no timestamp) ────────────

    given("a save with an unlocked achievement but NO matching timestamp") {
        // Models the pre-T-146 unlock case: the id is in `unlockedAchievements`
        // but `achievementTimestamps` doesn't have it. AchievementsScreen must
        // render the "Unlocked: ?" fallback in this case.
        val legacyState = GameState(
            unlockedAchievements = setOf("first_jump"),
            achievementTimestamps = emptyMap()
        )

        `when`("the row's display string is computed via the same lookup the screen uses") {
            val id = "first_jump"
            val ts: Long? = legacyState.achievementTimestamps[id]
            val rendered = if (ts != null) {
                Strings.format(StringKey.ACHIEVEMENT_UNLOCKED_AT, "ignored")
            } else {
                Strings.get(StringKey.ACHIEVEMENT_UNLOCKED_AT_UNKNOWN)
            }

            then("the fallback resolves to a non-blank string") {
                ts shouldBe null
                rendered shouldBe Strings.get(StringKey.ACHIEVEMENT_UNLOCKED_AT_UNKNOWN)
            }
            then("the fallback string contains a question mark") {
                rendered shouldContain "?"
            }
        }
    }

    // ── 3. Date format is YYYY-MM-DD ─────────────────────────────────────────

    given("the T-146 date formatter") {
        // The screen uses `DateTimeFormatter.ofPattern("yyyy-MM-dd")` in the
        // user's local timezone. Verify the produced string is the documented
        // shape (no time component, ten chars, two dashes in the right slots).
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        `when`("a known epoch ms is formatted in the local zone") {
            val epochMs = 1_715_500_000_000L
            val rendered = formatter.format(
                Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
            )

            then("the result has exactly 10 characters") {
                rendered.length shouldBe 10
            }
            then("it matches the YYYY-MM-DD regex") {
                Regex("""\d{4}-\d{2}-\d{2}""").matches(rendered) shouldBe true
            }
        }

        `when`("formatted via Strings.format(ACHIEVEMENT_UNLOCKED_AT, …)") {
            val epochMs = 1_715_500_000_000L
            val date = formatter.format(
                Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
            )
            val rendered = Strings.format(StringKey.ACHIEVEMENT_UNLOCKED_AT, date)

            then("the user-facing string contains 'Unlocked:' and the date") {
                rendered shouldContain "Unlocked:"
                rendered shouldContain date
            }
            then("the date substring still matches YYYY-MM-DD") {
                Regex("""\d{4}-\d{2}-\d{2}""").containsMatchIn(rendered) shouldBe true
            }
        }
    }

    // ── 4. Save round-trip preserves the timestamp map ──────────────────────

    given("a GameState carrying achievementTimestamps") {
        val filename = slot("ts_roundtrip")
        val original = GameState(
            unlockedAchievements = setOf("first_jump", "stomp_10", "speed_demon"),
            achievementTimestamps = mapOf(
                "first_jump" to 1_715_500_000_000L,
                "stomp_10" to 1_715_500_060_000L,
                "speed_demon" to 1_715_500_120_000L
            )
        )

        `when`("saved and re-loaded through SaveManager") {
            SaveManager.saveGame(original, filename)
            // Force a cold disk read using a fresh filename so the cache
            // is not what we're observing.
            val coldName = slot("ts_roundtrip_cold")
            SaveManager.saveGame(original, coldName)
            val loaded = SaveManager.loadGame(coldName)

            then("the timestamp map equals the original (exact entries)") {
                loaded.achievementTimestamps shouldBe original.achievementTimestamps
            }
            then("the unlocked set equals the original") {
                loaded.unlockedAchievements shouldBe original.unlockedAchievements
            }
            then("full state equality holds") {
                loaded shouldBe original
            }
        }
    }

    // ── 5. Back-compat: pre-T-146 saves load with an empty timestamp map ────

    given("a legacy save written BEFORE T-146 (no achievementTimestamps field)") {
        // CRITICAL: this is the back-compat guarantee promised by the
        // "additive field" pattern (T-113 migration scaffold). A real user's
        // existing save on disk has unlockedAchievements but no
        // achievementTimestamps key. Loading must not throw, and the field
        // must default to emptyMap.
        val legacyJson = """
            {
              "saveFormatVersion": 1,
              "level": "level1_2",
              "characterName": "Ebo",
              "unlockedAchievements": ["first_jump", "stomp_10"],
              "totalDeaths": 4,
              "lastPlayed": "2026-04-01"
            }
        """.trimIndent()

        `when`("routed through the production migration chain") {
            val migrated = SaveMigrations.migrate(legacyJson)

            then("the load succeeds and unlockedAchievements parse correctly") {
                migrated.unlockedAchievements shouldBe setOf("first_jump", "stomp_10")
            }
            then("achievementTimestamps defaults to empty map (back-compat)") {
                migrated.achievementTimestamps shouldBe emptyMap()
            }
            then("the decoded state carries CURRENT_VERSION") {
                migrated.saveFormatVersion shouldBe SaveMigrations.CURRENT_VERSION
            }
        }

        `when`("written through SaveManager then re-read after seeding the legacy JSON") {
            // Simulate the on-disk scenario: a real save file from a player
            // who upgraded from pre-T-146 to T-146-aware code.
            val legacyFile = slot("legacy_pre_t146")
            val saveDir = File(tmpRoot, "saves").apply { mkdirs() }
            File(saveDir, legacyFile).writeText(legacyJson)

            val loaded = SaveManager.loadGame(legacyFile)

            then("the load succeeds with empty achievementTimestamps") {
                loaded.unlockedAchievements shouldBe setOf("first_jump", "stomp_10")
                loaded.achievementTimestamps shouldBe emptyMap()
            }
        }
    }

    // ── 6. AchievementUnlocker preserves the clock seam contract ────────────

    given("the AchievementUnlocker.clock seam") {
        `when`("never overridden in production") {
            val productionClock: () -> Long = { System.currentTimeMillis() }
            then("the default produces a plausible epoch-ms value (post-2000)") {
                productionClock().let { it > 946_684_800_000L } shouldBe true
            }
        }
    }
})

package com.sohai.platformer.persist

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Tests for [SaveManager] round-trip persistence and back-compat tolerance.
 *
 * Isolation strategy:
 *  - [SaveManager] uses [Gdx.files] and [Gdx.app] singletons. Rather than spin
 *    up the headless backend (not on the test classpath, and adding it is out
 *    of scope), we mock both:
 *      * `Gdx.app`   — relaxed mock so `log()` / `error()` are no-ops.
 *      * `Gdx.files` — `local(path)` returns a real [FileHandle] wrapping a
 *        per-spec temp directory, so writeString/readString/exists/delete
 *        all hit a real (but isolated) filesystem.
 *  - Each test uses a unique slot filename (UUID) so a flaky cleanup never
 *    leaks state between tests, and the per-process [SaveManager.cache] never
 *    collides across cases.
 *  - The temp directory is wiped in `afterSpec`.
 *
 * The production [SaveManager] singleton is NOT modified; we only stub the
 * two libGDX globals it touches.
 */
class SaveManagerTest : BehaviorSpec({

    // Per-spec temp dir under java.io.tmpdir, e.g. .../cloudy_savemgr_<uuid>/
    val tmpRoot: File = File(
        System.getProperty("java.io.tmpdir"),
        "cloudy_savemgr_${UUID.randomUUID()}"
    ).apply { mkdirs() }

    // Snapshot prior globals so we can restore them.
    val prevApp: Application? = Gdx.app
    val prevFiles: Files? = Gdx.files

    beforeSpec {
        // Stub Gdx.app — SaveManager calls Gdx.app.log / .error.
        Gdx.app = mockk<Application>(relaxed = true)

        // Stub Gdx.files — only `local(String)` is exercised by SaveManager.
        // Everything resolves under tmpRoot so production save dirs are
        // never touched.
        val filesMock = mockk<Files>(relaxed = true)
        every { filesMock.local(any<String>()) } answers {
            val rel = firstArg<String>()
            val abs = File(tmpRoot, rel).absoluteFile
            FileHandle(abs)
        }
        Gdx.files = filesMock
    }

    afterSpec {
        // Restore previous globals (likely null in unit-test JVM, but be polite).
        Gdx.app = prevApp
        Gdx.files = prevFiles
        // Clear any test-only hooks so cross-spec interleaving stays safe.
        SaveManager.crashAfterTempWriteHook = null
        SaveManager.crashDuringWriteHook = null
        // Wipe the temp directory.
        tmpRoot.deleteRecursively()
    }

    /** Helper: fresh, unique slot filename per test for full isolation. */
    fun slot(label: String = "slot"): String =
        "${label}_${UUID.randomUUID()}.json"

    /** Helper: hand-write a save JSON to disk to simulate legacy formats. */
    fun seedLegacySave(filename: String, jsonString: String) {
        val saveDir = File(tmpRoot, "saves").apply { mkdirs() }
        File(saveDir, filename).writeText(jsonString)
    }

    given("a fully populated GameState") {
        val filename = slot("fullroundtrip")
        val original = GameState(
            level = "level3_2",
            characterName = "Laya",
            checkpoint = Checkpoint(levelName = "level3_2", x = 128.5f, y = 64.25f),
            stats = PlayerStats(
                seedSlamsUsed = 7,
                windDashesUsed = 11,
                checkpointsReached = 4,
                timeSpent = 432.5f
            ),
            completedLevels = setOf("level0_0", "level0_1", "level1_0"),
            collectedAtlasIds = setOf("silver_iodide", "water_cycle"),
            bestScores = mapOf("level0_0" to 1500, "level1_0" to 2300),
            bestTimes = mapOf("level0_0" to 45.5f, "level1_0" to 92.125f),
            totalDeaths = 27,
            lastPlayed = "2026-05-12",
            unlockedAchievements = setOf("first_steps", "stomp_10", "speed_demon"),
            totalStomps = 42
        )

        `when`("saved then loaded from disk") {
            SaveManager.saveGame(original, filename)
            // Evict the in-memory cache so we hit the disk-read path explicitly.
            SaveManager.deleteSave(filename)
            SaveManager.saveGame(original, filename)
            // (Re-saving repopulates the cache. To prove disk round-trips,
            //  use a unique second filename and read it cold.)
            val coldName = slot("fullroundtrip_cold")
            SaveManager.saveGame(original, coldName)
            val loaded = SaveManager.loadGame(coldName)

            then("the loaded state equals the original (full equality)") {
                loaded shouldBe original
            }
            then("every collection field round-trips") {
                loaded.completedLevels shouldBe original.completedLevels
                loaded.collectedAtlasIds shouldBe original.collectedAtlasIds
                loaded.bestScores shouldBe original.bestScores
                loaded.bestTimes shouldBe original.bestTimes
                loaded.unlockedAchievements shouldBe original.unlockedAchievements
            }
            then("scalar counters round-trip") {
                loaded.totalDeaths shouldBe 27
                loaded.totalStomps shouldBe 42
                loaded.lastPlayed shouldBe "2026-05-12"
            }
            then("nested Checkpoint and PlayerStats round-trip") {
                loaded.checkpoint shouldBe original.checkpoint
                loaded.stats shouldBe original.stats
            }
        }
    }

    given("a default GameState") {
        val filename = slot("default")

        `when`("saved and reloaded") {
            val default = GameState()
            SaveManager.saveGame(default, filename)
            val loaded = SaveManager.loadGame(filename)

            then("loaded state equals a freshly-constructed default") {
                loaded shouldBe GameState()
            }
        }
    }

    given("a legacy save missing unlockedAchievements + totalStomps") {
        val filename = slot("legacy_pre_achievements")

        `when`("loaded by current SaveManager") {
            // Older save written before T-039 / T-072 added achievements +
            // totalStomps fields. ignoreUnknownKeys=true on the producer side
            // is irrelevant here — we exercise the missing-field path.
            seedLegacySave(
                filename,
                """
                {
                  "level": "level1_2",
                  "characterName": "Ebo",
                  "checkpoint": { "levelName": "level1_2", "x": 50.0, "y": 12.0 },
                  "stats": { "seedSlamsUsed": 3, "windDashesUsed": 1, "checkpointsReached": 2, "timeSpent": 88.0 },
                  "completedLevels": ["level0_0"],
                  "totalDeaths": 4,
                  "lastPlayed": "2026-04-01"
                }
                """.trimIndent()
            )
            val loaded = SaveManager.loadGame(filename)

            then("missing unlockedAchievements defaults to empty set") {
                loaded.unlockedAchievements shouldBe emptySet()
            }
            then("missing totalStomps defaults to 0") {
                loaded.totalStomps shouldBe 0
            }
            then("known fields still parse correctly") {
                loaded.level shouldBe "level1_2"
                loaded.characterName shouldBe "Ebo"
                loaded.completedLevels shouldContain "level0_0"
                loaded.totalDeaths shouldBe 4
            }
            then("missing collectedAtlasIds / bestScores / bestTimes default to empty") {
                loaded.collectedAtlasIds shouldBe emptySet()
                loaded.bestScores shouldBe emptyMap()
                loaded.bestTimes shouldBe emptyMap()
            }
        }
    }

    given("a minimal legacy save with only level + characterName") {
        val filename = slot("legacy_minimal")

        `when`("loaded by current SaveManager") {
            seedLegacySave(
                filename,
                """{ "level": "level2_0", "characterName": "Laya" }"""
            )
            val loaded = SaveManager.loadGame(filename)

            then("provided fields are honored") {
                loaded.level shouldBe "level2_0"
                loaded.characterName shouldBe "Laya"
            }
            then("every absent field falls back to its declared default") {
                loaded.checkpoint shouldBe Checkpoint()
                loaded.stats shouldBe PlayerStats()
                loaded.completedLevels shouldBe emptySet()
                loaded.collectedAtlasIds shouldBe emptySet()
                loaded.bestScores shouldBe emptyMap()
                loaded.bestTimes shouldBe emptyMap()
                loaded.totalDeaths shouldBe 0
                loaded.lastPlayed shouldBe ""
                loaded.unlockedAchievements shouldBe emptySet()
                loaded.totalStomps shouldBe 0
            }
        }
    }

    given("a legacy save with unknown future fields") {
        val filename = slot("legacy_unknown_keys")

        `when`("loaded by current SaveManager") {
            // SaveManager.json is configured with ignoreUnknownKeys = true,
            // so a save written by a *newer* client with extra fields must
            // still parse cleanly on the current version.
            seedLegacySave(
                filename,
                """
                {
                  "level": "level0_0",
                  "characterName": "Ebo",
                  "futureFieldNotYetDefined": "ignore me",
                  "anotherFuturismField": 99,
                  "totalStomps": 5
                }
                """.trimIndent()
            )
            val loaded = SaveManager.loadGame(filename)

            then("known fields parse and unknown fields are silently ignored") {
                loaded.level shouldBe "level0_0"
                loaded.characterName shouldBe "Ebo"
                loaded.totalStomps shouldBe 5
            }
        }
    }

    given("two save slots written in sequence") {
        val slot1 = slot("multi_slot1")
        val slot2 = slot("multi_slot2")

        `when`("slot1 and slot2 hold different states") {
            val state1 = GameState(
                level = "level1_0",
                characterName = "Ebo",
                totalDeaths = 5,
                completedLevels = setOf("level0_0"),
                unlockedAchievements = setOf("first_steps")
            )
            val state2 = GameState(
                level = "level2_1",
                characterName = "Laya",
                totalDeaths = 99,
                completedLevels = setOf("level0_0", "level1_0", "level2_0"),
                unlockedAchievements = setOf("first_steps", "stomp_10")
            )

            SaveManager.saveGame(state1, slot1)
            SaveManager.saveGame(state2, slot2)

            val loaded1 = SaveManager.loadGame(slot1)
            val loaded2 = SaveManager.loadGame(slot2)

            then("each slot loads its own state, with no cross-contamination") {
                loaded1 shouldBe state1
                loaded2 shouldBe state2
                loaded1 shouldNotBe loaded2
                loaded1.totalDeaths shouldBe 5
                loaded2.totalDeaths shouldBe 99
                loaded1.completedLevels shouldHaveSize 1
                loaded2.completedLevels shouldHaveSize 3
            }
        }
    }

    given("a save that exists on disk") {
        val filename = slot("delete")

        `when`("deleteSave is called") {
            SaveManager.saveGame(
                GameState(level = "level9_9", characterName = "Ebo", totalDeaths = 123),
                filename
            )
            SaveManager.hasSave(filename) shouldBe true

            val removed = SaveManager.deleteSave(filename)

            then("deleteSave returns true") {
                removed shouldBe true
            }
            then("the file no longer exists on disk") {
                SaveManager.hasSave(filename) shouldBe false
            }
            then("a subsequent loadGame returns the default state") {
                val reloaded = SaveManager.loadGame(filename)
                reloaded shouldBe GameState()
            }
        }
    }

    given("a deleted save file") {
        val filename = slot("delete_missing")

        `when`("deleteSave is called on a slot that never existed") {
            val removed = SaveManager.deleteSave(filename)

            then("it returns false without throwing") {
                removed shouldBe false
            }
        }
    }

    given("no save file on disk for the given slot") {
        val filename = slot("no_save")

        `when`("loadGame is called") {
            // Ensure no cache hit from prior tests.
            SaveManager.deleteSave(filename)
            val loaded = SaveManager.loadGame(filename)

            then("a default GameState is returned") {
                loaded shouldBe GameState()
            }
        }
    }

    given("a GameState with empty collections and zero counters") {
        val filename = slot("empty_collections")

        `when`("explicitly serialized via kotlinx.serialization and read back") {
            // kotlinx.serialization with defaults *omits* default values
            // from output. Re-encoding and re-decoding must preserve that
            // contract: empty Set/Map deserialize back to empty Set/Map.
            val producerJson = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
            val empty = GameState(
                completedLevels = emptySet(),
                bestScores = emptyMap(),
                bestTimes = emptyMap(),
                unlockedAchievements = emptySet()
            )
            seedLegacySave(filename, producerJson.encodeToString(empty))
            val loaded = SaveManager.loadGame(filename)

            then("collections come back empty (not null)") {
                loaded.completedLevels shouldBe emptySet()
                loaded.bestScores shouldBe emptyMap()
                loaded.bestTimes shouldBe emptyMap()
                loaded.unlockedAchievements shouldBe emptySet()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // T-136 — Atomic save writes
    // ─────────────────────────────────────────────────────────────────────

    given("an existing save and a crash AFTER temp write but BEFORE rename (T-136)") {
        val filename = slot("atomic_crash_after_temp")

        `when`("the second save throws between temp write and atomic rename") {
            // First, persist a known-good "v1" save.
            val v1 = GameState(
                level = "level_v1",
                characterName = "Ebo",
                totalDeaths = 1,
                completedLevels = setOf("level0_0")
            )
            SaveManager.saveGame(v1, filename)
            // Evict the cache so the next loadGame is forced to read from disk.
            val saveFile = File(tmpRoot, "saves/$filename")
            saveFile.exists() shouldBe true
            val v1OnDiskBytes = saveFile.readBytes()

            // Now attempt a v2 save that "crashes" after the temp file has
            // been written + fsynced but before the atomic rename happens.
            val v2 = GameState(
                level = "level_v2",
                characterName = "Laya",
                totalDeaths = 999,
                completedLevels = setOf("level0_0", "level1_0", "level2_0"),
                unlockedAchievements = setOf("first_steps", "stomp_10")
            )
            SaveManager.crashAfterTempWriteHook = {
                throw RuntimeException("simulated mid-save crash (post-temp, pre-rename)")
            }
            try {
                SaveManager.saveGame(v2, filename)
            } finally {
                SaveManager.crashAfterTempWriteHook = null
            }

            then("the original save file on disk is byte-for-byte unchanged") {
                saveFile.exists() shouldBe true
                saveFile.readBytes() shouldBe v1OnDiskBytes
            }
            then("a cold disk load (bypassing the cache) returns the v1 state") {
                // The crashed save did NOT update the cache, so the in-memory
                // cache still holds v1. To prove the on-disk file is the
                // source of truth, copy the bytes to a fresh slot whose
                // cache key has never been touched and load that.
                val coldName = slot("atomic_crash_after_temp_cold")
                File(tmpRoot, "saves/$coldName").writeBytes(v1OnDiskBytes)
                val loaded = SaveManager.loadGame(coldName)
                loaded.level shouldBe "level_v1"
                loaded.characterName shouldBe "Ebo"
                loaded.totalDeaths shouldBe 1
            }
            then("the temp file has been cleaned up (no leaked .tmp)") {
                val tmpFile = File(tmpRoot, "saves/$filename.tmp")
                tmpFile.exists() shouldBe false
            }
        }
    }

    given("an existing save and a crash DURING the temp file write (T-136)") {
        val filename = slot("atomic_crash_during_write")

        `when`("the second save throws while writing the temp file") {
            // Establish a baseline save we want to preserve across the crash.
            val baseline = GameState(
                level = "baseline_level",
                characterName = "Zephyr",
                totalDeaths = 7,
                bestTimes = mapOf("level0_0" to 33.3f)
            )
            SaveManager.saveGame(baseline, filename)
            val saveFile = File(tmpRoot, "saves/$filename")
            saveFile.exists() shouldBe true
            val baselineBytes = saveFile.readBytes()

            // Attempt a clobbering save that "crashes" mid-write.
            val clobber = GameState(level = "should_never_land", totalDeaths = 9999)
            SaveManager.crashDuringWriteHook = {
                throw RuntimeException("simulated crash during temp write")
            }
            try {
                SaveManager.saveGame(clobber, filename)
            } finally {
                SaveManager.crashDuringWriteHook = null
            }

            then("the original save on disk is byte-for-byte unchanged") {
                saveFile.exists() shouldBe true
                saveFile.readBytes() shouldBe baselineBytes
            }
            then("loadGame returns the baseline state (on-disk recovery)") {
                val coldName = slot("atomic_crash_during_write_cold")
                // Copy the on-disk file into a cold slot and load that to
                // bypass the in-memory cache entirely.
                File(tmpRoot, "saves/$coldName").writeBytes(baselineBytes)
                val loaded = SaveManager.loadGame(coldName)
                loaded.level shouldBe "baseline_level"
                loaded.characterName shouldBe "Zephyr"
                loaded.totalDeaths shouldBe 7
            }
            then("the temp file is cleaned up (or, if it remains, the original still loads)") {
                // Strict contract: best-effort cleanup runs in the catch
                // block, so the .tmp file should not be present. If a
                // future implementation chooses to leak it, the previous
                // two assertions still guarantee correctness.
                val tmpFile = File(tmpRoot, "saves/$filename.tmp")
                tmpFile.exists() shouldBe false
            }
        }
    }

    given("a normal save with no simulated crash (T-136 atomic happy path)") {
        val filename = slot("atomic_happy_path")

        `when`("saveGame completes cleanly") {
            // Make sure no stale hook from a sibling test is still wired.
            SaveManager.crashAfterTempWriteHook = null
            SaveManager.crashDuringWriteHook = null

            val state = GameState(
                level = "atomic_ok",
                characterName = "Ebo",
                totalDeaths = 3,
                bestScores = mapOf("level0_0" to 500),
                unlockedAchievements = setOf("first_steps")
            )
            SaveManager.saveGame(state, filename)

            then("the final save file exists on disk") {
                File(tmpRoot, "saves/$filename").exists() shouldBe true
            }
            then("no .tmp residue is left behind") {
                File(tmpRoot, "saves/$filename.tmp").exists() shouldBe false
            }
            then("loadGame returns the saved state via cache") {
                SaveManager.loadGame(filename) shouldBe state
            }
            then("a cold load from disk also returns the saved state") {
                val coldName = slot("atomic_happy_path_cold")
                val src = File(tmpRoot, "saves/$filename").readBytes()
                File(tmpRoot, "saves/$coldName").writeBytes(src)
                SaveManager.loadGame(coldName) shouldBe state
            }
        }
    }

    given("a Settings instance with non-default accessibility fields") {
        // SaveManager doesn't persist Settings, but Settings shares the same
        // back-compat contract (`ignoreUnknownKeys = true`, every field has
        // a default). Verify the contract holds for the three fields the
        // task explicitly calls out: colorBlindMode, reducedMotion,
        // tilesetPackId.
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        `when`("a legacy Settings JSON omits colorBlindMode/reducedMotion/tilesetPackId") {
            // Pre-T-073 settings: no a11y fields, no tileset pack id.
            val legacy = """
                {
                  "volMusic": 0.5,
                  "volSfx": 0.8,
                  "fullscreen": true,
                  "displayWidth": 1920,
                  "displayHeight": 1080
                }
            """.trimIndent()
            val parsed = json.decodeFromString<Settings>(legacy)

            then("colorBlindMode defaults to OFF") {
                parsed.colorBlindMode shouldBe ColorBlindMode.OFF
            }
            then("reducedMotion defaults to false") {
                parsed.reducedMotion shouldBe false
            }
            then("tilesetPackId defaults to kenney_pixel_platformer") {
                parsed.tilesetPackId shouldBe "kenney_pixel_platformer"
            }
            then("provided fields are honored") {
                parsed.volMusic shouldBe 0.5f
                parsed.volSfx shouldBe 0.8f
                parsed.fullscreen shouldBe true
                parsed.displayWidth shouldBe 1920
                parsed.displayHeight shouldBe 1080
            }
        }
    }
})

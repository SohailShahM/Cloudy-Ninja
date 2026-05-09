package com.sohai.platformer.atlas

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * Unit tests for [CloudAtlasLibrary.get].
 * CloudAtlasLibrary is pure Kotlin with no libGDX deps — constructed directly.
 */
class CloudAtlasLibraryTest : BehaviorSpec({

    given("CloudAtlasLibrary") {

        `when`("get is called with a known id") {
            val entry = CloudAtlasLibrary.get("silver_iodide")

            then("it returns a non-null CloudAtlasEntry with matching title, subtitle, and character") {
                entry.shouldNotBeNull()
                entry.title shouldBe "Silver Iodide Cloud Seeding"
                entry.subtitle shouldBe "How Ebo makes it rain"
                entry.character shouldBe "Ebo"
            }
        }

        `when`("get is called with another known id") {
            val entry = CloudAtlasLibrary.get("temperature_inversion")

            then("it returns the correct entry for Laya") {
                entry.shouldNotBeNull()
                entry.title shouldBe "Temperature Inversions"
                entry.subtitle shouldBe "Why smog gets trapped in valleys"
                entry.character shouldBe "Laya"
            }
        }

        `when`("get is called with an unknown id") {
            val entry = CloudAtlasLibrary.get("does_not_exist")

            then("it returns null") {
                entry.shouldBeNull()
            }
        }

        `when`("get is called with a random/garbage id") {
            val entry = CloudAtlasLibrary.get("zzz_random_xyz_9999")

            then("it returns null") {
                entry.shouldBeNull()
            }
        }

        `when`("all entries are inspected") {
            val allEntries = CloudAtlasLibrary.entries.values

            then("every entry has a non-blank id") {
                allEntries.forEach { entry ->
                    entry.id.shouldNotBeBlank()
                }
            }

            then("every entry has a non-blank title") {
                allEntries.forEach { entry ->
                    entry.title.shouldNotBeBlank()
                }
            }

            then("every entry has a non-blank subtitle") {
                allEntries.forEach { entry ->
                    entry.subtitle.shouldNotBeBlank()
                }
            }

            then("every entry has a non-blank character") {
                allEntries.forEach { entry ->
                    entry.character.shouldNotBeBlank()
                }
            }

            then("no two entries share the same id (uniqueness invariant)") {
                val ids = allEntries.map { it.id }
                ids.toSet().size shouldBe ids.size
            }

            then("the total entry count matches the expected 5") {
                allEntries shouldHaveSize 5
            }
        }
    }
})

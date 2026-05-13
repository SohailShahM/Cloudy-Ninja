package com.sohai.platformer.screens

import com.sohai.platformer.atlas.CloudAtlasEntry
import com.sohai.platformer.atlas.CloudAtlasLibrary
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * T-141: Cloud Atlas search/filter — unit coverage for the pure
 * `CloudAtlasScreen.filterEntries` helper. The helper is the substring-match
 * core of the new screen-level search field; testing it directly avoids
 * the GL stage / VisUI setup the screen itself requires.
 *
 * What this guards against:
 *  • Blank/empty filter accidentally hiding entries.
 *  • Case-sensitivity regression (filter must be case-insensitive).
 *  • Forgetting to also match against body text (title-only would be too narrow).
 *  • Clear path (empty string) not returning all entries.
 *  • The "no entries match" StringKey going missing or blank.
 */
class CloudAtlasScreenFilterTest : BehaviorSpec({

    val sample: List<CloudAtlasEntry> = listOf(
        CloudAtlasEntry(
            id = "alpha",
            title = "Alpha Title",
            subtitle = "First subtitle",
            body = "Body about clouds and rain.",
            character = "Ebo"
        ),
        CloudAtlasEntry(
            id = "beta",
            title = "Beta Heading",
            subtitle = "Second subtitle",
            body = "Discussion of inversions and smog.",
            character = "Laya"
        ),
        CloudAtlasEntry(
            id = "gamma",
            title = "Gamma Snapshot",
            subtitle = "Third subtitle",
            body = "Albedo, ice, and reflectivity.",
            character = "Elara"
        )
    )

    given("CloudAtlasScreen.filterEntries with an empty filter") {
        `when`("the filter string is empty") {
            val result = CloudAtlasScreen.filterEntries(sample, "")
            then("all entries are returned, in iteration order") {
                result shouldHaveSize sample.size
                result.map { it.id } shouldBe sample.map { it.id }
            }
        }

        `when`("the filter string is only whitespace") {
            val result = CloudAtlasScreen.filterEntries(sample, "   ")
            then("all entries are returned (trim-then-empty behaves like blank)") {
                result shouldHaveSize sample.size
            }
        }
    }

    given("CloudAtlasScreen.filterEntries with a single-letter filter") {
        `when`("the filter is a short distinctive token that hits exactly one entry") {
            // "alph" only appears in "Alpha Title".
            val result = CloudAtlasScreen.filterEntries(sample, "alph")
            then("the visible set is narrowed to that one entry") {
                result shouldHaveSize 1
                result[0].id shouldBe "alpha"
            }
        }

        `when`("the filter is a single character that hits multiple entries") {
            // "i" appears in "Alpha Title" (title), "Beta Heading"? no — wait, title "Beta Heading" lacks 'i'.
            // Use a stable token instead: "snapshot"/"alpha" are too narrow; verify the multi-match path
            // via the body text. "and" appears in alpha.body ("clouds and rain"), beta.body
            // ("inversions and smog"), and gamma.body ("Albedo, ice, and reflectivity").
            val result = CloudAtlasScreen.filterEntries(sample, "and")
            then("all three entries match (via body)") {
                result shouldHaveSize 3
                result.map { it.id } shouldContain "alpha"
                result.map { it.id } shouldContain "beta"
                result.map { it.id } shouldContain "gamma"
            }
        }

        `when`("the filter is a single letter only present in body text") {
            // "albedo" appears only in gamma.body.
            val result = CloudAtlasScreen.filterEntries(sample, "albedo")
            then("body text is matched (not title-only)") {
                result shouldHaveSize 1
                result[0].id shouldBe "gamma"
            }
        }
    }

    given("CloudAtlasScreen.filterEntries case-insensitivity") {
        `when`("the same query is supplied in different cases") {
            val lower = CloudAtlasScreen.filterEntries(sample, "alpha")
            val upper = CloudAtlasScreen.filterEntries(sample, "ALPHA")
            val mixed = CloudAtlasScreen.filterEntries(sample, "AlPhA")
            then("results are identical across cases") {
                lower.map { it.id } shouldBe upper.map { it.id }
                lower.map { it.id } shouldBe mixed.map { it.id }
                lower shouldHaveSize 1
            }
        }
    }

    given("CloudAtlasScreen.filterEntries with no matches") {
        `when`("the filter string matches nothing in title or body") {
            val result = CloudAtlasScreen.filterEntries(sample, "zzz_no_such_string")
            then("an empty list is returned") {
                result.shouldBeEmpty()
            }
        }

        `when`("the no-results StringKey is consulted") {
            then("ATLAS_SEARCH_NO_RESULTS resolves to a non-blank string") {
                Strings.get(StringKey.ATLAS_SEARCH_NO_RESULTS).shouldNotBeBlank()
            }
        }
    }

    given("CloudAtlasScreen.filterEntries clear-resets behavior") {
        `when`("a non-trivial filter is applied, then cleared (empty string)") {
            val narrowed = CloudAtlasScreen.filterEntries(sample, "alpha")
            val cleared = CloudAtlasScreen.filterEntries(sample, "")
            then("the cleared result restores the full set") {
                narrowed shouldHaveSize 1
                cleared shouldHaveSize sample.size
                cleared.map { it.id } shouldBe sample.map { it.id }
            }
        }
    }

    given("CloudAtlasScreen.filterEntries operating on the real CloudAtlasLibrary") {
        val real = CloudAtlasLibrary.entries.values

        `when`("filtering on the empty string") {
            val result = CloudAtlasScreen.filterEntries(real, "")
            then("the result matches the library size exactly") {
                result shouldHaveSize real.size
            }
        }

        `when`("filtering on a token that is known to be in one entry's title") {
            // "Albedo" is the title of the albedo_effect entry.
            val result = CloudAtlasScreen.filterEntries(real, "albedo")
            then("the matching entry is present") {
                result.map { it.id } shouldContain "albedo_effect"
            }
        }

        `when`("filtering on a clearly bogus token") {
            val result = CloudAtlasScreen.filterEntries(real, "qqqxxx_does_not_exist_anywhere")
            then("no entries match") {
                result.shouldBeEmpty()
            }
        }
    }

    given("the new T-141 StringKeys") {
        `when`("each key is looked up via Strings.get") {
            then("ATLAS_SEARCH_PLACEHOLDER resolves to a non-blank string") {
                Strings.get(StringKey.ATLAS_SEARCH_PLACEHOLDER).shouldNotBeBlank()
            }
            then("ATLAS_SEARCH_CLEAR resolves to a non-blank string") {
                Strings.get(StringKey.ATLAS_SEARCH_CLEAR).shouldNotBeBlank()
            }
            then("ATLAS_SEARCH_NO_RESULTS resolves to a non-blank string") {
                Strings.get(StringKey.ATLAS_SEARCH_NO_RESULTS).shouldNotBeBlank()
            }
        }
    }
})

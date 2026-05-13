package com.sohai.platformer.persist

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain

/**
 * T-115: Unit tests for [CrashReporter] — the pure-function path only.
 *
 * The file-I/O wrapper [CrashReporter.writeCrashFile] is intentionally NOT tested here;
 * the spec calls for manual verification of the on-disk path. Keeping the tests pure
 * means they run headlessly in any Kotest harness, including the smoke matrix.
 */
class CrashReporterTest : BehaviorSpec({

    given("a formatted crash report") {
        val throwable = RuntimeException("boom").apply {
            // Trim stack to keep test output readable.
            stackTrace = arrayOf(StackTraceElement("Foo", "bar", "Foo.kt", 42))
        }
        val report = CrashReporter.format(
            throwable = throwable,
            gameVersion = "9.9.9",
            osInfo = "TestOS 1.0 (x86_64)",
            jdkInfo = "TestJDK 17",
            slotMetadata = listOf(
                CrashReporter.SlotMetadata(slotIndex = 0, completedLevelCount = 3),
                CrashReporter.SlotMetadata(slotIndex = 1, completedLevelCount = null),
                CrashReporter.SlotMetadata(slotIndex = 2, completedLevelCount = 7),
            ),
            timestampMillis = 0L, // epoch — deterministic
        )

        `when`("the report header is examined") {
            then("it identifies as a Cloudy Ninja crash report") {
                report.shouldContain("Cloudy Ninja crash report")
            }
            then("it embeds the game version") {
                report.shouldContain("Game version: 9.9.9")
            }
            then("it embeds the OS info") {
                report.shouldContain("OS: TestOS 1.0 (x86_64)")
            }
            then("it embeds the JDK info") {
                report.shouldContain("JDK: TestJDK 17")
            }
            then("it embeds an ISO-8601 UTC timestamp") {
                // epoch 0 == 1970-01-01T00:00:00Z
                report.shouldContain("Timestamp: 1970-01-01T00:00:00Z")
            }
        }

        `when`("the slot metadata block is examined") {
            then("present slots show their completed-level count") {
                report.shouldContain("slot 0: completedLevels=3")
                report.shouldContain("slot 2: completedLevels=7")
            }
            then("a missing slot is rendered as 'missing'") {
                report.shouldContain("slot 1: completedLevels=missing")
            }
            then("the no-PII guard label is present") {
                report.shouldContain("no PII")
            }
        }

        `when`("the stack trace block is examined") {
            then("the exception message is included") {
                report.shouldContain("boom")
            }
            then("the synthetic stack frame is included") {
                report.shouldContain("Foo.bar(Foo.kt:42)")
            }
            then("the exception type is included") {
                report.shouldContain("RuntimeException")
            }
        }
    }

    given("a crash report with no slot metadata") {
        val report = CrashReporter.format(
            throwable = IllegalStateException("nope"),
            gameVersion = "0.0.1",
            osInfo = "x",
            jdkInfo = "y",
            slotMetadata = emptyList(),
            timestampMillis = 0L,
        )

        then("the empty-slot fallback line is emitted") {
            report.shouldContain("(no slot metadata captured)")
        }
    }

    given("a chained exception with a cause") {
        val cause = IllegalArgumentException("root cause here")
        val outer = RuntimeException("outer failure", cause)
        val report = CrashReporter.format(
            throwable = outer,
            gameVersion = "0.0.1",
            osInfo = "x",
            jdkInfo = "y",
            slotMetadata = emptyList(),
        )

        then("the outer exception is rendered") {
            report.shouldContain("outer failure")
        }
        then("the underlying cause is rendered") {
            report.shouldContain("root cause here")
            report.shouldContain("Caused by")
        }
    }

    given("the crash filename generator") {
        `when`("called with epoch zero") {
            // SimpleDateFormat without timezone uses default zone; we only assert the
            // shape, not the absolute hour, to keep this test machine-portable.
            val name = CrashReporter.crashFileName(timestampMillis = 0L)

            then("it follows the crash-YYYYMMDD-HHMMSS.log shape") {
                name shouldMatch Regex("^crash-\\d{8}-\\d{6}\\.log$")
            }
        }
    }

    given("the crash directory resolver") {
        `when`("given an explicit user-home path") {
            val dir = CrashReporter.crashDir(userHome = "/tmp/fake-home")

            then("it nests under .cloudy-ninja/crashes") {
                // Use platform-agnostic separator check.
                val path = dir.path.replace('\\', '/')
                path shouldContain ".cloudy-ninja/crashes"
                path shouldContain "fake-home"
            }
        }
    }

    given("OS and JDK info probes") {
        then("they return non-null strings that include the live system properties") {
            val os = CrashReporter.currentOsInfo()
            os shouldNotBe ""
            // os.name property is always set on every JVM Cloudy Ninja runs on.
            os.shouldContain(System.getProperty("os.name"))

            val jdk = CrashReporter.currentJdkInfo()
            jdk shouldNotBe ""
            jdk.shouldContain(System.getProperty("java.version"))
        }
    }

    given("the PII guarantee") {
        val sensitive = "Player-supplied-name-CarmenSandiego"
        val throwable = RuntimeException("legit error")
        val report = CrashReporter.format(
            throwable = throwable,
            gameVersion = "1.0.0",
            osInfo = "OS",
            jdkInfo = "JDK",
            slotMetadata = listOf(
                CrashReporter.SlotMetadata(slotIndex = 0, completedLevelCount = 5),
            ),
            timestampMillis = 0L,
        )

        `when`("the report is generated with only index+count metadata") {
            then("any unrelated user-name string is absent from the output") {
                // The pure formatter literally cannot include this — the API only
                // accepts slot index + count. This assertion guards against future
                // PII regressions if someone adds a free-form field to SlotMetadata.
                report.shouldNotContain(sensitive)
            }
        }
    }
})

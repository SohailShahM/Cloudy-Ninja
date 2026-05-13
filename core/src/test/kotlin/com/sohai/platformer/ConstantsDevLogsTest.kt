package com.sohai.platformer

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * T-198: lock in the default state of [Constants.DEV_LOGS].
 *
 * `Constants.DEV_LOGS` is bound at JVM startup from the `cloudy.devLogs`
 * system property (via `java.lang.Boolean.getBoolean(...)`). Unit tests run
 * with that property unset, so the flag MUST be `false` by default — that's
 * the contract every `if (Constants.DEV_LOGS) Gdx.app.log(...)` call site
 * relies on to keep a default `./gradlew :lwjgl3:run` quiet.
 *
 * We can't flip the property in-test (it's read into a `final` JvmField at
 * class-init time), mirroring the [Constants.SMOKE_MODE] pattern locked in
 * by [com.sohai.platformer.util.ScreenshotWriterTest]. Verifying the default
 * is sufficient because the `-Dcloudy.devLogs=true` path is exercised by
 * `java.lang.Boolean.getBoolean`'s well-known semantics (no custom parsing
 * to test).
 */
class ConstantsDevLogsTest : BehaviorSpec({

    given("Constants.DEV_LOGS with the cloudy.devLogs property unset") {
        `when`("read from the test JVM (which never sets cloudy.devLogs)") {
            then("it is false — the default-quiet contract holds") {
                System.getProperty("cloudy.devLogs") shouldBe null
                Constants.DEV_LOGS shouldBe false
            }
        }
    }
})

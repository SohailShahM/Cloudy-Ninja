package com.sohai.platformer.persist

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * T-115: In-game crash report dumper.
 *
 * Pure object that knows how to (a) [format] a crash report from a Throwable and
 * environmental context, and (b) [writeCrashFile] to a `<userHome>/.cloudy-ninja/crashes/`
 * path. The pure [format] function is the unit-tested boundary; the file-I/O wrapper
 * is intentionally thin and exercised only via manual dev verification.
 *
 * **PII boundary:** This object never reads the *contents* of save slots. Callers must
 * pre-aggregate save metadata (slot index + completed-level count) into [SlotMetadata]
 * and pass it in. Save files may contain user-chosen names that we treat as PII.
 *
 * **Smoke mode:** The handler in `Main.kt` short-circuits via `Constants.SMOKE_MODE`
 * before calling into this object, so smoke CI never writes crash files. This object
 * itself is smoke-mode-agnostic so its pure-function tests stay simple.
 */
object CrashReporter {

    /** Per-slot metadata captured for crash diagnosis. Index + count only — no save contents. */
    data class SlotMetadata(
        val slotIndex: Int,
        /** Number of distinct level IDs marked completed in this slot, or `null` if slot missing. */
        val completedLevelCount: Int?,
    )

    /** Stable timestamp format used in the filename. UTC for cross-machine grep-ability. */
    private const val FILENAME_TIMESTAMP = "yyyyMMdd-HHmmss"

    /** Subdirectory under `user.home` where crash reports land. */
    private const val CRASH_SUBDIR = ".cloudy-ninja/crashes"

    /**
     * Build the crash log body. Pure — no I/O, no clock reads except via the
     * caller-provided [timestampMillis] (defaults to `System.currentTimeMillis()`).
     *
     * @param throwable the uncaught exception
     * @param gameVersion `Constants.BUILD_VERSION`
     * @param osInfo human-readable OS string, e.g. "Windows 11 10.0 (amd64)"
     * @param jdkInfo human-readable JDK string, e.g. "OpenJDK 17.0.10 (Oracle)"
     * @param slotMetadata metadata for each save slot — NEVER include raw save contents
     * @param timestampMillis epoch millis for the "Timestamp" header line (kept injectable for tests)
     */
    fun format(
        throwable: Throwable,
        gameVersion: String,
        osInfo: String,
        jdkInfo: String,
        slotMetadata: List<SlotMetadata>,
        timestampMillis: Long = System.currentTimeMillis(),
    ): String {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.appendLine("=== Cloudy Ninja crash report ===")
        sb.appendLine("Timestamp: ${isoFmt.format(Date(timestampMillis))}")
        sb.appendLine("Game version: $gameVersion")
        sb.appendLine("OS: $osInfo")
        sb.appendLine("JDK: $jdkInfo")
        sb.appendLine()
        sb.appendLine("--- Save slot metadata (no PII) ---")
        if (slotMetadata.isEmpty()) {
            sb.appendLine("(no slot metadata captured)")
        } else {
            slotMetadata.forEach { slot ->
                val countStr = slot.completedLevelCount?.toString() ?: "missing"
                sb.appendLine("slot ${slot.slotIndex}: completedLevels=$countStr")
            }
        }
        sb.appendLine()
        sb.appendLine("--- Stack trace ---")
        sb.append(stackTraceToString(throwable))
        return sb.toString()
    }

    /** Render a Throwable to a full stack trace string (includes causes). */
    private fun stackTraceToString(t: Throwable): String {
        val sw = StringWriter()
        PrintWriter(sw).use { t.printStackTrace(it) }
        return sw.toString()
    }

    /**
     * Build the crash filename. Pure.
     *
     * Format: `crash-yyyyMMdd-HHmmss.log` in the JVM's default time zone (matches
     * what a user filing a bug report sees on their wall clock).
     */
    fun crashFileName(timestampMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat(FILENAME_TIMESTAMP, Locale.ROOT)
        return "crash-${fmt.format(Date(timestampMillis))}.log"
    }

    /**
     * Resolve the crash directory under the JVM's user-home. Does not create it.
     */
    fun crashDir(userHome: String = System.getProperty("user.home")): File =
        File(userHome, CRASH_SUBDIR)

    /**
     * Build the OS info string from `System.getProperty` values. Pure.
     */
    fun currentOsInfo(): String {
        val name = System.getProperty("os.name") ?: "unknown"
        val version = System.getProperty("os.version") ?: "?"
        val arch = System.getProperty("os.arch") ?: "?"
        return "$name $version ($arch)"
    }

    /**
     * Build the JDK info string from `System.getProperty` values. Pure.
     */
    fun currentJdkInfo(): String {
        val vendor = System.getProperty("java.vendor") ?: "unknown"
        val version = System.getProperty("java.version") ?: "?"
        return "$vendor $version"
    }

    /**
     * I/O wrapper: write [contents] to a crash file in [crashDir]. Returns the file written,
     * or `null` if the directory could not be created or the write failed (e.g. read-only
     * filesystem). Never throws — caller is already in an uncaught-exception handler.
     *
     * Intentionally NOT covered by unit tests; verified manually per the T-115 spec.
     */
    fun writeCrashFile(
        contents: String,
        crashDir: File = crashDir(),
        timestampMillis: Long = System.currentTimeMillis(),
    ): File? {
        return try {
            if (!crashDir.exists() && !crashDir.mkdirs()) return null
            val target = File(crashDir, crashFileName(timestampMillis))
            target.writeText(contents)
            target
        } catch (e: Throwable) {
            // Last-resort: scream into stderr. The handler will fall back to stderr anyway.
            System.err.println("CrashReporter: failed to write crash file: ${e.message}")
            null
        }
    }
}

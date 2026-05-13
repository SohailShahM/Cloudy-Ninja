package com.sohai.platformer.util

/**
 * Pure utility for recording asset-preload step durations (T-162).
 *
 * A future change will wire this into [com.sohai.platformer.screens.SplashScreen]
 * so each preload step's wall-clock cost can be timed and surfaced in logs /
 * a debug overlay. That wiring is intentionally out of scope here — this file
 * is just the data sink and its formatter.
 *
 * ## Design notes
 *
 *  - No `Gdx.*` access and no I/O. The log is a plain in-memory list of
 *    `(step, durationMs)` pairs; printing / persisting is the caller's job.
 *  - Insertion order is preserved.
 *  - Duplicate step names are kept as separate entries — recording the same
 *    label twice produces two lines in [summary]. This matches how the splash
 *    preload step list may legitimately contain repeated work (e.g. multiple
 *    "Generating music" entries that share a base label across tracks).
 *  - Not thread-safe by design; the splash preload runs one step per render
 *    frame on the GL thread so a single producer is sufficient.
 */
class PreloadTimingLog {

    private val entries: MutableList<Pair<String, Long>> = mutableListOf()

    /**
     * Record a single preload step's duration.
     *
     * @param step       Human-readable label for the step (e.g. "Generating music: ambient_arid").
     * @param durationMs Wall-clock duration of the step in milliseconds.
     */
    fun record(step: String, durationMs: Long) {
        entries += step to durationMs
    }

    /**
     * Format the recorded entries as a multi-line `step -> ms` string, one
     * entry per line, in insertion order. Returns the empty string when no
     * entries have been recorded so callers can guard with `isEmpty()` or
     * simply concatenate the result into a larger log block without leading
     * whitespace.
     */
    fun summary(): String =
        entries.joinToString(separator = "\n") { (step, ms) -> "$step -> $ms ms" }
}

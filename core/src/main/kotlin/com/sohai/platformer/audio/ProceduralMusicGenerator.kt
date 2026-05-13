package com.sohai.platformer.audio

import com.badlogic.gdx.Gdx
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.*

/**
 * Generates 60-second ambient music tracks as WAV files on first run.
 * Call [generateAll] before [MusicManager] is used so the files exist.
 *
 * Follows the same WAV-writing pattern as [ProceduralSoundGenerator].
 * Output: `assets/audio/music/{trackId}.wav` — 16-bit mono PCM at 22,050 Hz.
 *
 * Three tracks are generated:
 *  - `ambient_arid` — warm, low drones with occasional wind texture
 *  - `ambient_wind` — airy sweeps, high whistles
 *  - `ambient_eco`  — lush harmonics, water-like tones
 */
object ProceduralMusicGenerator {

    private const val RATE = 22050   // 22 kHz mono — matches SFX generator
    private const val DURATION = 60f // 60 seconds per track

    /** Generate all music tracks. Safe to call multiple times — skips existing files. */
    fun generateAll() {
        val dir = Gdx.files.local("audio/music")
        if (!dir.exists()) dir.mkdirs()

        writeIfMissing("audio/music/ambient_arid.wav", makeWav(ambientArid()))
        writeIfMissing("audio/music/ambient_wind.wav", makeWav(ambientWind()))
        writeIfMissing("audio/music/ambient_eco.wav",  makeWav(ambientEco()))
    }

    /**
     * Generate a single music track by id. Used by the cold-start splash
     * (T-104) to spread WAV generation across multiple frames so a progress
     * bar can advance one step per track. Unknown ids are ignored.
     *
     * Safe to call multiple times — the on-disk write is skipped if the
     * file already exists.
     */
    fun generateOne(trackId: String) {
        val dir = Gdx.files.local("audio/music")
        if (!dir.exists()) dir.mkdirs()

        val samples: FloatArray = when (trackId) {
            "ambient_arid" -> ambientArid()
            "ambient_wind" -> ambientWind()
            "ambient_eco"  -> ambientEco()
            else           -> return
        }
        writeIfMissing("audio/music/$trackId.wav", makeWav(samples))
    }

    private fun writeIfMissing(path: String, data: ByteArray) {
        val f = Gdx.files.local(path)
        if (!f.exists()) {
            f.writeBytes(data, false)
            Gdx.app.log("MusicGen", "Generated $path (${data.size / 1024} KB)")
        }
    }

    // ── WAV builder (identical to ProceduralSoundGenerator) ──────────────────

    private fun makeWav(samples: FloatArray): ByteArray {
        val nSamples  = samples.size
        val dataBytes = nSamples * 2          // 16-bit mono
        val baos = ByteArrayOutputStream(44 + dataBytes)
        val dos  = DataOutputStream(baos)

        // RIFF header
        dos.writeBytes("RIFF")
        dos.writeIntLE(36 + dataBytes)
        dos.writeBytes("WAVE")
        // fmt chunk
        dos.writeBytes("fmt ")
        dos.writeIntLE(16)
        dos.writeShortLE(1)       // PCM
        dos.writeShortLE(1)       // mono
        dos.writeIntLE(RATE)
        dos.writeIntLE(RATE * 2)  // byteRate
        dos.writeShortLE(2)       // blockAlign
        dos.writeShortLE(16)      // bitsPerSample
        // data chunk
        dos.writeBytes("data")
        dos.writeIntLE(dataBytes)
        for (s in samples) {
            dos.writeShortLE((s.coerceIn(-1f, 1f) * 32767f).toInt())
        }
        dos.flush()
        return baos.toByteArray()
    }

    private fun DataOutputStream.writeIntLE(v: Int) {
        write(v         and 0xFF)
        write((v shr 8)  and 0xFF)
        write((v shr 16) and 0xFF)
        write((v shr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(v: Int) {
        write(v         and 0xFF)
        write((v shr 8)  and 0xFF)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun sine(freq: Float, t: Float) = sin(2.0 * PI * freq * t).toFloat()
    private fun tri(freq: Float, t: Float): Float {
        val phase = (freq * t) % 1f
        return (2f * abs(2f * phase - 1f) - 1f)
    }

    /** Slow LFO for amplitude modulation ("breathing" texture). */
    private fun lfo(rate: Float, t: Float) = (1f + sine(rate, t)) * 0.5f

    /** Soft fade at start and end of the track for seamless looping. */
    private fun loopEnv(t: Float, dur: Float, fade: Float = 2f): Float = when {
        t < fade       -> t / fade
        t > dur - fade -> (dur - t) / fade
        else           -> 1f
    }

    // ── Track 1: ambient_arid ────────────────────────────────────────────────
    // Warm, low drones with occasional wind-like texture.
    // Two detuned bass oscillators + filtered noise bursts.

    private fun ambientArid(): FloatArray {
        val n = (RATE * DURATION).toInt()
        val rng = java.util.Random(101L)
        var noisePrev = 0f
        return FloatArray(n) { i ->
            val t = i.toFloat() / RATE

            // Two detuned low drones (A1 = 55 Hz, slightly sharp)
            val drone1 = sine(55f, t) * 0.25f * lfo(0.08f, t)
            val drone2 = sine(55.4f, t) * 0.22f * lfo(0.07f, t + 5f)

            // Warm fifth harmonic (E2 = ~82 Hz)
            val fifth = tri(82.4f, t) * 0.10f * lfo(0.12f, t + 10f)

            // Occasional wind-like filtered noise
            val noise = rng.nextFloat() * 2f - 1f
            noisePrev = noisePrev * 0.92f + noise * 0.08f
            val windGate = lfo(0.15f, t) * lfo(0.07f, t + 3f)
            val wind = noisePrev * windGate * 0.12f

            // Sub-bass rumble
            val sub = sine(27.5f, t) * 0.08f * lfo(0.05f, t + 7f)

            val mix = drone1 + drone2 + fifth + wind + sub
            mix * loopEnv(t, DURATION) * 0.65f
        }
    }

    // ── Track 2: ambient_wind ────────────────────────────────────────────────
    // Airy sweeps, high whistles, breathy texture.
    // Sine sweeps at higher registers + heavy filtered noise.

    private fun ambientWind(): FloatArray {
        val n = (RATE * DURATION).toInt()
        val rng = java.util.Random(202L)
        var noisePrev1 = 0f
        var noisePrev2 = 0f
        return FloatArray(n) { i ->
            val t = i.toFloat() / RATE

            // Slow sweeping whistle (440-880 Hz range)
            val sweepHz = 440f + 220f * sine(0.03f, t) + 110f * sine(0.017f, t + 4f)
            val whistle = sine(sweepHz, t) * 0.12f * lfo(0.1f, t)

            // Higher harmonic whistle
            val sweepHz2 = 660f + 180f * sine(0.025f, t + 8f)
            val whistle2 = sine(sweepHz2, t) * 0.07f * lfo(0.13f, t + 3f)

            // Breathy filtered noise (two-pole for smoother character)
            val noise = rng.nextFloat() * 2f - 1f
            noisePrev1 = noisePrev1 * 0.85f + noise * 0.15f
            noisePrev2 = noisePrev2 * 0.90f + noisePrev1 * 0.10f
            val breath = noisePrev2 * 0.25f * lfo(0.2f, t)

            // Pad tone (gentle fifth intervals)
            val pad = sine(220f, t) * 0.06f * lfo(0.06f, t + 12f) +
                      sine(330f, t) * 0.04f * lfo(0.08f, t + 6f)

            // Gusts — periodic volume swells on noise
            val gust = noisePrev1 * 0.15f * lfo(0.04f, t + 2f) * lfo(0.11f, t)

            val mix = whistle + whistle2 + breath + pad + gust
            mix * loopEnv(t, DURATION) * 0.60f
        }
    }

    // ── Track 3: ambient_eco ─────────────────────────────────────────────────
    // Lush harmonics, water-like tones, forest alive.
    // Overtone series + pluck-like decaying tones + water drip texture.

    private fun ambientEco(): FloatArray {
        val n = (RATE * DURATION).toInt()
        val rng = java.util.Random(303L)
        var noisePrev = 0f

        // Pre-compute "water drip" event times (random intervals 1-4 seconds)
        val dripTimes = mutableListOf<Float>()
        var dt = 0.5f
        while (dt < DURATION) {
            dripTimes.add(dt)
            dt += 1f + rng.nextFloat() * 3f
        }
        // Drip pitches (pentatonic-ish: C4, D4, E4, G4, A4)
        val dripNotes = floatArrayOf(261.6f, 293.7f, 329.6f, 392f, 440f)

        return FloatArray(n) { i ->
            val t = i.toFloat() / RATE

            // Lush pad: stacked thirds (C3 + E3 + G3) with slow modulation
            val pad = sine(130.8f, t) * 0.15f * lfo(0.07f, t) +
                      sine(164.8f, t) * 0.10f * lfo(0.09f, t + 4f) +
                      sine(196f, t) * 0.08f * lfo(0.06f, t + 8f)

            // Overtone shimmer
            val shimmer = sine(523.2f, t) * 0.04f * lfo(0.15f, t + 2f) +
                          sine(659.2f, t) * 0.03f * lfo(0.18f, t + 5f)

            // Water-like drips: short decaying sine pings at random intervals
            var drip = 0f
            for (dripT in dripTimes) {
                val rel = t - dripT
                if (rel in 0f..0.5f) {
                    val dripHz = dripNotes[(dripT * 7f).toInt() % dripNotes.size]
                    val dripEnv = exp(-rel * 12.0).toFloat()
                    drip += sine(dripHz, t) * dripEnv * 0.10f
                }
            }

            // Gentle water/stream texture
            val noise = rng.nextFloat() * 2f - 1f
            noisePrev = noisePrev * 0.95f + noise * 0.05f
            val stream = noisePrev * 0.08f * lfo(0.1f, t + 1f)

            // Low forest hum
            val hum = tri(65.4f, t) * 0.06f * lfo(0.04f, t + 15f)

            val mix = pad + shimmer + drip + stream + hum
            mix * loopEnv(t, DURATION) * 0.60f
        }
    }
}

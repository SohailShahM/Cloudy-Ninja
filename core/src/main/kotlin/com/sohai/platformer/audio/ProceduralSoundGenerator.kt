package com.sohai.platformer.audio

import com.badlogic.gdx.Gdx
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.*

/**
 * Generates simple PCM sound effects and writes them as WAV files on first run.
 * Call generateAll() before SoundManager.init() so the files exist when loaded.
 *
 * On desktop the working directory is assets/, so files land in:
 *   assets/audio/sfx/   — the 8 T-013 SFX (new canonical location)
 *   assets/sounds/      — legacy sounds kept for backward compatibility
 */
object ProceduralSoundGenerator {

    private const val RATE = 22050   // 22 kHz — sufficient for game SFX

    /** Generate all SFX. Safe to call multiple times — skips existing files. */
    fun generateAll() {
        // ── New canonical location: audio/sfx/ ────────────────────────────
        val sfxDir = Gdx.files.local("audio/sfx")
        if (!sfxDir.exists()) sfxDir.mkdirs()

        writeIfMissing("audio/sfx/jump.wav",             makeWav(jump()))
        writeIfMissing("audio/sfx/land.wav",             makeWav(land()))
        writeIfMissing("audio/sfx/collect_token.wav",    makeWav(collectToken()))
        writeIfMissing("audio/sfx/collect_snapshot.wav", makeWav(collectSnapshot()))
        writeIfMissing("audio/sfx/death.wav",            makeWav(death()))
        writeIfMissing("audio/sfx/checkpoint.wav",       makeWav(checkpoint()))
        writeIfMissing("audio/sfx/level_complete.wav",   makeWav(levelComplete()))
        writeIfMissing("audio/sfx/hazard_cleansed.wav",  makeWav(hazardCleansed()))

        // ── Legacy location: sounds/ ───────────────────────────────────────
        val legacyDir = Gdx.files.local("sounds")
        if (!legacyDir.exists()) legacyDir.mkdirs()

        writeIfMissing("sounds/jump.wav",           makeWav(jump()))
        writeIfMissing("sounds/land.wav",           makeWav(land()))
        writeIfMissing("sounds/collect.wav",        makeWav(collectToken()))
        writeIfMissing("sounds/death.wav",          makeWav(death()))
        writeIfMissing("sounds/checkpoint.wav",     makeWav(checkpoint()))
        writeIfMissing("sounds/level_complete.wav", makeWav(levelComplete()))
        writeIfMissing("sounds/ability_ebo.wav",    makeWav(abilityEbo()))
        writeIfMissing("sounds/ability_laya.wav",   makeWav(abilityLaya()))
        writeIfMissing("sounds/cleanse.wav",        makeWav(hazardCleansed()))
    }

    private fun writeIfMissing(path: String, data: ByteArray) {
        val f = Gdx.files.local(path)
        if (!f.exists()) {
            f.writeBytes(data, false)
            Gdx.app.log("SoundGen", "Generated $path")
        }
    }

    // ── WAV builder ────────────────────────────────────────────────────────

    private fun makeWav(samples: FloatArray): ByteArray {
        val nSamples   = samples.size
        val dataBytes  = nSamples * 2          // 16-bit mono
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
        write(v        and 0xFF)
        write((v shr 8)  and 0xFF)
        write((v shr 16) and 0xFF)
        write((v shr 24) and 0xFF)
    }
    private fun DataOutputStream.writeShortLE(v: Int) {
        write(v        and 0xFF)
        write((v shr 8)  and 0xFF)
    }

    // ── Envelope helpers ────────────────────────────────────────────────────

    private fun env(t: Float, dur: Float, attack: Float = 0.005f, release: Float = 0.06f): Float = when {
        t < attack        -> t / attack
        t > dur - release -> (dur - t) / release
        else              -> 1f
    }

    private fun sine(freq: Float, t: Float) = sin(2.0 * PI * freq * t).toFloat()

    // ── T-013 SFX (canonical) ───────────────────────────────────────────────

    /** 1. jump.wav — short punchy blip ~0.1s: freq sweep 180→520 Hz */
    private fun jump(): FloatArray {
        val dur = 0.10f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val hz = 180f + p * 340f   // 180→520 Hz sweep
            sine(hz, t) * env(t, dur, 0.003f, 0.045f) * 0.55f
        }
    }

    /** 2. land.wav — soft thud ~0.1s: low sine + noise burst, fast exponential decay */
    private fun land(): FloatArray {
        val dur = 0.10f; val n = (RATE * dur).toInt()
        val rng = java.util.Random(42L)
        return FloatArray(n) { i ->
            val t   = i.toFloat() / RATE
            val dec = exp(-t * 40.0).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f) * 0.30f
            val thud  = sine(70f, t) * 0.55f
            (noise + thud) * dec * 0.80f
        }
    }

    /** 3. collect_token.wav — bright bell ~0.2s: C5→G5 two-tone */
    private fun collectToken(): FloatArray {
        val dur = 0.20f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val hz = if (t < 0.10f) 523f else 784f   // C5 → G5
            val dec = exp(-t * 8.0).toFloat()
            sine(hz, t) * dec * env(t, dur, 0.003f, 0.07f) * 0.50f
        }
    }

    /** 4. collect_snapshot.wav — bell with reverb tail ~0.4s: C5 E5 G5 arpeggio + long decay */
    private fun collectSnapshot(): FloatArray {
        val dur = 0.40f; val n = (RATE * dur).toInt()
        val notes = floatArrayOf(523f, 659f, 784f)   // C5 E5 G5
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val hz = notes[((p * notes.size).toInt()).coerceAtMost(notes.size - 1)]
            val dec = exp(-t * 4.0).toFloat()   // long tail for "glow" feel
            val sig = sine(hz, t) * 0.6f + sine(hz * 2f, t) * 0.25f
            sig * dec * env(t, dur, 0.003f, 0.15f) * 0.50f
        }
    }

    /** 5. death.wav — short break/noise ~0.3s: descending pitch + tremolo + noise */
    private fun death(): FloatArray {
        val dur = 0.30f; val n = (RATE * dur).toInt()
        val rng = java.util.Random(77L)
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val hz = 380f - p * 230f   // 380→150 Hz descend
            val tremolo = 1f + 0.30f * sine(12f, t)
            val noise = (rng.nextFloat() * 2f - 1f) * 0.20f * exp(-t * 5.0).toFloat()
            val tone = sine(hz, t) * tremolo * 0.45f
            (tone + noise) * env(t, dur, 0.008f, 0.15f) * 0.75f
        }
    }

    /** 6. checkpoint.wav — ascending arpeggio ~0.5s: C5 E5 G5 C6 */
    private fun checkpoint(): FloatArray {
        val dur = 0.50f; val n = (RATE * dur).toInt()
        val notes = floatArrayOf(523f, 659f, 784f, 1047f)   // C5 E5 G5 C6
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val noteIdx = ((p * notes.size).toInt()).coerceAtMost(notes.size - 1)
            val hz = notes[noteIdx]
            val noteStart = noteIdx / notes.size.toFloat() * dur
            val nt = t - noteStart
            val noteEnv = minOf(1f, nt / 0.008f) * exp(-nt * 9.0).toFloat()
            sine(hz, t) * noteEnv * 0.55f
        }
    }

    /** 7. level_complete.wav — celebratory stinger ~1.0s: C5 E5 G5 C6 + harmonics + long tail */
    private fun levelComplete(): FloatArray {
        val dur = 1.00f; val n = (RATE * dur).toInt()
        val notes = floatArrayOf(523f, 659f, 784f, 1047f)   // C5 E5 G5 C6
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val noteIdx = ((p * notes.size).toInt()).coerceAtMost(notes.size - 1)
            val hz = notes[noteIdx]
            val noteStart = noteIdx / notes.size.toFloat() * dur
            val nt = t - noteStart
            val noteEnv = minOf(1f, nt / 0.006f) * exp(-nt * 3.5).toFloat()
            val sig = sine(hz, t) + sine(hz * 1.25f, t) * 0.30f + sine(hz * 1.50f, t) * 0.20f
            sig * noteEnv * 0.40f
        }
    }

    /** 8. hazard_cleansed.wav — bubbly sizzle ~0.3s: rising filtered noise + sine sweep */
    private fun hazardCleansed(): FloatArray {
        val dur = 0.30f; val n = (RATE * dur).toInt()
        val rng = java.util.Random(55L)
        var prev = 0f
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val noise = rng.nextFloat() * 2f - 1f
            prev = prev * 0.60f + noise * 0.40f   // one-pole LP filter ("sizzle")
            val hz = 300f + p * 500f               // rising 300→800 Hz sweep
            val tone = sine(hz, t) * 0.35f + sine(hz * 1.5f, t) * 0.15f
            val bubbles = 1f + 0.40f * sine(18f + p * 10f, t)
            (prev * 0.35f + tone) * bubbles * env(t, dur, 0.010f, 0.10f) * 0.65f
        }
    }

    // ── Legacy sounds (kept for backward compatibility) ─────────────────────

    private fun abilityEbo(): FloatArray {
        val dur = 0.24f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t   = i.toFloat() / RATE
            val dec = exp(-t * 16.0).toFloat()
            val boom  = sine(85f, t)  * 0.70f
            val click = sine(620f, t) * exp(-t * 85.0).toFloat() * 0.30f
            (boom + click) * dec * 0.80f
        }
    }

    private fun abilityLaya(): FloatArray {
        val dur = 0.24f; val n = (RATE * dur).toInt()
        val rng = java.util.Random(99L)
        var prev = 0f
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val noise = rng.nextFloat() * 2f - 1f
            prev  = prev * 0.55f + noise * 0.45f    // one-pole low-pass
            val tone = sine(280f + p * 420f, t) * 0.30f
            (prev * 0.50f + tone) * env(t, dur, 0.018f, 0.12f) * 0.68f
        }
    }
}

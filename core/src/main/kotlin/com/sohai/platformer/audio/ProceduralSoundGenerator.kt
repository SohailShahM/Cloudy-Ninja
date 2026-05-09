package com.sohai.platformer.audio

import com.badlogic.gdx.Gdx
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.*

/**
 * Generates simple PCM sound effects and writes them as WAV files on first run.
 * Call generateAll() before SoundManager.init() so the files exist when loaded.
 * On desktop the working directory is assets/, so files land in assets/sounds/.
 */
object ProceduralSoundGenerator {

    private const val RATE = 22050   // 22 kHz — sufficient for game SFX

    fun generateAll() {
        val dir = Gdx.files.local("sounds")
        if (!dir.exists()) dir.mkdirs()

        writeIfMissing("sounds/jump.wav",           makeWav(jump()))
        writeIfMissing("sounds/land.wav",           makeWav(land()))
        writeIfMissing("sounds/collect.wav",        makeWav(collect()))
        writeIfMissing("sounds/death.wav",          makeWav(death()))
        writeIfMissing("sounds/checkpoint.wav",     makeWav(checkpoint()))
        writeIfMissing("sounds/level_complete.wav", makeWav(levelComplete()))
        writeIfMissing("sounds/ability_ebo.wav",    makeWav(abilityEbo()))
        writeIfMissing("sounds/ability_laya.wav",   makeWav(abilityLaya()))
        writeIfMissing("sounds/cleanse.wav",        makeWav(cleanse()))
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

    // ── Individual sounds ───────────────────────────────────────────────────

    private fun jump(): FloatArray {
        val dur = 0.13f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val hz = 170f + (t / dur) * 320f      // 170→490 Hz sweep
            sine(hz, t) * env(t, dur, 0.003f, 0.05f) * 0.55f
        }
    }

    private fun land(): FloatArray {
        val dur = 0.10f; val n = (RATE * dur).toInt()
        val rng = java.util.Random(42L)
        return FloatArray(n) { i ->
            val t   = i.toFloat() / RATE
            val dec = exp(-t * 38.0).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f) * 0.35f
            val thud  = sine(75f, t) * 0.55f
            (noise + thud) * dec * 0.80f
        }
    }

    private fun collect(): FloatArray {
        val dur = 0.20f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val hz = if (t < 0.10f) 523f else 784f  // C5 → G5
            sine(hz, t) * env(t, dur, 0.003f, 0.07f) * 0.50f
        }
    }

    private fun cleanse(): FloatArray {
        val dur = 0.28f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val hz = 300f + p * 500f              // rising whoosh
            val s1 = sine(hz, t) * 0.4f
            val s2 = sine(hz * 1.5f, t) * 0.2f   // fifth harmonic
            (s1 + s2) * env(t, dur, 0.01f, 0.10f) * 0.65f
        }
    }

    private fun death(): FloatArray {
        val dur = 0.42f; val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val p  = t / dur
            val hz = 360f - p * 210f              // 360→150 Hz descend
            val tremolo = 1f + 0.28f * sine(13f, t)
            sine(hz, t) * env(t, dur, 0.01f, 0.18f) * tremolo * 0.50f
        }
    }

    private fun checkpoint(): FloatArray {
        val dur = 0.38f; val n = (RATE * dur).toInt()
        val notes = floatArrayOf(523f, 659f, 784f)   // C5 E5 G5
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val hz = notes[((t / dur) * notes.size).toInt().coerceAtMost(notes.size - 1)]
            sine(hz, t) * env(t, dur, 0.004f, 0.10f) * 0.45f
        }
    }

    private fun levelComplete(): FloatArray {
        val dur = 0.60f; val n = (RATE * dur).toInt()
        val notes = floatArrayOf(523f, 659f, 784f, 1047f)  // C5 E5 G5 C6
        return FloatArray(n) { i ->
            val t  = i.toFloat() / RATE
            val hz = notes[((t / dur) * notes.size).toInt().coerceAtMost(notes.size - 1)]
            val s  = sine(hz, t) + sine(hz * 2f, t) * 0.30f
            s * env(t, dur, 0.004f, 0.15f) * 0.40f
        }
    }

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

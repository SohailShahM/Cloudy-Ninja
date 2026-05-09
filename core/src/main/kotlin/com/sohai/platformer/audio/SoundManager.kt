package com.sohai.platformer.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable

object SoundManager : Disposable {

    private var enabled = true
    private var volume = 0.8f
    private val sounds = mutableMapOf<String, Sound>()

    private val soundFiles = mapOf(
        "jump"           to "sounds/jump.wav",
        "land"           to "sounds/land.wav",
        "collect"        to "sounds/collect.wav",
        "cleanse"        to "sounds/cleanse.wav",
        "death"          to "sounds/death.wav",
        "checkpoint"     to "sounds/checkpoint.wav",
        "level_complete" to "sounds/level_complete.wav",
        "ability_ebo"    to "sounds/ability_ebo.wav",
        "ability_laya"   to "sounds/ability_laya.wav"
    )

    fun init() {
        if (sounds.isNotEmpty()) return
        for ((name, path) in soundFiles) {
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.log("SoundManager", "Sound not found: $path (drop file to enable)")
                continue
            }
            try {
                sounds[name] = Gdx.audio.newSound(Gdx.files.internal(path))
            } catch (e: Exception) {
                Gdx.app.error("SoundManager", "Failed to load: $path", e)
            }
        }
    }

    fun play(name: String, pitch: Float = 1f) {
        if (!enabled) return
        sounds[name]?.play(volume, pitch, 0f)
    }

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }
    fun setEnabled(e: Boolean) { enabled = e }

    override fun dispose() {
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }
}

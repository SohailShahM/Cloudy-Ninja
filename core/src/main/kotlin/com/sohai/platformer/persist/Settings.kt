package com.sohai.platformer.persist

import com.badlogic.gdx.Gdx
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cross-slot, cross-save settings: volumes, keybinds, accessibility.
 * Unlike GameState, this lives in a single file and persists across all save slots.
 */
@Serializable
data class Settings(
    // Audio (0..1)
    val volMusic: Float = 0.7f,
    val volSfx: Float = 0.9f,
    val volAmbient: Float = 0.6f,
    val volUi: Float = 0.8f,

    // Visual / accessibility
    val screenShake: Boolean = true,
    val deathFlash: Boolean = true,
    val showFps: Boolean = false,

    // Assist mode (Celeste-inspired) — flags relax difficulty for accessibility
    val assistInfiniteSpirits: Boolean = false,
    val assistSlowSpeed: Float = 1f,        // 1.0 = normal; 0.5 = half-speed
    val assistInvincible: Boolean = false
)

/**
 * Loads/saves [Settings] to a single shared `settings.json` file at the local
 * libGDX writable path. Independent of the save-slot system.
 */
object SettingsManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private const val FILE = "settings.json"

    @Volatile
    private var cached: Settings? = null

    fun load(): Settings {
        cached?.let { return it }
        return try {
            val f = Gdx.files.local(FILE)
            if (!f.exists()) Settings().also { cached = it }
            else json.decodeFromString<Settings>(f.readString()).also { cached = it }
        } catch (e: Exception) {
            Gdx.app.error("SettingsManager", "Failed to load: ${e.message}; using defaults")
            Settings().also { cached = it }
        }
    }

    fun save(s: Settings) {
        cached = s
        try {
            val tmp = Gdx.files.local("$FILE.tmp")
            tmp.writeString(json.encodeToString(s), false)
            val final = Gdx.files.local(FILE)
            if (final.exists()) final.delete()
            tmp.copyTo(final)
            tmp.delete()
        } catch (e: Exception) {
            Gdx.app.error("SettingsManager", "Failed to save: ${e.message}")
        }
    }

    /** Convenience: update a single field via lambda, save, and return the new state. */
    fun update(transform: (Settings) -> Settings): Settings {
        val next = transform(load())
        save(next)
        return next
    }

    /** For tests. Resets the in-memory cache so the next load() reads fresh. */
    fun resetCacheForTest() { cached = null }
}

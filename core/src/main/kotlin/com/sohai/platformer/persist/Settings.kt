package com.sohai.platformer.persist

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Color-blind palette modes. Selecting any non-OFF value swaps a curated set of
 * gameplay colors (hazards, eco-tokens, snapshot pickups, portals) for shades
 * that remain distinguishable under the most common forms of color-vision
 * deficiency. OFF keeps the original look exactly.
 */
enum class ColorBlindMode { OFF, DEUTERANOPIA, PROTANOPIA, TRITANOPIA }

/** Returns the default keyboard bindings: action-name → Input.Keys keycode. */
fun defaultKeybinds(): Map<String, Int> = mapOf(
    "left" to Input.Keys.A,
    "right" to Input.Keys.D,
    "jump" to Input.Keys.SPACE,
    "action" to Input.Keys.E,
    "swap" to Input.Keys.S
)

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
    val volUi: Float = 0.9f,

    // Display
    val fullscreen: Boolean = false,
    /** Windowed-mode width in physical pixels. Ignored when [fullscreen] is true. */
    val displayWidth: Int = 1280,
    /** Windowed-mode height in physical pixels. Ignored when [fullscreen] is true. */
    val displayHeight: Int = 720,

    // Visual / accessibility
    val screenShake: Boolean = true,
    val deathFlash: Boolean = true,
    val showFps: Boolean = false,

    // Keyboard bindings: action name → Input.Keys keycode
    val keybinds: Map<String, Int> = defaultKeybinds(),

    // Assist mode (Celeste-inspired) — flags relax difficulty for accessibility
    val assistInfiniteSpirits: Boolean = false,
    val assistSlowSpeed: Float = 1f,        // 1.0 = normal; 0.5 = half-speed
    val assistInvincible: Boolean = false,

    // Art style: id of the active TilesetPack (see TilesetRegistry).
    // Default keeps existing saves backward-compatible (kotlinx-serialization
    // returns this value when loading older settings.json files that lack the field).
    val tilesetPackId: String = "kenney_pixel_platformer",

    // Accessibility: color-blind palette. Default OFF keeps existing saves
    // backward-compatible — older settings.json files without this field will
    // deserialize with OFF and render exactly as before.
    val colorBlindMode: ColorBlindMode = ColorBlindMode.OFF,

    // Accessibility: reduced-motion mode. When true, disables screen shake,
    // clamps particle bursts to a single particle, and freezes the parallax
    // background scroll. Default false keeps existing saves byte-identical.
    val reducedMotion: Boolean = false,

    // Accessibility: high-contrast mode (T-132). When true, gameplay rendering
    // remaps every colour role to a maximum-contrast variant via
    // [com.sohai.platformer.rendering.HighContrastPalette]. Coexists with
    // [colorBlindMode] (both can be on; high-contrast wins when both apply to
    // the same role). Default false keeps existing saves byte-identical and
    // renders byte-identically to pre-T-132.
    val highContrast: Boolean = false,

    // Display feel: camera look-ahead in motion direction (T-144). When true
    // (the default), the camera smoothly offsets up to ±48px in the direction
    // the player is moving so more of the level ahead is visible. Players who
    // prefer a static camera can toggle this off; in that case the camera
    // re-centres on the player without bias. Coexists with screen shake — the
    // two offsets sum on each frame. Default true keeps the feel-good preset.
    val cameraLookAhead: Boolean = true
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

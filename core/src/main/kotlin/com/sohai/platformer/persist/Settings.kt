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
    // T-121: swap moved from S → Q. Every Layout-A platformer surveyed in
    // research/keyboard-layout-conventions.md reserves S for downward movement
    // (crouch/drop/duck); Q has the strongest "cycle/switch" precedent.
    // SettingsManager.load() auto-upgrades pre-T-121 saves to Q unless the
    // user already opened the Controls panel (keybindsCustomized = true).
    "swap" to Input.Keys.Q,
    // T-133: hold-R-to-restart hotkey. Bound to R by default; rebindable in
    // Settings → Controls. The 0.5s hold threshold is enforced at the call
    // site (GameScreen) — InputManager only reports raw held state.
    "restart" to Input.Keys.R,
    // T-118: master mute hotkey. Tap to toggle [Settings.muted]. The flag is
    // already wired into MusicManager/SoundManager (T-105), so this binding
    // is just an input edge — the slider position is preserved across
    // mute/unmute by the existing output-gate design.
    "mute" to Input.Keys.M
)

/**
 * Cross-slot, cross-save settings: volumes, keybinds, accessibility.
 * Unlike GameState, this lives in a single file and persists across all save slots.
 */
@Serializable
data class Settings(
    // Audio (0..1)
    //
    // T-105: master volume sits above the per-bus sliders. Effective volume
    // for any bus is `volMaster * volBus * (if (muted) 0f else 1f) * <duck>`.
    // Mute is intentionally a separate flag so the slider value is preserved
    // across mute/unmute — unmuting restores the user's previous master level.
    // T-118 reuses the [muted] flag for the M-keybind transient mute.
    val volMaster: Float = 1.0f,
    val muted: Boolean = false,
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

    // T-121: tracks whether the user has ever rebound a key in the Controls
    // panel. SettingsManager.update(fn) flips this true whenever the
    // [keybinds] map changes. It gates the one-shot S→Q swap-default
    // migration in SettingsManager.load(): a player who has never opened
    // Controls is treated as still on defaults and silently upgraded; a
    // player who explicitly rebound is left alone even if their swap is
    // still S. Default false keeps legacy saves byte-compatible — they
    // wouldn't be filing bug reports about S working — and naturally
    // routes them into the auto-upgrade branch on next launch.
    val keybindsCustomized: Boolean = false,

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
    val cameraLookAhead: Boolean = true,

    // T-142: Speedrun timer overlay. When true, the gameplay HUD renders a
    // high-precision MM:SS.mmm timer in the top-left corner driven by the
    // existing [LevelRunState.levelTimer] (no new clock). Default false keeps
    // existing saves byte-identical and renders byte-identically to pre-T-142.
    // Coexists with the existing best-time / score-block timer — both are
    // visible simultaneously when this is on.
    val speedrunTimer: Boolean = false
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
            val raw = if (!f.exists()) Settings()
            else json.decodeFromString<Settings>(f.readString())
            migrate(raw).also { cached = it }
        } catch (e: Exception) {
            Gdx.app.error("SettingsManager", "Failed to load: ${e.message}; using defaults")
            Settings().also { cached = it }
        }
    }

    /**
     * T-121 swap-default migration. If the user has never opened the Controls
     * panel ([Settings.keybindsCustomized] is false) and they're still on the
     * pre-T-121 default of S for swap, silently upgrade their swap binding
     * to Q. Players who already rebound (or who explicitly kept S) are
     * detected by [Settings.keybindsCustomized] = true and left alone.
     *
     * Idempotent: once the swap is Q, the function is a no-op on subsequent
     * calls regardless of the customized flag.
     */
    private fun migrate(s: Settings): Settings {
        if (!s.keybindsCustomized && s.keybinds["swap"] == Input.Keys.S) {
            return s.copy(keybinds = s.keybinds + ("swap" to Input.Keys.Q))
        }
        return s
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
        val prior = load()
        val transformed = transform(prior)
        // T-121: any change to the [keybinds] map (regardless of caller) flips
        // the customized flag so we never auto-upgrade a player who has
        // explicitly touched their controls. This is done in the persist
        // layer so SettingsScreen (and any future rebinding UI) doesn't
        // need to be aware of the flag — see T-121 PR for the scope note
        // on why this is not detected in SettingsScreen itself.
        val next = if (!transformed.keybindsCustomized && transformed.keybinds != prior.keybinds) {
            transformed.copy(keybindsCustomized = true)
        } else transformed
        save(next)
        return next
    }

    /** For tests. Resets the in-memory cache so the next load() reads fresh. */
    fun resetCacheForTest() { cached = null }
}

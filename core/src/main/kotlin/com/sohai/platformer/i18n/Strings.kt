package com.sohai.platformer.i18n

/**
 * String key catalog for the game's user-facing text.
 *
 * T-059: i18n scaffolding. Centralizes English copy so a future locale system
 * can plug in. No locale support yet — [Strings.get] always returns English.
 *
 * Keys are grouped by screen/area. Naming convention: SCREEN_PURPOSE.
 */
enum class StringKey {
    // ── Main menu ────────────────────────────────────────────────────────────
    MAIN_TITLE,
    MAIN_SLOT_EMPTY,
    MAIN_BTN_LOAD,
    MAIN_BTN_DELETE,
    MAIN_BTN_DELETE_CONFIRM,
    MAIN_BTN_NEW_GAME,
    MAIN_BTN_LEVEL_SELECT,
    MAIN_BTN_CLOUD_ATLAS,
    MAIN_BTN_STATS,
    MAIN_BTN_SETTINGS,
    MAIN_BTN_QUIT,

    // ── Settings: headers ────────────────────────────────────────────────────
    SETTINGS_TITLE,
    SETTINGS_DISPLAY,
    SETTINGS_AUDIO,
    SETTINGS_CONTROLS,
    SETTINGS_ACCESSIBILITY,

    // ── Settings: Display ────────────────────────────────────────────────────
    SETTINGS_RESOLUTION,
    SETTINGS_FULLSCREEN,
    SETTINGS_SPRITES_HINT,
    SETTINGS_SHOW_FPS,
    SETTINGS_RES_720,
    SETTINGS_RES_1080,
    SETTINGS_RES_1440,
    SETTINGS_RES_2160,

    // ── Settings: Audio ──────────────────────────────────────────────────────
    SETTINGS_MUSIC,
    SETTINGS_SFX,
    SETTINGS_UI_VOLUME,

    // ── Settings: Controls ───────────────────────────────────────────────────
    SETTINGS_MOVE_LEFT,
    SETTINGS_MOVE_RIGHT,
    SETTINGS_JUMP,
    SETTINGS_ACTION,
    SETTINGS_SWAP_CHARACTER,
    SETTINGS_PRESS_KEY,
    SETTINGS_RESET_DEFAULTS,
    SETTINGS_CONTROLS_RESET,

    // ── Settings: Accessibility ──────────────────────────────────────────────
    SETTINGS_COLORBLIND_MODE,
    SETTINGS_REDUCED_MOTION,
    SETTINGS_SCREEN_SHAKE,
    SETTINGS_DEATH_FLASH,
    SETTINGS_ASSIST_MODE_HINT,
    SETTINGS_ASSIST_INFINITE_SPIRITS,
    SETTINGS_ASSIST_INVINCIBLE,
    SETTINGS_ASSIST_SLOW_SPEED,

    // ── Settings: Save controls + feedback ───────────────────────────────────
    SETTINGS_SAVE,
    SETTINGS_LOAD,
    SETTINGS_DELETE,
    SETTINGS_TOAST_SAVED,
    SETTINGS_TOAST_LOADED,
    SETTINGS_TOAST_DELETED,
    SETTINGS_TOAST_DISPLAY_UPDATED,
    SETTINGS_BACK,

    // ── Cloud Atlas overlay ──────────────────────────────────────────────────
    ATLAS_OVERLAY_GOT_IT,
    ATLAS_OVERLAY_CLOSE_HINT,

    // ── Cloud Atlas screen ───────────────────────────────────────────────────
    ATLAS_TITLE,
    ATLAS_LOCKED,
    ATLAS_BACK,
    ATLAS_SELECT_HINT,

    // ── Game-over overlay ────────────────────────────────────────────────────
    GAME_OVER_TITLE,
    GAME_OVER_SUBTITLE,
    GAME_OVER_TRY_AGAIN,
    GAME_OVER_MAIN_MENU,

    // ── HUD ──────────────────────────────────────────────────────────────────
    HUD_BTN_LEFT,
    HUD_BTN_RIGHT,
    HUD_BTN_JUMP,
    HUD_BTN_ACTION,
    HUD_BTN_SWAP,
    HUD_INIT_CHAR_ABILITY,
    HUD_INIT_SPIRIT,
    HUD_INIT_SCORE,
    HUD_INIT_TIMER,
    HUD_INIT_STOPWATCH,

    // ── Level-complete overlay ───────────────────────────────────────────────
    LEVEL_COMPLETE_TITLE,
    LEVEL_COMPLETE_TIME,
    LEVEL_COMPLETE_SCORE,
    LEVEL_COMPLETE_ECO_TOKENS,
    LEVEL_COMPLETE_CONTINUE,

    // ── Level-select screen ──────────────────────────────────────────────────
    LEVEL_SELECT_TITLE,
    LEVEL_SELECT_NOT_CLEARED,
    LEVEL_SELECT_BTN_PLAY,
    LEVEL_SELECT_BTN_LOCKED,
    LEVEL_SELECT_BACK,

    // ── Pause overlay ────────────────────────────────────────────────────────
    PAUSE_TITLE,
    PAUSE_RESUME,
    PAUSE_RESTART,
    PAUSE_MAIN_MENU,
    PAUSE_EXIT_TIME_TRIAL,
    PAUSE_ENTER_TIME_TRIAL,
    PAUSE_KEY_ESC,

    // ── Stats screen ─────────────────────────────────────────────────────────
    STATS_TITLE,
    STATS_BACK,
    STATS_EMPTY,
    STATS_DASH,
    STATS_BEST_TIMES_HEADER,
    STATS_BEST_TIMES_EMPTY,
    STATS_ACHIEVEMENTS_MISSING,
    STATS_ACHIEVEMENTS_NONE,

    // ── Victory screen ───────────────────────────────────────────────────────
    VICTORY_TITLE,
    VICTORY_SUBTITLE,
    VICTORY_NEW_BEST,
    VICTORY_MAIN_MENU,
    VICTORY_PLAY_AGAIN,

    // ── Gameplay HUD transient messages ──────────────────────────────────────
    RUN_BOSS_DEFEATED,
    RUN_SPIRIT_EXHAUSTED,
    RUN_ECOSYSTEM_RESTORED,
}

/**
 * English-default string lookup. Future locale system can swap [english] for a
 * per-locale table without touching call sites.
 */
object Strings {
    private val english: Map<StringKey, String> = mapOf(
        // Main menu
        StringKey.MAIN_TITLE              to "Cloudy Ninja",
        StringKey.MAIN_SLOT_EMPTY         to "-- Empty --",
        StringKey.MAIN_BTN_LOAD           to "Load",
        StringKey.MAIN_BTN_DELETE         to "Delete",
        StringKey.MAIN_BTN_DELETE_CONFIRM to "Confirm?",
        StringKey.MAIN_BTN_NEW_GAME       to "New Game",
        StringKey.MAIN_BTN_LEVEL_SELECT   to "Level Select",
        StringKey.MAIN_BTN_CLOUD_ATLAS    to "Cloud Atlas",
        StringKey.MAIN_BTN_STATS          to "Stats",
        StringKey.MAIN_BTN_SETTINGS       to "Settings",
        StringKey.MAIN_BTN_QUIT           to "Quit",

        // Settings: headers
        StringKey.SETTINGS_TITLE         to "SETTINGS",
        StringKey.SETTINGS_DISPLAY       to "Display",
        StringKey.SETTINGS_AUDIO         to "Audio",
        StringKey.SETTINGS_CONTROLS      to "Controls",
        StringKey.SETTINGS_ACCESSIBILITY to "Accessibility",

        // Settings: Display
        StringKey.SETTINGS_RESOLUTION    to "Resolution",
        StringKey.SETTINGS_FULLSCREEN    to " Fullscreen",
        StringKey.SETTINGS_SPRITES_HINT  to "Sprites sharpen fully at next launch.",
        StringKey.SETTINGS_SHOW_FPS      to " Show FPS (console)",
        StringKey.SETTINGS_RES_720       to "1280 × 720  (HD)",
        StringKey.SETTINGS_RES_1080      to "1920 × 1080  (Full HD)",
        StringKey.SETTINGS_RES_1440      to "2560 × 1440  (2K / QHD)",
        StringKey.SETTINGS_RES_2160      to "3840 × 2160  (4K / UHD)",

        // Settings: Audio
        StringKey.SETTINGS_MUSIC     to "Music",
        StringKey.SETTINGS_SFX       to "SFX",
        StringKey.SETTINGS_UI_VOLUME to "UI",

        // Settings: Controls
        StringKey.SETTINGS_MOVE_LEFT       to "Move Left",
        StringKey.SETTINGS_MOVE_RIGHT      to "Move Right",
        StringKey.SETTINGS_JUMP            to "Jump",
        StringKey.SETTINGS_ACTION          to "Action",
        StringKey.SETTINGS_SWAP_CHARACTER  to "Swap Character",
        StringKey.SETTINGS_PRESS_KEY       to "Press a key...",
        StringKey.SETTINGS_RESET_DEFAULTS  to "Reset to Defaults",
        StringKey.SETTINGS_CONTROLS_RESET  to "Controls reset!",

        // Settings: Accessibility
        StringKey.SETTINGS_COLORBLIND_MODE        to "Color-blind mode",
        StringKey.SETTINGS_REDUCED_MOTION         to " Reduce motion (disable shake, limit particles, freeze background)",
        StringKey.SETTINGS_SCREEN_SHAKE           to " Screen Shake",
        StringKey.SETTINGS_DEATH_FLASH            to " Death Flash",
        StringKey.SETTINGS_ASSIST_MODE_HINT       to "Assist Mode — relax the challenge as needed.",
        StringKey.SETTINGS_ASSIST_INFINITE_SPIRITS to " Infinite Spirits (no game over)",
        StringKey.SETTINGS_ASSIST_INVINCIBLE      to " Invincible (no damage)",
        StringKey.SETTINGS_ASSIST_SLOW_SPEED      to "Slow Speed",

        // Settings: Save controls + feedback
        StringKey.SETTINGS_SAVE                  to "Save",
        StringKey.SETTINGS_LOAD                  to "Load",
        StringKey.SETTINGS_DELETE                to "Delete",
        StringKey.SETTINGS_TOAST_SAVED           to "Saved!",
        StringKey.SETTINGS_TOAST_LOADED          to "Loaded!",
        StringKey.SETTINGS_TOAST_DELETED         to "Deleted!",
        StringKey.SETTINGS_TOAST_DISPLAY_UPDATED to "Display updated — sprites sharpen at next launch",
        StringKey.SETTINGS_BACK                  to "Back",

        // Cloud Atlas overlay
        StringKey.ATLAS_OVERLAY_GOT_IT      to "Got it!",
        StringKey.ATLAS_OVERLAY_CLOSE_HINT  to "[ESC / tap to close]",

        // Cloud Atlas screen
        StringKey.ATLAS_TITLE        to "CLOUD ATLAS",
        StringKey.ATLAS_LOCKED       to "??? (locked)",
        StringKey.ATLAS_BACK         to "Back to Menu",
        StringKey.ATLAS_SELECT_HINT  to "Select a snapshot to read.",

        // Game-over overlay
        StringKey.GAME_OVER_TITLE      to "SPIRIT EXHAUSTED",
        StringKey.GAME_OVER_SUBTITLE   to "All spirit charges depleted",
        StringKey.GAME_OVER_TRY_AGAIN  to "Try Again",
        StringKey.GAME_OVER_MAIN_MENU  to "Main Menu",

        // HUD
        StringKey.HUD_BTN_LEFT         to "<",
        StringKey.HUD_BTN_RIGHT        to ">",
        StringKey.HUD_BTN_JUMP         to "Jump",
        StringKey.HUD_BTN_ACTION       to "Action",
        StringKey.HUD_BTN_SWAP         to "Swap",
        StringKey.HUD_INIT_CHAR_ABILITY to "Ebo — Seed Slam",
        StringKey.HUD_INIT_SPIRIT      to "Spirit: ***",
        StringKey.HUD_INIT_SCORE       to "Score: 0",
        StringKey.HUD_INIT_TIMER       to "0:00",
        StringKey.HUD_INIT_STOPWATCH   to "⏱ 0:00.0",

        // Level-complete overlay
        StringKey.LEVEL_COMPLETE_TITLE       to "LEVEL COMPLETE",
        StringKey.LEVEL_COMPLETE_TIME        to "Time",
        StringKey.LEVEL_COMPLETE_SCORE       to "Score",
        StringKey.LEVEL_COMPLETE_ECO_TOKENS  to "Eco-Tokens",
        StringKey.LEVEL_COMPLETE_CONTINUE    to "Continue",

        // Level-select screen
        StringKey.LEVEL_SELECT_TITLE       to "SELECT WORLD",
        StringKey.LEVEL_SELECT_NOT_CLEARED to "Not yet cleared",
        StringKey.LEVEL_SELECT_BTN_PLAY    to "Play",
        StringKey.LEVEL_SELECT_BTN_LOCKED  to "Locked",
        StringKey.LEVEL_SELECT_BACK        to "Back",

        // Pause overlay
        StringKey.PAUSE_TITLE             to "PAUSED",
        StringKey.PAUSE_RESUME            to "Resume",
        StringKey.PAUSE_RESTART           to "Restart Level",
        StringKey.PAUSE_MAIN_MENU         to "Main Menu",
        StringKey.PAUSE_EXIT_TIME_TRIAL   to "Exit Time Trial",
        StringKey.PAUSE_ENTER_TIME_TRIAL  to "▶ Time Trial",
        StringKey.PAUSE_KEY_ESC           to "Esc",

        // Stats screen
        StringKey.STATS_TITLE                 to "STATS",
        StringKey.STATS_BACK                  to "Back",
        StringKey.STATS_EMPTY                 to "— Empty —",
        StringKey.STATS_DASH                  to "—",
        StringKey.STATS_BEST_TIMES_HEADER     to "Best times:",
        StringKey.STATS_BEST_TIMES_EMPTY      to "Best times: (no times recorded)",
        StringKey.STATS_ACHIEVEMENTS_MISSING  to "Achievements unlocked: —",
        StringKey.STATS_ACHIEVEMENTS_NONE     to "—",

        // Victory screen
        StringKey.VICTORY_TITLE      to "MISSION COMPLETE!",
        StringKey.VICTORY_SUBTITLE   to "The ecosystem has been restored.",
        StringKey.VICTORY_NEW_BEST   to "★ NEW BEST! ★",
        StringKey.VICTORY_MAIN_MENU  to "Main Menu",
        StringKey.VICTORY_PLAY_AGAIN to "Play Again",

        // Gameplay HUD transient messages
        StringKey.RUN_BOSS_DEFEATED       to "Storm Sentinel defeated!",
        StringKey.RUN_SPIRIT_EXHAUSTED    to "Spirit Exhausted...",
        StringKey.RUN_ECOSYSTEM_RESTORED  to "Eco-System Restored!",
    )

    fun get(key: StringKey): String =
        english[key] ?: error("Missing English string for key: $key")
}

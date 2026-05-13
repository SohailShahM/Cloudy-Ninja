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
    // T-119: Save-slot delete confirmation modal
    MENU_DELETE_SLOT_CONFIRM_TITLE,
    MENU_DELETE_SLOT_CONFIRM_BODY,
    MENU_DELETE_SLOT_CONFIRM_DELETE,
    MENU_DELETE_SLOT_CONFIRM_CANCEL,
    MAIN_BTN_NEW_GAME,
    MAIN_BTN_LEVEL_SELECT,
    MAIN_BTN_CLOUD_ATLAS,
    MAIN_BTN_ACHIEVEMENTS,
    MAIN_BTN_STATS,
    MAIN_BTN_SETTINGS,
    MAIN_BTN_QUIT,

    // ── Achievements screen (T-108) ──────────────────────────────────────────
    ACHIEVEMENTS_SCREEN_TITLE,
    ACHIEVEMENTS_BACK_BUTTON,
    ACHIEVEMENTS_VIEW_ALL_BUTTON,
    ACHIEVEMENT_UNLOCKED_LABEL,
    ACHIEVEMENT_LOCKED_LABEL,
    // T-146: Per-achievement unlock-timestamp line under each unlocked row.
    ACHIEVEMENT_UNLOCKED_AT,
    ACHIEVEMENT_UNLOCKED_AT_UNKNOWN,
    MENU_ACHIEVEMENTS,
    STATS_ACHIEVEMENT_COUNT,
    // T-099: Achievement progress counter on MainMenu
    MENU_ACHIEVEMENT_PROGRESS,
    MENU_ACHIEVEMENT_PROGRESS_COMPLETE,
    // T-100: Build / version label on MainMenu (bottom-right corner)
    MENU_BUILD_INFO,

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
    // T-144: Camera look-ahead toggle (in motion direction)
    SETTINGS_CAMERA_LOOK_AHEAD,
    // T-142: Speedrun timer toggle — high-precision MM:SS.mmm HUD overlay
    SETTINGS_SPEEDRUN_TIMER,

    // ── Settings: Audio ──────────────────────────────────────────────────────
    // T-105: master volume + mute toggle sit above the per-bus sliders.
    SETTINGS_MASTER,
    SETTINGS_MUTE,
    SETTINGS_MUSIC,
    SETTINGS_SFX,
    SETTINGS_UI_VOLUME,
    // T-145: Sound-test subsection below the per-bus sliders. Lets players
    // verify their volume settings without entering gameplay.
    SETTINGS_SOUND_TEST_HEADING,
    SETTINGS_TEST_UI_CLICK,
    SETTINGS_TEST_SFX_JUMP,
    SETTINGS_TEST_MUSIC_AMBIENT,

    // ── Settings: Controls ───────────────────────────────────────────────────
    SETTINGS_MOVE_LEFT,
    SETTINGS_MOVE_RIGHT,
    SETTINGS_JUMP,
    SETTINGS_ACTION,
    SETTINGS_SWAP_CHARACTER,
    SETTINGS_RESTART_LEVEL,
    // T-118: label for the rebindable master-mute hotkey row in Settings →
    // Controls. Distinct from [SETTINGS_MUTE] (which is the Audio-section
    // checkbox " Mute all" with a leading space for the checkbox layout).
    SETTINGS_MUTE_TOGGLE,
    SETTINGS_PRESS_KEY,
    SETTINGS_RESET_DEFAULTS,
    SETTINGS_CONTROLS_RESET,

    // ── Settings: Accessibility ──────────────────────────────────────────────
    SETTINGS_COLORBLIND_MODE,
    SETTINGS_REDUCED_MOTION,
    SETTINGS_HIGH_CONTRAST,
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
    // T-141: Cloud Atlas search/filter
    ATLAS_SEARCH_PLACEHOLDER,
    ATLAS_SEARCH_CLEAR,
    ATLAS_SEARCH_NO_RESULTS,

    // ── Game-over overlay ────────────────────────────────────────────────────
    GAME_OVER_TITLE,
    GAME_OVER_SUBTITLE,
    GAME_OVER_TRY_AGAIN,
    GAME_OVER_MAIN_MENU,

    // ── Death recap overlay (T-130) ──────────────────────────────────────────
    DEATH_RECAP_TITLE,
    DEATH_RECAP_RETRY,
    DEATH_RECAP_QUIT,
    DEATH_CAUSE_ENEMY,
    DEATH_CAUSE_HAZARD,
    DEATH_CAUSE_FALL,
    DEATH_CAUSE_BOSS_ATTACK,

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
    // T-140: per-character ability tooltip rows. Templates take {0} = action-key
    // name (e.g. "E") resolved from Settings.keybinds["action"] via Input.Keys.toString.
    PAUSE_ABILITY_EBO,
    PAUSE_ABILITY_LAYA,
    PAUSE_ABILITY_ZEPHYR,

    // ── Hub tutorial overlay (T-137) ─────────────────────────────────────────
    TUTORIAL_TITLE,
    TUTORIAL_HINT_MOVE,
    TUTORIAL_HINT_SWAP,
    TUTORIAL_HINT_PORTAL,
    TUTORIAL_DISMISS_HINT,

    // ── Stats screen ─────────────────────────────────────────────────────────
    STATS_TITLE,
    STATS_BACK,
    STATS_EMPTY,
    STATS_DASH,
    STATS_BEST_TIMES_HEADER,
    STATS_BEST_TIMES_EMPTY,
    STATS_ACHIEVEMENTS_MISSING,
    STATS_ACHIEVEMENTS_NONE,
    // T-135: per-level eco-token completion rows
    STATS_LEVEL_TOKENS_HEADER,
    STATS_LEVEL_TOKEN_PROGRESS,
    STATS_HIDDEN_TOKEN_PROGRESS,

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

    // ── Credits screen (T-101) ───────────────────────────────────────────────
    // Title + back button
    CREDITS_TITLE,
    CREDITS_BACK,
    // Settings entry-point
    SETTINGS_CREDITS,
    // Section headers
    CREDITS_SECTION_GAME,
    CREDITS_SECTION_CODE_ASSISTANTS,
    CREDITS_SECTION_ART,
    CREDITS_SECTION_AUDIO,
    CREDITS_SECTION_ENGINE,
    CREDITS_SECTION_CLIMATE_SOURCES,
    CREDITS_SECTION_THANKS,
    // Game section
    CREDITS_GAME_AUTHOR,
    CREDITS_GAME_ROLE,
    CREDITS_GAME_YEAR,
    // Code-assistants entries
    CREDITS_CODE_CLAUDE,
    CREDITS_CODE_COPILOT,
    CREDITS_CODE_ANTIGRAVITY,
    CREDITS_CODE_NOTEBOOKLM,
    // Art entries
    CREDITS_ART_KENNEY,
    CREDITS_ART_KENNEY_LICENSE,
    CREDITS_ART_PIXEL_LINE,
    CREDITS_ART_PIXEL_REDUX,
    CREDITS_ART_FOREST_TILESET,
    CREDITS_ART_BLUEGRASS,
    CREDITS_ART_RESEARCH_NOTE,
    // Audio entries
    CREDITS_AUDIO_PROCEDURAL,
    CREDITS_AUDIO_KENNEY_SFX,
    CREDITS_AUDIO_RESEARCH_NOTE,
    // Engine entries
    CREDITS_ENGINE_LIBGDX,
    CREDITS_ENGINE_BOX2D,
    CREDITS_ENGINE_KOTLIN,
    CREDITS_ENGINE_VISUI,
    CREDITS_ENGINE_KOTEST,
    CREDITS_ENGINE_GRADLE,
    // Climate-source entries
    CREDITS_CLIMATE_NOAA,
    CREDITS_CLIMATE_NASA_EO,
    CREDITS_CLIMATE_NASA_CLIMATE,
    CREDITS_CLIMATE_NSIDC,
    CREDITS_CLIMATE_USGS,
    CREDITS_CLIMATE_IPCC,
    CREDITS_CLIMATE_ARXIV,
    CREDITS_CLIMATE_NOTE,
    // Closing thank-you
    CREDITS_THANKS_PLAYERS,
    CREDITS_THANKS_OPEN_SOURCE,

    // ── Splash screen (T-129) ────────────────────────────────────────────────
    SPLASH_PRESS_ANY_KEY,

    // ── Compositional templates (T-091) ──────────────────────────────────────
    // Used with Strings.format(key, *args). Placeholders {0}, {1}, … map to
    // varargs by position. English word-order is encoded in the template, so
    // future locales can reorder freely.
    //
    // Main menu / Stats: save-slot summaries
    SLOT_LABEL,
    ATLAS_PCT,
    DEATHS_COUNT,
    LAST_PLAYED,
    TOTAL_DEATHS,
    LEVELS_COMPLETED,
    ECO_TOKENS_COLLECTED,
    BEST_TIME_LINE,
    ACHIEVEMENTS_UNLOCKED,
    // HUD
    COMBO_MULTIPLIER,
    SCORE_VALUE,
    SPIRIT_VALUE,
    CHAR_ABILITY,
    // Level complete / level select
    ECO_FRACTION,
    WORLD_PORTAL,
    BEST_SCORE_VALUE,
    COMPLETE_WORLD_FIRST,
    // Cloud Atlas
    ATLAS_OVERLAY_HEADER,
    ATLAS_SNAPSHOTS_DISCOVERED,
    ATLAS_DISCOVERED_BY,
    // Gameplay run-state messages
    CHARACTER_ABILITY_SWAP,
    SPIRIT_DEATH,
    // Pause
    PAUSE_HINT,
    // Death recap (T-130) — formatted body rows
    DEATH_RECAP_CAUSE,
    DEATH_RECAP_TIME,
    DEATH_RECAP_STOMPS,
    DEATH_RECAP_TOKENS,
    // Victory
    VICTORY_FINAL_SCORE,
    VICTORY_TRIAL_TIME,
    // T-122: Victory-screen best-time delta + hub portal lock label (i18n wire-up)
    VICTORY_DELTA_UNDER,
    VICTORY_DELTA_OVER,
    HUB_PORTAL_LOCKED,
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
        // T-119: Save-slot delete confirmation modal. {0} is the 1-based slot number.
        StringKey.MENU_DELETE_SLOT_CONFIRM_TITLE  to "Delete slot {0}?",
        StringKey.MENU_DELETE_SLOT_CONFIRM_BODY   to "This cannot be undone.",
        StringKey.MENU_DELETE_SLOT_CONFIRM_DELETE to "Delete",
        StringKey.MENU_DELETE_SLOT_CONFIRM_CANCEL to "Cancel",
        StringKey.MAIN_BTN_NEW_GAME       to "New Game",
        StringKey.MAIN_BTN_LEVEL_SELECT   to "Level Select",
        StringKey.MAIN_BTN_CLOUD_ATLAS    to "Cloud Atlas",
        StringKey.MAIN_BTN_ACHIEVEMENTS   to "Achievements",
        StringKey.MAIN_BTN_STATS          to "Stats",
        StringKey.MAIN_BTN_SETTINGS       to "Settings",
        StringKey.MAIN_BTN_QUIT           to "Quit",

        // Achievements screen (T-108)
        StringKey.ACHIEVEMENTS_SCREEN_TITLE     to "Achievements — Slot {0}",
        StringKey.ACHIEVEMENTS_BACK_BUTTON      to "← Back",
        StringKey.ACHIEVEMENTS_VIEW_ALL_BUTTON  to "View All →",
        StringKey.ACHIEVEMENT_UNLOCKED_LABEL    to "✓ Unlocked",
        StringKey.ACHIEVEMENT_LOCKED_LABEL      to "🔒 Locked",
        // T-146: Per-achievement unlock-timestamp line under each unlocked row.
        StringKey.ACHIEVEMENT_UNLOCKED_AT         to "Unlocked: {0}",
        StringKey.ACHIEVEMENT_UNLOCKED_AT_UNKNOWN to "Unlocked: ?",
        StringKey.MENU_ACHIEVEMENTS             to "Achievements",
        StringKey.STATS_ACHIEVEMENT_COUNT       to "Achievements: {0}/{1} unlocked",
        // T-099: Achievement progress counter on MainMenu
        StringKey.MENU_ACHIEVEMENT_PROGRESS          to "Achievements: {0}/{1} unlocked",
        StringKey.MENU_ACHIEVEMENT_PROGRESS_COMPLETE to "Achievements: All {0} unlocked!",
        // T-100: Build / version label on MainMenu (bottom-right corner)
        StringKey.MENU_BUILD_INFO                    to "v{0} · {1}",

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
        // T-144: Camera look-ahead toggle
        StringKey.SETTINGS_CAMERA_LOOK_AHEAD to " Camera look-ahead (shifts view in direction of motion)",
        // T-142: Speedrun-timer toggle label. When on, GameScreen renders a
        // high-precision MM:SS.mmm timer in the top-left of the HUD, in
        // addition to the existing best-time / level-timer elements.
        StringKey.SETTINGS_SPEEDRUN_TIMER    to " Speedrun timer (millisecond HUD overlay)",
        StringKey.SETTINGS_RES_720       to "1280 × 720  (HD)",
        StringKey.SETTINGS_RES_1080      to "1920 × 1080  (Full HD)",
        StringKey.SETTINGS_RES_1440      to "2560 × 1440  (2K / QHD)",
        StringKey.SETTINGS_RES_2160      to "3840 × 2160  (4K / UHD)",

        // Settings: Audio
        // T-105: master volume + mute toggle sit above the per-bus sliders.
        StringKey.SETTINGS_MASTER    to "Master",
        StringKey.SETTINGS_MUTE      to " Mute all",
        StringKey.SETTINGS_MUSIC     to "Music",
        StringKey.SETTINGS_SFX       to "SFX",
        StringKey.SETTINGS_UI_VOLUME to "UI",
        // T-145: Sound-test subsection — three buttons that play a single sample
        // each so the player can verify their volume sliders without entering
        // gameplay. Music button auto-stops after 3s.
        StringKey.SETTINGS_SOUND_TEST_HEADING to "Sound Test",
        StringKey.SETTINGS_TEST_UI_CLICK      to "Play UI Click",
        StringKey.SETTINGS_TEST_SFX_JUMP      to "Play SFX (jump)",
        StringKey.SETTINGS_TEST_MUSIC_AMBIENT to "Play Music (ambient_arid 3s)",

        // Settings: Controls
        StringKey.SETTINGS_MOVE_LEFT       to "Move Left",
        StringKey.SETTINGS_MOVE_RIGHT      to "Move Right",
        StringKey.SETTINGS_JUMP            to "Jump",
        StringKey.SETTINGS_ACTION          to "Action",
        StringKey.SETTINGS_SWAP_CHARACTER  to "Swap Character",
        StringKey.SETTINGS_RESTART_LEVEL    to "Restart Level (hold)",
        // T-118: row label in Settings → Controls for the master-mute hotkey.
        StringKey.SETTINGS_MUTE_TOGGLE     to "Mute",
        StringKey.SETTINGS_PRESS_KEY       to "Press a key...",
        StringKey.SETTINGS_RESET_DEFAULTS  to "Reset to Defaults",
        StringKey.SETTINGS_CONTROLS_RESET  to "Controls reset!",

        // Settings: Accessibility
        StringKey.SETTINGS_COLORBLIND_MODE        to "Color-blind mode",
        StringKey.SETTINGS_REDUCED_MOTION         to " Reduce motion (disable shake, limit particles, freeze background)",
        StringKey.SETTINGS_HIGH_CONTRAST          to " High-contrast mode (max-contrast gameplay colors)",
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
        // T-141: search/filter
        StringKey.ATLAS_SEARCH_PLACEHOLDER to "Search snapshots…",
        StringKey.ATLAS_SEARCH_CLEAR       to "Clear",
        StringKey.ATLAS_SEARCH_NO_RESULTS  to "No entries match",

        // Game-over overlay
        StringKey.GAME_OVER_TITLE      to "SPIRIT EXHAUSTED",
        StringKey.GAME_OVER_SUBTITLE   to "All spirit charges depleted",
        StringKey.GAME_OVER_TRY_AGAIN  to "Try Again",
        StringKey.GAME_OVER_MAIN_MENU  to "Main Menu",

        // Death recap overlay (T-130)
        StringKey.DEATH_RECAP_TITLE       to "YOU DIED",
        StringKey.DEATH_RECAP_RETRY       to "Retry?",
        StringKey.DEATH_RECAP_QUIT        to "Quit to menu",
        StringKey.DEATH_CAUSE_ENEMY       to "enemy contact",
        StringKey.DEATH_CAUSE_HAZARD      to "lethal hazard",
        StringKey.DEATH_CAUSE_FALL        to "fell off the world",
        StringKey.DEATH_CAUSE_BOSS_ATTACK to "boss attack",

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
        // T-140: ability summary rows. {0} = current action-key display name.
        StringKey.PAUSE_ABILITY_EBO       to "Ebo — Seed Slam ({0})",
        StringKey.PAUSE_ABILITY_LAYA      to "Laya — Wind Dash ({0})",
        StringKey.PAUSE_ABILITY_ZEPHYR    to "Zephyr — Cloud Float ({0})",

        // Hub tutorial overlay (T-137) — shown once on first Sky Sanctuary entry
        StringKey.TUTORIAL_TITLE          to "WELCOME",
        StringKey.TUTORIAL_HINT_MOVE      to "Move with A/D, Jump with SPACE",
        StringKey.TUTORIAL_HINT_SWAP      to "Swap character with Q to use water-cycle abilities",
        StringKey.TUTORIAL_HINT_PORTAL    to "Walk into a portal to enter a world",
        StringKey.TUTORIAL_DISMISS_HINT   to "[ press any key to continue ]",

        // Stats screen
        StringKey.STATS_TITLE                 to "STATS",
        StringKey.STATS_BACK                  to "Back",
        StringKey.STATS_EMPTY                 to "— Empty —",
        StringKey.STATS_DASH                  to "—",
        StringKey.STATS_BEST_TIMES_HEADER     to "Best times:",
        StringKey.STATS_BEST_TIMES_EMPTY      to "Best times: (no times recorded)",
        StringKey.STATS_ACHIEVEMENTS_MISSING  to "Achievements unlocked: —",
        StringKey.STATS_ACHIEVEMENTS_NONE     to "—",
        // T-135: per-level eco-token completion rows
        // {0}=level name, {1}=collected, {2}=total, {3}=percent
        StringKey.STATS_LEVEL_TOKENS_HEADER   to "Eco-tokens by level:",
        StringKey.STATS_LEVEL_TOKEN_PROGRESS  to "{0}: {1}/{2} tokens ({3}%)",
        // {0}=hidden collected (across campaign), {1}=hidden total
        StringKey.STATS_HIDDEN_TOKEN_PROGRESS to "Hidden: {0}/{1} found",

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

        // Credits screen (T-101)
        StringKey.CREDITS_TITLE                  to "CREDITS",
        StringKey.CREDITS_BACK                   to "Back",
        StringKey.SETTINGS_CREDITS               to "Credits",
        StringKey.CREDITS_SECTION_GAME           to "Game",
        StringKey.CREDITS_SECTION_CODE_ASSISTANTS to "Code Assistants",
        StringKey.CREDITS_SECTION_ART            to "Art",
        StringKey.CREDITS_SECTION_AUDIO          to "Audio",
        StringKey.CREDITS_SECTION_ENGINE         to "Engine & Tools",
        StringKey.CREDITS_SECTION_CLIMATE_SOURCES to "Climate Sources",
        StringKey.CREDITS_SECTION_THANKS         to "Thanks",
        // Game section body
        StringKey.CREDITS_GAME_AUTHOR            to "Sohail Shah",
        StringKey.CREDITS_GAME_ROLE              to "Design, code, art direction",
        StringKey.CREDITS_GAME_YEAR              to "2026",
        // Code-assistant credits
        StringKey.CREDITS_CODE_CLAUDE            to "Claude Code — Anthropic (claude.ai/code)",
        StringKey.CREDITS_CODE_COPILOT           to "GitHub Copilot — GitHub / Microsoft",
        StringKey.CREDITS_CODE_ANTIGRAVITY       to "Antigravity — Google (Gemini 3)",
        StringKey.CREDITS_CODE_NOTEBOOKLM        to "NotebookLM — Google (research synthesis)",
        // Art credits
        StringKey.CREDITS_ART_KENNEY             to "Kenney — Pixel Platformer pack (kenney.nl)",
        StringKey.CREDITS_ART_KENNEY_LICENSE     to "Kenney assets used under CC0 1.0 Universal",
        StringKey.CREDITS_ART_PIXEL_LINE         to "Kenney — Pixel Line Platformer (CC0)",
        StringKey.CREDITS_ART_PIXEL_REDUX        to "Kenney — Platformer Art: Pixel Redux (CC0)",
        StringKey.CREDITS_ART_FOREST_TILESET     to "Pixel Art Forest Tilesets — OpenGameArt (CC0)",
        StringKey.CREDITS_ART_BLUEGRASS          to "Bluegrass Tileset & Backgrounds — OpenGameArt (CC0)",
        StringKey.CREDITS_ART_RESEARCH_NOTE      to "Full art-research list: art-research/tileset-candidates.md",
        // Audio credits
        StringKey.CREDITS_AUDIO_PROCEDURAL       to "Procedural ambient + SFX (T-013, T-030)",
        StringKey.CREDITS_AUDIO_KENNEY_SFX       to "Kenney — UI / Digital Audio packs (CC0)",
        StringKey.CREDITS_AUDIO_RESEARCH_NOTE    to "Full audio-research list: art-research/audio-candidates.md",
        // Engine credits
        StringKey.CREDITS_ENGINE_LIBGDX          to "libGDX — Apache 2.0 (libgdx.com)",
        StringKey.CREDITS_ENGINE_BOX2D           to "Box2D — zlib (box2d.org)",
        StringKey.CREDITS_ENGINE_KOTLIN          to "Kotlin — Apache 2.0 (JetBrains)",
        StringKey.CREDITS_ENGINE_VISUI           to "VisUI — Apache 2.0 (Kotcrab)",
        StringKey.CREDITS_ENGINE_KOTEST          to "Kotest — Apache 2.0 (kotest.io)",
        StringKey.CREDITS_ENGINE_GRADLE          to "Gradle — Apache 2.0 (gradle.org)",
        // Climate-source credits
        StringKey.CREDITS_CLIMATE_NOAA           to "NOAA — National Oceanic and Atmospheric Administration",
        StringKey.CREDITS_CLIMATE_NASA_EO        to "NASA Earth Observatory",
        StringKey.CREDITS_CLIMATE_NASA_CLIMATE   to "NASA Global Climate Change (climate.nasa.gov)",
        StringKey.CREDITS_CLIMATE_NSIDC          to "NSIDC — National Snow and Ice Data Center",
        StringKey.CREDITS_CLIMATE_USGS           to "USGS — U.S. Geological Survey",
        StringKey.CREDITS_CLIMATE_IPCC           to "IPCC — Intergovernmental Panel on Climate Change",
        StringKey.CREDITS_CLIMATE_ARXIV          to "arXiv preprints — open-access peer-review",
        StringKey.CREDITS_CLIMATE_NOTE           to "Full source index: research/climate-sources/INDEX.md",
        // Closing thanks
        StringKey.CREDITS_THANKS_PLAYERS         to "Thank you for playing.",
        StringKey.CREDITS_THANKS_OPEN_SOURCE     to "Built on the shoulders of open-source giants.",

        // Splash screen (T-129)
        StringKey.SPLASH_PRESS_ANY_KEY to "Press any key to continue",

        // Compositional templates (T-091). Args are substituted by {N} index.
        StringKey.SLOT_LABEL                 to "Slot {0}",
        StringKey.ATLAS_PCT                  to "Atlas: {0}%",
        StringKey.DEATHS_COUNT               to "Deaths: {0}",
        StringKey.LAST_PLAYED                to "Last: {0}",
        StringKey.TOTAL_DEATHS               to "Total deaths: {0}",
        StringKey.LEVELS_COMPLETED           to "Levels completed: {0}",
        StringKey.ECO_TOKENS_COLLECTED       to "Eco-tokens collected: {0}",
        StringKey.BEST_TIME_LINE             to "{0}: {1}",
        StringKey.ACHIEVEMENTS_UNLOCKED      to "Achievements unlocked: {0}/{1}",
        StringKey.COMBO_MULTIPLIER           to "x{0} COMBO!",
        StringKey.SCORE_VALUE                to "Score: {0}",
        StringKey.SPIRIT_VALUE               to "Spirit: {0}",
        StringKey.CHAR_ABILITY               to "{0} — {1}",
        StringKey.ECO_FRACTION               to "{0} / {1}",
        StringKey.WORLD_PORTAL               to "{0} World {1}: {2}",
        StringKey.BEST_SCORE_VALUE           to "Best score: {0}",
        StringKey.COMPLETE_WORLD_FIRST       to "Complete World {0} first",
        StringKey.ATLAS_OVERLAY_HEADER       to "CLOUD ATLAS  •  {0}",
        StringKey.ATLAS_SNAPSHOTS_DISCOVERED to "{0} / {1} snapshots discovered",
        StringKey.ATLAS_DISCOVERED_BY        to "Discovered by: {0}",
        StringKey.CHARACTER_ABILITY_SWAP     to "{0}: {1}",
        StringKey.SPIRIT_DEATH               to "{0} fell ({1} spirits left)",
        StringKey.PAUSE_HINT                 to "Press {0} to resume",
        // Death recap (T-130) — body rows
        StringKey.DEATH_RECAP_CAUSE          to "Cause: {0}",
        StringKey.DEATH_RECAP_TIME           to "Time: {0}",
        StringKey.DEATH_RECAP_STOMPS         to "Stomps: {0}",
        StringKey.DEATH_RECAP_TOKENS         to "Eco-tokens: {0}",
        StringKey.VICTORY_FINAL_SCORE        to "Final Score: {0}",
        StringKey.VICTORY_TRIAL_TIME         to "Trial Time: {0}",
        // T-122: i18n wire-up for VictoryScreen delta + hub portal lock label
        StringKey.VICTORY_DELTA_UNDER        to "−%.2fs under best",
        StringKey.VICTORY_DELTA_OVER         to "+%.2fs slower",
        StringKey.HUB_PORTAL_LOCKED          to "[Locked]",
    )

    fun get(key: StringKey): String =
        english[key] ?: error("Missing English string for key: $key")

    /**
     * Compositional lookup: returns the template for [key] with `{N}` placeholders
     * replaced by `args[N].toString()`. Unmatched placeholders are left intact.
     *
     * Example:
     *   `format(SLOT_LABEL, 2)` → `"Slot 2"`
     *   `format(WORLD_PORTAL, "[+] ", 3, "Cloud Forest")` → `"[+]  World 3: Cloud Forest"`
     *
     * Uses a simple `{N}` regex rather than `java.text.MessageFormat` to avoid
     * `MessageFormat`'s locale-sensitive number/date quirks — every {N} resolves
     * to a plain `toString()` no matter what.
     */
    fun format(key: StringKey, vararg args: Any): String {
        val template = english[key] ?: error("Missing English string for key: $key")
        return template.replace(Regex("\\{(\\d+)\\}")) { match ->
            val idx = match.groupValues[1].toInt()
            args.getOrNull(idx)?.toString() ?: match.value
        }
    }
}

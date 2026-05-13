package com.sohai.platformer.rendering

/**
 * T-160 — Standalone `ColorRole` enum, extracted from [HighContrastPalette].
 *
 * Semantic categories used by the renderer when asking any palette layer
 * (high-contrast, color-blind, future themes) for a swatch. Each role
 * corresponds to a *visual function* in the game world (what the player needs
 * to read at a glance), not a specific source colour. Palette implementations
 * pick the most readable swatch for each role under their own assumptions
 * (e.g. WCAG-AAA high-contrast, dichromat-safe hues, etc).
 *
 * Lives as a standalone file so future systems (T-057 color-blind palette,
 * future themes) can depend on the role taxonomy without depending on
 * [HighContrastPalette]. Behaviour is unchanged from the T-132 in-class
 * definition; this is a pure file relocation.
 */
enum class ColorRole {
    /** Player sprite tint. Mapped to pure white. */
    PLAYER,

    /** Generic enemy body / silhouette. Mapped to pure black. */
    ENEMY,

    /** Standard ground / platform tile face. Mapped to light grey. */
    PLATFORM,

    /** Slightly brighter highlight on top of a [PLATFORM]. */
    PLATFORM_HIGHLIGHT,

    /** Subtle bottom shadow under a [PLATFORM]. Mapped to dark grey. */
    PLATFORM_SHADOW,

    /** Vertical wall tiles. Mapped to dark grey for high contrast. */
    WALL,

    /** Wall side-edge highlight. */
    WALL_EDGE,

    /** Moving platform face. Mapped to mid grey (distinct from terrain). */
    MOVING_PLATFORM,

    /** Moving platform top highlight. */
    MOVING_PLATFORM_HIGHLIGHT,

    /** Lethal hazard (spike base/stripe/tip). Mapped to saturated red. */
    HAZARD,

    /** Cleansed/safe hazard. Mapped to saturated green. */
    HAZARD_CLEANED,

    /** Level exit door body. Mapped to saturated yellow. */
    EXIT,

    /** Level exit door edge highlight. */
    EXIT_EDGE,

    /** Locked-portal door (hub world). Mapped to dark grey. */
    PORTAL_LOCKED,

    /** Unlocked-portal door (hub world). Mapped to saturated yellow. */
    PORTAL_UNLOCKED,

    /** Inactive checkpoint orb. Mapped to mid grey. */
    CHECKPOINT_INACTIVE,

    /** Activated checkpoint orb. Mapped to saturated green. */
    CHECKPOINT_ACTIVE,

    /** Eco-token pickup. Mapped to saturated yellow. */
    TOKEN,

    /** Cloud Atlas snapshot pickup body. Mapped to saturated cyan. */
    SNAPSHOT,

    /** Snapshot/token sparkle particle. Mapped to pure white. */
    SPARKLE,

    /** Player-fired projectile. Mapped to saturated red (matches HAZARD). */
    PROJECTILE,

    /** Ability VFX (Ebo droplets, Laya/Zephyr wind trails). Mapped to white. */
    ABILITY_VFX,

    /** Grass / vegetation tuft. Mapped to saturated green (matches CHECKPOINT_ACTIVE). */
    GRASS,

    /** Ambient particle burst (smoke, dust, footstep). Mapped to mid grey. */
    PARTICLE
}

package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color

/**
 * T-132 — High-contrast accessibility palette.
 *
 * A pure, Gdx-free wrapper that maps a gameplay-rendering colour to its
 * maximum-contrast variant according to a small enum of "roles" (what is being
 * drawn, semantically). Used by [com.sohai.platformer.screens.LevelRenderer]
 * when `Settings.highContrast` is on; bypassed otherwise.
 *
 * Design notes
 * ────────────
 *  - **No `Gdx.*` imports.** The only libGDX type touched is [Color], which is
 *    pure data. This keeps the wrapper headless-testable so the mapping table
 *    can be covered by Kotest without `Gdx.files` / GL context.
 *  - **Alpha is preserved.** When a colour is remapped, the new colour copies
 *    the *original* alpha so glows/shadows that depend on translucency still
 *    fade correctly. The hue is what swings to high-contrast, not the opacity.
 *  - **Coexists with [com.sohai.platformer.persist.ColorBlindMode]** — the
 *    caller resolves the color-blind palette first, then (when high-contrast
 *    is on) passes the result through [mapColor]. The high-contrast mapping
 *    intentionally **overrides** the color-blind one for any role both apply
 *    to: at maximum contrast, hue distinctions for dichromat support are
 *    irrelevant because everything snaps to a small, deliberately distant
 *    swatch set (pure white / pure black / saturated primaries).
 *  - **Default = identity.** [mapColor] with `enabled = false` (or with
 *    [Color.WHITE] tints that the caller doesn't want remapped) returns the
 *    input unchanged, so the OFF render path is byte-identical to pre-T-132.
 */
object HighContrastPalette {

    /**
     * Semantic categories the renderer uses when asking for a high-contrast
     * replacement. Each role corresponds to a *visual function* in the game
     * world (what the player needs to read at a glance), not a specific
     * source colour. The mapping table below picks the most readable swatch
     * for each role under WCAG-AAA-style high-contrast assumptions.
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

    // ── Mapping table (immutable, single-instance Color allocations) ─────
    //
    // Each constant is allocated once at class-init time; the table never
    // mutates and never allocates per-frame. Callers that need a different
    // alpha must compose with [withAlpha] (also non-allocating: returns a
    // fresh Color only on actual swap).
    private val WHITE         = Color(1f,    1f,    1f,    1f)
    private val BLACK         = Color(0f,    0f,    0f,    1f)
    private val LIGHT_GREY    = Color(0.85f, 0.85f, 0.85f, 1f)
    private val MID_GREY      = Color(0.55f, 0.55f, 0.55f, 1f)
    private val DARK_GREY     = Color(0.18f, 0.18f, 0.18f, 1f)
    private val SAT_RED       = Color(1f,    0f,    0f,    1f)
    private val SAT_GREEN     = Color(0f,    1f,    0f,    1f)
    private val SAT_BLUE      = Color(0.05f, 0.40f, 1f,    1f)
    private val SAT_YELLOW    = Color(1f,    0.95f, 0f,    1f)
    private val SAT_CYAN      = Color(0f,    1f,    1f,    1f)

    /**
     * Resolve the high-contrast colour for [role], preserving the input's
     * alpha (so glows / translucent halos keep their opacity envelope).
     * When [enabled] is false this is the identity — returns [input] verbatim.
     *
     * The returned [Color] is either [input] itself (identity case) or a
     * fresh allocation that copies the role's swatch with `input.a`.
     * Hot-path callers should cache the result per role / per frame if they
     * draw thousands of primitives.
     */
    fun mapColor(input: Color, role: ColorRole, enabled: Boolean = true): Color {
        if (!enabled) return input
        val swatch = swatchFor(role)
        // Preserve the input's alpha exactly. If alpha is already 1f and the
        // swatch is opaque the result equals the swatch — but we still copy
        // so the caller can never mutate our cached table entry.
        return Color(swatch.r, swatch.g, swatch.b, input.a)
    }

    /**
     * Internal lookup. Exposed `internal` so the test suite can assert the
     * mapping table without invoking [mapColor]'s alpha-preservation logic.
     */
    internal fun swatchFor(role: ColorRole): Color = when (role) {
        ColorRole.PLAYER                    -> WHITE
        ColorRole.ENEMY                     -> BLACK
        ColorRole.PLATFORM                  -> LIGHT_GREY
        ColorRole.PLATFORM_HIGHLIGHT        -> WHITE
        ColorRole.PLATFORM_SHADOW           -> DARK_GREY
        ColorRole.WALL                      -> DARK_GREY
        ColorRole.WALL_EDGE                 -> MID_GREY
        ColorRole.MOVING_PLATFORM           -> MID_GREY
        ColorRole.MOVING_PLATFORM_HIGHLIGHT -> LIGHT_GREY
        ColorRole.HAZARD                    -> SAT_RED
        ColorRole.HAZARD_CLEANED            -> SAT_GREEN
        ColorRole.EXIT                      -> SAT_YELLOW
        ColorRole.EXIT_EDGE                 -> WHITE
        ColorRole.PORTAL_LOCKED             -> DARK_GREY
        ColorRole.PORTAL_UNLOCKED           -> SAT_YELLOW
        ColorRole.CHECKPOINT_INACTIVE       -> MID_GREY
        ColorRole.CHECKPOINT_ACTIVE         -> SAT_GREEN
        ColorRole.TOKEN                     -> SAT_YELLOW
        ColorRole.SNAPSHOT                  -> SAT_CYAN
        ColorRole.SPARKLE                   -> WHITE
        ColorRole.PROJECTILE                -> SAT_RED
        ColorRole.ABILITY_VFX               -> WHITE
        ColorRole.GRASS                     -> SAT_GREEN
        ColorRole.PARTICLE                  -> MID_GREY
    }
}

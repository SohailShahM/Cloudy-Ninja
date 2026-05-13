package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.sohai.platformer.Constants
import com.sohai.platformer.abilities.EboAbility
import com.sohai.platformer.abilities.LayaAbility
import com.sohai.platformer.abilities.ZephyrAbility
import com.sohai.platformer.entities.DriftHusk
import com.sohai.platformer.entities.EcoToken
import com.sohai.platformer.entities.Enemy
import com.sohai.platformer.entities.MovingPlatform
import com.sohai.platformer.entities.PlayerController
import com.sohai.platformer.entities.Projectile
import com.sohai.platformer.entities.SnapshotPickup
import com.sohai.platformer.entities.StormSentinel
import com.sohai.platformer.levels.Level0_0
import com.sohai.platformer.persist.ColorBlindMode
import com.sohai.platformer.persist.SaveManager
import com.sohai.platformer.persist.SettingsManager
import com.sohai.platformer.rendering.CharacterAnimator
import com.sohai.platformer.FontManager
import com.sohai.platformer.rendering.CameraLookAhead
import com.sohai.platformer.rendering.HighContrastPalette
import com.sohai.platformer.rendering.HighContrastPalette.ColorRole
import com.sohai.platformer.rendering.ParallaxBackground
import com.sohai.platformer.rendering.ParallaxTheme
import com.sohai.platformer.rendering.ParticleSystem
import com.sohai.platformer.rendering.ScreenShake
import com.sohai.platformer.rendering.SpriteFactory
import com.sohai.platformer.rendering.TileRenderer
import com.sohai.platformer.rendering.TilesetRegistry
import com.sohai.platformer.world.ObstacleKind
import com.sohai.platformer.world.ObstacleManager

/**
 * Owns all rendering for an active level: parallax background, obstacle geometry,
 * ability VFX, sprites, particles. Also exposes spawn helpers for game-event particles
 * so callers never touch [ParticleSystem] directly.
 */
class LevelRenderer(
    private val shapeRenderer: ShapeRenderer,
    private val spriteBatch: SpriteBatch,
    private val camera: OrthographicCamera,
    private val parallaxBg: ParallaxBackground,
    private val particles: ParticleSystem,
    private val eboAbility: EboAbility,
    private val layaAbility: LayaAbility,
    private val zephyrAbility: ZephyrAbility,
    private val obstacleManager: ObstacleManager,
    private val movingPlatforms: List<MovingPlatform>,
    private val ecoTokens: List<EcoToken>,
    private val snapshotPickups: List<SnapshotPickup>,
    private val enemies: List<Enemy>,
    private val player: PlayerController,
    private val eboAnimator: CharacterAnimator,
    private val layaAnimator: CharacterAnimator,
    private val footstepColor: Color,
    private val sentinel: StormSentinel? = null,
    private val tileRenderer: TileRenderer? = null,
    private val parallaxTheme: ParallaxTheme = ParallaxTheme.ARID,
    /** T-062: Drift Husk enemies, drawn after [enemies] so their trail wisps overlay terrain. */
    private val driftHusks: List<DriftHusk> = emptyList(),
    /**
     * T-144: total level width in virtual pixels, used to clamp the
     * look-ahead offset against the right edge so we never reveal
     * out-of-bounds space. `0f` (the default) disables clamping — useful
     * for tests that don't construct a Level.
     */
    private val levelWidthPx: Float = 0f
) {

    // ── Hot-path colour constants (hoisted to avoid per-frame allocation) ─────
    //
    // The companion below holds the DEFAULT (OFF) palette. Colors that need to
    // shift for color-blind accessibility are also exposed on [Palette] (a small
    // value-holder), and [Palette.forMode] returns a per-mode override set.
    //
    // Color choices for non-OFF modes are based on Brettel/Viénot/Mollon (1997)
    // dichromat simulation models and the IBM design library's color-blind safe
    // recommendations: red/green pairs become blue/orange under deuteranopia &
    // protanopia (both red-green deficiencies), and blue/yellow pairs become
    // teal/magenta under tritanopia. Luminance contrast is preserved so the
    // game still reads at a glance.
    private class Palette(
        val HAZARD_BASE: Color,
        val HAZARD_STRIPE: Color,
        val HAZARD_SPIKE: Color,
        val HAZARD_CLEAN_BASE: Color,
        val HAZARD_CLEAN_GLEAM: Color,
        val EXIT_BASE: Color,
        val EXIT_EDGE: Color,
        val CP_GLOW_INACTIVE: Color,
        val CP_GLOW_ACTIVE: Color,
        val CP_BODY_INACTIVE: Color,
        val CP_BODY_ACTIVE: Color,
        val SNAPSHOT_GLOW: Color,
        val SNAPSHOT_BODY: Color,
        val TOKEN: Color,
        val SPARKLE_TOKEN: Color,
        val SPARKLE_SNAPSHOT: Color,
        val GRASS_TUFT: Color,
        val PORTAL_UNLOCKED: Color,
        val PORTAL_UNLOCKED_EDGE: Color
    ) {
        companion object {
            /** OFF palette — the original colors verbatim. */
            val DEFAULT = Palette(
                HAZARD_BASE         = Color(0.75f, 0.15f, 0.15f, 1f),
                HAZARD_STRIPE       = Color(0.55f, 0.08f, 0.08f, 1f),
                HAZARD_SPIKE        = Color(0.95f, 0.10f, 0.10f, 1f),
                HAZARD_CLEAN_BASE   = Color(0.25f, 0.65f, 0.3f,  1f),
                HAZARD_CLEAN_GLEAM  = Color(0.5f,  0.95f, 0.55f, 0.8f),
                EXIT_BASE           = Color(0.15f, 0.9f,  0.55f, 0.45f),
                EXIT_EDGE           = Color(0.3f,  1f,    0.65f, 0.85f),
                CP_GLOW_INACTIVE    = Color(0.15f, 0.15f, 0.65f, 0.3f),
                CP_GLOW_ACTIVE      = Color(0.1f,  0.6f,  0.15f, 0.3f),
                CP_BODY_INACTIVE    = Color(0.2f,  0.2f,  0.9f,  1f),
                CP_BODY_ACTIVE      = Color(0.25f, 0.85f, 0.3f,  1f),
                SNAPSHOT_GLOW       = Color(0.1f,  0.8f,  0.9f,  0.35f),
                SNAPSHOT_BODY       = Color(0.15f, 0.85f, 0.95f, 1f),
                TOKEN               = Color(0.2f,  0.9f,  0.3f,  1f),
                SPARKLE_TOKEN       = Color(0.3f,  1f,    0.9f,  1f),
                SPARKLE_SNAPSHOT    = Color(1f,    0.9f,  0.2f,  1f),
                GRASS_TUFT          = Color(0.22f, 0.72f, 0.16f, 1f),
                PORTAL_UNLOCKED     = Color(0.2f,  0.45f, 0.95f, 0.85f),
                PORTAL_UNLOCKED_EDGE = Color(0.4f, 0.65f, 1f,    1f)
            )

            /**
             * Deuteranopia / protanopia (red-green): swap reds → high-luminance
             * orange (#FF8800), greens → blue (#0088FF), cleansed-hazard green →
             * blue-cyan (#00BBFF). Eco-token (was green) becomes orange to keep
             * it distinct from blue checkpoints. Yellow snapshot sparkle moves
             * to white so it never fights the orange/blue split.
             */
            private val RED_GREEN = Palette(
                HAZARD_BASE         = Color(1.00f, 0.53f, 0.00f, 1f),   // #FF8800
                HAZARD_STRIPE       = Color(0.70f, 0.30f, 0.00f, 1f),
                HAZARD_SPIKE        = Color(1.00f, 0.60f, 0.10f, 1f),
                HAZARD_CLEAN_BASE   = Color(0.00f, 0.73f, 1.00f, 1f),   // #00BBFF
                HAZARD_CLEAN_GLEAM  = Color(0.40f, 0.90f, 1.00f, 0.8f),
                EXIT_BASE           = Color(0.00f, 0.53f, 1.00f, 0.45f),// #0088FF
                EXIT_EDGE           = Color(0.40f, 0.75f, 1.00f, 0.85f),
                CP_GLOW_INACTIVE    = Color(0.10f, 0.10f, 0.50f, 0.3f), // deep blue
                CP_GLOW_ACTIVE      = Color(1.00f, 0.55f, 0.00f, 0.3f), // orange
                CP_BODY_INACTIVE    = Color(0.20f, 0.30f, 0.95f, 1f),
                CP_BODY_ACTIVE      = Color(1.00f, 0.65f, 0.10f, 1f),
                SNAPSHOT_GLOW       = Color(0.95f, 0.95f, 0.95f, 0.35f),
                SNAPSHOT_BODY       = Color(1.00f, 1.00f, 1.00f, 1f),
                TOKEN               = Color(1.00f, 0.60f, 0.00f, 1f),   // orange (distinct from blue)
                SPARKLE_TOKEN       = Color(1.00f, 0.80f, 0.30f, 1f),
                SPARKLE_SNAPSHOT    = Color(0.95f, 0.95f, 1.00f, 1f),
                GRASS_TUFT          = Color(0.55f, 0.55f, 0.55f, 1f),   // neutral grey — no red-green cue
                PORTAL_UNLOCKED     = Color(0.00f, 0.53f, 1.00f, 0.85f),
                PORTAL_UNLOCKED_EDGE = Color(0.40f, 0.75f, 1.00f, 1f)
            )

            /**
             * Tritanopia (blue-yellow): shift blues → magenta/red, yellows → cyan.
             * Hazards stay red (no blue-yellow component) and eco-tokens move to
             * magenta to remain distinguishable from the red hazard.
             */
            private val BLUE_YELLOW = Palette(
                HAZARD_BASE         = Color(0.85f, 0.15f, 0.15f, 1f),
                HAZARD_STRIPE       = Color(0.60f, 0.08f, 0.08f, 1f),
                HAZARD_SPIKE        = Color(1.00f, 0.20f, 0.20f, 1f),
                HAZARD_CLEAN_BASE   = Color(0.10f, 0.75f, 0.80f, 1f),   // teal
                HAZARD_CLEAN_GLEAM  = Color(0.40f, 0.95f, 1.00f, 0.8f),
                EXIT_BASE           = Color(0.95f, 0.30f, 0.80f, 0.45f),// magenta
                EXIT_EDGE           = Color(1.00f, 0.55f, 0.90f, 0.85f),
                CP_GLOW_INACTIVE    = Color(0.50f, 0.10f, 0.50f, 0.3f),
                CP_GLOW_ACTIVE      = Color(0.10f, 0.65f, 0.70f, 0.3f),
                CP_BODY_INACTIVE    = Color(0.85f, 0.20f, 0.75f, 1f),   // magenta
                CP_BODY_ACTIVE      = Color(0.15f, 0.80f, 0.85f, 1f),   // cyan
                SNAPSHOT_GLOW       = Color(0.10f, 0.75f, 0.80f, 0.35f),
                SNAPSHOT_BODY       = Color(0.20f, 0.85f, 0.90f, 1f),
                TOKEN               = Color(0.95f, 0.30f, 0.80f, 1f),   // magenta
                SPARKLE_TOKEN       = Color(1.00f, 0.60f, 0.95f, 1f),
                SPARKLE_SNAPSHOT    = Color(0.95f, 0.95f, 0.95f, 1f),   // white (yellow disappears)
                GRASS_TUFT          = Color(0.22f, 0.72f, 0.16f, 1f),   // green is fine
                PORTAL_UNLOCKED     = Color(0.95f, 0.30f, 0.80f, 0.85f),
                PORTAL_UNLOCKED_EDGE = Color(1.00f, 0.55f, 0.90f, 1f)
            )

            /** Resolve the active palette for [mode]. OFF returns [DEFAULT] (no allocation). */
            fun forMode(mode: ColorBlindMode): Palette = when (mode) {
                ColorBlindMode.OFF          -> DEFAULT
                ColorBlindMode.DEUTERANOPIA -> RED_GREEN
                ColorBlindMode.PROTANOPIA   -> RED_GREEN
                ColorBlindMode.TRITANOPIA   -> BLUE_YELLOW
            }
        }
    }

    // Non-mode-sensitive colours (terrain, particles, walls, etc.) — these stay
    // identical across all modes and don't need a swap table.
    private companion object SharedPalette {
        val DROPLET             = Color(0.3f, 0.6f, 1f, 0.7f)
        val WALL_BASE           = Color(0.20f, 0.20f, 0.22f, 1f)
        val WALL_EDGE           = Color(0.35f, 0.35f, 0.38f, 1f)
        val GROUND_BASE         = Color(0.40f, 0.42f, 0.45f, 1f)
        val GROUND_TOP          = Color(0.62f, 0.65f, 0.68f, 1f)
        val MP_BASE             = Color(0.50f, 0.33f, 0.14f, 1f)
        val MP_TOP              = Color(0.75f, 0.55f, 0.30f, 1f)
        val SMOKE_STOMP         = Color(0.45f, 0.42f, 0.40f, 0.85f)
        val PROJECTILE          = Color(1f,    0.6f,  0f,    1f)
        val PORTAL_LOCKED       = Color(0.35f, 0.35f, 0.38f, 0.65f)
        val PORTAL_LOCKED_EDGE  = Color(0.50f, 0.50f, 0.52f, 0.8f)
        val tmpWindCol          = Color(1f,    1f,    1f,    1f)
        val GROUND_SHADOW       = Color(0.28f, 0.29f, 0.32f, 1f)
        val MP_SHADOW           = Color(0.30f, 0.18f, 0.06f, 1f)
        // T-107: hidden ("golden") eco-token tint. Body is the spec-suggested
        // RGBA(1.0, 0.85, 0.3, 1.0); glow halo is a softer/translucent version
        // of the same hue so the token reads as "special" without needing new
        // art assets. Same shape as a normal token; only colour differs.
        val HIDDEN_TOKEN_BODY   = Color(1.00f, 0.85f, 0.30f, 1f)
        val HIDDEN_TOKEN_GLOW   = Color(1.00f, 0.88f, 0.40f, 0.45f)
    }

    /** Active mode-sensitive palette; resolved from SettingsManager at render time. */
    private var palette: Palette = Palette.forMode(SettingsManager.load().colorBlindMode)
    private var paletteMode: ColorBlindMode = SettingsManager.load().colorBlindMode

    /**
     * T-132: cached high-contrast flag. Refreshed once per [renderWorld] /
     * [renderPlayer] frame via [refreshPalette]. When false, [hc] is the
     * identity and rendering is byte-identical to pre-T-132.
     */
    private var highContrast: Boolean = SettingsManager.load().highContrast

    // Reusable scratch Color for [hc] so the high-contrast path doesn't
    // allocate per primitive. Safe because ShapeRenderer.setColor() copies
    // the value (does not retain the reference). Same lifecycle pattern as
    // [tmpWindCol].
    private val hcScratch: Color = Color()

    /**
     * Alpha multiplier applied to the player sprite during the T-097 death
     * animation. Written by [LevelRunState] each frame while the player is
     * dying; restored to 1f on respawn. Outside the death animation this is
     * always 1f, so default rendering is byte-identical to pre-T-097 behaviour.
     */
    var playerAlpha: Float = 1f

    /**
     * T-144: camera look-ahead state. One instance per LevelRenderer (per
     * active level) so the bias smoothly recenters on respawn / portal swap.
     * The offset is applied alongside the T-116 [ScreenShake] offset before
     * the projection matrix is built, and reverted afterwards so
     * [camera.position] never drifts across frames.
     */
    private val cameraLookAhead = CameraLookAhead()

    private fun refreshPalette() {
        val s = SettingsManager.load()
        val m = s.colorBlindMode
        if (m != paletteMode) {
            paletteMode = m
            palette = Palette.forMode(m)
        }
        highContrast = s.highContrast
    }

    /**
     * T-132: high-contrast colour interceptor. When [highContrast] is on,
     * remaps [c] to the swatch for [role] via [HighContrastPalette], reusing
     * [hcScratch] to avoid per-primitive allocation. When off, returns [c]
     * unchanged so the render path is byte-identical to pre-T-132.
     *
     * Alpha is preserved from the input so glow/halo translucency still
     * fades correctly under high contrast.
     */
    private fun hc(c: Color, role: ColorRole): Color {
        if (!highContrast) return c
        val swatch = HighContrastPalette.swatchFor(role)
        return hcScratch.set(swatch.r, swatch.g, swatch.b, c.a)
    }

    /**
     * Draws everything that lives in world-space: parallax, obstacles, abilities,
     * tokens, snapshots, platforms, particles. Opens and closes one ShapeRenderer
     * Filled block. Caller must NOT have an open SR block before calling this.
     */
    fun renderWorld(cleanseRatio: Float, currentCharacter: String, projectiles: List<Projectile> = emptyList()) {
        refreshPalette()

        // T-116: advance the screen-shake clock and offset the camera in-place
        // before computing the projection matrix. The offset is reverted at the
        // end of this method so [camera.position] is byte-identical to its
        // pre-render value (no drift over repeated frames; game logic that
        // reads camera.position sees an unaffected world).
        ScreenShake.update(Gdx.graphics.deltaTime)
        val shakeOffset   = ScreenShake.offset()
        val shakeOffsetX  = shakeOffset.x
        val shakeOffsetY  = shakeOffset.y

        // T-144: lerp the look-ahead bias toward the player's horizontal
        // direction of motion. Velocity is read from the player Body so the
        // offset honours any modifier that adjusts player speed (assist
        // slow-speed, etc.) — the look-ahead naturally relaxes when the
        // player slows down. Setting gate lives inside [CameraLookAhead].
        cameraLookAhead.update(player.body.linearVelocity.x)
        // CameraLookAhead.offsetPx() is in virtual pixels; the camera is in
        // world meters, so divide by PPM before applying. Clamp the total
        // (look-ahead + shake) so we never reveal past the right edge of
        // the level.
        val lookAheadOffsetX = cameraLookAhead.offsetPx() / Constants.PPM
        val totalOffsetX     = clampHorizontalOffset(lookAheadOffsetX + shakeOffsetX)
        val totalOffsetY     = shakeOffsetY
        val hasOffset        = totalOffsetX != 0f || totalOffsetY != 0f
        if (hasOffset) {
            camera.position.x += totalOffsetX
            camera.position.y += totalOffsetY
            camera.update()
        }

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Layer 0: parallax sky + terrain silhouettes
        parallaxBg.render(shapeRenderer, camera, cleanseRatio)

        // ── Ability VFX ────────────────────────────────────────────────────────
        shapeRenderer.color = hc(DROPLET, ColorRole.ABILITY_VFX)
        for (droplet in eboAbility.getActiveRaindrops()) {
            val pos = droplet.body.position
            shapeRenderer.circle(pos.x, pos.y, droplet.getRadius())
        }
        for (trail in layaAbility.getActiveWindTrails()) {
            val pos = trail.getCurrentPosition()
            tmpWindCol.set(1f, 1f, 1f, 0.6f * trail.getAlpha())
            shapeRenderer.color = hc(tmpWindCol, ColorRole.ABILITY_VFX)
            shapeRenderer.circle(pos.x / Constants.PPM, pos.y / Constants.PPM, trail.getRadius())
        }
        for (trail in zephyrAbility.getActiveWindTrails()) {
            val pos = trail.getCurrentPosition()
            tmpWindCol.set(0.75f, 0.55f, 1f, 0.65f * trail.getAlpha())
            shapeRenderer.color = hc(tmpWindCol, ColorRole.ABILITY_VFX)
            shapeRenderer.circle(pos.x / Constants.PPM, pos.y / Constants.PPM, trail.getRadius())
        }

        // ── Tile-rendered obstacles (batched before ShapeRenderer obstacles) ──
        // Render tiled obstacles in a single SpriteBatch pass.  We end the
        // ShapeRenderer, draw tiles, then resume.  This avoids per-tile begin/end
        // churn while keeping z-order consistent (tiles draw over the parallax
        // background, under abilities and particles).
        val rects = obstacleManager.rects()
        if (tileRenderer != null) {
            // Collect rects that will be tile-rendered so the SR loop can skip them.
            shapeRenderer.end()
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            for (rect in rects) {
                tileRenderer.renderObstacle(rect, parallaxTheme)
                // renderObstacle returns true/false but we still fall through to the
                // ShapeRenderer loop below for unmapped kinds (CHECKPOINT, EXIT, etc.)
            }
            Gdx.gl.glDisable(GL20.GL_BLEND)
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        }

        // ── Obstacles (ShapeRenderer fallback / CHECKPOINT / EXIT / hazard_cleaned)
        // When tileRenderer is active it has already drawn GROUND, WALL, and
        // uncleaned HAZARD tiles — skip those in the ShapeRenderer pass.
        for (rect in rects) {
            val ud = rect.fixture.userData as? String ?: ""
            val cx = rect.body.position.x
            val cy = rect.body.position.y
            val w  = rect.halfWidthPx  / Constants.PPM
            val he = rect.halfHeightPx / Constants.PPM

            // Skip tiled kinds (already drawn above) except cleaned hazards which
            // have a special green ShapeRenderer overlay that tiles don't cover.
            if (tileRenderer != null) {
                val hasTileMapping = TilesetRegistry.current.tileMap.containsKey(rect.kind to parallaxTheme)
                val isTiledHazard  = rect.kind == ObstacleKind.HAZARD && ud != "hazard_cleaned"
                if (hasTileMapping && (rect.kind != ObstacleKind.HAZARD || isTiledHazard)) continue
            }

            when {
                rect.kind == ObstacleKind.HAZARD && ud == "hazard_cleaned" -> {
                    shapeRenderer.color = hc(palette.HAZARD_CLEAN_BASE, ColorRole.HAZARD_CLEANED)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = hc(palette.HAZARD_CLEAN_GLEAM, ColorRole.HAZARD_CLEANED)
                    shapeRenderer.rect(cx - w, cy + he - 0.04f, w * 2f, 0.04f)
                }
                rect.kind == ObstacleKind.HAZARD -> {
                    shapeRenderer.color = hc(palette.HAZARD_BASE, ColorRole.HAZARD)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = hc(palette.HAZARD_STRIPE, ColorRole.HAZARD)
                    var sx = cx - w
                    while (sx < cx + w) {
                        shapeRenderer.rect(sx, cy - he, 0.05f, he * 2f)
                        sx += 0.2f
                    }
                    // Triangular spike shapes along the top edge
                    shapeRenderer.color = hc(palette.HAZARD_SPIKE, ColorRole.HAZARD)
                    var spx = cx - w + 0.04f
                    while (spx < cx + w - 0.04f) {
                        shapeRenderer.triangle(spx - 0.04f, cy + he, spx + 0.04f, cy + he, spx, cy + he + 0.12f)
                        spx += 0.13f
                    }
                }
                rect.kind == ObstacleKind.WALL -> {
                    shapeRenderer.color = hc(WALL_BASE, ColorRole.WALL)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = hc(WALL_EDGE, ColorRole.WALL_EDGE)
                    shapeRenderer.rect(cx - w, cy - he, 0.03f, he * 2f)
                }
                rect.kind == ObstacleKind.EXIT && ud.startsWith("portal_") -> {
                    // Hub-world portal door: blue if unlocked, grey if locked
                    val required  = Level0_0.portalUnlockRequirement(ud)
                    val completed = SaveManager.loadGame().completedLevels
                    val unlocked  = required.all { it in completed }
                    val baseCol   = if (unlocked) palette.PORTAL_UNLOCKED else PORTAL_LOCKED
                    val edgeCol   = if (unlocked) palette.PORTAL_UNLOCKED_EDGE else PORTAL_LOCKED_EDGE
                    val portalRole     = if (unlocked) ColorRole.PORTAL_UNLOCKED else ColorRole.PORTAL_LOCKED
                    shapeRenderer.color = hc(baseCol, portalRole)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = hc(edgeCol, portalRole)
                    shapeRenderer.rect(cx - w, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx + w - 0.05f, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx - w, cy + he - 0.04f, w * 2f, 0.04f)
                }
                rect.kind == ObstacleKind.EXIT -> {
                    shapeRenderer.color = hc(palette.EXIT_BASE, ColorRole.EXIT)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = hc(palette.EXIT_EDGE, ColorRole.EXIT_EDGE)
                    shapeRenderer.rect(cx - w, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx + w - 0.05f, cy - he, 0.05f, he * 2f)
                }
                else -> {
                    shapeRenderer.color = hc(GROUND_BASE, ColorRole.PLATFORM)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = hc(GROUND_TOP, ColorRole.PLATFORM_HIGHLIGHT)
                    shapeRenderer.rect(cx - w, cy + he - 0.05f, w * 2f, 0.05f)
                    // Grass tufts along the top surface — height varies with position
                    shapeRenderer.color = hc(palette.GRASS_TUFT, ColorRole.GRASS)
                    var gx = cx - w + 0.04f
                    while (gx < cx + w - 0.04f) {
                        val bh = 0.062f + MathUtils.sin(gx * 19.1f + cx * 4.3f) * 0.024f
                        shapeRenderer.triangle(gx - 0.022f, cy + he, gx + 0.022f, cy + he, gx, cy + he + bh)
                        gx += 0.10f
                    }
                    // Bottom shadow strip for a subtle 3-D depth impression
                    shapeRenderer.color = hc(GROUND_SHADOW, ColorRole.PLATFORM_SHADOW)
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, 0.04f)
                }
            }
        }

        // Moving platforms
        for (mp in movingPlatforms) {
            val pos = mp.body.position
            val hw = 50f / Constants.PPM
            val hh = 10f / Constants.PPM
            shapeRenderer.color = hc(MP_BASE, ColorRole.MOVING_PLATFORM)
            shapeRenderer.rect(pos.x - hw, pos.y - hh, hw * 2f, hh * 2f)
            shapeRenderer.color = hc(MP_TOP, ColorRole.MOVING_PLATFORM_HIGHLIGHT)
            shapeRenderer.rect(pos.x - hw, pos.y + hh - 0.04f, hw * 2f, 0.04f)
            // Underside shadow for depth
            shapeRenderer.color = hc(MP_SHADOW, ColorRole.PLATFORM_SHADOW)
            shapeRenderer.rect(pos.x - hw, pos.y - hh, hw * 2f, 0.025f)
        }

        // Checkpoints
        for (cp in obstacleManager.checkpoints()) {
            val activated = cp.fixture.userData as? String == "checkpoint_activated"
            val r = cp.radiusPx / Constants.PPM
            val glowCol = if (activated) palette.CP_GLOW_ACTIVE else palette.CP_GLOW_INACTIVE
            val bodyCol = if (activated) palette.CP_BODY_ACTIVE else palette.CP_BODY_INACTIVE
            val cpRole  = if (activated) ColorRole.CHECKPOINT_ACTIVE else ColorRole.CHECKPOINT_INACTIVE
            shapeRenderer.color = hc(glowCol, cpRole)
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r * 1.5f)
            shapeRenderer.color = hc(bodyCol, cpRole)
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r)
            shapeRenderer.color = Color.WHITE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r * 0.35f)
        }

        // Eco-tokens — regular = palette.TOKEN; hidden = golden tint (T-107).
        // We draw hidden tokens with a slightly larger glow halo so they're
        // visually distinct on top of the colour shift (the player who finds
        // them gets unambiguous feedback that this one was "special").
        for (token in ecoTokens) {
            if (token.isCollected) continue
            val p = token.body.position
            val r = token.getAnimatedRadius()
            if (token.isHidden) {
                // Golden tint, ~RGBA(1.0, 0.85, 0.3, 1.0) per spec
                shapeRenderer.color = hc(HIDDEN_TOKEN_GLOW, ColorRole.TOKEN)
                shapeRenderer.circle(p.x, p.y, r * 1.5f)
                shapeRenderer.color = hc(HIDDEN_TOKEN_BODY, ColorRole.TOKEN)
                shapeRenderer.circle(p.x, p.y, r)
            } else {
                shapeRenderer.color = hc(palette.TOKEN, ColorRole.TOKEN)
                shapeRenderer.circle(p.x, p.y, r)
            }
        }

        // Cloud Atlas snapshot stars
        for (snap in snapshotPickups) {
            if (!snap.isCollected) {
                val p  = snap.body.position
                val r  = snap.getAnimatedRadius()
                val ri = r * 0.45f
                shapeRenderer.color = hc(palette.SNAPSHOT_GLOW, ColorRole.SNAPSHOT)
                shapeRenderer.circle(p.x, p.y, r * 1.4f)
                shapeRenderer.color = hc(palette.SNAPSHOT_BODY, ColorRole.SNAPSHOT)
                shapeRenderer.triangle(p.x, p.y + r,  p.x + ri, p.y,      p.x, p.y - r)
                shapeRenderer.triangle(p.x, p.y + r,  p.x - ri, p.y,      p.x, p.y - r)
                shapeRenderer.triangle(p.x - r, p.y,  p.x, p.y + ri,  p.x + r, p.y)
                shapeRenderer.triangle(p.x - r, p.y,  p.x, p.y - ri,  p.x + r, p.y)
            }
        }

        // Projectiles
        shapeRenderer.color = hc(PROJECTILE, ColorRole.PROJECTILE)
        for (proj in projectiles) {
            val pos = proj.body.position
            shapeRenderer.circle(pos.x, pos.y, Projectile.RADIUS)
        }

        // Enemies (T-098: each enemy's draw() lerps its body colour toward white
        // by clamp(enemy.hitFlashTimer / Enemy.HIT_FLASH_SECONDS) for the brief
        // post-hit feedback flash. Outside the flash window the colour is
        // byte-identical to its pre-T-098 value.)
        for (enemy in enemies) {
            enemy.draw(shapeRenderer)
            // T-132: cover the enemy with a pure-black silhouette when
            // high-contrast is on. Enemies set their own colours inside
            // [Enemy.draw], so we paint over the result here rather than
            // editing every entity class. Size is a small generous box that
            // covers SmogSprite (0.15×0.12 m) and similar small patrollers.
            if (highContrast && !enemy.isDead) {
                shapeRenderer.color = hc(Color.BLACK, ColorRole.ENEMY)
                val ep = enemy.body.position
                shapeRenderer.rect(ep.x - 0.18f, ep.y - 0.16f, 0.36f, 0.32f)
            }
        }

        // T-062: Drift Husks (drop-from-above) -- draw() handles its own
        // visibility (skips while in COOLDOWN state). T-098 hit-flash applies
        // here too via the same Enemy.applyHitFlash helper.
        for (husk in driftHusks) {
            husk.draw(shapeRenderer)
            // T-132: same enemy-silhouette overlay (DriftHusk is 0.16×0.14 m,
            // and it skips its own draw while in COOLDOWN — we mirror that
            // by checking isDead, which covers the practical case).
            if (highContrast && !husk.isDead) {
                shapeRenderer.color = hc(Color.BLACK, ColorRole.ENEMY)
                val hp = husk.body.position
                shapeRenderer.rect(hp.x - 0.18f, hp.y - 0.16f, 0.36f, 0.32f)
            }
        }

        // Boss sentinel (drawn after enemies so telegraph rings appear on top)
        sentinel?.draw(shapeRenderer)
        // T-132: boss-sentinel silhouette overlay. BODY_RADIUS is 0.45 m, so
        // a 0.5 m circle covers the body cleanly while leaving the telegraph
        // rings (drawn outside the body radius) visible.
        if (highContrast && sentinel != null) {
            val sp = sentinel.body.position
            shapeRenderer.color = hc(Color.BLACK, ColorRole.ENEMY)
            shapeRenderer.circle(sp.x, sp.y, 0.5f)
        }

        // Particles (alpha blend enabled; works inside the Filled block via GL blend state)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        particles.render(shapeRenderer)

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // T-116 + T-144: revert the combined (look-ahead + shake) offset so
        // camera.position never drifts across frames. Pair with the apply at
        // the top of renderWorld.
        if (hasOffset) {
            camera.position.x -= totalOffsetX
            camera.position.y -= totalOffsetY
            camera.update()
        }
    }

    /**
     * T-144: clamp [desiredOffsetX] (world meters) so applying it to
     * [camera.position.x] never pushes the camera centre past the level's
     * playable bounds.
     *
     * Without this, the look-ahead near a level edge would reveal the void
     * beyond the rightmost wall. Mirrors the clamp logic in
     * `LevelRunState.update()` (lines ~847) but operates on the offset
     * delta rather than the absolute position.
     *
     * Returns the input unchanged when [levelWidthPx] is `0f` (default —
     * tests that don't supply a level).
     */
    private fun clampHorizontalOffset(desiredOffsetX: Float): Float {
        if (levelWidthPx <= 0f) return desiredOffsetX
        val halfW   = camera.viewportWidth / 2f
        val levelW  = levelWidthPx / Constants.PPM
        // Tightest the camera can be: [halfW, levelW - halfW]. Compute the
        // permitted delta from the current position.
        val minCam  = halfW
        val maxCam  = (levelW - halfW).coerceAtLeast(halfW)
        val curX    = camera.position.x
        val minDelta = minCam - curX
        val maxDelta = maxCam - curX
        return desiredOffsetX.coerceIn(minDelta, maxDelta)
    }

    /** Draws the player sprite using [SpriteBatch]. Handles flashing and Zephyr tint. */
    fun renderPlayer(currentCharacter: String) {
        refreshPalette()
        val flashVisible = !player.isFlashing || (player.deathFlashTimer * 8).toInt() % 2 == 0
        if (!flashVisible) return
        // T-097: skip the draw entirely once the death-fade is essentially invisible
        // so we don't issue a no-op batch call. Outside the death animation
        // [playerAlpha] is 1f so this is a no-op.
        if (playerAlpha <= 0.002f) return

        val playerPos = player.body.position
        val animator  = if (currentCharacter == "Ebo") eboAnimator else layaAnimator
        val frame = animator.getCurrentFrame()
        val sw = SpriteFactory.SPRITE_W / Constants.PPM
        val sh = SpriteFactory.SPRITE_H / Constants.PPM
        val sx = playerPos.x - sw / 2f
        val sy = playerPos.y - 32f / Constants.PPM

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.begin()
        // T-097: multiply the per-character tint by [playerAlpha] so the sprite
        // fades out during death. With playerAlpha == 1f the tints match the
        // pre-T-097 values exactly.
        val a = playerAlpha
        when {
            player.isFlashing              -> spriteBatch.setColor(1f, 0.35f, 0.35f, 0.85f * a)
            currentCharacter == "Zephyr"   -> spriteBatch.setColor(0.72f, 0.55f, 1f, 1f * a)
            a < 1f                          -> spriteBatch.setColor(1f, 1f, 1f, a)
        }
        if (player.isFacingRight) spriteBatch.draw(frame, sx, sy, sw, sh)
        else                      spriteBatch.draw(frame, sx + sw, sy, -sw, sh)
        if (player.isFlashing || currentCharacter == "Zephyr" || a < 1f) spriteBatch.setColor(Color.WHITE)
        spriteBatch.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // T-132: high-contrast overlay — paint a pure-white silhouette over the
        // player sprite via ShapeRenderer (the sprite texture itself can't be
        // recoloured without touching the atlas, so we cover it). The death
        // fade still applies via [playerAlpha] so the player still vanishes on
        // death; the per-character Zephyr tint is intentionally overridden
        // because at maximum contrast we want all three characters to read as
        // the SAME silhouette ("the player").
        if (highContrast) {
            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            // Match the player sprite's body footprint roughly: 28×32 px at
            // PPM=100 → 0.28×0.32 m. Centred at the body position (sy starts
            // 32 px below the body so the sprite includes legs).
            val whiteCol = Color(1f, 1f, 1f, a)
            shapeRenderer.color = whiteCol
            shapeRenderer.rect(playerPos.x - 0.14f, playerPos.y - 0.20f, 0.28f, 0.42f)
            shapeRenderer.end()
        }
    }

    /**
     * Draws text labels above each portal in the hub world.
     * Called from GameScreen after renderWorld/renderPlayer for Level0_0 only.
     */
    fun renderPortalLabels() {
        val font = FontManager.getShared(14)
        val completedLevels = SaveManager.loadGame().completedLevels
        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.begin()
        for (portal in Level0_0.PORTALS) {
            val required = Level0_0.portalUnlockRequirement(portal.userData)
            val unlocked = required.all { it in completedLevels }
            val label    = if (unlocked) portal.label else "[Locked]"
            val worldX   = portal.centerXPx / Constants.PPM
            val worldY   = (Level0_0.PORTAL_CENTER_Y_PX + Level0_0.PORTAL_HALF_H_PX + 15f) / Constants.PPM
            font.color = if (unlocked) Color.WHITE else Color.GRAY
            // GlyphLayout for centering; approximate width from label length
            font.draw(spriteBatch, label, worldX - 0.3f, worldY)
        }
        spriteBatch.end()
        font.color = Color.WHITE // reset
    }

    // ── Particle spawn helpers (called from LevelRunState on game events) ─────

    /**
     * Clamp a default particle-burst count when reduced-motion is enabled.
     * Returns 1 if the accessibility toggle is on, otherwise the unmodified default.
     * Behaviour with reducedMotion == false is byte-identical to before T-058.
     */
    private fun clampBurstCount(default: Int): Int =
        if (SettingsManager.load().reducedMotion) 1 else default

    fun spawnFootstep(x: Float, y: Float) {
        particles.spawn(x, y, 0f, 0f, 0.05f, 0.2f, footstepColor, gravity = 0f)
    }

    fun spawnJumpPuff(x: Float, y: Float, currentCharacter: String) {
        val col = when (currentCharacter) {
            "Ebo"    -> Color(0.7f, 0.55f, 0.35f, 0.8f)
            "Zephyr" -> Color(0.75f, 0.55f, 1.00f, 0.8f)
            else     -> Color(0.9f, 0.95f, 1f, 0.8f)
        }
        val puffMax = clampBurstCount(3)
        for (i in 0 until puffMax) {
            val ang   = (MathUtils.random() * 1.2f) + 0.2f
            val sign  = if (MathUtils.randomBoolean()) -1f else 1f
            val speed = MathUtils.random(0.4f, 0.8f)
            particles.spawn(
                x + MathUtils.random(-0.05f, 0.05f),
                y + MathUtils.random(-0.02f, 0.04f),
                vx      = sign * MathUtils.cos(ang) * speed,
                vy      = MathUtils.sin(ang) * speed * 0.6f,
                radius  = MathUtils.random(0.04f, 0.08f),
                life    = 0.18f,
                color   = col,
                gravity = 0f
            )
        }
    }

    fun spawnLandingDust(x: Float, y: Float, fallSpeed: Float) {
        val intensity = (-fallSpeed / 18f).coerceIn(0.5f, 1.5f)
        val count     = clampBurstCount((5 * intensity).toInt().coerceIn(4, 8))
        val col       = Color(0.55f, 0.50f, 0.42f, 0.85f)
        for (i in 0 until count) {
            val sign    = if (i % 2 == 0) -1f else 1f
            val outward = MathUtils.random(0.6f, 1.4f) * intensity
            particles.spawn(
                x + MathUtils.random(-0.08f, 0.08f), y,
                vx      = sign * outward,
                vy      = MathUtils.random(0.1f, 0.5f),
                radius  = MathUtils.random(0.05f, 0.10f),
                life    = MathUtils.random(0.25f, 0.40f),
                color   = col,
                gravity = 1f
            )
        }
    }

    fun spawnCleanseBurst(x: Float, y: Float) {
        val col = Color(0.25f, 0.85f, 0.60f, 0.9f)
        val count = clampBurstCount(12)
        for (i in 0 until count) {
            val ang   = MathUtils.random() * MathUtils.PI2
            val speed = MathUtils.random(1.0f, 2.8f)
            particles.spawn(
                x, y,
                vx      = MathUtils.cos(ang) * speed,
                vy      = MathUtils.sin(ang) * speed + 0.5f,
                radius  = MathUtils.random(0.06f, 0.12f),
                life    = MathUtils.random(0.35f, 0.60f),
                color   = col,
                gravity = 2f
            )
        }
    }

    fun spawnCollectSparkle(x: Float, y: Float, color: Color) {
        val count = clampBurstCount(8)
        for (i in 0 until count) {
            val ang   = MathUtils.random() * MathUtils.PI2
            val speed = MathUtils.random(0.8f, 2.0f)
            particles.spawn(
                x, y,
                vx      = MathUtils.cos(ang) * speed,
                vy      = MathUtils.sin(ang) * speed,
                radius  = MathUtils.random(0.05f, 0.09f),
                life    = 0.4f,
                color   = color,
                gravity = 1.5f
            )
        }
    }

    fun spawnTokenSparkle(x: Float, y: Float) {
        val count = clampBurstCount(6 + (Math.random() * 5).toInt())
        for (i in 0 until count) {
            particles.spawn(
                x + (Math.random() * 0.12 - 0.06).toFloat(),
                y + (Math.random() * 0.08).toFloat(),
                vx      = (Math.random() * 1.0 - 0.5).toFloat(),
                vy      = (0.8 + Math.random() * 0.4).toFloat(),
                radius  = 0.05f,
                life    = (0.35 + Math.random() * 0.10).toFloat(),
                color   = palette.SPARKLE_TOKEN,
                gravity = -2f
            )
        }
    }

    fun spawnSnapshotSparkle(x: Float, y: Float) {
        val count = clampBurstCount(6 + (Math.random() * 5).toInt())
        for (i in 0 until count) {
            particles.spawn(
                x + (Math.random() * 0.12 - 0.06).toFloat(),
                y + (Math.random() * 0.08).toFloat(),
                vx      = (Math.random() * 1.0 - 0.5).toFloat(),
                vy      = (0.8 + Math.random() * 0.4).toFloat(),
                radius  = 0.07f,
                life    = 0.5f,
                color   = palette.SPARKLE_SNAPSHOT,
                gravity = -2f
            )
        }
    }

    /** Spawn a grey smoke burst at the enemy position when stomped to defeat. */
    fun spawnStompSmokeBurst(x: Float, y: Float) {
        val count = clampBurstCount(5 + (Math.random() * 4).toInt())  // 5–8 particles
        for (i in 0 until count) {
            val ang   = MathUtils.random() * MathUtils.PI2
            val speed = MathUtils.random(0.8f, 1.8f)
            particles.spawn(
                x + MathUtils.random(-0.05f, 0.05f),
                y + MathUtils.random(-0.03f, 0.03f),
                vx      = MathUtils.cos(ang) * speed,
                vy      = MathUtils.sin(ang) * speed + 0.3f,
                radius  = MathUtils.random(0.06f, 0.11f),
                life    = MathUtils.random(0.30f, 0.50f),
                color   = SMOKE_STOMP,
                gravity = 1.5f
            )
        }
    }
}

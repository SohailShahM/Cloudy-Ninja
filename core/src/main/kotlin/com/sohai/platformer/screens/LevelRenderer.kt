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
import com.sohai.platformer.rendering.ParallaxBackground
import com.sohai.platformer.rendering.ParallaxTheme
import com.sohai.platformer.rendering.ParticleSystem
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
    private val parallaxTheme: ParallaxTheme = ParallaxTheme.ARID
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
    }

    /** Active mode-sensitive palette; resolved from SettingsManager at render time. */
    private var palette: Palette = Palette.forMode(SettingsManager.load().colorBlindMode)
    private var paletteMode: ColorBlindMode = SettingsManager.load().colorBlindMode

    private fun refreshPalette() {
        val m = SettingsManager.load().colorBlindMode
        if (m != paletteMode) {
            paletteMode = m
            palette = Palette.forMode(m)
        }
    }

    /**
     * Draws everything that lives in world-space: parallax, obstacles, abilities,
     * tokens, snapshots, platforms, particles. Opens and closes one ShapeRenderer
     * Filled block. Caller must NOT have an open SR block before calling this.
     */
    fun renderWorld(cleanseRatio: Float, currentCharacter: String, projectiles: List<Projectile> = emptyList()) {
        refreshPalette()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Layer 0: parallax sky + terrain silhouettes
        parallaxBg.render(shapeRenderer, camera, cleanseRatio)

        // ── Ability VFX ────────────────────────────────────────────────────────
        shapeRenderer.color = DROPLET
        for (droplet in eboAbility.getActiveRaindrops()) {
            val pos = droplet.body.position
            shapeRenderer.circle(pos.x, pos.y, droplet.getRadius())
        }
        for (trail in layaAbility.getActiveWindTrails()) {
            val pos = trail.getCurrentPosition()
            tmpWindCol.set(1f, 1f, 1f, 0.6f * trail.getAlpha())
            shapeRenderer.color = tmpWindCol
            shapeRenderer.circle(pos.x / Constants.PPM, pos.y / Constants.PPM, trail.getRadius())
        }
        for (trail in zephyrAbility.getActiveWindTrails()) {
            val pos = trail.getCurrentPosition()
            tmpWindCol.set(0.75f, 0.55f, 1f, 0.65f * trail.getAlpha())
            shapeRenderer.color = tmpWindCol
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
                    shapeRenderer.color = palette.HAZARD_CLEAN_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = palette.HAZARD_CLEAN_GLEAM
                    shapeRenderer.rect(cx - w, cy + he - 0.04f, w * 2f, 0.04f)
                }
                rect.kind == ObstacleKind.HAZARD -> {
                    shapeRenderer.color = palette.HAZARD_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = palette.HAZARD_STRIPE
                    var sx = cx - w
                    while (sx < cx + w) {
                        shapeRenderer.rect(sx, cy - he, 0.05f, he * 2f)
                        sx += 0.2f
                    }
                    // Triangular spike shapes along the top edge
                    shapeRenderer.color = palette.HAZARD_SPIKE
                    var spx = cx - w + 0.04f
                    while (spx < cx + w - 0.04f) {
                        shapeRenderer.triangle(spx - 0.04f, cy + he, spx + 0.04f, cy + he, spx, cy + he + 0.12f)
                        spx += 0.13f
                    }
                }
                rect.kind == ObstacleKind.WALL -> {
                    shapeRenderer.color = WALL_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = WALL_EDGE
                    shapeRenderer.rect(cx - w, cy - he, 0.03f, he * 2f)
                }
                rect.kind == ObstacleKind.EXIT && ud.startsWith("portal_") -> {
                    // Hub-world portal door: blue if unlocked, grey if locked
                    val required  = Level0_0.portalUnlockRequirement(ud)
                    val completed = SaveManager.loadGame().completedLevels
                    val unlocked  = required.all { it in completed }
                    val baseCol   = if (unlocked) palette.PORTAL_UNLOCKED else PORTAL_LOCKED
                    val edgeCol   = if (unlocked) palette.PORTAL_UNLOCKED_EDGE else PORTAL_LOCKED_EDGE
                    shapeRenderer.color = baseCol
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = edgeCol
                    shapeRenderer.rect(cx - w, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx + w - 0.05f, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx - w, cy + he - 0.04f, w * 2f, 0.04f)
                }
                rect.kind == ObstacleKind.EXIT -> {
                    shapeRenderer.color = palette.EXIT_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = palette.EXIT_EDGE
                    shapeRenderer.rect(cx - w, cy - he, 0.05f, he * 2f)
                    shapeRenderer.rect(cx + w - 0.05f, cy - he, 0.05f, he * 2f)
                }
                else -> {
                    shapeRenderer.color = GROUND_BASE
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, he * 2f)
                    shapeRenderer.color = GROUND_TOP
                    shapeRenderer.rect(cx - w, cy + he - 0.05f, w * 2f, 0.05f)
                    // Grass tufts along the top surface — height varies with position
                    shapeRenderer.color = palette.GRASS_TUFT
                    var gx = cx - w + 0.04f
                    while (gx < cx + w - 0.04f) {
                        val bh = 0.062f + MathUtils.sin(gx * 19.1f + cx * 4.3f) * 0.024f
                        shapeRenderer.triangle(gx - 0.022f, cy + he, gx + 0.022f, cy + he, gx, cy + he + bh)
                        gx += 0.10f
                    }
                    // Bottom shadow strip for a subtle 3-D depth impression
                    shapeRenderer.color = GROUND_SHADOW
                    shapeRenderer.rect(cx - w, cy - he, w * 2f, 0.04f)
                }
            }
        }

        // Moving platforms
        for (mp in movingPlatforms) {
            val pos = mp.body.position
            val hw = 50f / Constants.PPM
            val hh = 10f / Constants.PPM
            shapeRenderer.color = MP_BASE
            shapeRenderer.rect(pos.x - hw, pos.y - hh, hw * 2f, hh * 2f)
            shapeRenderer.color = MP_TOP
            shapeRenderer.rect(pos.x - hw, pos.y + hh - 0.04f, hw * 2f, 0.04f)
            // Underside shadow for depth
            shapeRenderer.color = MP_SHADOW
            shapeRenderer.rect(pos.x - hw, pos.y - hh, hw * 2f, 0.025f)
        }

        // Checkpoints
        for (cp in obstacleManager.checkpoints()) {
            val activated = cp.fixture.userData as? String == "checkpoint_activated"
            val r = cp.radiusPx / Constants.PPM
            shapeRenderer.color = if (activated) palette.CP_GLOW_ACTIVE else palette.CP_GLOW_INACTIVE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r * 1.5f)
            shapeRenderer.color = if (activated) palette.CP_BODY_ACTIVE else palette.CP_BODY_INACTIVE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r)
            shapeRenderer.color = Color.WHITE
            shapeRenderer.circle(cp.body.position.x, cp.body.position.y, r * 0.35f)
        }

        // Eco-tokens
        shapeRenderer.color = palette.TOKEN
        for (token in ecoTokens) {
            if (!token.isCollected) {
                val p = token.body.position
                shapeRenderer.circle(p.x, p.y, token.getAnimatedRadius())
            }
        }

        // Cloud Atlas snapshot stars
        for (snap in snapshotPickups) {
            if (!snap.isCollected) {
                val p  = snap.body.position
                val r  = snap.getAnimatedRadius()
                val ri = r * 0.45f
                shapeRenderer.color = palette.SNAPSHOT_GLOW
                shapeRenderer.circle(p.x, p.y, r * 1.4f)
                shapeRenderer.color = palette.SNAPSHOT_BODY
                shapeRenderer.triangle(p.x, p.y + r,  p.x + ri, p.y,      p.x, p.y - r)
                shapeRenderer.triangle(p.x, p.y + r,  p.x - ri, p.y,      p.x, p.y - r)
                shapeRenderer.triangle(p.x - r, p.y,  p.x, p.y + ri,  p.x + r, p.y)
                shapeRenderer.triangle(p.x - r, p.y,  p.x, p.y - ri,  p.x + r, p.y)
            }
        }

        // Projectiles
        shapeRenderer.color = PROJECTILE
        for (proj in projectiles) {
            val pos = proj.body.position
            shapeRenderer.circle(pos.x, pos.y, Projectile.RADIUS)
        }

        // Enemies
        for (enemy in enemies) {
            enemy.draw(shapeRenderer)
        }

        // Boss sentinel (drawn after enemies so telegraph rings appear on top)
        sentinel?.draw(shapeRenderer)

        // Particles (alpha blend enabled; works inside the Filled block via GL blend state)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        particles.render(shapeRenderer)

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    /** Draws the player sprite using [SpriteBatch]. Handles flashing and Zephyr tint. */
    fun renderPlayer(currentCharacter: String) {
        val flashVisible = !player.isFlashing || (player.deathFlashTimer * 8).toInt() % 2 == 0
        if (!flashVisible) return

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
        when {
            player.isFlashing              -> spriteBatch.setColor(1f, 0.35f, 0.35f, 0.85f)
            currentCharacter == "Zephyr"   -> spriteBatch.setColor(0.72f, 0.55f, 1f, 1f)
        }
        if (player.isFacingRight) spriteBatch.draw(frame, sx, sy, sw, sh)
        else                      spriteBatch.draw(frame, sx + sw, sy, -sw, sh)
        if (player.isFlashing || currentCharacter == "Zephyr") spriteBatch.setColor(Color.WHITE)
        spriteBatch.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
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

    fun spawnFootstep(x: Float, y: Float) {
        particles.spawn(x, y, 0f, 0f, 0.05f, 0.2f, footstepColor, gravity = 0f)
    }

    fun spawnJumpPuff(x: Float, y: Float, currentCharacter: String) {
        val col = when (currentCharacter) {
            "Ebo"    -> Color(0.7f, 0.55f, 0.35f, 0.8f)
            "Zephyr" -> Color(0.75f, 0.55f, 1.00f, 0.8f)
            else     -> Color(0.9f, 0.95f, 1f, 0.8f)
        }
        for (i in 0..2) {
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
        val count     = (5 * intensity).toInt().coerceIn(4, 8)
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
        for (i in 0 until 12) {
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
        for (i in 0 until 8) {
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
        val count = 6 + (Math.random() * 5).toInt()
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
        val count = 6 + (Math.random() * 5).toInt()
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
        val count = 5 + (Math.random() * 4).toInt()  // 5–8 particles
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

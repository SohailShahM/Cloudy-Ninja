package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Generates pixel-art character sprites procedurally via Pixmap.
 *
 * Sprite canvas: 32 × 80 px logical size (0.32 × 0.80 m at PPM=100).
 * Draw origin: (playerX – 0.16, playerY – 0.32) — bottom aligned to physics box bottom,
 * top extends 0.16 m above the physics box to accommodate the head.
 *
 * Pixmap y=0 is the TOP of the sprite; y=79 is the BOTTOM.
 *
 * ## 4 K / HiDPI support
 * [makeTex] draws at the base 32 × 80 resolution and then nearest-neighbour-upscales
 * to `32 * [DisplayScale.spriteScale] × 80 * [DisplayScale.spriteScale]`.
 * At 4 K (spriteScale = 3) each logical pixel becomes a crisp 3 × 3 block — the
 * classic pixel-art look — without any blurring from bilinear upsampling.
 * The [LevelRenderer] still renders the sprite at its fixed 0.32 × 0.80 m world size;
 * the larger texture simply maps 1:1 to the physical screen pixels.
 */
object SpriteFactory {

    const val SPRITE_W = 32
    const val SPRITE_H = 80

    // ── Pose enums ────────────────────────────────────────────────────────────

    private enum class LegPose { NEUTRAL, NEUTRAL_BOB, STRIDE_L, STRIDE_R, JUMP, FALL, WALL }
    private enum class ArmPose { NORMAL, SWING_L, SWING_R, RAISED, SPREAD, WALL }

    // ── Colour palettes ───────────────────────────────────────────────────────

    private data class Pal(
        val skin: Color,
        val cloth: Color,
        val accent: Color,
        val hair: Color,
        val eye: Color,
        val shadow: Color,
    )

    private val EBO = Pal(
        skin   = Color(0.83f, 0.57f, 0.29f, 1f),   // warm tan
        cloth  = Color(0.36f, 0.23f, 0.12f, 1f),   // dark earth brown
        accent = Color(0.24f, 0.48f, 0.16f, 1f),   // forest green
        hair   = Color(0.13f, 0.08f, 0.00f, 1f),   // dark brown-black
        eye    = Color(0.10f, 0.10f, 0.15f, 1f),   // near black
        shadow = Color(0.55f, 0.35f, 0.18f, 1f),   // mid brown
    )

    private val LAYA = Pal(
        skin   = Color(0.88f, 0.82f, 0.72f, 1f),   // pale warm
        cloth  = Color(0.29f, 0.50f, 0.66f, 1f),   // wind blue
        accent = Color(0.54f, 0.78f, 0.93f, 1f),   // sky blue
        hair   = Color(0.54f, 0.71f, 0.86f, 1f),   // blue-grey
        eye    = Color(0.10f, 0.19f, 0.31f, 1f),   // deep blue
        shadow = Color(0.42f, 0.60f, 0.73f, 1f),   // shadow blue
    )

    // ── Public factories ──────────────────────────────────────────────────────

    fun createEbo(): CharacterAtlas = create(EBO, slim = false)

    fun createLaya(): CharacterAtlas = create(LAYA, slim = true)

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun create(p: Pal, slim: Boolean): CharacterAtlas {
        val idle0 = makeTex(p, slim, LegPose.NEUTRAL,     ArmPose.NORMAL)
        val idle1 = makeTex(p, slim, LegPose.NEUTRAL_BOB, ArmPose.NORMAL)
        val walk0 = makeTex(p, slim, LegPose.STRIDE_L,    ArmPose.SWING_L)
        val walk1 = makeTex(p, slim, LegPose.NEUTRAL,     ArmPose.NORMAL)
        val walk2 = makeTex(p, slim, LegPose.STRIDE_R,    ArmPose.SWING_R)
        val walk3 = makeTex(p, slim, LegPose.NEUTRAL,     ArmPose.NORMAL)
        val jump  = makeTex(p, slim, LegPose.JUMP,        ArmPose.RAISED)
        val fall  = makeTex(p, slim, LegPose.FALL,        ArmPose.SPREAD)
        val wall  = makeTex(p, slim, LegPose.WALL,        ArmPose.WALL)

        val all = listOf(idle0, idle1, walk0, walk1, walk2, walk3, jump, fall, wall)
        return CharacterAtlas(
            idle      = arrayOf(TextureRegion(idle0), TextureRegion(idle1)),
            walk      = arrayOf(TextureRegion(walk0), TextureRegion(walk1),
                                TextureRegion(walk2), TextureRegion(walk3)),
            jump      = TextureRegion(jump),
            fall      = TextureRegion(fall),
            wallSlide = TextureRegion(wall),
            textures  = all,
        )
    }

    /**
     * Draws one full character frame onto a fresh 32×80 Pixmap and returns
     * the resulting Texture (caller is responsible for disposing via CharacterAtlas).
     *
     * Layout (y=0 = top of sprite):
     *   y  0-20 : head + hair
     *   y 20-23 : neck
     *   y 23-48 : torso + arms
     *   y 48-75 : legs
     *   y 75-80 : feet
     */
    private fun makeTex(p: Pal, slim: Boolean, legPose: LegPose, armPose: ArmPose): Texture {
        val pm = Pixmap(SPRITE_W, SPRITE_H, Pixmap.Format.RGBA8888)
        pm.blending = Pixmap.Blending.None  // overwrite, don't blend over transparent

        val bob = if (legPose == LegPose.NEUTRAL_BOB) -1 else 0

        // ── Torso width ──────────────────────────────────────────────────────
        // Ebo = stocky (18 px), Laya = slim (14 px), centred at x=16
        val tw = if (slim) 14 else 18          // torso width
        val tx = 16 - tw / 2                   // torso left x

        // ── Head ─────────────────────────────────────────────────────────────
        val headCy = 11 + bob
        val headR  = if (slim) 9 else 10
        pm.setColor(p.skin);   pm.fillCircle(16, headCy, headR)
        pm.setColor(p.hair);   pm.fillRectangle(16 - headR + 2, headCy - headR + 1, headR * 2 - 4, 7)
        // Eyes — 2 px wide
        pm.setColor(p.eye)
        pm.drawPixel(11, headCy - 1); pm.drawPixel(12, headCy - 1)
        pm.drawPixel(19, headCy - 1); pm.drawPixel(20, headCy - 1)
        // Mouth — small horizontal stripe
        pm.setColor(p.shadow)
        pm.drawPixel(14, headCy + 4); pm.drawPixel(15, headCy + 4)
        pm.drawPixel(16, headCy + 4); pm.drawPixel(17, headCy + 4)

        // ── Neck ─────────────────────────────────────────────────────────────
        val neckY = headCy + headR
        pm.setColor(p.skin); pm.fillRectangle(14, neckY, 4, 4)

        // ── Torso ─────────────────────────────────────────────────────────────
        val torsoTop = neckY + 4
        // Shoulder row (a touch wider than main torso)
        pm.setColor(p.cloth)
        pm.fillRectangle(tx - 2, torsoTop, tw + 4, 2)
        pm.fillRectangle(tx, torsoTop + 2, tw, 20)
        // Accent stripe (belt-ish at torso mid)
        pm.setColor(p.accent); pm.fillRectangle(tx, torsoTop + 17, tw, 3)
        // Lower torso / hips
        pm.setColor(p.cloth);  pm.fillRectangle(tx, torsoTop + 20, tw, 5)

        val torsoBot = torsoTop + 25   // y of top of legs

        // ── Arms ──────────────────────────────────────────────────────────────
        val aw = if (slim) 5 else 6    // arm width
        val armLen = 18
        when (armPose) {
            ArmPose.NORMAL -> {
                pm.setColor(p.skin)
                pm.fillRectangle(tx - aw - 1, torsoTop, aw, armLen)
                pm.fillRectangle(tx + tw + 1,  torsoTop, aw, armLen)
            }
            ArmPose.SWING_L -> {  // left arm forward (swings opposite to lead leg)
                pm.setColor(p.skin)
                pm.fillRectangle(tx - aw - 1, torsoTop + 4, aw, armLen - 5)  // left back
                pm.fillRectangle(tx + tw + 1,  torsoTop - 4, aw, armLen)      // right forward
            }
            ArmPose.SWING_R -> {
                pm.setColor(p.skin)
                pm.fillRectangle(tx - aw - 1, torsoTop - 4, aw, armLen)      // left forward
                pm.fillRectangle(tx + tw + 1,  torsoTop + 4, aw, armLen - 5)  // right back
            }
            ArmPose.RAISED -> {
                pm.setColor(p.skin)
                pm.fillRectangle(tx - aw - 1, torsoTop - 10, aw, armLen)
                pm.fillRectangle(tx + tw + 1,  torsoTop - 10, aw, armLen)
            }
            ArmPose.SPREAD -> {
                pm.setColor(p.skin)
                pm.fillRectangle(tx - aw - 1 - 4, torsoTop + 8, aw + 6, 6)
                pm.fillRectangle(tx + tw + 1,       torsoTop + 8, aw + 6, 6)
            }
            ArmPose.WALL -> {
                pm.setColor(p.skin)
                pm.fillRectangle(tx - aw - 1, torsoTop,      aw, armLen)       // left arm normal
                pm.fillRectangle(tx + tw + 1, torsoTop - 8,  aw, armLen - 2)  // right arm reaching up
            }
        }

        // ── Legs ──────────────────────────────────────────────────────────────
        val lw = if (slim) 6 else 7    // leg width
        val lx0 = tx                   // left leg default x
        val rx0 = tx + tw - lw         // right leg default x

        when (legPose) {
            LegPose.NEUTRAL, LegPose.NEUTRAL_BOB -> {
                pm.setColor(p.cloth)
                pm.fillRectangle(lx0,     torsoBot, lw, 15)
                pm.fillRectangle(rx0,     torsoBot, lw, 15)
                pm.setColor(p.shadow)
                pm.fillRectangle(lx0 - 1, torsoBot + 15, lw + 3, 5)
                pm.fillRectangle(rx0 - 1, torsoBot + 15, lw + 3, 5)
            }
            LegPose.STRIDE_L -> {  // left leg forward/up, right leg back/down
                pm.setColor(p.cloth)
                pm.fillRectangle(lx0 + 2, torsoBot, lw, 12)       // left upper
                pm.fillRectangle(rx0 - 2, torsoBot, lw, 18)       // right upper
                pm.setColor(p.shadow)
                pm.fillRectangle(lx0 + 1, torsoBot + 12, lw + 3, 5)  // left foot (higher)
                pm.fillRectangle(rx0 - 3, torsoBot + 18, lw + 3, 5)  // right foot (lower)
            }
            LegPose.STRIDE_R -> {  // right leg forward/up, left leg back/down
                pm.setColor(p.cloth)
                pm.fillRectangle(lx0 - 2, torsoBot, lw, 18)       // left upper
                pm.fillRectangle(rx0 + 2, torsoBot, lw, 12)       // right upper
                pm.setColor(p.shadow)
                pm.fillRectangle(lx0 - 3, torsoBot + 18, lw + 3, 5)  // left foot (lower)
                pm.fillRectangle(rx0 + 1, torsoBot + 12, lw + 3, 5)  // right foot (higher)
            }
            LegPose.JUMP -> {
                pm.setColor(p.cloth)
                pm.fillRectangle(lx0,     torsoBot, lw, 10)
                pm.fillRectangle(rx0,     torsoBot, lw, 10)
                pm.setColor(p.shadow)
                pm.fillRectangle(lx0 - 1, torsoBot + 10, lw + 3, 5)
                pm.fillRectangle(rx0 - 1, torsoBot + 10, lw + 3, 5)
            }
            LegPose.FALL -> {
                pm.setColor(p.cloth)
                pm.fillRectangle(lx0,     torsoBot, lw, 19)
                pm.fillRectangle(rx0,     torsoBot, lw, 19)
                pm.setColor(p.shadow)
                pm.fillRectangle(lx0 - 1, torsoBot + 19, lw + 3, 5)
                pm.fillRectangle(rx0 - 1, torsoBot + 19, lw + 3, 5)
            }
            LegPose.WALL -> {
                pm.setColor(p.cloth)
                pm.fillRectangle(lx0,     torsoBot, lw, 12)  // front leg bent
                pm.fillRectangle(rx0,     torsoBot, lw, 16)  // back leg hanging
                pm.setColor(p.shadow)
                pm.fillRectangle(lx0 - 1, torsoBot + 12, lw + 3, 5)
                pm.fillRectangle(rx0 - 1, torsoBot + 16, lw + 3, 5)
            }
        }

        val scale = DisplayScale.spriteScale
        val tex: Texture
        if (scale <= 1) {
            tex = Texture(pm)
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            pm.dispose()
        } else {
            // Nearest-neighbour upscale: each logical pixel → scale×scale physical pixels.
            // This preserves the crisp pixel-art look at integer display scales (2×, 3×…).
            val scaled = Pixmap(SPRITE_W * scale, SPRITE_H * scale, Pixmap.Format.RGBA8888)
            scaled.drawPixmap(pm, 0, 0, SPRITE_W, SPRITE_H, 0, 0, SPRITE_W * scale, SPRITE_H * scale)
            pm.dispose()
            tex = Texture(scaled)
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            scaled.dispose()
        }
        return tex
    }
}

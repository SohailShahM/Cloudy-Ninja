package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.sohai.platformer.Constants

/**
 * Visual theme for [ParallaxBackground].
 *
 * Each theme defines the corrupted→cleansed colour palette for one campaign world:
 * - [ARID]  Level 1 — parched expanse, warm browns/oranges → rich greens/golds
 * - [WIND]  Level 2 — wind and weather, slate greys/blues → crisp whites/sky-blues
 * - [ECO]   Level 3 — eco-restoration, deep swamp greens → bright teals/limes
 */
enum class ParallaxTheme { ARID, WIND, ECO }

/**
 * Renders a three-layer parallax background (distant mountains, midground hills, tree
 * silhouettes) plus a three-band sky gradient that lerps from a "corrupted" dark palette
 * to a bright "cleansed" palette as cleanseRatio approaches 1.
 *
 * Additional polish details:
 * - Stars appear in the corrupted sky and fade out as the level is cleansed.
 * - Distant mountain peaks get a lighter highlight cap.
 * - Tree trunks get a triangular crown for a pine-like silhouette.
 *
 * All geometry is drawn in Box2D world-space (meters) using the game camera's combined
 * matrix — no projection-matrix swap, no save/restore.
 * Caller must NOT have an open ShapeRenderer begin/end block.
 *
 * @param theme Per-level visual theme; defaults to [ParallaxTheme.ARID].
 */
class ParallaxBackground(private val theme: ParallaxTheme = ParallaxTheme.ARID) {

    private data class BgElement(
        val patternXPx: Float,
        val yPx: Float,
        val widthPx: Float,
        val heightPx: Float,
        val isTriangle: Boolean = false
    )

    private data class Layer(
        val scrollFactor: Float,
        val patternWidthPx: Float,
        val elements: List<BgElement>
    )

    private val layers = listOf(
        // Layer 0: Distant mountains — slowest scroll (farthest from camera)
        Layer(
            scrollFactor   = 0.15f,
            patternWidthPx = 1280f,
            elements = listOf(
                BgElement(   0f, 0f, 280f, 220f, isTriangle = true),
                BgElement( 240f, 0f, 300f, 180f, isTriangle = true),
                BgElement( 520f, 0f, 260f, 260f, isTriangle = true),
                BgElement( 780f, 0f, 290f, 200f, isTriangle = true),
                BgElement(1040f, 0f, 250f, 240f, isTriangle = true)
            )
        ),
        // Layer 1: Midground hills — medium scroll (depth between mountains and trees)
        Layer(
            scrollFactor   = 0.28f,
            patternWidthPx = 960f,
            elements = listOf(
                BgElement( 30f, 0f, 200f, 110f, isTriangle = true),
                BgElement(220f, 0f, 240f, 130f, isTriangle = true),
                BgElement(440f, 0f, 210f, 100f, isTriangle = true),
                BgElement(630f, 0f, 230f, 125f, isTriangle = true),
                BgElement(810f, 0f, 200f, 108f, isTriangle = true)
            )
        ),
        // Layer 2: Tree silhouettes — fastest scroll (closest to camera)
        Layer(
            scrollFactor   = 0.4f,
            patternWidthPx = 640f,
            elements = listOf(
                BgElement( 10f, 0f,  28f, 130f),
                BgElement( 80f, 0f,  32f, 160f),
                BgElement(160f, 0f,  25f, 110f),
                BgElement(240f, 0f,  30f, 150f),
                BgElement(315f, 0f,  27f, 135f),
                BgElement(395f, 0f,  33f, 145f),
                BgElement(470f, 0f,  28f, 125f),
                BgElement(550f, 0f,  31f, 155f)
            )
        )
    )

    // ── Per-theme colour palettes ────────────────────────────────────────────
    // Three-band sky: top (deep sky), mid (mid sky), bot (horizon glow)

    private data class Palette(
        val corSkyTop:    Color, val corSkyMid:    Color, val corSkyBot:    Color,
        val corMountains: Color, val corMidground: Color, val corTrees:     Color,
        val clrSkyTop:    Color, val clrSkyMid:    Color, val clrSkyBot:    Color,
        val clrMountains: Color, val clrMidground: Color, val clrTrees:     Color
    )

    private val palette: Palette = when (theme) {
        ParallaxTheme.ARID -> Palette(          // Level 1 — parched expanse
            corSkyTop    = Color(0.08f, 0.06f, 0.04f, 1f),
            corSkyMid    = Color(0.15f, 0.10f, 0.05f, 1f),
            corSkyBot    = Color(0.22f, 0.14f, 0.07f, 1f),
            corMountains = Color(0.22f, 0.16f, 0.08f, 1f),
            corMidground = Color(0.18f, 0.13f, 0.07f, 1f),
            corTrees     = Color(0.18f, 0.14f, 0.06f, 1f),
            clrSkyTop    = Color(0.78f, 0.48f, 0.16f, 1f),
            clrSkyMid    = Color(0.92f, 0.68f, 0.30f, 1f),
            clrSkyBot    = Color(0.98f, 0.84f, 0.52f, 1f),
            clrMountains = Color(0.55f, 0.40f, 0.20f, 1f),
            clrMidground = Color(0.44f, 0.30f, 0.12f, 1f),
            clrTrees     = Color(0.30f, 0.55f, 0.12f, 1f)
        )
        ParallaxTheme.WIND -> Palette(          // Level 2 — wind and weather
            corSkyTop    = Color(0.04f, 0.07f, 0.18f, 1f),
            corSkyMid    = Color(0.06f, 0.10f, 0.22f, 1f),
            corSkyBot    = Color(0.10f, 0.14f, 0.28f, 1f),
            corMountains = Color(0.12f, 0.15f, 0.28f, 1f),
            corMidground = Color(0.10f, 0.13f, 0.24f, 1f),
            corTrees     = Color(0.10f, 0.12f, 0.22f, 1f),
            clrSkyTop    = Color(0.42f, 0.62f, 0.92f, 1f),
            clrSkyMid    = Color(0.60f, 0.80f, 0.98f, 1f),
            clrSkyBot    = Color(0.82f, 0.93f, 1.00f, 1f),
            clrMountains = Color(0.62f, 0.72f, 0.88f, 1f),
            clrMidground = Color(0.50f, 0.62f, 0.80f, 1f),
            clrTrees     = Color(0.55f, 0.68f, 0.85f, 1f)
        )
        ParallaxTheme.ECO -> Palette(           // Level 3 — eco-restoration
            corSkyTop    = Color(0.02f, 0.08f, 0.04f, 1f),
            corSkyMid    = Color(0.04f, 0.12f, 0.06f, 1f),
            corSkyBot    = Color(0.06f, 0.18f, 0.10f, 1f),
            corMountains = Color(0.06f, 0.14f, 0.08f, 1f),
            corMidground = Color(0.05f, 0.16f, 0.08f, 1f),
            corTrees     = Color(0.04f, 0.18f, 0.08f, 1f),
            clrSkyTop    = Color(0.10f, 0.62f, 0.50f, 1f),
            clrSkyMid    = Color(0.18f, 0.80f, 0.62f, 1f),
            clrSkyBot    = Color(0.30f, 0.94f, 0.74f, 1f),
            clrMountains = Color(0.15f, 0.58f, 0.38f, 1f),
            clrMidground = Color(0.10f, 0.46f, 0.28f, 1f),
            clrTrees     = Color(0.10f, 0.75f, 0.35f, 1f)
        )
    }

    // Precomputed star positions — normalized (0..1, 0..1) screen coords.
    // Stars are camera-anchored (they do not scroll with the world).
    // ny is biased toward the upper portion of the sky.
    private val stars: List<Pair<Float, Float>> = List(28) { i ->
        val h1 = ((i.toLong()        * 2654435761L) and 0x7FFFFFFFL).toInt()
        val h2 = (((i + 37).toLong() * 2246822519L) and 0x7FFFFFFFL).toInt()
        Pair(
            (h1 % 10000) / 10000f,
            (h2 % 10000) / 10000f * 0.55f + 0.42f   // upper ~55% of sky
        )
    }

    private val tmp = Color()

    private fun lerp(a: Color, b: Color, t: Float): Color =
        tmp.set(a).lerp(b, t.coerceIn(0f, 1f))

    /**
     * Draws background geometry into an already-open ShapeRenderer Filled block.
     * Caller must set sr.projectionMatrix = camera.combined and call sr.begin()
     * before this, and sr.end() after.
     */
    fun render(sr: ShapeRenderer, camera: OrthographicCamera, cleanseRatio: Float = 0f) {
        val t = cleanseRatio.coerceIn(0f, 1f)
        val ppm = Constants.PPM

        val halfW   = camera.viewportWidth  / 2f
        val halfH   = camera.viewportHeight / 2f
        val camX    = camera.position.x
        val camY    = camera.position.y
        val left    = camX - halfW
        val bot     = camY - halfH
        val screenW = halfW * 2f
        val screenH = halfH * 2f

        // ── Three-band sky gradient ──────────────────────────────────────────
        // Copy lerp results immediately so tmp can be reused by subsequent lerps.
        val skyTopC = Color(lerp(palette.corSkyTop, palette.clrSkyTop, t))
        val skyMidC = Color(lerp(palette.corSkyMid, palette.clrSkyMid, t))
        sr.color = skyTopC
        sr.rect(left, bot + screenH * 0.67f, screenW, screenH * 0.33f)
        sr.color = skyMidC
        sr.rect(left, bot + screenH * 0.33f, screenW, screenH * 0.34f)
        sr.color = lerp(palette.corSkyBot, palette.clrSkyBot, t)
        sr.rect(left, bot, screenW, screenH * 0.33f)

        // ── Stars (corrupted sky only, fade as cleanseRatio → 0.5) ───────────
        val starAlpha = ((0.5f - t) * 2f).coerceIn(0f, 1f)
        if (starAlpha > 0.02f) {
            for ((nx, ny) in stars) {
                val sx = left + nx * screenW
                val sy = bot  + ny * screenH
                tmp.set(1f, 0.97f, 0.88f, starAlpha * 0.90f)
                sr.color = tmp
                sr.circle(sx, sy, 0.022f, 5)
            }
        }

        // ── Parallax layers ──────────────────────────────────────────────────
        // Pre-compute base colours as copies (not references to the shared tmp).
        val col0 = Color(lerp(palette.corMountains, palette.clrMountains, t))
        val col1 = Color(lerp(palette.corMidground, palette.clrMidground, t))
        val col2 = Color(lerp(palette.corTrees,     palette.clrTrees,     t))
        val layerColors = arrayOf(col0, col1, col2)

        for ((idx, layer) in layers.withIndex()) {
            val baseCol = layerColors[idx]

            val patternW      = layer.patternWidthPx / ppm
            val scrollOffset  = camX * layer.scrollFactor
            val patternOffset = ((scrollOffset % patternW) + patternW) % patternW

            for (el in layer.elements) {
                val elX = el.patternXPx / ppm
                val elY = el.yPx        / ppm
                val elW = el.widthPx    / ppm
                val elH = el.heightPx   / ppm

                val baseX = left + elX - patternOffset
                for (tile in -1..1) {
                    val sx = baseX + tile * patternW
                    if (sx + elW < left || sx > left + screenW) continue
                    val sy = bot + elY

                    sr.color = baseCol
                    if (el.isTriangle) {
                        // Mountain / hill triangle
                        sr.triangle(sx, sy, sx + elW, sy, sx + elW / 2f, sy + elH)

                        // Distant mountains (layer 0) get a bright highlight cap at the peak
                        if (idx == 0) {
                            val capPct   = 0.22f
                            val capBaseY = sy + elH * (1f - capPct)
                            val capHalfW = elW * capPct * 0.5f
                            val cx       = sx + elW / 2f
                            tmp.set(baseCol).add(0.22f, 0.22f, 0.22f, 0f).clamp()
                            sr.color = tmp
                            sr.triangle(cx - capHalfW, capBaseY, cx + capHalfW, capBaseY, cx, sy + elH)
                        }
                    } else {
                        // Tree trunk rectangle
                        sr.rect(sx, sy, elW, elH)
                        // Triangular pine crown above the trunk
                        tmp.set(baseCol).add(0.04f, 0.10f, 0.04f, 0f).clamp()
                        sr.color = tmp
                        sr.triangle(
                            sx - elW * 0.20f, sy + elH * 0.55f,
                            sx + elW * 1.20f, sy + elH * 0.55f,
                            sx + elW * 0.50f, sy + elH * 1.30f
                        )
                    }
                }
            }
        }
    }

    fun dispose() {}
}

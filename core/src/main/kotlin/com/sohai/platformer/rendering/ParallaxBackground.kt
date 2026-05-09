package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.sohai.platformer.Constants

/**
 * Renders a two-layer parallax background (distant mountains + mid-ground trees)
 * plus a two-tone sky that lerps from a "corrupted" dark palette to a bright
 * "cleansed" palette as cleanseRatio approaches 1.
 *
 * All geometry is drawn in Box2D world-space (meters) using the game camera's
 * combined matrix — no projection-matrix swap, no save/restore.
 * Caller must NOT have an open ShapeRenderer begin/end block.
 */
class ParallaxBackground {

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
        Layer(
            scrollFactor = 0.15f,
            patternWidthPx = 1280f,
            elements = listOf(
                BgElement(0f,    0f, 280f, 220f, isTriangle = true),
                BgElement(240f,  0f, 300f, 180f, isTriangle = true),
                BgElement(520f,  0f, 260f, 260f, isTriangle = true),
                BgElement(780f,  0f, 290f, 200f, isTriangle = true),
                BgElement(1040f, 0f, 250f, 240f, isTriangle = true)
            )
        ),
        Layer(
            scrollFactor = 0.4f,
            patternWidthPx = 640f,
            elements = listOf(
                BgElement(10f,  0f, 28f, 130f),
                BgElement(80f,  0f, 32f, 160f),
                BgElement(160f, 0f, 25f, 110f),
                BgElement(240f, 0f, 30f, 150f),
                BgElement(315f, 0f, 27f, 135f),
                BgElement(395f, 0f, 33f, 145f),
                BgElement(470f, 0f, 28f, 125f),
                BgElement(550f, 0f, 31f, 155f)
            )
        )
    )

    private val corruptedSkyTop    = Color(0.06f, 0.09f, 0.20f, 1f)
    private val corruptedSkyBot    = Color(0.10f, 0.14f, 0.26f, 1f)
    private val corruptedMountains = Color(0.12f, 0.15f, 0.28f, 1f)
    private val corruptedTrees     = Color(0.08f, 0.18f, 0.10f, 1f)

    private val cleansedSkyTop    = Color(0.22f, 0.55f, 0.90f, 1f)
    private val cleansedSkyBot    = Color(0.45f, 0.75f, 0.98f, 1f)
    private val cleansedMountains = Color(0.30f, 0.40f, 0.60f, 1f)
    private val cleansedTrees     = Color(0.12f, 0.55f, 0.22f, 1f)

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

        // Two-tone sky
        sr.color = lerp(corruptedSkyTop, cleansedSkyTop, t)
        sr.rect(left, bot + halfH, screenW, halfH)
        sr.color = lerp(corruptedSkyBot, cleansedSkyBot, t)
        sr.rect(left, bot, screenW, halfH)

        val col0 = Color(lerp(corruptedMountains, cleansedMountains, t))
        val col1 = Color(lerp(corruptedTrees, cleansedTrees, t))
        val layerColors = arrayOf(col0, col1)

        for ((idx, layer) in layers.withIndex()) {
            sr.color = layerColors[idx]

            val patternW      = layer.patternWidthPx / ppm
            val scrollOffset  = camX * layer.scrollFactor
            val patternOffset = ((scrollOffset % patternW) + patternW) % patternW

            for (el in layer.elements) {
                val elX = el.patternXPx / ppm
                val elY = el.yPx       / ppm
                val elW = el.widthPx   / ppm
                val elH = el.heightPx  / ppm

                val baseX = left + elX - patternOffset
                for (tile in -1..1) {
                    val sx = baseX + tile * patternW
                    if (sx + elW < left || sx > left + screenW) continue
                    val sy = bot + elY
                    if (el.isTriangle) {
                        sr.triangle(sx, sy, sx + elW, sy, sx + elW / 2f, sy + elH)
                    } else {
                        sr.rect(sx, sy, elW, elH)
                    }
                }
            }
        }
    }

    fun dispose() {}
}

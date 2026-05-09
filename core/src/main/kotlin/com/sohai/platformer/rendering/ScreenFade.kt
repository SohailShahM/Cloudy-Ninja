package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable

/**
 * Full-screen black fade overlay. Uses SpriteBatch + a 1×1 white texture so
 * it shares no state with the game's ShapeRenderer, eliminating a crash source.
 */
class ScreenFade(
    private val viewportW: Float,
    private val viewportH: Float
) : Disposable {

    private val batch   = SpriteBatch()
    private val tex: Texture
    private val region: TextureRegion
    private val proj    = Matrix4()
    private val color   = Color(0f, 0f, 0f, 1f)

    private var alpha       = 1f
    private var targetAlpha = 0f
    private var speed       = 2f

    var onFadeOutComplete: (() -> Unit)? = null

    init {
        val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pm.setColor(Color.WHITE); pm.fill()
        tex    = Texture(pm)
        region = TextureRegion(tex)
        pm.dispose()
        proj.setToOrtho2D(0f, 0f, viewportW, viewportH)
    }

    fun fadeIn(speed: Float = 2f) {
        alpha = 1f; targetAlpha = 0f; this.speed = speed
    }

    fun fadeOut(speed: Float = 1f, onComplete: (() -> Unit)? = null) {
        alpha = 0f; targetAlpha = 1f; this.speed = speed
        onFadeOutComplete = onComplete
    }

    fun update(delta: Float) {
        if (alpha < targetAlpha) {
            alpha = (alpha + delta * speed).coerceAtMost(targetAlpha)
            if (alpha >= targetAlpha) onFadeOutComplete?.invoke()
        } else if (alpha > targetAlpha) {
            alpha = (alpha - delta * speed).coerceAtLeast(targetAlpha)
        }
    }

    fun render() {
        if (alpha <= 0.002f) return
        batch.projectionMatrix = proj
        batch.setColor(0f, 0f, 0f, alpha)
        batch.enableBlending()
        batch.begin()
        batch.draw(region, 0f, 0f, viewportW, viewportH)
        batch.end()
        batch.setColor(Color.WHITE)
    }

    override fun dispose() {
        batch.dispose()
        tex.dispose()
    }
}

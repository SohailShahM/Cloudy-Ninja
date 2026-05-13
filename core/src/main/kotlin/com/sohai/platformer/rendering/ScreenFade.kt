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

    var onFadeToBlackComplete: (() -> Unit)? = null

    init {
        val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pm.setColor(Color.WHITE); pm.fill()
        tex    = Texture(pm)
        region = TextureRegion(tex)
        pm.dispose()
        proj.setToOrtho2D(0f, 0f, viewportW, viewportH)
    }

    /**
     * Reveal the gameplay scene by clearing the black overlay.
     *
     * Starts with the screen fully covered (alpha=1f) and lerps alpha DOWN to 0f
     * so the scene becomes visible. Use this when entering a level or scene.
     */
    fun fadeFromBlack(speed: Float = 2f) {
        alpha = 1f; targetAlpha = 0f; this.speed = speed
    }

    /**
     * Cover the gameplay scene with a black overlay.
     *
     * Starts with the screen fully clear (alpha=0f) and lerps alpha UP to 1f
     * so the scene is hidden behind black. Use this when leaving a level,
     * on death, or before a transition. The optional [onComplete] callback
     * fires exactly once when alpha reaches the target.
     */
    fun fadeToBlack(speed: Float = 1f, onComplete: (() -> Unit)? = null) {
        alpha = 0f; targetAlpha = 1f; this.speed = speed
        onFadeToBlackComplete = onComplete
    }

    fun update(delta: Float) {
        if (alpha < targetAlpha) {
            alpha = (alpha + delta * speed).coerceAtMost(targetAlpha)
            if (alpha >= targetAlpha) onFadeToBlackComplete?.invoke()
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

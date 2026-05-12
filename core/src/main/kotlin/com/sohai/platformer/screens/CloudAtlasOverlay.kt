package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.sohai.platformer.Constants
import com.sohai.platformer.FontManager
import com.sohai.platformer.atlas.CloudAtlasEntry
import com.sohai.platformer.i18n.StringKey
import com.sohai.platformer.i18n.Strings

/**
 * Full-screen educational card shown when a Cloud Atlas Snapshot is collected.
 * Caller should check [isDismissed] each frame and dispose when true.
 */
class CloudAtlasOverlay(entry: CloudAtlasEntry, private val onDismiss: () -> Unit) : Disposable {

    private val viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
    val stage = Stage(viewport)
    private val sr = ShapeRenderer()

    private val titleFont = FontManager.getShared(30)
    private val subtitleFont = FontManager.getShared(20)
    private val bodyFont = FontManager.getShared(17)

    var isDismissed = false
        private set

    init {
        val titleStyle    = Label.LabelStyle(titleFont,    Color(0.3f, 1f, 0.85f, 1f))
        val subtitleStyle = Label.LabelStyle(subtitleFont, Color(0.7f, 0.9f, 1f,  1f))
        val bodyStyle     = Label.LabelStyle(bodyFont,     Color.WHITE)
        val hintStyle     = Label.LabelStyle(bodyFont,     Color(0.6f, 0.6f, 0.6f, 1f))

        val table = VisTable()
        table.setFillParent(true)
        table.center()
        table.pad(80f)

        // Header: "Cloud Atlas  •  #character"
        val headerStyle = Label.LabelStyle(subtitleFont, Color(0.4f, 0.8f, 0.5f, 1f))
        table.add(Label("CLOUD ATLAS  •  ${entry.character.uppercase()}", headerStyle))
            .padBottom(16f).row()

        table.add(Label(entry.title, titleStyle)).padBottom(8f).row()
        table.add(Label(entry.subtitle, subtitleStyle)).padBottom(24f).row()

        val bodyLabel = Label(entry.body, bodyStyle)
        bodyLabel.wrap = true
        table.add(bodyLabel).width(700f).padBottom(40f).row()

        val btnDismiss = VisTextButton(Strings.get(StringKey.ATLAS_OVERLAY_GOT_IT))
        btnDismiss.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { dismiss() }
        })
        table.add(btnDismiss).size(180f, 55f).padBottom(10f).row()

        table.add(Label(Strings.get(StringKey.ATLAS_OVERLAY_CLOSE_HINT), hintStyle))

        stage.addActor(table)
    }

    private fun dismiss() {
        isDismissed = true
        onDismiss()
    }

    fun render() {
        // Dark translucent background
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        sr.projectionMatrix = stage.camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.setColor(0.02f, 0.06f, 0.14f, 0.92f)
        sr.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        // Decorative teal border box
        sr.setColor(0.15f, 0.55f, 0.65f, 0.6f)
        val bx = 60f; val by = 80f
        val bw = Constants.VIRTUAL_WIDTH - bx * 2f
        val bh = Constants.VIRTUAL_HEIGHT - by * 2f
        sr.rect(bx, by, bw, 4f)                     // top
        sr.rect(bx, by + bh - 4f, bw, 4f)           // bottom
        sr.rect(bx, by, 4f, bh)                     // left
        sr.rect(bx + bw - 4f, by, 4f, bh)           // right
        sr.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.justTouched()) {
            dismiss()
        }

        stage.act()
        stage.draw()
    }

    fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    override fun dispose() {
        stage.dispose()
        sr.dispose()
        // Fonts are shared (FontManager.getShared); do NOT dispose here.
    }
}

package com.sohai.platformer.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.sohai.platformer.FontManager
import com.sohai.platformer.input.InputManager

class Hud(private val viewportWidth: Float, private val viewportHeight: Float) : Disposable {
    var onSwapCharacter: (() -> Unit)? = null

    val stage: Stage
    private val viewport: Viewport

    // Fonts owned by this Hud — disposed on dispose()
    private val statusFont: BitmapFont = FontManager.create(22)
    private val charFont: BitmapFont = FontManager.create(18)
    private val comboFont: BitmapFont = FontManager.create(28)

    private val statusLabel: VisLabel
    private var statusTimer = 0f
    private val charLabel: VisLabel
    private val spiritLabel: VisLabel
    private val scoreLabel: VisLabel
    private val timerLabel: VisLabel
    private val progressBarBg: VisImage
    private val progressBarFill: VisImage
    private val comboLabel: VisLabel
    private var comboLabelTimer = 0f
    private val cooldownBarImage: VisImage
    private lateinit var btnAction: VisTextButton
    private lateinit var stopwatchLabel: VisLabel
    private var isTrialMode = false

    /** When true, the action button pulses with a warm glow to direct player attention. */
    var showActionHint: Boolean = false
    private var actionHintTimer = 0f

    // Reusable 1×1 white texture for the cooldown bar
    private val whiteTexture: Texture

    init {
        viewport = FitViewport(viewportWidth, viewportHeight)
        stage = Stage(viewport)

        // Minimal skin — only used for the cooldown bar image and labels.
        // Buttons use VisUI styles so they match the main menu.
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).also { it.setColor(Color.WHITE); it.fill() }
        whiteTexture = Texture(pixmap)
        pixmap.dispose()

        val barSkin = Skin()
        barSkin.add("white", whiteTexture)

        // ---------- Left movement buttons ----------
        val btnLeft = VisTextButton("<")
        btnLeft.addListener(object : InputListener() {
            override fun touchDown(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                InputManager.uiLeftPressed = true; return true
            }
            override fun touchUp(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                InputManager.uiLeftPressed = false
            }
        })

        val btnRight = VisTextButton(">")
        btnRight.addListener(object : InputListener() {
            override fun touchDown(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                InputManager.uiRightPressed = true; return true
            }
            override fun touchUp(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                InputManager.uiRightPressed = false
            }
        })

        // ---------- Right action buttons ----------
        val btnJump = VisTextButton("Jump")
        btnJump.addListener(object : InputListener() {
            override fun touchDown(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                InputManager.uiJumpPressed = true; return true
            }
            override fun touchUp(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                InputManager.uiJumpPressed = false
            }
        })

        btnAction = VisTextButton("Action")
        btnAction.addListener(object : InputListener() {
            override fun touchDown(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                InputManager.uiActionPressed = true; InputManager.uiActionJustPressed = true; return true
            }
            override fun touchUp(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                InputManager.uiActionPressed = false
            }
        })

        // ---------- Swap button (centre) ----------
        val btnSwap = VisTextButton("Swap")
        btnSwap.addListener(object : InputListener() {
            override fun touchDown(e: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                onSwapCharacter?.invoke(); return true
            }
        })

        // ---------- Layout ----------
        val table = VisTable()
        table.bottom().padBottom(20f)
        table.setFillParent(true)

        val leftTable = VisTable()
        leftTable.add(btnLeft).size(100f, 100f).padRight(16f)
        leftTable.add(btnRight).size(100f, 100f)

        cooldownBarImage = VisImage(barSkin.newDrawable("white", Color.GREEN))
        val actionColumn = VisTable()
        actionColumn.add(cooldownBarImage).width(120f).height(10f).right()
        actionColumn.row()
        actionColumn.add(btnAction).size(120f, 100f)

        val rightTable = VisTable()
        rightTable.add(btnJump).size(120f, 100f).padRight(16f)
        rightTable.add(actionColumn)

        table.add(leftTable).expandX().left().padLeft(40f)
        table.add(btnSwap).size(100f, 60f)
        table.add(rightTable).expandX().right().padRight(40f)

        stage.addActor(table)

        // ---------- Status label (top-centre, transient) ----------
        val statusStyle = Label.LabelStyle(statusFont, Color.WHITE)
        val statusTable = VisTable()
        statusTable.top().padTop(20f)
        statusTable.setFillParent(true)
        statusLabel = VisLabel("", statusStyle)
        statusLabel.isVisible = false
        statusTable.add(statusLabel)
        stage.addActor(statusTable)

        // ---------- Character / ability + spirit health (top-left) ----------
        val charStyle = Label.LabelStyle(charFont, Color.WHITE)
        val topLeft = VisTable()
        topLeft.top().left().padTop(20f).padLeft(40f)
        topLeft.setFillParent(true)
        charLabel = VisLabel("Ebo — Seed Slam", charStyle)
        topLeft.add(charLabel).row()
        spiritLabel = VisLabel("Spirit: ***", Label.LabelStyle(charFont, Color(0.3f, 1f, 0.4f, 1f)))
        topLeft.add(spiritLabel).left().padTop(4f)
        stage.addActor(topLeft)

        // ---------- Score + timer (top-right) ----------
        val topRight = VisTable()
        topRight.top().right().padTop(20f).padRight(40f)
        topRight.setFillParent(true)
        scoreLabel = VisLabel("Score: 0", Label.LabelStyle(charFont, Color(0.3f, 1f, 0.4f, 1f)))
        timerLabel = VisLabel("0:00", Label.LabelStyle(charFont, Color(0.75f, 0.75f, 1f, 1f)))
        progressBarBg   = VisImage(barSkin.newDrawable("white", Color(0.2f, 0.2f, 0.2f, 0.6f)))
        progressBarFill = VisImage(barSkin.newDrawable("white", Color(0.3f, 1f, 0.4f, 0.9f)))

        topRight.add(scoreLabel).right().row()
        topRight.add(timerLabel).right().padTop(4f).row()
        // Eco-token progress bar: fixed 120px wide, 6px tall
        val barContainer = com.badlogic.gdx.scenes.scene2d.ui.Stack()
        progressBarBg.setSize(120f, 6f)
        progressBarFill.setSize(0f, 6f)
        barContainer.add(progressBarBg)
        barContainer.add(progressBarFill)
        topRight.add(barContainer).width(120f).height(6f).right().padTop(6f)
        stage.addActor(topRight)

        // ---------- Stopwatch (top-centre, time trial only) ----------
        val stopwatchFont = FontManager.getShared(34)
        val stopwatchTable = VisTable()
        stopwatchTable.top().padTop(16f)
        stopwatchTable.setFillParent(true)
        stopwatchLabel = VisLabel("⏱ 0:00.0", Label.LabelStyle(stopwatchFont, Color(0.1f, 0.95f, 0.85f, 1f)))
        stopwatchLabel.isVisible = false
        stopwatchTable.add(stopwatchLabel)
        stage.addActor(stopwatchTable)

        // ---------- Combo label (centre, transient) ----------
        val comboStyle = Label.LabelStyle(comboFont, Color(1f, 0.85f, 0.1f, 1f))
        val comboTable = VisTable()
        comboTable.center().padTop(60f)
        comboTable.setFillParent(true)
        comboLabel = VisLabel("", comboStyle)
        comboLabel.isVisible = false
        comboTable.add(comboLabel)
        stage.addActor(comboTable)
    }

    fun setTimeTrial(enabled: Boolean) {
        isTrialMode = enabled
        stopwatchLabel.isVisible = enabled
        // In trial mode dim the regular timer so the stopwatch dominates
        timerLabel.color = if (enabled) Color(0.5f, 0.5f, 0.5f, 0.5f) else Color(0.75f, 0.75f, 1f, 1f)
    }

    fun updateStopwatch(seconds: Float) {
        val mins  = (seconds / 60f).toInt()
        val secs  = (seconds % 60f).toInt()
        val tenth = ((seconds % 1f) * 10f).toInt()
        stopwatchLabel.setText("⏱ %d:%02d.%d".format(mins, secs, tenth))
    }

    fun showTransientMessage(message: String, durationSeconds: Float = 1f) {
        statusLabel.setText(message)
        statusLabel.isVisible = true
        statusTimer = durationSeconds
    }

    fun showCombo(multiplier: Int) {
        comboLabel.setText("x$multiplier COMBO!")
        comboLabel.isVisible = true
        comboLabelTimer = 1.2f
    }

    fun update(delta: Float) {
        if (statusTimer > 0f) {
            statusTimer -= delta
            if (statusTimer <= 0f) statusLabel.isVisible = false
        }
        if (comboLabelTimer > 0f) {
            comboLabelTimer -= delta
            if (comboLabelTimer <= 0f) comboLabel.isVisible = false
        }
        if (showActionHint) {
            actionHintTimer += delta
            // Pulse at 1.5 Hz between a muted white and a warm orange
            val pulse = MathUtils.sin(actionHintTimer * MathUtils.PI2 * 1.5f) * 0.5f + 0.5f  // 0..1
            btnAction.color.set(1f, 0.55f + 0.45f * pulse, 0.1f + 0.2f * pulse, 1f)
        } else if (actionHintTimer != 0f) {
            btnAction.color.set(Color.WHITE)
            actionHintTimer = 0f
        }
    }

    fun updateScore(score: Int) {
        scoreLabel.setText("Score: $score")
    }

    fun updateTimer(seconds: Float) {
        val mins = (seconds / 60f).toInt()
        val secs = (seconds % 60f).toInt()
        timerLabel.setText("%d:%02d".format(mins, secs))
    }

    /** collected / total eco-tokens, 0..1 */
    fun updateProgress(ratio: Float) {
        val clamped = ratio.coerceIn(0f, 1f)
        progressBarFill.width = 120f * clamped
        progressBarFill.color = when {
            clamped >= 1f  -> Color(0.1f, 0.9f, 0.9f, 1f)   // cyan = all done
            clamped > 0.5f -> Color(0.3f, 1.0f, 0.4f, 0.9f)  // green
            else           -> Color(0.9f, 0.85f, 0.2f, 0.9f)  // yellow
        }
    }

    fun updateSpiritHealth(lives: Int, maxLives: Int = 3) {
        val pips = "*".repeat(lives.coerceAtLeast(0)) + "-".repeat((maxLives - lives).coerceAtLeast(0))
        spiritLabel.setText("Spirit: $pips")
        spiritLabel.color = when {
            lives >= maxLives -> Color(0.3f, 1f, 0.4f, 1f)
            lives == 2        -> Color(1f, 0.85f, 0.1f, 1f)
            lives == 1        -> Color(1f, 0.4f, 0.1f, 1f)
            else              -> Color(0.5f, 0.5f, 0.5f, 1f)
        }
    }

    fun updateAbilityState(cooldownRatio: Float, characterName: String, abilityName: String) {
        charLabel.setText("$characterName — $abilityName")
        cooldownBarImage.width = 120f * cooldownRatio
        cooldownBarImage.color = when {
            cooldownRatio > 0.9f -> Color.RED
            cooldownRatio > 0.1f -> Color(1f, 0.6f, 0f, 1f)
            else -> Color.GREEN
        }
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        whiteTexture.dispose()
        statusFont.dispose()
        charFont.dispose()
        comboFont.dispose()
    }
}

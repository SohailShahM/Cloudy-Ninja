package com.sohai.platformer.rendering

/**
 * T-176 dynamic camera zoom-out helper.
 *
 * When the player's vertical position approaches (or exceeds) the top edge
 * of the camera viewport — typically because Laya just fired her Wind Dash
 * or another character cleared the room with a strong jump — the camera
 * smoothly zooms out so the landing point stays on-screen. Pure function +
 * constants here; the actual `camera.viewportHeight` write lives in
 * [com.sohai.platformer.screens.LevelRenderer.renderWorld] and composes with
 * the T-116 [ScreenShake] offset and the T-144 [CameraLookAhead] offset —
 * the three are independent components of the camera rig.
 *
 * Character-agnostic by design: any character who clears the top of the
 * viewport gets the zoom-out, not just Laya. Per ticket: "if Ebo's
 * double-jump ever exceeds the viewport, the camera should zoom out for
 * him too. Don't gate on `currentCharacter == \"Laya\"`."
 *
 * The lerp factor matches [CameraLookAhead.LERP_FACTOR] (0.15) so all three
 * camera composers feel like one coherent rig — reaches ~99% of the new
 * zoom level in ~30 frames (~0.5 s at 60 Hz), with the first ~15% (≈0.15 s)
 * covering most of the perceptual shift.
 */
object DynamicZoom {
    /**
     * Maximum camera zoom-out multiplier. 1.4× covers the mid-Wind-Dash
     * apex with margin to spare — chosen to cap so a rapid-fire dash +
     * wall-jump combo can't keep growing the viewport (beyond the cap, the
     * player has to come back down). Ticket spec.
     */
    const val ZOOM_MAX: Float = 1.4f

    /**
     * Per-frame lerp factor for the zoom multiplier. Matches T-144
     * [CameraLookAhead.LERP_FACTOR] = 0.15 so the camera rig feels coherent.
     */
    const val ZOOM_LERP_FACTOR: Float = 0.15f

    /**
     * Trigger band: when the player's Y is within this fraction of the
     * top-edge-to-centre distance of the viewport, the zoom target climbs
     * toward [ZOOM_MAX]. 0.10 = "within 10% of the top edge", per the T-176
     * ticket spec. Once the player passes the top edge, the target
     * saturates at [ZOOM_MAX].
     */
    const val ZOOM_TRIGGER_BAND: Float = 0.10f

    /** Snap-to-1 epsilon to avoid floating residuals dragging across frames. */
    const val ZOOM_SNAP_EPSILON: Float = 1e-4f

    /**
     * Pure-function zoom target.
     *
     * Returns 1.0 when the player is well inside the viewport (clearly
     * below the top edge). As the player rises into the trigger band
     * (within [ZOOM_TRIGGER_BAND] of the top edge), the target lerps
     * linearly from 1.0 toward [ZOOM_MAX]. Once the player passes the top
     * edge, the target saturates at [ZOOM_MAX].
     *
     * Extracted so Kotest can verify the curve in isolation without
     * instantiating the surrounding [com.sohai.platformer.screens.LevelRenderer]
     * (which needs an OpenGL context). The caller lerps a stored
     * `currentZoomMultiplier` toward this target each frame at
     * [ZOOM_LERP_FACTOR].
     *
     * Contract:
     *   - `playerY <= viewportTopY - band` → 1.0
     *   - `playerY` inside the band → linear interpolation 1.0..ZOOM_MAX
     *   - `playerY >= viewportTopY`       → ZOOM_MAX (saturated)
     *
     * @param playerY       player body Y in world meters
     * @param viewportTopY  baseline top-edge Y of the camera viewport
     *                      (`camera.position.y + baseViewportHeight / 2`)
     * @param baseHeight    baseline `camera.viewportHeight` (pre-zoom)
     */
    fun computeZoomTarget(playerY: Float, viewportTopY: Float, baseHeight: Float): Float {
        // Band width = ZOOM_TRIGGER_BAND × (half-viewport). Use the
        // half-viewport so the trigger scales with the camera regardless
        // of which level we're in (different viewports → proportional
        // trigger distances).
        val bandHeight = (baseHeight / 2f) * ZOOM_TRIGGER_BAND
        val bandBottom = viewportTopY - bandHeight
        return when {
            playerY <= bandBottom -> 1f
            playerY >= viewportTopY -> ZOOM_MAX
            else -> {
                val t = (playerY - bandBottom) / bandHeight
                1f + t * (ZOOM_MAX - 1f)
            }
        }
    }

    /**
     * Pure-function one-step lerp. Same math the renderer does each frame:
     * `current += (target - current) * ZOOM_LERP_FACTOR`. Exposed so tests
     * can verify the lerp curve verbatim.
     */
    fun lerpStep(currentMultiplier: Float, targetMultiplier: Float): Float =
        currentMultiplier + (targetMultiplier - currentMultiplier) * ZOOM_LERP_FACTOR
}

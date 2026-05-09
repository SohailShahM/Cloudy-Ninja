package com.sohai.platformer.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.EllipseMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.physics.box2d.World
import com.sohai.platformer.Constants
import com.sohai.platformer.entities.MovingPlatform

/**
 * Loads a Tiled TMX map into the physics world via ObstacleManager.
 *
 * Layer name contract: ground | walls | hazards | checkpoints | moving_platforms
 *
 * flipY=true  → standard Tiled y-down coordinates (flip to match Box2D y-up).
 * flipY=false → TMX was authored in y-up virtual-pixel space; use y values as-is.
 *               This is the convention used by the Cloudy Ninja level TMX files.
 */
object MapLevelLoader {
    fun load(
        tmxPath: String,
        manager: ObstacleManager,
        movingPlatforms: MutableList<MovingPlatform>,
        world: World,
        flipY: Boolean = true
    ) {
        if (!Gdx.files.internal(tmxPath).exists()) {
            Gdx.app.error("MapLevelLoader", "TMX not found: $tmxPath")
            return
        }

        val map = TmxMapLoader().load(tmxPath)
        val mapHeightPixels: Int = if (flipY) {
            map.properties.get("height", Int::class.java) * map.properties.get("tileheight", Int::class.java)
        } else {
            0
        }

        fun centerOf(obj: RectangleMapObject): Pair<Float, Float> {
            val r = obj.rectangle
            val cx = r.x + r.width / 2f
            val cy = if (flipY) {
                (mapHeightPixels - r.y - r.height) + r.height / 2f
            } else {
                r.y + r.height / 2f
            }
            return cx to cy
        }

        fun addRect(id: String, kind: ObstacleKind, obj: MapObject) {
            val rectObj = obj as? RectangleMapObject ?: return
            val (cx, cy) = centerOf(rectObj)
            val hw = rectObj.rectangle.width / 2f
            val hh = rectObj.rectangle.height / 2f
            manager.addRectNormalized(
                id, kind,
                cx / Constants.VIRTUAL_WIDTH,
                cy / Constants.VIRTUAL_HEIGHT,
                hw / Constants.VIRTUAL_WIDTH,
                hh / Constants.VIRTUAL_HEIGHT
            )
        }

        // Ground
        map.layers.get("ground")?.objects?.forEachIndexed { i, obj ->
            val id = obj.name?.takeIf { it.isNotEmpty() } ?: "ground_$i"
            addRect(id, ObstacleKind.GROUND, obj)
        }

        // Walls
        map.layers.get("walls")?.objects?.forEachIndexed { i, obj ->
            val id = obj.name?.takeIf { it.isNotEmpty() } ?: "wall_$i"
            addRect(id, ObstacleKind.WALL, obj)
        }

        // Hazards
        map.layers.get("hazards")?.objects?.forEachIndexed { i, obj ->
            val id = obj.name?.takeIf { it.isNotEmpty() } ?: "hazard_$i"
            addRect(id, ObstacleKind.HAZARD, obj)
        }

        // Checkpoints — ellipse or rect both supported
        map.layers.get("checkpoints")?.objects?.forEachIndexed { i, obj ->
            val id = obj.name?.takeIf { it.isNotEmpty() } ?: "checkpoint_$i"
            when (obj) {
                is EllipseMapObject -> {
                    val e = obj.ellipse
                    val cx = e.x + e.width / 2f
                    val rawY = e.y + e.height / 2f
                    val cy = if (flipY) mapHeightPixels - e.y - e.height + e.height / 2f else rawY
                    val radius = maxOf(e.width, e.height) / 2f
                    manager.addCheckpointNormalized(id, cx / Constants.VIRTUAL_WIDTH, cy / Constants.VIRTUAL_HEIGHT, radius / Constants.VIRTUAL_WIDTH)
                }
                is RectangleMapObject -> {
                    val (cx, cy) = centerOf(obj)
                    val radius = maxOf(obj.rectangle.width, obj.rectangle.height) / 2f
                    manager.addCheckpointNormalized(id, cx / Constants.VIRTUAL_WIDTH, cy / Constants.VIRTUAL_HEIGHT, radius / Constants.VIRTUAL_WIDTH)
                }
            }
        }

        // Moving platforms
        map.layers.get("moving_platforms")?.objects?.forEach { obj ->
            val rectObj = obj as? RectangleMapObject ?: return@forEach
            val (startX, startY) = centerOf(rectObj)
            val props = obj.properties
            val pW = rectObj.rectangle.width
            val pH = rectObj.rectangle.height
            // endX/endY in the TMX are the top-left corner of the end position (Tiled convention).
            // Convert to center the same way centerOf() does for rectangles.
            val rawEndX = (props.get("endX") as? Float) ?: (props.get("endx") as? Float)
            val rawEndY = (props.get("endY") as? Float) ?: (props.get("endy") as? Float)
            val endX = if (rawEndX != null) rawEndX + pW / 2f else startX + 200f
            val endY = when {
                rawEndY == null -> startY
                flipY           -> rawEndY + pH / 2f
                else            -> rawEndY
            }
            val speed = (props.get("speed") as? Float) ?: 2f
            movingPlatforms.add(MovingPlatform(world, startX, startY, endX, endY, speed))
        }

        map.dispose()
    }
}

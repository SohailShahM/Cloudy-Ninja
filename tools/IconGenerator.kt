/*
 * T-078: Procedural achievement icon generator.
 *
 * Generates 16x16 PNG icons for every achievement listed in AchievementRegistry,
 * using only the JDK standard library (BufferedImage + Graphics2D + ImageIO).
 *
 * Invocation:
 *   ./gradlew :core:generateAchievementIcons          (wired in core/build.gradle)
 *   kotlinc -script is NOT used; this is a JVM main().
 *
 * Output:
 *   <repo>/assets/icons/achievements/<achievement_id>.png   (12 files, 16x16)
 *
 * Idempotence:
 *   - Achievements are processed in a fixed, hard-coded order.
 *   - Antialiasing is explicitly disabled and we only fill integer-aligned shapes,
 *     so rasterization is deterministic across runs.
 *   - No timestamps, no RNG, no environment-dependent state are written into the
 *     image. Re-running the generator MUST produce byte-identical PNGs.
 *
 * The achievement ID list here is intentionally a stand-alone copy of the IDs in
 * AchievementRegistry.kt rather than a runtime dep on the libGDX-flavored core
 * module — this keeps the tool runnable from a plain `kotlinc + java` invocation
 * with zero extra classpath setup. The list is asserted equal in a unit test
 * (IconGeneratorCoverageTest) so drift is caught at CI time.
 */
@file:JvmName("IconGenerator")

package com.sohai.platformer.tools

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private const val SIZE = 16

// Shared palette — chosen for legibility at 16x16.
private val BG = Color(40, 60, 90)            // dark blue-grey background
private val FG = Color(230, 220, 200)         // off-white silhouette
private val ACCENT = Color(250, 200, 100)     // gold accent (special icons)
private val DANGER = Color(200, 80, 80)       // red accent (defeat / X-eye)
private val LEAF = Color(140, 200, 120)       // green accent (seed / eco)

// Canonical ordered list of achievement IDs (mirrors AchievementRegistry.ALL order).
// Keep this in sync with core/src/main/kotlin/.../progression/AchievementRegistry.kt.
val ACHIEVEMENT_IDS: List<String> = listOf(
    "first_jump",
    "first_cleanse",
    "eco_sweep",
    "no_death_run",
    "speed_demon",
    "atlas_half",
    "atlas_full",
    "first_enemy",
    "stomp_10",
    "boss_defeated",
    "world_1_clear",
    "all_clear",
    // T-107: hidden eco-token meta-achievement
    "collector"
)

fun main(args: Array<String>) {
    val outDir = File(if (args.isNotEmpty()) args[0] else "assets/icons/achievements")
    if (!outDir.exists() && !outDir.mkdirs()) {
        error("Could not create output directory: ${outDir.absolutePath}")
    }
    for (id in ACHIEVEMENT_IDS) {
        val img = renderIcon(id)
        val outFile = File(outDir, "$id.png")
        ImageIO.write(img, "png", outFile)
        println("wrote ${outFile.path} (${img.width}x${img.height})")
    }
    println("done: ${ACHIEVEMENT_IDS.size} icons -> ${outDir.absolutePath}")
}

/** Build a 16x16 icon for [id]. Throws if the ID has no draw routine. */
fun renderIcon(id: String): BufferedImage {
    val img = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    try {
        // Pixel-art crispness — no AA, no fancy interpolation.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.stroke = BasicStroke(1f)

        // Background fill.
        g.color = BG
        g.fillRect(0, 0, SIZE, SIZE)

        when (id) {
            "first_jump"     -> drawUpArrow(g)
            "first_cleanse"  -> drawSprout(g)
            "eco_sweep"      -> drawLeaf(g)
            "no_death_run"   -> drawGhost(g)
            "speed_demon"    -> drawLightning(g)
            "atlas_half"     -> drawBook(g, open = false)
            "atlas_full"     -> drawBook(g, open = true)
            "first_enemy"    -> drawEnemyFace(g)
            "stomp_10"       -> drawBoot(g)
            "boss_defeated"  -> drawCrown(g)
            "world_1_clear"  -> drawShieldWithOne(g)
            "all_clear"      -> drawTrophy(g)
            "collector"      -> drawGoldenTokenTrio(g)
            else -> error("No draw routine for achievement id: $id")
        }
    } finally {
        g.dispose()
    }
    return img
}

// ---------- Individual silhouettes ----------
// All draws use integer pixel rects + polygons; no AA, no sub-pixel positions.

/** Upward chevron arrow centered horizontally. */
private fun drawUpArrow(g: Graphics2D) {
    g.color = FG
    // Arrow head (triangle pointing up).
    g.fillPolygon(intArrayOf(8, 3, 13), intArrayOf(3, 8, 8), 3)
    // Arrow shaft.
    g.fillRect(6, 8, 5, 6)
    // Ground line.
    g.fillRect(2, 14, 12, 1)
}

/** Small seedling: stem + two leaves. */
private fun drawSprout(g: Graphics2D) {
    // Soil line.
    g.color = Color(120, 90, 60)
    g.fillRect(2, 13, 12, 2)
    // Stem.
    g.color = LEAF
    g.fillRect(8, 6, 1, 7)
    // Left leaf.
    g.fillPolygon(intArrayOf(8, 3, 8), intArrayOf(8, 7, 6), 3)
    // Right leaf.
    g.fillPolygon(intArrayOf(8, 13, 8), intArrayOf(8, 5, 4), 3)
}

/** Stylized leaf with central vein. */
private fun drawLeaf(g: Graphics2D) {
    g.color = LEAF
    // Leaf body (diamond-ish shape via two triangles).
    g.fillPolygon(intArrayOf(8, 13, 8, 3), intArrayOf(2, 8, 14, 8), 4)
    // Vein.
    g.color = BG
    g.fillRect(8, 3, 1, 11)
}

/** Ghost: dome head + scalloped bottom + two eyes. */
private fun drawGhost(g: Graphics2D) {
    g.color = FG
    // Body block.
    g.fillRect(3, 5, 10, 9)
    // Rounded top — fake by trimming corners.
    g.color = BG
    g.fillRect(3, 5, 1, 2)
    g.fillRect(12, 5, 1, 2)
    // Bottom scallops (three little notches).
    g.fillRect(4, 13, 1, 1)
    g.fillRect(8, 13, 1, 1)
    g.fillRect(11, 13, 1, 1)
    // Eyes.
    g.fillRect(6, 8, 1, 2)
    g.fillRect(10, 8, 1, 2)
}

/** Classic lightning bolt zig-zag. */
private fun drawLightning(g: Graphics2D) {
    g.color = ACCENT
    // Top wedge.
    g.fillPolygon(
        intArrayOf(10, 4, 9, 6, 12, 7),
        intArrayOf(2, 9, 9, 14, 7, 7),
        6
    )
}

/**
 * Book silhouette.
 * - open = true  -> both halves visible with V crease in the middle.
 * - open = false -> half-open: one full page, other page hinted as a thin spine.
 */
private fun drawBook(g: Graphics2D, open: Boolean) {
    g.color = FG
    if (open) {
        // Left page.
        g.fillRect(2, 4, 6, 9)
        // Right page.
        g.fillRect(9, 4, 6, 9)
        // Spine gap stays as BG between x=8.
        // Page lines (accent).
        g.color = ACCENT
        g.fillRect(3, 6, 4, 1)
        g.fillRect(3, 9, 4, 1)
        g.fillRect(10, 6, 4, 1)
        g.fillRect(10, 9, 4, 1)
        // Base shelf line.
        g.color = FG
        g.fillRect(2, 13, 13, 1)
    } else {
        // Half-open: only the right page is full, left side is the closed spine.
        g.fillRect(8, 4, 6, 9)
        // Spine (thin strip).
        g.fillRect(6, 5, 1, 8)
        // Page lines.
        g.color = ACCENT
        g.fillRect(9, 6, 4, 1)
        g.fillRect(9, 9, 4, 1)
        // Shelf.
        g.color = FG
        g.fillRect(5, 13, 9, 1)
    }
}

/** Enemy face: rounded square + two X eyes. */
private fun drawEnemyFace(g: Graphics2D) {
    g.color = FG
    g.fillRect(3, 3, 10, 10)
    // Trim corners to suggest a round-ish blob.
    g.color = BG
    g.fillRect(3, 3, 1, 1); g.fillRect(12, 3, 1, 1)
    g.fillRect(3, 12, 1, 1); g.fillRect(12, 12, 1, 1)
    // Eyes — Xs in danger color.
    g.color = DANGER
    // Left X
    g.fillRect(5, 6, 1, 1); g.fillRect(6, 7, 1, 1); g.fillRect(5, 8, 1, 1)
    g.fillRect(7, 6, 1, 1); g.fillRect(7, 8, 1, 1)
    // Right X
    g.fillRect(9, 6, 1, 1); g.fillRect(10, 7, 1, 1); g.fillRect(9, 8, 1, 1)
    g.fillRect(11, 6, 1, 1); g.fillRect(11, 8, 1, 1)
    // Frown.
    g.fillRect(6, 11, 5, 1)
}

/** Side-view boot (hiking boot silhouette). */
private fun drawBoot(g: Graphics2D) {
    g.color = FG
    // Shaft of the boot.
    g.fillRect(5, 3, 4, 7)
    // Foot (extends forward).
    g.fillRect(5, 10, 9, 2)
    // Sole (slightly thicker, accent color).
    g.color = ACCENT
    g.fillRect(4, 12, 11, 1)
    // Heel.
    g.color = FG
    g.fillRect(5, 13, 2, 1)
    g.fillRect(12, 13, 2, 1)
    // Lace dots.
    g.color = ACCENT
    g.fillRect(6, 5, 1, 1)
    g.fillRect(6, 7, 1, 1)
}

/** Five-point crown. */
private fun drawCrown(g: Graphics2D) {
    g.color = ACCENT
    // Crown base.
    g.fillRect(2, 9, 12, 3)
    // Three spikes.
    g.fillPolygon(intArrayOf(2, 4, 6), intArrayOf(9, 3, 9), 3)
    g.fillPolygon(intArrayOf(5, 8, 11), intArrayOf(9, 2, 9), 3)
    g.fillPolygon(intArrayOf(10, 12, 14), intArrayOf(9, 3, 9), 3)
    // Gems on spikes.
    g.color = DANGER
    g.fillRect(4, 4, 1, 1)
    g.fillRect(8, 3, 1, 1)
    g.fillRect(12, 4, 1, 1)
    // Velvet under crown.
    g.color = FG
    g.fillRect(2, 12, 12, 1)
}

/** Shield silhouette with a "1" digit etched in BG color. */
private fun drawShieldWithOne(g: Graphics2D) {
    g.color = FG
    // Shield outline drawn as a tapered polygon.
    g.fillPolygon(
        intArrayOf(3, 12, 13, 12, 8, 3, 2),
        intArrayOf(3,  3, 6, 11, 14, 11, 6),
        7
    )
    // Inner border (accent rim).
    g.color = ACCENT
    g.fillRect(4, 4, 8, 1)
    // Digit "1" in the middle, drawn pixel-by-pixel in BG color.
    g.color = BG
    // Top hook
    g.fillRect(7, 6, 1, 1)
    g.fillRect(8, 6, 1, 1)
    // Vertical stem
    g.fillRect(8, 6, 1, 5)
    // Base
    g.fillRect(7, 10, 3, 1)
}

/** Trophy with two handles + base. */
private fun drawTrophy(g: Graphics2D) {
    g.color = ACCENT
    // Cup body.
    g.fillRect(5, 3, 6, 6)
    // Handles.
    g.fillRect(3, 4, 2, 3)
    g.fillRect(11, 4, 2, 3)
    // Stem.
    g.fillRect(7, 9, 2, 2)
    // Base.
    g.fillRect(4, 11, 8, 2)
    g.fillRect(3, 13, 10, 1)
    // Highlight on cup.
    g.color = FG
    g.fillRect(6, 4, 1, 3)
}

/**
 * T-107: three golden eco-tokens arranged in a small triangle — one per
 * campaign level. Drawn with the gold ACCENT colour to match the in-game
 * hidden-token render path. Highlight pip on each token gives it a coin-y
 * "shiny" feel at 16x16.
 */
private fun drawGoldenTokenTrio(g: Graphics2D) {
    g.color = ACCENT
    // Token 1: top-center.
    g.fillRect(6, 2, 4, 4)
    // Token 2: bottom-left.
    g.fillRect(2, 9, 4, 4)
    // Token 3: bottom-right.
    g.fillRect(10, 9, 4, 4)
    // Coin highlight pips (one off-white pixel per token for that "shine").
    g.color = FG
    g.fillRect(7, 3, 1, 1)
    g.fillRect(3, 10, 1, 1)
    g.fillRect(11, 10, 1, 1)
}

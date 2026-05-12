// run via `kotlinc -script _gen_placeholders.kt` to regenerate
//
// Generates placeholder PNG assets for the Cloudy Ninja press kit using
// only the JDK's java.awt + javax.imageio. No third-party deps.
//
// Outputs (alongside this script):
//   cover.png          1920x1080  solid #1a2a3e + centered "COVER"
//   screenshot-01.png  1920x1080  solid #3e3e3e + centered "SCREENSHOT 1"
//   screenshot-02..06.png         same, numbered
//   logo.png           512x512    transparent + centered "CLOUDY NINJA"
//   icon.png           256x256    solid #1a2a3e + centered "CN"
//
// These are sticky placeholders — they live in the repo until real assets
// arrive post-alpha. Re-run this script if dimensions ever need to change.

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val OUT_DIR: File = File(System.getProperty("user.dir"))
    .resolve("marketing/presskit")
    .takeIf { it.isDirectory }
    ?: File(".") // fallback: assume cwd is already marketing/presskit

private fun makePng(
    width: Int,
    height: Int,
    bg: Color?,                 // null = transparent
    text: String,
    textColor: Color = Color.WHITE,
    outName: String,
) {
    val type = if (bg == null) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    val img = BufferedImage(width, height, type)
    val g = img.createGraphics()
    try {
        // Background
        if (bg == null) {
            g.composite = AlphaComposite.Clear
            g.fillRect(0, 0, width, height)
            g.composite = AlphaComposite.SrcOver
        } else {
            g.color = bg
            g.fillRect(0, 0, width, height)
        }

        // Text — size scales with shortest side so it reads at any dimension
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        )
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )
        val fontSize = (minOf(width, height) / 10f).coerceAtLeast(24f)
        g.font = Font("SansSerif", Font.BOLD, fontSize.toInt())
        g.color = textColor
        val fm = g.fontMetrics
        val tx = (width - fm.stringWidth(text)) / 2
        val ty = (height - fm.height) / 2 + fm.ascent
        g.drawString(text, tx, ty)
    } finally {
        g.dispose()
    }

    val outFile = File(OUT_DIR, outName)
    outFile.parentFile?.mkdirs()
    ImageIO.write(img, "png", outFile)
    println("wrote ${outFile.path} (${width}x${height})")
}

// Cover — 1920x1080, ninja-blue
makePng(
    width = 1920, height = 1080,
    bg = Color(0x1a, 0x2a, 0x3e),
    text = "COVER",
    outName = "cover.png",
)

// Screenshots 1..6 — 1920x1080, mid-grey
for (i in 1..6) {
    makePng(
        width = 1920, height = 1080,
        bg = Color(0x3e, 0x3e, 0x3e),
        text = "SCREENSHOT $i",
        outName = "screenshot-%02d.png".format(i),
    )
}

// Logo — 512x512, transparent
makePng(
    width = 512, height = 512,
    bg = null,
    text = "CLOUDY NINJA",
    textColor = Color(0xfa, 0xfa, 0xfa),
    outName = "logo.png",
)

// Icon — 256x256, ninja-blue, "CN"
makePng(
    width = 256, height = 256,
    bg = Color(0x1a, 0x2a, 0x3e),
    text = "CN",
    outName = "icon.png",
)

println("done.")

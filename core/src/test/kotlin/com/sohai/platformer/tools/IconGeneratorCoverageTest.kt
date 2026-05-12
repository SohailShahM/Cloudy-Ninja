package com.sohai.platformer.tools

import com.sohai.platformer.progression.AchievementRegistry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Coverage + determinism contract for the procedural achievement icon generator
 * (tools/IconGenerator.kt, T-078).
 *
 * 1. Every achievement in AchievementRegistry MUST have a corresponding icon ID in
 *    ACHIEVEMENT_IDS, and vice versa — drift between metadata and icons is caught
 *    at CI time rather than ship time.
 *
 * 2. Every icon renders successfully at exactly 16x16.
 *
 * 3. Two back-to-back renders of the same icon ID produce byte-identical PNG output,
 *    which is the load-bearing property for "running the generator twice is a no-op
 *    in git diff" per T-078 hard rules.
 */
class IconGeneratorCoverageTest : BehaviorSpec({

    given("ACHIEVEMENT_IDS in IconGenerator") {
        `when`("compared to AchievementRegistry.ALL") {
            then("they cover the same set of IDs in the same order") {
                val registryIds = AchievementRegistry.ALL.map { it.id }
                // Set equality — order is intentional but not strictly required by callers.
                ACHIEVEMENT_IDS shouldContainExactlyInAnyOrder registryIds
                // And the hard-coded ordering matches registry order, which is the
                // form the generator iterates in.
                ACHIEVEMENT_IDS shouldBe registryIds
            }
        }
    }

    given("renderIcon") {
        `when`("invoked for every known achievement ID") {
            then("each output is exactly 16x16") {
                ACHIEVEMENT_IDS.forEach { id ->
                    val img: BufferedImage = renderIcon(id)
                    img.width shouldBe 16
                    img.height shouldBe 16
                }
            }
            then("re-rendering the same ID produces byte-identical PNG bytes") {
                ACHIEVEMENT_IDS.forEach { id ->
                    val a = encodePng(renderIcon(id))
                    val b = encodePng(renderIcon(id))
                    a.contentEquals(b) shouldBe true
                }
            }
        }
    }
})

private fun encodePng(img: BufferedImage): ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(img, "png", baos)
    return baos.toByteArray()
}

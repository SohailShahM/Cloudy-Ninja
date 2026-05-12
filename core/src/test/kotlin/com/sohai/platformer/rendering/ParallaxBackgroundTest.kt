package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.sohai.platformer.Constants
import com.sohai.platformer.persist.Settings
import com.sohai.platformer.persist.SettingsManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

// ── Top-level helpers (declared outside the spec lambda so Kotlin allows
//    them to contain nested data classes / member fields). ───────────────────

private data class TriCall(
    val color: Color, val x1: Float, val y1: Float,
    val x2: Float, val y2: Float, val x3: Float, val y3: Float
)
private data class RectCall(
    val color: Color, val x: Float, val y: Float, val w: Float, val h: Float
)
private data class CircleCall(
    val color: Color, val x: Float, val y: Float,
    val radius: Float, val segments: Int
)

/**
 * Recorder that captures every triangle/rect/circle call against a mocked
 * [ShapeRenderer] along with the colour active at the time of the call
 * (deep-copied — production code reuses a shared `tmp` Color).
 */
private class Recorder {
    val triangles = mutableListOf<TriCall>()
    val rects = mutableListOf<RectCall>()
    val circles = mutableListOf<CircleCall>()
    private var currentColor: Color = Color(1f, 1f, 1f, 1f)

    fun newMockSr(): ShapeRenderer {
        val sr = mockk<ShapeRenderer>(relaxed = true)
        every { sr.color = any() } answers {
            currentColor = Color(firstArg<Color>())  // snapshot
        }
        every { sr.color } answers { currentColor }
        every { sr.rect(any(), any(), any(), any()) } answers {
            rects += RectCall(Color(currentColor),
                firstArg(), secondArg(), thirdArg(), arg(3))
        }
        every { sr.triangle(any(), any(), any(), any(), any(), any()) } answers {
            triangles += TriCall(Color(currentColor),
                firstArg(), secondArg(), thirdArg(),
                arg(3), arg(4), arg(5))
        }
        every { sr.circle(any(), any(), any(), any()) } answers {
            circles += CircleCall(Color(currentColor),
                firstArg(), secondArg(), thirdArg(), arg(3))
        }
        return sr
    }
}

/** Seed SettingsManager's cached Settings so load() never touches Gdx.files. */
private fun seedSettings(reducedMotion: Boolean) {
    val cachedField = SettingsManager::class.java.getDeclaredField("cached")
    cachedField.isAccessible = true
    cachedField.set(SettingsManager, Settings(reducedMotion = reducedMotion))
}

private fun clearSettingsCache() { SettingsManager.resetCacheForTest() }

/** Build a non-throwing OrthographicCamera (pure-Java, no GL required). */
private fun camera(camX: Float, camY: Float = 4f): OrthographicCamera {
    val cam = OrthographicCamera()
    cam.viewportWidth  = 12.8f    // 1280 px / PPM=100 → 12.8 m
    cam.viewportHeight = 7.2f     //  720 px / PPM=100 →  7.2 m
    cam.position.set(camX, camY, 0f)
    return cam
}

/** Read the private layers list's scrollFactor values. */
private fun scrollFactors(bg: ParallaxBackground): List<Float> {
    val layersField = ParallaxBackground::class.java.getDeclaredField("layers")
    layersField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val layers = layersField.get(bg) as List<Any>
    return layers.map { layer ->
        val f = layer.javaClass.getDeclaredField("scrollFactor")
        f.isAccessible = true
        f.getFloat(layer)
    }
}

/** Read the private layers list's patternWidthPx values. */
private fun patternWidthsPx(bg: ParallaxBackground): List<Float> {
    val layersField = ParallaxBackground::class.java.getDeclaredField("layers")
    layersField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val layers = layersField.get(bg) as List<Any>
    return layers.map { layer ->
        val f = layer.javaClass.getDeclaredField("patternWidthPx")
        f.isAccessible = true
        f.getFloat(layer)
    }
}

/** Read the private palette field. */
private fun palette(bg: ParallaxBackground): Any {
    val f = ParallaxBackground::class.java.getDeclaredField("palette")
    f.isAccessible = true
    return f.get(bg)!!
}

/** Pull a Color field by name from a Palette instance via reflection. */
private fun paletteColor(pal: Any, name: String): Color {
    val f = pal.javaClass.getDeclaredField(name)
    f.isAccessible = true
    return f.get(pal) as Color
}

/**
 * Collect every captured x-coord for a given parallax layer's "base"
 * geometry (mountains / hills / tree trunks) by filtering on colour
 * equality with the per-layer base palette colour at cleanseRatio=0:
 *   layer 0 (mountains)  → triangles whose colour == palette.corMountains
 *   layer 1 (hills)      → triangles whose colour == palette.corMidground
 *   layer 2 (tree trunks)→ rects     whose colour == palette.corTrees
 *
 * Returns the sorted list of x-coords. Identifying by colour avoids
 * fragile indexing into the call stream, which can shift under viewport
 * culling.
 */
private fun layerXs(rec: Recorder, bg: ParallaxBackground, layerIdx: Int): List<Float> {
    val pal = palette(bg)
    return when (layerIdx) {
        0 -> {
            val c = paletteColor(pal, "corMountains")
            rec.triangles.filter { it.color == c }.map { it.x1 }.sorted()
        }
        1 -> {
            val c = paletteColor(pal, "corMidground")
            rec.triangles.filter { it.color == c }.map { it.x1 }.sorted()
        }
        2 -> {
            val c = paletteColor(pal, "corTrees")
            rec.rects.filter { it.color == c }.map { it.x }.sorted()
        }
        else -> error("layerIdx must be 0..2")
    }
}

/**
 * Assert `xs` contains a point within `tol` of `expected + k * patternW` for
 * some integer k in [-2..2]. This handles cases where the predicted baseX is
 * just off-screen and is observed only as a wrap-around (tile = ±1) copy.
 */
private fun assertContainsXModWrap(
    xs: List<Float>, expected: Float, patternW: Float, tol: Float = 1e-3f
) {
    val match = (-2..2).any { k ->
        val target = expected + k * patternW
        xs.any { kotlin.math.abs(it - target) <= tol }
    }
    match shouldBe true
}

/**
 * Compare two sorted x-coord lists element-wise within a float tolerance.
 * Different cameras can produce slightly different float-drift on the same
 * world-space value (e.g. `(1-6.4)+x-1` vs `(2-6.4)+x-2`), so exact-equality
 * on Float-typed lists is unsafe.
 */
private fun assertNearlyEqual(a: List<Float>, b: List<Float>, tol: Float = 5e-3f) {
    a.size shouldBe b.size
    a.zip(b).forEachIndexed { i, (x, y) ->
        if (kotlin.math.abs(x - y) > tol) {
            throw AssertionError("Index $i differs: $x vs $y (tol=$tol)\nfull a=$a\nfull b=$b")
        }
    }
}

/**
 * Pure-logic tests for [ParallaxBackground].
 *
 * ## libGDX constraint
 * `ParallaxBackground.render()` writes into a [ShapeRenderer], which would
 * normally require a live OpenGL context. To stay headless we:
 *
 *   1. Mock [ShapeRenderer] with MockK (relaxed = true) so the geometry calls
 *      become inspectable no-ops. Per-vertex coordinates are captured via the
 *      [Recorder] helper above.
 *   2. Construct [OrthographicCamera] directly — its constructor is pure-Java
 *      and `render()` only reads position / viewport fields (no `update()`).
 *
 * Settings access (`SettingsManager.load().reducedMotion`) is short-circuited
 * by seeding the private `cached` field via reflection so the production path
 * never touches `Gdx.files.local(...)`.
 *
 * ## Style follows
 *  - world/MapLevelLoaderCoordTest: BehaviorSpec for pure-math assertions.
 *  - rendering/ParticleSystemTest: reflection on private fields.
 *
 * ## Scope
 *  - Theme palette pairwise-distinctness for ARID / WIND / ECO.
 *  - Per-layer scrollFactor values (read via reflection from `layers`).
 *  - Reduced-motion path: identical layer offsets across camera moves.
 *  - Star alpha curve as a function of cleanseRatio.
 */
class ParallaxBackgroundTest : BehaviorSpec({

    val eps = 0.0005f

    // ── 1. Theme palette distinctness ────────────────────────────────────────

    given("three ParallaxBackground instances, one per theme") {
        seedSettings(reducedMotion = false)
        val arid = ParallaxBackground(ParallaxTheme.ARID)
        val wind = ParallaxBackground(ParallaxTheme.WIND)
        val eco  = ParallaxBackground(ParallaxTheme.ECO)

        // Compare a handful of palette fields that should differ across worlds.
        val fields = listOf("corMountains", "corSkyTop", "clrSkyTop",
                            "clrMountains", "clrTrees")

        `when`("comparing each themed palette field across themes") {
            then("ARID, WIND and ECO produce pairwise-distinct colours for every field") {
                fields.forEach { f ->
                    val a = paletteColor(palette(arid), f)
                    val w = paletteColor(palette(wind), f)
                    val e = paletteColor(palette(eco),  f)

                    (a == w) shouldBe false
                    (a == e) shouldBe false
                    (w == e) shouldBe false
                }
            }
        }

        clearSettingsCache()
    }

    given("ARID palette signature check") {
        seedSettings(reducedMotion = false)
        val bg = ParallaxBackground(ParallaxTheme.ARID)
        `when`("reading corMountains") {
            val c = paletteColor(palette(bg), "corMountains")
            then("matches the documented warm-brown corruption tone (0.22, 0.16, 0.08)") {
                c.r shouldBe (0.22f plusOrMinus eps)
                c.g shouldBe (0.16f plusOrMinus eps)
                c.b shouldBe (0.08f plusOrMinus eps)
            }
        }
        clearSettingsCache()
    }

    // ── 2. Per-layer scrollFactor values ─────────────────────────────────────

    given("the three-layer parallax pyramid") {
        seedSettings(reducedMotion = false)
        val bg = ParallaxBackground(ParallaxTheme.ARID)

        `when`("inspecting each layer's scrollFactor") {
            val factors = scrollFactors(bg)

            then("there are exactly three layers") {
                factors shouldHaveSize 3
            }
            then("layer 0 (mountains) has the slowest scroll = 0.15") {
                factors[0] shouldBe (0.15f plusOrMinus eps)
            }
            then("layer 1 (hills) scrolls at 0.28") {
                factors[1] shouldBe (0.28f plusOrMinus eps)
            }
            then("layer 2 (trees) has the fastest scroll = 0.40") {
                factors[2] shouldBe (0.40f plusOrMinus eps)
            }
            then("scroll factors are strictly increasing (depth ordering)") {
                (factors[0] < factors[1]) shouldBe true
                (factors[1] < factors[2]) shouldBe true
            }
        }
        clearSettingsCache()
    }

    // ── 3. Rendered offset matches camX * scrollFactor (no reduced motion) ───

    given("ParallaxBackground rendered at two camera positions (non-reduced)") {
        seedSettings(reducedMotion = false)
        val bg = ParallaxBackground(ParallaxTheme.ARID)
        val factors = scrollFactors(bg)
        val patternsPx = patternWidthsPx(bg)
        val ppm = Constants.PPM

        // Choose camera moves that keep scrollOffset under any layer's
        // patternW (smallest = 6.4m). With camXb=5m and max scrollFactor=0.4
        // the largest scrollOffset is 2.0m — well within bounds.
        val camXa = 0f
        val camXb = 5f

        val recA = Recorder()
        val recB = Recorder()
        bg.render(recA.newMockSr(), camera(camXa))
        bg.render(recB.newMockSr(), camera(camXb))

        `when`("checking that each layer's element-0 lands at the predicted baseX") {
            // For layer i, element-0 has elX_0 = elements[0].patternXPx / ppm.
            //   patternOffset = (camX * scrollFactor) mod patternW (kept positive)
            //   baseX        = left + elX_0 - patternOffset
            //   Δ(baseX)     = (camB - camA) * (1 - scrollFactor) — parallax law.
            //
            // We verify the captured x-coord set contains the predicted baseX,
            // which exercises the rendered output rather than a private path.
            val halfW = 12.8f / 2f
            // patternXPx of element-0 per layer: 0 (mountain), 30 (hill), 10 (tree).
            val firstElXLayer = listOf(0f / ppm, 30f / ppm, 10f / ppm)
            val patternsM = patternsPx.map { it / ppm }

            fun predictBaseX(camX: Float, idx: Int): Float {
                val patternW = patternsM[idx]
                val scrollOffset = camX * factors[idx]
                val pOff = ((scrollOffset % patternW) + patternW) % patternW
                return (camX - halfW) + firstElXLayer[idx] - pOff
            }

            then("layer 0 element-0 baseX appears in both captures and Δ obeys parallax law") {
                val pa = predictBaseX(camXa, 0)
                val pb = predictBaseX(camXb, 0)
                assertContainsXModWrap(layerXs(recA, bg, 0), pa, patternsM[0])
                assertContainsXModWrap(layerXs(recB, bg, 0), pb, patternsM[0])
                val expectedDelta = (camXb - camXa) * (1f - factors[0])
                (pb - pa) shouldBe (expectedDelta plusOrMinus 1e-3f)
            }
            then("layer 1 element-0 baseX appears in both captures and Δ obeys parallax law") {
                val pa = predictBaseX(camXa, 1)
                val pb = predictBaseX(camXb, 1)
                assertContainsXModWrap(layerXs(recA, bg, 1), pa, patternsM[1])
                assertContainsXModWrap(layerXs(recB, bg, 1), pb, patternsM[1])
                val expectedDelta = (camXb - camXa) * (1f - factors[1])
                (pb - pa) shouldBe (expectedDelta plusOrMinus 1e-3f)
            }
            then("layer 2 element-0 trunk baseX appears in both captures and Δ obeys parallax law") {
                val pa = predictBaseX(camXa, 2)
                val pb = predictBaseX(camXb, 2)
                assertContainsXModWrap(layerXs(recA, bg, 2), pa, patternsM[2])
                assertContainsXModWrap(layerXs(recB, bg, 2), pb, patternsM[2])
                val expectedDelta = (camXb - camXa) * (1f - factors[2])
                (pb - pa) shouldBe (expectedDelta plusOrMinus 1e-3f)
            }
            then("layer pattern widths match documented values (mountain=1280, hill=960, tree=640)") {
                patternsPx[0] shouldBe (1280f plusOrMinus eps)
                patternsPx[1] shouldBe ( 960f plusOrMinus eps)
                patternsPx[2] shouldBe ( 640f plusOrMinus eps)
                (5f * factors.max() < (640f / ppm)) shouldBe true
            }
        }
        clearSettingsCache()
    }

    // ── 4. Reduced-motion path → effectiveScroll forced to 1f ────────────────

    given("ParallaxBackground rendered with reducedMotion = true") {
        seedSettings(reducedMotion = true)
        val bg = ParallaxBackground(ParallaxTheme.ARID)

        val rec1 = Recorder()
        val rec2 = Recorder()
        bg.render(rec1.newMockSr(), camera(1.0f))
        bg.render(rec2.newMockSr(), camera(2.0f))

        `when`("comparing the layer-base x-coord sets for each layer") {
            // With effectiveScroll = 1.0:
            //   scrollOffset  = camX
            //   patternOffset = camX mod patternW  (camX < patternW → patternOffset = camX)
            //   baseX = (camX - halfW) + elX - camX = elX - halfW
            //
            // baseX is independent of camX → the *world-space* x-coords of
            // every layer element are byte-identical across camera positions.
            // That is precisely the "no parallax drift" guarantee.
            //
            // Viewport-culling note: the visibility check `sx > left + screenW`
            // can drop a wrap-tile at one camX while keeping it at the other
            // (since `left` shifts). So we only assert equality on positions
            // that lie strictly inside *both* viewports' interior region —
            // that's where the "no parallax drift" property is observable.
            val common = -3.0f..6.0f   // safely inside both [-5.4, 7.4] and [-4.4, 8.4]

            fun inCommon(xs: List<Float>) = xs.filter { it in common }

            then("layer 0 x-coords inside the shared viewport are identical across camera moves") {
                assertNearlyEqual(inCommon(layerXs(rec1, bg, 0)),
                                  inCommon(layerXs(rec2, bg, 0)))
            }
            then("layer 1 x-coords inside the shared viewport are identical across camera moves") {
                assertNearlyEqual(inCommon(layerXs(rec1, bg, 1)),
                                  inCommon(layerXs(rec2, bg, 1)))
            }
            then("layer 2 (tree trunks) x-coords inside the shared viewport are identical across camera moves") {
                assertNearlyEqual(inCommon(layerXs(rec1, bg, 2)),
                                  inCommon(layerXs(rec2, bg, 2)))
            }
        }
        clearSettingsCache()
    }

    given("Non-reduced-motion sanity: layer offsets MUST differ between camera positions") {
        seedSettings(reducedMotion = false)
        val bg = ParallaxBackground(ParallaxTheme.ARID)

        val rec1 = Recorder()
        val rec2 = Recorder()
        bg.render(rec1.newMockSr(), camera(1.0f))
        bg.render(rec2.newMockSr(), camera(2.0f))

        `when`("comparing world-space x-coord sets between camX=1m and camX=2m") {
            // With each layer's scrollFactor < 1, baseX shifts by
            // (Δcam)·(1 - scrollFactor) ≠ 0 — sets cannot be identical.
            then("layer 0 (slowest) x-coord set changes between captures") {
                (layerXs(rec1, bg, 0) == layerXs(rec2, bg, 0)) shouldBe false
            }
            then("layer 2 (fastest) x-coord set changes between captures") {
                (layerXs(rec1, bg, 2) == layerXs(rec2, bg, 2)) shouldBe false
            }
        }
        clearSettingsCache()
    }

    // ── 5. Star fade by cleanseRatio ─────────────────────────────────────────

    given("Star alpha as a function of cleanseRatio") {
        seedSettings(reducedMotion = false)
        val bg = ParallaxBackground(ParallaxTheme.ARID)

        `when`("rendering at cleanseRatio = 0 (corrupted sky)") {
            val rec = Recorder()
            bg.render(rec.newMockSr(), camera(0f), cleanseRatio = 0f)

            then("stars are drawn (circle calls present)") {
                (rec.circles.isNotEmpty()) shouldBe true
            }
            then("each star alpha == 0.90 (starAlpha=1 multiplied by 0.90 fixed)") {
                // tmp.set(1f, 0.97f, 0.88f, starAlpha * 0.90f)
                // starAlpha = ((0.5 - 0) * 2).coerceIn(0,1) = 1 → alpha = 0.90
                rec.circles.forEach { c ->
                    c.color.a shouldBe (0.90f plusOrMinus eps)
                }
            }
        }

        `when`("rendering at cleanseRatio = 0.5 (half-cleansed)") {
            val rec = Recorder()
            bg.render(rec.newMockSr(), camera(0f), cleanseRatio = 0.5f)

            then("no stars are drawn (alpha threshold 0.02 not met)") {
                // starAlpha at t=0.5 → 0 → if-guard short-circuits the loop.
                rec.circles shouldHaveSize 0
            }
        }

        `when`("rendering at cleanseRatio = 0.25 (quarter-cleansed)") {
            val rec = Recorder()
            bg.render(rec.newMockSr(), camera(0f), cleanseRatio = 0.25f)

            // starAlpha = (0.5 - 0.25) * 2 = 0.5; circle alpha = 0.5 * 0.90 = 0.45
            then("each star alpha is the linear lerp value 0.45") {
                (rec.circles.isNotEmpty()) shouldBe true
                rec.circles.forEach { c ->
                    c.color.a shouldBe (0.45f plusOrMinus eps)
                }
            }
        }

        `when`("rendering at cleanseRatio = 1.0 (fully cleansed)") {
            val rec = Recorder()
            bg.render(rec.newMockSr(), camera(0f), cleanseRatio = 1.0f)

            then("no stars are drawn at all") {
                rec.circles shouldHaveSize 0
            }
        }

        clearSettingsCache()
    }

    // ── 6. cleanseRatio clamping (negative / >1 inputs) ──────────────────────

    given("cleanseRatio out-of-range inputs are clamped") {
        seedSettings(reducedMotion = false)
        val bg = ParallaxBackground(ParallaxTheme.ARID)

        `when`("rendering at cleanseRatio = -1f (below range)") {
            val rec = Recorder()
            bg.render(rec.newMockSr(), camera(0f), cleanseRatio = -1f)
            then("treated identically to cleanseRatio = 0 → stars drawn at α≈0.9") {
                (rec.circles.isNotEmpty()) shouldBe true
                rec.circles.first().color.a shouldBe (0.90f plusOrMinus eps)
            }
        }

        `when`("rendering at cleanseRatio = 2f (above range)") {
            val rec = Recorder()
            bg.render(rec.newMockSr(), camera(0f), cleanseRatio = 2f)
            then("treated identically to cleanseRatio = 1 → no stars") {
                rec.circles shouldHaveSize 0
            }
        }

        clearSettingsCache()
    }
})

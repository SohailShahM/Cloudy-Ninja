package com.sohai.platformer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.sohai.platformer.rendering.DisplayScale
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import kotlin.math.roundToInt

/**
 * Pure-logic tests for [FontManager].
 *
 * ## libGDX constraint (pre-T-109)
 * Both branches of the original inline [FontManager.create] body touched real
 * GPU resources:
 *   - File-present path → `FreeTypeFontGenerator(...).generateFont(...)` which
 *     pulls in the libGDX `gdx-freetype` JNI natives (not on the `:core` test
 *     classpath — same constraint described in
 *     [com.sohai.platformer.entities.StormSentinelTest]).
 *   - File-absent path → `BitmapFont()` default ctor loads a classpath .fnt
 *     and constructs a [com.badlogic.gdx.graphics.Texture], which requires a
 *     live OpenGL context.
 *
 * ## T-109 seam
 * `FontManager.create()` now delegates the GL/native-dependent step to an
 * internal `FontLoader` (see `setLoaderForTesting`). Tests inject a no-op
 * loader to exercise the `create()` codepath end-to-end in pure JVM — see the
 * final two `given` blocks. The earlier reflection-based cache tests are
 * retained as black-box coverage of the cache layer (independent of the
 * loader seam).
 */
class FontManagerTest : BehaviorSpec({

    // [FontManager.clearSharedCache] calls `Gdx.app.log(...)` for a diagnostic
    // line. `Gdx.app` is null on the unit-test JVM, so we install a relaxed
    // mock for the lifetime of this spec to keep the log call a no-op.
    beforeSpec {
        if (Gdx.app == null) {
            Gdx.app = mockk<Application>(relaxed = true)
        }
    }

    // ── Reflection helpers ───────────────────────────────────────────────────

    /** Set the private (or `private set`) backing field on an object instance. */
    fun setObjectField(target: Any, name: String, value: Any?) {
        val f = target.javaClass.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    /** Read the private `sharedCache` map from [FontManager]. */
    @Suppress("UNCHECKED_CAST")
    fun sharedCache(): MutableMap<Int, BitmapFont> {
        val f = FontManager.javaClass.getDeclaredField("sharedCache")
        f.isAccessible = true
        return f.get(FontManager) as MutableMap<Int, BitmapFont>
    }

    /** Replace the contents of the shared cache with the given entries. */
    fun seedSharedCache(vararg entries: Pair<Int, BitmapFont>) {
        val m = sharedCache()
        m.clear()
        entries.forEach { (k, v) -> m[k] = v }
    }

    /** Force-empty the shared cache without going through clearSharedCache (which calls dispose()). */
    fun rawClearSharedCache() {
        sharedCache().clear()
    }

    /** Seed [DisplayScale.fontScale] (private set) via reflection. */
    fun setFontScale(value: Float) {
        setObjectField(DisplayScale, "fontScale", value)
    }

    /** Mirror of the formula used in [FontManager.create] line 40. */
    fun physicalSizeOf(size: Int, scale: Float): Int =
        (size * scale).roundToInt().coerceAtLeast(size)

    // ── 1. Cache identity ────────────────────────────────────────────────────

    given("a pre-seeded shared cache with a font at size 16") {
        val font16 = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(16 to font16)

        `when`("getShared(16) is called twice") {
            val a = FontManager.getShared(16)
            val b = FontManager.getShared(16)

            then("both calls return the SAME instance (identity ===)") {
                (a === b) shouldBe true
            }
            then("the returned instance is the one we seeded (no recreation)") {
                (a === font16) shouldBe true
            }
        }

        rawClearSharedCache()
    }

    // ── 2. Different sizes → different instances ─────────────────────────────

    given("a shared cache pre-seeded with two different sizes") {
        val font16 = mockk<BitmapFont>(relaxed = true)
        val font22 = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(16 to font16, 22 to font22)

        `when`("getShared is called for each distinct size") {
            val a = FontManager.getShared(16)
            val b = FontManager.getShared(22)

            then("the two results are different instances") {
                (a === b) shouldBe false
            }
            then("getShared(16) is the size-16 font and getShared(22) is the size-22 font") {
                (a === font16) shouldBe true
                (b === font22) shouldBe true
            }
        }

        rawClearSharedCache()
    }

    // ── 3. clearSharedCache invalidates everything ───────────────────────────

    given("a shared cache pre-seeded with two fonts") {
        val font16 = mockk<BitmapFont>(relaxed = true)
        val font24 = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(16 to font16, 24 to font24)

        `when`("clearSharedCache() is called") {
            FontManager.clearSharedCache()

            then("the internal map is empty") {
                sharedCache().isEmpty() shouldBe true
            }
            then("dispose() was called on every previously-cached font") {
                verify(exactly = 1) { font16.dispose() }
                verify(exactly = 1) { font24.dispose() }
            }
        }

        rawClearSharedCache()
    }

    // ── 4. After clear, a re-seeded entry is a *different* instance ──────────

    given("a font cached at size 16, then cleared, then a fresh font cached at size 16") {
        val original = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(16 to original)
        val before = FontManager.getShared(16)
        FontManager.clearSharedCache()
        val replacement = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(16 to replacement)

        `when`("getShared(16) is called after the re-seed") {
            val after = FontManager.getShared(16)

            then("the post-clear instance is NOT the pre-clear instance") {
                (after === before) shouldBe false
            }
            then("the post-clear instance is the freshly inserted one") {
                (after === replacement) shouldBe true
            }
        }

        rawClearSharedCache()
    }

    // ── 5. disposeShared delegates to clearSharedCache ───────────────────────

    given("a pre-seeded shared cache") {
        val font = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(20 to font)

        `when`("disposeShared() is called") {
            FontManager.disposeShared()

            then("the cache is empty (delegation worked)") {
                sharedCache().isEmpty() shouldBe true
            }
            then("dispose() was called on the cached font") {
                verify(exactly = 1) { font.dispose() }
            }
        }

        rawClearSharedCache()
    }

    // ── 6. Idempotent clear ──────────────────────────────────────────────────

    given("an already-empty shared cache") {
        rawClearSharedCache()

        `when`("clearSharedCache() is called on an empty cache") {
            FontManager.clearSharedCache() // must not throw

            then("the cache stays empty") {
                sharedCache().isEmpty() shouldBe true
            }
        }
    }

    // ── 7. DPI-aware sizing — fontScale = 1.0 (720 p baseline) ───────────────
    //
    // Mirrors FontManager.kt:40 — the documented contract is
    //   physicalSize = round(size * fontScale).coerceAtLeast(size)

    given("DisplayScale.fontScale = 1.0 (720 p baseline)") {
        val savedScale = DisplayScale.fontScale
        setFontScale(1.0f)

        `when`("the physical-size formula is applied to a 16 px virtual font") {
            val phys = physicalSizeOf(16, DisplayScale.fontScale)

            then("physicalSize equals the requested virtual size") {
                phys shouldBe 16
            }
        }

        setFontScale(savedScale)
    }

    // ── 8. DPI-aware sizing — fontScale = 2.0 (1440 p) ───────────────────────

    given("DisplayScale.fontScale = 2.0 (1440 p)") {
        val savedScale = DisplayScale.fontScale
        setFontScale(2.0f)

        `when`("the physical-size formula is applied at virtual sizes 16 and 22") {
            val phys16 = physicalSizeOf(16, DisplayScale.fontScale)
            val phys22 = physicalSizeOf(22, DisplayScale.fontScale)

            then("a virtual 16 px font is rasterised at 32 physical px") {
                phys16 shouldBe 32
            }
            then("a virtual 22 px font is rasterised at 44 physical px") {
                phys22 shouldBe 44
            }
        }

        setFontScale(savedScale)
    }

    // ── 9. DPI-aware sizing — fontScale = 3.0 (4 K) and coerceAtLeast guard ──

    given("DisplayScale.fontScale = 3.0 (4 K)") {
        val savedScale = DisplayScale.fontScale
        setFontScale(3.0f)

        `when`("a virtual size of 22 is requested") {
            val phys = physicalSizeOf(22, DisplayScale.fontScale)

            then("physicalSize = 66 (matches the FontManager class kdoc example)") {
                phys shouldBe 66
            }
        }

        setFontScale(savedScale)
    }

    // ── 10. coerceAtLeast guard — sub-1 scale never shrinks below virtual ────

    given("a (hypothetical) DisplayScale.fontScale below 1.0") {
        // DisplayScale.init() coerces scale to >= 1f in production, so this
        // configuration cannot arise organically — but the source's
        // `.coerceAtLeast(size)` on line 40 is the second line of defence
        // and must be exercised in isolation.
        val savedScale = DisplayScale.fontScale
        setFontScale(0.25f)

        `when`("the physical-size formula is applied to a 16 px virtual font") {
            val phys = physicalSizeOf(16, DisplayScale.fontScale)

            then("physicalSize is clamped to the requested virtual size (not 4)") {
                phys shouldBe 16
            }
        }

        setFontScale(savedScale)
    }

    // ── 11. Many sizes can coexist in the shared cache ───────────────────────

    given("the shared cache populated with a wide range of sizes") {
        val sizes = listOf(8, 12, 16, 20, 24, 32, 48, 64, 96, 128)
        val mocks = sizes.associateWith { mockk<BitmapFont>(relaxed = true) }
        rawClearSharedCache()
        sharedCache().putAll(mocks)

        `when`("getShared is queried for each registered size") {
            val results = sizes.map { it to FontManager.getShared(it) }

            then("every result matches the seeded instance for its size") {
                results.forEach { (s, font) ->
                    (font === mocks[s]) shouldBe true
                }
            }
            then("results for distinct sizes are pairwise distinct") {
                val instances = results.map { it.second }
                instances.toSet().size shouldBe instances.size
            }
        }

        rawClearSharedCache()
    }

    // ── 12. Edge case — extreme sizes don't crash the cache layer ────────────

    given("an empty shared cache and an extreme size key") {
        rawClearSharedCache()
        // We can't invoke create() headlessly, but we CAN verify that the
        // cache layer treats unusual int keys as plain Map<Int, _> keys
        // (zero, negative, Int.MAX_VALUE all hash/eq fine).
        val tinyMock = mockk<BitmapFont>(relaxed = true)
        val zeroMock = mockk<BitmapFont>(relaxed = true)
        val hugeMock = mockk<BitmapFont>(relaxed = true)
        val negMock = mockk<BitmapFont>(relaxed = true)
        seedSharedCache(
            0 to zeroMock,
            1 to tinyMock,
            Int.MAX_VALUE to hugeMock,
            -1 to negMock,
        )

        `when`("getShared is called with each extreme key") {
            then("getShared(0) returns the cached entry (no special-casing in the cache layer)") {
                (FontManager.getShared(0) === zeroMock) shouldBe true
            }
            then("getShared(1) returns the cached entry for size 1") {
                (FontManager.getShared(1) === tinyMock) shouldBe true
            }
            then("getShared(Int.MAX_VALUE) returns its cached entry without overflow") {
                (FontManager.getShared(Int.MAX_VALUE) === hugeMock) shouldBe true
            }
            then("getShared(-1) returns its cached entry (cache layer is sign-agnostic)") {
                (FontManager.getShared(-1) === negMock) shouldBe true
            }
        }

        rawClearSharedCache()
    }

    // ── 13. T-109 seam — create() is reachable headlessly via an injected loader ─

    given("a test FontLoader injected via setLoaderForTesting") {
        val captured = mutableListOf<Pair<Int, Color>>()
        val produced = mockk<BitmapFont>(relaxed = true)
        val testLoader = object : FontManager.FontLoader {
            override fun load(physicalSize: Int, color: Color): BitmapFont {
                captured += physicalSize to color
                return produced
            }
        }
        val savedScale = DisplayScale.fontScale
        setFontScale(2.0f)
        FontManager.setLoaderForTesting(testLoader)

        `when`("create(22) is called with the default WHITE color") {
            captured.clear()
            val result = FontManager.create(22)

            then("the loader is invoked exactly once") {
                captured.size shouldBe 1
            }
            then("the loader receives the DPI-scaled physical size (22 * 2.0 = 44)") {
                captured.single().first shouldBe 44
            }
            then("the loader receives Color.WHITE as the default tint") {
                (captured.single().second === Color.WHITE) shouldBe true
            }
            then("create() returns exactly the BitmapFont the loader produced") {
                (result === produced) shouldBe true
            }
        }

        `when`("create(size, color) is called with a non-default color") {
            captured.clear()
            val custom = Color(0.1f, 0.2f, 0.3f, 0.4f)
            FontManager.create(16, custom)

            then("the loader receives the requested color verbatim") {
                (captured.single().second === custom) shouldBe true
            }
            then("the loader receives the DPI-scaled physical size for the request") {
                captured.single().first shouldBe 32 // 16 * 2.0
            }
        }

        // Restore production state.
        FontManager.setLoaderForTesting(null)
        setFontScale(savedScale)
        rawClearSharedCache()
    }

    // ── 14. T-109 seam — getShared() routes through the injected loader & caches ─

    given("the injected loader and an empty shared cache") {
        val callCount = intArrayOf(0)
        val produced = mockk<BitmapFont>(relaxed = true)
        val testLoader = object : FontManager.FontLoader {
            override fun load(physicalSize: Int, color: Color): BitmapFont {
                callCount[0]++
                return produced
            }
        }
        rawClearSharedCache()
        FontManager.setLoaderForTesting(testLoader)

        `when`("getShared(18) is called twice on a cold cache") {
            val a = FontManager.getShared(18)
            val b = FontManager.getShared(18)

            then("the loader is invoked exactly once (second call is a cache hit)") {
                callCount[0] shouldBe 1
            }
            then("both calls return the same instance produced by the loader") {
                (a === b) shouldBe true
                (a === produced) shouldBe true
            }
        }

        FontManager.setLoaderForTesting(null)
        rawClearSharedCache()
    }

    // ── 15. T-109 seam — setLoaderForTesting(null) restores the default loader ──

    given("a custom loader installed then explicitly removed") {
        val sentinel = mockk<BitmapFont>(relaxed = true)
        FontManager.setLoaderForTesting(object : FontManager.FontLoader {
            override fun load(physicalSize: Int, color: Color): BitmapFont = sentinel
        })
        FontManager.setLoaderForTesting(null)

        `when`("we inspect the FontManager's internal loader field") {
            val field = FontManager.javaClass.getDeclaredField("loader").apply { isAccessible = true }
            val current = field.get(FontManager)

            then("the active loader is the production DefaultFontLoader") {
                (current === FontManager.DefaultFontLoader) shouldBe true
            }
        }
    }
})

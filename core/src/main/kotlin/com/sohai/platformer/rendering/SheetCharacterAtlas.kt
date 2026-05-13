package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

/**
 * Per-character sprite-sheet atlas for the T-046 rendering path (T-180 scaffold).
 *
 * Each field holds a frame strip for one animation state, sliced from a LuizMelo
 * (or compatible) horizontal-strip sheet. Optional fields ([attack2], [attack3])
 * are null when the source pack does not provide that state.
 *
 * Sits parallel to the existing procedural [CharacterAtlas] (which holds Pixmap-generated
 * frames keyed by `idle/walk/jump/fall/wallSlide`). T-186 will wire this sheet-based
 * atlas into rendering; until then this is a dormant scaffold.
 *
 * Frame counts are baked from `research/asset-pack-inventory.md` rather than re-derived
 * at runtime — the inventory is authoritative, and baking in the counts catches PNG /
 * inventory mismatches at integration time (T-186+).
 */
class SheetCharacterAtlas(
    val idle: Array<TextureRegion>,
    val run: Array<TextureRegion>,
    val jump: Array<TextureRegion>,
    val fall: Array<TextureRegion>,
    val attack1: Array<TextureRegion>,
    /** Null when the source pack lacks a second attack sheet (none in the bundled MH1/2/3 set). */
    val attack2: Array<TextureRegion>?,
    /** Null for MH1/MH2 (both lack a third attack). Non-null for MH3 only. */
    val attack3: Array<TextureRegion>?,
    val takeHit: Array<TextureRegion>,
    val death: Array<TextureRegion>,
) {
    companion object {

        /** Loader for one LuizMelo Martial Hero pack (MH1, MH2, or MH3). */
        private data class LuizMeloSheetSpec(
            /** Filename without extension, e.g. "Idle". */
            val file: String,
            /** Inventory-authoritative frame count. */
            val frameCount: Int,
        )

        // ── Inventory tables (from research/asset-pack-inventory.md, post-T-181 48 px) ──

        // MH1: idle 8, run 8, jump 2, fall 2, attack1 6, attack2 6, take-hit 4, death 6.
        // attack3: absent.
        private val MH1 = listOf(
            "Idle"     to 8,
            "Run"      to 8,
            "Jump"     to 2,
            "Fall"     to 2,
            "Attack1"  to 6,
            "Attack2"  to 6,
            "Take Hit" to 4,
            "Death"    to 6,
        ).map { LuizMeloSheetSpec(it.first, it.second) }

        // MH2: idle 4, run 8, jump 2, fall 2, attack1 4, attack2 4, take-hit 3, death 7.
        // attack3: absent.
        private val MH2 = listOf(
            "Idle"     to 4,
            "Run"      to 8,
            "Jump"     to 2,
            "Fall"     to 2,
            "Attack1"  to 4,
            "Attack2"  to 4,
            "Take Hit" to 3,
            "Death"    to 7,
        ).map { LuizMeloSheetSpec(it.first, it.second) }

        // MH3: idle 10, run 8, going-up 3, going-down 3, attack1 7, attack2 6, attack3 9,
        // take-hit 3, death 11. "Going Up" / "Going Down" map to jump / fall semantically.
        private val MH3 = listOf(
            "Idle"       to 10,
            "Run"        to 8,
            "Going Up"   to 3,
            "Going Down" to 3,
            "Attack1"    to 7,
            "Attack2"    to 6,
            "Attack3"    to 9,
            "Take Hit"   to 3,
            "Death"      to 11,
        ).map { LuizMeloSheetSpec(it.first, it.second) }

        /** All packs are 48×48 px/frame post-T-181 downsample. */
        const val LUIZMELO_FRAME_SIZE = 48

        /**
         * Load a LuizMelo Martial Hero pack from its repo-relative root directory.
         *
         * [packRoot] examples: `"sprites/luizmelo/martial-hero-1"`,
         * `"sprites/luizmelo/martial-hero-2"`, `"sprites/luizmelo/martial-hero-3"`.
         *
         * MH3's "Going Up" sheet maps to [jump] and "Going Down" to [fall] — these are
         * semantic equivalents to MH1/MH2's `Jump.png` / `Fall.png` (per the inventory's
         * naming-normalization callout). [attack3] is non-null only for MH3.
         *
         * Frame counts are taken from the inventory tables baked into this companion;
         * the [SpriteSheetFactory] caches the underlying [com.badlogic.gdx.graphics.Texture]
         * instances so calling this loader twice for the same pack does not re-load PNGs.
         */
        fun loadLuizMelo(packRoot: String): SheetCharacterAtlas {
            val specs = pickSpecsFor(packRoot)

            fun load(file: String): Array<TextureRegion> {
                val spec = specs.firstOrNull { it.file == file }
                    ?: error("LuizMelo pack at '$packRoot' has no sheet named '$file' " +
                             "in the inventory table. Available: ${specs.map { it.file }}")
                return SpriteSheetFactory.loadFrameStrip(
                    internalPath = "$packRoot/$file.png",
                    frameWidth = LUIZMELO_FRAME_SIZE,
                    frameHeight = LUIZMELO_FRAME_SIZE,
                ).also { regions ->
                    check(regions.size == spec.frameCount) {
                        "Inventory mismatch: $packRoot/$file.png produced ${regions.size} frames " +
                            "but inventory expects ${spec.frameCount}. Either the PNG was re-imported " +
                            "with different dimensions, or research/asset-pack-inventory.md drifted."
                    }
                }
            }

            // MH3 uses Going Up / Going Down for vertical-aerial states; MH1/MH2 use Jump / Fall.
            val isMh3 = isMartialHero3(packRoot)
            val jumpFile = if (isMh3) "Going Up" else "Jump"
            val fallFile = if (isMh3) "Going Down" else "Fall"

            return SheetCharacterAtlas(
                idle    = load("Idle"),
                run     = load("Run"),
                jump    = load(jumpFile),
                fall    = load(fallFile),
                attack1 = load("Attack1"),
                attack2 = if (specs.any { it.file == "Attack2" }) load("Attack2") else null,
                attack3 = if (specs.any { it.file == "Attack3" }) load("Attack3") else null,
                takeHit = load("Take Hit"),
                death   = load("Death"),
            )
        }

        private fun pickSpecsFor(packRoot: String): List<LuizMeloSheetSpec> = when {
            packRoot.endsWith("martial-hero-1") -> MH1
            packRoot.endsWith("martial-hero-2") -> MH2
            packRoot.endsWith("martial-hero-3") -> MH3
            else -> error(
                "Unknown LuizMelo pack root '$packRoot'. Expected a path ending in " +
                    "'martial-hero-1', 'martial-hero-2', or 'martial-hero-3'."
            )
        }

        private fun isMartialHero3(packRoot: String): Boolean =
            packRoot.endsWith("martial-hero-3")
    }
}

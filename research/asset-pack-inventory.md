# Asset pack inventory (T-179 + T-181 deliverable)

> Snapshot taken **2026-05-13** (T-179) and updated **2026-05-14** (T-181 — three additional manually-downloaded packs placed; LuizMelo Martial Hero 1/2/3 downsampled in-place from upstream 200/200/126 px frames to **48 px frames** via nearest-neighbor interpolation). Companion to `research/anime-asset-pack-evaluation.md` (T-177). Lists exactly which CC0 asset packs are now bundled in this repo, where they live, what gameplay role they fill, and the gaps remaining for the T-046 graphics-overhaul integration tickets.

## What's here

### 1. LuizMelo — Martial Hero (1)

- **Source URL (storefront):** https://luizmelo.itch.io/martial-hero
- **Mirror used:** https://github.com/gengen1988/unity-martial-hero (Unity package mirror — bundles the unmodified LuizMelo PNGs under `Textures/` alongside the original `License.txt`). The itch.io storefront download is JavaScript-gated (no direct CDN href), so we used this CC0-redistributing mirror as the fetch source.
- **License:** Creative Commons Zero v1.0 Universal. Bundled `LICENSE.txt` verbatim: *"This pack — Martial Hero Asset Pack is Creative Commons Zero (CC-0). Can be used in commercial and non-commercial projects."* Original creator's storefront language: "This package can be used freely and commercially - CC0 (creative commons zero). Credits are not required but I would appreciate it."
- **Files in this repo:** `assets/sprites/luizmelo/martial-hero-1/` — 9 PNG sheets (`Idle.png`, `Run.png`, `Jump.png`, `Fall.png`, `Attack1.png`, `Attack2.png`, `Take Hit.png`, `Take Hit - white silhouette.png`, `Death.png`) plus `LICENSE.txt`.
- **Contributes to Cloudy-Ninja:** **Player character base sheet** — shounen-action martial hero, 8 animation states. Strongest "anime" silhouette in the bundled set. Candidate for **Ebo** (signature protagonist).
- **Animation inventory (post-T-181 downsample):** Each PNG is a horizontal strip at **48×48 px per frame** (downsampled in-place from upstream 200×200 via nearest-neighbor). Frame counts unchanged: idle 8, run 8, jump 2, fall 2, attack1 6, attack2 6, take-hit 4, take-hit-silhouette 4, death 6 — total **46 frames** across 9 sheets. Sheet widths: idle 384, run 384, jump 96, fall 96, attack1 288, attack2 288, take-hit 192, take-hit-silhouette 192, death 288.

### 1b. LuizMelo — Martial Hero 2

- **Source URL (storefront):** https://luizmelo.itch.io/martial-hero-2
- **Acquisition path:** Manual user download from itch.io (storefront is JS-gated; no direct CDN href). `Martial Hero 2.zip` (28 KB) acquired 2026-05-14 (T-181).
- **License:** CC0 v1.0 Universal. Bundled `LICENSE.txt` verbatim: *"This pack - Martial Hero 2 is Creative Commons Zero (CC-0). Can be used in commercial and non-commercial projects."*
- **Files in this repo:** `assets/sprites/luizmelo/martial-hero-2/` — 8 PNG sheets (`Idle.png`, `Run.png`, `Jump.png`, `Fall.png`, `Attack1.png`, `Attack2.png`, `Take Hit.png`, `Death.png`) plus `LICENSE.txt`. Upstream's `Sprites/Take hit.png` was renamed to `Take Hit.png` to match MH1's filename convention.
- **Contributes to Cloudy-Ninja:** **Second player-character candidate** — slightly more polished sequel to MH1, same "anime martial hero" silhouette. Candidate for **Ebo (upgraded)** OR a sibling protagonist.
- **Animation inventory (post-T-181 downsample):** 48×48 px/frame strips (downsampled from upstream 200×200 via nearest-neighbor). Frame counts: idle 4, run 8, jump 2, fall 2, attack1 4, attack2 4, take-hit 3, death 7 — total **34 frames**. Sheet widths: idle 192, run 384, jump 96, fall 96, attack1 192, attack2 192, take-hit 144, death 336.

### 1c. LuizMelo — Martial Hero 3

- **Source URL (storefront):** https://luizmelo.itch.io/martial-hero-3
- **Acquisition path:** Manual user download from itch.io (storefront is JS-gated). `Martial Hero 3.zip` (40 KB) acquired 2026-05-14 (T-181).
- **License:** CC0 v1.0 Universal. Bundled `LICENSE.txt` verbatim: *"This pack - Martial Hero 3 Asset Pack is Creative Commons Zero (CC-0). Can be used in commercial and non-commercial projects."*
- **Files in this repo:** `assets/sprites/luizmelo/martial-hero-3/` — 9 PNG sheets (`Idle.png`, `Run.png`, `Going Up.png`, `Going Down.png`, `Attack1.png`, `Attack2.png`, `Attack3.png`, `Take Hit.png`, `Death.png`) plus `LICENSE.txt`. The pack's upstream marketing `Preview.png` (630×504 single still) was NOT bundled — it is not a sprite asset. Source-pack subdirectory `Sprite/` was flattened into the pack root to match MH1/MH2 convention.
- **Contributes to Cloudy-Ninja:** **Third player-character candidate** — MH3 introduces a third attack state and uses `Going Up.png` / `Going Down.png` instead of `Jump.png` / `Fall.png` (cleaner naming for a vertical-mobility hero). Candidate for **Laya** (mobility-focused protagonist) OR ability-cast base.
- **Animation inventory (post-T-181 downsample):** 48×48 px/frame strips (downsampled from upstream **126×126** — note this pack ships smaller native frames than MH1/MH2 — via nearest-neighbor). Frame counts: idle 10, run 8, going-up 3, going-down 3, attack1 7, attack2 6, attack3 9, take-hit 3, death 11 — total **60 frames**. Sheet widths: idle 480, run 384, going-up 144, going-down 144, attack1 336, attack2 288, attack3 432, take-hit 144, death 528.

> **Naming-normalization callout for integration tickets:** Across the three Martial Hero packs, **all sheet filenames are normalized to `Title-Case.png` matching MH1's convention** (e.g. `Take Hit.png` not `Take hit.png`). However, MH3 uses `Going Up.png` / `Going Down.png` (vertical-aerial states) instead of MH1/MH2's `Jump.png` / `Fall.png`. T-046 rendering code should treat these as semantically equivalent (going-up = jump-ascent, going-down = jump-descent / fall).

### 2. Pixel Frog — Pixel Adventure 1

- **Source URL (storefront):** https://pixelfrog-assets.itch.io/pixel-adventure-1
- **Mirror used:** https://github.com/marpor/PixelAdventure (Godot 3 starter, README explicitly states: *"All assets courtesy of Pixel Frog, who kindly released these assets under CC0 Public Domain."* PNGs are unmodified mirrors of the official pack — only the Godot `.tres`/`.tscn`/`.import` engine files are mirror-specific). itch.io storefront download is JavaScript-gated.
- **License:** Creative Commons Zero v1.0 Universal. Per the storefront: *"Creative Commons Zero v1.0 Universal — The assets are released under CC0, permitting distribution, remixing, adaptation, and commercial use without requiring attribution."* Mirror README preserved at `assets/sprites/pixelfrog/MIRROR-README.md`.
- **Files in this repo:** `assets/sprites/pixelfrog/` — **270 PNGs** (Godot engine metadata stripped, only the raw CC0 art retained). Breakdown:
  - `Background/` — 7 looping background tints (Blue, Brown, Gray, Green, Pink, Purple, Yellow)
  - `Main Characters/` — 30 PNGs across 4 protagonists (Mask Dude, Ninja Frog, Pink Man, Virtual Guy) plus Appearing/Desappearing fx
  - `Enemies/` — 97 PNGs across **20 enemy types** (AngryPig, Bat, Bee, BlueBird, Bunny, Chameleon, Chicken, Duck, FatBird, Ghost, Mushroom, Plant, Radish, Rino, Rocks, Skull, Slime, Snail, Trunk, Turtle)
  - `Items/` — 25 PNGs (Boxes, Checkpoints, Fruits)
  - `Terrain/Terrain (16x16).png` — single 352×176 master tileset PNG
  - `Traps/` — 43 PNGs (Arrow, Blocks, Falling Platforms, Fan, Fire, Platforms, Rock Head, Sand/Mud/Ice, Saw, Spike Head, Spiked Ball, Spikes, Trampoline)
  - `Menu/` — 63 PNGs (Buttons, Levels, Text)
  - `Other/` — 4 PNGs (Confetti, Dust Particle, Shadow, Transition)
- **Contributes to Cloudy-Ninja:**
  - 4 candidate **player characters** with 7 animation states each including **Wall Jump** (fills the wall-slide gap noted in T-177)
  - 20 candidate enemy archetypes for **Smog Sprite** + **Drift Husk** substitution
  - **Wind biome** tileset (the floating-platform/rock-head/fan motifs read as windy/sky terrain)
  - UI/menu kit (buttons, level numbers, text) — fills the UI flourish gap
  - Trap library (saws, spikes, fans, falling platforms) — directly maps to platformer hazards
- **Frame sizes:** Main characters 32×32 px (per-frame). Enemies range 32×32 to 46×30 (size baked into filename, e.g. `Idle (36x30).png`). Terrain tile 16×16.

### 3. ansimuz — Sunny Land (Pixel Game Art Collection)

- **Source URL (storefront):** https://ansimuz.itch.io/sunny-land-pixel-game-art
- **Mirror used:** https://opengameart.org/content/sunny-land-2d-pixel-art-pack — direct ZIP at https://opengameart.org/sites/default/files/sunny-land-files.zip (7.1 MB, no JS gate).
- **License:** CC0 v1.0 Universal. Bundled `LICENSE.txt` verbatim: *"Artwork created by Luis Zuno @ansimuz. License for Everyone. Public domain and free to use on whatever you want, personal or commercial. Credit is not required but appreciated."* (NB: the pack's bundled OGG music has a separate "credit required" license — **no music was copied into this repo**; only PNGs from the `PNG/` tree are bundled, which are unambiguous CC0.)
- **Files in this repo:** `assets/tilesets/sunny-land/` — **96 PNGs** (and `LICENSE.txt`):
  - `environment/layers/` — 4 parallax layers (back, middle, props, tileset) — `tileset.png` is 400×368
  - `environment/props/` — 23 individual prop sprites (crates, doors, blocks, bushes, signs, spikes, skulls, trees, platforms, etc.)
  - `sprites/player/` — 6 animation states for a fox-rabbit-style 32×32 character (idle 4f, run 6f, jump 2f, crouch 2f, climb 3f, hurt 2f) **as individual frames**
  - `sprites/eagle/` — 4-frame eagle attack (flying enemy candidate)
  - `sprites/opossum/` — 6-frame walking opossum (ground patroller)
  - `sprites/frog/` — idle + jump frames (4 + 2)
  - `sprites/cherry/`, `sprites/gem/` — collectible animations
  - `sprites/enemy-death/`, `sprites/item-feedback/` — VFX flipbooks (6f + 4f)
  - `spritesheets/` — 13 horizontal-strip versions of the above (engine-ready)
- **Contributes to Cloudy-Ninja:**
  - **Arid / sandy biome** tileset and parallax layers (the storefront preview shows a desert/canyon palette)
  - 3 enemy archetypes (eagle = flying patroller → **Smog Sprite**; opossum = ground patroller → **Drift Husk** ambush base; frog = secondary)
  - VFX flipbooks (enemy death poof, item pickup feedback) — fills part of the VFX gap
  - Secondary player character option (fox-style, smaller/cuter than LuizMelo's hero)
- **Frame sizes:** Player ~32×32 (sheet `player-idle.png` is 132×32 = 4 frames). Tileset 400×368 at 16×16 grid.

### 4. ansimuz — SunnyLand Forest of Illusion (background-only subset)

- **Source URL (storefront):** https://ansimuz.itch.io/sunnyland-forest-of-illusion
- **Mirror used:** https://opengameart.org/content/sunnyland-forest-of-illusion — direct ZIP at https://opengameart.org/sites/default/files/forest_of_illusion_files.zip (175 KB, no JS gate).
- **License:** CC0 v1.0 Universal (page-level OGA tag) — bundled `LICENSE.txt`: *"Artwork created by Luis Zuno (@ansimuz). LICENSE: You may use these assets in personal or commercial projects. You can modify these assets to suit your needs. You can re-distribute the file. Credit no required but appreciated it."*
- **Files in this repo:** `assets/tilesets/sunnyland-forest-of-illusion/`:
  - `Layers/back.png`, `Layers/middle.png`, `Layers/tiles.png` (176×96 tileset)
  - `Previews/Preview.png`, `Previews/Previewx3.png` (reference shots)
  - `LICENSE.txt`
- **Contributes to Cloudy-Ninja:**
  - **Eco / forest biome** parallax background + a small tileset
  - Castle-of-Illusion-inspired Sega-Genesis pixel feel — pairs cleanly with the Sunny Land arid pack (same artist, same line-work language)
- **Caveat:** This OGA subset is the **background/tileset only** (~175 KB). It is a separate itch.io pack from "Sunny Land Forest" (item 5 below), despite the similar name — see item 5's "Distinct from..." note.

### 5. ansimuz — Sunny Land Forest (full pack)

- **Source URL (storefront):** https://ansimuz.itch.io/sunnyland-forest
- **Acquisition path:** Manual user download from itch.io (storefront JS-gated). `Sunny-land-forest-files.zip` (3.8 MB) acquired 2026-05-14 (T-181).
- **License:** CC0 v1.0 Universal. Upstream `public-license.pdf` (text reproduced at `assets/tilesets/sunnyland-forest/LICENSE.txt`): *"All assets included in this package are licensed under the Creative Commons Zero (CC0) license, which means you can use them freely in any project, whether personal or commercial, without the need for attribution. There are no restrictions on use, modification, or redistribution of these assets."*
- **Files in this repo:** `assets/tilesets/sunnyland-forest/` — **106 PNGs** from the upstream `Assets/PNG/` tree (and `LICENSE.txt`):
  - `environment/layers/` — 4 parallax layers (`background.png` 192×240, `middleground.png` 384×240, `props.png` 544×256, `tileset.png` 320×192)
  - `environment/props/` — 6 standalone props (`house.png`, `mushroom-brown.png`, `mushroom-red.png`, `plant.png`, `rock.png`, `tree.png`, `vine.png`) + `environment-preview.png` reference render
  - `sprites/player/` — 7 animation states as individual frames (`player-idle`, `player-run`, `player-jump`, `player-fall`, `player-climb`, `player-duck`, `player-hurt`, `player-skip`) at ~33×32 px/frame
  - `sprites/enemies/` — 4 enemy types (`bee`, `slug`, `piranha-plant`, `piranha-plant-attack`)
  - `sprites/misc/` — `carrot`, `chest`, `enemy-death`, `hud`, `star`
  - `spritesheets/` — horizontal-strip versions of all of the above, engine-ready (player sheets at 148×32 / 296×32 / 333×32; enemy sheets at ~250-300 × 21-45)
- **NOT bundled from upstream:** `Assets/PSD/` (Photoshop sources) and `Assets/GIF/` (preview GIFs) — recoverable from the upstream pack if needed, not required for libGDX rendering.
- **Contributes to Cloudy-Ninja:**
  - **Eco / forest biome** richer tileset + parallax layers (complements item 4's Forest of Illusion partial)
  - Second alternative **player character** (small woodland-style ~32×32 — alternative or companion to PixelFrog/Sunny Land players)
  - 4 enemy archetypes for forest biome (bee = flying patroller, slug = slow ground patroller, piranha plants = ambush/static hazard)
  - Forest props library (mushrooms, vines, trees, rocks, house) for level decoration
  - Forest HUD/UI assets
- **Distinct from `assets/tilesets/sunnyland-forest-of-illusion/`** (item 4): the two are separate itch.io packs by the same artist (`sunnyland-forest` vs `sunnyland-forest-of-illusion`), with different art, palettes, and zero file overlap. Both are bundled deliberately as sibling forest-biome sources.
- **Frame sizes:** Player ~32×32, enemies 21–45 px tall (size-baked-into-art convention). Tileset 320×192 at 16×16 grid (different from FoI's 176×96).

---

## Packs not acquired

| Pack | Reason | Recommended next step |
|---|---|---|
| **LuizMelo SunnyLand Forest expansion ($5 paid tier)** (https://ansimuz.itch.io/sunnyland-forest extra) | Optional paid expansion to the base Sunny Land Forest pack (which is itself acquired — see item 5). Adds extra enemies / bosses according to the storefront description. | Defer until/if a forest-biome integration ticket establishes a real need. The free base pack covers the documented Cloudy-Ninja spec. |
| **Pixel Frog Pixel Adventure 1 (storefront ZIP)** | itch.io storefront JS-gated. **However** the marpor mirror gives us the full PNG set already, so this is acquired in practice — the only thing we'd gain from the storefront ZIP is bit-for-bit identity with the upstream `Pixel Adventure 1.zip`. | No action — `assets/sprites/pixelfrog/` contains the unmodified PNGs. If a hash-audit ever requires upstream-identity verification, fetch the 204 KB storefront ZIP. |

### Resolved in T-181 (previously listed as not acquired)

- **LuizMelo Martial Hero 2** — manual user-side download via itch.io click-through, now placed at `assets/sprites/luizmelo/martial-hero-2/`. See item 1b above.
- **LuizMelo Martial Hero 3** — same path, now at `assets/sprites/luizmelo/martial-hero-3/`. See item 1c.
- **ansimuz Sunny Land Forest (full pack)** — manual download, now at `assets/tilesets/sunnyland-forest/`. See item 5.

---

## Gaps remaining for T-046 integration

Across the **six** bundled packs (post-T-181), what's **still missing** from Cloudy-Ninja's full asset spec:

| Asset class | Spec | Covered by bundled packs? | Gap |
|---|---|---|---|
| **Ebo** sprite (signature hero) | idle/run/jump/fall/wall-slide/dash/ability-cast | Partial — LuizMelo MH1 (48px) covers 8 states minus wall-slide, dash, ability-cast. PixelFrog Mask Dude has Wall Jump. | Need to author **dash** + **ability-cast** + **wall-slide** frames matching MH1's 48-px line-work. |
| **Laya** sprite | same 7 states | **Resolved — choose LuizMelo MH3** (mobility-focused triple-attack hero, 9 sheets incl. dedicated vertical-aerial states). Still need to author dash, ability-cast, wall-slide frames. | Dash + ability-cast + wall-slide author task. |
| **Zephyr** sprite | same 7 states | **Resolved — choose LuizMelo MH2** (more polished MH1 sibling). Still need to author dash, ability-cast, wall-slide frames. OR use SL Forest player as a more pixel-cute alternative. | Dash + ability-cast + wall-slide author task. |
| **Arid biome** tileset | desert/canyon | Yes — Sunny Land `environment/layers/tileset.png`. | None. |
| **Wind biome** tileset | sky/cliff/cloud | Partial — PixelFrog Terrain + Background tints read "sky-cliff" with palette swap. | A bespoke cloud-cliff tileset would land better; consider a future CC0 sky pack. |
| **Eco biome** tileset | lush forest/restored zone | Yes — SL Forest of Illusion `Layers/tiles.png` (item 4) **AND** SL Forest `environment/layers/tileset.png` (item 5, 320×192 — bigger). Use item 5 as primary. | None — choose between the two flavors. |
| **Smog Sprite** enemy | small flying/floating | Yes — Sunny Land `eagle`, PixelFrog `Bat`/`Bee`/`BlueBird`, or SL Forest `bee`. | Pick one (SL Forest `bee` matches forest-biome palette best). |
| **Drift Husk** enemy | ambush drop-down | Yes — PixelFrog `Plant` or `Mushroom`, or SL Forest `slug` (slow ground patroller) or `piranha-plant` (ambush). | Pick one. |
| **Storm Sentinel** boss | large-sprite multi-state | **No.** Nothing in the bundled packs scales to boss-size. | **Commission or hand-author.** LuizMelo's catalog has "Evil Wizard" / boss packs (paid) — flagged for follow-up. |
| **VFX / projectiles** | lightning, dust burst, hit sparkles | Partial — PixelFrog `Other/Dust Particle.png` + `Other/Confetti.png`; Sunny Land `enemy-death` + `item-feedback`; SL Forest `enemy-death`. | **Lightning bolt VFX** still uncovered. Hand-author or grab a CC0 VFX micropack. |
| **UI / HUD** | icons, pause overlay, menu frames, **Cloud Atlas content** | Partial — PixelFrog `Menu/` (Buttons, Levels, Text), SL Forest `hud.png`, existing Kenney pack. | **Cloud Atlas flourishes** unique to Cloudy-Ninja — not in any pack, will be authored. |
| **Wall-slide animation** | per character | Yes for PixelFrog (`Wall Jump (32x32).png`). **No** for any LuizMelo MH1/MH2/MH3. | Author wall-slide frames for whichever LuizMelo packs are used as protagonists. |
| **Dash animation** | per character | **No.** | Hand-author. |
| **Ability-cast animation** | per character (one per ability) | **No.** | Hand-author per ability. |

### Summary of remaining gaps for T-046

After T-181, the missing-protagonist problem is **resolved** — Ebo/Laya/Zephyr can be assigned to MH1/MH2/MH3 (or one of the alternates) without acquiring further packs. What remains:

1. **Storm Sentinel boss** — needs hand-authoring or a separate CC0 boss pack acquisition.
2. **Per-character custom states** — dash + ability-cast + wall-slide frames for the three LuizMelo protagonists (must match downsampled 48-px line-work).
3. **Lightning-bolt VFX + Cloud Atlas UI flourishes** — hand-author.

---

## Integration handoff (T-180+ / T-046)

Recommended assignments after T-181 — subject to user art-direction call:

| Cloudy-Ninja role | Recommended source | File path |
|---|---|---|
| **Ebo** (signature shounen hero) | LuizMelo Martial Hero 1 (48px, downsampled) | `assets/sprites/luizmelo/martial-hero-1/*.png` |
| **Laya** (mobility-focused, multi-attack) | LuizMelo Martial Hero 3 (48px, downsampled — has 3 attack states + dedicated vertical-aerial sheets) | `assets/sprites/luizmelo/martial-hero-3/*.png` |
| **Zephyr** (third hero — polished sibling silhouette) | LuizMelo Martial Hero 2 (48px, downsampled) | `assets/sprites/luizmelo/martial-hero-2/*.png` |
| **Arid biome tileset** | Sunny Land | `assets/tilesets/sunny-land/environment/layers/tileset.png` |
| **Eco biome tileset (primary)** | Sunny Land Forest (full pack) | `assets/tilesets/sunnyland-forest/environment/layers/tileset.png` |
| **Eco biome parallax (alternate)** | SunnyLand Forest of Illusion | `assets/tilesets/sunnyland-forest-of-illusion/Layers/{back,middle}.png` |
| **Wind biome tileset** | PixelFrog Terrain (recoloured to sky palette) | `assets/sprites/pixelfrog/Terrain/Terrain (16x16).png` |
| **Smog Sprite enemy** | Sunny Land Forest `bee` (or PixelFrog Bat / Sunny Land eagle) | `assets/tilesets/sunnyland-forest/spritesheets/enemies/bee.png` |
| **Drift Husk enemy** | Sunny Land Forest `piranha-plant` (ambush) or PixelFrog Mushroom | `assets/tilesets/sunnyland-forest/spritesheets/enemies/piranha-plant.png` |
| **UI buttons / menu chrome** | PixelFrog Menu + SL Forest `hud.png` + existing Kenney | `assets/sprites/pixelfrog/Menu/` + `assets/tilesets/sunnyland-forest/spritesheets/misc/hud.png` |
| **Alternate player (small/cute)** | PixelFrog Ninja Frog (has Wall Jump + Double Jump already) | `assets/sprites/pixelfrog/Main Characters/Ninja Frog/*.png` |

### Sprite-frame-dimension consistency (post-T-181)

T-181 **resolved the cross-pack pixel-scale mismatch** flagged in the T-179 inventory:

| Pack | Character frame size (current, post-T-181) | Sheet format |
|---|---|---|
| LuizMelo Martial Hero 1 | **48×48 px per frame** (downsampled in-place from 200×200, nearest-neighbor) | Horizontal strip, 384×48 for 8-frame idle |
| LuizMelo Martial Hero 2 | **48×48 px per frame** (downsampled in-place from 200×200, nearest-neighbor) | Horizontal strip, 192×48 for 4-frame idle |
| LuizMelo Martial Hero 3 | **48×48 px per frame** (downsampled in-place from 126×126, nearest-neighbor) | Horizontal strip, 480×48 for 10-frame idle |
| Sunny Land player | **~33×32 px per frame** | Individual frames AND horizontal strips |
| PixelFrog Main Characters | **32×32 px per frame** | Horizontal strips, e.g. `Idle (32x32).png` at 352×32 |
| Sunny Land Forest player | **~37×32 px per frame** | Individual frames AND `spritesheets/player/*.png` strips |

All five character sources now sit in the **32–48 px range** — within a single zoom level. T-046 rendering code reads the LuizMelo sheets at **48 px frame size**, not 200/126. The downsample was nearest-neighbor (pixel-art-correct: hard edges, no anti-aliasing introduced — verified post-resize that output PNGs contain only alpha=0 and alpha=255).

### Frame-count consistency

Across the three LuizMelo Martial Hero protagonists (post-T-181), every basic state (idle/run/jump-or-going-up/fall-or-going-down/take-hit/death/attack) is covered. MH3 uses `Going Up.png` / `Going Down.png` instead of `Jump.png` / `Fall.png` — treat as semantic equivalents. **Wall-slide, dash, and ability-cast** are the universal gap across all three packs and must be authored regardless of which is chosen.

# Asset pack inventory (T-179 deliverable)

> Snapshot taken **2026-05-13**. Companion to `research/anime-asset-pack-evaluation.md` (T-177). Lists exactly which CC0 asset packs are now bundled in this repo, where they live, what gameplay role they fill, and the gaps remaining for the T-046 graphics-overhaul integration tickets.

## What's here

### 1. LuizMelo — Martial Hero (1)

- **Source URL (storefront):** https://luizmelo.itch.io/martial-hero
- **Mirror used:** https://github.com/gengen1988/unity-martial-hero (Unity package mirror — bundles the unmodified LuizMelo PNGs under `Textures/` alongside the original `License.txt`). The itch.io storefront download is JavaScript-gated (no direct CDN href), so we used this CC0-redistributing mirror as the fetch source.
- **License:** Creative Commons Zero v1.0 Universal. Bundled `LICENSE.txt` verbatim: *"This pack — Martial Hero Asset Pack is Creative Commons Zero (CC-0). Can be used in commercial and non-commercial projects."* Original creator's storefront language: "This package can be used freely and commercially - CC0 (creative commons zero). Credits are not required but I would appreciate it."
- **Files in this repo:** `assets/sprites/luizmelo/martial-hero-1/` — 9 PNG sheets (`Idle.png`, `Run.png`, `Jump.png`, `Fall.png`, `Attack1.png`, `Attack2.png`, `Take Hit.png`, `Take Hit - white silhouette.png`, `Death.png`) plus `LICENSE.txt`.
- **Contributes to Cloudy-Ninja:** **Player character base sheet** — shounen-action martial hero, 8 animation states. Strongest "anime" silhouette in the bundled set. Candidate for **Ebo** (signature protagonist).
- **Animation inventory:** Each PNG is a horizontal strip at **200×200 px per frame** (sheet size 1600×200 = 8 frames for `Idle.png`; other sheets vary by state). Frame counts per the creator's spec: idle 8, run 8, jump 4, fall 4, attack1 6, attack2 6, take-hit 4, death 6 — total **46 frames** across 8 states.

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
- **Caveat:** This OGA subset is the **background/tileset only** (~175 KB). The fuller **SunnyLand Forest** itch.io pack (~3.6 MB main, ~2.2 MB expansion) contains additional character + enemy sprites we did NOT acquire (see "Packs not acquired" below).

---

## Packs not acquired

| Pack | Reason | Recommended next step |
|---|---|---|
| **LuizMelo Martial Hero 2** (https://luizmelo.itch.io/martial-hero-2) | itch.io storefront uses JavaScript-gated "Download Now" button. No direct CDN href exposed. No CC0-redistributing GitHub mirror surfaced in search (Martial Hero 1's mirror at `gengen1988/unity-martial-hero` is MH1-only). | User downloads the 28 KB `Martial Hero 2.zip` manually from itch.io and drops it into `assets/sprites/luizmelo/martial-hero-2/`. CC0, no license risk. |
| **LuizMelo Martial Hero 3** (https://luizmelo.itch.io/martial-hero-3) | Same JS-gated issue. No clean GitHub mirror surfaced. | Same as MH2 — manual 38 KB download. |
| **ansimuz SunnyLand Forest (full pack)** (https://ansimuz.itch.io/sunnyland-forest) | itch.io storefront JS-gated; OGA mirror covers only the background/tileset subset (acquired above), not the full character + enemy + props set. | User downloads `Sunny-land-forest-files.zip` (3.6 MB) from itch.io manually. The "$5 expansion" is OPTIONAL — base pack is NYOP free. |
| **Pixel Frog Pixel Adventure 1 (storefront ZIP)** | itch.io storefront JS-gated. **However** the marpor mirror gives us the full PNG set already, so this is acquired in practice — the only thing we'd gain from the storefront ZIP is bit-for-bit identity with the upstream `Pixel Adventure 1.zip`. | No action — `assets/sprites/pixelfrog/` contains the unmodified PNGs. If a hash-audit ever requires upstream-identity verification, fetch the 204 KB storefront ZIP. |

A note has been added to `QUESTIONS.md` so the next interactive session can grab MH2 + MH3 + the full SunnyLand Forest pack via the click-through itch.io flow.

---

## Gaps remaining for T-046 integration

Across the four bundled packs, what's **still missing** from Cloudy-Ninja's full asset spec:

| Asset class | Spec | Covered by bundled packs? | Gap |
|---|---|---|---|
| **Ebo** sprite (signature hero) | idle/run/jump/fall/wall-slide/dash/ability-cast | Partial — LuizMelo MH1 covers 8 states minus wall-slide, dash, ability-cast. PixelFrog Mask Dude has Wall Jump. | Need to author/commission **dash** + **ability-cast** frames matching MH1's 200-px line-work. |
| **Laya** sprite | same 7 states | Partial — PixelFrog Pink Man or Virtual Guy gives full sheet incl. Wall Jump. | Different art style from MH1 — needs scaling or palette unification. |
| **Zephyr** sprite | same 7 states | Partial — Sunny Land player covers 6 states (no wall-slide, no dash). | Same issue: cross-pack pixel-scale mismatch. |
| **Arid biome** tileset | desert/canyon | Yes — Sunny Land `environment/layers/tileset.png`. | None. |
| **Wind biome** tileset | sky/cliff/cloud | Partial — PixelFrog Terrain + Background tints + Traps/Platforms read "sky-cliff" with palette swap. | A bespoke cloud-cliff tileset would land better; the LuizMelo storefront has additional environment packs ($5+) if budget unlocks. |
| **Eco biome** tileset | lush forest/restored zone | Yes — SunnyLand Forest of Illusion `Layers/tiles.png` + back/middle parallax. | Could use a fuller forest tileset (acquire SunnyLand Forest full pack — see "not acquired" table). |
| **Smog Sprite** enemy | small flying/floating | Yes — Sunny Land `eagle` OR PixelFrog `Bat`/`Bee`/`BlueBird`. | Pick one. |
| **Drift Husk** enemy | ambush drop-down | Yes — PixelFrog `Plant` or `Mushroom` (drop-down idle → attack pattern). | Pick one. |
| **Storm Sentinel** boss | large-sprite multi-state | **No.** Nothing in the bundled packs scales to boss-size. | **Commission or hand-author.** LuizMelo's catalog has "Evil Wizard 2" ($X) and a boss pack — flagged for follow-up. |
| **VFX / projectiles** | lightning, dust burst, hit sparkles | Partial — PixelFrog `Other/Dust Particle.png` + `Other/Confetti.png`; Sunny Land `enemy-death` + `item-feedback`. | Lightning bolt VFX not covered. Hand-author or grab a CC0 VFX micropack. |
| **UI / HUD** | icons, pause overlay, menu frames | Partial — PixelFrog `Menu/` (Buttons, Levels, Text) + existing Kenney pack. | Cloud Atlas flourishes are unique to Cloudy-Ninja — not in any pack, will be authored. |
| **Wall-slide animation** | per character | Yes — PixelFrog `Wall Jump (32x32).png` per character. | Use PixelFrog for all three protagonists OR author matching frames for the LuizMelo hero. |
| **Dash animation** | per character | **No.** | Hand-author. |
| **Ability-cast animation** | per character (one per ability) | **No.** | Hand-author per ability. |

---

## Integration handoff (T-180+)

Recommended assignments — subject to user art-direction call:

| Cloudy-Ninja role | Recommended source | File path |
|---|---|---|
| **Ebo** (signature shounen hero) | LuizMelo Martial Hero 1 | `assets/sprites/luizmelo/martial-hero-1/*.png` |
| **Laya** (mobility-focused, wall-slide-capable) | PixelFrog Ninja Frog (already pixel-anime, has Wall Jump + Double Jump) | `assets/sprites/pixelfrog/Main Characters/Ninja Frog/*.png` |
| **Zephyr** (third hero — could be the "earnest naturalist") | Sunny Land player | `assets/tilesets/sunny-land/spritesheets/player-*.png` |
| **Arid biome tileset** | Sunny Land | `assets/tilesets/sunny-land/environment/layers/tileset.png` |
| **Eco biome tileset** | SunnyLand Forest of Illusion | `assets/tilesets/sunnyland-forest-of-illusion/Layers/tiles.png` |
| **Wind biome tileset** | PixelFrog Terrain (recoloured to sky palette) | `assets/sprites/pixelfrog/Terrain/Terrain (16x16).png` |
| **Smog Sprite enemy** | PixelFrog Bat or Sunny Land eagle | `assets/sprites/pixelfrog/Enemies/Bat/` OR `assets/tilesets/sunny-land/sprites/eagle/` |
| **Drift Husk enemy** | PixelFrog Mushroom or Plant | `assets/sprites/pixelfrog/Enemies/Mushroom/` OR `.../Plant/` |
| **UI buttons / menu chrome** | PixelFrog Menu + existing Kenney | `assets/sprites/pixelfrog/Menu/` |

### Critical sprite-frame-dimension consistency check

The bundled character sheets are at three different scales:

| Pack | Character frame size | Sheet format |
|---|---|---|
| LuizMelo Martial Hero 1 | **200×200 px per frame** | Horizontal strip, 1600×200 for 8-frame idle |
| Sunny Land player | **~33×32 px per frame** | Individual frames AND horizontal strips |
| PixelFrog Main Characters | **32×32 px per frame** (with one 96×96 fx) | Horizontal strips, e.g. `Idle (32x32).png` at 352×32 |

**This is a known integration risk.** LuizMelo's hero is ~6× the pixel scale of the other two. Three options for T-180:
1. **Standardize down** — re-render Martial Hero at 32-pixel scale (loses detail).
2. **Standardize up** — re-render PixelFrog/Sunny Land at 200-pixel scale (loses pixel-art charm, requires re-art).
3. **Two-tier camera** — use Martial Hero as a "cinematic" boss-character zoom while keeping the other two at 32-px ambient zoom. Probably not viable for a unified platformer.

The realistic path is **(1)** — downscale LuizMelo's MH1 to fit a 32–48 px target. The sheet structure (state-per-PNG) makes that mechanical. Flagged for the integration-ticket author.

### Frame-count consistency

Across all three protagonists, every basic state (idle/run/jump/fall) is covered. Wall-jump is covered by PixelFrog. Dash + ability-cast are the universal gap and must be authored regardless of which packs are chosen.

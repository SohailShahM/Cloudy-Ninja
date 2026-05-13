# NOTICE — third-party assets and licenses

This repository's source code is proprietary (see [LICENSE](LICENSE)), but several
**third-party assets bundled in this repository** are distributed under permissive
open-source licenses. Those assets retain their original licenses, **including any
attribution requirements**, regardless of the proprietary license on the surrounding
code.

This file enumerates every such bundled asset so you can comply with the original
license terms when redistributing those assets (e.g. if you fork this repo for an
allowed purpose, or if a copy of an asset ends up in a shipped build).

---

## Bundled visual assets

### Kenney `pixel-platformer` pack
- **Location in this repo:** `assets/tilesets/kenney_pixel_platformer/`
- **Original source:** https://kenney.nl/assets/pixel-platformer
- **License:** [Creative Commons CC0 1.0 Universal (Public Domain Dedication)](https://creativecommons.org/publicdomain/zero/1.0/)
- **Attribution required:** No (CC0 waives attribution requirements)
- **Attribution provided anyway:** *Tiles from Kenney's "Pixel Platformer" asset pack
  (kenney.nl) under CC0.* We attribute as a courtesy and to recognize the contribution.
- **Bundled subset:** the `Tiles/`, `Tilemap/`, `Tiled/` subdirectories and associated
  sample/license files distributed with the pack.

### LuizMelo — Martial Hero (1) sprite pack
- **Location in this repo:** `assets/sprites/luizmelo/martial-hero-1/`
- **Original source:** https://luizmelo.itch.io/martial-hero
- **Mirror used for download:** https://github.com/gengen1988/unity-martial-hero (CC0-redistributing mirror — itch.io storefront is JavaScript-gated). The mirror's `License.txt` is reproduced verbatim at `assets/sprites/luizmelo/martial-hero-1/LICENSE.txt`.
- **License:** [Creative Commons Zero v1.0 Universal (Public Domain Dedication)](https://creativecommons.org/publicdomain/zero/1.0/)
- **Attribution required:** No (CC0 waives attribution requirements)
- **Attribution provided anyway:** *"Martial Hero" sprite pack by LuizMelo
  (https://luizmelo.itch.io/martial-hero) under CC0. Credits not required but provided
  as a courtesy.* Acquired via T-179 for the T-046 graphics overhaul.
- **Bundled subset:** the 9 PNG animation sheets (`Idle.png`, `Run.png`, `Jump.png`,
  `Fall.png`, `Attack1.png`, `Attack2.png`, `Take Hit.png`, `Take Hit - white silhouette.png`,
  `Death.png`) and the upstream `LICENSE.txt`.

### Pixel Frog — Pixel Adventure 1 sprite pack
- **Location in this repo:** `assets/sprites/pixelfrog/`
- **Original source:** https://pixelfrog-assets.itch.io/pixel-adventure-1
- **Mirror used for download:** https://github.com/marpor/PixelAdventure (Godot 3 starter; mirror README states verbatim: *"All assets courtesy of Pixel Frog, who kindly released these assets under CC0 Public Domain."* Mirror README preserved at `assets/sprites/pixelfrog/MIRROR-README.md`. Only the raw PNGs were copied — the Godot engine `.tres`/`.tscn`/`.import` metadata files were NOT bundled.)
- **License:** [Creative Commons Zero v1.0 Universal (Public Domain Dedication)](https://creativecommons.org/publicdomain/zero/1.0/)
- **Attribution required:** No (CC0 waives attribution requirements)
- **Attribution provided anyway:** *"Pixel Adventure 1" asset pack by Pixel Frog
  (https://pixelfrog-assets.itch.io/pixel-adventure-1) under CC0. Credits not required
  but provided as a courtesy.* Acquired via T-179 for the T-046 graphics overhaul.
- **Bundled subset:** 270 PNGs across `Background/`, `Main Characters/` (Mask Dude,
  Ninja Frog, Pink Man, Virtual Guy), `Enemies/` (20 archetypes), `Items/`, `Terrain/`,
  `Traps/`, `Menu/`, `Other/`.

### ansimuz — Sunny Land Pixel Game Art Collection
- **Location in this repo:** `assets/tilesets/sunny-land/`
- **Original source:** https://ansimuz.itch.io/sunny-land-pixel-game-art
- **Mirror used for download:** https://opengameart.org/content/sunny-land-2d-pixel-art-pack — direct ZIP at https://opengameart.org/sites/default/files/sunny-land-files.zip (the OGA mirror exposes the same CC0 files without itch.io's JS gate). Upstream `public-license.txt` reproduced verbatim at `assets/tilesets/sunny-land/LICENSE.txt`.
- **License:** [Creative Commons Zero v1.0 Universal (Public Domain Dedication)](https://creativecommons.org/publicdomain/zero/1.0/)
- **Attribution required:** No (CC0 waives attribution requirements)
- **Attribution provided anyway:** *"Sunny Land Pixel Game Art Collection" by
  Luis Zuno / @ansimuz (https://ansimuz.itch.io/sunny-land-pixel-game-art) under CC0.
  Credits not required but provided as a courtesy.* Acquired via T-179 for the T-046
  graphics overhaul.
- **Bundled subset:** the `PNG/` tree from the upstream pack — environment layers
  (back, middle, props, tileset), 23 individual prop sprites, animation frames + horizontal
  spritesheets for player (idle/run/jump/crouch/climb/hurt), eagle, opossum, frog, cherry,
  gem, enemy-death VFX, and item-feedback VFX. **96 PNGs total.**
- **Explicitly NOT bundled:** the pack's OGG music file (`platformer_level03_loop.ogg`)
  is under a separate credit-required license (music by Pascal Belisle, not CC0) — not
  copied into this repo.

### ansimuz — SunnyLand Forest of Illusion (background subset)
- **Location in this repo:** `assets/tilesets/sunnyland-forest-of-illusion/`
- **Original source:** https://ansimuz.itch.io/sunnyland-forest-of-illusion (itch.io storefront, JS-gated)
- **Mirror used for download:** https://opengameart.org/content/sunnyland-forest-of-illusion — direct ZIP at https://opengameart.org/sites/default/files/forest_of_illusion_files.zip. Upstream `public-license.txt` reproduced verbatim at `assets/tilesets/sunnyland-forest-of-illusion/LICENSE.txt`.
- **License:** [Creative Commons Zero v1.0 Universal (Public Domain Dedication)](https://creativecommons.org/publicdomain/zero/1.0/) (per the OGA page-level CC0 tag; upstream `public-license.txt` grants the equivalent rights: personal+commercial use, modification, redistribution, no credit required)
- **Attribution required:** No (CC0 waives attribution requirements)
- **Attribution provided anyway:** *"SunnyLand Forest of Illusion" by Luis Zuno /
  @ansimuz (https://ansimuz.itch.io/sunnyland-forest-of-illusion) under CC0. Credits
  not required but provided as a courtesy.* Acquired via T-179 for the T-046 graphics
  overhaul.
- **Bundled subset:** `Layers/back.png`, `Layers/middle.png`, `Layers/tiles.png` (the
  parallax background + tileset), plus the two preview reference shots in `Previews/`.

---

## Bundled font assets

### Inter (Rasmus Andersson)
- **Location in this repo:** `assets/fonts/main.ttf` (Inter Regular v4.0, extracted from
  `extras/ttf/Inter-Regular.ttf` of the upstream release)
- **Original source:** https://github.com/rsms/inter (release v4.0)
- **License:** [SIL Open Font License, Version 1.1](https://scripts.sil.org/OFL)
- **Copyright:** `Copyright (c) 2016 The Inter Project Authors (https://github.com/rsms/inter)`
- **Attribution required:** Yes — under OFL 1.1, redistribution of the Font Software
  must include the copyright notice and license. Bundled below in machine-readable
  form (this file) per OFL §2 ("These can be included either as stand-alone text files,
  human-readable headers or in the appropriate machine-readable metadata fields").
- **Reserved Font Name:** "Inter" is a Reserved Font Name under OFL §3. We bundle
  the unmodified font binary under its original name; no modified version is shipped.
- **Replaces:** the previously-bundled Microsoft Calibri Regular (T-126). Calibri is
  Microsoft-proprietary and was not redistributable; it has been removed and overwritten
  by Inter at the same path (`assets/fonts/main.ttf`) so the `FontManager.FONT_PATH`
  reference (`fonts/main.ttf`) remains valid.

**License preamble (verbatim from the upstream `LICENSE.txt`):**

> Copyright (c) 2016 The Inter Project Authors (https://github.com/rsms/inter)
>
> This Font Software is licensed under the SIL Open Font License, Version 1.1.
> This license is copied below, and is also available with a FAQ at:
> http://scripts.sil.org/OFL

The full SIL Open Font License 1.1 text is available at
https://scripts.sil.org/OFL and ships verbatim with every upstream Inter release
(`LICENSE.txt`). The font binary itself also carries the license in its name table.

---

## Generated assets (original work, included for completeness)

These are produced by code in this repository at build time or runtime. They are
**original works** of the Cloudy Ninja project and fall under the proprietary
[LICENSE](LICENSE), not under any third-party license.

- **Procedural ambient music** (`assets/audio/music/ambient_*.wav`) — generated by
  `core/src/main/kotlin/com/sohai/platformer/audio/ProceduralMusicGenerator.kt`
  at first run. T-030.
- **Procedural SFX** (`assets/audio/sfx/*.wav`) — generated by
  `core/src/main/kotlin/com/sohai/platformer/audio/ProceduralSoundGenerator.kt`. T-013.
- **Procedural achievement icons** (`assets/icons/achievements/*.png`) — generated by
  `tools/IconGenerator.kt`. T-078. 12 distinct 16×16 PNGs.

---

## Code dependencies (NOT bundled — resolved via Gradle at build time)

These are referenced by `core/build.gradle` and downloaded from Maven Central
during build. They are not redistributed in source form in this repository.
Their licenses apply to compiled artifacts in shipped builds — relevant only if
you redistribute a built binary.

- **libGDX** — Apache License 2.0 — https://libgdx.com
- **Box2D (via gdx-box2d)** — zlib License — bundled with libGDX
- **Kotlin standard library** — Apache License 2.0
- **VisUI** — Apache License 2.0 — https://github.com/kotcrab/vis-ui
- **Kotest** (test only) — Apache License 2.0 — https://kotest.io
- **MockK** (test only) — Apache License 2.0 — https://mockk.io
- **kotlinx.serialization** — Apache License 2.0

A binary distribution of Cloudy Ninja must include the Apache 2.0 notice text for
the libGDX-family dependencies it links to. This NOTICE.md does not contain that
notice text because no shipped binary exists yet; this is documentation for
future-self when packaging for itch.io / Steam.

---

## Research-only / not bundled

These files document or reference third-party work but do not redistribute it:

- `art-research/audio-candidates.md` — points to CC0/CC-BY candidates we may
  license later. No bundled audio files.
- `art-research/character-sprite-candidates.md` — points to candidate sprite
  packs. No bundled sprite files.
- `art-research/tileset-candidates.md` — additional tileset candidates beyond
  the Kenney pack. No bundled tilesets beyond Kenney.
- `research/climate-sources/` — links and PDFs documenting climate-science
  sources for the Cloud Atlas content (T-049). Public-domain government
  publications (NOAA, NASA Earth Observatory, IPCC, EPA, USGS) and other
  open-access materials.

If any of those research-listed assets are later **bundled** into the repo or
into a shipped build, this NOTICE must be updated to reflect the licenses of
the bundled-at-that-point items.

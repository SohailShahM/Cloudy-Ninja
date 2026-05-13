# Anime-style asset pack evaluation (T-177)

> Research deliverable. **No assets purchased, downloaded, or integrated.** This document collects links, license terms, and content coverage so the user can pick a direction for the T-046 graphics overhaul.

## Executive summary

- **No single off-the-shelf pack covers Cloudy-Ninja's full asset spec** (3 distinct biomes + 3 player characters with 6-ability movesets + Smog Sprite + Drift Husk + Storm Sentinel boss + matching UI). Every realistic path is a *base pack + gap-fill* strategy: pick a base, commission or hand-author the missing pieces.
- **Top pick: LuizMelo "Martial Hero" family + ansimuz "Sunny Land/SunnyLand Forest" + Pixel Adventure (Pixel Frog).** All three are CC0, all three are free, all three are battle-tested in shipped commercial games. Together they cover ~70% of the spec (player movesets, two biomes, generic enemies), with shounen-action and naturalistic styles that mix cleanly. Aesthetic is **pixel-anime** rather than hand-drawn-anime — this is the realistic anime aesthetic in the 2D platformer asset market.
- **Runner-up: Szadi Art "Fantasy Platformer All Sets" ($4.50) + Pixel Platformer World (NYOP).** One author, consistent style across all 6 biomes — eliminates the visual-mismatch risk of mixing CC0 packs from different authors. Slightly less "anime" and more "Western fantasy pixel" but coherent.
- **Budget pick (truly free, CC0-only): ansimuz Legacy Collection + Brackeys Platformer Bundle + Martial Hero 1/2.** Zero dollars, full commercial rights, but you inherit a less unified visual identity and will need the most gap-fill work.
- True **hand-drawn anime sprite packs at platformer fidelity** (Ghibli-naturalistic, shoujo-soft) **do not appear to exist** on the surveyed marketplaces in a form that covers a 3-biome 3-character spec. Hand-drawn anime is almost exclusively visual-novel / portrait art. If the user wants that aesthetic, **commission is the only path** — flagged as a decision below.

---

## Cloudy-Ninja's asset needs

Synthesized from the T-046 umbrella ticket and the T-177 prompt. Acceptance for a "complete" asset migration:

| Asset class | Spec |
|---|---|
| Player characters | Ebo, Laya, Zephyr — each with **idle, run, jump, fall, wall-slide, dash, ability cast** (~7 animation states minimum) |
| Biome tilesets | **Arid** (desert/canyon), **Wind** (sky/cliff/cloud), **Eco** (lush forest/restored-zone) — distinct enough to feel like separate acts |
| Enemies | **Smog Sprite** (small flying/floating patroller), **Drift Husk** (ambush drop-down) |
| Boss | **Storm Sentinel** — large-sprite, multi-state boss |
| VFX / projectiles | Lightning, dust burst, hit sparkles, particles |
| UI / HUD | Achievement icons, pause overlay, menu frames |

Cloudy-Ninja currently uses **ShapeRenderer-rendered silhouettes** for characters (post-T-170) and **Kenney CC0 packs** for UI/icons. The migration target is anime-styled sprite art replacing the silhouettes, plus tilesets replacing whatever solid-color geometry currently fills the levels.

The LICENSE for the repo intends **commercial release** (paid product on itch.io and Steam per T-163), which rules out any "personal use only" or "no commercial use" asset.

---

## Candidate matrix

Currency: USD. Coverage uses C=characters, T=tilesets, E=enemies, B=boss-scale, V=VFX, U=UI. ✓ = covered, ◐ = partial, ✗ = absent. Aesthetic codes: SA=shounen-action, NAT=studio-Ghibli-naturalistic, CYB=cyberpunk-anime, FANT=Western-fantasy-pixel (not strictly anime but in the candidate set for coherence reasons), CHI=chibi/cute.

| # | Pack | Source | License | C | T | E | B | V | U | Aesthetic | Cost | Integration days est. |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | LuizMelo — Martial Hero 1 | itch.io | CC0 | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | SA | Free / NYOP | 1-2 (single char) |
| 2 | LuizMelo — Martial Hero 2 | itch.io | CC0 | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | SA | Free / NYOP | 1-2 |
| 3 | ansimuz — Sunny Land | itch.io / OGA | CC0 | ✓ | ✓ | ✓ | ✗ | ◐ | ✗ | NAT | Free / NYOP | 3-5 |
| 4 | ansimuz — SunnyLand Forest | itch.io | CC0 | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | NAT | NYOP ($5 expansion) | 3-5 |
| 5 | ansimuz — Warped City | itch.io | CC0 | ✓ | ✓ | ✓ | ✗ | ◐ | ✗ | CYB | NYOP ($9 addon) | 3-5 |
| 6 | Pixel Frog — Pixel Adventure 1 | itch.io | CC0 | ✓ | ✓ | ✓ | ✗ | ◐ | ✗ | CHI | Free / NYOP | 2-4 |
| 7 | Brackeys — Platformer Bundle | itch.io | CC0 | ◐ | ✓ | ◐ | ✗ | ◐ | ◐ | FANT | Free / NYOP | 2-3 (style mismatch risk) |
| 8 | Szadi Art — Fantasy Platformer All Sets | itch.io | Custom royalty-free (no resale) | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | FANT | $4.50 (sale) / $15 list | 4-6 |
| 9 | Szadi Art — Pixel Platformer World | itch.io | Custom royalty-free | ✓ | ✓ | ✗ | ✗ | ◐ | ✗ | FANT | NYOP | 3-4 |
| 10 | rvros — Animated Pixel Adventurer | itch.io | Personal+commercial OK, no resale | ✓ (39 anims) | ✗ | ✗ | ✗ | ✗ | ✗ | SA | NYOP | 1-2 (single char) |
| 11 | Zegley — 2D Pixel Art Character Template | itch.io | CC BY 4.0 | ✓ (40+ anims) | ✗ | ✗ | ✗ | ✗ | ✗ | SA | $1+ | 1-2 |
| 12 | PushPlayArt — Hero Hack and Slash Anime | itch.io | "Use however you like" (creator-confirmed) | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | SA | NYOP | 1-2 (license-flagged) |
| 13 | CraftPix — Free Schoolgirls Anime Sprite Pack | craftpix.net | Royalty-free, unlimited projects | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | NAT/CHI | Free | 1-2 |
| 14 | Penzilla — Huge RPG Forest Platformer | itch.io | Penzilla Standard (commercial OK) | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ | FANT | $6 / bundle $11.99 | 3-5 |
| 15 | VEXED — Retro Lines | itch.io | CC0 | ✓ | ✓ | ✓ | ◐ (premium) | ✗ | ✗ | (abstract/neon) | Free / NYOP | 2-4 (style mismatch) |
| 16 | Anokolisa — Legacy Fantasy: High Forest 2.0 | itch.io | Custom "free for commercial" | ✓ | ✓ | ✓ | ✗ | ✗ | ◐ | FANT | Free | 3-5 |

**16 packs evaluated.** No "no-commercial-use" pack appears above — the few we found in passing (mostly hobby uploads with `non-commercial` flagged) were filtered out per the hard-rule constraint.

---

## Per-pack deep-dives

### 1. LuizMelo — Martial Hero

- **Source URL:** https://luizmelo.itch.io/martial-hero
- **Preview:** Animated GIFs on the storefront (the page features the red-haired, hat-wearing swordsman in idle/attack/run states)
- **License:** **CC0** ("This package can be used freely and commercially"). No attribution required, credits appreciated.
- **Coverage:**
  - Characters: ✓ — single male swordsman, **8 states**: idle (8f), run (8f), jump (4f), fall (4f), attack1 (6f), attack2 (6f), take-hit (4f), death (6f). **Missing for Cloudy-Ninja:** wall-slide, dash, ability-cast — these would need supplemental authoring or substitution from another pack.
  - Tilesets: ✗
  - Enemies: ✗
  - Boss: ✗
  - VFX: ✗
  - UI: ✗
- **Aesthetic:** Shounen-action. Strong "Samurai Jack / classic action anime" silhouette read. Reads clearly as a martial hero even at small zoom.
- **Cost:** Free (NYOP).
- **Integration days estimate:** 1-2 days *if used as just one of three protagonists*. Need ~2-3 days per supplemental ability animation if hand-authoring.
- **Gap analysis:** Player-only. To use as Ebo/Laya/Zephyr base, would need (a) palette swaps for the other two characters, (b) wall-slide/dash/ability-cast frames authored to match.
- **Notes:** LuizMelo's broader catalog includes additional martial heroes and opponents (Knight Pack, Martial Hero 2, etc.) that share visual language — likely the best **single-author** path to 3 distinct anime-styled protagonists with consistent line work.

### 2. LuizMelo — Martial Hero 2

- **Source URL:** https://luizmelo.itch.io/martial-hero-2
- **Preview:** Storefront GIFs of a different martial character at 33×56px.
- **License:** **CC0**. Same as Martial Hero 1.
- **Coverage:**
  - Characters: ✓ — idle (4f), run (8f), jump (2f), fall (2f), attack1 (4f), attack2 (4f), take-hit (3f), death (7f). **Same gap as #1: no wall-slide/dash/cast.**
  - Tilesets / Enemies / Boss / VFX / UI: ✗
- **Aesthetic:** Shounen-action — distinct character design from MH1, intentionally engineered to coexist with it (LuizMelo's "Martial Hero" series is a multi-character roster).
- **Cost:** Free (NYOP).
- **Integration days:** 1-2 days as one of three protagonists.
- **Gap analysis:** Same as MH1.
- **Notes:** Pairing MH1 + MH2 + one more LuizMelo hero (Knight Pack, etc.) gives the user three distinct anime-styled protagonists from a single artist — *the single most attractive route* to a coherent character roster.

### 3. ansimuz — Sunny Land (Pixel Game Art Collection)

- **Source URL:** https://ansimuz.itch.io/sunny-land-pixel-game-art
- **Preview:** itch.io storefront images; OpenGameArt mirror at https://opengameart.org/content/sunny-land-2d-pixel-art-pack
- **License:** **CC0 v1.0 Universal**. Verbatim from creator: assets can be used in commercial projects, multiple commenters have shipped Steam games on this base.
- **Coverage:**
  - Characters: ✓ — fully animated player (the trademark "Sunny Land" fox protagonist).
  - Tilesets: ✓ — colorful forest/grass tileset.
  - Enemies: ✓ — multiple animated enemies (snake, eagle, frog).
  - Boss: ✗
  - VFX: ◐ — visual effects included but not boss-scale.
  - UI: ✗
- **Aesthetic:** Studio-Ghibli-naturalistic *adjacent*. Warm palette, rounded character design, soft forest aesthetic. Reads more "indie platformer" than "anime" per se, but the aesthetic family lines up with Cloudy-Ninja's eco-restoration theme **better than any other pack on this list**.
- **Cost:** Free (NYOP).
- **Integration days estimate:** 3-5 days for full biome wiring.
- **Gap analysis:** No boss-scale sprite (Storm Sentinel would need commission). Player is a fox — would need to be retired or repurposed as an NPC if Ebo/Laya/Zephyr come from a different source.
- **Notes:** ansimuz is widely regarded as the gold-standard CC0 platformer asset author. Bundled Phaser + Godot demos are nice for sanity-checking the art, even though Cloudy-Ninja is libGDX.

### 4. ansimuz — SunnyLand Forest

- **Source URL:** https://ansimuz.itch.io/sunnyland-forest
- **Preview:** Storefront images of forest scenes + sprite sheets.
- **License:** **CC0 v1.0 Universal** (NYOP expansion at $5+ adds 12 extra char animations, 4 new enemies, 4 vehicles).
- **Coverage:**
  - Characters: ✓ — player + 7 animations.
  - Tilesets: ✓ — 16×16 forest tileset, 2-layer parallax.
  - Enemies: ✓ — 3 animated enemies; expansion adds more.
  - Boss: ✗
  - VFX: ✗ (limited)
  - UI: ✗
- **Aesthetic:** Naturalistic. Companion to Sunny Land — same visual language, extends it.
- **Cost:** Free (NYOP, $5 for the expansion).
- **Integration days:** 3-5 days, can share work with Sunny Land base.
- **Gap analysis:** Same author/style as Sunny Land — safe stack. Still no boss, still no UI.
- **Notes:** Best **Eco biome** candidate in the whole survey. The houses/tower/treehouse expansion fits restored-zone storytelling.

### 5. ansimuz — Warped City

- **Source URL:** https://ansimuz.itch.io/warped-city
- **Preview:** Cyberpunk neon city sprites + parallax.
- **License:** **CC0 v1.0 Universal**. Optional $9+ addon adds more characters, enemies, vehicles.
- **Coverage:**
  - Characters: ✓ — player with 10 animations.
  - Tilesets: ✓ — 16×16, 3-layer parallax.
  - Enemies: ✓ — 2 animated; addon adds more.
  - Boss: ✗
  - VFX: ◐
  - UI: ✗
- **Aesthetic:** **Cyberpunk-anime.** Neon, dark backgrounds, vibrant pinks/blues/purples.
- **Cost:** Free (NYOP).
- **Integration days:** 3-5 days.
- **Gap analysis:** Aesthetic is a poor fit for the eco-restoration core theme. Could potentially work for a *single* late-game biome representing pre-restoration industrial corruption — but probably better treated as out of scope.
- **Notes:** Excellent quality, wrong vibe. Listed for completeness.

### 6. Pixel Frog — Pixel Adventure 1

- **Source URL:** https://pixelfrog-assets.itch.io/pixel-adventure-1
- **Preview:** Playable web demo embedded on storefront.
- **License:** **CC0 v1.0 Universal.** "Distribute, remix, adapt, and build upon... even for commercial purposes. Attribution is not required."
- **Coverage:**
  - Characters: ✓ — multiple selectable player characters, each fully animated (20 FPS animations).
  - Tilesets: ✓ — full platformer tileset.
  - Enemies: ✓ — varied enemy roster.
  - Boss: ✗
  - VFX: ◐
  - UI: ◐ — basic.
- **Aesthetic:** **Chibi/cute.** Small (~16×16) cute-platformer characters in the "indie cute" vein. Reads more "Celeste/Hollow Knight chibi" than anime per se.
- **Cost:** Free (NYOP).
- **Integration days:** 2-4 days.
- **Gap analysis:** No boss-scale art. Aesthetic skews younger than typical anime; could work if Cloudy-Ninja leans into a softer Studio Ghibli register, but the *anime* descriptor is a stretch.
- **Notes:** One of the most-downloaded CC0 platformer packs on itch.io. Pixel Frog is rebuilding it from scratch (launch projected May 2026 — same month as this report). Worth monitoring.

### 7. Brackeys — Platformer Bundle

- **Source URL:** https://brackeysgames.itch.io/brackeys-platformer-bundle
- **Preview:** Storefront images of knight + slime + world tileset.
- **License:** **CC0.** Verbatim: "You can distribute, remix, adapt, and build upon the material in any medium or format, even for commercial purposes."
- **Coverage:**
  - Characters: ◐ — Knight + Slime, limited animations.
  - Tilesets: ✓ — World tileset.
  - Enemies: ◐
  - Boss: ✗
  - VFX: ◐ — minimal.
  - UI: ◐ — pixel fonts + basic UI elements.
- **Aesthetic:** **Generic cartoon/retro pixel** — NOT anime. Medieval/fantasy register.
- **Cost:** Free (NYOP).
- **Integration days:** 2-3 days (small in scope).
- **Gap analysis:** Aesthetic mismatch makes this a *complement*, not a base. Pixel fonts and sound effects might still be useful.
- **Notes:** Bundled music + SFX are useful for non-art needs even if visual style doesn't match.

### 8. Szadi Art — Fantasy Platformer All Sets

- **Source URL:** https://szadiart.itch.io/fantasy-platformer-all-sets
- **Preview:** Storefront thumbnails showing 6 biome maps.
- **License:** Custom royalty-free. Verbatim: "Edit and modify the assets... use the asset for commercial purposes... sell works created with the assets." **Resale prohibited.** Credit appreciated, not required. **No use in logos/trademarks/service-marks.** No generative AI used.
- **Coverage:**
  - Characters: ✓
  - Tilesets: ✓ — **6 distinct biomes** (forest, grass, stone/cave, tropical island, graveyard, mountain). 16×16. PNG + PSD.
  - Enemies: ✓
  - Boss: ✗
  - VFX: ✗
  - UI: ✗
- **Aesthetic:** Western-fantasy pixel. Lacks specific anime read, but **the single-author consistency across 6 biomes is unique** in this survey.
- **Cost:** $4.50 USD (currently 70% off $15 list).
- **Integration days:** 4-6 days (more biomes = more work but consistent style).
- **Gap analysis:** Lacks the anime tag, but Cloudy-Ninja's 3-biome spec (arid/wind/eco) maps cleanly onto 3 of Szadi's 6 (mountain/sky-substitute/forest). Single-author = no visual mismatch.
- **Notes:** **Strong runner-up.** If single-author coherence trumps strict anime fidelity, this is the best option.

### 9. Szadi Art — Pixel Platformer World

- **Source URL:** https://szadiart.itch.io/pixel-platformer-world
- **Preview:** Storefront GIFs.
- **License:** "You can use this asset personally and commercially." Same family of terms as Fantasy Platformer All Sets. No generative AI.
- **Coverage:**
  - Characters: ✓ — attack, run, idle, jump, death.
  - Tilesets: ✓ — 16×16, 6-layer parallax.
  - Enemies: ✗ (not included in this pack specifically)
  - Boss: ✗
  - VFX: ◐ — animated torches.
  - UI: ✗
- **Aesthetic:** 16-bit fantasy.
- **Cost:** NYOP.
- **Integration days:** 3-4 days.
- **Gap analysis:** Companion to Fantasy Platformer All Sets — same author, designed to mix.
- **Notes:** "All sets are compatible with each other" per the storefront — confirms Szadi's catalog interoperates.

### 10. rvros — Animated Pixel Adventurer

- **Source URL:** https://rvros.itch.io/animated-pixel-hero
- **Preview:** Animated GIFs on storefront.
- **License:** Personal + commercial allowed. Modification allowed. **Reselling/redistribution prohibited.** Credit appreciated, not required.
- **Coverage:**
  - Characters: ✓ — single hero with **39 animations** including wall-slide, wall-climb, wall-run, corner-grab, dash-equivalents, multiple attack chains, cast-spell, hurt, die. **The most complete moveset in the entire survey.**
  - Tilesets / Enemies / Boss / VFX / UI: ✗
- **Aesthetic:** Shounen-action. 50×37 sprite, classic 2D action anime read.
- **Cost:** NYOP.
- **Integration days:** 1-2 days for one protagonist, plus the time to recolor for a 3-character roster.
- **Gap analysis:** **Best single-character animation completeness in the survey.** 4.9/5 across 267 ratings. Includes the wall-slide, dash, and cast-spell animations Cloudy-Ninja needs.
- **Notes:** Includes original `.aseprite` files — easy to recolor/edit for Ebo/Laya/Zephyr variants. **License gotcha:** "no resale/redistribution" — fine for shipping in a compiled game, but the user can't post the raw spritesheet as a marketing asset.

### 11. Zegley — 2D Pixel Art Character Template

- **Source URL:** https://zegley.itch.io/2d-platformermetroidvania-asset-pack
- **Preview:** Multiple animated GIFs on storefront.
- **License:** **CC BY 4.0.** Requires attribution to Hayden Zegley. Commercial use OK with credit. (Cloudy-Ninja's NOTICE.md already handles third-party attribution.)
- **Coverage:**
  - Characters: ✓ — 40+ animations: idle, run, walk, jump, landing, wall-slide, ledge-grab, dash, slide, sword/katana attacks, gun, push/pull, crouch, ladder, roll, hurt, death, knocked-down. PSD + PNG.
  - Tilesets / Enemies / Boss / VFX / UI: ✗
- **Aesthetic:** Shounen-action. Designed as a **template** with interchangeable body parts — *intentionally swappable for a multi-character roster*.
- **Cost:** $1+ USD.
- **Integration days:** 1-2 days.
- **Gap analysis:** Player-only, but the template structure makes 3-character authoring trivially easier than any other option.
- **Notes:** Probably the most *technically optimal* base for the 3-character spec. CC BY 4.0 attribution is the only friction.

### 12. PushPlayArt — Hero Hack and Slash Anime

- **Source URL:** https://pushplayart.itch.io/hero-hack-and-slash-anime
- **Preview:** https://img.itch.zone/aW1hZ2UvNDU2NTEzLzIzMTg0MzAucG5n/347x500/NGnnzd.png
- **License:** **Not explicitly stated on the storefront page.** Commercial use confirmed by creator in comments ("Yes" when asked). **License-ambiguous** — see flag below.
- **Coverage:**
  - Characters: ✓ — Attack×3, Death, Hurt, Idle, Jump_fall, Speed×2.
  - Tilesets / Enemies / Boss / VFX / UI: ✗
- **Aesthetic:** **Shounen-action.** One of the few packs surveyed that the *creator explicitly tagged "anime"* — visual identity matches the user's stated direction.
- **Cost:** NYOP.
- **Integration days:** 1-2 days.
- **Gap analysis:** Missing wall-slide, dash, cast. No tilesets/enemies/etc.
- **Notes:** **License-ambiguous; flagged to QUESTIONS.md.** The visual quality is genuinely strong and aesthetically the most "anime" of any pack here, but a comment-thread "yes" isn't the same as a written CC0/CC-BY/EULA. Recommend the user contact creator for a written license before shipping commercially.

### 13. CraftPix — Free Schoolgirls Anime Character Pixel Sprite Pack

- **Source URL:** https://craftpix.net/freebies/free-schoolgirls-anime-character-pixel-sprite-pack/
- **License:** Royalty-free, unlimited projects, **commercial use explicitly allowed** ("You can sell and distribute games with our assets"). Full terms at https://craftpix.net/file-licenses/. **Cannot be redistributed as part of another asset pack.** No issue for an end-product game.
- **Coverage:**
  - Characters: ✓ — 3 distinct schoolgirl characters with run, dialogue, walk, attack, protect, "and more."
  - Tilesets / Enemies / Boss / VFX / UI: ✗
- **Aesthetic:** Naturalistic / chibi anime. **Three characters in one pack** — interesting structural fit for the 3-character spec (Ebo/Laya/Zephyr).
- **Cost:** Free.
- **Integration days:** 1-2 days.
- **Gap analysis:** Animations are likely less platformer-focused (no explicit wall-slide/dash). Need to confirm by inspection.
- **Notes:** Aesthetic skews "school RPG" — Cloudy-Ninja's eco-restoration setting is a poor narrative match. Probably not a base, but interesting as a *style reference* if commissioning new art.

### 14. Penzilla — Huge RPG Forest Platformer

- **Source URL:** https://penzilla.itch.io/huge-platformer-pack
- **License:** Penzilla Standard License (commercial use permitted, royalty-free). Detailed PDF included with download.
- **Coverage:**
  - Characters: ✓ — idle, walk, run, attack, jump, damage, death.
  - Tilesets: ✓ — full terrain + 8-layer parallax.
  - Enemies: ✓ — 4 variations.
  - Boss: ✗
  - VFX: ✗
  - UI: ✓ — 50+ UI elements.
- **Aesthetic:** Fantasy pixel art, not anime-styled.
- **Cost:** $6 USD or bundle for $11.99.
- **Integration days:** 3-5 days.
- **Gap analysis:** **The only paid pack here that includes a 50+ UI element library** — useful even if the character art isn't picked.
- **Notes:** **Bamboo Platformer companion pack** (also Penzilla, $5) is more visually Japanese but doesn't include a player character.

### 15. VEXED — Retro Lines

- **Source URL:** https://v3x3d.itch.io/retro-lines
- **License:** **CC0.** "Feel free to use these in commercial projects, and to modify the tiles as you wish."
- **Coverage:**
  - Characters: ✓ — animated player.
  - Tilesets: ✓ — hundreds of 16×16 tiles, multiple environments.
  - Enemies: ✓
  - Boss: ◐ — boss characters in premium tier ($3.25+).
  - VFX: ✗
  - UI: ✗
- **Aesthetic:** **Stylized retro neon line-art.** Distinct enough that it's not really anime — but the consistent visual language and boss-tier premium upgrade make it worth listing.
- **Cost:** Free (NYOP); premium $3.25+.
- **Integration days:** 2-4 days.
- **Gap analysis:** Style is striking but doesn't match anime brief.
- **Notes:** Listed for completeness; not a serious contender for *anime*, but a strong CC0 stack with boss-tier coverage if the user re-opens the aesthetic question.

### 16. Anokolisa — Legacy Fantasy: High Forest 2.0

- **Source URL:** https://anokolisa.itch.io/sidescroller-pixelart-sprites-asset-pack-forest-16x16
- **License:** Custom permissive. Creator: "You can use the packs for your project or for commercial projects freely." Full terms via storefront Google Drive link. No-resale-as-asset-pack implied.
- **Coverage:**
  - Characters: ✓ — Level-1 warrior with 7 animation types.
  - Tilesets: ✓ — Forest + Ruins + Lake + Bees Nest + Cave; 5 trees with 5 color variations.
  - Enemies: ✓ — Wild boar, snail, small bee.
  - Boss: ✗
  - VFX: ✗
  - UI: ◐ — HUD elements present.
- **Aesthetic:** Western-fantasy pixel, "Sidescroller Fantasy" branding.
- **Cost:** Free.
- **Integration days:** 3-5 days.
- **Gap analysis:** Largest free *content* footprint of any single pack here (600+ sprites including props/HUD/destructibles).
- **Notes:** Solid CC0-adjacent base if the user is comfortable with a fantasy register instead of strict anime.

---

## Top-3 recommendation

### 1st pick: LuizMelo Martial Hero series + ansimuz Sunny Land/SunnyLand Forest + Pixel Frog Pixel Adventure 1 stack (all CC0)

**Why:**
- **License simplicity.** All three are CC0. Zero ambiguity, zero attribution friction, zero per-game royalty. NOTICE.md gets three new bundled-asset entries and that's the entire compliance overhead.
- **Coverage.** LuizMelo gives 3 anime-styled protagonists (MH1 + MH2 + Knight Pack from same artist). ansimuz gives forest (Eco) tileset + enemies. Pixel Adventure 1 gives a second tileset for arid/wind biomes.
- **Battle-tested.** All three are widely shipped on Steam by indie devs; no rug-pull risk.
- **Aesthetic mix.** LuizMelo's shounen-action + ansimuz's naturalistic Sunny Land lean enough toward anime to honor the user's brief without forcing the survey-non-existent "pure hand-drawn anime platformer pack."
- **Estimated integration:** 7-12 days total to swap silhouettes → full pack.
- **Estimated gap-fill:** Wall-slide / dash / ability-cast animations need to be hand-authored or substituted from rvros pack (CC-BY-style). Storm Sentinel boss needs to be commissioned or hand-authored. Both are scoped follow-ups, not blockers.

### 2nd pick: Szadi Art "Fantasy Platformer All Sets" + "Pixel Platformer World"

**Why:**
- **Single-author coherence across 6 biomes.** Eliminates the mix-and-match visual mismatch that the #1 pick has to manage.
- **Cost: $4.50** (currently 70% off) for full 6-biome coverage. Cheapest non-free path to coherent multi-biome.
- **Commercial license is clean** (resale only restricted, which doesn't apply to a shipped game).
- **Estimated integration:** 4-6 days for biomes + 1-2 days for player.
- **Drawback:** Aesthetic is Western-fantasy pixel, not strictly anime. If "anime" is hard-requirement, this is a poor pick. If "stylized 2D platformer that doesn't look like a programmer-art prototype" is good enough, this is the most efficient path.

### 3rd pick: rvros Animated Pixel Adventurer + ansimuz catalog (Sunny Land + Warped City)

**Why:**
- **rvros has the only complete moveset in the survey** (wall-slide, dash, cast-spell, 39 anims total) — the *exact* Cloudy-Ninja player-spec gap that every other character pack leaves open.
- ansimuz provides the biomes (Sunny Land for Eco, Warped City optionally for a corrupted-zone biome).
- **License:** rvros is "personal + commercial OK, no resale" — fine for the shipped game, but **not as bulletproof as CC0.** Doesn't allow distributing the raw sprite sheet.
- **Estimated integration:** 5-8 days.
- **Drawback:** rvros is a single character. Three-character roster needs palette swaps + possibly head/body remixes.

---

## Decisions for user

1. **Budget cap?** Free-only constrains to picks 1 + 3 (CC0/permissive only). Allowing ~$5-15 unlocks pick 2 (Szadi) and Penzilla's UI library.
2. **Anime aesthetic strictness — pixel-anime vs hand-drawn-anime?** The 2D platformer asset market is almost entirely pixel art. If the user is set on hand-drawn anime (Ghibli/shoujo-soft), this survey suggests **commissioning is the realistic path** and we should restructure T-046 around that. (No off-the-shelf pack found that meets that bar at platformer fidelity.)
3. **Aesthetic register — shounen-action (LuizMelo, rvros) vs naturalistic (ansimuz Sunny Land) vs unified-fantasy (Szadi)?** Each maps to a different first-pick. The eco-restoration core theme leans toward naturalistic. The action gameplay leans toward shounen.
4. **Storm Sentinel boss — commission or substitute?** No surveyed pack includes a boss-scale sprite that matches the rest. Likely a separate commission ticket (~$50-200 freelance for a single boss with attack/idle/death) or a hand-authored hand-off to a future agent.
5. **Wall-slide / dash / ability-cast animations — author in-house, commission, or substitute from rvros pack?** Pick determines whether the LuizMelo or rvros path lands as #1.
6. **PushPlayArt license follow-up — does the user want to pursue a written license** for the visually-strongest "anime" pack in the survey? (See QUESTIONS.md flag.)

---

## Out of scope (don't pick these, and why)

- **The Japan Collection (GuttyKreum, $5-10 each).** Gorgeous Japanese-themed tilesets, but **top-down RPG-Maker style — not sidescroller platformer.** Creator confirmed on storefront they have no plans for sidescroller versions. Listed in initial pass; rejected on fit.
- **Mystic Woods / Ninja Adventure / Cainos (top-down packs).** Same reason — top-down. Not applicable to a sidescroller spec.
- **Visual-novel anime portrait packs (CC0 Portraits, LPC Anime Portrait #2 on OpenGameArt).** Beautiful art, wrong shape — bust-shot portraits, no platformer animation frames.
- **Generative-AI-disclosed asset packs.** Per hard rule #4, rejected on sight. None of the 16 packs above disclose AI generation; the explicitly-disclaimed packs (Szadi Art, Penzilla, ansimuz Warped City all explicitly state "no generative AI was used") were preferred.
- **No-commercial-use packs.** Per hard rule #3, filtered. Cloudy-Ninja's LICENSE intends commercial release.
- **Unity/Unreal-engine-exclusive packs.** The Unity Asset Store has anime platformer packs (Sunny Land's Unity version, e.g.) but the asset store license is per-seat-per-engine and integration into libGDX would violate the EULA. Stay on itch / OpenGameArt / CraftPix where licenses are engine-agnostic.

---

## Methodology and limitations

- **Survey was non-purchase, non-download.** All license and content claims are sourced from public storefront pages as of 2026-05-13. License terms may change; user should re-read the license at the moment of download before integration.
- **Animation-frame counts are storefront-reported,** not verified by opening the asset. Integration agent should validate counts during the T-046 implementation pass.
- **Comment-thread license confirmations (PushPlayArt) are weaker than CC0/CC-BY written license** — flagged to QUESTIONS.md.
- **Aesthetic categorization is judgment-based** from storefront preview images, not a controlled rubric. The "is this pack anime?" question genuinely lacks a hard line — pixel art at 16×16 inevitably abstracts away the line-work and shading that distinguishes anime from generic platformer style. Pick #1 (LuizMelo + ansimuz) is the closest the *pixel platformer asset market* gets to "anime"; pick #2 (Szadi) sacrifices strict anime fidelity for single-author coherence; pick #3 (rvros) is the best mechanical fit for the player-spec gap.

---

## Sources

- itch.io — [Top Platformer game assets tagged Anime](https://itch.io/game-assets/genre-platformer/tag-anime)
- itch.io — [Top game assets tagged Anime and Tileset](https://itch.io/game-assets/tag-anime/tag-tileset)
- itch.io — [LuizMelo - Martial Hero](https://luizmelo.itch.io/martial-hero) (CC0)
- itch.io — [LuizMelo - Martial Hero 2](https://luizmelo.itch.io/martial-hero-2) (CC0)
- itch.io — [ansimuz - Sunny Land](https://ansimuz.itch.io/sunny-land-pixel-game-art) (CC0)
- itch.io — [ansimuz - SunnyLand Forest](https://ansimuz.itch.io/sunnyland-forest) (CC0)
- itch.io — [ansimuz - Warped City](https://ansimuz.itch.io/warped-city) (CC0)
- itch.io — [Pixel Frog - Pixel Adventure 1](https://pixelfrog-assets.itch.io/pixel-adventure-1) (CC0)
- itch.io — [Brackeys' Platformer Bundle](https://brackeysgames.itch.io/brackeys-platformer-bundle) (CC0)
- itch.io — [Szadi Art - Fantasy Platformer All Sets](https://szadiart.itch.io/fantasy-platformer-all-sets)
- itch.io — [Szadi Art - Pixel Platformer World](https://szadiart.itch.io/pixel-platformer-world)
- itch.io — [rvros - Animated Pixel Adventurer](https://rvros.itch.io/animated-pixel-hero)
- itch.io — [Zegley - 2D Platformer/Metroidvania Asset Pack](https://zegley.itch.io/2d-platformermetroidvania-asset-pack) (CC BY 4.0)
- itch.io — [PushPlayArt - Hero Hack and Slash Anime](https://pushplayart.itch.io/hero-hack-and-slash-anime) (license-ambiguous)
- itch.io — [PushPlayArt - Anime Hero 2 Hack and Slash](https://pushplayart.itch.io/hero-sprite-animation-2d-assets) (license-ambiguous)
- itch.io — [Penzilla - Huge RPG Forest Platformer](https://penzilla.itch.io/huge-platformer-pack)
- itch.io — [Penzilla - Bamboo Platformer Asset Pack](https://penzilla.itch.io/bamboo-platformer)
- itch.io — [VEXED - Retro Lines](https://v3x3d.itch.io/retro-lines) (CC0)
- itch.io — [Anokolisa - Legacy Fantasy: High Forest 2.0](https://anokolisa.itch.io/sidescroller-pixelart-sprites-asset-pack-forest-16x16)
- CraftPix — [Free Schoolgirls Anime Character Pixel Sprite Pack](https://craftpix.net/freebies/free-schoolgirls-anime-character-pixel-sprite-pack/)
- CraftPix — [License terms](https://craftpix.net/file-licenses/)
- OpenGameArt — [Sunny Land 2D Pixel Art Pack](https://opengameart.org/content/sunny-land-2d-pixel-art-pack) (CC0)
- OpenGameArt — [CC0 Platformer collection](https://opengameart.org/content/cc0-platformer)
- OpenGameArt — [Surt's CC0 Scraps](https://opengameart.org/content/surts-cc0-scraps-tilesets-platformers-sprites)

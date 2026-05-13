# Cloudy Ninja — itch.io page draft

> Drop-in copy for the itch.io CMS. Plain markdown, no autolinks, no Steam-store
> terminology. Cross-references:
> - Tag order sourced from **T-075** — `marketing/steam-tags-research.md` (the 4 primary
>   + 7 stretch tag recommendation, plus the 6 anti-tags we avoid). Steam taxonomy
>   names are mapped to itch's looser tag system per T-075 section (g).
> - Controls section reflects the **T-073** default mapping — `research/keyboard-layout-conventions.md`
>   (Layout-A "modern indie / WASD + SPACE"). The "Switch character" key shown here is
>   **Q**, matching T-073's recommendation; T-121 (separate ticket) is the source-side
>   change to flip the shipped default from S → Q. If T-121 has not merged when this
>   page goes live, swap `Q` for `S` in §6 below.
>
> **Author:** claude-code-sub-agent · **Compiled:** 2026-05-13 · **Ticket:** T-124
>
> Editing notes: this draft is intentionally conservative. Every feature bullet, every
> system-requirements line, every tag is something Cloudy Ninja **already ships**, not
> something on the roadmap. The user should feel free to add personality + a tagline
> in their own voice on top — but should not extend any feature claim without confirming
> the underlying ticket landed.

---

## 1. Title and short description (the 160-character field)

**Title:** `Cloudy Ninja`

**Short description** (itch.io's `Short description or tagline` field caps at 160 characters):

```
A 2D pixel-art platformer about restoring corrupted ecosystems. Switch between three water-cycle heroes mid-jump to traverse 8 hand-crafted levels.
```

Character count: **147 / 160** — comfortable headroom for later tweaks. The string is one breath, leads with the genre signature (`2D pixel-art platformer`), names the differentiator (`restoring corrupted ecosystems`), and ends with the mechanic players actually do (`switch between three… mid-jump`). No "Celeste-like" anchor; no superlatives.

**Alternative shorter variant** (132 chars) for use if itch later lowers the cap or if a marquee gets clipped:

```
A 2D pixel-art platformer about healing broken ecosystems. Switch between three water-cycle heroes mid-jump across 8 levels.
```

---

## 2. Long description (~500 words, plain markdown)

> Paste this directly into the itch.io "Description" field. Itch renders standard
> markdown — headings, lists, **bold**, *italic*. No autolinking; if a link is wanted,
> write it explicitly as `[text](url)`. No HTML embeds in this section — screenshots
> and the trailer live in their own slots on the itch.io page (see §7 and §8 below).

---

### Heal a broken world, one cloud at a time.

Cloudy Ninja is a **2D pixel-art platformer about restoring corrupted ecosystems**. Instead of fighting your way through a level, you clean it — clearing smog, planting growth, and reconnecting the water cycle until each stage reads as alive again.

You play **three switchable heroes**, each carrying a piece of the water cycle as a movement ability:

- **Ebo** — *Seed Slam.* A downward ground-pound that breaks corrupted tiles, plants growth, and stuns smog enemies. Your "down + commit" answer.
- **Laya** — *Wind Dash.* A short horizontal burst that crosses gaps no walk-jump can reach. Your "go fast" answer.
- **Zephyr** — *Float.* A gentle hover that buys you time in the air to read what's next. Your "what now?" answer.

The catch: you swap between them **mid-jump**. The level isn't asking who's strongest, it's asking who's right for the next half-second. A Laya dash to clear a chasm, a swap to Zephyr to glide past a hazard, a swap to Ebo to slam through the floor on the other side — chained into one move.

### Eight stages, one boss, no busywork

The campaign is **eight hand-crafted levels** built across three biomes — sky, eco-ruin, and arid wastes — with a hub world (Sky Sanctuary) connecting them. Four are tutorial stages that introduce each character + the swap, three are full campaign rooms, and the run ends at the **multi-phase Storm Sentinel boss arena**. No procedural levels. No grind. The whole run is around 60–90 minutes the first time through.

Scattered across the levels are **Cloud Atlas entries** — short collectible cards drawn from real NOAA and NASA climate research. Picking them up is optional; the game doesn't lecture if you don't.

### Accessibility-first, not difficulty-flavored

Cloudy Ninja ships **Assist Mode** (toggleable invincibility, infinite spirit charges, and a slow-speed slider for motor accessibility), **color-blind palettes** (4 modes), **reduced-motion mode**, and **full keyboard rebinding**. All 12 achievements unlock in Assist Mode — accessibility settings never gate progression. The game is built so that "I want to see the ending" and "I want to push myself" are both first-class choices, made per session, with one click.

### Other things in the box

- **Three save slots** with checkpoint autosave; runs are never half-saved.
- **4K / HiDPI scaling** that holds the pixel-art look at modern desktop resolutions.
- **Three ambient music tracks** with crossfade, plus eight gameplay SFX.
- **Mobile two-thumb HUD** on Android in addition to desktop keyboard.

This is an **alpha release**. Expect rough edges; please tell me about them. Bug reports and accessibility feedback go through the GitHub issues link below.

---

## 3. Genre + tag list (itch.io's tag field)

Itch.io's tag system is looser than Steam's — there's no popularity ranking on the page, no Tag Wizard equivalent at upload time, and tags are entered free-form (although itch auto-suggests popular ones). But itch's discovery + browse pages **do** behave more like a tag-similarity engine the more tags overlap with other listings in a player's recently-viewed set. So the same prioritization principle applies: **put the four primary tags first, then the stretch tags, and skip the anti-tags entirely**.

The tags below map T-075's recommended Steam-style list onto itch's actual lowercase tag namespace. Order matches T-075 section (f) — primary tags 1-4, then stretch tags 5-11. `Indie` is dropped from this list because every itch.io game is implicitly indie and the tag adds no information on this storefront.

### Primary tags (the 4 anchors — T-075 must-have set)

1. `pixel-art` — visual style anchor (itch's spelling of Steam's `Pixel Graphics`)
2. `platformer` — genre anchor
3. `2d` — sub-genre cluster
4. `nature` — angle / niche differentiator (the eco-restoration theme)

### Stretch tags (7 — T-075 stretch set, in priority order)

5. `cute` — highest-leverage mood tag in T-075's survey
6. `atmospheric` — mood reinforcement
7. `exploration` — Cloud Atlas pickup loop
8. `colorful` — biome visual signal
9. `side-scroller` — sub-genre disambiguator
10. `family-friendly` — accessibility-mode signal
11. `accessibility` — itch.io has this tag natively, and Cloudy Ninja qualifies hard (Assist Mode + color-blind palettes + reduced-motion + key rebinding)

### Tags we are explicitly NOT applying (the 6 anti-tags from T-075 section e)

Do not add these to the itch.io tag list, even if itch's auto-suggest offers them:

- `difficult` — Assist Mode is shipped; claiming `difficult` would mismatch the audience that filters for it.
- `precision-platformer` — same problem, narrower.
- `metroidvania` — Cloudy Ninja is linear with a hub, not an interconnected map.
- `educational` / `education` — the eco angle is marketing copy, not a tag bucket. Edutainment audiences and gaming audiences don't overlap on this storefront.
- `walking-simulator` — Cloudy Ninja has fail states (death, hazards, the boss).
- `casual` (when combined with `family-friendly` + `cute`) — would over-bias us into the cozy-only market. Platforming + traversal puzzles are the core loop.

### Tag order at upload time (the "Tag Wizard" equivalent)

Itch.io doesn't have a real Tag Wizard — tags are just listed in a single field, separated by commas. But the **order tags appear in the field carries some weight**: tag-auto-complete shows your earlier tags first when itch recommends related listings, and itch's "more like this" sidebar weights earlier tags higher. So enter them in priority order:

```
pixel-art, platformer, 2d, nature, cute, atmospheric, exploration, colorful, side-scroller, family-friendly, accessibility
```

**Genre dropdown** (separate field, single value):

- Primary: `Platformer`
- Subgenre (if itch asks): `2D Platformer`

---

## 4. Feature list (the "More information" bullet block)

Six bullets — enough to justify a price, short enough to scan. Each one is something already in the codebase (cross-checked against `TASKS.md ## Done`, the presskit, and the README).

- **Three-character switching, mid-jump.** Chain Ebo's Seed Slam, Laya's Wind Dash, and Zephyr's Float across a single move.
- **8 levels plus a multi-phase Storm Sentinel boss.** Sky Sanctuary hub + four tutorial stages + three campaign stages + boss arena. No procedural padding.
- **12 achievements, all unlockable in Assist Mode.** Accessibility never gates progression.
- **Accessibility-first design.** Assist Mode (invincibility + infinite spirit + slow-speed slider), 4 color-blind palettes, reduced-motion mode, full key rebinding, 4K / HiDPI scaling.
- **Cloud Atlas codex.** Six in-game collectible entries drawn from real NOAA / NASA climate research. Optional — does not block any achievement.
- **Three save slots with checkpoint autosave.** Atomic writes; you never lose a run mid-save.

---

## 5. System requirements

> Itch.io has a structured "System requirements" widget. The minimums below reflect what
> the libGDX desktop launcher needs. Mobile two-thumb controls are available on Android
> via a separate APK upload — list those requirements in the Android-build's own
> "Platforms" entry on the page.

### Desktop (Windows / macOS / Linux)

| Field | Minimum |
|---|---|
| Operating system | Windows 10 64-bit, macOS 10.15 Catalina, or 64-bit Linux with a modern desktop environment |
| Processor | Dual-core x86_64 @ 2.0 GHz |
| Memory | 2 GB RAM |
| Graphics | OpenGL 3.2-compatible GPU; 256 MB VRAM. Integrated Intel HD 4000+ works. |
| Storage | 200 MB |
| Java | A bundled JRE is included in the desktop build; no separate Java install is needed. |
| Input | Keyboard. Gamepad support is in development; some controllers may already work via the libGDX gamepad layer. |

### Android

| Field | Minimum |
|---|---|
| Operating system | Android 8.0 (Oreo) or later, 64-bit (arm64-v8a) |
| Memory | 2 GB RAM |
| Storage | 150 MB |
| Input | Touch (two-thumb HUD); external gamepads not yet supported. |

> Notes for the page: the desktop build is currently distributed as a single ZIP with
> the JRE bundled alongside the JAR. No installer is required — extract and run the
> launcher script.

---

## 6. Controls reference

Defaults match T-073's recommended **Layout A** (modern indie WASD + SPACE). All keys are rebindable in **Settings → Controls**. The "Switch character" key is **Q** per T-073's recommendation; if T-121's source-side default flip hasn't shipped to the build you're uploading, swap `Q` for `S` in the row below and add a note that S is the current binding.

### Keyboard (default)

| Action | Default key | Notes |
|---|---|---|
| Move left | `A` | Hold to walk. |
| Move right | `D` | Hold to walk. |
| Jump | `Space` | Hold for slightly higher jump. |
| Use ability | `E` | Triggers the active character's water-cycle ability (Seed Slam / Wind Dash / Float). Some abilities are hold-to-sustain. |
| Switch character | `Q` | Cycles Ebo → Laya → Zephyr → Ebo. Works mid-jump. |
| Pause | `Esc` | Opens the pause overlay. |

### Touch (Android)

- Left half of the screen: virtual move pad.
- Right half: jump button + ability button + character-switch button.
- Pause: top-right `≡` icon.

### Rebinding

All five keyboard bindings (left, right, jump, action, swap) are rebindable from **Settings → Controls**. The Controls panel shows each action, its current key, and a "Rebind" button that listens for the next key press. Conflicts are highlighted and refused.

### Gamepad

Gamepad support is in development; some XInput controllers will already work via the libGDX gamepad layer, but no profile is officially shipped yet. If your controller is recognized, the typical mapping is left stick / D-pad for movement, A / cross for jump, X / square for ability, Y / triangle for character switch.

---

## 7. Screenshots — placeholders

> Itch.io accepts up to 5 screenshot uploads in the page sidebar plus inline images
> embedded in the description. Recommended: upload 5 to the sidebar widget, then leave
> the description's image block empty (cleaner page, less scroll). Order in the
> sidebar matters — itch shows them in the order you upload, and the **first one is
> the marquee image** used by browse / search results.

Drop the existing presskit screenshots into the sidebar in this order (each is at `marketing/presskit/screenshot-0N.png`):

1. **`screenshot-01.png`** — Sky Sanctuary hub. Marquee image. Use the widest establishing shot — the one that reads cleanly as a thumbnail.
2. **`screenshot-02.png`** — A mid-game level showing a character mid-air post-swap. Player must be readable at thumbnail size; avoid action so frenetic that it's unclear at 240px.
3. **`screenshot-03.png`** — Eco-restoration moment (corrupted tile becoming clean / planted). Sells the "restoration not combat" hook.
4. **`screenshot-04.png`** — Storm Sentinel boss arena. Optional spoiler; itch.io players generally accept boss screenshots in this slot.
5. **`screenshot-05.png`** — Settings / accessibility screen showing Assist Mode + color-blind toggles. Sells the accessibility positioning without needing the description to lead with it.

> Reserve `screenshot-06.png` for the press kit only — too many screenshots on the itch.io page dilutes the marquee.

### Inline image placeholders (optional — leave commented out for now)

If the user later wants to embed an inline image in the description (e.g. a logo at the top), the markdown is:

```markdown
![Cloudy Ninja logo](https://img.itch.zone/[path-after-upload]/logo.png)
```

itch.io rewrites local paths after upload; do not pre-fill the URL. Upload `marketing/presskit/logo.png` to the itch CMS and let it return the rewritten URL.

---

## 8. Trailer — placeholder

There is no trailer cut yet (the presskit explicitly marks the trailer slot pending). Leave the itch.io "Trailer/YouTube URL" field **blank at upload time**. When the trailer ships:

1. Upload to YouTube (unlisted is fine for alpha; flip to public for full launch).
2. Paste the full URL (e.g. `https://www.youtube.com/watch?v=XXXXXXXXXXX`) into itch.io's "Trailer" field — itch auto-embeds the YouTube player above the screenshot strip.
3. Optional: also embed it inline at the top of the description with a one-line caption above.

Until then, the page leads with the marquee screenshot — that's fine for alpha and matches the pattern in T-048's listing-style-guide survey (8 of 12 reference itch.io pages have no trailer).

---

## 9. Pricing + "How are you releasing it?" block

The presskit positions Cloudy Ninja at **USD $2.99–4.99** (one-time purchase, no IAP, no ads). For the alpha launch on itch.io, the recommended setup:

- **Price:** Free OR pay-what-you-want with a $2.99 suggested price.
  - T-048's survey found pay-what-you-want correlates strongly with higher rating counts on itch.io (Frogfall 1,126; SELF 1,090; Sheepy 554 — all pay-what-you-want, all 4.8+). Friction matters more than dollar amount for early reach.
  - The full $2.99–4.99 fixed price can land at v1.0 (post-alpha), once the page has a real review base and a trailer.
- **Demo:** Not needed — the alpha *is* the available build.
- **Devlogs:** Use itch's devlog feature for build-update announcements. Keep them short, image-led, link to the GitHub Discussions page for longer threads.
- **Steam key bundling:** **Leave off for now.** Cloudy Ninja has no Steam page yet; T-075's tag research targets the eventual Steam upload but Sprint D ships itch-only first.

---

## 10. Page-bottom contact / links block

```markdown
### Links

- **Source:** [github.com/SohailShahM/Cloudy-Ninja](https://github.com/SohailShahM/Cloudy-Ninja) (source-visible, proprietary license — see `LICENSE` in the repo)
- **Issues / feedback:** [github.com/SohailShahM/Cloudy-Ninja/issues](https://github.com/SohailShahM/Cloudy-Ninja/issues)
- **Discussions:** [github.com/SohailShahM/Cloudy-Ninja/discussions](https://github.com/SohailShahM/Cloudy-Ninja/discussions)
- **Press kit:** download the press kit ZIP from this page's sidebar, or browse the same files at `marketing/presskit/` in the source repo.

### Credits

- Design, code, art direction: Sohail Shah (solo)
- Base pixel art: [Kenney](https://kenney.nl/) — pixel-platformer pack (CC0)
- Theme accents: OpenGameArt — Pixel Art Forest tilesets (CC0)
- Built with: [libGDX](https://libgdx.com/) (Kotlin)
- AI-assisted development: see the repo's `START_HERE.md` for the AI agent routing matrix
```

---

## 11. Pre-flight checklist before hitting "Publish" on itch.io

Quick pass the user should run before flipping the page live, in order:

- [ ] Title field set to `Cloudy Ninja`.
- [ ] Short description (160-char field) pasted from §1 above. Verify count after paste.
- [ ] Long description pasted from §2. Verify no markdown rendering errors (itch previews live — check the bold + the bullet lists).
- [ ] Genre dropdown set to `Platformer`.
- [ ] Tags entered in §3's recommended order: `pixel-art, platformer, 2d, nature, cute, atmospheric, exploration, colorful, side-scroller, family-friendly, accessibility`. Confirm none of the 6 anti-tags from §3 were auto-added by itch's auto-complete.
- [ ] System requirements filled per §5 (separate entries for desktop and Android builds).
- [ ] Five screenshots uploaded to the sidebar in §7's order. Marquee renders cleanly at thumbnail size.
- [ ] Trailer field left blank (or filled with YouTube URL if a trailer has been cut).
- [ ] Pricing model set per §9.
- [ ] Links + credits block from §10 appended to the bottom of the description.
- [ ] Build artifacts (desktop ZIP, Android APK if shipped) uploaded with correct platform tags.
- [ ] Press kit ZIP uploaded as a downloadable extra (or linked to the GitHub `marketing/presskit/` path).
- [ ] Page set to **public** but **unlisted** for the first 24h — flip to fully listed only after a smoke-pass review of the live page (typos, broken images, tag spelling).

---

*End of draft. T-124. See `marketing/steam-tags-research.md` (T-075) for the tag rationale, `research/keyboard-layout-conventions.md` (T-073) for the controls rationale, and `marketing/presskit/` (T-077) for the asset bundle this page references.*

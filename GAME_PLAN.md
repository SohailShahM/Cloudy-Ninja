# GAME_PLAN.md — Cloudy Ninja

> Living roadmap. For task-level tracking see TASKS.md; for architecture see AGENTS.md;
> for technical specs see GDD_ADDENDUM.md.

Last updated: 2026-05-12

---

## The pitch

Cloudy Ninja is a momentum-based 2D pixel-art platformer about restoring corrupted ecosystems. Players switch between three characters — each with a distinct ability tied to the water cycle — across hand-crafted levels that double as light interactive science lessons. It targets casual-to-enthusiast platformer fans who want satisfying movement and a reason to care about what they're doing.

---

## Resolved decisions (v1.0 scope)

| Topic | Decision |
|---|---|
| Art style | Pixel art, 32×32 base resolution, scaled at load time via `DisplayScale.spriteScale`. **Base assets: Kenney's [`pixel-platformer`](https://kenney.nl/assets/pixel-platformer) (CC0, ~350 files; includes terrain, characters, enemies, pickups, hazards) — already in repo at `assets/tilesets/kenney_pixel_platformer/`.** Theme accents from CC0 OpenGameArt: [Pixel Art Forest](https://opengameart.org/content/pixel-art-forest-tilesets) for ECO. ARID/WIND use Kenney's sandy + sky tiles within the base pack. Decision validated 2026-05-12 via T-046a. **Architectural constraint:** the rendering layer must support swapping the entire tileset pack at runtime via `TilesetPack` + `TilesetRegistry` + `Settings.tilesetPackId` (see T-031). This keeps a future custom-art commission a one-line `register(...)` call rather than a renderer rewrite. |
| Monetization | Premium one-time purchase. Price: **$2.99–4.99**. Launch on **itch.io** first, then **Google Play**. No ads. No IAP. Educational angle is marketing, not a paywall mechanic. |
| v1.0 scope | **3 worlds × 3 characters = what we already have.** Ship v1.0 with the current 7 levels (4 tutorial + 3 campaign + Storm Sentinel boss + Cloud Atlas with 12 entries), polish it, then add World 4 / Char 4 in a free v1.1 update. |
| Platforms | Desktop (Windows/Mac/Linux via lwjgl3) + Android. iOS deferred. |

---

## Current state (Sprint D in progress)

Seven levels are playable end-to-end with a boss encounter, full audio, persistence, achievements, and stats tracking. Sprint D is underway — T-031 (tile renderer) is in progress; T-038, T-045, T-046b–T-049 are queued.

**Shipped systems:**
- **Levels:** Sky Sanctuary hub (Level0_0), tutorials Level0_1–0_4, campaign Level1 "First Rain" (Ebo), Level2 "Winds of Change" (Laya), Level3 "Stormy Heights" with boss arena
- **Characters:** Ebo (Seed Slam), Laya (Wind Dash), Zephyr (Float) — switchable mid-level
- **Boss:** Storm Sentinel (Level3) — 3-phase attack cycle (lightning columns, wind sweep, rest); defeated in 3 Seed Slam hits
- **Enemies:** Smog Sprite patrollers (patrol AI, stomp-defeat + Seed Slam defeat)
- **Audio:** 8 procedural SFX + 3 ambient music tracks with 1.5s crossfade between levels
- **Cloud Atlas:** 6 entries collectible in-game (target 12 via T-045)
- **Persistence:** 3 save slots, atomic writes, checkpoint autosave, time-trial best times
- **Achievements:** 12 achievements with in-game toast notifications (`AchievementRegistry` + `AchievementToast`) (T-037)
- **Stats screen:** Per-save-slot stats (deaths, levels completed, achievements) on main menu (T-041)
- **Display:** 4K/HiDPI via `DisplayScale` singleton; fullscreen + resolution presets in Settings (T-042)
- **Tile rendering (in progress):** `TilesetPack` / `TilesetRegistry` abstraction for runtime art-style swapping, wired to Kenney `pixel-platformer` base pack — CC0 assets already in repo at `assets/tilesets/kenney_pixel_platformer/` (T-031 in progress)
- **Pixel-art base assets:** Kenney `pixel-platformer` pack (~350 CC0 files: terrain, characters, enemies, pickups, hazards) committed to repo (T-046a)
- **Accessibility:** Assist Mode (invincibility, infinite spirits, slow-speed slider)
- **Controls:** Mobile two-thumb HUD (semi-transparent buttons) + keyboard, fully rebindable in Settings
- **Engine internals:** Celeste-calibrated movement, asymmetric gravity, coyote time, jump buffer, corner correction, wall-jump, particle pool, screen shake, hitstop, deferred body destruction, fixed-timestep physics
- **Testing:** 9 Kotest unit specs + AI smoke testing in CI (headless 8-level matrix via `xvfb-run`, gates `main` branch) (T-A1) + determinism audit with seeded RNG wrapper identified (T-A2)

---

## Three-horizon roadmap

### Horizon 1 — Sprint D "Ship-ready" (next 2 weeks)

No new tech. Finish what's in flight and get to a releasable alpha.

- ~~**T-037** — Achievement system + toast notifications (12 achievements)~~ ✓ Done
- ~~**T-041** — Stats screen on main menu~~ ✓ Done
- ~~**T-A1 / T-A2** — AI smoke testing in CI + determinism audit~~ ✓ Done
- ~~**T-046a** — Tileset research; Kenney pixel-platformer pack in repo~~ ✓ Done
- **T-031** — Tile-based terrain rendering (in progress — `TilesetPack`/`TilesetRegistry` abstraction + Kenney wiring)
- **T-035** — Audio bus sliders: Music / SFX / UI in Settings
- **T-038** — Ghost replay in time trials
- **T-045** — Cloud Atlas expansion to 12 entries (blocked on T-049 climate-source compilation)
- **T-034 doc surgery** — This ticket (brings GAME_PLAN, GDD_ADDENDUM, PATH1 in sync)
- **T-046 (long-term)** — Full graphics overhaul: pixel-art sprites + tilesets (blocked on T-031; art production)
- **Alpha launch** — First public build to itch.io (private link, ~5 testers)

**Antigravity research suite — queued for Antigravity execution:**
- **T-046b** — Character sprite-sheet research (CC0 candidates for Ebo/Laya/Zephyr; markdown only)
- **T-047** — Audio asset research: CC0 music + SFX supplements (OpenGameArt/FMA/Freesound candidates)
- **T-048** — Marketing research: itch.io listing style guide (8–12 reference listings analysis)
- **T-049** — Climate-source compilation for NotebookLM (verified-live NOAA/NASA/IPCC sources; unblocks T-045)

### Horizon 2 — Content (3–6 months)

- **T-046 ships in full** — Replace all procedural geometry on existing 7 levels with pixel-art assets
- Replace all ShapeRenderer primitives for terrain and characters with TextureRegion draws
- Polish and retune pacing of existing levels using playtest feedback
- Localization scaffold: `Strings.kt` key-based lookup; English-only at launch
- Store presence: itch.io page, screenshots, trailer, demo build

### Horizon 3 — Post-v1.0 (aspirational)

- World 4 + Character 4 free update (v1.1)
- Speedrun community: online leaderboards, ghost sharing
- Daily challenge mode (procedural level + modifier)
- Optional: level editor / Steam Workshop, Switch port

---

## Explicitly cut / deferred

| Item | Decision |
|---|---|
| Multiplayer co-op | Cut — probably never worth it at this scope |
| Characters 4–6 | Deferred to v1.1+ |
| Worlds 4–6 | Deferred to v1.1+ |
| iOS | Deferred (libGDX iOS toolchain complexity vs. audience size) |
| Ads / IAP | Cut entirely |
| Steam | Deferred until itch.io + Google Play prove traction |

---

## Open questions (genuinely undecided)

- **Localization timing:** Launch English-only, add languages post-v1.0? Or scaffold `Strings.kt` from day 1 to avoid a painful retrofit?
- **Demo build:** Standalone level-select with 3 sample levels, or first 3 levels free / rest paid?
- **Educational partnerships:** Pursue school/nonprofit licensing? Defer decision until v1.0 ships and we see uptake.

---

## What this doc is not

This is a plan, not a contract. Update it when scope or decisions change. If something you're building isn't on this page, either it belongs in TASKS.md already or this doc needs an edit.

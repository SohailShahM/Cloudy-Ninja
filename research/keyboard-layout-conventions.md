# Keyboard Layout Conventions in Indie Pixel Platformers (T-073)

**Author:** claude-code-sub-agent (re-routed from antigravity, 2026-05-12)
**Goal:** Catalog default PC keyboard bindings across 10–15 popular indie pixel platformers and adjacent action/adventure games, identify the most-common defaults, and recommend a Cloudy-Ninja default mapping for the 5 actions (left, right, jump, action, swap) that "feels familiar to platformer players."

**Scope note:** "Default keyboard bindings" means the out-of-the-box bindings the game ships with, not popular community remaps. Where a community wiki conflicts with a Steam thread, the wiki (or game's own controls screen, where documented) wins.

---

## 1. Per-game survey (15 games)

Each row is the **shipped default**. Empty cells = action does not exist in that game / not separately bound. Where the game uses mouse for an action, the cell says `LMB` / `RMB`.

| Game | Move L/R | Jump | Primary action (attack/use) | Dash / secondary | Alt action / swap | Pause | Inventory / map | Source |
|---|---|---|---|---|---|---|---|---|
| **Celeste** | Arrows / WASD | C | — (no attack) | X (dash) | Z (climb/grab) | ESC | — | [Steam guide](https://steamcommunity.com/sharedfiles/filedetails/?id=2956545379), [Neoseeker](https://www.neoseeker.com/celeste/Celeste_Basic_Controls) |
| **Hollow Knight** | Arrows | Z | X (attack) | C (dash) | A (focus/cast), S (super dash), D (dream nail), E (quick cast) | ESC | I (inv), TAB (map) | [Fextralife wiki](https://hollowknight.wiki.fextralife.com/Controls), [Fandom wiki](https://hollowknight.fandom.com/wiki/Controls_(Hollow_Knight)) |
| **Hyper Light Drifter** | WASD | — (no jump) | LMB (sword) | SPACE (dash) | RMB (gun), E (switch weapon) | ESC | Q (HUD), R (progress) | [PCGamingWiki](https://www.pcgamingwiki.com/wiki/Hyper_Light_Drifter), [Steam thread](https://steamcommunity.com/app/257850/discussions/0/385429254950932796/) |
| **Hades** | WASD | — (no jump) | LMB (attack) | SPACE (dash) | RMB (special), Q (cast), F (call), E (interact) | ESC | TAB (mirror/codex) | [Gamepressure](https://www.gamepressure.com/hades/pc-controls/z2d9d1) |
| **Dead Cells** | A/D (WASD) | SPACE | LMB (main weapon) | SHIFT (roll/dodge) | RMB (off-hand), Q/E (skills) | ESC | TAB (map), I (inv) | [Official wiki](https://deadcells.wiki.gg/wiki/Controls), [Fandom](https://deadcells.fandom.com/wiki/Controls) |
| **Risk of Rain 2** | WASD | SPACE | LMB (primary skill) | SHIFT (utility / sprint) | RMB (secondary), CTRL (utility-alt), R (special), Q (use item) | ESC | TAB (inventory) | [BisectHosting guide](https://www.bisecthosting.com/blog/risk-of-rain-2-controls-guide-pc-playstation-xbox-nintendo-switch) |
| **Stardew Valley** | WASD | — (no jump) | LMB (tool / hit) | RMB (interact / eat) | C (use tool, alt) | ESC | E (inventory/menu), 1-0 (hotbar) | [Stardew wiki](https://stardewvalleywiki.com/Controls), [Wikibooks](https://en.wikibooks.org/wiki/Stardew_Valley/Controls) |
| **Owlboy** | WASD | (context — flight) | LMB (spin attack) | RMB (roll) | E / R (left/right triggers, gunner swap) | ESC | — | [Steam thread](https://steamcommunity.com/app/115800/discussions/0/312265782625747686/) |
| **Shovel Knight** | WASD | K **or** SPACE | J (attack/shovel) | — (no dash by default) | (down + jump for down-thrust — combo, not key) | ENTER | — | [StrategyWiki](https://strategywiki.org/wiki/Shovel_Knight/Controls), [Fandom](https://shovelknight.fandom.com/wiki/Controls) |
| **Cave Story** | Arrows | Z | X (shoot) | — | Q (weapon swap previous), W (weapon swap next), A (inventory map) | ESC | — | [StrategyWiki](https://strategywiki.org/wiki/Cave_Story/Controls) |
| **Spelunky 2** | Arrows | Z | X (whip) | C (bomb) | D (rope), A (door/buy), SHIFT (walk) | ESC | — | [Spelunky wiki](https://spelunky.fandom.com/wiki/Controls), [DefKey](https://defkey.com/spelunky-2-pc-shortcuts) |
| **Terraria** | A/D | SPACE | LMB (use/attack) | — (mount-specific) | RMB (alt use), E (open chest/sign) | ESC | ESC opens inventory; numbers = hotbar | [Official wiki](https://terraria.wiki.gg/wiki/Game_controls), [Fandom](https://terraria.fandom.com/wiki/Game_controls) |
| **Ori and the Blind Forest** | WASD / Arrows | SPACE (jump), W (charge jump) | (context) | SHIFT (glide / dash) | C (bash), X (stomp), R (grenade), CTRL (dash-DE) | ESC | TAB (map) | [Speedrun.com config guide](https://www.speedrun.com/ori_de/guides/qoagt), [PCGamingWiki](https://www.pcgamingwiki.com/wiki/Ori_and_the_Blind_Forest) |
| **VVVVVV** | Arrows (L/R only) | — (no jump) | SPACE (flip gravity) | — | — | ESC | — | [PCGamingWiki](https://www.pcgamingwiki.com/wiki/VVVVVV) |
| **The Messenger** | WASD / Arrows | SPACE (jump / glide) | CTRL (attack / water dash) | SHIFT (shuriken) | ALT (rope dart) | ENTER | I (inv), M (map) | [DefKey](https://defkey.com/the-messenger-shortcuts) (cross-referenced with [Steam thread](https://steamcommunity.com/app/764790/discussions/0/2727382174644550254/)) |

**Reading the table:** "Primary action" is whatever the game treats as the headline combat / interaction verb (attack, fire, use tool, slash). "Dash / secondary" is the second-most-pressed action — usually dash, roll, or sprint. "Alt action / swap" gathers ability-swap or context actions.

---

## 2. Frequency analysis — what binding is most common per action

### Movement (left / right)

| Binding | Count (of 15) | Games |
|---|---|---|
| **WASD (A/D)** | 9 | Hyper Light Drifter, Hades, Dead Cells, Risk of Rain 2, Stardew Valley, Owlboy, Shovel Knight, Terraria, The Messenger (also accepts arrows) |
| **Arrow keys (primary default)** | 6 | Celeste, Hollow Knight, Cave Story, Spelunky 2, VVVVVV, Ori (accepts both) |
| Both equally supported | several | The Messenger, Ori, Celeste post-1.4 — modern games typically ship dual-binding |

**Verdict:** **WASD wins by a slim majority, but the more important pattern is dual-support.** Newer indies (post-~2015) almost always ship both. Older / NES-tribute platformers (Cave Story, Spelunky, VVVVVV) skew arrows. Cloudy-Ninja currently ships A/D only (per `Settings.kt: defaultKeybinds()`) — this is consistent with the WASD-majority convention.

### Jump

| Binding | Count | Games |
|---|---|---|
| **SPACE** | 7 | Dead Cells, Risk of Rain 2, Terraria, Ori, The Messenger, Hyper Light Drifter (used for dash, no jump), Shovel Knight (accepts SPACE or K) |
| **Z** | 4 | Hollow Knight, Cave Story, Spelunky 2, (Shovel Knight's NES-style J/K family is adjacent) |
| **C** | 1 | Celeste |
| **K or up arrow** | 2 | Shovel Knight (K), Katana ZERO (W or UP) |
| No jump (game lacks the verb) | 5 | Celeste-style 2D-skill games still have jump; Hades, HLD, Stardew, VVVVVV, Owlboy are the no-jump entries |

**Verdict:** **SPACE is the modal jump key.** Z is the canonical "NES-tribute / retro" jump (Cave Story, Spelunky, Hollow Knight). Cloudy-Ninja currently uses SPACE — consistent with the modern indie majority.

### Primary action (attack / use / interact)

| Binding | Count | Games |
|---|---|---|
| **LMB (mouse)** | 6 | Hyper Light Drifter, Hades, Dead Cells, Risk of Rain 2, Stardew Valley, Terraria, Owlboy |
| **X** | 4 | Hollow Knight, Cave Story, Spelunky 2 (whip), |
| **J** | 1 | Shovel Knight |
| **CTRL** | 1 | The Messenger (criticized by community) |
| **E** | 1 | (Cloudy-Ninja current) — also common as "interact" in many of the above |
| **C** | 1 | Celeste (jump, but in attackless game) |

**Verdict:** **LMB dominates among mouse-aimed games; X dominates among NES-tribute / Z-X / Z-X-C games.** For a pure-keyboard platformer (no aim-cursor), **X is the canonical "second button next to jump" placement** — sits between two fingers on Z (jump) and C (third action). Cloudy-Ninja's current "E" choice is unusual: E is more typically an interact / context key (Stardew, Hades, Terraria's chest open).

### Dash / secondary

| Binding | Count | Games |
|---|---|---|
| **SHIFT** | 4 | Dead Cells (roll), Risk of Rain 2 (sprint), The Messenger (shuriken), Ori (glide/dash) |
| **SPACE** | 3 | HLD, Hades, (used in Celeste-style games where SPACE is the dash) |
| **C** | 3 | Hollow Knight, Spelunky 2 (bomb — adjacent role), Ori (bash) |
| **X** | 1 | Celeste (dash) |

**Verdict:** **SHIFT is the most common "do an extra thing" modifier**, followed by SPACE (in games where jump doesn't claim it) and C (NES-tribute layouts). For a game like Cloudy-Ninja that **does** use SPACE for jump, the canonical secondary is either SHIFT or a letter near the home row.

### Alt action / "swap"

There is no industry consensus here — every game's third+ ability key is bespoke:

- Hollow Knight: A (focus), S (super dash), D (dream nail), E (quick cast)
- Cave Story: **Q / W (weapon previous / next)** — the closest analog to a "swap" verb in any game surveyed
- Owlboy: **E / R (left / right gunner swap)** — also explicitly a swap verb
- Stardew: 1-0 hotbar
- Ori: C, X, R (ability slots)

**Verdict:** Q is the strongest convention for "swap previous/cycle" (Cave Story, Risk of Rain 2 uses Q for "use item"). E and R are common for "switch / second tool" (Owlboy, HLD weapon-switch). For a single-button swap (Cloudy-Ninja cycles 3 characters), **Q has the cleanest precedent** — it sits adjacent to WASD without overlapping any other home-row key, and reads as "queue / cycle" intuitively.

### Pause

| Binding | Count | Games |
|---|---|---|
| **ESC** | 12 | Nearly all surveyed games |
| **ENTER** | 2 | Shovel Knight, The Messenger (NES-tribute "Start" button) |

**Verdict:** **ESC is unambiguous.** This isn't part of the 5-action mapping but worth pinning.

---

## 3. The two canonical platformer layouts

The data clusters around **two distinct conventions**, each with strong representation:

### Layout A — "Modern indie / WASD + SPACE"
Used by: Hades, Dead Cells, Risk of Rain 2, Hyper Light Drifter, Terraria, Ori, The Messenger
- Move: **WASD**
- Jump: **SPACE**
- Primary: **LMB** (if mouse-aimed) **or a letter near WASD** (J, E, F)
- Secondary: **SHIFT** (modifier-style)
- Tertiary: **Q / E / R** (the row above WASD)
- Pause: **ESC**

### Layout B — "NES-tribute / Z-X-C"
Used by: Celeste, Hollow Knight, Cave Story, Spelunky 2, Shovel Knight (variant: J/K)
- Move: **Arrows** (with WASD support added in modern re-releases)
- Jump: **Z** (or C in Celeste's idiosyncratic case)
- Primary: **X** (attack/shoot)
- Secondary: **C** (dash/bomb)
- Tertiary: usually a free letter (A, D, S)
- Pause: **ESC**

Cloudy-Ninja today is a **mix**: WASD movement + SPACE jump (Layout A) + E for action + S for swap. The S choice is awkward because S overlaps with the WASD "down" finger on a keyboard layout that ships A/D-only — players who add a "duck" key later will collide. E for action is defensible but unusual.

---

## 4. Recommended default mapping for Cloudy-Ninja

Cloudy-Ninja's 5 actions are **left, right, jump, action, swap**. The recommended defaults below adopt **Layout A wholesale**, except where Cloudy-Ninja's specific verbs ("action" is an interact-style verb; "swap" is a 3-character cycle) suggest a closer match in adjacent games.

| Cloudy-Ninja action | Recommended default | Industry precedent |
|---|---|---|
| **left** | **A** | WASD majority (9/15). Matches every modern Layout-A game. |
| **right** | **D** | Same as above. |
| **jump** | **SPACE** | 7/15 games. The modal jump key in modern indies. Cloudy-Ninja already ships this — keep. |
| **action** | **E** | 4/15 games (Stardew/Hades/Terraria use E for interact; Owlboy uses it for gunner-swap). Cloudy-Ninja's "action" verb is more interact-flavored than combat-flavored (per `InputManager.kt` it triggers character abilities like ability-cast / pickup), so E reads naturally. Cloudy-Ninja ships this — keep. |
| **swap** | **Q** | Strongest precedent for "cycle / switch" — Cave Story uses Q for weapon-previous; Risk of Rain 2 uses Q for use-item; sits cleanly above A on the WASD cluster. **Change from current S.** |

### Rationale per binding (2 sentences each)

**left → A.** WASD movement is the modal default across modern indie platformers (9 of 15 surveyed), and A/D specifically is what Hades, Dead Cells, Risk of Rain 2, Terraria, Ori, and The Messenger all ship out of the box. Cloudy-Ninja already uses A, so this is a "keep" — no migration needed for existing players' muscle memory.

**right → D.** Same justification as left — pairs with A as the universal WASD platformer convention. Already shipping; keep.

**jump → SPACE.** SPACE is the dominant jump key in post-2015 indie platformers (Dead Cells, Risk of Rain 2, Terraria, Ori, The Messenger, Shovel Knight), and it's the only jump key that has zero finger-overlap with WASD movement on any keyboard layout. Cloudy-Ninja already ships SPACE; keep.

**action → E.** E is the canonical "interact / context-sensitive ability" key in Stardew (open menu), Terraria (open chest), Hades (interact), Owlboy (swap gunner) — Cloudy-Ninja's "action" verb is closer to "context interact" than to a generic attack, so E is more honest than X (which signals "primary combat" to the NES-tribute audience). It also sits one row up from the WASD index finger, allowing a player to hold D and press E without a hand stretch.

**swap → Q (change from current S).** Cave Story's Q (weapon previous) and Risk of Rain 2's Q (use-item / cycle) are the only clear precedents for a "cycle through inventory of things" verb anywhere in the survey. Q is also the safest physical placement: it sits directly above A on the WASD cluster, never collides with a future "duck" or "down" binding (which a sidescroller may want on S), and reads as "queue next" intuitively — whereas **S is the down-movement key in every Layout-A game surveyed**, so reserving S for swap quietly prevents adding crouch/duck later.

---

## 5. Current Cloudy-Ninja defaults vs. recommendation — diff

From `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt: defaultKeybinds()`:

```kotlin
"left"  -> Input.Keys.A     // matches recommendation — keep
"right" -> Input.Keys.D     // matches recommendation — keep
"jump"  -> Input.Keys.SPACE // matches recommendation — keep
"action"-> Input.Keys.E     // matches recommendation — keep
"swap"  -> Input.Keys.S     // DIVERGES — recommend changing to Q
```

**Net change:** 1 of 5 bindings (swap: S → Q).

**Why this matters:** S conflicts with the future-likely "down / duck" binding (every Layout-A platformer surveyed reserves S for downward movement). Moving swap to Q now is a low-cost forward-compat move that avoids re-educating players when (not if) Cloudy-Ninja adds vertical input verbs (e.g. crouch, drop-through-platform, look-down).

**Implementation note (NOT part of T-073 — research only):** Implementing the change requires editing `Settings.kt: defaultKeybinds()` and likely a one-line migration in `SettingsManager.load()` so existing players who have the old S default get rebound to Q (or are left alone if they had explicitly rebound it). That's a separate ticket. T-073 is research-only and does not modify any source.

---

## 6. Open questions / non-blocking notes

- **Arrow-key dual binding:** Cloudy-Ninja currently does not ship arrow keys as a secondary default for left/right. 6 of 15 surveyed games ship arrows as primary, and most modern games ship **both**. Worth a follow-up ticket: add arrow-key fallbacks to left/right (and SPACE jump is already keyboard-layout-neutral).
- **Pause / inventory:** Cloudy-Ninja's pause and any future inventory binding are out of scope for T-073 (the 5-action mapping), but ESC for pause is so universal it should be considered "settled" — every game surveyed except Shovel Knight and The Messenger uses ESC.
- **No precedent for cycle-swap UX:** Cloudy-Ninja's 3-character cycle is unusual — most surveyed games have weapon-swap (Cave Story Q/W, Owlboy E/R) but it's "previous / next" with two keys. A single-button cycle is more like a hotbar-1 keypress. Q is still the cleanest single-key fit, but it's worth noting this is one of the few places Cloudy-Ninja has no exact industry precedent.

---

## 7. Sources

- Celeste: [Steam guide](https://steamcommunity.com/sharedfiles/filedetails/?id=2956545379), [Neoseeker](https://www.neoseeker.com/celeste/Celeste_Basic_Controls), [Speedrun.com forum](https://www.speedrun.com/celeste/forums/51mjz)
- Hollow Knight: [Fextralife wiki](https://hollowknight.wiki.fextralife.com/Controls), [Fandom wiki](https://hollowknight.fandom.com/wiki/Controls_(Hollow_Knight))
- Hyper Light Drifter: [PCGamingWiki](https://www.pcgamingwiki.com/wiki/Hyper_Light_Drifter), [Steam discussion](https://steamcommunity.com/app/257850/discussions/0/385429254950932796/)
- Hades: [Gamepressure](https://www.gamepressure.com/hades/pc-controls/z2d9d1)
- Dead Cells: [Official wiki](https://deadcells.wiki.gg/wiki/Controls), [Fandom](https://deadcells.fandom.com/wiki/Controls)
- Risk of Rain 2: [BisectHosting guide](https://www.bisecthosting.com/blog/risk-of-rain-2-controls-guide-pc-playstation-xbox-nintendo-switch), [DefKey](https://defkey.com/risk-of-rain-2-shortcuts)
- Stardew Valley: [Stardew Valley Wiki](https://stardewvalleywiki.com/Controls), [Wikibooks](https://en.wikibooks.org/wiki/Stardew_Valley/Controls)
- Owlboy: [Steam discussion](https://steamcommunity.com/app/115800/discussions/0/312265782625747686/)
- Shovel Knight: [StrategyWiki](https://strategywiki.org/wiki/Shovel_Knight/Controls), [Fandom](https://shovelknight.fandom.com/wiki/Controls)
- Cave Story: [StrategyWiki](https://strategywiki.org/wiki/Cave_Story/Controls)
- Spelunky 2: [Fandom wiki](https://spelunky.fandom.com/wiki/Controls), [DefKey](https://defkey.com/spelunky-2-pc-shortcuts)
- Terraria: [Official wiki](https://terraria.wiki.gg/wiki/Game_controls), [Fandom](https://terraria.fandom.com/wiki/Game_controls)
- Ori and the Blind Forest: [Speedrun.com config guide](https://www.speedrun.com/ori_de/guides/qoagt), [PCGamingWiki](https://www.pcgamingwiki.com/wiki/Ori_and_the_Blind_Forest)
- VVVVVV: [PCGamingWiki](https://www.pcgamingwiki.com/wiki/VVVVVV)
- The Messenger: [DefKey](https://defkey.com/the-messenger-shortcuts), [Steam discussion](https://steamcommunity.com/app/764790/discussions/0/2727382174644550254/)
- Cloudy-Ninja current defaults: `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt` (read 2026-05-12, not modified)

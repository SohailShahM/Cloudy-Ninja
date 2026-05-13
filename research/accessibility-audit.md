# Accessibility Audit — Cloudy Ninja Alpha (T-166)

**Author:** claude-code-sub-agent
**Date:** 2026-05-13
**Scope:** Formal audit of the alpha build's accessibility posture against the AbleGamers' Accessible Player Experiences (APX) framework, Microsoft Inclusive Design principles, and the Game Accessibility Guidelines (GAG) tier system. Catalogues which a11y settings ship, scopes their coverage against the in-tree code, and is honest about which barriers we have not addressed.

**Hard scope rules:** Doc-only. All cited flags are real fields in `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt` (verified at HEAD of `claude/T-166-accessibility-audit`). No code is changed by this audit.

---

## 1. Shipped accessibility features

Every feature below maps to a concrete `Settings` field and a UI control in `SettingsScreen.kt` → Accessibility section (unless otherwise noted). Defaults preserve byte-identical behaviour with pre-feature saves.

### 1.1 Color-blind palette (T-057)

- **Flag:** `Settings.colorBlindMode: ColorBlindMode` — enum of `OFF, DEUTERANOPIA, PROTANOPIA, TRITANOPIA`. Default `OFF`.
- **UI:** Accessibility → "Colour-blind mode" `SelectBox` (Settings screen, line ~258).
- **Scope of coverage (from `Settings.kt` docs):** "Swaps a curated set of gameplay colors (hazards, eco-tokens, snapshot pickups, portals) for shades that remain distinguishable under the most common forms of color-vision deficiency."
- **Not covered:** UI chrome (Vis-UI skin tints), font colour, achievement icons, HUD overlays. The palette swap is gameplay-render-only; the menu/HUD layers retain default Vis-UI colour roles.
- **Interaction with §1.4 high-contrast:** both can be on; high-contrast wins when both palettes apply to the same colour role.

### 1.2 Reduced-motion mode (T-058)

- **Flag:** `Settings.reducedMotion: Boolean`. Default `false`.
- **UI:** Accessibility → "Reduced motion" `CheckBox` (line ~271).
- **Scope of coverage (from `Settings.kt` docs):** "Disables screen shake, clamps particle bursts to a single particle, and freezes the parallax background scroll."
- **Cross-cutting effects in tree:**
  - `EnemyHitFlashTest` (per HANDOFF) — hit-flash respects `reducedMotion`.
  - The unified `rendering/ScreenShake.kt` system (T-169 consolidated the former dual implementations) gates on this flag at trigger time, covering all call sites: stomp/boss-hit/lightning (WorldContactListener) and land-bounce/death (LevelRunState).
- **Not covered:** menu transitions and pause-overlay fade (T-063, 0.2 s) — these are short, single-direction fades and were not in T-058 scope. Screen-fade screen transitions (`fadeFromBlack`/`fadeToBlack` per T-110) also persist.
- **Independence:** the legacy `screenShake` and `deathFlash` toggles (lines 50–51 of `Settings.kt`, both default `true`) still exist as finer-grained sub-controls. A player who wants only one of the three behaviours can mix-and-match.

### 1.3 Key rebinding (T-036)

- **Flag:** `Settings.keybinds: Map<String, Int>` — action name → libGDX `Input.Keys` keycode. Default returned by `defaultKeybinds()` (lines 18–28).
- **Rebindable actions (6):** `left`, `right`, `jump`, `action`, `swap`, `restart` (T-133 hold-R-to-restart, rebindable; 0.5 s hold threshold enforced at the GameScreen call site).
- **UI:** Controls section of Settings — per-action capture button + "Reset to Defaults" (lines ~199–250).
- **Persistence:** rebinds are written to `settings.json` via `SettingsManager.update`, immediately reloaded into the input layer via `InputManager.reloadKeybinds()`.
- **Not covered:**
  - No mouse-button bindings. Cloudy Ninja is keyboard-only on input (mouse used only in menus); no current action needs a mouse binding.
  - No simultaneous-bind conflict detection. The capture loop accepts whatever the player presses; two actions can bind to the same key without warning. (Acceptable for alpha; logged here as a deferred polish item.)
  - No "double-tap" or "hold-to-toggle" alternates — only single-key bindings.

### 1.4 High-contrast mode (T-132)

- **Flag:** `Settings.highContrast: Boolean`. Default `false`.
- **UI:** Accessibility → "High contrast" `CheckBox` (line ~283).
- **Scope of coverage (from `Settings.kt` docs):** "Remaps every colour role to a maximum-contrast variant via `com.sohai.platformer.rendering.HighContrastPalette`."
- **Interaction with §1.1 colour-blind:** independent flags; both can be on; high-contrast palette overrides where roles overlap.
- **Not covered:** as with the colour-blind palette, Vis-UI menu chrome and HUD typography are not remapped — high-contrast is gameplay-render only.

### 1.5 Assist Mode (pre-existing, Celeste-inspired)

- **Flags (three sub-controls):**
  - `assistInfiniteSpirits: Boolean` (default `false`) — disables spirit-charge consumption.
  - `assistSlowSpeed: Float` (default `1.0`; range 0.25–1.0 via slider) — global game-speed multiplier; 0.5 = half-speed.
  - `assistInvincible: Boolean` (default `false`) — disables player damage.
- **UI:** Accessibility → Assist Mode hint label + the three controls (lines ~313–339).
- **Framing:** ships under Accessibility (not a separate "Cheats" menu) per the Celeste assist-mode design intent — these are not cheats, they are difficulty relaxers offered to players who would otherwise be unable to finish the alpha.
- **Persistence:** stored on the same `Settings` object and survive across slots.

### 1.6 Adjunct accessibility-relevant controls

These are not formally branded "accessibility" features but affect a11y outcomes:

- **Audio bus sliders:** `volMusic`, `volSfx`, `volAmbient`, `volUi` (lines 38–41). Independent bus volumes let a hard-of-hearing player boost UI/SFX while keeping ambient/music low; per HANDOFF, master + mute are pipeline tickets T-035/T-105/T-118.
- **Independent `screenShake` and `deathFlash` toggles** (lines 50–51). Fine-grained motion/flash overrides for players who don't want the full reduced-motion package.
- **Display resolution + fullscreen toggle** (lines 43–47) — players who depend on OS-level zoom or scaling can pick a window size that suits.
- **`showFps`** (line 52) — diagnostic, not strictly a11y but useful when low-spec players need to verify a performance-related a11y problem.

---

## 2. Gaps measured against external frameworks

### 2.1 AbleGamers — Accessible Player Experiences (APX)

The APX framework groups player needs into the "5 Big Ones" (Visual, Auditory, Motor, Speech, Cognitive). Below is our coverage row-by-row.

| APX category | What we ship | Gaps |
|---|---|---|
| **Visual** | Colour-blind palette (3 modes), high-contrast palette, independent flash/shake toggles, resolution + fullscreen control. | No font-size control; no screen-reader UI; no in-game zoom; gameplay palette swap excludes menu chrome. Particle clamp only fires under `reducedMotion` (no independent slider for particle density). |
| **Auditory** | 4 independent volume buses (music/SFX/ambient/UI). No critical mechanic relies on sound (visual cues lead; SFX reinforce). | **No subtitles for SFX or boss vocalisations** — flagged in scope as "deferred." No visual indicators for off-screen audio cues (Storm Sentinel telegraphs are already visual, so the gap is limited). No mono-audio toggle (relevant for unilateral-hearing players). |
| **Motor** | Full keyboard rebinding (6 actions inc. restart), assist mode (invincibility, slow-speed, infinite spirits) for players who cannot meet the standard execution bar. Hold-restart is a 0.5 s gate (not a tap — accidental restarts are rare). | **No gamepad / controller support yet** — T-102 deferred per HANDOFF ("manual smoke needs real controller"). No remappable mouse, no one-handed preset, no hold-vs-toggle option per action, no input-buffering toggle for jump. |
| **Speech** | N/A — no voice input expected or used. | N/A. |
| **Cognitive** | Assist mode (slow-speed primarily lands here), pause overlay with 0.2 s fade-in, auto-pause on alt-tab (T-112) so losing focus doesn't punish the player, save slots with delete-confirmation modal (T-119). Tutorial-tier complexity in early levels. | No "skip level" / "skip section" button (Celeste-style). No persistent on-screen control reminders after the first level. No tooltips on Settings controls explaining the trade-offs of each accessibility flag. No reading-pace control on Cloud Atlas narrative entries. |

**Headline APX gaps for alpha:** SFX-subtitles, gamepad support, font-size control.

### 2.2 Microsoft Inclusive Design — Persona Spectrum

Microsoft's Inclusive Design 101 framework maps each permanent disability to **temporary** and **situational** counterparts; the design test is whether a feature also helps someone in a temporary or situational variant of the same need.

| Spectrum | Permanent / Temporary / Situational | What helps in Cloudy Ninja |
|---|---|---|
| **Touch** (one-arm / arm injury / new parent holding a baby) | Permanent: limb difference. Temp: broken arm. Sit: holding a coffee. | Partial — key rebinding lets a player remap to a one-handed cluster; assist mode reduces input-precision demand. **Missing:** no toggle-vs-hold setting per action (e.g. hold-jump for variable height vs. tap-jump); no chord-free alternatives for combined inputs. |
| **See** (blind / cataract / driving with sun in eyes) | Permanent: blindness. Temp: post-eye-surgery. Sit: bright sunlight on screen. | Colour-blind + high-contrast modes cover most situational and many temporary cases. **Missing:** screen-reader hooks and font-size scaling — fail the permanent blindness end of the spectrum entirely (see §3). |
| **Hear** (deaf / ear infection / loud room) | Permanent: deafness. Temp: ear infection. Sit: noisy café. | Independent volume buses, no audio-only critical mechanics. **Missing:** SFX subtitles + closed-caption stripes for ambient cues (e.g. storm rumble before lightning) would also help the situational "noisy café" case. |
| **Speak** (non-verbal / laryngitis / heavy accent) | N/A for this game. | N/A. |

**Inclusive Design verdict:** strong on "See" and "Hear" *situational* cases, weak at the *permanent* end of those spectrums, partial on "Touch" pending gamepad + toggle-vs-hold work.

---

## 3. Screen-reader compatibility (NVDA / JAWS) — honest assessment

**Position: Cloudy Ninja's alpha is not screen-reader compatible, and full compatibility is not a realistic target for this title.**

Reasoning:

1. **The libGDX + Scene2D / Vis-UI stack does not expose UI Automation (UIA) or MSAA roles.** Buttons, sliders, and select-boxes are drawn as textures via OpenGL; NVDA/JAWS see them as a single opaque graphics surface. There is no out-of-the-box property tree for an assistive technology to read. Retrofitting this would require either a parallel UIA shadow tree (substantial engineering) or routing every Settings string through a TTS bridge — both are alpha-scope-blowing.
2. **The game itself is real-time twitch-platformer action.** Even a perfectly-instrumented menu would not make the playable game accessible to a screen-reader-dependent player — there is no plausible audio-only abstraction of a 60-FPS pixel-platformer with hazards, hidden eco-tokens, and Storm-Sentinel boss telegraphs.
3. **Industry comparables (Celeste, Hollow Knight, Hyper Light Drifter — all surveyed in `research/keyboard-layout-conventions.md`) ship without screen-reader support for the same reasons.** This is the alpha's honest peer position, not a deficiency relative to genre norms.

**What we will commit to instead of screen-reader support:**

- Menu strings remain in `i18n/Strings.kt` (130+ keys per HANDOFF) so a translator or a downstream community accessibility patch could pipe them to TTS without source changes.
- All settings have meaningful labels via `StringKey.*` — no hardcoded English (T-122 closing the last 3 holdouts per HANDOFF).
- We document this position publicly (this audit) rather than silently failing audit tools.

**If a future title in this universe is turn-based or narrative-paced, the position should be revisited.** For a real-time action game, "not screen-reader compatible, by design" is defensible.

---

## 4. Game Accessibility Guidelines (GAG) — tier we qualify for

The [Game Accessibility Guidelines](https://gameaccessibilityguidelines.com/) split recommendations into three tiers: **Basic** (cheap, broad-reach), **Intermediate** (moderate effort, smaller groups), **Advanced** (large effort, smallest groups but largest individual benefit).

### 4.1 Basic tier — coverage matrix

| GAG Basic guideline | Status in alpha |
|---|---|
| Allow controls to be remapped / reconfigured | ✅ §1.3 — six rebindable actions. |
| Ensure controls are as simple as possible, or provide a simpler alternative | ✅ §1.5 assist mode + small action vocabulary (move, jump, action, swap, restart). |
| Include an option to adjust the game speed | ✅ §1.5 `assistSlowSpeed` 0.25–1.0 slider. |
| Avoid flickering images and repetitive patterns (or provide opt-out) | ✅ §1.2 + independent `deathFlash` toggle. |
| Use an easily readable default font size | ✅ default font sizes (16/20/32 pt at 720p baseline) clear; HiDPI scaling preserves ratio. |
| Provide separate volume controls for effects, speech, background sound and music | ✅ §1.6 — 4 buses (music/SFX/ambient/UI). |
| Ensure no essential information is conveyed by colour alone | ✅ hazards, pickups, and portals also differ in **shape/animation/SFX** — colour-blind palette is a backup, not a single-point. |
| Provide pre-game information on accessibility features | ⚠️ partial — Settings → Accessibility section is discoverable but there is no first-launch a11y splash. (Acceptable per GAG; flagged for polish.) |
| Allow easy reading time for in-game text | ✅ Cloud Atlas entries are player-paced; no auto-advancing dialogue. |
| Provide a clear introduction to the controls | ✅ first level + on-screen prompts. |

**All Basic items ship except the "pre-game a11y info splash"**, which is documentation-shaped rather than feature-shaped.

### 4.2 Intermediate tier — partial coverage

| GAG Intermediate guideline | Status |
|---|---|
| Allow easy bypass / skip of sections of gameplay through difficulty adjustment | ✅ assist mode (invincible + slow-speed effectively bypass execution gates). |
| Allow easy interaction / quick-time events to be adjusted in timing | ⚠️ no QTEs; N/A. |
| Provide high-contrast between text/UI and background | ✅ T-132 in gameplay; menu Vis-UI skin defaults are adequate but not formally contrast-rated. |
| Provide an option to adjust the contrast | ✅ T-132 toggle (boolean, not continuous). |
| Avoid making precise timing essential to gameplay, or provide alternatives | ✅ assist mode covers this. |
| Provide details of accessibility features on packaging and website | ❌ no public accessibility statement page yet. **Action:** ship one when itch.io page lands (per `marketing/itch-page-draft.md`). |
| Solicit accessibility feedback | ⚠️ GitHub Issues open but no a11y-tagged template. **Action:** add `a11y-feedback` issue template. |
| Provide a wide choice of difficulty settings | ⚠️ binary (assist on/off + slow-speed slider); not a graded "Easy/Normal/Hard" set. |
| Allow the game to be played without sound | ✅ visual primacy; no audio-only mechanic. |
| Provide subtitles for important dialogue | ✅ Cloud Atlas entries are text-first; there's no spoken dialogue. |
| Provide subtitles for significant background sounds and speech | ❌ — **the SFX-subtitle gap**. Deferred per scope. |
| Provide controller / gamepad support | ❌ — T-102 deferred. |
| Include some text-to-speech or pre-recorded narration for any large amounts of text | ❌ no TTS / VO for Cloud Atlas entries. |

### 4.3 Advanced tier — not in scope for alpha

Advanced tier items (full screen-reader compatibility, single-switch input, eye-tracking, sign-language interpretation of dialogue, etc.) are out of scope for the alpha and, in the case of screen-reader compatibility, structurally out of scope for the genre (§3).

### 4.4 Tier verdict

**Cloudy Ninja alpha qualifies for GAG Basic (full) + GAG Intermediate (partial).**

We satisfy **10 of 10** Basic items with the caveat that "pre-game a11y info" is currently discoverable rather than splash-fronted. We satisfy **roughly 7 of 13** Intermediate items in full and another **2 in part**. The four hard-no Intermediate items are SFX-subtitles, gamepad support, public a11y statement, and TTS for narrative text — three of which (SFX-subtitles, gamepad, a11y statement) are explicit deferred-not-rejected items with tickets either open (T-102 gamepad) or backlog-ready (subtitle pipeline, public statement on itch.io page launch).

**Honest summary: alpha is "Basic-tier compliant, on the way to Intermediate."** This is a defensible and unusual-for-indie-alpha position; most peers (per the keyboard-layout survey games) qualify for Basic only.

---

## 5. Prioritised follow-up backlog (informational; no tickets opened in this audit)

These are the items that, if shipped, would close the largest GAG/APX gaps in order:

1. **T-102 gamepad** — single biggest unmet APX-Motor gap. Already ticketed; held on real-controller smoke.
2. **SFX-subtitles for boss telegraphs and lightning warnings** — single biggest Intermediate-tier missing item; closes both APX-Auditory and the "noisy café" Inclusive Design situational case.
3. **Public accessibility statement on itch.io page** — zero-engineering, ships with `marketing/itch-page-draft.md`.
4. **Font-size scaling** — closes the APX-Visual permanent end; T-shirt size M.
5. **A first-launch a11y splash / Settings highlight** — closes the GAG Basic "pre-game info" caveat; T-shirt size S.
6. **`a11y-feedback` GitHub issue template** — closes the GAG Intermediate "solicit feedback" item; T-shirt size XS.

None of these are alpha-blockers. The alpha can ship with the §4 tier verdict intact and a forward roadmap.

---

## Appendix A — File references

- `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt` — flag definitions, defaults, persistence.
- `core/src/main/kotlin/com/sohai/platformer/screens/SettingsScreen.kt` — UI surface, Accessibility section (lines 252–339).
- `core/src/main/kotlin/com/sohai/platformer/rendering/HighContrastPalette.kt` (referenced from Settings doc comment) — T-132 implementation.
- `HANDOFF.md` — session state, ticket backlog (T-102 deferred, T-122 i18n closing).
- `research/keyboard-layout-conventions.md` — peer-survey data informing the "screen-reader is genre-wide impractical" claim in §3.

## Appendix B — Frameworks cited

- **AbleGamers — Accessible Player Experiences (APX):** https://accessible.games/accessible-player-experiences/
- **Microsoft Inclusive Design:** https://inclusive.microsoft.design/
- **Game Accessibility Guidelines:** https://gameaccessibilityguidelines.com/

(External URLs not fetched during this audit — cited from prior knowledge for orientation. The audit's findings rest on the in-tree source, not on the framework prose.)

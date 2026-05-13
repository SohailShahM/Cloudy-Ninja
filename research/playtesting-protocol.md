# Playtesting Protocol — Cloudy Ninja Alpha (T-155)

**Author:** claude-code-sub-agent
**Compiled:** 2026-05-13
**Status:** Draft v1 — applies to the ~5-tester private alpha that ships off Sprint D (see `GAME_PLAN.md` → Horizon 1 → "Alpha launch"). Re-evaluate after the first cohort closes; protocol changes go via a new ticket, not in-place edits to this file.

> **What this is:** a repeatable script for running structured playtest sessions on the alpha build. It covers who to recruit, how to run the session, what questions to ask, what data to capture, and how to turn the output into ticket candidates.
>
> **What this is not:** a marketing survey, a Steam-wishlist optimization tool, or a generic "did you have fun?" form. The point is to surface concrete UX, accessibility, and difficulty defects we can ticket and fix before v1.0 — and to validate that the eco-restoration angle and character-switch mechanic are reading the way the design assumes.
>
> **Cross-refs:**
> - Alpha build positioning: `marketing/itch-page-draft.md` (T-124)
> - Crash-log capture mechanism: T-115 — writes to `<userHome>/.cloudy-ninja/crashes/crash-{ts}.log`
> - Screenshot capture: T-139 (in-game screenshot hotkey → `<userHome>/.cloudy-ninja/screenshots/`)
> - Default keyboard layout assumptions: `research/keyboard-layout-conventions.md` (T-073)
> - Accessibility features under test: T-057 (color-blind palettes), T-058 (reduced-motion), T-035-derived rebind UI, Assist Mode
> - Character-swap default key change in flight: T-121 (S→Q). If T-121 has not merged at session time, use the build's actual default and note it in the session log.

---

## 1. Recruitment

### Cohort size and shape

The alpha targets **5–8 testers total**, run as **two waves** of 3–4 each so that fixes from wave 1 can land before wave 2 sessions. This is small-N qualitative research, not a stats exercise — we are looking for repeated failure modes, not statistical significance. A single tester hitting a hard navigation block is signal; an N=2 agreement is strong signal.

### Target audience — three subgroups, recruited intentionally

We are NOT recruiting "anyone who plays games." Each subgroup tests a specific design assumption baked into the pitch:

| Subgroup | What we are testing they validate | Target headcount |
|---|---|---|
| **Indie platformer players** | Movement feel, swap-mid-jump mechanic, level pacing read against Celeste/Hollow Knight/Ori expectations | 3 |
| **Eco / climate-curious players** | Cloud Atlas entries read as interesting, not preachy; restoration framing lands without explicit lecture | 2 |
| **Accessibility-needs players** | Assist Mode is genuinely usable; color-blind palettes work for the corruption→clean visual gradient; reduced-motion mode is not a degraded experience; key rebinding is discoverable and complete | 2 |

It is fine for one tester to span subgroups (e.g. a colorblind platformer veteran covers groups 1 and 3). The headcounts above are minimums per subgroup, not exclusive buckets.

### Recruitment channels (priority order)

1. **Personal network — direct outreach.** The fastest path. Frame it as "20 minutes of your time + a free copy of the v1.0 release." Bias-aware: avoid people who already know the game's design rationale, since their feedback on clarity is contaminated.
2. **Itch.io feedback DM via the page's "Contact" link.** Soft-recruit from anyone who downloads the unlisted alpha build. Higher noise, but unfiltered "stranger touching the game for the first time" is the gold-standard data we cannot get from friends.
3. **r/playmygame and r/indiegaming** — single post per subreddit, lead with the eco-restoration hook (it is the strongest differentiator). Expect 0–2 replies; this is supplementary, not primary.
4. **Accessibility-specific:** r/blind, r/colorblind, the `caniplaythat.com` Discord. Disclose accessibility features in the recruitment message — testers self-select against their needs. **Do not** ask testers to disclose specific disability details; ask them what they need from the game.

### Disqualifying / undesirable conditions

- Anyone who has already played Cloudy Ninja in any pre-alpha build — they have priors we can't reset.
- Anyone unwilling to be screen-recorded or to share crash logs / screenshots (these are the primary data captures; without consent the session is unusable).
- Anyone who cannot commit to the full 25-minute slot (15-min play + 10-min interview). Truncated sessions skew toward "first impressions only" data we already get from the itch.io page.

### Consent script (read at session start)

> "I'm running a 25-minute playtest for an unreleased game. We'll spend ~15 minutes with you playing while I take notes and a screen recording, and ~10 minutes after for a structured interview. I'll be asking you to think out loud — say what you're noticing, what's confusing, what you're trying to do. There are no wrong answers and nothing here is a test of your skill; we're testing the game, not you. If the game crashes, that's useful data. If you want to stop, say so and we stop with no questions asked. Any output of the session — recording, notes, transcripts — is internal to the dev team only and won't be published. Are you good to go?"

The consent script is mandatory. Skipping it is a recruitment failure — re-run.

---

## 2. Session structure

Each session is **25 minutes**, divided into one **15-minute hands-on segment** and one **10-minute interview segment**. Sessions are conducted remotely via voice + screen share (Discord, Zoom, or whatever the tester already has installed — do not require a new install).

### Pre-session setup (facilitator, 5 min before tester joins)

1. Confirm the alpha build version the tester downloaded matches the build under test. Ask the tester to read `Settings → About` or the main-menu build label (`v0.1.0 · 2026-05-12` style, see T-100).
2. Confirm `<userHome>/.cloudy-ninja/crashes/` is empty (or note its starting contents) so any new crash log is attributable to this session.
3. Open a fresh session-notes file using §4's template.
4. Have the question bank (§3) open in a second window.
5. Mute notifications.

### 0:00–2:00 — Intro + consent

Read the consent script (§1). Confirm screen-share is live. Confirm the tester can hear audio from the game (the game ships with 3 ambient tracks + 8 SFX; muted audio invalidates several question-bank items in §3.4 and §3.5).

### 2:00–4:00 — Cold start, no instructions

Ask the tester to launch the game **without** any guidance from us. We are watching for:
- Whether the cold-start splash + asset preload (T-104) reads as "loading" or as "broken / hung."
- How they navigate the main menu — do they find New Game on the first try? Do they notice the build label? The Achievements: N/13 counter?
- Whether they read the controls anywhere before pressing keys (most testers don't — note who does).

Do not answer questions about controls in this window. Say: "I want to see what you figure out on your own — try things."

### 4:00–14:00 — Open play through tutorial + Level 1

The tester plays Level0_0 (Sky Sanctuary hub) → Level0_1 → Level0_2 → ideally into Level1 ("First Rain"). If they get hard-stuck for 90+ seconds with no progress, give a single targeted hint and note it as a navigation defect. Do not coach them past the second hint — let them quit-out into the main menu and try again, or end the play segment early.

**Facilitator behavior:**
- **Encourage think-aloud:** every 60–90 seconds of silence, prompt "what are you noticing?" or "what are you trying to do right now?"
- **Take notes on the §4 template in real time.** Do not pause the tester to write.
- **Do not pre-emptively explain.** If the tester asks "is this a bug?", reply "what do you think?" and capture both their interpretation and your ground truth.
- **Note the timestamp** (session-relative, e.g. `+04:30`) on every defect / friction event. Recordings get reviewed later by ticket number.

### 14:00–15:00 — Free-form 60-second cooldown

Let the tester finish whatever they're mid-attempt on, or stop. Ask: "Anywhere you want to revisit before we switch to questions?" Some testers ask to re-try the boss or visit Settings — let them, briefly.

### 15:00–25:00 — Structured interview

Work the question bank (§3) in order. Skip questions that are obviously redundant given what they already volunteered (note "covered in-session" rather than re-asking — re-asking erodes their trust).

### Post-session (facilitator, immediately after)

1. Save the session-notes file under `playtesting/session-{date}-{tester-pseudonym}.md` (this dir does not exist yet — create on first session; do NOT commit notes to git, they live in a local-only or shared-team-only location per the consent script).
2. Pull any new `crash-*.log` files from `<userHome>/.cloudy-ninja/crashes/` and any new `screenshot-*.png` files from `<userHome>/.cloudy-ninja/screenshots/` into the same session folder.
3. Save the screen recording.
4. Within 24 hours, write the synthesis pass (§5) — defects fade in memory fast.

---

## 3. Question bank

The questions below are **structured prompts**, not a survey. They are open-ended where we want story, closed where we want a discrete answer for cross-session comparison. Asked in order. Skip duplicates.

### 3.1 First impressions (target: 90 seconds)

1. "Before you launched, what did you think this game was going to be?" *(Tests whether the itch.io page sets correct expectations — feeds back into `marketing/itch-page-draft.md`.)*
2. "In the first 30 seconds of seeing the main menu, what did you notice first?"
3. "Did the cold-start splash read as 'the game is loading,' 'the game is broken,' or something else?"
4. "Did you notice the achievements counter on the main menu? Did it make you want to play differently?" *(Tests T-099's read; we expect "noticed but didn't change behavior" — anything else is interesting.)*

### 3.2 Navigation clarity (target: 90 seconds)

1. "In Sky Sanctuary, did you know where to go? At what moment did the path become obvious — or did it never?"
2. "When you entered the first tutorial level, did the level itself teach you what to do, or did you have to guess?"
3. "Was there ever a moment you got stuck and didn't know if it was you, the level, or a bug?" *(This is the highest-signal navigation question — capture verbatim.)*
4. "If you used the pause menu (ESC), did you find what you were looking for? Anything missing?"

### 3.3 Ability discovery (target: 120 seconds)

1. "Who did you think Ebo was, the first time you saw him? What did you expect Seed Slam to do, before you used it?"
2. "Same question for Laya and Wind Dash."
3. "Same for Zephyr and Float."
4. "Did any of the three abilities feel like they did something different than what their name implied?" *(Edge case — we expect Float to underperform expectations; capture if so.)*
5. "Was there an ability you didn't realize you had until partway through the session?"
6. "If you remember: which character do you think you used the most? The least?" *(Cross-check against the screen recording — many testers misremember.)*

### 3.4 Character-swap intuitiveness (target: 120 seconds)

> **This is the most important question block in the bank.** Mid-jump character swapping is the game's signature mechanic; if it doesn't read clearly, the entire pitch breaks.

1. "When did you realize you could switch characters?" *(In Level0_3 by design; if the answer is "later," that's a tutorial defect.)*
2. "Did you ever try to switch in mid-air? Did it work the way you expected?"
3. "What key did you press to switch?" *(Tests the T-121 default-key migration: if the answer is `S` and the build has `Q`, or vice versa, the rebind UX is invisible. Pre-T-121: the default is `S`. Post-T-121: `Q`. Confirm the build under test before judging the answer.)*
4. "Was the order of the swap cycle (Ebo → Laya → Zephyr → Ebo) intuitive? Would you have ordered them differently?"
5. "Was there ever a moment when you swapped to the wrong character mid-jump? What happened?" *(Captures the actual failure mode we'd ticket — e.g. "I died because I expected Laya but got Zephyr.")*
6. "If you could redesign one thing about how character swapping works, what would it be?"

### 3.5 Accessibility — color-blind, reduced-motion, key rebinding (target: 180 seconds)

> Ask the full block even of testers who do not self-identify as needing accessibility — they're the control case for whether the features are visible / discoverable at all. Adjust depth based on what the tester actually uses.

**Color-blind palettes (T-057):**

1. "Did you notice the game has 4 color-blind palettes in Settings? If yes — did you try them?"
2. "Looking at the corruption-vs-restoration visual cue (smog gray → clean green), can you tell them apart easily? On every palette you tried?" *(Critical question for the eco-restoration mechanic.)*
3. "Were there any specific UI elements where the color choice felt off — health, ability cooldown, achievement toast?"

**Reduced-motion mode (T-058):**

1. "Did you try reduced-motion mode? If yes, was the game still playable / readable, or did something important become invisible?" *(Tests T-098 hit-flash and T-116 screen-shake both respect the toggle; the death animation (T-097) should also respect it.)*
2. "Was there any animation in the game that you wished you could turn off — even without using reduced-motion mode?"

**Key rebinding (T-035 / Settings):**

1. "Did you find the rebinding screen?"
2. "Were all the keys you wanted to rebind actually rebindable?" *(We expect: yes for the 5 gameplay keys; no for menu/pause — if a tester wanted to rebind ESC, that's a ticket candidate.)*
3. "If you rebound any keys, did the new bindings persist when you came back to the menu and re-entered a level?"
4. "Was the conflict detection clear? (E.g. if you tried to bind two actions to the same key.)"

**Assist Mode (general):**

1. "Did you try Assist Mode? What made you turn it on (or not)?"
2. "If you used it: did it feel like a degraded version of the game, or just a different way to play it?" *(The design intent is the latter; the former is a ticketable framing problem.)*

### 3.6 Difficulty curve (target: 120 seconds)

1. "Map the session onto a difficulty curve in your head. Where did it feel easiest? Where did it feel hardest?"
2. "Was there a single death (or close call) that felt unfair — like the game didn't tell you what was coming?" *(Capture the level + the exact mechanic. This is high-signal for level-design tickets.)*
3. "Was there a single moment when you felt like you 'got it' — a movement or swap clicking into place?" *(Capture for marketing — these are testimonial quotes.)*
4. "Did Level1 (First Rain) feel like a step up from the tutorials, or a step sideways?"
5. "If you reached the boss: did you feel prepared for it by the levels leading up?"
6. "On a 1-to-5 scale, where 1 is 'too easy, I was bored' and 5 is 'too hard, I quit', where did the session land?" *(One discrete number per session — used in cohort comparison.)*

### 3.7 Eco / restoration framing (target: 60 seconds — fastest block, mostly for the eco-curious subgroup)

1. "Did you notice you were 'cleaning' the level rather than fighting your way through it?"
2. "Did the eco angle make the levels feel more meaningful, less meaningful, or about the same as a normal platformer would?"
3. "Did you pick up any Cloud Atlas entries? Did you read them?" *(If yes:)* "Did they feel like a lecture, a fun extra, or somewhere in between?"
4. "Does the framing 'restore corrupted ecosystems' from the itch.io short-description match what you actually did in the levels?" *(Tests the marketing-vs-product match.)*

### 3.8 Wrap (target: 60 seconds)

1. "Did the game ever crash on you, freeze, or do something visibly broken?" *(Cross-check against `<userHome>/.cloudy-ninja/crashes/` even if they say no — T-115 captures crashes silently if smoke-mode is not on.)*
2. "If you had a one-line tagline for this game, what would it be?" *(Capture verbatim — even the bad ones; bad taglines surface real misreadings of the pitch.)*
3. "Would you tell a friend to play this? If yes — what kind of friend? If no — what would have to change?"
4. "Is there anything you wanted to mention that none of the questions touched?"

---

## 4. Data capture

Every session produces **five artifacts**. All five must be saved before the synthesis pass (§5) begins.

### 4.1 Crash logs (from T-115)

Path: `<userHome>/.cloudy-ninja/crashes/crash-{timestamp}.log`

- Pre-session: note existing files (or, ideally, archive them out of the dir so the session starts clean).
- Post-session: copy every `crash-*.log` newer than session start into the session folder.
- **Read every crash log.** T-115 dumps the stack + thread state; if the file exists, the game crashed on this tester, even if they didn't notice. Ticket every distinct stack-trace as a separate bug, prefixed with `[playtest crash]`.
- File size sanity: T-115's verification artifact was 512 bytes (see HANDOFF.md). Files significantly larger than that — the real ones — typically carry full stack + a few seconds of preceding log lines.

### 4.2 Screenshots (from T-139)

Path: `<userHome>/.cloudy-ninja/screenshots/screenshot-{timestamp}.png`

- The screenshot hotkey is part of T-139; confirm the tester knows it before the play segment starts (they'll only use it if asked). The facilitator can also drive screenshots — but tester-initiated screenshots are higher signal because they captured "this moment was interesting to me."
- Post-session: copy every screenshot newer than session start.
- For each screenshot, the facilitator writes a 1-line caption in the session notes — what the tester said when they took it. A screenshot without a caption is half-useless three weeks later.

### 4.3 Screen recording

The full session (both segments) recorded via OBS, Discord's screen-record, or the meeting platform's built-in record. Resolution: whatever the tester's screen ships at — do not ask them to change it.

- Saved as `recording-{date}-{tester-pseudonym}.mp4` in the session folder.
- Mark interesting timestamps in the session notes (e.g. `+07:23 — first attempted mid-jump swap, failed`) so we don't have to re-watch the whole hour later.
- **Privacy note:** if the tester's desktop is visible at any point (alt-tab, notifications), trim it out before the recording leaves the session folder. The consent script promises internal-only use; treat tester-side info accordingly.

### 4.4 Session notes (template)

Use this template verbatim. One file per session, markdown.

```markdown
# Playtest session — {date} — {tester pseudonym}

**Build under test:** v{x.y.z} · {build date} (from main menu label, T-100)
**Subgroup(s):** [indie-platformer | eco-curious | accessibility | combo]
**Facilitator:** {name}
**Duration:** {actual minutes}
**Consent:** [given verbatim | given paraphrased | declined]
**Artifacts:** [crash logs: N | screenshots: N | recording: yes/no]

## Setup
- OS: {Windows / macOS / Linux + version}
- Display: {resolution, scaling — relevant for 4K/HiDPI bugs}
- Input: {keyboard layout — QWERTY / AZERTY / Dvorak / other}
- Accessibility needs disclosed: {free text — only what tester volunteered}

## Timeline (session-relative timestamps)
- +00:00 — launched game
- +HH:MM — {event} — {tester quote, verbatim if possible}
- (repeat for every defect, friction moment, or notable quote)

## Levels reached
- Level0_0: {time entered → time left, or "did not reach"}
- Level0_1: ...
- (continue for all levels touched)

## Interview answers (§3 question bank)
### 3.1 First impressions
1. {verbatim or near-verbatim answer}
...

## Crash logs collected
- crash-{ts}.log — {1-line description of the crash from the stack trace}

## Screenshots collected
- screenshot-{ts}.png — {tester's caption or facilitator's recall of context}

## Facilitator gut-check (NOT shown to tester)
- 1-sentence read of how the session went overall.
- Top 3 defects we'd ticket if we could only ticket 3.
- Anything that surprised the facilitator.
```

### 4.5 Build-version + environment metadata

Captured in the "Setup" block above. Critical for filing defects against the right build — if a defect repros only on macOS HiDPI 4K, that constraint goes in the ticket and the playtest notes are the source.

---

## 5. Synthesis — themes to ticket candidates

The synthesis pass runs **after each wave of 3–4 sessions**, not after every individual session. Single-session synthesis over-weights one tester's idiosyncrasies. Wave-level synthesis (3–4 sessions in aggregate) is where repeated failure modes surface.

### 5.1 Read pass (per wave, ~30 minutes)

1. Re-read all session-notes files in the wave back-to-back. Do not yet open ticket templates.
2. For each defect / friction event noted in §4.4 timelines, tag it with **one of four labels**:
   - `BUG` — game-side defect (crash, broken physics, missing asset, save corruption)
   - `UX` — works as coded but reads wrong (e.g. tester thought ability X did Y, expected swap cycle was different)
   - `A11Y` — accessibility defect (color-blind miss, reduced-motion gap, rebind not persisting, keyboard-only path broken)
   - `DESIGN` — works as designed but the design itself is wrong (e.g. difficulty cliff at Level1, Cloud Atlas reads as a lecture)
3. For each labeled event, tally cross-session frequency. **An event mentioned by 2+ testers gets ticket priority; 1-tester events get logged but not ticketed without facilitator override.**

### 5.2 Theme extraction

Cluster the tagged events into **themes**. A theme is a short sentence describing a single coherent failure mode. Good theme examples:

- "Players don't realize mid-air character-swap is allowed until ~Level0_3 or later."
- "Reduced-motion mode silently disables hit-flash, which removes the only damage feedback for some enemies."
- "Color-blind palette 3 makes corrupted-vs-clean tiles ambiguous in ECO biome."

Bad theme examples (too vague to ticket):

- "Tutorial could be better." *(What part? Reword.)*
- "Some testers found it hard." *(Which level, which mechanic? Reword.)*

Three to seven themes per wave is normal. More than ten means the clustering is too fine-grained — merge.

### 5.3 Ticket conversion

For each theme, draft a ticket candidate using TASKS.md's existing format. **The ticket goes in the queue, not directly into Sprint D** — the user prioritizes against current Sprint capacity.

Ticket template:

```markdown
### T-XXX — {short title}

**Source:** Playtest wave {N}, sessions {pseudonyms}, {date range}
**Type:** [BUG | UX | A11Y | DESIGN]
**Severity:** [alpha-blocking | v1.0-blocking | v1.1-or-later]
**Frequency:** {N of M testers hit this}
**Acceptance criteria:** {concrete behavior change that closes the ticket}
**Repro:** {steps from the session timeline}
**Refs:** session-{date}-{tester}.md timeline at +HH:MM
```

**Severity heuristic:**

- `alpha-blocking` — anyone hit a hard wall they could not get past, OR any crash repro'd 2+ times, OR an accessibility feature was advertised but didn't work.
- `v1.0-blocking` — 3+ testers in a 4-session wave hit the same UX defect, OR difficulty curve has a documented cliff, OR the eco-framing is reading wrong against the marketing pitch.
- `v1.1-or-later` — single-tester observations, polish requests, framing nits, feature requests for things outside current scope.

### 5.4 Reporting back

After every wave, write a one-page summary (`playtesting/wave-{N}-summary.md`) covering:

1. Cohort composition (which subgroups, headcount).
2. Themes identified (full list).
3. Tickets filed (with T-numbers).
4. Open questions raised by the wave that need user input before next wave (e.g. "should we change the default swap key before wave 2? Wave 1 evenly split on Q vs E preference.").

This summary is the artifact that drives the user's decision on whether to ship a wave-2 build with fixes, or proceed to wave 2 on the same build.

---

## 6. What this protocol explicitly does NOT cover

- **Quantitative metrics (deaths per minute, time-to-completion, retention).** Out of scope for alpha; sample size won't support inference. Defer to the post-launch telemetry conversation (no ticket yet).
- **A/B testing.** Same reason — N=5 doesn't A/B anything.
- **Public beta surveys (e.g. itch.io comment scraping).** Different workflow; if/when alpha goes wider, draft a new protocol.
- **Performance / FPS testing.** Smoke CI already covers headless runs across the 8-level matrix. Add to playtest only if a tester reports stuttering — then capture their hardware in the Setup block and ticket separately.
- **Localization testing.** Game is English-only at alpha (i18n scaffold exists per T-059/T-091 but no second locale ships). Add a "language" question to §3 when the second locale lands.

---

*End of protocol. T-155. See `HANDOFF.md` for the crash-log + screenshot capture mechanisms (T-115, T-139), `marketing/itch-page-draft.md` for the marketing claims this protocol validates (T-124), and `research/keyboard-layout-conventions.md` for the default-keys assumptions referenced in §3.4 (T-073).*

# T-048 — Marketing research: itch.io listing style guide (Antigravity)

**Target tool:** Google Antigravity — model: **Gemini 3.1 Pro**
**Ticket tier:** S — research, no code
**Autonomous:** yes, auto-merge eligible

## Pre-flight

Nothing blocks this. High-value pre-launch decision-support work.

## Launch procedure

1. Open **https://antigravity.google.com** → New session → Gemini 3.1 Pro
2. Point at **https://github.com/SohailShahM/Cloudy-Ninja**
3. Paste the prompt below
4. Auto-merges on CI green

## Prompt body (paste into Antigravity)

```
Read START_HERE.md and work on T-048 from TASKS.md. Your identity is `antigravity`.

Strict obedience required:
- Stay within the hard limits in START_HERE.md §3 for the `antigravity` identity.
- ONLY modify files under `marketing/`. Do NOT touch any code, tests, gradle, docs outside marketing, or any other ticket's files.
- Do NOT scrape itch.io aggressively — limit to ~12 reference listings.
- Read LEARNINGS.md + GAME_PLAN.md before starting. GAME_PLAN.md has the pitch and resolved decisions you need to match Cloudy Ninja's positioning.

Task:
Research 8–12 highly-rated indie 2D pixel-art platformers on **itch.io** and produce a style guide for Cloudy Ninja's eventual itch.io listing.

Reference listings to analyse (mix of these + your own discoveries):
- Celeste (the original itch.io page if available; or Steam page for inspiration)
- Hollow Knight (large-scale reference)
- Animal Well (recent eco-flavored hit)
- VVVVVV, Super Meat Boy Forever, Pikuniku, Tunche (style references)
- Any "eco" or "climate" themed platformer you can find
- Top-rated itch.io pixel-art platformers from 2023–2025 listings

For each reference listing capture:
- title
- listing URL
- rating / sales tier (if visible)
- screenshot composition (4-up grid? hero shot first? animated gifs?)
- headline copy pattern (1-sentence pitch + tagline format)
- trailer length and structure (does it open with gameplay, character, or world?)
- price point
- key conversion elements (price anchoring? demo offering? dev-update cadence? "early access" framing?)
- visual identity (logo style, color palette, banner composition)

Synthesize into `marketing/itch-listing-style-guide.md` with these sections:
1. Reference table — one row per analysed listing with the captured fields
2. Recommended headline-copy patterns for Cloudy Ninja (3 candidate one-liners + a longer 2-sentence pitch)
3. Screenshot composition rules (count, order, what to show first, what NOT to show)
4. Trailer structure recommendation — length, opening hook, mid-section, closing CTA, music style
5. Three recommended differentiators specific to Cloudy Ninja's pitch:
   - Climate / eco angle (without being preachy)
   - Multi-character switching mechanic
   - Accessibility-first design (Assist Mode, key rebinding, HiDPI)
6. Recommended price point given Cloudy Ninja's scope (3 worlds, 7 levels, 1 boss) — compare against the reference listings

When done:
1. Move T-048 from `## Todo` to `## Done` in TASKS.md.
2. Open a PR titled "T-048: itch.io listing style guide" using branch `antigravity/T-048-marketing-research`.
3. Comment on the PR with your single top headline-copy recommendation for Cloudy Ninja.

The PR will auto-merge on CI green.

If ambiguity arises (especially around the price point — there's no single right answer), give your best recommendation in the doc and note alternatives. Don't park in QUESTIONS.md unless truly blocked.
```

## What to verify when the PR appears

- 8+ reference listings analysed (not all need to be itch.io; Steam references are fine)
- Concrete recommendations — not just "depends" / "varies"
- Headline copy candidates are punchy, specific, not generic ("an exciting platformer adventure" = reject)
- Price recommendation has reasoning attached (anchoring against comparable scopes)

This output feeds the eventual launch checklist. If it's good, we can iterate from there. If it's vague, we'll re-prompt with sharper constraints.

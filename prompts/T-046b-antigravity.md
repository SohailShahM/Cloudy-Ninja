# T-046b — Character sprite-sheet research (Antigravity)

**Target tool:** Google Antigravity (https://antigravity.google.com) — model: **Gemini 3.1 Pro**
**Ticket tier:** S — research, no code
**Autonomous:** yes-with-review (visual side-scroller-perspective verification required)

## Pre-flight

- Ensure PR #10 (T-046a tileset research) has merged to `main` so Antigravity can read the chosen base (Kenney `pixel-platformer`) and match style against it
- Open `art-research/tileset-candidates.md` on main and confirm it's there — that's the visual reference Antigravity will style-match against

## Launch procedure

1. Open **https://antigravity.google.com** → New session → model **Gemini 3.1 Pro**
2. Point at **https://github.com/SohailShahM/Cloudy-Ninja**
3. Paste the prompt below
4. Antigravity runs autonomously, opens a PR scoped to `art-research/`, auto-merges on CI green

## Prompt body (paste into Antigravity)

```
Read START_HERE.md and work on T-046b from TASKS.md. Your identity is `antigravity`.

Strict obedience required:
- Stay within the hard limits in START_HERE.md §3 for the `antigravity` identity.
- ONLY modify files under `art-research/`. Do NOT touch any code, tests, gradle, docs outside art-research, or any other ticket's files.
- Do NOT download any binary asset files. Research-only — produce markdown notes.
- Do NOT add new dependencies.
- Read LEARNINGS.md before starting — ESPECIALLY the 2026-05-12 entry about perspective mismatch in T-046a. You MUST verify each candidate's camera perspective is side-scrolling, not top-down.

Task:
Find CC0 or CC-BY-licensed 32×32 character sprite sheets for THREE distinct characters:
- Ebo — earth / seed theme (brown / green palette)
- Laya — wind / storm theme (blue / grey palette)
- Zephyr — sky / air theme (purple / white palette)

Each character needs animation frames for: idle, run (4 frames), jump, fall, wall-slide.

Search Kenney.nl and OpenGameArt.org. The base art style is Kenney's `pixel-platformer` pack (cartoony pixel art, ~16×16 to 32×32 base, soft cute creatures). Style-match candidates to that aesthetic — see `art-research/tileset-candidates.md` for context.

For each candidate capture:
- name
- source URL
- license (CC0 / CC-BY / other)
- frame count per animation state (idle/run/jump/fall/wall-slide)
- palette match to character theme
- art style 1-5 match vs Kenney pixel-platformer
- camera perspective (MUST be side-scrolling; reject top-down)
- notes on completeness (does it have all 5 animation states?)

Output to `art-research/character-sprite-candidates.md` with one section per character (Ebo / Laya / Zephyr), each section containing a markdown table of ≥2 candidates. Aim for ≥6 total candidates.

When done:
1. Move T-046b from `## Todo` to `## Done` in TASKS.md.
2. Open a PR titled "T-046b: character sprite-sheet research" using branch `antigravity/T-046b-character-sprites`.
3. Comment on the PR with your top recommendation per character + why it style-matches Kenney pixel-platformer.

The PR will auto-merge on CI green (CI is trivial since no code changed).

If you hit ambiguity, append to QUESTIONS.md and release the claim instead of guessing.
```

## What to verify when the PR appears

Per LEARNINGS.md (T-046a perspective lesson): **examine each candidate's preview image** to confirm side-scrolling. Reject anything that's top-down RPG perspective even if Antigravity rated it high.

Expected output:
- ≥6 candidates (2+ per character)
- All side-scrolling perspective
- All with full animation frame counts
- License + URL verified

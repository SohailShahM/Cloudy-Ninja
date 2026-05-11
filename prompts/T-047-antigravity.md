# T-047 — Audio asset research (Antigravity)

**Target tool:** Google Antigravity — model: **Gemini 3.1 Pro**
**Ticket tier:** S — research, no code
**Autonomous:** yes, auto-merge eligible (PR contains only research notes)

## Pre-flight

Nothing blocks this. Independent of the tileset/sprite work.

## Launch procedure

1. Open **https://antigravity.google.com** → New session → Gemini 3.1 Pro
2. Point at **https://github.com/SohailShahM/Cloudy-Ninja**
3. Paste the prompt below
4. Auto-merges on CI green

## Prompt body (paste into Antigravity)

```
Read START_HERE.md and work on T-047 from TASKS.md. Your identity is `antigravity`.

Strict obedience required:
- Stay within the hard limits in START_HERE.md §3 for the `antigravity` identity.
- ONLY modify files under `art-research/`. Do NOT touch any code, tests, gradle, docs outside art-research, or any other ticket's files.
- Do NOT download any binary audio files. Research-only — produce markdown notes.
- Do NOT add new dependencies.
- Read LEARNINGS.md before starting.

Task:
Find CC0 / CC-BY ambient music tracks + supplementary SFX for Cloudy Ninja, a 2D pixel-art platformer. Currently the game uses 3 procedurally-generated ambient music tracks (functional but not shipping quality). Find candidates from:
- OpenGameArt.org (audio)
- Free Music Archive (FMA)
- Freesound.org

Music — find ≥3 candidates per category (15+ total):
- `arid` theme — sparse, warm, dry, desert-like ambient
- `wind` theme — airy, building, slightly tense, sky/storm ambient
- `eco` theme — lush, organic, hopeful, forest/water ambient
- `menu` — calm but interesting, looping main-menu vibe
- `boss` — tense, escalating, percussive, climactic

SFX — find ≥10 supplementary candidates (current 8 SFX are procedural):
- footstep variants (grass, stone, water)
- UI clicks (button press, menu open, menu close)
- ambient loops (wind, water, distant thunder)
- collectible chimes (token, snapshot, achievement)

For each candidate capture:
- name
- source URL
- license (CC0 / CC-BY / other; CC-BY requires attribution in credits)
- length in seconds
- file format (WAV / OGG / MP3)
- theme/category fit (arid/wind/eco/menu/boss for music; footstep/ui/ambient/chime for SFX)
- mood tags (calm/tense/triumphant/curious/peaceful/etc.)
- looping suitability (does it loop cleanly? if not, length is ≥60s acceptable?)

Output to `art-research/audio-candidates.md` with sections:
1. Music — one subsection per theme, table of ≥3 candidates each
2. SFX — one subsection per category, table of ≥3 candidates each

When done:
1. Move T-047 from `## Todo` to `## Done` in TASKS.md.
2. Open a PR titled "T-047: audio asset research" using branch `antigravity/T-047-audio-research`.
3. Comment on the PR with your top recommendation per theme/category + a sentence on why.

The PR will auto-merge on CI green.

If ambiguity arises, append to QUESTIONS.md and release the claim.
```

## What to verify when the PR appears

- All URLs resolve (spot-check 3)
- Licenses captured for each
- Loopable music tracks are marked as such (if not loopable, length should be sufficient for ambient looping)
- No CC-BY-SA or CC-NC items (those have viral / non-commercial restrictions we don't want)

If any candidates are CC-BY (attribution required), the credits screen will need to list them — note this for future T-Credits ticket.

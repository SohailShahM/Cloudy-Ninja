# T-046a — Tileset research (Antigravity)

**Target tool:** Google Antigravity (https://antigravity.google.com)
**Ticket tier:** S — research, no code
**Autonomous:** yes, auto-merge eligible (PR contains only research notes)

## Pre-flight

**Wait until PR #1 has merged to `main`** before launching this. T-046a's ticket definition lives on the feature branch; once PR #1 merges, it'll be on `main` where Antigravity clones from. Check https://github.com/SohailShahM/Cloudy-Ninja shows recent activity on the home page before proceeding.

## Launch procedure

1. Open **https://antigravity.google.com**
2. Point it at **https://github.com/SohailShahM/Cloudy-Ninja**
3. New session
4. Paste the prompt body below
5. Antigravity runs autonomously, opens a PR scoped to `art-research/`, auto-merges on CI green

## Prompt body (paste into Antigravity)

```
Read START_HERE.md and work on T-046a from TASKS.md. Your identity is `antigravity`.

Strict obedience required:
- Stay within the hard limits in START_HERE.md §3 for the `antigravity` identity.
- ONLY modify files under `art-research/`. Do NOT touch any code, tests, gradle, docs outside art-research, or any other ticket's files.
- Do NOT download any binary asset files. Research-only — produce markdown notes.
- Do NOT add new dependencies.
- Read LEARNINGS.md before starting.

Task:
Research pixel-art tilesets for 3 themes: ARID (desert/parched), WIND (storm/sky),
ECO (forest/swamp). Base resolution 32×32. Sources: Kenney.nl and OpenGameArt.org.

For each promising candidate capture:
- name
- source URL
- license (CC0 / CC-BY / other)
- file count
- theme fit (which of arid/wind/eco)
- art quality (subjective 1–5)
- notes on whether it includes character sprites or just terrain

Output a single markdown file `art-research/tileset-candidates.md` with one table
per theme. Aim for at least 3 candidates per theme (9+ total).

When done:
1. Move T-046a from `## Todo` to `## Done` in TASKS.md with Outcome and Commit/PR filled in.
2. Open a PR titled "T-046a: tileset research" using branch `antigravity/T-046a-tileset-research`.
3. The PR must contain only files under `art-research/`.
4. Comment on the PR with a 5-line summary of your top 2–3 recommendations per theme.

The PR will auto-merge on CI green (CI is trivial since no code changed).

If you hit ambiguity, append to QUESTIONS.md and release the claim instead of guessing.
```

## What to check after

When the PR appears (~30–90 min if Antigravity is doing thorough research):
- ✅ Only `art-research/` files touched
- ✅ Real candidates (not hallucinated; URLs work)
- ✅ Licenses captured for each
- ✅ At least 3 candidates per theme

If any of those are missing, comment on the PR asking Antigravity to redo it: `@antigravity please re-run, your previous output didn't meet the constraints in the issue body`.

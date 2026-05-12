# T-049 — Climate-source compilation for NotebookLM (Antigravity)

**Target tool:** Google Antigravity — model: **Gemini 3.1 Pro**
**Ticket tier:** S — research, no code
**Autonomous:** yes, auto-merge eligible

## Why this exists

The T-045 NotebookLM prompt originally cited `climate.gov` URLs that have since been **archived** — the live host is now `noaa.gov`. Rather than patch one URL at a time, T-049 compiles a complete verified-live source bundle that can be uploaded to NotebookLM in one step. Also widens the net beyond what the original prompt suggested — Antigravity should look for sources we didn't think of.

## Launch procedure

1. Open **https://antigravity.google.com** → New session → **Gemini 3.1 Pro**
2. Point at **https://github.com/SohailShahM/Cloudy-Ninja**
3. Paste the prompt below
4. Auto-merges on CI green

## Prompt body (paste into Antigravity)

```
Read START_HERE.md and work on T-049 from TASKS.md. Your identity is `antigravity`.

Strict obedience required:
- Stay within the hard limits in START_HERE.md §3 for the `antigravity` identity.
- ONLY modify files under `research/climate-sources/`. Do NOT touch any code, tests, gradle, docs outside that folder, or any other ticket's files.
- Read LEARNINGS.md before starting. Per the 2026-05-12 entry, verify URLs are live and current — climate.gov is now archived; use noaa.gov instead.
- Do NOT add new dependencies.

Task:
Build a curated, verified-live set of climate-science sources ready to feed into NotebookLM for the T-045 Cloud Atlas content generation step. Cover all 12 topics:

1. water_cycle
2. silver_iodide
3. temperature_inversion
4. albedo_effect
5. transpiration
6. groundwater_recharge
7. carbon_sequestration
8. storm_system
9. biodiversity_index
10. soil_microbiome
11. ocean_acidification
12. cloud_seeding

For each topic, find ≥3 authoritative sources from:
- noaa.gov (the new home of what used to be climate.gov)
- nasa.gov / earthobservatory.nasa.gov
- ipcc.ch
- epa.gov
- usgs.gov
- university extension publications (.edu)
- peer-reviewed open-access papers (DOAJ, PMC, Nature open, etc.)

Cast a wider net than the topic list strictly requires. Find supplementary sources NotebookLM might pull into entries we haven't planned (e.g. weather-modification ethics, regional groundwater case studies, indigenous land-management resources, eco-restoration success stories). Breadth > strict topic match.

Verification (this is what makes T-049 useful):
- Every URL must return HTTP 200 when fetched.
- Reject any URL that redirects to "archive" or "moved" pages.
- For PDFs: download to `research/climate-sources/*.pdf`. Name files descriptively (e.g. `noaa_water_cycle_primer.pdf`, `ipcc_ar6_wg1_spm.pdf`).
- For URL-only resources NotebookLM can fetch live: list in `research/climate-sources/urls.txt`, one per line, with a short `# comment` indicating which topic(s) it covers.
- Skip CC-incompatible sources (no proprietary textbooks, no paywalled journal articles, no .pdf behind login). Open-access only.

Outputs:
1. `research/climate-sources/INDEX.md` — table with columns: source (filename or URL), topic coverage (one or more of the 12), source type (PDF / URL), authority tier (gov / edu / peer-review / nonprofit), publication date if known.
2. `research/climate-sources/*.pdf` — the downloaded PDFs. Keep total folder size under 100 MB; pick the most authoritative source per topic if there are duplicates.
3. `research/climate-sources/urls.txt` — verified-live URLs for NotebookLM to fetch directly.

When done:
1. Move T-049 from `## Todo` to `## Done` in TASKS.md.
2. Open a PR titled "T-049: climate sources for NotebookLM" using branch `antigravity/T-049-climate-sources`.
3. Comment on the PR with: total source count, topics best-covered, topics that needed the most digging.

The PR will auto-merge on CI green (CI is trivial since no code changed).

If ambiguity arises, append to QUESTIONS.md and release the claim instead of guessing.
```

## What to verify when the PR appears

- ≥3 sources per topic (36+ total)
- All URLs in `urls.txt` actually live (spot-check 5)
- Total folder size under 100 MB
- Authority mix — should be predominantly .gov / .edu / peer-review, not random blogs
- INDEX.md is sortable/scannable

## After T-049 merges

The user opens NotebookLM:
1. New notebook
2. Drag-drop everything in `research/climate-sources/` (PDFs auto-upload; for `urls.txt`, copy-paste each URL into "Add source" → "URL")
3. Run the T-045 NotebookLM prompt
4. Save output to `prompts/T-045-content-from-notebooklm.md`
5. Assign Issue → Copilot for T-045 wiring

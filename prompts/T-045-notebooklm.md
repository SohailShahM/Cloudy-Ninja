# T-045 — Cloud Atlas to 12 entries (NotebookLM → Copilot agent)

**Target tools (chained):** NotebookLM (content) → Copilot coding agent (wire)
**Ticket tier:** S — content generation
**Autonomous:** yes-with-review (human skim-reviews NotebookLM output before wiring)

## Step 1 — NotebookLM (you, ~20 min)

### Launch procedure

1. Open **https://notebooklm.google.com** → "New notebook"
2. Upload 8–15 source documents. Recommended free sources:
   - **IPCC AR6 WG1 SPM** — search "IPCC AR6 working group 1 summary for policymakers" → download PDF
   - **NOAA climate.gov primers** (paste URLs):
     - https://www.climate.gov/news-features/understanding-climate/water-cycle
     - https://www.climate.gov/news-features/featured-images/understanding-albedo
     - https://www.climate.gov/news-features/understanding-climate/ocean-acidification
   - **NASA Earth Observatory** — search and paste article URLs for: cloud seeding, transpiration, temperature inversion, carbon sequestration
   - **Wikipedia** (paste URLs):
     - https://en.wikipedia.org/wiki/Cloud_seeding
     - https://en.wikipedia.org/wiki/Silver_iodide
     - https://en.wikipedia.org/wiki/Temperature_inversion
     - https://en.wikipedia.org/wiki/Albedo
     - https://en.wikipedia.org/wiki/Transpiration
     - https://en.wikipedia.org/wiki/Groundwater_recharge
     - https://en.wikipedia.org/wiki/Biodiversity_index
     - https://en.wikipedia.org/wiki/Soil_microbiology
3. Wait for "X sources" indicator. Open the chat.

### Prompt body — paste verbatim into the NotebookLM chat

```
I'm writing 12 short educational entries for an in-game "Cloud Atlas" collectible in a 2D platformer about restoring corrupted ecosystems. The game is called "Cloudy Ninja". The audience skews young — think ages 10–16 — but the content must be scientifically accurate. Tone: curious, slightly poetic, never lecturing.

Every entry must be grounded in my uploaded sources. Do NOT hallucinate facts. If you cannot ground a topic in my sources, output it as "NEEDS-SOURCE" so I know to add more material.

Format each entry exactly like this (copy this template):

---
id: snake_case_id_here
title: Short Title (max 25 chars)
hook: One-sentence hook shown when the player picks up the snapshot. Max 80 chars. Plain language, no jargon, evocative.
body: 2–3 sentences. Max 250 chars total. Kid-friendly. Define any term that a 10-year-old wouldn't know in-line, without using the word "literally" or "basically".
source: cite the specific document(s) and page/section you grounded each fact in
---

Generate exactly 12 entries, in this order:

1. water_cycle           — title hint: "The Water Cycle"
2. silver_iodide         — title hint: "Silver Iodide"
3. temperature_inversion — title hint: "Temperature Inversion"
4. albedo_effect         — title hint: "Albedo Effect"
5. transpiration         — title hint: "Transpiration"
6. groundwater_recharge  — title hint: "Groundwater Recharge"
7. carbon_sequestration  — title hint: "Carbon Sink"
8. storm_system          — title hint: "Storm System"
9. biodiversity_index    — title hint: "Biodiversity Index"
10. soil_microbiome      — title hint: "Soil Microbiome"
11. ocean_acidification  — title hint: "Ocean Acidification"
12. cloud_seeding        — title hint: "Cloud Seeding"

After generating all 12, append a "Confidence audit" section listing:
- Topics with strong source coverage (>2 sources): list them
- Topics with weak source coverage (1 source): list them and suggest 1–2 specific additional sources I should upload
- Any topic marked NEEDS-SOURCE: explain what's missing

Do not summarize or rephrase the format. Stick to it exactly.
```

### After NotebookLM responds

1. Copy the full output.
2. Save to a new file `prompts/T-045-content-from-notebooklm.md` in the repo.
3. Skim-review for accuracy. Any `NEEDS-SOURCE` entries → either upload more material to the notebook and re-prompt, or remove from the list and pick a substitute topic.
4. Once satisfied, the content is ready for Copilot to wire into `CloudAtlasLibrary.kt`.

## Step 2 — Copilot agent (autonomous, ~10 min)

Once `prompts/T-045-content-from-notebooklm.md` is committed to the repo:

1. Open https://github.com/SohailShahM/Cloudy-Ninja/issues/new
2. **Title:** `T-045 — Cloud Atlas to 12 entries`
3. **Body:** paste the block below
4. Assign to `Copilot`
5. Submit. Copilot opens PR within ~10 min.

### Prompt body for the GitHub Issue

```markdown
## Task: T-045

Read `START_HERE.md` and work on this ticket. Your identity is `copilot-agent`. Stay within the hard limits in §3.

### Content
The 12 entries are pre-generated in `prompts/T-045-content-from-notebooklm.md` (in the repo). Use those exactly — do not modify the text. Each entry has fields: id, title, hook, body, source.

### Goal
1. Expand `core/src/main/kotlin/com/sohai/platformer/atlas/CloudAtlasLibrary.kt` to register all 12 entries (currently has 6).
2. Update level definitions in `levels/TmxLevelDefinition.kt` (the `LevelRegistry.ALL` list) so each campaign level (Level1, Level2, Level3) has 2–3 `SnapshotDef` entries placed in reachable spots. The `storm_system` entry is already placed in Level 3 boss arena — leave it alone. Distribute the remaining 11 across all 3 levels.

### Files
- `core/src/main/kotlin/com/sohai/platformer/atlas/CloudAtlasLibrary.kt`
- `core/src/main/kotlin/com/sohai/platformer/levels/TmxLevelDefinition.kt`

### Done when
- 12 entries in `CloudAtlasLibrary.ALL`
- Each level has at least 2 SnapshotDef placements
- All snapshot positions are reachable (don't place inside walls or beyond level bounds)
- Compiles clean, smoke test passes
- The CloudAtlasScreen displays all 12 cards correctly

### Read first
- `START_HERE.md`
- `AGENTS.md` (architecture)
- `LEARNINGS.md`
- `prompts/T-045-content-from-notebooklm.md` (the actual content to wire in)

Branch: `copilot/T-045-cloud-atlas-12`.
```

## Done when

- 12 entries shipped in `CloudAtlasLibrary`
- All reachable in-game (test by playing each campaign level)
- Atlas screen renders all 12 cards correctly
- PR merged

# T-045 — Cloud Atlas to 12 entries (NotebookLM, then Copilot agent)

**Target tools (chained):** NotebookLM (content) → Copilot coding agent (wire)
**Ticket tier:** S — content generation
**Autonomous:** yes-with-review (skim NotebookLM output for accuracy before wiring)

## Step 1 — NotebookLM (you, ~20 min)

### Launch procedure
1. Open **https://notebooklm.google.com** → "New notebook"
2. Upload 8–15 source documents. Recommended (all free):
   - IPCC AR6 Summary for Policymakers (PDF — search "IPCC AR6 WG1 SPM")
   - NASA Earth Observatory articles on weather/climate (paste URLs directly)
   - NOAA climate.gov primers on water cycle, albedo, ocean acidification
   - Wikipedia: cloud seeding, silver iodide, temperature inversion, albedo effect, transpiration, biodiversity index, soil microbiome
   - Any climate textbook PDFs you own
3. In the notebook chat, paste the prompt body below
4. Save the output to `prompts/T-045-content-from-notebooklm.md` (create this file when done)
5. Skim-review for accuracy (don't trust uncited claims)

### Prompt body (paste into NotebookLM chat)

```
I'm writing 12 short educational entries for an in-game "Cloud Atlas" collectible
in a 2D platformer about restoring ecosystems. Each entry must be grounded in my
uploaded sources. Format each entry exactly like this:

---
id: snake_case_id_here
title: Short Title (max 25 chars)
hook: One-sentence hook visible on pickup (max 80 chars, plain language, no jargon)
body: 2–3 sentences (max 250 chars). Accurate, kid-friendly, no jargon.
source: <cite the document(s) you used>
---

Cover these 12 topics, in this order:
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

If you can't ground a topic in my sources, mark it as `NEEDS-SOURCE` instead of
hallucinating content.
```

## Step 2 — Copilot agent (autonomous, ~10 min)

Once you've reviewed the NotebookLM output and saved it to `prompts/T-045-content-from-notebooklm.md`:

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

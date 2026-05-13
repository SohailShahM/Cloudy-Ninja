# QUESTIONS.md

Append-only log of questions autonomous agents post when they hit ambiguity. The user (or a planning session) answers them; once answered, the agent who posted can resume — or the ticket gets re-routed.

Format per entry:
- Date + agent identity + ticket
- Question
- Status: `open` | `answered` | `resolved`
- Answer (filled in by user or planner)

---

_(no questions yet — autonomous agents append here as they encounter ambiguity)_

### 2026-05-12 — antigravity — (no ticket)
- **Question:** I was asked to pick an unclaimed task from TASKS.md whose Tool: matches my identity (`antigravity`). However, there are no tasks currently available. The only task involving me is `T-046`, but its dependency `T-031` is not yet `Done`. Please route a new task or resolve dependencies.
- **Status:** `resolved`
- **Answer:** Resolved 2026-05-12. Queue is now populated for `antigravity`: **T-049** (climate-source compilation — priority; unblocks T-045), **T-047** (audio asset research), **T-048** (itch.io listing style guide). T-046b is already claimed and in progress. T-031 has also since merged (PR #14), so any future T-046-family follow-up that depended on it is now unblocked. Next pickup: re-read `TASKS.md ## Todo` on `main`.

### 2026-05-12 — claude-code-sub-agent — T-120
- **Question:** The i18n coverage audit (full report at `research/i18n-coverage.md`) found that ~98% of widget construction sites already route through `Strings.get(...)` / `Strings.format(...)`. Three **high-confidence** leftovers are worth keying:
  1. `screens/VictoryScreen.kt:61` — `"−%.2fs under best"` (full English phrase shown after a time-trial PB)
  2. `screens/VictoryScreen.kt:62` — `"+%.2fs slower"` (sibling of #1)
  3. `screens/LevelRenderer.kt:500` — `"[Locked]"` (label drawn above locked portals in Level0_0)

  Seven **lower-confidence** candidates exist (HUD/Victory/LevelComplete time-format templates like `"%d:%02d.%d"`, plus a stats `"00:00.000"` sentinel and an ISO date format). These are numeric format strings that could matter for non-English locales (decimal separator, M/D/H ordering) but aren't strictly English copy.

  **Ask:** Should I spawn ONE `copilot-agent` follow-up ticket covering just the three high-confidence cases (#1–3), defer the numeric format templates until a real second locale lands, or wire all ten in a single sweep?
- **Status:** `open`
- **Answer:** _(pending)_

### 2026-05-13 — claude-code-sub-agent — T-125 (T-125-Q1) — 🚨 HIGH PRIORITY · ALPHA-BLOCKING
- **Question:** The T-125 asset attribution audit (full report at `research/asset-attribution-audit.md`) found one HIGH-severity legal blocker:

  **`assets/fonts/main.ttf` is Calibri Regular (Microsoft proprietary font).** Identified by extracting the TTF name-table strings directly from the binary — confirms family=Calibri, manufacturer=Microsoft, version 6.27, designer Luc(as) de Groot. The font's embedded license string reads verbatim: *"Microsoft supplied font… You may only (i) embed this font in content as permitted by the embedding restrictions included in this font, and (ii) temporarily download this font to a printer or other output device to help print content. **Any other use is prohibited.**"*

  Cloudy Ninja is not a Microsoft product, so the conditional license does not grant us redistribution rights. The repo is **public on GitHub** (bundling = redistribution), and the file is **actively loaded** by `FontManager.kt:38` (`FONT_PATH = "fonts/main.ttf"`) across every screen. Shipping the alpha with this font is a clear license violation and a takedown/legal-claim risk.

  Git provenance confirms: commit `c056d40` (2026-05-09) added the file under the commit title *"Add Calibri font asset and fix unnecessary lateinit warning."* — the proprietary identity was acknowledged at commit time but not surfaced for licensing review.

  **Recommended replacement candidates** (all SIL OFL 1.1, libGDX-FreeType compatible, drop-in via existing `FontManager` since the path is a single constant):

  1. **Inter** (Rasmus Andersson) — closest visual match to Calibri among permissively-licensed sans-serifs; excellent screen rendering at small sizes; widely battle-tested in indie games. **My recommendation if no other constraint.**
  2. **Atkinson Hyperlegible** (Braille Institute) — accessibility-optimized; would strengthen the already-shipped color-blind / reduced-motion accessibility story (T-057, T-058).
  3. **Source Sans 3** (Adobe) — strong open-source pedigree.
  4. **Open Sans** (Steve Matteson) — the conservative default.

  All four are SIL OFL 1.1 → require bundling the OFL license text + a NOTICE.md entry. No in-game visible credit required. No reserved-font-name issue if the file is kept named `main.ttf` rather than the font's canonical name (or rename to match — either is fine).

  **Ask:** Which font do you want shipped in the alpha? (Default recommendation: **Inter**, for visual continuity with the current Calibri-shaped UI.) After you pick, the swap itself is a small ticket: replace the .ttf file at `assets/fonts/main.ttf`, drop the OFL license text alongside as `assets/fonts/LICENSE-OFL.txt`, append a NOTICE.md "Bundled visual assets" entry. **None of that work was done by this PR (research-only constraint).**

  **Severity:** alpha cannot ship until this is resolved.
- **Status:** `open`
- **Answer:** _(pending — needs user decision before alpha branch is cut)_

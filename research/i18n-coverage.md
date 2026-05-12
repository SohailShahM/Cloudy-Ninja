# i18n coverage audit (T-120)

**Date:** 2026-05-12
**Auditor:** claude-code-sub-agent (re-routed from antigravity)
**Branch:** `claude/T-120-i18n-coverage-audit`
**Scope:** All `*.kt` files under `core/src/main/kotlin/com/sohai/platformer/{screens,entities}` plus other UI-adjacent paths (`progression/`, `LevelRenderer.kt`).
**Verifies:** HANDOFF.md claim that "130+ i18n keys exist; nothing leaked."

## Methodology

Patterns scanned (via Grep):
- `Label("...")`, `TextButton("...")`, `VisLabel("...")`, `VisTextButton("...")`, `CheckBox("...")` — direct widget constructors with string literals.
- `.setText("...")` — runtime label/button text updates.
- `font.draw(batch, "...")` — direct font rendering.
- Format strings `"%d:..."`, `"%.2f..."` that produce user-visible text.
- Multi-word literals matching `"[A-Z][a-zA-Z]+ [a-z]+..."` (excluding log tags / message bodies passed to `Gdx.app.{log,error,debug}`).

Excluded per ticket constraints:
- `Gdx.app.log/error/debug` messages (developer-facing).
- KDoc / line comments.
- Character ID equality checks (e.g. `currentCharacter == "Zephyr"`).
- Asset paths and TMX object names.
- Single-character / hex / numeric literals.

## Headline result

**Coverage is excellent.** Of ~80 widget construction sites in `screens/` examined, **all but the items listed below route through `Strings.get(...)` or `Strings.format(...)`**. 130+ keys in `Strings.kt` (T-059 + T-091) are doing their job.

The leftover hits fall into one tight category: **numeric format templates for time and best-time-delta strings.** These are the only true user-facing literals not yet keyed.

## Candidates (10 hits across 5 files)

Grouped by category. Confidence reflects how clearly the literal is end-user-visible vs. a debatable display-format detail.

### Gameplay HUD — time / stopwatch formatting

| # | File | Line | Literal | Recommended `StringKey` | Confidence |
|---|------|------|---------|------------------------|------------|
| 1 | `core/src/main/kotlin/com/sohai/platformer/screens/Hud.kt` | 232 | `"⏱ %d:%02d.%d"` | `HUD_STOPWATCH_FORMAT` | High — emoji + format string visible every frame in time-trial mode. Static placeholder `HUD_INIT_STOPWATCH` already exists; the dynamic format is the missing companion. |
| 2 | `core/src/main/kotlin/com/sohai/platformer/screens/Hud.kt` | 274 | `"%d:%02d"` | `HUD_TIMER_FORMAT` | Medium — pure numeric, but it's the live timer the player reads. Existing `HUD_INIT_TIMER = "0:00"` documents the intent. |

### Level-complete / Victory — time + delta strings

| # | File | Line | Literal | Recommended `StringKey` | Confidence |
|---|------|------|---------|------------------------|------------|
| 3 | `core/src/main/kotlin/com/sohai/platformer/screens/LevelCompleteOverlay.kt` | 44 | `"%d:%05.2f"` | `LEVEL_COMPLETE_TIME_FORMAT` | Medium — displayed in the level-complete stats row. |
| 4 | `core/src/main/kotlin/com/sohai/platformer/screens/VictoryScreen.kt` | 53 | `"%d:%02d.%d"` | `VICTORY_TIME_FORMAT` | Medium — formats `bestTrialTime` shown to the player. |
| 5 | `core/src/main/kotlin/com/sohai/platformer/screens/VictoryScreen.kt` | 61 | `"−%.2fs under best"` | `VICTORY_DELTA_UNDER_BEST` | **High** — full English phrase, user-facing, contains translatable copy ("under best"). Pairs with #6 below. |
| 6 | `core/src/main/kotlin/com/sohai/platformer/screens/VictoryScreen.kt` | 62 | `"+%.2fs slower"` | `VICTORY_DELTA_SLOWER` | **High** — same as #5. The two strings are sibling templates and should be wired together. |

### Stats screen — best-time format

| # | File | Line | Literal | Recommended `StringKey` | Confidence |
|---|------|------|---------|------------------------|------------|
| 7 | `core/src/main/kotlin/com/sohai/platformer/screens/StatsScreen.kt` | 170 | `"00:00.000"` | `STATS_BEST_TIME_DEFAULT` | Low — null/NaN sentinel only. Could just be a constant. |
| 8 | `core/src/main/kotlin/com/sohai/platformer/screens/StatsScreen.kt` | 175–177 | Multi-line `padStart` MM:SS.mmm template | `STATS_BEST_TIME_FORMAT` (numeric) | Low — composed via `padStart`, not a single literal. Argues for a `format()` style template if/when wired. |

### World map — locked portal label

| # | File | Line | Literal | Recommended `StringKey` | Confidence |
|---|------|------|---------|------------------------|------------|
| 9 | `core/src/main/kotlin/com/sohai/platformer/screens/LevelRenderer.kt` | 500 | `"[Locked]"` | `WORLD_PORTAL_LOCKED` | **High** — full English word, rendered directly above each locked portal in Level0_0. The unlock-required keys (`LEVEL_SELECT_BTN_LOCKED = "Locked"`, `COMPLETE_WORLD_FIRST = "Complete World {0} first"`) confirm this pattern is already partly keyed elsewhere. |

### Main menu — ISO date format

| # | File | Line | Literal | Recommended `StringKey` | Confidence |
|---|------|------|---------|------------------------|------------|
| 10 | `core/src/main/kotlin/com/sohai/platformer/screens/MainMenuScreen.kt` | 323 | `"%04d-%02d-%02d"` | (skip — keep as-is) | Very low — ISO 8601 is locale-neutral by design. Listed for completeness. |

## What's **not** a hit (and why)

For the next auditor's reference — these patterns came up in the scan and were verified clean:

| Site | Verdict |
|------|---------|
| `LevelCompleteOverlay.kt:58 Label("$score", …)` | Kotlin string template (`"$score"` == `score.toString()`). No translatable text. |
| `AchievementToast.kt:142 titleLabel.setText(next.achievement.title)` | Achievement title/desc are data fields; localizing those is an `Achievement` schema change, out of scope. |
| `AchievementsScreen.kt:128 VisTextButton(label)` | `label` is composed from `SLOT_LABEL` plus `[` / `]` decorations. The brackets are visual chrome, not translatable copy. |
| `SettingsScreen.kt:220 btn.setText(Input.Keys.toString(keycode))` | Keycode name comes from libGDX; not our copy to translate. |
| `MainMenuScreen.kt:152 VisLabel(levelName)` | `levelName` comes from `LevelManager.getLevel(...).name` — dynamic, data-driven. |
| All `Gdx.app.{log,error,debug}` strings | Dev-facing per ticket exclusion. |

## Verdict

**Coverage is ~98% by call-site count.** The two true gaps worth wiring are:

1. **`VictoryScreen.kt` lines 61–62** — `"−%.2fs under best"` / `"+%.2fs slower"`. Real English phrases that any localizer would flag immediately.
2. **`LevelRenderer.kt` line 500** — `"[Locked]"`. Single word, but rendered directly to the world canvas.

The remaining 7 candidates are numeric format templates (time, stopwatch, date). They're lower-priority but worth keying if/when full locale support is added, since some locales use `,` instead of `.` as the decimal separator and reorder the M/D/H components.

A single follow-up Copilot ticket covering items 1–2 plus #9 (the three high-confidence hits) would close the verifiable leak surface.

## Recommended next step

File a Copilot-agent ticket to wire the **three high-confidence** items (#5, #6, #9 above) — single-file scope each, mechanical edit. Defer #1–4, #7–8, #10 until a real second locale lands and the format-string question can be answered against a concrete translator workflow.

See QUESTIONS.md for the summary entry asking the user to greenlight the follow-up.

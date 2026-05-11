# T-035 — Audio bus sliders (Copilot coding agent)

**Target tool:** GitHub Copilot coding agent (formerly Copilot Workspace)
**Ticket tier:** S — single-screen, mechanical
**Autonomous:** yes, auto-merge eligible

## Launch procedure

1. Open **https://github.com/SohailShahM/Cloudy-Ninja/issues/new**
2. **Title:** `T-035 — Audio bus sliders: music / sfx / ui`
3. **Body:** paste the block below
4. **Assignees** (right sidebar): select **Copilot**
5. Submit
6. Copilot opens a draft PR within ~5–15 minutes. When it marks the PR "Ready for review," CI runs, auto-merge fires on green.

## Prompt body (paste this into the GitHub Issue)

```markdown
## Task: T-035

Read `START_HERE.md` and work on this ticket. Your identity is `copilot-agent`. Stay within the hard limits in §3 of START_HERE.md (Tier S only; single-file or single-screen scope; abort and post to QUESTIONS.md if scope grows beyond that).

### Goal
Add per-bus volume sliders to `SettingsScreen`. Currently there's one SFX slider. Add `volMusic: Float = 0.7f` and `volUi: Float = 0.9f` to `persist/Settings.kt` (keep existing `volSfx`). Replace the single slider in `SettingsScreen` with three VisUI sliders labelled `Music`, `SFX`, `UI`.

On slider change:
- Music slider → `MusicManager.setMusicVolume(v)` (verify the method exists; create if missing using existing `MusicManager` patterns)
- SFX slider → `SoundManager.setVolume(v)` (already exists)
- UI slider → store in Settings only (no playback yet; future UI sounds will read from it)

Persist immediately via `SettingsManager.save()` on every change. In `GameScreen.init`, apply all three volumes once at startup.

### Files
- `core/src/main/kotlin/com/sohai/platformer/persist/Settings.kt` (add fields)
- `core/src/main/kotlin/com/sohai/platformer/screens/SettingsScreen.kt` (replace 1 slider with 3)
- `core/src/main/kotlin/com/sohai/platformer/audio/MusicManager.kt` (verify/add setMusicVolume)
- `core/src/main/kotlin/com/sohai/platformer/screens/GameScreen.kt` (apply at init)

### Done when
- Three sliders visible in Settings, all three volumes respond in real-time
- Settings persist across sessions
- Compiles clean (no new warnings)
- AI smoke test (`.github/workflows/ai-smoke.yml`) passes on the PR

### Read these before claiming
- `START_HERE.md` (the entry point — identity, capability gates, claim protocol)
- `AGENTS.md` (architecture — `audio/` and `persist/` package layout)
- `LEARNINGS.md` — especially T-044's lesson: body text Labels must use `FontManager.getShared(N)`, NOT VisUI baked skin fonts

### Move ticket to In Progress in TASKS.md when claimed
Per START_HERE.md §4. Branch: `copilot/T-035-audio-sliders`.
```

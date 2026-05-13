# Demo pre-flight checklist

> One-time validation pass before showing the game to anyone. ~15 minutes total. The smoke CI proves "doesn't crash" — this proves "looks and feels right." If anything below fails, log it to QUESTIONS.md (or file a ticket) and decide before the demo whether to fix or work around.

## Build + launch (~2 min)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :lwjgl3:run
```

If the build fails: `.\gradlew.bat :core:test` first to see whether it's a code regression or an environment issue. Recent merges (PRs #137-#149) all CI-green, so a clean local fail is environmental.

## Part 1 — Visual font pass (T-168, ~5 min)

This is the most important pass — PR #137 swapped Calibri → Inter and **smoke CI cannot see fonts.** Take a screenshot at each surface so we have a baseline.

- [ ] **MainMenu** — title legible, "Play / Settings / Achievements / Stats / Credits / Atlas" all read cleanly. Build label bottom-right (`v0.1.0 · 2026-05-12` or similar) readable.
- [ ] **Settings** — section headings (Audio, Display, Controls, Accessibility) crisp. Slider labels not clipping. Three Sound Test buttons readable. New "Reset to defaults" button at bottom visible.
- [ ] **Achievements** — `Achievements: N/13` counter visible. Per-row icons + titles + descriptions readable. Locked rows distinct from unlocked.
- [ ] **Credits / Stats / CloudAtlas** — text doesn't clip card edges; no missing-glyph squares.
- [ ] **Pause overlay** (Esc during gameplay) — Resume/Quit buttons + 3-character ability card all readable.
- [ ] **HUD in-game** — best-time + level-timer top-right, character + spirit-pips top-left, no clipping at any zoom.

**Pass criteria:** at every surface, text is legible AND looks roughly equivalent to (or better than) before the swap. Inter is metrics-compatible with Calibri so dramatic regressions are unlikely; subtle ones at small sizes are the realistic risk.

If any surface looks off: file as a sub-ticket of T-168 with screenshot + size + surface. Don't try to fix during the demo.

## Part 2 — 10-minute golden-path play-through

A demo viewer's first impression. Goal: confirm nothing visibly broken and the loop feels intact.

### Save & onboarding (~2 min)
- [ ] MainMenu → Play → pick fresh slot 1 → starts in Level 0-0 hub
- [ ] Hub portal entry flows correctly (greyed = locked, lit = unlocked)
- [ ] Enter Level 1 portal → splash → game starts

### Gameplay loop (~5 min)
- [ ] Move + jump feels responsive
- [ ] Character swap works (Q — was S; verify default is now Q after T-121)
- [ ] Action key (jump-slam / dash / float depending on character) triggers ability + animation
- [ ] Smog Sprite or Drift Husk: stomp it from above (defeated + bounce) — hit-flash visible
- [ ] Take damage from lateral enemy contact → death animation → respawn at checkpoint
- [ ] Pick up a Cloud Atlas snapshot → overlay opens → reads cleanly → close → game resumes
- [ ] Reach Level 1 exit → victory screen → best-time recorded → **screenshot auto-saves** to `~/.cloudy-ninja/screenshots/victory-level1-*.png` (verify file exists after)

### Audio & accessibility (~2 min)
- [ ] Settings → Audio: drag Master slider → all sounds scale; drag Music slider → only music; drag SFX → only SFX
- [ ] Click "Play SFX (jump)" button → hear jump sound
- [ ] Click "Play Music (ambient_arid 3s)" → music starts → stops automatically after ~3s (don't leave Settings before it stops — verify cancel-on-hide works)
- [ ] Master mute checkbox → silence; uncheck → restore
- [ ] **Press M anywhere** (in-game) → mute toggle; M again → unmute
- [ ] Settings → Display → toggle Speedrun Timer ON → return to game → millisecond timer top-left
- [ ] Settings → Accessibility → High Contrast ON → enemies become black silhouettes, player white silhouette. **Hit an enemy** → silhouette briefly flashes white (this was broken until T-170 today)
- [ ] Settings → Reset to Defaults → confirmation modal opens → Cancel button default-focus → ESC dismisses → confirm Reset → all sliders return to defaults

### System-level (~1 min)
- [ ] **Press F12 anytime** → screenshot saved to `~/.cloudy-ninja/screenshots/manual-{ScreenName}-*.png` → verify file exists
- [ ] Alt-Tab out of game → **auto-pause** triggers (T-112) → alt-tab back → game still paused → Esc to resume
- [ ] Esc from MainMenu → quit cleanly (no crash dump in `~/.cloudy-ninja/crashes/`)

## Pass criteria

- **Demo to friends / streamers / casual press:** all of Part 1 + the gameplay loop and audio/accessibility sections of Part 2 must pass. F12 and auto-pause are bonus polish — non-blocking.
- **Demo to publisher / investor:** all of the above PLUS a visible answer for T-046 (art direction). The procedural ShapeRenderer look is the elephant in the room for a polished pitch.

## If something fails

1. Capture: screenshot + reproduction steps + exact build (`git log -1 --oneline`).
2. Decide: blocker for the demo, or known-issue-show-anyway?
3. If blocker: file a P0 ticket with the same template the existing TASKS.md entries use, dispatch via Sonnet sub-agent next session.
4. If show-anyway: add to a short "known issues" note you can hand the viewer.

## After the demo

- Update LEARNINGS.md with anything the audience flagged that the team missed.
- If T-168 passed cleanly: append a one-liner "Inter visually verified across N surfaces YYYY-MM-DD" to LEARNINGS.md and close T-168.

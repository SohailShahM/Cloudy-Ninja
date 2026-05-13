# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-13 (very late) by Claude Opus — Sprint D wave-10 dispatch-heavy session, picking up directly from the previous handoff. **14 PRs merged + 5 specs + 1 LEARNINGS entry + 4 direct-to-main housekeeping pushes.** Roughly:

- **13 code/feature tickets shipped end-to-end** with tests: T-126 (Calibri→Inter), T-127 (dead deps), T-139 (screenshot-on-victory), T-105 (master volume + mute), T-147 (F12 hotkey), T-118 (M-key mute), T-121 (swap S→Q migration), T-145 (sound test), T-142 (speedrun timer), T-169 (consolidate dual shake), T-143 (reset to defaults), T-170 (silhouette → entities), T-171 (GlobalInputRouter Phase A).
- **5 specs added**: T-168 (visual font verify; human), T-169/170 (implemented same session), T-171 (Phase A done), T-172 (router Phase B).
- **Alpha-blocking T-126 Calibri CLOSED.** Inter (SIL OFL 1.1) shipped via PR #137; no Microsoft-proprietary fonts remain in the repo. **T-168 visual-verify is the pre-alpha gate** — human eye across ~17 FontManager surfaces, not autonomous-eligible.
- **Two architectural smells from prior HANDOFF closed**: dual screen-shake systems (T-169), silhouette overlay hack (T-170). Hit-flash now also visible in high-contrast mode as a positive side-effect of T-170.
- **Queue-audit discovery**: T-035 was already shipped (PR #9, 2026-05-12) but never moved out of `## Todo`. Caught by sub-agent's first read, fixed in commit `5dc9594`. Worth doing a queue-vs-main audit at the start of each session.

**Previous session (2026-05-13 early) summary, preserved:** 63 PRs merged across ~38 feature tickets + ~13 research/marketing + ~12 doc-only PRs. All 4 then-prior source-side quirks closed. T-126 Calibri surfaced as alpha-blocker. Two Copilot dogfood PRs (#68 + #85) blocked on the GitHub Actions first-time-contributor approval policy — both have since shipped (#134 + #135), policy gate no longer blocking new Copilot work but no new Copilot work was dispatched today.

### Repo state: public + proprietary-licensed (unchanged)

`SohailShahM/Cloudy-Ninja` is **public** (CI free + unlimited) but **proprietary** per `LICENSE`. Don't flip private without `docs/SELF_HOSTED_RUNNER.md` set up first — the Education Pack Actions quota was burned in the 2026-05-12 session.

### Branch protection — admin-merge is the default (unchanged)

`main` has `required_conversation_resolution: true` — never clears for AI-opened PRs. Default workflow after CI green:

```
gh pr merge <N> --repo SohailShahM/Cloudy-Ninja --admin --squash --delete-branch
```

Direct `git push origin HEAD:main` works for docs-only / TASKS.md / LEARNINGS changes via admin bypass. Used 4× this session — keep it for housekeeping only, never code.

### cc-agv-bridge (unchanged from prior HANDOFF)

Still wired but **still not dogfooded on a real Cloudy-Ninja ticket.** AGV was offline this session — re-tag-to-Sonnet via the `Agent` tool kept the pipeline moving. The bridge tools loaded fine in this CC session; capacity, not capability, was the limiter. First dogfood opportunity = next ticket that naturally splits between CC and AGV.

### Sub-agent dispatch patterns — affirmed by 14/14 success rate this session

1. **One sub-agent per ticket**, `general-purpose`, `isolation: "worktree"`, `run_in_background: true`. Worktree auto-cleans.
2. **Brief inline + verbatim spec + hard rules.** Each prompt 100-200 lines. Zero TASKS.md-claim race because parent edits TASKS.md.
3. **File-conflict matrix before each dispatch.** Most-contested files this session: `Settings.kt`, `SettingsScreen.kt`, `Strings.kt`, `LevelRenderer.kt`, `Main.kt`, `InputManager.kt`. T-105 unblocked a Settings-family chain that had to serialize (T-118 → T-121 → T-145 → T-142 → T-143).
4. **Parallel batches of 2-4** when file scopes are disjoint. Largest concurrent in flight this session: 4 (T-126/T-127/T-139/T-105 wave 1). Lower than the prior session's 20-agent push because the queue was smaller and contention tighter.
5. **Explicit "do NOT touch X.kt — other agent's territory" hard rules in each prompt** prevent rebase storms. Used heavily once Settings-family work began.
6. **Sub-agent surprises documented in PR body** — collected 6 useful source-side facts this session (see "Source-side quirks discovered" below).

---

## Live state of the project

**Main HEAD at handoff:** `0043983` (T-171 Phase A). The session ran cleanly through to a dry queue.

**New since prior HANDOFF (additions THIS session in **bold**):**
- **Inter font (SIL OFL 1.1) replaces Calibri** — alpha legal blocker cleared
- **Master volume + mute (Settings + checkbox + M-key) (T-105 + T-118)** — composes with T-117 audio ducking; mute preserves slider position
- **Screenshot system**: victory screen auto-capture (T-139) + F12 anytime hotkey (T-147) — writes to `~/.cloudy-ninja/screenshots/`
- **Sound test buttons in Settings Audio section (T-145)** — UI Click / SFX (jump) / Music (ambient_arid 3s with Timer-scheduled stop)
- **Speedrun timer HUD overlay (T-142)** — `MM:SS.mmm` top-left, off by default, carry-glitch-safe via `floor(seconds*1000)`
- **Reset-to-defaults button in Settings (T-143)** — confirmation modal mirroring T-119; `SettingsManager.reset()` bypasses `update()` so `keybindsCustomized` stays false
- **Default swap keybind: Q (was S) (T-121)** — migration auto-upgrades legacy users who never opened Controls
- **Unified screen-shake system (T-169)** — `LevelRunState.triggerShake` retired; ScreenShake singleton handles all shake; replace-stacking; +`Settings.screenShake` gate
- **High-contrast silhouettes owned by entities (T-170)** — `Enemy.drawHighContrast(renderer, color)` + `Player`/`StormSentinel` overrides; LevelRenderer no longer hardcodes per-entity bounds; hit-flash now visible in high-contrast (silent T-132 regression fixed)
- **GlobalInputRouter Phase A (T-171)** — `input/GlobalInputRouter.kt` singleton + Kotest spec; MainMenuScreen migrated to push/pop; T-147 F12 + T-118 M-key polling gated with `!GlobalInputRouter.isActive()` to prevent double-fire. Other ~14 screens still use legacy `Gdx.input.inputProcessor = stage`; **T-172 Phase B** is the bulk migration follow-up.

**~700+ Kotest tests total** (this session added ~50: SettingsManagerTest grew significantly across T-118/T-121/T-142/T-143; new test files for SpeedrunTimerFormat, HighContrastSilhouette, GlobalInputRouter, ScreenshotWriter, MuteHotkey).

---

## In-flight threads / pipeline state — end of 2026-05-13 (very late) session

### Queue is DRY for autonomous dispatch
After this session every Tier-S/M ticket the previous HANDOFF queued has shipped. The only remaining Todo items are:

- **T-168** — Visual font-regression smoke pass. **Human-only.** Pre-alpha gate. Manually launch the build, eyeball Inter across ~17 FontManager surfaces, file regression tickets if anything looks wrong. Result captured in LEARNINGS.md + screenshots in `research/font-screenshots/`.
- **T-172** — `GlobalInputRouter` Phase B (bulk screen migration + delete polling fallbacks). Sonnet-dispatchable. Touches ~14 Screen files + Main + InputManager. **Recommended:** let Phase A bake for one session, then dispatch. Will surface overlay/focus quirks if any exist.
- **Pre-existing not-autonomous tickets unchanged**: T-038 (ghost replay, determinism), T-061 (CI matrix), T-045 (NotebookLM), T-102 (gamepad hardware), T-076 (deps, held), T-081 (Android CI), T-046 (graphics overhaul, art direction).

### 🚨 ALPHA-BLOCKING — none. T-126 Calibri closed this session.

T-168 is a **pre-alpha gate**, not a blocker — needs a manual visual pass to confirm Inter renders cleanly across all surfaces. If you want to play it safe, do it before public alpha drops.

### Source-side quirks pinned this session (and prior status updates)

1. ✅ **Closed (prior session, this HANDOFF cleanup):** SoundManager log→error, FontManager seam, ScreenFade rename, Achievement predicates refactor.
2. ✅ **CLOSED this session:** Two screen-shake systems coexisting (T-169 consolidated onto ScreenShake).
3. ✅ **CLOSED this session:** LevelRenderer silhouette overlay hack (T-170 moved geometry into entities; hit-flash regression silently fixed).
4. **PARTIALLY CLOSED:** Per-screen `Gdx.input.inputProcessor = stage` clobbering. T-171 Phase A introduces `GlobalInputRouter`; MainMenuScreen migrated as proof. Phase B (T-172) does the rest.
5. **NEW:** `InputMultiplexer.size` is a **method** in libGDX 1.14, not a property — use `mux.processors.size` from Kotlin. (Surfaced in T-171 Phase A.)
6. **NEW:** `GlobalInputRouter.install()` must run BEFORE `setScreen()` in `Main.create()` — otherwise the first screen's input-processor assignment in its `init` clobbers the router immediately. Documented in code.
7. **NEW:** `SettingsManager` lives **inside** `Settings.kt` (one file), not a separate file. Surfaced repeatedly across T-121/T-143 sub-agent reports — pre-emptively note in future Settings-touching tickets to save the sub-agent a grep.
8. **NEW:** `StormSentinel` is NOT an `Enemy` subclass — it's a standalone class. Future entity-base refactors must remember it doesn't inherit. (Surfaced in T-170.)
9. **NEW:** Smoke autopilot path uses `InputManager.setDebugOverrideEnabled` + per-key force fields — completely orthogonal to `Gdx.input.inputProcessor`. The router doesn't interfere with smoke runs. Important for future router/input work.
10. **Carried forward (unchanged):** Smoke CI runner stalls on `apt-get install xvfb`; mitigation `gh run rerun --failed`. GitHub Copilot auto-review rate-limited until May 18 — red X on every PR, not a required check, ignore.

### Tooling gotchas — added one
All prior gotchas remain in `LEARNINGS.md`. Added this session: **"Every Screen resets Gdx.input.inputProcessor — root multiplexers get clobbered (T-147)"** — see LEARNINGS.md 2026-05-13 section. Architectural-cleanup ticket candidate noted (now filed as T-171/T-172).

---

## Known issues / open questions

- **QUESTIONS.md status:** T-125-Q1 (Calibri) now resolved (T-126 shipped). T-120-Q1 (i18n numeric formats) still deferred until a second locale lands.
- **Crash artifact** at `C:\Users\Radmin\.cloudy-ninja\crashes\crash-20260513-085452.log` (from prior session's T-115 verification) and **screenshot artifacts** at `C:\Users\Radmin\.cloudy-ninja\screenshots\` (from T-139 + T-147 testing if any) — safe to delete.
- **Next free ticket number is T-173.** (T-148 was used; T-149-T-167 mostly research/marketing already shipped per prior HANDOFF. T-168-T-172 spec'd this session.)
- **Many `agent-*` worktrees** under `.claude/worktrees/` from this session's parallel dispatches — leave them; the harness manages cleanup. The successful ones (changes pushed) keep their branches for trail; the dead ones auto-clean.

---

## At end of your session

1. Bump "Last updated" + summary
2. Update "Live state" with anything new
3. Update "In-flight threads" — remove what's done, add what's new
4. Capture new gotchas in `LEARNINGS.md` and reference here
5. Commit + push to main (direct push via admin bypass for docs-only)

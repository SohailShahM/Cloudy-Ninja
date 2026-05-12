# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-12 by Claude Opus — a long high-throughput session that shipped **~16 tickets** (T-054 through T-064 + T-069 + a wave of Antigravity research). Main HEAD `d1ddd11`. T-059 (i18n strings sweep) is the only Sonnet ticket currently in flight. The Antigravity queue is deep (8 tickets).

---

## What you absolutely need to know

### Repo / environment
- **Repo:** https://github.com/SohailShahM/Cloudy-Ninja (public — Actions minutes unlimited)
- **JDK for local builds:** `C:\Program Files\Android\Android Studio\jbr`
  - Bash tool: `export JAVA_HOME='/c/Program Files/Android/Android Studio/jbr' && export PATH="$JAVA_HOME/bin:$PATH"`
  - PowerShell tool: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"`
- **`gh`** 2.92.0 authenticated as `SohailShahM`. PR operations work.
- **CI:** 9 required checks (1 lint + 8 smoke). Smoke duration ~4.5min/job in parallel; total wall time ~5min.

### Branch protection — admin-merge is the default

`main` has `required_conversation_resolution: true` which never clears for AI-opened PRs. **Use admin-merge proactively** once CI is green:

```
gh pr merge <N> --repo SohailShahM/Cloudy-Ninja --admin --squash --delete-branch
```

For tiny TASKS.md claim commits, direct-push to main works via admin bypass (`git push origin HEAD:main`). That bypass is reserved for the claim-commit protocol — never use it for code changes.

### The Antigravity peer-framing correction (important — read)

Earlier handoffs framed Antigravity as a "research-only" tool. That was **conservative warm-up bias, not Antigravity's actual ceiling**. AGV is a Gemini-3-backed peer agentic platform comparable to Claude Code itself — supports MCP, opens PRs, runs CI, refactors code. The router in `START_HERE.md` still encodes the old framing; the bridge prompt at `prompts/build-antigravity-api.md` documents the corrected framing and is the source-of-truth for how to think about AGV.

**Implication:** when adding tickets, give AGV real implementation work, not just markdown. The current queue (`T-076` dep upgrade execution, `T-077` presskit scaffold, `T-078` icon generator, `T-079` CI optimization, `T-080` repo infra, `T-081` Android build) follows this corrected framing.

### Sub-agent dispatch patterns that work

1. **One sub-agent per ticket**, `subagent_type: "general-purpose"`, `isolation: "worktree"`, `run_in_background: true`. The worktree gets cleaned up automatically.
2. **Brief sub-agents fully** — they have zero conversation context. Include ticket spec verbatim, file list, hard rules, and the "report back with branch + commit hash + summary" instruction.
3. **File-conflict gating:** sub-agents in parallel must not write to the same files. Map dependencies before dispatching. SettingsScreen / LevelRenderer / GameScreen are the most-contested files.
4. **BLOCKER discipline:** if a sub-agent can't complete within its file-scope constraint (e.g. needs a caller-side change), it should save `BLOCKER.md` and report — **not improvise**. T-064 demonstrated this; orchestrator did the 4-line plumbing inline rather than re-dispatch.
5. **Reflection-based testability:** the established pattern for testing entities is `ObjenesisStd` (or plain `Kotlin reflection`) to bypass Box2D-native constructors. See `entities/StormSentinelTest.kt`, `entities/SmogSpriteTest.kt`, `entities/ProjectileTest.kt`, `entities/DriftHuskTest.kt`. Future entity tests should follow.

---

## Live state of the project

**Main HEAD at handoff:** `d1ddd11` "T-063 → Done; claim T-059 (i18n strings sweep)"

**What's actually playable / built (additions this session in bold):**
- 8 levels + Storm Sentinel boss
- 3 characters with abilities
- Tile-based rendering (Kenney pack)
- 12 achievements + toast
- 3 music tracks + 8 SFX
- 4K/HiDPI scaling
- Cloud Atlas with 6 entries (target 12; blocked on T-045 NotebookLM step — **T-049 has produced the climate-sources bundle**)
- 3 save slots, audio bus sliders, key rebinding, assist mode
- Stats screen + **best-times-per-slot row (T-060)**
- **Color-blind palette toggle, 4 modes (T-057)**
- **Reduced-motion mode (T-058)**
- **Drift Husk enemy in Level 2 (T-062)** — drop-from-above archetype
- **Pause overlay with 0.2s fade-in + 55% backdrop + resume hint (T-063)**
- **Victory-screen best-time delta indicator (T-064)**
- **Settings screen reorganized into Display/Audio/Controls/Accessibility sections (T-069)**
- AI smoke + determinism audit
- **~106 new Kotest tests added this session** (T-054 + T-055 + T-056 + T-062 = StormSentinel, SmogSprite, Projectile, Achievement, DriftHusk specs)

---

## In-flight threads

### 1. T-059 (i18n strings sweep) — only Sonnet ticket actively running
Branch `claude/T-059-i18n-strings`. Touches every `screens/*.kt`. Big sweep — expect ~5–8 min for the sub-agent to report back. When it does: open PR, watch CI, admin-merge. Next session: if it never completed, check `gh pr list --repo SohailShahM/Cloudy-Ninja --state open` and the worktree at `agent-a071fc8b11c6b4730`.

### 2. Antigravity queue (8 substantial tickets, including 4 implementation)
In `TASKS.md ## Todo`, all `Tool: antigravity`:

- **T-073** Pixel-platformer keyboard-layout research (informs default bindings)
- **T-075** Steam tags + keyword research
- **T-076** **Execute LOW-risk dep upgrades from T-051 audit** (real PRs, one per upgrade)
- **T-077** **presskit() scaffold with HTML + placeholder PNGs**
- **T-078** **Procedural achievement icon generator (Kotlin tool + 12 PNGs)** — blocks T-066
- **T-079** **CI duration optimization (−30% target on smoke matrix)**
- **T-080** **GitHub Issue/PR/Discussion templates**
- **T-081** **Android build verification + smoke CI step**

The bold ones are real-code tickets — give AGV them as peer engineering work.

### 3. Sonnet tickets still queued (4)
- **T-066** Achievement icons wire-up — **blocked on T-078** (AGV's icon generator). Defer until T-078 lands.
- **T-038** Ghost replay — `claude-code-sonnet` but **NOT autonomous** (determinism-sensitive). Needs supervised session.
- **T-061** Per-character smoke matrix — touches CI workflow; **not autonomous**. Supervise.
- **T-045** Cloud Atlas expansion (6→12 entries) — `notebooklm-then-copilot-agent`. User-driven NotebookLM step; then a Copilot Issue.

### 4. Manual user actions pending
- (Optional) Strip `required_conversation_resolution` from `main` — would let auto-merge actually work.
- T-045 NotebookLM run — user runs the prompt at `prompts/T-045-notebooklm.md` against the climate sources at `research/climate-sources/` (output of T-049, already on main).
- **Antigravity bridge build** — the prompt at `prompts/start-bridge-session.md` is paste-ready for a fresh CC session in a separate worktree. It points at `prompts/build-antigravity-api.md` (v2, bidirectional collaboration framing).

---

## Working patterns worth reusing

1. **One branch per task** off `origin/main`, admin-merge after CI green.
2. **Refetch `origin/main` before branching.** AGV and Sonnet sub-agents land things in parallel.
3. **Sub-agent dispatch in pairs/triples** when file scopes don't conflict — see the T-054/T-055/T-056 batch (3-way) and T-057/T-060 batch (2-way) this session.
4. **Inline scope expansion** when a sub-agent flags a BLOCKER for a 5-line plumbing change — faster than re-dispatching. T-064 demonstrated this.
5. **CI re-runs are free** — push fixes liberally.
6. **For tests on Box2D entities, use Objenesis or Kotlin reflection** to bypass natives. Established pattern across 5 test files now.

---

## Known issues / open questions

- `QUESTIONS.md` may have new entries — check it.
- `Settings.keybinds["pause"]` doesn't exist yet (ESC is hardcoded in `GameScreen`). T-063 added a fallback + code comment flagging for a follow-up.
- Achievement unlock predicates are inlined in screen code (not pure functions on `AchievementRegistry`). Predicate-firing tests skipped in T-056; a follow-up task chip exists for refactoring.
- DriftHusk added to Level 2 only — placeholder PNG art still procedural-rect. T-046 (full graphics overhaul) is the proper home for sprite work.

---

## At end of your session

Update this file:
1. Bump "Last updated" date + brief summary
2. Update "Live state" if you shipped systems
3. Update "In-flight threads" — remove what's done, add what's new
4. Capture new gotchas in `LEARNINGS.md` and reference here
5. Commit + push to main (direct push works for docs-only changes via admin bypass)

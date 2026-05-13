# Multi-AI Development Workflow — Case Study (Cloudy Ninja)

**Project:** Cloudy Ninja — libGDX Kotlin 2D pixel-art platformer (Sprint D, public alpha)
**Window covered:** 2026-05-09 → 2026-05-13 (~5 days; primarily two multi-hour autonomous sessions on 05-12 and 05-13)
**Author identity:** `claude-code-sub-agent` (Sonnet) writing a retrospective from the project's transcript / HANDOFF.md / TASKS.md / LEARNINGS.md
**Status:** Honest retrospective, not a sales pitch. Failures are catalogued alongside wins.

---

## 1. Roles

The project used five distinct AI roles, each routed by a `Tool:` tag on every ticket in `TASKS.md`. The routing matrix lives in `START_HERE.md` §9. Skipping it is the primary failure mode of multi-AI work, so the project enforced it as a hard gate before claim.

### 1.1 Planner — Claude Code Opus (`claude-code-opus`)
- One human session, long-running, in interactive Claude Code (MSIX-packaged CLI).
- Responsibilities: ticket selection, ticket specification, sub-agent briefing, dispatch, merge gating, HANDOFF updates, file-conflict adjudication, inline plumbing when a sub-agent flagged a scope BLOCKER (e.g. T-064 victory-screen delta — Opus added the 4-line caller-side change inline rather than re-dispatch).
- Used sparingly: roughly one ticket per dozen was implemented by Opus directly. Most Opus turns dispatched Sonnet sub-agents or coordinated merges.

### 1.2 Implementer pool — Claude Sonnet sub-agents (`claude-code-sub-agent`)
- Spawned via the `Agent` tool with `subagent_type: "general-purpose"`, `isolation: "worktree"`, `run_in_background: true`.
- Each sub-agent gets the ticket spec verbatim + a file list + hard rules + a report-back format. Zero conversational context. The worktree path (`.claude/worktrees/agent-<id>/`) keeps each agent's filesystem disjoint.
- Tier coverage: S, M, L. Routine refactors, test-spec authoring, screen additions, single-subsystem features. Almost every test-spec ticket (T-054 through T-096) went to this pool.

### 1.3 GitHub Copilot coding agent (`copilot-agent`)
- Runs autonomously on github.com from an Issue. No local IDE access; cannot read TASKS.md, only the Issue body.
- Hard limit: **Tier S only** (single-file or single-screen scope). Multi-file refactors are out of scope and the agent is supposed to abort to `QUESTIONS.md`.
- Successful runs: T-041 (StatsScreen end-to-end, PR #3), T-111 (SoundManager log→error, PR #68 — diff verified correct).
- Less successful: T-122 (i18n wire-up) sat in draft for hours; T-035 (audio bus sliders) and T-127 (dead gradle deps) were held back to avoid expanding the `action_required` policy queue (see §4).

### 1.4 Google Antigravity (`antigravity`) via cc-agv-bridge
- Gemini-3-backed peer agentic platform. Six models with separate quota buckets (Gemini 3 Flash + Pro low/high, Sonnet/Opus 4.6 Thinking, GPT-OSS 120B). Flash is the workhorse; Pro/Opus reserved for harder reasoning.
- Coordinated with Claude Code via the `cc-agv-bridge` MCP server (https://github.com/SohailShahM/cc-agv-bridge) — shared SQLite state at `C:\Users\Radmin\cc-agv-bridge\state.sqlite`. Six bridge tools: `bridge_ask`, `bridge_request_review`, `bridge_submit_review`, `bridge_send`, `bridge_receive`, `bridge_handoff`, plus `bridge_diagnose` and `bridge_status`.
- AGV self-rates per-model capacity green/yellow/red on every status broadcast. Capacity, not capability, is the bottleneck.
- Used for research-heavy tickets (T-046a tileset research, T-047 audio research, T-048 itch.io listing study, T-049 climate sources, T-050 outreach list, T-051 dep upgrade audit, T-052 festivals, T-053 eco-game comparison). Also tried for some implementation work (T-077, T-078, T-080) but those got re-routed to Sonnet sub-agents when AGV went quiet (see §4 silent-death pattern).
- Bridge was wired mid-session on 2026-05-12. End-to-end smoke-tested on the bridge repo, but **not yet dogfooded on a real Cloudy-Ninja ticket** as of session close on 2026-05-13.

### 1.5 Humans (`human`)
- Reserved for: branch protection, git config, secrets, asset-style decisions, real-controller gamepad smoke, font replacement (the alpha-blocking T-126 Calibri swap — `autonomous-eligible: no` because the font touches every UI screen and smoke CI does not validate font readability).

---

## 2. Dispatch patterns

### 2.1 File-conflict gating
Parallel sub-agents must not write to overlapping files. The most-contested files were tracked explicitly in HANDOFF.md and avoided when batching:
- `screens/SettingsScreen.kt`
- `screens/LevelRenderer.kt`
- `screens/MainMenuScreen.kt`
- `i18n/Strings.kt`
- `screens/GameScreen.kt` (the big one — pause-overlay tickets, camera tickets, hotkey tickets all contend here)

When a batch dispatch hit GameScreen contention, the planner either serialized those tickets or added a `Depends on:` line so the second ticket would wait for the first to merge. The 2026-05-13 In-Progress section shows this in action: T-140, T-130, T-138 all depend on T-128 (the predicates refactor) precisely because they touch overlapping surfaces.

### 2.2 Parallel batches of 2–7
The 2026-05-12 session ran multiple 4-way parallel dispatches successfully. The 2026-05-13 session pushed this to 7-way batches when scopes were fully disjoint (test files, new files, distinct subsystems). The pattern: spawn N sub-agents in one message via parallel tool calls, let them race to claim TASKS.md (rebase rounds expected), then collect PRs as they come in.

### 2.3 Isolation via worktree
`isolation: "worktree"` is mandatory for parallel dispatch. Each sub-agent runs under `.claude/worktrees/agent-<id>/` with its own working copy. The harness cleans up worktrees on completion. Without isolation, two parallel sub-agents would step on each other's `git checkout` calls.

Source-side quirk learned: a sub-agent's worktree may be dozens of commits behind the main repo's working copy if it was created early in the session. T-117 hit this — its agent edited files that didn't reflect recent merged work. Recovery: have the agent rebase its branch on `origin/main` before reporting back.

### 2.4 Brief fully, broaden nothing
Sub-agent prompts are self-contained. The dispatch includes the ticket spec verbatim, the file list, hard rules ("don't touch files outside this list"), and the report-back format. **No conversational context.** If the agent hits ambiguity, it writes `BLOCKER.md` and reports — it does not improvise. T-064 and T-066 are good examples: both flagged scope blockers; the planner either patched inline (T-064) or spawned a follow-up ticket via the `spawn_task` chip (T-066 → T-108).

### 2.5 BLOCKER discipline
The contract: a sub-agent that can't complete within its scope produces `BLOCKER.md` describing the blocker and reports back. It does **not** edit caller-side files unprompted. This kept scope creep nearly zero across ~30+ sub-agent dispatches. The cost: the planner has to handle the occasional 4-line inline fix or spawn a follow-up. Worth it.

---

## 3. What worked

### 3.1 Volume — 40+ PRs in a multi-hour autonomous run
The 2026-05-12 session shipped roughly 32 tickets end-to-end. The 2026-05-13 session added 22 more merges + 14 tickets shipped + 19 new tickets specced. Across the two sessions, **more than 40 PRs were merged autonomously** — the human approved the session start and reviewed at the end; everything between was AI dispatch + AI implementation + AI merge.

### 3.2 Doc-PR skip filter (T-079 v2)
The CI workflow has a gate job that inspects changed paths. PRs touching only `*.md`, `prompts/`, `marketing/`, `research/`, `.github/ISSUE_TEMPLATE/`, etc. skip the entire 8-job smoke matrix. Empirically validated by PR #63: gate ran in 6s, all 8 smoke jobs reported `skipped`, total wall time ~2m31s vs ~5m baseline for code PRs. Saved significant Actions minutes once the project hit the Education-Pack 3,000-min/mo private-Actions cap on 2026-05-12.

The v1 attempt at the same ticket tried matrix-packing (8 jobs → 3 jobs) and was **reverted** because real CI data showed projected savings didn't materialize (warm-gradle assumption was wrong) and wall time doubled. Lesson: sub-agent projections can be optimistic; verify against real CI data before merging. This is documented as a `Lesson learned` line on T-079 itself in TASKS.md.

### 3.3 Admin-merge on conversation-resolution lock
`main` has `required_conversation_resolution: true`, which never clears for AI-opened PRs (no human reviewer to mark threads resolved). The workflow: once CI is green, admin-merge proactively:
```
gh pr merge <N> --repo SohailShahM/Cloudy-Ninja --admin --squash --delete-branch
```
This is **the** unblocker. Without it, every AI PR would hang on a process gate that exists for human review. For tiny `claim T-XXX` commits on TASKS.md, `git push origin HEAD:main` works via admin bypass — reserved for claim commits only, never for code changes.

Side effect: `gh pr merge --admin` returns silent (no stdout/stderr) on success. Looks like nothing happened. Verify with `git log -1 origin/main` after.

### 3.4 Reflection-based testability for libGDX entities
Box2D entity classes (`StormSentinel`, `SmogSprite`, `Projectile`, `DriftHusk`) require a live GL context to instantiate. Pattern established: bypass the constructor with `ObjenesisStd.newInstance(Class)` or, for screens that touch `SpriteBatch`/`Texture`, `sun.misc.Unsafe.allocateInstance(Class)`. Then set private fields via Kotlin reflection. This unlocked ~270 Kotest specs across the 2026-05-13 session alone.

Companion pattern: **MockK on libGDX statics.** `Gdx.app`, `Gdx.audio`, `Gdx.files` get mocked in `beforeSpec`, restored in `afterSpec`. Applied to `SaveManagerTest`, `MusicManagerTest`, `SoundManagerTest`, `FontManagerTest`.

### 3.5 Re-route pattern when AGV is quiet
Used 4× across the 2026-05-12 session. When Antigravity went silent on a critical-path ticket, the planner re-tagged the ticket to `claude-code-sub-agent` and dispatched directly. Format: `Tool: claude-code-sub-agent *(re-routed YYYY-MM-DD from antigravity — reason)*`. T-077 (presskit), T-078 (icon generator), T-080 (repo infra) all shipped clean within ~10 minutes of re-route.

### 3.6 The single command (`START_HERE.md` §10)
The user pastes this one line into any AI in any tool:
> *"Read `START_HERE.md` and work on T-XXX."*

The AI reads the doc, identifies itself, finds appropriate work, claims, implements, opens a PR. Identical text regardless of tool. Cut session start-up overhead to near zero.

---

## 4. What failed

This section is non-negotiable. Every multi-AI retrospective tends to oversell; this one won't.

### 4.1 Three silent agent deaths (recovered)
The 2026-05-13 session had **three sub-agents die silently mid-task**: the predicates refactor (spawn-task chip), T-098 (enemy hit-flash), T-104 (splash screen). Each agent claimed the ticket (pushed a `claim T-XXX` commit to main) but never pushed implementation work. Detection: branch exists on origin but has only the claim commit, hours old.

Recovery: re-dispatch with an explicit "this is a re-dispatch" briefing, instruct the new agent **not** to re-claim (it should update the existing In-Progress block's `Agent:` line instead). Force-removing the dead worktree may be required if its lock blocks branch reuse.

This is the dominant failure mode of parallel dispatch. The cause is opaque from outside (agent transcript is gone when the harness ends the run). Adding heartbeat output to sub-agent prompts is a recommended mitigation — none of the dead agents emitted progress logs before disappearing.

### 4.2 Copilot `action_required` policy gate
PRs #68 (T-111) and #85 (T-122) ended the 2026-05-13 session stuck in `action_required` state. GitHub Actions treats first-time bot contributors as untrusted and requires approval for each workflow run. The fix is either:
- Approve each run in the Actions UI tab (manual click per run), or
- Change repo Settings → Actions → "Require approval for first-time contributors" policy (one-time, repo-level).

The `POST /actions/runs/{id}/approve` API returns **404 for non-fork bot PRs** — it only works for fork PRs. This blocked at least three downstream tickets (T-127, T-035, T-105, T-118, T-121 all held back to avoid expanding the queue). Held tickets compounded into a bottleneck the human had to clear on return.

Secondary trap: `gh pr close && gh pr reopen` on a bot-authored PR deletes in-flight CI runs **without triggering new ones**. Don't do this on Copilot PRs. Documented in HANDOFF.md gotchas §2.

### 4.3 `xvfb-run` apt-get flake (T-A1 era)
The AI smoke workflow runs an 8-level matrix via `xvfb-run` on `ubuntu-latest`. Early in the project (T-A1), CI burned 8 layers of bugs before going green: desktop.ini junk file in repo, gradlew chmod missing, threshold tuning (`deltaX<0.3`, `frameP99>80ms`), JVM queue saturation, level-hopping in autoquit logic, atlas-overlay blocking the update loop, cold-runner timeout, and finally an `apt-get update` flake on `xvfb` install. Each layer is documented in `LEARNINGS.md`. Six PR cycles before the first green smoke run.

### 4.4 v1 of T-079 — optimistic projections
Already covered in §3.2. The point: a sub-agent will sometimes confidently project savings that don't materialize. **Verify against real CI data before merging optimization work.** The v1 → v2 iteration was the right move and is now policy.

### 4.5 `gh issue edit --add-assignee` falls back to repo owner
Using `@copilot`, `Copilot`, or `copilot-swe-agent` as the assignee may silently re-assign the issue creator (the human, `SohailShahM`). Workaround: `gh issue edit N --add-assignee copilot-swe-agent` does assign Copilot; then explicitly `--remove-assignee SohailShahM` to clean up. Or use the web UI. This wasted a debugging cycle the first time it happened.

### 4.6 The bridge isn't dogfooded yet
`cc-agv-bridge` is wired into both sides (CC `~/.claude.json`, AGV `~/.gemini/antigravity/mcp_config.json`) and smoke-tested end-to-end on its own repo. But as of 2026-05-13 session close, **no real Cloudy-Ninja ticket has gone through a `bridge_ask` or `bridge_handoff` between live agents.** Calling this validated would be overclaiming.

### 4.7 Other gotchas worth pinning
- **Worktree path gotcha**: sub-agents must use their worktree path, not the main repo path. T-117 hit this; recovered.
- **Manual exception verification leaves artifacts**: T-115 (crash reporter) verified by throwing a real exception, leaving `C:\Users\Radmin\.cloudy-ninja\crashes\crash-*.log` behind. Harmless; document so the human knows it's safe to delete.
- **Workflow trigger gap**: `ai-smoke.yml` does NOT include `ready_for_review` in its `pull_request` activity types. Promoting a draft Copilot PR to "ready" does not trigger CI. This is a CI-policy change requiring a human surface.

---

## 5. Recommended workflow for similar projects

This is what the project would do again, tightened by everything in §4.

### 5.1 Pre-flight (one-time)
1. **Write `START_HERE.md` first.** Identity table + capability gates + the single command. Without this, every session is rediscovery.
2. **Write `HANDOFF.md` immediately after.** Live-state continuity doc. Short (under 200 lines). Updated at every session close.
3. **Set up branch protection with admin-merge as the planned path.** `required_conversation_resolution: true` will never clear for AI PRs. Plan for admin bypass from day 1, not as a workaround.
4. **Build a CI gate job with a doc-PR skip filter** before the project's compute budget tightens. The savings are real and compound.
5. **Decide upfront whether the repo is public or private.** Private Actions cost money once you exceed the free-plan limit (or zero on Free without spending-limit changes). Public is unlimited + free. The middle ground of "public + proprietary-licensed" is reasonable for unreleased indie projects (visibility for community, rights reserved for commercial).

### 5.2 Per-ticket dispatch sizing
- **Sub-agent prompts:** Self-contained, 30–80 lines. Ticket spec verbatim + file list + hard rules + report format. No conversational context. No "you know this already" assumptions.
- **Hard rules to include in every dispatch:** "don't touch files outside this list", "if blocked, write `BLOCKER.md` and report — don't improvise", "report-back format: PR number, line count, CI status".
- **Tier sizing:** Sub-agents reliably handle Tier S and Tier M (test specs, single-subsystem features, screen additions). Tier L is doable but watch for scope creep. Don't dispatch architecture tickets — those go to the planner directly.

### 5.3 When to plan vs dispatch
- **Plan (Opus / human planner)** when: spec is ambiguous, multiple files contend, a previous attempt died, or the ticket touches branch protection / secrets / CI workflow files. Also: every HANDOFF update.
- **Dispatch (Sonnet sub-agent)** when: file scope is clear, spec is unambiguous, no upstream dependency is in flight on the same files. Default to this.
- **Re-route from external agents** when a tool goes quiet for >30 min on a critical-path ticket. Don't wait indefinitely for AGV / Copilot. The capacity / availability is the bottleneck, not capability.

### 5.4 Parallel batch sizing
- **Safe ceiling for fully-disjoint file scopes:** 7 sub-agents. Tested live on 2026-05-13.
- **Practical default:** 2–4. Most batches of 4 hit zero conflicts; batches of 7 hit conflicts in TASKS.md (claim contention) but resolved cleanly via rebase rounds.
- **Always:** dispatch in a single message via parallel tool calls. Sequential dispatch loses the parallelism benefit and confuses the audit trail.

### 5.5 Failure detection
- **Treat sub-agent silence as failure**, not patience. If a sub-agent doesn't report back within ~15 min and the ticket isn't an L-tier ticket, check the branch. Only-claim-commit-on-origin = dead agent.
- **Add a heartbeat instruction** to sub-agent prompts: "if your work takes longer than 10 min, emit a one-line status update". None of the three dead agents in §4.1 emitted one before disappearing — adding this won't catch all silent deaths, but it'll catch some.
- **Keep a roll-up of held PRs.** When `action_required` or any external policy gate blocks merges, downstream tickets compound. Track them explicitly in HANDOFF.md.

### 5.6 Non-negotiables
- **Don't oversell.** A smoke-tested integration is not "battle-tested". One green CI run is not "validated under load". Write retrospectives that future agents will trust.
- **Document gotchas immediately.** `LEARNINGS.md` is read by every new agent. An undocumented gotcha will be re-hit by the next session.
- **Keep the planner cheap.** Don't burn Opus on mechanical work. Sub-agents are the volume tier; the planner is for decisions, not implementation.

---

## 6. Numbers (honest)

- **PRs merged across the two main autonomous sessions:** 40+ (32 on 2026-05-12, ~22 on 2026-05-13).
- **Test specs added:** ~600+ Kotest tests project-wide; ~270 of them on 2026-05-13 alone.
- **Tickets specced for follow-up:** 19 new tickets (T-109..T-127) including 1 alpha-blocking legal issue (T-126 Calibri replacement).
- **Sub-agent silent deaths:** 3 (predicates refactor, T-098, T-104). All recovered via re-dispatch.
- **Copilot PRs stuck on `action_required`:** 2 at session close (#68, #85). Required human intervention to unblock.
- **External agent re-routes:** 4 from `antigravity` to `claude-code-sub-agent` due to AGV going quiet.
- **`cc-agv-bridge` real-ticket dogfood count:** 0 as of 2026-05-13. Wired + smoke-tested only.

---

## 7. Open questions for the next iteration

1. **Is there a way to detect silent sub-agent deaths from outside the harness?** A timeout + branch-state poll might work but adds polling overhead.
2. **Does the `cc-agv-bridge` dogfood land cleanly?** First real cross-agent task (CC requests review from AGV-Flash on a single-file PR) is the test. Until that happens, the bridge is theoretical infrastructure.
3. **Can the `action_required` policy be flipped at the repo level without weakening security?** The current policy gate exists for good reason (untrusted-bot protection). Disabling it project-wide is a tradeoff worth the human's explicit consent.
4. **How small should sub-agent prompts get before they hurt?** Current size (30–80 lines) feels right, but the floor hasn't been tested. Sub-S tickets (one-line fixes) might be cheaper as direct Opus implementations than as dispatched sub-agents — the dispatch overhead may exceed the work.

---

*Written 2026-05-13. References: `HANDOFF.md`, `START_HERE.md`, `TASKS.md` (## Done section), `LEARNINGS.md`, `DETERMINISM.md`. Counter-anchored against the user-memory note "smoke ≠ dogfood; don't overclaim."*

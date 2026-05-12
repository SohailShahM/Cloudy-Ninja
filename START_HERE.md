# START_HERE.md — onboarding for any AI agent

> The single entry point for every AI working on Cloudy Ninja. Read this first.
> If you skip this doc, you'll waste tokens rediscovering things.

## You are working on Cloudy Ninja
A libGDX Kotlin 2D pixel-art platformer about restoring corrupted ecosystems. Three switchable characters with water-cycle-themed abilities, 7 levels including a Storm Sentinel boss, ambient music, save slots, 4K/HiDPI, mobile + desktop. Currently post-Sprint-C; Sprint D goal is shipping a public alpha to itch.io.

## 1. Identify yourself

When claiming any task, set `Agent:` to your handle from this list (pick the closest match):

| Identity tag | Use this if you are... |
|---|---|
| `claude-code-opus` | Claude Code session running Opus (planning/architecture work) |
| `claude-code-sonnet` | Claude Code session running Sonnet (implementation) |
| `claude-code-sub-agent` | A Sonnet sub-agent dispatched from a parent Claude Code session |
| `copilot-agent` | GitHub Copilot coding agent (autonomous from a GitHub Issue, runs on github.com) |
| `copilot-android-studio` | GitHub Copilot inside Android Studio / JetBrains IDE (user-driven) |
| `copilot-vscode` | GitHub Copilot in VS Code (user-driven) |
| `antigravity` | Google Antigravity agent (Gemini-backed, autonomous, web/CLI) |
| `gemini-code-assist` | Gemini Code Assist in IDE (user-driven) |
| `notebooklm` | Google NotebookLM (research/content generation only, no code) |
| `human` | The user is doing this manually |

## 2. Read these files in order

0. **`HANDOFF.md`** — if it exists at repo root, **read it before anything else**. Captures live state from the previous session: branch-protection quirks, in-flight threads, pending user actions, working patterns. Short by design. If it's missing/stale (>1 week old), treat the rest of this doc as authoritative and start from scratch.
1. **This file** (`START_HERE.md`) — you're here
2. **`AGENTS.md`** — architecture, module layout, conventions
3. **`TASKS.md`** — work queue. Find tasks tagged for YOUR identity in `## Todo`
4. **`LEARNINGS.md`** — gotchas previous agents discovered. **Read this before claiming.**
5. **`GAME_PLAN.md`** — vision and scope (skim for context)
6. **`GDD_ADDENDUM.md`** — technical reference. Only read sections relevant to your ticket.

## 3. Capability gates — what you ARE and AREN'T allowed to do

Hard rules. The user routes tickets to specific tools. If your identity isn't on the ticket's `Tool:` line, **do not claim that ticket**, even if it's unclaimed and looks easy. Routing wrong tools at tasks is the primary failure mode of multi-AI systems — these gates exist to prevent it.

| Identity | Hard limits |
|---|---|
| `claude-code-opus` | Any tier, any complexity. Reserved for architecture, ambiguous specs, ticket selection, planning. Don't burn Opus on mechanical work — delegate to Sonnet sub-agents. |
| `claude-code-sonnet` | Tier S, M, L. Any subsystem. Spawn sub-agents for parallel mechanical work. |
| `claude-code-sub-agent` | Whatever scope your parent session briefed you with. Don't broaden scope. Report back to parent. |
| `copilot-agent` | **Tier S only.** Single-file or single-screen scope. If a ticket needs multi-file refactor, abort and post to `QUESTIONS.md`. You run on github.com from issues; no local IDE access. |
| `copilot-android-studio` | Tier S–M. Single subsystem. User supervises — not autonomous. Don't run terminal commands without confirmation. |
| `copilot-vscode` | Same as Copilot in Android Studio. |
| `antigravity` | Tier S, M, L — capability per token is high (Gemini 3.x, MCP, sub-agents, opens PRs), but **capacity is the bottleneck** — quotas refresh every 5h and burn fast. Best leveraged as a consultant + limited executor: planning input, second-opinion reviews, blocking Q&A, and small surgical tasks sized to remaining quota. **Bulk mechanical work — multi-file refactors, dep upgrades, asset pipelines — goes to `copilot-agent` or `claude-code-sonnet`, not AGV.** Coordinate with Claude Code via [cc-agv-bridge](https://github.com/SohailShahM/cc-agv-bridge). Report PR-comment status every 30 min if running >1 hour. |
| `gemini-code-assist` | Tier S. IDE-driven, user supervises. |
| `notebooklm` | **Content generation only — no code, no commits.** Output is markdown delivered to the user or pasted into a ticket spec. |
| `human` | You're the user. Do the work manually. |

## 4. How to claim a task

1. Read `## Todo` in TASKS.md. Find tasks tagged with your identity in the `Tool:` field whose `Depends on:` tasks are all `Done`.
2. Move the entire task block from `## Todo` to `## In Progress`.
3. Fill in `Agent:` (your identity), `Tool:` (already filled, leave it), `Branch:`, `Started:` (today's date).
4. Commit ONLY this TASKS.md change. Message: `claim T-XXX`. Push.
5. If `git push` rejects (conflict — someone claimed it first): pull, pick a different task.

## 5. How to work

1. Create worktree: `git worktree add ../cn-T-XXX -b <your-identity-prefix>/T-XXX-short-slug`
   - Branch naming: `claude/...`, `copilot/...`, `antigravity/...`
2. Do the work per the ticket's `Goal` and `Done when` fields.
3. Commit small, push often.
4. Open a PR against `main` when done. PR title: `T-XXX: <one-line summary>`.
5. CI must pass before merge — see `.github/workflows/ai-smoke.yml` and `.github/workflows/ci.yml`.

## 6. How to finish

1. Merge to main (auto-merge if CI green + 1 review, or manual).
2. Move the task block to `## Done` in TASKS.md.
3. Fill in `Completed:`, `Outcome:` (one-line summary), `Commit/PR:` (hash or PR link).
4. **If you hit a non-obvious gotcha**, append an entry to `LEARNINGS.md` so the next agent doesn't repeat it.

## 7. When to ask vs proceed

| Situation | Do this |
|---|---|
| Ambiguous spec — multiple defensible interpretations | Pick the most conservative one. Document why in the PR. |
| You need a decision the user hasn't made (e.g. art style, naming) | Append to `QUESTIONS.md` and release the claim. |
| Touching files outside the ticket's `Files:` list | Stop. Append to `QUESTIONS.md` asking whether to expand scope. |
| Your CI smoke test fails after your change | Diagnose. If you fixed a regression, document in `LEARNINGS.md`. |
| Ticket spec is wrong or outdated | Append to `QUESTIONS.md`, don't silently "fix" it. |
| Adding a new dependency | Stop. Append to `QUESTIONS.md`. |
| Renaming a file or moving a package | Stop. Append to `QUESTIONS.md`. |

**Never:** force-push to main, bypass git hooks with `--no-verify`, disable failing tests, edit `.gitignore` to hide your changes, modify `git config`.

## 8. Workflow notes per tool

### `notebooklm` — content generation
You produce *spec content* that other agents wire into code. Never write or commit code.

Workflow for content tickets (e.g. T-045 Cloud Atlas entries):
1. User uploads source documents to a notebook at notebooklm.google.com
2. User chats with the notebook to draft content
3. Output is markdown — pasted into the relevant ticket's spec or a `content/` file
4. Ticket is then re-tagged with the next tool in the chain (e.g. `copilot-agent`) to wire the content into code

The `Tool:` field for chained tickets uses the syntax `notebooklm-then-X` (e.g. `notebooklm-then-copilot-agent`).

### `copilot-agent` — GitHub Copilot coding agent
You operate from GitHub Issues. The user (or a sync script) copies a TASKS.md ticket into an issue and assigns it to `@copilot`. You read the issue, plan, write code on a branch, and open a draft PR.

You cannot read TASKS.md directly during your run — work only from the issue text. Stay within the issue's described scope.

### `antigravity` — Google Antigravity
Capacity-limited peer. Not a junior research helper, but also not the workhorse — bulk mechanical work belongs with `copilot-agent` or `claude-code-sonnet`. AGV's value is high-quality input on a smaller volume of work.

**Coordinating with Claude Code on the same work:** the [cc-agv-bridge](https://github.com/SohailShahM/cc-agv-bridge) MCP server is wired into both AGV (`~/.gemini/antigravity/mcp_config.json`) and Claude Code (`~/.claude.json`). At session start, paste the AGV starter prompt from [the bridge's docs/starter-prompts.md](https://github.com/SohailShahM/cc-agv-bridge/blob/main/docs/starter-prompts.md) — it calls `bridge_diagnose`, drains pending work, and **calls `bridge_set_status` with your remaining Gemini quota** in the format `"quota: <input>/<output> tokens left, refreshes <time>"`, so CC can size future asks appropriately.

**Primary tools:** `bridge_ask` (you give a second opinion on CC's plan), `bridge_request_review` (you review a branch CC built), `bridge_send` (share an insight or question). `bridge_handoff` is for accepting small surgical tasks — **decline handoffs that don't fit your remaining capacity** (`bridge_decline(handoff_id, reason="quota: X tokens left, this needs ~Y")`) rather than silently failing partway through.

Post PR-comment status updates every 30 minutes if running >1 hour. If you get stuck on a Box2D native lib issue or any platform-specific build error, abort and post to QUESTIONS.md — don't burn cycles debugging environment problems.

### `claude-code-*` — Claude Code sessions
You have access to the `Agent` tool to dispatch Sonnet sub-agents for parallel work. Use this for embarrassingly-parallel work (multiple test files, multiple level definitions). Brief each sub-agent fully — they don't have your context.

## 9. Routing matrix — for the planner (Claude Opus only)

Other AIs: skip this section. The planner uses it to assign `Tool:` tags.

| Ticket type | Default tool | Notes |
|---|---|---|
| Single-screen / single-file UI work | `copilot-agent` | Tier S, autonomous from issue |
| Multi-screen UI or single-subsystem refactor | `copilot-android-studio` | User supervises |
| Multi-file refactor with clear spec | `claude-code-sonnet` | Sub-agents for parallel |
| Architecture / planning / ambiguous | `claude-code-opus` | Reserved high-leverage use |
| Content generation (text, lore, educational) | `notebooklm-then-copilot-agent` | Chain |
| Long-running autonomous loops (deps, CI flake, assets) | `antigravity` | Designed for it |
| Code work where a second opinion / fresh-eyes review adds value | implementer = `claude-code-*` or `copilot-agent`; reviewer = `antigravity` | Reviewer pulled in via [cc-agv-bridge](https://github.com/SohailShahM/cc-agv-bridge) `bridge_request_review` — capacity-light for AGV |
| Mid-flight ambiguity that needs the other agent's judgement | originator calls `bridge_ask` | Either direction; AGV's quota cost is one round-trip |
| Determinism-sensitive (see DETERMINISM.md) | `claude-code-sonnet` | Not autonomous |
| Anything touching `git config`, branch protection, secrets | `human` | Never delegate |

## 10. The single command

Pasted to any AI in any tool:

> **"Read `START_HERE.md` and work on T-XXX."**

Or if no ticket specified:

> **"Read `START_HERE.md`. Pick an unclaimed task from TASKS.md `## Todo` whose `Tool:` matches your identity. Do it."**

Identical text regardless of tool. The AI reads this doc, identifies itself, finds appropriate work, and proceeds.

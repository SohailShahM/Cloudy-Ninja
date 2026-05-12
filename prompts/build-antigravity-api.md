# Build a Claude Code ↔ Antigravity bridge

> Paste this entire document as the first message of a fresh Claude Code session in a separate worktree. The implementing session should not pollute the main Cloudy-Ninja work tree — spin up a `bridges/antigravity/` subdirectory or a sibling repo.

---

## TL;DR

Build a programmatic dispatcher so a Claude Code session can hand a research-only ticket to **Google Antigravity** (Gemini-backed autonomous agent at https://antigravity.google.com) without the user manually pasting the prompt into a browser. The goal is to remove the human-paste step from the multi-AI workflow already running in this repo.

---

## Context — read this first

You are working on the auxiliary tooling for **Cloudy Ninja**, a libGDX Kotlin 2D pixel-art platformer that uses a multi-AI orchestration pattern:

- **Claude Code (Opus)** does planning + orchestration in a long-running session.
- **Claude Code (Sonnet) sub-agents** execute mechanical work, dispatched via the `Agent` tool with `isolation: "worktree"` and `run_in_background: true`.
- **GitHub Copilot agent** handles single-file UI tickets from GitHub Issues.
- **Antigravity** handles long-running research tasks (CC0 asset hunting, climate-source compilation, marketing research, dependency audits). Output is markdown in research/marketing/art-research folders. No code.

Current Antigravity tickets (`Tool: antigravity` in [TASKS.md](../TASKS.md)) include T-049 (climate sources, in flight), T-050 (press outreach), T-051 (dep audit), T-052 (festival research), T-053 (eco-game design study). New tickets get added monthly.

**The orchestration doc that explains the routing model is** [START_HERE.md](../START_HERE.md). Read sections 1, 3, 8 to understand how identities and routing work today.

**The handoff doc that explains current operating practice is** [HANDOFF.md](../HANDOFF.md). Note specifically the "Branch-protection quirk — DO NOT trust auto-merge" section and the working-pattern around admin-merge.

---

## What this bridge replaces (the manual workflow today)

For each Antigravity ticket today:

1. Claude Code session decides T-XXX is ready to dispatch (deps satisfied, identity matches, in `## Todo`).
2. Claude Code session opens [prompts/T-XXX-antigravity.md](.) and tells the user *"go paste this into Antigravity."*
3. User opens https://antigravity.google.com in a browser, signs in, starts a new session with Gemini 3.1 Pro, points it at https://github.com/SohailShahM/Cloudy-Ninja, and pastes the prompt body.
4. Antigravity works for 5–60 min.
5. Antigravity pushes a branch / opens a PR.
6. Claude Code session (or the user) admin-merges once CI is green.

**Pain point:** steps 2–3 require the user to be present. We want to remove that — Claude Code should be able to programmatically dispatch a ticket to Antigravity from the `Agent` tool or a Bash tool call, and check its status.

---

## What we want — functional requirements

The bridge must expose **at minimum** these capabilities to a Claude Code session:

1. **`dispatch(ticket_id)`** — given a ticket ID like `T-051`:
   - Reads [prompts/T-051-antigravity.md](.) for the prompt body.
   - Submits the prompt + the GitHub repo URL to an Antigravity session.
   - Returns a session handle (an ID and/or URL Claude can store).
   - Idempotent: if a session for this ticket already exists and is `running` or `completed`, return that one instead of launching a duplicate.

2. **`status(session_handle)`** — returns one of `queued | running | completed | failed | unknown` plus a human-readable detail string.

3. **`get_result(session_handle)`** — for completed sessions, returns the GitHub branch name (and PR URL if Antigravity opened one) Antigravity produced. For failed sessions, returns the error message.

4. **`list_sessions()`** — returns all known sessions and their current statuses, so Claude Code can recover state when a new CC session starts.

5. **`cancel(session_handle)`** — abort a stuck session. Best-effort; document if Antigravity doesn't support cancellation.

**Persistence:** session handles must survive across Claude Code session boundaries. Store them somewhere durable on disk (the user's `~/.config/<this-tool>/` or similar).

**Auth:** must work with a single Google account. One-time setup is fine — Claude Code session should not need to do OAuth interactively each dispatch.

---

## Phase 0 — research before you build (≤30 min, mandatory)

**Do not write any implementation code until you have answers to these.** Report back to the user with findings before scaffolding anything.

1. **Does Antigravity have an official or undocumented API?** Check:
   - Google Cloud's public API directories.
   - The Network panel in Antigravity itself (the user can capture an HAR while creating a session manually).
   - Google's documentation for Gemini Agents.
   - Whether Antigravity uses Vertex AI Agent Builder under the hood (which *does* have an API).
2. **Authentication surface** — Google OAuth 2.0? Application Default Credentials? Service account? Cookie + CSRF token?
3. **Rate limits / quotas / cost** — per session, per day, per account. Is there a billable cost?
4. **Result delivery** — does Antigravity push to a webhook, expose a polling endpoint, or only show results in the UI?
5. **If no API exists at all**, decide between:
   - **Browser automation** (Playwright/Selenium with persisted Google session cookies). Brittle, anti-bot risk, but doable.
   - **Chrome extension** — runs in the user's logged-in browser; CC posts to a local extension endpoint. More robust against anti-bot but requires manual extension install.
   - **Recommend deferring** until Google ships an API. Tell the user honestly.

Report findings to the user and **let them choose the path** before continuing.

---

## Phase 1 — MVP

Whichever path Phase 0 picks, the MVP must:

1. Support `dispatch` + `status` + `get_result` for the 5 currently-queued Antigravity tickets (T-050, T-051, T-052, T-053 + future ones following the same shape).
2. Persist session state in a single JSON file on disk (no database needed).
3. Surface itself to Claude Code as either:
   - **An MCP server** with tools `antigravity_dispatch`, `antigravity_status`, `antigravity_get_result`, `antigravity_list_sessions`, `antigravity_cancel`. *(Strongly preferred — matches the rest of the CC ecosystem.)*
   - Or a CLI (`antigravity-bridge dispatch T-051`) that CC can call via the Bash tool.
4. Ship with one-page setup docs: prerequisites, auth flow, how to run it, how to point a Claude Code session at it.

**Out of scope for MVP:** webhooks, multi-user, retries, observability beyond `print()` debug, the `cancel` operation (stub it).

---

## Phase 2 — nice to have

- `cancel(session_handle)` if Antigravity supports it.
- A background watcher that posts to a local file when a session completes, so CC can notice without polling.
- Cost tracking per session.
- A diff-preview function that returns Antigravity's branch diff inline so CC can review before opening the PR.

---

## Out of scope (do not build these)

- A web UI for the bridge.
- Multi-account support.
- A general-purpose Google Agents wrapper — keep this Antigravity-specific.
- Replacing the existing PR-opens-and-CI-runs flow on GitHub. The bridge ends when Antigravity pushes a branch; standard GitHub flow takes over.
- Any feature that requires writing Cloudy-Ninja code. This tool is orchestration only.

---

## Constraints

- **Host machine:** Windows 11 Pro, PowerShell + Bash both available. `gh` CLI 2.92.0 installed and authenticated as `SohailShahM`.
- **Language choice:** prefer Python (3.11+) or Node.js (LTS). The user is fluent in both. Avoid Go/Rust unless there is a hard reason.
- **Persistent state:** store under `%APPDATA%\antigravity-bridge\` on Windows; document the corresponding `~/.config/antigravity-bridge/` path for macOS/Linux portability.
- **No secrets in git.** Auth tokens go in a gitignored credentials file. The repo this lives in (TBD with user) should have `.env.example` only.
- **MCP-mode:** if you build it as an MCP server, target the spec used by Claude Code today (read `~/.claude/mcp_servers.json` for examples if you can).
- **Don't fight Google's TOS.** If Phase 0 reveals that automation violates the Antigravity terms, surface this to the user *immediately* — don't proceed without explicit go-ahead.

---

## Deliverables

1. **Phase 0 findings doc** — markdown, ≤2 pages, in the bridge repo. Includes recommendation + tradeoffs.
2. **Working MVP** (CLI or MCP per Phase 0 decision).
3. **Setup doc** for the bridge — how to install, how to authenticate once, how to wire it into Claude Code (`claude mcp add ...` example or equivalent).
4. **Smoke test** — a script that dispatches a synthetic 30-second "hello world" prompt and verifies the full lifecycle (`dispatch → status → get_result`).

---

## How the implementing session should proceed

1. Acknowledge the prompt and confirm the host environment (Python or Node, etc.).
2. **Do Phase 0 first.** Report findings before writing any non-trivial code.
3. Pause and get user buy-in on the chosen path.
4. Implement Phase 1 MVP. Ship the smoke test alongside.
5. Document setup.
6. Hand back to the user with one paragraph on what works, what's stubbed, and what should land in Phase 2.

---

## One question for the user before you start

If Phase 0 reveals there is *no* programmatic API and *no* viable automation path, would you rather:

- **(A)** Build a thinner alternative — e.g. a one-click PowerShell shortcut that opens Antigravity in a browser with the right prompt pre-filled in clipboard? (Removes 80% of the manual friction without violating any TOS.)
- **(B)** Defer the project and wait for Google to ship an Antigravity API?
- **(C)** Press ahead with browser automation anyway?

Ask the user before assuming.

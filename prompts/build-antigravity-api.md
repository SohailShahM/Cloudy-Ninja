# Build a Claude Code ↔ Antigravity collaboration bridge

> Paste this entire document as the first message of a fresh Claude Code session in a separate worktree (a sibling repo or a `bridges/antigravity/` subdirectory — do not pollute the Cloudy-Ninja code tree).

---

## TL;DR

Build a **bidirectional collaboration channel** between **Claude Code (Anthropic Claude)** and **Google Antigravity (Gemini)** so the two agents can work together as peers on the same repo. Either side must be able to: send messages, ask the other a question and block on the answer, hand off a task with context, request review of a branch, and read the other's intermediate artifacts. The goal is **peer-to-peer engineering collaboration**, not a one-way job queue.

---

## Context — important framing correction

Cloudy Ninja currently routes Antigravity only to "research-only markdown tickets" (T-046a/b, T-047, T-048, T-049 — all just produced files in `research/` or `art-research/`). **That was a conservative warm-up, not a hard ceiling.** It came from the orchestrator's bias, not Antigravity's actual capability surface.

Antigravity is positioned by Google as a full **Agentic Development Platform**:

- Gemini 3.x backbone (Gemini 3.1 Pro at minimum tier).
- First-class shell, file editor, terminal panels, browser preview.
- Multi-step planning + artifact tracking.
- Subagent spawning.
- Native MCP (Model Context Protocol) support.
- Native A2A (Agent-to-Agent) protocol support per Google's interop spec (verify in Phase 0).
- Long-horizon autonomous loops with human-in-the-loop checkpoints.
- Opens PRs, runs CI, debugs, refactors — i.e. it does what Claude Code does.

The bridge being built here should treat Antigravity as a **peer agent**, not a worker. The right mental model is *two senior engineers DMing while working on the same repo*, not *a dispatcher feeding a queue*.

The orchestration doc that explains the routing model is [START_HERE.md](../START_HERE.md). The handoff doc that explains current operating practice is [HANDOFF.md](../HANDOFF.md). Read both before writing code, but **expect to update them** once the bridge ships — the current docs encode the old "Antigravity is for research" framing and will be wrong by the time you're done.

---

## What we want — functional requirements

The bridge must expose, **to both sides**, primitives in roughly these categories. Either agent can initiate any of these — it's not a master/worker relationship.

### Messaging
1. **`send_message(to, text, ref=None)`** — post a message to the other agent. `ref` optionally links to a task / branch / PR / artifact.
2. **`receive_messages(since=None)`** — return all messages from the other side since a cursor. Used for polling-based clients.
3. **`subscribe()`** — for push-capable clients (MCP `notifications`, webhooks, etc.): get notified when a new message arrives without polling.

### Question / answer (blocking)
4. **`ask(to, question, timeout)`** — block until the other side answers (or timeout). Returns the answer text. Either side can call this — Antigravity should be able to ask Claude Code "should this be a P0 or P1?" mid-run, and Claude Code should be able to ask Antigravity "what's blocking you?" without manually paging through its UI.
5. **`answer(question_id, text)`** — respond to an outstanding `ask`.

### Task handoff
6. **`hand_off(to, task_spec)`** — transfer ownership of a unit of work. `task_spec` includes: ticket reference, branch (if started), what's done, what's left, blocking issues, attached artifacts. Returns a handoff handle the originator can later check on.
7. **`accept(handoff_id) | decline(handoff_id, reason)`** — receiver's response.

### Review / artifact sharing
8. **`request_review(branch_or_pr_url, focus_areas)`** — ask the other side to review a branch or PR. Returns a review handle.
9. **`submit_review(review_handle, verdict, comments)`** — verdict ∈ `{approve, request_changes, comment}` plus inline comments.
10. **`list_artifacts(filter)`** — see what the other agent has produced recently (branches, PRs, files, plans). The implementing session should think about whether to surface these as virtual files, MCP resources, or plain list-of-URLs.

### Shared scratch state
11. **`scratch.get(key) / scratch.set(key, value)`** — a tiny shared KV store, ideally backed by a single JSON file in a known location, for things both agents need to coordinate on (e.g. "current sprint focus", "do not touch this branch", "blocked on user input").

### Lifecycle
12. **`status()`** — what is the other agent currently doing, in one line.
13. **`interrupt(task_id, reason)`** — politely pause the other side's current task. Best-effort.

**Persistence:** all of the above must survive across CC session boundaries and AGV session boundaries. Both sides should be able to walk in cold and read the conversation history + outstanding asks + open handoffs.

---

## Phase 0 — research before you build (≤45 min, mandatory)

**Do not write any implementation code until you have answers to all of these.** Report back to the user with findings before scaffolding anything. The user will likely have opinions.

1. **Antigravity's actual surface — what can it natively do?**
   - Read Google's Antigravity docs (https://antigravity.google.com, plus Vertex AI Agent Builder if relevant). Confirm: shell access, file editing, MCP support, A2A support, sub-agent spawning, webhook callouts, scheduled tasks.
   - Capture an HAR while the user creates a session manually (network panel). Look for: REST endpoints, websocket frames, auth tokens, session IDs.

2. **Does Antigravity expose an API at all?**
   - Official documented API → ideal path.
   - Undocumented internal API discovered via HAR → workable but TOS-sensitive.
   - Neither → fallback options (see below).

3. **Does Antigravity support A2A (Google's Agent-to-Agent protocol)?**
   - If yes, the bridge becomes "ship an A2A endpoint from Claude Code's side and we're done." This is the dream path.
   - Check: https://github.com/google/A2A — protocol spec, client libs, identity model.

4. **Does Antigravity support MCP servers we can run locally?**
   - If yes, we can expose a Claude Code "inbox" MCP server that Antigravity connects to. CC writes messages; AGV's MCP client reads them. And vice versa.

5. **Authentication surface** — Google OAuth 2.0? ADC? Service account? Cookie + CSRF? Whatever it is, it must survive across machine reboots and run unattended.

6. **Rate limits / quota / cost** — per session, per day, per account. Surface to the user — if each AGV session costs $0.50 the bridge needs to be careful about chatty messaging.

7. **Result delivery** — does Antigravity push to a webhook, expose a polling endpoint, support long-polling, or only display in the UI?

8. **If none of the above paths exist**, fall back options in user-preference order:
   - **(A)** A shared GitHub repo as the "inbox": both agents commit messages as files. Slow but works with zero new infra.
   - **(B)** A shared local file watcher: messages written to `bridge-state/inbox/` polled by both. Fast but requires AGV to have local file access on the user's machine.
   - **(C)** Browser automation against antigravity.google.com (Playwright + persisted Google session). Brittle, anti-bot risk.
   - **(D)** Recommend deferring until Google ships A2A or an official API.

**Report Phase 0 findings to the user. Wait for go-ahead before continuing.**

---

## Phase 1 — MVP

Whichever path Phase 0 picks, the MVP must support **at minimum**:

1. **`send_message` + `receive_messages` working both directions**, end-to-end, on a real Antigravity session.
2. **`ask` + `answer`** with blocking semantics on the asking side (≤5 min default timeout, configurable).
3. **`hand_off` + `accept` / `decline`** with at least the ticket reference, branch name, and notes fields populated.
4. **A smoke-test script** that:
   - Claude Code sends "ping" to Antigravity.
   - Antigravity replies "pong".
   - Claude Code asks "what's 7 * 6?" — Antigravity answers "42" — script asserts equality.
   - Claude Code hands off a tiny task (e.g. "create file `marketing/bridge-test.md` with one line"), Antigravity accepts, completes, branches, and `submit_review`s. Claude Code approves.
   - All of the above logged to a single session transcript.
5. **Surface to Claude Code as an MCP server** (preferred — matches the rest of the CC ecosystem) with tools `bridge_send`, `bridge_receive`, `bridge_ask`, `bridge_answer`, `bridge_handoff`, `bridge_accept`, `bridge_review_request`, `bridge_review_submit`, `bridge_status`. **Surface to Antigravity** as either an A2A endpoint or a parallel MCP server it can connect to.
6. **Setup doc** — prerequisites, one-time auth, how to wire it into both Claude Code and Antigravity.

**Out of scope for MVP:** webhooks, observability beyond `print()`, the `interrupt` call (stub it), `scratch` shared KV (defer to Phase 2), multi-conversation isolation (assume one shared conversation thread).

---

## Phase 2 — nice to have

- `scratch.get / scratch.set` shared KV.
- `interrupt` if Antigravity supports it.
- `subscribe` push notifications (vs. polling).
- A 30-second-resolution event log so the human can later replay a full collaboration session.
- Cost tracking per session on the Antigravity side.
- Web preview pane (`view artifact in browser`) for branches under review — saves the human a `git checkout`.

---

## Out of scope (do not build these)

- Replacing either tool's existing UI.
- Multi-account support — single Google account, single Anthropic account.
- A general-purpose multi-agent framework. Keep it Antigravity ↔ Claude Code specific.
- Trying to host Antigravity yourself — it's Google's service, treat it as a network dependency.
- Writing any Cloudy-Ninja game code. This bridge is orchestration infrastructure only.

---

## Constraints

- **Host:** Windows 11 Pro. PowerShell + Bash both available. `gh` CLI 2.92.0 authenticated as `SohailShahM`. Python 3.11+ and Node.js LTS both installable.
- **Language:** prefer Python or Node. Reason for choice goes in the README.
- **State location:** `%APPDATA%\cc-agv-bridge\` on Windows; document `~/.config/cc-agv-bridge/` for portability.
- **No secrets in git.** Auth tokens go in a gitignored credentials file. `.env.example` committed.
- **TOS-aware.** If Phase 0 reveals that any chosen mechanism violates Antigravity's or Google's TOS, surface to the user *immediately* with the relevant clause cited. Do not "ship and find out."
- **Don't fight anti-bot.** If browser automation is the only path, document the risk plainly and let the user pick.

---

## Deliverables

1. **Phase 0 findings doc** — markdown, ≤3 pages, in the bridge repo. Must include: what Antigravity natively supports, A2A status, recommendation + 2 alternative paths, TOS notes.
2. **Working MVP** with the 6 capabilities listed in Phase 1.
3. **Smoke test** — runs end-to-end ping/pong/task-handoff with one command.
4. **Two setup snippets** — one for the Claude Code side (`claude mcp add ...`), one for the Antigravity side (whatever the connect-to-MCP-or-A2A-endpoint flow looks like there).
5. **A handoff to the user** with one paragraph each: what works, what's stubbed, what should land in Phase 2, what would unblock making the bridge significantly better (e.g. "Google ships official A2A in 4 months — wait for that").

---

## How the implementing session should proceed

1. Acknowledge the prompt + confirm host environment.
2. **Do Phase 0 first.** Report findings before any non-trivial code.
3. Pause and get user buy-in on the chosen path.
4. Implement Phase 1 MVP + ship the smoke test.
5. Write the two setup snippets.
6. Hand back to the user with the wrap-up paragraph.

---

## Questions to ask the user up front

Before writing the Phase 0 doc, get answers (or explicit "use your judgement") on:

1. **Cost cap** — what's the upper bound on Antigravity usage per day during development? Per month after?
2. **Latency expectation** — is "messages arrive within 5 seconds" enough, or is it OK if `ask` takes 30 seconds to round-trip?
3. **TOS posture** — does the user prefer hard "documented APIs only" or "undocumented internal APIs OK if they work"?
4. **Conversation model** — single long-running conversation thread, or one thread per ticket?
5. **If Phase 0 reveals no viable path**, would the user rather:
   - **(A)** Defer the project (wait for Google's A2A or an official API).
   - **(B)** Build the slim fallback (shared-GitHub-repo as inbox — slow but unblocks the workflow).
   - **(C)** Press ahead with browser automation despite the brittleness.

Do not assume any of these. Ask, then build.

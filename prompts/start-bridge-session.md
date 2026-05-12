# Start-bridge-session — paste this into a fresh Claude Code session

> Wrapper prompt for kicking off the Claude Code ↔ Antigravity bridge build in a **separate** directory. Pairs with [build-antigravity-api.md](build-antigravity-api.md) — that file is the actual spec; this file is the launcher.

---

You're building a Claude Code ↔ Google Antigravity collaboration bridge.

## Working directory — set this up first
Create a fresh git repo at `C:\Users\Radmin\Documents\GitHub\cc-agv-bridge\` and `cd` into it. Run `git init` once empty. All work happens there.

**Do NOT touch `C:\Users\Radmin\Documents\GitHub\Cloudy-Ninja\`.** That repo is the *consumer* of this bridge — it stays untouched. The only thing you may read from it is the spec doc and the reference docs (via the shallow clone below).

## Reference material — read-only
Inside `cc-agv-bridge/`, shallow-clone the Cloudy-Ninja repo for reference:

    git clone --depth 1 https://github.com/SohailShahM/Cloudy-Ninja.git reference/cloudy-ninja

Add `reference/` to `.gitignore` in your new repo. Never write into `reference/`.

## Spec to follow
Read this end-to-end before writing any code:

    reference/cloudy-ninja/prompts/build-antigravity-api.md

That doc covers: TL;DR, 13 functional requirements (messaging / blocking Q&A / task handoff / review / shared scratch / lifecycle), **mandatory Phase 0 research** (do not skip), Phase 1 MVP scope, constraints, deliverables, and **5 questions to ask me up front**.

You'll also want to skim `reference/cloudy-ninja/START_HERE.md` and `reference/cloudy-ninja/HANDOFF.md` to understand the multi-AI orchestration the bridge plugs into — the spec notes that those docs encode the *old* "Antigravity is for research" framing and should be updated as part of this work.

## What to do, in order
1. Create the working dir + shallow clone above. `git init` your new repo. Add `reference/` to `.gitignore`.
2. Read the spec end-to-end.
3. **Ask me the 5 up-front questions** from the spec's "Questions to ask the user up front" section (cost cap, latency, TOS posture, conversation model, fallback preference).
4. Do **Phase 0 research** (≤45 min). Report findings — does Antigravity support A2A? MCP servers it can connect to? Native API? Auth surface? — before scaffolding anything.
5. Pause for my buy-in on the chosen path.
6. Implement Phase 1 MVP + the smoke test (ping/pong + ask/answer + handoff + review).
7. Hand back per the spec's "Deliverables" section.

**Hard rules:** Don't skip Phase 0. Don't start coding before the 5 questions are answered. Don't write anything outside the `cc-agv-bridge/` directory. Don't violate Antigravity TOS — if the only path requires it, surface that to me and wait.

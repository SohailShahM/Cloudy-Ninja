[![CI](https://github.com/SohailShahM/Cloudy-Ninja/actions/workflows/ci.yml/badge.svg)](https://github.com/SohailShahM/Cloudy-Ninja/actions/workflows/ci.yml)
[![AI smoke test](https://github.com/SohailShahM/Cloudy-Ninja/actions/workflows/ai-smoke.yml/badge.svg)](https://github.com/SohailShahM/Cloudy-Ninja/actions/workflows/ai-smoke.yml)
[![License: Proprietary](https://img.shields.io/badge/license-Proprietary-red)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![libGDX](https://img.shields.io/badge/libGDX-1.14.0-E74C3C)](https://libgdx.com/)

# Cloudy Ninja

A 2D pixel-art platformer about restoring corrupted ecosystems. Three switchable characters with water-cycle-themed abilities (Ebo / Laya / Zephyr), 8 levels including a Storm Sentinel boss, ambient music, save slots, 4K/HiDPI scaling, accessibility-first design (color-blind palette, reduced motion, key rebinding, assist mode), and a Cloud Atlas of climate-science snapshots collected during play.

Built on [libGDX](https://libgdx.com) (Kotlin) with [Box2D](https://box2d.org/) physics and [VisUI](https://github.com/kotcrab/vis-ui) for UI. Currently in active development; targeting a public alpha on itch.io.

## How it was built

Cloudy Ninja is developed using a multi-AI orchestration workflow, with each tool routed to the kind of work it does best:

- **Claude Code (Opus)** acts as the planner. It owns architecture decisions, ticket triage, scope-setting, and high-leverage reasoning — never mechanical work.
- **Claude Code Sonnet sub-agents** are the parallel workers. Opus dispatches them via the `Agent` tool, one ticket per sub-agent, in worktree isolation. Batches of two to four run concurrently when their file scopes are disjoint.
- **GitHub Copilot coding agent** handles tier-S, single-file fixes that fit cleanly in a GitHub Issue (the autonomous, github.com-hosted variant — not the IDE plugin).
- **Google Antigravity** (Gemini-3-backed peer platform) handles long-running research, fresh-eyes review, and bulk mechanical work. It coordinates with Claude Code over the [cc-agv-bridge](https://github.com/SohailShahM/cc-agv-bridge) MCP server, an agent-to-agent channel with `bridge_ask`, `bridge_request_review`, and `bridge_handoff` primitives.

Routing is enforced by a `Tool:` tag on every ticket in [TASKS.md](TASKS.md) (`claude-code-opus`, `claude-code-sonnet`, `claude-code-sub-agent`, `copilot-agent`, `antigravity`, `notebooklm`, `human`). An agent that does not match the tag may not claim the ticket — the matrix is the primary defense against tool-task mismatch, the most common failure mode of multi-AI systems.

Two dispatch patterns keep the throughput honest. **Parallel sub-agents** run only when file scopes do not overlap; the planner audits ticket `Files:` lists up front and serializes anything that touches contended files (settings UI, the main level renderer, the i18n string table). **File-conflict gating** at the planner layer means sub-agents never need to merge each other's work — every parallel batch lands as independent PRs.

## License

**Cloudy Ninja is proprietary — Copyright © 2026 Sohail Shah / MashxLabz. All rights reserved.**

Source is publicly visible for transparency, code review, and educational reference. You may view, clone for personal study, and submit Pull Requests, but **redistribution, derivative works, and commercial use are not permitted** without prior written permission.

See **[LICENSE](LICENSE)** for the full terms.

Third-party assets bundled in this repository (notably the Kenney `pixel-platformer` tile pack under CC0) retain their original licenses regardless of the above. See **[NOTICE.md](NOTICE.md)** for the full list.

## For contributors + AI agents

This repo uses a multi-AI orchestration workflow (Claude Code, GitHub Copilot, Google Antigravity, NotebookLM). Coordination is via a task board.

- **[START_HERE.md](START_HERE.md)** — entry point for any AI agent: identity table, capability gates, claim protocol.
- **[HANDOFF.md](HANDOFF.md)** — session-to-session continuity notes; read this first if picking up where a previous session left off.
- **[AGENTS.md](AGENTS.md)** — architecture and module layout.
- **[TASKS.md](TASKS.md)** — work queue.
- **[LEARNINGS.md](LEARNINGS.md)** — gotchas previous sessions found.
- **[GAME_PLAN.md](GAME_PLAN.md)** — vision + scope.

## Commercial / licensing inquiries

Contact the copyright holder via the GitHub profile linked from this repository: https://github.com/SohailShahM

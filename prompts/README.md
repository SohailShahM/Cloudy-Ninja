# prompts/ — paste-ready launch prompts for each ticket

Every Sprint D ticket in `TASKS.md` has a `Tool:` tag. This directory contains a paste-ready prompt for each non-trivial ticket — open the matching file, copy its body, paste into the right tool, walk away.

## How to use

1. Pick a ticket from `TASKS.md ## Todo` whose dependencies are `Done`
2. Open the matching prompt file in this directory: `prompts/T-XXX-<tool>.md`
3. Copy the **prompt body** (the fenced section)
4. Paste into the tool named in the filename
5. The AI follows `START_HERE.md`, claims the ticket, opens a PR
6. Auto-merge fires when CI passes

## File naming

`T-XXX-<tool>.md` where `<tool>` matches the identity tag in `START_HERE.md` §1:
- `T-035-copilot-agent.md` — paste body into a GitHub Issue, assign `@Copilot`
- `T-037-claude-code.md` — paste body into a fresh Claude Code session terminal
- `T-046a-antigravity.md` — paste body into Antigravity at antigravity.google.com
- `T-045-notebooklm.md` — paste body into a new NotebookLM notebook chat (after uploading sources)

## When a ticket isn't here

Some tickets are gated by a human-step-first (e.g. T-031 needs you to pick tilesets first, T-046 needs you to pick art direction). Those prompts get added once the human step is done.

## Adding a new prompt

When the planner (Claude Code Opus) adds a new ticket to TASKS.md, it also adds the matching `prompts/T-XXX-<tool>.md` file in the same commit. Keep them in sync.

# Contributing to Cloudy Ninja

Thanks for taking the time to look at the source. This document explains how outside
contributors can usefully engage with the project — issues, pull requests, and the
ground rules that apply.

If you are an **AI agent** working from a task in `TASKS.md`, read
[START_HERE.md](../START_HERE.md) instead — this file is for human contributors who
have arrived from the public GitHub repo.

---

## 1. License posture — please read first

**Cloudy Ninja is proprietary.** The source is publicly visible for transparency,
code review, and educational reference, but it is **not** released under an
open-source license. See [LICENSE](../LICENSE) for the full terms.

What this means for contributors in practice:

- You may **view, clone for personal study, and submit Pull Requests** back to this
  repository. Forks must exist only to support a PR back here — they cannot be
  published as standalone products or releases. (LICENSE §"PERMITTED".)
- **By opening a Pull Request, you grant the copyright holder a perpetual,
  worldwide, non-exclusive, royalty-free, irrevocable license to use your
  contribution as part of the Work, under the LICENSE terms.** This is the
  "CONTRIBUTIONS" clause of the LICENSE — opening a PR is your agreement to it.
  There is no separate CLA to sign; the LICENSE handles it.
- Redistribution, derivative works, and commercial use require prior written
  permission from the copyright holder. The LICENSE is the source of truth on
  what is and isn't allowed.
- Third-party assets bundled in this repo (notably the Kenney `pixel-platformer`
  tiles under CC0) retain their original licenses regardless. See
  [NOTICE.md](../NOTICE.md) for the complete list and attribution requirements.

If any of the above is a problem for you, please **don't open a PR** — open a
GitHub Discussion instead and we'll talk first.

---

## 2. Opening an issue

Use the [issue templates](../.github/ISSUE_TEMPLATE/) — blank issues are disabled
on purpose so we get the information we need up front:

- **[Bug report](../.github/ISSUE_TEMPLATE/bug-report.yml)** — something doesn't
  work. Include build version (shown bottom-right on the main menu), platform,
  active character, level ID, and reproduction steps.
- **[Feature request](../.github/ISSUE_TEMPLATE/feature-request.yml)** — a new
  idea. Please read [GAME_PLAN.md](../GAME_PLAN.md) first; the scope is
  intentionally narrow.
- **[Accessibility issue](../.github/ISSUE_TEMPLATE/accessibility-issue.yml)** —
  anything that makes the game harder to use. We treat these as priority bugs.

For questions, half-formed ideas, or "is this a bug or am I holding it wrong"
discussions, use [GitHub Discussions](https://github.com/SohailShahM/Cloudy-Ninja/discussions)
instead.

---

## 3. Opening a Pull Request

For anything beyond a typo fix, **open an issue first** so we can confirm scope
before you spend time on a patch. Drive-by refactors of subsystems you haven't
discussed will almost certainly be closed.

### Branch naming

Branches against this repo follow `<role>/<short-slug>` — e.g. `human/fix-laya-dash`,
`claude/T-080-issue-templates`. If you've claimed a ticket from `TASKS.md`, include
the ticket ID: `human/T-XXX-short-slug`.

### Before you push

- `./gradlew :core:compileKotlin` passes
- `./gradlew :core:test` passes
- Manually verified on desktop: `./gradlew lwjgl3:run`

### CI requirements

CI runs on every PR via [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
and must pass before merge:

- Compile `:core` (Kotlin, JDK 17)
- Run `:core:test` (Kotest suites)
- `android:lint` when the PR touches `android/`, `core/`, or build files

There is no separate `CODE_OF_CONDUCT.md` file. The rule is simple: be civil,
assume good faith, focus on the code. Personal attacks, harassment, or
discriminatory language will get a PR closed and the author blocked. The
maintainer's call is final.

### PR template

Fill out the [PR template](../.github/PULL_REQUEST_TEMPLATE.md) — Summary,
Closes (issue/ticket ID), and Test plan checklist. If your PR was authored with
AI assistance, keep the trailer; if it was entirely human, delete it.

---

## 4. Style notes

- **Language:** Kotlin (JVM target 17). The project does **not** currently run
  ktlint or detekt in CI, so there's no auto-formatter to please — just match the
  surrounding code's style. Four-space indent, no wildcard imports, trailing
  commas where the file already uses them.
- **Tests first.** New gameplay logic should land with a Kotest spec under
  `core/src/test/kotlin/`. See existing specs (e.g. `SaveManagerTest.kt`,
  `PlayerControllerTest.kt`) for the in-house style — `StringSpec` or
  `BehaviorSpec`, no MockK unless you genuinely need it.
- **Determinism matters** in the parts of the codebase that touch save data,
  level state, or physics tuning. See [DETERMINISM.md](../DETERMINISM.md) before
  changing anything in those subsystems.
- **Module boundaries:** put shared gameplay in `core/`. Platform-specific code
  (window creation, native loaders) goes in `lwjgl3/` or `android/`. See
  [AGENTS.md](../AGENTS.md) for the full module + package map.
- **Constants:** tuning values (physics, jump windows, collision bits) live in
  `core/src/main/kotlin/com/sohai/platformer/Constants.kt`. Don't scatter magic
  numbers across call sites.

---

## 5. What NOT to PR

These will be closed without merge. Open an issue or Discussion first instead:

- **Art, audio, or font assets you don't have a clear license for.** This repo
  is careful about asset provenance (see [NOTICE.md](../NOTICE.md) — currently
  Kenney `pixel-platformer` under CC0 is the only bundled third-party art).
  PRs that drop in sprites/music/fonts without a documented permissive license
  (CC0 / CC-BY / Apache-2.0 / similar) cannot be accepted. If you want to
  contribute original art, open an issue first so we can talk about scope and
  attribution.
- **Save-data schema changes without prior discussion.** `SaveManager` and
  `SaveMigrations` in `core/src/main/kotlin/com/sohai/platformer/persist/` are
  versioned for a reason; changing the schema mid-flight breaks every existing
  save. Open an issue describing the migration path before writing code.
- **New runtime dependencies** (anything added to `core/build.gradle` or the
  Gradle version catalog). Each dependency adds maintenance and license
  surface. Open an issue with the justification first.
- **File or package renames / moves.** Disruptive to in-flight work across
  multiple parallel agents. Discuss in an issue first.
- **"Cleanup" PRs that touch dozens of files** to apply your preferred style.
  See the Style note above — if the file already has a style, match it.
- **Disabling tests** or bypassing git hooks with `--no-verify`. If a test is
  flaky, file a bug; don't silence it.
- **Anything modifying `git config`, branch protection, repo secrets, or
  workflow permissions.** These are operator-only.

---

## 6. Licensing inquiries

Commercial licensing, distribution rights, or any use not explicitly permitted
by the [LICENSE](../LICENSE): contact the copyright holder via the GitHub
profile linked from this repository — https://github.com/SohailShahM.

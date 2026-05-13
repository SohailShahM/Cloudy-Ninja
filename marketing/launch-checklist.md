# Cloudy Ninja — alpha launch checklist

> Concrete, actionable pre-launch task list for the itch.io alpha release. Every
> item references a real ticket or a real file in the repo. No timelines invented —
> ordering is dependency-driven, not date-driven. Tick boxes top-to-bottom on
> launch day; the order is rough-but-defensible (legal blockers first, then build,
> then marketing surfaces, then community channels, then day-of operations).
>
> **Author:** claude-code-sub-agent · **Compiled:** 2026-05-13 · **Ticket:** T-153
>
> Cross-references:
> - `marketing/itch-page-draft.md` (T-124) — the page copy this checklist verifies got applied
> - `marketing/steam-tags-research.md` (T-075) — tag list this checklist verifies got pasted
> - `marketing/presskit/` (T-077) — screenshots + press materials this checklist references
> - `HANDOFF.md` — alpha-blocker context (T-126 Calibri, PR #68 / #85 gates)
> - `research/asset-attribution-audit.md` (T-125) — the audit that surfaced T-126
> - `LICENSE`, `NOTICE.md` — proprietary repo license + third-party attributions

---

## 1. Legal / license

These are the **hard blockers**. Do not flip the itch.io page to public until every
box in this section is checked.

- [ ] **T-126 — Calibri Regular font replaced** in `assets/fonts/main.ttf`.
  - The repo is public and Calibri is Microsoft-proprietary → keeping it bundled is a
    redistribution violation. T-126 is marked **ALPHA-BLOCKING LEGAL** in `TASKS.md`
    and `autonomous-eligible: no` (human picks the replacement font and visually
    approves regression).
  - Recommended replacement per `research/asset-attribution-audit.md` §HIGH-1: **Inter**
    (SIL OFL 1.1). Alt: Atkinson Hyperlegible (accessibility-first).
  - Verify legibility across MainMenu, SettingsScreen, AchievementsScreen,
    CreditsScreen, StatsScreen, VictoryScreen, GameScreen HUD, CloudAtlasScreen at
    `FontManager` sizes 11, 14, 22.
- [ ] **`NOTICE.md` updated** with the new font's SIL OFL 1.1 attribution block,
  matching the existing Kenney CC0 block format.
- [ ] **`research/asset-attribution-audit.md` MEDIUM / LOW items reviewed.** Two MEDIUMs
  and three LOWs flagged by T-125 — confirm each is either resolved or accepted-as-is
  for alpha (not silently ignored).
- [ ] **`LICENSE` final read.** Proprietary "all rights reserved" terms still match the
  intent for alpha (source-visible, redistribution + derivatives + commercial use
  require permission). If anything in `LICENSE` no longer matches the launch posture,
  edit before publishing.
- [ ] **`NOTICE.md` Kenney attribution block is accurate.** Confirm
  `assets/tilesets/kenney_pixel_platformer/` is still the bundled subset described in
  NOTICE; if any tile files were added or removed during alpha polish, NOTICE needs
  to reflect that.
- [ ] **No other proprietary assets in the bundle.** Re-grep `assets/` for `.ttf`,
  `.otf`, `.wav`, `.mp3`, `.png` files added since T-125's audit (2026-05-13) and
  verify each new asset has a known license logged in NOTICE.md.
- [ ] **GitHub repo metadata matches LICENSE.** The repo's GitHub "About" sidebar
  license field auto-detects from `LICENSE`. Confirm it reads as proprietary / "Other"
  and not as a permissive OSS license. (Public + proprietary is intentional per
  HANDOFF.md "Repo state".)

---

## 2. Build

Verify the build artifact that ships to itch.io is actually playable, and that the
deploy pipeline reaches itch.io successfully.

- [ ] **Desktop JAR built via `./gradlew :lwjgl3:dist`** locally with the documented
  JDK (`C:\Program Files\Android\Android Studio\jbr` per `HANDOFF.md`).
  - Note: `:lwjgl3:dist` is an alias for `:lwjgl3:jar` (line 197 of
    `lwjgl3/build.gradle`) per HANDOFF source-side quirk #7. The module is `:lwjgl3`,
    not `:desktop`.
- [ ] **Built JAR launches and runs through Level 1** without an uncaught exception.
  Verify no crash files written to `<userHome>/.cloudy-ninja/crashes/` during the
  smoke run (T-115's crash reporter — empty dir = good).
- [ ] **Build version label correct.** Check `Constants.BUILD_VERSION` and
  `Constants.BUILD_DATE` (T-100) match the launch version/date as shown bottom-right
  on MainMenu.
- [ ] **All 9 required CI checks green on `main`** (1 lint + 8 smoke per `HANDOFF.md`
  "Repo / environment"). No PR is mergeable into the launch SHA without all 9 green.
- [ ] **`.github/workflows/itch-deploy.yml` workflow exists** (T-114) and the
  `workflow_dispatch` trigger is configured for `channel` + `version-tag` inputs.
- [ ] **`ITCH_API_KEY` secret set** at repo level. The user must run
  `gh secret set ITCH_API_KEY` manually — T-114's PR explicitly told the sub-agent
  NOT to set this. Verify with `gh secret list --repo SohailShahM/Cloudy-Ninja`.
- [ ] **itch-deploy workflow dry-run** — trigger `workflow_dispatch` with a test
  `version-tag` (e.g. `alpha-dryrun-<sha>`) and confirm butler push succeeds. The
  itch.io page should show the new build version after the run completes. If using
  a real channel name, push to a draft/restricted channel first; if itch allows,
  use a `desktop-test` channel and delete after.
- [ ] **Android APK build** (if shipping Android at alpha) — separately verified, with
  its own platform-tagged upload on the itch.io page. See `marketing/itch-page-draft.md`
  §5 for the Android system-requirements block.
- [ ] **Press kit ZIP built / uploaded.** Either upload the contents of
  `marketing/presskit/` as a ZIP downloadable extra on itch.io, or link the GitHub
  source path in the page footer per `marketing/itch-page-draft.md` §10. The
  presskit screenshot files (`screenshot-01.png` through `screenshot-06.png`) and
  `logo.png` should all be present and unchanged from T-077's scaffold.

---

## 3. Marketing surfaces

The itch.io page itself + everywhere a player might land before reaching it.

- [ ] **`marketing/itch-page-draft.md` (T-124) pasted into itch.io CMS.** Every
  section in that draft has a corresponding itch.io field — work through it top-to-
  bottom. Use the page's own "Pre-flight checklist before hitting Publish" (§11 of
  the draft) as the inner checklist for this item.
- [ ] **Title:** `Cloudy Ninja`.
- [ ] **Short description (160-char field):** pasted from §1 of the draft. Verify
  character count after paste (target: 147/160).
- [ ] **Long description:** pasted from §2. Verify markdown renders (bold, lists, no
  broken `[text](url)` links).
- [ ] **Genre dropdown:** `Platformer`. Subgenre (if asked): `2D Platformer`.
- [ ] **Tags entered in T-075 priority order:**
  `pixel-art, platformer, 2d, nature, cute, atmospheric, exploration, colorful, side-scroller, family-friendly, accessibility`.
  Confirm none of the 6 anti-tags from `marketing/itch-page-draft.md` §3 were auto-
  added by itch's auto-complete (`difficult`, `precision-platformer`, `metroidvania`,
  `educational`, `walking-simulator`, `casual`).
- [ ] **System requirements** filled per draft §5 (separate desktop + Android entries).
- [ ] **Five screenshots uploaded** in the order documented in draft §7 (sidebar slots
  1–5). Marquee (`screenshot-01.png`) renders cleanly at thumbnail size in itch's
  browse/search results.
- [ ] **Controls reference** filled per draft §6. If T-121 (default swap S→Q) has
  NOT merged into the launch SHA, swap `Q` for `S` in the swap-key row of §6 before
  pasting. (T-121 is currently blocked on T-118 → T-105 → T-035 chain per HANDOFF.md
  "Sonnet pipeline".)
- [ ] **Trailer field:** leave blank if no trailer cut, OR paste YouTube URL if one
  exists. T-048 / draft §8 confirms blank-trailer is acceptable for alpha (8/12
  reference pages had no trailer).
- [ ] **Pricing model** per draft §9 — recommended pay-what-you-want with $2.99
  suggested price for alpha.
- [ ] **Links + credits block** from draft §10 appended to the bottom of the
  description. GitHub source + issues + discussions URLs are correct (the public-repo
  URL `github.com/SohailShahM/Cloudy-Ninja` is the canonical one).
- [ ] **Press outreach list ready.** `marketing/press-outreach-list.md` reviewed —
  no outreach emails sent yet, but the list of names + outlets is current.
- [ ] **Festival eligibility decision logged.** `marketing/festival-eligibility.md`
  read; if any festival submissions are part of the launch wave, those deadlines are
  tracked separately (this checklist does not commit to festival timing).
- [ ] **Page set to public + unlisted for the first 24h.** Flip to fully listed only
  after a live-page smoke-pass (typos, broken images, tag spelling, link checks).
  Per draft §11 final bullet.

---

## 4. Community channels

Prep the inbound + outbound channels before the page goes live, so the first
player who finds the game can actually reach you.

- [ ] **GitHub Discussions enabled** at `github.com/SohailShahM/Cloudy-Ninja/discussions`
  and seeded with at least one welcome thread (template suggestion: "Found a bug?
  Tell us here" + "What did you think of Level X?"). The itch.io page footer links
  to Discussions per draft §10.
- [ ] **GitHub Issues** still open for bug reports. Confirm issue templates from
  `.github/ISSUE_TEMPLATE/` (prior-session scaffold) render correctly — a fresh "New
  Issue" page should show the bug/feature/accessibility-feedback template menu.
- [ ] **Discord server / forum thread (optional).** The launch does not require a
  Discord; if one is set up, link it in itch.io draft §10 BEFORE publishing. Do NOT
  link a placeholder server. If no Discord exists yet, leave it out — GitHub
  Discussions is the documented community surface.
- [ ] **Social post queued for launch announcement.** At minimum one post on each
  channel the user actively uses (e.g. personal Twitter/Mastodon/Bluesky, gamedev
  subreddit, indie devlog blog). Copy should match the itch.io short-description
  voice — do not invent feature claims beyond what `marketing/itch-page-draft.md`
  already commits to. Schedule (or hold for manual post) at the same time the page
  flips to fully listed.
- [ ] **Cross-link present in README.md.** The repo's top-level README links to the
  itch.io page once it exists (currently links don't exist in README because the
  page isn't live). Add the link in the same commit as the launch flip; do not
  pre-link to a 404.
- [ ] **`prompts/` references intentional.** The repo's `prompts/` directory is
  visible to anyone who clones; nothing alpha-confidential should be in there.
  Spot-check.

---

## 5. Day-of operations

The launch-day-itself runbook — keep this section short and tactical.

- [ ] **Admin contacts confirmed reachable.** Sohail Shah (sole owner, per HANDOFF.md
  + draft §10 credits) is at the keyboard or has email/Discord notifications on for
  the first 4–8 hours after the page flips to listed.
- [ ] **GitHub Actions tab open.** If a hotfix needs to ship via itch-deploy.yml
  during the launch window, the user should have the Actions tab tabbed-up so the
  `workflow_dispatch` button is one click away. Note: `ITCH_API_KEY` secret must
  already be set (see §2 above).
- [ ] **`gh` CLI authenticated and PATH-available.** Per HANDOFF.md the CLI is
  `gh 2.92.0` authenticated as `SohailShahM` — re-verify with `gh auth status`
  before the launch window.
- [ ] **Rollback plan written down (here).** If a critical issue is reported within
  the first hour:
    1. Flip the itch.io page back to **draft** (itch's "Visibility & access" →
       "Draft" — instantly hides the page).
    2. If the bug is a crash on launch, push a hotfix branch + admin-merge once CI
       is green, then trigger `itch-deploy.yml` with an incremented `version-tag`.
    3. If the bug is content/copy, just edit the itch.io page in place — no rebuild
       needed.
    4. If the bug is legal (e.g. an asset license issue surfaces post-launch like
       T-126 did pre-launch), draft → fix → NOTICE.md update → republish. Do NOT
       leave a license-violating build downloadable while fixing.
- [ ] **Crash-report dir watched.** T-115's crash reporter writes to
  `<userHome>/.cloudy-ninja/crashes/crash-{ts}.log`. The user can't see *players'*
  crash files, but if any internal smoke / re-test produces one during the launch
  window, that's a strong "pull the build" signal.
- [ ] **Issues inbox monitored.** Subscribe / enable notifications on
  `github.com/SohailShahM/Cloudy-Ninja/issues` for the first 24h. New issues from
  external accounts (non-SohailShahM, non-Copilot, non-`claude-code-*`) get a same-
  day response even if the resolution is "thanks, tracking as T-XXX".
- [ ] **itch.io page analytics tab open.** Itch surfaces views + downloads + browse-
  referrals per-page. First 24h numbers are the marketing signal for whether the
  tag + screenshot ordering is working — they don't need a written report, just a
  glance.
- [ ] **HANDOFF.md alpha-blocker section cleared.** Once T-126 has merged, edit
  HANDOFF.md's "ALPHA-BLOCKING (human-required)" section to remove T-126. Do NOT
  publish the itch.io page while T-126 is still listed as blocking — that section
  is the canonical "are we ready" gate.
- [ ] **Pre-launch session-state captured.** Update HANDOFF.md ("Last updated" line
  + summary) immediately before the launch with the SHA being published, the itch.io
  channel name used, and the `version-tag` butler push value. Anyone picking up
  the project mid-launch needs to know exactly what shipped.

---

## Out of scope for this checklist (handled separately)

These belong to other tickets / docs and are intentionally NOT in the boxes above:

- Steam launch — covered by `marketing/steam-tags-research.md` (T-075) as future-state.
  Sprint D ships itch-only first. itch-page-draft §9 explicitly says "Steam key
  bundling: leave off for now."
- HTML5/web demo — `research/html5-web-demo-viability.md` (T-123) recommended Option 2
  with a Box2D-Teavm de-risk spike first; not on the alpha launch path.
- Trailer cut — see itch-page-draft §8. Acceptable to ship alpha without one.
- Cloud Atlas expansion to 12 entries — blocked on T-045 (NotebookLM step), out of
  alpha scope per HANDOFF.md "Not autonomous".
- Gamepad support — T-102 needs manual smoke with a real controller; not on the
  alpha launch path. itch-page-draft §6 already discloses this honestly.

---

*End of checklist. T-153. References: T-073, T-075, T-077, T-100, T-114, T-115, T-121,
T-124, T-125, T-126, T-048, T-045, T-102, T-123 (none invented — all live in `TASKS.md`).*

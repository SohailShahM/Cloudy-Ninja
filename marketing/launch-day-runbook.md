# Cloudy Ninja — alpha launch-day runbook

> Minute-by-minute checklist for the day the user flips Cloudy Ninja's itch.io page
> from `draft` (or `public + unlisted`) to fully live. Everything here is concrete:
> real commands, real URLs, real files. If a step references a system we do not
> actually have (a metrics dashboard, an automated rollback bot, a status page),
> it is not in this runbook.
>
> **Author:** claude-code-sub-agent · **Compiled:** 2026-05-13 · **Ticket:** T-154
>
> Cross-references:
> - `.github/workflows/itch-deploy.yml` (T-114) — the deploy workflow this runbook fires.
> - `docs/itch-deploy.md` — full setup guide (API key, secret install, troubleshooting).
> - `marketing/itch-page-draft.md` (T-124) — the page content + pre-flight checklist for the page itself.
> - `marketing/itch-listing-style-guide.md` — long-form listing rationale.
> - `marketing/presskit/` (T-077) — screenshots and brand assets the page links.
>
> **Assumptions before T-0:** the alpha-blocking issues (notably T-126 / Calibri font
> replacement) are closed, the itch.io page draft from §1-§10 of `marketing/itch-page-draft.md`
> is already pasted into the itch CMS as `public + unlisted`, and the build artifact
> from the most recent `main` is verified hand-runnable on a clean machine.

---

## Timeline overview

| Phase | Wall time | What you are doing |
|---|---|---|
| Pre-flight | T-0 minus 60 min | Verify CI green, lock the deploy SHA, take final screenshots, post the tease |
| Launch | T-0 | Fire `itch-deploy.yml`, verify the upload, flip unlisted → public |
| First hour | T+0 to T+60 min | Monitor comments and Issues, respond to early reports |
| First day | T+60 min to T+24h | Daily metrics pass, patch-PR readiness, second-tease post |
| Rollback (if needed) | any time | Push the previous build via `butler` + post an explanatory comment |

The whole pre-flight + launch sequence is **~75 minutes**. Don't compress it — the
manual smoke step is the load-bearing one.

---

## 1. Pre-flight (T-0 minus 60 min)

### 1a. Verify the last CI run on `main` is green

```bash
gh run list --repo SohailShahM/Cloudy-Ninja --branch main --limit 5
```

Confirm the most recent `ai-smoke.yml` run on `main` is `completed / success`. If
the latest run is `in_progress`, wait for it. If it is `failure`, **abort the
launch** and triage — do not ship a build off a red `main`.

To watch the in-flight run:

```bash
gh run watch --repo SohailShahM/Cloudy-Ninja
```

### 1b. Lock the deploy SHA

```bash
git fetch origin main
git log -1 origin/main --pretty=format:"%H %s"
```

Copy the full SHA somewhere outside this terminal (sticky note, separate text file).
This is the **deploy SHA** — the exact commit that will become the alpha build.
If anything merges to `main` between now and T-0, you'll either accept the new
HEAD or `gh workflow run` with `--ref <SHA>` to pin.

### 1c. Smoke-test the build locally on a clean profile

Run the JAR from a clean home directory (no existing save slots, no `~/.cloudy-ninja/`
cache) to catch first-launch issues:

```bash
# Build it locally so you ship the same artifact CI will:
./gradlew :lwjgl3:dist

# Move any existing save dir aside so first-launch is exercised:
mv ~/.cloudy-ninja ~/.cloudy-ninja.bak 2>/dev/null || true

# Run the JAR:
java -jar "lwjgl3/build/libs/Cloudy Ninja-1.0.0.jar"
```

Manual checks (5 min):
- Main menu loads, build label reads correctly (`v0.1.0 · YYYY-MM-DD`, per T-100).
- New game → first level loads, character renders, jump works.
- Pause overlay (Esc) opens cleanly with the fade-in.
- Settings → Controls page shows the swap key as **Q** (T-073 / T-121). If it
  shows **S**, T-121 has not landed; update the controls table in
  `marketing/itch-page-draft.md` §6 before launch.
- Settings → Accessibility shows Assist Mode + color-blind + reduced-motion toggles.
- Quit cleanly.

When done:

```bash
mv ~/.cloudy-ninja.bak ~/.cloudy-ninja 2>/dev/null || true
```

If anything is broken, **abort** — fix-forward via a normal PR, then re-run the
pre-flight from the top once `main` is green again.

### 1d. Take the final pre-launch screenshots

If `marketing/presskit/screenshot-0N.png` is older than the last UI-affecting
merge to `main` (check with `git log --since="14 days ago" -- core/src/`), retake
the five page screenshots before the page goes live.

Order to capture, matching `marketing/itch-page-draft.md` §7:

1. Sky Sanctuary hub (marquee — must read at 240px).
2. Mid-game level, character mid-air post-swap.
3. Eco-restoration moment (a corrupted tile becoming clean / planted).
4. Storm Sentinel boss arena.
5. Settings screen with Assist Mode + color-blind toggles visible.

Save into `marketing/presskit/` overwriting the old PNGs, then on the itch.io
page sidebar, replace the existing uploads (drag-drop in the CMS — itch keeps
the order you upload them in).

### 1e. Post the "going live" tease

Pick the channels you actually use. Suggested:

- **GitHub Discussions** (https://github.com/SohailShahM/Cloudy-Ninja/discussions) — open a new "Announcements" thread titled `Cloudy Ninja alpha goes live in ~1h`. One paragraph. Link to the itch.io page once it's public — for now link to the GitHub repo.
- **Mastodon / Bluesky / X** (whichever the user has the most followers on) — one short post, no link yet, image: the marquee screenshot.
- **r/IndieDev** / **r/IndieGaming** Reddit posts — skip the tease, wait for the launch post (T-0) so the timestamp matches the actual play link.

Template (paste-ready):

```
Cloudy Ninja alpha drops in ~1h. A 2D pixel platformer about restoring
broken ecosystems — three switchable heroes, eight hand-crafted levels,
ships with Assist Mode + colour-blind palettes. Itch.io link in the
next post.
```

### 1f. Verify itch.io page is paste-ready

Open https://sohailshahm.itch.io/cloudy-ninja/edit and walk through the §11
pre-flight checklist at the bottom of `marketing/itch-page-draft.md`. Visibility
should be **public + unlisted** at this point (not draft, not fully listed).

If anything in that checklist is unchecked, fix it now. The Tag Wizard tag order
is the most-skipped step.

---

## 2. Launch (T-0)

### 2a. Fire the deploy workflow

```bash
gh workflow run itch-deploy.yml \
  --repo SohailShahM/Cloudy-Ninja \
  --ref main \
  -f channel=desktop \
  -f version-tag=0.1.0-alpha
```

If the deploy SHA from §1b is no longer HEAD of `main` (something merged in the
last hour and you don't want it in the alpha), pin to the SHA instead:

```bash
gh workflow run itch-deploy.yml \
  --repo SohailShahM/Cloudy-Ninja \
  --ref <40-char-SHA-from-1b> \
  -f channel=desktop \
  -f version-tag=0.1.0-alpha
```

### 2b. Watch the run

```bash
gh run list --repo SohailShahM/Cloudy-Ninja --workflow itch-deploy.yml --limit 1
gh run watch --repo SohailShahM/Cloudy-Ninja
```

The run takes roughly 4–6 minutes:
- ~30s checkout + JDK setup
- ~2–3 min gradle build (`:lwjgl3:dist`)
- ~30s butler download + push
- ~30s artifact upload

If the `Verify ITCH_API_KEY is set` step fails: the secret is missing or set on
the wrong repo. See `docs/itch-deploy.md` → "Troubleshooting".

If the `butler push` step fails with `403 Forbidden`: the API key is invalid or
expired. Generate a new one at https://itch.io/user/settings/api-keys and
update the secret:

```bash
gh secret set ITCH_API_KEY --repo SohailShahM/Cloudy-Ninja
```

Then re-run the workflow.

### 2c. Verify the upload landed on itch.io

```bash
# From a local checkout, with butler already on PATH (or use .butler/butler):
export BUTLER_API_KEY="<the-same-key-as-the-secret>"
butler status sohailshahm/cloudy-ninja:desktop
```

You should see a build with the version label `0.1.0-alpha` and a recent
timestamp. Or check the web UI: https://sohailshahm.itch.io/cloudy-ninja/edit/uploads —
the uploads list shows the file size, version, and channel of every push.

### 2d. Smoke the live build

Download the JAR from the public itch.io page **in an incognito window** (no
itch.io session) — this verifies the page is reachable and the download works
for non-logged-in players:

1. Open https://sohailshahm.itch.io/cloudy-ninja in incognito.
2. Click the download button.
3. Run the JAR from a clean home directory the same way as §1c.
4. Confirm the build version label matches (`0.1.0-alpha` if you used the suggested tag).

If the download is gated by a "you must be logged in" prompt, the page is still
in **draft** — go back to itch.io edit and check the visibility setting.

### 2e. Flip unlisted → public

In the itch.io CMS (https://sohailshahm.itch.io/cloudy-ninja/edit), under
"Visibility & access":

- Change from **Public (but not listed)** to **Public**.
- Save the page.

The page is now indexed by itch.io's browse / tag pages. Discovery latency is
~10–30 min before the listing appears in tag feeds.

### 2f. Post the live link

Update the GitHub Discussions thread from §1e with the live URL:

```
Live: https://sohailshahm.itch.io/cloudy-ninja
```

Post the launch announcement on socials + Reddit. Template:

```
Cloudy Ninja alpha is live on itch.io.
https://sohailshahm.itch.io/cloudy-ninja

A 2D pixel platformer about restoring broken ecosystems. Three
switchable heroes, eight hand-crafted levels, multi-phase boss.
Ships with Assist Mode + colour-blind palettes — accessibility
never gates progression.

Free / pay-what-you-want. Source-visible on GitHub. Bug reports
welcome.
```

---

## 3. First hour (T+0 to T+60 min)

### 3a. Monitor itch.io comments

There is no email notification by default. Refresh manually every ~10 min for
the first hour, then every ~30 min for the rest of the day:

```
https://sohailshahm.itch.io/cloudy-ninja/community
```

Or enable email notifications under https://itch.io/user/settings → Notifications →
"Comments on my projects". Worth doing today specifically.

### 3b. Monitor GitHub issues

```bash
# Refresh in one command — issues opened in the last hour:
gh issue list --repo SohailShahM/Cloudy-Ninja --search "created:>$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)" --state open
```

(On Windows PowerShell: use `gh issue list --repo SohailShahM/Cloudy-Ninja --state open --limit 20` and eyeball the timestamps — the date math is shell-specific.)

Skim every new issue immediately. Triage rule of thumb:
- **Crash / can't launch the game** → P0, respond within 15 min ("thanks, looking now"), open a tracking comment.
- **Wrong key / wrong text / cosmetic bug** → P1, acknowledge, label, queue for the patch PR.
- **Feature request** → thank, label `enhancement`, leave for the post-launch backlog pass.

### 3c. Respond to early bug reports

For a confirmed crash on launch, the player likely has a crash log already
written by T-115's crash reporter at `<userHome>/.cloudy-ninja/crashes/crash-<ts>.log`.
Ask them to attach it:

```
Thanks for the report — the build writes a crash log to:

  Windows: %USERPROFILE%\.cloudy-ninja\crashes\
  macOS:   ~/.cloudy-ninja/crashes/
  Linux:   ~/.cloudy-ninja/crashes/

Could you attach the most recent `crash-*.log` from there? It will
have the stack trace I need to fix this.
```

### 3d. Watch the upload count

itch.io's creator dashboard shows download count under
https://sohailshahm.itch.io/cloudy-ninja/edit/analytics. Expect single digits in
the first hour for an alpha with no audience pre-built — that's normal, not a
problem to solve at T+30 min.

---

## 4. First day (T+60 min to T+24h)

### 4a. End-of-day metrics pass

Once at roughly T+12h, once at roughly T+24h. Check the itch.io creator dashboard:

- **Downloads** — https://sohailshahm.itch.io/cloudy-ninja/edit/analytics
- **Page views** — same dashboard, "Views" tab
- **Ratings** — same dashboard, "Ratings" tab (likely zero on day 1; ratings require players to play + return to itch)
- **Comments** — https://sohailshahm.itch.io/cloudy-ninja/community

These are the four numbers itch.io actually exposes for a project at this
stage. Don't extrapolate to a funnel, don't compute conversion rates — the
sample is too small. The number that matters most in the first 24h is
**comments + issues opened** (signal of engagement), not downloads.

### 4b. GitHub issues sweep

```bash
gh issue list --repo SohailShahM/Cloudy-Ninja --state open --search "created:>$(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%SZ)"
```

For every issue opened in the first 24h:
- Labelled and triaged within 24h of creation.
- A maintainer comment within 24h (even just "thanks, queued").
- Crash-class issues responded to within 4h.

### 4c. Patch-PR readiness

By the end of day 1 you should know whether you need a patch build. If yes,
prepare it on a normal feature branch (`claude/patch-0.1.1`), get CI green,
merge to `main`, then deploy:

```bash
gh workflow run itch-deploy.yml \
  --repo SohailShahM/Cloudy-Ninja \
  --ref main \
  -f channel=desktop \
  -f version-tag=0.1.1-alpha
```

Itch.io keeps the old upload available unless you explicitly delete it; the new
upload becomes the default download. Post a short devlog entry on the itch.io
page after deploy:

- https://sohailshahm.itch.io/cloudy-ninja/edit/devlog/new
- Title: `0.1.1 — <one-line summary of the fix>`
- Body: 2-3 sentences, link to the merged PR.

### 4d. Second-tease / day-1 follow-up post

A short follow-up on the same channels as §1e + §2f, around T+12h:

```
Day 1 of the Cloudy Ninja alpha — thanks for the early plays + bug
reports. Already queued <N> fixes for 0.1.1. Keep them coming:
https://github.com/SohailShahM/Cloudy-Ninja/issues
```

Skip this if engagement was zero — it's a thank-you post, not a "please look at
me" post. If nobody engaged, the right move is to come back in week 2 with
something new to say.

---

## 5. Rollback

Used only if the live alpha is **critically broken** — i.e. it crashes on launch
for most players, corrupts saves, or ships something that violates licensing
(an asset that shouldn't have shipped, a credit that's wrong, etc.). Bug reports
that affect a single configuration are NOT rollback-worthy — fix-forward via a
patch PR + §4c.

### 5a. Identify the previous good build

```bash
export BUTLER_API_KEY="<the-itch-api-key>"
butler status sohailshahm/cloudy-ninja:desktop
```

`butler status` lists the current build and previous builds with their
`userversion` labels and upload timestamps. The previous-good build is whichever
one preceded the broken upload.

### 5b. Build the previous-good artifact locally

If the previous build was deployed from a tagged commit (e.g. `v0.1.0-alpha`
was deployed from commit `abc1234`), check out that commit and rebuild:

```bash
git fetch --tags origin
git checkout <previous-good-SHA>
./gradlew clean :lwjgl3:dist
# Output: lwjgl3/build/libs/Cloudy Ninja-1.0.0.jar
```

### 5c. Push it as a new build

You cannot "restore" a deleted itch.io build; the path forward is to push the
previous-good artifact with a higher version label so it becomes the new
current build:

```bash
export BUTLER_API_KEY="<the-itch-api-key>"
export ITCH_JAR="lwjgl3/build/libs/Cloudy Ninja-1.0.0.jar"
export ITCH_CHANNEL="desktop"
export ITCH_VERSION_TAG="0.1.0-alpha-rollback"
bash scripts/deploy-itch.sh
```

Or via the workflow if you tag the previous-good SHA and push it as a ref:

```bash
git tag rollback-0.1.0 <previous-good-SHA>
git push origin rollback-0.1.0
gh workflow run itch-deploy.yml \
  --repo SohailShahM/Cloudy-Ninja \
  --ref rollback-0.1.0 \
  -f channel=desktop \
  -f version-tag=0.1.0-alpha-rollback
```

### 5d. Post the explanatory comment

On the itch.io page community thread + the GitHub Discussions announcement,
plus a devlog entry. Be specific, be calm, name the symptom, name the fix:

```
Rolling the 0.1.X build back to 0.1.0-alpha (build label 0.1.0-alpha-rollback).
The X.Y build had a launch crash on Windows < 11 caused by <one-line>.
A patched 0.1.Y build will go live in the next 24-48h after I fix and verify
the regression.

If you already downloaded 0.1.X, please re-download — the previous build is
back at the top of the uploads list. Save files are compatible across both
versions.

Thanks for catching this fast.
```

The post-mortem TL;DR (what broke, what the fix is, what process change keeps
it from happening again) goes in `LEARNINGS.md` after the patch ships, not in
the public comment.

---

## Appendix A — copy-paste command reference

```bash
# CI status before deploy:
gh run list --repo SohailShahM/Cloudy-Ninja --branch main --limit 5
gh run watch --repo SohailShahM/Cloudy-Ninja

# Deploy:
gh workflow run itch-deploy.yml --repo SohailShahM/Cloudy-Ninja --ref main -f channel=desktop -f version-tag=0.1.0-alpha

# Inspect what's live on itch:
export BUTLER_API_KEY="<key>"
butler status sohailshahm/cloudy-ninja:desktop

# Local rollback push (after rebuilding the previous-good JAR):
export ITCH_JAR="lwjgl3/build/libs/Cloudy Ninja-1.0.0.jar"
export ITCH_CHANNEL="desktop"
export ITCH_VERSION_TAG="0.1.0-alpha-rollback"
bash scripts/deploy-itch.sh

# Recent issues:
gh issue list --repo SohailShahM/Cloudy-Ninja --state open --limit 20

# Rotate the API key:
gh secret set ITCH_API_KEY --repo SohailShahM/Cloudy-Ninja
```

---

## Appendix B — what this runbook intentionally does NOT promise

- **No automated rollback.** Itch.io has no atomic rollback API; the only path
  is push-the-previous-build-with-a-new-label per §5. If you need a 60-second
  rollback you need a different storefront.
- **No status page.** Cloudy Ninja has no separate status page. Outage
  communication runs through the itch.io page community thread + GitHub
  Discussions.
- **No metrics dashboard beyond itch.io's built-in analytics.** No Mixpanel,
  no PostHog, no GA — the four numbers in §4a are the four numbers we have.
- **No SLA on issue response.** The numbers in §4b ("within 24h", "within 4h"
  for crashes) are aspirational targets for the launch window, not contractual
  commitments. Day 2+ should drift toward a normal-cadence triage pass.
- **No marketing push beyond §1e + §2f + §4d.** This is an alpha launch, not a
  product launch. If a press push is wanted later, that's a separate ticket
  (see `marketing/press-outreach-list.md`).

---

*End of runbook. T-154. See `docs/itch-deploy.md` for the workflow setup
guide, `marketing/itch-page-draft.md` (T-124) for the page content, and
`HANDOFF.md` for repo-level context (CI billing, admin-merge, branch
protection).*

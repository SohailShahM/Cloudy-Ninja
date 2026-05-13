# Community Management Playbook — Cloudy Ninja Alpha

This playbook documents how the Cloudy Ninja alpha community is managed by a **solo developer (Sohail Shah / MashxLabz)**. Bandwidth is the binding constraint, so every SLA, channel choice, and escalation path below is sized for one human running this in evenings and weekends — not a community team. If a process here would only work with two or more full-time community managers, it has been cut or downgraded.

The repository is published under a **proprietary "source-visible" license** (see `LICENSE`). That posture shapes parts of this playbook — particularly how we talk to volunteer contributors and how license-violation reports are handled.

---

## 1. Channels

The alpha runs across three intentionally separated surfaces. Each surface has a single purpose; routing a conversation to the wrong surface is the most common time-sink and is actively redirected.

### 1.1 GitHub Issues — bugs, security, feature requests (structured)

- **Purpose:** Anything that needs a fix, a tracked decision, or a security disclosure.
- **Templates already in place** (`.github/ISSUE_TEMPLATE/`): `bug-report.yml`, `feature-request.yml`, `accessibility-issue.yml`. Blank issues are disabled (`config.yml`) — this is deliberate: it forces reporters to pick a structured form and saves triage time.
- **Why GitHub Issues for bugs (not Discord):** Issues are searchable, dedupable, linkable from commits/PRs, and survive the lifetime of the project. Chat channels are append-only firehoses where bug reports rot.
- **Out of scope here:** "How do I do X?" questions, general feedback, build chatter — those go to Discussions (see 1.2). The issue templates' `config.yml` already nudges users toward Discussions for non-bug questions.

### 1.2 GitHub Discussions — community chat (the pick)

**Decision: Use GitHub Discussions, not Discord.** Rationale:

| Factor | GitHub Discussions | Discord |
|---|---|---|
| Solo-dev moderation load | Low — async, no real-time pressure, no voice channels to police | High — requires near-constant presence, mod bots, role config |
| Discoverability | Public, indexed by Google, linkable from Issues/PRs | Walled garden; nothing indexable; users re-ask the same question forever |
| Identity & trust | Same GitHub identity used for Issues/PRs | Separate Discord identity; no cross-link to contribution history |
| Setup cost | Zero — already enabled, templates landed in T-080 (`.github/DISCUSSION_TEMPLATE/`) | Server, roles, channels, rules, bots, verification flow |
| Risk if I disappear for a weekend | Threads sit idle; nothing breaks | Real-time vacuum, drama escalates without a mod |
| License-violation evidence trail | Public, archived, citeable | Ephemeral, screenshot-only evidence |

Discord is the "right" tool for a community with a paid moderator team. We are not that. We will revisit if and only if Discussions becomes the limiting factor on engagement (concrete trigger: >20 active threads/week sustained for 4 weeks, *and* users explicitly request real-time chat).

Existing discussion templates: `announcements.yml` (outbound from the dev), `help.yml` (inbound questions). General/Ideas/Show-and-tell can be enabled as default GitHub categories.

### 1.3 itch.io comments — first-day feedback

- **Purpose:** Capture the reaction of users who installed a build via itch.io and would never open a GitHub account.
- **Realistic expectation:** Most itch.io comments are short ("loved it!" / "crashed on level 3"). They are signal, not a support channel.
- **Routing rule:** If a comment contains a reproducible bug, reply with a thank-you and a one-line "I've filed this here: \<issue link\>" — *I* file the issue on the user's behalf. Do not ask the user to re-file on GitHub; that loses 90% of them.
- **Cadence:** Skim itch.io comments once per build-day (the day a new build ships) and then weekly during the alpha. Do not monitor in real time.

### 1.4 Out-of-scope channels (explicit non-goals)

- **No Twitter/X DMs for support.** Replies fine; DMs route to "please file an issue."
- **No email support during alpha.** The contact path is the GitHub profile linked from `LICENSE` and that is reserved for licensing inquiries (see Section 4.1).
- **No Reddit subreddit run by me.** If one emerges organically, fine; I do not moderate it.

---

## 2. Response SLA (sized for one human)

These SLAs are **internal targets, not promises to users.** They are posted here so future-me can be honest about whether I'm keeping up. Missing an SLA is a signal to re-scope, not to apologize publicly.

| Surface / event | Target first-touch | Target resolution | Notes |
|---|---|---|---|
| **GitHub Issue — Critical severity bug** (crash on launch, save corruption, security) | **24 hours** acknowledgment | Hotfix branch within 72h; ship within 1 week | "Acknowledgment" = a labeled, triaged issue with at least one substantive comment. Not a fix. |
| **GitHub Issue — High severity bug** | **3 business days** | Triaged into a milestone; fix when prioritized | |
| **GitHub Issue — Normal / Low** | **1 week** | Backlog; may be closed `wontfix` with rationale | |
| **GitHub Issue — Feature request** | **2 weeks** | Triaged: accepted, deferred, or declined with rationale | Most feature requests are politely declined during alpha; that's fine. |
| **GitHub Issue — Accessibility report** | **3 business days** | Treated as High severity by default | A11y reports are disproportionately valuable — the user did unpaid QA work. |
| **Community PR from outside contributor** | **3 business days** acknowledgment | **Code review within 1 week**; merge or close within 2 weeks | See Section 5 for the review checklist. |
| **GitHub Discussion — Help / question** | **1 week** | No resolution target — community-answerable | If a question is answered by another user, leave it. Don't post a "thanks!" on every thread. |
| **GitHub Discussion — Announcement reply** | No SLA | n/a | |
| **itch.io comment** | Build-day, then weekly | n/a | See 1.3. |
| **Security disclosure** (via Issue or via the GitHub profile contact) | **24 hours** | Patch released or mitigation posted within 14 days | See Section 4.3. |

**Honest caveats:**

- "Business day" = Mon–Fri, my timezone. Weekends and holidays roll forward.
- These SLAs assume I am not on vacation. If I'm offline for >7 days, a pinned "away" announcement goes up in Discussions and a banner on the README. There is no on-call backup.
- Alpha-phase volume is expected to be low (single-digit issues/week). If volume spikes 5x, the first thing to slip is the *feature-request* and *Normal/Low* SLAs — those move to "best-effort, no target." Critical bugs and a11y stay at the stated targets.

---

## 3. Tone

The tone is **warm and technical.** Not corporate, not chummy, not defensive.

### 3.1 What "warm + technical" sounds like

- Open with a thank-you for the report or PR. Not effusive — one short sentence.
- Use the user's terminology before introducing mine. ("You're seeing the wall-jump reset under low fps — that's the `gravityScale` not getting cleared in `LayaController.onLand()`.")
- Show your work. Cite line numbers, commit hashes, ticket IDs (`T-XXX`). The audience is technical alpha testers; they appreciate it.
- It is okay to say "I don't know yet" or "this is going to take a while."
- It is okay to say "no" to feature requests. Cite scope or the roadmap.

### 3.2 What it does *not* sound like

- No "Hey friend!" / "Awesome!!!" / exclamation-heavy retail-support voice.
- No "we" if there is no team. Use "I." Pretending to be a company is dishonest and easy to spot.
- No defensive language when bugs are reported. The reporter did unpaid work; the reply is gratitude plus a fix or a triage.

### 3.3 Citing the proprietary license — openly, without apology

The repo is source-visible, not open-source. This will come up. Use plain, factual language. Do not soften it; do not lecture.

**When to cite the license explicitly:**

- Someone asks "can I fork this and ship my own version?" — answer: cite `LICENSE`'s "NOT PERMITTED" section directly. Forking for PRs is fine; redistribution as a competing product is not.
- Someone asks "is this open source?" — answer: "No — it's source-visible under a proprietary license (see `LICENSE`). You can read it, learn from it, and submit PRs. Redistribution and derivative works need written permission."
- Someone asks "can I use this code in my game?" — answer: short excerpts (~30 lines) with attribution are fine; substantial use needs a license inquiry through the GitHub profile.
- Someone submits a PR — the `LICENSE` already grants the project a license to their contribution; do not surprise contributors with this after the fact. If a contributor has questions about the CLA-equivalent paragraph in `LICENSE`, answer them before merging.

**Template lines (reuse, don't reinvent each time):**

> Cloudy Ninja is source-visible, not open-source. The `LICENSE` file in the repo covers what's permitted — forking for PRs and code review is fine; redistribution and derivative works aren't, without written permission.

> Thanks for the interest! That use case falls outside what the current `LICENSE` permits. If you'd like to talk about a commercial license, please reach out via the GitHub profile linked at the bottom of `LICENSE`.

---

## 4. Crisis playbook

Three scenarios that need a pre-written response so I don't improvise badly under stress.

### 4.1 License violation reports

**Trigger:** Someone reports (or I notice) a repo, asset pack, itch.io page, or commercial product that has redistributed Cloudy Ninja source, compiled builds, or assets in a way `LICENSE` does not permit.

**Severity tiers:**

- **Tier 1 — Public fork republished as a standalone game** (e.g., on itch.io, Steam, an app store). Highest priority. Concrete commercial harm.
- **Tier 2 — Asset extraction** from shipped binaries, reposted as a free asset pack or used in another game.
- **Tier 3 — Substantial source-code copy** into another public repo with no attribution.
- **Tier 4 — Excerpt over the ~30-line / no-attribution threshold.** Often unintentional.

**Response sequence (do these in order, do not skip):**

1. **Capture evidence.** Screenshot, archive.org snapshot (`web.archive.org/save/<url>`), commit hash, store-page snapshot. Time-stamp it locally. Do this *before* contacting anyone.
2. **Open a private tracking issue / note** for myself. Not a public GitHub issue — a private note in my tickets folder, because public discussion can taint a future takedown.
3. **First contact: the alleged violator, not the platform.** Polite, factual, cite `LICENSE`, give a 14-day window to remove or come into compliance. Template:
   > Hi — I'm the author of Cloudy Ninja (`https://github.com/SohailShahM/Cloudy-Ninja`). The project is published under a source-visible proprietary license (see `LICENSE`) that doesn't permit \<the specific use\>. Could you take \<specific action\> within 14 days? Happy to discuss licensing if there's a use case I can support. — Sohail
4. **If no response in 14 days, escalate to the hosting platform.** GitHub DMCA (`https://github.com/contact/dmca`), itch.io takedown, store-specific abuse forms. Provide the evidence captured in step 1.
5. **Public communication:** none until resolved. Do not post about an active dispute in Discussions, on Twitter, or in the repo. After resolution, a one-line `NOTICE.md` or release-notes mention is fine — no naming-and-shaming.
6. **Do not threaten legal action you won't take.** A polite, citable request followed by a platform DMCA is far more effective than lawyer-cosplay from a solo dev.

### 4.2 Harassment in comments (Issues, Discussions, itch.io)

**Trigger:** A comment, issue, or thread that contains personal attacks, slurs, doxxing, sexual harassment, or targeted abuse — of me, of other users, or of third parties.

**Response sequence:**

1. **First, breathe.** Do not respond inside 1 hour of seeing it. Heated replies from the maintainer make everything worse.
2. **Hide the comment** (GitHub: "Hide" with reason → "Abuse"; itch.io: delete). This is moderation, not censorship — `LICENSE`-unrelated abuse is out of scope for any channel I run.
3. **Lock the thread** if there is an active back-and-forth that is heating up. State the reason in a short, neutral final comment: "Locking this thread — please keep replies on-topic and civil. Re-file the underlying bug in a new issue if needed."
4. **Block the user** if the abuse is targeted or repeated. GitHub block is per-account and global to my account; itch.io has a block as well. Do not feel obligated to warn first if the comment is severe.
5. **Report to the platform** if the behavior crosses into harassment-policy territory: GitHub `https://github.com/contact/report-abuse`, itch.io support.
6. **Do not screenshot-and-quote the abuse publicly.** Amplifying the comment to a wider audience helps the abuser, not the community.
7. **If the abuse is *of me* and severe:** step back from the channel for 24 hours. The repo will survive a day without a maintainer reply. Friends, family, or a therapist outrank a GitHub issue.

A short Code of Conduct will land separately (see follow-up: a CoC referencing the Contributor Covenant or similar, scaled for a solo project). Until then, the operating principle is: this is my repo, abuse gets removed, no further justification owed.

### 4.3 Critical bug in shipped build

**Trigger:** A bug in a publicly distributed build (itch.io page, Steam page, web demo) that causes crashes on launch, save-file corruption, hardware-damaging behavior (rare, but e.g., unbounded GPU usage), or a security issue (e.g., RCE via asset loading, credential leak in logs).

**Response sequence:**

1. **Confirm and reproduce within 4 hours of first report.** If reproducible: this is a P0.
2. **Pull the build** if the severity is "save corruption" or worse. On itch.io, unpublish the affected version while keeping the page up. On Steam (future), use depot-rollback. Better to ship nothing for a day than to keep shipping a save-eater.
3. **Pin an announcement** in GitHub Discussions (`announcements.yml` template) and on the itch.io page: what's broken, what users should do (e.g., "back up your save folder before re-launching"), expected ETA.
4. **Hotfix branch** off the release tag, narrowest possible diff, full CI run (compile, test, lint per `.github/workflows/ci.yml`). Do not bundle unrelated fixes.
5. **Ship the patch and update the announcement** with the fixed version + commit hash. Leave the original announcement visible — do not memory-hole it.
6. **Postmortem within 7 days.** Short markdown in `docs/incidents/` (folder TBD): what happened, why CI didn't catch it, what test or check would have caught it, what's now in place. Public. Boring is good — boring postmortems build trust.
7. **For security-specific issues:** do not include the exploit details in the public announcement until after the patch ships. After patch ships, a CVE-style write-up is fine.

---

## 5. Volunteer-PR review checklist (outside contributors)

The `LICENSE` already covers the legal side: by opening a PR, the contributor grants the project a perpetual license to their work. The review checklist below covers the technical and social side. Goal: a contributor who submits a thoughtful PR gets it merged or gets clear, actionable feedback inside the **1-week SLA** from Section 2.

The checklist is ordered — **stop at the first failed item, ask for changes, do not continue review.** This protects both reviewer time and the contributor's morale (it is worse to get a 12-item review on a PR that has a scope problem than to get one comment asking for the scope to be tightened first).

### 5.1 Triage gate (before any code review)

- [ ] **PR has a ticket ID or a linked issue.** No drive-by PRs that change behavior without a ticket. Trivial typo / doc-only PRs are exempt. (Matches `PULL_REQUEST_TEMPLATE.md` "Closes T-XXX.")
- [ ] **Scope matches the ticket.** A PR claiming to fix one bug should not also refactor an unrelated subsystem. If it does, ask the contributor to split it. Politely.
- [ ] **License acknowledgment.** Contributor has not added a header claiming a different license (MIT, Apache, etc.) to new files. If they have, point at the `CONTRIBUTIONS` paragraph in `LICENSE` and ask them to confirm they're okay with it. Do not merge until they confirm.
- [ ] **No vendored third-party code without a `NOTICE.md` update.** New dependencies, fonts, sprites, sounds, or snippets pulled from elsewhere must show up in `NOTICE.md` with source and license.

### 5.2 Code review

- [ ] **Style matches the existing codebase.** Kotlin conventions, naming, file layout. Don't bikeshed — if it compiles and matches what's around it, move on.
- [ ] **`./gradlew :core:compileKotlin` passes** (CI will catch this, but a contributor whose CI is red and who hasn't fixed it within 48 hours gets a nudge, not silence).
- [ ] **`./gradlew :core:test` passes.** Same CI rule.
- [ ] **No new `[Perf]` warnings** (`PULL_REQUEST_TEMPLATE.md` test plan item: fps ≥ 100, maxDelta ≤ 0.05).
- [ ] **Behavior change has a test.** A bug fix should add a regression test. A new feature should add at least one happy-path test. The bar is "if this regresses six months from now, will the test catch it?"
- [ ] **No dead code, no commented-out code blocks.** Git keeps history; the file shouldn't.
- [ ] **No `println` / `System.out` / debug logging left in.** Use the existing logger.
- [ ] **No secrets, no personal paths, no `.env` content.** Check the diff for `C:\Users\` and similar.
- [ ] **Public API surface changes are intentional and minimal.** Adding a new public field/function is fine; changing an existing signature should be flagged.
- [ ] **Touches only the files implied by the ticket.** Stray formatting changes across the repo are a scope problem — ask for them to be reverted or split out.

### 5.3 Social / contributor experience

- [ ] **First review comment is a thank-you.** One sentence. Then the substantive feedback.
- [ ] **Feedback is specific.** "This could be cleaner" is not feedback. "Pull this branch into a `private fun` to match the pattern in `EboController.kt:42`" is.
- [ ] **If the PR is being closed without merging, explain why.** Scope, direction, license, duplicate — whatever. Closing without a comment burns the contributor permanently.
- [ ] **If the PR sits idle on the contributor's side for >2 weeks after change requests, close it politely.** Offer to reopen if they come back. Long-stale PRs are a tax on every future review.

### 5.4 Merge

- [ ] **Squash-merge by default**, with a commit message of the form `T-XXX: <imperative summary>` matching the project's commit-log style.
- [ ] **Credit the contributor in the commit message.** Squash-merge preserves the GitHub `Co-authored-by` line automatically; do not strip it.
- [ ] **Close the linked ticket or issue.**
- [ ] **Reply on the PR once merged**, with a thank-you and the commit hash. This is the part most projects skip; doing it costs 15 seconds and is the difference between a one-time contributor and a repeat one.

---

## Appendix: review cadence for this playbook

This document is a living playbook. Review triggers:

- After any incident handled under Section 4 — update the playbook with what worked and what didn't.
- After every 10 volunteer PRs — sanity-check Section 5 against actual experience.
- Quarterly, otherwise.

If two consecutive reviews find nothing to change, drop the cadence to twice yearly.

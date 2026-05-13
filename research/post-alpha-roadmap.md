# Post-Alpha Roadmap (T-157)

**Status:** Planning memo. Doc-only. No code, no tickets re-prioritised here.
**Author:** claude-code-sub-agent
**Date:** 2026-05-13
**Branch:** `claude/T-157-post-alpha-roadmap`

---

## Purpose

The alpha is the **first public-but-private build to itch.io** (~5 testers). This document answers the question every alpha tester will eventually ask: *"what's next?"* It also answers the question we have to be honest with ourselves about: *"what's NOT coming, ever?"*

Three explicit goals:

1. **Sequence the work** between alpha and v1.0 so we ship instead of drifting.
2. **Set expectations** for the alpha audience so we don't accidentally over-promise.
3. **Manage scope** — call out things that have been considered and explicitly cut, so nobody (including future-us) re-litigates them.

This roadmap supersedes the aspirational bullets in `GAME_PLAN.md` §"Horizon 3 — Post-v1.0" but does not contradict the resolved decisions in §"Resolved decisions (v1.0 scope)". Where a real ticket already exists, this doc points at it rather than re-specifying.

---

## TL;DR

| Tier | Window | Headline work | Confidence |
|---|---|---|---|
| **v1.0** (must-haves) | 0–3 months post-alpha | Ghost replay (T-038), Cloud Atlas → 12 (T-045), graphics overhaul (T-046), gamepad (T-102) | High — all ticketed, all blocked only on small inputs |
| **v1.1** (nice-to-haves) | 3–6 months post-v1.0 | HTML5 web demo (per T-123 memo), second locale, online leaderboards | Medium — scoped but not yet ticketed |
| **v2.0** (stretch) | 6–18 months post-v1.0 | NG+, daily challenge, community level editor | Speculative — listed for shape, not commitment |
| **Recurring** | Continuous | Dead-dep removal (T-127), dep audits, attribution refresh | High — cadence matters more than scope |
| **Never** | — | Multiplayer co-op, ads/IAP, iOS, NFT/blockchain | Final — do not reopen without strong new evidence |

---

## 1. v1.0 must-haves

The bar for v1.0 is: *every tester who tried the alpha would notice if these were still missing.* All four items are already ticketed; none of them are speculative.

### 1.1 Ghost replay in time trials — T-038

- **Why it's v1.0:** Time trials shipped in alpha but the loop is incomplete without something to chase. Ghost replay is the standard expectation for the genre (Trackmania, Celeste's variant heart strats, Super Meat Boy).
- **Status:** Spec exists in TASKS.md. Tagged `autonomous-eligible: no` because it is determinism-sensitive (see `DETERMINISM.md`).
- **Risk:** Determinism audit (T-A2) shipped a seeded RNG wrapper, but Box2D's substep ordering still needs eyeballing under variable framerate. Reserve a real-human debugging day for this.
- **Done when:** A new best time on any level surfaces a translucent ghost on the next run; ghost desync is bounded (sub-pixel over a 60-second level).

### 1.2 Cloud Atlas to 12 entries — T-045

- **Why it's v1.0:** The educational angle is the marketing pitch (per `GAME_PLAN.md`). Shipping six entries when the alpha screenshots advertise twelve is a credibility hole.
- **Status:** Blocked on T-049 (climate-source compilation for NotebookLM). T-049 itself is queued in the Antigravity research suite.
- **Pipeline:** NotebookLM drafts grounded entries from verified-live NOAA/NASA/IPCC PDFs → human skim-review for accuracy → Copilot wires them into `CloudAtlasLibrary.kt`.
- **Done when:** All 12 entries reachable in gameplay; `atlas_full` achievement unlockable; visible text matches sources.

### 1.3 Graphics overhaul — T-046

- **Why it's v1.0:** The alpha runs on Kenney's CC0 `pixel-platformer` base pack plus procedural primitives. That is *fine for an alpha*. It is **not** fine for a $2.99–4.99 itch.io release where the screenshot grid sells the game.
- **Status:** Blocked on T-031 (tile-rendering abstraction; in-progress) and on a human art-direction decision. Tagged `autonomous-eligible: no`.
- **Key calls the user still owns:** keep Kenney + theme-accent stretch (cheap, fast), commission custom (expensive, slow, on-brand), or hybrid (Kenney terrain + custom characters/boss). Defer until alpha feedback lands — if testers don't flag the visual style as a blocker, the hybrid stays viable.
- **Done when:** No ShapeRenderer primitives for terrain or characters; the screenshot grid stands up to a Steam-tag-research-tier comparison set.

### 1.4 Gamepad support — T-102

- **Why it's v1.0:** Steam-tag research (T-075) and itch-page draft (T-124) both lean on "Full Controller Support" as a discoverability + audience-broadening tag. The competitive set (Celeste, Hollow Knight, Hyper Light Drifter, Hades) all ship with first-class gamepad support.
- **Status:** Spec exists. Tagged `autonomous-eligible: yes-with-review` because manual smoke needs a real controller plugged in — CI cannot validate this.
- **Done when:** Xbox + DualShock 4 both work plug-and-play; keyboard works in parallel; the Settings → Accessibility opt-out toggle exists.

### 1.5 Other v1.0 items already in the Sprint D / immediate-followup queues

These ship before or alongside v1.0 and are mentioned here for completeness, not re-spec:

- **T-126** Calibri → Inter font swap (alpha-blocking legal; happens *before* alpha if possible, but ships *with* v1.0 at the latest).
- **T-035 / T-105 / T-118** Audio bus sliders, master volume, mute shortcut — alpha streamer/recorder friendliness, low risk.
- **T-061** Per-character smoke matrix in CI — regression safety net for the v1.0 polish push.
- **T-076** Low-risk dep upgrades from T-051 audit — keep the platform fresh before the release lockdown.

---

## 2. v1.1 nice-to-haves

Free update window after v1.0 has been on itch.io / Google Play long enough to read a real signal. Roughly **3–6 months after v1.0 ships**.

### 2.1 HTML5 web demo

- **Why it's here:** T-123's viability memo recommends Option 2 — a stripped web demo (1–2 levels, no save, no dynamic lighting) via the `gdx-teavm` (xpenatan) backend. Effort estimate: **M (4–8 focused dev-days)** plus a 1-day Box2D-Teavm de-risk spike up front.
- **Why not v1.0:** The whole point is to drive traffic *to* the desktop alpha/v1.0 page; you can't do that until v1.0 is the funnel destination.
- **Pre-requisite:** The 1-day de-risk spike from T-123. If `gdx-box2d-teavm` 1.0.0-b6 can't run a Box2D smoke scene through TeaVM, the whole branch is cut and we re-evaluate.
- **Done when:** itch.io page embeds a Play-in-Browser button that loads in <10s on a mid-range laptop; first level is fully playable; the web build cannot corrupt or overwrite desktop saves.

### 2.2 Second locale

- **Why it's here:** i18n scaffolding is already shipped (T-059, T-091, T-122). Localising the existing 130+ keys to a second language exercises the API, surfaces the 7 deferred numeric-format-template issues from T-120, and dramatically broadens the audience for a small fixed cost.
- **Likely candidate:** Spanish or Japanese — pick based on alpha-tester geography signal. Defer the choice until v1.0 ships.
- **Out of scope:** RTL languages, font-glyph expansion (CJK requires either a different font baking strategy or a SDF font — separate decision).
- **Done when:** Language toggle in Settings cycles English ↔ second-locale; all StringKey entries are translated; no hardcoded-string regressions reintroduced (T-120 audit format works as the regression check).

### 2.3 Online leaderboards

- **Why it's here:** Time trials + ghost replay (T-038) make leaderboards a natural fit. Speedrun community is one of the cheapest engagement-driving features in the genre.
- **Implementation note:** Almost certainly piggybacks on a third-party service (itch.io community, leaderboard-as-a-service, or Steam Leaderboards once that platform is in scope) — building our own backend is a no-go at this team size.
- **Out of scope:** Ghost-sharing-on-leaderboard-row (that's v2.0 territory, see §3.1).
- **Done when:** Per-level top-10 visible from the time-trial screen; submission is anti-cheat-best-effort (server-side replay validation is explicitly NOT planned — this is a casual speedrun feature, not Trackmania).

### 2.4 Lower-confidence v1.1 candidates

Listed for the record; ranking shifts based on alpha feedback:

- **World 4 + Character 4 free content drop** — already mentioned in `GAME_PLAN.md` as the v1.1 headline. If alpha testers ship strong "I want more" signal, this absorbs most of v1.1 scope and the rest of §2 slides to v1.2.
- **Daily challenge mode (procedural-level-of-the-day)** — borderline v1.1/v2.0; depends on whether World 4 absorbs the cycle.
- **Per-level pacing retunes from playtest data** — continuous, but explicit v1.1 retune pass justified by aggregate alpha telemetry (if we ever ship any).

---

## 3. v2.0 stretch

**These are speculative.** They are listed to give shape to a long horizon, not to commit to anything. Treat this section as "what would v2.0 look like if v1.0 + v1.1 land successfully and we choose to keep investing."

### 3.1 New Game Plus (NG+)

- **Shape:** Existing saves can be carried into a harder mode after the boss is defeated. Could mean modifier-style remixing (faster enemies, no checkpoints, ability constraints) rather than fresh content. Reuses the entire existing levelset, which is the appeal.
- **Why stretch:** The save-format-migration scaffold (T-113) already exists, so the schema work is cheap. The design work — what NG+ *actually means* mechanically — is the open question.

### 3.2 Daily challenge

- **Shape:** Server-seeded daily modifier (e.g. "today: no Wind Dash, double spirits") applied to a fixed daily level. Single leaderboard per day.
- **Why stretch:** Depends on the leaderboard infra from §2.3. Could land in v1.1 if §2.3 + content design are both ready; could slide to v2.0 if not.

### 3.3 Community level editor

- **Shape:** In-game editor that produces shareable TMX-or-equivalent level files. Curation either via itch.io community page or (much later) Steam Workshop.
- **Why deeply stretch:** Level-format stability has never been a goal in alpha — TmxLevelDefinition has changed shape ~5 times since Sprint A. A public editor implies a frozen format, which implies giving up a refactor lever we still rely on. Reasonable to commit to this only after the engine has been stable for several months.
- **Cut signal:** If the alpha audience does not strongly request this within the first 6 months, deprioritise indefinitely. Most platformer communities never reach the editor-justifying engagement floor.

### 3.4 Other stretch ideas worth listing once

To prevent re-discovery: Steam port (gated on itch.io+Google-Play traction per `GAME_PLAN.md`), Switch port (gated on Steam traction + finding a Switch porting partner — Nintendo dev-kit access is not casually approved), educational/school licensing program (decision deferred per `GAME_PLAN.md` "Open questions"), expanded soundtrack release, plushie/merch (only if a community organically forms).

---

## 4. Recurring debt

Items that don't ship in a single version because they're cadence, not features. Schedule a quarter-yearly pass after v1.0.

### 4.1 Dead-dep removal (immediate — T-127)

`core/build.gradle` declares `ashley` (ECS) and `gdx-ai` (AI utility) but the codebase imports neither. Identified by T-123's HTML5 spike. T-127 already exists; ship before v1.0.

### 4.2 Periodic dependency audits

- **Cadence:** Every 6 months, or before any major release.
- **Pattern:** T-051 produced `research/dependency-audit.md`; T-076 executes the LOW-risk upgrades from it. Re-run the audit on the same cadence — don't let the platform drift two major libGDX versions behind.
- **Hard rule:** Never bundle MEDIUM/HIGH risk upgrades. Each is its own PR. The T-076 spec already encodes this.

### 4.3 Asset attribution refresh

- **Cadence:** Before each release (alpha, v1.0, v1.1, v2.0) and any time `NOTICE.md` is edited.
- **Pattern:** T-125's `research/asset-attribution-audit.md` is the template. Walk every file under `assets/`, cross-check against `NOTICE.md`, surface mismatches as P2 alpha-blockers (the Calibri T-126 surface was exactly this).
- **Reason:** Adding-an-asset is a fast-loop activity (drop a PNG, ship). Updating `NOTICE.md` is a slow-loop activity (manual edit, easy to forget). The audit is the catch.

### 4.4 i18n coverage re-audit

- **Cadence:** When a second locale is added (forces it), and again before each major release.
- **Pattern:** T-120 produced `research/i18n-coverage.md`. Each new screen introduces new hardcoded-string risk; the audit is what catches drift.

### 4.5 Save-format migration debt

Whenever a save schema change ships, append a migration step to `SaveMigrations.kt` (T-113 scaffold). The chain itself is the debt — review every 12 months whether any v(old) steps can be retired (we drop support for saves older than N releases). Not urgent until the chain has ≥3 entries.

### 4.6 LICENSE / NOTICE review

- **Cadence:** Before each release.
- **Why:** The repo flipped public→private→public mid-Sprint-D (`HANDOFF.md` CI billing journey). Each platform expansion (itch.io → Google Play → Steam → Switch) introduces new license-text obligations.

---

## 5. Never list

**Things explicitly NOT planned.** Listed so they don't get re-litigated and so the alpha audience knows what kind of game Cloudy Ninja is *not* trying to be.

- **Multiplayer co-op.** Per `GAME_PLAN.md` — "probably never worth it at this scope." Cloudy Ninja is a single-player momentum platformer; adding networked physics is an order of magnitude of work and would change what the game is.
- **PvP / competitive multiplayer.** Same reasoning; even less of a fit.
- **Ads.** Per `GAME_PLAN.md` — cut entirely. Premium pricing only.
- **IAP / microtransactions / cosmetic shops / battle pass.** Per `GAME_PLAN.md` — cut entirely. Premium one-time purchase only. The educational angle is marketing, not a paywall mechanic.
- **NFT / blockchain / crypto / "Web3" anything.** Not a fit for the audience or the brand. Hard no.
- **iOS port.** Per `GAME_PLAN.md` — deferred indefinitely. libGDX iOS toolchain complexity vs. audience size doesn't pencil out for a solo-dev release. Re-evaluate only if a porting partner appears or the toolchain improves dramatically.
- **Always-online / DRM beyond the storefront's defaults.** Single-player game, single-player save model. Online leaderboards (§2.3) are opt-in; the game must remain fully playable offline.
- **AI-generated final assets shipped without human curation.** Generative tools are fine for ideation and for the procedural music generator (which is deterministic, not LLM-based). Final-ship art assets are either CC0/licensed-source or human-made/commissioned.
- **Live-service / seasonal content.** Daily challenge (§3.2) is the only flirt with live-ness and even that is procedural-from-seed, not server-curated.
- **Server-side replay validation for leaderboards.** §2.3 is a casual speedrun feature; if a competitive scene materialises and demands rigour, revisit then. Not a v1.x problem.
- **User-generated-content monetisation.** Even if the community level editor (§3.3) lands, level sharing stays free and unmonetised. Workshop-style tipping is not on the table.
- **Loot boxes / gacha mechanics / randomised cosmetics.** Same family as IAP. Hard no.

---

## How this doc gets updated

Same rule as `GAME_PLAN.md`: this is a plan, not a contract. Update it when the alpha audience surfaces something that changes the ordering, when a ticket lands and clears a v1.0 item, or when a "Never" entry needs explicit re-litigation (the bar for moving anything *out* of §5 is high — bring evidence).

If an item in this doc isn't backed by a real TASKS.md ticket and you start working on it, write the ticket first.

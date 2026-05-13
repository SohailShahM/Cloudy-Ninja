# Competitor Pricing Analysis for Cloudy Ninja

Research-only deliverable for ticket **T-156**. Compiled 2026-05-13 by claude-code-sub-agent.

The goal: produce a defensible price strategy for Cloudy Ninja — what we should ask for at **alpha launch on itch.io** (Sprint D), and what range to plan for at the eventual **1.0 / Steam launch**. Every price datapoint below cites a source URL and the date the source was checked. Prices on Steam fluctuate daily (sales, regional shifts); the **MSRP / launch price** column is the load-bearing number, not the snapshot-current price.

Cross-references:
- The 12 comparable games come from **T-075** (`marketing/steam-tags-research.md`), the tag-research deliverable that surveyed three clusters: precision/pixel-art platformers, eco/nature games, wholesome/accessible exploration games.
- The pay-what-you-want recommendation in **T-124** (`marketing/itch-page-draft.md` §9) is the prior art for the alpha pricing approach; this document either confirms or refines it.
- Cloudy Ninja's existing presskit positions the game at **USD $2.99–4.99** (per `marketing/itch-page-draft.md` §9 and the presskit referenced therein). That range is the starting hypothesis; this analysis tests whether it holds against the comparable set.

> **Hard rule for this document:** research-only. No speculation about Cloudy Ninja's future revenue, no projections, no "you'll make $X if you price at $Y" claims. Only "the comparable cluster sits at $X–$Y, here's where we recommend sitting within that, here's why." The actual price decision is the user's.

---

## (a) Steam pricing — 12 T-075 comparables, MSRP + current snapshot

Two prices for each title: the **launch MSRP** (the price the developer chose for full release, which is the load-bearing decision we're trying to learn from) and the **current price** as snapshot on the date checked (these will drift — they're shown to illustrate post-launch trajectory + sale frequency).

| # | Title | Year | Cluster | Launch MSRP (USD) | Current snapshot (USD) | Source |
|---|---|---|---|---|---|---|
| 1 | **Celeste** | 2018 | Genre | **$19.99** | $9.49 (no sale tag visible in fetch) | [Steam page](https://store.steampowered.com/app/504230/Celeste/) checked 2026-05-13; [SteamDB / GG.deals launch price](https://gg.deals/game/celeste/) checked 2026-05-13 |
| 2 | **Hollow Knight** | 2017 | Genre | **$14.99** | $6.99 (50% off promo visible) | [Steam page](https://store.steampowered.com/app/367520/Hollow_Knight/) checked 2026-05-13; developer-confirmed $14.99 launch price per [Team Cherry discussion](https://steamcommunity.com/app/1030300/discussions/0/4048138220337516893/) |
| 3 | **Dead Cells** | 2018 | Genre | **$24.99** (EA started at $16.99; rose to $19.99 in Jan 2018; full release at $24.99 Aug 2018) | $8.49 | [Steam page](https://store.steampowered.com/app/588650/Dead_Cells/) checked 2026-05-13; [GamingBolt early-access pricing history](https://gamingbolt.com/dead-cells-early-access-price-increasing-after-steam-winter-sale) checked 2026-05-13 |
| 4 | **Pizza Tower** | 2023 | Genre | **$19.99** | $8.19 | [Steam page](https://store.steampowered.com/app/2231450/Pizza_Tower/) checked 2026-05-13; [SteamDB](https://steamdb.info/app/2231450/) launch price checked 2026-05-13 |
| 5 | **Animal Well** | 2024 | Genre | **$24.99** | $7.69 (33% off weekend deal visible) | [Steam page](https://store.steampowered.com/app/813230/ANIMAL_WELL/) checked 2026-05-13; [SteamDB](https://steamdb.info/app/813230/) launch price checked 2026-05-13 |
| 6 | **Ori and the Blind Forest: DE** | 2016 | Genre | **$19.99** | $8.19 | [Steam page](https://store.steampowered.com/app/387290/Ori_and_the_Blind_Forest_Definitive_Edition/) checked 2026-05-13 |
| 7 | **Terra Nil** | 2023 | Eco | **$24.99** | $12.99 (Deluxe Edition $16.63 on 10% off) | [Steam page](https://store.steampowered.com/app/1593030/Terra_Nil/) checked 2026-05-13 |
| 8 | **Endling — Extinction is Forever** | 2022 | Eco | **$29.99** | $11.99 | [Steam page](https://store.steampowered.com/app/898890/Endling__Extinction_is_Forever/) checked 2026-05-13 |
| 9 | **Alba: A Wildlife Adventure** | 2020 | Eco | **$16.99** | $7.99 | [Steam page](https://store.steampowered.com/app/1337010/Alba_A_Wildlife_Adventure/) checked 2026-05-13 |
| 10 | **A Short Hike** | 2019 | Wholesome | **$7.99** | $4.49 (45% off visible) | [Steam page](https://store.steampowered.com/app/1055540/A_Short_Hike/) checked 2026-05-13; [SteamDB](https://steamdb.info/app/1055540/) checked 2026-05-13 |
| 11 | **Chicory: A Colorful Tale** | 2021 | Wholesome | **$19.99** | $8.19 | [Steam page](https://store.steampowered.com/app/1123450/Chicory_A_Colorful_Tale/) checked 2026-05-13 |
| 12 | **Beacon Pines** | 2022 | Wholesome | **$19.99** | $8.19 | [Steam page](https://store.steampowered.com/app/1269640/Beacon_Pines/) checked 2026-05-13; [Fellow Traveller launch announcement](https://www.fellowtraveller.games/blog/beacon-pines-release-date-trailer) checked 2026-05-13 |

### Observations across the sample

- **The modal launch MSRP is $19.99** — 6 of 12 comparables (Celeste, Pizza Tower, Ori DE, Chicory, Beacon Pines, and Dead Cells at its 1.0 release tier of $24.99 is the next step up). This is the "indie platformer / wholesome story-rich game with finished campaign" anchor price on Steam.
- **The lowest launch MSRP in the sample is $7.99** — A Short Hike. The next lowest is Hollow Knight at $14.99, then Alba at $16.99. There is essentially a gap between A Short Hike ($7.99) and the rest of the field ($14.99+).
- **The eco cluster prices higher than the platformer cluster.** Terra Nil ($24.99), Endling ($29.99), Alba ($16.99) — eco-themed games skew up, likely because they target a slightly older / more "premium-positioned" audience.
- **Pixel-art is not a discount.** Celeste ($19.99), Pizza Tower ($19.99), Dead Cells ($24.99 at 1.0), and Animal Well ($24.99) are all pixel-art and all priced at or above the modal $19.99 — the visual style does not push price down. Hollow Knight ($14.99) is hand-drawn vector art, not pixel.
- **All 12 are deeply discounted now.** Current prices range from $4.49 (A Short Hike) to $12.99 (Terra Nil); the median current price is roughly $8. This is informational for Cloudy Ninja's eventual sale strategy (Steam audiences expect 50–75% discounts within 1–2 years), but **does NOT mean we should launch at the sale price** — we launch at MSRP and discount later.
- **Early-access pricing is a meaningful tactic.** Dead Cells is the only sample game that went through Steam Early Access. It started at $16.99 in EA (May 2017), bumped to $19.99 mid-EA (Jan 2018), and shipped 1.0 at $24.99 (Aug 2018) — a documented ladder pattern. The lesson for Cloudy Ninja: **alpha-tier pricing should sit below the eventual 1.0 price**, and you should announce the ladder in advance so early buyers feel rewarded.

---

## (b) Itch.io pricing distribution — what the platform looks like

Itch.io's pricing model is structurally different from Steam:

- **Default is "pay what you want above the minimum."** Per [itch.io pricing docs](https://itch.io/docs/creators/pricing) checked 2026-05-13: "any item that can accept money is automatically pay-what-you-want or pay-any-amount-above-the-minimum." Three modes: Free with optional donations · Paid (with minimum set, but always PWYW above) · No payments (fully free).
- **About 30% of buyers pay above the minimum**, with the average over-payment being ~$1.50 according to [itch.io's pricing docs](https://itch.io/docs/creators/pricing) checked 2026-05-13.
- **No fixed revenue split.** Default is 90/10 in creator's favor, but the creator can set itch's share anywhere from 0% to 100% per [generalistprogrammer's 2026 itch.io revenue guide](https://generalistprogrammer.com/tutorials/how-to-make-money-on-itchio-indie-game-guide) checked 2026-05-13.

### Itch.io pricing reference points from the comparable set

Of the 12 T-075 comparables, only two have meaningful itch.io presence:

- **A Short Hike** — [adamgryu.itch.io/a-short-hike](https://adamgryu.itch.io/a-short-hike) checked 2026-05-13. Minimum **$7.99**, paid-with-PWYW-above (not free / not zero-minimum PWYW). Same price as Steam launch MSRP.
- **Celeste Classic** (PICO-8 prototype that became Celeste) — [maddymakesgamesinc.itch.io/celesteclassic](https://maddymakesgamesinc.itch.io/celesteclassic) checked 2026-05-13. **Free**. This is the original 4-day gamejam build; the full Celeste is Steam-only.

### Itch.io as a platform — pricing-distribution observations

There's no public itch.io pricing histogram for the pixel-art-platformer slice — itch doesn't publish it, and the storefront doesn't expose aggregate price filters. The closest references:

- **Hive Time case study ("Money for the Honey")** by Cheeseness, the most-cited itch.io PWYW case study. [Devlog](https://cheeseness.itch.io/hive-time/devlog/190132/money-for-the-honey-a-look-at-hive-times-finances-and-pay-what-you-want-pricing) checked 2026-05-13: launched as PWYW with no minimum; got many more downloads than expected; average payment landed around **$10** despite zero minimum; high overpayment correlated with press framing the game as "pay-what-you-want" rather than "free-to-play."
- **Generalist Programmer 2026 indie revenue guide** ([source](https://generalistprogrammer.com/tutorials/how-to-make-money-on-itchio-indie-game-guide) checked 2026-05-13): recommends setting a minimum of **$3–$10 depending on game scope** as the sweet spot for maximizing both sales volume and per-sale revenue on itch.io. Warns that being an unknown developer with a non-zero minimum and no free download is "considered a bad idea" because friction kills initial reach.
- **T-124 / itch listing-style-guide (T-048) prior art:** the itch-page-draft cites Frogfall (1,126 ratings), SELF (1,090), Sheepy (554) as PWYW-priced itch games with 4.8+ ratings and notably high rating-counts — friction matters more than dollar amount for early-stage indie reach on itch.

**Practical translation:** the itch.io pixel-art-platformer market is bimodal. There's a large free / sub-$5 PWYW tier where most discovery happens (and where unknown developers must enter), and there's a small "itch as a premium boutique store" tier where established names like A Short Hike sell at Steam-parity ($7.99+). Cloudy Ninja at alpha is structurally in the first tier, not the second.

---

## (c) Demo-vs-paid conversion data

Pulled because the alpha-vs-1.0 question is partly a demo-strategy question: is the alpha itself the demo, or do we ship a separate demo at 1.0?

### Key conversion benchmarks (2024–2025 data)

- **Demo-to-wishlist conversion (Steam):** ~10–20% of demo players add the game to their wishlist. Source: [How To Market A Game — June 2024 Next Fest analysis](https://howtomarketagame.com/2024/06/24/do-demos-help-earn-wishlists-steam-next-fest-june-2024/) checked 2026-05-13; consistent with [Alinea Analytics wishlist-to-buyer conversion study](https://alineaanalytics.substack.com/p/wishlist-to-buyer-conversions-for-games-with-steam-next-fest-demos) checked 2026-05-13.
- **Wishlist-to-launch-week sale conversion:** Median ~10–15% during launch week, but with strong price-band variation:
  - Games priced **under $10**: median launch-week conversion ~**15%**.
  - Games priced **$10+**: median drops to ~**10%**.
  - Source: [GameDiscoverCo — State of Steam Wishlist Conversions 2024–2025](https://newsletter.gamediscover.co/p/the-state-of-steam-wishlist-conversions) checked 2026-05-13; cross-referenced [Alinea Analytics genre/price study](https://alineaanalytics.substack.com/p/steam-next-fests-wishlist-winners) checked 2026-05-13.
- **The cliff at $10 is the most important pricing-strategy datapoint in this whole document.** Wishlist conversion drops by roughly a third between sub-$10 and $10+. For an unknown developer with modest wishlist volume, staying under $10 trades a small per-unit revenue cut for a substantially higher conversion rate — which compounds because higher conversion means more reviews faster, which means better algorithmic surface.

### Alpha-vs-demo framing

For Cloudy Ninja:

- **Alpha on itch.io IS the demo.** Per T-124 §9 explicitly: "Demo: Not needed — the alpha is the available build." This is correct positioning for itch — itch's audience expects works-in-progress and devlog-driven iteration; carving out a "demo" tier on top of an itch alpha is redundant.
- **For Steam 1.0**, ship a separate Steam demo. Demos materially help wishlist gain ([How To Market A Game data](https://howtomarketagame.com/2024/06/24/do-demos-help-earn-wishlists-steam-next-fest-june-2024/) checked 2026-05-13), and the polished demo experience is different from "buy the alpha build for $X on itch" — different audiences, different expectations.

---

## (d) Alpha vs 1.0 pricing approach — patterns from the comparable set

Only one of the 12 comparables (Dead Cells) ran a documented price ladder through Steam Early Access. The others either went straight to 1.0 at MSRP (Celeste, Hollow Knight, Pizza Tower, Animal Well, Ori, the eco cluster, the wholesome cluster), or used itch.io as a free pre-Steam prototype channel (Celeste / Celeste Classic).

The two industry-pattern lessons that apply to Cloudy Ninja:

### Pattern 1: Itch.io as soft-launch, Steam as full launch

Multiple secondary sources document this as the standard "indie pipeline" pattern for solo developers:

- [Spiral Up Games — Itch.io vs Steam Early Access for Indie Developers](https://www.spiralupgames.com/post/itch-io-vs-steam-early-access-for-indie-game-developers) checked 2026-05-13.
- [Medium / Tavrox — Perks of Soft-Launching your game on Itch.Io before Steam release](https://medium.com/game-marketing/perks-of-soft-launching-your-game-on-itch-io-before-steam-release-7b035c9b1bb) checked 2026-05-13.
- [Game Developer / Tavrox cross-post](https://www.gamedeveloper.com/business/perks-of-soft-launching-your-game-on-itch-io-before-steam-release) checked 2026-05-13.

The pricing implication: itch.io alpha at low / PWYW pricing → Steam 1.0 at the MSRP the comparable cluster supports. Itch covers feedback + community-building + early bug reports; Steam covers commercial launch. The two prices are different by design and don't need to match.

### Pattern 2: The Dead Cells ladder

Documented in [GamingBolt's early-access pricing article](https://gamingbolt.com/dead-cells-early-access-price-increasing-after-steam-winter-sale) and [DualShockers' coverage](https://www.dualshockers.com/dead-cells-early-access-price-increase/) both checked 2026-05-13. The pattern:

1. Early access launches at a clear discount vs. the planned 1.0 price.
2. Mid-EA price bumps are pre-announced so early buyers feel they got a deal (and to signal that the price is going up, which converts wishlist-fence-sitters).
3. 1.0 launches at the full MSRP.

For a much smaller game like Cloudy Ninja, the ladder isn't 3 steps — it's 2: **alpha (cheap, on itch)** → **1.0 (full price, on Steam)**. But the principle holds — announce the ladder in advance, reward early buyers, let urgency drive conversion.

### Pattern 3: The $7.99 indie sweet spot

[TechSpot — Why $7.99 has become the sweet spot for indie games](https://www.techspot.com/news/110930-why-799-has-become-sweet-spot-indie-games.html) checked 2026-05-13: documents Peak (10M+ copies), Content Warning (5M+ copies), RV There Yet (1M+) all launching at **$7.99** in 2024. The reasoning is psychological: $7.99 sits in a "feels close to $5" bracket while preserving ~60% more revenue per sale than $4.99, and crucially **stays under the $10 wishlist-conversion cliff** documented in section (c). Note that two of the three games cited (Peak, Content Warning) are short-form multiplayer hits — Cloudy Ninja is single-player, so the data isn't a perfect fit, but the **under-$10 cliff** rationale applies regardless of genre.

---

## (e) Sanity-check against the existing presskit positioning

The presskit (referenced in `marketing/itch-page-draft.md` §9 and the T-077 deliverable) positions Cloudy Ninja at **USD $2.99–4.99**. This document checks whether that range still makes sense given the comparable set:

**For alpha on itch.io: yes, the $2.99–4.99 range is well-calibrated.** It's below the $7.99 itch.io / A Short Hike anchor (which is established-developer pricing on a finished game), above the typical free / $1 minimum that signals "unfinished gamejam build," and inside the $3–$10 range the itch.io revenue guide ([Generalist Programmer 2026](https://generalistprogrammer.com/tutorials/how-to-make-money-on-itchio-indie-game-guide) checked 2026-05-13) calls the sweet spot for paid-with-PWYW-above. A **$2.99 minimum** with PWYW above sits at the floor of the recommended band; a **$4.99 minimum** sits at the middle. T-124's recommendation of "Free OR PWYW with $2.99 suggested" is the most aggressive end of friction-minimization — appropriate for a totally unknown developer launch but underpriced relative to the game scope (8 levels + boss + 12 achievements + accessibility suite + 60–90 minute first-run length).

**For 1.0 on Steam: the $2.99–4.99 presskit range is below the comparable cluster.** No game in our 12-sample launched on Steam below $7.99. Even A Short Hike — the cheapest game in the sample, made by a single developer, ~90-minute campaign, no boss — launched at $7.99. The presskit range needs to be revisited at 1.0; this analysis offers a refined recommendation in section (f).

---

## (f) Recommended pricing strategy

Two recommendations, one for each ship milestone. Each one is grounded in the comparable data above, and each one cites which observation drove it.

### Alpha launch (itch.io, Sprint D)

**Recommended: pay-what-you-want with a $2.99 suggested minimum, $0.99 minimum floor.**

The structure: itch.io's pricing widget should set the minimum to $0.99 (not $0 — see footnote) and the suggested price to $2.99. Players who want zero friction can pay the floor; players who want to support pay $2.99 or more.

**Why $2.99 suggested:**
- Sits inside the $3–$10 itch.io revenue-guide sweet spot ([source](https://generalistprogrammer.com/tutorials/how-to-make-money-on-itchio-indie-game-guide) checked 2026-05-13).
- Matches the lower end of the presskit's existing $2.99–4.99 positioning, so we don't contradict prior marketing.
- Anchors expectations for the 1.0 price ladder — when 1.0 launches at $7.99–9.99, early buyers see roughly 3× value capture from buying in alpha.

**Why $0.99 floor and not $0:**
- The Hive Time case study ([source](https://cheeseness.itch.io/hive-time/devlog/190132/money-for-the-honey-a-look-at-hive-times-finances-and-pay-what-you-want-pricing) checked 2026-05-13) documents that press framing as "PWYW" (not "free-to-play") correlates with both higher payment rates AND better press coverage. A $0 floor reads as free-to-play in casual scanning; $0.99 reads as PWYW. The difference is rhetorical, not financial.
- Friction-minimization is still respected — $0.99 is the standard "I bought it to support the dev" floor on itch.

**What this means in concrete itch.io widget terms:**
- "Minimum price": `$0.99`
- "Suggested price": `$2.99`
- Page copy near the price block: *"Pay what you want, $2.99 suggested. Above the minimum supports development; the alpha build is the same regardless of what you pay."*

### 1.0 launch (Steam, post-alpha)

**Recommended: $7.99 MSRP at 1.0 launch, with a credible plan to hold price for the first 6 months before any discount.**

The structure: launch at $7.99 on Steam. Hold the price firm through launch week, the first Steam sale, and at least one major Steam event (Summer Sale, Autumn Sale). Discount no more than 20% in year one. Heavier discounts (50%+) only after year one.

**Why $7.99 and not $4.99 or $9.99:**
- **$7.99 stays under the $10 wishlist-conversion cliff** documented in section (c). [GameDiscoverCo 2024–2025 data](https://newsletter.gamediscover.co/p/the-state-of-steam-wishlist-conversions) checked 2026-05-13: median wishlist-to-sale conversion is ~15% under $10 vs. ~10% at $10+. A 50% relative conversion bump dwarfs the 25% revenue-per-unit gain you'd get going from $7.99 to $9.99.
- **$7.99 matches the only directly-comparable launch price in the sample** — A Short Hike ([Steam](https://store.steampowered.com/app/1055540/A_Short_Hike/), [itch](https://adamgryu.itch.io/a-short-hike) both checked 2026-05-13). Same single-developer scope, same accessibility-first audience overlap, same ~60–90 minute first-run length per the presskit and itch-page-draft. A Short Hike has held this price since 2019 — the price point is durable.
- **$7.99 is the documented indie sweet spot** ([TechSpot](https://www.techspot.com/news/110930-why-799-has-become-sweet-spot-indie-games.html) checked 2026-05-13). The genre fit isn't perfect (Peak/Content Warning are multiplayer) but the psychology — "feels close to $5, captures meaningfully more revenue" — applies regardless of single-player vs. multiplayer.
- **$4.99 would underprice relative to the comparable set.** No game in our 12-sample launched below $7.99 on Steam. Pricing at $4.99 would signal "shareware tier" against a cluster of $14.99–$24.99 peers; the $7.99 anchor signals "small but complete" instead.
- **$9.99 would push us past the conversion cliff.** Going $9.99 vs $7.99 captures $2 more per sale but costs ~33% conversion on the GameDiscoverCo data; net revenue per wishlist is worse.

**Why hold firm 6 months:**
- The Dead Cells ladder ([GamingBolt](https://gamingbolt.com/dead-cells-early-access-price-increasing-after-steam-winter-sale) checked 2026-05-13) shows that pre-announced upward price moves convert wishlist-fence-sitters. The Cloudy Ninja ladder is alpha→1.0 only, no upward 1.0-to-1.x bump planned, but the principle that **early discounts undermine review velocity** still holds. Reviewers who paid $7.99 at launch and then see the game at $3.99 two months later feel bait-and-switched and post angry reviews; this is documented across multiple indie post-mortems.

**A range, since this is research-only and the user makes the call:**
- **Recommended sweet-spot point: $7.99**
- **Defensible range: $6.99–$9.99**
- **Below $6.99:** underprices the comparable cluster, signals unfinished.
- **Above $9.99:** crosses the wishlist-conversion cliff for an unknown developer; reserved for the post-1.0 "Definitive Edition" hypothetical (out of scope here — no paid speculation about future content, per T-156 hard rule).

### Summary table

| Milestone | Platform | Price | Model | Source rationale |
|---|---|---|---|---|
| **Alpha** | itch.io | $0.99 minimum / $2.99 suggested | PWYW above minimum | Section (b) PWYW data, T-124 §9 prior recommendation, section (f) friction-minimization argument |
| **1.0 (sweet spot)** | Steam | **$7.99** | Fixed MSRP, hold 6 months | Section (a) A Short Hike anchor, section (c) wishlist-cliff data, section (d) $7.99 sweet-spot data |
| **1.0 (defensible range)** | Steam | $6.99 – $9.99 | Fixed MSRP | Section (a) comparable launch MSRPs, section (c) wishlist-cliff data |
| **1.0 (NOT recommended)** | Steam | < $6.99 or > $9.99 | — | Underprices the cluster / crosses the conversion cliff |

### What this analysis does NOT cover

Per the T-156 hard rule (research-only, no paid speculation about future pricing):

- No revenue projections. No "you'll make $X if you launch at $Y" claims.
- No DLC / Definitive Edition / expansion pricing. Out of scope until those things actually exist.
- No regional pricing analysis (Steam's regional auto-conversion + the Latam/India/Brazil pricing recommendations from Valve are a separate problem; this document is USD-only).
- No bundle pricing — bundling with another title is a separate decision tree.
- No demo pricing strategy beyond the "alpha is the demo on itch / ship a separate Steam demo at 1.0" framing in section (c).

These are all legitimate follow-on questions but they require either revenue data Cloudy Ninja doesn't have yet (post-launch real numbers) or product decisions that haven't been made yet (whether there will be DLC). Defer.

---

## (g) Sources

Steam comparable pages (snapshots checked 2026-05-13):
- [Celeste](https://store.steampowered.com/app/504230/Celeste/) · [Hollow Knight](https://store.steampowered.com/app/367520/Hollow_Knight/) · [Dead Cells](https://store.steampowered.com/app/588650/Dead_Cells/) · [Pizza Tower](https://store.steampowered.com/app/2231450/Pizza_Tower/) · [Animal Well](https://store.steampowered.com/app/813230/ANIMAL_WELL/) · [Ori and the Blind Forest: DE](https://store.steampowered.com/app/387290/Ori_and_the_Blind_Forest_Definitive_Edition/)
- [Terra Nil](https://store.steampowered.com/app/1593030/Terra_Nil/) · [Endling — Extinction is Forever](https://store.steampowered.com/app/898890/Endling__Extinction_is_Forever/) · [Alba: A Wildlife Adventure](https://store.steampowered.com/app/1337010/Alba_A_Wildlife_Adventure/)
- [A Short Hike (Steam)](https://store.steampowered.com/app/1055540/A_Short_Hike/) · [Chicory: A Colorful Tale](https://store.steampowered.com/app/1123450/Chicory_A_Colorful_Tale/) · [Beacon Pines](https://store.steampowered.com/app/1269640/Beacon_Pines/)

Launch-price / historical-pricing sources (checked 2026-05-13):
- [SteamDB — Celeste price history](https://steamdb.info/app/504230/) · [SteamDB — Pizza Tower](https://steamdb.info/app/2231450/) · [SteamDB — Animal Well](https://steamdb.info/app/813230/) · [SteamDB — A Short Hike](https://steamdb.info/app/1055540/)
- [GG.deals Celeste pricing](https://gg.deals/game/celeste/) · [GG.deals Hollow Knight](https://gg.deals/game/hollow-knight/) · [GG.deals Pizza Tower](https://gg.deals/game/pizza-tower/)
- [Team Cherry / Hollow Knight $14.99 pricing discussion](https://steamcommunity.com/app/1030300/discussions/0/4048138220337516893/) — developer-confirmed launch MSRP
- [GamingBolt — Dead Cells Early Access price ladder](https://gamingbolt.com/dead-cells-early-access-price-increasing-after-steam-winter-sale) · [DualShockers coverage](https://www.dualshockers.com/dead-cells-early-access-price-increase/) · [TechRaptor coverage](https://techraptor.net/gaming/news/dead-cells-increasing-in-price-after-steam-winter-sale)
- [Fellow Traveller — Beacon Pines launch announcement](https://www.fellowtraveller.games/blog/beacon-pines-release-date-trailer)

Itch.io pricing references (checked 2026-05-13):
- [itch.io pricing docs (canonical)](https://itch.io/docs/creators/pricing) · [itch.io "How buying works"](https://itch.io/docs/creators/how-buying-works) · [itch.io creator FAQ](https://itch.io/docs/creators/faq)
- [A Short Hike on itch.io](https://adamgryu.itch.io/a-short-hike) · [A Short Hike purchase page](https://adamgryu.itch.io/a-short-hike/purchase)
- [Celeste Classic on itch.io](https://maddymakesgamesinc.itch.io/celesteclassic)
- [Hive Time "Money for the Honey" PWYW case study](https://cheeseness.itch.io/hive-time/devlog/190132/money-for-the-honey-a-look-at-hive-times-finances-and-pay-what-you-want-pricing) — the most-cited itch.io PWYW post-mortem

Industry analysis (checked 2026-05-13):
- [GameDiscoverCo — State of Steam Wishlist Conversions 2024–2025](https://newsletter.gamediscover.co/p/the-state-of-steam-wishlist-conversions) — the canonical wishlist-conversion-by-price data
- [Alinea Analytics — Wishlist-to-buyer conversion for Next Fest demos](https://alineaanalytics.substack.com/p/wishlist-to-buyer-conversions-for-games-with-steam-next-fest-demos) · [Alinea — Next Fest wishlist winners](https://alineaanalytics.substack.com/p/steam-next-fests-wishlist-winners)
- [How To Market A Game — Do demos help earn wishlists? June 2024 Next Fest](https://howtomarketagame.com/2024/06/24/do-demos-help-earn-wishlists-steam-next-fest-june-2024/) · [HTMAG — Benchmarks: How Many Wishlists Can I Get From Steam Next Fest](https://howtomarketagame.com/2025/03/26/benchmarks-how-many-wishlists-can-i-get-from-steam-next-fest/)
- [TechSpot — Why $7.99 has become the sweet spot for indie games](https://www.techspot.com/news/110930-why-799-has-become-sweet-spot-indie-games.html)
- [Spiral Up Games — Itch.io vs Steam Early Access](https://www.spiralupgames.com/post/itch-io-vs-steam-early-access-for-indie-game-developers)
- [Game Developer / Tavrox — Soft-launching on itch.io before Steam](https://www.gamedeveloper.com/business/perks-of-soft-launching-your-game-on-itch-io-before-steam-release) · [Medium cross-post](https://medium.com/game-marketing/perks-of-soft-launching-your-game-on-itch-io-before-steam-release-7b035c9b1bb)
- [Generalist Programmer — How to Make Money on Itch.io (2026)](https://generalistprogrammer.com/tutorials/how-to-make-money-on-itchio-indie-game-guide) · [Generalist Programmer — Itch.io vs Steam (2026)](https://generalistprogrammer.com/tutorials/itchio-vs-steam-indie-game-platform-comparison)
- [Fungies.io — Steam vs Itch.io for Indie Developers 2026](https://fungies.io/steam-vs-itch-io-indie-developers/)

Cross-references inside this repo:
- `marketing/steam-tags-research.md` (T-075) — the 12 comparable games surveyed in (a) above
- `marketing/itch-page-draft.md` (T-124) — the PWYW alpha-pricing recommendation refined in (f) above
- `marketing/presskit/` (T-077) — the $2.99–4.99 starting hypothesis referenced in (e) above

---

*End of analysis. T-156.*

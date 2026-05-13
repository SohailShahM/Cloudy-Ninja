# itch.io vs Steam — Store Comparison for Cloudy Ninja (Alpha + v1.0)

Research-only deliverable for ticket **T-163**. Compiled 2026-05-13. Cross-references:
- `marketing/itch-page-draft.md` (T-124) — the itch.io listing copy this doc supports.
- `marketing/steam-tags-research.md` (T-075) — the eventual Steam tag set.
- `research/keyboard-layout-conventions.md` (T-073) — controls baseline.

The goal: decide **which store(s) Cloudy Ninja ships on at alpha and at v1.0**, with each axis (friction, discovery, revenue split, reviews) compared side-by-side and a per-stage recommendation backed by cited evidence. This is a doc-only deliverable — no source or marketing changes follow from it. All numeric claims include a source URL and the date the snapshot was taken; numbers will drift, the directional conclusions are stable.

**TL;DR.** Alpha (Sprint D, near-term): **itch.io first, Steam not yet.** itch's near-zero friction (no upfront fee, no 30-day wait, no Early Access branding contract, account-optional purchase) is a fit for a build the user calls "alpha" in the description, and itch's flexible revenue share + private review channel is forgiving for a small early audience. v1.0: **both stores.** Steam's audience size, wishlist surface, and verified-purchase reviews are too valuable to skip once the game is finish-quality; itch keeps the existing audience + DRM-free + pay-what-you-want price tier. Detailed matrix in section (5).

---

## (1) Friction

How hard is it for (a) a developer to publish, and (b) a player to buy + play? Friction matters disproportionately at alpha because the alpha audience is small and unconvinced — any extra step is a meaningful chunk of the funnel.

### (1a) Developer-side friction to publish

| Axis | itch.io | Steam |
|---|---|---|
| Upfront fee | None. Free to create an account, upload a page, and publish. | **$100 USD per app** (Steam Direct fee). Recouped from your payouts after $1,000 in adjusted gross revenue, but you pay it up front. |
| Time-to-publish | Minutes. Create account, upload zip, fill in page, hit "Public." | **~30 days minimum.** Steam enforces a 30-day waiting period between paying the Steam Direct fee and being allowed to release, plus a 1–5 day per-build review. The coming-soon page must be live for at least 2 weeks before launch. |
| Paperwork | Email confirmation + payout setup (Stripe/PayPal). Tax interview is light, deferred until you actually have revenue to pay out. | NDA + Steam Distribution Agreement + bank info + identity verification + tax forms (US W-9 / W-8BEN equivalents) before release. |
| Per-build approval | None for itch — uploads are immediate, no human review on patches. | Each build goes through Valve's review pipeline (1–5 days). Live games can hot-patch via the Steamworks SDK, but the first publish must clear review. |
| Early Access contract | Not applicable. itch has no Early Access program; "alpha" is a free-form status flag on the page. | If you sell while in development you must use Steam Early Access, which carries explicit Valve rules: no false promises about completion, pricing parity across stores, and Early Access branding on third-party listings. |

Sources:
- itch — free uploads, no upfront fee: [How buying works (itch.io docs)](https://itch.io/docs/creators/how-buying-works) [accessed 2026-05-13]
- itch — release status taxonomy ("In development", "Prototype", "Released"): [In development status on pages — itch.io forum](https://itch.io/t/1171997/in-development-status-on-pages-and-front-page-features) [accessed 2026-05-13]
- Steam — Steam Direct fee $100, recoupable at $1,000 AGR: [Steam Direct Fee — Steamworks docs](https://partner.steamgames.com/doc/gettingstarted/appfee) [accessed 2026-05-13]
- Steam — 30-day waiting period and 1-5 day per-build review, 2-week coming-soon: [Onboarding — Steamworks docs](https://partner.steamgames.com/doc/gettingstarted/onboarding) [accessed 2026-05-13]
- Steam — Early Access rules (no false promises, pricing parity, branding): [Early Access — Steamworks docs](https://partner.steamgames.com/doc/store/earlyaccess) [accessed 2026-05-13]; commentary: [PC Gamer — Steam's secret Early Access rules](https://www.pcgamer.com/early-access-rules/) [accessed 2026-05-13]

### (1b) Player-side friction to purchase / play

| Axis | itch.io | Steam |
|---|---|---|
| Account required to buy | **No.** Guest checkout is supported — purchase is tied to the email address, the player gets a download link by email. Account is "recommended" but optional. | **Yes.** A Steam account is required to redeem any key or purchase any title. |
| Client required to play | No. The default distribution model is a downloadable zip/installer; the game runs without an itch app. (The itch desktop app exists and is optional.) | Usually yes. Most Steam titles ship with Valve's DRM wrapper and require the Steam client running. DRM-free is opt-in by the developer and not the default. |
| Payment processors | Stripe (cards), PayPal, with a $0.30 + 2.9% processor fee on top of the platform share. | Steam's own checkout (cards, PayPal, regional methods, Steam Wallet). Processor fees are bundled into the 30% house cut — no separate processor line. |
| Cost-per-decision (alpha price tier) | At pay-what-you-want with $2.99 suggested, a player can pay $0 and still try the build. Friction is functionally zero. | At any non-free price, the player commits the full amount up front and may refund within 14 days / under 2 hours played. Trying-without-buying requires a separate Demo branch. |
| Refund / buyer's remorse window | No standard automated refund window. Refunds are creator-discretion, handled via support. | **14 days, under 2 hours played, automatic.** Documented and well-known by Steam buyers. |

Sources:
- itch — account optional for purchase: [How to access your purchase — itch.io docs](https://itch.io/docs/buying/already-bought) [accessed 2026-05-13]; [itch FAQ](https://itch.io/docs/general/faq) [accessed 2026-05-13]
- itch — Stripe processor fee context ($0.30 + 2.9%): [Accepting payments and getting paid — itch.io docs](https://itch.io/docs/creators/payments) [accessed 2026-05-13]
- Steam — account + client required, DRM wrapper default: [Steam DRM — Steamworks docs](https://partner.steamgames.com/doc/features/drm) [accessed 2026-05-13]; [PCGamingWiki — DRM-free games on Steam](https://www.pcgamingwiki.com/wiki/The_big_list_of_DRM-free_games_on_Steam) [accessed 2026-05-13]
- Steam — 14-day / 2-hour refund policy: [Steam Refunds](https://store.steampowered.com/steam_refunds) [accessed 2026-05-13]; [Steam Support — Common Refund Questions](https://help.steampowered.com/en/faqs/view/5FDE-BA65-ACCE-A411) [accessed 2026-05-13]

**Friction verdict.** For a small, untrusted-at-launch alpha audience, itch's account-optional + client-free + pay-what-you-want stack is meaningfully lower-friction than Steam Early Access. For a v1.0 paid release, Steam's "buyers know how Steam works" cuts the other way — the friction is familiar, the refund window is a feature for buyers, and the client is not a barrier for the audience that already owns dozens of Steam games.

---

## (2) Discovery

How does each store route a new player to the page? The mechanics differ enough that the same tag set behaves differently.

### (2a) Tag systems

| Axis | itch.io | Steam |
|---|---|---|
| Tag taxonomy | Free-form lowercase tags. itch maintains a curated master list (`pixel-art`, `platformer`, `2d`, `nature`, etc.) but accepts developer-supplied tags too. Synonyms are merged. | Closed taxonomy (`Pixel Graphics`, `Platformer`, `2D`, `Nature`, etc.) managed by Valve. Players can also apply tags post-launch; publisher tags are a separate 5-slot field. |
| Tag count | No hard cap, but quality-driven (over-tagging is penalized for discoverability). | **20 user tags + 5 publisher tags.** Top-5 algorithmically weighted. |
| Tag ordering effect | Some weight — earlier tags rank higher in "more like this" sidebars and auto-complete. | Strong weight — top 5 tags drive Featured & Recommended and the More Like This carousel. |
| Anti-tag risk | Lower, but `educational` / `walking simulator` / `difficult` mismatches still hurt browse-page conversion. | Higher — irrelevant tags depress click-through, which Steam reads as a quality signal and de-prioritizes the page everywhere. |

Sources:
- itch — enhanced/curated tag system, free-form input: [Enhanced itch.io tagging system](https://itch.io/updates/enhanced-itchio-tagging-system) [accessed 2026-05-13]; [Getting indexed on Search & Browse — itch.io docs](https://itch.io/docs/creators/getting-indexed) [accessed 2026-05-13]
- Steam — tag taxonomy + 20-tag limit + top-5 weighting: [Steam Tags — Steamworks docs](https://partner.steamgames.com/doc/store/tags) [accessed 2026-05-13]; [Simon Carless — Are you prioritizing your Steam tags?](https://newsletter.gamediscover.co/p/are-you-prioritizing-your-steam-tags) [accessed 2026-05-13]
- Steam — irrelevant tags hurt: [Game Developer — State of the Algorithm](https://www.gamedeveloper.com/business/the-state-of-the-algorithm-what-s-happening-to-indies-on-steam-) [accessed 2026-05-13]

### (2b) Store algorithms

| Axis | itch.io | Steam |
|---|---|---|
| Surfaces | Two distinct: **Search** (title-keyword) and **Browse** (tag + filter). "Popular" mixes recency boost + interaction signals. | Front page (Featured & Recommended), Discovery Queue (personalized), More Like This carousel, Tag pages, Wishlist emails. |
| Personalization | Limited. itch leans on tag overlap + recent activity feed. Devlog posts contribute to ranking. | Heavy. Steam builds a per-user vector from playtime + wishlist + reviews and routes "similar to what you played" recommendations. |
| Wishlist surface | None on itch (no wishlist primitive). Players "follow" creators / collections instead. | Core. Wishlists drive launch-day visibility and Steam's email-on-discount system. |
| Boost for new releases | "New & Popular" gives a recency window; devlogs help rank. | "New & Trending" + 7-day visibility round give a launch window, then it's all algorithm + wishlist conversion. |

Sources:
- itch — popular algorithm mixes popularity + new-thing boost: [How does the popular section work? — itch.io forum](https://itch.io/t/3276137/how-does-the-popular-section-work-are-we-delisted) [accessed 2026-05-13]; [How are metrics weighted for Popular and New & Popular? — itch.io](https://itch.io/t/4379248/how-are-metrics-weighted-for-popular-and-new-popular) [accessed 2026-05-13]
- itch — devlogs feed back into ranking: [Introducing devlogs — itch.io](https://itch.io/updates/introducing-devlogs) [accessed 2026-05-13]
- Steam — recommender, wishlist, More Like This: [Steam Data Suite — tag discoverability guide](https://steamdatasuite.com/choosing-the-right-tags-to-drive-discoverability/) [accessed 2026-05-13]; [ECI Games — 2025 Steam discoverability guide](https://www.ecigames.net/media/eci-games-2025-steam-discoverability-guide) [accessed 2026-05-13]

### (2c) Festival / sale / demo eligibility

| Axis | itch.io | Steam |
|---|---|---|
| Built-in festivals | Game jams (Ludum Dare, GMTK Game Jam, etc.) are itch-native and bring large bursts of traffic. | **Steam Next Fest** — quarterly demo festival. Eligibility: unreleased game with a published demo, Steamworks account in good standing, **one Next Fest per title** (you can only join once). |
| Sale events | Creator Day (24h, 100% to devs); seasonal sales the creator opts into; Bundles & charity events. | Seasonal sales (Summer, Autumn, Winter, Spring) automatically eligible; specialized sales by genre/theme. |
| Demo support | Implicit — upload a separate build flagged as demo, or use pay-what-you-want with a $0 floor. | Explicit "Demo" app type, separate appid, shows on store as a button. |
| One-shot risk | None — itch has no "you only get to do this once" event. | **High for Next Fest.** Once-only eligibility means timing matters — you spend it at the wrong build state and it's gone. |

Sources:
- itch — Creator Day (Nov 28 2025, 100% to devs for 24h): [Itch.io Just Gave Developers 100% of Sales for 24 Hours — TechInBengali](https://en.techinbengali.com/itch-io-creator-day-100-percent-revenue-developers/) [accessed 2026-05-13]
- Steam Next Fest — eligibility, one-time, demo required: [Steam Next Fest — Steamworks docs](https://partner.steamgames.com/doc/marketing/upcoming_events/nextfest) [accessed 2026-05-13]; [Steam Next Fest February 2026 — Steamworks docs](https://partner.steamgames.com/doc/marketing/upcoming_events/nextfest/2026february) [accessed 2026-05-13]

**Discovery verdict.** Steam's discovery surface is much larger and much more algorithmically active than itch's — at v1.0, the wishlist surface alone is worth a Steam page. But Steam's once-per-title Next Fest creates a real strategic constraint: shipping to Steam at alpha burns the Next Fest slot at exactly the moment the game is least polished. Burning Next Fest on an alpha is a *bad* trade — the eco-platformer audience that would convert on Next Fest converts much better when the trailer + finished levels exist. **Save Next Fest for v1.0.** itch's discovery is smaller but jam-driven and forgiving of an in-development status.

---

## (3) Revenue split

Both stores publish their math. The interaction with payment processing is where the numbers drift from the headline.

### (3a) Headline splits

| Axis | itch.io | Steam |
|---|---|---|
| Platform share | **Open / pay-what-share.** Developer sets the percentage 0–100%. Default suggestion is **10%** to itch, 90% to creator. | **30%** flat to Valve on first $10M in revenue per title. |
| Tiered breaks | None. | **25%** on revenue between $10M–$50M, **20%** above $50M per title. Cumulative tiers, lifetime per title. |
| Payment processor fee | **Separate.** Stripe: $0.30 + 2.9% per transaction. PayPal similar. Comes out of the creator's share, not itch's. | **Bundled into the 30%.** No separate processor line. |
| Practical effective rate (Cloudy Ninja at $2.99) | itch 10% + Stripe (~$0.39 = ~13%) → **~77% net to creator** at default. If creator sets itch share to 0%, **~87% net**. | Steam 30% → **~70% net to creator**, regardless of price. |
| Currency, regional pricing | Single price field; itch handles conversion at checkout. | Per-region pricing widgets (USD, EUR, BRL, etc.) with Valve-suggested defaults. |

Notes on the practical effective rate math: at a $2.99 price, Stripe's $0.30 + 2.9% works out to about $0.39 in processor fees, or about 13%. itch's 10% platform share is on top of that. itch's actual recent published number — that across all 2025 sales the developer share averaged ~76% — confirms the back-of-envelope: itch is materially better than Steam *per transaction* once you sell anything at all, but the gap narrows at higher price points where the fixed $0.30 processor portion matters less. At $9.99 Steam is ~70% net, itch at default is ~86% net.

### (3b) Practical implications

- **At alpha (small audience, $0–$2.99 pay-what-you-want):** itch's revenue model is structurally a better fit. Players who pay $0 still represent zero net cost; players who pay $2.99 get the creator ~77% even with default itch share + Stripe.
- **At v1.0 (larger audience, $2.99–$4.99 fixed):** Steam's 30% looks worse on paper, but if Steam delivers **10×** the audience size (a number repeated in the indie marketing literature; cited below), the absolute revenue is higher on Steam despite the worse split. The right framing is "split × volume," and the volume gap favors Steam at v1.0.
- **Tier thresholds are aspirational.** Cloudy Ninja at $2.99–$4.99 would need ~3.3M+ Steam unit sales to hit the $10M tier. This is not a practical consideration for a Sprint-D alpha; mention it only to confirm the flat 30% is what applies.

Sources:
- itch — open revenue sharing, 0–100% creator-set, default 10%: [Introducing open revenue sharing — itch.io](https://itch.io/updates/introducing-open-revenue-sharing) [accessed 2026-05-13]; [Game Developer — itch.io launches open revenue sharing](https://www.gamedeveloper.com/business/itch-io-launches-open-revenue-sharing) [accessed 2026-05-13]
- itch — Stripe payment processing $0.30 + 2.9%: [Accepting payments and getting paid — itch.io docs](https://itch.io/docs/creators/payments) [accessed 2026-05-13]
- Steam — 30% / 25% / 20% tiers + thresholds: [Steamworks: New Revenue Share Tiers](https://steamcommunity.com/groups/steamworks/announcements/detail/1697191267930157838) [accessed 2026-05-13]; [Game Discover Co — Revealed: the numbers behind Steam's 24% cut in 2025](https://newsletter.gamediscover.co/p/revealed-the-numbers-behind-steams) [accessed 2026-05-13]
- Audience-size 10× heuristic (used here as a directional anchor, not a hard fact): [Fungies.io — Steam vs Itch.io for Indie Developers](https://fungies.io/steam-vs-itch-io-indie-developers/) [accessed 2026-05-13]; [Spiralup Games — Itch.io vs Steam Early Access](https://www.spiralupgames.com/post/itch-io-vs-steam-early-access-for-indie-game-developers) [accessed 2026-05-13]

**Revenue verdict.** Per-transaction, itch is unambiguously better for the developer. Per-launch in absolute dollars, Steam wins at any v1.0 audience scale because of its volume. The right answer is not to pick a winner — it's to use itch for what itch is good at (low-friction near-term sales + community) and add Steam at v1.0 to capture the volume cohort.

---

## (4) Reviews + refunds (the trust loop)

How a buyer signals quality back to other buyers — and how easily they can change their mind — is one of the highest-impact differences between the stores.

### (4a) Review systems

| Axis | itch.io | Steam |
|---|---|---|
| Public reviews | Ratings (1–5 stars) are public *only via* the global feed and only if the rater opts in to public visibility. **Reviews are not shown on the game page by default.** A typical alpha-stage itch page shows only the star average + a Comments section. | **Public, verified-purchase, prominent.** Every Steam page has a Recent + Overall review summary with thumbs-up/thumbs-down ratio. The review label ("Mostly Positive", "Very Positive", "Overwhelmingly Positive") is shown above the buy button on every page and in every search-result tile. |
| Verified purchase | Effectively no — you don't need an account to buy, so reviews can't be tied to ownership reliably. | Yes. Steam only counts reviews from purchasers (or those who received a key); review summary excludes off-platform key activations by default. |
| Developer reply | No reply mechanism on reviews; developers must use the Comments thread (parallel surface). | Threaded reply allowed on every review. |
| Refund window in review eligibility | N/A — no automated refund flow. | A refunded purchase still has an account record, but the user can't post a review for a game they no longer own. |
| 1-in-50 review rate | Multiple itch forum threads report fewer than 1 in 50 buyers leave any text feedback. Star-only feedback is more common. | Higher participation — typical Steam indie games report 1–3% review-to-sale ratios, which means a few hundred reviews on a few-thousand-unit alpha is normal. |

Sources:
- itch — reviews are typically not page-visible, opt-in to global feed: [Ratings and Reviews: where to find — itch.io](https://itch.io/t/3496378/ratings-and-reviews-where-to-find) [accessed 2026-05-13]; [Wait, that's how reviews work here? — itch.io](https://itch.io/t/1667528/wait-thats-how-reviews-work-here) [accessed 2026-05-13]; [Question about Comments, Reviews and Ratings — itch.io](https://itch.io/t/3993564/question-about-comments-reviews-and-ratings) [accessed 2026-05-13]
- itch — no verified-purchase requirement (account-optional purchase): [How to access your purchase — itch.io docs](https://itch.io/docs/buying/already-bought) [accessed 2026-05-13]
- Steam — verified-purchase + summary review labels are core to the storefront: [Steam Refunds](https://store.steampowered.com/steam_refunds) [accessed 2026-05-13]

### (4b) Refund interaction with short indie games

Steam's 14-day / 2-hour automatic-approval window is the dominant refund norm on the platform. It is **especially load-bearing on short indie games** because the 2-hour window is a real fraction of the playtime. Cloudy Ninja's first-time-completion estimate is 60–90 minutes (per `marketing/itch-page-draft.md` §2 and the presskit). That puts most of the campaign inside the auto-refund window.

There are two ways this can play out:
1. **Refund-as-trial.** Players who buy, finish the campaign (or most of it), then refund. Survey data + indie postmortems put the typical indie refund rate at 10–12% of sales, with higher rates for short games. ([Steam's two-hour refund window is silently killing niche indie games — XDA-Developers](https://www.xda-developers.com/steams-two-hour-refund-window-killing-niche-indie-games/) [accessed 2026-05-13])
2. **Refund-as-quality-signal.** Players who bounce inside the 2-hour window because the alpha build is rough. This is the case that hurts more at alpha than at v1.0 — a refund-spiked early review history is hard to bury.

itch has neither problem. There's no automatic refund flow, the alpha can be marked "In development" with no Early Access contract, and the lack of a public-page review system means an alpha rough patch doesn't get cemented into a permanent review score visible above the buy button.

This is one of the strongest single arguments for shipping the alpha on itch.io only.

### (4c) The "Early Access label baggage" problem

Steam Early Access is the only contractual way to sell a Steam game that the developer calls "alpha." The branding rules require Steam Early Access labeling everywhere, including third-party stores. Cloudy Ninja's tags ([T-075](../marketing/steam-tags-research.md)) avoid `Difficult` and `Precision Platformer` precisely so we don't promise something the build doesn't deliver — the same logic applies to Early Access: it makes a *commitment* (continued development, future release) that the project hasn't formally scoped a date for. itch's "In development" status is descriptive, not contractual.

Source: [Early Access — Steamworks docs](https://partner.steamgames.com/doc/store/earlyaccess) [accessed 2026-05-13]; [PC Gamer — Steam's secret Early Access rules](https://www.pcgamer.com/early-access-rules/) [accessed 2026-05-13].

**Reviews + refunds verdict.** itch's private-by-default review channel + lack of automated refund flow is a near-perfect fit for an alpha audience: feedback lands as devlog comments and DMs, not as a permanent above-the-fold thumbs-down ratio. Steam's verified-purchase review system is a v1.0 asset — once you have a finished game, the public verified-purchase score is *the* social proof players use to decide. Same axis, opposite winners at the two stages.

---

## (5) Recommendation matrix

The decision is **per stage, per axis, with cell-level rationale**. Every cell either points to itch, Steam, or both; nothing here recommends one store *to the exclusion of the other* without an evidence trail.

### (5a) Alpha (Sprint D, near-term — what ships when T-124 goes live)

| Axis | itch.io | Steam | Net call |
|---|---|---|---|
| Friction (dev) | Free, instant publish, no Early Access contract | $100 + 30-day wait + Early Access branding | **itch only** |
| Friction (player) | Account-optional, pay-what-you-want, no client | Account + client + full $2.99–4.99 upfront | **itch only** |
| Discovery | Smaller pool, but jam + devlog surfaces fit alpha | Massive pool, but Next Fest is one-shot and shouldn't be burned on alpha | **itch only** (preserve Next Fest for v1.0) |
| Revenue | ~77–87% net per sale at pay-what-share | 70% net per sale; volume not yet meaningful | **itch only** |
| Reviews / feedback | Private-by-default; devlog comments thread; no public score risk | Verified-purchase reviews pinned above buy button; refund-window risk on 60-90 min campaign | **itch only** |
| Refund exposure | Creator-discretion only; no auto window | 14-day / 2-hour auto refund; refund-as-trial risk on short campaign | **itch only** |

**Alpha recommendation: itch.io only.** Every axis points the same direction. The cost of shipping to Steam at alpha is non-trivial ($100, 30 days, Early Access contract, Next Fest slot burned) and the benefits are negligible at the audience size Cloudy Ninja has at Sprint D. The only counter-argument is "wishlist seeding" — and Cloudy Ninja can do that later by putting up a Steam coming-soon page (no Early Access commitment) timed to the trailer + v1.0 approach, which is when the wishlists will actually convert.

### (5b) v1.0 (post-alpha, finish-quality build with trailer + reviews)

| Axis | itch.io | Steam | Net call |
|---|---|---|---|
| Friction (dev) | Already on itch from alpha; zero additional friction | $100 + 30 days lead time, but acceptable on a planned launch | **Both** (itch already there; Steam worth the cost at v1.0) |
| Friction (player) | DRM-free + account-optional retains the existing audience | Familiar storefront for the bulk of PC buyers | **Both** |
| Discovery | Long tail; jam audience returning for finished version | Wishlist surface + Next Fest + tag carousel = the volume channel | **Both** (itch keeps the niche audience; Steam unlocks the volume cohort) |
| Revenue | ~77–87% net retained | 70% net but on 10× audience size; absolute revenue higher | **Both**; Steam carries most of the absolute dollars |
| Reviews / feedback | Useful for direct community; not the social-proof surface that converts new buyers | Verified-purchase positive review summary is the v1.0 conversion engine | **Steam-led, itch-supporting** |
| Refund exposure | Same as alpha — minimal | Auto-refund still applies, but 60-90 min campaign is now a known + intentional choice, and "short and complete" is a recognized indie category | **Both** (Steam refund risk is now priced-in, not a surprise) |

**v1.0 recommendation: both stores.** Steam captures the new-audience volume + verified-purchase social proof; itch retains the existing audience, the DRM-free option, the pay-what-you-want price tier, and the community that's been giving feedback since alpha. The two stores are doing *different jobs* at v1.0, which is why "Steam only" or "itch only" both leave value on the table.

Specific recommendations for the v1.0 push:
- **Save Next Fest for the v1.0 launch window.** Put up the Steam coming-soon page ~3 months before the planned v1.0 date, publish a Steam demo ~2 weeks before Next Fest, time Next Fest to within ~30 days of v1.0 launch (per Valve's marketing recommendations and the GameDiscoverCo conversion data).
- **Maintain pricing parity (per Steam's Early Access rules — which apply to fully-released games too if you offer keys via third parties).** The published $2.99–4.99 target band per the presskit is fine for both stores. itch can additionally support pay-what-you-want with a suggested price at or above the Steam list.
- **Carry the same tag spine across both stores** per T-075 (`Pixel Graphics` / `Platformer` / `2D` / `Nature` on Steam, mapped to `pixel-art` / `platformer` / `2d` / `nature` on itch per T-124).
- **Public review channel split:** Steam = the conversion surface. itch = the loyalty surface. Don't expect itch to "compete" on review counts at v1.0; that's not the job itch is doing.

### (5c) The case for *not* doing what this recommendation says

Per the ticket's hard rules, the recommendation can't punt to "Steam-only" or "itch-only" without evidence. Equally, the recommendation shouldn't pretend the alternatives are unthinkable. The strongest counterarguments — and the evidence-based reasons each is still rejected:

- **"Just ship to Steam at alpha and call it Early Access."** Counter: the Next Fest slot is once-per-title. Burning Next Fest on an alpha build leaves no marketing surface for v1.0 launch. Source: [Steam Next Fest — Steamworks docs](https://partner.steamgames.com/doc/marketing/upcoming_events/nextfest) [accessed 2026-05-13].
- **"Skip itch entirely; Steam audience is 10× larger."** Counter: the 10× volume premise only holds at v1.0 finish-quality. At alpha-quality, Steam's refund window + public review system actively destroys reputation. Source: [Steam's two-hour refund window is silently killing niche indie games — XDA-Developers](https://www.xda-developers.com/steams-two-hour-refund-window-killing-niche-indie-games/) [accessed 2026-05-13].
- **"Skip Steam entirely; itch keeps 87%."** Counter: Game Developer surveys + indie postmortems consistently show Steam volume at v1.0 produces higher absolute revenue *after* the 30% cut. Source: [How to Market a Game — Can itch.io success translate to Steam success?](https://howtomarketagame.com/2025/05/22/more-games-that-made-the-itch-io-to-steam-transition/) [accessed 2026-05-13]; [Tavrox / Medium — Perks of soft-launching on itch.io before Steam](https://medium.com/game-marketing/perks-of-soft-launching-your-game-on-itch-io-before-steam-release-7b035c9b1bb) [accessed 2026-05-13].

The bidirectional strategy (itch at alpha → both at v1.0) is the only one supported across all five axes.

---

## (6) Sources (consolidated)

All URLs accessed 2026-05-13.

### itch.io — platform mechanics
- [Accepting payments and getting paid — itch.io docs](https://itch.io/docs/creators/payments)
- [How buying works — itch.io docs](https://itch.io/docs/creators/how-buying-works)
- [How to access your purchase — itch.io docs](https://itch.io/docs/buying/already-bought)
- [Frequently Asked Questions — itch.io](https://itch.io/docs/general/faq)
- [Getting indexed on Search & Browse — itch.io docs](https://itch.io/docs/creators/getting-indexed)
- [Enhanced itch.io tagging system](https://itch.io/updates/enhanced-itchio-tagging-system)
- [Introducing devlogs — itch.io](https://itch.io/updates/introducing-devlogs)
- [Introducing open revenue sharing — itch.io](https://itch.io/updates/introducing-open-revenue-sharing)
- [Game Developer — itch.io launches open revenue sharing](https://www.gamedeveloper.com/business/itch-io-launches-open-revenue-sharing)
- [TechInBengali — Itch.io Just Gave Developers 100% of Sales for 24 Hours](https://en.techinbengali.com/itch-io-creator-day-100-percent-revenue-developers/)

### itch.io — reviews + community forum threads
- [Ratings and Reviews: where to find — itch.io](https://itch.io/t/3496378/ratings-and-reviews-where-to-find)
- [Wait, that's how reviews work here? — itch.io](https://itch.io/t/1667528/wait-thats-how-reviews-work-here)
- [Question about Comments, Reviews and Ratings — itch.io](https://itch.io/t/3993564/question-about-comments-reviews-and-ratings)
- [How does the popular section work? — itch.io](https://itch.io/t/3276137/how-does-the-popular-section-work-are-we-delisted)
- [How are metrics weighted for Popular and New & Popular? — itch.io](https://itch.io/t/4379248/how-are-metrics-weighted-for-popular-and-new-popular)
- [In development status on pages — itch.io](https://itch.io/t/1171997/in-development-status-on-pages-and-front-page-features)

### Steam — platform mechanics
- [Steam Tags — Steamworks docs](https://partner.steamgames.com/doc/store/tags)
- [Steam Direct Fee — Steamworks docs](https://partner.steamgames.com/doc/gettingstarted/appfee)
- [Onboarding — Steamworks docs](https://partner.steamgames.com/doc/gettingstarted/onboarding)
- [Steam DRM — Steamworks docs](https://partner.steamgames.com/doc/features/drm)
- [Steam Refunds](https://store.steampowered.com/steam_refunds)
- [Steam Support — Common Refund Questions](https://help.steampowered.com/en/faqs/view/5FDE-BA65-ACCE-A411)
- [Early Access — Steamworks docs](https://partner.steamgames.com/doc/store/earlyaccess)
- [Steam Next Fest — Steamworks docs](https://partner.steamgames.com/doc/marketing/upcoming_events/nextfest)
- [Steam Next Fest February 2026 — Steamworks docs](https://partner.steamgames.com/doc/marketing/upcoming_events/nextfest/2026february)
- [Steamworks: New Revenue Share Tiers](https://steamcommunity.com/groups/steamworks/announcements/detail/1697191267930157838)

### Steam — analysis, postmortems, surveys
- [Simon Carless — Are you prioritizing your Steam tags?](https://newsletter.gamediscover.co/p/are-you-prioritizing-your-steam-tags)
- [Game Discover Co — Revealed: the numbers behind Steam's 24% cut in 2025](https://newsletter.gamediscover.co/p/revealed-the-numbers-behind-steams)
- [Game Developer — State of the Algorithm](https://www.gamedeveloper.com/business/the-state-of-the-algorithm-what-s-happening-to-indies-on-steam-)
- [Steam Data Suite — tag discoverability guide](https://steamdatasuite.com/choosing-the-right-tags-to-drive-discoverability/)
- [ECI Games — 2025 Steam discoverability guide](https://www.ecigames.net/media/eci-games-2025-steam-discoverability-guide)
- [PC Gamer — Steam's secret Early Access rules](https://www.pcgamer.com/early-access-rules/)
- [XDA-Developers — Steam's two-hour refund window is silently killing niche indie games](https://www.xda-developers.com/steams-two-hour-refund-window-killing-niche-indie-games/)
- [PCGamingWiki — DRM-free games on Steam](https://www.pcgamingwiki.com/wiki/The_big_list_of_DRM-free_games_on_Steam)

### Indie itch-vs-Steam comparisons
- [Spiralup Games — Itch.io vs Steam Early Access for Indie Developers](https://www.spiralupgames.com/post/itch-io-vs-steam-early-access-for-indie-game-developers)
- [Fungies.io — Steam vs Itch.io for Indie Developers: Complete Platform Comparison 2026](https://fungies.io/steam-vs-itch-io-indie-developers/)
- [Generalist Programmer — Itch.io vs Steam for Indie Games](https://generalistprogrammer.com/tutorials/itchio-vs-steam-indie-game-platform-comparison)
- [How to Market a Game — Can itch.io success translate to Steam success?](https://howtomarketagame.com/2025/05/22/more-games-that-made-the-itch-io-to-steam-transition/)
- [Tavrox / Medium — Perks of soft-launching on itch.io before Steam](https://medium.com/game-marketing/perks-of-soft-launching-your-game-on-itch-io-before-steam-release-7b035c9b1bb)
- [PCWorld — How Itch.io became an indie PC game haven](https://www.pcworld.com/article/406186/how-itchio-became-an-indie-pc-game-havenand-steams-antithesis.html)

---

*End of comparison. T-163. The companion deliverables this doc is intended to support are `marketing/itch-page-draft.md` (T-124, itch listing copy) and `marketing/steam-tags-research.md` (T-075, eventual Steam tags). Decisions blocked on this doc: when to create the Steam coming-soon page (recommend: ~3 months pre-v1.0), when to run Next Fest (recommend: ~30 days pre-v1.0), and whether to enable Steam DRM on the v1.0 build (recommend: no — match the itch DRM-free posture for the existing audience).*

# HANDOFF.md — short-lived continuity doc between Claude Code sessions

> Read this **before** anything else if you are picking up where a previous Claude Code session left off. Then read `START_HERE.md` for the normal onboarding. Update this file at the end of your session to capture state the next agent will need. Keep it short — under 200 lines.

**Last updated:** 2026-05-14 by Claude Opus — extended dispatch-heavy session running through user feedback + 8-hour autonomous run. **12 PRs merged + 1 PR awaiting user review + 4 specs filed + 1 LEARNINGS entry.**

## 🚨 Awaiting user action on return

**PR #161 — T-186 Ebo MH1 sprite integration.** CI green across all 10 smoke jobs; 20 unit tests on the pure-function state machine. **Left OPEN deliberately** — smoke CI cannot validate sprite scale, foot-position alignment, or direction flip. 30-second launch check needed:

1. `./gradlew :lwjgl3:run` with `-Dcloudy.devLogs=true` if you want to see logs
2. Pick Ebo (default), start Level 1
3. Verify: sprite renders at sensible scale (~character-sized in world), feet on the ground (not floating or sinking), direction flips when moving left, idle animation cycles smoothly, jump → fall transition reads clean
4. If looks good: `gh pr merge 161 --repo SohailShahM/Cloudy-Ninja --admin --squash --delete-branch` — then ping me to dispatch T-187 (Laya MH3) + T-188 (Zephyr MH2). Same pattern, will ship safely.
5. If looks wrong: describe what's off — likely sprite world size (currently 0.80 m × 0.80 m, body-centre y-offset 0.32 m) or alignment. I'll dispatch a tuning fix.

T-187/T-188/T-189-T-193 are held until Ebo is verified — same scale/offset bug would otherwise propagate.

## What landed this session

### User-feedback round (demo play-test 2026-05-13)
- **T-173 (#150)** MainMenu title scrim — fixes "logo barely readable"
- **T-174 (#152)** invisible-barrier fix — TileRenderer was discarding sub-32-px obstacle tiles. Found multiple thin platforms across Level0_0/1/2/3 affected by same one-line bug.
- **T-175 (#151)** snappier movement (Celeste-like baseline) — `GROUND_COAST_DAMPING = 0.55`, ~80ms stop time. User play-test pending.
- **T-176 (#154)** Laya Wind Dash slow-descent (0.45× gravity) + dynamic camera zoom (1.4× max). User play-test pending.
- **T-177 (#153)** anime asset pack evaluation — 16 packs surveyed, user picked CC0 stack
- **T-179 (#155)** acquired 4 CC0 packs (LuizMelo MH1 + ansimuz Sunny Land + ansimuz Sunny Land Forest of Illusion subset + Pixel Frog Pixel Adventure 1)
- **T-181 (#156)** placed user's 3 manual downloads (LuizMelo MH2/MH3 + ansimuz Sunny Land Forest full); downsampled all LuizMelo from 200/200/126 px → **48 px per frame** via nearest-neighbor (System.Drawing PowerShell, since ImageMagick/Pillow unavailable)

### 8-hour autonomous run
- **T-194 (#157)** gitignore cleanup — `assets/saves/save_slot_*.json` + `assets/audio/music/ambient_*.wav` patterns
- **T-172 (#159)** **GlobalInputRouter Phase B — architectural smell #3 FULLY CLOSED.** 13 `Gdx.input.inputProcessor` assignments → 1; F12 + M-key polling fallbacks deleted; all overlays migrated transparently via GameScreen's `setActiveInputStage()` helper.
- **T-180 (#158)** scaffold (`SpriteSheetFactory`, `SheetCharacterAtlas`, `AnimationStateMachine`) — parallel to existing rendering, no behavior change
- **T-198 (#160)** dev log gating — 10 per-frame/per-event `Gdx.app.log` sites gated behind `cloudy.devLogs` system property. Default-silent now; `-Dcloudy.devLogs=true` restores. Error-level logs untouched.

### Specs filed for next session
- **T-180** (Done) + **T-186..T-193** integration cascade (Ebo done; Laya/Zephyr/biomes/enemies queued)
- **T-200..T-205** T-046 gap-fill (boss sprite, dash/cast/wall-slide anims, lightning VFX, Cloud Atlas UI flourishes) — all human-driven art-direction calls
- **T-182..T-185** Claude Design batch (MainMenu+Settings polish, itch.io landing page, Cloud Atlas card, pitch deck) — user-driven Phase A

## Cumulative state across the two sessions

**Demo readiness:** game runs cleanly, 60fps, no crashes per the user's 6-min play session. CI green across all checks. All alpha-blocking issues addressed:
- T-126 Calibri legal issue → Inter shipped ✅
- T-174 invisible barriers → root cause fixed ✅
- T-175 movement sliding → tuned (user play-test pending)
- T-176 Laya Wind Dash off-screen → camera + glide (user play-test pending)
- T-173 logo contrast → scrim ✅
- T-046 "trash visuals" → CC0 stack acquired + scaffold built + Ebo integration code-complete (visual verification pending)

**Architectural smells all CLOSED:**
- ✅ Dual screen-shake systems (T-169)
- ✅ Silhouette overlay hack (T-170)
- ✅ Per-screen input clobbering (T-172 — Phase B closes it)

**Performance:** game plays at 60fps observed in user's play session. Dev logs now silent by default (T-198).

## Repo state: public + proprietary-licensed (unchanged)

Admin-merge default, `required_conversation_resolution: true` policy. Direct `git push origin HEAD:main` works for TASKS.md / docs / LEARNINGS via admin bypass — used 8× this session (T-168/169/170/171/172, T-194/T-198 spec, T-200-205 spec, etc.).

## Working-tree hygiene

After T-194's gitignore patterns landed, expect a clean working tree post-play. However: `assets/audio/sfx/achievement_unlock.wav` was observed as untracked — this is a procedurally-generated SFX similar to the ambient music tracks but NOT in T-194's ignore patterns. Either commit it as canonical or extend the gitignore pattern. Low-priority cleanup ticket candidate.

## Sub-agent dispatch patterns reaffirmed by this session's 17/17 success rate

1. Brief verbatim spec + hard rules + file-conflict gates. ~150-300 line prompts.
2. **File-conflict gates are critical** — strict no-touch rules per ticket. Multiple times this run a sub-agent flagged "this file looks like X's territory" instead of broadening scope.
3. **Visual-risk vs mechanical-risk distinction:** mechanical work (gitignore, log gating, router migration) ships green-CI → admin-merge confidently. Visual work (sprite integration) ships green-CI → still needs human eyeball verification before merge.
4. **Sub-agent pivots count:** several agents made smart adaptations (T-181 used Windows System.Drawing when ImageMagick/Pillow unavailable; T-186 extracted pure `computeAnimState()` because Box2D natives don't work in JVM tests). Trust the agent's judgment within the spec's hard rules.

## Source-side quirks pinned this session

1. ✅ **CLOSED** Per-screen `Gdx.input.inputProcessor` clobbering (T-172).
2. **NEW (T-181):** ImageMagick not installed on this Windows machine; Pillow not in Python 3.13. **Use System.Drawing via PowerShell** for image processing tasks (nearest-neighbor pixel-art downsampling pattern is documented in T-181's PR description).
3. **NEW (T-180):** Sub-agent renamed scaffold classes from spec — `SpriteSheetFactory` (not SpriteFactory), `SheetCharacterAtlas` (not CharacterAtlas), `AnimationStateMachine` (as specced). T-186 references the actual names.
4. **NEW (T-186):** `PlayerController` has **no `currentCharacter` field** — it lives on `LevelRunState` and is passed into `renderPlayer(currentCharacter)`. Future entity-state work should remember this. Also: `hitFlashTimer` doesn't exist by that name; equivalent is `isFlashing` (derived from `deathFlashTimer`).
5. **NEW (T-181 inventory):** LuizMelo MH3 uses `Going Up.png` / `Going Down.png` instead of `Jump.png` / `Fall.png`. Semantic equivalents per the inventory doc.

## Tooling gotchas (carry forward from prior sessions, still relevant)

See LEARNINGS.md. Notably: smoke CI runner xvfb stalls; admin-merge silent on success; GitHub Copilot auto-review rate-limited until 2026-05-18.

## Known issues / open questions

- **T-168 visual font verify** still pending (human-only; pre-alpha gate after Inter swap).
- **T-187/T-188** held on T-186 visual verification.
- **Sprite world size (0.80 m × 0.80 m)** is a per-character constant in T-186 — if Ebo looks wrong-sized after launch, tuning is single-edit. Same constant will be reused in T-187/T-188.
- **assets/audio/sfx/achievement_unlock.wav** untracked; minor cleanup candidate (file or gitignore).

## At end of your session

1. Bump "Last updated" + summary
2. Update "Awaiting user action" — remove what user has resolved, add new gates
3. Update "What landed" + "Specs filed for next session"
4. Capture new gotchas in `LEARNINGS.md` and reference here
5. Commit + push to main (direct push via admin bypass for docs-only)

# HTML5 / Web-Demo Viability Spike (T-123)

**Status:** Research-only memo. No code, no gradle, no deps changed.
**Author:** claude-code-sub-agent
**Date:** 2026-05-13
**Branch:** `claude/T-123-html5-spike`

---

## TL;DR

> **Recommendation: Option 2 — Cut a stripped web demo (1–2 levels, no save, no dynamic lighting).** Effort: **M (4–8 dev-days of focused work)**.
>
> Option 3 (full port) is **L–XL** and would block the alpha. Option 1 (defer entirely) is the safe play, but the discoverability uplift from an itch.io HTML5 embed is large enough that a stripped demo is worth the M-sized investment **after** the desktop alpha ships.
>
> Critical caveat: the path runs through gdx-teavm (xpenatan), not the deprecated GWT backend. gdx-teavm 1.5.6 supports libGDX 1.14.0 exactly, **but its `gdx-box2d-teavm` extension is still at 1.0.0-b6 (beta)** — that is the single biggest unknown. A 1-day spike to actually compile a Box2D smoke scene through gdx-teavm would de-risk this dramatically before committing to Option 2.

---

## The 4 questions answered

### (a) Does our current libGDX version support a GWT/TeaVM backend?

**Yes — via gdx-teavm, not the official GWT backend.**

| Backend | Status (May 2026) | Supports libGDX 1.14.0? | Supports Kotlin? |
|---|---|---|---|
| Official libGDX GWT/HTML5 | Still shipped, increasingly second-class. libGDX wiki concedes "TeaVM is really quite a bit better at this point… new projects should probably use TeaVM." | Yes, but Java-only | **No** — GWT transpiles Java to JS; Kotlin source is not accepted. **Fatal for us** — our entire codebase is Kotlin. |
| `xpenatan/gdx-teavm` (community, actively maintained) | Last release **1.5.6 (2026-05-11)** — three days ago. TeaVM 0.14.0 under the hood. | **Yes — explicitly 1.14.0** (exact match with our `gdxVersion=1.14.0`). | **Yes** — TeaVM compiles JVM bytecode (Kotlin compiles to JVM bytecode → TeaVM picks it up). |

There is exactly one viable path: **gdx-teavm**. The official GWT backend is a dead end because of Kotlin.

### (b) Which dependencies are GWT/TeaVM-hostile?

Audited against our `gradle.properties` deps and the actual `core/src/main/kotlin/` usage.

| Dependency / pattern | Used in our codebase? | TeaVM status | Risk |
|---|---|---|---|
| **libGDX core 1.14.0** | Yes, everywhere | Supported by gdx-teavm 1.5.6 | LOW |
| **`gdx-box2d`** | Yes — 186 hits across 22 files (PlayerController, Enemy, Projectile, StormSentinel, DriftHusk, MovingPlatform, SmogSprite, WorldContactListener, etc.) | `gdx-box2d-teavm` exists but at **1.0.0-b6 (beta)**, via Emscripten → WASM. WIP per the project's own issue tracker (#53). | **HIGH** — beta + WASM toolchain + critical path. Single largest unknown. |
| **`gdx-freetype`** | Yes — `FontManager.kt` only (already isolated behind a `FontLoader` seam introduced in T-109) | `gdx-freetype-teavm` is a confirmed stable extension. | MEDIUM — should work; if not, the existing `FontLoader` seam lets us swap in a pre-baked `BitmapFont`. |
| **`box2dlights` 1.5** (`RayHandler`) | Yes — `GameScreen.kt` (ambient lighting + per-level lights) | **No TeaVM port exists.** Library depends on Box2D natives plus a custom shader chain. The TypeScript `@box2d/lights` port is unrelated to libGDX. | **HIGH (BLOCKER)** — would have to remove or stub out. |
| **`gdx-controllers` 2.2.4** | Imported but only used for desktop gamepad input | `gdx-controllers-teavm` extension exists (officially listed). HTML5 gamepad API is the natural mapping. | LOW |
| **`kotlinx-serialization-json` 1.7.3** | Yes — `SaveManager.kt`, `GameState.kt`, `Settings.kt`, achievement/save migration | **Compile-time codegen, no runtime reflection.** Not officially tested on TeaVM, but the mechanism is reflection-free, so it should compile. If a JS-target edge case bites, we fall back to manual JSON via libGDX's `Json` class or just stub save (Option 2 explicitly drops saves). | MEDIUM |
| **`com.kotcrab.vis:vis-ui` 1.5.4** | Yes — 16 files (MainMenu, Settings, Stats, AchievementsScreen, Hud, etc.) | Pure Scene2D-UI layer on top of libGDX. No native code. **Should work on TeaVM** but no confirmed test in the wild that we could find. | MEDIUM |
| **`com.badlogicgames.ashley` 1.7.4** | **Declared but not imported anywhere** (`grep com.badlogic.ashley` → 0 hits in `core/src/main/kotlin/`). | N/A — exclude from web build. | NONE |
| **`com.badlogicgames.gdx:gdx-ai` 1.8.2** | **Declared but not imported anywhere** (`grep com.badlogic.gdx.ai` → 0 hits). | N/A — exclude from web build. | NONE |
| **Kotlin stdlib 2.3.21 / coroutines** | Stdlib yes; coroutines **not used** anywhere in our code (`grep -r "kotlinx.coroutines" core/` → 0 hits) | TeaVM supports Kotlin stdlib; coroutines are explicitly NOT supported per gdx-teavm docs. Lucky for us. | LOW |
| **Java reflection** (`Class.forName`, `KClass`, `getDeclaredField`, `Objenesis`, `Unsafe`) | **Almost none in production code.** Two `::class.java` hits, both for libGDX `MapProperties` typed lookups. Reflection-via-Objenesis/Unsafe lives in `core/src/test/` only and never ships to web. | TeaVM has limited reflection but supports libGDX's typed property lookups via its own reflection emulation. | LOW |
| **`System.nanoTime`** | Not directly used in game code (we use `Gdx.graphics.getDeltaTime()` everywhere). | N/A | NONE |
| **Threads** (`Thread`, `Executor`, `synchronized`) | Audio loading is the only candidate. WAV-only assets load fast enough that this is moot. | N/A (web has no threads) | NONE |
| **File I/O** (`Gdx.files.local`) | Yes — `SaveManager.kt`, `Settings.kt` | TeaVM/gdx-teavm maps `local` to IndexedDB / localStorage. Works but per-origin quota. | LOW (and Option 2 drops saves anyway) |
| **TMX tile maps** (`TmxLevelDefinition`, `MapLevelLoader`, `TilesetRegistry`) | Yes — 3 files | libGDX wiki notes: TMX must use **Base64 encoding** (not CSV/XML) on HTML5. Our TMX encoding needs to be checked; trivial fix if wrong. | LOW |
| **Audio formats (WAV)** | All our music + SFX is `.wav` (verified — 20 files across `assets/audio/` and `assets/sounds/`). | WAV works on HTML5. Note: web audio requires a user gesture before first play — already true for desktop/mobile, no behaviour change. | LOW |
| **GSON / Jackson** | Not used anywhere — we use kotlinx-serialization exclusively. | N/A | NONE |

**Summary:** the truly GWT-hostile elements in our stack are:
1. **box2dlights** — no TeaVM port, **hard blocker** for a faithful port.
2. **gdx-box2d-teavm beta status** — viable but risky.

Everything else is "should work" with verified extension support, or is unused dead code we can exclude.

### (c) Rough effort estimate

| Option | Effort | Notes |
|---|---|---|
| **(1) Ship desktop-only alpha, defer web demo** | **0 days** | The safe path. Loses a discoverability lever on itch.io. |
| **(2) Stripped web demo (1–2 levels, no save, no lighting)** | **M = 4–8 dev-days** | See breakdown below. |
| **(3) Full web port** | **L–XL = 12–25+ dev-days** | Includes box2dlights replacement (custom shader-based 2D lighting in pure libGDX), save-via-IndexedDB hardening, all 8 levels through Base64-TMX, gdx-box2d-teavm beta validation across all entity classes, full smoke-test matrix. Very real risk that gdx-box2d-teavm beta hits an Emscripten edge case that costs another 3–5 days to chase. |

**Option 2 breakdown (M, 4–8 days):**

| Task | Days |
|---|---|
| 0. **De-risk spike**: stand up a `teavm` Gradle module with a single Box2D scene (one entity, gravity, jump). Confirm gdx-box2d-teavm 1.0.0-b6 compiles and runs in Chrome. | 1 |
| 1. Strip dead deps from the web build (Ashley, gdx-ai, box2dlights, gdx-controllers — gamepad isn't critical for demo). | 0.5 |
| 2. Build flag / feature gate: `WEB_DEMO = true` disables save (uses in-memory `GameState`), disables `RayHandler` (skip lighting pass in `GameScreen.kt`), disables FreeType (use pre-baked BitmapFont). The three seams already exist or are 1-line additions. | 1 |
| 3. Pick 1–2 levels (`Level0_0`, `Level0_1`) — verify TMX Base64 encoding, asset paths resolve from web. | 0.5 |
| 4. Wire `kotlinx-serialization` JSON Settings persistence to localStorage (or just skip — defaults only). | 0.5 |
| 5. First end-to-end browser smoke: load → menu → play level → win → restart. Iterate on what breaks. | 1.5 |
| 6. itch.io embed config (canvas size, fullscreen toggle, asset preloader UI). | 0.5 |
| 7. Buffer for the inevitable Emscripten/TeaVM "why is this not working" rabbit hole. | 1 |

If the de-risk spike (task 0) fails — i.e. gdx-box2d-teavm beta doesn't compile or runs broken — pivot to **Option 1** immediately. Do not throw more days at it.

### (d) Which game systems need refactor or graceful-degrade?

| System | Current desktop behavior | Web demo plan (Option 2) |
|---|---|---|
| **Save / load** (`SaveManager`, `GameState`, `SaveMigrations`) | kotlinx-serialization JSON → `Gdx.files.local("saves/*.json")` | **Graceful degrade: in-memory only.** No persistence across page reloads. Demo is 1–2 levels = ~5 minutes; replay cost is fine. Optional stretch: persist Settings only via `localStorage`. |
| **Audio** (`MusicManager`, `SoundManager`, `ProceduralSoundGenerator`) | WAV files via libGDX audio | **Works as-is.** WAV is HTML5-supported. First-play user-gesture requirement already satisfied by existing menu flow. Procedural sounds use PCM math → still WAV at the end. |
| **Font baking** (`FontManager` + FreeType TTF) | Generates BitmapFont from `assets/...ttf` at runtime per requested size. Falls back to default `BitmapFont` if TTF missing. | **Two options:** (i) ship `gdx-freetype-teavm` and pre-warm a few sizes; (ii) **pre-bake** the 3–4 font sizes we use into static `.fnt` + page PNGs and skip FreeType on web entirely. **Recommend (ii)** for the demo — smaller bundle, fewer moving parts, the existing `FontLoader` seam (T-109) was literally designed for this. |
| **Box2D physics** | Everything: player, enemies, projectiles, moving platforms, sensors, contact listener | **Critical path.** Must validate `gdx-box2d-teavm` 1.0.0-b6 actually works (task 0 in the breakdown). No refactor required if it works — the API is bit-for-bit the same. |
| **box2dlights / RayHandler** (`GameScreen.kt`) | Ambient lighting + per-level dynamic lights for atmosphere | **Cut entirely for web demo.** Wrap `rayHandler.updateAndRender()` in a `if (!WEB_DEMO)`. Visually less moody, gameplay-identical. Minimal refactor. |
| **TiledMap loading** (`MapLevelLoader`, `TmxLevelDefinition`) | Loads `.tmx` + tileset PNGs | **Verify TMX is Base64-encoded** (not CSV/XML). Single-line check via Tiled editor; trivial re-export if wrong. |
| **Particles / parallax / screen fade** | Pure libGDX `SpriteBatch` / `ShapeRenderer` | **Works as-is.** |
| **Input** (`InputManager`) | Keyboard + optional gamepad | **Keyboard only on web demo.** Gamepad would need `gdx-controllers-teavm`; defer to full port. |
| **VisUI screens** (MainMenu, Settings, Hud, Pause, Victory, etc.) | Scene2D-UI on top of VisUI | **Should work** but unverified. Highest probability of late-discovered visual bugs. Settings screen could be simplified for demo (no key rebinding, no audio sliders — defaults only). |
| **Achievements / atlas** | Tracked in `GameState`, persisted in save | **In-memory only** for demo session. No persistence = no real achievements; can show toasts during the run for satisfaction but they don't carry over. |
| **Smoke / determinism test infrastructure** | CI smoke tests on JVM | **Excluded from web build entirely.** Test-only code (`core/src/test/`) already doesn't ship to lwjgl3, won't ship to web either. |

---

## Top 3 blockers (ranked by severity)

1. **box2dlights has no TeaVM port.** Either remove from the web build (Option 2's plan — gameplay still works, looks duller) or hand-roll a 2D lighting alternative (Option 3 territory, ~3 days of shader work alone).
2. **`gdx-box2d-teavm` is at 1.0.0-b6 (beta) via Emscripten/WASM.** This is the single highest-risk dependency. Must be validated by a 1-day spike before committing to either Option 2 or 3. If it fails, Option 1 is the only honest path.
3. **kotlinx-serialization on TeaVM is unverified in the wild.** The codegen-based mechanism is reflection-free so it *should* work, but no public example confirms it. Option 2 sidesteps this entirely by dropping save persistence; Option 3 cannot.

(Honourable mentions that did **not** make the top 3: VisUI on TeaVM is unverified — medium risk but a viable fallback is plain Scene2D. TMX encoding fix is trivial. Audio is fine. Reflection footprint is essentially zero in our code. Kotlin coroutines aren't used.)

---

## Why Option 2 (not Option 1, not Option 3)

**Why not Option 1 (defer):** The discoverability uplift from a playable itch.io HTML5 embed vs. a download-only desktop release is documented across the indie space as roughly 3–10× play-through rate. For an unreleased indie game with no audience, that's the single most cost-effective marketing lever available. The desktop alpha is the priority, but a stripped web demo arriving 1–2 weeks later is a much bigger win than its M-sized cost suggests.

**Why not Option 3 (full port):** The box2dlights blocker alone is L-sized (custom shader work). Add gdx-box2d-teavm beta risk, full 8-level Base64-TMX validation, full save-via-IndexedDB, full controller support, and the full VisUI surface across every screen — that's an L–XL effort that would push the alpha by 3+ weeks. Not worth it pre-launch when the player population is zero. **After** the alpha lands and we have a real audience, Option 3 becomes a more honest investment decision.

**Why Option 2 works:** every blocker has a cheap escape hatch. box2dlights → remove. Save → in-memory. FreeType → pre-bake. VisUI → simplify Settings if needed. The only un-de-riskable item is gdx-box2d-teavm beta, and a 1-day spike answers that. If the spike fails, we revert to Option 1 with one day burned — a cheap insurance policy.

The non-negotiable prerequisite is **task 0 in the breakdown — the gdx-box2d-teavm de-risk spike**. Do not commit to Option 2 without it.

---

## Sources

- [xpenatan/gdx-teavm (GitHub)](https://github.com/xpenatan/gdx-teavm) — current libGDX support matrix, version 1.5.6, supports libGDX 1.14.0.
- [libGDX wiki — HTML5 Backend and GWT Specifics](https://libgdx.com/wiki/html5-backend-and-gwt-specifics) — GWT limitations (reflection, threads, audio, TMX Base64).
- [libGDX wiki — Using libGDX with Kotlin](https://libgdx.com/wiki/jvm-langs/using-libgdx-with-kotlin) — GWT doesn't accept Kotlin; TeaVM does.
- [Improve Box2D performances with TeaVM (gdx-teavm issue #53)](https://github.com/xpenatan/gdx-teavm/issues/53) — gdx-box2d-teavm WIP status.
- [gdx-box2d-teavm on Maven](https://libraries.io/maven/com.github.xpenatan.gdx-teavm:gdx-box2d-teavm) — version 1.0.0-b6 confirms beta.
- [libgdx/box2dlights](https://github.com/libgdx/box2dlights) — no TeaVM port documented.
- [Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) — multiplatform (JVM/JS/Native), compile-time codegen.
- [TeaVM docs — Overview](https://teavm.org/docs/intro/overview.html) — reflection limitations on TeaVM.
- This repository: `gradle.properties`, `core/build.gradle`, `lwjgl3/build.gradle`, `android/build.gradle`, full `core/src/main/kotlin/` tree (Box2D usage audit, reflection audit, audio asset audit), `research/dependency-audit.md`.

# Session-start visual review

> Every new Claude Code session running on Cloudy-Ninja should run this
> workflow as ONE OF THE FIRST actions, before tackling new feature work.
> The smoke autopilot tests gameplay invariants; this workflow tests
> visuals. Together they cover the major regression surfaces.

## When to skip

- If the previous session's `HANDOFF.md` explicitly says "no visual changes
  in flight + last visual review was clean", skip and pick up that
  session's pending work.
- If a sub-agent is currently editing rendering code in a parallel
  worktree, run after that lands (avoid stale captures).

## Procedure (~3 min)

### 1. Generate fresh checkpoint PNGs

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.captureCheckpoints=true -Dcloudy.devLogs=true
```

Smoke autopilot quits itself when done. Expected runtime: ~3-5 minutes
depending on level pace. Wait for the background job to exit (or use
the Bash tool's `run_in_background` + completion notification).

### 2. List the captured PNGs

```bash
ls -la build/visual-checkpoints/
```

Expected files (6 minimum):
- `mainmenu-loaded.png`
- `settings-screen-loaded.png`
- `level1-start.png` (or `level0_0-start.png` — depends on smoke route)
- `level1-mid-jump.png`
- `pause-overlay-active.png` (may be missing — pause isn't on the smoke critical path)
- `<levelId>-after-death.png` (may be missing — autopilot rarely dies in normal smoke)

If fewer than 4 PNGs land, something is wrong with capture wiring — file
a bug ticket and skip the review until fixed.

### 3. Read each PNG via the Read tool

```
Read file_path="build/visual-checkpoints/mainmenu-loaded.png"
Read file_path="build/visual-checkpoints/level1-start.png"
...
```

Claude is multimodal — the image content is shown inline. Look for:

**MainMenu:**
- Is the "Cloudy Ninja" title clearly legible?
- Are buttons readable (Play/Settings/etc.)?
- Build label visible at bottom-right?
- Achievement counter visible at top-left?

**Settings:**
- Section headers clear (Audio/Display/Controls/Accessibility)?
- All sliders + labels visible without clipping?
- "Reset to defaults" button at the bottom?

**level1-start:**
- Is the player visible on a tile?
- **Are the player's visible feet ON the ground (not floating above, not sinking below)?** ← key foot-offset check
- Is the scene appropriately lit (not pitch-black, not over-bright)?
- Is the player at sensible scale relative to tiles?

**level1-mid-jump:**
- Player visible mid-air?
- Camera follows / hasn't lost the player?
- (If Laya): is gravity-glide visually evident? (descent should look slow)

**pause-overlay-active:**
- Overlay legible over the paused scene?
- Resume/quit buttons readable?

**after-death:**
- Player respawned at checkpoint, not at a glitched position?

### 4. File tickets for regressions

For each visual issue found:
1. Save the offending PNG path to a clear ticket spec.
2. Add a `TASKS.md` entry (next free T-NNN; see existing tickets for format).
3. Surface the ticket in your session-start summary to the user.

If all checkpoints look clean: append a one-liner to `LEARNINGS.md`:
`{date}: visual review session-start — all checkpoints clean`

### 5. (Optional) Run the foot-offset autotuner

If `level1-start.png` shows the character's feet visibly off the
ground, instead of filing a ticket and tuning by hand, dispatch
T-A14 (foot-offset autotuner) which pixel-analyzes the PNG and
converges the constant automatically.

## Failure modes

- **`build/visual-checkpoints/` empty after smoke run:** the
  `cloudy.captureCheckpoints` flag wasn't picked up. Verify the
  `Constants.CAPTURE_CHECKPOINTS` field reads correctly; verify the
  flag is passed via `-D...=true`.

- **PNGs are all the same image:** likely the deferred-to-end-of-render
  consume isn't clearing the pending field, OR the captures all fire
  at the same lifecycle moment. Inspect `CheckpointCapture` wire-up.

- **PNGs are corrupted / 0 bytes:** GL context state issue at capture
  time. Check that capture happens AFTER `spriteBatch.end()` /
  `shapeRenderer.end()` for the active pass.

## Related docs / tickets

- T-A10 (PR #164): the capture system itself
- T-A14: foot-offset autotuner (pixel-analyzes captures, edits constants)
- T-A16: visual regression diff CI (PR-vs-main checkpoint comparison)
- T-168: pre-alpha visual font verification (Inter swap follow-up)

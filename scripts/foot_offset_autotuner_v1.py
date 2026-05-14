#!/usr/bin/env python3
"""Foot-offset autotuner V1 — baseline-subtraction character detection.

V0 (`scripts/foot_offset_autotuner.py`) used a color heuristic
(everything-not-background-inside-a-fixed-window) to find the character's
visible feet. That heuristic mis-measured the search-window-bottom rather
than the sprite — V0 wrote 0.030f which made Ebo float visibly; the value
was reverted to 0.3f and the heuristic flagged as the problem.

V1 takes a tighter approach: capture TWO checkpoints — one WITHOUT the
character (baseline, drawn by toggling `cloudy.captureBaseline=true` which
short-circuits the player-render call in `GameScreen.render()`) and one
WITH the character (the normal `level1-start.png` capture). Subtract them
pixel-by-pixel — the only significant differences are the character itself
plus a small amount of lighting noise (torch animation, particles). The
bottom-most significantly-different row inside the spawn search window is
the character's visible feet.

Compared to V0 this is:

  - Resilient to HUD chrome, sky color, grass color (the diff cancels them).
  - Robust against shader / palette changes (no hard-coded color thresholds
    against the level art).
  - Easy to debug: a noisy result means the diff itself is noisy (e.g. the
    torch is too bright or the player jitter changed the camera) which is
    surfaced rather than papered over.

Inputs (all relative to repo root):
  build/visual-checkpoints/level1-start-baseline.png  — baseline, no player
  build/visual-checkpoints/level1-start.png           — with player

The two captures come from SEPARATE runs:

  # 1. Baseline (no player drawn). Smoke autopilot still drives the game;
  #    `cloudy.captureBaseline=true` only suppresses the player sprite.
  ./gradlew :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 \
      -Dcloudy.captureCheckpoints=true -Dcloudy.captureBaseline=true

  # 2. Normal with-player capture.
  ./gradlew :lwjgl3:run -Dcloudy.smoke=true -Dcloudy.smokeLevel=level1 \
      -Dcloudy.captureCheckpoints=true

  # 3. Run the V1 analyzer.
  python scripts/foot_offset_autotuner_v1.py

Per T-A18 hard rules: this script writes to the SAME target as V0
(`SPRITE_FOOT_OFFSET_EBO` in `SpriteFactory.kt`) but ships a tighter
DETECTION method. Whether to actually apply the new measurement is a
follow-up decision — by default V1 prints the gap and the proposed new
value WITHOUT writing. Pass `--apply` to rewrite the constant. This keeps
PR #170's revert (0.3f) intact unless the user opts in.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image


# Pixels-per-meter for the rendered view (matches Constants.PPM).
PPM = 100.0

# Significant-pixel threshold. The diff array is the per-pixel sum-of-channel
# absolute differences; in a clean RGB capture the channels are 0–255 each so
# the max possible diff is 765. Torch flicker / particles produce diffs in
# the single-digit-to-low-double-digit range across many pixels; the actual
# character sprite has zones with diffs well above 100 (its outline is
# completely different from the level background).
#
# 30 is empirically chosen to:
#   - Comfortably reject torch flicker (typically <15 per-pixel).
#   - Reject minor camera jitter between runs (a 1-pixel shift in fully
#     uniform sky produces 0 diff; the level geometry is static between
#     captures so geometry-edge aliasing diffs are negligible).
#   - Accept the character even where it's anti-aliased against grass.
# Surface this as a CLI flag in case the threshold needs to be tuned per-run.
DEFAULT_DIFF_THRESHOLD = 30

# Spawn search window (image coords, top-down). The level1 player spawn is
# in the lower-left of the frame; constrain the search there so that any
# distant moving element (a torch flicker on the right side of the screen)
# can't be mistaken for the character.
#
# These bounds are intentionally GENEROUS — the subtraction itself does the
# heavy lifting. The window only exists to drop diffs from far-away parts
# of the frame that would confuse the bottom-row scan.
SPAWN_Y_MIN = 600   # rows above y=600 are well above spawn → not the character
SPAWN_X_MIN = 40    # cols before x=40 are off-screen / HUD chrome
SPAWN_X_MAX = 200   # cols past x=200 are far past the spawn


def find_character_bottom_via_subtraction(
    baseline_png: Path,
    with_char_png: Path,
    threshold: int = DEFAULT_DIFF_THRESHOLD,
) -> tuple[int, int]:
    """Locate the character's bottom-Y by pixel-subtracting baseline from
    with-character.

    Returns ``(bottom_y, significant_pixel_count)`` where ``bottom_y`` is the
    image-coords (top-down) row index of the lowest pixel that differs by
    more than ``threshold`` from the baseline, constrained to the spawn
    window. The count is included for diagnostics — a tiny count means the
    threshold may be too aggressive and the result should be treated with
    suspicion.
    """
    base = np.array(Image.open(baseline_png).convert("RGB"), dtype=np.int16)
    char = np.array(Image.open(with_char_png).convert("RGB"), dtype=np.int16)
    if base.shape != char.shape:
        raise RuntimeError(
            f"Image shape mismatch: baseline={base.shape}, with_char={char.shape}. "
            "Re-capture both PNGs at the same window size."
        )
    diff = np.abs(base - char).sum(axis=-1)  # per-pixel total channel diff
    significant = diff > threshold

    # Constrain to the spawn window — see SPAWN_* constants above for the
    # reasoning. Anything outside is forced to False so the bottom-row scan
    # can't be tricked by torch-flicker on the far right.
    mask = np.zeros_like(significant)
    mask[SPAWN_Y_MIN:, SPAWN_X_MIN:SPAWN_X_MAX] = True
    significant = significant & mask

    sig_count = int(significant.sum())
    rows_with_char = np.where(significant.any(axis=1))[0]
    if rows_with_char.size == 0:
        raise RuntimeError(
            "No significantly-different pixels found via subtraction inside the "
            "spawn window. Either:\n"
            "  - baseline + with-char PNGs are identical (did you forget the "
            "    `cloudy.captureBaseline=true` flag on one run?), or\n"
            f"  - the threshold ({threshold}) is too high — try --threshold 15.\n"
            "Tip: the V0 script's CHAR_WINDOW vs. V1's spawn window are wider "
            "than the actual character to give some slack; if the character is "
            "outside the window you'll get this same error."
        )
    return int(rows_with_char.max()), sig_count


def find_ground_top_via_subtraction(
    baseline_png: Path,
    threshold: int = DEFAULT_DIFF_THRESHOLD,  # noqa: ARG001  (unused for now)
) -> int:
    """Locate the ground-tile top inside the spawn x band of the BASELINE
    PNG.

    Heuristic: same color-band scan as V0's ``find_ground_top`` — we can't
    subtract here because there's no "no-ground" reference frame. But unlike
    V0 the baseline has no character to occlude the grass, so the scan is
    deterministic.

    Returns image-coords y of the topmost grass-colored row in the spawn x
    range, scanned in the bottom strip of the frame.
    """
    img = np.array(Image.open(baseline_png).convert("RGB"))
    h, _, _ = img.shape
    # Scan the bottom strip — the spawn ground is in the last ~120 px.
    y0 = max(SPAWN_Y_MIN, h - 120)
    y1 = h
    window = img[y0:y1, SPAWN_X_MIN:SPAWN_X_MAX, :]
    r, g, b = window[..., 0], window[..., 1], window[..., 2]
    is_grass = (g > r) & (g > b) & (g > 80)
    grass_rows = np.any(is_grass, axis=1)
    rows_with_grass = np.where(grass_rows)[0]
    if rows_with_grass.size == 0:
        raise RuntimeError(
            "No grass pixels found in baseline bottom strip — the level art may "
            "have changed (e.g. snow tileset) and the ground-top scan needs a "
            "new color heuristic."
        )
    return int(y0 + rows_with_grass.min())


def compute_gap(
    baseline_png: Path,
    with_char_png: Path,
    threshold: int = DEFAULT_DIFF_THRESHOLD,
) -> tuple[int, int, int]:
    """Returns ``(char_bottom_y, grass_top_y, gap_px)`` where ``gap_px =
    char_bottom_y - grass_top_y``. Positive = character below grass top
    (sinking into ground). Negative = character above (floating)."""
    char_bottom, _sig_count = find_character_bottom_via_subtraction(
        baseline_png, with_char_png, threshold=threshold
    )
    grass_top = find_ground_top_via_subtraction(baseline_png, threshold=threshold)
    return char_bottom, grass_top, char_bottom - grass_top


# ───────────────────────────────────────────────────────────────────────────
# Constant rewriting — same target as V0, gated behind --apply.
# ───────────────────────────────────────────────────────────────────────────

_CONST_RE = re.compile(r"const val SPRITE_FOOT_OFFSET_EBO\s*=\s*([\-0-9.]+)f?")
_LINE_RE = re.compile(r"    const val SPRITE_FOOT_OFFSET_EBO    =[^\n]+")


def read_current_offset(kt_path: Path) -> float:
    """Parse SPRITE_FOOT_OFFSET_EBO from SpriteFactory.kt."""
    text = kt_path.read_text(encoding="utf-8")
    m = _CONST_RE.search(text)
    if not m:
        raise RuntimeError(
            "Could not find SPRITE_FOOT_OFFSET_EBO in SpriteFactory.kt — has "
            "the constant been renamed?"
        )
    return float(m.group(1))


def write_new_offset(kt_path: Path, new_value: float) -> str:
    """Rewrite SpriteFactory.kt with the new SPRITE_FOOT_OFFSET_EBO. Returns
    the old line for the report."""
    text = kt_path.read_text(encoding="utf-8")
    old_match = _LINE_RE.search(text)
    if not old_match:
        raise RuntimeError(
            "Could not find the SPRITE_FOOT_OFFSET_EBO line — has the source "
            "formatting changed? Expected '    const val SPRITE_FOOT_OFFSET_EBO    = ...'."
        )
    old_line = old_match.group(0)
    new_line = f"    const val SPRITE_FOOT_OFFSET_EBO    = {new_value:.3f}f"
    kt_path.write_text(text.replace(old_line, new_line), encoding="utf-8")
    return old_line


# ───────────────────────────────────────────────────────────────────────────


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument(
        "--baseline",
        type=Path,
        default=None,
        help=(
            "Path to the baseline (no-player) PNG. "
            "Default: <repo>/build/visual-checkpoints/level1-start-baseline.png"
        ),
    )
    parser.add_argument(
        "--with-char",
        type=Path,
        default=None,
        help=(
            "Path to the with-character PNG. "
            "Default: <repo>/build/visual-checkpoints/level1-start.png"
        ),
    )
    parser.add_argument(
        "--threshold",
        type=int,
        default=DEFAULT_DIFF_THRESHOLD,
        help=(
            "Significant-diff pixel threshold (sum-of-channel-abs). "
            f"Default {DEFAULT_DIFF_THRESHOLD}; lower if subtraction misses "
            "anti-aliased character edges, raise if torch flicker leaks in."
        ),
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help=(
            "Actually rewrite SPRITE_FOOT_OFFSET_EBO in SpriteFactory.kt. "
            "Default is dry-run: print the measurement only."
        ),
    )
    args = parser.parse_args(argv)

    repo_root = Path(__file__).resolve().parents[1]
    baseline_png = args.baseline or (
        repo_root / "build" / "visual-checkpoints" / "level1-start-baseline.png"
    )
    with_char_png = args.with_char or (
        repo_root / "build" / "visual-checkpoints" / "level1-start.png"
    )
    kt_path = (
        repo_root
        / "core" / "src" / "main" / "kotlin"
        / "com" / "sohai" / "platformer" / "rendering" / "SpriteFactory.kt"
    )

    for p, label in ((baseline_png, "baseline"), (with_char_png, "with-character")):
        if not p.exists():
            print(f"ERROR: {label} checkpoint PNG not found at {p}", file=sys.stderr)
            print(
                "See the module docstring for the two-step capture invocation "
                "(toggle `cloudy.captureBaseline=true` for the baseline run).",
                file=sys.stderr,
            )
            return 1

    char_bottom, grass_top, gap_px = compute_gap(
        baseline_png, with_char_png, threshold=args.threshold
    )
    correction_m = gap_px / PPM
    print(f"baseline: {baseline_png}")
    print(f"with-char: {with_char_png}")
    print(f"threshold: {args.threshold}")
    print(f"char_bottom_y={char_bottom}  grass_top_y={grass_top}")
    print(f"gap_px={gap_px}  ({correction_m * 100:+.1f} cm in world)")

    current_offset = read_current_offset(kt_path)
    # Sign rationale (mirrors V0): SPRITE_FOOT_OFFSET pushes the sprite DOWN
    # in render-space (subtract from sy). gap_px > 0 means the character is
    # rendered BELOW the grass top → sinking → we need to LIFT it → DECREASE
    # the offset. So new = current - correction_m. Clamp to >= 0.
    proposed = max(0.0, current_offset - correction_m)
    print(f"current SPRITE_FOOT_OFFSET_EBO = {current_offset:.3f}f")
    print(f"proposed SPRITE_FOOT_OFFSET_EBO = {proposed:.3f}f")

    if not args.apply:
        print("(dry-run; pass --apply to rewrite SpriteFactory.kt)")
        return 0

    old_line = write_new_offset(kt_path, proposed)
    print(f"Updated {kt_path}")
    print(f"  was: {old_line.strip()}")
    print(f"  now: const val SPRITE_FOOT_OFFSET_EBO    = {proposed:.3f}f")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

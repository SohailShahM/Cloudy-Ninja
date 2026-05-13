#!/usr/bin/env python3
"""Foot-offset autotuner V0.

Reads assets/build/visual-checkpoints/level1-start.png, locates the
character's visible feet and the ground tile top, computes the gap in
world meters, and writes a corrected SPRITE_FOOT_OFFSET_EBO to
SpriteFactory.kt.

This is V0 — hard-coded to the procedural-sprite Ebo at level1 spawn.
Future iterations: support all 3 characters, support post-T-186 MH1
sprites, support multiple checkpoint references, support a convergence
loop.

Run: python scripts/foot_offset_autotuner.py
"""
import re
from pathlib import Path

import numpy as np
from PIL import Image


# Pixels-per-meter for the rendered view (matches Constants.PPM).
PPM = 100.0
# Character search window in level1-start.png (x_min, y_min, x_max, y_max)
CHAR_WINDOW = (60, 600, 130, 720)
# Tile-top expected y range — scan for the brightest green-row inside this band
TILE_SEARCH_Y = (680, 720)
# Tolerance: if the gap is within this many pixels of zero, treat as converged
TOLERANCE_PX = 2


def find_character_bottom(png: Path) -> int:
    """Return the y-coord (image-coords, top-down) of the lowest opaque pixel
    inside CHAR_WINDOW that's NOT background. Heuristic: scan rows bottom-up
    for any pixel inside the character window that's NOT close to the
    background color. The bottom row containing such a pixel is the character's
    visible feet."""
    img = np.array(Image.open(png).convert("RGB"))
    x0, y0, x1, y1 = CHAR_WINDOW
    window = img[y0:y1, x0:x1, :]
    # Background heuristic: dark gray/black or the deep-blue HUD/menu chrome.
    # The character has tinted skin (orange-ish) / cloth (brown/red/etc) — NOT the
    # deep-blue (~10,30,60) menu chrome and NOT the near-black (~0,0,0) sky.
    # A pixel "is character" if any channel > 80 AND it's not the brown grass.
    r, g, b = window[..., 0], window[..., 1], window[..., 2]
    is_dark = (r < 50) & (g < 50) & (b < 50)        # sky / black HUD
    is_grass = (g > r) & (g > b) & (g > 80)          # green-ish grass top
    is_chrome = (b > 100) & (b > r) & (b > g)        # blue HUD chrome
    is_character = ~(is_dark | is_grass | is_chrome)
    # Find lowest row in window with any character pixel
    char_rows = np.any(is_character, axis=1)
    rows_with_char = np.where(char_rows)[0]
    if rows_with_char.size == 0:
        raise RuntimeError("No character pixels found in CHAR_WINDOW — heuristic failed.")
    char_bottom_y = y0 + int(rows_with_char.max())
    return char_bottom_y


def find_ground_top(png: Path) -> int:
    """Return the y-coord of the ground tile top inside the character's x band.
    Heuristic: scan the same x range, find the topmost green-ish pixel in
    TILE_SEARCH_Y."""
    img = np.array(Image.open(png).convert("RGB"))
    x0, _, x1, _ = CHAR_WINDOW
    y0, y1 = TILE_SEARCH_Y
    window = img[y0:y1, x0:x1, :]
    r, g, b = window[..., 0], window[..., 1], window[..., 2]
    is_grass = (g > r) & (g > b) & (g > 80)
    grass_rows = np.any(is_grass, axis=1)
    rows_with_grass = np.where(grass_rows)[0]
    if rows_with_grass.size == 0:
        raise RuntimeError("No grass pixels found in TILE_SEARCH_Y — heuristic failed.")
    grass_top_y = y0 + int(rows_with_grass.min())
    return grass_top_y


def compute_offset_correction(png: Path) -> tuple[int, float]:
    """Return (gap_px, correction_m). gap_px > 0 = character below grass top
    (sinking); < 0 = above (floating). correction_m is in world meters to
    ADD to SPRITE_FOOT_OFFSET_EBO (positive = push sprite UP, ie REDUCE the
    current value)."""
    char_bottom = find_character_bottom(png)
    grass_top = find_ground_top(png)
    # If character bottom is BELOW grass top → sinking → REDUCE offset
    # If character bottom is ABOVE grass top → floating → INCREASE offset
    gap_px = char_bottom - grass_top
    correction_m = gap_px / PPM  # positive = push DOWN ... wait check sign
    # SPRITE_FOOT_OFFSET pushes sprite DOWN (subtract from sy). Increasing
    # the offset pushes the sprite further down = INTO the ground. If sprite
    # is currently BELOW ground (sinking, gap_px > 0), we need to LIFT the
    # sprite — decrease the offset. So correction_m should be SUBTRACTED.
    # Caller does: new_offset = old_offset - correction_m
    return gap_px, correction_m


def read_current_offset(kt_path: Path) -> float:
    """Parse SPRITE_FOOT_OFFSET_EBO from SpriteFactory.kt."""
    text = kt_path.read_text(encoding="utf-8")
    m = re.search(r"const val SPRITE_FOOT_OFFSET_EBO\s*=\s*([\-0-9.]+)f?", text)
    if not m:
        raise RuntimeError("Could not find SPRITE_FOOT_OFFSET_EBO in SpriteFactory.kt")
    return float(m.group(1))


def write_new_offset(kt_path: Path, new_value: float) -> str:
    """Rewrite SpriteFactory.kt with the new SPRITE_FOOT_OFFSET_EBO. Returns
    the old line for the report."""
    text = kt_path.read_text(encoding="utf-8")
    new_line = f"    const val SPRITE_FOOT_OFFSET_EBO    = {new_value:.3f}f"
    old_match = re.search(r"    const val SPRITE_FOOT_OFFSET_EBO    =[^\n]+", text)
    if not old_match:
        raise RuntimeError("Could not find the SPRITE_FOOT_OFFSET_EBO line")
    old_line = old_match.group(0)
    text = text.replace(old_line, new_line)
    kt_path.write_text(text, encoding="utf-8")
    return old_line


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    png_path = repo_root / "assets" / "build" / "visual-checkpoints" / "level1-start.png"
    kt_path = (
        repo_root
        / "core"
        / "src"
        / "main"
        / "kotlin"
        / "com"
        / "sohai"
        / "platformer"
        / "rendering"
        / "SpriteFactory.kt"
    )

    if not png_path.exists():
        print(f"ERROR: checkpoint PNG not found at {png_path}")
        print(
            "Run: ./gradlew :lwjgl3:run -Dcloudy.smoke=true "
            "-Dcloudy.smokeLevel=level1 -Dcloudy.captureCheckpoints=true"
        )
        return 1

    gap_px, correction_m = compute_offset_correction(png_path)
    print(
        f"Character bottom vs grass top: gap_px={gap_px} "
        f"({correction_m*100:+.1f} cm in world)"
    )
    if abs(gap_px) <= TOLERANCE_PX:
        print(f"Within tolerance ({TOLERANCE_PX}px). No change needed.")
        return 0

    old_offset = read_current_offset(kt_path)
    new_offset = max(0.0, old_offset - correction_m)
    print(f"Current SPRITE_FOOT_OFFSET_EBO = {old_offset:.3f}")
    print(f"Computed correction = {correction_m:+.3f}m")
    print(f"New value = {new_offset:.3f}")

    old_line = write_new_offset(kt_path, new_offset)
    print(f"Updated {kt_path}")
    print(f"  was: {old_line.strip()}")
    print(f"  now: const val SPRITE_FOOT_OFFSET_EBO    = {new_offset:.3f}f")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

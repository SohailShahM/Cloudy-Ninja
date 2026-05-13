#!/usr/bin/env python3
"""Pixel-diff between two checkpoint PNG dirs. Emits a markdown report."""
import argparse
import sys
from pathlib import Path
from PIL import Image
import numpy as np


def diff_image(a: Path, b: Path) -> float:
    """Return % of pixels that differ (0.0 - 100.0)."""
    img_a = np.array(Image.open(a).convert("RGBA"))
    img_b = np.array(Image.open(b).convert("RGBA"))
    if img_a.shape != img_b.shape:
        return 100.0  # size mismatch = full diff
    diff = np.any(img_a != img_b, axis=-1)
    return float(diff.mean() * 100.0)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--baseline", type=Path, required=True)
    p.add_argument("--head", type=Path, required=True)
    p.add_argument("--threshold", type=float, default=5.0)
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()

    baseline_files = sorted(args.baseline.glob("*.png")) if args.baseline.exists() else []
    head_files = {p.name: p for p in args.head.glob("*.png")} if args.head.exists() else {}

    rows = []
    flagged = 0
    for b in baseline_files:
        h = head_files.get(b.name)
        if h is None:
            rows.append(f"| {b.name} | MISSING in PR | — | ⚠️ removed |")
            flagged += 1
            continue
        pct = diff_image(b, h)
        flag = "\U0001f534" if pct >= args.threshold else "✅"
        if pct >= args.threshold:
            flagged += 1
        rows.append(f"| {b.name} | {pct:.2f}% | {args.threshold:.1f}% | {flag} |")

    # New checkpoints only in PR head:
    for name, h in head_files.items():
        if not (args.baseline / name).exists():
            rows.append(f"| {name} | NEW in PR | — | ℹ️ added |")

    if not baseline_files and not head_files:
        body = (
            f"## Visual regression report\n\n"
            f"**Threshold: {args.threshold:.1f}% pixels changed**\n\n"
            f"No checkpoints captured on either side (baseline or PR head). "
            f"This usually means the smoke autopilot did not run to completion in CI. "
            f"Check the workflow logs for the capture steps.\n"
        )
    else:
        body = (
            f"## Visual regression report\n\n"
            f"**Threshold: {args.threshold:.1f}% pixels changed**\n\n"
            f"**{flagged} of {len(baseline_files)} checkpoints flagged.**\n\n"
            f"| Checkpoint | Diff | Threshold | Status |\n"
            f"|---|---|---|---|\n"
            + "\n".join(rows)
            + "\n\n"
            f"Artifacts (baseline + head PNGs + this report) uploaded to "
            f"the Actions run for inspection.\n"
        )

    args.output.write_text(body, encoding="utf-8")
    print(f"Wrote diff report ({flagged} flagged checkpoints)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

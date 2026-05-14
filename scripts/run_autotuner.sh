#!/usr/bin/env bash
# T-A18: orchestrates the foot-offset autotuner V1 — baseline-subtraction.
#
# Drives the game TWICE (baseline + with-player) under the visual-checkpoint
# capture flag, then runs the V1 analyzer against the resulting PNGs.
#
# Linux/WSL/macOS. For native-Windows PowerShell use `scripts/run_autotuner.ps1`.
#
# Usage:
#   bash scripts/run_autotuner.sh            # dry-run: prints gap_px, no rewrite
#   bash scripts/run_autotuner.sh --apply    # rewrite SPRITE_FOOT_OFFSET_EBO
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

APPLY_FLAG=""
if [[ "${1:-}" == "--apply" ]]; then
    APPLY_FLAG="--apply"
fi

CAPTURE_DIR="build/visual-checkpoints"
BASELINE_PNG="$CAPTURE_DIR/level1-start-baseline.png"
WITH_CHAR_PNG="$CAPTURE_DIR/level1-start.png"

echo "[autotuner] step 1/3: capture BASELINE (no player render)"
./gradlew :lwjgl3:run \
    -Dcloudy.smoke=true \
    -Dcloudy.smokeLevel=level1 \
    -Dcloudy.smokeMode=true \
    -Dcloudy.captureCheckpoints=true \
    -Dcloudy.captureBaseline=true

if [[ ! -f "$BASELINE_PNG" ]]; then
    echo "[autotuner] ERROR: baseline PNG missing at $BASELINE_PNG"
    exit 1
fi

echo "[autotuner] step 2/3: capture WITH-PLAYER (normal)"
./gradlew :lwjgl3:run \
    -Dcloudy.smoke=true \
    -Dcloudy.smokeLevel=level1 \
    -Dcloudy.smokeMode=true \
    -Dcloudy.captureCheckpoints=true

if [[ ! -f "$WITH_CHAR_PNG" ]]; then
    echo "[autotuner] ERROR: with-player PNG missing at $WITH_CHAR_PNG"
    exit 1
fi

echo "[autotuner] step 3/3: run V1 analyzer"
python3 scripts/foot_offset_autotuner_v1.py $APPLY_FLAG

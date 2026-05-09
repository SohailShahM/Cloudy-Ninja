"""
Cloudy Ninja — Automated Test + Monitor Script
Polls TASK_SPEC.md every 30s. When COMPLETED_BY_COPILOT detected:
  1. Launches the game, captures stdout/stderr for 20s
  2. Parses log for errors, key events
  3. Writes analysis to .github/logs/analysis_<timestamp>.md
  4. Archives completed task and updates TASK_SPEC.md status to ARCHIVED

Usage: python .github/scripts/monitor_and_test.py
Run from project root.
"""

import subprocess, time, re, os, sys
from datetime import datetime
from pathlib import Path

TASK_SPEC   = Path(".github/TASK_SPEC.md")
LOG_DIR     = Path(".github/logs")
GAME_CMD    = ["cmd", "/c", "gradlew.bat", "lwjgl3:run"]
POLL_SEC    = 30
RUN_TIMEOUT = 25  # seconds to let the game run before killing

LOG_DIR.mkdir(parents=True, exist_ok=True)

# ─── Patterns ──────────────────────────────────────────────────────────────
ERRORS    = re.compile(r"(Exception|Error|FATAL|NullPointer|ClassCast)", re.I)
EVENTS    = re.compile(r"(Checkpoint activated|Respawning player|Seed Slam|Wind Dash|Switched to|Spawning)", re.I)
WARN      = re.compile(r"(WARN|warning)", re.I)

def read_spec():
    return TASK_SPEC.read_text(encoding="utf-8") if TASK_SPEC.exists() else ""

def get_status(text):
    m = re.search(r"\*\*Status:\*\*\s*`([^`]+)`", text)
    return m.group(1).strip() if m else "UNKNOWN"

def get_task_id(text):
    m = re.search(r"## Task ID:\s*(TASK-\d+)", text)
    return m.group(1) if m else "TASK-???"

def launch_and_capture():
    ts   = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_path = LOG_DIR / f"game_run_{ts}.log"
    print(f"[Monitor] Launching game... output → {log_path}")
    with open(log_path, "w", encoding="utf-8") as lf:
        proc = subprocess.Popen(
            GAME_CMD, stdout=lf, stderr=subprocess.STDOUT,
            cwd=Path(".").resolve(), creationflags=subprocess.CREATE_NEW_PROCESS_GROUP
        )
        try:
            proc.wait(timeout=RUN_TIMEOUT)
        except subprocess.TimeoutExpired:
            proc.terminate()
            print(f"[Monitor] Game killed after {RUN_TIMEOUT}s (expected).")
    return log_path

def analyse_log(log_path: Path, task_id: str):
    lines       = log_path.read_text(encoding="utf-8", errors="replace").splitlines()
    errors      = [l for l in lines if ERRORS.search(l)]
    events      = [l for l in lines if EVENTS.search(l)]
    warnings    = [l for l in lines if WARN.search(l)]

    ts          = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    analysis    = LOG_DIR / f"analysis_{log_path.stem}.md"

    status = "✅ PASS" if not errors else "❌ FAIL"

    with open(analysis, "w", encoding="utf-8") as f:
        f.write(f"# Test Analysis — {task_id}\n")
        f.write(f"**Run:** {ts}  |  **Result:** {status}\n\n")
        f.write(f"## Key Game Events\n")
        f.writelines(f"- `{e.strip()}`\n" for e in events) if events else f.write("- *(none detected)*\n")
        f.write(f"\n## Warnings ({len(warnings)})\n")
        f.writelines(f"- `{w.strip()}`\n" for w in warnings[:10]) if warnings else f.write("- *(none)*\n")
        f.write(f"\n## Errors ({len(errors)})\n")
        f.writelines(f"- `{e.strip()}`\n" for e in errors[:20]) if errors else f.write("- *(none — clean run!)*\n")
        f.write(f"\n## Raw Log\n`{log_path}`\n")

    print(f"[Monitor] Analysis written → {analysis}")
    return analysis, errors

def archive_task(spec_text: str, task_id: str, errors: list, analysis_path: Path):
    """Mark task ARCHIVED and append summary to archive section."""
    result_str = "PASS — no errors" if not errors else f"FAIL — {len(errors)} error(s), see {analysis_path.name}"
    updated = spec_text.replace(
        "**Status:** `COMPLETED_BY_COPILOT`",
        f"**Status:** `ARCHIVED`"
    )
    archive_entry = (
        f"\n---\n### {task_id} — Archived {datetime.now().strftime('%Y-%m-%d %H:%M')}\n"
        f"Result: {result_str}  \nAnalysis: `.github/logs/{analysis_path.name}`\n"
    )
    # Append to the archive section
    updated = updated.replace("*(No completed tasks yet.)*", "").rstrip()
    updated += archive_entry
    TASK_SPEC.write_text(updated, encoding="utf-8")
    print(f"[Monitor] {task_id} archived in TASK_SPEC.md")

def main():
    print(f"[Monitor] Started. Polling every {POLL_SEC}s ...")
    last_status = None
    while True:
        spec = read_spec()
        status = get_status(spec)

        if status != last_status:
            print(f"[Monitor] Status changed → '{status}'")
            last_status = status

        if status == "COMPLETED_BY_COPILOT":
            task_id = get_task_id(spec)
            print(f"[Monitor] {task_id} complete! Running game test...")
            log_path             = launch_and_capture()
            analysis_path, errors = analyse_log(log_path, task_id)
            archive_task(spec, task_id, errors, analysis_path)
            last_status = "ARCHIVED"

        time.sleep(POLL_SEC)

if __name__ == "__main__":
    main()

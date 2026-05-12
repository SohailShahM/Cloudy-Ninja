# Self-hosted GitHub Actions runner — setup guide

> Setting up a GitHub Actions runner on this dev machine. Saves the metered Actions minutes that kicked in when `SohailShahM/Cloudy-Ninja` flipped from public → private at end-of-session 2026-05-12.

## Why we did this

After the visibility flip, Actions runs on private repos started rejecting at the workflow-startup level (zero steps recorded, BlobNotFound on logs) — classic symptom of the personal-account Free plan's `$0` default spending limit blocking private Actions even within the free 2,000-min tier. Three options were on the table: raise the spending limit, flip back to public, or run a self-hosted runner. We picked self-hosted.

## What you're setting up

A long-lived GitHub Actions runner process on this Windows machine, running inside **WSL2 Ubuntu**. The runner pulls jobs from GitHub when our CI workflows trigger, executes them locally (no Actions minutes billed), and reports results back to GitHub. Functionally identical to `ubuntu-latest` GitHub-hosted runners — same Linux, same gradle, same xvfb — just running on your hardware.

**Costs:** electricity + your machine's CPU during CI runs (~5 min wall × ~30 PRs/month ≈ ~2.5 hours of background CPU/month). **Doesn't cost cash.**

**Trade-offs:** if your machine is OFF, CI jobs queue and wait until it's back. If you're doing heavy dev work mid-CI, the runner competes for CPU.

## Setup steps (one-time, ~30 min)

### 1. Verify / enable WSL2

In an elevated PowerShell window:

```powershell
wsl --status         # If WSL2 is not installed, run:  wsl --install -d Ubuntu-22.04
wsl --update         # ensure kernel is current
wsl --set-default-version 2
```

Reboot if `wsl --install` asked you to. Then open a WSL Ubuntu shell (Start menu → "Ubuntu") and continue inside it.

### 2. Inside the WSL Ubuntu shell — install dependencies

```bash
sudo apt-get update
sudo apt-get install -y curl tar git xvfb openjdk-21-jdk-headless build-essential
# Verify:
java -version    # should print openjdk 21
xvfb-run --help  # should print xvfb-run usage
```

### 3. Create a runner workdir + download the runner

```bash
mkdir -p ~/actions-runner && cd ~/actions-runner
curl -o actions-runner-linux-x64.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.330.0/actions-runner-linux-x64-2.330.0.tar.gz
# Validate (optional): the expected SHA-256 is on the release page
tar xzf actions-runner-linux-x64.tar.gz
```

### 4. Register the runner with the repo

Use the fresh registration token from your Claude Code session (valid ~1 hour from issue). If the token has expired, regenerate via:

```bash
gh api -X POST repos/SohailShahM/Cloudy-Ninja/actions/runners/registration-token --jq .token
```

Then configure:

```bash
./config.sh \
  --url https://github.com/SohailShahM/Cloudy-Ninja \
  --token <PASTE_TOKEN_HERE> \
  --name cloudy-ninja-wsl \
  --labels self-hosted,linux,x64 \
  --work _work \
  --unattended
```

Verify registration on the GitHub side:

```bash
gh api repos/SohailShahM/Cloudy-Ninja/actions/runners --jq '.runners[] | {name, status, labels: [.labels[].name]}'
```

Should show one entry with `status: "offline"` (we haven't started it yet).

### 5. Start the runner

For a quick smoke test, run it in foreground first:

```bash
./run.sh
```

Leave that terminal open. In another WSL shell, verify it shows online:

```bash
gh api repos/SohailShahM/Cloudy-Ninja/actions/runners --jq '.runners[] | {name, status}'
# Should now show:  {"name":"cloudy-ninja-wsl","status":"online"}
```

If you `Ctrl+C` the `./run.sh`, the runner goes back offline. For persistent operation, install it as a systemd service:

```bash
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
# To stop:  sudo ./svc.sh stop
# To uninstall the service entirely:  sudo ./svc.sh uninstall
```

WSL2 caveat: systemd inside WSL2 needs `systemd=true` in `/etc/wsl.conf` for the `svc.sh` approach to work cleanly. If that's missing, add it then `wsl --shutdown` from PowerShell and reopen Ubuntu.

### 6. Workflow change (this is what makes it kick in)

After your runner is online, Claude Code will open a small PR updating both workflows from `runs-on: ubuntu-latest` → `runs-on: [self-hosted, linux, x64]`. Until that PR lands, CI still routes to GitHub-hosted runners and stays blocked.

The PR is held until the runner status check confirms `online` — otherwise jobs queue indefinitely with no runner to claim them.

## Operations

### Daily usage
- Runner runs in the background (via systemd). PRs trigger CI as before. Watch via `gh pr checks <N>` same as always.
- When the machine sleeps or you `wsl --shutdown`, the runner goes offline. Jobs queue. They pick up when WSL comes back.

### Monitor runner status
```bash
gh api repos/SohailShahM/Cloudy-Ninja/actions/runners --jq '.runners[] | {name, status, busy}'
```

### View runner logs (if jobs misbehave)
```bash
sudo journalctl -u 'actions.runner.*' -f
```
Or `~/actions-runner/_diag/Runner_*.log` for older entries.

### Disk usage
The runner caches gradle dirs under `~/actions-runner/_work/`. Over time this can grow to several GB. Periodically prune:

```bash
cd ~/actions-runner
sudo ./svc.sh stop
rm -rf _work/Cloudy-Ninja/Cloudy-Ninja
sudo ./svc.sh start
```

(Drops the per-run workspace. Next job re-clones. Doesn't lose anything important.)

### Roll back (if you ever want to)

```bash
cd ~/actions-runner
sudo ./svc.sh stop && sudo ./svc.sh uninstall
./config.sh remove --token <fresh-removal-token>   # regenerate with the API
```

Then flip the workflow `runs-on:` back to `ubuntu-latest` and raise the spending limit at https://github.com/settings/billing/spending_limit so hosted-runner Actions work.

## Security note

A self-hosted runner executes whatever's in your workflow files on your machine. For a solo private repo this is minimal risk, but **never make this repo public again without removing this runner first** — public repos can have PRs from anyone, and `pull_request` workflows on a public repo with a self-hosted runner = arbitrary code execution on your machine by random contributors.

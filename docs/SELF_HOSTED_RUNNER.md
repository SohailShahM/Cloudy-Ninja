# Self-hosted GitHub Actions runner — setup guide

> Documentation for the future case where you flip `SohailShahM/Cloudy-Ninja` back to private and want to bypass the Actions-quota wall. The repo is currently public, so GitHub-hosted runners are free + unlimited and this guide is **not active** — it sits here as reference.

## Why you'd want this

You'd want this when **all three** are true:

1. The repo is private (or you plan to make it private).
2. Your shipping volume burns your Actions quota each cycle (Education Pack gives 3,000 min/mo via GitHub Pro).
3. You don't want to pay overage charges (~$0.008/min Linux on private).

If any one of those isn't true, you don't need this. Stay on GitHub-hosted runners.

## ⚠ Critical security rule — read before doing anything

**Self-hosted runners are safe on private repos. They're dangerous on public repos.** Pull-request workflows on a public repo execute whatever code is in the PR branch — meaning a stranger opening a PR can run arbitrary code on your runner hardware.

**Never wire self-hosted runners into a repo that's currently public** without one of these mitigations:

- Switch workflows from `on: pull_request` to `on: workflow_run` (only run after a maintainer-approved push)
- Add **environment protection rules** with manual approval gates on every workflow
- Restrict workflows to PRs from specific approved contributors

For Cloudy Ninja's actual usage pattern, the simpler rule is: **only attach self-hosted runners when the repo is private**. Don't make this repo public again with self-hosted runners attached.

---

## Two deployment paths

| | Path A: Remote PC with multiple VMs (recommended) | Path B: WSL2 on this Windows dev machine |
|---|---|---|
| Hardware | Always-on remote PC capable of running 3–8 VMs | Your daily-driver Windows laptop/desktop |
| Always-on? | Yes — independent of your work patterns | No — runner offline when you sleep/shutdown WSL |
| Slows dev work? | No — runs on different machine | Yes — competes for CPU during CI |
| Parallelism | 3–8 way (matches GitHub-hosted matrix) | 1-way (single runner) |
| Setup time | ~60 min (VMs + 3-8 runners) | ~30 min (WSL + 1 runner) |
| Maintenance | Periodic VM updates + disk cleanup | Same + WSL state hygiene |
| When to choose | Sustained CI capacity, multi-PR parallelism, no competition with dev work | Quick standup, occasional CI, no remote hardware available |

---

## Path A: Remote PC with multiple VMs (recommended)

### Architecture choice — start with 3 VMs

For Cloudy Ninja's current 8-job smoke matrix:
- **3 VMs:** queues 8 jobs across 3 runners; total wall time ~10-12 min (close to GitHub-hosted parity)
- **5 VMs:** ~7-8 min wall time
- **8 VMs:** full parity with GitHub-hosted; ~5 min wall time
- **1 beefy VM with 3-8 runner processes** in parallel directories: cheaper resource overhead (single OS), but a single VM crash takes out the whole pool

**Recommended starting point: 3 separate VMs.** Failure-isolated, easy to add more later. Upgrade to 8 if you want the wall-time win.

### VM specs per runner

- **OS:** Ubuntu 22.04 LTS (matches `ubuntu-latest` GitHub-hosted runners — same toolchain expectations)
- **CPU:** 2 vCPUs minimum, 4 vCPUs comfortable (gradle parallelism + xvfb + game)
- **RAM:** 4 GB minimum, 8 GB comfortable (gradle is memory-hungry on big projects)
- **Disk:** 20 GB minimum (gradle cache + git workspace + xvfb scratch)
- **Network:** outbound HTTPS to `github.com` (no inbound ports needed)

### Setup per VM (repeat for each)

#### 1. SSH into the remote PC + spin up a VM

Whatever your remote PC's hypervisor is (Proxmox, ESXi, VirtualBox, QEMU/KVM, Hyper-V…), provision a fresh Ubuntu 22.04 VM with the specs above. Give each a distinct hostname:

```
cloudy-runner-1
cloudy-runner-2
cloudy-runner-3
```

(Hostnames just for your own bookkeeping — GitHub uses the runner-name label, set below.)

#### 2. SSH into the VM and install dependencies

```bash
sudo apt-get update
sudo apt-get install -y curl tar git xvfb openjdk-21-jdk-headless build-essential
# Verify:
java -version    # should print openjdk 21
xvfb-run --help  # should print xvfb-run usage
```

#### 3. Download the runner binary

```bash
mkdir -p ~/actions-runner && cd ~/actions-runner
curl -o actions-runner-linux-x64.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.330.0/actions-runner-linux-x64-2.330.0.tar.gz
tar xzf actions-runner-linux-x64.tar.gz
```

(Check https://github.com/actions/runner/releases for newer versions when you set this up.)

#### 4. Get a fresh registration token

Run from any machine that has `gh` authenticated as you:

```bash
gh api -X POST repos/SohailShahM/Cloudy-Ninja/actions/runners/registration-token --jq .token
```

The token is valid for ~1 hour. Generate one per VM (single-use).

#### 5. Register the runner with a unique name + shared labels

```bash
./config.sh \
  --url https://github.com/SohailShahM/Cloudy-Ninja \
  --token <PASTE_FRESH_TOKEN_HERE> \
  --name cloudy-runner-1 \         # change to -2, -3, etc. per VM
  --labels self-hosted,linux,x64 \
  --work _work \
  --unattended
```

The **labels must be identical** across all VMs so GitHub's scheduler treats them as interchangeable in the matrix. The **name must be unique** so you can track them individually.

#### 6. Install as a systemd service (so it persists across reboots)

```bash
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

#### 7. Verify all runners are online

From your local machine:

```bash
gh api repos/SohailShahM/Cloudy-Ninja/actions/runners --jq '.runners[] | {name, status, labels: [.labels[].name]}'
```

Should show 3 entries, all `status: "online"`.

### "Swap in flight" — the public → private + self-hosted choreography

The trick to avoid CI downtime when you flip private:

1. **While still public,** spin up the VMs + register all 3 runners. They sit idle (no jobs to grab — workflows still target `ubuntu-latest`).
2. **Open a PR** that changes both `.github/workflows/ai-smoke.yml` and `.github/workflows/ci.yml` from `runs-on: ubuntu-latest` → `runs-on: [self-hosted, linux, x64]`. **Don't merge it yet.**
3. **Verify the runners show `status: "online"`** via the gh API command above.
4. **Flip the repo private:**
   ```
   gh repo edit SohailShahM/Cloudy-Ninja --visibility private --accept-visibility-change-consequences
   ```
5. **Merge the workflow PR immediately.** All subsequent CI dispatches go to your self-hosted runners. Zero quota concerns.
6. Test with one doc-only PR + one code-touching PR to confirm both code paths work on self-hosted.

If anything goes wrong at step 5, you can flip back to public (`--visibility public`) and you're back to GitHub-hosted unlimited, no harm done.

---

## Path B: WSL2 on this Windows dev machine

Use this only if you don't have remote hardware available. Same steps as Path A but on the local Windows machine via WSL2. Worse trade-offs (runner offline when laptop sleeps; competes with dev work for CPU) but cheaper to set up.

### 1. Enable WSL2 (elevated PowerShell)

```powershell
wsl --install -d Ubuntu-22.04
wsl --update
wsl --set-default-version 2
```

Reboot if `wsl --install` asked you to. Then open the WSL Ubuntu shell ("Ubuntu" in Start menu).

### 2-6. Same as Path A steps 2-6

Run the apt install, runner download, token fetch, `config.sh` (use name `cloudy-ninja-wsl`), `svc.sh install` inside the WSL Ubuntu shell.

**WSL2 systemd caveat:** `sudo ./svc.sh install` requires systemd inside WSL2, which needs `[boot]\nsystemd=true` in `/etc/wsl.conf` plus `wsl --shutdown` from PowerShell and reopen Ubuntu. Without that, the service install won't work; you can still run the runner foreground via `./run.sh` for testing.

### 7. The workflow swap (same as Path A)

Open the workflow PR + flip private + merge — same choreography.

---

## Operations (applies to both paths)

### Daily usage
Runners run in the background via systemd. PRs trigger CI as before. Watch via `gh pr checks <N>` same as always.

### Monitor runner status
```bash
gh api repos/SohailShahM/Cloudy-Ninja/actions/runners --jq '.runners[] | {name, status, busy}'
```

`busy: true` means the runner is currently executing a job. `status: "offline"` means the runner process isn't reachable — check the VM is up and the systemd service is running.

### View runner logs
```bash
sudo journalctl -u 'actions.runner.*' -f
```
Or `~/actions-runner/_diag/Runner_*.log` for older entries.

### Disk usage maintenance
The runner caches gradle dirs under `~/actions-runner/_work/`. Over time this grows to several GB. Periodically per VM:

```bash
cd ~/actions-runner
sudo ./svc.sh stop
rm -rf _work/Cloudy-Ninja/Cloudy-Ninja
sudo ./svc.sh start
```

(Drops the per-run workspace. Next job re-clones. Doesn't lose anything important.)

### Updating the runner binary

Periodically (every few months or when GitHub deprecates older versions):

```bash
cd ~/actions-runner
sudo ./svc.sh stop
./config.sh remove --token <fresh-removal-token>
# Re-download the new version from https://github.com/actions/runner/releases
# Re-run config.sh + svc.sh install
```

### Roll back to GitHub-hosted

```bash
cd ~/actions-runner
sudo ./svc.sh stop && sudo ./svc.sh uninstall
./config.sh remove --token <fresh-removal-token>
```

Then flip the workflow `runs-on:` back to `ubuntu-latest`. If the repo is still private, raise the spending limit at https://github.com/settings/billing/spending_limit so hosted-runner Actions work. Or flip the repo public.

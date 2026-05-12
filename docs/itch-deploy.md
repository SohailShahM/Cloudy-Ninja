# itch.io deploy — setup + usage

> One-page setup guide for the `itch-deploy` GitHub Actions workflow. Covers itch.io page creation, API-key generation, secret installation, and how to fire the workflow. The workflow itself lives at `.github/workflows/itch-deploy.yml`; the shell wrapper at `scripts/deploy-itch.sh`.

## What this is

A **manual-trigger** workflow that builds the desktop JAR (`./gradlew :lwjgl3:dist`), downloads itch.io's [butler](https://itch.io/docs/butler/) CLI, and uploads the JAR to your itch.io project page. The workflow runs only when you click "Run workflow" in the Actions tab (or invoke `gh workflow run`) — it never fires on push or pull request, so merging to `main` does not publish a build.

## One-time setup

### 1. Create the itch.io project page

1. Sign in at https://itch.io as `sohailshahm` (or the account that will own this title).
2. Go to https://itch.io/game/new.
3. Fill in:
   - **Title:** Cloudy Ninja
   - **Project URL:** `cloudy-ninja` (this becomes `sohailshahm.itch.io/cloudy-ninja`)
   - **Classification:** Games
   - **Kind of project:** Downloadable
   - **Pricing:** whatever you want — `$0 or donate` is the typical alpha-stage choice
   - **Visibility:** Draft, until you are ready to publish
4. Save the page. You can come back later to fill in the screenshots, description, devlog, etc. The presskit scaffold at `marketing/presskit/` (T-077) has copy you can paste.

The full deploy target slug — used by butler — is `sohailshahm/cloudy-ninja`. If you change the project URL, update `ITCH_TARGET` in `scripts/deploy-itch.sh` (or pass it via the env var on a per-run basis).

### 2. Generate an API key

1. Go to https://itch.io/user/settings/api-keys while signed in as `sohailshahm`.
2. Click **Generate new API key**. Give it a label like `cloudy-ninja-ci`.
3. Copy the key. You won't be able to see it again — if you lose it, revoke it and generate a new one.

The API key authorizes butler to push uploads to any project owned by `sohailshahm`. Treat it like a password.

### 3. Install the key as a repo secret

```bash
# From a terminal where you're authenticated with `gh`:
gh secret set ITCH_API_KEY --repo SohailShahM/Cloudy-Ninja
# Paste the key when prompted, then Enter, then Ctrl-D (or Ctrl-Z + Enter on Windows).
```

Or via the GitHub UI: **Settings → Secrets and variables → Actions → New repository secret** → name `ITCH_API_KEY`, value the API key from step 2.

The workflow has a guard step that fails fast with a useful error if the secret is missing, so you'll know immediately if step 3 was skipped.

## Running a deploy

### Via the GitHub UI

1. Go to https://github.com/SohailShahM/Cloudy-Ninja/actions/workflows/itch-deploy.yml.
2. Click **Run workflow** in the top-right.
3. Pick the branch (usually `main`).
4. Fill in inputs:
   - **channel** (default `desktop`): the itch.io channel slug. Common values are `desktop` (one cross-platform JAR), or `windows` / `linux` / `mac` if you start producing per-OS builds via the `jarWin` / `jarLinux` / `jarMac` tasks already defined in `lwjgl3/build.gradle`.
   - **version-tag** (optional): the version label shown to players on the itch page (e.g. `0.1.0-alpha`). If left empty, the workflow uses the short commit SHA.
5. Click **Run workflow**.

### Via the CLI

```bash
# Build channel=desktop, version tag from short SHA:
gh workflow run itch-deploy.yml --repo SohailShahM/Cloudy-Ninja

# Explicit channel + version:
gh workflow run itch-deploy.yml \
  --repo SohailShahM/Cloudy-Ninja \
  -f channel=desktop \
  -f version-tag=0.1.0-alpha
```

Track the run with `gh run watch` or in the Actions tab.

## What the workflow does

```
1. Checkout
2. Verify ITCH_API_KEY secret is set (fail-fast if not)
3. Set up JDK 17 (temurin) with gradle cache
4. ./gradlew :lwjgl3:dist  →  lwjgl3/build/libs/Cloudy Ninja-<version>.jar
5. Locate the built JAR
6. Resolve version tag (input override, else short SHA)
7. Run scripts/deploy-itch.sh:
   - Download butler for linux-amd64 into .butler/
   - butler push <jar> sohailshahm/cloudy-ninja:<channel> --userversion <tag>
8. Upload the JAR as a workflow artifact (so you have a copy without re-running gradle)
```

Concurrency is set to `group: itch-deploy, cancel-in-progress: false`. If you click Run twice in quick succession, the second run queues behind the first — we never want a half-finished upload getting cancelled mid-push.

## Running it locally (no CI)

Useful for debugging or one-off pushes. From the repo root:

```bash
export BUTLER_API_KEY="<your-key>"   # same value as the ITCH_API_KEY secret
./gradlew :lwjgl3:dist
export ITCH_JAR="lwjgl3/build/libs/Cloudy Ninja-1.0.0.jar"
export ITCH_CHANNEL="desktop"
export ITCH_VERSION_TAG="0.1.0-alpha"
bash scripts/deploy-itch.sh
```

The script auto-installs butler into `.butler/` (already gitignored if you keep the existing `.butler/` out of source control — add it to `.gitignore` if you run this locally and want to keep the dir out of git).

## Channels — what to pick

itch.io uses **channels** to differentiate downloads on the same project page. butler infers the platform from the channel name when the name contains a hint:

- `desktop` — neutral; itch will tag it as "All platforms" unless butler detects platform-specific files in the upload
- `windows` / `windows-x64` — Windows
- `linux` / `linux-x64` — Linux
- `mac` / `osx` — macOS

For the alpha launch, the simplest path is one `desktop` channel with the cross-platform JAR (`./gradlew :lwjgl3:dist`). Players need a JRE installed — that limitation is documented on the itch page.

If you later want platform-bundled executables (no JRE required), the `construo` plugin is already configured in `lwjgl3/build.gradle` for `linuxX64`, `macM1`, `macX64`, `winX64`. A future ticket can add a matrix step to this workflow that builds each target and pushes to its own channel. Out of scope for T-114.

## Troubleshooting

**"ITCH_API_KEY is not set"** — Step 3 of setup wasn't done, or the secret was set on the wrong repo (e.g. a fork). Verify with `gh secret list --repo SohailShahM/Cloudy-Ninja`.

**"butler: command not found"** — The script installs butler into `.butler/butler` on each run. If you see this error locally, check that `curl` and `unzip` are available on your PATH.

**"403 Forbidden" from butler push** — The API key is invalid or doesn't have permission on the target. Generate a new key at https://itch.io/user/settings/api-keys and update the secret.

**"target sohailshahm/cloudy-ninja does not exist"** — The itch.io project page hasn't been created yet, or the URL slug is different. Fix the page first; butler does not auto-create projects.

**The JAR uploads but won't run for players** — Players need Java installed (Java 8+; we target Java 8 source compatibility but the JAR will run on any newer JRE). Add a note to the itch page or switch to construo-built bundles in a follow-up ticket.

## References

- itch.io docs: https://itch.io/docs/butler/
- butler push command: https://itch.io/docs/butler/pushing.html
- butler channels & platforms: https://itch.io/docs/butler/single-files.html
- itch.io API keys: https://itch.io/user/settings/api-keys

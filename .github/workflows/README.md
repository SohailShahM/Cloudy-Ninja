# Workflows — disabled 2026-05-14

The four workflows in this directory are intentionally renamed to
`.yml.disabled` so GitHub Actions does not auto-trigger them. The
project entered a wind-down state on 2026-05-14.

## To re-enable

Rename the files back:

```bash
cd .github/workflows
git mv ai-smoke.yml.disabled ai-smoke.yml
git mv ci.yml.disabled ci.yml
git mv itch-deploy.yml.disabled itch-deploy.yml
git mv visual-regression.yml.disabled visual-regression.yml
git commit -m "re-enable CI workflows"
git push origin main
```

Branch protection on `main` requires 9 status checks; with workflows
disabled those checks will never report, so PRs cannot be merged via
the normal flow. Admin-merge (`gh pr merge --admin`) still works.

## What each workflow does

| File | Trigger | Purpose |
|---|---|---|
| `ai-smoke.yml` | every PR + push to main | autopilot run across 8 levels, asserts no crashes + 60fps |
| `ci.yml` | every PR + push to main | `Compile, Test & Lint` + the `gate` job that gathers required-check status |
| `itch-deploy.yml` | push to release tag | builds + uploads the desktop JAR to itch.io |
| `visual-regression.yml` | every PR | captures checkpoint PNGs on PR head + main baseline, pixel-diffs, comments diff report |

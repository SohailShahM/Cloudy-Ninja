# Troubleshooting

Common dev-setup issues and fixes when working on Cloudy-Ninja locally or in CI. Sections are intentionally terse — follow the links for deeper context.

## 1. JDK setup

Local builds need the Android Studio bundled JBR (Java 17). Point `JAVA_HOME` at it before invoking Gradle.

- **Bash / Git Bash:** `export JAVA_HOME='/c/Program Files/Android/Android Studio/jbr' && export PATH="$JAVA_HOME/bin:$PATH"`
- **PowerShell:** `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"`

Verify with `java -version` — should report a 17.x build. See [`HANDOFF.md`](../HANDOFF.md) §"JDK for local builds".

## 2. Gradle errors

If you hit `Could not resolve…`, `Daemon disappeared`, or stale-config weirdness:

1. Stop daemons: `./gradlew --stop`
2. Clear the cache: delete `%USERPROFILE%\.gradle\caches` (Windows) or `~/.gradle/caches` (Bash)
3. Re-run with `./gradlew clean build --no-daemon` to bypass a corrupt daemon
4. If a build script edit isn't being picked up, also delete `.gradle/` in the repo root

## 3. Box2D native missing in tests

Entities like Storm Sentinel, Drift Husk, and Projectile call into Box2D in their constructors, which requires the native lib at test time. Tests bypass this by allocating instances without invoking the constructor.

- Use `ObjenesisStd` (or `sun.misc.Unsafe.allocateInstance` for screens) and reflectively set private fields.
- Reference implementation: [`core/src/test/kotlin/com/sohai/platformer/entities/StormSentinelTest.kt`](../core/src/test/kotlin/com/sohai/platformer/entities/StormSentinelTest.kt).
- Same pattern across `SmogSpriteTest`, `ProjectileTest`, `DriftHuskTest`, `ParallaxBackgroundTest`, `ScreenFadeTest`.

## 4. MockK for libGDX statics

`Gdx.app`, `Gdx.audio`, `Gdx.files` are null in plain JUnit/Kotest runs — any code that touches them NPEs. Mock them in `beforeSpec`, restore in `afterSpec`.

- Save the previous reference, install a `mockk(relaxed = true)`, restore on teardown.
- Reference implementation: [`core/src/test/kotlin/com/sohai/platformer/audio/MusicManagerTest.kt`](../core/src/test/kotlin/com/sohai/platformer/audio/MusicManagerTest.kt) (see lines 51–53 + 139–150).
- Same pattern across `SaveManagerTest`, `SoundManagerTest`, `FontManagerTest`.

## 5. Smoke CI: runner stalls and xvfb apt-get flake

The Linux smoke runner occasionally stalls during the `apt-get install xvfb` step or hangs partway through the headless gameplay smoke. Recurring, environment-flake — not a code bug.

- First action: re-run the failed job from the Actions UI. Most flakes clear on retry.
- If it stalls repeatedly (>2 retries) check the apt mirror status and the GitHub Actions Linux runner status page.
- Path filters skip smoke on doc-only PRs (see [`HANDOFF.md`](../HANDOFF.md) §"CI"); if you're touching only `docs/`, the smoke job should be skipped — confirm via the checks tab.

## 6. `gh pr merge --admin` returns no output

Admin-merge succeeds silently — no stdout, no stderr, exit 0. It looks like nothing happened.

- Verify the merge landed: `git fetch && git log -1 origin/main`
- The PR will also flip to "Merged" in the Actions UI within a few seconds.
- Documented in [`HANDOFF.md`](../HANDOFF.md) §"Tooling gotchas" #7.

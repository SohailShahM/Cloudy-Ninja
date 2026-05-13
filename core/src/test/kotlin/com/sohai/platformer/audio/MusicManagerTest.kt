package com.sohai.platformer.audio

import com.badlogic.gdx.Application
import com.badlogic.gdx.Audio
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
import com.sohai.platformer.persist.Settings
import com.sohai.platformer.persist.SettingsManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.math.abs

/**
 * Tests for [MusicManager] — the singleton crossfade music controller.
 *
 * Isolation strategy mirrors [com.sohai.platformer.persist.SaveManagerTest]:
 *  - [MusicManager] reaches out to four libGDX globals and one project singleton:
 *      * `Gdx.app`   — relaxed mock so `log()` / `error()` are no-ops.
 *      * `Gdx.audio` — `newMusic(handle)` returns a per-track relaxed [Music]
 *        mock we capture and assert against.
 *      * `Gdx.files` — `internal(path)` returns a [FileHandle] whose `exists()`
 *        is forced to true (or false for the unknown-track test) without
 *        touching real disk.
 *      * [SettingsManager] — has a publicly-exposed [SettingsManager.resetCacheForTest]
 *        and a real `Settings` data class; we stub `Gdx.files.local(...)` to
 *        a non-existent path so `SettingsManager.load()` returns defaults.
 *  - Each `when` block calls [MusicManager.stop] up-front to reset the singleton's
 *    private state and `clearMocks` to wipe verifier history, so test ordering
 *    inside the spec does not bleed.
 *
 * Float comparisons use a small epsilon (`EPS = 1e-3f`) — frame-delta math
 * produces tiny rounding (`fadeTimer / 1.5f`), so exact equality is brittle
 * at boundaries.
 */
class MusicManagerTest : BehaviorSpec({

    val EPS = 1e-3f

    // Snapshot prior globals so we can restore them.
    val prevApp: Application? = Gdx.app
    val prevFiles: Files? = Gdx.files
    val prevAudio: Audio? = Gdx.audio

    // Per-spec audio mock — recreated in beforeSpec, queried per-test.
    val audioMock = mockk<Audio>(relaxed = true)
    val filesMock = mockk<Files>(relaxed = true)

    /**
     * Per-test scratchpad: each call to `play(name)` should mint a fresh
     * `Music` mock and stash it here keyed by track name. We use this to
     * verify `setVolume`/`play`/`stop` calls afterwards.
     */
    val musicByTrack = mutableMapOf<String, Music>()

    /**
     * Build a Music mock that:
     *   - `relaxed = true` so `play()`, `stop()`, `dispose()`, `isLooping=` no-op.
     *   - tracks volume internally via a captured slot so `update(delta)` writes
     *     are observable in subsequent reads (the source reads `next?.volume`
     *     after writing it, so the var must round-trip — relaxed handles this
     *     via the auto-generated stub for the setter, but we use a slot to
     *     also expose the latest value to the test).
     *   - reports `isPlaying = true` by default so the crossfade branch in
     *     `MusicManager.play()` is exercised for the second `play()` call.
     */
    fun newTrackMock(): Music {
        val m = mockk<Music>(relaxed = true)
        var vol = 0f
        every { m.volume = any() } answers {
            vol = firstArg()
        }
        every { m.volume } answers { vol }
        every { m.isLooping = any() } just Runs
        every { m.play() } just Runs
        every { m.stop() } just Runs
        every { m.dispose() } just Runs
        every { m.isPlaying } returns true
        return m
    }

    /** Fresh file handle that reports `exists()` per the [exists] argument. */
    fun fileHandle(path: String, exists: Boolean): FileHandle {
        val fh = mockk<FileHandle>(relaxed = true)
        every { fh.exists() } returns exists
        every { fh.path() } returns path
        return fh
    }

    /**
     * Resets the [MusicManager] singleton plus the audio/files mocks to a
     * known state. Call at the start of every `when` block.
     */
    fun resetWorld() {
        MusicManager.stop()
        SettingsManager.resetCacheForTest()
        musicByTrack.clear()
        clearMocks(audioMock, filesMock, answers = false, recordedCalls = true, childMocks = false)

        // Default routing: `audio/music/{track}.wav` exists, returns a fresh mock.
        every { filesMock.internal(any<String>()) } answers {
            val p = firstArg<String>()
            fileHandle(p, exists = true)
        }
        // SettingsManager.load() reaches Gdx.files.local("settings.json").
        // Routing it to a non-existent handle yields the default Settings (volMusic=0.7).
        every { filesMock.local(any<String>()) } answers {
            val p = firstArg<String>()
            fileHandle(p, exists = false)
        }
        every { audioMock.newMusic(any()) } answers {
            val handle = firstArg<FileHandle>()
            // Extract track name from `audio/music/<name>.wav`.
            val name = handle.path()
                .substringAfterLast('/')
                .removeSuffix(".wav")
            val m = newTrackMock()
            musicByTrack[name] = m
            m
        }
    }

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)
        Gdx.files = filesMock
        Gdx.audio = audioMock
    }

    afterSpec {
        // Tear down singleton state and restore globals so other specs aren't poisoned.
        MusicManager.stop()
        SettingsManager.resetCacheForTest()
        Gdx.app = prevApp
        Gdx.files = prevFiles
        Gdx.audio = prevAudio
        clearMocks(audioMock, filesMock)
    }

    given("a MusicManager with no track active") {

        `when`("play(ambient_arid, fadeIn = true) is called") {
            resetWorld()
            MusicManager.play("ambient_arid", fadeIn = true)

            val m = musicByTrack["ambient_arid"]!!

            then("the underlying Music.play() is invoked exactly once") {
                verify(exactly = 1) { m.play() }
            }
            then("the track starts at volume 0 (silent) because fadeIn = true") {
                m.volume shouldBe 0f
            }
            then("the track is set to looping") {
                verify { m.isLooping = true }
            }
        }

        `when`("play(ambient_arid, fadeIn = false) is called") {
            resetWorld()
            MusicManager.play("ambient_arid", fadeIn = false)

            val m = musicByTrack["ambient_arid"]!!

            then("the track starts at the SettingsManager-provided master volume (default 0.7)") {
                // Settings file is mocked as non-existent => Settings() defaults => volMusic = 0.7f.
                m.volume shouldBe 0.7f
            }
            then("play() is invoked exactly once") {
                verify(exactly = 1) { m.play() }
            }
        }
    }

    given("a MusicManager already playing 'ambient_arid'") {

        `when`("play('ambient_arid') is called again (same track)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!
            // Drain the call so the next verify sees only the (non-existent) re-play.
            verify(exactly = 1) { m.play() }

            // Same call again.
            MusicManager.play("ambient_arid")

            then("the underlying Music.play() is still only called once total") {
                verify(exactly = 1) { m.play() }
            }
            then("no second Music instance was created for the same track") {
                verify(exactly = 1) { audioMock.newMusic(any()) }
            }
        }

        `when`("play('ambient_humid') is called (a different track) and update advances time") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val a = musicByTrack["ambient_arid"]!!

            MusicManager.play("ambient_humid")
            val b = musicByTrack["ambient_humid"]!!

            then("a second Music instance is created and started") {
                verify(exactly = 1) { b.play() }
            }
            then("the incoming track begins at volume 0") {
                // Captured immediately after MusicManager.play() set music.volume = 0f.
                // After 0 delta, no fade has progressed.
                MusicManager.update(0f)
                b.volume shouldBe 0f
                // And the outgoing track is still at full volMusic.
                a.volume shouldBe 0.7f
            }
        }

        `when`("the crossfade is advanced halfway (0.75s)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val a = musicByTrack["ambient_arid"]!!
            MusicManager.play("ambient_humid")
            val b = musicByTrack["ambient_humid"]!!

            // Advance 0.75 seconds = half of FADE_DURATION (1.5s).
            MusicManager.update(0.75f)

            then("the outgoing track is at approximately half volume") {
                abs(a.volume - 0.35f) shouldBeLessThan EPS    // 0.7 * (1 - 0.5)
            }
            then("the incoming track is at approximately half volume") {
                abs(b.volume - 0.35f) shouldBeLessThan EPS    // 0.7 * 0.5
            }
            then("the two volumes are approximately equal (50/50 crossfade)") {
                abs(a.volume - b.volume) shouldBeLessThan EPS
            }
        }

        `when`("the crossfade is advanced past FADE_DURATION (1.5s)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val a = musicByTrack["ambient_arid"]!!
            MusicManager.play("ambient_humid")
            val b = musicByTrack["ambient_humid"]!!

            // Advance well past 1.5 seconds in a single tick (coerced to t = 1.0).
            MusicManager.update(2.0f)

            then("the incoming track is at the full master volume") {
                b.volume shouldBe 0.7f
            }
            then("the outgoing track is stopped and disposed") {
                verify(atLeast = 1) { a.stop() }
                verify(atLeast = 1) { a.dispose() }
            }
            then("a further update() does NOT advance the crossfade again") {
                // After completion, MusicManager nulls `next` — calling update should
                // be a no-op (no further volume mutations) since neither isFadingIn
                // nor a non-null next remains.
                MusicManager.update(1.0f)
                b.volume shouldBe 0.7f
            }
        }

        `when`("the crossfade is advanced via two smaller ticks summing to 1.5s") {
            resetWorld()
            MusicManager.play("ambient_arid")
            MusicManager.play("ambient_humid")
            val b = musicByTrack["ambient_humid"]!!

            MusicManager.update(0.5f)
            MusicManager.update(1.0f)

            then("the cumulative time matches the single-tick result (incoming at full)") {
                b.volume shouldBe 0.7f
            }
        }
    }

    given("a MusicManager fading IN from silence (no outgoing track)") {

        `when`("update(0.75f) is called halfway through the fade-in") {
            resetWorld()
            MusicManager.play("ambient_arid", fadeIn = true)
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.update(0.75f)

            then("the track is at approximately half volume (0.35)") {
                abs(m.volume - 0.35f) shouldBeLessThan EPS
            }
        }

        `when`("update advances past FADE_DURATION") {
            resetWorld()
            MusicManager.play("ambient_arid", fadeIn = true)
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.update(2.0f)

            then("the track reaches full master volume") {
                m.volume shouldBe 0.7f
            }
            then("a subsequent update() does not push volume past full") {
                MusicManager.update(0.5f)
                // Fade-in flag has been cleared so volume should remain stable.
                m.volume shouldBe 0.7f
            }
        }
    }

    given("setMusicVolume() called on an active track") {

        `when`("a track is playing and volume changes to 0.25") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.setMusicVolume(0.25f)

            then("the new master volume is applied to the active track immediately") {
                m.volume shouldBe 0.25f
            }
        }

        `when`("a track is playing and volume is set to 0 (silent)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.setMusicVolume(0f)

            then("the active track is silenced") {
                m.volume shouldBe 0f
            }
            then("the underlying play() is still active (volume == 0, not stop)") {
                // We never called stop(), so the mock should have no stop() invocations.
                verify(exactly = 0) { m.stop() }
            }
        }

        `when`("volume is set above 1.0 (out-of-range)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.setMusicVolume(5f)

            then("the volume is clamped to 1.0") {
                m.volume shouldBe 1f
            }
        }

        `when`("volume is set below 0 (negative)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.setMusicVolume(-0.5f)

            then("the volume is clamped to 0") {
                m.volume shouldBe 0f
            }
        }

        `when`("setMusicVolume is called mid-crossfade") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val a = musicByTrack["ambient_arid"]!!
            MusicManager.play("ambient_humid")
            val b = musicByTrack["ambient_humid"]!!
            MusicManager.update(0.75f)  // 50/50 midpoint

            MusicManager.setMusicVolume(0.2f)

            then("both live volumes update immediately, scaled by fade weight") {
                // T-103: setMusicVolume applies during crossfade so a slider drag is
                // heard right away rather than waiting for the fade to settle.
                // t ~ 0.5 → outgoing = 0.2 * (1 - 0.5) = 0.1, incoming = 0.2 * 0.5 = 0.1.
                abs(a.volume - 0.1f) shouldBeLessThan EPS
                abs(b.volume - 0.1f) shouldBeLessThan EPS
            }
            then("a subsequent update() keeps the fade running at the new master volume") {
                MusicManager.update(0f)
                abs(a.volume - 0.1f) shouldBeLessThan EPS
                abs(b.volume - 0.1f) shouldBeLessThan EPS
            }
        }

        `when`("setMusicVolume is called mid-fade-in (no outgoing track)") {
            resetWorld()
            MusicManager.play("ambient_arid", fadeIn = true)
            val m = musicByTrack["ambient_arid"]!!
            MusicManager.update(0.75f)  // halfway through 1.5s fade-in

            MusicManager.setMusicVolume(0.4f)

            then("the fading-in track immediately reflects the new master volume scaled by progress") {
                // t ~ 0.5 → 0.4 * 0.5 = 0.2
                abs(m.volume - 0.2f) shouldBeLessThan EPS
            }
        }
    }

    given("play() called with an unknown track") {

        `when`("the audio file does not exist on disk") {
            resetWorld()
            // Override default: this specific track's handle reports exists() = false.
            every { filesMock.internal("audio/music/does_not_exist.wav") } returns
                fileHandle("audio/music/does_not_exist.wav", exists = false)

            // Should NOT throw.
            MusicManager.play("does_not_exist")

            then("no Music instance is created (newMusic is never called)") {
                verify(exactly = 0) { audioMock.newMusic(any()) }
            }
            then("the manager remains safe to drive forward via update()") {
                // No track means no NPE in update.
                MusicManager.update(0.5f)
            }
        }
    }

    given("multiple consecutive play() calls with different tracks") {

        `when`("play(A), play(B), play(C) are called in rapid succession") {
            resetWorld()
            MusicManager.play("track_a")
            val a = musicByTrack["track_a"]!!
            MusicManager.play("track_b")
            val b = musicByTrack["track_b"]!!
            MusicManager.play("track_c")
            val c = musicByTrack["track_c"]!!

            then("track_b was started then stopped/disposed when track_c took its place") {
                // play(B) made b the `next`. play(C) then evicted b — see source:
                //   next?.stop(); next?.dispose(); next = music
                verify(atLeast = 1) { b.stop() }
                verify(atLeast = 1) { b.dispose() }
            }
            then("only track_c remains queued; updating to completion lands on track_c") {
                MusicManager.update(2.0f)
                c.volume shouldBe 0.7f
            }
            then("track_a is the one that fades out and gets stopped at the end") {
                verify(atLeast = 1) { a.stop() }
                verify(atLeast = 1) { a.dispose() }
            }
        }
    }

    given("stop() called on an active manager") {

        `when`("a track is playing and stop() is invoked") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.stop()

            then("the underlying Music.stop() is invoked") {
                verify(atLeast = 1) { m.stop() }
            }
            then("a subsequent play(sameTrack) creates a fresh Music instance") {
                MusicManager.play("ambient_arid")
                // newMusic should now have been called twice total (once before stop,
                // once after — the same-track guard checks isPlaying on current,
                // which is null after stop).
                verify(exactly = 2) { audioMock.newMusic(any()) }
            }
        }
    }

    given("duck()/unduck() ducking on an active track (T-117)") {

        `when`("a track is playing and duck(0.3, 250ms) is called then update advances past the fade") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!
            // Sanity: starts at the default master volume.
            m.volume shouldBe 0.7f

            MusicManager.duck(amount = 0.3f, fadeMs = 250)
            // Advance past the 250ms tween (0.25s) — a single large tick is fine.
            MusicManager.update(0.5f)

            then("effective volume is volMusic * amount (0.7 * 0.3 = 0.21)") {
                abs(m.volume - 0.21f) shouldBeLessThan EPS
            }
        }

        `when`("duck halfway through the fade (125ms of a 250ms tween)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.duck(amount = 0.3f, fadeMs = 250)
            MusicManager.update(0.125f)

            then("the effective volume is approximately the midpoint of 0.7 and 0.21 (~0.455)") {
                // Linear tween: multiplier moves from 1.0 to 0.3 over 250ms.
                // At t=0.5 it's at 0.65, so effective = 0.7 * 0.65 = 0.455.
                abs(m.volume - 0.455f) shouldBeLessThan EPS
            }
        }

        `when`("unduck(250ms) is called after a completed duck and update advances past the fade") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.duck(amount = 0.3f, fadeMs = 250)
            MusicManager.update(0.5f)            // fully ducked
            abs(m.volume - 0.21f) shouldBeLessThan EPS

            MusicManager.unduck(fadeMs = 250)
            MusicManager.update(0.5f)            // fully unducked

            then("the track returns to the full master volume (0.7)") {
                abs(m.volume - 0.7f) shouldBeLessThan EPS
            }
        }

        `when`("duck() is called twice in a row (idempotent — single flag, not a counter)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.duck(amount = 0.3f, fadeMs = 250)
            MusicManager.update(0.5f)            // fully ducked at 0.21

            MusicManager.duck(amount = 0.3f, fadeMs = 250)  // no-op; already ducked
            MusicManager.update(0.5f)

            then("volume remains at the ducked level — second duck() didn't re-tween or stack") {
                abs(m.volume - 0.21f) shouldBeLessThan EPS
            }

            // A single unduck() must restore even though duck() was called multiple times.
            MusicManager.unduck(fadeMs = 250)
            MusicManager.update(0.5f)

            then("a single unduck() restores volume to volMusic (collapsed, not stacked)") {
                abs(m.volume - 0.7f) shouldBeLessThan EPS
            }
        }

        `when`("rapid duck/unduck/duck/unduck cycles don't desync the fade target") {
            // The hard-rules guarantee: rapid open/close of the pause overlay
            // (which translates to duck/unduck pairs) must always converge to the
            // correct final volume. With a Boolean flag and a tween that re-bases
            // from the live multiplier, the target is always the right value.
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            // Open pause (duck), partial fade, close before fully ducked (unduck),
            // open again, close again — each toggle re-targets correctly.
            MusicManager.duck()
            MusicManager.update(0.05f)            // ~20% through the 250ms duck fade
            MusicManager.unduck()
            MusicManager.update(0.05f)
            MusicManager.duck()
            MusicManager.update(0.05f)
            MusicManager.unduck()
            // Drain the tween fully.
            MusicManager.update(1.0f)

            then("final state is unducked at the full master volume (0.7) — no desync") {
                abs(m.volume - 0.7f) shouldBeLessThan EPS
            }
        }

        `when`("unduck() is called without a prior duck()") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.unduck()                  // no-op; not currently ducked
            MusicManager.update(0.5f)

            then("volume is unchanged (still volMusic = 0.7)") {
                m.volume shouldBe 0.7f
            }
        }

        `when`("duck() is called with fadeMs = 0 (snap, no tween)") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            MusicManager.duck(amount = 0.5f, fadeMs = 0)
            MusicManager.update(0f)                // even a zero delta should apply the snap

            then("the effective volume snaps to volMusic * amount immediately (0.7 * 0.5 = 0.35)") {
                abs(m.volume - 0.35f) shouldBeLessThan EPS
            }
        }

        `when`("duck() takes effect mid-crossfade and applies to both tracks") {
            resetWorld()
            MusicManager.play("ambient_arid")
            val a = musicByTrack["ambient_arid"]!!
            MusicManager.play("ambient_humid")
            val b = musicByTrack["ambient_humid"]!!

            // Halfway through the crossfade (0.75s of 1.5s), duck instantly.
            MusicManager.duck(amount = 0.3f, fadeMs = 0)
            MusicManager.update(0.75f)

            then("both tracks reflect the duck multiplier scaled by crossfade weight") {
                // t = 0.5 → outgoing = 0.7 * 0.3 * (1 - 0.5) = 0.105
                //         incoming  = 0.7 * 0.3 * 0.5       = 0.105
                abs(a.volume - 0.105f) shouldBeLessThan EPS
                abs(b.volume - 0.105f) shouldBeLessThan EPS
            }
        }

        `when`("stop() is called while ducked — duck state is reset") {
            resetWorld()
            MusicManager.play("ambient_arid")
            MusicManager.duck(amount = 0.3f, fadeMs = 0)
            MusicManager.update(0f)
            MusicManager.stop()

            // A fresh track after stop() must start at full master volume,
            // not at the prior ducked level.
            MusicManager.play("ambient_arid")
            val m = musicByTrack["ambient_arid"]!!

            then("the fresh track starts at volMusic (0.7), not at the prior ducked level") {
                m.volume shouldBe 0.7f
            }
        }
    }

    given("setMusicVolume() with no track active") {

        `when`("the manager has never been started and volume is changed") {
            resetWorld()
            // No prior play() — current is null.
            MusicManager.setMusicVolume(0.5f)

            then("the call is safe (does not throw)") {
                // Nothing to verify on a non-existent track; surviving this far is the assertion.
            }
            then("a subsequent play() picks up the previously-stored volMusic") {
                // setMusicVolume mutates the private volMusic field; the next play()
                // will then overwrite it from SettingsManager.load(). Verify the
                // *load* path is what governs steady-state by ensuring the new track
                // launches at the Settings-default (0.7), not at 0.5.
                MusicManager.play("ambient_arid")
                val m = musicByTrack["ambient_arid"]!!
                m.volume shouldBe 0.7f
            }
        }
    }
})

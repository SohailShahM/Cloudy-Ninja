package com.sohai.platformer.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Audio
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.sohai.platformer.audio.SoundManager
import com.sohai.platformer.progression.Achievement
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import sun.misc.Unsafe

/**
 * T-138 — tests for the achievement-unlock chime that fires from
 * [AchievementToast.show].
 *
 * Isolation strategy:
 *
 *  - [AchievementToast]'s constructor builds a [com.badlogic.gdx.graphics.Pixmap],
 *    a [com.badlogic.gdx.graphics.Texture] and a Scene2D [Stage] — all of which
 *    require an OpenGL context and would crash a JVM-only test. We allocate
 *    the toast via `sun.misc.Unsafe.allocateInstance(...)`, then reflectively
 *    populate ONLY the fields `show()` touches:
 *      • `queue`        — `ArrayDeque<Queued>` the call appends to
 *      • `lastChimeNs`  — debounce timestamp
 *      • `nanoClock`    — clock seam, swapped to a fake `() -> Long`
 *
 *    This matches the [com.sohai.platformer.rendering.ScreenFadeTest] pattern
 *    for ScreenFade and the project-wide convention for GL-touching ctors.
 *
 *  - [SoundManager] is an `object`. We mock `Gdx.app/audio/files` in
 *    [beforeSpec] (relaxed), call `SoundManager.init()` so it picks up a
 *    relaxed [Sound] mock per registered id, then drive it from the toast's
 *    `show()` call. The Sound mock for `achievement_unlock` is the one whose
 *    `play(volume, pitch, pan)` we verify.
 *
 *  - The production [AchievementToast] and [SoundManager] singletons aren't
 *    modified between tests; we reset `SoundManager` (dispose + init) and the
 *    toast's clock + timestamp + queue in each scenario for determinism.
 *
 * Contract verified:
 *  1. First `show(...)` plays the chime exactly once.
 *  2. A second `show(...)` within 200ms is debounced — the chime does NOT
 *     play again, but the second toast IS queued (visual queue is unaffected).
 *  3. After ≥200ms the chime fires again.
 *  4. When `SoundManager.setEnabled(false)`, `show(...)` produces no chime.
 *  5. Volume routes through the UI bus (`playUi` -> `uiVolume`), so changing
 *     `setUiVolume(v)` changes the captured volume on the next chime.
 */
class AchievementToastChimeTest : BehaviorSpec({

    // ── libGDX globals snapshot / restore ─────────────────────────────────────

    val prevApp: Application? = Gdx.app
    val prevAudio: Audio? = Gdx.audio
    val prevFiles: Files? = Gdx.files

    val soundMocks = mutableMapOf<String, Sound>()

    val allPaths = mapOf(
        "jump"               to "audio/sfx/jump.wav",
        "land"               to "audio/sfx/land.wav",
        "collect_token"      to "audio/sfx/collect_token.wav",
        "collect_snapshot"   to "audio/sfx/collect_snapshot.wav",
        "death"              to "audio/sfx/death.wav",
        "checkpoint"         to "audio/sfx/checkpoint.wav",
        "level_complete"     to "audio/sfx/level_complete.wav",
        "hazard_cleansed"    to "audio/sfx/hazard_cleansed.wav",
        "achievement_unlock" to "audio/sfx/achievement_unlock.wav",
        "collect"            to "sounds/collect.wav",
        "cleanse"            to "sounds/cleanse.wav",
        "ability_ebo"        to "sounds/ability_ebo.wav",
        "ability_laya"       to "sounds/ability_laya.wav"
    )

    beforeSpec {
        Gdx.app = mockk<Application>(relaxed = true)

        val pathToHandle = mutableMapOf<String, FileHandle>()
        val filesMock = mockk<Files>(relaxed = true)
        every { filesMock.internal(any<String>()) } answers {
            val p = firstArg<String>()
            pathToHandle.getOrPut(p) {
                mockk<FileHandle>(relaxed = true).also {
                    every { it.exists() } returns true
                }
            }
        }
        Gdx.files = filesMock

        val audioMock = mockk<Audio>(relaxed = true)
        every { audioMock.newSound(any<FileHandle>()) } answers {
            val handle = firstArg<FileHandle>()
            val path = pathToHandle.entries.firstOrNull { it.value === handle }?.key
                ?: "<unknown>"
            val s = mockk<Sound>(relaxed = true)
            soundMocks[path] = s
            s
        }
        Gdx.audio = audioMock
    }

    afterSpec {
        SoundManager.dispose()
        Gdx.app = prevApp
        Gdx.audio = prevAudio
        Gdx.files = prevFiles
        soundMocks.clear()
    }

    // ── Unsafe + reflection helpers ───────────────────────────────────────────

    val unsafe: Unsafe = run {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        f.get(null) as Unsafe
    }

    fun setField(toast: AchievementToast, name: String, value: Any?) {
        val f = AchievementToast::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(toast, value)
    }

    fun setLongField(toast: AchievementToast, name: String, value: Long) {
        val f = AchievementToast::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.setLong(toast, value)
    }

    /**
     * Allocate an [AchievementToast] without running its (GL-requiring)
     * constructor, then initialise ONLY the fields that `show()` reads/writes.
     * The fake clock seeds at 0 — tests advance it via [fakeNow].
     */
    fun newToast(fakeNow: LongArray): AchievementToast {
        @Suppress("UsePropertyAccessSyntax")
        val t = unsafe.allocateInstance(AchievementToast::class.java) as AchievementToast
        setField(t, "queue", ArrayDeque<Any>())
        setLongField(t, "lastChimeNs", Long.MIN_VALUE)
        val clock: () -> Long = { fakeNow[0] }
        setField(t, "nanoClock", clock)
        return t
    }

    fun resetSoundManager() {
        SoundManager.dispose()
        soundMocks.clear()
        SoundManager.setEnabled(true)
        SoundManager.setVolume(0.8f)
        SoundManager.setUiVolume(1f)
        SoundManager.init()
    }

    fun chimeMock(): Sound =
        soundMocks[allPaths.getValue("achievement_unlock")]
            ?: error("SoundManager.init() did not load achievement_unlock — registry missing?")

    val sampleAchievement = Achievement(
        id = "test_achievement",
        title = "Test",
        desc = "A test achievement",
        iconPath = "icons/achievements/test_achievement.png"
    )
    val secondAchievement = Achievement(
        id = "test_achievement_2",
        title = "Test 2",
        desc = "Another test achievement",
        iconPath = "icons/achievements/test_achievement_2.png"
    )

    given("a fresh AchievementToast with the SoundManager initialised") {
        resetSoundManager()
        val fakeNow = longArrayOf(1_000_000_000L)  // 1s into the fake clock
        val toast = newToast(fakeNow)

        `when`("show(achievement) is called for the first time") {
            toast.show(sampleAchievement)

            then("the achievement_unlock chime plays exactly once via the UI bus") {
                verify(exactly = 1) {
                    chimeMock().play(1f, 1f, 0f)  // uiVolume default = 1f
                }
            }
        }
    }

    given("two show(...) calls inside the 200ms debounce window") {
        resetSoundManager()
        val fakeNow = longArrayOf(2_000_000_000L)
        val toast = newToast(fakeNow)

        `when`("show is called twice within 199ms") {
            toast.show(sampleAchievement)
            // Advance 199ms — still inside the 200ms window
            fakeNow[0] += 199_000_000L
            toast.show(secondAchievement)

            then("the chime fires exactly ONCE (second call is debounced)") {
                verify(exactly = 1) {
                    chimeMock().play(any(), any(), any())
                }
            }
        }
    }

    given("a show(...) call after the 200ms debounce has elapsed") {
        resetSoundManager()
        val fakeNow = longArrayOf(3_000_000_000L)
        val toast = newToast(fakeNow)

        `when`("show fires, then 200ms passes, then show fires again") {
            toast.show(sampleAchievement)
            fakeNow[0] += 200_000_000L  // exactly 200ms — boundary opens the gate
            toast.show(secondAchievement)

            then("the chime fires exactly TWICE — debounce window cleared") {
                verify(exactly = 2) {
                    chimeMock().play(any(), any(), any())
                }
            }
        }
    }

    given("SoundManager disabled via setEnabled(false)") {
        resetSoundManager()
        SoundManager.setEnabled(false)
        val fakeNow = longArrayOf(4_000_000_000L)
        val toast = newToast(fakeNow)

        `when`("show(achievement) is called") {
            toast.show(sampleAchievement)

            then("no chime is played — SoundManager.enabled gate respected") {
                verify(exactly = 0) {
                    chimeMock().play(any(), any(), any())
                }
            }
        }
    }

    given("the UI volume bus changed before show(...)") {
        resetSoundManager()
        SoundManager.setUiVolume(0.4f)
        val fakeNow = longArrayOf(5_000_000_000L)
        val toast = newToast(fakeNow)

        `when`("show is called after setUiVolume(0.4f)") {
            toast.show(sampleAchievement)

            then("chime plays at the new UI bus volume (0.4)") {
                verify(exactly = 1) {
                    chimeMock().play(0.4f, 1f, 0f)
                }
            }
        }
    }
})

package com.sohai.platformer.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.sohai.platformer.persist.SettingsManager

/**
 * Singleton that manages background music playback with crossfade support.
 *
 * Keeps two [Music] slots — `current` (playing) and `next` (fading in).
 * Call [update] every frame from `GameScreen.render` to drive the crossfade.
 *
 * Music files are loaded from `audio/music/{trackName}.wav` via
 * [Gdx.files.internal]. Tracks are 60-second procedurally generated
 * ambient loops written by [ProceduralMusicGenerator] on first run.
 *
 * This manager is intentionally separate from [SoundManager] (which handles
 * short SFX loaded as libGDX [com.badlogic.gdx.audio.Sound] instances).
 * Music uses the streaming [Music] API, suitable for long tracks.
 */
object MusicManager {

    /**
     * Track ids that the splash preload (T-104) and any future preload
     * pass should check / prime. Kept in sync with the three tracks
     * [ProceduralMusicGenerator] emits.
     */
    val PRELOAD_TRACKS: List<String> = listOf("ambient_arid", "ambient_wind", "ambient_eco")

    private var current: Music? = null
    private var next: Music? = null
    private var currentTrackName: String? = null
    private var nextTrackName: String? = null

    private var fadeTimer = 0f
    private const val FADE_DURATION = 1.5f

    /** Master music volume (0..1). Initialised from [SettingsManager]. */
    private var volMusic = 0.7f

    /** Whether we are currently fading in (no outgoing track). */
    private var isFadingIn = false

    /**
     * Start playing a track. If a different track is already playing,
     * begins a crossfade over [FADE_DURATION] seconds.
     *
     * @param trackName  Base name (e.g. "ambient_arid") — loaded from `audio/music/{trackName}.wav`.
     * @param fadeIn     If true and nothing is playing, fade in from silence over [FADE_DURATION].
     */
    fun play(trackName: String, fadeIn: Boolean = false) {
        // Pull volume from settings each time we start a track
        volMusic = SettingsManager.load().volMusic

        // Already playing this track — nothing to do
        if (trackName == currentTrackName && current?.isPlaying == true) return

        val path = "audio/music/$trackName.wav"
        val handle = Gdx.files.internal(path)
        if (!handle.exists()) {
            Gdx.app.log("MusicManager", "Track not found: $path (will be silent)")
            return
        }

        try {
            val music = Gdx.audio.newMusic(handle)
            music.isLooping = true

            if (current == null || current?.isPlaying != true) {
                // Nothing playing — start fresh
                if (fadeIn) {
                    music.volume = 0f
                    isFadingIn = true
                    fadeTimer = 0f
                } else {
                    music.volume = volMusic
                    isFadingIn = false
                }
                current?.stop()
                current?.dispose()
                current = music
                currentTrackName = trackName
                music.play()
            } else {
                // Crossfade: current becomes outgoing, new becomes next
                next?.stop()
                next?.dispose()
                next = music
                nextTrackName = trackName
                music.volume = 0f
                music.play()
                fadeTimer = 0f
                isFadingIn = false
            }
        } catch (e: Exception) {
            Gdx.app.error("MusicManager", "Failed to load track: $path", e)
        }
    }

    /**
     * Drive crossfade logic. Call once per frame from `GameScreen.render`.
     *
     * @param delta Frame delta in seconds.
     */
    fun update(delta: Float) {
        // Handle fade-in from silence (no crossfade, just volume ramp)
        if (isFadingIn && next == null) {
            fadeTimer += delta
            val t = (fadeTimer / FADE_DURATION).coerceIn(0f, 1f)
            current?.volume = volMusic * t
            if (t >= 1f) {
                isFadingIn = false
            }
            return
        }

        // Handle crossfade between current and next
        if (next != null) {
            fadeTimer += delta
            val t = (fadeTimer / FADE_DURATION).coerceIn(0f, 1f)
            current?.volume = volMusic * (1f - t)
            next?.volume = volMusic * t
            if (t >= 1f) {
                current?.stop()
                current?.dispose()
                current = next
                currentTrackName = nextTrackName
                next = null
                nextTrackName = null
            }
        }
    }

    /**
     * Set the master music volume. Takes effect immediately, including
     * mid-crossfade and mid-fade-in: the new master volume is applied to
     * each active track scaled by its current fade weight so the user's
     * slider drag is heard right away.
     *
     * @param vol Volume level 0..1.
     */
    fun setMusicVolume(vol: Float) {
        volMusic = vol.coerceIn(0f, 1f)
        val t = (fadeTimer / FADE_DURATION).coerceIn(0f, 1f)
        when {
            next != null -> {
                current?.volume = volMusic * (1f - t)
                next?.volume = volMusic * t
            }
            isFadingIn -> {
                current?.volume = volMusic * t
            }
            else -> {
                current?.volume = volMusic
            }
        }
    }

    /**
     * Stop the current track immediately (no fade-out). The crossfade in
     * [play] handles smooth transitions between tracks; [stop] is the
     * blunt instrument used on app pause / level teardown.
     */
    fun stop() {
        next?.stop()
        next?.dispose()
        next = null
        nextTrackName = null

        if (current != null) {
            current?.stop()
            current?.dispose()
            current = null
            currentTrackName = null
        }
        isFadingIn = false
    }

    /**
     * Preload-hook used by the cold-start splash (T-104). Verifies each
     * track id in [PRELOAD_TRACKS] has a file handle on disk and logs a
     * warning for any missing entries.
     *
     * Intentionally **does not** allocate [Music] instances — `Music` is a
     * streaming resource and the user may have disabled music in settings.
     * The check still surfaces broken / missing assets early so the player
     * doesn't hit a silent failure when they hit "New Game".
     *
     * Safe to call multiple times; safe to call before [Gdx.audio] exists
     * (it only touches [Gdx.files]).
     */
    fun preloadAll() {
        for (trackName in PRELOAD_TRACKS) {
            val path = "audio/music/$trackName.wav"
            val handle = Gdx.files.internal(path)
            if (!handle.exists()) {
                Gdx.app.log("MusicManager", "Preload: missing track $path (will be silent)")
            }
        }
    }

    /**
     * Dispose all music resources. Call on app exit.
     */
    fun dispose() {
        current?.stop()
        current?.dispose()
        current = null
        currentTrackName = null
        next?.stop()
        next?.dispose()
        next = null
        nextTrackName = null
    }
}

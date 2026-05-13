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
 *
 * ### Ducking (T-117)
 *
 * [duck] and [unduck] temporarily dip the *effective* playback volume without
 * mutating the user's [volMusic] setting. Used by `GameScreen` when the pause
 * overlay opens/closes so the music dips while the player reads the menu and
 * restores when they resume. The duck state is a single boolean — repeated
 * [duck] calls collapse to one, a single [unduck] restores. See `duckMultiplier`.
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

    // ── Ducking state (T-117) ───────────────────────────────────────────────
    //
    // `duckMultiplier` is the live scalar applied on top of `volMusic` for the
    // effective playback volume. Tweens between 1.0 (not ducked) and the most
    // recent `duck(amount, …)` target over `duckTweenDuration` seconds.
    //
    // `ducked` is the single source of truth for whether the manager is in the
    // ducked state — NOT a counter. This is deliberate: multiple `duck()` calls
    // are idempotent and collapse into one; a single `unduck()` restores.
    /** True iff currently ducked (target multiplier < 1.0). Single flag, not a counter. */
    private var ducked = false
    /** Current duck scalar in flight. 1.0 = full volume; less than 1.0 = ducked. */
    private var duckMultiplier = 1f
    /** Source value of the duck tween (where the multiplier was when the tween started). */
    private var duckFrom = 1f
    /** Target value of the duck tween (where the multiplier should end up). */
    private var duckTarget = 1f
    /** Elapsed seconds of the duck tween. */
    private var duckTweenTimer = 0f
    /** Total seconds the duck tween should run. 0 means snap immediately. */
    private var duckTweenDuration = 0f
    /** True for one frame after a zero-fade duck/unduck so [update] pushes the snap to the live track. */
    private var duckPendingSnap = false

    /** Effective volume scalar applied to a track at full fade-in weight. */
    private fun effectiveVolume(): Float = volMusic * duckMultiplier

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
                    music.volume = effectiveVolume()
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
        // Advance the duck tween (independent of fade state) so the duck dip
        // happens smoothly whether or not a crossfade is in flight.
        // `duckTweening` flag: true if we wrote a new multiplier this frame and
        // therefore need to push the new effective volume through to the live
        // tracks below (relevant only for steady-state playback — the crossfade
        // and fade-in branches recompute volume from `effectiveVolume()` already).
        var duckTweening = false
        if (duckTweenTimer < duckTweenDuration) {
            duckTweening = true
            duckTweenTimer += delta
            val td = (duckTweenTimer / duckTweenDuration).coerceIn(0f, 1f)
            duckMultiplier = duckFrom + (duckTarget - duckFrom) * td
            if (td >= 1f) {
                duckMultiplier = duckTarget
                duckTweenTimer = duckTweenDuration
            }
        } else if (duckPendingSnap) {
            // Zero-fade duck/unduck snap: multiplier was set synchronously in
            // [startDuckTween]; consume the flag here so the steady-state
            // branch pushes the new effective volume to the live track once.
            duckTweening = true
            duckPendingSnap = false
        }

        // Handle fade-in from silence (no crossfade, just volume ramp)
        if (isFadingIn && next == null) {
            fadeTimer += delta
            val t = (fadeTimer / FADE_DURATION).coerceIn(0f, 1f)
            current?.volume = effectiveVolume() * t
            if (t >= 1f) {
                isFadingIn = false
            }
            return
        }

        // Handle crossfade between current and next
        if (next != null) {
            fadeTimer += delta
            val t = (fadeTimer / FADE_DURATION).coerceIn(0f, 1f)
            current?.volume = effectiveVolume() * (1f - t)
            next?.volume = effectiveVolume() * t
            if (t >= 1f) {
                current?.stop()
                current?.dispose()
                current = next
                currentTrackName = nextTrackName
                next = null
                nextTrackName = null
            }
        } else if (duckTweening && current != null) {
            // Steady-state playback with an active duck tween: push the new
            // effective volume to the live track so the dip is heard.
            current?.volume = effectiveVolume()
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
                current?.volume = effectiveVolume() * (1f - t)
                next?.volume = effectiveVolume() * t
            }
            isFadingIn -> {
                current?.volume = effectiveVolume() * t
            }
            else -> {
                current?.volume = effectiveVolume()
            }
        }
    }

    /**
     * Duck the music to a fraction of the user's master volume.
     *
     * Effective volume becomes `volMusic * amount`. The transition is a linear
     * tween over [fadeMs] milliseconds driven by [update].
     *
     * **Idempotent:** if the manager is already ducked, this call is a no-op —
     * we use a single [ducked] flag (not a counter), so a single matching
     * [unduck] always restores full volume regardless of how many [duck] calls
     * preceded it. This prevents rapid pause-toggle from desynchronizing the
     * fade target.
     *
     * The user's [volMusic] setting is **never** mutated by ducking.
     *
     * @param amount  Multiplier applied while ducked. 0..1. Default 0.3 — dip
     *                to 30% of master so the pause menu doesn't compete with
     *                the music but the loop is still audible.
     * @param fadeMs  Tween duration in milliseconds. Default 250.
     */
    fun duck(amount: Float = 0.3f, fadeMs: Int = 250) {
        if (ducked) return                       // idempotent — single flag, not a counter
        ducked = true
        startDuckTween(amount.coerceIn(0f, 1f), fadeMs)
    }

    /**
     * Restore the music to the user's master volume after a prior [duck].
     *
     * Single call restores regardless of how many [duck] calls preceded it.
     * No-op if not currently ducked.
     *
     * @param fadeMs Tween duration in milliseconds. Default 250.
     */
    fun unduck(fadeMs: Int = 250) {
        if (!ducked) return
        ducked = false
        startDuckTween(1f, fadeMs)
    }

    /** Common path for both [duck] and [unduck] — kicks off a linear tween. */
    private fun startDuckTween(target: Float, fadeMs: Int) {
        duckFrom = duckMultiplier
        duckTarget = target
        duckTweenTimer = 0f
        duckTweenDuration = (fadeMs.coerceAtLeast(0)) / 1000f
        if (duckTweenDuration <= 0f) {
            // Snap immediately for zero-fade duck. Flag the next update() to
            // push the new effective volume to the live track.
            duckMultiplier = target
            duckPendingSnap = true
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

        // Reset duck state so a fresh play() after stop() starts at full volume.
        ducked = false
        duckMultiplier = 1f
        duckFrom = 1f
        duckTarget = 1f
        duckTweenTimer = 0f
        duckTweenDuration = 0f
        duckPendingSnap = false
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

package com.sohai.platformer.persist

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Manages saving and loading game state to/from JSON files.
 * Uses kotlinx.serialization for clean serialization API.
 *
 * Caches the last-loaded state per filename so callers in the hot render path
 * (e.g. LevelRenderer portal colours) never hit the disk more than once per load.
 * The cache for a file is invalidated on every [saveGame] or [deleteSave] call.
 */
object SaveManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private const val SAVE_DIR = "saves"
    private const val DEFAULT_SAVE_FILE = "save_slot_1.json"

    /** In-memory cache: filename → last loaded GameState. */
    private val cache = mutableMapOf<String, GameState>()

    /**
     * Test-only hook: if non-null, invoked after the temp file has been written
     * and fsync'd but BEFORE the atomic rename. Throwing from this hook
     * simulates a process crash mid-save and lets tests assert that the
     * original target file is left intact.
     *
     * Production code MUST NOT set this. It is `internal` for visibility from
     * tests in the same module.
     */
    internal var crashAfterTempWriteHook: (() -> Unit)? = null

    /**
     * Test-only hook: if non-null, invoked DURING the write to the temp file
     * (after open, before fsync/close). Throwing simulates a crash mid-write
     * and lets tests assert that the original target file is untouched.
     */
    internal var crashDuringWriteHook: (() -> Unit)? = null

    /**
     * Save the current game state to a JSON file using atomic write semantics.
     *
     * Sequence (T-136):
     *  1. Serialize `state` to JSON.
     *  2. Write bytes to `<filename>.tmp` and fsync via [java.nio.channels.FileChannel.force].
     *  3. Atomically rename `<filename>.tmp` → `<filename>` using
     *     [Files.move] with [StandardCopyOption.ATOMIC_MOVE].
     *  4. Fallback to non-atomic [StandardCopyOption.REPLACE_EXISTING] move
     *     and log a warning if the filesystem rejects atomic moves
     *     (e.g. some FAT/SMB shares).
     *
     * Crash semantics:
     *  - Crash during step 2 → temp file may be partial; original target
     *    untouched and still loadable. Temp file is best-effort cleaned up.
     *  - Crash between step 2 and step 3 → original target untouched.
     *  - Crash during step 3 → either the old or the new file is at the
     *    target path; both are valid JSON, so the load path always succeeds.
     */
    fun saveGame(state: GameState, filename: String = DEFAULT_SAVE_FILE) {
        // Resolve via libGDX so the save directory lives wherever the
        // platform wants it (desktop: working dir, Android: app data, etc.),
        // but perform the actual I/O through java.nio.file for atomic move
        // semantics that FileHandle doesn't expose.
        val saveDirHandle = Gdx.files.local(SAVE_DIR)
        if (!saveDirHandle.exists()) saveDirHandle.mkdirs()

        val tmpHandle   = Gdx.files.local("$SAVE_DIR/$filename.tmp")
        val finalHandle = Gdx.files.local("$SAVE_DIR/$filename")
        val tmpPath   = tmpHandle.file().toPath()
        val finalPath = finalHandle.file().toPath()

        val jsonString = json.encodeToString(state)
        val payload = jsonString.toByteArray(Charsets.UTF_8)

        try {
            // Step 2: write + fsync. RandomAccessFile gives us a FileChannel
            // so we can force(true) (data + metadata) before close.
            RandomAccessFile(tmpPath.toFile(), "rw").use { raf ->
                raf.setLength(0L)
                raf.write(payload)
                crashDuringWriteHook?.invoke()
                raf.fd.sync()
            }

            // Test-only seam between fsync and rename.
            crashAfterTempWriteHook?.invoke()

            // Step 3: atomic rename. Fall back if the filesystem refuses.
            try {
                Files.move(
                    tmpPath,
                    finalPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: AtomicMoveNotSupportedException) {
                Gdx.app.log(
                    "SaveManager",
                    "Atomic move not supported on this filesystem; falling back to REPLACE_EXISTING. ${e.message}"
                )
                Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING)
            }

            cache[filename] = state   // keep cache coherent after save
            Gdx.app.log("SaveManager", "Saved to $filename")
        } catch (e: Exception) {
            // Best-effort cleanup of the partial temp file. The original
            // target (if any) was never touched, so the previous save is
            // still loadable.
            try {
                Files.deleteIfExists(tmpPath)
            } catch (cleanup: IOException) {
                Gdx.app.error(
                    "SaveManager",
                    "Failed to clean up temp file after save error: ${cleanup.message}"
                )
            }
            Gdx.app.error("SaveManager", "Failed to save: ${e.message}")
        }
    }

    /**
     * Load game state from a JSON file.
     * Returns a default state if the file doesn't exist.
     */
    fun loadGame(filename: String = DEFAULT_SAVE_FILE): GameState {
        cache[filename]?.let { return it }
        return try {
            val saveFile = Gdx.files.local("$SAVE_DIR/$filename")
            if (!saveFile.exists()) {
                return GameState().also { cache[filename] = it }
            }

            val jsonString = saveFile.readString()
            // Route every load through the migration chain (T-113). Pre-T-113
            // saves omit `saveFormatVersion` and are treated as v1 — the
            // current version at scaffold introduction — so they pass through
            // the chain unchanged.
            val state = SaveMigrations.migrate(jsonString)
            Gdx.app.log("SaveManager", "Loaded from $filename")
            cache[filename] = state
            state
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to load: ${e.message}")
            GameState()
        }
    }

    /**
     * List all available save files.
     */
    fun listSaves(): List<String> {
        val saveDir = Gdx.files.local(SAVE_DIR)
        if (!saveDir.exists()) return emptyList()

        return saveDir.list()
            .filter { it.name().endsWith(".json") }
            .map { it.name() }
    }

    /**
     * Returns true if a save file with the given filename exists on disk.
     */
    fun hasSave(filename: String): Boolean {
        return Gdx.files.local("$SAVE_DIR/$filename").exists()
    }

    /**
     * Delete a specific save file.
     */
    fun deleteSave(filename: String = DEFAULT_SAVE_FILE): Boolean {
        return try {
            val saveFile = Gdx.files.local("$SAVE_DIR/$filename")
            if (saveFile.exists()) {
                saveFile.delete()
                cache.remove(filename)   // evict cache entry
                Gdx.app.log("SaveManager", "Deleted $filename")
                true
            } else {
                cache.remove(filename)
                false
            }
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to delete: ${e.message}")
            false
        }
    }
}


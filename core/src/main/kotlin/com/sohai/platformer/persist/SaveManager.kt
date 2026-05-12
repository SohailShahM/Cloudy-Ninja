package com.sohai.platformer.persist

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
     * Save the current game state to a JSON file.
     *
     * Atomic-ish: writes to `<filename>.tmp`, then copies to the final path.
     * A crash mid-write leaves the previous save intact (libGDX has no rename
     * primitive on the FileHandle API; copyTo + delete is the closest we get).
     */
    fun saveGame(state: GameState, filename: String = DEFAULT_SAVE_FILE) {
        try {
            val saveDir = Gdx.files.local(SAVE_DIR)
            if (!saveDir.exists()) saveDir.mkdirs()

            val tmp   = Gdx.files.local("$SAVE_DIR/$filename.tmp")
            val final = Gdx.files.local("$SAVE_DIR/$filename")
            val jsonString = json.encodeToString(state)
            tmp.writeString(jsonString, false)
            // Replace target atomically (within libGDX's API)
            if (final.exists()) final.delete()
            tmp.copyTo(final)
            tmp.delete()
            cache[filename] = state   // keep cache coherent after save
            Gdx.app.log("SaveManager", "Saved to $filename")
        } catch (e: Exception) {
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


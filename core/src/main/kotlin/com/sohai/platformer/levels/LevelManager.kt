package com.sohai.platformer.levels

/**
 * Manages available levels and provides registry/lookup functionality.
 *
 * The ordered level sequence is built from two sources:
 *  1. [Level0_1] — hand-built tutorial geometry, registered directly.
 *  2. [LevelRegistry.ALL] — data-driven TMX levels; adding a new level
 *     requires only a new [TmxLevelDefinition] entry there, not a new class.
 */
object LevelManager {
    /**
     * Ordered list of all levels in sequence.
     *
     * Ordering matters: [getNextLevel] uses list position, not the id string.
     * World-0 tutorial rooms (Level0_1, Level0_2) are registered here directly
     * because they hand-build their geometry procedurally and have no TMX file;
     * all campaign levels come from [LevelRegistry].
     */
    private val levels: List<Level> = buildList {
        add(Level0_1())
        add(Level0_2())
        LevelRegistry.ALL.forEach { def -> add(TmxLevel(def)) }
    }

    /** Fast id → level lookup built once at startup. */
    private val byId: Map<String, Level> = levels.associateBy { it.id }

    fun getLevel(id: String): Level? = byId[id]

    fun getAllLevels(): List<Level> = levels

    fun getNextLevel(currentId: String): Level? {
        val currentIndex = levels.indexOfFirst { it.id == currentId }
        return if (currentIndex >= 0 && currentIndex < levels.size - 1) {
            levels[currentIndex + 1]
        } else {
            null
        }
    }

    fun isLastLevel(levelId: String): Boolean = getNextLevel(levelId) == null
}

package com.sohai.platformer.levels

/**
 * Manages available levels and provides registry/lookup functionality.
 */
object LevelManager {
    private val levels: Map<String, Level> = mapOf(
        "level0_1" to Level0_1(),
        "level0_2" to Level0_2(),
        "level1" to Level1(),
        "level2" to Level2(),
        "level3" to Level3()
    )

    fun getLevel(id: String): Level? = levels[id]

    fun getAllLevels(): List<Level> = levels.values.toList()

    fun getNextLevel(currentId: String): Level? {
        val allLevels = getAllLevels()
        val currentIndex = allLevels.indexOfFirst { it.id == currentId }
        return if (currentIndex >= 0 && currentIndex < allLevels.size - 1) {
            allLevels[currentIndex + 1]
        } else {
            null
        }
    }

    fun isLastLevel(levelId: String): Boolean = getNextLevel(levelId) == null
}


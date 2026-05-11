package com.sohai.platformer.progression

object AchievementRegistry {
    val ALL = listOf(
        Achievement("first_jump",      "First Flight",    "Complete your first jump"),
        Achievement("first_cleanse",   "Seed Planter",    "Cleanse your first hazard with Seed Slam"),
        Achievement("eco_sweep",       "Eco Champion",    "Collect all eco-tokens in any one level"),
        Achievement("no_death_run",    "Ghost Walker",    "Complete a level without dying"),
        Achievement("speed_demon",     "Speed Demon",     "Complete any level under 2 minutes in Time Trial"),
        Achievement("atlas_half",      "Cloud Watcher",   "Collect 6 Cloud Atlas snapshots"),
        Achievement("atlas_full",      "Sky Scholar",     "Collect all 12 Cloud Atlas snapshots"),
        Achievement("first_enemy",     "Cleanse Warrior", "Defeat your first Smog Sprite"),
        Achievement("stomp_10",        "Stomper",         "Stomp 10 enemies"),
        Achievement("boss_defeated",   "Storm Breaker",   "Defeat the Storm Sentinel"),
        Achievement("world_1_clear",   "The First Rain",  "Complete World 1"),
        Achievement("all_clear",       "Eco Restored",    "Complete all worlds")
    )

    fun get(id: String): Achievement? = ALL.firstOrNull { it.id == id }
}

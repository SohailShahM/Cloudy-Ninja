package com.sohai.platformer.atlas

/**
 * One educational "Snapshot" card from the Cloud Atlas.
 * Cards are collected in-world and explain the real climate science
 * behind each gameplay mechanic.
 */
data class CloudAtlasEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val body: String,
    /** Which character's ability this card relates to */
    val character: String = "Ebo"
)

/**
 * The full library of Cloud Atlas entries, keyed by id.
 * Add entries here as new worlds and mechanics are built.
 */
object CloudAtlasLibrary {

    val entries: Map<String, CloudAtlasEntry> = listOf(

        CloudAtlasEntry(
            id       = "silver_iodide",
            title    = "Silver Iodide Cloud Seeding",
            subtitle = "How Ebo makes it rain",
            body     = "Silver iodide particles are fired into cold clouds, " +
                       "giving water molecules something to crystallise around. " +
                       "The result: artificial rain that cools the land and " +
                       "replenishes drought-stricken soil — exactly what " +
                       "Ebo's Seed Slam recreates at high speed.",
            character = "Ebo"
        ),

        CloudAtlasEntry(
            id       = "temperature_inversion",
            title    = "Temperature Inversions",
            subtitle = "Why smog gets trapped in valleys",
            body     = "Normally, warm air rises and carries pollutants away. " +
                       "A temperature inversion flips this: a warm layer sits " +
                       "above cooler air, acting as a lid. Smog pools beneath it, " +
                       "choking cities. Laya's Wind Dash shatters inversions by " +
                       "blasting a jet stream through the ceiling.",
            character = "Laya"
        ),

        CloudAtlasEntry(
            id       = "albedo_effect",
            title    = "The Albedo Effect",
            subtitle = "Reflection as a cooling weapon",
            body     = "Albedo is how much sunlight a surface reflects. " +
                       "Fresh snow reflects ~90 % of incoming solar energy; " +
                       "dark ocean absorbs ~94 %. As ice melts, the planet " +
                       "absorbs more heat — a runaway feedback loop. " +
                       "Restoring reflective surfaces is one of Earth's " +
                       "most powerful natural defences.",
            character = "Elara"
        ),

        CloudAtlasEntry(
            id       = "water_cycle",
            title    = "The Water Cycle",
            subtitle = "The engine behind every level",
            body     = "Evaporation, condensation, precipitation, collection — " +
                       "the water cycle redistributes heat and freshwater across " +
                       "the entire planet. When the Great Haze disrupts it, " +
                       "droughts, floods, and toxic storms follow. " +
                       "Every level you cleanse is one more link in the chain " +
                       "restored.",
            character = "Ebo"
        ),

        CloudAtlasEntry(
            id       = "solar_radiation_management",
            title    = "Solar Radiation Management",
            subtitle = "The geoengineering gamble",
            body     = "SRM proposals suggest placing reflective aerosols in " +
                       "the stratosphere — or deploying orbital mirrors — to " +
                       "bounce sunlight before it reaches Earth. It could buy " +
                       "time, but terminating it abruptly would cause rapid " +
                       "rebound warming. The hard-light arrays in World 4 are " +
                       "a fictional, weaponised version of this real concept.",
            character = "Silas"
        ),

        CloudAtlasEntry(
            id       = "storm_system",
            title    = "Storm Systems",
            subtitle = "The sentinel's power source",
            body     = "Large-scale storm systems form when warm, moist air rises " +
                       "rapidly, creating low-pressure centres that draw in " +
                       "surrounding air. As the inflow spirals upward it releases " +
                       "enormous latent energy as precipitation. The Storm Sentinel " +
                       "corrupts this natural process, converting the Great Haze into " +
                       "concentrated lightning — until Ebo's cleansing rain breaks " +
                       "the feedback loop.",
            character = "Ebo"
        )

    ).associateBy { it.id }

    fun get(id: String): CloudAtlasEntry? = entries[id]
}

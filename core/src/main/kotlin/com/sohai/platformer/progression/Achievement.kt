package com.sohai.platformer.progression

import kotlinx.serialization.Serializable

@Serializable
data class Achievement(val id: String, val title: String, val desc: String)

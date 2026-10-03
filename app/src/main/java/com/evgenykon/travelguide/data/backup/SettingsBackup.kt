package com.evgenykon.travelguide.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class SettingsBackupFile(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val promptTemplate: String,
    val model: String,
    val voice: String,
    val speed: Float,
    val autoPlay: Boolean,
    val radiusMeters: Float,
    val styleUrl: String
)

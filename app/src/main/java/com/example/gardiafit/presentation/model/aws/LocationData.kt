package com.example.gardiafit.model.aws

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)
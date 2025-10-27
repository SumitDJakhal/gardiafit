package com.example.gardiafit.model.aws

import kotlinx.serialization.Serializable

@Serializable
data class HealthRecord(
    val recordId: String,
    val userId: String,
    val timestamp: Long,
    val heartRate: Int,
    val steps: Int,
    val calories: Int,
    val distance: Int,
    val location: LocationData? = null,
    val sessionId: String? = null
)
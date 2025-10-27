package com.example.gardiafit.model.aws

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSession(
    val sessionId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalSteps: Int = 0,
    val totalCalories: Int = 0,
    val averageHeartRate: Double = 0.0,
    val locations: List<LocationData> = emptyList(),
    val status: SessionStatus = SessionStatus.ACTIVE
)

enum class SessionStatus {
    ACTIVE, COMPLETED, PAUSED
}

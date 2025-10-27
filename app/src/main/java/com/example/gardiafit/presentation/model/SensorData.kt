package com.example.gardiafit.model

data class SensorData(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val calories: Int = 0,
    val distance: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
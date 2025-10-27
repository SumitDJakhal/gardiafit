package com.example.gardiafit.service

import com.example.gardiafit.model.SensorData
import kotlin.random.Random

class SensorSimulator {
    private var isSimulating = false
    private var simulationThread: Thread? = null

    fun startSimulation(callback: (SensorData) -> Unit) {
        if (isSimulating) return

        isSimulating = true
        simulationThread = Thread {
            var stepCount = 0
            var baseHeartRate = 70

            while (isSimulating && !Thread.currentThread().isInterrupted) {
                try {
                    stepCount += Random.nextInt(1, 3)

                    val activityLevel = if (stepCount % 100 < 50) 0 else 1
                    baseHeartRate = when (activityLevel) {
                        0 -> Random.nextInt(60, 80)
                        else -> Random.nextInt(80, 120)
                    }

                    val heartRate = baseHeartRate + Random.nextInt(-5, 5)
                    val calories = (stepCount * 0.04).toInt()
                    val distance = (stepCount * 0.0008).toInt()

                    val sensorData = SensorData(
                        heartRate = heartRate,
                        steps = stepCount,
                        calories = calories,
                        distance = distance,
                        timestamp = System.currentTimeMillis()
                    )

                    callback(sensorData)
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { start() }
    }

    fun stopSimulation() {
        isSimulating = false
        simulationThread?.interrupt()
        simulationThread = null
    }
}
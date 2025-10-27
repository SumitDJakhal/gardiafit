package com.example.gardiafit.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.getCapabilities
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.gardiafit.model.SensorData
import com.example.gardiafit.model.aws.HealthRecord
import com.example.gardiafit.model.aws.WorkoutSession
import com.example.gardiafit.model.aws.SessionStatus
import com.example.gardiafit.service.aws.AWSService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val ExerciseCapabilities.supportedDataTypes: String
    get() {
        TODO()
    }

class HealthSensorService : LifecycleService() {
    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData

    private lateinit var healthServicesClient: HealthServicesClient
    private val sensorSimulator = SensorSimulator()
    private lateinit var awsService: AWSService
    private var currentSession: WorkoutSession? = null

    private val userId = "user_${android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)}"

    override fun onCreate() {
        super.onCreate()
        healthServicesClient = HealthServices.getClient(this)
        awsService = AWSService(this)
        startSensorCollection()
    }

    private fun startSensorCollection() {
        lifecycleScope.launch {
            try {
                initializeHealthServices()
            } catch (e: Exception) {
                Log.w("HealthSensorService", "Health Services not available, using simulation", e)
                startSimulation()
            }
        }
    }


    private suspend fun initializeHealthServices() {
        try {
            // Get the ExerciseClient from the main HealthServicesClient
            val exerciseClient = healthServicesClient.exerciseClient

            // Retrieve exercise capabilities (suspend function)
            val capabilities = exerciseClient.getCapabilities()

            Log.d(
                "HealthSensorService",
                "Supported Exercise Data Types: ${capabilities.supportedDataTypes}"
            )

            // Start your simulation or real data collection here
            startSimulation()

        } catch (e: Exception) {
            Log.e("HealthSensorService", "Failed to initialize Health Services", e)
            startSimulation() // fallback to simulation if capabilities not available
        }
    }

    private fun startSimulation() {
        startWorkoutSession()

        sensorSimulator.startSimulation { simulatedData ->
            lifecycleScope.launch {
                _sensorData.value = simulatedData
                saveHealthRecord(simulatedData)
            }
        }
    }

    private fun startWorkoutSession() {
        val sessionId = awsService.generateSessionId()
        currentSession = WorkoutSession(
            sessionId = sessionId,
            userId = userId,
            startTime = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            currentSession?.let { awsService.saveWorkoutSession(it) }
        }
    }

    private fun stopWorkoutSession() {
        currentSession?.let { session ->
            val updatedSession = session.copy(
                endTime = System.currentTimeMillis(),
                status = SessionStatus.COMPLETED
            )
            lifecycleScope.launch {
                awsService.saveWorkoutSession(updatedSession)
            }
        }
        currentSession = null
    }

    private suspend fun saveHealthRecord(sensorData: SensorData) {
        val record = HealthRecord(
            recordId = awsService.generateRecordId(),
            userId = userId,
            timestamp = sensorData.timestamp,
            heartRate = sensorData.heartRate,
            steps = sensorData.steps,
            calories = sensorData.calories,
            distance = sensorData.distance,
            sessionId = currentSession?.sessionId
        )

        awsService.saveHealthRecord(record)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorSimulator.stopSimulation()
        stopWorkoutSession()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

private fun AWSService.saveHealthRecord(record: HealthRecord) {
    TODO("Not yet implemented")
}

annotation class AWSService

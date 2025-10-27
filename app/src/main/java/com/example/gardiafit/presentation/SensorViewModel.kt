package com.example.gardiafit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gardiafit.model.AuthState
import com.example.gardiafit.model.SensorData
import com.example.gardiafit.model.aws.HealthRecord
import com.example.gardiafit.service.AuthService
import com.example.gardiafit.service.SensorSimulator
import com.example.gardiafit.service.aws.AWSService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * SensorViewModel - Manages health sensor data and AWS synchronization
 */
class SensorViewModel(
    private val awsService: AWSService,
    val authService: AuthService
) : ViewModel() {

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData

    // Use authState directly from AuthService
    val authState: StateFlow<AuthState> = authService.authState

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val sensorSimulator = SensorSimulator()

    init {
        // Auto-start monitoring if user is already signed in
        if (authService.getCurrentUser() != null) {
            startSensorMonitoring()
        }
    }

    fun startSensorMonitoring() {
        if (authService.getCurrentUser() != null) {
            sensorSimulator.startSimulation { data ->
                _sensorData.value = data
                viewModelScope.launch {
                    saveHealthRecordToAWS(data)
                }
            }
        }
    }

    suspend fun signUp(
        username: String,
        email: String?,
        phone: String?,
        emergencyContact: String,
        emergencyNumber: String,
        emergencyNote: String
    ) {
        authService.signUp(
            username = username,
            email = email,
            phone = phone,
            emergencyContact = emergencyContact,
            emergencyNumber = emergencyNumber,
            emergencyNote = emergencyNote
        )
    }

    fun signOut() {
        sensorSimulator.stopSimulation()
        authService.signOut()
    }

    fun resetAuthState() {
        authService.resetAuthState()
    }

    private suspend fun saveHealthRecordToAWS(sensorData: SensorData) {
        val currentUser = authService.getCurrentUser()
        if (currentUser != null) {
            val record = HealthRecord(
                recordId = awsService.generateRecordId(),
                userId = currentUser.userId,
                timestamp = sensorData.timestamp,
                heartRate = sensorData.heartRate,
                steps = sensorData.steps,
                calories = sensorData.calories,
                distance = sensorData.distance
            )
            awsService.saveHealthRecord(record)
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(1000) // Simulate sync time
            _isSyncing.value = false
        }
    }

    fun stopSensorMonitoring() {
        sensorSimulator.stopSimulation()
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorMonitoring()
    }
}

/**
 * Factory for creating SensorViewModel instances
 */
class SensorViewModelFactory(
    private val awsService: AWSService,
    private val authService: AuthService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            return SensorViewModel(awsService, authService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
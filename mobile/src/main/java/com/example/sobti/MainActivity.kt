package com.example.sobti

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.sobti.aws.AWSConfig
import com.example.sobti.aws.DynamoDBManager
import com.example.sobti.aws.DynamoDBManager.GetUserCallback
import com.example.sobti.aws.DynamoDBManager.UpdateCallback
import com.example.sobti.aws.DynamoDBManager.UserData
import com.google.android.gms.location.*
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity(), OnDataChangedListener {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvStatus: TextView

    private lateinit var dbManager: DynamoDBManager
    private lateinit var prefs: SharedPreferences
    private var userEmail: String? = null
    private var emergencyNumber: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: String = "Loading..."

    private var currentHeartRate = 0
    private var previousHeartRate = 0
    private var heartRateTrendCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("SobtiPrefs", MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "")

        // Initialize AWS
        AWSConfig.initialize(this)
        dbManager = DynamoDBManager(AWSConfig.getDDBClient())

        initViews()
        requestPermissions()
        setupLocationTracking()
        loadUserData()
        connectToWearable()
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvSteps = findViewById(R.id.tvSteps)
        tvLocation = findViewById(R.id.tvLocation)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    private fun setupLocationTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // ✅ Modern LocationRequest API
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                30000L // interval
            )
                .setMinUpdateIntervalMillis(15000L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        location?.let {
                            currentLocation = String.format("%.6f, %.6f", it.latitude, it.longitude)
                            updateLocationUI()
                        }
                    }
                },
                null
            )
        }
    }

    private fun loadUserData() {
        dbManager.getUserData(userEmail, object : GetUserCallback {
            override fun onSuccess(userData: UserData) {
                runOnUiThread {
                    tvUserName.text = "Welcome, ${userData.name}!"
                    emergencyNumber = userData.emergencyNumber

                    if (userData.lastHeartRate > 0) {
                        tvHeartRate.text = "${userData.lastHeartRate} bpm"
                    }
                    if (userData.lastSteps > 0) {
                        tvSteps.text = userData.lastSteps.toString()
                    }
                    userData.lastLocation?.let {
                        currentLocation = it
                        updateLocationUI()
                    }
                }
            }

            override fun onError(e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading user data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun connectToWearable() {
        Wearable.getDataClient(this).addListener(this)
        tvStatus.text = "Status: Connected to Watch"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri.path == WEAR_DATA_PATH) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val heartRate = dataMap.getInt("heartRate", 0)
                    val steps = dataMap.getInt("steps", 0)

                    runOnUiThread {
                        updateHealthData(heartRate, steps)
                    }
                }
            }
        }
    }

    private fun updateHealthData(heartRate: Int, steps: Int) {
        tvHeartRate.text = "$heartRate bpm"
        tvSteps.text = steps.toString()

        checkHeartRateTrend(heartRate)

        dbManager.updateHealthData(
            userEmail, heartRate, steps, currentLocation,
            object : UpdateCallback {
                override fun onSuccess() {}
                override fun onError(e: Exception?) {}
            })
    }

    private fun checkHeartRateTrend(heartRate: Int) {
        currentHeartRate = heartRate

        if (currentHeartRate > previousHeartRate && currentHeartRate > HIGH_HR_THRESHOLD) {
            heartRateTrendCount++
        } else if (currentHeartRate < previousHeartRate && currentHeartRate < LOW_HR_THRESHOLD) {
            heartRateTrendCount++
        } else {
            heartRateTrendCount = 0
        }

        if (heartRateTrendCount >= TREND_THRESHOLD) {
            triggerEmergency(heartRate)
            heartRateTrendCount = 0
        }

        previousHeartRate = currentHeartRate
    }

    private fun triggerEmergency(heartRate: Int) {
        if (emergencyNumber.isNullOrEmpty()) return

        val message = String.format(
            "SOBTI ALERT!\nAbnormal heart rate detected: %d bpm\nLocation: %s\nImmediate assistance needed!",
            heartRate, currentLocation
        )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                // ✅ Modern API to get SmsManager
                val smsManager = getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(emergencyNumber, null, message, null, null)

                runOnUiThread {
                    tvStatus.text = "Status: Emergency SMS Sent!"
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    Toast.makeText(this, "Emergency alert sent!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLocationUI() {
        tvLocation.text = currentLocation
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    this,
                    "Some permissions denied. App may not work properly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val WEAR_DATA_PATH = "/health_data"

        private const val TREND_THRESHOLD = 3
        private const val HIGH_HR_THRESHOLD = 120
        private const val LOW_HR_THRESHOLD = 50
    }
}

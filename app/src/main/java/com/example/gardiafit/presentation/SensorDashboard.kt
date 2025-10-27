package com.example.gardiafit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*

/**
 * Main dashboard - ONE DEFINITION ONLY
 */
@Composable
fun SensorDashboard(viewModel: SensorViewModel) {
    val sensorData by viewModel.sensorData.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val currentUser = viewModel.authService.getCurrentUser()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Welcome Header
        Text(
            text = "Welcome, ${currentUser?.username ?: "User"}!",
            style = MaterialTheme.typography.title2,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Health Monitoring Active",
            style = MaterialTheme.typography.body2,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Health Metrics
        HealthMetricCard("Heart Rate", "${sensorData.heartRate}", "BPM")
        Spacer(Modifier.height(8.dp))
        HealthMetricCard("Steps", "${sensorData.steps}", "steps")
        Spacer(Modifier.height(8.dp))
        HealthMetricCard("Calories", "${sensorData.calories}", "cal")

        Spacer(Modifier.height(16.dp))

        // Emergency Info
        var showEmergency by remember { mutableStateOf(false) }

        Chip(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showEmergency = !showEmergency },
            colors = ChipDefaults.chipColors(backgroundColor = Color.Red.copy(alpha = 0.15f)),
            label = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🆘 Emergency", color = Color.Red)
                    Text(if (showEmergency) "▲" else "▼", color = Color.Red)
                }
            }
        )

        if (showEmergency && currentUser != null) {
            Card(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Contact: ${currentUser.emergencyContact}")
                    Text("Phone: ${currentUser.emergencyNumber}")
                    Text("Notes: ${currentUser.emergencyNote}")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Sync Status
        Text(
            text = if (isSyncing) "Syncing..." else "Synced ✓",
            color = if (isSyncing) Color.Blue else Color.Green
        )

        Spacer(Modifier.height(8.dp))

        // Buttons
        Button(onClick = { viewModel.manualSync() }, enabled = !isSyncing, modifier = Modifier.fillMaxWidth()) {
            Text("Sync Now")
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out", color = Color.Red)
        }
    }
}

@Composable
private fun HealthMetricCard(label: String, value: String, unit: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.body2)
            Text(value, style = MaterialTheme.typography.title1)
            Text(unit, style = MaterialTheme.typography.caption1, color = Color.Gray)
        }
    }
}


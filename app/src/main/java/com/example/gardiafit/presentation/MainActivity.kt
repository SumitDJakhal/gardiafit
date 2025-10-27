package com.example.gardiafit.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.gardiafit.model.AuthState
import com.example.gardiafit.service.AuthService
import com.example.gardiafit.service.aws.AWSService

/**
 * Main Activity - Entry point of GardiaFit Wear OS app
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SensorViewModel by viewModels {
        SensorViewModelFactory(
            awsService = AWSService(this),
            authService = AuthService(this, AWSService(this))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AppContent(viewModel)
            }
        }
    }
}

/**
 * Main app content - handles different authentication states
 */
@Composable
private fun AppContent(viewModel: SensorViewModel) {
    val authState by viewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.SignedOut -> {
            // AuthScreen is defined in AuthScreen.kt
            AuthScreen(viewModel = viewModel)
        }
        is AuthState.SignedIn -> {
            // SensorDashboard is defined in SensorDashboard.kt
            SensorDashboard(viewModel)

            LaunchedEffect(Unit) {
                viewModel.startSensorMonitoring()
            }
        }
        is AuthState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetry = { viewModel.resetAuthState() }
            )
        }
    }
}

/**
 * Loading screen shown during initialization
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading GardiaFit...",
                style = MaterialTheme.typography.body2
            )
        }
    }
}

/**
 * Error screen shown when authentication fails
 */
@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Authentication Error",
            style = MaterialTheme.typography.title2,
            color = MaterialTheme.colors.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.body2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}


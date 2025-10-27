package com.example.gardiafit.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch

/**
 * Authentication screen - SINGLE DEFINITION ONLY
 */
@Composable
fun AuthScreen(viewModel: SensorViewModel) {
    var username by remember { mutableStateOf("TestUser") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("Mom") }
    var emergencyNumber by remember { mutableStateOf("911") }
    var emergencyNote by remember { mutableStateOf("No allergies") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏥 GardiaFit",
            style = MaterialTheme.typography.title1,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Health Guardian",
            style = MaterialTheme.typography.caption1,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Username
        Chip(
            onClick = { },
            label = { Text("Username: $username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Email
        Chip(
            onClick = { },
            label = { Text("Email: ${email.ifEmpty { "Optional" }}") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Phone
        Chip(
            onClick = { },
            label = { Text("Phone: ${phone.ifEmpty { "Optional" }}") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "🆘 Emergency Contacts",
            style = MaterialTheme.typography.body2,
            color = Color.Red
        )

        Spacer(Modifier.height(4.dp))

        // Emergency Contact
        Chip(
            onClick = { },
            label = { Text("Contact: $emergencyContact") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Emergency Number
        Chip(
            onClick = { },
            label = { Text("Phone: $emergencyNumber") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Emergency Note
        Chip(
            onClick = { },
            label = { Text("Notes: $emergencyNote") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.caption1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                if (username.isEmpty() || emergencyContact.isEmpty() || emergencyNumber.isEmpty()) {
                    errorMessage = "Fill required fields"
                    return@Button
                }

                isLoading = true
                errorMessage = ""

                scope.launch {
                    try {
                        viewModel.signUp(
                            username = username,
                            email = email.ifEmpty { null },
                            phone = phone.ifEmpty { null },
                            emergencyContact = emergencyContact,
                            emergencyNumber = emergencyNumber,
                            emergencyNote = emergencyNote
                        )
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Signup failed"
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Creating..." else "Start Monitoring")
        }
    }
}

// ⚠️ END OF FILE - NO MORE AuthScreen FUNCTIONS BELOW THIS LINE
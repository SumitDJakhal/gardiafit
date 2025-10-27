package com.example.gardiafit.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.gardiafit.model.AuthState
import com.example.gardiafit.model.aws.User
import com.example.gardiafit.service.aws.AWSService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AuthService(private val context: Context, private val awsService: AWSService) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val prefs: SharedPreferences = context.getSharedPreferences("gardiafit_auth", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val userId = prefs.getString(KEY_USER_ID, null)
            val username = prefs.getString(KEY_USERNAME, null)
            if (userId != null && username != null) {
                val user = User(
                    userId = userId,
                    username = username,
                    emergencyContact = "",
                    emergencyNumber = "",
                    emergencyNote = ""
                )
                _authState.value = AuthState.SignedIn(user)
            } else {
                _authState.value = AuthState.SignedOut
            }
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    suspend fun signUp(
        username: String,
        email: String?,
        phone: String?,
        emergencyContact: String,
        emergencyNumber: String,
        emergencyNote: String
    ): Boolean {
        return try {
            val userId = "user_${UUID.randomUUID()}"
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            val user = User(
                userId = userId,
                username = username,
                email = email,
                phone = phone,
                emergencyContact = emergencyContact,
                emergencyNumber = emergencyNumber,
                emergencyNote = emergencyNote,
                deviceId = deviceId
            )

            val success = awsService.saveUser(user)

            if (success) {
                saveLoginState(user)
                _authState.value = AuthState.SignedIn(user)
                Log.d("AuthService", "User signed up successfully: $username")
                true
            } else {
                _authState.value = AuthState.Error("Failed to create user account")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthService", "Signup error", e)
            _authState.value = AuthState.Error("Signup failed: ${e.message}")
            false
        }
    }

    suspend fun signIn(userId: String): Boolean {
        return try {
            val result = awsService.getUser(userId)

            val user: User? = when (result) {
                is User -> result
                is List<*> -> result.filterIsInstance<User>().firstOrNull()
                else -> null
            }

            if (user != null) {
                saveLoginState(user)
                _authState.value = AuthState.SignedIn(user)
                Log.d("AuthService", "User signed in successfully: ${user.username}")
                true
            } else {
                _authState.value = AuthState.Error("User not found")
                Log.w("AuthService", "User not found for ID: $userId")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthService", "Signin error", e)
            _authState.value = AuthState.Error("Signin failed: ${e.message}")
            false
        }
    }


    fun signOut() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.SignedOut
    }

    private fun saveLoginState(user: User) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.userId)
            putString(KEY_USERNAME, user.username)
            apply()
        }
    }

    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.SignedIn -> state.user
            else -> null
        }
    }

    fun resetAuthState() {
        checkAuthState()
    }
}
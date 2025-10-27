package com.example.gardiafit.model

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: com.example.gardiafit.model.aws.User) : AuthState()
    data class Error(val message: String) : AuthState()
}
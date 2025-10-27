package com.example.gardiafit.model.aws

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val emergencyContact: String,
    val emergencyNumber: String,
    val emergencyNote: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long? = null,
    val deviceId: String? = null,
    val isActive: Boolean = true
)
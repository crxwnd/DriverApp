package com.palace.driverapp.network.models

import com.google.gson.annotations.SerializedName

// ==================== LOGIN ====================

data class LoginRequest(
    val login: String,      // code o phone
    val password: String,
    val deviceId: String
)

data class LoginResponse(
    val token: String,
    val driver: DriverInfo,
    val session: SessionInfo
)

data class DriverInfo(
    val id: String,
    val code: String,
    val status: String      // "ACTIVE", "INACTIVE", "SUSPENDED"
)

data class SessionInfo(
    val sessionId: String,
    val expiresAt: String   // ISO timestamp
)

// ==================== TELEMETRÍA ====================

data class TelemetryRequest(
    val driverId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: String,  // ISO timestamp
    val accuracyM: Float?,
    val speedKph: Float?,
    val headingDeg: Float?,
    val deviceId: String
)

// ==================== DRIVERS EN VIVO ====================

data class LiveDriversResponse(
    val drivers: List<LiveDriver>
)

data class LiveDriver(
    val id: String,
    val code: String,
    val firstName: String,
    val lastNameP: String,
    val lastNameM: String?,
    val currentActivity: String,  // "AVAILABLE", "ON_TRIP", etc.
    val lastLat: Double?,
    val lastLng: Double?,
    val lastSeenAt: String?,
    val headingDeg: Float?,
    val speedKph: Float?
)

// ==================== ERRORES ====================

data class ErrorResponse(
    val error: String
)
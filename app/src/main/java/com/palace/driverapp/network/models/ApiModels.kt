package com.palace.driverapp.network.models

import com.google.gson.annotations.SerializedName

// ==================== LOGIN ====================

data class LoginRequest(
    val login: String,
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
    val status: String,
    val firstName: String?,
    val lastNameP: String?,
    val lastNameM: String?
)

data class SessionInfo(
    val sessionId: String,
    val expiresAt: String
)

// ==================== TELEMETRÍA ====================

data class TelemetryRequest(
    val driverId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: String,
    val accuracyM: Float?,
    val speedKph: Float?,
    val headingDeg: Float?,
    val deviceId: String
)

// ==================== DRIVERS EN VIVO ====================

data class LiveDriversResponse(
    val drivers: List<LiveDriver>?
)

data class LiveDriver(
    val id: String,
    val code: String,
    val firstName: String,
    val lastNameP: String,
    val lastNameM: String?,
    val currentActivity: String,
    val lastLat: Double?,
    val lastLng: Double?,
    val lastSeenAt: String?,
    val headingDeg: Float?,
    val speedKph: Float?
)

// ==================== VEHÍCULOS (CORRECTO SEGÚN TU BD) ====================

data class Vehicle(
    val id: Int,
    val code: String,
    val plate: String?,
    val make: String?,
    val model: String?,
    val year: Int?,
    val color: String?,
    val capacity: Int?,
    val status: String,
    val modelGlbUrl: String?,
    val modelPreviewUrl: String?,
    val modelScale: Float?,
    val modelHeadingOffsetDeg: Float?,
    val modelYOffsetM: Float?
)

data class GetVehiclesResponse(
    val total: Int?,
    val page: Int?,
    val pageSize: Int?,
    val items: List<Vehicle>?
)

data class DriverAttachVehicleDTO(
    val vehicleId: Int?,
    val vehicleCode: String?
)

data class AttachVehicleResponse(
    val assignment: VehicleAssignment?,
    val vehicle: Vehicle?,
    val idempotent: Boolean?
)

data class VehicleAssignment(
    val id: String,
    val driverId: String,
    val vehicleId: Int,
    val startsAt: String,
    val endsAt: String?,
    val active: Boolean,
    val createdAt: String?
)

// ==================== ERRORES ====================

data class ErrorResponse(
    val error: String
)
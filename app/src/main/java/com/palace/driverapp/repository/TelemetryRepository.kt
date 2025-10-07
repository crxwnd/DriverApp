package com.palace.driverapp.repository

import android.content.Context
import android.location.Location
import com.palace.driverapp.network.ApiConfig
import com.palace.driverapp.network.DriverApiService
import com.palace.driverapp.network.models.LiveDriver
import com.palace.driverapp.network.models.TelemetryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TelemetryRepository(context: Context) {

    private val authRepository = AuthRepository(context)
    private val apiService: DriverApiService = ApiConfig.createApiService {
        authRepository.getToken()
    }

    // ==================== ENVIAR TELEMETRÍA ====================

    suspend fun sendTelemetry(location: Location): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driverId = authRepository.getDriverId()
                ?: return@withContext Result.failure(Exception("No hay sesión activa"))

            val telemetry = TelemetryRequest(
                driverId = driverId,
                lat = location.latitude,
                lng = location.longitude,
                timestamp = formatTimestamp(Date()),
                accuracyM = location.accuracy,
                speedKph = if (location.hasSpeed()) location.speed * 3.6f else null, // m/s a km/h
                headingDeg = if (location.hasBearing()) location.bearing else null,
                deviceId = authRepository.getDeviceId()
            )

            val response = apiService.sendTelemetry(telemetry)

            when {
                response.isSuccessful -> {
                    Result.success(Unit)
                }
                response.code() == 401 -> {
                    // Token expirado, limpiar sesión
                    authRepository.clearSession()
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    Result.failure(Exception("Error al enviar telemetría: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ==================== OBTENER OTROS DRIVERS ====================

    suspend fun getLiveDrivers(): Result<List<LiveDriver>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLiveDrivers()

            when {
                response.isSuccessful && response.body() != null -> {
                    val myDriverId = authRepository.getDriverId()
                    // Filtrar: no mostrar mi propio driver
                    val otherDrivers = response.body()!!.drivers.filter {
                        it.id != myDriverId && it.lastLat != null && it.lastLng != null
                    }
                    Result.success(otherDrivers)
                }
                else -> {
                    Result.failure(Exception("Error al obtener drivers: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ==================== UTILIDADES ====================

    private fun formatTimestamp(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}
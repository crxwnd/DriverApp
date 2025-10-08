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

                    // El backend devuelve { drivers: [...] } con format=mobile
                    val allDrivers = response.body()!!.drivers ?: emptyList()

                    android.util.Log.d("TelemetryRepo", "🔍 Mi ID: $myDriverId")
                    android.util.Log.d("TelemetryRepo", "🔍 Total drivers en respuesta: ${allDrivers.size}")

                    allDrivers.forEach { driver ->
                        android.util.Log.d("TelemetryRepo", """
                        📍 ${driver.code} (${driver.firstName} ${driver.lastNameP})
                           - ID: ${driver.id}
                           - Lat: ${driver.lastLat}, Lng: ${driver.lastLng}
                           - Activity: ${driver.currentActivity}
                    """.trimIndent())
                    }

                    // Filtrar: no mostrar mi propio driver y solo los que tienen ubicación
                    val otherDrivers = allDrivers.filter {
                        val isNotMe = it.id != myDriverId
                        val hasLocation = it.lastLat != null && it.lastLng != null

                        android.util.Log.d("TelemetryRepo", "   Filter ${it.code}: isNotMe=$isNotMe, hasLocation=$hasLocation")

                        isNotMe && hasLocation
                    }

                    android.util.Log.d("TelemetryRepo", "✅ Drivers después de filtrar: ${otherDrivers.size}")

                    return@withContext Result.success(otherDrivers)
                }
                else -> {
                    android.util.Log.e("TelemetryRepo", "❌ Response code: ${response.code()}")
                    android.util.Log.e("TelemetryRepo", "❌ Response body: ${response.errorBody()?.string()}")
                    return@withContext Result.failure(Exception("Error al obtener drivers: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TelemetryRepo", "❌ Exception: ${e.message}", e)
            e.printStackTrace()
            return@withContext Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ==================== UTILIDADES ====================

    private fun formatTimestamp(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}
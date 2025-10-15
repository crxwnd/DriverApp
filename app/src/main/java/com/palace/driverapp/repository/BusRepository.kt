package com.palace.driverapp.repository

import android.content.Context
import com.palace.driverapp.network.ApiConfig
import com.palace.driverapp.network.DriverApiService
import com.palace.driverapp.network.models.AttachVehicleResponse
import com.palace.driverapp.network.models.DriverAttachVehicleDTO
import com.palace.driverapp.network.models.GetVehiclesResponse
import com.palace.driverapp.network.models.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusRepository(context: Context) {

    private val authRepository = AuthRepository(context)
    private val apiService: DriverApiService = ApiConfig.createApiService {
        authRepository.getToken()
    }

    // ==================== OBTENER AUTOBUSES ====================
    suspend fun getAvailableBuses(): Result<List<Vehicle>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAvailableBuses()

            return@withContext when {
                response.isSuccessful -> {
                    val body: GetVehiclesResponse? = response.body()
                    // Si items es null devolvemos lista vacía (o podrías devolver null según tu preferencia)
                    val items = body?.items ?: emptyList()
                    Result.success(items)
                }
                response.code() == 401 -> {
                    authRepository.clearSession()
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    Result.failure(Exception("Error al obtener autobuses: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ==================== SELECCIONAR AUTOBÚS ====================
    suspend fun selectBus(vehicleId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // validación de sesión / driverId (si lo necesitas)
            val driverId = authRepository.getDriverId()
            if (driverId == null) {
                return@withContext Result.failure(Exception("No hay sesión activa"))
            }

            val request = DriverAttachVehicleDTO(
                vehicleId = vehicleId,
                vehicleCode = null
            )

            val response = apiService.selectBus(request)

            return@withContext when {
                response.isSuccessful -> {
                    // opcional: comprobar body si quieres validar el contenido
                    val body: AttachVehicleResponse? = response.body()
                    // si necesitas alguna comprobación adicional sobre body, hazla aquí
                    Result.success(Unit)
                }
                response.code() == 401 -> {
                    authRepository.clearSession()
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    Result.failure(Exception("Error al seleccionar autobús: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }
}

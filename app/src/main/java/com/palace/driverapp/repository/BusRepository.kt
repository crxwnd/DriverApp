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
                    val items = body?.items ?: emptyList()
                    Result.success(items)
                }
                response.code() == 401 -> {
                    // ✅ CORREGIDO: Usar logout() en lugar de clearSession()
                    authRepository.logout()
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

    // ==================== ASIGNAR VEHÍCULO (ATTACH) ====================
    suspend fun attachVehicle(vehicleId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driverId = authRepository.getDriverId()
            if (driverId == null) {
                return@withContext Result.failure(Exception("No hay sesión activa"))
            }

            val request = DriverAttachVehicleDTO(
                vehicleId = vehicleId,
                vehicleCode = null
            )

            val response = apiService.attachVehicle(request)

            return@withContext when {
                response.isSuccessful -> {
                    // 200 (idempotente) o 201 (nueva asignación)
                    Result.success(Unit)
                }
                response.code() == 401 -> {
                    authRepository.clearSessionQuick()
                    Result.failure(SessionExpiredException("Sesión expirada"))
                }
                response.code() == 403 -> {
                    Result.failure(Exception("Driver no elegible para asignación"))
                }
                response.code() == 404 -> {
                    Result.failure(Exception("Vehículo no encontrado"))
                }
                response.code() == 409 -> {
                    Result.failure(Exception("Vehículo no disponible"))
                }
                else -> {
                    Result.failure(Exception("Error al asignar vehículo: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ==================== DESASIGNAR VEHÍCULO (DETACH) ====================
    suspend fun detachVehicle(): Result<Int?> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.detachVehicle()

            return@withContext when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body?.ok == true) {
                        // Limpiar datos del vehículo en local
                        authRepository.clearVehicleData()
                        Result.success(body.detachedVehicleId)
                    } else {
                        Result.failure(Exception(body?.error ?: "Error al desasignar"))
                    }
                }
                response.code() == 401 -> {
                    authRepository.clearSessionQuick()
                    Result.failure(SessionExpiredException("Sesión expirada"))
                }
                else -> {
                    Result.failure(Exception("Error al desasignar vehículo: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // Alias para compatibilidad con código existente
    suspend fun selectBus(vehicleId: Int): Result<Unit> = attachVehicle(vehicleId)
}

// Excepción específica para sesión expirada (útil para manejo global)
class SessionExpiredException(message: String) : Exception(message)
package com.palace.driverapp.repository

import android.content.Context
import com.palace.driverapp.network.ApiConfig
import com.palace.driverapp.network.DriverApiService
import com.palace.driverapp.network.models.Bus
import com.palace.driverapp.network.models.SelectBusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusRepository(context: Context) {

    private val authRepository = AuthRepository(context)
    private val apiService: DriverApiService = ApiConfig.createApiService {
        authRepository.getToken()
    }

    // ==================== OBTENER AUTOBUSES ====================

    suspend fun getAvailableBuses(): Result<List<Bus>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAvailableBuses()

            when {
                response.isSuccessful && response.body() != null -> {
                    val buses = response.body()!!.buses
                    Result.success(buses)
                }
                response.code() == 401 -> {
                    authRepository.clearSession()
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    Result.failure(Exception("Error al obtener autobuses: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ==================== SELECCIONAR AUTOBÚS ====================

    suspend fun selectBus(busId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driverId = authRepository.getDriverId()
                ?: return@withContext Result.failure(Exception("No hay sesión activa"))

            val request = SelectBusRequest(
                busId = busId,
                driverId = driverId
            )

            val response = apiService.selectBus(request)

            when {
                response.isSuccessful && response.body() != null -> {
                    Result.success(Unit)
                }
                response.code() == 401 -> {
                    authRepository.clearSession()
                    Result.failure(Exception("Sesión expirada"))
                }
                else -> {
                    Result.failure(Exception("Error al seleccionar autobús: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }
}
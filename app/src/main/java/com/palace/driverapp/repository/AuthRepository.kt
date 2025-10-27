// ⚠️ PRIMERO: Agrega este método a DriverApiService.kt

// En DriverApiService.kt agregar:
/*
@POST("api/driver/auth/logout")
suspend fun logout(
    @Query("all") logoutAll: Boolean = false
): Response<Unit>
*/

package com.palace.driverapp.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.palace.driverapp.network.ApiConfig
import com.palace.driverapp.network.DriverApiService
import com.palace.driverapp.network.models.LoginRequest
import com.palace.driverapp.network.models.LoginResponse
import com.palace.driverapp.network.models.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DriverAppPrefs", Context.MODE_PRIVATE)

    private val apiService: DriverApiService = ApiConfig.createApiService { getToken() }

    // ==================== GETTERS/SETTERS - AUTENTICACIÓN ====================

    fun getToken(): String? = prefs.getString("token", null)
    fun getDriverId(): String? = prefs.getString("driverId", null)
    fun getDriverCode(): String? = prefs.getString("driverCode", null)

    fun getDriverFullName(): String? {
        val firstName = prefs.getString("firstName", null)
        val lastNameP = prefs.getString("lastNameP", null)
        val lastNameM = prefs.getString("lastNameM", null)

        return when {
            firstName != null && lastNameP != null -> {
                if (lastNameM != null) {
                    "$firstName $lastNameP $lastNameM"
                } else {
                    "$firstName $lastNameP"
                }
            }
            firstName != null -> firstName
            else -> null
        }
    }

    fun getDriverFirstName(): String? = prefs.getString("firstName", null)
    fun isLoggedIn(): Boolean = getToken() != null && getDriverId() != null

    fun getDeviceId(): String {
        var deviceId = prefs.getString("deviceId", null)
        if (deviceId == null) {
            deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                UUID.randomUUID().toString()
            }
            prefs.edit().putString("deviceId", deviceId).apply()
        }
        return deviceId
    }

    private fun saveLoginData(response: LoginResponse) {
        prefs.edit().apply {
            putString("token", response.token)
            putString("driverId", response.driver.id)
            putString("driverCode", response.driver.code)
            putString("driverStatus", response.driver.status)
            response.driver.firstName?.let { putString("firstName", it) }
            response.driver.lastNameP?.let { putString("lastNameP", it) }
            response.driver.lastNameM?.let { putString("lastNameM", it) }
            putString("sessionId", response.session.sessionId)
            putString("expiresAt", response.session.expiresAt)
            putLong("loginTime", System.currentTimeMillis())
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    // ⚠️ NUEVO: Limpia solo SharedPreferences (para uso interno)
    private fun clearSessionLocal() {
        prefs.edit().apply {
            remove("token")
            remove("driverId")
            remove("driverCode")
            remove("driverStatus")
            remove("firstName")
            remove("lastNameP")
            remove("lastNameM")
            remove("sessionId")
            remove("expiresAt")
            remove("loginTime")
            remove("isLoggedIn")
            apply()
        }
    }

    // ==================== GETTERS/SETTERS - VEHÍCULOS ====================

    fun saveVehicleData(vehicle: Vehicle) {
        prefs.edit().apply {
            putInt("vehicleId", vehicle.id)
            putString("vehicleCode", vehicle.code)
            putString("vehiclePlate", vehicle.plate ?: "")
            putString("vehicleModel", vehicle.model ?: "")
            putString("vehicleMake", vehicle.make ?: "")
            putInt("vehicleCapacity", vehicle.capacity ?: 0)
            putString("vehicleStatus", vehicle.status)
            apply()
        }
    }

    fun getVehicleId(): Int? {
        val id = prefs.getInt("vehicleId", -1)
        return if (id == -1) null else id
    }

    fun getVehicleCode(): String? = prefs.getString("vehicleCode", null)
    fun getVehiclePlate(): String? = prefs.getString("vehiclePlate", null)
    fun getVehicleModel(): String? = prefs.getString("vehicleModel", null)
    fun getVehicleMake(): String? = prefs.getString("vehicleMake", null)
    fun getVehicleCapacity(): Int = prefs.getInt("vehicleCapacity", 0)
    fun getVehicleStatus(): String? = prefs.getString("vehicleStatus", null)

    fun clearVehicleData() {
        prefs.edit().apply {
            remove("vehicleId")
            remove("vehicleCode")
            remove("vehiclePlate")
            remove("vehicleModel")
            remove("vehicleMake")
            remove("vehicleCapacity")
            remove("vehicleStatus")
            apply()
        }
    }

    // ==================== AUTENTICACIÓN ====================

    suspend fun login(username: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(
                login = username,
                password = password,
                deviceId = getDeviceId()
            )

            val response = apiService.login(request)

            when {
                response.isSuccessful && response.body() != null -> {
                    val loginResponse = response.body()!!
                    saveLoginData(loginResponse)
                    Result.success(loginResponse)
                }
                response.code() == 401 -> {
                    Result.failure(Exception("Credenciales inválidas"))
                }
                response.code() == 403 -> {
                    Result.failure(Exception("Driver inactivo o suspendido"))
                }
                else -> {
                    Result.failure(Exception("Error en el servidor: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    // ✅ NUEVO: Logout real al backend
    suspend fun logout(logoutAll: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getToken()

            // Si no hay token, solo limpiamos local
            if (token == null) {
                clearSessionLocal()
                clearVehicleData()
                return@withContext Result.success(Unit)
            }

            // Llamar al backend para desasignar vehículo y cerrar sesión
            val response = apiService.logout(logoutAll)

            when {
                response.isSuccessful -> {
                    // Limpiar datos locales
                    clearSessionLocal()
                    clearVehicleData()
                    Result.success(Unit)
                }
                response.code() == 401 -> {
                    // Token inválido/expirado, limpiar igual
                    clearSessionLocal()
                    clearVehicleData()
                    Result.success(Unit)
                }
                else -> {
                    // Error en el servidor, pero limpiamos local de todos modos
                    android.util.Log.e("AuthRepository", "Error en logout: ${response.code()}")
                    clearSessionLocal()
                    clearVehicleData()
                    Result.failure(Exception("Error al cerrar sesión: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Excepción en logout", e)
            // Limpiar local aunque falle la petición
            clearSessionLocal()
            clearVehicleData()
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    // ✅ NUEVO: Versión síncrona para casos de emergencia (401)
    // Solo limpia local, NO llama al backend
    fun clearSessionQuick() {
        clearSessionLocal()
        clearVehicleData()
    }

    // ==================== VALIDACIÓN DE SESIÓN ====================

    fun isSessionExpired(): Boolean {
        val expiresAtStr = prefs.getString("expiresAt", null) ?: return true

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val expiresAt = format.parse(expiresAtStr)
            val now = Date()
            now.after(expiresAt)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun renewSessionIfNeeded(): Boolean {
        if (!isSessionExpired()) return true
        clearSessionLocal()
        clearVehicleData()
        return false
    }
}
package com.palace.driverapp.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.palace.driverapp.network.ApiConfig
import com.palace.driverapp.network.DriverApiService
import com.palace.driverapp.network.models.LoginRequest
import com.palace.driverapp.network.models.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DriverAppPrefs", Context.MODE_PRIVATE)

    // API Service con provider de token
    private val apiService: DriverApiService = ApiConfig.createApiService { getToken() }

    // ==================== GETTERS/SETTERS ====================

    fun getToken(): String? = prefs.getString("token", null)

    fun getDriverId(): String? = prefs.getString("driverId", null)

    fun getDriverCode(): String? = prefs.getString("driverCode", null)

    fun getDriverName(): String? = prefs.getString("driverName", null)

    fun isLoggedIn(): Boolean = getToken() != null && getDriverId() != null

    fun getDeviceId(): String {
        var deviceId = prefs.getString("deviceId", null)
        if (deviceId == null) {
            // Generar ID único del dispositivo
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
            putString("sessionId", response.session.sessionId)
            putString("expiresAt", response.session.expiresAt)
            putLong("loginTime", System.currentTimeMillis())
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // ==================== AUTENTICACIÓN ====================

    suspend fun login(username: String, password: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(login = username, password = password, deviceId = getDeviceId())
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        saveLoginData(body)
                        Result.success(body)              // ahora es Result<LoginResponse>
                    } else {
                        Result.failure(Exception("Respuesta vacía del servidor"))
                    }
                } else {
                    when (response.code()) {
                        401 -> Result.failure(Exception("Credenciales inválidas"))
                        403 -> Result.failure(Exception("Driver inactivo o suspendido"))
                        else -> Result.failure(Exception("Error en el servidor: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error de conexión: ${e.message}"))
            }
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

        // Si expiró, cerrar sesión y requerir login manual
        clearSession()
        return false
    }
}
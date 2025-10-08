package com.palace.driverapp.network

import com.palace.driverapp.network.models.*
import retrofit2.Response
import retrofit2.http.*

interface DriverApiService {

    /**
     * Login del driver
     * POST /api/driver/auth/login
     */
    @POST("api/driver/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Enviar telemetría
     * POST /api/driver/telemetry
     * Requiere token en el header (automático con AuthInterceptor)
     */
    @POST("api/driver/telemetry")
    suspend fun sendTelemetry(
        @Body telemetry: TelemetryRequest
    ): Response<Unit>

    /**
     * Obtener drivers en vivo
     * GET /api/live/drivers?format=mobile
     */
    @GET("api/live/drivers?format=mobile")  // ⬅️ Agrega el parámetro aquí
    suspend fun getLiveDrivers(): Response<LiveDriversResponse>
}
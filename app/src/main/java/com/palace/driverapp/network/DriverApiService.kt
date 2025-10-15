package com.palace.driverapp.network

import com.palace.driverapp.network.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
     * Requiere token en el header (AuthInterceptor)
     */
    @POST("api/driver/telemetry")
    suspend fun sendTelemetry(
        @Body telemetry: TelemetryRequest
    ): Response<Unit>

    /**
     * Obtener drivers en vivo
     * GET /api/live/drivers?format=mobile
     * Uso de @Query para mayor flexibilidad
     */
    @GET("api/live/drivers")
    suspend fun getLiveDrivers(
        @Query("format") format: String = "mobile"
    ): Response<LiveDriversResponse>

    /**
     * Obtener autobuses disponibles
     * GET /api/driver/vehicles
     * Requiere token en el header
     */
    @GET("api/driver/vehicles")
    suspend fun getAvailableBuses(): Response<GetVehiclesResponse>

    /**
     * Seleccionar un autobús (adjuntar vehículo al driver)
     * POST /api/driver/buses/select
     * Requiere token en el header
     *
     * Usamos DriverAttachVehicleDTO y retornamos AttachVehicleResponse
     * (ajusta la ruta si tu backend usa otra)
     */
    @POST("api/driver/buses/select")
    suspend fun selectBus(
        @Body request: DriverAttachVehicleDTO
    ): Response<AttachVehicleResponse>
}

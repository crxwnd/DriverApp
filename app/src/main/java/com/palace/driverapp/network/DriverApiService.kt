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
     * ✅ NUEVO: Logout del driver
     * POST /api/driver/auth/logout
     * @param logoutAll Si es true, cierra todas las sesiones del driver
     */
    @POST("api/driver/auth/logout")
    suspend fun logout(
        @Query("all") logoutAll: Boolean = false
    ): Response<Unit>

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
     * Asignar vehículo al driver (attach)
     * POST /api/driver/vehicle/attach
     * Requiere token en el header
     *
     * Respuestas:
     * - 201: Asignación creada exitosamente
     * - 200: Idempotente (ya existía la asignación)
     * - 400: JSON inválido
     * - 403: Driver no elegible (INACTIVE/SUSPENDED)
     * - 404: Vehículo no encontrado
     * - 409: Vehículo no disponible (INACTIVE/MAINTENANCE)
     */
    @POST("api/driver/vehicle/attach")
    suspend fun attachVehicle(
        @Body request: DriverAttachVehicleDTO
    ): Response<AttachVehicleResponse>

    /**
     * Desasignar vehículo actual del driver (detach)
     * POST /api/driver/vehicle/detach
     * Requiere token en el header
     * NO cierra sesión, solo desvincula el vehículo
     *
     * Respuestas:
     * - 200: Desasignación exitosa
     * - 401: No autenticado (sesión inválida/expirada)
     */
    @POST("api/driver/vehicle/detach")
    suspend fun detachVehicle(): Response<DetachVehicleResponse>

    /**
     * Obtener estado actual del driver (para validar sesión al inicio)
     * GET /api/driver/me
     * Requiere token en el header
     *
     * Si devuelve 401, la sesión ha expirado y hay que ir a login
     */
    @GET("api/driver/me")
    suspend fun getCurrentDriver(): Response<CurrentDriverResponse>
}
package com.palace.driverapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.palace.driverapp.MainActivity
import com.palace.driverapp.R
import com.palace.driverapp.repository.AuthRepository
import com.palace.driverapp.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TelemetryService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var telemetryRepository: TelemetryRepository
    private lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "telemetry_service_channel"
        const val ACTION_START = "ACTION_START_TELEMETRY"
        const val ACTION_STOP = "ACTION_STOP_TELEMETRY"

        fun start(context: Context) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        telemetryRepository = TelemetryRepository(this)
        authRepository = AuthRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
            }
        }

        // Si el sistema mata el servicio, NO lo reinicie (evita consumo innecesario)
        return START_NOT_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendTelemetryToBackend(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.create().apply {
                interval = 1000 // 1 segundo
                fastestInterval = 500
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                smallestDisplacement = 1f
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            android.util.Log.e("TelemetryService", "Permiso de ubicación denegado", e)
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun sendTelemetryToBackend(location: Location) {
        serviceScope.launch {
            val result = telemetryRepository.sendTelemetry(location)

            result.onFailure { exception ->
                android.util.Log.e("TelemetryService", "Error al enviar telemetría: ${exception.message}")

                // Si la sesión expiró, detener el servicio
                if (exception.message?.contains("Sesión expirada") == true) {
                    stopSelf()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val driverName = authRepository.getDriverFullName() ?: "Driver"
        val vehicleCode = authRepository.getVehicleCode() ?: "Sin vehículo"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rastreo Activo")
            .setContentText("$driverName • Vehículo: $vehicleCode")
            .setSmallIcon(R.drawable.ic_bus)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // No se puede deslizar para cerrar
            .setPriority(NotificationCompat.PRIORITY_LOW) // Baja prioridad (menos intrusivo)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetría GPS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Rastreo de ubicación en tiempo real"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
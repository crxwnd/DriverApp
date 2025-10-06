package com.palace.driverapp

import android.location.Location
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Gestor centralizado para manejar ubicaciones de drivers en Firebase
 */
class DriverLocationManager {

    private val database: DatabaseReference = Firebase.database.reference
    private val driversRef: DatabaseReference = database.child("drivers")

    // Listeners para detectar cambios
    private var driversListener: ChildEventListener? = null

    // Callback para notificar cambios
    private var onDriverLocationChanged: ((DriverLocation) -> Unit)? = null
    private var onDriverAdded: ((DriverLocation) -> Unit)? = null
    private var onDriverRemoved: ((String) -> Unit)? = null

    /**
     * Data class para representar la ubicación de un driver
     */
    data class DriverLocation(
        val driverId: String = "",
        val name: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val bearing: Float = 0f,
        val timestamp: Long = 0L,
        val isOnline: Boolean = true
    ) {
        // Constructor secundario para Firebase
        constructor() : this("", "", 0.0, 0.0, 0f, 0L, true)

        fun toMap(): Map<String, Any> {
            return mapOf(
                "driverId" to driverId,
                "name" to name,
                "latitude" to latitude,
                "longitude" to longitude,
                "bearing" to bearing,
                "timestamp" to timestamp,
                "isOnline" to isOnline
            )
        }
    }

    /**
     * Actualizar la ubicación del driver actual en Firebase
     */
    fun updateMyLocation(
        driverId: String,
        driverName: String,
        location: Location
    ) {
        val driverLocation = DriverLocation(
            driverId = driverId,
            name = driverName,
            latitude = location.latitude,
            longitude = location.longitude,
            bearing = if (location.hasBearing()) location.bearing else 0f,
            timestamp = System.currentTimeMillis(),
            isOnline = true
        )

        // Actualizar en Firebase
        driversRef.child(driverId).setValue(driverLocation.toMap())
            .addOnFailureListener { exception ->
                // Log error
                android.util.Log.e("DriverLocationManager", "Error updating location", exception)
            }
    }

    /**
     * Escuchar cambios en las ubicaciones de TODOS los drivers
     */
    fun startListeningToDrivers(
        onLocationChanged: (DriverLocation) -> Unit,
        onDriverAdded: (DriverLocation) -> Unit,
        onDriverRemoved: (String) -> Unit
    ) {
        this.onDriverLocationChanged = onLocationChanged
        this.onDriverAdded = onDriverAdded
        this.onDriverRemoved = onDriverRemoved

        driversListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(DriverLocation::class.java)?.let { driverLocation ->
                    onDriverAdded(driverLocation)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(DriverLocation::class.java)?.let { driverLocation ->
                    onLocationChanged(driverLocation)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val driverId = snapshot.key ?: return
                onDriverRemoved(driverId)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesario para este caso
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("DriverLocationManager", "Error listening to drivers", error.toException())
            }
        }

        driversRef.addChildEventListener(driversListener!!)
    }

    /**
     * Dejar de escuchar cambios
     */
    fun stopListeningToDrivers() {
        driversListener?.let {
            driversRef.removeEventListener(it)
        }
        driversListener = null
    }

    /**
     * Marcar driver como offline cuando cierre la app
     */
    fun markDriverOffline(driverId: String) {
        driversRef.child(driverId).child("isOnline").setValue(false)
    }

    /**
     * Eliminar driver de Firebase (cuando cierra sesión completamente)
     */
    fun removeDriver(driverId: String) {
        driversRef.child(driverId).removeValue()
    }

    /**
     * Configurar desconexión automática cuando se pierda conexión
     */
    fun setupDisconnectHandler(driverId: String) {
        // Cuando se pierda la conexión, marcar como offline
        driversRef.child(driverId).child("isOnline").onDisconnect().setValue(false)

        // También actualizar timestamp
        driversRef.child(driverId).child("timestamp").onDisconnect().setValue(ServerValue.TIMESTAMP)
    }

    /**
     * Limpiar drivers offline antiguos (más de 5 minutos)
     */
    fun cleanupOldDrivers() {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)

        driversRef.orderByChild("timestamp")
            .endAt(fiveMinutesAgo.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        val driverLocation = child.getValue(DriverLocation::class.java)
                        if (driverLocation?.isOnline == false) {
                            child.ref.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Log error
                }
            })
    }
}
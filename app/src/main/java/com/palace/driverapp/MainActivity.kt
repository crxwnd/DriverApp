package com.palace.driverapp

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    // Google Maps
    private lateinit var mMap: GoogleMap
    private var currentLocationMarker: Marker? = null
    private var accuracyCircle: Circle? = null

    // Marcadores de otros drivers
    private val otherDriversMarkers = mutableMapOf<String, Marker>()

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private var lastBearing: Float = 0f

    // Firebase
    private lateinit var driverLocationManager: DriverLocationManager
    private lateinit var myDriverId: String
    private lateinit var myDriverName: String

    // Navigation Drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    // Views del header
    private lateinit var headerView: View
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverEmail: TextView
    private lateinit var imgDriverProfile: CircleImageView

    // Permisos
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Control de cámara
    private var isFirstLocationUpdate = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar ID único del driver
        initializeDriverInfo()

        // Configurar Firebase
        setupFirebase()

        // Configurar toolbar
        setupToolbar()

        // Configurar Navigation Drawer
        setupNavigationDrawer()

        // Cargar datos del usuario
        loadUserDataInDrawer()

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initializeDriverInfo() {
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)

        // Obtener o crear ID único del driver
        myDriverId = sharedPref.getString("driverId", null) ?: run {
            val newId = "driver_" + UUID.randomUUID().toString().substring(0, 8)
            sharedPref.edit().putString("driverId", newId).apply()
            newId
        }

        // Obtener nombre del driver
        myDriverName = sharedPref.getString("username", "Driver") ?: "Driver"
    }

    private fun setupFirebase() {
        driverLocationManager = DriverLocationManager()

        // Configurar handler de desconexión
        driverLocationManager.setupDisconnectHandler(myDriverId)

        // Empezar a escuchar otros drivers
        driverLocationManager.startListeningToDrivers(
            onLocationChanged = { driverLocation ->
                // Actualizar marcador de driver existente
                if (driverLocation.driverId != myDriverId) {
                    updateOtherDriverMarker(driverLocation)
                }
            },
            onDriverAdded = { driverLocation ->
                // Nuevo driver apareció
                if (driverLocation.driverId != myDriverId) {
                    addOtherDriverMarker(driverLocation)
                }
            },
            onDriverRemoved = { driverId ->
                // Driver se fue
                removeOtherDriverMarker(driverId)
            }
        )

        // Limpiar drivers antiguos cada 5 minutos
        android.os.Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                driverLocationManager.cleanupOldDrivers()
                android.os.Handler(Looper.getMainLooper()).postDelayed(this, 5 * 60 * 1000)
            }
        }, 5 * 60 * 1000)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white, theme)

        navigationView.setNavigationItemSelectedListener(this)

        headerView = navigationView.getHeaderView(0)
        tvDriverName = headerView.findViewById(R.id.tvDriverName)
        tvDriverEmail = headerView.findViewById(R.id.tvDriverEmail)
        imgDriverProfile = headerView.findViewById(R.id.imgDriverProfile)

        val imgCompanyLogo = headerView.findViewById<ImageView>(R.id.imgCompanyLogo)
        animateCompanyLogo(imgCompanyLogo)
    }

    private fun animateCompanyLogo(logo: ImageView) {
        logo.post {
            val rotation = ObjectAnimator.ofFloat(logo, "rotation", 0f, 360f)
            rotation.duration = 20000
            rotation.repeatCount = ObjectAnimator.INFINITE
            rotation.interpolator = android.view.animation.LinearInterpolator()
            rotation.start()
        }
    }

    private fun loadUserDataInDrawer() {
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Usuario") ?: "Usuario"

        tvDriverName.text = "Juan Pérez"
        tvDriverEmail.text = username
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMap()
        checkLocationPermissions()
    }

    private fun setupMap() {
        try {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

            mMap.uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = true
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
                isTiltGesturesEnabled = true
                isRotateGesturesEnabled = true
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLocationPermissions() {
        if (hasLocationPermissions()) {
            enableLocationTracking()
        } else {
            requestLocationPermissions()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                enableLocationTracking()
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableLocationTracking() {
        if (!hasLocationPermissions()) return

        try {
            mMap.isMyLocationEnabled = false

            val locationRequest = LocationRequest.create().apply {
                interval = 1000
                fastestInterval = 500
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                smallestDisplacement = 1f
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        updateLocationOnMap(location)

                        // Enviar ubicación a Firebase
                        driverLocationManager.updateMyLocation(
                            myDriverId,
                            myDriverName,
                            location
                        )
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateLocationOnMap(it)
                }
            }

        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos de ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLocationOnMap(location: Location) {
        currentLocation = location
        val latLng = LatLng(location.latitude, location.longitude)

        if (currentLocationMarker == null) {
            val markerOptions = MarkerOptions()
                .position(latLng)
                .icon(getCustomLocationIcon())
                .anchor(0.5f, 0.5f)
                .flat(true)
                .rotation(location.bearing)
                .title("Tú")

            currentLocationMarker = mMap.addMarker(markerOptions)

            val circleOptions = CircleOptions()
                .center(latLng)
                .radius(location.accuracy.toDouble())
                .strokeColor(ContextCompat.getColor(this, R.color.primary))
                .strokeWidth(2f)
                .fillColor(0x40254D6E)

            accuracyCircle = mMap.addCircle(circleOptions)

        } else {
            animateMarkerToPosition(currentLocationMarker!!, latLng)

            if (location.hasBearing() && location.bearing != lastBearing) {
                animateMarkerRotation(currentLocationMarker!!, location.bearing)
                lastBearing = location.bearing
            }

            accuracyCircle?.center = latLng
            accuracyCircle?.radius = location.accuracy.toDouble()
        }

        // Cámara SIEMPRE sigue al usuario con rotación
        if (isFirstLocationUpdate) {
            val cameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(18f)
                .bearing(location.bearing)
                .tilt(45f)
                .build()

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1500, null)
            isFirstLocationUpdate = false
        } else {
            val cameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(mMap.cameraPosition.zoom)
                .bearing(location.bearing)
                .tilt(45f)
                .build()

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)
        }
    }

    // ==================== GESTIÓN DE OTROS DRIVERS ====================

    private fun addOtherDriverMarker(driverLocation: DriverLocationManager.DriverLocation) {
        val latLng = LatLng(driverLocation.latitude, driverLocation.longitude)

        val markerOptions = MarkerOptions()
            .position(latLng)
            .icon(getOtherDriverIcon())
            .anchor(0.5f, 0.5f)
            .flat(true)
            .rotation(driverLocation.bearing)
            .title(driverLocation.name)
            .snippet("Driver ID: ${driverLocation.driverId}")

        val marker = mMap.addMarker(markerOptions)
        marker?.let {
            otherDriversMarkers[driverLocation.driverId] = it

            // Mostrar info window brevemente
            it.showInfoWindow()
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                it.hideInfoWindow()
            }, 3000)
        }
    }

    private fun updateOtherDriverMarker(driverLocation: DriverLocationManager.DriverLocation) {
        val marker = otherDriversMarkers[driverLocation.driverId] ?: return
        val latLng = LatLng(driverLocation.latitude, driverLocation.longitude)

        animateMarkerToPosition(marker, latLng)
        animateMarkerRotation(marker, driverLocation.bearing)
    }

    private fun removeOtherDriverMarker(driverId: String) {
        otherDriversMarkers[driverId]?.let { marker ->
            marker.remove()
            otherDriversMarkers.remove(driverId)

            Toast.makeText(this, "${marker.title} se desconectó", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCustomLocationIcon(): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_arrow)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun getOtherDriverIcon(): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_other_driver)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun animateMarkerToPosition(marker: Marker, targetPosition: LatLng) {
        val startPosition = marker.position
        val startTime = System.currentTimeMillis()
        val duration = 1000L

        val handler = android.os.Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t = elapsed.toFloat() / duration

                if (t < 1.0) {
                    val lat = startPosition.latitude + (targetPosition.latitude - startPosition.latitude) * t
                    val lng = startPosition.longitude + (targetPosition.longitude - startPosition.longitude) * t
                    marker.position = LatLng(lat, lng)
                    handler.postDelayed(this, 16)
                } else {
                    marker.position = targetPosition
                }
            }
        })
    }

    private fun animateMarkerRotation(marker: Marker, targetRotation: Float) {
        val startRotation = marker.rotation
        var rotation = targetRotation

        var diff = rotation - startRotation
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        rotation = startRotation + diff

        val startTime = System.currentTimeMillis()
        val duration = 500L

        val handler = android.os.Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t = elapsed.toFloat() / duration

                if (t < 1.0) {
                    val currentRotation = startRotation + (rotation - startRotation) * t
                    marker.rotation = currentRotation
                    handler.postDelayed(this, 16)
                } else {
                    marker.rotation = rotation
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_help -> showHelp()
            R.id.nav_guide -> showGuide()
            R.id.nav_logout -> showLogoutConfirmation()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showHelp() {
        Toast.makeText(this, "Ayuda", Toast.LENGTH_SHORT).show()
    }

    private fun showGuide() {
        Toast.makeText(this, "Guía", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Cerrar sesión") { _, _ ->
                performLogout()
            }
            .show()
    }

    private fun performLogout() {
        // Marcar como offline en Firebase
        driverLocationManager.markDriverOffline(myDriverId)

        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Salir")
                .setMessage("¿Desea salir de la aplicación?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí") { _, _ ->
                    super.onBackPressed()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermissions() && ::locationCallback.isInitialized) {
            enableLocationTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        // Marcar como offline temporalmente
        driverLocationManager.markDriverOffline(myDriverId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        // Dejar de escuchar
        driverLocationManager.stopListeningToDrivers()
        // Eliminar de Firebase
        driverLocationManager.removeDriver(myDriverId)
    }
}
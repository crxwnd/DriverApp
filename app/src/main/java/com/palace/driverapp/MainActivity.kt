package com.palace.driverapp

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.palace.driverapp.network.models.LiveDriver
import com.palace.driverapp.repository.AuthRepository
import com.palace.driverapp.repository.TelemetryRepository
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Repositories
    private lateinit var authRepository: AuthRepository
    private lateinit var telemetryRepository: TelemetryRepository

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

    // Control
    private var isFirstLocationUpdate = true
    private var isFollowingUser = true
    private var isPollingOtherDrivers = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar repositories
        authRepository = AuthRepository(this)
        telemetryRepository = TelemetryRepository(this)

        // Verificar sesión
        if (!authRepository.isLoggedIn()) {
            goToLogin()
            return
        }

        // ✅ VALIDACIÓN NUEVA: Verificar que haya vehículo seleccionado
        if (authRepository.getVehicleId() == null) {
            val intent = Intent(this, BusSelectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

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

        // Listener personalizado para animaciones
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val contentView = findViewById<View>(R.id.map)
                contentView?.translationX = navigationView.width * slideOffset * 0.5f
                contentView?.scaleX = 1 - (slideOffset * 0.1f)
                contentView?.scaleY = 1 - (slideOffset * 0.1f)
            }

            override fun onDrawerOpened(drawerView: View) {
                animateDrawerContent()
            }

            override fun onDrawerClosed(drawerView: View) {
                val contentView = findViewById<View>(R.id.map)
                contentView?.animate()
                    ?.scaleX(1f)
                    ?.scaleY(1f)
                    ?.translationX(0f)
                    ?.setDuration(200)
                    ?.start()
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })

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

            val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.1f, 1f)
            val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.1f, 1f)
            scaleX.duration = 2000
            scaleY.duration = 2000
            scaleX.repeatCount = ObjectAnimator.INFINITE
            scaleY.repeatCount = ObjectAnimator.INFINITE
            scaleX.start()
            scaleY.start()
        }
    }

    private fun animateDrawerContent() {
        imgDriverProfile.scaleX = 0.8f
        imgDriverProfile.scaleY = 0.8f
        imgDriverProfile.alpha = 0.5f

        imgDriverProfile.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator())
            .start()

        tvDriverName.translationY = -20f
        tvDriverName.alpha = 0f

        tvDriverName.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay(100)
            .setDuration(500)
            .setInterpolator(BounceInterpolator())
            .start()

        tvDriverEmail.translationY = -20f
        tvDriverEmail.alpha = 0f

        tvDriverEmail.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay(200)
            .setDuration(500)
            .setInterpolator(BounceInterpolator())
            .start()

        animateMenuItems()
    }

    private fun animateMenuItems() {
        val menu = navigationView.menu
        var delay = 0L

        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val view = navigationView.findViewById<View>(menuItem.itemId)

            view?.let {
                it.translationX = -100f
                it.alpha = 0f

                it.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setStartDelay(delay)
                    .setDuration(300)
                    .setInterpolator(OvershootInterpolator())
                    .start()

                delay += 50
            }
        }
    }

    private fun loadUserDataInDrawer() {
        // Cargar datos reales del driver desde AuthRepository
        val driverCode = authRepository.getDriverCode() ?: "Driver"
        val driverFullName = authRepository.getDriverFullName() ?: "Nombre del Driver"

        tvDriverName.text = driverFullName  // Nombre completo real
        tvDriverEmail.text = driverCode      // Código del driver
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

            // Listener cuando el usuario mueve el mapa manualmente
            mMap.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isFollowingUser = false
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLocationPermissions() {
        if (hasLocationPermissions()) {
            enableLocationTracking()
            startPollingOtherDrivers()
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
                startPollingOtherDrivers()
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

                        // Enviar telemetría al backend
                        sendTelemetryToBackend(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { updateLocationOnMap(it) }
            }

        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendTelemetryToBackend(location: Location) {
        lifecycleScope.launch {
            val result = telemetryRepository.sendTelemetry(location)

            result.onFailure { exception ->
                if (exception.message?.contains("Sesión expirada") == true) {
                    Toast.makeText(this@MainActivity, "Sesión expirada", Toast.LENGTH_SHORT).show()
                    goToLogin()
                }
            }
        }
    }

    private fun startPollingOtherDrivers() {
        if (isPollingOtherDrivers) return
        isPollingOtherDrivers = true

        lifecycleScope.launch {
            while (isPollingOtherDrivers) {
                fetchOtherDrivers()
                delay(1000) // Actualizar cada 1 segundo (más rápido)
            }
        }
    }

    private suspend fun fetchOtherDrivers() {
        val result = telemetryRepository.getLiveDrivers()

        result.onSuccess { drivers ->
            android.util.Log.d("MainActivity", "✅ Drivers recibidos: ${drivers.size}")
            drivers.forEach { driver ->
                android.util.Log.d("MainActivity", "  - ${driver.code}: ${driver.lastLat}, ${driver.lastLng}")
            }
            updateOtherDriversOnMap(drivers)
        }.onFailure { exception ->
            android.util.Log.e("MainActivity", "❌ Error al obtener drivers: ${exception.message}")
        }
    }

    private fun updateOtherDriversOnMap(drivers: List<LiveDriver>) {
        // Eliminar marcadores de drivers que ya no están
        val currentDriverIds = drivers.map { it.id }.toSet()
        val markersToRemove = otherDriversMarkers.keys.filter { it !in currentDriverIds }

        markersToRemove.forEach { driverId ->
            otherDriversMarkers[driverId]?.remove()
            otherDriversMarkers.remove(driverId)
        }

        // Actualizar o crear marcadores
        drivers.forEach { driver ->
            if (driver.lastLat != null && driver.lastLng != null) {
                val latLng = LatLng(driver.lastLat, driver.lastLng)

                val existingMarker = otherDriversMarkers[driver.id]
                if (existingMarker != null) {
                    // Actualizar marcador existente
                    animateMarkerToPosition(existingMarker, latLng)
                    driver.headingDeg?.let { heading ->
                        animateMarkerRotation(existingMarker, heading)
                    }
                } else {
                    // Crear nuevo marcador
                    addOtherDriverMarker(driver, latLng)
                }
            }
        }
    }

    private fun addOtherDriverMarker(driver: LiveDriver, latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .icon(getOtherDriverIcon())
            .anchor(0.5f, 0.5f)
            .flat(true)
            .rotation(driver.headingDeg ?: 0f)
            .title("${driver.firstName} ${driver.lastNameP}") // Título (no se muestra)
            .snippet("${driver.currentActivity}")              // Snippet (no se muestra)

        val marker = mMap.addMarker(markerOptions)
        marker?.let {
            otherDriversMarkers[driver.id] = it
            // REMOVIDO: No mostrar info window automáticamente
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

        // Mover cámara solo si es la primera actualización o si está siguiendo al usuario
        if (isFirstLocationUpdate || isFollowingUser) {
            val cameraUpdate = if (isFirstLocationUpdate) {
                CameraUpdateFactory.newLatLngZoom(latLng, 18f)
            } else {
                CameraUpdateFactory.newLatLng(latLng)
            }

            mMap.animateCamera(cameraUpdate, 1000, null)
            isFirstLocationUpdate = false
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
        val dialog = Dialog(this, R.style.CustomDialogTheme)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_help, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val ivHelpIcon = view.findViewById<ImageView>(R.id.ivHelpIcon)
        val llEmail = view.findViewById<LinearLayout>(R.id.llEmail)
        val llPhone = view.findViewById<LinearLayout>(R.id.llPhone)
        val llWhatsApp = view.findViewById<LinearLayout>(R.id.llWhatsApp)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)

        ivHelpIcon.post { animateIcon(ivHelpIcon) }

        llEmail.setOnClickListener {
            animateClick(it)
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:soporte@driverapp.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Solicitud de Ayuda - Driver App")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el cliente de email", Toast.LENGTH_SHORT).show()
            }
        }

        llPhone.setOnClickListener {
            animateClick(it)
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:8001234567")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
            }
        }

        llWhatsApp.setOnClickListener {
            animateClick(it)
            try {
                val phoneNumber = "529981234567"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$phoneNumber?text=Hola, necesito ayuda con la aplicación")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showGuide() {
        val dialog = Dialog(this, R.style.CustomDialogTheme)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_guide, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val ivGuideIcon = view.findViewById<ImageView>(R.id.ivGuideIcon)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)

        ivGuideIcon.post { animateIcon(ivGuideIcon) }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun animateIcon(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1.2f, 1f)

        scaleX.duration = 600
        scaleY.duration = 600
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        scaleX.start()
        scaleY.start()

        val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 10f, -10f, 0f)
        rotation.duration = 800
        rotation.startDelay = 200
        rotation.start()
    }

    private fun animateClick(view: View) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f)
        scaleDown.duration = 150
        scaleDownY.duration = 150
        scaleDown.start()
        scaleDownY.start()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Cerrar sesión") { _, _ -> performLogout() }
            .show()
    }

    private fun performLogout() {
        authRepository.clearSession()
        authRepository.clearVehicleData()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
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
                .setPositiveButton("Sí") { _, _ -> super.onBackPressed() }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermissions() && ::locationCallback.isInitialized) {
            enableLocationTracking()
        }
        if (!isPollingOtherDrivers) {
            startPollingOtherDrivers()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPollingOtherDrivers = false
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
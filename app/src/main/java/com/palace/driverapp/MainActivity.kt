package com.palace.driverapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    // Views del header del navigation drawer
    private lateinit var headerView: android.view.View
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverEmail: TextView
    private lateinit var imgDriverProfile: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar toolbar
        setupToolbar()

        // Configurar Navigation Drawer
        setupNavigationDrawer()

        // Cargar datos del usuario en el drawer
        loadUserDataInDrawer()

        // Obtener el SupportMapFragment y notificar cuando el mapa esté listo
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupToolbar() {
        // Configurar la toolbar personalizada
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Ocultar el título de la app en la toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Mostrar el icono de menú hamburguesa
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Configurar el toggle (hamburguesa)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Cambiar color del icono hamburguesa
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white, theme)

        // Configurar el listener para los items del menú
        navigationView.setNavigationItemSelectedListener(this)

        // Obtener referencia al header view
        headerView = navigationView.getHeaderView(0)
        tvDriverName = headerView.findViewById(R.id.tvDriverName)
        tvDriverEmail = headerView.findViewById(R.id.tvDriverEmail)
        imgDriverProfile = headerView.findViewById(R.id.imgDriverProfile)
    }

    private fun loadUserDataInDrawer() {
        // Cargar datos del usuario desde SharedPreferences
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Usuario") ?: "Usuario"

        // Por ahora usaremos datos de prueba, después los obtendrás del servidor
        tvDriverName.text = "Juan Pérez"  // Nombre completo del driver
        tvDriverEmail.text = username  // Usuario o email

        // La foto de perfil por defecto está en el drawable
        // Cuando tengas URL de fotos, puedes usar Glide:
        // Glide.with(this).load(photoUrl).into(imgDriverProfile)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configurar el mapa
        setupMap()

        // Por ahora, centrar el mapa en una ubicación de ejemplo (Cancún)
        val cancun = LatLng(21.1619, -86.8515)
        mMap.addMarker(MarkerOptions().position(cancun).title("Mi ubicación"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cancun, 13f))

        // Configurar controles del mapa
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = false
        }

        // TODO: Aquí agregarás la lógica para:
        // - Obtener rutas del servidor
        // - Mostrar la ubicación actual
        // - Dibujar la ruta asignada
        // - Actualizar posición en tiempo real
    }

    private fun setupMap() {
        try {
            // Configurar estilo del mapa si lo deseas
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

            // Habilitar la ubicación si tienes permisos
            // Recuerda solicitar permisos en runtime para Android 6+
            // if (checkLocationPermission()) {
            //     mMap.isMyLocationEnabled = true
            // }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_help -> {
                showHelp()
            }
            R.id.nav_guide -> {
                showGuide()
            }
            R.id.nav_logout -> {
                showLogoutConfirmation()
            }
        }

        // Cerrar el drawer después de seleccionar una opción
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showHelp() {
        // Aquí puedes abrir una actividad de ayuda o mostrar un diálogo
        MaterialAlertDialogBuilder(this)
            .setTitle("Ayuda")
            .setMessage("Para soporte técnico:\n\n" +
                    "📧 Email: soporte@driverapp.com\n" +
                    "📱 Teléfono: 800-123-4567\n" +
                    "💬 WhatsApp: +52 998 123 4567\n\n" +
                    "Horario de atención:\n" +
                    "Lunes a Viernes: 8:00 AM - 8:00 PM\n" +
                    "Sábados: 9:00 AM - 2:00 PM")
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun showGuide() {
        // Aquí puedes mostrar una guía de uso o abrir una actividad
        MaterialAlertDialogBuilder(this)
            .setTitle("Guía de Uso")
            .setMessage("1. Visualización del Mapa:\n" +
                    "   • Tu ubicación actual se muestra con un punto azul\n" +
                    "   • La ruta asignada aparece en color verde\n\n" +
                    "2. Navegación:\n" +
                    "   • Usa los controles de zoom para acercar/alejar\n" +
                    "   • Desliza para moverte por el mapa\n\n" +
                    "3. Cronómetro:\n" +
                    "   • Se iniciará automáticamente al comenzar tu ruta\n" +
                    "   • Pausar/Reanudar con el botón correspondiente\n\n" +
                    "4. Reportes:\n" +
                    "   • Usa el botón de reporte para informar incidencias")
            .setPositiveButton("Entendido", null)
            .show()
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
        // Limpiar datos de sesión
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        // Regresar al login
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejar el click en el icono hamburguesa
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // Si el drawer está abierto, cerrarlo
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Mostrar confirmación para salir
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
}
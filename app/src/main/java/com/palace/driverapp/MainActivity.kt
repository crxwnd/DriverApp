package com.palace.driverapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
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
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator

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

        // Listener personalizado para animaciones del drawer
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Animación suave del contenido principal
                val moveFactor = navigationView.width * slideOffset
                val contentView = findViewById<View>(R.id.map)
                contentView?.translationX = moveFactor * 0.5f
                contentView?.scaleX = 1 - (slideOffset * 0.1f)
                contentView?.scaleY = 1 - (slideOffset * 0.1f)
            }

            override fun onDrawerOpened(drawerView: View) {
                // Animar elementos del drawer cuando se abre
                animateDrawerContent()
            }

            override fun onDrawerClosed(drawerView: View) {
                // Restaurar escala del contenido
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

        // Configurar el listener para los items del menú
        navigationView.setNavigationItemSelectedListener(this)

        // Obtener referencia al header view
        headerView = navigationView.getHeaderView(0)
        tvDriverName = headerView.findViewById(R.id.tvDriverName)
        tvDriverEmail = headerView.findViewById(R.id.tvDriverEmail)
        imgDriverProfile = headerView.findViewById(R.id.imgDriverProfile)

        // Animar logo de la empresa
        val imgCompanyLogo = headerView.findViewById<ImageView>(R.id.imgCompanyLogo)
        animateCompanyLogo(imgCompanyLogo)
    }

    private fun animateCompanyLogo(logo: ImageView) {
        logo.post {
            // Rotación continua sutil
            val rotation = ObjectAnimator.ofFloat(logo, "rotation", 0f, 360f)
            rotation.duration = 20000
            rotation.repeatCount = ObjectAnimator.INFINITE
            rotation.interpolator = android.view.animation.LinearInterpolator()
            rotation.start()

            // Escala sutil (breathing effect)
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
        // Animar foto de perfil
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

        // Animar nombre con bounce
        tvDriverName.translationY = -20f
        tvDriverName.alpha = 0f

        tvDriverName.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay(100)
            .setDuration(500)
            .setInterpolator(BounceInterpolator())
            .start()

        // Animar email
        tvDriverEmail.translationY = -20f
        tvDriverEmail.alpha = 0f

        tvDriverEmail.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay(200)
            .setDuration(500)
            .setInterpolator(BounceInterpolator())
            .start()

        // Animar items del menú secuencialmente
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
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Usuario") ?: "Usuario"

        // Por ahora datos de prueba
        tvDriverName.text = "Juan Pérez"
        tvDriverEmail.text = username

        // Efecto de typing animation para el nombre (opcional)
        //animateTextTyping(tvDriverName, "Juan Pérez")
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

    // Función mejorada para mostrar el diálogo de Ayuda
    private fun showHelp() {
        // Crear diálogo personalizado
        val dialog = Dialog(this, R.style.CustomDialogTheme)

        // Inflar el layout personalizado
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_help, null)
        dialog.setContentView(view)

        // Hacer que el diálogo sea cancelable tocando fuera
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // Referencias a las vistas
        val ivHelpIcon = view.findViewById<ImageView>(R.id.ivHelpIcon)
        val llEmail = view.findViewById<LinearLayout>(R.id.llEmail)
        val llPhone = view.findViewById<LinearLayout>(R.id.llPhone)
        val llWhatsApp = view.findViewById<LinearLayout>(R.id.llWhatsApp)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)

        // Animación de entrada del icono (rotación + escala)
        ivHelpIcon.post {
            animateIcon(ivHelpIcon)
        }

        // Click en Email - Abrir cliente de email
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

        // Click en Teléfono - Abrir marcador
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

        // Click en WhatsApp - Abrir WhatsApp
        llWhatsApp.setOnClickListener {
            animateClick(it)
            try {
                val phoneNumber = "529981234567" // Formato internacional sin +
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$phoneNumber?text=Hola, necesito ayuda con la aplicación")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón de cerrar
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Mostrar el diálogo
        dialog.show()
    }

    // Función mejorada para mostrar el diálogo de Guía
    private fun showGuide() {
        // Crear diálogo personalizado
        val dialog = Dialog(this, R.style.CustomDialogTheme)

        // Inflar el layout personalizado
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_guide, null)
        dialog.setContentView(view)

        // Hacer que el diálogo sea cancelable tocando fuera
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // Referencias a las vistas
        val ivGuideIcon = view.findViewById<ImageView>(R.id.ivGuideIcon)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)

        // Animación de entrada del icono
        ivGuideIcon.post {
            animateIcon(ivGuideIcon)
        }

        // Botón de cerrar
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Mostrar el diálogo
        dialog.show()
    }

    // Función para animar el icono del diálogo (efecto de "bounce")
    private fun animateIcon(view: View) {
        // Animación de escala (bounce effect)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1.2f, 1f)

        scaleX.duration = 600
        scaleY.duration = 600

        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()

        scaleX.start()
        scaleY.start()

        // Animación de rotación sutil
        val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 10f, -10f, 0f)
        rotation.duration = 800
        rotation.startDelay = 200
        rotation.start()
    }

    // Función para animar el click en las opciones
    private fun animateClick(view: View) {
        // Efecto de "press" al tocar
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f)

        scaleDown.duration = 150
        scaleDownY.duration = 150

        scaleDown.start()
        scaleDownY.start()
    }

    // NOTA: También puedes mantener la función showLogoutConfirmation con estilo mejorado
    private fun showLogoutConfirmation() {
        // Puedes usar el mismo estilo personalizado aquí también
        MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
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
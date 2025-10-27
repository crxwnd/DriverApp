package com.palace.driverapp

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palace.driverapp.adapter.BusAdapter
import com.palace.driverapp.network.models.Vehicle
import com.palace.driverapp.repository.AuthRepository
import com.palace.driverapp.repository.BusRepository
import kotlinx.coroutines.launch

class BusSelectionActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var rvBuses: RecyclerView
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var cvProgressContainer: CardView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvEmptyDescription: TextView

    private lateinit var authRepository: AuthRepository
    private lateinit var busRepository: BusRepository
    private lateinit var busAdapter: BusAdapter

    private var selectedBus: Vehicle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_selection)

        authRepository = AuthRepository(this)
        busRepository = BusRepository(this)

        // Validar sesión activa
        if (!authRepository.isLoggedIn()) {
            goToLogin()
            return
        }

        // Si ya hay vehículo seleccionado, ir directo al mapa
        if (authRepository.getVehicleId() != null) {
            navigateToMainActivity()
            return
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadBuses()
        startEntranceAnimations()
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        rvBuses = findViewById(R.id.rvBuses)
        btnContinue = findViewById(R.id.btnContinue)
        btnLogout = findViewById(R.id.btnLogout)
        cvProgressContainer = findViewById(R.id.cvProgressContainer)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        tvEmptyDescription = findViewById(R.id.tvEmptyDescription)
    }

    private fun setupRecyclerView() {
        busAdapter = BusAdapter(emptyList()) { vehicle ->
            selectedBus = vehicle
            btnContinue.isEnabled = true
            animateButtonEnable()
        }

        rvBuses.apply {
            layoutManager = LinearLayoutManager(this@BusSelectionActivity)
            adapter = busAdapter
        }
    }

    private fun setupListeners() {
        btnContinue.apply {
            isEnabled = false
            setOnClickListener {
                if (selectedBus != null) {
                    selectBusAndContinue(selectedBus!!)
                } else {
                    Toast.makeText(this@BusSelectionActivity, "Selecciona un vehículo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadBuses() {
        showLoading(true)

        lifecycleScope.launch {
            val result = busRepository.getAvailableBuses()

            showLoading(false)

            result.onSuccess { vehicles ->
                if (vehicles.isEmpty()) {
                    showEmptyState("No hay vehículos disponibles", "Contacta al administrador para obtener acceso a un vehículo")
                } else {
                    busAdapter.updateBuses(vehicles)
                    rvBuses.visibility = View.VISIBLE
                    llEmptyState.visibility = View.GONE
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Error desconocido"
                showEmptyState("Error al cargar vehículos", errorMsg)
            }
        }
    }

    private fun selectBusAndContinue(vehicle: Vehicle) {
        showLoading(true)

        lifecycleScope.launch {
            val result = busRepository.selectBus(vehicle.id)

            showLoading(false)

            result.onSuccess {
                authRepository.saveVehicleData(vehicle)
                Toast.makeText(
                    this@BusSelectionActivity,
                    "Vehículo ${vehicle.code} seleccionado",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToMainActivity()
            }.onFailure { exception ->
                Toast.makeText(
                    this@BusSelectionActivity,
                    "Error: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showEmptyState(title: String, description: String) {
        rvBuses.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmptyMessage.text = title
        tvEmptyDescription.text = description
        btnContinue.isEnabled = false
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            cvProgressContainer.visibility = View.VISIBLE
            cvProgressContainer.alpha = 0f
            cvProgressContainer.scaleX = 0.8f
            cvProgressContainer.scaleY = 0.8f

            cvProgressContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()

            btnContinue.isEnabled = false
            btnLogout.isEnabled = false
            rvBuses.isEnabled = false
        } else {
            cvProgressContainer.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction {
                    cvProgressContainer.visibility = View.GONE
                }
                .start()

            rvBuses.isEnabled = true
            btnLogout.isEnabled = true
        }
    }

    private fun animateButtonEnable() {
        val scaleX = ObjectAnimator.ofFloat(btnContinue, "scaleX", 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(btnContinue, "scaleY", 0.95f, 1f)
        scaleX.duration = 200
        scaleY.duration = 200
        scaleX.start()
        scaleY.start()
    }

    private fun startEntranceAnimations() {
        tvTitle.alpha = 0f
        tvTitle.translationY = -20f
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator())
            .start()

        tvSubtitle.alpha = 0f
        tvSubtitle.translationY = -20f
        tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(100)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator())
            .start()

        rvBuses.alpha = 0f
        rvBuses.translationY = 50f
        rvBuses.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Cerrar sesión") { _, _ -> performLogout() }
            .show()
    }

    // ✅ ACTUALIZADO: Ahora llama al backend antes de limpiar
    private fun performLogout() {
        showLoading(true)

        lifecycleScope.launch {
            // Llamar al backend para desasignar vehículo y cerrar sesión
            val result = authRepository.logout(logoutAll = false)

            showLoading(false)

            result.onSuccess {
                android.util.Log.d("BusSelection", "✅ Logout exitoso")
                Toast.makeText(
                    this@BusSelectionActivity,
                    "Sesión cerrada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { exception ->
                android.util.Log.e("BusSelection", "❌ Error en logout: ${exception.message}")
                // No mostramos error al usuario, ya limpiamos local
                Toast.makeText(
                    this@BusSelectionActivity,
                    "Sesión cerrada",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Ir al login (ya limpiamos en AuthRepository.logout())
            goToLogin()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        showLogoutConfirmation()
    }
}
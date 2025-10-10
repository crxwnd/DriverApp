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
import com.palace.driverapp.adapter.BusAdapter
import com.palace.driverapp.network.models.Bus
import com.palace.driverapp.repository.AuthRepository
import com.palace.driverapp.repository.BusRepository
import kotlinx.coroutines.launch

class BusSelectionActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var rvBuses: RecyclerView
    private lateinit var btnContinue: MaterialButton
    private lateinit var cvProgressContainer: CardView
    private lateinit var llEmptyState: LinearLayout

    private lateinit var authRepository: AuthRepository
    private lateinit var busRepository: BusRepository
    private lateinit var busAdapter: BusAdapter

    private var selectedBus: Bus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_selection)

        authRepository = AuthRepository(this)
        busRepository = BusRepository(this)

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
        cvProgressContainer = findViewById(R.id.cvProgressContainer)
        llEmptyState = findViewById(R.id.llEmptyState)
    }

    private fun setupRecyclerView() {
        busAdapter = BusAdapter(emptyList()) { selectedBus ->
            this.selectedBus = selectedBus
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
                    Toast.makeText(this@BusSelectionActivity, "Selecciona un autobús", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadBuses() {
        showLoading(true)

        lifecycleScope.launch {
            val result = busRepository.getAvailableBuses()

            showLoading(false)

            result.onSuccess { buses ->
                if (buses.isEmpty()) {
                    showEmptyState()
                } else {
                    busAdapter.updateBuses(buses)
                    rvBuses.visibility = View.VISIBLE
                    llEmptyState.visibility = View.GONE
                }
            }.onFailure { exception ->
                Toast.makeText(
                    this@BusSelectionActivity,
                    "Error al cargar autobuses: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState()
            }
        }
    }

    private fun selectBusAndContinue(bus: Bus) {
        showLoading(true)

        lifecycleScope.launch {
            val result = busRepository.selectBus(bus.id)

            showLoading(false)

            result.onSuccess {
                authRepository.saveBusData(bus)
                navigateToMainActivity()
            }.onFailure { exception ->
                Toast.makeText(
                    this@BusSelectionActivity,
                    "Error al seleccionar autobús: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showEmptyState() {
        rvBuses.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
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

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        // Prevenir que el usuario regrese al login
        moveTaskToBack(true)
    }
}
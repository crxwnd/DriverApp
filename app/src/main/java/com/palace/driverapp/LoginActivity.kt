package com.palace.driverapp


import kotlinx.coroutines.*
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    // Declaración de vistas
    private lateinit var cvLogo: CardView
    private lateinit var tvWelcomeTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var cvLoginForm: CardView
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var cvProgressContainer: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var cvError: CardView
    private lateinit var tvError: TextView

    // Scope para corrutinas
    private val loginScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar vistas
        initializeViews()

        // Configurar listeners
        setupListeners()

        // Verificar si ya hay una sesión activa
        checkExistingSession()

        // Iniciar animaciones de entrada
        startEntranceAnimations()
    }

    private fun initializeViews() {
        cvLogo = findViewById(R.id.cvLogo)
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        cvLoginForm = findViewById(R.id.cvLoginForm)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        cvProgressContainer = findViewById(R.id.cvProgressContainer)
        progressBar = findViewById(R.id.progressBar)
        cvError = findViewById(R.id.cvError)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupListeners() {
        // Listener del botón de login
        btnLogin.setOnClickListener {
            animateButtonClick(it)
            attemptLogin()
        }

        // Limpiar errores cuando el usuario empiece a escribir
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilUsername.error = null
                hideError()
            }
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPassword.error = null
                hideError()
            }
        }
    }

    private fun startEntranceAnimations() {
        // Hacer invisibles los elementos inicialmente
        cvLogo.alpha = 0f
        cvLogo.translationY = -100f
        tvWelcomeTitle.alpha = 0f
        tvSubtitle.alpha = 0f
        cvLoginForm.alpha = 0f
        cvLoginForm.translationY = 50f

        // Animar logo con bounce
        cvLogo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(BounceInterpolator())
            .start()

        // Animar título
        tvWelcomeTitle.animate()
            .alpha(1f)
            .setStartDelay(300)
            .setDuration(600)
            .start()

        // Animar subtítulo
        tvSubtitle.animate()
            .alpha(1f)
            .setStartDelay(450)
            .setDuration(600)
            .start()

        // Animar formulario
        cvLoginForm.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(600)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()

        // Rotación sutil del logo
        ObjectAnimator.ofFloat(cvLogo, "rotation", 0f, 360f).apply {
            duration = 1000
            startDelay = 200
            start()
        }
    }

    private fun animateButtonClick(view: View) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleDown).with(scaleDownY)
        animatorSet.play(scaleUp).with(scaleUpY).after(scaleDown)
        animatorSet.duration = 100
        animatorSet.start()
    }

    private fun attemptLogin() {
        // Obtener valores de los campos
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validar campos
        if (!validateInputs(username, password)) {
            return
        }

        // Mostrar loading
        showLoading(true)

        // Realizar login
        loginScope.launch {
            try {
                val loginSuccess = performLogin(username, password)

                if (loginSuccess) {
                    onLoginSuccess(username)
                } else {
                    onLoginError("Usuario o contraseña incorrectos")
                }
            } catch (e: Exception) {
                onLoginError("Error de conexión. Intente nuevamente")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            tilUsername.error = "Este campo es requerido"
            shakeView(tilUsername)
            isValid = false
        } else if (!isValidEmailOrUsername(username)) {
            tilUsername.error = "Ingrese un usuario o correo válido"
            shakeView(tilUsername)
            isValid = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "Este campo es requerido"
            shakeView(tilPassword)
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Mínimo 6 caracteres"
            shakeView(tilPassword)
            isValid = false
        }

        return isValid
    }

    private fun isValidEmailOrUsername(input: String): Boolean {
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        return emailPattern.matcher(input).matches() || input.length >= 3
    }

    private suspend fun performLogin(username: String, password: String): Boolean {
        delay(1500) // Simular llamada a API

        // TODO: Reemplazar con llamada real a tu API
        return (username == "admin" || username == "admin@driver.com") && password == "123456"
    }

    private fun onLoginSuccess(username: String) {
        // Animar éxito
        animateSuccess()

        // Guardar sesión
        saveUserSession(username)

        // Navegar después de la animación
        loginScope.launch {
            delay(800)
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun onLoginError(message: String) {
        tvError.text = message
        showError()
        shakeView(cvLoginForm)
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

            btnLogin.isEnabled = false
            etUsername.isEnabled = false
            etPassword.isEnabled = false
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

            btnLogin.isEnabled = true
            etUsername.isEnabled = true
            etPassword.isEnabled = true
        }
    }

    private fun showError() {
        cvError.visibility = View.VISIBLE
        cvError.alpha = 0f
        cvError.translationY = -20f

        cvError.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun hideError() {
        if (cvError.visibility == View.VISIBLE) {
            cvError.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(200)
                .withEndAction {
                    cvError.visibility = View.GONE
                }
                .start()
        }
    }

    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, -25f, 25f, -25f, 25f, -15f, 15f, -5f, 5f, 0f)
        shake.duration = 500
        shake.start()
    }

    private fun animateSuccess() {
        // Animar el formulario con efecto de éxito
        val scaleX = ObjectAnimator.ofFloat(cvLoginForm, "scaleX", 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(cvLoginForm, "scaleY", 1f, 1.05f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 400
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()

        // Cambiar color del botón temporalmente (efecto de éxito)
        btnLogin.setIconResource(android.R.drawable.ic_menu_send)
        btnLogin.text = "¡Bienvenido!"
    }

    private fun saveUserSession(username: String) {
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", true)
            putString("username", username)
            putLong("loginTime", System.currentTimeMillis())
            apply()
        }
    }

    private fun checkExistingSession() {
        val sharedPref = getSharedPreferences("DriverAppPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Si ya hay sesión, ir directamente a MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loginScope.cancel()
    }
}
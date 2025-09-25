package com.palace.driverapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    // Declaración de vistas
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
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

        // Verificar si ya hay una sesión activa (opcional)
        checkExistingSession()
    }

    private fun initializeViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupListeners() {
        // Listener del botón de login
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        // Limpiar errores cuando el usuario empiece a escribir
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilUsername.error = null
                tvError.visibility = View.GONE
            }
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPassword.error = null
                tvError.visibility = View.GONE
            }
        }
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

        // Realizar login (simulado por ahora)
        loginScope.launch {
            try {
                // Simulamos una llamada a la API con delay
                val loginSuccess = performLogin(username, password)

                if (loginSuccess) {
                    // Login exitoso
                    onLoginSuccess(username)
                } else {
                    // Login fallido
                    onLoginError("Usuario o contraseña incorrectos")
                }
            } catch (e: Exception) {
                // Error de red u otro
                onLoginError("Error de conexión. Intente nuevamente")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true

        // Validar usuario/email
        if (username.isEmpty()) {
            tilUsername.error = "Este campo es requerido"
            isValid = false
        } else if (!isValidEmailOrUsername(username)) {
            tilUsername.error = "Ingrese un usuario o correo válido"
            isValid = false
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.error = "Este campo es requerido"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        return isValid
    }

    private fun isValidEmailOrUsername(input: String): Boolean {
        // Verificar si es un email válido o un username (mínimo 3 caracteres)
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        return emailPattern.matcher(input).matches() || input.length >= 3
    }

    private suspend fun performLogin(username: String, password: String): Boolean {
        // Simulamos una llamada a la API con delay
        delay(2000)

        // TODO: Aquí deberías hacer la llamada real a tu API
        // Por ahora, simulamos un login exitoso con credenciales de prueba
        return (username == "admin" || username == "admin@driver.com") && password == "123456"

        /* Cuando tengas tu API, el código sería algo así:
        return try {
            val response = RetrofitClient.apiService.login(
                LoginRequest(username, password)
            )
            if (response.isSuccessful && response.body() != null) {
                // Guardar token o datos del usuario
                saveUserSession(response.body()!!)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            throw e
        }
        */
    }

    private fun onLoginSuccess(username: String) {
        // Guardar sesión en SharedPreferences
        saveUserSession(username)

        // Mostrar mensaje de éxito
        Toast.makeText(this, "Bienvenido $username", Toast.LENGTH_SHORT).show()

        // Navegar a MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun onLoginError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE

        // Vibrar el botón (opcional)
        btnLogin.animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction {
                btnLogin.animate()
                    .translationX(10f)
                    .setDuration(100)
                    .withEndAction {
                        btnLogin.animate()
                            .translationX(0f)
                            .setDuration(50)
                    }
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        etUsername.isEnabled = !show
        etPassword.isEnabled = !show
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
            // Si ya hay sesión, ir directamente a Home
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar corrutinas cuando se destruya la actividad
        loginScope.cancel()
    }
}
package com.example.luxhotel.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.R
import com.example.luxhotel.models.UserRole
import com.example.luxhotel.utils.AuthManager
import com.example.luxhotel.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import android.widget.Toast
import com.example.luxhotel.network.RetrofitClient
import com.example.luxhotel.network.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var cardLogin: CardView
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        tokenManager = TokenManager(this)

        if (sessionManager.isLoggedIn) {
            val role = sessionManager.userRole
            if (role != null) {
                navigateToDashboard(role)
                return
            } else {
                // If logged in but role is missing, clear and stay at login
                sessionManager.clearSession()
            }
        }

        setContentView(R.layout.activity_login)
        bindViews()
        setupListeners()
        playEntranceAnimation()
    }

    private fun bindViews() {
        cardLogin   = findViewById(R.id.cardLogin)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        etUsername  = findViewById(R.id.etUsername)
        etPassword  = findViewById(R.id.etPassword)
        btnLogin    = findViewById(R.id.btnLogin)
        tvError     = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener { attemptLogin() }
        etUsername.setOnFocusChangeListener { _, _ -> tilUsername.error = null }
        etPassword.setOnFocusChangeListener { _, _ -> tilPassword.error = null }
    }

    private fun attemptLogin() {
        val username = etUsername.text?.toString().orEmpty().trim()
        val password = etPassword.text?.toString().orEmpty()

        if (username.isEmpty()) {
            tilUsername.error = getString(R.string.error_empty_username)
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.error_empty_password)
            return
        }

        tilUsername.error = null
        tilPassword.error = null
        tvError.visibility = View.GONE
        setLoadingState(loading = true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@LoginActivity)
                val response = apiService.login(mapOf("username" to username, "password" to password))

                setLoadingState(loading = false)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val user = loginResponse?.user

                    if (loginResponse != null && user != null && user.role != null) {
                        // Save token
                        tokenManager.saveAuthData(loginResponse.token, user.username, user.role.name)
                        
                        // Save session
                        sessionManager.saveSession(
                            username    = user.username,
                            displayName = user.displayName,
                            role        = user.role,
                            phone       = user.phone,
                            email       = user.email,
                            department  = user.department,
                            joinDate    = user.joinDate,
                            staffId     = user.staffId,
                            avatarPath  = user.avatarPath
                        )
                        navigateToDashboard(user.role)
                    } else {
                        showLoginError("Dữ liệu phản hồi từ server không hợp lệ")
                    }
                } else {
                    showLoginError(getString(R.string.error_invalid_credentials))
                }
            } catch (e: Exception) {
                setLoadingState(loading = false)
                Toast.makeText(this@LoginActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoginError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        cardLogin.animate().translationX(-20f).setDuration(50)
            .withEndAction {
                cardLogin.animate().translationX(20f).setDuration(50)
                    .withEndAction {
                        cardLogin.animate().translationX(0f).setDuration(50).start()
                    }.start()
            }.start()
    }

    private fun navigateToDashboard(role: UserRole?) {
        val intent = when (role) {
            UserRole.ADMIN        -> Intent(this, AdminDashboardActivity::class.java)
            UserRole.RECEPTIONIST -> Intent(this, ReceptionistDashboardActivity::class.java)
            UserRole.HOUSEKEEPING -> Intent(this, HousekeepingDashboardActivity::class.java)
            null                  -> return
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoadingState(loading: Boolean) {
        btnLogin.isEnabled     = !loading
        btnLogin.alpha         = if (loading) 0.6f else 1.0f
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun playEntranceAnimation() {
        cardLogin.alpha = 0f
        cardLogin.translationY = 40f
        cardLogin.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100).start()
    }
}
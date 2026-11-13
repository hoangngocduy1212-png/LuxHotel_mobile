package com.example.luxhotel.network

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("luxhotel_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TOKEN_KEY = "jwt_token"
        private const val USERNAME_KEY = "username"
        private const val ROLE_KEY = "role"
    }

    fun saveAuthData(token: String?, username: String?, role: String?) {
        prefs.edit().apply {
            putString(TOKEN_KEY, token)
            putString(USERNAME_KEY, username)
            putString(ROLE_KEY, role)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(TOKEN_KEY, null)
    fun getUsername(): String? = prefs.getString(USERNAME_KEY, null)
    fun getRole(): String? = prefs.getString(ROLE_KEY, null)

    fun clearAuthData() {
        prefs.edit().clear().apply()
    }
}

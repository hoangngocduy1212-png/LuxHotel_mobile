package com.example.luxhotel.utils

import android.content.Context
import com.example.luxhotel.models.User
import com.example.luxhotel.models.UserRole

object AuthManager {
    private const val PREFS_NAME = "luxhotel_auth"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_PHONE = "phone"
    private const val KEY_EMAIL = "email"
    private const val KEY_DEPARTMENT = "department"
    private const val KEY_JOIN_DATE = "join_date"
    private const val KEY_STAFF_ID = "staff_id"

    fun saveUser(context: Context, user: User) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USERNAME, user.username)
            putString(KEY_ROLE, user.role?.name ?: UserRole.RECEPTIONIST.name)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_PHONE, user.phone)
            putString(KEY_EMAIL, user.email)
            putString(KEY_DEPARTMENT, user.department)
            putString(KEY_JOIN_DATE, user.joinDate)
            putString(KEY_STAFF_ID, user.staffId)
            apply()
        }
    }

    fun getUser(context: Context): User? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val roleStr = prefs.getString(KEY_ROLE, UserRole.RECEPTIONIST.name) ?: UserRole.RECEPTIONIST.name
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val phone = prefs.getString(KEY_PHONE, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val department = prefs.getString(KEY_DEPARTMENT, "") ?: ""
        val joinDate = prefs.getString(KEY_JOIN_DATE, "") ?: ""
        val staffId = prefs.getString(KEY_STAFF_ID, "") ?: ""
        
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.RECEPTIONIST
        }

        return User(
            username = username,
            role = role,
            displayName = displayName,
            phone = phone,
            email = email,
            department = department,
            joinDate = joinDate,
            staffId = staffId
        )
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun authenticate(context: Context, username: String, password: String): User? {
        val dbHelper = DatabaseHelper(context)
        return dbHelper.login(username, password)
    }

    private fun hashPassword(plaintext: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(plaintext.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
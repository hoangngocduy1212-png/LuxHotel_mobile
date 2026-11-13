package com.example.luxhotel.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.luxhotel.models.UserRole

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME      = "luxhotel_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME     = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE         = "role"
        private const val KEY_PHONE        = "phone"
        private const val KEY_EMAIL        = "email"
        private const val KEY_DEPARTMENT   = "department"
        private const val KEY_JOIN_DATE    = "join_date"
        private const val KEY_STAFF_ID     = "staff_id"
        private const val KEY_AVATAR_PATH  = "avatar_path"
        private const val KEY_PAYMENT_QR   = "payment_qr_path"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var paymentQRPath: String?
        get() = prefs.getString(KEY_PAYMENT_QR, null)
        set(value) = prefs.edit().putString(KEY_PAYMENT_QR, value).apply()

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val username: String?
        get() = prefs.getString(KEY_USERNAME, null)

    val displayName: String?
        get() = prefs.getString(KEY_DISPLAY_NAME, null)

    val phone: String?
        get() = prefs.getString(KEY_PHONE, null)

    val email: String?
        get() = prefs.getString(KEY_EMAIL, null)

    val department: String?
        get() = prefs.getString(KEY_DEPARTMENT, null)

    val joinDate: String?
        get() = prefs.getString(KEY_JOIN_DATE, null)

    val staffId: String?
        get() = prefs.getString(KEY_STAFF_ID, null)

    var avatarPath: String?
        get() = prefs.getString(KEY_AVATAR_PATH, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_PATH, value).apply()

    val userRole: UserRole?
        get() {
            val roleStr = prefs.getString(KEY_ROLE, null) ?: return null
            return runCatching { UserRole.valueOf(roleStr) }.getOrNull()
        }

    fun saveSession(
        username: String?,
        displayName: String?,
        role: UserRole?,
        phone: String? = "",
        email: String? = "",
        department: String? = "",
        joinDate: String? = "",
        staffId: String? = "",
        avatarPath: String? = null
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USERNAME, username)
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_ROLE, role?.name)
            putString(KEY_PHONE, phone)
            putString(KEY_EMAIL, email)
            putString(KEY_DEPARTMENT, department)
            putString(KEY_JOIN_DATE, joinDate)
            putString(KEY_STAFF_ID, staffId)
            putString(KEY_AVATAR_PATH, avatarPath)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
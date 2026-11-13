package com.example.luxhotel.activities

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.R
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import com.example.luxhotel.utils.SessionManager

abstract class BaseDashboardActivity : AppCompatActivity() {

    protected val sessionManager: SessionManager by lazy { SessionManager(this) }

    protected fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?")
            .setPositiveButton("Đăng xuất") { _, _ -> performLogout() }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun performLogout() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    protected fun showChangePasswordDialog() {
        val context = this
        val username = sessionManager.username ?: return

        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
        builder.setTitle("Đổi mật khẩu")

        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_change_password, null)
        builder.setView(dialogView)

        val etNewPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPass)
        val etConfirmPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPass)

        val dialog = builder.create()

        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnSubmit).setOnClickListener {
            val newPass = etNewPass.text.toString()
            val confirmPass = etConfirmPass.text.toString()

            if (newPass.isEmpty()) {
                etNewPass.error = "Mật khẩu không được để trống"
            } else if (newPass != confirmPass) {
                etConfirmPass.error = "Mật khẩu xác nhận không khớp"
            } else {
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.getApiService(context)
                            .changePassword(username, mapOf("newPassword" to newPass))
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "Có lỗi xảy ra: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }
}

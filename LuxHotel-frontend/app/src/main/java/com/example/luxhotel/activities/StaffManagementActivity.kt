package com.example.luxhotel.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luxhotel.R
import com.example.luxhotel.adapters.StaffAdapter
import com.example.luxhotel.models.User
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch

class StaffManagementActivity : AppCompatActivity() {

    private lateinit var adapter: StaffAdapter
    private lateinit var rvStaffList: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_management)

        rvStaffList = findViewById(R.id.rvStaffList)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        
        setupSwipeRefresh()
        setupRecyclerView()
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
        swipeRefreshLayout.setOnRefreshListener {
            loadStaffData()
        }
    }

    private fun loadStaffData() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@StaffManagementActivity).getAllUsers()
                if (response.isSuccessful) {
                    adapter.updateData(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                Toast.makeText(this@StaffManagementActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StaffAdapter(
            users = emptyList(),
            onStatusChanged = { user, isActive ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.getApiService(this@StaffManagementActivity)
                            .setUserActive(user.username ?: "", mapOf("isActive" to isActive))
                        if (response.isSuccessful) {
                            val status = if (isActive) "đã mở khóa" else "đã khóa"
                            Toast.makeText(this@StaffManagementActivity, "Tài khoản ${user.username ?: ""} $status", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@StaffManagementActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onItemClicked = { user ->
                showUserDetails(user)
            }
        )
        rvStaffList.layoutManager = LinearLayoutManager(this)
        rvStaffList.adapter = adapter
        loadStaffData()
    }

    private fun showUserDetails(user: User) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_details, null)
        builder.setView(dialogView)

        val tvStaffId = dialogView.findViewById<TextView>(R.id.tvDialogStaffId)
        val tvDisplayName = dialogView.findViewById<TextView>(R.id.tvDialogDisplayName)
        val tvRoleDept = dialogView.findViewById<TextView>(R.id.tvDialogRoleDept)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvDialogPhone)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDialogEmail)
        val tvJoinDate = dialogView.findViewById<TextView>(R.id.tvDialogJoinDate)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvDialogStatus)
        val btnCall = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogCall)

        tvStaffId.text = user.staffId.orEmpty().ifEmpty { "N/A" }
        tvDisplayName.text = user.displayName
        tvRoleDept.text = "${user.role?.name ?: "N/A"} • ${user.department.orEmpty().ifEmpty { "Chưa cập nhật" }}"
        tvPhone.text = user.phone.orEmpty().ifEmpty { "Chưa cập nhật" }
        tvEmail.text = user.email.orEmpty().ifEmpty { "Chưa cập nhật" }
        tvJoinDate.text = user.joinDate.orEmpty().ifEmpty { "Chưa cập nhật" }
        
        val isActiveStatus = user.isActive ?: false
        tvStatus.text = if (isActiveStatus) "Đang hoạt động" else "Đang bị khóa"
        tvStatus.setTextColor(if (isActiveStatus) getColor(R.color.green_available) else getColor(R.color.red_alert))

        val dialog = builder.create()
        dialog.show()

        btnCall.setOnClickListener {
            if (!user.phone.isNullOrEmpty()) {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:${user.phone}")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Nhân viên chưa cập nhật số điện thoại", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnDialogClose).setOnClickListener {
            dialog.dismiss()
        }
    }
}

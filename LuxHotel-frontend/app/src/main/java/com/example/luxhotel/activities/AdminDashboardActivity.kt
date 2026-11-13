package com.example.luxhotel.activities

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luxhotel.R
import com.example.luxhotel.utils.DatabaseHelper
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*

/**
 * AdminDashboardActivity — Dashboard dành cho Quản trị viên.
 */
class AdminDashboardActivity : BaseDashboardActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivQRPreview: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            saveQRInternal(it)
        }
    }

    private fun saveQRInternal(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(filesDir, "payment_qr.png")
                val outputStream = FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    sessionManager.paymentQRPath = Uri.fromFile(file).toString()
                    updateQRPreview()
                    Toast.makeText(this@AdminDashboardActivity, "Đã cập nhật mã QR thanh toán", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminDashboardActivity, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateQRPreview() {
        val path = sessionManager.paymentQRPath
        if (path != null) {
            ivQRPreview.setImageURI(Uri.parse(path))
            ivQRPreview.setPadding(2, 2, 2, 2)
            ivQRPreview.imageTintList = null
        } else {
            ivQRPreview.setImageResource(R.drawable.ic_camera)
            ivQRPreview.setPadding(8, 8, 8, 8)
            ivQRPreview.setColorFilter(getColor(R.color.gold_primary))
        }
    }

    private lateinit var tvRevenueAmount: TextView
    private lateinit var tvRevenueTrend: TextView
    private lateinit var ivRevenueTrend: ImageView
    private lateinit var tvRoomUsageText: TextView
    private lateinit var pbRoomUsage: LinearProgressIndicator
    private lateinit var tvRoomUsagePercent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        ivQRPreview = findViewById(R.id.ivQRPreview)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        
        // Bind stat views
        tvRevenueAmount = findViewById(R.id.tvRevenueAmount)
        tvRevenueTrend = findViewById(R.id.tvRevenueTrend)
        ivRevenueTrend = findViewById(R.id.ivRevenueTrend)
        tvRoomUsageText = findViewById(R.id.tvRoomUsageText)
        pbRoomUsage = findViewById(R.id.pbRoomUsage)
        tvRoomUsagePercent = findViewById(R.id.tvRoomUsagePercent)

        setupGreeting()
        setupLogout()
        setupSwipeRefresh()
        loadStatistics()
        setupDashboardCards()
        setupQREdit()
        updateQRPreview()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
        swipeRefreshLayout.setOnRefreshListener {
            loadStatistics()
        }
    }

    private fun setupGreeting() {
        val displayName = sessionManager.displayName ?: getString(R.string.role_admin)
        findViewById<TextView>(R.id.tvUsername).text = displayName
    }

    private fun setupLogout() {
        findViewById<android.widget.ImageButton>(R.id.btnLogout).setOnClickListener {
            confirmLogout()
        }
    }

    private fun setupQREdit() {
        ivQRPreview.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun loadStatistics() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1

                val apiService = RetrofitClient.getApiService(this@AdminDashboardActivity)
                
                // 1. THỐNG KÊ DOANH THU
                val currentRevenueResponse = apiService.getRevenueByMonth(currentYear, currentMonth)
                val currentRevenue = if (currentRevenueResponse.isSuccessful) currentRevenueResponse.body()?.totalRevenue ?: 0L else 0L

                // Lấy doanh thu tháng trước để so sánh
                val prevCal = Calendar.getInstance()
                prevCal.add(Calendar.MONTH, -1)
                val prevYear = prevCal.get(Calendar.YEAR)
                val prevMonth = prevCal.get(Calendar.MONTH) + 1
                val prevRevenueResponse = apiService.getRevenueByMonth(prevYear, prevMonth)
                val prevRevenue = if (prevRevenueResponse.isSuccessful) prevRevenueResponse.body()?.totalRevenue ?: 0L else 0L

                val statusCountsResponse = apiService.getRoomStatusCounts()
                val statusCounts = if (statusCountsResponse.isSuccessful) statusCountsResponse.body() ?: emptyMap() else emptyMap()

                val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
                tvRevenueAmount.text = formatter.format(currentRevenue)

                if (prevRevenue > 0) {
                    val percentChange = (((currentRevenue - prevRevenue).toDouble() / prevRevenue) * 100).toInt()
                    if (percentChange >= 0) {
                        tvRevenueTrend.text = getString(R.string.label_revenue_trend_positive, percentChange)
                        tvRevenueTrend.setTextColor(getColor(R.color.green_available))
                        ivRevenueTrend.setImageResource(R.drawable.ic_chevron_down)
                        ivRevenueTrend.rotation = 180f
                        ivRevenueTrend.setColorFilter(getColor(R.color.green_available))
                    } else {
                        tvRevenueTrend.text = getString(R.string.label_revenue_trend_negative, percentChange)
                        tvRevenueTrend.setTextColor(getColor(R.color.red_alert))
                        ivRevenueTrend.setImageResource(R.drawable.ic_chevron_down)
                        ivRevenueTrend.rotation = 0f
                        ivRevenueTrend.setColorFilter(getColor(R.color.red_alert))
                    }
                } else {
                    tvRevenueTrend.text = getString(R.string.empty_checkout_search)
                }

                // 2. TÌNH TRẠNG PHÒNG
                val occupied = statusCounts[DatabaseHelper.ROOM_STATUS_OCCUPIED] ?: 0
                val totalRooms = statusCounts.values.sum()
                
                tvRoomUsageText.text = getString(R.string.label_room_usage_format, occupied, totalRooms)
                pbRoomUsage.max = if (totalRooms > 0) totalRooms else 100
                pbRoomUsage.progress = occupied
                
                val percentUsage = if (totalRooms > 0) (occupied * 100 / totalRooms) else 0
                tvRoomUsagePercent.text = getString(R.string.label_occupancy_rate, percentUsage)
                
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupDashboardCards() {
        // "Quản lý phòng" → mở màn hình danh sách phòng
        setupCard(R.id.cardManageRooms, "Quản lý phòng", "Thêm và sửa trạng thái phòng", R.drawable.ic_building) {
            startActivity(Intent(this, RoomListActivity::class.java))
        }

        // "Quản lý tài khoản" → mở màn hình danh sách nhân viên
        setupCard(R.id.cardManageAccounts, "Quản lý tài khoản", "Khóa/Mở khóa tài khoản nhân viên", R.drawable.ic_lock) {
            startActivity(Intent(this, StaffManagementActivity::class.java))
        }

        // "Báo cáo sửa chữa" → hiển thị danh sách phòng cần bảo trì
        setupCard(R.id.cardRoomList, "Báo cáo sửa chữa", "Phê duyệt các yêu cầu bảo trì", R.drawable.ic_alert_triangle) {
            startActivity(Intent(this, AdminMaintenanceActivity::class.java))
        }

        // "Danh sách khách" → hiển thị khách đang ở/sắp đến/đã rời đi
        setupCard(R.id.cardGuestList, "Danh sách khách", "Quản lý khách hàng đang lưu trú", R.drawable.ic_customer) {
            startActivity(Intent(this, CustomerManagementActivity::class.java))
        }

        // "Lịch sử hóa đơn" → hiển thị các đơn đã thanh toán
        setupCard(R.id.cardInvoiceHistory, "Lịch sử hóa đơn", "Xem lại các giao dịch cũ", R.drawable.ic_checkout) {
            startActivity(Intent(this, InvoiceHistoryActivity::class.java))
        }
    }

    private fun setupCard(cardId: Int, title: String, subtitle: String, iconRes: Int, onClick: () -> Unit) {
        val card = findViewById<CardView>(cardId)
        card?.let {
            it.findViewById<TextView>(R.id.tvCardTitle)?.text = title
            it.findViewById<TextView>(R.id.tvCardSubtitle)?.text = subtitle
            it.findViewById<ImageView>(R.id.ivCardIcon)?.setImageResource(iconRes)
            it.setOnClickListener { onClick() }
        }
    }
}

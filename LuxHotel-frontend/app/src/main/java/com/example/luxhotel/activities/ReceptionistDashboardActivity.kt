package com.example.luxhotel.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luxhotel.R
import com.example.luxhotel.network.RetrofitClient
import com.example.luxhotel.utils.DatabaseHelper
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch

/**
 * ReceptionistDashboardActivity - Phiên bản an toàn (Safety Version)
 * Giảm thiểu tối đa nguy cơ crash do View Null hoặc lỗi thư viện biểu đồ.
 */
class ReceptionistDashboardActivity : BaseDashboardActivity() {

    private var pieChart: PieChart? = null
    private var swipeRefresh: SwipeRefreshLayout? = null

    private val bookingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadRoomStats()
            Toast.makeText(this, "Cập nhật dữ liệu thành công", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_receptionist_dashboard)
            
            // Tìm các View chính
            pieChart = findViewById(R.id.pieChartRoomStatus)
            swipeRefresh = findViewById(R.id.swipeRefreshLayout)

            setupPieChart()
            setupListeners()
            
            // Tải dữ liệu ban đầu
            loadRoomStats()
        } catch (e: Exception) {
            e.printStackTrace()
            // Nếu crash ở mức layout, hiển thị thông báo thay vì văng app
            Toast.makeText(this, "Lỗi giao diện: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        // Dashboard Cards - Sử dụng findViewById trực tiếp để tránh lỗi biến lateinit chưa khởi tạo
        findViewById<View>(R.id.cardBooking)?.setOnClickListener { 
            bookingLauncher.launch(Intent(this, BookingActivity::class.java)) 
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.cardCheckIn)?.setOnClickListener { 
            startActivity(Intent(this, CheckInActivity::class.java)) 
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.cardCheckOut)?.setOnClickListener { 
            startActivity(Intent(this, CheckoutListActivity::class.java)) 
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.cardProfile)?.setOnClickListener { 
            startActivity(Intent(this, ReceptionistProfileActivity::class.java)) 
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.cardCustomer)?.setOnClickListener { 
            startActivity(Intent(this, CustomerManagementActivity::class.java)) 
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.cardRoomCalendar)?.setOnClickListener { 
            startActivity(Intent(this, RoomCalendarActivity::class.java)) 
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        findViewById<View>(R.id.cardMap)?.setOnClickListener {
            startActivity(Intent(this, HotelMapActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Stats Layouts (Các số liệu tổng quan)
        findViewById<View>(R.id.layoutStatAvailable)?.setOnClickListener { openRoomList(DatabaseHelper.ROOM_STATUS_AVAILABLE) }
        findViewById<View>(R.id.layoutStatBooked)?.setOnClickListener { openRoomList(DatabaseHelper.ROOM_STATUS_BOOKED) }
        findViewById<View>(R.id.layoutStatOccupied)?.setOnClickListener { openRoomList(DatabaseHelper.ROOM_STATUS_OCCUPIED) }
        findViewById<View>(R.id.layoutStatDirty)?.setOnClickListener { openRoomList(DatabaseHelper.ROOM_STATUS_MAINTENANCE) }

        // Logout
        findViewById<View>(R.id.btnLogout)?.setOnClickListener { confirmLogout() }

        // Refresh
        swipeRefresh?.setOnRefreshListener { loadRoomStats() }
    }

    override fun onResume() {
        super.onResume()
        loadRoomStats()
        checkSyncStatus()
    }

    private fun loadRoomStats() {
        swipeRefresh?.isRefreshing = true
        lifecycleScope.launch {
            try {
                // Thử lấy dữ liệu từ Server
                val apiService = RetrofitClient.getApiService(this@ReceptionistDashboardActivity)
                val response = apiService.getRoomStatusCounts()
                
                if (response.isSuccessful) {
                    val counts = response.body() ?: emptyMap()
                    updateUI(counts)
                } else {
                    Toast.makeText(this@ReceptionistDashboardActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReceptionistDashboardActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh?.isRefreshing = false
                checkSyncStatus()
            }
        }
    }

    private fun loadLocalStats() {
        try {
            val dbHelper = DatabaseHelper(this)
            updateUI(dbHelper.getRoomStatusCounts())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI(counts: Map<String, Int>) {
        // Cập nhật text an toàn
        findViewById<TextView>(R.id.tvRoomAvailable)?.text = (counts[DatabaseHelper.ROOM_STATUS_AVAILABLE] ?: 0).toString()
        findViewById<TextView>(R.id.tvRoomBooked)?.text = (counts[DatabaseHelper.ROOM_STATUS_BOOKED] ?: 0).toString()
        findViewById<TextView>(R.id.tvRoomOccupied)?.text = (counts[DatabaseHelper.ROOM_STATUS_OCCUPIED] ?: 0).toString()
        findViewById<TextView>(R.id.tvRoomDirty)?.text = (counts[DatabaseHelper.ROOM_STATUS_MAINTENANCE] ?: 0).toString()

        // Cập nhật biểu đồ
        updatePieChart(
            (counts[DatabaseHelper.ROOM_STATUS_AVAILABLE] ?: 0).toFloat(),
            (counts[DatabaseHelper.ROOM_STATUS_BOOKED] ?: 0).toFloat(),
            (counts[DatabaseHelper.ROOM_STATUS_OCCUPIED] ?: 0).toFloat(),
            (counts[DatabaseHelper.ROOM_STATUS_MAINTENANCE] ?: 0).toFloat()
        )
    }

    private fun setupPieChart() {
        try {
            pieChart?.apply {
                isDrawHoleEnabled = true
                setHoleColor(Color.parseColor("#1E1E1E"))
                centerText = "Phòng"
                setCenterTextColor(Color.parseColor("#D4AF37"))
                setCenterTextSize(12f)
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false) // Tắt tương tác để tránh crash cảm ứng
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updatePieChart(av: Float, bk: Float, oc: Float, mt: Float) {
        if (pieChart == null) return
        
        try {
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()
            
            // Chỉ thêm các phần có giá trị > 0
            if (av > 0) { entries.add(PieEntry(av, "")); colors.add(Color.parseColor("#48BB78")) }
            if (bk > 0) { entries.add(PieEntry(bk, "")); colors.add(Color.parseColor("#4299E1")) }
            if (oc > 0) { entries.add(PieEntry(oc, "")); colors.add(Color.parseColor("#D4AF37")) }
            if (mt > 0) { entries.add(PieEntry(mt, "")); colors.add(Color.parseColor("#E53E3E")) }

            // Trường hợp dữ liệu trống
            if (entries.isEmpty()) {
                entries.add(PieEntry(1f, ""))
                colors.add(Color.parseColor("#2D3748"))
            }

            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                sliceSpace = 2f
                setDrawValues(false)
            }
            
            pieChart?.data = PieData(dataSet)
            pieChart?.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkSyncStatus() {
        try {
            val dbHelper = DatabaseHelper(this)
            val pendingItems = dbHelper.getPendingSyncItems()
            findViewById<ImageView>(R.id.ivSyncStatus)?.visibility = 
                if (pendingItems.isNotEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) { }
    }

    private fun openRoomList(status: String) {
        try {
            val intent = Intent(this, RoomListActivity::class.java)
            intent.putExtra("FILTER_STATUS", status)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở danh sách phòng", Toast.LENGTH_SHORT).show()
        }
    }
}

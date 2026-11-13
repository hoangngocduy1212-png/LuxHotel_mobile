package com.example.luxhotel.activities

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.example.luxhotel.R
import com.example.luxhotel.adapters.AdminMaintenanceAdapter
import com.example.luxhotel.utils.DatabaseHelper
import com.google.android.material.button.MaterialButton
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminMaintenanceActivity : AppCompatActivity() {

    private lateinit var rvMaintenance: RecyclerView
    private lateinit var adapter: AdminMaintenanceAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var tvPageInfo: TextView

    private var allReports = listOf<DatabaseHelper.MaintenanceReportItem>()
    private var currentPage = 0
    private val pageSize = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_maintenance)

        bindViews()
        setupListeners()
        loadReports()
    }

    private fun bindViews() {
        rvMaintenance = findViewById(R.id.rvMaintenance)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        tvPageInfo = findViewById(R.id.tvPageInfo)

        rvMaintenance.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
        swipeRefreshLayout.setOnRefreshListener {
            currentPage = 0
            loadReports()
        }

        btnPrev.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updateDisplayList()
            }
        }

        btnNext.setOnClickListener {
            if ((currentPage + 1) * pageSize < allReports.size) {
                currentPage++
                updateDisplayList()
            }
        }
    }

    private fun loadReports() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@AdminMaintenanceActivity)
                val response = apiService.getMaintenanceReports()
                if (response.isSuccessful) {
                    val reports = response.body() ?: emptyList()
                    allReports = reports.filter { 
                        it.status != DatabaseHelper.REPORT_STATUS_DONE 
                    }
                    updateDisplayList()
                } else {
                    Toast.makeText(this@AdminMaintenanceActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminMaintenanceActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateDisplayList() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allReports.size)
        val pagedList = if (allReports.isEmpty()) emptyList() else allReports.subList(start, end)

        adapter = AdminMaintenanceAdapter(
            pagedList,
            onItemClick = { report ->
                showReportDetailDialog(report)
            },
            onComplete = { report ->
                completeReport(report)
            }
        )
        rvMaintenance.adapter = adapter

        // Update pagination UI
        val totalPages = if (allReports.isEmpty()) 1 else kotlin.math.ceil(allReports.size.toDouble() / pageSize).toInt()
        tvPageInfo.text = "Trang ${currentPage + 1}/$totalPages"
        btnPrev.isEnabled = currentPage > 0
        btnNext.isEnabled = (currentPage + 1) < totalPages
    }

    private fun completeReport(report: DatabaseHelper.MaintenanceReportItem) {
        val syncRepository = com.example.luxhotel.network.SyncRepository(this)
        lifecycleScope.launch {
            val result = syncRepository.updateMaintenanceReportStatus(report.reportId, DatabaseHelper.REPORT_STATUS_DONE)
            when (result) {
                is com.example.luxhotel.network.SyncRepository.Result.Success -> {
                    Toast.makeText(this@AdminMaintenanceActivity, "Đã hoàn thành sửa chữa phòng ${report.roomNumber}", Toast.LENGTH_SHORT).show()
                    loadReports()
                }
                is com.example.luxhotel.network.SyncRepository.Result.SavedOffline -> {
                    Toast.makeText(this@AdminMaintenanceActivity, "Đã lưu ngoại tuyến, sẽ đồng bộ khi có mạng", Toast.LENGTH_SHORT).show()
                    loadReports()
                }
                is com.example.luxhotel.network.SyncRepository.Result.Error -> {
                    Toast.makeText(this@AdminMaintenanceActivity, "Lỗi: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showReportDetailDialog(report: DatabaseHelper.MaintenanceReportItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_maintenance_detail)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvRoom = dialog.findViewById<TextView>(R.id.tvDialogRoom)
        val tvDesc = dialog.findViewById<TextView>(R.id.tvDialogDesc)
        val tvReporter = dialog.findViewById<TextView>(R.id.tvDialogReporter)
        val ivPhoto = dialog.findViewById<ImageView>(R.id.ivDialogPhoto)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnDialogClose)

        tvRoom.text = "Phòng ${report.roomNumber}"
        tvDesc.text = report.description
        tvReporter.text = "Báo cáo bởi: ${report.reportedBy}"

        if (!report.imagePath.isNullOrEmpty()) {
            ivPhoto.visibility = View.VISIBLE
            ivPhoto.load(report.imagePath) {
                crossfade(true)
                placeholder(R.color.bg_dark_secondary)
            }
        } else {
            ivPhoto.visibility = View.GONE
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

package com.example.luxhotel.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import com.example.luxhotel.R
import com.example.luxhotel.helpers.TaskDrawerHelper
import com.example.luxhotel.models.DrawerType
import com.example.luxhotel.models.RoomTier
import com.example.luxhotel.models.TaskDrawerData
import com.example.luxhotel.models.TaskItem
import com.example.luxhotel.models.TaskPriority
import com.example.luxhotel.models.UserRole
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import com.example.luxhotel.utils.DatabaseHelper
import com.example.luxhotel.utils.SessionManager
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HousekeepingDashboardActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvTaskSummary: TextView
    private lateinit var tvCurrentLocation: TextView
    private lateinit var tvTotalRooms: TextView
    private lateinit var tvDoneCount: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            tvCurrentLocation.text = getString(R.string.label_location_denied)
        }
    }

    private lateinit var drawerHelpers: List<TaskDrawerHelper>
    private lateinit var allDrawers: List<TaskDrawerData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_housekeeping_dashboard)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        bindViews()
        setupToolbar()
        setupBottomNav()
        
        // Load data first, which will then setup drawers and stats
        setupData()

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Vui lòng bật GPS", Toast.LENGTH_SHORT).show()
            tvCurrentLocation.text = getString(R.string.label_location_gps_off)
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val lat = String.format(Locale.getDefault(), "%.4f", location.latitude)
                    val lng = String.format(Locale.getDefault(), "%.4f", location.longitude)
                    tvCurrentLocation.text = getString(R.string.label_location_format, lat, lng)
                } else {
                    tvCurrentLocation.text = getString(R.string.label_location_failed)
                }
            }
        } catch (e: SecurityException) {
            tvCurrentLocation.text = getString(R.string.label_location_error)
        }
    }

    private fun bindViews() {
        tvUsername    = findViewById(R.id.tvUsername)
        tvTaskSummary = findViewById(R.id.tvTaskSummary)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)
        tvTotalRooms  = findViewById(R.id.tvTotalRooms)
        tvDoneCount   = findViewById(R.id.tvDoneCount)
        bottomNav     = findViewById(R.id.bottomNav)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            handleLogout()
        }

        val staffName = SessionManager(this).displayName
            ?: getString(R.string.default_staff_name)
        tvUsername.text = staffName

        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        swipeRefreshLayout.isRefreshing = true
        setupData()
        setupDrawers()
        updateStats()
        getCurrentLocation()
        swipeRefreshLayout.isRefreshing = false
    }

    private fun setupData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@HousekeepingDashboardActivity)
                
                val cleanTasks = apiService.getTasksByType(today, DrawerType.CLEAN.name).body() ?: emptyList()
                val urgentTasks = apiService.getTasksByType(today, DrawerType.URGENT.name).body() ?: emptyList()
                val supplyTasks = apiService.getTasksByType(today, DrawerType.SUPPLY.name).body() ?: emptyList()
                val inspectTasks = apiService.getTasksByType(today, DrawerType.INSPECT.name).body() ?: emptyList()

                allDrawers = listOf(
                    TaskDrawerData(type = DrawerType.CLEAN, items = cleanTasks.toMutableList()),
                    TaskDrawerData(type = DrawerType.URGENT, items = urgentTasks.toMutableList()),
                    TaskDrawerData(type = DrawerType.SUPPLY, items = supplyTasks.toMutableList()),
                    TaskDrawerData(type = DrawerType.INSPECT, items = inspectTasks.toMutableList())
                )
                
                setupDrawers()
                updateStats()
            } catch (e: Exception) {
                Toast.makeText(this@HousekeepingDashboardActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                // Initialize with empty list to avoid lateinit issues
                if (!::allDrawers.isInitialized) {
                    allDrawers = listOf(
                        TaskDrawerData(type = DrawerType.CLEAN, items = mutableListOf()),
                        TaskDrawerData(type = DrawerType.URGENT, items = mutableListOf()),
                        TaskDrawerData(type = DrawerType.SUPPLY, items = mutableListOf()),
                        TaskDrawerData(type = DrawerType.INSPECT, items = mutableListOf())
                    )
                    setupDrawers()
                    updateStats()
                }
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupDrawers() {
        val drawerViewIds = listOf(
            R.id.drawerClean,
            R.id.drawerUrgent,
            R.id.drawerSupply,
            R.id.drawerInspect
        )

        val syncRepository = com.example.luxhotel.network.SyncRepository(this)

        drawerHelpers = allDrawers.mapIndexed { index, drawerData ->
            val view = findViewById<android.view.View>(drawerViewIds[index])
            TaskDrawerHelper(
                context = this,
                drawerView = view,
                data = drawerData
            ) { task -> 
                lifecycleScope.launch {
                    val result = syncRepository.setTaskDone(task.id.toLong(), task.isDone)
                    if (result is com.example.luxhotel.network.SyncRepository.Result.SavedOffline) {
                        Toast.makeText(this@HousekeepingDashboardActivity, "Đã lưu ngoại tuyến, sẽ đồng bộ khi có mạng", Toast.LENGTH_SHORT).show()
                    }
                    updateStats()
                }
            }.also { it.bind() }
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_dashboard

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true

                R.id.nav_report -> {
                    val options = ActivityOptionsCompat
                        .makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
                    startActivity(
                        Intent(this, MaintenanceReportActivity::class.java),
                        options.toBundle()
                    )
                    true
                }

                // ✅ Truyền role HOUSEKEEPING khi mở hồ sơ
                R.id.nav_profile -> {
                    val options = ActivityOptionsCompat
                        .makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
                    val intent = Intent(this, StaffProfileActivity::class.java).apply {
                        putExtra(StaffProfileActivity.EXTRA_ROLE, UserRole.HOUSEKEEPING.name)
                    }
                    startActivity(intent, options.toBundle())
                    true
                }

                else -> false
            }
        }
    }

    private fun updateStats() {
        val totalTasks = allDrawers.sumOf { it.items.size }
        val doneTasks  = allDrawers.sumOf { it.doneCount }
        val totalRooms = allDrawers
            .filter { (it.type == DrawerType.CLEAN || it.type == DrawerType.INSPECT) }
            .sumOf { it.items.size }

        tvTotalRooms.text  = totalRooms.toString()
        tvDoneCount.text   = doneTasks.toString()
        tvTaskSummary.text = getString(R.string.task_summary_format, totalTasks)
    }

    private fun handleLogout() {
        SessionManager(this).clearSession()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
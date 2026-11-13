package com.example.luxhotel.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.luxhotel.R
import com.example.luxhotel.adapters.MaintenanceReportAdapter
import com.example.luxhotel.databinding.ActivityMaintenanceReportBinding
import com.example.luxhotel.utils.DatabaseHelper
import com.example.luxhotel.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MaintenanceReportActivity : BaseDashboardActivity() {

    private lateinit var binding: ActivityMaintenanceReportBinding

    private var selectedCategory: String? = null
    private var selectedPriority: String? = null
    private val categoryViews = mutableListOf<LinearLayout>()

    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivIncidentPhoto.visibility = View.VISIBLE
            binding.ivIncidentPhoto.load(photoUri) {
                crossfade(true)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            captureIncidentPhoto()
        } else {
            Toast.makeText(this, "Cần quyền Camera để chụp ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaintenanceReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackPress()
        setupBottomNav()
        setupCategorySelection()
        setupPrioritySelection()
        setupDateTimePicker()
        setupButtons()
        loadRecentReports()
    }

    // ─── BACK PRESS (AndroidX OnBackPressedDispatcher) ────────────────────────
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    navigateToDashboard()
                }
            },
        )
    }

    // ─── BOTTOM NAV ───────────────────────────────────────────────────────────
    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_report

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateToDashboard()
                    true
                }
                R.id.nav_report -> true
                R.id.nav_profile -> {
                    val intent = Intent(this, StaffProfileActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, HousekeepingDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // ─── CATEGORY SELECTION ───────────────────────────────────────────────────
    private fun setupCategorySelection() {
        val categories = mapOf(
            binding.categoryElectric  to "Điện",
            binding.categoryWater     to "Nước",
            binding.categoryFurniture to "Nội thất",
            binding.categoryAC        to "Điều hòa",
            binding.categoryOther     to "Khác"
        )

        categoryViews.addAll(categories.keys)

        categories.forEach { (view, name) ->
            view.setOnClickListener {
                categoryViews.forEach { it.setBackgroundResource(R.drawable.bg_category_unselected) }
                view.setBackgroundResource(R.drawable.bg_category_selected)
                selectedCategory = name
            }
        }
    }

    // ─── PRIORITY SELECTION ───────────────────────────────────────────────────
    private fun setupPrioritySelection() {
        binding.priorityLow.setOnClickListener    { selectPriority("Thấp") }
        binding.priorityMedium.setOnClickListener { selectPriority("Trung bình") }
        binding.priorityHigh.setOnClickListener   { selectPriority("Khẩn cấp") }
    }

    private fun selectPriority(priority: String) {
        // Reset tất cả về unselected
        binding.priorityLow.setBackgroundResource(R.drawable.bg_priority_low_unselected)
        binding.priorityMedium.setBackgroundResource(R.drawable.bg_priority_medium_unselected)
        binding.priorityHigh.setBackgroundResource(R.drawable.bg_priority_high_unselected)

        // Highlight cái được chọn
        when (priority) {
            "Thấp"       -> binding.priorityLow.setBackgroundResource(R.drawable.bg_priority_low_selected)
            "Trung bình" -> binding.priorityMedium.setBackgroundResource(R.drawable.bg_priority_medium_selected)
            "Khẩn cấp"  -> binding.priorityHigh.setBackgroundResource(R.drawable.bg_priority_high_selected)
        }
        selectedPriority = priority

        // Hiển thị nút gọi kỹ thuật viên nếu là khẩn cấp
        binding.btnCallTechnician.visibility = if (priority == "Khẩn cấp") View.VISIBLE else View.GONE
    }

    // ─── DATE TIME PICKER ─────────────────────────────────────────────────────
    private fun setupDateTimePicker() {
        // EditText mở picker khi bấm
        binding.etDateTime.setOnClickListener { showDateTimePicker() }
        // Icon calendar mở picker
        binding.tilDateTime.setEndIconOnClickListener { showDateTimePicker() }
    }

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            R.style.CustomPickerTheme,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    R.style.CustomPickerTheme,
                    { _, hour, minute ->
                        val formatted = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d %02d:%02d",
                            day, month + 1, year, hour, minute
                        )
                        binding.etDateTime.setText(formatted)
                    },
                    cal[Calendar.HOUR_OF_DAY],
                    cal[Calendar.MINUTE],
                    true
                ).show()
            },
            cal[Calendar.YEAR],
            cal[Calendar.MONTH],
            cal[Calendar.DAY_OF_MONTH]
        ).show()
    }

    // ─── BUTTONS ──────────────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnSubmitReport.setOnClickListener { submitReport() }
        binding.btnSaveDraft.setOnClickListener    { saveDraft() }
        binding.btnCapturePhoto.setOnClickListener { captureIncidentPhoto() }
        binding.btnCallTechnician.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:0909123456")
            }
            startActivity(intent)
        }
    }

    private fun captureIncidentPhoto() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            return
        }
        val photoFile: File? = try {
            createImageFile()
        } catch (ignored: Exception) {
            Toast.makeText(this, "Lỗi tạo file ảnh", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            val uri = FileProvider.getUriForFile(
                this,
                "com.example.luxhotel.fileprovider",
                it
            )
            photoUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_INCIDENT_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun submitReport() {
        val roomNumber  = binding.etRoomNumber.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        when {
            roomNumber.isEmpty() -> {
                binding.etRoomNumber.error = "Vui lòng nhập số phòng"
                return
            }
            selectedCategory == null -> {
                Toast.makeText(this, "Vui lòng chọn loại sự cố", Toast.LENGTH_SHORT).show()
                return
            }
            selectedPriority == null -> {
                Toast.makeText(this, "Vui lòng chọn mức độ ưu tiên", Toast.LENGTH_SHORT).show()
                return
            }
            description.isEmpty() -> {
                binding.etDescription.error = "Vui lòng mô tả sự cố"
                return
            }
        }

        val fullDescription = "[$selectedCategory] - [Ưu tiên: $selectedPriority] - $description"
        val reportedBy = sessionManager.username ?: "unknown"

        val syncRepository = com.example.luxhotel.network.SyncRepository(this)
        lifecycleScope.launch {
            // Nếu có ảnh, ta ưu tiên gửi trực tiếp qua API (vốn có Multipart support)
            // Nếu không có mạng, SyncRepository sẽ lưu text draft
            if (currentPhotoPath != null) {
                try {
                    val apiService = RetrofitClient.getApiService(this@MaintenanceReportActivity)
                    val roomNumberBody = roomNumber.toRequestBody("text/plain".toMediaTypeOrNull())
                    val descriptionBody = fullDescription.toRequestBody("text/plain".toMediaTypeOrNull())
                    val reportedByBody = reportedBy.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val file = File(currentPhotoPath!!)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                    val response = apiService.addMaintenanceReport(roomNumberBody, descriptionBody, reportedByBody, imagePart)
                    if (response.isSuccessful) {
                        Toast.makeText(this@MaintenanceReportActivity, "Đã gửi báo cáo thành công!", Toast.LENGTH_SHORT).show()
                        clearForm()
                        loadRecentReports()
                    } else {
                        Toast.makeText(this@MaintenanceReportActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Fallback to sync repository if network fails (without image)
                    val result = syncRepository.addMaintenanceReport(roomNumber, fullDescription, reportedBy)
                    handleSyncResult(result)
                }
            } else {
                val result = syncRepository.addMaintenanceReport(roomNumber, fullDescription, reportedBy)
                handleSyncResult(result)
            }
        }
    }

    private fun handleSyncResult(result: com.example.luxhotel.network.SyncRepository.Result) {
        when (result) {
            is com.example.luxhotel.network.SyncRepository.Result.Success -> {
                Toast.makeText(this, "Đã gửi báo cáo thành công!", Toast.LENGTH_SHORT).show()
                clearForm()
                loadRecentReports()
            }
            is com.example.luxhotel.network.SyncRepository.Result.SavedOffline -> {
                Toast.makeText(this, "Đã lưu báo cáo ngoại tuyến", Toast.LENGTH_SHORT).show()
                clearForm()
                loadRecentReports()
            }
            is com.example.luxhotel.network.SyncRepository.Result.Error -> {
                Toast.makeText(this, "Lỗi: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDraft() {
        Toast.makeText(this, "Đã lưu nháp", Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        binding.etRoomNumber.text?.clear()
        binding.etFloor.text?.clear()
        binding.etDescription.text?.clear()
        binding.etDateTime.text?.clear()
        binding.ivIncidentPhoto.visibility = View.GONE
        binding.btnCallTechnician.visibility = View.GONE
        currentPhotoPath = null
        photoUri = null
        selectedCategory = null
        selectedPriority = null
        categoryViews.forEach { it.setBackgroundResource(R.drawable.bg_category_unselected) }
        binding.priorityLow.setBackgroundResource(R.drawable.bg_priority_low_unselected)
        binding.priorityMedium.setBackgroundResource(R.drawable.bg_priority_medium_unselected)
        binding.priorityHigh.setBackgroundResource(R.drawable.bg_priority_high_unselected)
    }

    // ─── RECENT REPORTS LIST ──────────────────────────────────────────────────
    private fun loadRecentReports() {
        binding.rvRecentReports.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@MaintenanceReportActivity).getMaintenanceReports()
                if (response.isSuccessful) {
                    val reports = response.body() ?: emptyList()
                    binding.rvRecentReports.adapter = MaintenanceReportAdapter(reports)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}

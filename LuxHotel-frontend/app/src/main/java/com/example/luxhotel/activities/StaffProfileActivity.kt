package com.example.luxhotel.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.load
import coil.transform.CircleCropTransformation
import com.example.luxhotel.R
import com.example.luxhotel.databinding.ActivityStaffProfileBinding
import com.example.luxhotel.databinding.ItemProfileRowBinding
import com.example.luxhotel.models.UserRole
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import com.example.luxhotel.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream

class StaffProfileActivity : BaseDashboardActivity() {

    private lateinit var binding: ActivityStaffProfileBinding
    private var currentRole: UserRole = UserRole.HOUSEKEEPING

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            updateAvatar(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)
            ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

        currentRole = roleFromIntent
            ?: SessionManager(this).userRole
                    ?: UserRole.HOUSEKEEPING

        setupBackPress()
        setupBottomNav()
        setupLogout()
        setupSettings()
        setupAvatar()
        loadProfileData()
    }

    // ─── AVATAR ──────────────────────────────────────────────────────────────
    private fun setupAvatar() {
        loadAvatarImage()

        binding.btnEditAvatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivAvatar.setOnLongClickListener {
            val avatarPath = sessionManager.avatarPath
            if (avatarPath != null) {
                showAvatarOptions(avatarPath)
            }
            true
        }
    }

    private fun loadAvatarImage() {
        val avatarPath = sessionManager.avatarPath
        if (avatarPath != null) {
            val url = if (avatarPath.startsWith("/")) {
                RetrofitClient.BASE_URL.trimEnd('/') + avatarPath
            } else {
                avatarPath
            }
            binding.ivAvatar.load(url) {
                crossfade(true)
                transformations(CircleCropTransformation())
                error(R.drawable.ic_person)
            }
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_person)
        }
    }

    private fun updateAvatar(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = File(cacheDir, "temp_avatar.jpg")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", tempFile.name, requestFile)

                val response = RetrofitClient.getApiService(this@StaffProfileActivity)
                    .updateAvatar(sessionManager.username ?: "", body)

                if (tempFile.exists()) tempFile.delete()

                if (response.isSuccessful) {
                    val newPath = response.body()?.avatarPath
                    sessionManager.avatarPath = newPath
                    loadAvatarImage()
                    Toast.makeText(this@StaffProfileActivity, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@StaffProfileActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@StaffProfileActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAvatarOptions(path: String) {
        val options = arrayOf("Lưu vào bộ sưu tập", "Xóa ảnh đại diện")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Ảnh đại diện")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveImageToGallery(path)
                    1 -> deleteAvatar()
                }
            }
            .show()
    }

    private fun saveImageToGallery(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) return

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
            val savedImageUri = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "LuxHotel_Avatar_${System.currentTimeMillis()}",
                "Avatar from LuxHotel App"
            )

            if (savedImageUri != null) {
                Toast.makeText(this, "Đã lưu ảnh vào bộ sưu tập", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không thể lưu ảnh", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAvatar() {
        val username = sessionManager.username ?: return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@StaffProfileActivity)
                    .deleteAvatar(username)

                if (response.isSuccessful) {
                    sessionManager.avatarPath = null
                    loadAvatarImage()
                    Toast.makeText(this@StaffProfileActivity, "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@StaffProfileActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StaffProfileActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── BACK PRESS ───────────────────────────────────────────────────────────
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToDashboard()
            }
        })
    }

    // ─── BOTTOM NAV ───────────────────────────────────────────────────────────
    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_profile

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateToDashboard()
                    true
                }
                R.id.nav_report -> {
                    navigateTo(MaintenanceReportActivity::class.java)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun navigateToDashboard() {
        navigateTo(HousekeepingDashboardActivity::class.java)
    }

    private fun <T> navigateTo(clazz: Class<T>) {
        val intent = Intent(this, clazz)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // ─── LOAD PROFILE DATA ────────────────────────────────────────────────────
    private fun loadProfileData() {
        // StaffProfileActivity chỉ dùng cho Housekeeping
        // Receptionist dùng ReceptionistProfileActivity riêng
        loadHousekeepingProfile()
    }

    private fun loadHousekeepingProfile() {
        val session = SessionManager(this)
        binding.tvProfileName.text = session.displayName ?: getString(R.string.default_staff_name)
        binding.tvStaffId.text     = getString(R.string.label_staff_id_format, session.staffId ?: "—")
        binding.tvShiftBadge.text  = "Ca sáng  •  07:00 – 15:00"
        binding.tvRoleBadge.text   = getString(R.string.role_housekeeping)

        setupRow(ItemProfileRowBinding.bind(binding.rowFullName.root),
            R.drawable.ic_person,   "Họ và tên",     session.displayName ?: "—")
        setupRow(ItemProfileRowBinding.bind(binding.rowPhone.root),
            R.drawable.ic_phone,    "Số điện thoại", session.phone ?: "—")
        setupRow(ItemProfileRowBinding.bind(binding.rowEmail.root),
            R.drawable.ic_email,    "Email",          session.email ?: "—")
        setupRow(ItemProfileRowBinding.bind(binding.rowDepartment.root),
            R.drawable.ic_building, "Phòng ban",      session.department ?: "—")
        setupRow(ItemProfileRowBinding.bind(binding.rowJoinDate.root),
            R.drawable.ic_calendar, "Ngày vào làm",  session.joinDate ?: "—")
    }

    private fun setupRow(
        rowBinding: ItemProfileRowBinding,
        iconRes: Int,
        label: String,
        value: String
    ) {
        rowBinding.ivRowIcon.setImageResource(iconRes)
        rowBinding.tvRowLabel.text = label
        rowBinding.tvRowValue.text = value
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────
    private fun setupLogout() {
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnLogoutMain.setOnClickListener { confirmLogout() }
    }

    // ─── SETTINGS ─────────────────────────────────────────────────────────────
    private fun setupSettings() {
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.switchNotification.isChecked = sessionManager.notificationsEnabled
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.notificationsEnabled = isChecked
            val msg = if (isChecked) "Đã bật thông báo" else "Đã tắt thông báo"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // ─── COMPANION ────────────────────────────────────────────────────────────
    companion object {
        const val EXTRA_ROLE = "extra_role"
    }
}
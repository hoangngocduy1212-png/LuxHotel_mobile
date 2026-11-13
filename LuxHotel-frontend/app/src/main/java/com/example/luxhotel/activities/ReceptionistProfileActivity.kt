package com.example.luxhotel.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.luxhotel.R
import com.example.luxhotel.databinding.ActivityReceptionistProfileBinding
import com.example.luxhotel.databinding.ItemProfileRowBinding
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.luxhotel.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream

class ReceptionistProfileActivity : BaseDashboardActivity() {

    private lateinit var binding: ActivityReceptionistProfileBinding

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            updateAvatar(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceptionistProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackPress()
        setupToolbar()
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

                val response = RetrofitClient.getApiService(this@ReceptionistProfileActivity)
                    .updateAvatar(sessionManager.username ?: "", body)

                if (tempFile.exists()) tempFile.delete()

                if (response.isSuccessful) {
                    val newPath = response.body()?.avatarPath
                    sessionManager.avatarPath = newPath
                    loadAvatarImage()
                    Toast.makeText(this@ReceptionistProfileActivity, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ReceptionistProfileActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ReceptionistProfileActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
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
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bitmap = if (path.startsWith("http")) {
                    // Nếu là URL, tải về bằng Coil hoặc OkHttp
                    val loader = this@ReceptionistProfileActivity.imageLoader
                    val request = ImageRequest.Builder(this@ReceptionistProfileActivity)
                        .data(path)
                        .allowHardware(false) // Cần thiết để lấy bitmap
                        .build()
                    val result = loader.execute(request)
                    (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else {
                    // Nếu là local path
                    android.graphics.BitmapFactory.decodeFile(path)
                }

                if (bitmap == null) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(this@ReceptionistProfileActivity, "Không thể lấy dữ liệu ảnh", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val filename = "LuxHotel_Avatar_${System.currentTimeMillis()}.jpg"
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                    }
                }

                val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(this@ReceptionistProfileActivity, "Đã lưu ảnh vào bộ sưu tập", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(this@ReceptionistProfileActivity, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteAvatar() {
        val username = sessionManager.username ?: return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@ReceptionistProfileActivity)
                    .deleteAvatar(username)

                if (response.isSuccessful) {
                    sessionManager.avatarPath = null
                    loadAvatarImage()
                    Toast.makeText(this@ReceptionistProfileActivity, "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ReceptionistProfileActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReceptionistProfileActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── BACK PRESS (nút vật lý) ──────────────────────────────────────────────
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    goBackToDashboard()
                }
            }
        )
    }

    // ─── TOOLBAR ──────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        // Nút back trên toolbar
        binding.btnBack.setOnClickListener {
            goBackToDashboard()
        }

        // Nút logout trên toolbar
        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun goBackToDashboard() {
        val intent = Intent(this, ReceptionistDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // ─── LOAD PROFILE DATA ────────────────────────────────────────────────────
    private fun loadProfileData() {
        val session = SessionManager(this)

        binding.tvProfileName.text = session.displayName ?: "—"
        binding.tvStaffId.text     = getString(R.string.label_staff_id_format, session.staffId ?: "—")
        binding.tvShiftBadge.text  = "Ca chiều  •  15:00 – 23:00"
        binding.tvRoleBadge.text   = getString(R.string.role_receptionist)

        setupRow(
            rowBinding = ItemProfileRowBinding.bind(binding.rowFullName.root),
            iconRes    = R.drawable.ic_person,
            label      = "Họ và tên",
            value      = session.displayName ?: "—"
        )
        setupRow(
            rowBinding = ItemProfileRowBinding.bind(binding.rowPhone.root),
            iconRes    = R.drawable.ic_phone,
            label      = "Số điện thoại",
            value      = session.phone ?: "—"
        )
        setupRow(
            rowBinding = ItemProfileRowBinding.bind(binding.rowEmail.root),
            iconRes    = R.drawable.ic_email,
            label      = "Email",
            value      = session.email ?: "—"
        )
        setupRow(
            rowBinding = ItemProfileRowBinding.bind(binding.rowDepartment.root),
            iconRes    = R.drawable.ic_building,
            label      = "Phòng ban",
            value      = session.department ?: "—"
        )
        setupRow(
            rowBinding = ItemProfileRowBinding.bind(binding.rowJoinDate.root),
            iconRes    = R.drawable.ic_calendar,
            label      = "Ngày vào làm",
            value      = session.joinDate ?: "—"
        )
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
}

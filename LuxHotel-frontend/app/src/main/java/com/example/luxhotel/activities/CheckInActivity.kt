package com.example.luxhotel.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.R
import com.example.luxhotel.utils.DatabaseHelper
import com.example.luxhotel.utils.SessionManager
import com.example.luxhotel.viewmodels.CheckInViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CheckInActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private val viewModel: CheckInViewModel by viewModels()
    private val sessionManager by lazy { SessionManager(this) }

    // ── Search
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var cardSearchResult: androidx.cardview.widget.CardView
    private lateinit var tvBookingId: TextView
    private lateinit var tvBookingStatus: TextView
    private lateinit var tvResultGuestName: TextView
    private lateinit var tvResultRoomNumber: TextView
    private lateinit var tvResultCheckIn: TextView
    private lateinit var tvResultCheckOut: TextView

    // ── Guest info
    private lateinit var etGuestName: TextInputEditText
    private lateinit var etGuestId: TextInputEditText
    private lateinit var etGuestPhone: TextInputEditText

    // ── Room info
    private lateinit var tvRoomNumber: TextView
    private lateinit var tvRoomType: TextView
    private lateinit var tvCheckInDate: TextView
    private lateinit var tvCheckOutDate: TextView
    private lateinit var layoutTotalPrice: LinearLayout
    private lateinit var tvNightCount: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var tvExchangeRate: TextView

    // ── Actual check-in time
    private lateinit var layoutActualCheckInDate: LinearLayout
    private lateinit var layoutActualCheckInTime: LinearLayout
    private lateinit var tvActualCheckInDate: TextView
    private lateinit var tvActualCheckInTime: TextView

    // ── Guest counter
    private lateinit var btnGuestMinus: ImageButton
    private lateinit var btnGuestPlus: ImageButton
    private lateinit var tvGuestCount: TextView

    // ── Payment
    private lateinit var tilPaymentMethod: TextInputLayout
    private lateinit var actvPaymentMethod: AutoCompleteTextView
    private lateinit var ivPaymentQR: ImageView
    private lateinit var etDeposit: TextInputEditText

    // ── Note & Buttons
    private lateinit var etNote: TextInputEditText
    private lateinit var btnConfirmCheckIn: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnBack: ImageButton

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkin)

        initViews()
        setupToolbar()
        setupPaymentDropdown()
        setupGuestCounter()
        setupDateTimePickers()
        setupSearch()
        setupButtons()
        observeViewModel()

        if (viewModel.bookingId.value == -1L) {
            // Pre-fill actual check-in with current date/time only on first launch
            tvActualCheckInDate.text = dateFormat.format(calendar.time)
            tvActualCheckInDate.setTextColor(getColor(R.color.white))
            tvActualCheckInTime.text = timeFormat.format(calendar.time)
            tvActualCheckInTime.setTextColor(getColor(R.color.white))
            viewModel.setActualDate(tvActualCheckInDate.text.toString())
            viewModel.setActualTime(tvActualCheckInTime.text.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.bookingId.observe(this) { id ->
            if (id != -1L) {
                cardSearchResult.visibility = View.VISIBLE
                tvBookingId.text = getString(R.string.booking_id_format, id.toString())
            } else {
                cardSearchResult.visibility = View.GONE
            }
        }

        viewModel.roomNumber.observe(this) { num -> tvRoomNumber.text = num.ifEmpty { "—" } }
        viewModel.roomType.observe(this) { type -> tvRoomType.text = type.ifEmpty { "—" } }
        viewModel.checkInDate.observe(this) { date -> tvCheckInDate.text = date.ifEmpty { "—" } }
        viewModel.checkOutDate.observe(this) { date -> tvCheckOutDate.text = date.ifEmpty { "—" } }
        
        viewModel.nights.observe(this) { n -> 
            tvNightCount.text = getString(R.string.night_count_format, n)
        }
        
        viewModel.total.observe(this) { t ->
            if (t > 0) {
                updateTotalPriceDisplay(t)
                layoutTotalPrice.visibility = View.VISIBLE
            } else {
                layoutTotalPrice.visibility = View.GONE
                tvExchangeRate.visibility = View.GONE
            }
        }

        viewModel.guestCount.observe(this) { count ->
            tvGuestCount.text = count.toString()
        }

        viewModel.actualDate.observe(this) { date ->
            if (date.isNotEmpty()) {
                tvActualCheckInDate.text = date
                tvActualCheckInDate.setTextColor(getColor(R.color.white))
            }
        }

        viewModel.actualTime.observe(this) { time ->
            if (time.isNotEmpty()) {
                tvActualCheckInTime.text = time
                tvActualCheckInTime.setTextColor(getColor(R.color.white))
            }
        }
        
        viewModel.searchQuery.observe(this) { query ->
            if (etSearch.text.toString() != query) {
                etSearch.setText(query)
            }
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)

        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        cardSearchResult = findViewById(R.id.cardSearchResult)
        tvBookingId = findViewById(R.id.tvBookingId)
        tvBookingStatus = findViewById(R.id.tvBookingStatus)
        tvResultGuestName = findViewById(R.id.tvResultGuestName)
        tvResultRoomNumber = findViewById(R.id.tvResultRoomNumber)
        tvResultCheckIn = findViewById(R.id.tvResultCheckIn)
        tvResultCheckOut = findViewById(R.id.tvResultCheckOut)

        etGuestName = findViewById(R.id.etGuestName)
        etGuestId = findViewById(R.id.etGuestId)
        etGuestPhone = findViewById(R.id.etGuestPhone)

        tvRoomNumber = findViewById(R.id.tvRoomNumber)
        tvRoomType = findViewById(R.id.tvRoomType)
        tvCheckInDate = findViewById(R.id.tvCheckInDate)
        tvCheckOutDate = findViewById(R.id.tvCheckOutDate)
        layoutTotalPrice = findViewById(R.id.layoutTotalPrice)
        tvNightCount = findViewById(R.id.tvNightCount)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        tvExchangeRate = findViewById(R.id.tvExchangeRate)

        layoutActualCheckInDate = findViewById(R.id.layoutActualCheckInDate)
        layoutActualCheckInTime = findViewById(R.id.layoutActualCheckInTime)
        tvActualCheckInDate = findViewById(R.id.tvActualCheckInDate)
        tvActualCheckInTime = findViewById(R.id.tvActualCheckInTime)

        btnGuestMinus = findViewById(R.id.btnGuestMinus)
        btnGuestPlus = findViewById(R.id.btnGuestPlus)
        tvGuestCount = findViewById(R.id.tvGuestCount)

        tilPaymentMethod = findViewById(R.id.tilPaymentMethod)
        actvPaymentMethod = findViewById(R.id.actvPaymentMethod)
        ivPaymentQR = findViewById(R.id.ivPaymentQR)
        etDeposit = findViewById(R.id.etDeposit)

        etNote = findViewById(R.id.etNote)
        btnConfirmCheckIn = findViewById(R.id.btnConfirmCheckIn)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupToolbar() {
        btnBack.setOnClickListener { finish() }
    }

    private fun setupPaymentDropdown() {
        val methods = listOf("Tiền mặt", "Chuyển khoản", "Thẻ tín dụng / Ghi nợ", "Ví điện tử")
        val adapter = ArrayAdapter(this, R.layout.dropdown_item_dark, methods)
        actvPaymentMethod.setAdapter(adapter)

        actvPaymentMethod.setOnItemClickListener { _, _, position, _ ->
            val selected = methods[position]
            if (selected == "Chuyển khoản") {
                val qrPath = sessionManager.paymentQRPath
                if (qrPath != null) {
                    ivPaymentQR.visibility = View.VISIBLE
                    ivPaymentQR.setImageURI(Uri.parse(qrPath))
                } else {
                    ivPaymentQR.visibility = View.GONE
                    Toast.makeText(this, "Chưa có mã QR thanh toán (Admin cần thiết lập)", Toast.LENGTH_SHORT).show()
                }
            } else {
                ivPaymentQR.visibility = View.GONE
            }
        }
    }

    private fun setupGuestCounter() {
        btnGuestMinus.setOnClickListener {
            val current = viewModel.guestCount.value ?: 1
            if (current > 1) {
                viewModel.setGuestCount(current - 1)
            }
        }
        btnGuestPlus.setOnClickListener {
            val current = viewModel.guestCount.value ?: 1
            if (current < 10) {
                viewModel.setGuestCount(current + 1)
            }
        }
    }

    private fun setupDateTimePickers() {
        layoutActualCheckInDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                R.style.CustomPickerTheme,
                { _, y, m, d ->
                    val cal = Calendar.getInstance()
                    cal.set(y, m, d)
                    val dateStr = dateFormat.format(cal.time)
                    viewModel.setActualDate(dateStr)
                },
                c[Calendar.YEAR],
                c[Calendar.MONTH],
                c[Calendar.DAY_OF_MONTH]
            ).show()
        }

        layoutActualCheckInTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(
                this,
                R.style.CustomPickerTheme,
                { _, h, min ->
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", h, min)
                    viewModel.setActualTime(timeStr)
                },
                c[Calendar.HOUR_OF_DAY],
                c[Calendar.MINUTE],
                true
            ).show()
        }
    }

    private fun setupSearch() {
        btnSearch.setOnClickListener { performSearch() }
        etSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        viewModel.setSearchQuery(query)
        if (query.isEmpty()) {
            showSnackbar("Vui lòng nhập thông tin tìm kiếm")
            return
        }

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@CheckInActivity)
                val response = apiService.getBookingsByStatus(DatabaseHelper.BOOKING_STATUS_BOOKED)
                if (response.isSuccessful) {
                    val bookings = response.body() ?: emptyList()
                    displaySearchResult(bookings, query)
                } else {
                    showSnackbar("Lỗi server: ${response.code()}")
                }
            } catch (e: Exception) {
                showSnackbar("Lỗi kết nối: ${e.message}")
            }
        }
    }

    private fun displaySearchResult(bookings: List<com.example.luxhotel.models.CheckoutBooking>, query: String) {
        val result = bookings.find { it.bookingId == query || it.guestName?.contains(query, ignoreCase = true) == true }

        if (result != null) {
            viewModel.setBookingId(result.bookingId?.toLongOrNull() ?: -1L)
            
            // Fill Search result display fields directly since they are static for the result
            tvResultGuestName.text = result.guestName
            tvResultRoomNumber.text = getString(R.string.room_info_format, result.roomNumber ?: "", result.roomType ?: "")
            tvResultCheckIn.text = result.checkInDate
            tvResultCheckOut.text = result.checkOutDate

            // Fill form info
            etGuestName.setText(result.guestName)
            etGuestPhone.setText(result.guestPhone)

            // Save booking details to view model to persist across rotation
            viewModel.setBookingDetails(
                result.roomNumber ?: "",
                result.roomType ?: "",
                result.checkInDate ?: "",
                result.checkOutDate ?: "",
                result.nights ?: 0,
                result.grandTotal,
                result.guestIdCard ?: ""
            )
        } else {
            viewModel.setBookingId(-1L)
            viewModel.setBookingDetails("", "", "", "", 0, 0L, "")
            showSnackbar("Không tìm thấy đặt phòng phù hợp")
        }
    }

    private fun setupButtons() {
        btnConfirmCheckIn.setOnClickListener { validateAndCheckIn() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun validateAndCheckIn() {
        val id = viewModel.bookingId.value ?: -1L
        if (id == -1L) {
            showSnackbar("Vui lòng tìm và chọn đặt phòng trước")
            return
        }

        val name = etGuestName.text.toString().trim()
        val idCard = etGuestId.text.toString().trim()
        val payment = actvPaymentMethod.text.toString().trim()
        val originalId = viewModel.originalIdCard.value ?: ""

        if (name.isEmpty()) { showSnackbar("Vui lòng nhập họ tên khách"); return }
        
        // Kiểm tra định dạng CCCD (12 số) hoặc CMND (9 số)
        val idRegex = Regex("^[0-9]{9}\$|^[0-9]{12}\$")
        if (idCard.isEmpty()) { 
            showSnackbar("Vui lòng nhập CCCD / Hộ chiếu")
            return 
        } else if (!idRegex.matches(idCard)) {
            showSnackbar("Số CCCD/CMND không hợp lệ (phải có 9 hoặc 12 chữ số)")
            return
        }

        // Kiểm tra khớp với CCCD khi đặt phòng
        if (originalId.isNotEmpty() && idCard != originalId) {
            showSnackbar("Số CCCD không khớp với thông tin khi đặt phòng ($originalId)")
            return
        }

        if (payment.isEmpty()) { showSnackbar("Vui lòng chọn phương thức thanh toán"); return }

        val syncRepository = com.example.luxhotel.network.SyncRepository(this)
        lifecycleScope.launch {
            val result = syncRepository.checkIn(id)
            when (result) {
                is com.example.luxhotel.network.SyncRepository.Result.Success -> {
                    Toast.makeText(this@CheckInActivity, "✅ Check-in thành công!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is com.example.luxhotel.network.SyncRepository.Result.SavedOffline -> {
                    Toast.makeText(this@CheckInActivity, "Đã lưu ngoại tuyến, sẽ đồng bộ khi có mạng", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is com.example.luxhotel.network.SyncRepository.Result.Error -> {
                    showSnackbar("Lỗi: ${result.message}")
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getColor(R.color.gold_primary))
            .setTextColor(getColor(android.R.color.black))
            .show()
    }

    private fun updateTotalPriceDisplay(vndAmount: Long) {
        val vndFmt = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        tvTotalPrice.text = "${vndFmt.format(vndAmount)} ₫"

        lifecycleScope.launch {
            val rate = com.example.luxhotel.utils.NetworkHelper.fetchUsdToVnd()
            if (rate > 0) {
                val usdAmount = vndAmount.toDouble() / rate
                val usdFmt = java.text.NumberFormat.getCurrencyInstance(Locale.US)
                
                tvTotalPrice.text = getString(R.string.total_with_usd, vndFmt.format(vndAmount), usdFmt.format(usdAmount))
                
                tvExchangeRate.visibility = View.VISIBLE
                tvExchangeRate.text = getString(R.string.label_exchange_rate, vndFmt.format(rate))
            } else {
                tvExchangeRate.visibility = View.GONE
            }
        }
    }

    private fun formatCurrency(amount: Long): String {
        val formatted = String.format(Locale.getDefault(), "%,d", amount).replace(',', '.')
        return "$formatted ₫"
    }
}

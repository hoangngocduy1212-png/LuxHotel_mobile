package com.example.luxhotel.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.CheckoutBooking
import com.example.luxhotel.models.CheckoutStatus
import com.example.luxhotel.models.ServiceCharge
import com.example.luxhotel.utils.DatabaseHelper
import com.example.luxhotel.utils.NetworkHelper
import com.example.luxhotel.utils.SessionManager
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CheckoutDetailActivity : AppCompatActivity() {

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var btnBack: ImageButton
    private lateinit var tvRoomNumber: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvGuestName: TextView
    private lateinit var tvCheckinDate: TextView
    private lateinit var tvCheckoutDate: TextView
    private lateinit var tvNights: TextView
    private lateinit var tvRoomType: TextView
    private lateinit var rvServiceCharges: RecyclerView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvExchangeRate: TextView
    private lateinit var spinnerPayment: Spinner
    private lateinit var ivPaymentQR: ImageView
    private lateinit var btnConfirmCheckout: Button
    private lateinit var btnAddService: Button

    private val sessionManager by lazy { SessionManager(this) }

    // ── Data ─────────────────────────────────────────────────────────────────
    private lateinit var booking: CheckoutBooking

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout_detail)

        // Nhận booking từ Intent
        @Suppress("DEPRECATION")
        booking = intent.getSerializableExtra(EXTRA_BOOKING) as? CheckoutBooking
            ?: run {
                Toast.makeText(this, "Không tìm thấy thông tin đặt phòng.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        bindViews()
        populateData()
        setupServiceList()
        setupPaymentSpinner()
        setupListeners()
        loadExchangeRate()
    }

    private fun loadExchangeRate() {
        lifecycleScope.launch {
            val rate = NetworkHelper.fetchUsdToVnd()
            if (rate > 0) {
                tvExchangeRate.visibility = View.VISIBLE
                val vndAmount = booking.grandTotal
                val usdAmount = vndAmount.toDouble() / rate
                
                val vndFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                val usdFmt = NumberFormat.getCurrencyInstance(Locale.US)
                
                // Cập nhật lại TextView hiển thị tổng tiền để kèm theo USD
                tvTotalAmount.text = getString(R.string.total_with_usd, vndFmt.format(vndAmount), usdFmt.format(usdAmount))
                
                // Hiển thị tỷ giá ở dưới
                tvExchangeRate.text = getString(R.string.label_exchange_rate, vndFmt.format(rate))
            }
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun bindViews() {
        btnBack            = findViewById(R.id.btnBack)
        tvRoomNumber       = findViewById(R.id.tvRoomNumber)
        tvStatusBadge      = findViewById(R.id.tvStatusBadge)
        tvGuestName        = findViewById(R.id.tvGuestName)
        tvCheckinDate      = findViewById(R.id.tvCheckinDate)
        tvCheckoutDate     = findViewById(R.id.tvCheckoutDate)
        tvNights           = findViewById(R.id.tvNights)
        tvRoomType         = findViewById(R.id.tvRoomType)
        rvServiceCharges   = findViewById(R.id.rvServiceCharges)
        tvTotalAmount      = findViewById(R.id.tvTotalAmount)
        tvExchangeRate     = findViewById(R.id.tvExchangeRate)
        spinnerPayment     = findViewById(R.id.spinnerPayment)
        ivPaymentQR        = findViewById(R.id.ivPaymentQR)
        btnConfirmCheckout = findViewById(R.id.btnConfirmCheckout)
        btnAddService      = findViewById(R.id.btnAddService)
    }

    /** Đổ dữ liệu booking lên các View. */
    private fun populateData() {
        tvRoomNumber.text  = getString(R.string.room_label_format, booking.roomNumber ?: "N/A")
        tvGuestName.text   = booking.guestName ?: "N/A"
        tvCheckinDate.text = booking.checkInDate ?: "—"
        tvCheckoutDate.text = booking.checkOutDate ?: "—"
        tvNights.text      = booking.computedNights.toString()
        tvRoomType.text    = booking.roomType ?: "N/A"
        tvTotalAmount.text = formatMoney(booking.grandTotal)

        applyStatusBadge()
    }

    /** Màu sắc badge theo trạng thái. */
    private fun applyStatusBadge() {
        when (booking.status ?: CheckoutStatus.DUE_TODAY) {
            CheckoutStatus.DUE_TODAY -> {
                tvStatusBadge.text = getString(R.string.badge_due_today)
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.green_primary))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green)
            }
            CheckoutStatus.OVERDUE -> {
                tvStatusBadge.text = getString(R.string.badge_overdue)
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.red_primary))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red)
            }
            CheckoutStatus.CHECKED_OUT -> {
                tvStatusBadge.text = getString(R.string.badge_checked_out)
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_gray)
                btnConfirmCheckout.isEnabled = false
            }
            else -> {
                tvStatusBadge.text = booking.rawStatus ?: "N/A"
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_gray)
            }
        }
    }

    /** Gắn RecyclerView chi tiết dịch vụ. */
    private fun setupServiceList() {
        // Ghép tiền phòng vào đầu danh sách
        val allLines = mutableListOf<ServiceCharge>().apply {
            add(ServiceCharge(
                name   = "Tiền phòng (${booking.computedNights} đêm × ${formatMoney(booking.roomPricePerNight ?: 0L)})",
                amount = booking.roomTotal
            ))
            addAll(booking.serviceCharges ?: emptyList())
        }

        rvServiceCharges.layoutManager = LinearLayoutManager(this)
        rvServiceCharges.adapter = ServiceChargeAdapter(allLines)
    }

    /** Spinner phương thức thanh toán. */
    private fun setupPaymentSpinner() {
        val methods = listOf(
            getString(R.string.hint_choose_payment_checkout),
            getString(R.string.payment_cash),
            getString(R.string.payment_card),
            getString(R.string.payment_transfer),
            getString(R.string.payment_app)
        )
        // Dùng layout tùy chỉnh dropdown_item_dark để chữ trắng nền tối
        val spinnerAdapter = ArrayAdapter(this, R.layout.dropdown_item_dark, methods)
        spinnerPayment.adapter = spinnerAdapter

        spinnerPayment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = methods[position]
                if (selected == getString(R.string.payment_transfer)) {
                    val qrPath = sessionManager.paymentQRPath
                    if (qrPath != null) {
                        ivPaymentQR.visibility = View.VISIBLE
                        ivPaymentQR.setImageURI(Uri.parse(qrPath))
                    } else {
                        ivPaymentQR.visibility = View.GONE
                        Toast.makeText(this@CheckoutDetailActivity, "Chưa có mã QR thanh toán (Admin cần thiết lập)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    ivPaymentQR.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnConfirmCheckout.setOnClickListener {
            if (!validatePaymentSelected()) return@setOnClickListener
            showConfirmDialog()
        }

        btnAddService.setOnClickListener {
            showAddServiceDialog()
        }
    }

    private fun showAddServiceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_service, null)
        val etName = dialogView.findViewById<EditText>(R.id.etServiceName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etServiceAmount)

        AlertDialog.Builder(this)
            .setTitle("Thêm Phí Dịch Vụ")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val name = etName.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                if (name.isNotEmpty() && amountStr.isNotEmpty()) {
                    val amount = amountStr.toLong()
                    addServiceCharge(name, amount)
                } else {
                    Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun addServiceCharge(name: String, amount: Long) {
        val syncRepository = com.example.luxhotel.network.SyncRepository(this)
        lifecycleScope.launch {
            val bookingId = (booking.bookingId ?: "0").toLong()
            val result = syncRepository.addServiceCharge(bookingId, name, amount)
            when (result) {
                is com.example.luxhotel.network.SyncRepository.Result.Success -> {
                    Toast.makeText(this@CheckoutDetailActivity, "Đã thêm dịch vụ: $name", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    // Thay vì finish(), ta nên load lại data hoặc thông báo người dùng
                    // Nhưng để đơn giản theo logic cũ, ta có thể quay lại list
                    finish() 
                }
                is com.example.luxhotel.network.SyncRepository.Result.SavedOffline -> {
                    Toast.makeText(this@CheckoutDetailActivity, "Đã lưu dịch vụ ngoại tuyến", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is com.example.luxhotel.network.SyncRepository.Result.Error -> {
                    Toast.makeText(this@CheckoutDetailActivity, "Lỗi: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Validation & Confirm ──────────────────────────────────────────────────

    private fun validatePaymentSelected(): Boolean {
        if (spinnerPayment.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                getString(R.string.checkout_error_no_payment),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun showConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_checkout_title))
            .setMessage(
                getString(
                    R.string.dialog_checkout_message,
                    booking.guestName ?: "N/A",
                    booking.roomNumber ?: "N/A"
                )
            )
            .setPositiveButton(getString(R.string.dialog_checkout_positive)) { _, _ ->
                performCheckout()
            }
            .setNegativeButton(getString(R.string.dialog_checkout_negative), null)
            .show()
    }

    /**
     * Thực hiện checkout:
     * 1. Gọi API hoặc lưu offline qua SyncRepository.
     * 2. Trả kết quả về CheckoutListActivity.
     * 3. Đóng màn hình.
     */
    private fun performCheckout() {
        val syncRepository = com.example.luxhotel.network.SyncRepository(this)
        lifecycleScope.launch {
            val result = syncRepository.checkOut((booking.bookingId ?: "0").toLong())
            when (result) {
                is com.example.luxhotel.network.SyncRepository.Result.Success -> {
                    Toast.makeText(
                        this@CheckoutDetailActivity,
                        getString(R.string.checkout_success, booking.roomNumber),
                        Toast.LENGTH_LONG
                    ).show()

                    setResult(RESULT_OK)
                    finish()
                }
                is com.example.luxhotel.network.SyncRepository.Result.SavedOffline -> {
                    Toast.makeText(
                        this@CheckoutDetailActivity,
                        "Đã lưu ngoại tuyến, sẽ đồng bộ khi có mạng",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is com.example.luxhotel.network.SyncRepository.Result.Error -> {
                    Toast.makeText(
                        this@CheckoutDetailActivity,
                        "Lỗi: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatMoney(amount: Long): String {
        val fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return "${fmt.format(amount)} đ"
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val EXTRA_BOOKING    = "extra_checkout_booking"
        const val RESULT_BOOKING_ID = "result_booking_id"
    }

    // ── Inner Adapter: Service charges ────────────────────────────────────────

    /**
     * Adapter nhỏ cho danh sách chi tiết dịch vụ trong hóa đơn.
     */
    private inner class ServiceChargeAdapter(
        private val items: List<ServiceCharge>
    ) : RecyclerView.Adapter<ServiceChargeAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView   = view.findViewById(R.id.tvServiceName)
            val tvAmount: TextView = view.findViewById(R.id.tvServiceAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_service_charge, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvName.text   = item.name
            holder.tvAmount.text = formatMoney(item.amount)
        }

        override fun getItemCount() = items.size
    }
}

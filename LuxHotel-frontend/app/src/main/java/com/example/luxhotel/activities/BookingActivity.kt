package com.example.luxhotel.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.R
import com.example.luxhotel.databinding.ActivityBookingBinding
import com.example.luxhotel.utils.NetworkHelper
import com.example.luxhotel.viewmodels.BookingViewModel
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding
    private val viewModel: BookingViewModel by viewModels()

    private val roomPriceMap = mutableMapOf<String, Long>()
    private val roomsByType = mutableMapOf<String, MutableList<String>>()

    private val displayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val vndFmt     = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackPress()
        setupToolbar()
        setupGuestCounter()
        setupDatePickers()
        setupButtons()
        observeViewModel()
        
        loadDataFromDb()
    }

    private fun observeViewModel() {
        viewModel.guestCount.observe(this) { count ->
            binding.tvGuestCount.text = count.toString()
        }

        viewModel.checkInCal.observe(this) { cal ->
            if (cal != null) {
                binding.tvCheckInDate.text = displayFmt.format(cal.time)
                binding.tvCheckInDate.setTextColor(getColor(android.R.color.white))
            } else {
                binding.tvCheckInDate.text = getString(R.string.choose_date)
                binding.tvCheckInDate.setTextColor(0xFF666666.toInt())
            }
            recalcTotal()
        }

        viewModel.checkOutCal.observe(this) { cal ->
            if (cal != null) {
                binding.tvCheckOutDate.text = displayFmt.format(cal.time)
                binding.tvCheckOutDate.setTextColor(getColor(android.R.color.white))
            } else {
                binding.tvCheckOutDate.text = getString(R.string.choose_date)
                binding.tvCheckOutDate.setTextColor(0xFF666666.toInt())
            }
            recalcTotal()
        }

        viewModel.selectedType.observe(this) { type ->
            if (type.isNotEmpty() && binding.actvRoomType.text.toString() != type) {
                binding.actvRoomType.setText(type, false)
                updateRoomNumberDropdown(type)
            }
        }

        viewModel.selectedRoomNumber.observe(this) { num ->
            if (num.isNotEmpty() && binding.actvRoomNumber.text.toString() != num) {
                binding.actvRoomNumber.setText(num, false)
            }
        }
    }

    private fun loadDataFromDb() {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@BookingActivity)
                val response = apiService.getAvailableRooms()
                if (response.isSuccessful) {
                    val availableRooms = response.body() ?: emptyList()
                    roomsByType.clear()
                    roomPriceMap.clear()

                    if (availableRooms.isEmpty()) {
                        showToast("Hiện không còn phòng trống!")
                        return@launch
                    }

                    for (room in availableRooms) {
                        val type = room.roomType ?: continue
                        val roomNum = room.roomNumber ?: continue
                        val price = room.price ?: 0L

                        if (!roomPriceMap.containsKey(type)) {
                            roomPriceMap[type] = price
                        }
                        val list = roomsByType.getOrPut(type) { mutableListOf() }
                        list.add(roomNum)
                    }
                    setupRoomTypeDropdown()
                }
            } catch (e: Exception) {
                showToast("Lỗi tải dữ liệu phòng: ${e.message}")
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    finish()
                    slideOut()
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun slideOut() =
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRoomTypeDropdown() {
        val types = roomsByType.keys.toList().sorted()
        if (types.isEmpty()) {
            binding.tilRoomType.isEnabled = false
            binding.actvRoomType.hint = "Không có phòng trống"
            return
        }

        binding.tilRoomType.isEnabled = true
        binding.actvRoomType.hint = getString(R.string.error_choose_room_type)

        val adapter = ArrayAdapter(this, R.layout.dropdown_item_dark, types)
        (binding.tilRoomType.editText as? AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            // Đảm bảo có thể click để hiện dropdown ngay cả khi đã có focus
            setOnClickListener { showDropDown() }
            
            setOnItemClickListener { _, _, pos, _ ->
                val type = types[pos]
                viewModel.setSelectedType(type)
                updateRoomNumberDropdown(type)
                recalcTotal()
            }
        }
    }

    private fun updateRoomNumberDropdown(type: String) {
        val rooms = roomsByType[type] ?: emptyList()
        if (rooms.isEmpty()) {
            binding.tilRoomNumber.isEnabled = false
            return
        }
        
        binding.tilRoomNumber.isEnabled = true
        val adapter = ArrayAdapter(this, R.layout.dropdown_item_dark, rooms)
        (binding.tilRoomNumber.editText as? AutoCompleteTextView)?.apply {
            if (viewModel.selectedRoomNumber.value !in rooms) {
                setText("", false)
                viewModel.setSelectedRoomNumber("")
            }
            setAdapter(adapter)
            setOnClickListener { showDropDown() }

            setOnItemClickListener { _, _, pos, _ ->
                viewModel.setSelectedRoomNumber(rooms[pos])
            }
        }
    }

    private fun setupGuestCounter() {
        binding.btnGuestMinus.setOnClickListener {
            val current = viewModel.guestCount.value ?: 1
            if (current > 1) {
                viewModel.setGuestCount(current - 1)
            }
        }
        binding.btnGuestPlus.setOnClickListener {
            val current = viewModel.guestCount.value ?: 1
            if (current < 10) {
                viewModel.setGuestCount(current + 1)
            }
        }
    }

    private fun setupDatePickers() {
        binding.layoutCheckIn.setOnClickListener  { showCheckInPicker() }
        binding.layoutCheckOut.setOnClickListener { showCheckOutPicker() }
    }

    private fun showCheckInPicker() {
        val now = Calendar.getInstance()
        val current = viewModel.checkInCal.value ?: now
        DatePickerDialog(
            this,
            R.style.CustomPickerTheme,
            { _, y, m, d ->
                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                viewModel.setCheckInCal(newCal)
                
                val checkOut = viewModel.checkOutCal.value
                if (checkOut != null && (checkOut <= newCal)) {
                    viewModel.setCheckOutCal(null)
                }
            },
            current[Calendar.YEAR], current[Calendar.MONTH], current[Calendar.DAY_OF_MONTH]
        ).apply { datePicker.minDate = now.timeInMillis; show() }
    }

    private fun showCheckOutPicker() {
        val minCal = (viewModel.checkInCal.value?.clone() as? Calendar ?: Calendar.getInstance()).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val current = viewModel.checkOutCal.value ?: minCal
        DatePickerDialog(
            this,
            R.style.CustomPickerTheme,
            { _, y, m, d ->
                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                viewModel.setCheckOutCal(newCal)
            },
            current[Calendar.YEAR], current[Calendar.MONTH], current[Calendar.DAY_OF_MONTH]
        ).apply { datePicker.minDate = minCal.timeInMillis; show() }
    }

    private fun recalcTotal() {
        val ci = viewModel.checkInCal.value ?: return
        val co = viewModel.checkOutCal.value ?: return
        val type = viewModel.selectedType.value ?: ""
        
        val nights = ((co.timeInMillis - ci.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        if (nights <= 0) return
        val total = nights * (roomPriceMap[type] ?: 0L)
        binding.layoutNightSummary.visibility = android.view.View.VISIBLE
        binding.tvNightCount.text = getString(R.string.night_count_format, nights)
        binding.tvTotalPrice.text = getString(R.string.price_format, vndFmt.format(total))
    }

    private fun setupButtons() {
        binding.btnConfirmBooking.setOnClickListener { submitBooking() }
        binding.btnCancel.setOnClickListener { finish(); slideOut() }
    }

    private fun submitBooking() {
        val name     = binding.etGuestName.text?.toString()?.trim() ?: ""
        val idNumber = binding.etGuestId.text?.toString()?.trim()   ?: ""
        val phone    = binding.etGuestPhone.text?.toString()?.trim() ?: ""
        val roomNum  = binding.actvRoomNumber.text.toString().trim()
        val type     = viewModel.selectedType.value ?: ""
        val checkIn  = viewModel.checkInCal.value
        val checkOut = viewModel.checkOutCal.value

        when {
            name.isEmpty()     -> { binding.etGuestName.error = getString(R.string.error_enter_name); binding.etGuestName.requestFocus(); return }
            idNumber.isEmpty() -> { binding.etGuestId.error = getString(R.string.error_enter_id); binding.etGuestId.requestFocus(); return }
            phone.isEmpty()    -> { binding.etGuestPhone.error = getString(R.string.hint_enter_phone); binding.etGuestPhone.requestFocus(); return }
            type.isEmpty()     -> { showToast(getString(R.string.error_choose_room_type)); return }
            roomNum.isEmpty()  -> { showToast(getString(R.string.error_choose_room_number)); return }
            checkIn == null    -> { showToast(getString(R.string.error_choose_checkin)); return }
            checkOut == null   -> { showToast(getString(R.string.error_choose_checkout)); return }
        }

        lifecycleScope.launch {
            try {
                val syncRepository = com.example.luxhotel.network.SyncRepository(this@BookingActivity)

                // 1. Tìm hoặc thêm khách hàng
                var customerId: Long
                val apiService = RetrofitClient.getApiService(this@BookingActivity)
                
                android.util.Log.d("BookingDebug", "Searching customer by phone: $phone")
                val searchResponse = apiService.findCustomerByPhone(phone)
                
                if (searchResponse.isSuccessful && !searchResponse.body().isNullOrEmpty()) {
                    val customer = searchResponse.body()!![0]
                    android.util.Log.d("BookingDebug", "Found customer: ${customer.id}")
                    customerId = customer.id?.toLongOrNull() ?: -1L
                } else {
                    android.util.Log.d("BookingDebug", "Customer not found or search failed. Adding new customer.")
                    val addCustResponse = apiService.addCustomer(mapOf(
                        "fullName" to name,
                        "phone" to phone,
                        "idCard" to idNumber,
                        "email" to "",
                        "address" to ""
                    ))
                    if (addCustResponse.isSuccessful) {
                        customerId = addCustResponse.body()?.customerId ?: -1
                        android.util.Log.d("BookingDebug", "Added customer. ID: $customerId")
                    } else {
                        android.util.Log.e("BookingDebug", "Failed to add customer. Code: ${addCustResponse.code()}")
                        showToast("Không thể thêm thông tin khách hàng lên hệ thống")
                        return@launch
                    }
                }

                if (customerId != -1L) {
                    val dbDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val bookingBody = mutableMapOf<String, Any?>()
                    try {
                        bookingBody["roomNumber"] = roomNum
                        bookingBody["customerId"] = customerId
                        bookingBody["checkinDate"] = dbDateFmt.format(checkIn!!.time)
                        bookingBody["checkoutDate"] = dbDateFmt.format(checkOut!!.time)
                        bookingBody["pricePerNight"] = (roomPriceMap[type] ?: 0L)
                        bookingBody["totalGuests"] = viewModel.guestCount.value ?: 1
                        bookingBody["checkInNow"] = false
                        bookingBody["note"] = binding.etNote.text?.toString() ?: ""
                    } catch (e: Exception) {
                        android.util.Log.e("BookingDebug", "Error preparing booking body", e)
                        throw Exception("Lỗi xử lý dữ liệu ngày tháng: ${e.message}")
                    }
                    
                    android.util.Log.d("BookingDebug", "Creating booking: $bookingBody")
                    when (val result = syncRepository.createBooking(bookingBody)) {
                        is com.example.luxhotel.network.SyncRepository.Result.Success -> {
                            showSuccessDialog(name, roomNum)
                            NetworkHelper.postBookingConfirmation(name, roomNum)
                        }
                        is com.example.luxhotel.network.SyncRepository.Result.SavedOffline -> {
                            Toast.makeText(this@BookingActivity, "Đã lưu đơn đặt phòng ngoại tuyến", Toast.LENGTH_SHORT).show()
                            showSuccessDialog(name, roomNum)
                        }
                        is com.example.luxhotel.network.SyncRepository.Result.Error -> {
                            showToast("Lỗi: ${result.message}")
                        }
                    }
                } else {
                    showToast("Lỗi khi lưu thông tin khách hàng (ID không hợp lệ)")
                }
            } catch (e: Exception) {
                android.util.Log.e("BookingDebug", "Booking process exception", e)
                showToast("Lỗi kết nối: ${e.message}")
            }
        }
    }

    private fun showSuccessDialog(guestName: String, roomNumber: String) {
        val checkIn = viewModel.checkInCal.value ?: Calendar.getInstance()
        val checkOut = viewModel.checkOutCal.value ?: Calendar.getInstance()
        val type = viewModel.selectedType.value ?: ""

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
            .setTitle(getString(R.string.booking_success_title))
            .setMessage(getString(R.string.booking_success_message, 
                guestName, roomNumber, type, 
                displayFmt.format(checkIn.time), 
                displayFmt.format(checkOut.time),
                binding.tvNightCount.text,
                binding.tvTotalPrice.text))
            .setPositiveButton(getString(R.string.done)) { _, _ ->
                setResult(RESULT_OK)
                finish()
                slideOut()
            }
            .setCancelable(false)
            .show()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
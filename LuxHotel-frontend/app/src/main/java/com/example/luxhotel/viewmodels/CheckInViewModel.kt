package com.example.luxhotel.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.util.Calendar

class CheckInViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_BOOKING_ID = "booking_id"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_GUEST_NAME = "guest_name"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_GUEST_PHONE = "guest_phone"
        private const val KEY_GUEST_COUNT = "guest_count"
        private const val KEY_PAYMENT_METHOD = "payment_method"
        private const val KEY_DEPOSIT = "deposit"
        private const val KEY_NOTE = "note"
        private const val KEY_ACTUAL_DATE = "actual_date"
        private const val KEY_ACTUAL_TIME = "actual_time"
        
        // For booking details display
        private const val KEY_ROOM_NUM = "room_num"
        private const val KEY_ROOM_TYPE = "room_type"
        private const val KEY_CI_DATE = "ci_date"
        private const val KEY_CO_DATE = "co_date"
        private const val KEY_NIGHTS = "nights"
        private const val KEY_TOTAL = "total"
        private const val KEY_ORIGINAL_ID_CARD = "original_id_card"
    }

    val bookingId: LiveData<Long> = savedStateHandle.getLiveData(KEY_BOOKING_ID, -1L)
    fun setBookingId(id: Long) { savedStateHandle[KEY_BOOKING_ID] = id }

    val searchQuery: LiveData<String> = savedStateHandle.getLiveData(KEY_SEARCH_QUERY, "")
    fun setSearchQuery(query: String) { savedStateHandle[KEY_SEARCH_QUERY] = query }

    val guestName: LiveData<String> = savedStateHandle.getLiveData(KEY_GUEST_NAME, "")
    fun setGuestName(name: String) { savedStateHandle[KEY_GUEST_NAME] = name }

    val guestId: LiveData<String> = savedStateHandle.getLiveData(KEY_GUEST_ID, "")
    fun setGuestId(id: String) { savedStateHandle[KEY_GUEST_ID] = id }

    val guestPhone: LiveData<String> = savedStateHandle.getLiveData(KEY_GUEST_PHONE, "")
    fun setGuestPhone(phone: String) { savedStateHandle[KEY_GUEST_PHONE] = phone }

    val guestCount: LiveData<Int> = savedStateHandle.getLiveData(KEY_GUEST_COUNT, 1)
    fun setGuestCount(count: Int) { savedStateHandle[KEY_GUEST_COUNT] = count }

    val paymentMethod: LiveData<String> = savedStateHandle.getLiveData(KEY_PAYMENT_METHOD, "")
    fun setPaymentMethod(method: String) { savedStateHandle[KEY_PAYMENT_METHOD] = method }

    val deposit: LiveData<String> = savedStateHandle.getLiveData(KEY_DEPOSIT, "")
    fun setDeposit(amount: String) { savedStateHandle[KEY_DEPOSIT] = amount }

    val note: LiveData<String> = savedStateHandle.getLiveData(KEY_NOTE, "")
    fun setNote(text: String) { savedStateHandle[KEY_NOTE] = text }

    val actualDate: LiveData<String> = savedStateHandle.getLiveData(KEY_ACTUAL_DATE, "")
    fun setActualDate(date: String) { savedStateHandle[KEY_ACTUAL_DATE] = date }

    val actualTime: LiveData<String> = savedStateHandle.getLiveData(KEY_ACTUAL_TIME, "")
    fun setActualTime(time: String) { savedStateHandle[KEY_ACTUAL_TIME] = time }

    // Booking detail display state
    val roomNumber: LiveData<String> = savedStateHandle.getLiveData(KEY_ROOM_NUM, "")
    val roomType: LiveData<String> = savedStateHandle.getLiveData(KEY_ROOM_TYPE, "")
    val checkInDate: LiveData<String> = savedStateHandle.getLiveData(KEY_CI_DATE, "")
    val checkOutDate: LiveData<String> = savedStateHandle.getLiveData(KEY_CO_DATE, "")
    val nights: LiveData<Int> = savedStateHandle.getLiveData(KEY_NIGHTS, 0)
    val total: LiveData<Long> = savedStateHandle.getLiveData(KEY_TOTAL, 0L)
    val originalIdCard: LiveData<String> = savedStateHandle.getLiveData(KEY_ORIGINAL_ID_CARD, "")

    fun setBookingDetails(roomNum: String, type: String, ci: String, co: String, n: Int, t: Long, originalId: String = "") {
        savedStateHandle[KEY_ROOM_NUM] = roomNum
        savedStateHandle[KEY_ROOM_TYPE] = type
        savedStateHandle[KEY_CI_DATE] = ci
        savedStateHandle[KEY_CO_DATE] = co
        savedStateHandle[KEY_NIGHTS] = n
        savedStateHandle[KEY_TOTAL] = t
        savedStateHandle[KEY_ORIGINAL_ID_CARD] = originalId
    }
}

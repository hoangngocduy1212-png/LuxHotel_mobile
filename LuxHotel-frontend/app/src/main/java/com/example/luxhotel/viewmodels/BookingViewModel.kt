package com.example.luxhotel.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.util.Calendar

class BookingViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_GUEST_COUNT = "guest_count"
        private const val KEY_CHECK_IN = "check_in"
        private const val KEY_CHECK_OUT = "check_out"
        private const val KEY_ROOM_TYPE = "room_type"
        private const val KEY_ROOM_NUMBER = "room_number"
        private const val KEY_GUEST_NAME = "guest_name"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_GUEST_PHONE = "guest_phone"
        private const val KEY_NOTE = "note"
    }

    // Guest Count
    val guestCount: LiveData<Int> = savedStateHandle.getLiveData(KEY_GUEST_COUNT, 1)
    fun setGuestCount(count: Int) {
        savedStateHandle[KEY_GUEST_COUNT] = count
    }

    // Dates
    val checkInCal: LiveData<Calendar?> = savedStateHandle.getLiveData(KEY_CHECK_IN)
    fun setCheckInCal(cal: Calendar?) {
        savedStateHandle[KEY_CHECK_IN] = cal
    }

    val checkOutCal: LiveData<Calendar?> = savedStateHandle.getLiveData(KEY_CHECK_OUT)
    fun setCheckOutCal(cal: Calendar?) {
        savedStateHandle[KEY_CHECK_OUT] = cal
    }

    // Room Info
    val selectedType: LiveData<String> = savedStateHandle.getLiveData(KEY_ROOM_TYPE, "")
    fun setSelectedType(type: String) {
        savedStateHandle[KEY_ROOM_TYPE] = type
    }

    val selectedRoomNumber: LiveData<String> = savedStateHandle.getLiveData(KEY_ROOM_NUMBER, "")
    fun setSelectedRoomNumber(num: String) {
        savedStateHandle[KEY_ROOM_NUMBER] = num
    }

    // Guest Details (Backup for process death)
    fun saveGuestDetails(name: String, id: String, phone: String, note: String) {
        savedStateHandle[KEY_GUEST_NAME] = name
        savedStateHandle[KEY_GUEST_ID] = id
        savedStateHandle[KEY_GUEST_PHONE] = phone
        savedStateHandle[KEY_NOTE] = note
    }

    val guestName: LiveData<String> = savedStateHandle.getLiveData(KEY_GUEST_NAME, "")
    val guestId: LiveData<String> = savedStateHandle.getLiveData(KEY_GUEST_ID, "")
    val guestPhone: LiveData<String> = savedStateHandle.getLiveData(KEY_GUEST_PHONE, "")
    val note: LiveData<String> = savedStateHandle.getLiveData(KEY_NOTE, "")
}

package com.example.luxhotel.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.luxhotel.models.CheckoutBooking

class CheckoutViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
    }

    val searchQuery: LiveData<String> = savedStateHandle.getLiveData(KEY_SEARCH_QUERY, "")

    fun setSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    private var _allBookings = mutableListOf<CheckoutBooking>()
    val allBookings: List<CheckoutBooking> get() = _allBookings

    fun setBookings(bookings: List<CheckoutBooking>) {
        _allBookings = bookings.toMutableList()
    }

    fun getFilteredBookings(): List<CheckoutBooking> {
        val query = searchQuery.value ?: ""
        if (query.isEmpty()) return _allBookings

        val q = query.trim().lowercase()
        return _allBookings.filter { booking ->
            booking.roomNumber.orEmpty().lowercase().contains(q) ||
            booking.guestName.orEmpty().lowercase().contains(q)
        }
    }
}

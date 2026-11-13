package com.example.luxhotel.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.luxhotel.models.Customer
import com.example.luxhotel.utils.DatabaseHelper

class CustomerViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_CURRENT_PAGE = "current_page"
        const val PAGE_SIZE = 10
    }

    val searchQuery: LiveData<String> = savedStateHandle.getLiveData(KEY_SEARCH_QUERY, "")
    val currentPage: LiveData<Int> = savedStateHandle.getLiveData(KEY_CURRENT_PAGE, 0)

    fun setSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        savedStateHandle[KEY_CURRENT_PAGE] = 0 // Reset to first page on search
    }

    fun setCurrentPage(page: Int) {
        savedStateHandle[KEY_CURRENT_PAGE] = page
    }

    private var _allCustomers = mutableListOf<Customer>()
    val allCustomers: List<Customer> get() = _allCustomers

    fun setCustomers(customers: List<Customer>) {
        _allCustomers = customers.toMutableList()
    }

    fun getFilteredCustomers(): List<Customer> {
        val query = searchQuery.value ?: ""
        val list = if (query.isEmpty()) {
            _allCustomers
        } else {
            _allCustomers.filter {
                it.name.orEmpty().contains(query, ignoreCase = true) || 
                it.roomNumber.orEmpty().contains(query, ignoreCase = true)
            }
        }

        // Sắp xếp theo thứ tự ưu tiên: STAYING (Đang ở) -> ARRIVING (Sắp đến) -> LEFT (Đã đi)
        return list.sortedWith(compareBy<Customer> {
            when (it.status) {
                com.example.luxhotel.models.CustomerStatus.STAYING -> 0
                com.example.luxhotel.models.CustomerStatus.ARRIVING -> 1
                com.example.luxhotel.models.CustomerStatus.LEFT -> 2
                null -> 3
            }
        }.thenBy { it.name.orEmpty() })
    }

    fun getPagedCustomers(): List<Customer> {
        val filtered = getFilteredCustomers()
        val page = currentPage.value ?: 0
        val start = page * PAGE_SIZE
        if (start >= filtered.size) return emptyList()
        val end = minOf(start + PAGE_SIZE, filtered.size)
        return filtered.subList(start, end)
    }

    fun getTotalPages(): Int {
        val filteredSize = getFilteredCustomers().size
        return if (filteredSize == 0) 1 else kotlin.math.ceil(filteredSize.toDouble() / PAGE_SIZE).toInt()
    }
}

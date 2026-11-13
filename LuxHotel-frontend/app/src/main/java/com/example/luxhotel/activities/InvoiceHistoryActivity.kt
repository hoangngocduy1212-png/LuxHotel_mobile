package com.example.luxhotel.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.adapters.InvoiceAdapter
import com.example.luxhotel.models.CheckoutBooking
import com.example.luxhotel.utils.DatabaseHelper
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch

class InvoiceHistoryActivity : AppCompatActivity() {

    private lateinit var invoiceAdapter: InvoiceAdapter
    private var allInvoices: List<CheckoutBooking> = emptyList()

    private lateinit var rvInvoiceList: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_history)

        initViews()
        setupRecyclerView()
        loadData()
        setupSearch()
    }

    private fun initViews() {
        rvInvoiceList = findViewById(R.id.rvInvoiceList)
        etSearch = findViewById(R.id.etSearch)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        invoiceAdapter = InvoiceAdapter { _ ->
            // Khi click vào hóa đơn cũ, có thể mở lại màn hình chi tiết hoặc in ấn
            // Hiện tại chúng ta chỉ hiển thị, có thể mở CheckoutDetailActivity ở chế độ chỉ xem
        }
        rvInvoiceList.layoutManager = LinearLayoutManager(this)
        rvInvoiceList.adapter = invoiceAdapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@InvoiceHistoryActivity)
                    .getBookingsByStatus(DatabaseHelper.BOOKING_STATUS_CHECKED_OUT)
                if (response.isSuccessful) {
                    allInvoices = response.body() ?: emptyList()
                    updateList(allInvoices)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filter(query: String) {
        val filtered = if (query.isEmpty()) {
            allInvoices
        } else {
            allInvoices.filter { 
                it.guestName?.contains(query, ignoreCase = true) == true || 
                it.roomNumber?.contains(query, ignoreCase = true) == true ||
                it.bookingId?.contains(query, ignoreCase = true) == true
            }
        }
        updateList(filtered)
    }

    private fun updateList(list: List<CheckoutBooking>) {
        invoiceAdapter.submitList(list)
        layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
}

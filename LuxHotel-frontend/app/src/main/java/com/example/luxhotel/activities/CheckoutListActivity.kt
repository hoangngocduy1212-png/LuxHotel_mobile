package com.example.luxhotel.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luxhotel.R
import com.example.luxhotel.adapters.CheckoutAdapter
import com.example.luxhotel.models.CheckoutBooking
import com.example.luxhotel.models.ServiceCharge
import com.example.luxhotel.utils.DatabaseHelper
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import com.example.luxhotel.viewmodels.CheckoutViewModel

class CheckoutListActivity : AppCompatActivity() {

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var btnBack: ImageButton
    private lateinit var etSearch: EditText
    private lateinit var rvCheckoutList: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val viewModel: CheckoutViewModel by viewModels()

    // ── Adapter & data ───────────────────────────────────────────────────────
    private lateinit var adapter: CheckoutAdapter

    // ── Activity Result API ──────────────────────────────────────────────────
    private val checkoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadData()
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout_list)

        bindViews()
        setupRecyclerView()
        observeViewModel()
        setupSearch()
        setupListeners()

        if (viewModel.allBookings.isEmpty()) {
            loadData()
        }
    }

    private fun observeViewModel() {
        viewModel.searchQuery.observe(this) { query ->
            if (etSearch.text.toString() != query) {
                etSearch.setText(query)
                etSearch.setSelection(query.length)
            }
            submitFilteredList()
        }
    }

    private fun loadData() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@CheckoutListActivity)
                val response = apiService.getBookingsByStatus(DatabaseHelper.BOOKING_STATUS_CHECKED_IN)
                if (response.isSuccessful) {
                    val bookings = response.body() ?: emptyList()
                    viewModel.setBookings(bookings)
                    submitFilteredList()
                } else {
                    showEmptyState("Lỗi tải dữ liệu từ server (${response.code()})")
                }
            } catch (e: Exception) {
                showEmptyState("Lỗi kết nối: ${e.message}")
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showEmptyState(message: String) {
        viewModel.setBookings(emptyList())
        submitFilteredList()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun bindViews() {
        btnBack          = findViewById(R.id.btnBack)
        etSearch         = findViewById(R.id.etSearch)
        rvCheckoutList   = findViewById(R.id.rvCheckoutList)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupRecyclerView() {
        adapter = CheckoutAdapter(
            onCardClick = { booking -> openDetail(booking) },
        ) { booking -> openDetail(booking) }
        rvCheckoutList.layoutManager = LinearLayoutManager(this)
        rvCheckoutList.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.setSearchQuery(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) = Unit
            }
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
        // Custom color for swipe refresh
        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    /**
     * Lọc danh sách theo từ khoá tìm kiếm (số phòng hoặc tên khách).
     */
    private fun submitFilteredList() {
        val filtered = viewModel.getFilteredBookings()
        adapter.submitList(filtered)

        // Hiển thị empty state nếu không có kết quả
        val isEmpty = filtered.isEmpty()
        rvCheckoutList.visibility   = if (isEmpty) View.GONE else View.VISIBLE
        layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    /**
     * Mở màn hình chi tiết check-out, truyền toàn bộ đối tượng booking.
     */
    private fun openDetail(booking: CheckoutBooking) {
        val intent = Intent(this, CheckoutDetailActivity::class.java).apply {
            putExtra(CheckoutDetailActivity.EXTRA_BOOKING, booking)
        }
        checkoutLauncher.launch(intent)
    }
}

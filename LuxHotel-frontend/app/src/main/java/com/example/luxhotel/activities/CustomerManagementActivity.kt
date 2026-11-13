package com.example.luxhotel.activities

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luxhotel.R
import com.example.luxhotel.adapters.CustomerAdapter
import com.example.luxhotel.models.Customer
import com.example.luxhotel.models.CustomerStatus
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import com.example.luxhotel.viewmodels.CustomerViewModel
import com.google.android.material.button.MaterialButton

class CustomerManagementActivity : AppCompatActivity() {

    private lateinit var rvCustomers: RecyclerView
    private lateinit var customerAdapter: CustomerAdapter
    private lateinit var etSearchCustomer: EditText
    private lateinit var tvTotalCustomers: TextView
    private lateinit var tvCurrentlyStaying: TextView
    private lateinit var tvPageInfo: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnAddCustomer: ImageButton
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val viewModel: CustomerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_management)

        initViews()
        setupRecyclerView()
        observeViewModel()
        loadData()
        setupListeners()
    }

    private fun observeViewModel() {
        viewModel.searchQuery.observe(this) { query ->
            if (etSearchCustomer.text.toString() != query) {
                etSearchCustomer.setText(query)
                etSearchCustomer.setSelection(query.length)
            }
            updateFilteredList()
        }
        
        viewModel.currentPage.observe(this) {
            updateFilteredList()
        }
    }

    private fun updateFilteredList() {
        val paged = viewModel.getPagedCustomers()
        customerAdapter.updateData(paged)
        
        val currentPage = (viewModel.currentPage.value ?: 0) + 1
        val totalPages = viewModel.getTotalPages()
        tvPageInfo.text = "Trang $currentPage/$totalPages"
        
        btnPrev.isEnabled = currentPage > 1
        btnNext.isEnabled = currentPage < totalPages
        
        if (paged.isEmpty() && viewModel.allCustomers.isNotEmpty()) {
            layoutEmpty.visibility = if (viewModel.getFilteredCustomers().isEmpty()) View.VISIBLE else View.GONE
            rvCustomers.visibility = if (viewModel.getFilteredCustomers().isEmpty()) View.GONE else View.VISIBLE
        } else if (viewModel.allCustomers.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvCustomers.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvCustomers.visibility = View.VISIBLE
        }
    }

    private fun initViews() {
        rvCustomers = findViewById(R.id.rvCustomers)
        etSearchCustomer = findViewById(R.id.etSearchCustomer)
        tvTotalCustomers = findViewById(R.id.tvTotalCustomers)
        tvCurrentlyStaying = findViewById(R.id.tvCurrentlyStaying)
        tvPageInfo = findViewById(R.id.tvPageInfo)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        btnBack = findViewById(R.id.btnBack)
        btnAddCustomer = findViewById(R.id.btnAddCustomer)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter(mutableListOf()) { customer ->
            Toast.makeText(this, "Chọn khách hàng: ${customer.name}", Toast.LENGTH_SHORT).show()
        }
        rvCustomers.layoutManager = LinearLayoutManager(this)
        rvCustomers.adapter = customerAdapter
    }

    private fun loadData() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@CustomerManagementActivity)
                val response = apiService.getAllCustomersWithStatus()
                if (response.isSuccessful) {
                    val customers = response.body() ?: emptyList()
                    viewModel.setCustomers(customers)
                    
                    tvTotalCustomers.text = customers.size.toString()
                    tvCurrentlyStaying.text = customers.count { it.status == CustomerStatus.STAYING }.toString()
                    
                    updateFilteredList()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CustomerManagementActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAddCustomer.setOnClickListener {
            Toast.makeText(this, "Chức năng thêm khách hàng mới", Toast.LENGTH_SHORT).show()
        }

        btnPrev.setOnClickListener {
            val current = viewModel.currentPage.value ?: 0
            if (current > 0) viewModel.setCurrentPage(current - 1)
        }

        btnNext.setOnClickListener {
            val current = viewModel.currentPage.value ?: 0
            if (current < viewModel.getTotalPages() - 1) viewModel.setCurrentPage(current + 1)
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
        swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }

        etSearchCustomer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.setSearchQuery(query)
                // Persist query to SharedPreferences
                getSharedPreferences("customer_prefs", MODE_PRIVATE).edit {
                    putString("last_search", query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}

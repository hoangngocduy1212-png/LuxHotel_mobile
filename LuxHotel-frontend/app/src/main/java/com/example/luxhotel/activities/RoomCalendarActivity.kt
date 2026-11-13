package com.example.luxhotel.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.adapters.CalendarDayAdapter
import com.example.luxhotel.adapters.RoomDetailAdapter
import com.example.luxhotel.models.CalendarDay
import com.example.luxhotel.models.RoomInfo
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random

class RoomCalendarActivity : AppCompatActivity(),
    CalendarDayAdapter.OnDayClickListener {

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvMonthYear: TextView
    private lateinit var btnToday: TextView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var cardDateDetail: CardView
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnCloseDetail: ImageButton
    private lateinit var rvRoomsForDate: RecyclerView

    // State
    private lateinit var currentMonth: Calendar
    private lateinit var calendarAdapter: CalendarDayAdapter
    private lateinit var roomDetailAdapter: RoomDetailAdapter

    private val fmtMonthYear = SimpleDateFormat("MMMM, yyyy", Locale.forLanguageTag("vi-VN"))
    private val fmtDetailHeader = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_calendar)

        initViews()
        initCalendar()
        setupRecyclerViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnToday = findViewById(R.id.btnToday)
        rvCalendar = findViewById(R.id.rvCalendar)
        cardDateDetail = findViewById(R.id.cardDateDetail)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnCloseDetail = findViewById(R.id.btnCloseDetail)
        rvRoomsForDate = findViewById(R.id.rvRoomsForDate)
    }

    private fun initCalendar() {
        currentMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun setupRecyclerViews() {
        // 7-column grid for calendar
        calendarAdapter = CalendarDayAdapter(this)
        rvCalendar.layoutManager = GridLayoutManager(this, 7)
        rvCalendar.adapter = calendarAdapter

        // Linear list for details
        roomDetailAdapter = RoomDetailAdapter()
        rvRoomsForDate.layoutManager = LinearLayoutManager(this)
        rvRoomsForDate.adapter = roomDetailAdapter

        renderMonth()
    }

    private fun setupListeners() {
        // Back to dashboard
        btnBack.setOnClickListener { finish() }

        // Navigation
        btnPrevMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            renderMonth()
            hideDetailPanel()
        }
        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            renderMonth()
            hideDetailPanel()
        }

        // Jump to today
        btnToday.setOnClickListener {
            currentMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            renderMonth()
            hideDetailPanel()
        }

        // Close detail panel
        btnCloseDetail.setOnClickListener { hideDetailPanel() }
    }

    private fun renderMonth() {
        tvMonthYear.text = fmtMonthYear.format(currentMonth.time)
        lifecycleScope.launch {
            calendarAdapter.submitList(buildDayList())
        }
    }

    /**
     * Build the list of calendar days for the current month.
     */
    private suspend fun buildDayList(): List<CalendarDay> {
        val list = mutableListOf<CalendarDay>()
        val today = Calendar.getInstance()
        val cal = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Empty cells at the start
        repeat(firstDow) { list.add(CalendarDay.empty()) }

        // Actual days
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val apiService = RetrofitClient.getApiService(this@RoomCalendarActivity)
        
        for (d in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            val isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

            val dateStr = sdf.format(cal.time)
            
            var available = 0
            var occupied = 0
            var maintenance = 0
            
            try {
                val response = apiService.getRoomCalendar(dateStr)
                if (response.isSuccessful) {
                    val body = response.body()
                    available = body?.available ?: 0
                    occupied = body?.occupied ?: 0
                    maintenance = body?.maintenance ?: 0
                }
            } catch (e: Exception) {}

            list.add(
                CalendarDay(
                    calendar = cal.clone() as Calendar,
                    dayNumber = d,
                    isToday = isToday,
                    isSelected = false,
                    availableCount = available,
                    occupiedCount = occupied,
                    maintenanceCount = maintenance
                )
            )
        }

        // Empty cells at the end
        val remainder = list.size % 7
        if (remainder != 0) repeat(7 - remainder) { list.add(CalendarDay.empty()) }

        return list
    }

    override fun onDayClick(day: CalendarDay) {
        if (day.isEmpty) return
        day.calendar?.let {
            calendarAdapter.setSelectedDate(it)
            tvSelectedDate.text = fmtDetailHeader.format(it.time)
            
            lifecycleScope.launch {
                roomDetailAdapter.submitList(buildRoomsForDay(day))
                cardDateDetail.visibility = View.VISIBLE
            }
        }
    }

    private fun hideDetailPanel() {
        cardDateDetail.visibility = View.GONE
        calendarAdapter.clearSelection()
    }

    /**
     * Build sample room data for a specific day.
     */
    private suspend fun buildRoomsForDay(day: CalendarDay): List<RoomInfo> {
        val cal = day.calendar ?: return emptyList()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(cal.time)
        
        return try {
            val response = RetrofitClient.getApiService(this@RoomCalendarActivity).getRoomCalendar(dateStr)
            if (response.isSuccessful) {
                response.body()?.rooms ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

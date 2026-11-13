package com.example.luxhotel.activities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luxhotel.R
import com.example.luxhotel.adapters.RoomAdapter
import com.example.luxhotel.adapters.RoomGroup
import com.example.luxhotel.models.RoomTier
import com.example.luxhotel.models.UserRole
import com.example.luxhotel.utils.DatabaseHelper
import com.example.luxhotel.utils.SessionManager
import androidx.lifecycle.lifecycleScope
import com.example.luxhotel.network.RetrofitClient
import kotlinx.coroutines.launch

class RoomListActivity : AppCompatActivity() {

    private lateinit var rvRooms: RecyclerView
    private lateinit var adapter: RoomAdapter
    private lateinit var tvTitle: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_list)

        sessionManager = SessionManager(this)
        
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        tvTitle = findViewById(R.id.tvTitle)
        rvRooms = findViewById(R.id.rvRooms)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        setupSwipeRefresh()
        setupAddRoomButton()
        loadRooms()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.gold_primary)
        swipeRefreshLayout.setOnRefreshListener {
            loadRooms()
        }
    }

    private fun setupAddRoomButton() {
        val btnAddRoom = findViewById<ImageButton>(R.id.btnAddRoom)
        if (sessionManager.userRole == UserRole.ADMIN) {
            btnAddRoom.visibility = View.VISIBLE
            btnAddRoom.setOnClickListener {
                showAddRoomDialog()
            }
        } else {
            btnAddRoom.visibility = View.GONE
        }
    }

    private fun loadRooms() {
        swipeRefreshLayout.isRefreshing = true
        val filterStatus = intent.getStringExtra("FILTER_STATUS")
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@RoomListActivity)
                val response = apiService.getAllRoomsInfo()
                
                if (response.isSuccessful) {
                    val roomList = response.body() ?: emptyList()
                    displayRooms(roomList, filterStatus)
                } else {
                    Toast.makeText(this@RoomListActivity, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoomListActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun displayRooms(roomList: List<com.example.luxhotel.models.RoomInfo>, filterStatus: String?) {
        var filteredList = roomList
        if (filterStatus != null) {
            filteredList = filteredList.filter { it.status == filterStatus }
            updateTitle(filterStatus)
        }

        val groups = filteredList.groupBy { it.roomType }.map { (type, roomsInGroup) ->
            RoomGroup(type, roomsInGroup)
        }

        adapter = RoomAdapter(groups)
        
        val layoutManager = GridLayoutManager(this@RoomListActivity, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == RoomAdapter.TYPE_HEADER) 2 else 1
            }
        }
        
        rvRooms.layoutManager = layoutManager
        rvRooms.adapter = adapter
    }

    private fun updateTitle(status: String) {
        val statusText = when (status) {
            DatabaseHelper.ROOM_STATUS_AVAILABLE -> "Phòng Trống"
            DatabaseHelper.ROOM_STATUS_OCCUPIED -> "Phòng Đang Ở"
            DatabaseHelper.ROOM_STATUS_MAINTENANCE -> "Phòng Đang Dọn"
            DatabaseHelper.ROOM_STATUS_BOOKED -> "Phòng Đã Đặt"
            else -> "Danh Sách Phòng"
        }
        tvTitle.text = statusText
    }

    private fun showAddRoomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_room, null)
        val etRoomNumber = dialogView.findViewById<EditText>(R.id.etRoomNumber)
        val etRoomType = dialogView.findViewById<EditText>(R.id.etRoomType)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val etFloor = dialogView.findViewById<EditText>(R.id.etFloor)
        val spTier = dialogView.findViewById<Spinner>(R.id.spTier)

        AlertDialog.Builder(this)
            .setTitle("Thêm Phòng Mới")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val number = etRoomNumber.text.toString()
                val type = etRoomType.text.toString()
                val priceStr = etPrice.text.toString()
                val price = if (priceStr.isEmpty()) 0L else priceStr.toLong()
                val floorStr = etFloor.text.toString()
                val floor = if (floorStr.isEmpty()) 1 else floorStr.toInt()
                val tier = if (spTier.selectedItemPosition == 1) RoomTier.VIP else RoomTier.STANDARD

                if (number.isNotEmpty() && type.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val roomBody = mapOf(
                                "roomNumber" to number,
                                "roomType" to type,
                                "roomTier" to tier.name,
                                "pricePerNight" to price,
                                "floor" to floor,
                                "bedCount" to 1,
                                "description" to ""
                            )
                            val response = RetrofitClient.getApiService(this@RoomListActivity).addRoom(roomBody)
                            if (response.isSuccessful) {
                                Toast.makeText(this@RoomListActivity, "Đã thêm phòng $number", Toast.LENGTH_SHORT).show()
                                loadRooms()
                            } else {
                                Toast.makeText(this@RoomListActivity, "Lỗi khi thêm phòng. Có thể số phòng đã tồn tại.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@RoomListActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}

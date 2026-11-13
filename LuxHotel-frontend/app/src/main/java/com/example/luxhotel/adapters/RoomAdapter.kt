package com.example.luxhotel.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.RoomInfo
import com.example.luxhotel.utils.DatabaseHelper
import java.text.NumberFormat
import java.util.Locale

data class RoomGroup(
    val type: String?,
    val rooms: List<RoomInfo>,
    var isExpanded: Boolean = false
)

sealed class RoomListItem {
    data class Header(val group: RoomGroup) : RoomListItem()
    data class Room(val info: RoomInfo) : RoomListItem()
}

class RoomAdapter(private val groups: List<RoomGroup>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var flattenedItems = mutableListOf<RoomListItem>()

    init {
        flatten()
    }

    private fun flatten() {
        flattenedItems.clear()
        for (group in groups) {
            flattenedItems.add(RoomListItem.Header(group))
            if (group.isExpanded) {
                for (room in group.rooms) {
                    flattenedItems.add(RoomListItem.Room(room))
                }
            }
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ROOM = 1
    }

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomNumber: TextView = view.findViewById(R.id.tvRoomNumber)
        val tvRoomStatus: TextView = view.findViewById(R.id.tvRoomStatus)
        val tvRoomType: TextView = view.findViewById(R.id.tvRoomType)
        val tvBedCount: TextView = view.findViewById(R.id.tvBedCount)
        val tvRoomPrice: TextView = view.findViewById(R.id.tvRoomPrice)
        val tvGuestName: TextView = view.findViewById(R.id.tvGuestName)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeaderTitle: TextView = view.findViewById(R.id.tvHeaderTitle)
        val tvHeaderCount: TextView = view.findViewById(R.id.tvHeaderCount)
        val ivArrow: ImageView = view.findViewById(R.id.ivArrow) // Ensure this exists in item_room_header
    }

    override fun getItemViewType(position: Int): Int {
        return when (flattenedItems[position]) {
            is RoomListItem.Header -> TYPE_HEADER
            is RoomListItem.Room -> TYPE_ROOM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_room_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_room_card, parent, false)
            RoomViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = flattenedItems[position]
        if (holder is HeaderViewHolder && item is RoomListItem.Header) {
            val group = item.group
            holder.tvHeaderTitle.text = group.type ?: "Loại phòng khác"
            holder.tvHeaderCount.text = "${group.rooms.size} phòng"
            holder.ivArrow.rotation = if (group.isExpanded) 180f else 0f
            
            holder.itemView.setOnClickListener {
                group.isExpanded = !group.isExpanded
                flatten()
                notifyDataSetChanged()
            }
        } else if (holder is RoomViewHolder && item is RoomListItem.Room) {
            val room = item.info
            val context = holder.itemView.context

            holder.tvRoomNumber.text = "Phòng ${room.roomNumber ?: "N/A"}"
            holder.tvRoomType.text = room.roomType ?: "N/A"
            holder.tvBedCount.text = "${room.bedCount ?: 0} giường"
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            holder.tvRoomPrice.text = formatter.format(room.price ?: 0L)
            
            if (room.guestName != null) {
                holder.tvGuestName.visibility = View.VISIBLE
                holder.tvGuestName.text = "Khách: ${room.guestName}"
            } else {
                holder.tvGuestName.visibility = View.GONE
            }

            when (room.status) {
                DatabaseHelper.ROOM_STATUS_AVAILABLE -> {
                    holder.tvRoomStatus.text = "Trống"
                    holder.tvRoomStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
                    holder.tvRoomStatus.setBackgroundResource(R.drawable.bg_badge_green)
                }
                DatabaseHelper.ROOM_STATUS_BOOKED -> {
                    holder.tvRoomStatus.text = "Đã đặt"
                    holder.tvRoomStatus.setTextColor(Color.parseColor("#4299E1"))
                    holder.tvRoomStatus.setBackgroundResource(R.drawable.bg_badge_blue)
                }
                DatabaseHelper.ROOM_STATUS_OCCUPIED -> {
                    holder.tvRoomStatus.text = "Đang ở"
                    holder.tvRoomStatus.setTextColor(ContextCompat.getColor(context, R.color.gold_primary))
                    holder.tvRoomStatus.setBackgroundResource(R.drawable.bg_badge_gold_filled)
                }
                DatabaseHelper.ROOM_STATUS_MAINTENANCE -> {
                    holder.tvRoomStatus.text = "Đang dọn"
                    holder.tvRoomStatus.setTextColor(ContextCompat.getColor(context, R.color.red_primary))
                    holder.tvRoomStatus.setBackgroundResource(R.drawable.bg_badge_red)
                }
                else -> {
                    holder.tvRoomStatus.text = "Khác"
                    holder.tvRoomStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    holder.tvRoomStatus.setBackgroundResource(R.drawable.bg_badge_gray)
                }
            }
        }
    }

    override fun getItemCount() = flattenedItems.size
}

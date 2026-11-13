package com.example.luxhotel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.RoomInfo

/**
 * Hiển thị danh sách phòng trong panel chi tiết ngày được chọn.
 */
class RoomDetailAdapter : ListAdapter<RoomInfo, RoomDetailAdapter.RoomViewHolder>(DiffCallback) {

    // ── DiffUtil ───────────────────────────────────────────────────────────

    private object DiffCallback : DiffUtil.ItemCallback<RoomInfo>() {
        override fun areItemsTheSame(old: RoomInfo, new: RoomInfo) =
            old.roomNumber == new.roomNumber

        override fun areContentsTheSame(old: RoomInfo, new: RoomInfo) =
            old.status == new.status
    }

    // ── Adapter overrides ──────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_detail, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val viewStatusBar: View   = itemView.findViewById(R.id.viewStatusBar)
        private val tvRoomNumber: TextView = itemView.findViewById(R.id.tvRoomNumber)
        private val tvRoomType: TextView   = itemView.findViewById(R.id.tvRoomType)
        private val tvGuestName: TextView  = itemView.findViewById(R.id.tvGuestName)
        private val tvRoomStatus: TextView = itemView.findViewById(R.id.tvRoomStatus)

        fun bind(room: RoomInfo) {
            val ctx = itemView.context

            tvRoomNumber.text = room.roomNumber
            tvRoomType.text   = room.roomType

            // Use hardcoded colors if resources are missing, or better, try to use resources
            val (color, statusLabel, showGuest) = when (room.status) {
                RoomInfo.STATUS_OCCUPIED -> Triple(
                    0xFFD4AF37.toInt(), // Gold
                    "Đang ở",
                    true
                )
                RoomInfo.STATUS_MAINTENANCE -> Triple(
                    0xFFE53E3E.toInt(), // Red
                    "Đang dọn",
                    false
                )
                else -> Triple(
                    0xFF48BB78.toInt(), // Green
                    "Trống",
                    false
                )
            }

            viewStatusBar.setBackgroundColor(color)
            tvRoomStatus.setTextColor(color)
            tvRoomStatus.text = statusLabel

            if (showGuest) {
                tvGuestName.visibility = View.VISIBLE
                tvGuestName.text       = room.guestName
            } else {
                tvGuestName.visibility = View.GONE
            }
        }
    }
}

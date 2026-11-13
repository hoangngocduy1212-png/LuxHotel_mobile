package com.example.luxhotel.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.CalendarDay
import java.util.Calendar

/**
 * Adapter cho lưới lịch 7 cột.
 *
 * Mỗi ô hiển thị:
 *  – Số ngày (bold + màu vàng nếu là hôm nay / đang chọn)
 *  – Tối đa 3 chấm trạng thái: xanh lá = trống, vàng = đang ở, đỏ = cần dọn
 */
class CalendarDayAdapter(
    private val listener: OnDayClickListener
) : ListAdapter<CalendarDay, CalendarDayAdapter.DayViewHolder>(DiffCallback) {

    // Kotlin fun interface → implement bằng lambda nếu muốn
    fun interface OnDayClickListener {
        fun onDayClick(day: CalendarDay)
    }

    private var selectedDate: Calendar? = null

    // ── DiffUtil ───────────────────────────────────────────────────────────

    private object DiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(old: CalendarDay, new: CalendarDay): Boolean {
            if (old.isEmpty || new.isEmpty) return old.isEmpty == new.isEmpty
            return old.dayNumber == new.dayNumber
        }

        override fun areContentsTheSame(old: CalendarDay, new: CalendarDay): Boolean {
            if (old.isEmpty && new.isEmpty) return true
            return old.dayNumber      == new.dayNumber
                    && old.isToday        == new.isToday
                    && old.isSelected     == new.isSelected
                    && old.availableCount == new.availableCount
                    && old.occupiedCount  == new.occupiedCount
                    && old.maintenanceCount == new.maintenanceCount
        }
    }

    // ── Selection ──────────────────────────────────────────────────────────

    fun setSelectedDate(cal: Calendar) {
        selectedDate = cal
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedDate = null
        notifyDataSetChanged()
    }

    private fun isSelected(day: CalendarDay): Boolean {
        val sel = selectedDate ?: return false
        val cal = day.calendar   ?: return false
        return cal.get(Calendar.YEAR) == sel.get(Calendar.YEAR) &&
               cal.get(Calendar.MONTH) == sel.get(Calendar.MONTH) &&
               cal.get(Calendar.DAY_OF_MONTH) == sel.get(Calendar.DAY_OF_MONTH)
    }

    // ── Adapter overrides ──────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val container: View     = itemView.findViewById(R.id.containerDay)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val dotAvailable: View  = itemView.findViewById(R.id.dotAvailable)
        private val dotOccupied: View   = itemView.findViewById(R.id.dotOccupied)
        private val dotDirty: View      = itemView.findViewById(R.id.dotDirty)

        fun bind(day: CalendarDay) {
            if (day.isEmpty) {
                itemView.visibility = View.INVISIBLE
                itemView.setOnClickListener(null)
                return
            }

            itemView.visibility = View.VISIBLE
            tvDayNumber.text    = day.dayNumber.toString()

            val selected = isSelected(day)

            // Background + màu chữ
            when {
                selected -> {
                    container.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                    tvDayNumber.setTextColor(itemView.context.getColor(R.color.gold))
                    tvDayNumber.setTypeface(null, Typeface.BOLD)
                }
                day.isToday -> {
                    container.setBackgroundResource(R.drawable.bg_calendar_day_today)
                    tvDayNumber.setTextColor(itemView.context.getColor(R.color.gold))
                    tvDayNumber.setTypeface(null, Typeface.BOLD)
                }
                else -> {
                    container.setBackgroundResource(R.drawable.bg_calendar_day)
                    tvDayNumber.setTextColor(itemView.context.getColor(android.R.color.white))
                    tvDayNumber.setTypeface(null, Typeface.NORMAL)
                }
            }

            // Chấm trạng thái
            dotAvailable.visibility = if (day.availableCount > 0) View.VISIBLE else View.GONE
            dotOccupied.visibility  = if (day.occupiedCount  > 0) View.VISIBLE else View.GONE
            dotDirty.visibility     = if (day.maintenanceCount > 0) View.VISIBLE else View.GONE

            // Click
            itemView.setOnClickListener { listener.onDayClick(day) }
        }
    }
}

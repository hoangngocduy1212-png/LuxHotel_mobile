package com.example.luxhotel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.CheckoutBooking
import com.example.luxhotel.models.CheckoutStatus
import java.text.NumberFormat
import java.util.Locale

class CheckoutAdapter(
    private val onCardClick: (CheckoutBooking) -> Unit,
    private val onCheckoutClick: (CheckoutBooking) -> Unit,
) : ListAdapter<CheckoutBooking, CheckoutAdapter.ViewHolder>(DiffCallback()) {

    // ── ViewHolder ──────────────────────────────────────────────────────────

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRoomNumber: TextView = itemView.findViewById(R.id.tvRoomNumber)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvGuestName: TextView = itemView.findViewById(R.id.tvGuestName)
        val tvDateRange: TextView = itemView.findViewById(R.id.tvDateRange)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val btnCheckout: Button = itemView.findViewById(R.id.btnCheckout)
    }

    // ── Adapter overrides ───────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkout_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = getItem(position)
        val ctx = holder.itemView.context

        // Số phòng
        holder.tvRoomNumber.text = booking.roomNumber

        // Tên khách
        holder.tvGuestName.text = booking.guestName

        // Khoảng ngày
        holder.tvDateRange.text = ctx.getString(
            R.string.date_range_format,
            booking.checkInDate,
            booking.checkOutDate,
            booking.nights,
        )

        // Tổng tiền
        holder.tvTotalAmount.text = formatMoney(booking.grandTotal)

        // Badge trạng thái
        applyStatusBadge(holder, booking.status)

        // Click cả card → vào detail
        holder.itemView.setOnClickListener { onCardClick(booking) }

        // Click nút Check-out → xác nhận
        holder.btnCheckout.setOnClickListener { onCheckoutClick(booking) }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun applyStatusBadge(holder: ViewHolder, status: CheckoutStatus?) {
        val ctx = holder.itemView.context
        when (status ?: CheckoutStatus.DUE_TODAY) {
            CheckoutStatus.DUE_TODAY -> {
                holder.tvStatusBadge.text = ctx.getString(R.string.badge_due_today)
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(ctx, R.color.green_primary)
                )
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green)
                holder.btnCheckout.setBackgroundResource(R.drawable.bg_btn_gold)
                holder.btnCheckout.setTextColor(
                    ContextCompat.getColor(ctx, R.color.bg_dark)
                )
            }
            CheckoutStatus.OVERDUE -> {
                holder.tvStatusBadge.text = ctx.getString(R.string.badge_overdue)
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(ctx, R.color.red_primary)
                )
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red)
                holder.btnCheckout.setBackgroundResource(R.drawable.bg_btn_gold) // Using gold for both for now or I can create bg_btn_red
                holder.btnCheckout.setTextColor(
                    ContextCompat.getColor(ctx, R.color.white)
                )
            }
            CheckoutStatus.CHECKED_OUT -> {
                holder.tvStatusBadge.text = ctx.getString(R.string.badge_checked_out)
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(ctx, R.color.text_muted)
                )
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_gray)
                holder.btnCheckout.isEnabled = false
            }
            else -> {
                holder.tvStatusBadge.text = status?.name ?: "N/A"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_gray)
            }
        }
    }

    private fun formatMoney(amount: Long): String {
        val fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return "${fmt.format(amount)} đ"
    }

    // ── DiffCallback ─────────────────────────────────────────────────────────

    class DiffCallback : DiffUtil.ItemCallback<CheckoutBooking>() {
        override fun areItemsTheSame(old: CheckoutBooking, new: CheckoutBooking) =
            old.bookingId == new.bookingId

        override fun areContentsTheSame(old: CheckoutBooking, new: CheckoutBooking) =
            old == new
    }
}

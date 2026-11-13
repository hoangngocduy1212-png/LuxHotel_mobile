package com.example.luxhotel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.CheckoutBooking
import java.util.Locale

class InvoiceAdapter(
    private val onItemClick: (CheckoutBooking) -> Unit
) : ListAdapter<CheckoutBooking, InvoiceAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInvoiceId: TextView = itemView.findViewById(R.id.tvInvoiceId)
        val tvRoomNumber: TextView = itemView.findViewById(R.id.tvRoomNumber)
        val tvGuestName: TextView = itemView.findViewById(R.id.tvGuestName)
        val tvDateRange: TextView = itemView.findViewById(R.id.tvDateRange)
        val tvCheckoutDate: TextView = itemView.findViewById(R.id.tvCheckoutDate)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = getItem(position)
        val context = holder.itemView.context

        holder.tvInvoiceId.text = context.getString(R.string.label_invoice_id, booking.bookingId)
        holder.tvRoomNumber.text = context.getString(R.string.room_label_format, booking.roomNumber)
        holder.tvGuestName.text = booking.guestName
        holder.tvDateRange.text = context.getString(R.string.date_range_format, booking.checkInDate, booking.checkOutDate, booking.nights)
        
        // Hiện tại booking date format là dd/MM/yyyy
        holder.tvCheckoutDate.text = context.getString(R.string.label_checkout_at, booking.checkOutDate)
        
        holder.tvTotalAmount.text = context.getString(R.string.total_amount_format, booking.grandTotal)

        holder.itemView.setOnClickListener { onItemClick(booking) }
    }

    class DiffCallback : DiffUtil.ItemCallback<CheckoutBooking>() {
        override fun areItemsTheSame(oldItem: CheckoutBooking, newItem: CheckoutBooking) = oldItem.bookingId == newItem.bookingId
        override fun areContentsTheSame(oldItem: CheckoutBooking, newItem: CheckoutBooking) = oldItem == newItem
    }
}

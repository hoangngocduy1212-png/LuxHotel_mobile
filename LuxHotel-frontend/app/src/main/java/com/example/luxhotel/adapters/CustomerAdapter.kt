package com.example.luxhotel.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.Customer
import com.example.luxhotel.models.CustomerStatus

class CustomerAdapter(
    private var customers: List<Customer>,
    private val onItemClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private var filteredList: List<Customer> = customers

    class CustomerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCustomerInitial: TextView = view.findViewById(R.id.tvCustomerInitial)
        val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvRoomInfo: TextView = view.findViewById(R.id.tvRoomInfo)
        val tvCustomerStatus: TextView = view.findViewById(R.id.tvCustomerStatus)
        val btnCall: ImageButton = view.findViewById(R.id.btnCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = filteredList[position]
        
        holder.tvCustomerName.text = customer.name ?: "N/A"
        holder.tvRoomInfo.text = "Phòng ${customer.roomNumber ?: ""} - ${customer.roomType ?: ""}"
        holder.tvCustomerInitial.text = customer.name?.firstOrNull()?.toString()?.uppercase() ?: ""
        
        val statusText: String
        val statusColor: Int
        val statusBg: Int

        when (customer.status) {
            CustomerStatus.ARRIVING -> {
                statusText = "Sắp đến"
                statusColor = ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_light)
                statusBg = R.drawable.bg_badge_blue
            }
            CustomerStatus.STAYING -> {
                statusText = "Đang ở"
                statusColor = ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light)
                statusBg = R.drawable.bg_badge_green
            }
            CustomerStatus.LEFT -> {
                statusText = "Đã đi"
                statusColor = ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                statusBg = R.drawable.bg_badge_gray
            }
            else -> {
                statusText = "N/A"
                statusColor = ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                statusBg = R.drawable.bg_badge_gray
            }
        }

        holder.tvCustomerStatus.text = statusText
        holder.tvCustomerStatus.setTextColor(statusColor)
        // Note: We might need to handle background if we have them, otherwise just set text color or a generic badge
        try {
            holder.tvCustomerStatus.setBackgroundResource(statusBg)
        } catch (e: Exception) {
            // Fallback if drawable doesn't exist
        }

        holder.itemView.setOnClickListener { onItemClick(customer) }

        holder.btnCall.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${customer.phone}")
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = filteredList.size

    fun updateData(newCustomers: List<Customer>) {
        customers = newCustomers
        filter("")
    }

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            customers
        } else {
            customers.filter {
                it.name?.contains(query, ignoreCase = true) == true || 
                it.roomNumber?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }
}

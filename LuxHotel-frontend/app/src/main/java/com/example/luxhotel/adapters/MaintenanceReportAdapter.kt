package com.example.luxhotel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.luxhotel.R
import com.example.luxhotel.utils.DatabaseHelper

class MaintenanceReportAdapter(
    private val reports: List<DatabaseHelper.MaintenanceReportItem>
) : RecyclerView.Adapter<MaintenanceReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomNumber: TextView = view.findViewById(R.id.tvRoomNumber)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val ivPhoto: ImageView = view.findViewById(R.id.ivIncidentPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maintenance_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.tvRoomNumber.text = "Phòng ${report.roomNumber}"
        holder.tvStatus.text = report.status
        holder.tvDescription.text = report.description

        if (!report.imagePath.isNullOrEmpty()) {
            holder.ivPhoto.visibility = View.VISIBLE
            holder.ivPhoto.load(report.imagePath) {
                crossfade(true)
            }
        } else {
            holder.ivPhoto.visibility = View.GONE
        }
    }

    override fun getItemCount() = reports.size
}

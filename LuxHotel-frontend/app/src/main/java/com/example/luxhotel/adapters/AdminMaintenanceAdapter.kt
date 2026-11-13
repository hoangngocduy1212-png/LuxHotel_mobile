package com.example.luxhotel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.luxhotel.R
import com.example.luxhotel.utils.DatabaseHelper

class AdminMaintenanceAdapter(
    private var reports: List<DatabaseHelper.MaintenanceReportItem>,
    private val onItemClick: (DatabaseHelper.MaintenanceReportItem) -> Unit,
    private val onComplete: (DatabaseHelper.MaintenanceReportItem) -> Unit
) : RecyclerView.Adapter<AdminMaintenanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomNumber: TextView = view.findViewById(R.id.tvRoomNumber)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvReportedBy: TextView = view.findViewById(R.id.tvReportedBy)
        val ivIncidentPhoto: ImageView = view.findViewById(R.id.ivIncidentPhoto)
        val btnComplete: Button = view.findViewById(R.id.btnComplete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maintenance_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.tvRoomNumber.text = "Phòng ${report.roomNumber}"
        holder.tvStatus.text = report.status
        holder.tvDescription.text = report.description
        holder.tvReportedBy.text = "Báo cáo bởi: ${report.reportedBy}"

        if (!report.imagePath.isNullOrEmpty()) {
            holder.ivIncidentPhoto.visibility = View.VISIBLE
            holder.ivIncidentPhoto.load(report.imagePath) {
                crossfade(true)
                placeholder(R.color.bg_dark_secondary)
                error(R.drawable.ic_alert_triangle) // Hoặc icon lỗi bất kỳ
            }
        } else {
            holder.ivIncidentPhoto.visibility = View.GONE
        }

        holder.btnComplete.setOnClickListener {
            onComplete(report)
        }

        holder.itemView.setOnClickListener {
            onItemClick(report)
        }
    }

    override fun getItemCount() = reports.size

    fun updateData(newReports: List<DatabaseHelper.MaintenanceReportItem>) {
        reports = newReports
        notifyDataSetChanged()
    }
}

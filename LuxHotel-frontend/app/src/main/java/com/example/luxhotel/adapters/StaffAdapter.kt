package com.example.luxhotel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.luxhotel.R
import com.example.luxhotel.models.User
import com.example.luxhotel.models.UserRole
import com.google.android.material.switchmaterial.SwitchMaterial

class StaffAdapter(
    private var users: List<User>,
    private val onStatusChanged: (User, Boolean) -> Unit,
    private val onItemClicked: (User) -> Unit
) : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {

    class StaffViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewRoleIndicator: View = view.findViewById(R.id.viewRoleIndicator)
        val tvDisplayName: TextView = view.findViewById(R.id.tvDisplayName)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val switchActive: SwitchMaterial = view.findViewById(R.id.switchActive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff, parent, false)
        return StaffViewHolder(view)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val user = users[position]
        holder.tvDisplayName.text = user.displayName
        holder.tvUsername.text = "@${user.username ?: ""}"
        holder.tvRole.text = user.role?.name ?: "UNKNOWN"
        
        holder.switchActive.setOnCheckedChangeListener(null)
        holder.switchActive.isChecked = user.isActive ?: false
        
        val context = holder.itemView.context
        val (colorRes, badgeRes) = when (user.role) {
            UserRole.ADMIN -> R.color.badge_admin to R.drawable.bg_role_badge_gold
            UserRole.RECEPTIONIST -> R.color.badge_receptionist to R.drawable.bg_role_badge_blue
            UserRole.HOUSEKEEPING -> R.color.badge_housekeeping to R.drawable.bg_role_badge_green
            null -> R.color.badge_housekeeping to R.drawable.bg_role_badge_green // Default
        }
        
        holder.viewRoleIndicator.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        holder.tvRole.setBackgroundResource(badgeRes)

        holder.switchActive.setOnCheckedChangeListener { _, isChecked ->
            onStatusChanged(user, isChecked)
        }

        holder.itemView.setOnClickListener {
            onItemClicked(user)
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}

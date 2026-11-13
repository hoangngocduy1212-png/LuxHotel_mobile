package com.example.luxhotel.helpers

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.luxhotel.R
import com.example.luxhotel.models.DrawerType
import com.example.luxhotel.models.RoomTier
import com.example.luxhotel.models.TaskDrawerData
import com.example.luxhotel.models.TaskItem
import com.example.luxhotel.models.TaskPriority

class TaskDrawerHelper(
    private val context: Context,
    drawerView: View,
    private val data: TaskDrawerData,
    private val onTaskToggled: (TaskItem) -> Unit,
) {

    private val header: LinearLayout = drawerView.findViewById(R.id.drawerHeader)
    private val iconBubble: FrameLayout = drawerView.findViewById(R.id.iconBubble)
    private val ivIcon: ImageView = drawerView.findViewById(R.id.ivDrawerIcon)
    private val tvTitle: TextView = drawerView.findViewById(R.id.tvDrawerTitle)
    private val tvSubtitle: TextView = drawerView.findViewById(R.id.tvDrawerSubtitle)
    private val tvCount: TextView = drawerView.findViewById(R.id.tvDrawerCount)
    private val ivArrow: ImageView = drawerView.findViewById(R.id.ivArrow)
    private val divider: View = drawerView.findViewById(R.id.drawerDivider)
    private val body: LinearLayout = drawerView.findViewById(R.id.drawerBody)

    fun bind() {
        applyDrawerStyle()
        tvTitle.text    = data.title
        tvSubtitle.text = data.subtitle
        tvCount.text    = data.items.size.toString()

        body.removeAllViews()
        data.items.forEachIndexed { index, task ->
            if (index > 0) {
                val divView = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(ContextCompat.getColor(context, R.color.border_light))
                }
                body.addView(divView)
            }
            body.addView(buildTaskRow(task))
        }

        setExpanded(data.isExpanded, animate = false)
        header.setOnClickListener {
            data.isExpanded = !data.isExpanded
            setExpanded(data.isExpanded, animate = true)
        }
    }

    private fun setExpanded(expanded: Boolean, animate: Boolean) {
        body.visibility    = if (expanded) View.VISIBLE else View.GONE
        divider.visibility = if (expanded) View.VISIBLE else View.GONE

        val target = if (expanded) 180f else 0f
        if (animate) {
            ObjectAnimator
                .ofFloat(ivArrow, "rotation", ivArrow.rotation, target)
                .setDuration(200)
                .start()
        } else {
            ivArrow.rotation = target
        }
    }

    private fun applyDrawerStyle() {
        val style = when (data.type) {
            DrawerType.CLEAN -> DrawerStyle(
                R.drawable.bg_icon_bubble_green, R.drawable.ic_sparkles,
                R.color.green_dark, R.drawable.bg_badge_green, R.color.green_dark
            )
            DrawerType.URGENT -> DrawerStyle(
                R.drawable.bg_icon_bubble_red, R.drawable.ic_alert_triangle,
                R.color.red_dark, R.drawable.bg_badge_red, R.color.red_dark
            )
            DrawerType.SUPPLY -> DrawerStyle(
                R.drawable.bg_icon_bubble_amber, R.drawable.ic_package,
                R.color.amber_dark, R.drawable.bg_badge_amber, R.color.amber_dark
            )
            DrawerType.INSPECT -> DrawerStyle(
                R.drawable.bg_icon_bubble_blue, R.drawable.ic_clipboard_check,
                R.color.blue_dark, R.drawable.bg_badge_blue, R.color.blue_dark
            )
        }
        iconBubble.setBackgroundResource(style.bubbleBg)
        ivIcon.setImageResource(style.iconRes)
        ivIcon.imageTintList = ContextCompat.getColorStateList(context, style.iconTint)
        tvCount.setBackgroundResource(style.badgeBg)
        tvCount.setTextColor(ContextCompat.getColor(context, style.badgeTextColor))
    }

    private fun buildTaskRow(task: TaskItem): View {
        val row = LayoutInflater.from(context).inflate(R.layout.item_task_row, body, false)
        val ivCheck: ImageView = row.findViewById(R.id.ivCheck)
        val tvName: TextView   = row.findViewById(R.id.tvRoomName)
        val tvTier: TextView   = row.findViewById(R.id.tvTier)
        val tvPrio: TextView   = row.findViewById(R.id.tvPriority)

        renderRow(task, ivCheck, tvName, tvTier, tvPrio)

        row.setOnClickListener {
            task.isDone   = !task.isDone
            task.priority = if (task.isDone) TaskPriority.DONE else TaskPriority.NORMAL
            renderRow(task, ivCheck, tvName, tvTier, tvPrio)
            refreshCount()
            onTaskToggled(task)
        }
        return row
    }

    private fun renderRow(
        task: TaskItem,
        ivCheck: ImageView,
        tvName: TextView,
        tvTier: TextView,
        tvPrio: TextView
    ) {
        ivCheck.setImageResource(
            if (task.isDone) R.drawable.ic_check_circle_filled
            else R.drawable.ic_check_circle_empty
        )

        tvName.text = task.label
        tvName.setTextColor(
            ContextCompat.getColor(
                context,
                if (task.isDone) R.color.text_muted else R.color.text_primary
            )
        )
        tvName.paintFlags = if (task.isDone) {
            tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        when (task.tier) {
            RoomTier.VIP -> {
                tvTier.setText(R.string.tier_vip)
                tvTier.setBackgroundResource(R.drawable.bg_badge_gold_filled)
                tvTier.setTextColor(ContextCompat.getColor(context, R.color.gold_primary))
            }
            RoomTier.STANDARD -> {
                tvTier.setText(R.string.tier_standard)
                tvTier.setBackgroundResource(R.drawable.bg_badge_gray)
                tvTier.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            }
        }

        when (task.priority) {
            TaskPriority.URGENT -> {
                tvPrio.setText(R.string.priority_urgent)
                tvPrio.setBackgroundResource(R.drawable.bg_badge_red)
                tvPrio.setTextColor(ContextCompat.getColor(context, R.color.red_dark))
            }
            TaskPriority.NORMAL -> {
                tvPrio.setText(R.string.priority_normal)
                tvPrio.setBackgroundResource(R.drawable.bg_badge_amber)
                tvPrio.setTextColor(ContextCompat.getColor(context, R.color.amber_dark))
            }
            TaskPriority.DONE -> {
                tvPrio.setText(R.string.priority_done)
                tvPrio.setBackgroundResource(R.drawable.bg_badge_green)
                tvPrio.setTextColor(ContextCompat.getColor(context, R.color.green_dark))
            }
        }
    }

    fun refreshCount() {
        tvCount.text    = data.items.size.toString()
        tvSubtitle.text = data.subtitle
    }

    private data class DrawerStyle(
        val bubbleBg: Int,
        val iconRes: Int,
        val iconTint: Int,
        val badgeBg: Int,
        val badgeTextColor: Int
    )
}
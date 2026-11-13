package com.example.luxhotel.models

import java.util.Calendar

/**
 * Đại diện cho một ô trong lưới lịch tháng.
 * Ô trống (đầu/cuối tháng) được tạo qua [CalendarDay.empty].
 */
data class CalendarDay(
    val calendar: Calendar?,
    val dayNumber: Int,
    val isToday: Boolean,
    var isSelected: Boolean,
    val availableCount: Int,
    val occupiedCount: Int,
    val maintenanceCount: Int,
    val isEmpty: Boolean = false
) {
    companion object {
        /** Tạo ô placeholder không có nội dung */
        fun empty() = CalendarDay(
            calendar       = null,
            dayNumber      = 0,
            isToday        = false,
            isSelected     = false,
            availableCount = 0,
            occupiedCount  = 0,
            maintenanceCount = 0,
            isEmpty        = true
        )
    }
}
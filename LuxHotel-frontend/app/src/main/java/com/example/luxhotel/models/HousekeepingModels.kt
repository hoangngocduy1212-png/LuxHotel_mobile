package com.example.luxhotel.models

import com.google.gson.annotations.SerializedName

enum class RoomTier { VIP, STANDARD }

enum class TaskPriority { URGENT, NORMAL, DONE }

enum class DrawerType { CLEAN, URGENT, SUPPLY, INSPECT }

data class TaskItem(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String,
    @SerializedName("tier") val tier: RoomTier = RoomTier.STANDARD,
    @SerializedName("priority") var priority: TaskPriority = TaskPriority.NORMAL,
    @SerializedName("isDone") var isDone: Boolean = false,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("taskType") val taskType: String? = null,
    @SerializedName("taskDate") val taskDate: String? = null,
    @SerializedName("assignedTo") val assignedTo: String? = null
)

data class TaskDrawerData(
    val type: DrawerType,
    val items: MutableList<TaskItem>,
    var isExpanded: Boolean = false,
) {
    val title: String
        get() = when (type) {
            DrawerType.CLEAN   -> "Dọn phòng"
            DrawerType.URGENT  -> "Yêu cầu khẩn"
            DrawerType.SUPPLY  -> "Bổ sung vật tư"
            DrawerType.INSPECT -> "Kiểm tra phòng"
        }

    val subtitle: String
        get() = "${items.size} ${
            when (type) {
                DrawerType.CLEAN   -> "phòng hôm nay"
                DrawerType.URGENT  -> "yêu cầu"
                DrawerType.SUPPLY  -> "việc"
                DrawerType.INSPECT -> "phòng"
            }
        }"

    val doneCount: Int get() = items.count { it.isDone }
}
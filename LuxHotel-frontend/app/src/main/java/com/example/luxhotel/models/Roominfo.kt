package com.example.luxhotel.models

import com.google.gson.annotations.SerializedName

/**
 * Thông tin trạng thái và chi tiết của một phòng.
 */
data class RoomInfo(
    @SerializedName("roomNumber", alternate = ["room_number"]) val roomNumber: String? = null,
    @SerializedName("roomType", alternate = ["room_type"]) val roomType: String? = null,
    @SerializedName("roomTier", alternate = ["room_tier"]) val roomTier: String? = "STANDARD",
    @SerializedName("status") val status: String? = null,
    @SerializedName("price", alternate = ["price_per_night"]) val price: Long? = 0,
    @SerializedName("floor") val floor: Int? = 1,
    @SerializedName("description") val description: String? = null,
    @SerializedName("bedCount", alternate = ["bed_count"]) val bedCount: Int? = 1,
    @SerializedName("guestName", alternate = ["guest_name"]) val guestName: String? = null
) {
    companion object {
        const val STATUS_AVAILABLE = "available"
        const val STATUS_OCCUPIED  = "occupied"
        const val STATUS_MAINTENANCE = "maintenance"
        @Deprecated("Use STATUS_MAINTENANCE instead", ReplaceWith("STATUS_MAINTENANCE"))
        const val STATUS_DIRTY     = "maintenance"
    }
}
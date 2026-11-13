package com.example.luxhotel.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Trạng thái booking khi check-out
 */
enum class CheckoutStatus {
    DUE_TODAY,
    OVERDUE,
    CHECKED_OUT,
    BOOKED,      // Thêm để khớp backend nếu cần
    CANCELLED    // Thêm để khớp backend nếu cần
}

/**
 * Một dòng chi tiết dịch vụ trong hóa đơn
 */
data class ServiceCharge(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Long
) : Serializable

/**
 * Model đặt phòng dùng cho màn hình Check-out
 */
data class CheckoutBooking(
    @SerializedName("bookingId") val bookingId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("roomType") val roomType: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("guestPhone") val guestPhone: String? = "",
    @SerializedName("guestIdCard") val guestIdCard: String? = "",
    @SerializedName("checkInDate", alternate = ["checkinDate", "checkin_date"]) val checkInDate: String? = null,   // định dạng dd/MM/yyyy
    @SerializedName("checkOutDate", alternate = ["checkoutDate", "checkout_date"]) val checkOutDate: String? = null,  // định dạng dd/MM/yyyy
    @SerializedName("actualCheckin") val actualCheckin: String? = null,
    @SerializedName("actualCheckout") val actualCheckout: String? = null,
    @SerializedName("nights") val nights: Int? = 0,
    @SerializedName("roomPricePerNight") val roomPricePerNight: Long? = 0,
    @SerializedName("serviceCharges") val serviceCharges: List<ServiceCharge>? = emptyList(),
    @SerializedName("status") val rawStatus: String? = null,
    @SerializedName("createdBy") val createdBy: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) : Serializable {

    /** Trạng thái hiển thị (DUE_TODAY / OVERDUE / CHECKED_OUT) */
    val status: CheckoutStatus get() {
        if (rawStatus == "CHECKED_OUT") return CheckoutStatus.CHECKED_OUT
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val outDate = checkOutDate ?: ""
        return if (outDate < today) CheckoutStatus.OVERDUE else CheckoutStatus.DUE_TODAY
    }

    /** Số đêm tính toán thực tế */
    val computedNights: Int get() {
        if (nights != null && nights > 0) return nights
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val d1 = fmt.parse(checkInDate ?: "")?.time ?: 0L
            val d2 = fmt.parse(checkOutDate ?: "")?.time ?: 0L
            val diff = (d2 - d1) / (1000L * 60 * 60 * 24)
            if (diff < 1) 1 else diff.toInt()
        } catch (e: Exception) { 1 }
    }

    /** Tổng tiền phòng */
    val roomTotal: Long get() = (roomPricePerNight ?: 0L) * computedNights

    /** Tổng dịch vụ thêm */
    val servicesTotal: Long get() = serviceCharges?.sumOf { it.amount } ?: 0L

    /** Tổng hóa đơn */
    val grandTotal: Long get() = roomTotal + servicesTotal
}

package com.example.luxhotel.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class CustomerStatus {
    @SerializedName("ARRIVING") ARRIVING, // Sắp đến
    @SerializedName("STAYING") STAYING,  // Đang ở
    @SerializedName("LEFT") LEFT      // Đã đi
}

data class Customer(
    @SerializedName("id", alternate = ["customerId", "customer_id"]) val id: String? = null,
    @SerializedName("name", alternate = ["fullName", "full_name"]) val name: String? = null,
    @SerializedName("roomNumber", alternate = ["room_number"]) val roomNumber: String? = "",
    @SerializedName("roomType", alternate = ["room_type"]) val roomType: String? = "",
    @SerializedName("status") val status: CustomerStatus? = CustomerStatus.STAYING,
    @SerializedName("phone") val phone: String = ""
) : Serializable

package com.example.luxhotel.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class UserRole {
    @SerializedName("ADMIN") ADMIN,
    @SerializedName("RECEPTIONIST") RECEPTIONIST,
    @SerializedName("HOUSEKEEPING") HOUSEKEEPING
}

data class User(
    @SerializedName("username") val username: String? = null,
    @SerializedName("role") val role: UserRole? = null,
    @SerializedName("displayName", alternate = ["display_name"]) val displayName: String? = null,
    @SerializedName("phone") val phone: String? = "",
    @SerializedName("email") val email: String? = "",
    @SerializedName("department") val department: String? = "",
    @SerializedName("joinDate", alternate = ["join_date"]) val joinDate: String? = "",
    @SerializedName("staffId", alternate = ["staff_id"]) val staffId: String? = "",
    @SerializedName("isActive", alternate = ["is_active"]) val isActive: Boolean? = true,
    @SerializedName("avatarPath", alternate = ["avatar_path"]) val avatarPath: String? = null
) : Serializable

package com.example.luxhotel.network

import com.example.luxhotel.models.*
import com.example.luxhotel.utils.DatabaseHelper.MaintenanceReportItem
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import kotlin.jvm.JvmSuppressWildcards

@JvmSuppressWildcards
interface ApiService {

    // --- Auth ---
    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    // --- Users ---
    @GET("api/users")
    suspend fun getAllUsers(@Query("role") role: String? = null): Response<List<User>>

    @GET("api/users/{username}")
    suspend fun getUser(@Path("username") username: String): Response<User>

    @POST("api/users")
    suspend fun addUser(@Body user: Map<String, Any?>): Response<Unit>

    @PATCH("api/users/{username}/status")
    suspend fun setUserActive(@Path("username") username: String, @Body body: Map<String, Boolean>): Response<Unit>

    @PATCH("api/users/{username}/password")
    suspend fun changePassword(@Path("username") username: String, @Body body: Map<String, String>): Response<Unit>

    @Multipart
    @PATCH("api/users/{username}/avatar")
    suspend fun updateAvatar(
        @Path("username") username: String,
        @Part avatar: MultipartBody.Part
    ): Response<SimpleMessageResponse>

    @DELETE("api/users/{username}/avatar")
    suspend fun deleteAvatar(@Path("username") username: String): Response<SimpleMessageResponse>

    // --- Rooms ---
    @GET("api/rooms")
    suspend fun getAllRoomsInfo(): Response<List<RoomInfo>>

    @GET("api/rooms/available")
    suspend fun getAvailableRooms(): Response<List<RoomInfo>>

    @GET("api/rooms/status-counts")
    suspend fun getRoomStatusCounts(): Response<Map<String, Int>>

    @GET("api/rooms/calendar")
    suspend fun getRoomCalendar(@Query("date") date: String): Response<CalendarResponse>

    @POST("api/rooms")
    suspend fun addRoom(@Body room: Map<String, Any?>): Response<Unit>

    @PATCH("api/rooms/{roomNumber}/status")
    suspend fun updateRoomStatus(@Path("roomNumber") roomNumber: String, @Body body: Map<String, String>): Response<Unit>

    // --- Customers ---
    @GET("api/customers")
    suspend fun findCustomerByPhone(@Query("phone") phone: String): Response<List<Customer>>

    @GET("api/customers/status")
    suspend fun getAllCustomersWithStatus(): Response<List<Customer>>

    @POST("api/customers")
    suspend fun addCustomer(@Body customer: Map<String, String?>): Response<AddCustomerResponse>

    // --- Bookings ---
    @GET("api/bookings")
    suspend fun getBookingsByStatus(@Query("status") status: String): Response<List<CheckoutBooking>>

    @POST("api/bookings")
    suspend fun createBooking(@Body booking: Map<String, Any?>): Response<CreateBookingResponse>

    @POST("api/bookings/{id}/checkin")
    suspend fun checkIn(@Path("id") bookingId: Long): Response<Unit>

    @POST("api/bookings/{id}/checkout")
    suspend fun checkOut(@Path("id") bookingId: Long): Response<CheckoutResultResponse>

    @POST("api/bookings/{id}/service-charges")
    suspend fun addServiceCharge(@Path("id") bookingId: Long, @Body body: Map<String, Any>): Response<Unit>

    // --- Housekeeping tasks ---
    @GET("api/housekeeping-tasks")
    suspend fun getTasksByType(
        @Query("date") date: String,
        @Query("taskType") type: String,
        @Query("assignedTo") assignedTo: String? = null
    ): Response<List<TaskItem>>

    @GET("api/housekeeping-tasks/summary")
    suspend fun getTaskSummary(
        @Query("date") date: String,
        @Query("assignedTo") assignedTo: String? = null
    ): Response<TaskSummaryResponse>

    @POST("api/housekeeping-tasks")
    suspend fun addTask(@Body body: Map<String, Any?>): Response<Unit>

    @PATCH("api/housekeeping-tasks/{id}/done")
    suspend fun setTaskDone(@Path("id") taskId: Long, @Body body: Map<String, Boolean>): Response<Unit>

    // --- Maintenance reports ---
    @GET("api/maintenance-reports")
    suspend fun getMaintenanceReports(@Query("status") status: String? = null): Response<List<MaintenanceReportItem>>

    @Multipart
    @POST("api/maintenance-reports")
    suspend fun addMaintenanceReport(
        @Part("roomNumber") roomNumber: RequestBody,
        @Part("description") description: RequestBody,
        @Part("reportedBy") reportedBy: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<Unit>

    @PATCH("api/maintenance-reports/{id}/status")
    suspend fun updateMaintenanceReportStatus(@Path("id") reportId: Long, @Body body: Map<String, String>): Response<Unit>

    // --- Reports ---
    @GET("api/reports/dashboard")
    suspend fun getAdminDashboard(): Response<DashboardResponse>

    @GET("api/reports/revenue")
    suspend fun getRevenueByMonth(@Query("year") year: Int, @Query("month") month: Int): Response<RevenueResponse>
}

// --- Specific Response Models ---

data class SimpleMessageResponse(
    val message: String,
    @SerializedName("avatarPath") val avatarPath: String? = null
)

data class LoginResponse(
    val token: String? = null,
    val user: User? = null
)

data class CalendarResponse(
    val date: String,
    val available: Int,
    val occupied: Int,
    val maintenance: Int,
    val rooms: List<RoomInfo>
)

data class AddCustomerResponse(
    @SerializedName("customerId", alternate = ["customer_id"]) val customerId: Long,
    val message: String
)

data class CreateBookingResponse(
    @SerializedName("bookingId", alternate = ["booking_id"]) val bookingId: Long,
    val message: String
)

data class CheckoutResultResponse(
    val message: String,
    val nights: Int,
    @SerializedName("roomTotal", alternate = ["room_total"]) val roomTotal: Long,
    @SerializedName("serviceTotal", alternate = ["service_total"]) val serviceTotal: Long,
    @SerializedName("totalAmount", alternate = ["total_amount"]) val totalAmount: Long
)

data class TaskSummaryResponse(
    val total: Int,
    val done: Int
)

data class DashboardResponse(
    @SerializedName("roomStatus", alternate = ["room_status"]) val roomStatus: Map<String, Int>,
    @SerializedName("todayCheckins", alternate = ["today_checkins"]) val todayCheckins: Int,
    @SerializedName("todayCheckouts", alternate = ["today_checkouts"]) val todayCheckouts: Int,
    @SerializedName("pendingMaintenance", alternate = ["pending_maintenance"]) val pendingMaintenance: Int
)

data class RevenueResponse(
    val year: Int,
    val month: Int,
    @SerializedName("totalRevenue", alternate = ["total_revenue"]) val totalRevenue: Long,
    @SerializedName("totalBookings", alternate = ["total_bookings"]) val totalBookings: Int
)

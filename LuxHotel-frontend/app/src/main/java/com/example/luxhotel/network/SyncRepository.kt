package com.example.luxhotel.network

import android.content.Context
import androidx.work.*
import com.example.luxhotel.utils.DatabaseHelper
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SyncRepository(private val context: Context) {
    private val apiService = RetrofitClient.getApiService(context)
    private val dbHelper = DatabaseHelper(context)
    private val gson = Gson()

    sealed class Result {
        object Success : Result()
        object SavedOffline : Result()
        data class Error(val message: String) : Result()
    }

    // 1. Create booking
    suspend fun createBooking(bookingBody: Map<String, Any?>): Result {
        return try {
            val response = apiService.createBooking(bookingBody)
            if (response.isSuccessful) {
                Result.Success
            } else {
                val errorMsg = try {
                    val json = response.errorBody()?.string()
                    val map = gson.fromJson(json, Map::class.java)
                    map["message"]?.toString() ?: "Server error: ${response.code()}"
                } catch (e: Exception) {
                    "Server error: ${response.code()}"
                }
                Result.Error(errorMsg)
            }
        } catch (e: IOException) {
            // Save to sync queue
            dbHelper.addToSyncQueue("CREATE_BOOKING", "0", gson.toJson(bookingBody))
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 2. Check-in booking
    suspend fun checkIn(bookingId: Long): Result {
        return try {
            val response = apiService.checkIn(bookingId)
            if (response.isSuccessful) {
                dbHelper.checkIn(bookingId)
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.checkIn(bookingId)
            dbHelper.addToSyncQueue("CHECKIN", bookingId.toString(), "")
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 2. Check-out booking
    suspend fun checkOut(bookingId: Long): Result {
        return try {
            val response = apiService.checkOut(bookingId)
            if (response.isSuccessful) {
                dbHelper.checkOut(bookingId)
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.checkOut(bookingId)
            dbHelper.addToSyncQueue("CHECKOUT", bookingId.toString(), "")
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 2.5 Add service charge
    suspend fun addServiceCharge(bookingId: Long, name: String, amount: Long): Result {
        val body = mapOf("name" to name, "amount" to amount)
        return try {
            val response = apiService.addServiceCharge(bookingId, body)
            if (response.isSuccessful) {
                // local update if needed, but usually we just refresh from server
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.addToSyncQueue("ADD_SERVICE", bookingId.toString(), gson.toJson(body))
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 3. Update room status
    suspend fun updateRoomStatus(roomNumber: String, status: String): Result {
        val body = mapOf("status" to status)
        return try {
            val response = apiService.updateRoomStatus(roomNumber, body)
            if (response.isSuccessful) {
                dbHelper.updateRoomStatus(roomNumber, status)
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.updateRoomStatus(roomNumber, status)
            dbHelper.addToSyncQueue("ROOM_STATUS", roomNumber, gson.toJson(body))
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 4. Set task done
    suspend fun setTaskDone(taskId: Long, isDone: Boolean): Result {
        val body = mapOf("isDone" to isDone)
        return try {
            val response = apiService.setTaskDone(taskId, body)
            if (response.isSuccessful) {
                dbHelper.setTaskDone(taskId, isDone)
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.setTaskDone(taskId, isDone)
            dbHelper.addToSyncQueue("TASK_DONE", taskId.toString(), gson.toJson(body))
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 5. Update maintenance report status
    suspend fun updateMaintenanceReportStatus(reportId: Long, status: String): Result {
        val body = mapOf("status" to status)
        return try {
            val response = apiService.updateMaintenanceReportStatus(reportId, body)
            if (response.isSuccessful) {
                dbHelper.updateMaintenanceReportStatus(reportId, status)
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.updateMaintenanceReportStatus(reportId, status)
            dbHelper.addToSyncQueue("MAINTENANCE_STATUS", reportId.toString(), gson.toJson(body))
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // 6. Add maintenance report (offline support limited for Multipart, but we can save local and sync text)
    suspend fun addMaintenanceReport(roomNumber: String, description: String, reportedBy: String): Result {
        // Note: Full offline with image requires saving image to local storage and path to DB
        // For now, we sync the text part
        return try {
            val roomNumberBody = roomNumber.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val reportedByBody = reportedBy.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = apiService.addMaintenanceReport(roomNumberBody, descriptionBody, reportedByBody, null)
            if (response.isSuccessful) {
                dbHelper.addMaintenanceReport(roomNumber, description, reportedBy, null)
                Result.Success
            } else {
                Result.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            dbHelper.addMaintenanceReport(roomNumber, description, reportedBy, null)
            val payload = gson.toJson(mapOf("roomNumber" to roomNumber, "description" to description, "reportedBy" to reportedBy))
            dbHelper.addToSyncQueue("ADD_MAINTENANCE", roomNumber, payload)
            triggerSync()
            Result.SavedOffline
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    private fun triggerSync() {
        // This will be called to enqueue the SyncWorker
        // We will implement SyncWorker in the next step.
        // For now, we can write the enqueue code.
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "luxhotel_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}

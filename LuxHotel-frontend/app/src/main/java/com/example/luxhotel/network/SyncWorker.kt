package com.example.luxhotel.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.luxhotel.utils.DatabaseHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val context = applicationContext
        val apiService = RetrofitClient.getApiService(context)
        val dbHelper = DatabaseHelper(context)
        val gson = Gson()

        val items = try {
            dbHelper.getPendingSyncItems()
        } catch (e: Exception) {
            emptyList<DatabaseHelper.SyncQueueItem>()
        }

        if (items.isEmpty()) return Result.success()

        for (item in items) {
            val success = try {
                when (item.actionType) {
                    "CREATE_BOOKING" -> {
                        val type = object : TypeToken<Map<String, Any?>>() {}.type
                        val body: Map<String, Any?> = gson.fromJson(item.payload, type)
                        apiService.createBooking(body).isSuccessful
                    }
                    "CHECKIN" -> {
                        val id = item.targetId.toLongOrNull() ?: -1L
                        if (id != -1L) apiService.checkIn(id).isSuccessful else true
                    }
                    "CHECKOUT" -> {
                        val id = item.targetId.toLongOrNull() ?: -1L
                        if (id != -1L) apiService.checkOut(id).isSuccessful else true
                    }
                    "ADD_SERVICE" -> {
                        val id = item.targetId.toLongOrNull() ?: -1L
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val body: Map<String, Any> = gson.fromJson(item.payload, type)
                        if (id != -1L) apiService.addServiceCharge(id, body).isSuccessful else true
                    }
                    "ROOM_STATUS" -> {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val body: Map<String, String> = gson.fromJson(item.payload, type)
                        apiService.updateRoomStatus(item.targetId, body).isSuccessful
                    }
                    "TASK_DONE" -> {
                        val id = item.targetId.toLongOrNull() ?: -1L
                        val type = object : TypeToken<Map<String, Boolean>>() {}.type
                        val body: Map<String, Boolean> = gson.fromJson(item.payload, type)
                        if (id != -1L) apiService.setTaskDone(id, body).isSuccessful else true
                    }
                    "MAINTENANCE_STATUS" -> {
                        val id = item.targetId.toLongOrNull() ?: -1L
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val body: Map<String, String> = gson.fromJson(item.payload, type)
                        if (id != -1L) apiService.updateMaintenanceReportStatus(id, body).isSuccessful else true
                    }
                    "ADD_MAINTENANCE" -> {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val data: Map<String, String> = gson.fromJson(item.payload, type)
                        val roomNumberBody = data["roomNumber"]?.toRequestBody("text/plain".toMediaTypeOrNull())
                        val descriptionBody = data["description"]?.toRequestBody("text/plain".toMediaTypeOrNull())
                        val reportedByBody = data["reportedBy"]?.toRequestBody("text/plain".toMediaTypeOrNull())
                        
                        if (roomNumberBody != null && descriptionBody != null && reportedByBody != null) {
                            apiService.addMaintenanceReport(roomNumberBody, descriptionBody, reportedByBody, null).isSuccessful
                        } else true
                    }
                    else -> true
                }
            } catch (e: Exception) {
                false
            }

            if (success) {
                dbHelper.markSyncItemDone(item.id)
            } else {
                dbHelper.incrementSyncRetry(item.id)
                return Result.retry()
            }
        }

        return Result.success()
    }
}

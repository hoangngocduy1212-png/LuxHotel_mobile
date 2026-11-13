package com.example.luxhotel.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NetworkHelper {

    private const val TAG = "NetworkHelper"
    private const val EXCHANGE_RATE_URL = "https://api.exchangerate-api.com/v4/latest/USD"
    private const val POST_BOOKING_URL = "https://httpbin.org/post"

    suspend fun fetchUsdToVnd(): Double = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(EXCHANGE_RATE_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = connection.inputStream.bufferedReader()
                val response = reader.readText()
                val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                val rates = jsonObject.getAsJsonObject("rates")
                return@withContext rates.get("VND").asDouble
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching exchange rate", e)
        } finally {
            connection?.disconnect()
        }
        return@withContext 0.0
    }

    suspend fun postBookingConfirmation(guestName: String, roomNumber: String) = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(POST_BOOKING_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val jsonBody = JsonObject().apply {
                addProperty("hotel", "LuxHotel")
                addProperty("guest", guestName)
                addProperty("room", roomNumber)
                addProperty("action", "booking_confirmed")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(Gson().toJson(jsonBody))
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "No error stream"
            }
            
            Log.d(TAG, "Post Booking Response ($responseCode): $responseText")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting booking confirmation", e)
        } finally {
            connection?.disconnect()
        }
    }
}

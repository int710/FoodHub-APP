package com.example.foodhubapp.core.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val FOOD_HUB_BASE_URL = "https://foodhub-8lv1.onrender.com/api/v1"

data class FoodHubApiResponse(
    val message: String,
    val dataCount: Int
)

class FoodHubApiClient(
    private val baseUrl: String = FOOD_HUB_BASE_URL
) {
    fun get(path: String): FoodHubApiResponse {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode !in 200..299) {
                throw IllegalStateException("API lỗi $responseCode: $body")
            }

            val json = JSONObject(body)
            FoodHubApiResponse(
                message = json.optString("message", "Request completed"),
                dataCount = json.opt("data").countItems()
            )
        } finally {
            connection.disconnect()
        }
    }
}

private fun Any?.countItems(): Int = when (this) {
    is JSONArray -> length()
    is JSONObject -> length()
    null, JSONObject.NULL -> 0
    else -> 1
}

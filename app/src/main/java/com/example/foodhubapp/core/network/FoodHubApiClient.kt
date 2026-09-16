package com.example.foodhubapp.core.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Địa chỉ cơ sở (Base URL) mặc định cho các yêu cầu API của ứng dụng FoodHub.
 */
private const val FOOD_HUB_BASE_URL = "https://foodhub-8lv1.onrender.com/api/v1"

/**
 * Đại diện cho cấu trúc phản hồi chuẩn từ API FoodHub.
 *
 * @property message Thông điệp phản hồi từ máy chủ.
 * @property dataCount Số lượng phần tử dữ liệu trả về (nếu có).
 */
data class FoodHubApiResponse(
    val message: String,
    val dataCount: Int
)

/**
 * Client HTTP gọn nhẹ sử dụng [HttpURLConnection] để thực hiện các yêu cầu GET và POST
 * đến máy chủ FoodHub API.
 *
 * @property baseUrl Đường dẫn cơ sở của API, mặc định là [FOOD_HUB_BASE_URL].
 */
class FoodHubApiClient(
    private val baseUrl: String = FOOD_HUB_BASE_URL
) {
    /**
     * Thực hiện yêu cầu HTTP GET đến một đường dẫn API cụ thể.
     *
     * @param path Đường dẫn phụ (endpoint path) nối tiếp vào baseUrl.
     * @return Đối tượng [FoodHubApiResponse] chứa thông tin phản hồi.
     * @throws IllegalStateException nếu phản hồi trả về mã lỗi HTTP.
     */
    fun get(path: String): FoodHubApiResponse {
        // Thiết lập kết nối HTTP URL Connection
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000 // Thời gian chờ kết nối (15 giây)
        connection.readTimeout = 15_000    // Thời gian chờ đọc dữ liệu (15 giây)
        connection.setRequestProperty("Accept", "application/json")

        return try {
            val responseCode = connection.responseCode
            // Chọn luồng đọc dữ liệu dựa trên mã phản hồi (Thành công hay Lỗi)
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            // Nếu mã trạng thái không nằm trong khoảng thành công, ném ngoại lệ với nội dung lỗi
            if (responseCode !in 200..299) {
                throw IllegalStateException(parseApiError(responseCode, body))
            }

            // Phân tích cú pháp JSON phản hồi
            val json = JSONObject(body)
            FoodHubApiResponse(
                message = json.optString("message", "Request completed"),
                dataCount = json.opt("data").countItems()
            )
        } finally {
            // Đảm bảo ngắt kết nối HTTP
            connection.disconnect()
        }
    }

    /**
     * Thực hiện yêu cầu HTTP POST gửi dữ liệu JSON lên máy chủ.
     *
     * @param path Đường dẫn phụ (endpoint path) nối tiếp vào baseUrl.
     * @param body Đối tượng [JSONObject] chứa dữ liệu yêu cầu.
     * @return Đối tượng [JSONObject] chứa phản hồi từ máy chủ.
     * @throws IllegalStateException nếu phản hồi trả về mã lỗi HTTP.
     */
    fun post(path: String, body: JSONObject): JSONObject {
        // Thiết lập kết nối HTTP URL Connection cho phương thức POST
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")

        return try {
            // Ghi dữ liệu JSON vào luồng đầu ra của kết nối
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val responseCode = connection.responseCode
            // Chọn luồng đọc tương ứng
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            // Kiểm tra lỗi phản hồi từ API
            if (responseCode !in 200..299) {
                throw IllegalStateException(parseApiError(responseCode, responseBody))
            }

            JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Thực hiện yêu cầu GET và trả về toàn bộ JSON response để Repository tự parse dữ liệu.
     *
     * @param path Endpoint cần gọi.
     * @param accessToken Token đăng nhập, dùng cho các API yêu cầu xác thực.
     */
    fun getJson(path: String, accessToken: String? = null): JSONObject {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            if (responseCode !in 200..299) {
                throw IllegalStateException(parseApiError(responseCode, body))
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

}

/**
 * Phân tích thông điệp lỗi trả về từ phản hồi API khi gặp sự cố (mã lỗi HTTP khác 2xx).
 *
 * @param responseCode Mã trạng thái HTTP (ví dụ: 400, 401, 500).
 * @param body Nội dung chuỗi phản hồi từ máy chủ.
 * @return Thông điệp lỗi đã được định dạng rõ ràng cho người dùng.
 */
private fun parseApiError(responseCode: Int, body: String): String {
    return runCatching {
        val json = JSONObject(body)
        val errors = json.optJSONObject("errors")

        // Nếu có danh sách lỗi chi tiết theo trường, ghép nối chúng lại
        if (errors != null && errors.length() > 0) {
            errors.keys().asSequence()
                .mapNotNull { key -> errors.optString(key).ifBlank { null } }
                .joinToString(separator = "\n")
        } else {
            // Nếu không, lấy thông điệp chung hoặc thông báo mặc định
            json.optString("message").ifBlank {
                "API lỗi $responseCode"
            }
        }
    }.getOrDefault("API lỗi $responseCode")
}

/**
 * Hàm mở rộng (Extension function) giúp đếm số lượng phần tử của một đối tượng bất kỳ từ phản hồi API
 * (hỗ trợ [JSONArray], [JSONObject], null hoặc các kiểu dữ liệu khác).
 *
 * @return Số lượng phần tử hoặc 0/1 tùy thuộc vào kiểu dữ liệu.
 */
private fun Any?.countItems(): Int = when (this) {
    is JSONArray -> length()
    is JSONObject -> length()
    null, JSONObject.NULL -> 0
    else -> 1
}

package com.example.foodhubapp.core.network

import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

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
 * Client HTTP gọn nhẹ sử dụng OkHttp để thực hiện các yêu cầu API
 * đến máy chủ FoodHub API.
 *
 * @property baseUrl Đường dẫn cơ sở của API, mặc định là [FOOD_HUB_BASE_URL].
 */
class FoodHubApiClient(
    private val baseUrl: String = FOOD_HUB_BASE_URL,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Thực hiện yêu cầu HTTP GET đến một đường dẫn API cụ thể.
     *
     * @param path Đường dẫn phụ (endpoint path) nối tiếp vào baseUrl.
     * @return Đối tượng [FoodHubApiResponse] chứa thông tin phản hồi.
     * @throws IllegalStateException nếu phản hồi trả về mã lỗi HTTP.
     */
    fun get(path: String): FoodHubApiResponse {
        val json = getJson(path)
        return FoodHubApiResponse(
            message = json.optString("message", "Request completed"),
            dataCount = json.opt("data").countItems()
        )
    }

    fun getJson(path: String, headers: Map<String, String> = emptyMap()): JSONObject =
        request("GET", path, null, headers)

    /**
     * Thực hiện yêu cầu HTTP POST gửi dữ liệu JSON lên máy chủ.
     *
     * @param path Đường dẫn phụ (endpoint path) nối tiếp vào baseUrl.
     * @param body Đối tượng [JSONObject] chứa dữ liệu yêu cầu.
     * @return Đối tượng [JSONObject] chứa phản hồi từ máy chủ.
     * @throws IllegalStateException nếu phản hồi trả về mã lỗi HTTP.
     */
    fun post(path: String, body: JSONObject, headers: Map<String, String> = emptyMap()): JSONObject =
        request("POST", path, body, headers)

    /** Dùng cho cập nhật từng phần, ví dụ sửa quantity/options/note của cart item. */
    fun patch(path: String, body: JSONObject, headers: Map<String, String> = emptyMap()): JSONObject =
        request("PATCH", path, body, headers)

    /** DELETE không gửi body; endpoint cart dùng hàm này cho xóa dòng và clear. */
    fun delete(path: String, headers: Map<String, String> = emptyMap()): JSONObject =
        request("DELETE", path, null, headers)

    /** Điểm chung xử lý header, JSON, HTTP status và lỗi cho mọi method. */
    private fun request(method: String, path: String, body: JSONObject?, headers: Map<String, String>): JSONObject {
        val requestBody = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl$path")
            .header("Accept", "application/json")
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .method(method, requestBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val responseCode = response.code
            val responseBody = response.body?.string().orEmpty()
            if (responseCode !in 200..299) {
                throw FoodHubApiException(responseCode, parseApiError(responseCode, responseBody))
            }
            // DELETE thành công thường trả 204 No Content; coi đó là JSON rỗng hợp lệ.
            if (responseBody.isBlank()) return JSONObject()
            if (!responseBody.trimStart().startsWith("{")) {
                throw IllegalStateException("Máy chủ chưa trả dữ liệu JSON. Vui lòng thử lại sau.")
            }
            JSONObject(responseBody)
        }
    }
}

class FoodHubApiException(val statusCode: Int, message: String) : IllegalStateException(message)

/**
 * Phân tích thông điệp lỗi trả về từ phản hồi API khi gặp sự cố (mã lỗi HTTP khác 2xx).
 *
 * @param responseCode Mã trạng thái HTTP (ví dụ: 400, 401, 500).
 * @param body Nội dung chuỗi phản hồi từ máy chủ.
 * @return Thông điệp lỗi đã được định dạng rõ ràng cho người dùng.
 */
private fun parseApiError(responseCode: Int, body: String): String {
    if (body.trimStart().startsWith("<")) {
        return "Máy chủ đang chặn yêu cầu hoặc chưa sẵn sàng (HTTP $responseCode). Vui lòng thử lại sau."
    }
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

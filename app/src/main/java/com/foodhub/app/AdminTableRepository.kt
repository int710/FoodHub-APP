package com.foodhub.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AdminTableApiException(message: String) : IOException(message)

class AdminTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("admin_session", Context.MODE_PRIVATE)

    fun token(): String = preferences.getString(KEY_ACCESS_TOKEN, "").orEmpty()

    fun save(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token.trim().removePrefix("Bearer ")).apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}

class AdminTableRepository(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val tokenStore = AdminTokenStore(context.applicationContext)

    fun savedToken(): String = tokenStore.token()

    fun saveToken(token: String) = tokenStore.save(token)

    suspend fun getTables(): List<AdminRestaurantTable> = withContext(Dispatchers.IO) {
        val request = authorizedRequest("$API_BASE/table").get().build()
        execute(request) { root ->
            val data = root.opt("data")
            val array = when (data) {
                is JSONArray -> data
                is JSONObject -> data.firstArray("tables", "items", "results", "data") ?: JSONArray()
                else -> root.firstArray("tables", "items", "results") ?: JSONArray()
            }
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toAdminTable()?.let(::add)
                }
            }
        }
    }

    suspend fun createTable(
        name: String,
        capacity: Int,
        floor: String?,
        note: String?,
    ): AdminRestaurantTable = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("capacity", capacity)
            .put("floor", floor?.trim()?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
            .put("note", note?.trim()?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
        val request = authorizedRequest("$API_BASE/table")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request) { root ->
            val data = root.optJSONObject("data") ?: root
            val table = data.optJSONObject("table") ?: data
            table.toAdminTable() ?: throw AdminTableApiException("Backend không trả về thông tin bàn vừa tạo")
        }
    }

    suspend fun getQrContent(tableId: String): String = withContext(Dispatchers.IO) {
        val request = authorizedRequest("$API_BASE/table/$tableId/qr").get().build()
        execute(request) { root ->
            val data = root.opt("data")
            val content = when (data) {
                is String -> data.takeIf(String::isNotBlank)
                is JSONObject -> data.firstString("qrContent", "content", "qrUrl", "url", "qrToken", "token")
                else -> null
            } ?: root.firstString("qrContent", "content", "qrUrl", "url", "qrToken", "token")
            content ?: throw AdminTableApiException("Backend chưa trả về nội dung QR")
        }
    }

    private fun authorizedRequest(url: String): Request.Builder {
        val token = tokenStore.token()
        if (token.isBlank()) throw AdminTableApiException("Vui lòng nhập access token của Admin")
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
    }

    private fun <T> execute(request: Request, transform: (JSONObject) -> T): T {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val root = runCatching { JSONObject(text) }.getOrNull() ?: JSONObject()
            if (!response.isSuccessful) {
                val fallback = when (response.code) {
                    401, 403 -> "Access token không hợp lệ hoặc không có quyền quản lý bàn"
                    404 -> "Không tìm thấy API hoặc bàn yêu cầu"
                    else -> "Không thể kết nối API quản lý bàn (${response.code})"
                }
                throw AdminTableApiException(root.firstString("message", "error") ?: fallback)
            }
            return transform(root)
        }
    }

    private fun JSONObject.toAdminTable(): AdminRestaurantTable? {
        val id = firstString("id", "_id", "tableId") ?: return null
        return AdminRestaurantTable(
            id = id,
            name = firstString("name", "tableName") ?: "Bàn",
            capacity = optInt("capacity", 2),
            floor = firstString("floor"),
            isActive = if (has("isActive")) optBoolean("isActive") else true,
            note = firstString("note"),
        )
    }

    private fun JSONObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
        }

    private fun JSONObject.firstArray(vararg keys: String): JSONArray? =
        keys.firstNotNullOfOrNull { key -> optJSONArray(key) }

    private companion object {
        const val API_BASE = "https://foodhub-8lv1.onrender.com/api/v1"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

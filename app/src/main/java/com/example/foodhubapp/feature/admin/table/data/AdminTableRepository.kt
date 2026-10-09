package com.example.foodhubapp.feature.admin.table.data

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.admin.model.AdminRestaurantTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AdminTableApiException(message: String) : IOException(message)

class AdminTableRepository internal constructor(
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodHubApiClient()) : this(
        apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )

    suspend fun getTables(): List<AdminRestaurantTable> = withContext(Dispatchers.IO) {
        val root = apiClient.getJson("/table", authHeaders())
        val array = when (val data = root.opt("data")) {
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

    suspend fun getTable(tableId: String): AdminRestaurantTable = withContext(Dispatchers.IO) {
        val root = apiClient.getJson("/table/$tableId", authHeaders())
        root.optJSONObject("data")?.toAdminTable()
            ?: throw AdminTableApiException("Không thể đọc thông tin bàn")
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
        val root = apiClient.post("/table/new", payload, authHeaders())
        val data = root.optJSONObject("data") ?: root
        val table = data.optJSONObject("table") ?: data
        table.toAdminTable() ?: throw AdminTableApiException("Backend không trả về thông tin bàn vừa tạo")
    }

    suspend fun getQrContent(tableId: String): String = withContext(Dispatchers.IO) {
        val root = apiClient.getJson("/table/$tableId/qr", authHeaders())
        val content = when (val data = root.opt("data")) {
            is String -> data.takeIf(String::isNotBlank)
            is JSONObject -> data.firstString("qrToken", "token", "qrContent", "content", "qrUrl", "url")
            else -> null
        } ?: root.firstString("qrToken", "token", "qrContent", "content", "qrUrl", "url")
        content ?: throw AdminTableApiException("Backend chưa trả về nội dung QR")
    }

    suspend fun toggleTable(tableId: String): Boolean = withContext(Dispatchers.IO) {
        val root = apiClient.patch("/table/$tableId/toggle", JSONObject(), authHeaders())
        val data = root.optJSONObject("data") ?: root
        data.optBoolean("isActive")
    }

    suspend fun regenerateQrContent(tableId: String): String = withContext(Dispatchers.IO) {
        val root = apiClient.post("/table/$tableId/regenerate-qr", JSONObject(), authHeaders())
        val data = root.optJSONObject("data") ?: root
        data.firstString("qrToken", "token", "qrContent", "content")
            ?: throw AdminTableApiException("Backend chưa trả về mã QR mới")
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw AdminTableApiException("Phiên đăng nhập không tồn tại. Vui lòng đăng nhập lại.")
        return mapOf("Authorization" to "Bearer $token")
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

}

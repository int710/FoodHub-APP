package com.example.foodhubapp.feature.admin.table.data

import com.example.foodhubapp.core.network.*
import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.admin.model.AdminRestaurantTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.IOException

class AdminTableApiException(message: String) : IOException(message)

class AdminTableRepository internal constructor(
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient) : this(
        apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )

    suspend fun getTables(): List<AdminRestaurantTable> = withContext(Dispatchers.IO) {
        val root = apiClient.execute(
            apiClient.adminTableApi.getTables(
                headers = authHeaders()
            )
        )
        val array = when (val data = root.opt("data")) {
            is JsonArray -> data
            is JsonObject -> data.firstArray("tables", "items", "results", "data") ?: JsonArray()
            else -> root.firstArray("tables", "items", "results") ?: JsonArray()
        }
        buildList {
            for (index in 0 until array.size()) {
                array.optObject(index)?.toAdminTable()?.let(::add)
            }
        }
    }

    suspend fun getTable(tableId: String): AdminRestaurantTable = withContext(Dispatchers.IO) {
        val root = apiClient.execute(
            apiClient.adminTableApi.getTable(
                id = tableId,
                headers = authHeaders()
            )
        )
        root.optObject("data")?.toAdminTable()
            ?: throw AdminTableApiException("Không thể đọc thông tin bàn")
    }

    suspend fun createTable(
        name: String,
        capacity: Int,
        floor: String?,
        note: String?,
    ): AdminRestaurantTable = withContext(Dispatchers.IO) {
        val payload = JsonObject()
            .put("name", name.trim())
            .put("capacity", capacity)
            .put("floor", floor?.trim()?.takeIf(String::isNotBlank) ?: com.google.gson.JsonNull.INSTANCE)
            .put("note", note?.trim()?.takeIf(String::isNotBlank) ?: com.google.gson.JsonNull.INSTANCE)
        val root = apiClient.execute(
            apiClient.adminTableApi.createTable(
                headers = authHeaders(),
                body = payload
            )
        )
        val data = root.optObject("data") ?: root
        val table = data.optObject("table") ?: data
        table.toAdminTable() ?: throw AdminTableApiException("Backend không trả về thông tin bàn vừa tạo")
    }

    suspend fun getQrContent(tableId: String): String = withContext(Dispatchers.IO) {
        val root = apiClient.execute(
            apiClient.adminTableApi.getQrContent(
                id = tableId,
                headers = authHeaders()
            )
        )
        val content = when (val data = root.opt("data")) {
            is String -> data.takeIf(String::isNotBlank)
            is JsonObject -> data.firstString("qrToken", "token", "qrContent", "content", "qrUrl", "url")
            else -> null
        } ?: root.firstString("qrToken", "token", "qrContent", "content", "qrUrl", "url")
        content ?: throw AdminTableApiException("Backend chưa trả về nội dung QR")
    }

    suspend fun toggleTable(tableId: String): Boolean = withContext(Dispatchers.IO) {
        val root = apiClient.execute(
            apiClient.adminTableApi.toggleTable(
                id = tableId,
                headers = authHeaders(),
                body = JsonObject()
            )
        )
        val data = root.optObject("data") ?: root
        data.optBoolean("isActive")
    }

    suspend fun deleteTable(tableId: String) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.adminTableApi.deleteTable(
                id = tableId,
                headers = authHeaders()
            )
        )
        Unit
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw AdminTableApiException("Phiên đăng nhập không tồn tại. Vui lòng đăng nhập lại.")
        return mapOf("Authorization" to "Bearer $token")
    }

    suspend fun regenerateQrContent(tableId: String): String = withContext(Dispatchers.IO) {
        val root = apiClient.execute(apiClient.adminTableApi.regenerateQrContent(
            id = tableId, headers = authHeaders(), body = JsonObject()
        ))
        val data = root.optObject("data") ?: root
        data.firstString("qrToken", "token", "qrContent", "content")
            ?: throw AdminTableApiException("Backend chưa trả về mã QR mới")
    }

    private fun JsonObject.toAdminTable(): AdminRestaurantTable? {
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

    private fun JsonObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
        }

    private fun JsonObject.firstArray(vararg keys: String): JsonArray? =
        keys.firstNotNullOfOrNull { key -> optArray(key) }

}

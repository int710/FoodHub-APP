package com.example.foodhubapp.feature.table.data

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.table.AdminConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdminConversationRepository(
    context: Context,
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
) {
    private val tokenStore = TokenStore(context.applicationContext)

    suspend fun getOpenConversations(): List<AdminConversation> = withContext(Dispatchers.IO) {
        val response = apiClient.getJson("/conversations", headers())
        (response.optJSONArray("data") ?: JSONArray()).objects().map(JSONObject::toAdminConversation)
    }

    suspend fun close(id: String) = withContext(Dispatchers.IO) {
        apiClient.patch("/conversations/$id/close", JSONObject(), headers())
        Unit
    }

    private suspend fun headers(): Map<String, String> {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank)
            ?: error("Vui lòng đăng nhập lại")
        return mapOf("Authorization" to "Bearer $token")
    }
}

internal fun JSONObject.toAdminConversation() = AdminConversation(
    id = optString("_id", optString("id")),
    customer = optString("customerName", "Khách hàng"),
    lastMessage = optString("lastMessage", "Chưa có tin nhắn"),
    time = formatConversationTime(optString("lastMessageAt", optString("updatedAt"))),
    unread = if (optString("lastMessageSenderId") != optString("assignedHostId")) 1 else 0,
    isOnline = true,
)

private fun formatConversationTime(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault(value.take(5))

private fun JSONArray.objects() = (0 until length()).mapNotNull(::optJSONObject)

package com.example.foodhubapp.feature.admin.chat.data

import com.example.foodhubapp.core.network.*
import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.admin.chat.model.AdminConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdminConversationRepository internal constructor(
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient) : this(
        apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )

    suspend fun getOpenConversations(): List<AdminConversation> = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.conversationApi.getConversations(
                headers = headers()
            )
        )
        (response.optArray("data") ?: JsonArray()).objects()
            .map(JsonObject::toAdminConversation)
    }

    suspend fun close(id: String) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.conversationApi.closeConversation(
                id = id,
                headers = headers(),
                body = JsonObject()
            )
        )
        Unit
    }

    private suspend fun headers(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: error("Vui lòng đăng nhập lại")
        return mapOf("Authorization" to "Bearer $token")
    }
}

internal fun JsonObject.toAdminConversation() = AdminConversation(
    id = optString("_id", optString("id")),
    customer = optString("customerName", "Khách hàng"),
    lastMessage = optString("lastMessage", "Chưa có tin nhắn"),
    time = formatConversationTime(optString("lastMessageAt", optString("updatedAt"))),
    unread = if (optString("lastMessageSenderId") != optString("assignedHostId")) 1 else 0,
    isOnline = true,
    orderCode = optString("orderCode").takeIf(String::isNotBlank),
    conversationType = optString("conversationType", "USER"),
    contextLabel = optString("contextLabel", "Tài khoản khách hàng"),
)

private fun formatConversationTime(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        .format(Instant.parse(value))
}.getOrDefault(value.take(5))

private fun JsonArray.objects() = (0 until size()).mapNotNull(::optObject)

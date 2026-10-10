package com.example.foodhubapp.feature.shared.notification.data

import com.example.foodhubapp.core.network.*
import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class FoodHubNotification(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val orderId: String?,
    val orderCode: String?,
    val readAt: String?,
    val createdAt: String,
) {
    val isRead: Boolean get() = !readAt.isNullOrBlank()
}

class NotificationRepository internal constructor(
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient) : this(
        apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )

    suspend fun getNotifications(unreadOnly: Boolean): List<FoodHubNotification> = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.notificationApi.getNotifications(
                unreadOnly = unreadOnly,
                headers = authHeaders()
            )
        )
        (response.optArray("data") ?: JsonArray()).objects().map { it.toNotification() }
    }

    suspend fun getUnreadCount(): Int = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.notificationApi.getUnreadCount(
                headers = authHeaders()
            )
        )
            .optObject("data")
            ?.optInt("count", 0)
            ?: 0
    }

    suspend fun markAsRead(id: String) = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.notificationApi.markAsRead(
                id = id,
                headers = authHeaders(),
                body = JsonObject()
            )
        )
        Unit
    }

    suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        apiClient.execute(
            apiClient.notificationApi.markAllAsRead(
                headers = authHeaders(),
                body = JsonObject()
            )
        )
        Unit
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Vui lòng đăng nhập để xem thông báo")
        return mapOf("Authorization" to "Bearer $token")
    }
}

private fun JsonObject.toNotification() = FoodHubNotification(
    id = optString("_id", optString("id")),
    type = optString("type"),
    title = optString("title", "Thông báo"),
    message = optString("message"),
    orderId = optionalString("orderId"),
    orderCode = optionalString("orderCode"),
    readAt = optionalString("readAt"),
    createdAt = optString("createdAt"),
)

private fun JsonObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

private fun JsonArray.objects(): List<JsonObject> =
    (0 until size()).mapNotNull(::optObject)

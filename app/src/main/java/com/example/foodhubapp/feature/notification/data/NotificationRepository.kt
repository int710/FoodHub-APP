package com.example.foodhubapp.feature.notification.data

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

class NotificationRepository(
    context: Context,
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
) {
    private val tokenStore = TokenStore(context.applicationContext)

    suspend fun getNotifications(unreadOnly: Boolean): List<FoodHubNotification> = withContext(Dispatchers.IO) {
        val response = apiClient.getJson(
            "/notifications?page=1&limit=50&unreadOnly=$unreadOnly",
            authHeaders(),
        )
        (response.optJSONArray("data") ?: JSONArray()).objects().map { it.toNotification() }
    }

    suspend fun getUnreadCount(): Int = withContext(Dispatchers.IO) {
        apiClient.getJson("/notifications/unread-count", authHeaders())
            .optJSONObject("data")
            ?.optInt("count", 0)
            ?: 0
    }

    suspend fun markAsRead(id: String) = withContext(Dispatchers.IO) {
        apiClient.patch("/notifications/$id/read", JSONObject(), authHeaders())
        Unit
    }

    suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        apiClient.patch("/notifications/read-all", JSONObject(), authHeaders())
        Unit
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Vui lòng đăng nhập để xem thông báo")
        return mapOf("Authorization" to "Bearer $token")
    }
}

private fun JSONObject.toNotification() = FoodHubNotification(
    id = optString("_id", optString("id")),
    type = optString("type"),
    title = optString("title", "Thông báo"),
    message = optString("message"),
    orderId = optionalString("orderId"),
    orderCode = optionalString("orderCode"),
    readAt = optionalString("readAt"),
    createdAt = optString("createdAt"),
)

private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

private fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull(::optJSONObject)

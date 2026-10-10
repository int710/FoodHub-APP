package com.example.foodhubapp.feature.shared.notification.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface NotificationApi {

    @GET("notifications")
    fun getNotifications(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Query("unreadOnly") unreadOnly: Boolean,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Call<JsonObject>

    @GET("notifications/unread-count")
    fun getUnreadCount(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @PATCH("notifications/{id}/read")
    fun markAsRead(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Path("id", encoded = true) id: String,
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>

    @PATCH("notifications/read-all")
    fun markAllAsRead(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: JsonObject = JsonObject()
    ): Call<JsonObject>
}

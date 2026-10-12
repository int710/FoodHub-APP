package com.kma.foodhub.feature.admin.chat.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

/** Endpoint contracts; repositories perform blocking calls on Dispatchers.IO. */
interface ConversationApi {

    @GET("conversations")
    fun getConversations(
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Call<JsonObject>

    @DELETE("conversations/{id}")
    fun deleteConversation(
        @HeaderMap headers: Map<String, String>,
        @Path("id") id: String,
    ): Call<JsonObject>

}

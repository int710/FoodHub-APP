package com.example.foodhubapp.feature.admin.chat.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ConversationApi {
    @GET("conversations")
    suspend fun getConversations(
        @Header("Authorization") authorization : String
    ): ConversationListResponse
    @PATCH("conversations/{id}/close")
    suspend fun closeConversation(
        @Header("Authorization") authorization : String,
        @Path("id") conversationId : String
    ) : ConversationListResponse
}
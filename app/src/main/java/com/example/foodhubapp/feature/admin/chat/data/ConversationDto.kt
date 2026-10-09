package com.example.foodhubapp.feature.admin.chat.data

import com.google.gson.annotations.SerializedName

data class ConversationListResponse(
    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<ConversationDto>?
)

data class ConversationDto(
    @SerializedName("_id", alternate = ["id"])
    val id: String?,

    @SerializedName("customerName")
    val customerName: String?,

    @SerializedName("lastMessage")
    val lastMessage: String?,

    @SerializedName("lastMessageAt")
    val lastMessageAt: String?,

    @SerializedName("updatedAt", alternate = ["updateAt"])
    val updatedAt: String?,

    @SerializedName("lastMessageSenderId")
    val lastMessageSenderId: String?,

    @SerializedName("assignedHostId", alternate = ["assignHostId"])
    val assignedHostId: String?
)
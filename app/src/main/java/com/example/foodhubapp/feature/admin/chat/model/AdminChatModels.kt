package com.example.foodhubapp.feature.admin.chat.model

data class AdminConversation(
    val id: String,
    val customer: String,
    val lastMessage: String,
    val time: String,
    val unread: Int,
    val isOnline: Boolean,
    val orderCode: String? = null,
    val conversationType: String = "USER",
    val contextLabel: String = "Tài khoản khách hàng",
)

val AdminConversation.isTableSession: Boolean
    get() = conversationType.equals("TABLE", ignoreCase = true)

data class AdminUiMessage(
    val id: String,
    val content: String,
    val time: String,
    val fromAdmin: Boolean,
)

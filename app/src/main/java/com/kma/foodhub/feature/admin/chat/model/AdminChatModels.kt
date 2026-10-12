package com.kma.foodhub.feature.admin.chat.model

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
    val status: String = "OPEN",
)

val AdminConversation.isTableSession: Boolean
    get() = conversationType.equals("TABLE", ignoreCase = true)

val AdminConversation.isClosed: Boolean
    get() = status.equals("CLOSED", ignoreCase = true)

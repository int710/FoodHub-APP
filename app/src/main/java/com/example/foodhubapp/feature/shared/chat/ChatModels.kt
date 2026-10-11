package com.example.foodhubapp.feature.shared.chat

import org.json.JSONArray
import org.json.JSONObject

enum class ChatConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderRole: String,
    val content: String,
    val type: String,
    val createdAt: String,
) {
    val isCustomer: Boolean get() = senderRole.equals("customer", ignoreCase = true)
}

interface ChatSocketListener {
    fun onConnectionStateChanged(state: ChatConnectionState)
    fun onHistory(messages: List<ChatMessage>)
    fun onMessage(message: ChatMessage)
    fun onTyping(isTyping: Boolean)
    fun onError(message: String)
    fun onSessionEnded() {}
}

fun JSONObject.toChatMessage(): ChatMessage? {
    val content = optStringValue("content") ?: return null
    return ChatMessage(
        id = optStringValue("_id", "id")
            ?: "${optStringValue("senderId")}-${optStringValue("createdAt")}-$content",
        conversationId = optStringValue("conversationId").orEmpty(),
        senderId = optStringValue("senderId").orEmpty(),
        senderRole = optStringValue("senderRole") ?: "customer",
        content = content,
        type = optStringValue("type") ?: "TEXT",
        createdAt = optStringValue("createdAt").orEmpty(),
    )
}

fun JSONArray?.toChatMessages(): List<ChatMessage> = if (this == null) {
    emptyList()
} else {
    buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.toChatMessage()?.let(::add)
        }
    }
}

fun JSONObject.optStringValue(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
    }


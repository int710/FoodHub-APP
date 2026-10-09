package com.example.foodhubapp.feature.shared.chat

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


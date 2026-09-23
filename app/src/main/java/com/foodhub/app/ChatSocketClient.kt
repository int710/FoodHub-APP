package com.foodhub.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

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
}

class ChatSocketClient(context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionStore = TableSessionStore(context.applicationContext)
    private var listener: ChatSocketListener? = null
    private var socket: Socket? = null

    private val onConnect = Emitter.Listener {
        dispatch {
            listener?.onConnectionStateChanged(ChatConnectionState.CONNECTED)
            sessionStore.conversationId()?.let(::joinConversation)
        }
    }
    private val onDisconnect = Emitter.Listener {
        dispatch { listener?.onConnectionStateChanged(ChatConnectionState.DISCONNECTED) }
    }
    private val onConnectError = Emitter.Listener { args ->
        val message = args.firstOrNull()?.toString()?.takeIf(String::isNotBlank)
            ?: "Không thể kết nối kênh hỗ trợ"
        dispatch {
            listener?.onConnectionStateChanged(ChatConnectionState.ERROR)
            listener?.onError(message)
        }
    }
    private val onNewMessage = Emitter.Listener { args ->
        val message = (args.firstOrNull() as? JSONObject)?.toChatMessage() ?: return@Listener
        dispatch { listener?.onMessage(message) }
    }
    private val onHistory = Emitter.Listener { args ->
        val payload = args.firstOrNull() as? JSONObject ?: return@Listener
        val conversationId = payload.optStringValue("conversationId")
        if (conversationId != null) sessionStore.saveConversationId(conversationId)
        val messages = payload.optJSONArray("messages").toChatMessages()
        dispatch { listener?.onHistory(messages) }
    }
    private val onTypingStart = Emitter.Listener {
        dispatch { listener?.onTyping(true) }
    }
    private val onTypingStop = Emitter.Listener {
        dispatch { listener?.onTyping(false) }
    }

    fun connect(listener: ChatSocketListener) {
        this.listener = listener
        val tableToken = sessionStore.current()?.tableToken
        if (tableToken.isNullOrBlank()) {
            listener.onConnectionStateChanged(ChatConnectionState.ERROR)
            listener.onError("Chưa có phiên bàn. Vui lòng quét QR trước khi mở chat.")
            return
        }

        if (socket?.connected() == true) {
            listener.onConnectionStateChanged(ChatConnectionState.CONNECTED)
            return
        }

        listener.onConnectionStateChanged(ChatConnectionState.CONNECTING)
        val options = IO.Options.builder()
            .setAuth(mapOf("tableToken" to tableToken))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on(Socket.EVENT_CONNECT, onConnect)
            on(Socket.EVENT_DISCONNECT, onDisconnect)
            on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            on(EVENT_MESSAGE_NEW, onNewMessage)
            on(EVENT_MESSAGE_HISTORY, onHistory)
            on(EVENT_TYPING_START, onTypingStart)
            on(EVENT_TYPING_STOP, onTypingStop)
            connect()
        }
    }

    fun disconnect() {
        socket?.apply {
            off()
            disconnect()
        }
        socket = null
        listener = null
    }

    fun sendMessage(content: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val activeSocket = socket
        if (activeSocket?.connected() != true) {
            onResult(false, "Kênh chat đang mất kết nối")
            return
        }

        val payload = JSONObject()
            .put("content", content.trim())
            .put("type", "TEXT")
        sessionStore.conversationId()?.let { payload.put("conversationId", it) }

        activeSocket.emit(EVENT_MESSAGE_SEND, payload, Ack { args ->
            val response = args.firstOrNull() as? JSONObject
            val success = response?.optBoolean("success", false) == true
            val data = response?.optJSONObject("data")
            val conversationId = data?.optStringValue("conversationId")
            if (success && conversationId != null) {
                sessionStore.saveConversationId(conversationId)
            }
            data?.optJSONObject("message")?.toChatMessage()?.let { message ->
                dispatch { listener?.onMessage(message) }
            }
            val error = response?.optStringValue("message")
            dispatch { onResult(success, error) }
        })
    }

    fun setTyping(typing: Boolean) {
        val conversationId = sessionStore.conversationId() ?: return
        val event = if (typing) EVENT_TYPING_START else EVENT_TYPING_STOP
        socket?.takeIf(Socket::connected)?.emit(
            event,
            JSONObject().put("conversationId", conversationId),
        )
    }

    private fun joinConversation(conversationId: String) {
        val payload = JSONObject()
            .put("conversationId", conversationId)
            .put("page", 1)
            .put("limit", 50)
        socket?.emit(EVENT_CONVERSATION_JOIN, payload, Ack { args ->
            val response = args.firstOrNull() as? JSONObject ?: return@Ack
            if (!response.optBoolean("success", false)) {
                dispatch {
                    listener?.onError(
                        response.optStringValue("message") ?: "Không thể tải lịch sử trò chuyện",
                    )
                }
            }
        })
    }

    private fun dispatch(block: () -> Unit) {
        mainHandler.post(block)
    }

    private fun JSONObject.toChatMessage(): ChatMessage? {
        val content = optStringValue("content") ?: return null
        return ChatMessage(
            id = optStringValue("_id", "id") ?: "${optStringValue("senderId")}-${optStringValue("createdAt")}-$content",
            conversationId = optStringValue("conversationId").orEmpty(),
            senderId = optStringValue("senderId").orEmpty(),
            senderRole = optStringValue("senderRole") ?: "customer",
            content = content,
            type = optStringValue("type") ?: "TEXT",
            createdAt = optStringValue("createdAt").orEmpty(),
        )
    }

    private fun JSONArray?.toChatMessages(): List<ChatMessage> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.toChatMessage()?.let(::add)
            }
        }
    }

    private fun JSONObject.optStringValue(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
        }

    private companion object {
        const val SOCKET_URL = "https://foodhub-8lv1.onrender.com"
        const val EVENT_CONVERSATION_JOIN = "conversation:join"
        const val EVENT_MESSAGE_SEND = "message:send"
        const val EVENT_MESSAGE_NEW = "message:new"
        const val EVENT_MESSAGE_HISTORY = "message:history"
        const val EVENT_TYPING_START = "typing:start"
        const val EVENT_TYPING_STOP = "typing:stop"
    }
}

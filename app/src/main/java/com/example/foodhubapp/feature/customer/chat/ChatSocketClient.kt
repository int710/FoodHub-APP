package com.example.foodhubapp.feature.customer.chat

import com.example.foodhubapp.feature.shared.chat.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubSessionRefresh
import com.example.foodhubapp.feature.customer.table.data.TableSessionStore
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

class ChatSocketClient(context: Context) {
    private val tokenStore = TokenStore(context.applicationContext)
    private val conversations = context.getSharedPreferences("customer_chat", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connectionJob: Job? = null
    private var accountId: String? = null
    private var accountAccessToken: String? = null
    private var usingTable = true
    private var triedRefresh = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionStore = TableSessionStore(context.applicationContext)
    private var listener: ChatSocketListener? = null
    private var socket: Socket? = null

    private val onConnect = Emitter.Listener {
        dispatch {
            listener?.onConnectionStateChanged(ChatConnectionState.CONNECTED)
            conversationId()?.let(::joinConversation)
        }
    }
    private val onDisconnect = Emitter.Listener {
        dispatch {
            listener?.onConnectionStateChanged(ChatConnectionState.DISCONNECTED)
        }
    }
    private val onSessionEnded = Emitter.Listener { dispatch { endTableSession() } }
    private val onConversationClosed = Emitter.Listener { args ->
        val id = (args.firstOrNull() as? JSONObject)?.optString("conversationId")
        dispatch {
            if (id == conversationId()) {
                if (usingTable) sessionStore.clearConversationId()
                else accountId?.let { conversations.edit().remove(it).apply() }
                listener?.onHistory(emptyList())
                listener?.onError("Hội thoại đã đóng. Tin nhắn tiếp theo sẽ mở hội thoại mới.")
            }
        }
    }
    private val onConnectError = Emitter.Listener { args ->
        val raw = args.firstOrNull()?.toString().orEmpty()
        val payload = runCatching { JSONObject(raw) }.getOrNull()
        val unauthorized = payload?.optJSONObject("data")?.optInt("httpStatusCode") == 401 ||
            raw.contains("expired", true) || raw.contains("unauthenticated", true)
        dispatch {
            if (unauthorized && usingTable) {
                endTableSession()
                return@dispatch
            }
            if (unauthorized && !usingTable && !triedRefresh) {
                triedRefresh = true
                connectAccount(refresh = true)
                return@dispatch
            }
            socket?.disconnect()
            listener?.onConnectionStateChanged(ChatConnectionState.ERROR)
            listener?.onError(if (unauthorized) "Phiên chat đã hết hạn. Vui lòng quét lại QR bàn hoặc đăng nhập lại." else "Không thể kết nối với quản lý. Kiểm tra mạng rồi thử lại.")
        }
    }
    private val onNewMessage = Emitter.Listener { args ->
        val message = (args.firstOrNull() as? JSONObject)?.toChatMessage() ?: return@Listener
        dispatch { listener?.onMessage(message) }
    }
    private val onHistory = Emitter.Listener { args ->
        val payload = args.firstOrNull() as? JSONObject ?: return@Listener
        val conversationId = payload.optStringValue("conversationId")
        if (conversationId != null) saveConversationId(conversationId)
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
        triedRefresh = false
        usingTable = true
        accountId = null
        val tableToken = sessionStore.current()?.tableToken
        if (tableToken.isNullOrBlank()) {
            connectAccount()
            return
        }
        val expiresAt = runCatching {
            JSONObject(String(Base64.decode(tableToken.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP)))
                .optLong("exp") * 1_000
        }.getOrDefault(0)
        if (expiresAt > 0 && expiresAt <= System.currentTimeMillis()) {
            endTableSession()
            return
        }

        if (socket?.connected() == true) {
            listener.onConnectionStateChanged(ChatConnectionState.CONNECTED)
            return
        }

        openSocket(mapOf("tableToken" to tableToken))
    }

    private fun connectAccount(refresh: Boolean = false) {
        socket?.off()
        socket?.disconnect()
        socket = null
        listener?.onConnectionStateChanged(ChatConnectionState.CONNECTING)
        connectionJob?.cancel()
        connectionJob = scope.launch {
            val token = runCatching {
                if (refresh) withContext(Dispatchers.IO) {
                    FoodHubSessionRefresh.refresh(OkHttpClient(), "https://foodhub-8lv1.onrender.com/api/v1", accountAccessToken)
                } else tokenStore.getAccessToken()
            }.getOrNull()
            ensureActive()
            val user = tokenStore.getUser()
            if (token.isNullOrBlank() || user == null || !user.role.equals("CUSTOMER", true)) {
                listener?.onConnectionStateChanged(ChatConnectionState.ERROR)
                listener?.onError("Phiên bàn đã hết hạn. Vui lòng quét lại QR bàn để chat với quản lý.")
                return@launch
            }
            usingTable = false
            accountId = user.id
            accountAccessToken = token
            openSocket(mapOf("token" to token))
        }
    }

    private fun openSocket(auth: Map<String, String>) {
        socket?.off()
        socket?.disconnect()
        listener?.onConnectionStateChanged(ChatConnectionState.CONNECTING)
        val options = IO.Options.builder()
            .setAuth(auth)
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
            on("table-session:ended", onSessionEnded)
            on("conversation:closed", onConversationClosed)
            connect()
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        socket?.apply {
            off()
            disconnect()
        }
        socket = null
        listener = null
    }

    private fun endTableSession() {
        socket?.off()
        socket?.disconnect()
        socket = null
        sessionStore.clear()
        listener?.onConnectionStateChanged(ChatConnectionState.ERROR)
        listener?.onSessionEnded()
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
        conversationId()?.let { payload.put("conversationId", it) }

        var completed = false
        val timeout = Runnable {
            if (!completed) {
                completed = true
                onResult(false, "Chưa nhận được xác nhận gửi. Kiểm tra kết nối trước khi gửi lại.")
            }
        }
        mainHandler.postDelayed(timeout, 15_000)
        activeSocket.emit(EVENT_MESSAGE_SEND, payload, Ack { args ->
            val response = args.firstOrNull() as? JSONObject
            val success = response?.optBoolean("success", false) == true
            val data = response?.optJSONObject("data")
            val conversationId = data?.optStringValue("conversationId")
            if (success && conversationId != null) {
                saveConversationId(conversationId)
            }
            data?.optJSONObject("message")?.toChatMessage()?.let { message ->
                dispatch { listener?.onMessage(message) }
            }
            val error = response?.optStringValue("message")
            if (!success && error?.contains("closed", true) == true) {
                if (usingTable) sessionStore.clearConversationId()
                else accountId?.let { conversations.edit().remove(it).apply() }
            }
            dispatch {
                mainHandler.removeCallbacks(timeout)
                if (!completed) {
                    completed = true
                    onResult(success, if (success) null else "Không thể gửi tin nhắn. Vui lòng thử lại.")
                }
            }
        })
    }

    fun setTyping(typing: Boolean) {
        val conversationId = conversationId() ?: return
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
                val message = response.optStringValue("message").orEmpty()
                if (message.contains("not found", true) || message.contains("closed", true) || message.contains("forbidden", true)) {
                    if (usingTable) sessionStore.clearConversationId()
                    else conversations.edit().remove(accountId).apply()
                }
                dispatch {
                    listener?.onError(
                        "Không thể tải hội thoại cũ. Hãy thử gửi tin nhắn mới.",
                    )
                }
            }
        })
    }

    private fun dispatch(block: () -> Unit) {
        mainHandler.post(block)
    }

    private fun conversationId(): String? = if (usingTable) sessionStore.conversationId()
        else accountId?.let { conversations.getString(it, null) }

    private fun saveConversationId(id: String) {
        if (usingTable) sessionStore.saveConversationId(id)
        else accountId?.let { conversations.edit().putString(it, id).apply() }
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

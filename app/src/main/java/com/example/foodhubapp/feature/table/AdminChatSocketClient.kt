package com.example.foodhubapp.feature.table

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.foodhubapp.core.datastore.TokenStore
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class AdminChatSocketClient(context: Context) {
    private val tokenStore = TokenStore(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null

    suspend fun connect(
        conversationId: String,
        onState: (ChatConnectionState) -> Unit,
        onHistory: (List<ChatMessage>) -> Unit,
        onMessage: (ChatMessage) -> Unit,
        onError: (String) -> Unit,
    ) {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank)
            ?: return onError("Vui lòng đăng nhập lại")
        onState(ChatConnectionState.CONNECTING)
        val options = IO.Options.builder().setAuth(mapOf("token" to token)).setReconnection(true).build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on(Socket.EVENT_CONNECT) {
                handler.post { onState(ChatConnectionState.CONNECTED) }
                emit("conversation:join", JSONObject().put("conversationId", conversationId).put("limit", 100))
            }
            on(Socket.EVENT_DISCONNECT) { handler.post { onState(ChatConnectionState.DISCONNECTED) } }
            on(Socket.EVENT_CONNECT_ERROR) { args -> handler.post { onError(args.firstOrNull()?.toString() ?: "Không thể kết nối chat") } }
            on("message:history") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                handler.post { onHistory(data.optJSONArray("messages").messages()) }
            }
            on("message:new") { args ->
                (args.firstOrNull() as? JSONObject)?.message()?.let { value -> handler.post { onMessage(value) } }
            }
            connect()
        }
    }

    fun send(conversationId: String, content: String, onResult: (Boolean, String?) -> Unit) {
        socket?.emit(
            "message:send",
            JSONObject().put("conversationId", conversationId).put("content", content).put("type", "TEXT"),
            Ack { args ->
                val response = args.firstOrNull() as? JSONObject
                handler.post { onResult(response?.optBoolean("success") == true, response?.optString("message")) }
            },
        )
    }

    fun disconnect() { socket?.off(); socket?.disconnect(); socket = null }

    private fun JSONObject.message(): ChatMessage? {
        val content = optString("content").takeIf(String::isNotBlank) ?: return null
        return ChatMessage(
            optString("_id", optString("id")), optString("conversationId"), optString("senderId"),
            optString("senderRole", "staff"), content, optString("type", "TEXT"), optString("createdAt"),
        )
    }

    private fun JSONArray?.messages() = if (this == null) emptyList() else
        (0 until length()).mapNotNull { optJSONObject(it)?.message() }

    private companion object { const val SOCKET_URL = "https://foodhub-8lv1.onrender.com" }
}

package com.example.foodhubapp.feature.admin.chat

import com.example.foodhubapp.feature.shared.chat.ChatConnectionState
import com.example.foodhubapp.feature.shared.chat.ChatMessage

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubSessionRefresh
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null

    suspend fun connect(
        conversationId: String,
        onState: (ChatConnectionState) -> Unit,
        onHistory: (List<ChatMessage>) -> Unit,
        onMessage: (ChatMessage) -> Unit,
        onError: (String) -> Unit,
        refreshed: Boolean = false,
    ) {
        socket?.off()
        socket?.disconnect()
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank)
            ?: run { onState(ChatConnectionState.ERROR); return onError("Vui lòng đăng nhập lại") }
        onState(ChatConnectionState.CONNECTING)
        val options = IO.Options.builder().setAuth(mapOf("token" to token)).setReconnection(true).build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on(Socket.EVENT_CONNECT) {
                handler.post { onState(ChatConnectionState.CONNECTED) }
                emit("conversation:join", JSONObject().put("conversationId", conversationId).put("limit", 100))
            }
            on(Socket.EVENT_DISCONNECT) { handler.post { onState(ChatConnectionState.DISCONNECTED) } }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val raw = args.firstOrNull()?.toString().orEmpty()
                val data = runCatching { JSONObject(raw) }.getOrNull()
                val unauthorized = data?.optJSONObject("data")?.optInt("httpStatusCode") == 401 || raw.contains("expired", true)
                handler.post {
                    if (unauthorized && !refreshed && refreshJob?.isActive != true) {
                        socket?.off()
                        socket?.disconnect()
                        onState(ChatConnectionState.CONNECTING)
                        refreshJob = scope.launch {
                            val newToken = runCatching {
                                withContext(Dispatchers.IO) {
                                    FoodHubSessionRefresh.refresh(OkHttpClient(), "https://foodhub-8lv1.onrender.com/api/v1", token)
                                }
                            }.getOrNull()
                            ensureActive()
                            if (newToken != null) connect(conversationId, onState, onHistory, onMessage, onError, refreshed = true)
                            else { onState(ChatConnectionState.ERROR); onError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.") }
                        }
                    } else {
                        onState(ChatConnectionState.ERROR)
                        onError(if (unauthorized) "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại." else "Không thể kết nối chat. Vui lòng kiểm tra mạng.")
                    }
                }
            }
            on("message:history") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                handler.post { onHistory(data.optJSONArray("messages").messages()) }
            }
            on("message:new") { args ->
                (args.firstOrNull() as? JSONObject)?.message()?.let { value -> handler.post { onMessage(value) } }
            }
            on("conversation:closed") { args ->
                val id = (args.firstOrNull() as? JSONObject)?.optString("conversationId")
                if (id == conversationId) handler.post {
                    onState(ChatConnectionState.ERROR)
                    onError("Hội thoại đã đóng hoặc phiên bàn đã kết thúc.")
                }
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

    fun disconnect() { refreshJob?.cancel(); socket?.off(); socket?.disconnect(); socket = null }

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

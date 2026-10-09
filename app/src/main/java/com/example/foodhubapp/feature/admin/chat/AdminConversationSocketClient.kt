package com.example.foodhubapp.feature.admin.chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.foodhubapp.core.datastore.TokenStore
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI

class AdminConversationSocketClient(context: Context) {
    private val tokenStore = TokenStore(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null

    suspend fun connect(onConversationChanged: (Boolean) -> Unit) {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank) ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on("conversation:new") { handler.post { onConversationChanged(true) } }
            on("conversation:updated") { handler.post { onConversationChanged(false) } }
            connect()
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private companion object {
        const val SOCKET_URL = "https://foodhub-8lv1.onrender.com"
    }
}

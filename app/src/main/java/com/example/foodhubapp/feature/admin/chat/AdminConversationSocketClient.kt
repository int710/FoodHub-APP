package com.example.foodhubapp.feature.admin.chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.foodhubapp.core.datastore.TokenStore
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI
import org.json.JSONObject
import com.example.foodhubapp.R
import com.example.foodhubapp.core.media.AppSoundPlayer

class AdminConversationSocketClient(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStore = TokenStore(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null

    suspend fun connect(onConversationChanged: (Boolean) -> Unit) {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank) ?: return
        val currentUserId = tokenStore.getUser()?.id
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on("conversation:new") { args -> handler.post {
                val senderId = (args.firstOrNull() as? JSONObject)?.optString("lastMessageSenderId")
                if (senderId.isNullOrBlank() || senderId != currentUserId) AppSoundPlayer.play(appContext, R.raw.sound_foodhub_message)
                onConversationChanged(true)
            } }
            on("conversation:updated") { args -> handler.post {
                val senderId = (args.firstOrNull() as? JSONObject)?.optString("lastMessageSenderId")
                if (senderId.isNullOrBlank() || senderId != currentUserId) AppSoundPlayer.play(appContext, R.raw.sound_foodhub_message)
                onConversationChanged(false)
            } }
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

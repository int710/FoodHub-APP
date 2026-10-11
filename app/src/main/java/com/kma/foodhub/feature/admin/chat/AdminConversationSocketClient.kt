package com.kma.foodhub.feature.admin.chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kma.foodhub.core.datastore.TokenStore
import com.kma.foodhub.core.network.FoodhubRetrofit
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI
import org.json.JSONObject
import com.kma.foodhub.R
import com.kma.foodhub.core.media.AppSoundPlayer

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
        socket = IO.socket(URI.create(FoodhubRetrofit.SERVER_URL), options).apply {
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

}

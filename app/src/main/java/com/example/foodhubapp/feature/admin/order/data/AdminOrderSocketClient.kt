package com.example.foodhubapp.feature.admin.order.data

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

data class AdminOrderSocketEvent(val name: String, val orderId: String, val status: String?)

class AdminOrderSocketClient(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStore = TokenStore(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null

    suspend fun connect(onOrderChanged: (AdminOrderSocketEvent) -> Unit) {
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank) ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            listOf("order:new", "order:status:update", "order:item:update").forEach { event ->
                on(event) { args ->
                    val payload = args.firstOrNull() as? JSONObject
                    handler.post {
                        if (event == "order:new") AppSoundPlayer.play(appContext, R.raw.sound_foodhub_neworder)
                        onOrderChanged(
                            AdminOrderSocketEvent(
                                name = event,
                                orderId = payload?.optString("orderId").orEmpty(),
                                status = payload?.optString("status")?.takeIf(String::isNotBlank),
                            ),
                        )
                    }
                }
            }
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

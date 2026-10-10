package com.example.foodhubapp.feature.customer.menu.data

import android.os.Handler
import android.os.Looper
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI

class MenuSocketClient {
    private val handler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null

    fun connect(onMenuUpdated: () -> Unit) {
        disconnect()
        val options = IO.Options.builder().setReconnection(true).build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on("menu:updated") { handler.post(onMenuUpdated) }
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

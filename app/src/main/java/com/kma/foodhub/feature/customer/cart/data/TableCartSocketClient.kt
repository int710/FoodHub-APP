package com.kma.foodhub.feature.customer.cart.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kma.foodhub.core.network.FoodhubRetrofit
import com.kma.foodhub.feature.customer.table.data.TableSessionStore
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI

/** Receives invalidation signals for the shared dine-in cart of a table. */
class TableCartSocketClient(context: Context) {
    private val sessionStore = TableSessionStore(context.applicationContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null

    fun connect(onCartUpdated: () -> Unit) {
        disconnect()
        val tableToken = sessionStore.current()?.tableToken?.takeIf(String::isNotBlank) ?: return
        val options = IO.Options.builder()
            .setAuth(mapOf("tableToken" to tableToken))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .build()
        socket = IO.socket(URI.create(FoodhubRetrofit.SERVER_URL), options).apply {
            on("cart:updated") { mainHandler.post(onCartUpdated) }
            connect()
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }
}

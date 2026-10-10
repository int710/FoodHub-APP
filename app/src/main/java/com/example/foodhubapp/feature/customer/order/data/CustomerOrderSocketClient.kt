package com.example.foodhubapp.feature.customer.order.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.customer.table.data.TableSessionStore
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URI
import com.example.foodhubapp.R
import com.example.foodhubapp.core.media.AppSoundPlayer

data class CustomerOrderSocketEvent(
    val orderId: String,
    val status: String?,
    val isItemUpdate: Boolean,
)

class CustomerOrderSocketClient(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStore = TokenStore(context.applicationContext)
    private val tableSessionStore = TableSessionStore(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null
    private var orderIds: Set<String> = emptySet()

    suspend fun connect(onChanged: (CustomerOrderSocketEvent) -> Unit) {
        val tableToken = tableSessionStore.current()?.tableToken?.takeIf(String::isNotBlank)
        val token = tokenStore.getAccessToken()?.takeIf(String::isNotBlank)
        if (tableToken == null && token == null) return
        disconnect()
        val options = IO.Options.builder()
            .setAuth(if (token != null) mapOf("token" to token) else mapOf("tableToken" to tableToken!!))
            .setReconnection(true)
            .build()
        socket = IO.socket(URI.create(SOCKET_URL), options).apply {
            on(Socket.EVENT_CONNECT) { joinCurrentOrders() }
            on("order:status:update") { args ->
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                handler.post {
                    if (payload.optString("status") == "COMPLETED") {
                        AppSoundPlayer.play(appContext, R.raw.sound_tingting)
                    }
                    onChanged(
                        CustomerOrderSocketEvent(
                            orderId = payload.optString("orderId"),
                            status = payload.optString("status").takeIf(String::isNotBlank),
                            isItemUpdate = false,
                        ),
                    )
                }
            }
            on("order:item:update") { args ->
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                handler.post {
                    onChanged(CustomerOrderSocketEvent(payload.optString("orderId"), null, true))
                }
            }
            connect()
        }
    }

    fun watch(ids: Collection<String>) {
        orderIds = ids.filter(String::isNotBlank).toSet()
        if (socket?.connected() == true) joinCurrentOrders()
    }

    private fun joinCurrentOrders() {
        orderIds.forEach { socket?.emit("order:join", JSONObject().put("orderId", it)) }
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

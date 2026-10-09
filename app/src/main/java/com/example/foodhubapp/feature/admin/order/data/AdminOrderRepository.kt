package com.example.foodhubapp.feature.admin.order.data

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.admin.model.AdminItemStatus
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminOrderLine
import com.example.foodhubapp.feature.admin.model.AdminOrderStatus
import com.example.foodhubapp.feature.admin.model.AdminOrderType
import com.example.foodhubapp.feature.admin.model.AdminPaymentMethod
import com.example.foodhubapp.feature.admin.model.AdminZaloPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AdminOrderRepository internal constructor(
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodHubApiClient()) : this(
        apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )

    suspend fun getOrders(): List<AdminOrder> = withContext(Dispatchers.IO) {
        val response = apiClient.getJson("/order/history?page=1&limit=100", headers())
        (response.optJSONArray("data") ?: JSONArray()).objects().map(JSONObject::toAdminOrder)
    }

    suspend fun getKitchenItems(page: Int = 1): List<KitchenItem> = withContext(Dispatchers.IO) {
        val response = apiClient.getJson("/order/kitchen?page=$page&limit=100", headers())
        (response.optJSONArray("data") ?: JSONArray()).objects().map { item ->
            val order = item.getJSONObject("order")
            KitchenItem(item.getString("id"), item.getString("orderId"),
                item.optJSONObject("snapshot")?.optString("name").orEmpty().ifBlank { "Món ăn" },
                item.optInt("quantity", 1), item.optionalString("note"),
                enumValueOrDefault(item.optString("status"), AdminItemStatus.WAITING),
                order.optJSONObject("table")?.optString("name")?.takeIf(String::isNotBlank)
                    ?: order.optString("pickupCode").ifBlank { when (order.optString("type")) {
                        "TAKEAWAY" -> "Mang về"; "DELIVERY" -> "Giao hàng"; else -> "Tại bàn"
                    } },
                options = item.optJSONObject("snapshot")?.optJSONArray("variantOptions")?.strings().orEmpty())
        }
    }

    suspend fun confirm(orderId: String) = mutate("/order/$orderId/confirm", JSONObject())

    suspend fun reject(orderId: String, reason: String) =
        mutate("/order/$orderId/reject", JSONObject().put("reason", reason.trim()))

    suspend fun updateItem(itemId: String, status: AdminItemStatus) =
        mutate("/order/kitchen/$itemId/status", JSONObject().put("status", status.name))

    suspend fun serve(orderId: String) = mutate("/order/$orderId/serve", JSONObject())

    suspend fun complete(orderId: String) = mutate("/order/$orderId/complete", JSONObject())

    suspend fun confirmCash(orderId: String) = mutate("/payment/$orderId/cash-confirm", JSONObject())

    suspend fun convertCashToZaloPay(orderId: String): AdminZaloPayment = withContext(Dispatchers.IO) {
        val response = apiClient.patch("/payment/$orderId/zalopay/convert", JSONObject(), headers())
        val data = response.optJSONObject("data") ?: error("Máy chủ chưa trả giao dịch ZaloPay")
        AdminZaloPayment(
            orderId = data.optString("orderId", orderId),
            orderCode = data.optString("orderCode"),
            paymentUrl = data.optString("paymentUrl", data.optString("orderUrl"))
                .takeIf(String::isNotBlank) ?: error("Máy chủ chưa trả URL ZaloPay"),
            qrContent = data.optionalString("qrCode"),
        )
    }

    private suspend fun mutate(path: String, body: JSONObject) = withContext(Dispatchers.IO) {
        apiClient.patch(path, body, headers())
        Unit
    }

    private suspend fun headers(): Map<String, String> {
        val token = accessToken()?.takeIf(String::isNotBlank)
            ?: error("Vui lòng đăng nhập lại bằng tài khoản Admin.")
        return mapOf("Authorization" to "Bearer $token")
    }
}

data class KitchenItem(val id: String, val orderId: String, val name: String, val quantity: Int,
    val note: String?, val status: AdminItemStatus, val destination: String, val options: List<String> = emptyList())

private fun JSONObject.toAdminOrder(): AdminOrder {
    val table = optJSONObject("table")
    val delivery = optJSONObject("deliveryInfo")
    val payments = optJSONArray("payments")
    val payment = payments?.optJSONObject(0) ?: optJSONObject("payment")
    val created = optString("createdAt")
    val createdInstant = runCatching { Instant.parse(created) }
        .recoverCatching { OffsetDateTime.parse(created).toInstant() }
        .getOrNull()
    val destination = when (optString("type")) {
        "DINE_IN" -> listOfNotNull(table?.optString("name"), table?.optString("floor")).filter(String::isNotBlank).joinToString(" • ").ifBlank { "Tại bàn" }
        "DELIVERY" -> delivery?.optString("address").orEmpty().ifBlank { "Giao hàng" }
        else -> optString("pickupCode").takeIf(String::isNotBlank)?.let { "Mã nhận $it" } ?: "Mang về"
    }
    return AdminOrder(
        id = optString("id"),
        code = optString("orderCode", optString("id")),
        type = enumValueOrDefault(optString("type"), AdminOrderType.TAKEAWAY),
        destination = destination,
        createdAt = createdInstant?.atZone(ZoneId.systemDefault())?.toLocalTime()?.toString()?.take(5) ?: created.take(16),
        waitMinutes = createdInstant?.let { ChronoUnit.MINUTES.between(it, Instant.now()).toInt().coerceAtLeast(0) } ?: 0,
        items = (optJSONArray("items") ?: JSONArray()).objects().map { item ->
            val snapshot = item.optJSONObject("snapshot")
            AdminOrderLine(
                id = item.optString("id"),
                name = snapshot?.optString("name").orEmpty().ifBlank { item.optJSONObject("menuItem")?.optString("name").orEmpty().ifBlank { "Món ăn" } },
                quantity = item.optInt("quantity", 1),
                unitPrice = item.money("unitPrice"),
                options = snapshot?.optJSONArray("variantOptions")?.strings().orEmpty(),
                note = item.optionalString("note"),
                status = enumValueOrDefault(item.optString("status"), AdminItemStatus.WAITING),
            )
        },
        total = money("totalAmount"),
        status = enumValueOrDefault(optString("status"), AdminOrderStatus.PENDING_CONFIRMATION),
        paymentMethod = enumValueOrDefault(payment?.optString("method").orEmpty(), AdminPaymentMethod.CASH),
        paid = payment?.optString("status") == "PAID" || !optionalString("paidAt").isNullOrBlank(),
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun JSONObject.money(key: String): Long = opt(key)?.toString()?.toBigDecimalOrNull()?.toLong() ?: 0L
private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
private fun JSONArray.objects() = (0 until length()).mapNotNull(::optJSONObject)
private fun JSONArray.strings() = (0 until length()).mapNotNull { index ->
    when (val value = opt(index)) {
        is JSONObject -> value.optString("name").takeIf(String::isNotBlank)
        is String -> value.takeIf(String::isNotBlank)
        else -> null
    }
}

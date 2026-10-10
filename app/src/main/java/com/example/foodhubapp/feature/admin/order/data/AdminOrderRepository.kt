package com.example.foodhubapp.feature.admin.order.data

import com.example.foodhubapp.core.network.*
import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.admin.model.AdminItemStatus
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminOrderLine
import com.example.foodhubapp.feature.admin.model.AdminOrderStatus
import com.example.foodhubapp.feature.admin.model.AdminOrderType
import com.example.foodhubapp.feature.admin.model.AdminPaymentMethod
import com.example.foodhubapp.feature.admin.model.AdminZaloPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AdminOrderRepository internal constructor(
    private val apiClient: FoodHubApiClient,
    private val accessToken: suspend () -> String?,
) {
    constructor(context: Context, apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient) : this(
        apiClient, { TokenStore(context.applicationContext).getAccessToken() },
    )

    suspend fun getOrders(): List<AdminOrder> = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.adminOrderApi.getOrders(
                headers = headers()
            )
        )
        (response.optArray("data") ?: JsonArray()).objects().map(JsonObject::toAdminOrder)
    }

    suspend fun getKitchenItems(page: Int = 1): List<KitchenItem> = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.adminOrderApi.getKitchenItems(
                page = page,
                headers = headers()
            )
        )
        (response.optArray("data") ?: JsonArray()).objects().map { item ->
            val order = item.getObject("order")
            KitchenItem(item.getString("id"), item.getString("orderId"),
                item.optObject("snapshot")?.optString("name").orEmpty().ifBlank { "Món ăn" },
                item.optInt("quantity", 1), item.optionalString("note"),
                enumValueOrDefault(item.optString("status"), AdminItemStatus.WAITING),
                order.optObject("table")?.optString("name")?.takeIf(String::isNotBlank)
                    ?: order.optString("pickupCode").ifBlank { when (order.optString("type")) {
                        "TAKEAWAY" -> "Mang về"; "DELIVERY" -> "Giao hàng"; else -> "Tại bàn"
                    } },
                options = item.optObject("snapshot")?.optArray("variantOptions")?.strings().orEmpty())
        }
    }

    suspend fun confirm(orderId: String) = mutate { apiClient.adminOrderApi.confirm(it, orderId) }

    suspend fun reject(orderId: String, reason: String) =
        mutate { apiClient.adminOrderApi.reject(it, orderId, JsonObject().put("reason", reason.trim())) }

    suspend fun updateItem(itemId: String, status: AdminItemStatus) =
        mutate { apiClient.adminOrderApi.updateItem(it, itemId, JsonObject().put("status", status.name)) }

    suspend fun serve(orderId: String) = mutate { apiClient.adminOrderApi.serve(it, orderId) }

    suspend fun complete(orderId: String) = mutate { apiClient.adminOrderApi.complete(it, orderId) }

    suspend fun confirmCash(orderId: String) = mutate { apiClient.adminOrderApi.confirmCash(it, orderId) }

    suspend fun convertCashToZaloPay(orderId: String): AdminZaloPayment = withContext(Dispatchers.IO) {
        val response = apiClient.execute(
            apiClient.adminOrderApi.convertCashToZaloPay(
                id = orderId,
                headers = headers(),
                body = JsonObject()
            )
        )
        val data = response.optObject("data") ?: error("Máy chủ chưa trả giao dịch ZaloPay")
        AdminZaloPayment(
            orderId = data.optString("orderId", orderId),
            orderCode = data.optString("orderCode"),
            paymentUrl = data.optString("paymentUrl", data.optString("orderUrl"))
                .takeIf(String::isNotBlank) ?: error("Máy chủ chưa trả URL ZaloPay"),
            qrContent = data.optionalString("qrCode"),
        )
    }

    private suspend fun mutate(
        call: (Map<String, String>) -> retrofit2.Call<JsonObject>
    ) = withContext(Dispatchers.IO) {
        apiClient.execute(call(headers()))
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

private fun JsonObject.toAdminOrder(): AdminOrder {
    val table = optObject("table")
    val delivery = optObject("deliveryInfo")
    val payments = optArray("payments")
    val payment = payments?.optObject(0) ?: optObject("payment")
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
        items = (optArray("items") ?: JsonArray()).objects().map { item ->
            val snapshot = item.optObject("snapshot")
            AdminOrderLine(
                id = item.optString("id"),
                name = snapshot?.optString("name").orEmpty().ifBlank { item.optObject("menuItem")?.optString("name").orEmpty().ifBlank { "Món ăn" } },
                quantity = item.optInt("quantity", 1),
                unitPrice = item.money("unitPrice"),
                options = snapshot?.optArray("variantOptions")?.strings().orEmpty(),
                note = item.optionalString("note"),
                status = enumValueOrDefault(item.optString("status"), AdminItemStatus.WAITING),
                imageUrl = snapshot?.optionalString("image") ?: item.optObject("menuItem")?.optionalString("image"),
            )
        },
        total = money("totalAmount"),
        status = enumValueOrDefault(optString("status"), AdminOrderStatus.PENDING_CONFIRMATION),
        paymentMethod = enumValueOrDefault(payment?.optString("method").orEmpty(), AdminPaymentMethod.CASH),
        paid = payment?.optString("status") == "PAID" || !optionalString("paidAt").isNullOrBlank(),
        createdAtEpochMillis = createdInstant?.toEpochMilli() ?: 0L,
        subtotal = money("subtotal"),
        vatAmount = money("vatAmount"),
        deliveryFee = money("deliveryFee"),
        serviceFee = money("serviceFee"),
        discountAmount = money("discountAmount"),
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun JsonObject.money(key: String): Long = opt(key)?.toString()?.toBigDecimalOrNull()?.toLong() ?: 0L
private fun JsonObject.optionalString(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
private fun JsonArray.objects() = (0 until size()).mapNotNull(::optObject)
private fun JsonArray.strings() = (0 until size()).mapNotNull { index ->
    when (val value = opt(index)) {
        is JsonObject -> value.optString("name").takeIf(String::isNotBlank)
        is String -> value.takeIf(String::isNotBlank)
        else -> null
    }
}

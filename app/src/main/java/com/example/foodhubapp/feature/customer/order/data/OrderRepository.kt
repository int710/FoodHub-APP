package com.example.foodhubapp.feature.customer.order.data

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.feature.customer.menu.data.LoginRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.math.BigDecimal
import java.net.URLEncoder

enum class OrderType { DINE_IN, TAKEAWAY, DELIVERY }
enum class CheckoutPaymentMethod { CASH, VNPAY, ZALOPAY }

enum class OrderStatus {
    PENDING_PAYMENT,
    PENDING_CONFIRMATION,
    CONFIRMED,
    PREPARING,
    READY,
    SERVED,
    COMPLETED,
    CANCELLED,
    PAYMENT_FAILED,
    UNKNOWN
}

data class OrderItem(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val quantity: Int,
    val unitPrice: Long,
    val subTotal: Long,
    val description: String,
    val menuItemId: String = "",
)

data class CustomerOrder(
    val id: String,
    val orderCode: String,
    val pickupCode: String?,
    val type: OrderType,
    val status: OrderStatus,
    val createdAt: String,
    val totalAmount: Long,
    val tableName: String?,
    val tableFloor: String?,
    val deliveryAddress: String?,
    val cancelReason: String?,
    val paymentMethod: String?,
    val paymentStatus: String?,
    val items: List<OrderItem>,
    val reviewedMenuItemIds: Set<String> = emptySet(),
) {
    val canCancel: Boolean
        get() = paymentStatus?.uppercase() in setOf("PENDING", "UNPAID", "FAILED") &&
            status in setOf(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_CONFIRMATION)
}

data class OrderPage(
    val orders: List<CustomerOrder>,
    val page: Int,
    val hasMore: Boolean
)

data class PaymentLaunch(val url: String, val qrContent: String? = null)

data class PaymentStatusResult(
    val orderId: String,
    val orderCode: String,
    val orderStatus: OrderStatus,
    val paymentStatus: String?,
    val paidAt: String?,
    val shouldPoll: Boolean,
)

interface OrderRepository {
    suspend fun getHistory(page: Int, limit: Int = 20, type: OrderType? = null): OrderPage
    suspend fun cancel(orderId: String, reason: String)
    suspend fun createVnPayUrl(orderCode: String): String
    suspend fun createPaymentLaunch(orderCode: String, method: CheckoutPaymentMethod): PaymentLaunch
    suspend fun getPaymentStatus(orderCode: String, method: CheckoutPaymentMethod): PaymentStatusResult
    suspend fun submitReview(orderId: String, menuItemId: String, rating: Int, comment: String)
}

/** Lịch sử dùng Bearer token hoặc phiên bàn; các thao tác cá nhân yêu cầu đăng nhập. */
class RemoteOrderRepository(
    private val apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient,
    private val tableToken: () -> String? = { null },
    private val accessToken: suspend () -> String? = { null }
) : OrderRepository {
    override suspend fun getHistory(page: Int, limit: Int, type: OrderType?): OrderPage = withContext(Dispatchers.IO) {
        require(page >= 1)
        require(limit in 1..100)
        val access = accessToken()?.takeIf(String::isNotBlank)
        val call = if (access != null) {
            apiClient.orderApi.getHistory(
                headers = mapOf("Authorization" to "Bearer $access"),
                page = page, limit = limit, type = type?.name
            )
        } else {
            val table = tableToken()?.takeIf(String::isNotBlank) ?: throw LoginRequiredException()
            apiClient.orderApi.getTableHistory(
                headers = mapOf("X-Table-Token" to table),
                page = page, limit = limit, type = type?.name
            )
        }
        parseOrderPage(apiClient.execute(call), page, limit)
    }

    override suspend fun cancel(orderId: String, reason: String) = withContext(Dispatchers.IO) {
        require(orderId.isNotBlank())
        require(reason.isNotBlank() && reason.length <= 255)
        apiClient.execute(
            apiClient.orderApi.cancel(
                id = orderId.encoded(),
                headers = authHeaders(),
                body = JsonObject().put("reason", reason.trim())
            )
        )
        Unit
    }

    override suspend fun createVnPayUrl(orderCode: String): String = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        val response = apiClient.execute(
            apiClient.paymentApi.createPayment(
                provider = "vnpay",
                headers = authHeaders(),
                body = JsonObject().put("orderCode", orderCode)
            )
        )
        val data = response.optObject("data")
        response.optionalString("paymentUrl")
            ?: data?.optionalString("paymentUrl")
            ?: error("Máy chủ chưa trả đường dẫn thanh toán VNPay.")
    }

    override suspend fun createPaymentLaunch(
        orderCode: String,
        method: CheckoutPaymentMethod,
    ): PaymentLaunch = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        require(method != CheckoutPaymentMethod.CASH)
        val provider = method.name.lowercase()
        val response = apiClient.execute(
            apiClient.paymentApi.createPayment(
                provider = provider,
                headers = authHeaders(),
                body = JsonObject().put("orderCode", orderCode)
            )
        )
        val data = response.optObject("data") ?: response
        PaymentLaunch(
            url = data.optionalString("paymentUrl")
                ?: data.optionalString("orderUrl")
                ?: error("Máy chủ chưa trả đường dẫn thanh toán ${method.displayName()}."),
            qrContent = data.optionalString("qrCode"),
        )
    }

    override suspend fun getPaymentStatus(
        orderCode: String,
        method: CheckoutPaymentMethod,
    ): PaymentStatusResult = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        require(method != CheckoutPaymentMethod.CASH)
        val provider = method.name.lowercase()
        val response = apiClient.execute(
            apiClient.paymentApi.getStatus(
                provider = provider,
                code = orderCode.encoded(),
            )
        )
        val data = response.optObject("data")
            ?: error("Máy chủ chưa trả trạng thái thanh toán.")
        PaymentStatusResult(
            orderId = data.firstString("orderId", "id"),
            orderCode = data.firstString("orderCode", "code"),
            orderStatus = enumValueOrNull<OrderStatus>(data.optString("orderStatus")) ?: OrderStatus.UNKNOWN,
            paymentStatus = data.optionalString("paymentStatus"),
            paidAt = data.optionalString("paidAt"),
            shouldPoll = data.optBoolean("shouldPoll", false),
        )
    }

    override suspend fun submitReview(orderId: String, menuItemId: String, rating: Int, comment: String) =
        withContext(Dispatchers.IO) {
            require(orderId.isNotBlank() && menuItemId.isNotBlank())
            require(rating in 1..5 && comment.trim().length <= 1000)
            apiClient.execute(
                apiClient.reviewApi.submitReview(
                    headers = authHeaders(),
                    body = JsonObject().put("orderId", orderId).put("menuItemId", menuItemId)
                            .put("rating", rating).put("comment", comment.trim()).put("images", JsonArray())
                )
            )
            Unit
        }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf { it.isNotBlank() } ?: throw LoginRequiredException()
        return mapOf("Authorization" to "Bearer $token")
    }
}

fun CheckoutPaymentMethod.displayName(): String = when (this) {
    CheckoutPaymentMethod.CASH -> "tiền mặt"
    CheckoutPaymentMethod.VNPAY -> "VNPay"
    CheckoutPaymentMethod.ZALOPAY -> "ZaloPay"
}

/** Swagger chưa khai báo schema data, parser chấp nhận cả data dạng mảng và object phân trang. */
internal fun parseOrderPage(response: JsonObject, requestedPage: Int, limit: Int): OrderPage {
    val data = response.opt("data")
    val array = when (data) {
        is JsonArray -> data
        is JsonObject -> data.firstArray("orders", "items", "results", "data") ?: JsonArray()
        else -> JsonArray()
    }
    val orders = array.objects().map(::parseOrder)
    val pagination = response.optObject("pagination")
        ?: (data as? JsonObject)?.optObject("pagination")
    val page = pagination?.firstInt("page", "currentPage") ?: requestedPage
    val totalPages = pagination?.firstInt("totalPages", "pages")
    val total = pagination?.firstInt("total", "totalItems")
    val hasMore = when {
        pagination?.has("hasNext") == true -> pagination.optBoolean("hasNext")
        totalPages != null -> page < totalPages
        total != null -> page * limit < total
        else -> orders.size >= limit
    }
    return OrderPage(orders, page, hasMore)
}

private fun parseOrder(json: JsonObject): CustomerOrder {
    val table = json.optObject("table")
    val delivery = json.optObject("deliveryInfo")
    val payments = json.optArray("payments")
    val payment = payments?.objects()?.firstOrNull { it.optString("status") == "PAID" }
        ?: payments?.optObject(0) ?: json.optObject("payment")
    val items = json.optArray("items")?.objects()?.map(::parseOrderItem).orEmpty()
    return CustomerOrder(
        id = json.firstString("id", "orderId"),
        orderCode = json.firstString("orderCode", "code").ifBlank { json.firstString("id").take(8) },
        pickupCode = json.optionalString("pickupCode"),
        type = enumValueOrNull<OrderType>(json.optString("type")) ?: OrderType.TAKEAWAY,
        status = enumValueOrNull<OrderStatus>(json.optString("status")) ?: OrderStatus.UNKNOWN,
        createdAt = json.firstString("createdAt", "orderedAt"),
        totalAmount = json.firstMoney("totalAmount", "total") ?: items.sumOf { it.subTotal },
        tableName = table?.firstString("name", "tableName")?.ifBlank { null }
            ?: json.optionalString("tableName"),
        tableFloor = table?.optionalString("floor"),
        deliveryAddress = delivery?.firstString("address", "fullAddress", "deliveryAddress")?.ifBlank { null }
            ?: json.optionalString("deliveryAddress"),
        cancelReason = json.optionalString("cancelReason"),
        paymentMethod = payment?.optionalString("method") ?: json.optionalString("paymentMethod"),
        paymentStatus = if (json.optionalString("paidAt") != null) "PAID"
            else payment?.optionalString("status") ?: json.optionalString("paymentStatus"),
        items = items,
        reviewedMenuItemIds = json.optArray("reviews")?.objects()
            ?.mapNotNull { it.optionalString("menuItemId") }?.toSet().orEmpty(),
    )
}

private fun parseOrderItem(json: JsonObject): OrderItem {
    val menuItem = json.optObject("menuItem") ?: json.optObject("item")
    val snapshot = json.optObject("snapshot")
    val quantity = json.optInt("quantity", 1).coerceAtLeast(1)
    val name = snapshot?.firstString("name", "menuItemName")?.ifBlank { null }
        ?: menuItem?.firstString("name")?.ifBlank { null }
        ?: json.firstString("name", "menuItemName").ifBlank { "Món ăn" }
    val image = snapshot?.firstString("image", "imageUrl")?.ifBlank { null }
        ?: menuItem?.firstString("image", "imageUrl")?.ifBlank { null }
        ?: json.firstString("image", "imageUrl").ifBlank { null }
    val options = snapshot?.firstArray("variantOptions", "options")
        ?: json.firstArray("variantOptions", "options")
    val optionNames = options?.values()?.mapNotNull { raw ->
        when (raw) {
            is JsonObject -> raw.firstString("name", "optionName").ifBlank { null }
            is String -> raw.takeIf { it.isNotBlank() }
            else -> null
        }
    }.orEmpty()
    val note = json.optionalString("note")
    return OrderItem(
        id = json.firstString("id", "orderItemId"),
        menuItemId = json.firstString("menuItemId").ifBlank { menuItem?.firstString("id").orEmpty() },
        name = name,
        imageUrl = image,
        quantity = quantity,
        unitPrice = json.firstMoney("unitPrice", "price") ?: 0L,
        subTotal = json.firstMoney("subTotal", "subtotal", "totalPrice") ?: 0L,
        description = (optionNames + listOfNotNull(note)).joinToString(", ")
    )
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }

private fun String.encoded() = URLEncoder.encode(this, "UTF-8")
private fun JsonArray.objects(): List<JsonObject> = (0 until size()).mapNotNull { optObject(it) }
private fun JsonArray.values(): List<Any> = (0 until size()).mapNotNull { opt(it) }
private fun JsonObject.firstArray(vararg keys: String): JsonArray? = keys.firstNotNullOfOrNull(::optArray)
private fun JsonObject.firstString(vararg keys: String): String =
    keys.firstNotNullOfOrNull { optionalString(it) }.orEmpty()
private fun JsonObject.optionalString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
private fun JsonObject.firstInt(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
    if (!has(key) || isNull(key)) null else runCatching { opt(key).toString().toInt() }.getOrNull()
}
private fun JsonObject.firstMoney(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
    if (!has(key) || isNull(key)) null
    else runCatching { BigDecimal(opt(key).toString()).longValueExact() }.getOrNull()
}

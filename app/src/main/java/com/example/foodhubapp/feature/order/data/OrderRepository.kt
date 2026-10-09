package com.example.foodhubapp.feature.order.data

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.menu.data.LoginRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    val description: String
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
    val items: List<OrderItem>
)

data class OrderPage(
    val orders: List<CustomerOrder>,
    val page: Int,
    val hasMore: Boolean
)

data class CreatedVnPayOrder(
    val orderId: String,
    val orderCode: String,
    val paymentUrl: String
)

data class CreatedOrder(
    val orderId: String,
    val orderCode: String,
    val paymentMethod: CheckoutPaymentMethod,
    val paymentUrl: String?,
    val qrContent: String? = null,
)

data class PaymentLaunch(val url: String, val qrContent: String? = null)

data class VnPayPaymentStatus(
    val orderId: String,
    val orderCode: String,
    val orderStatus: OrderStatus,
    val paymentStatus: String?,
    val paidAt: String?,
    val shouldPoll: Boolean
)

interface OrderRepository {
    suspend fun getHistory(page: Int, limit: Int = 20, type: OrderType? = null): OrderPage
    suspend fun cancel(orderId: String, reason: String)
    suspend fun createOrder(
        type: OrderType = OrderType.TAKEAWAY,
        paymentMethod: CheckoutPaymentMethod = CheckoutPaymentMethod.CASH,
        note: String? = null
    ): CreatedOrder
    suspend fun createVnPayOrder(type: OrderType = OrderType.TAKEAWAY, note: String? = null): CreatedVnPayOrder {
        val created = createOrder(type, CheckoutPaymentMethod.VNPAY, note)
        return CreatedVnPayOrder(
            orderId = created.orderId,
            orderCode = created.orderCode,
            paymentUrl = requireNotNull(created.paymentUrl) { "Máy chủ chưa trả đường dẫn thanh toán VNPay." }
        )
    }
    suspend fun createVnPayUrl(orderCode: String): String
    suspend fun getVnPayStatus(orderCode: String): VnPayPaymentStatus
    suspend fun createPaymentLaunch(orderCode: String, method: CheckoutPaymentMethod): PaymentLaunch
    suspend fun getPaymentStatus(orderCode: String, method: CheckoutPaymentMethod): VnPayPaymentStatus
}

/** API của màn Đơn hàng. Mọi thao tác CUSTOMER đều dùng Bearer token hiện tại. */
class RemoteOrderRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
    private val tableToken: suspend () -> String? = { null },
    private val accessToken: suspend () -> String? = { null }
) : OrderRepository {
    override suspend fun getHistory(page: Int, limit: Int, type: OrderType?): OrderPage = withContext(Dispatchers.IO) {
        require(page >= 1)
        require(limit in 1..100)
        val query = buildString {
            append("/order/history?page=$page&limit=$limit")
            type?.let { append("&type=${it.name}") }
        }
        parseOrderPage(apiClient.getJson(query, authHeaders()), page, limit)
    }

    override suspend fun cancel(orderId: String, reason: String) = withContext(Dispatchers.IO) {
        require(orderId.isNotBlank())
        require(reason.isNotBlank() && reason.length <= 255)
        apiClient.patch(
            "/order/${orderId.encoded()}/cancel",
            JSONObject().put("reason", reason.trim()),
            authHeaders()
        )
        Unit
    }

    override suspend fun createOrder(
        type: OrderType,
        paymentMethod: CheckoutPaymentMethod,
        note: String?
    ): CreatedOrder =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("paymentMethod", paymentMethod.name)
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { body.put("note", it) }

            val response = apiClient.post(
                "/order/${type.name}/new",
                body,
                orderHeaders(type)
            )
            val data = response.optJSONObject("data")
                ?: error("Máy chủ chưa trả thông tin đơn hàng VNPay.")
            val order = data.optJSONObject("order")
            val orderCode = data.optionalString("orderCode")
                ?: order?.optionalString("orderCode")
                ?: error("Máy chủ chưa trả mã đơn hàng.")
            val paymentUrl = data.optionalString("paymentUrl")
            if (paymentMethod != CheckoutPaymentMethod.CASH && paymentUrl == null) {
                error("Máy chủ chưa trả đường dẫn thanh toán ${paymentMethod.displayName()}.")
            }

            CreatedOrder(
                orderId = order?.optionalString("id").orEmpty(),
                orderCode = orderCode,
                paymentMethod = paymentMethod,
                paymentUrl = paymentUrl,
                qrContent = data.optionalString("qrCode")
            )
        }

    override suspend fun createVnPayUrl(orderCode: String): String = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        val response = apiClient.post(
            "/payment/vnpay/create",
            JSONObject().put("orderCode", orderCode),
            optionalSessionHeaders()
        )
        val data = response.optJSONObject("data")
        response.optionalString("paymentUrl")
            ?: data?.optionalString("paymentUrl")
            ?: error("Máy chủ chưa trả đường dẫn thanh toán VNPay.")
    }

    override suspend fun getVnPayStatus(orderCode: String): VnPayPaymentStatus = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        val response = apiClient.getJson(
            "/payment/vnpay/status/${orderCode.encoded()}",
            optionalSessionHeaders()
        )
        val data = response.optJSONObject("data")
            ?: error("Máy chủ chưa trả trạng thái thanh toán.")

        VnPayPaymentStatus(
            orderId = data.firstString("orderId", "id"),
            orderCode = data.firstString("orderCode", "code"),
            orderStatus = enumValueOrNull<OrderStatus>(data.optString("orderStatus")) ?: OrderStatus.UNKNOWN,
            paymentStatus = data.optionalString("paymentStatus"),
            paidAt = data.optionalString("paidAt"),
            shouldPoll = data.optBoolean("shouldPoll", false)
        )
    }

    override suspend fun createPaymentLaunch(
        orderCode: String,
        method: CheckoutPaymentMethod
    ): PaymentLaunch = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        require(method != CheckoutPaymentMethod.CASH)
        val provider = method.name.lowercase()
        val response = apiClient.post(
            "/payment/$provider/create",
            JSONObject().put("orderCode", orderCode),
            optionalSessionHeaders()
        )
        val data = response.optJSONObject("data") ?: response
        val url = data.optionalString("paymentUrl")
            ?: data.optionalString("orderUrl")
            ?: error("Máy chủ chưa trả đường dẫn thanh toán ${method.displayName()}.")
        PaymentLaunch(url = url, qrContent = data.optionalString("qrCode"))
    }

    override suspend fun getPaymentStatus(
        orderCode: String,
        method: CheckoutPaymentMethod
    ): VnPayPaymentStatus = withContext(Dispatchers.IO) {
        require(orderCode.isNotBlank())
        require(method != CheckoutPaymentMethod.CASH)
        val provider = method.name.lowercase()
        val response = apiClient.getJson(
            "/payment/$provider/status/${orderCode.encoded()}",
            optionalSessionHeaders()
        )
        val data = response.optJSONObject("data")
            ?: error("Máy chủ chưa trả trạng thái thanh toán.")
        VnPayPaymentStatus(
            orderId = data.firstString("orderId", "id"),
            orderCode = data.firstString("orderCode", "code"),
            orderStatus = enumValueOrNull<OrderStatus>(data.optString("orderStatus")) ?: OrderStatus.UNKNOWN,
            paymentStatus = data.optionalString("paymentStatus"),
            paidAt = data.optionalString("paidAt"),
            shouldPoll = data.optBoolean("shouldPoll", false)
        )
    }

    private suspend fun authHeaders(): Map<String, String> {
        val token = accessToken()?.takeIf { it.isNotBlank() } ?: throw LoginRequiredException()
        return mapOf("Authorization" to "Bearer $token")
    }

    private suspend fun orderHeaders(type: OrderType): Map<String, String> {
        if (type == OrderType.DINE_IN) {
            val token = tableToken()?.takeIf { it.isNotBlank() } ?: throw LoginRequiredException()
            return buildMap {
                put("X-Table-Token", token)
                accessToken()?.takeIf { it.isNotBlank() }?.let { put("Authorization", "Bearer $it") }
            }
        }
        return authHeaders()
    }

    private suspend fun optionalSessionHeaders(): Map<String, String> {
        tableToken()?.takeIf { it.isNotBlank() }?.let { return mapOf("X-Table-Token" to it) }
        accessToken()?.takeIf { it.isNotBlank() }?.let { return mapOf("Authorization" to "Bearer $it") }
        return emptyMap()
    }
}

fun CheckoutPaymentMethod.displayName(): String = when (this) {
    CheckoutPaymentMethod.CASH -> "tiền mặt"
    CheckoutPaymentMethod.VNPAY -> "VNPay"
    CheckoutPaymentMethod.ZALOPAY -> "ZaloPay"
}

/** Swagger chưa khai báo schema data, parser chấp nhận cả data dạng mảng và object phân trang. */
internal fun parseOrderPage(response: JSONObject, requestedPage: Int, limit: Int): OrderPage {
    val data = response.opt("data")
    val array = when (data) {
        is JSONArray -> data
        is JSONObject -> data.firstArray("orders", "items", "results", "data") ?: JSONArray()
        else -> JSONArray()
    }
    val orders = array.objects().map(::parseOrder)
    val pagination = response.optJSONObject("pagination")
        ?: (data as? JSONObject)?.optJSONObject("pagination")
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

private fun parseOrder(json: JSONObject): CustomerOrder {
    val table = json.optJSONObject("table")
    val delivery = json.optJSONObject("deliveryInfo")
    val payments = json.optJSONArray("payments")
    val payment = payments?.firstObject() ?: json.optJSONObject("payment")
    val items = json.optJSONArray("items")?.objects()?.map(::parseOrderItem).orEmpty()
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
        paymentStatus = payment?.optionalString("status") ?: json.optionalString("paymentStatus"),
        items = items
    )
}

private fun parseOrderItem(json: JSONObject): OrderItem {
    val menuItem = json.optJSONObject("menuItem") ?: json.optJSONObject("item")
    val snapshot = json.optJSONObject("snapshot")
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
            is JSONObject -> raw.firstString("name", "optionName").ifBlank { null }
            is String -> raw.takeIf { it.isNotBlank() }
            else -> null
        }
    }.orEmpty()
    val note = json.optionalString("note")
    return OrderItem(
        id = json.firstString("id", "orderItemId"),
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
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
private fun JSONArray.values(): List<Any> = (0 until length()).mapNotNull { opt(it) }
private fun JSONArray.firstObject(): JSONObject? = if (length() == 0) null else optJSONObject(0)
private fun JSONObject.firstArray(vararg keys: String): JSONArray? = keys.firstNotNullOfOrNull(::optJSONArray)
private fun JSONObject.firstString(vararg keys: String): String =
    keys.firstNotNullOfOrNull { optionalString(it) }.orEmpty()
private fun JSONObject.optionalString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
private fun JSONObject.firstInt(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
    if (!has(key) || isNull(key)) null else runCatching { get(key).toString().toInt() }.getOrNull()
}
private fun JSONObject.firstMoney(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
    if (!has(key) || isNull(key)) null
    else runCatching { BigDecimal(get(key).toString()).longValueExact() }.getOrNull()
}

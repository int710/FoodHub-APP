package com.example.foodhubapp.feature.customer.cart.data

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.customer.menu.data.LoginRequiredException
import com.example.foodhubapp.feature.customer.menu.data.RemoteFoodRepository
import com.example.foodhubapp.feature.customer.menu.ui.FoodDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.net.URLEncoder

/**
 * Ngữ cảnh đặt món theo API. Backend lưu một giỏ riêng cho từng type nên không
 * được trộn item giữa DINE_IN, TAKEAWAY và DELIVERY.
 */
enum class CartType { DINE_IN, TAKEAWAY, DELIVERY }

data class CartOption(
    val id: String,
    val name: String,
    val priceAdd: Long
)

/**
 * Một dòng trong giỏ hàng.
 *
 * [id] là cart item ID dùng cho PATCH/DELETE. [menuItemId] là ID món trong menu,
 * chỉ dùng để tải lại chi tiết món; hai giá trị này không thể thay thế cho nhau.
 */
data class CartItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val imageUrl: String?,
    val quantity: Int,
    val unitPrice: Long,
    val subTotal: Long,
    val options: List<CartOption>,
    val note: String
)

data class Cart(
    val items: List<CartItem>,
    val totalAmount: Long
)

data class CheckoutRequest(
    val type: CartType,
    val paymentMethod: String,
    val note: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val address: String = "",
)

data class CheckoutResult(
    val orderId: String,
    val orderCode: String,
    val paymentUrl: String?,
)

/** Body của PATCH cart item; theo docs phải có ít nhất một trường khác null. */
data class CartItemUpdate(
    val quantity: Int? = null,
    val variantOptionIds: List<String>? = null,
    val note: String? = null
)

/**
 * Contract của tầng dữ liệu cart, ánh xạ trực tiếp tới nhóm `/cart/{type}`:
 * GET items, PATCH item, DELETE item và DELETE clear.
 */
interface CartRepository {
    suspend fun getCart(type: CartType = CartType.TAKEAWAY): Cart
    suspend fun updateItem(itemId: String, update: CartItemUpdate, type: CartType = CartType.TAKEAWAY)
    suspend fun deleteItem(itemId: String, type: CartType = CartType.TAKEAWAY)
    suspend fun clear(type: CartType = CartType.TAKEAWAY)
    suspend fun checkout(request: CheckoutRequest): CheckoutResult
}

/**
 * Repository gọi API thật. Mọi request cart hiện dùng Bearer token CUSTOMER;
 * công việc mạng được chuyển sang Dispatchers.IO để không chặn main thread.
 */
class RemoteCartRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
    private val tableToken: () -> String? = { null },
    private val accessToken: suspend () -> String? = { null },
) : CartRepository {
    override suspend fun getCart(type: CartType): Cart = withContext(Dispatchers.IO) {
        val response = apiClient.getJson("/cart/${type.name}/items", requestHeaders(type))
        completeCartItems(response)
        parseCart(response)
    }

    /** Older servers return Redis cart entries without menu display data or prices. */
    private suspend fun completeCartItems(response: JSONObject) {
        val foodRepository = RemoteFoodRepository(apiClient)
        val foodById = mutableMapOf<String, FoodDetail>()
        for (item in response.cartItems().objects()) {
            val menu = item.optJSONObject("menuItem") ?: item.optJSONObject("item")
            val hasName = item.firstString("name", "menuItemName").isNotBlank() ||
                menu?.optionalString("name") != null
            val hasPrice = item.firstMoney("unitPrice", "price", "basePrice") != null ||
                menu?.firstMoney("salePrice", "basePrice", "price") != null
            if (hasName && hasPrice) continue
            val menuItemId = item.firstString("menuItemId").ifBlank { menu?.optionalString("id").orEmpty() }
            require(menuItemId.isNotBlank()) { "Không xác định được món trong giỏ hàng. Vui lòng tải lại giỏ." }
            val food = foodById[menuItemId] ?: foodRepository.getFood(menuItemId).also {
                foodById[menuItemId] = it
            }
            if (!hasName) item.put("name", food.name)
            if (item.optionalString("image") == null && item.optionalString("imageUrl") == null &&
                menu?.optionalString("image") == null && menu?.optionalString("imageUrl") == null) {
                item.put("imageUrl", food.imageUrl ?: JSONObject.NULL)
            }
            var options = item.firstArray("variantOptions", "selectedOptions", "options")?.cartOptions()
            if (options == null) {
                val ids = item.optJSONArray("variantOptionIds")
                val selectedIds = ids?.let { array -> (0 until array.length()).map { array.getString(it) } }.orEmpty()
                val availableOptions = food.groups.flatMap { it.options }.associateBy { it.id }
                options = selectedIds.map { id ->
                    val option = availableOptions[id]
                        ?: error("Tùy chọn món không còn hợp lệ. Vui lòng chỉnh sửa món trong giỏ.")
                    CartOption(option.id, option.name, option.priceAdd)
                }
                item.put("variantOptions", JSONArray(options.map { option ->
                    JSONObject().put("id", option.id).put("name", option.name).put("priceAdd", option.priceAdd)
                }))
            }
            if (!hasPrice) {
                item.put("unitPrice", (food.salePrice ?: food.basePrice) + options.sumOf { it.priceAdd })
            }
        }
    }

    override suspend fun updateItem(itemId: String, update: CartItemUpdate, type: CartType) =
        withContext(Dispatchers.IO) {
            // Chặn request sai contract trước khi gửi lên server.
            require(itemId.isNotBlank())
            require(update.quantity != null || update.variantOptionIds != null || update.note != null) {
                "Cần ít nhất một thay đổi cho món trong giỏ."
            }
            update.quantity?.let { require(it in 1..99) }
            update.note?.let { require(it.length <= 255) }
            // Chỉ đưa trường được sửa vào body PATCH; null nghĩa là giữ nguyên.
            val body = JSONObject()
            update.quantity?.let { body.put("quantity", it) }
            update.variantOptionIds?.let { body.put("variantOptionIds", JSONArray(it.distinct())) }
            update.note?.let { body.put("note", it) }
            apiClient.patch(
                "/cart/${type.name}/items/${itemId.encoded()}",
                body,
                requestHeaders(type)
            )
            Unit
        }

    override suspend fun deleteItem(itemId: String, type: CartType) = withContext(Dispatchers.IO) {
        require(itemId.isNotBlank())
        apiClient.delete("/cart/${type.name}/items/${itemId.encoded()}", requestHeaders(type))
        Unit
    }

    override suspend fun clear(type: CartType) = withContext(Dispatchers.IO) {
        apiClient.delete("/cart/${type.name}/clear", requestHeaders(type))
        Unit
    }

    override suspend fun checkout(request: CheckoutRequest): CheckoutResult = withContext(Dispatchers.IO) {
        require(request.paymentMethod in setOf("CASH", "VNPAY"))
        if (request.type == CartType.DELIVERY) {
            require(request.recipientName.isNotBlank() && request.phone.isNotBlank() && request.address.isNotBlank()) {
                "Vui lòng nhập đủ thông tin giao hàng."
            }
        }
        val body = JSONObject()
            .put("note", request.note.trim())
            .put("paymentMethod", request.paymentMethod)
        if (request.type == CartType.DELIVERY) {
            body.put(
                "deliveryInfo",
                JSONObject()
                    .put("recipientName", request.recipientName.trim())
                    .put("phone", request.phone.trim())
                    .put("address", request.address.trim()),
            )
        }
        val response = apiClient.post("/order/${request.type.name}/new", body, requestHeaders(request.type))
        val data = response.optJSONObject("data") ?: response
        val order = data.optJSONObject("order") ?: data
        CheckoutResult(
            orderId = order.optString("id"),
            orderCode = data.optString("orderCode", order.optString("orderCode")),
            paymentUrl = data.optionalString("paymentUrl") ?: response.optionalString("paymentUrl"),
        )
    }

    private suspend fun requestHeaders(type: CartType): Map<String, String> {
        if (type == CartType.DINE_IN) {
            val token = tableToken()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("Phiên bàn không còn hợp lệ. Vui lòng quét lại QR.")
            val headers = mutableMapOf("x-table-token" to token)
            accessToken()?.takeIf(String::isNotBlank)?.let { headers["Authorization"] = "Bearer $it" }
            return headers
        }
        val token = accessToken()?.takeIf(String::isNotBlank) ?: throw LoginRequiredException()
        return mapOf("Authorization" to "Bearer $token")
    }
}

/**
 * Chuyển ApiResponse JSON thành model UI. Swagger chưa mô tả schema `data`
 * chi tiết, nên parser chấp nhận cả data dạng mảng và object chứa items/cartItems.
 */
internal fun parseCart(response: JSONObject): Cart {
    val data = response.opt("data")
    val itemsArray = response.cartItems()
    val items = itemsArray.objects().map(::parseCartItem)
    val container = data as? JSONObject
    // Ưu tiên tổng do backend trả về vì backend quyết định sale/thuế; phép cộng chỉ là fallback.
    val total = container?.firstMoney("totalAmount", "total", "subtotal", "subTotal")
        ?: items.sumOf { it.subTotal }
    return Cart(items, total)
}

private fun parseCartItem(json: JSONObject): CartItem {
    val menuItem = json.optJSONObject("menuItem") ?: json.optJSONObject("item")
    val optionsArray = json.firstArray("variantOptions", "selectedOptions", "options")
    val options = optionsArray?.cartOptions().orEmpty()
    val quantity = json.optInt("quantity", 1).coerceIn(1, 99)
    val unitPrice = json.firstMoney("unitPrice", "price", "basePrice")
        ?: menuItem?.firstMoney("salePrice", "basePrice", "price")
        ?: 0L
    val subTotal = json.firstMoney("subTotal", "subtotal", "totalPrice", "itemTotal")
        ?: ((if (json.firstMoney("unitPrice") != null) unitPrice else unitPrice + options.sumOf { it.priceAdd }) * quantity)
    return CartItem(
        id = json.firstString("id", "cartItemId"),
        menuItemId = json.firstString("menuItemId").ifBlank { menuItem?.optString("id").orEmpty() },
        name = json.firstString("name", "menuItemName").ifBlank {
            menuItem?.optString("name").orEmpty().ifBlank { "Món ăn" }
        },
        imageUrl = json.optionalString("image") ?: json.optionalString("imageUrl")
            ?: menuItem?.optionalString("image") ?: menuItem?.optionalString("imageUrl"),
        quantity = quantity,
        unitPrice = unitPrice,
        subTotal = subTotal,
        options = options,
        note = json.optionalString("note").orEmpty()
    )
}

private fun String.encoded() = URLEncoder.encode(this, "UTF-8")
private fun JSONObject.cartItems(): JSONArray = when (val data = opt("data")) {
    is JSONArray -> data
    is JSONObject -> data.optJSONArray("items") ?: data.optJSONArray("cartItems") ?: JSONArray()
    else -> JSONArray()
}
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.cartOptions(): List<CartOption> = (0 until length()).mapNotNull { index ->
    when (val raw = opt(index)) {
        is JSONObject -> {
            val option = raw.optJSONObject("option") ?: raw
            CartOption(
                id = option.firstString("id", "variantOptionId"),
                name = option.firstString("name", "optionName").ifBlank { "Tùy chọn" },
                priceAdd = option.firstMoney("priceAdd", "price") ?: 0L
            )
        }
        is String -> CartOption(raw, raw, 0L)
        else -> null
    }
}
private fun JSONObject.firstArray(vararg keys: String): JSONArray? =
    keys.firstNotNullOfOrNull { optJSONArray(it) }
private fun JSONObject.firstString(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> optionalString(key) }.orEmpty()
private fun JSONObject.optionalString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
private fun JSONObject.firstMoney(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
    if (!has(key) || isNull(key)) null
    else runCatching { BigDecimal(get(key).toString()).longValueExact() }.getOrNull()
}

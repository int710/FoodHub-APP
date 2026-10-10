package com.example.foodhubapp.feature.customer.menu.data

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.feature.customer.menu.ui.FoodCartSelection
import com.example.foodhubapp.feature.customer.menu.ui.FoodDetail
import com.example.foodhubapp.feature.customer.menu.ui.FoodOption
import com.example.foodhubapp.feature.customer.menu.ui.FoodOptionGroup
import com.example.foodhubapp.feature.customer.cart.data.CartType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Model đại diện cho một món ăn hiển thị trong danh sách thực đơn.
 */
data class MenuFood(
    val id: String, 
    val name: String, 
    val price: Long, 
    val imageUrl: String?,
    val description: String = "",
    val rating: String = "0",
    val isFeatured: Boolean = false,
    val totalOrder: Int = 0,
    val available: Boolean = true
)

/**
 * Model đại diện cho danh mục món ăn (ví dụ: Burger, Pizza, Tráng miệng).
 */
data class MenuCategory(
    val id: String, 
    val name: String, 
    val items: List<MenuFood>
)

data class FoodReview(
    val id: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val createdAt: String,
)

/**
 * Ngoại lệ ném ra khi người dùng chưa đăng nhập nhưng cố gắng thêm món vào giỏ hàng.
 */
class LoginRequiredException :
    IllegalStateException("Vui lòng đăng nhập để thêm món vào giỏ mang đi.")

/**
 * Giao diện (Interface) định nghĩa các phương thức làm việc với dữ liệu thực đơn và món ăn.
 */
interface FoodRepository {
    /** Lấy toàn bộ danh sách thực đơn được nhóm theo danh mục. */
    suspend fun getMenu(): List<MenuCategory>

    /** Lấy thông tin chi tiết của một món ăn theo ID. */
    suspend fun getFood(id: String): FoodDetail

    suspend fun getReviews(id: String): List<FoodReview> = emptyList()

    /** Thêm món ăn được chọn vào giỏ hàng trên server. */
    suspend fun addToCart(selection: FoodCartSelection)
}

/**
 * Lớp thực thi kết nối API thực tế [FoodRepository] qua [FoodHubApiClient].
 */
class RemoteFoodRepository(
    private val apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient,
    private val cartType: () -> CartType = { CartType.TAKEAWAY },
    private val tableToken: () -> String? = { null },
    private val accessToken: suspend () -> String? = { null },
) : FoodRepository {

    /**
     * Tải danh sách toàn bộ danh mục và món ăn từ API /menu/all.
     */
    override suspend fun getMenu(): List<MenuCategory> = withContext(Dispatchers.IO) {
        val data = apiClient.execute(apiClient.menuApi.getMenu()).getArray("data")
        data.objects().map { category ->
            MenuCategory(
                id = category.getString("id"),
                name = category.getString("name"),
                items = category.getArray("items").objects().map { item ->
                    MenuFood(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        price = item.optionalMoney("salePrice") ?: item.money("basePrice"),
                        imageUrl = item.optionalString("image"),
                        description = item.optionalString("description").orEmpty(),
                        rating = item.optionalString("avgRating") ?: "0",
                        isFeatured = item.optBoolean("isFeatured", false),
                        totalOrder = item.optInt("totalOrder", 0),
                        available = item.optBoolean("isAvailable", true)
                    )
                }
            )
        }
    }

    /**
     * Tải thông tin chi tiết một món ăn theo [id] từ API /menu/item/{id}.
     */
    override suspend fun getFood(id: String): FoodDetail = withContext(Dispatchers.IO) {
        require(id.isNotBlank()) { "ID món ăn không được để trống" }
        // Mã hóa URL đường dẫn ID để tránh ký tự đặc biệt
        val encodedId = URLEncoder.encode(id, "UTF-8")
        val responseJson = apiClient.execute(
            apiClient.menuApi.getFood(
                id = encodedId
            )
        ).getObject("data")
        parseFoodDetail(responseJson)
    }

    override suspend fun getReviews(id: String): List<FoodReview> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, "UTF-8")
        val response = apiClient.execute(
            apiClient.reviewApi.getReviews(
                id = encodedId
            )
        )
        val payload = response.optArray("data")
            ?: response.optObject("data")?.optArray("data") ?: JsonArray()
        payload.objects().map { review ->
            FoodReview(
                id = review.optString("id", review.optString("_id")),
                customerName = review.optObject("customer")?.optString("name")
                    ?.takeIf(String::isNotBlank) ?: "Khách hàng",
                rating = review.optInt("rating", 0),
                comment = review.optionalString("comment").orEmpty(),
                createdAt = review.optString("createdAt"),
            )
        }
    }

    /**
     * Gửi request thêm món vào giỏ hàng mang đi (TAKEAWAY) lên server.
     */
    override suspend fun addToCart(selection: FoodCartSelection) = withContext(Dispatchers.IO) {
        // Kiểm tra tính hợp lệ của số lượng (1-99) và ghi chú (tối đa 255 ký tự)
        require(selection.quantity in 1..99 && selection.note.length <= 255) {
            "Số lượng hoặc ghi chú không hợp lệ"
        }

        val type = cartType()
        val headers = if (type == CartType.DINE_IN) {
            val token = tableToken()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("Phiên bàn không còn hợp lệ. Vui lòng quét lại QR.")
            mutableMapOf("x-table-token" to token).apply {
                accessToken()?.takeIf(String::isNotBlank)?.let { put("Authorization", "Bearer $it") }
            }
        } else {
            val token = accessToken()?.takeIf(String::isNotBlank) ?: throw LoginRequiredException()
            mapOf("Authorization" to "Bearer $token")
        }

        // Tạo JSON body gửi lên API /cart/TAKEAWAY/items/add
        val bodyJson = JsonObject()
            .put("menuItemId", selection.menuItemId)
            .put("quantity", selection.quantity)
            .put("variantOptionIds", jsonArray(selection.variantOptionIds))
            .put("note", selection.note)

        apiClient.execute(
            apiClient.cartApi.addItem(
                type = type.name,
                headers = headers,
                body = bodyJson
            )
        )
        Unit
    }
}

/**
 * Hàm hỗ trợ phân tích dữ liệu [JsonObject] trả về từ server thành đối tượng [FoodDetail].
 * Xử lý nhóm tùy chọn (Size, Topping) và tính toán giá khuyến mãi Flash Sale nếu có.
 */
fun parseFoodDetail(data: JsonObject, now: Long = System.currentTimeMillis()): FoodDetail {
    // 1. Parse danh sách các nhóm tùy chọn (Variant Groups: Size, Topping)
    val groups = data.optArray("variantGroups")?.objects().orEmpty().map { group ->
        val type = group.getString("type")
        require(type == "SINGLE" || type == "MULTIPLE") { "Loại tùy chọn món không được hỗ trợ." }

        FoodOptionGroup(
            id = group.getString("id"),
            name = group.getString("name"),
            multiple = type == "MULTIPLE",
            required = group.getBoolean("isRequired"),
            options = group.getArray("options").objects()
                .filter { it.optBoolean("isActive", true) } // Chỉ lấy các tùy chọn đang hoạt động
                .map { FoodOption(it.getString("id"), it.getString("name"), it.money("priceAdd")) }
        )
    }

    val basePrice = data.money("basePrice")
    
    // 2. Kiểm tra chương trình Flash Sale (nếu có và còn thời hạn hoạt động)
    val sale = data.optObject("flashSale")
    val discount = sale?.optString("discountPercent")?.toBigDecimalOrNull()

    val salePrice = if (
        sale != null && 
        sale.optBoolean("isActive") && 
        discount != null && 
        discount > BigDecimal.ZERO && 
        discount <= BigDecimal(100) && 
        sale.optionalString("startsAt")?.let(::timestamp)?.let { it <= now } == true && 
        sale.optionalString("endsAt")?.let(::timestamp)?.let { it > now } == true
    ) {
        // Áp dụng công thức giảm giá: basePrice * (1 - discount / 100)
        BigDecimal(basePrice)
            .multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal(100))))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    } else {
        null
    }

    // 3. Trả về đối tượng FoodDetail hoàn chỉnh
    return FoodDetail(
        id = data.getString("id"),
        name = data.getString("name"),
        description = data.optionalString("description").orEmpty(),
        basePrice = basePrice,
        rating = data.optionalString("avgRating") ?: "0",
        available = data.getBoolean("isAvailable"),
        groups = groups,
        imageUrl = data.optionalString("image"),
        salePrice = salePrice
    )
}

/**
 * Hàm phân tích định dạng chuỗi thời gian ISO 8601 sang timestamp (Miliseconds).
 */
private fun timestamp(value: String): Long? {
    for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX")) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)?.time
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

/** Extension biến [JsonArray] thành [List<JsonObject>] để dễ duyệt danh sách. */
private fun JsonArray.objects(): List<JsonObject> = (0 until size()).map { getObject(it) }

/** Extension lấy chuỗi [String?] an toàn từ [JsonObject]. */
private fun JsonObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

/** Extension chuyển đổi giá tiền từ JSON sang kiểu số nguyên [Long] dùng [BigDecimal] để đảm bảo độ chính xác. */
private fun JsonObject.money(key: String): Long = BigDecimal(getString(key)).longValueExact()

/** Extension lấy giá tiền [Long?] không bắt buộc. */
private fun JsonObject.optionalMoney(key: String): Long? = if (isNull(key)) null else money(key)

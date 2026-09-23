package com.example.foodhubapp.feature.menu.data

import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.menu.model.HomeMenuData
import com.example.foodhubapp.feature.menu.model.MenuCategoryUiModel
import com.example.foodhubapp.feature.menu.model.MenuItemDetailUiModel
import com.example.foodhubapp.feature.menu.model.MenuItemUiModel
import com.example.foodhubapp.feature.menu.model.VariantGroupUiModel
import com.example.foodhubapp.feature.menu.model.VariantOptionUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Kết quả đồng bộ thực đơn từ server.
 */
data class MenuSyncResult(
    val categoriesCount: Int,
    val menuItemsCount: Int
)

/**
 * Giao diện (Interface) định nghĩa các thao tác lấy dữ liệu Thực đơn (Menu) từ nguồn.
 */
interface MenuRepository {
    /**
     * Đồng bộ dữ liệu menu và trả về số lượng danh mục và món ăn.
     */
    suspend fun syncMenu(): MenuSyncResult

    /**
     * Lấy toàn bộ menu public từ API /menu/all.
     * Dữ liệu trả về đã được nhóm theo danh mục, đúng cấu trúc backend.
     */
    suspend fun getMenu(): Result<List<MenuCategoryUiModel>>

    /**
     * Lấy menu cho màn Home.
     * Repository vẫn gọi cùng API /menu/all, sau đó chia dữ liệu thành các section UI cần dùng.
     */
    suspend fun getHomeMenu(): Result<HomeMenuData>

    /**
     * Lấy chi tiết một món khi người dùng mở màn chi tiết món.
     */
    suspend fun getItemDetail(id: String): Result<MenuItemDetailUiModel>
}

/**
 * Lớp thực thi của [MenuRepository], chịu trách nhiệm gọi API qua [FoodHubApiClient]
 * và phân tích cú pháp (parse) JSON trả về từ server thành các UI Model tương ứng.
 */
class RemoteMenuRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient()
) : MenuRepository {

    override suspend fun syncMenu(): MenuSyncResult = withContext(Dispatchers.IO) {
        val categories = apiClient.get("/menu/categories")
        val menu = apiClient.get("/menu/all")

        MenuSyncResult(
            categoriesCount = categories.dataCount,
            menuItemsCount = menu.dataCount
        )
    }

    /**
     * Gọi API /menu/all trên luồng IO và parse kết quả thành danh sách [MenuCategoryUiModel].
     */
    override suspend fun getMenu(): Result<List<MenuCategoryUiModel>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiClient.getJson("/menu/all")
            val data = json.optJSONArray("data") ?: JSONArray()

            // API trả data là mảng category; mỗi category chứa mảng items.
            List(data.length()) { index ->
                data.getJSONObject(index).toMenuCategoryUiModel()
            }
        }
    }

    /**
     * Lấy và xử lý dữ liệu cho màn hình Home từ danh sách menu đầy đủ.
     * Sắp xếp các món bán chạy (best sellers) và món gợi ý (suggestions).
     */
    override suspend fun getHomeMenu(): Result<HomeMenuData> = withContext(Dispatchers.IO) {
        getMenu().map { categories ->
            val allItems = categories.flatMap { it.items }

            // Backend chưa có endpoint riêng cho "bán chạy", nên dùng totalOrder làm tiêu chí.
            val bestSellers = allItems
                .sortedWith(
                    compareByDescending<MenuItemUiModel> { it.totalOrder }
                        .thenByDescending { it.avgRating }
                )
                .take(2)

            // Gợi ý tạm thời dựa trên rating/lượt đặt, loại các món đã nằm trong bán chạy.
            val suggestions = allItems
                .sortedWith(
                    compareByDescending<MenuItemUiModel> { it.avgRating }
                        .thenByDescending { it.totalOrder }
                )
                .filterNot { item -> bestSellers.any { it.id == item.id } }
                .take(3)

            HomeMenuData(
                categories = categories,
                bestSellers = bestSellers,
                suggestions = suggestions
            )
        }
    }

    /**
     * Gọi API lấy chi tiết món ăn theo ID.
     */
    override suspend fun getItemDetail(id: String): Result<MenuItemDetailUiModel> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiClient.getJson("/menu/item/$id")
            val data = json.optJSONObject("data")
                ?: throw IllegalStateException("API không trả dữ liệu chi tiết món")

            data.toMenuItemDetailUiModel()
        }
    }
}

/**
 * Hàm mở rộng (Extension function) parse [JSONObject] thành [MenuCategoryUiModel].
 */
private fun JSONObject.toMenuCategoryUiModel(): MenuCategoryUiModel {
    val itemsJson = optJSONArray("items") ?: JSONArray()

    // Field icon từ backend là String?, không phải drawable resource.
    return MenuCategoryUiModel(
        id = optString("id"),
        name = optString("name"),
        icon = optNullableString("icon"),
        sortOrder = optInt("sortOrder", 0),
        items = List(itemsJson.length()) { index ->
            itemsJson.getJSONObject(index).toMenuItemUiModel()
        }
    )
}

/**
 * Hàm mở rộng parse [JSONObject] thành [MenuItemUiModel].
 */
private fun JSONObject.toMenuItemUiModel(): MenuItemUiModel {
    // Model ngắn gọn cho list/home; /menu/all hiện chưa trả description.
    return MenuItemUiModel(
        id = optString("id"),
        name = optString("name"),
        description = optNullableString("description"),
        imageUrl = optNullableString("image"),
        basePrice = optFlexibleDouble("basePrice"),
        salePrice = optFlexibleNullableDouble("salePrice"),
        salePercent = optFlexibleNullableDouble("salePercent"),
        saleEndsAt = optNullableString("saleEndsAt"),
        totalOrder = optInt("totalOrder", 0),
        avgRating = optFlexibleDouble("avgRating"),
        sortOrder = optInt("sortOrder", 0)
    )
}

/**
 * Hàm mở rộng parse [JSONObject] thành [MenuItemDetailUiModel].
 */
private fun JSONObject.toMenuItemDetailUiModel(): MenuItemDetailUiModel {
    val variantGroupsJson = optJSONArray("variantGroups") ?: JSONArray()

    // Chi tiết món có thêm description, trạng thái món và các nhóm variant.
    return MenuItemDetailUiModel(
        id = optString("id"),
        categoryId = optString("categoryId"),
        name = optString("name"),
        description = optNullableString("description"),
        imageUrl = optNullableString("image"),
        basePrice = optFlexibleDouble("basePrice"),
        salePrice = optFlexibleNullableDouble("salePrice"),
        salePercent = optFlexibleNullableDouble("salePercent"),
        saleEndsAt = optNullableString("saleEndsAt"),
        isAvailable = optBoolean("isAvailable", true),
        isFeatured = optBoolean("isFeatured", false),
        totalOrder = optInt("totalOrder", 0),
        avgRating = optFlexibleDouble("avgRating"),
        variantGroups = List(variantGroupsJson.length()) { index ->
            variantGroupsJson.getJSONObject(index).toVariantGroupUiModel()
        }
    )
}

/**
 * Hàm mở rộng parse [JSONObject] thành [VariantGroupUiModel].
 */
private fun JSONObject.toVariantGroupUiModel(): VariantGroupUiModel {
    val optionsJson = optJSONArray("options") ?: JSONArray()

    return VariantGroupUiModel(
        id = optString("id"),
        name = optString("name"),
        type = optString("type"),
        isRequired = optBoolean("isRequired", true),
        sortOrder = optInt("sortOrder", 0),
        options = List(optionsJson.length()) { index ->
            optionsJson.getJSONObject(index).toVariantOptionUiModel()
        }
    )
}

/**
 * Hàm mở rộng parse [JSONObject] thành [VariantOptionUiModel].
 */
private fun JSONObject.toVariantOptionUiModel(): VariantOptionUiModel {
    return VariantOptionUiModel(
        id = optString("id"),
        name = optString("name"),
        priceAdd = optFlexibleDouble("priceAdd"),
        sortOrder = optInt("sortOrder", 0),
        isActive = optBoolean("isActive", true)
    )
}

/**
 * Trả về String? nếu giá trị là null hoặc rỗng.
 */
private fun JSONObject.optNullableString(key: String): String? {
    return if (isNull(key)) null else optString(key).ifBlank { null }
}

/**
 * Lấy giá trị Double linh hoạt (mặc định 0.0 nếu không có).
 */
private fun JSONObject.optFlexibleDouble(key: String): Double {
    return optFlexibleNullableDouble(key) ?: 0.0
}

/**
 * Xử lý dữ liệu kiểu số thập phân (Decimal) từ Prisma:
 * Có lúc trả về kiểu Number, có lúc trả về String.
 */
private fun JSONObject.optFlexibleNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)

    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        null -> null
        else -> null
    }
}

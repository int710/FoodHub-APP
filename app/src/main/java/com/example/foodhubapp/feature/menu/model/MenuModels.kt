package com.example.foodhubapp.feature.menu.model

/**
 * Model cho từng danh mục món ăn từ API GET /menu/all.
 *
 * Backend trả field icon là String? nên không dùng @DrawableRes Int ở đây.
 */
data class MenuCategoryUiModel(
    val id: String,
    val name: String,
    val icon: String?,
    val sortOrder: Int,
    val items: List<MenuItemUiModel>
)

/**
 * Model món ăn ngắn gọn dùng cho Home và màn danh sách tất cả món.
 *
 * Field description hiện không có trong GET /menu/all, nên để nullable để sau này
 * có thể lấy từ GET /menu/item/{id} hoặc backend bổ sung vào response list.
 */
data class MenuItemUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val basePrice: Double,
    val salePrice: Double?,
    val salePercent: Double?,
    val saleEndsAt: String?,
    val totalOrder: Int,
    val avgRating: Double,
    val sortOrder: Int
)

/**
 * Dữ liệu đã được Repository/ViewModel chia sẵn cho màn Home.
 */
data class HomeMenuData(
    val categories: List<MenuCategoryUiModel>,
    val bestSellers: List<MenuItemUiModel>,
    val suggestions: List<MenuItemUiModel>
)

/**
 * Model chi tiết món ăn từ API GET /menu/item/{id}.
 */
data class MenuItemDetailUiModel(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val basePrice: Double,
    val salePrice: Double?,
    val salePercent: Double?,
    val saleEndsAt: String?,
    val isAvailable: Boolean,
    val isFeatured: Boolean,
    val totalOrder: Int,
    val avgRating: Double,
    val variantGroups: List<VariantGroupUiModel>
)

/**
 * Nhóm tùy chọn của món, ví dụ size, topping, độ cay.
 */
data class VariantGroupUiModel(
    val id: String,
    val name: String,
    val type: String,
    val isRequired: Boolean,
    val sortOrder: Int,
    val options: List<VariantOptionUiModel>
)

/**
 * Một tùy chọn cụ thể trong nhóm variant.
 */
data class VariantOptionUiModel(
    val id: String,
    val name: String,
    val priceAdd: Double,
    val sortOrder: Int,
    val isActive: Boolean
)

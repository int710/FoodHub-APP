package com.kma.foodhub.feature.customer.menu.model

data class MenuFood(
    val id: String,
    val name: String,
    val price: Long,
    val basePrice: Long = price,
    val salePercent: Double? = null,
    val imageUrl: String?,
    val description: String = "",
    val rating: String = "0",
    val isFeatured: Boolean = false,
    val totalOrder: Int = 0,
    val isBestSeller: Boolean = false,
    val available: Boolean = true,
)

data class MenuCategory(
    val id: String,
    val name: String,
    val items: List<MenuFood>,
)

data class FoodReview(
    val id: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val createdAt: String,
    val images: List<String> = emptyList(),
)

data class FoodOption(
    val id: String,
    val name: String,
    val priceAdd: Long,
)

data class FoodOptionGroup(
    val id: String,
    val name: String,
    val multiple: Boolean,
    val required: Boolean,
    val options: List<FoodOption>,
)

data class FoodDetail(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Long,
    val rating: String,
    val available: Boolean = true,
    val groups: List<FoodOptionGroup> = emptyList(),
    val imageUrl: String? = null,
    val salePrice: Long? = null,
    val totalOrder: Int = 0,
    val isBestSeller: Boolean = false,
)

data class FoodCartSelection(
    val menuItemId: String,
    val quantity: Int,
    val variantOptionIds: List<String>,
    val note: String,
)

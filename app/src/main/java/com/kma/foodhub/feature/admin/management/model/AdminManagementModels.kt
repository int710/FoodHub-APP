package com.kma.foodhub.feature.admin.management.model

data class AdminReview(
    val id: String,
    val rating: Int,
    val comment: String,
    val images: List<String>,
    val createdAt: String,
    val customerName: String,
    val customerAvatar: String?,
    val menuItemId: String,
    val menuItemName: String,
    val menuItemImage: String?,
    val orderCode: String,
)

data class SystemSetting(
    val appName: String = "FoodHub",
    val restaurantName: String = "FoodHub",
    val supportEmail: String = "",
    val supportPhone: String = "",
    val address: String = "",
    val logoUrl: String = "",
    val vatRate: Double = 10.0,
    val serviceFeeRate: Double = 0.0,
    val defaultDeliveryFee: Long = 0,
    val currency: String = "VND",
)

data class AdminVoucher(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val discountType: String = "PERCENT",
    val distribution: String = "SECRET",
    val discountValue: Double = 0.0,
    val minOrderAmount: Long = 0,
    val maxDiscountAmount: Long? = null,
    val applicableOrderTypes: List<String> = listOf("DINE_IN", "TAKEAWAY", "DELIVERY"),
    val startsAt: String = "",
    val endsAt: String = "",
    val usageLimit: Int? = null,
    val usedCount: Int = 0,
    val perCustomerLimit: Int = 1,
    val isActive: Boolean = true,
)

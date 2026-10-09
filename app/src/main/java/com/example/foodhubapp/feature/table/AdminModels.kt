package com.example.foodhubapp.feature.table

enum class AdminOrderStatus(val label: String) {
    PENDING_PAYMENT("Chờ thanh toán"),
    PENDING_CONFIRMATION("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PREPARING("Đang chế biến"),
    READY("Sẵn sàng"),
    SERVED("Đã phục vụ"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    PAYMENT_FAILED("Thanh toán lỗi"),
}

enum class AdminOrderType(val label: String) {
    DINE_IN("Tại bàn"),
    TAKEAWAY("Mang về"),
    DELIVERY("Giao hàng"),
}

enum class AdminPaymentMethod(val label: String) {
    CASH("Tiền mặt"),
    VNPAY("VNPay"),
    MOMO("MoMo"),
}

enum class AdminItemStatus(val label: String) {
    WAITING("Chờ làm"),
    PREPARING("Đang làm"),
    READY("Đã xong"),
    SERVED("Đã phục vụ"),
}

data class AdminOrderLine(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val options: List<String> = emptyList(),
    val note: String? = null,
    val status: AdminItemStatus = AdminItemStatus.WAITING,
    val id: String = "",
)

data class AdminOrder(
    val id: String,
    val code: String,
    val type: AdminOrderType,
    val destination: String,
    val createdAt: String,
    val waitMinutes: Int,
    val items: List<AdminOrderLine>,
    val total: Long,
    val status: AdminOrderStatus,
    val paymentMethod: AdminPaymentMethod,
    val paid: Boolean,
)

data class AdminMenuItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Long,
    val salePrice: Long? = null,
    val isAvailable: Boolean,
    val isFeatured: Boolean,
    val icon: String,
    val categoryId: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val sortOrder: Int = 0,
)

data class AdminMenuCategory(
    val id: String,
    val name: String,
    val icon: String = "🍽",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val itemCount: Int = 0,
)

data class AdminMenuCategoryInput(
    val name: String,
    val icon: String,
    val sortOrder: Int,
)

data class AdminMenuInput(
    val categoryId: String,
    val name: String,
    val description: String?,
    val basePrice: Long,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val isFeatured: Boolean,
    val sortOrder: Int,
)

data class AdminFlashSaleInput(val itemId: String, val discountPercent: Double, val durationHours: Int)
data class AdminVariantOptionInput(val name: String, val priceAdd: Long)
data class AdminVariantInput(
    val name: String,
    val multiple: Boolean,
    val required: Boolean,
    val options: List<AdminVariantOptionInput>,
)

data class AdminConversation(
    val id: String,
    val customer: String,
    val lastMessage: String,
    val time: String,
    val unread: Int,
    val isOnline: Boolean,
    val orderCode: String? = null,
)

data class AdminUiMessage(
    val id: String,
    val content: String,
    val time: String,
    val fromAdmin: Boolean,
)

data class AdminRestaurantTable(
    val id: String,
    val name: String,
    val capacity: Int,
    val floor: String?,
    val isActive: Boolean,
    val note: String? = null,
)

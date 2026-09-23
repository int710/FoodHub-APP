package com.foodhub.app

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

data class AdminOrderLine(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val options: List<String> = emptyList(),
    val note: String? = null,
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

object AdminMockData {
    val orders = listOf(
        AdminOrder(
            id = "order-9082",
            code = "#FH-9082",
            type = AdminOrderType.DINE_IN,
            destination = "Bàn 05 • Tầng 1",
            createdAt = "12:20",
            waitMinutes = 8,
            items = listOf(
                AdminOrderLine("Truffle Smash Burger", 2, 145_000, listOf("Double phô mai")),
                AdminOrderLine("Trà đào cam sả", 2, 55_000, note = "Ít đá"),
            ),
            total = 432_000,
            status = AdminOrderStatus.PENDING_CONFIRMATION,
            paymentMethod = AdminPaymentMethod.VNPAY,
            paid = true,
        ),
        AdminOrder(
            id = "order-8942",
            code = "#FH-8942",
            type = AdminOrderType.DELIVERY,
            destination = "124 Keangnam Landmark",
            createdAt = "11:48",
            waitMinutes = 20,
            items = listOf(
                AdminOrderLine("Pizza hải sản sốt Pesto", 1, 185_000),
                AdminOrderLine("Cánh gà cay Gochujang", 1, 119_000),
            ),
            total = 328_320,
            status = AdminOrderStatus.PREPARING,
            paymentMethod = AdminPaymentMethod.CASH,
            paid = false,
        ),
        AdminOrder(
            id = "order-9101",
            code = "#FH-9101",
            type = AdminOrderType.TAKEAWAY,
            destination = "Mã nhận TA-18",
            createdAt = "12:31",
            waitMinutes = 3,
            items = listOf(AdminOrderLine("Combo Burger FoodHub", 1, 199_000)),
            total = 199_000,
            status = AdminOrderStatus.READY,
            paymentMethod = AdminPaymentMethod.MOMO,
            paid = true,
        ),
        AdminOrder(
            id = "order-8874",
            code = "#FH-8874",
            type = AdminOrderType.DINE_IN,
            destination = "Bàn 12 • Phòng VIP",
            createdAt = "10:52",
            waitMinutes = 0,
            items = listOf(AdminOrderLine("Bít tết sốt tiêu đen", 2, 239_000)),
            total = 516_240,
            status = AdminOrderStatus.COMPLETED,
            paymentMethod = AdminPaymentMethod.CASH,
            paid = true,
        ),
    )

    val menu = listOf(
        AdminMenuItem("menu-1", "Truffle Smash Burger", "Burger", 145_000, 129_000, true, true, "B"),
        AdminMenuItem("menu-2", "Pizza hải sản Pesto", "Pizza", 185_000, null, true, true, "P"),
        AdminMenuItem("menu-3", "Trà đào cam sả", "Đồ uống", 55_000, null, true, false, "T"),
        AdminMenuItem("menu-4", "Súp nấm Truffle", "Khai vị", 89_000, null, false, false, "S"),
        AdminMenuItem("menu-5", "Cánh gà Gochujang", "Món phụ", 119_000, 99_000, true, false, "G"),
    )

    val conversations = listOf(
        AdminConversation("conv-1", "Bàn 05 • Tầng 1", "Cho mình xin thêm tương ớt và khăn giấy nhé.", "12:25", 2, true, "#FH-9082"),
        AdminConversation("conv-2", "Khách giao hàng", "Khoảng bao lâu nữa tài xế nhận đơn vậy shop?", "12:15", 1, true, "#FH-8942"),
        AdminConversation("conv-3", "Bàn 02 • Ngoài trời", "Mình đã nhận đủ món, cảm ơn quán.", "11:50", 0, false),
    )
}

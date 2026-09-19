package com.example.foodhubapp

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodhubapp.feature.order.data.CustomerOrder
import com.example.foodhubapp.feature.order.data.OrderItem
import com.example.foodhubapp.feature.order.data.OrderStatus
import com.example.foodhubapp.feature.order.data.OrderType
import com.example.foodhubapp.feature.order.ui.OrderListScreen
import com.example.foodhubapp.feature.order.viewmodel.OrderListUiState
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OrderListScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun orderListMatchesFigmaWithoutUnsupportedProgressData() {
        val order = CustomerOrder(
            id = "order-1",
            orderCode = "FH-9082",
            pickupCode = null,
            type = OrderType.DINE_IN,
            status = OrderStatus.PENDING_PAYMENT,
            createdAt = "2026-09-19T12:20:00Z",
            totalAmount = 358_000,
            tableName = "Bàn 05",
            tableFloor = "Tầng 1",
            deliveryAddress = null,
            cancelReason = null,
            paymentMethod = "VNPAY",
            paymentStatus = "PENDING",
            items = listOf(
                OrderItem("1", "Truffle Smash Burger Bò Úc", null, 1, 145_000, 145_000, "Sốt cay nhẹ"),
                OrderItem("2", "Trà Đào Cam Sả Tươi", null, 2, 55_000, 110_000, "Ít đá")
            )
        )
        compose.setContent {
            FoodHubAppTheme(dynamicColor = false) {
                OrderListScreen(
                    state = OrderListUiState(orders = listOf(order), isLoading = false),
                    onGroupClick = {}, onTypeClick = {}, onRefresh = {}, onRetry = {}, onLoadMore = {},
                    onCancel = {}, onPay = {}, onHomeClick = {}, onNotificationClick = {}, onProfileClick = {}
                )
            }
        }

        compose.onAllNodesWithText("Đơn hàng").onFirst().assertExists()
        compose.onNodeWithText("Đang xử lý").assertExists()
        compose.onNodeWithText("#FH-9082").assertExists()
        compose.onNodeWithText("Thanh toán ngay").assertExists()
        compose.onNodeWithText("Thông báo").assertExists()
        compose.onNodeWithText("75%").assertDoesNotExist()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "orders-figma.png")
        compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())
    }
}

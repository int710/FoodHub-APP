@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.overview.ui

import com.example.foodhubapp.feature.admin.chat.ui.AdminConversationRow

import com.example.foodhubapp.feature.admin.components.AdminSectionHeader
import com.example.foodhubapp.feature.admin.components.vnd
import com.example.foodhubapp.feature.admin.order.ui.AdminOrderCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.navigation.AdminAmber
import com.example.foodhubapp.feature.admin.navigation.AdminBlue
import com.example.foodhubapp.feature.admin.chat.model.AdminConversation
import com.example.foodhubapp.feature.admin.navigation.AdminGreen
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminOrderStatus
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodhubapp.theme.FoodHubAppTheme

@Composable
fun AdminOverviewScreen(
    orders: List<AdminOrder>,
    menuItems: List<AdminMenuItem>,
    conversations: List<AdminConversation>,
    onOpenOrders: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenMenu: () -> Unit,
) {
    val activeOrders = orders.filter { it.status !in listOf(AdminOrderStatus.COMPLETED, AdminOrderStatus.CANCELLED) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Chào buổi trưa, FoodHub", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(15.dp), tint = AdminMuted)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Hôm nay, ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    color = AdminMuted,
                    fontSize = 13.sp,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Doanh thu", orders.filter { it.paid }.sumOf { it.total }.vnd(), "Đã thanh toán", Icons.AutoMirrored.Filled.TrendingUp,
                        AdminGreen, Modifier.weight(1f))
                    AdminMetric("Đơn đang xử lý", activeOrders.size.toString(), "${orders.size} tổng đơn", Icons.AutoMirrored.Filled.ReceiptLong,
                        AdminBlue, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Chờ xác nhận", orders.count { it.status == AdminOrderStatus.PENDING_CONFIRMATION }.toString(), "Cần xử lý", Icons.Default.AccessTime,
                        AdminAmber, Modifier.weight(1f))
                    AdminMetric("Tin chưa đọc", conversations.sumOf { it.unread }.toString(), "${conversations.count { it.isOnline }} đang online", Icons.Default.ChatBubbleOutline,
                        AdminPrimary, Modifier.weight(1f))
                }
            }
        }
        if (menuItems.any { !it.isAvailable }) {
            item {
                AdminAlert(
                    title = "${menuItems.count { !it.isAvailable }} món đang tạm hết",
                    message = "Kiểm tra tồn kho để mở bán lại.",
                    onClick = onOpenMenu,
                )
            }
        }
        item { AdminSectionHeader("Đơn cần xử lý", "Xem tất cả", onOpenOrders) }
        items(activeOrders.take(3), key = { it.id }) { order ->
            AdminOrderCard(order, onClick = { onOpenOrder(order.id) })
        }
        item { AdminSectionHeader("Tin nhắn gần đây") }
        items(conversations.filter { it.unread > 0 }, key = { it.id }) { conversation ->
            AdminConversationRow(conversation, onClick = { onOpenChat(conversation.id) })
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdminOverviewScreenPreview() {
    FoodHubAppTheme {
        AdminOverviewScreen(
            orders = emptyList(),
            menuItems = emptyList(),
            conversations = emptyList(),
            onOpenOrders = {},
            onOpenOrder = {},
            onOpenChat = {},
            onOpenMenu = {}
        )
    }
}



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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.foodhubapp.feature.admin.model.AdminOrderType
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
    var period by remember { mutableStateOf(AdminDashboardPeriod.TODAY) }
    var selectedType by remember { mutableStateOf<AdminOrderType?>(null) }
    var paymentFilter by remember { mutableStateOf(AdminPaymentFilter.ALL) }
    val cutoff = period.cutoffEpochMillis()
    val filteredOrders = orders.filter { order ->
        (cutoff == null || order.createdAtEpochMillis == 0L || order.createdAtEpochMillis >= cutoff) &&
            (selectedType == null || order.type == selectedType) &&
            when (paymentFilter) {
                AdminPaymentFilter.ALL -> true
                AdminPaymentFilter.PAID -> order.paid
                AdminPaymentFilter.UNPAID -> !order.paid
            }
    }
    val activeOrders = filteredOrders.filter { it.status !in listOf(AdminOrderStatus.COMPLETED, AdminOrderStatus.CANCELLED) }
    val paidOrders = filteredOrders.filter { it.paid }
    val revenue = paidOrders.sumOf { it.total }
    val averageOrder = if (paidOrders.isEmpty()) 0L else revenue / paidOrders.size
    val completedCount = filteredOrders.count { it.status == AdminOrderStatus.COMPLETED }
    val completionRate = if (filteredOrders.isEmpty()) 0 else completedCount * 100 / filteredOrders.size
    val topItems = filteredOrders.asSequence()
        .filter { it.status != AdminOrderStatus.CANCELLED }
        .flatMap { it.items.asSequence() }
        .groupBy { it.name }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
        .entries.sortedByDescending { it.value }.take(5)
    val maxTopQuantity = topItems.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val typeCounts = com.example.foodhubapp.feature.admin.model.AdminOrderType.entries.map { type ->
        type.label to filteredOrders.count { it.type == type }
    }
    val maxTypeCount = typeCounts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Tổng quan vận hành", fontSize = 23.sp, fontWeight = FontWeight.Bold)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bộ lọc thống kê", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AdminDashboardPeriod.entries) { option ->
                        FilterChip(period == option, { period = option }, { Text(option.label) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedType == null, { selectedType = null }, { Text("Mọi loại đơn") }) }
                    items(AdminOrderType.entries) { type ->
                        FilterChip(selectedType == type, { selectedType = type }, { Text(type.label) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AdminPaymentFilter.entries) { option ->
                        FilterChip(paymentFilter == option, { paymentFilter = option }, { Text(option.label) })
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Doanh thu", revenue.vnd(), "${paidOrders.size} đơn đã thu", Icons.AutoMirrored.Filled.TrendingUp,
                        AdminGreen, Modifier.weight(1f))
                    AdminMetric("Đơn đang xử lý", activeOrders.size.toString(), "${filteredOrders.size} tổng đơn", Icons.AutoMirrored.Filled.ReceiptLong,
                        AdminBlue, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Chờ xác nhận", filteredOrders.count { it.status == AdminOrderStatus.PENDING_CONFIRMATION }.toString(), "Cần xử lý", Icons.Default.AccessTime,
                        AdminAmber, Modifier.weight(1f))
                    AdminMetric("Tin chưa đọc", conversations.sumOf { it.unread }.toString(), "${conversations.count { it.isOnline }} đang online", Icons.Default.ChatBubbleOutline,
                        AdminPrimary, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Giá trị TB/đơn", averageOrder.vnd(), "Đơn đã thanh toán", Icons.Default.Payments,
                        AdminPrimary, Modifier.weight(1f))
                    AdminMetric("Tỷ lệ hoàn tất", "$completionRate%", "$completedCount/${filteredOrders.size} đơn", Icons.Default.RestaurantMenu,
                        AdminGreen, Modifier.weight(1f))
                }
            }
        }
        if (topItems.isNotEmpty()) {
            item {
                AdminInsightCard(
                    "Top món bán chạy",
                    topItems.mapIndexed { index, entry ->
                        Triple("${index + 1}. ${entry.key}", "${entry.value} phần", entry.value.toFloat() / maxTopQuantity)
                    },
                )
            }
        }
        if (filteredOrders.isNotEmpty()) {
            item {
                AdminInsightCard(
                    "Cơ cấu loại đơn",
                    typeCounts.map { (label, count) -> Triple(label, "$count đơn", count.toFloat() / maxTypeCount) },
                )
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

private enum class AdminDashboardPeriod(val label: String, val days: Long?) {
    TODAY("Hôm nay", 1),
    LAST_7_DAYS("7 ngày", 7),
    LAST_30_DAYS("30 ngày", 30),
    ALL("Tất cả", null);

    fun cutoffEpochMillis(): Long? = days?.let {
        LocalDate.now().minusDays(it - 1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

private enum class AdminPaymentFilter(val label: String) {
    ALL("Mọi thanh toán"),
    PAID("Đã thu"),
    UNPAID("Chưa thu"),
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



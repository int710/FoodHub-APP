@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.order.ui

import com.example.foodhubapp.feature.admin.model.AdminPaymentMethod

import com.example.foodhubapp.feature.admin.components.AdminSectionHeader
import com.example.foodhubapp.feature.admin.components.vnd

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.navigation.AdminAmber
import com.example.foodhubapp.feature.admin.navigation.AdminGreen
import com.example.foodhubapp.feature.admin.model.AdminItemStatus
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminOrderStatus
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminPrimarySoft
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import com.example.foodhubapp.feature.customer.menu.ui.FoodImage
import kotlin.collections.forEach

@Composable
fun AdminOrderDetailScreen(
    order: AdminOrder,
    busyId: String? = null,
    onConfirm: (AdminOrder) -> Unit,
    onConfirmCash: (AdminOrder) -> Unit,
    onUpdateItem: (String, AdminItemStatus) -> Unit,
    onServe: (AdminOrder) -> Unit,
    onComplete: (AdminOrder) -> Unit,
    onConvertToZaloPay: (AdminOrder) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(order.code, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        AdminStatusPill(order.status)
                    }
                    Text("${order.type.label} • ${order.destination}", color = AdminMuted)
                    Text("Tạo lúc ${order.createdAt} • Đã chờ ${order.waitMinutes} phút", color = AdminMuted, fontSize = 13.sp)
                }
            }
        }
        item { com.example.foodhubapp.feature.shared.payment.PaymentDetailButton(order.id) }
        item { AdminSectionHeader("Món đã đặt") }
        items(order.items, key = { it.id.ifBlank { it.name } }) { line ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AdminSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AdminPrimary.copy(alpha = .12f)),
            ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                FoodImage(line.imageUrl, line.name, Modifier.size(68.dp).clip(RoundedCornerShape(13.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(line.name, fontWeight = FontWeight.SemiBold)
                    Text("${line.quantity} phần × ${line.unitPrice.vnd()}", color = AdminMuted, fontSize = 12.sp)
                    if (line.options.isNotEmpty()) Text(line.options.joinToString(), color = AdminMuted, fontSize = 13.sp)
                    line.note?.let { Text("Ghi chú: $it", color = AdminPrimary, fontSize = 13.sp) }
                }
                Text((line.unitPrice * line.quantity).vnd(), fontWeight = FontWeight.Medium)
            }
            if (order.status in listOf(AdminOrderStatus.CONFIRMED, AdminOrderStatus.PREPARING)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when (line.status) {
                        AdminItemStatus.WAITING -> TextButton(
                            onClick = { onUpdateItem(line.id, AdminItemStatus.PREPARING) },
                            enabled = busyId == null,
                        ) { Text("Bắt đầu làm") }
                        AdminItemStatus.PREPARING -> TextButton(
                            onClick = { onUpdateItem(line.id, AdminItemStatus.READY) },
                            enabled = busyId == null,
                        ) { Text("Đánh dấu đã xong") }
                        else -> Text(line.status.label, color = AdminGreen, fontSize = 12.sp)
                    }
                }
            }
            }
            }
        }
        item {
            val subtotal = order.subtotal.takeIf { it > 0 } ?: order.items.sumOf { it.unitPrice * it.quantity }
            Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("CHI TIẾT THANH TOÁN", color = AdminMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    AdminPriceRow("Giá món", subtotal)
                    if (order.vatAmount > 0) AdminPriceRow("Thuế GTGT", order.vatAmount)
                    if (order.deliveryFee > 0) AdminPriceRow("Phí giao hàng", order.deliveryFee)
                    if (order.serviceFee > 0) AdminPriceRow("Phí dịch vụ", order.serviceFee)
                    if (order.discountAmount > 0) AdminPriceRow("Giảm giá", -order.discountAmount, AdminGreen)
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng khách phải trả", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(order.total.vnd(), color = AdminPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${order.paymentMethod.label} • ${if (order.paid) "Đã thanh toán" else "Chưa thanh toán"}", color = if (order.paid) AdminGreen else AdminAmber, fontSize = 13.sp)
                }
            }
        }
        if (order.paymentMethod in setOf(AdminPaymentMethod.CASH, AdminPaymentMethod.ZALOPAY) && !order.paid) {
            item {
                OutlinedButton(
                    onClick = { onConvertToZaloPay(order) },
                    enabled = busyId == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text(if (order.paymentMethod == AdminPaymentMethod.ZALOPAY) "Mở lại QR ZaloPay" else "Tạo QR ZaloPay cho khách")
                }
            }
        }
        item { AdminSectionHeader("Tiến độ đơn hàng") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                orderTimeline(order.status).forEach { status ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = if (status == order.status) AdminPrimary else AdminGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(status.label, fontWeight = if (status == order.status) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        if (order.status == AdminOrderStatus.PENDING_CONFIRMATION) {
            item {
                Button(
                    onClick = { onConfirm(order) },
                    enabled = busyId == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Xác nhận tiếp nhận đơn") }
            }
        }
        if (order.status == AdminOrderStatus.READY) {
            item {
                Button(
                    onClick = { onServe(order) },
                    enabled = busyId == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Xác nhận đã phục vụ") }
            }
        }
        if (order.status == AdminOrderStatus.SERVED) {
            if (order.paymentMethod == AdminPaymentMethod.CASH && !order.paid) {
                item {
                    Button(
                        onClick = { onConfirmCash(order) },
                        enabled = busyId == null,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("Xác nhận đã thu ${order.total.vnd()} tiền mặt") }
                }
            }
            item {
                Button(
                    onClick = { onComplete(order) },
                    enabled = busyId == null && order.paid,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(if (order.paid) "Hoàn tất đơn" else "Đơn chưa thanh toán") }
            }
        }
    }
}

@Composable
private fun AdminPriceRow(label: String, amount: Long, color: androidx.compose.ui.graphics.Color = AdminMuted) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AdminMuted, fontSize = 13.sp)
        Text(amount.vnd(), color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun nextOrderStatus(status: AdminOrderStatus): AdminOrderStatus? = when (status) {
    AdminOrderStatus.PENDING_PAYMENT -> AdminOrderStatus.PENDING_CONFIRMATION
    AdminOrderStatus.PENDING_CONFIRMATION -> AdminOrderStatus.CONFIRMED
    AdminOrderStatus.CONFIRMED -> AdminOrderStatus.PREPARING
    AdminOrderStatus.PREPARING -> AdminOrderStatus.READY
    AdminOrderStatus.READY -> AdminOrderStatus.SERVED
    AdminOrderStatus.SERVED -> AdminOrderStatus.COMPLETED
    AdminOrderStatus.COMPLETED, AdminOrderStatus.CANCELLED, AdminOrderStatus.PAYMENT_FAILED -> null
}

private fun orderTimeline(current: AdminOrderStatus): List<AdminOrderStatus> {
    val flow = listOf(AdminOrderStatus.PENDING_CONFIRMATION, AdminOrderStatus.CONFIRMED, AdminOrderStatus.PREPARING, AdminOrderStatus.READY, AdminOrderStatus.SERVED, AdminOrderStatus.COMPLETED)
    val index = flow.indexOf(current)
    return if (index >= 0) flow.take(index + 1) else listOf(current)
}


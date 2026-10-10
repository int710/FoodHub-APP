@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.order.ui

import com.example.foodhubapp.feature.admin.components.vnd

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.navigation.AdminAmber
import com.example.foodhubapp.feature.admin.navigation.AdminAmberSoft
import com.example.foodhubapp.feature.admin.navigation.AdminBlue
import com.example.foodhubapp.feature.admin.navigation.AdminBlueSoft
import com.example.foodhubapp.feature.admin.navigation.AdminGreen
import com.example.foodhubapp.feature.admin.navigation.AdminGreenSoft
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminOrderStatus
import com.example.foodhubapp.feature.admin.navigation.AdminRed
import com.example.foodhubapp.feature.admin.navigation.AdminRedSoft
import com.example.foodhubapp.feature.admin.navigation.AdminSurface

@Composable
internal fun AdminOrderCard(order: AdminOrder, onClick: () -> Unit) {
    val accent = when (order.status) {
        AdminOrderStatus.PENDING_PAYMENT, AdminOrderStatus.PENDING_CONFIRMATION -> AdminAmber
        AdminOrderStatus.CONFIRMED, AdminOrderStatus.PREPARING -> AdminBlue
        AdminOrderStatus.READY, AdminOrderStatus.SERVED, AdminOrderStatus.COMPLETED -> AdminGreen
        AdminOrderStatus.CANCELLED, AdminOrderStatus.PAYMENT_FAILED -> AdminRed
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AdminSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(order.code, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("${order.createdAt} • chờ ${order.waitMinutes} phút", color = AdminMuted, fontSize = 11.sp)
                }
                AdminStatusPill(order.status)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    order.type.label,
                    Modifier.background(AdminGreenSoft, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                    color = AdminGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                )
                Text(order.destination, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            }
            HorizontalDivider(color = Color(0xFFEAE7E4))
            order.items.take(3).forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.quantity}x", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.options.isNotEmpty()) Text(item.options.joinToString(" • "), color = AdminMuted, fontSize = 10.sp, maxLines = 1)
                        item.note?.takeIf(String::isNotBlank)?.let { Text("Lưu ý: $it", color = AdminRed, fontSize = 10.sp, maxLines = 1) }
                    }
                    Text((item.unitPrice * item.quantity).vnd(), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }
            if (order.items.size > 3) Text("+${order.items.size - 3} món khác", color = AdminMuted, fontSize = 11.sp)
            HorizontalDivider(color = Color(0xFFEAE7E4))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (order.paid) "Đã thanh toán ${order.paymentMethod.label}" else "${order.paymentMethod.label} chưa thu",
                    Modifier.background(
                        if (order.paid) AdminGreenSoft else AdminAmberSoft,
                        RoundedCornerShape(7.dp),
                    ).padding(horizontal = 8.dp, vertical = 5.dp),
                    color = if (order.paid) AdminGreen else AdminAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
                Text(order.total.vnd(), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = accent)
            }
        }
    }
}

@Composable
internal fun AdminStatusPill(status: AdminOrderStatus) {
    val (background, foreground) = when (status) {
        AdminOrderStatus.PENDING_PAYMENT, AdminOrderStatus.PENDING_CONFIRMATION -> AdminAmberSoft to AdminAmber
        AdminOrderStatus.CONFIRMED, AdminOrderStatus.PREPARING -> AdminBlueSoft to AdminBlue
        AdminOrderStatus.READY, AdminOrderStatus.SERVED, AdminOrderStatus.COMPLETED -> AdminGreenSoft to AdminGreen
        AdminOrderStatus.CANCELLED, AdminOrderStatus.PAYMENT_FAILED -> AdminRedSoft to AdminRed
    }
    Text(
        status.label,
        modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 5.dp),
        color = foreground,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
}


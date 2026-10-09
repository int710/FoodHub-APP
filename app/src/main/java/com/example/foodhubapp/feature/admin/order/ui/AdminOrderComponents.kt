@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.order.ui

import com.example.foodhubapp.feature.admin.components.vnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AdminSurface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.code, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                AdminStatusPill(order.status)
            }
            Text("${order.type.label} • ${order.destination}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${order.items.sumOf { it.quantity }} món • ${order.createdAt}", color = AdminMuted, fontSize = 13.sp)
                Text(order.total.vnd(), fontWeight = FontWeight.Bold)
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


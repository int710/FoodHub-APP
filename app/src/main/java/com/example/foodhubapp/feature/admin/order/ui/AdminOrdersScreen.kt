@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.order.ui

import com.example.foodhubapp.feature.admin.model.AdminPaymentMethod

import com.example.foodhubapp.feature.admin.components.AdminSearchField

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.model.AdminOrder
import com.example.foodhubapp.feature.admin.model.AdminOrderStatus

@Composable
fun AdminOrdersScreen(
    orders: List<AdminOrder>,
    onOpenOrder: (String) -> Unit,
    busyId: String? = null,
    onConfirm: (AdminOrder) -> Unit,
    onReject: (AdminOrder, String) -> Unit,
    onRefresh: () -> Unit = {},
) {
    var showKitchen by remember { mutableStateOf(false) }
    if (showKitchen) {
        AdminKitchenScreen(onBack = { showKitchen = false; onRefresh() })
        return
    }
    var query by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<AdminOrderStatus?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<AdminOrder, Boolean>?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    val filtered = orders.filter { order ->
        (selectedStatus == null || order.status == selectedStatus) &&
            (query.isBlank() || order.code.contains(query, true) || order.destination.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        TextButton({ showKitchen = true }, Modifier.padding(horizontal = 16.dp)) { Text("Danh sách món bếp") }
        AdminSearchField(query, { query = it }, "Tìm mã đơn, bàn hoặc địa chỉ", Modifier.padding(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selectedStatus == null, { selectedStatus = null }, { Text("Tất cả") }) }
            items(listOf(AdminOrderStatus.PENDING_CONFIRMATION, AdminOrderStatus.CONFIRMED, AdminOrderStatus.PREPARING, AdminOrderStatus.READY, AdminOrderStatus.COMPLETED)) { status ->
                FilterChip(selectedStatus == status, { selectedStatus = status }, { Text(status.label) })
            }
        }
        Text("${filtered.size} đơn hàng", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = AdminMuted, fontSize = 13.sp)
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.id }) { order ->
                Column {
                    AdminOrderCard(order, onClick = { onOpenOrder(order.id) })
                    if (order.status == AdminOrderStatus.PENDING_CONFIRMATION) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ pendingAction = order to false }, Modifier.weight(1f), enabled = busyId == null) { Text("Từ chối") }
                            Button({ pendingAction = order to true }, Modifier.weight(1f), enabled = busyId == null) { Text("Xác nhận") }
                        }
                    }
                }
            }
        }
    }

    pendingAction?.let { (order, approve) ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(if (approve) "Xác nhận đơn ${order.code}?" else "Từ chối đơn ${order.code}?") },
            text = {
                if (approve) Text(
                    if (order.paymentMethod == AdminPaymentMethod.CASH && !order.paid)
                        "Xác nhận tiếp nhận đơn. Tiền mặt sẽ được ghi nhận sau khi món đã phục vụ cho khách."
                    else "Đơn sẽ được chuyển sang trạng thái đã xác nhận."
                )
                else OutlinedTextField(
                    rejectReason,
                    { rejectReason = it.take(255) },
                    label = { Text("Lý do từ chối") },
                    minLines = 2,
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (approve) onConfirm(order) else onReject(order, rejectReason.trim())
                    pendingAction = null
                    rejectReason = ""
                }, enabled = approve || rejectReason.isNotBlank()) { Text(if (approve) "Xác nhận" else "Từ chối") }
            },
            dismissButton = { TextButton({ pendingAction = null }) { Text("Đóng") } },
        )
    }
}


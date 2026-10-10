package com.example.foodhubapp.feature.shared.payment

import com.example.foodhubapp.core.network.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import com.example.foodhubapp.feature.shared.ui.FoodHubAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.gson.JsonObject
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentDetailButton(orderId: String) {
    val context = LocalContext.current
    val repository = remember { PaymentRepository(context) }
    var open by remember(orderId) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<JsonObject?>(null) }
    TextButton({ open = true }) { Text("Chi tiết thanh toán") }
    if (open) {
        LaunchedEffect(orderId, retry) {
            loading = true; error = null; detail = null
            try {
                detail = repository.getDetail(orderId)
            } catch (e: Exception) { error = e.message ?: "Không thể tải thanh toán" }
            finally { loading = false }
        }
        AlertDialog(onDismissRequest = { open = false }, title = { Text("Chi tiết thanh toán") }, text = {
            Column {
                if (loading) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton({ retry++ }) { Text("Thử lại") } }
                if (!loading && error == null) {
                    val data = detail
                    if (data == null) Text("Đơn chưa có thông tin thanh toán") else {
                        Text("Phương thức: " + when (data.optString("method")) { "CASH" -> "Tiền mặt"; "VNPAY" -> "VNPay"; else -> data.optString("method") })
                        Text("Trạng thái: " + when (data.optString("status")) { "PAID" -> "Đã thanh toán"; "PENDING", "UNPAID" -> "Chờ thanh toán"; "FAILED" -> "Thanh toán thất bại"; "REFUNDED" -> "Đã hoàn tiền"; else -> data.optString("status") })
                        Text("Số tiền: ${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(data.optString("amount").toBigDecimalOrNull() ?: 0)} đ")
                        if (!data.isNull("paidAt")) Text("Thanh toán lúc: ${data.optString("paidAt")}")
                    }
                }
            }
        }, confirmButton = { TextButton({ open = false }) { Text("Đóng") } })
    }
}

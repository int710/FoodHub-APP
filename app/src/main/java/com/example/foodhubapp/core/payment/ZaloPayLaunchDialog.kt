package com.example.foodhubapp.core.payment

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.EnumMap
import java.text.NumberFormat
import java.util.Locale
import com.example.foodhubapp.theme.Brand

@Composable
fun ZaloPayLaunchDialog(
    paymentUrl: String,
    qrContent: String?,
    orderCode: String? = null,
    amount: Long? = null,
    isChecking: Boolean = false,
    onOpenOnThisDevice: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Sandbox trial có thể không trả qr_code VietQR. Khi đó order_url vẫn là
    // payload QR hợp lệ để một điện thoại khác mở trang thanh toán ZaloPay.
    val content = qrContent?.takeIf(String::isNotBlank) ?: paymentUrl
    val bitmap = remember(content) { runCatching { content.toQrBitmap(720) }.getOrNull() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 8.dp, color = Color.White) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF0878C9)).padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ZaloPay", color = Color(0xFF0878C9), fontWeight = FontWeight.Black, fontSize = 11.sp,
                        modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
                    Text("Cổng thanh toán điện tử", Modifier.padding(start = 8.dp).weight(1f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onDismiss, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Đóng", tint = Color.White) }
                }
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                orderCode?.takeIf(String::isNotBlank)?.let {
                    Text("ĐƠN HÀNG: #${it.removePrefix("#")}", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                amount?.let {
                    Text(NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(it) + "đ", fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
                bitmap?.let {
                    Surface(shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E2E2)), color = Color.White) {
                        Image(bitmap = it.asImageBitmap(), contentDescription = "Mã QR thanh toán ZaloPay", modifier = Modifier.padding(10.dp).size(220.dp))
                    }
                }
                Text("Quét bằng ứng dụng ngân hàng hoặc Ví ZaloPay", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFEDF6FF), RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isChecking) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Color(0xFF0878C9))
                    else Icon(Icons.Default.VerifiedUser, null, Modifier.size(17.dp), tint = Color(0xFF0878C9))
                    Text(if (isChecking) "Đang tự động kiểm tra thanh toán…" else "Thanh toán được xác nhận tự động", Modifier.padding(start = 8.dp), color = Color(0xFF075A99), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onOpenOnThisDevice,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0878C9)),
                ) { Text("Mở ZaloPay trên thiết bị này", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp)) { Text("Theo dõi trong Đơn hàng", color = Brand) }
                }
            }
        }
    }
}

private fun String.toQrBitmap(size: Int): Bitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.MARGIN, 1)
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
    }
    val matrix: BitMatrix = MultiFormatWriter().encode(this, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) pixels[y * size + x] = if (matrix[x, y]) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

package com.kma.foodhub.feature.admin.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kma.foodhub.feature.admin.management.model.SystemSetting
import com.kma.foodhub.feature.admin.management.viewmodel.AdminManagementState
import com.kma.foodhub.feature.admin.navigation.*

@Composable
fun AdminSettingsScreen(
    state: AdminManagementState,
    onLoad: () -> Unit,
    onSave: (SystemSetting) -> Unit,
) {
    var form by remember { mutableStateOf(state.settings) }
    var vatText by remember { mutableStateOf(form.vatRate.cleanNumber()) }
    var serviceFeeText by remember { mutableStateOf(form.serviceFeeRate.cleanNumber()) }
    var deliveryFeeText by remember { mutableStateOf(form.defaultDeliveryFee.toString()) }
    LaunchedEffect(Unit) { onLoad() }
    LaunchedEffect(state.settings) {
        form = state.settings
        vatText = state.settings.vatRate.cleanNumber()
        serviceFeeText = state.settings.serviceFeeRate.cleanNumber()
        deliveryFeeText = state.settings.defaultDeliveryFee.toString()
    }

    Column(
        Modifier.fillMaxSize().background(AdminBackground).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Thiết lập dùng chung cho ứng dụng và quá trình tính tiền.", color = AdminMuted, fontSize = 13.sp)
        SettingsSection("Thông tin thương hiệu") {
            SettingField("Tên ứng dụng", form.appName) { form = form.copy(appName = it) }
            SettingField("Tên nhà hàng", form.restaurantName) { form = form.copy(restaurantName = it) }
            SettingField("URL logo", form.logoUrl) { form = form.copy(logoUrl = it) }
        }
        SettingsSection("Thông tin liên hệ") {
            SettingField("Email hỗ trợ", form.supportEmail) { form = form.copy(supportEmail = it) }
            SettingField("Số điện thoại", form.supportPhone) { form = form.copy(supportPhone = it) }
            SettingField("Địa chỉ", form.address, singleLine = false) { form = form.copy(address = it) }
        }
        SettingsSection("Thuế và phí") {
            SettingField("VAT (%)", vatText, KeyboardType.Decimal) {
                vatText = it.filter { char -> char.isDigit() || char == '.' }
            }
            SettingField("Phí dịch vụ (%)", serviceFeeText, KeyboardType.Decimal) {
                serviceFeeText = it.filter { char -> char.isDigit() || char == '.' }
            }
            SettingField("Phí giao hàng mặc định (đ)", deliveryFeeText, KeyboardType.Number) {
                deliveryFeeText = it.filter(Char::isDigit)
            }
            SettingField("Tiền tệ", form.currency) { form = form.copy(currency = it.uppercase().take(3)) }
            Text("VAT mới áp dụng cho giỏ hàng và đơn tạo sau khi lưu. Đơn cũ giữ nguyên số tiền đã chốt.", color = AdminMuted, fontSize = 12.sp)
        }
        Button(
            onClick = {
                onSave(form.copy(
                    vatRate = vatText.toDoubleOrNull() ?: 0.0,
                    serviceFeeRate = serviceFeeText.toDoubleOrNull() ?: 0.0,
                    defaultDeliveryFee = deliveryFeeText.toLongOrNull() ?: 0,
                ))
            },
            enabled = !state.isSaving && form.appName.isNotBlank() && form.restaurantName.isNotBlank() &&
                vatText.toDoubleOrNull()?.let { it in 0.0..100.0 } == true &&
                serviceFeeText.toDoubleOrNull()?.let { it in 0.0..100.0 } == true,
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
        ) {
            if (state.isSaving) { CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(if (state.isSaving) "Đang lưu..." else "Lưu cấu hình", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            content()
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine, minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(12.dp),
    )
}

private fun Double.cleanNumber() = if (this % 1.0 == 0.0) toInt().toString() else toString()

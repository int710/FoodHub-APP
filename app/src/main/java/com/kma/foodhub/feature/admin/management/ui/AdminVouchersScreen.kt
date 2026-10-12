package com.kma.foodhub.feature.admin.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kma.foodhub.core.ui.icons.TablerIcons
import com.kma.foodhub.feature.admin.management.model.AdminVoucher
import com.kma.foodhub.feature.admin.management.viewmodel.AdminManagementState
import com.kma.foodhub.feature.admin.navigation.*
import com.kma.foodhub.feature.admin.components.vnd
import java.time.LocalDate

@Composable
fun AdminVouchersScreen(
    state: AdminManagementState,
    onLoad: () -> Unit,
    onSave: (AdminVoucher) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<AdminVoucher?>(null) }
    var deleting by remember { mutableStateOf<AdminVoucher?>(null) }
    LaunchedEffect(Unit) { onLoad() }
    editing?.let { VoucherEditor(it, { editing = null }) { value -> onSave(value); editing = null } }
    deleting?.let { voucher ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("Xóa voucher ${voucher.code}?") },
            text = { Text("Voucher đã có lịch sử sử dụng sẽ không thể xóa; bạn có thể tắt voucher thay thế.") },
            confirmButton = { TextButton(onClick = { onDelete(voucher.id); deleting = null }) { Text("Xóa", color = AdminRed) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy") } },
        )
    }
    Scaffold(
        containerColor = AdminBackground,
        floatingActionButton = { FloatingActionButton(onClick = { editing = newVoucher() }, containerColor = AdminPrimary) { Icon(TablerIcons.Add, "Thêm voucher", tint = Color.White) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Tạo mã giảm giá và giới hạn theo thời gian, loại đơn, giá trị đơn hoặc lượt sử dụng.", color = AdminMuted, fontSize = 13.sp) }
            if (state.isLoadingVouchers) item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            else if (state.vouchers.isEmpty()) item { Text("Chưa có voucher", color = AdminMuted, modifier = Modifier.padding(24.dp)) }
            else items(state.vouchers, key = { it.id }) { voucher ->
                VoucherCard(voucher, state.busyVoucherId == voucher.id, { editing = voucher }, { deleting = voucher })
            }
        }
    }
}

@Composable
private fun VoucherCard(voucher: AdminVoucher, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(voucher.code, color = AdminPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(voucher.name, fontWeight = FontWeight.SemiBold)
                }
                AssistChip(onClick = {}, label = { Text(if (voucher.isActive) "Đang bật" else "Đã tắt") })
            }
            Text(
                if (voucher.discountType == "PERCENT") "Giảm ${voucher.discountValue.clean()}%${voucher.maxDiscountAmount?.let { " · tối đa ${it.vnd()}" }.orEmpty()}"
                else "Giảm ${voucher.discountValue.toLong().vnd()}",
                fontWeight = FontWeight.Bold,
            )
            Text("Đơn từ ${voucher.minOrderAmount.vnd()} · Đã dùng ${voucher.usedCount}/${voucher.usageLimit?.toString() ?: "∞"} · Mỗi khách ${voucher.perCustomerLimit} lượt", color = AdminMuted, fontSize = 12.sp)
            Text(voucher.applicableOrderTypes.joinToString(" · ") { it.typeLabel() }, color = AdminMuted, fontSize = 12.sp)
            Text(if (voucher.distribution == "ACCOUNT") "Đã phát vào tài khoản khách hàng" else "Mã bí mật · chỉ người biết mã mới dùng được", color = if (voucher.distribution == "ACCOUNT") AdminPrimary else AdminMuted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit, enabled = !busy) { Icon(TablerIcons.Edit, null); Spacer(Modifier.width(4.dp)); Text("Sửa") }
                TextButton(onClick = onDelete, enabled = !busy) { Icon(TablerIcons.Delete, null, tint = AdminRed); Spacer(Modifier.width(4.dp)); Text("Xóa", color = AdminRed) }
            }
        }
    }
}

@Composable
private fun VoucherEditor(initial: AdminVoucher, onDismiss: () -> Unit, onSave: (AdminVoucher) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    var discount by remember(initial) { mutableStateOf(initial.discountValue.clean()) }
    var minimum by remember(initial) { mutableStateOf(initial.minOrderAmount.toString()) }
    var maximum by remember(initial) { mutableStateOf(initial.maxDiscountAmount?.toString().orEmpty()) }
    var usage by remember(initial) { mutableStateOf(initial.usageLimit?.toString().orEmpty()) }
    var perCustomer by remember(initial) { mutableStateOf(initial.perCustomerLimit.toString()) }
    var startDate by remember(initial) { mutableStateOf(initial.startsAt.take(10)) }
    var endDate by remember(initial) { mutableStateOf(initial.endsAt.take(10)) }
    val valid = value.code.length >= 3 && value.name.length >= 2 && (discount.toDoubleOrNull() ?: 0.0) > 0 &&
        runCatching { LocalDate.parse(startDate) < LocalDate.parse(endDate) }.getOrDefault(false) && value.applicableOrderTypes.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Tạo voucher" else "Chỉnh sửa voucher") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoucherField("Mã voucher", value.code) { value = value.copy(code = it.uppercase().filterNot(Char::isWhitespace).take(30)) }
                VoucherField("Tên chương trình", value.name) { value = value.copy(name = it) }
                VoucherField("Mô tả", value.description) { value = value.copy(description = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(value.discountType == "PERCENT", { value = value.copy(discountType = "PERCENT") }, { Text("Giảm %") })
                    FilterChip(value.discountType == "FIXED_AMOUNT", { value = value.copy(discountType = "FIXED_AMOUNT") }, { Text("Giảm tiền") })
                }
                VoucherField(if (value.discountType == "PERCENT") "Phần trăm giảm" else "Số tiền giảm", discount, KeyboardType.Decimal) { discount = it.numeric(true) }
                VoucherField("Giá trị đơn tối thiểu", minimum, KeyboardType.Number) { minimum = it.numeric() }
                if (value.discountType == "PERCENT") VoucherField("Giảm tối đa (để trống nếu không giới hạn)", maximum, KeyboardType.Number) { maximum = it.numeric() }
                Surface(color = AdminPrimarySoft, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = value.distribution == "ACCOUNT",
                                onCheckedChange = { checked -> value = value.copy(distribution = if (checked) "ACCOUNT" else "SECRET") },
                            )
                            Column {
                                Text("Phát cho toàn bộ khách hàng", fontWeight = FontWeight.SemiBold)
                                Text("Voucher xuất hiện trong tài khoản của mọi CUSTOMER.", color = AdminMuted, fontSize = 12.sp)
                            }
                        }
                        if (value.distribution == "SECRET") {
                            Text("Đang là voucher bí mật: không hiện trong tài khoản, chỉ người biết mã mới sử dụng được.", color = AdminMuted, fontSize = 12.sp)
                        }
                    }
                }
                Text("Áp dụng cho", fontWeight = FontWeight.SemiBold)
                listOf("DINE_IN", "TAKEAWAY", "DELIVERY").forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(type in value.applicableOrderTypes, { checked -> value = value.copy(applicableOrderTypes = if (checked) value.applicableOrderTypes + type else value.applicableOrderTypes - type) })
                        Text(type.typeLabel())
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoucherField("Bắt đầu", startDate, modifier = Modifier.weight(1f)) { startDate = it.take(10) }
                    VoucherField("Kết thúc", endDate, modifier = Modifier.weight(1f)) { endDate = it.take(10) }
                }
                VoucherField("Tổng lượt (trống = không giới hạn)", usage, KeyboardType.Number) { usage = it.numeric() }
                VoucherField("Lượt mỗi khách", perCustomer, KeyboardType.Number) { perCustomer = it.numeric() }
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(value.isActive, { value = value.copy(isActive = it) }); Spacer(Modifier.width(8.dp)); Text("Kích hoạt voucher") }
            }
        },
        confirmButton = { Button(onClick = {
            onSave(value.copy(
                discountValue = discount.toDoubleOrNull() ?: 0.0, minOrderAmount = minimum.toLongOrNull() ?: 0,
                maxDiscountAmount = maximum.toLongOrNull(), usageLimit = usage.toIntOrNull(), perCustomerLimit = perCustomer.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                startsAt = "${startDate}T00:00:00.000Z", endsAt = "${endDate}T23:59:59.999Z",
            ))
        }, enabled = valid) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun VoucherField(label: String, text: String, keyboard: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(text, onChange, modifier, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard), singleLine = true)
}

private fun newVoucher(): AdminVoucher {
    val today = LocalDate.now()
    return AdminVoucher(startsAt = today.toString(), endsAt = today.plusMonths(1).toString())
}
private fun String.numeric(decimal: Boolean = false) = filter { it.isDigit() || decimal && it == '.' }
private fun Double.clean() = if (this % 1.0 == 0.0) toLong().toString() else toString()
private fun String.typeLabel() = when (this) { "DINE_IN" -> "Tại bàn"; "DELIVERY" -> "Giao hàng"; else -> "Mang về" }

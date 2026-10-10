package com.example.foodhubapp.feature.admin.table.ui

import com.example.foodhubapp.feature.admin.model.AdminRestaurantTable
import com.example.foodhubapp.feature.admin.components.vnd
import com.example.foodhubapp.feature.admin.navigation.AdminBackground
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminRedSoft
import com.example.foodhubapp.feature.admin.navigation.AdminRed
import com.example.foodhubapp.feature.admin.navigation.AdminGreenSoft
import com.example.foodhubapp.feature.admin.navigation.AdminGreen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TableRestaurant
import com.example.foodhubapp.feature.shared.ui.FoodHubAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.table.data.AdminTableRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap

@Composable
fun AdminTablesScreen() {
    val context = LocalContext.current
    val repository = remember { AdminTableRepository(context) }
    val scope = rememberCoroutineScope()
    val tables = remember { mutableStateListOf<AdminRestaurantTable>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var qrState by remember { mutableStateOf<QrDialogState?>(null) }
    var busyTableId by remember { mutableStateOf<String?>(null) }
    var regenerateTarget by remember { mutableStateOf<AdminRestaurantTable?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminRestaurantTable?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            isLoading = true
            errorMessage = null
            runCatching { repository.getTables() }
                .onSuccess {
                    tables.clear()
                    tables.addAll(it)
                }
                .onFailure { errorMessage = it.message ?: "Không thể tải danh sách bàn" }
            isLoading = false
        }
    }

    fun openQr(table: AdminRestaurantTable) {
        qrState = QrDialogState(table, null, true, null)
        scope.launch {
            runCatching { repository.getTable(table.id) to repository.getQrContent(table.id) }
                .onSuccess { (detail, content) -> qrState = QrDialogState(detail, content, false, null) }
                .onFailure { qrState = QrDialogState(table, null, false, it.message ?: "Không thể tải QR") }
        }
    }

    fun toggleTable(table: AdminRestaurantTable) {
        if (busyTableId != null) return
        busyTableId = table.id
        scope.launch {
            errorMessage = null
            runCatching { repository.toggleTable(table.id) }
                .onSuccess { isActive ->
                    val index = tables.indexOfFirst { it.id == table.id }
                    if (index >= 0) tables[index] = tables[index].copy(isActive = isActive)
                }
                .onFailure { errorMessage = it.message ?: "Không thể cập nhật trạng thái bàn" }
            busyTableId = null
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Scaffold(
        containerColor = AdminBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateDialog = true
                },
                containerColor = AdminPrimary,
            ) { Icon(Icons.Default.Add, "Thêm bàn", tint = Color.White) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().background(AdminSurface).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Sơ đồ bàn", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${tables.size} bàn từ hệ thống", color = AdminMuted, fontSize = 12.sp)
                }
                IconButton(onClick = ::reload, enabled = !isLoading) { Icon(Icons.Default.Refresh, "Tải lại") }
            }

            when {
                isLoading && tables.isEmpty() -> AdminTableLoading()
                errorMessage != null && tables.isEmpty() -> AdminTableError(errorMessage.orEmpty(), ::reload)
                tables.isEmpty() -> AdminTableEmpty { showCreateDialog = true }
                else -> Column(Modifier.fillMaxSize()) {
                    errorMessage?.let { message ->
                            Text(
                                message, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                    .background(AdminRedSoft, RoundedCornerShape(12.dp)).padding(12.dp),
                                color = AdminRed,
                                fontSize = 13.sp,
                            )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(170.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    items(tables, key = { it.id }) { table ->
                        AdminTableRow(
                            table = table,
                            isBusy = busyTableId == table.id,
                            onQr = { openQr(table) },
                            onToggle = { toggleTable(table) },
                            onRegenerateQr = { regenerateTarget = table },
                            onDelete = {
                                deleteError = null
                                deleteTarget = table
                            },
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AdminCreateTableDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, capacity, floor, note ->
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    runCatching { repository.createTable(name, capacity, floor, note) }
                        .mapCatching { createdTable ->
                            val persistedTables = repository.getTables()
                            check(persistedTables.any { it.id == createdTable.id }) {
                                "Backend đã phản hồi tạo bàn nhưng không đọc lại được bàn vừa tạo. Hãy kiểm tra DATABASE_URL_POSTGRESQL trên Render."
                            }
                            createdTable to persistedTables
                        }
                        .onSuccess { (table, persistedTables) ->
                            tables.clear()
                            tables.addAll(persistedTables)
                            showCreateDialog = false
                            openQr(table)
                        }
                        .onFailure { errorMessage = it.message ?: "Không thể tạo bàn" }
                    isLoading = false
                }
            },
            isSubmitting = isLoading,
            errorMessage = errorMessage,
        )
    }

    qrState?.let { state ->
        AdminQrDialog(state, onDismiss = { qrState = null }, onRetry = { openQr(state.table) })
    }

    regenerateTarget?.let { table ->
        AlertDialog(
            onDismissRequest = { if (busyTableId == null) regenerateTarget = null },
            title = { Text("Tạo lại QR ${table.name}?") },
            text = { Text("Mã QR cũ sẽ không còn dùng được. Bạn cần in hoặc dán lại mã QR mới tại bàn.") },
            confirmButton = {
                Button(
                    onClick = {
                        busyTableId = table.id
                        scope.launch {
                            errorMessage = null
                            runCatching { repository.regenerateQrContent(table.id) }
                                .onSuccess { content ->
                                    qrState = QrDialogState(table, content, false, null)
                                    regenerateTarget = null
                                }
                                .onFailure { errorMessage = it.message ?: "Không thể tạo lại QR" }
                            busyTableId = null
                        }
                    },
                    enabled = busyTableId == null,
                ) { Text("Tạo QR mới") }
            },
            dismissButton = { TextButton(onClick = { regenerateTarget = null }) { Text("Hủy") } },
        )
    }

    deleteTarget?.let { table ->
        val isDeleting = busyTableId == table.id
        AlertDialog(
            onDismissRequest = { if (!isDeleting) deleteTarget = null },
            title = { Text("Xóa ${table.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bàn sẽ bị xóa khỏi sơ đồ và mã QR hiện tại không còn sử dụng được. Lịch sử đơn hàng vẫn được giữ lại.")
                    deleteError?.let { message ->
                        Text(message, color = AdminRed, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        busyTableId = table.id
                        deleteError = null
                        scope.launch {
                            runCatching { repository.deleteTable(table.id) }
                                .onSuccess {
                                    tables.removeAll { it.id == table.id }
                                    deleteTarget = null
                                }
                                .onFailure {
                                    deleteError = it.message ?: "Không thể xóa bàn"
                                }
                            busyTableId = null
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminRed),
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Xóa bàn")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }, enabled = !isDeleting) { Text("Hủy") }
            },
        )
    }
}

@Composable
private fun AdminTableRow(
    table: AdminRestaurantTable,
    isBusy: Boolean,
    onQr: () -> Unit,
    onToggle: () -> Unit,
    onRegenerateQr: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val occupied = table.status == "OCCUPIED"
    val (statusLabel, statusBackground, statusColor) = when (table.status) {
        "OCCUPIED" -> Triple("Đang có khách", Color(0xFFFFF0DA), Color(0xFFB45A00))
        "INACTIVE" -> Triple("Tạm ngưng", AdminRedSoft, AdminRed)
        else -> Triple("Bàn trống", AdminGreenSoft, AdminGreen)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (occupied) Color(0xFFFF9A3D) else Color(0xFFE7E2DE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(table.name, Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(
                    statusLabel,
                    Modifier.background(statusBackground, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    color = statusColor, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(table.floor ?: "Chưa đặt khu vực", Modifier.weight(1f), color = AdminMuted, fontSize = 11.sp, maxLines = 1)
                Icon(Icons.Default.Chair, null, Modifier.size(14.dp), tint = AdminMuted)
                Spacer(Modifier.width(4.dp))
                Text("${table.capacity} khách", color = AdminMuted, fontSize = 11.sp)
            }
            Surface(color = Color(0xFFF9F8F7), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (occupied && table.currentOrderCode != null) {
                        Text("ĐƠN HIỆN TẠI", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        Row(Modifier.fillMaxWidth()) {
                            Text(table.currentOrderCode, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            table.currentOrderTotal?.let { Text(it.vnd(), color = AdminRed, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        }
                    } else {
                        Text(if (table.status == "INACTIVE") "Bàn đang tạm ngưng nhận phiên" else "Sẵn sàng tạo phiên QR mới", color = AdminMuted, fontSize = 10.sp)
                        table.qrToken?.let { token ->
                            Text("Token: ${token.take(18)}${if (token.length > 18) "…" else ""}", color = AdminMuted, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
            if (isBusy) {
                Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(23.dp), strokeWidth = 2.dp)
                }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onQr, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.QrCode2, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Xem mã QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Tùy chọn bàn") }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (table.isActive) "Tạm ngưng bàn" else "Mở lại bàn") },
                            leadingIcon = { Icon(Icons.Default.PowerSettingsNew, null) },
                            onClick = { menuOpen = false; onToggle() },
                        )
                        DropdownMenuItem(
                            text = { Text("Tạo lại QR") },
                            leadingIcon = { Icon(Icons.Default.RestartAlt, null) },
                            onClick = { menuOpen = false; onRegenerateQr() },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Xóa bàn", color = AdminRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = AdminRed) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTableLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Đang tải danh sách bàn...", color = AdminMuted)
        }
    }
}

@Composable
private fun AdminTableError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.TableRestaurant, null, Modifier.size(42.dp), tint = AdminPrimary)
        Spacer(Modifier.height(12.dp))
        Text("Chưa thể tải dữ liệu bàn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(message, color = AdminMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRetry) { Text("Thử lại") }
    }
}

@Composable
private fun AdminTableEmpty(onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.TableRestaurant, null, Modifier.size(48.dp), tint = AdminMuted)
        Spacer(Modifier.height(12.dp))
        Text("Chưa có bàn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Tạo bàn đầu tiên để backend sinh qrToken.", color = AdminMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onCreate) { Text("Tạo bàn") }
    }
}

@Composable
private fun AdminCreateTableDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, String?, String?) -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
) {
    var name by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("2") }
    var floor by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val capacityValue = capacity.toIntOrNull()
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Tạo bàn mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errorMessage?.let {
                    Text(it, color = AdminRed, fontSize = 12.sp)
                }
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Tên bàn") }, singleLine = true)
                OutlinedTextField(capacity, { capacity = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(), label = { Text("Số chỗ") }, singleLine = true)
                OutlinedTextField(floor, { floor = it }, Modifier.fillMaxWidth(), label = { Text("Khu vực / tầng") }, singleLine = true)
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Ghi chú") }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, capacityValue ?: 2, floor, note) },
                enabled = !isSubmitting && name.isNotBlank() && capacityValue != null && capacityValue > 0,
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Tạo bàn")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Hủy") } },
    )
}

@Composable
private fun AdminQrDialog(state: QrDialogState, onDismiss: () -> Unit, onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR ${state.table.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.error != null -> {
                        Icon(Icons.Default.QrCode2, null, Modifier.size(48.dp), tint = AdminRed)
                        Text(state.error, color = AdminRed, textAlign = TextAlign.Center)
                        OutlinedButton(onClick = onRetry) { Text("Tải lại QR") }
                    }
                    state.content != null -> {
                        val bitmap = remember(state.content) { createQrBitmap(state.content) }
                        Image(bitmap.asImageBitmap(), "Mã QR của ${state.table.name}", Modifier.size(240.dp))
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chair, null, tint = AdminPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("${state.table.capacity} chỗ • ${state.table.floor ?: "Chưa đặt khu vực"}")
                        }
                        Text("Mã QR xác thực ${state.table.name}", color = AdminMuted, fontSize = 12.sp)
                        Text("Dùng màn quét QR của khách hàng để kiểm thử.", color = AdminGreen, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Đóng") } },
    )
}

private fun createQrBitmap(content: String): Bitmap {
    val size = 768
    val matrix = MultiFormatWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return createBitmap(size, size).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

private data class QrDialogState(
    val table: AdminRestaurantTable,
    val content: String?,
    val loading: Boolean,
    val error: String?,
)

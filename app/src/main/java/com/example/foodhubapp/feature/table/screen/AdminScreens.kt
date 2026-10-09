@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.table.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.table.AdminAmber
import com.example.foodhubapp.feature.table.AdminAmberSoft
import com.example.foodhubapp.feature.table.AdminBackground
import com.example.foodhubapp.feature.table.AdminBlue
import com.example.foodhubapp.feature.table.AdminBlueSoft
import com.example.foodhubapp.feature.table.AdminConversation
import com.example.foodhubapp.feature.table.AdminGreen
import com.example.foodhubapp.feature.table.AdminGreenSoft
import com.example.foodhubapp.feature.table.AdminMenuItem
import com.example.foodhubapp.feature.table.AdminMenuCategory
import com.example.foodhubapp.feature.table.AdminMenuCategoryInput
import com.example.foodhubapp.feature.table.AdminMenuInput
import com.example.foodhubapp.feature.table.AdminFlashSaleInput
import com.example.foodhubapp.feature.table.AdminVariantInput
import com.example.foodhubapp.feature.table.AdminVariantOptionInput
import com.example.foodhubapp.feature.table.AdminItemStatus
import com.example.foodhubapp.feature.table.AdminMuted
import com.example.foodhubapp.feature.table.AdminOrder
import com.example.foodhubapp.feature.table.AdminOrderStatus
import com.example.foodhubapp.feature.table.AdminPrimary
import com.example.foodhubapp.feature.table.AdminPrimarySoft
import com.example.foodhubapp.feature.table.AdminRed
import com.example.foodhubapp.feature.table.AdminRedSoft
import com.example.foodhubapp.feature.table.AdminSurface
import com.example.foodhubapp.feature.table.AdminText
import com.example.foodhubapp.feature.table.AdminUiMessage
import com.example.foodhubapp.feature.table.AdminChatSocketClient
import com.example.foodhubapp.feature.table.ChatConnectionState
import com.example.foodhubapp.feature.table.ChatMessage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodhubapp.feature.auth.model.UserDto
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
    val activeOrders = orders.filter { it.status !in listOf(AdminOrderStatus.COMPLETED, AdminOrderStatus.CANCELLED) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Chào buổi trưa, FoodHub", fontSize = 23.sp, fontWeight = FontWeight.Bold)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Doanh thu", orders.filter { it.paid }.sumOf { it.total }.vnd(), "Đã thanh toán", Icons.AutoMirrored.Filled.TrendingUp,
                        AdminGreen, Modifier.weight(1f))
                    AdminMetric("Đơn đang xử lý", activeOrders.size.toString(), "${orders.size} tổng đơn", Icons.AutoMirrored.Filled.ReceiptLong,
                        AdminBlue, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Chờ xác nhận", orders.count { it.status == AdminOrderStatus.PENDING_CONFIRMATION }.toString(), "Cần xử lý", Icons.Default.AccessTime,
                        AdminAmber, Modifier.weight(1f))
                    AdminMetric("Tin chưa đọc", conversations.sumOf { it.unread }.toString(), "${conversations.count { it.isOnline }} đang online", Icons.Default.ChatBubbleOutline,
                        AdminPrimary, Modifier.weight(1f))
                }
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

@Composable
fun AdminOrdersScreen(
    orders: List<AdminOrder>,
    onOpenOrder: (String) -> Unit,
    busyId: String? = null,
    onConfirm: (AdminOrder) -> Unit,
    onReject: (AdminOrder, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<AdminOrderStatus?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<AdminOrder, Boolean>?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    val filtered = orders.filter { order ->
        (selectedStatus == null || order.status == selectedStatus) &&
            (query.isBlank() || order.code.contains(query, true) || order.destination.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
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
                if (approve) Text("Đơn sẽ được chuyển sang trạng thái đã xác nhận.")
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

@Composable
fun AdminOrderDetailScreen(
    order: AdminOrder,
    busyId: String? = null,
    onConfirm: (AdminOrder) -> Unit,
    onUpdateItem: (String, AdminItemStatus) -> Unit,
    onServe: (AdminOrder) -> Unit,
    onComplete: (AdminOrder) -> Unit,
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
        item { AdminSectionHeader("Món đã đặt") }
        items(order.items, key = { it.id.ifBlank { it.name } }) { line ->
            Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(34.dp).background(AdminPrimarySoft, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text("${line.quantity}x", color = AdminPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(line.name, fontWeight = FontWeight.SemiBold)
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
        item {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng cộng", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(order.total.vnd(), color = AdminPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Text("${order.paymentMethod.label} • ${if (order.paid) "Đã thanh toán" else "Chưa thanh toán"}", color = if (order.paid) AdminGreen else AdminAmber, fontSize = 13.sp)
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
                ) { Text("Xác nhận đơn") }
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
fun AdminMenuScreen(
    items: List<AdminMenuItem>,
    categories: List<AdminMenuCategory>,
    isLoading: Boolean,
    isSaving: Boolean,
    isUploading: Boolean,
    busyItemId: String?,
    busyCategoryId: String?,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onCreate: (AdminMenuInput) -> Unit,
    onUpdate: (String, AdminMenuInput) -> Unit,
    onDelete: (String) -> Unit,
    onToggleAvailability: (String) -> Unit,
    onCreateCategory: (AdminMenuCategoryInput) -> Unit,
    onUpdateCategory: (String, AdminMenuCategoryInput) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onCreateFlashSale: (AdminFlashSaleInput) -> Unit,
    onDeleteFlashSale: (String) -> Unit,
    onCreateVariant: (String, AdminVariantInput) -> Unit,
    onUploadImage: (Uri, (String) -> Unit) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    var tab by remember { mutableStateOf("Món ăn") }
    var editingItem by remember { mutableStateOf<AdminMenuItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AdminMenuItem?>(null) }
    var editingCategory by remember { mutableStateOf<AdminMenuCategory?>(null) }
    var showCategoryEditor by remember { mutableStateOf(false) }
    var deleteCategoryTarget by remember { mutableStateOf<AdminMenuCategory?>(null) }
    var showFlashSaleEditor by remember { mutableStateOf(false) }
    var flashSaleTargetId by remember { mutableStateOf<String?>(null) }
    var variantTarget by remember { mutableStateOf<AdminMenuItem?>(null) }
    val categoryFilters = listOf("Tất cả") + items.map { it.category }.distinct()
    val filtered = items.filter { (category == "Tất cả" || it.category == category) && (query.isBlank() || it.name.contains(query, true)) }

    Scaffold(
        containerColor = AdminBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (tab) {
                        "Món ăn" -> {
                            editingItem = null
                            showEditor = true
                        }
                        "Danh mục" -> {
                            editingCategory = null
                            showCategoryEditor = true
                        }
                        else -> showFlashSaleEditor = true
                    }
                },
                containerColor = AdminPrimary,
            ) {
                Icon(Icons.Default.Add, "Thêm", tint = Color.White)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AdminSearchField(
                query,
                { query = it },
                if (tab == "Danh mục") "Tìm danh mục" else "Tìm món ăn",
                Modifier.padding(16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Món ăn", "Danh mục", "Flash sale").forEach { label ->
                    FilterChip(tab == label, { tab = label }, { Text(label) })
                }
            }
            if (tab == "Món ăn") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryFilters) { item -> FilterChip(category == item, { category = item }, { Text(item) }) }
                }
                when {
                    isLoading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPrimary)
                    }
                    errorMessage != null && items.isEmpty() -> AdminPlaceholder(
                        icon = Icons.Default.WarningAmber,
                        title = "Không tải được thực đơn",
                        message = errorMessage,
                        button = "Thử lại",
                        onClick = onRefresh,
                    )
                    filtered.isEmpty() -> AdminPlaceholder(
                        icon = Icons.Default.RestaurantMenu,
                        title = "Chưa có món ăn",
                        message = if (query.isBlank()) "Hãy thêm món đầu tiên vào thực đơn." else "Không tìm thấy món phù hợp.",
                        button = "Tải lại",
                        onClick = onRefresh,
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { menuItem ->
                            AdminMenuRow(
                                item = menuItem,
                                isBusy = busyItemId == menuItem.id,
                                onToggle = { onToggleAvailability(menuItem.id) },
                                onEdit = {
                                    editingItem = menuItem
                                    showEditor = true
                                },
                                onDelete = { deleteTarget = menuItem },
                                onVariant = { variantTarget = menuItem },
                                onFlashSale = {
                                    flashSaleTargetId = menuItem.id
                                    showFlashSaleEditor = true
                                },
                            )
                        }
                    }
                }
            } else if (tab == "Danh mục") {
                val filteredCategories = categories.filter { query.isBlank() || it.name.contains(query, true) }
                when {
                    isLoading && categories.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPrimary)
                    }
                    filteredCategories.isEmpty() -> AdminPlaceholder(
                        icon = Icons.Default.RestaurantMenu,
                        title = "Chưa có danh mục",
                        message = "Tạo danh mục để sắp xếp các món trong thực đơn.",
                        button = "Thêm danh mục",
                        onClick = {
                            editingCategory = null
                            showCategoryEditor = true
                        },
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredCategories, key = { it.id }) { menuCategory ->
                            AdminCategoryRow(
                                category = menuCategory,
                                isBusy = busyCategoryId == menuCategory.id,
                                onEdit = {
                                    editingCategory = menuCategory
                                    showCategoryEditor = true
                                },
                                onDelete = { deleteCategoryTarget = menuCategory },
                            )
                        }
                    }
                }
            } else {
                val saleItems = items.filter { it.salePrice != null }
                if (saleItems.isEmpty()) AdminPlaceholder(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Chưa có flash sale",
                    message = "Tạo chương trình giảm giá theo khung giờ.",
                    button = "Tạo flash sale",
                    onClick = { showFlashSaleEditor = true },
                ) else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(saleItems, key = { it.id }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = AdminSurface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("${item.price.vnd()} → ${item.salePrice?.vnd()}", color = AdminPrimary)
                                }
                                TextButton(onClick = { onDeleteFlashSale(item.id) }) { Text("Kết thúc", color = AdminRed) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryEditor) {
        AdminCategoryEditorDialog(
            category = editingCategory,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showCategoryEditor = false },
            onSubmit = { input ->
                editingCategory?.let { onUpdateCategory(it.id, input) } ?: onCreateCategory(input)
                showCategoryEditor = false
            },
        )
    }

    if (showEditor) {
        AdminMenuEditorDialog(
            item = editingItem,
            categories = categories,
            isSaving = isSaving,
            isUploading = isUploading,
            onUploadImage = onUploadImage,
            onDismiss = { if (!isSaving) showEditor = false },
            onSubmit = { input ->
                editingItem?.let { onUpdate(it.id, input) } ?: onCreate(input)
                showEditor = false
            },
        )
    }

    if (showFlashSaleEditor) {
        AdminFlashSaleDialog(
            items = items,
            initialItemId = flashSaleTargetId,
            isSaving = isSaving,
            onDismiss = {
                showFlashSaleEditor = false
                flashSaleTargetId = null
            },
            onSubmit = {
                onCreateFlashSale(it)
                showFlashSaleEditor = false
                flashSaleTargetId = null
            },
        )
    }

    variantTarget?.let { item ->
        AdminVariantDialog(
            itemName = item.name,
            isSaving = isSaving,
            onDismiss = { variantTarget = null },
            onSubmit = { onCreateVariant(item.id, it); variantTarget = null },
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa món ăn?") },
            text = { Text("${item.name} sẽ bị xóa hoặc ẩn nếu đang có trong đơn hàng.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(item.id)
                        deleteTarget = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AdminRed),
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Hủy") } },
        )
    }

    deleteCategoryTarget?.let { menuCategory ->
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = { Text("Xóa danh mục?") },
            text = {
                Text(
                    if (menuCategory.itemCount > 0) {
                        "${menuCategory.name} đang có ${menuCategory.itemCount} món. Hãy chuyển hoặc xóa các món trước."
                    } else {
                        "Danh mục ${menuCategory.name} sẽ bị xóa khỏi thực đơn."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(menuCategory.id)
                        deleteCategoryTarget = null
                    },
                    enabled = menuCategory.itemCount == 0,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AdminRed),
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteCategoryTarget = null }) { Text("Hủy") } },
        )
    }
}

@Composable
private fun AdminCategoryRow(
    category: AdminMenuCategory,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(AdminPrimarySoft, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(category.icon, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Bold)
                Text("${category.itemCount} món • Thứ tự ${category.sortOrder}", color = AdminMuted, fontSize = 12.sp)
            }
            if (isBusy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onEdit) { Text("Sửa") }
                TextButton(onClick = onDelete) { Text("Xóa", color = AdminRed) }
            }
        }
    }
}

@Composable
private fun AdminCategoryEditorDialog(
    category: AdminMenuCategory?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AdminMenuCategoryInput) -> Unit,
) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember(category?.id) { mutableStateOf(category?.icon ?: "🍽") }
    var sortOrder by remember(category?.id) { mutableStateOf(category?.sortOrder?.toString() ?: "0") }
    val parsedSortOrder = sortOrder.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Thêm danh mục" else "Sửa danh mục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên danh mục") }, singleLine = true)
                OutlinedTextField(icon, { icon = it.take(8) }, label = { Text("Biểu tượng") }, singleLine = true)
                OutlinedTextField(
                    sortOrder,
                    { sortOrder = it.filter(Char::isDigit) },
                    label = { Text("Thứ tự hiển thị") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(AdminMenuCategoryInput(name.trim(), icon.trim(), parsedSortOrder ?: 0)) },
                enabled = !isSaving && name.isNotBlank() && icon.isNotBlank() && parsedSortOrder != null,
            ) { Text(if (category == null) "Thêm" else "Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") } },
    )
}

@Composable
private fun AdminFlashSaleDialog(
    items: List<AdminMenuItem>,
    initialItemId: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AdminFlashSaleInput) -> Unit,
) {
    var selectedId by remember(initialItemId, items) {
        mutableStateOf(initialItemId?.takeIf { id -> items.any { it.id == id } } ?: items.firstOrNull()?.id.orEmpty())
    }
    var discount by remember { mutableStateOf("10") }
    var hours by remember { mutableStateOf("24") }
    var menuOpen by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.id == selectedId }
    val parsedDiscount = discount.toDoubleOrNull()
    val parsedHours = hours.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo flash sale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected?.name ?: "Chọn món", Modifier.weight(1f))
                    }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        items.forEach { item ->
                            DropdownMenuItem({ Text(item.name) }, onClick = { selectedId = item.id; menuOpen = false })
                        }
                    }
                }
                OutlinedTextField(discount, { discount = it.filter { char -> char.isDigit() || char == '.' } }, label = { Text("Phần trăm giảm") })
                OutlinedTextField(hours, { hours = it.filter(Char::isDigit) }, label = { Text("Thời lượng (giờ)") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(AdminFlashSaleInput(selectedId, parsedDiscount ?: 0.0, parsedHours ?: 0)) },
                enabled = !isSaving && selectedId.isNotBlank() && parsedDiscount != null && parsedDiscount in 0.01..100.0 && parsedHours != null && parsedHours > 0,
            ) { Text("Tạo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun AdminVariantDialog(
    itemName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AdminVariantInput) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var optionsText by remember { mutableStateOf("") }
    var multiple by remember { mutableStateOf(false) }
    var required by remember { mutableStateOf(true) }
    val options = optionsText.lines().mapNotNull { line ->
        val parts = line.split("|").map(String::trim)
        parts.firstOrNull()?.takeIf(String::isNotBlank)?.let { optionName ->
            AdminVariantOptionInput(optionName, parts.getOrNull(1)?.toLongOrNull() ?: 0L)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tùy chọn cho $itemName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên nhóm, ví dụ: Kích cỡ") })
                OutlinedTextField(
                    optionsText,
                    { optionsText = it },
                    label = { Text("Mỗi dòng: Tên | Giá cộng") },
                    placeholder = { Text("Nhỏ | 0\nLớn | 15000") },
                    minLines = 3,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cho chọn nhiều", Modifier.weight(1f)); Switch(multiple, { multiple = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bắt buộc chọn", Modifier.weight(1f)); Switch(required, { required = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(AdminVariantInput(name.trim(), multiple, required, options)) },
                enabled = !isSaving && name.isNotBlank() && options.isNotEmpty(),
            ) { Text("Thêm nhóm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun AdminMenuEditorDialog(
    item: AdminMenuItem?,
    categories: List<AdminMenuCategory>,
    isSaving: Boolean,
    isUploading: Boolean,
    onUploadImage: (Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (AdminMenuInput) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var description by remember(item?.id) { mutableStateOf(item?.description.orEmpty()) }
    var price by remember(item?.id) { mutableStateOf(item?.price?.toString().orEmpty()) }
    var imageUrl by remember(item?.id) { mutableStateOf(item?.imageUrl.orEmpty()) }
    var sortOrder by remember(item?.id) { mutableStateOf(item?.sortOrder?.toString() ?: "0") }
    var categoryId by remember(item?.id, categories) {
        mutableStateOf(item?.categoryId?.takeIf(String::isNotBlank) ?: categories.firstOrNull()?.id.orEmpty())
    }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var isAvailable by remember(item?.id) { mutableStateOf(item?.isAvailable ?: true) }
    var isFeatured by remember(item?.id) { mutableStateOf(item?.isFeatured ?: true) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val parsedPrice = price.toLongOrNull()
    val parsedSortOrder = sortOrder.toIntOrNull()
    val isValid = name.trim().length >= 2 && categoryId.isNotBlank() && parsedPrice != null && parsedPrice > 0 && parsedSortOrder != null
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadImage(it) { uploadedUrl -> imageUrl = uploadedUrl } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Thêm món ăn" else "Chỉnh sửa món ăn") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên món") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        enabled = categories.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(selectedCategory?.name ?: "Chọn danh mục") }
                    DropdownMenu(categoryMenuOpen, { categoryMenuOpen = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    categoryMenuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    label = { Text("Giá bán") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    label = { Text("Mô tả") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text("URL ảnh") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isUploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(if (isUploading) "Đang tải ảnh..." else "Chọn ảnh từ thiết bị")
                }
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter(Char::isDigit) },
                    label = { Text("Thứ tự hiển thị") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Đang bán", Modifier.weight(1f))
                    Switch(isAvailable, { isAvailable = it })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Món nổi bật", Modifier.weight(1f))
                    Switch(isFeatured, { isFeatured = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        AdminMenuInput(
                            categoryId = categoryId,
                            name = name.trim(),
                            description = description.trim().takeIf(String::isNotBlank),
                            basePrice = parsedPrice ?: 0,
                            imageUrl = imageUrl.trim().takeIf(String::isNotBlank),
                            isAvailable = isAvailable,
                            isFeatured = isFeatured,
                            sortOrder = parsedSortOrder ?: 0,
                        )
                    )
                },
                enabled = isValid && !isSaving && !isUploading,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (item == null) "Thêm món" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") } },
    )
}

@Composable
fun AdminMessagesScreen(conversations: List<AdminConversation>, onOpenChat: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var unreadOnly by remember { mutableStateOf(false) }
    val filtered = conversations.filter {
        (!unreadOnly || it.unread > 0) && (query.isBlank() || it.customer.contains(query, true) || it.lastMessage.contains(query, true))
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(AdminGreenSoft).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(AdminGreen, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("Đang trực tuyến • Sẵn sàng nhận tin", color = AdminGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        AdminSearchField(query, { query = it }, "Tìm khách hàng hoặc nội dung", Modifier.padding(16.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!unreadOnly, { unreadOnly = false }, { Text("Tất cả") })
            FilterChip(unreadOnly, { unreadOnly = true }, { Text("Chưa đọc") })
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { conversation ->
                AdminConversationRow(conversation, { onOpenChat(conversation.id) })
            }
        }
    }
}

@Composable
fun AdminChatDetailScreen(conversation: AdminConversation, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val client = remember { AdminChatSocketClient(context) }
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(ChatConnectionState.CONNECTING) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.id) {
        client.connect(
            conversationId = conversation.id,
            onState = { connectionState = it },
            onHistory = { history -> messages.clear(); messages.addAll(history) },
            onMessage = { message -> if (messages.none { it.id == message.id }) messages.add(message) },
            onError = { error = it },
        )
    }
    DisposableEffect(client) { onDispose { client.disconnect() } }

    fun send() {
        val content = draft.trim()
        if (content.isBlank() || isSending || connectionState != ChatConnectionState.CONNECTED) return
        isSending = true
        client.send(conversation.id, content) { success, message ->
            isSending = false
            if (success) draft = "" else error = message ?: "Không thể gửi tin nhắn"
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(AdminSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AdminAvatar(conversation.customer)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(conversation.customer, fontWeight = FontWeight.Bold)
                Text(
                    if (connectionState == ChatConnectionState.CONNECTED) "Đã kết nối" else "Đang kết nối...",
                    color = if (connectionState == ChatConnectionState.CONNECTED) AdminGreen else AdminMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            conversation.orderCode?.let { Text(it, color = AdminPrimary, fontWeight = FontWeight.Bold) }
            TextButton(onClick = onClose) { Text("Đóng hội thoại") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            error?.let { item { Text(it, color = AdminRed, fontSize = 12.sp) } }
            items(messages, key = { it.id }) { message ->
                AdminMessageBubble(
                    AdminUiMessage(
                        id = message.id,
                        content = message.content,
                        time = message.createdAt.take(16),
                        fromAdmin = !message.isCustomer,
                    )
                )
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Quán đã nhận yêu cầu", "Món sắp hoàn thành", "Cảm ơn bạn")) { reply ->
                AssistChip(onClick = { draft = reply }, label = { Text(reply) })
            }
        }
        Row(Modifier.fillMaxWidth().background(AdminSurface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = ::send,
                enabled = !isSending && connectionState == ChatConnectionState.CONNECTED,
                modifier = Modifier.background(AdminPrimary, CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White) }
        }
    }
}

@Composable
fun AdminProfileScreen(user: UserDto?, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(84.dp).background(AdminPrimary, CircleShape), contentAlignment = Alignment.Center) {
            Text("AD", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                user?.fullName?.takeIf(String::isNotBlank) ?: "Quản trị viên FoodHub",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(user?.email.orEmpty(), color = AdminMuted)
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AdminProfileItem(Icons.Default.Person, "Vai trò", user?.role ?: "ADMIN")
                user?.phoneNumber?.takeIf(String::isNotBlank)?.let { phone ->
                    HorizontalDivider()
                    AdminProfileItem(Icons.Default.Person, "Số điện thoại", phone)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Đăng xuất khỏi Admin") }
    }
}

@Composable
private fun AdminMetric(title: String, value: String, caption: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(18.dp), tint = color)
                }
                Spacer(Modifier.weight(1f))
                Text(caption, color = color, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, color = AdminMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AdminAlert(title: String, message: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AdminAmberSoft, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = AdminAmber)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = AdminMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = AdminMuted)
    }
}

@Composable
private fun AdminSectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun AdminOrderCard(order: AdminOrder, onClick: () -> Unit) {
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
private fun AdminStatusPill(status: AdminOrderStatus) {
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

@Composable
private fun AdminSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Search, null) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun AdminMenuRow(
    item: AdminMenuItem,
    isBusy: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVariant: () -> Unit,
    onFlashSale: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(AdminPrimarySoft, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(item.icon, color = AdminPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.category, color = AdminMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.salePrice?.let { Text(it.vnd(), color = AdminPrimary, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(6.dp))
                    Text(item.price.vnd(), color = if (item.salePrice != null) AdminMuted else AdminText, fontSize = 13.sp)
                }
            }
            if (isBusy) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = AdminPrimary)
            } else {
                Switch(checked = item.isAvailable, onCheckedChange = { onToggle() })
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Tùy chọn") }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Chỉnh sửa") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem({ Text("Thêm nhóm tùy chọn") }, onClick = { menuOpen = false; onVariant() })
                    DropdownMenuItem({ Text("Tạo giảm giá") }, onClick = { menuOpen = false; onFlashSale() })
                    DropdownMenuItem({ Text("Xóa", color = AdminRed) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun AdminConversationRow(conversation: AdminConversation, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AdminSurface, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AdminAvatar(conversation.customer)
            if (conversation.isOnline) Box(Modifier.align(Alignment.BottomEnd).size(11.dp).background(
                AdminGreen, CircleShape))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(conversation.customer, fontWeight = if (conversation.unread > 0) FontWeight.Bold else FontWeight.Medium)
                Text(conversation.time, color = AdminMuted, fontSize = 11.sp)
            }
            Text(conversation.lastMessage, color = AdminMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (conversation.unread > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(22.dp).background(AdminPrimary, CircleShape), contentAlignment = Alignment.Center) {
                Text(conversation.unread.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminAvatar(name: String) {
    Box(Modifier.size(42.dp).background(AdminBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "K", color = AdminBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminMessageBubble(message: AdminUiMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromAdmin) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(0.78f).background(if (message.fromAdmin) AdminPrimary else AdminSurface, RoundedCornerShape(12.dp)).padding(11.dp),
            horizontalAlignment = if (message.fromAdmin) Alignment.End else Alignment.Start,
        ) {
            Text(message.content, color = if (message.fromAdmin) Color.White else AdminText)
            Text(message.time, color = if (message.fromAdmin) Color.White.copy(alpha = 0.75f) else AdminMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AdminPlaceholder(icon: ImageVector, title: String, message: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).background(AdminPrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(30.dp), tint = AdminPrimary)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(message, color = AdminMuted)
        Spacer(Modifier.height(18.dp))
        ElevatedButton(onClick = onClick) { Text(button) }
    }
}

@Composable
private fun AdminProfileItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AdminPrimary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = AdminMuted, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Medium)
        }
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

private fun Long.vnd(): String = String.format(Locale.forLanguageTag("vi-VN"), "%,d đ", this)

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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdminProfileScreenPreview() {
    FoodHubAppTheme {
        AdminProfileScreen(user = null, onLogout = {})
    }
}

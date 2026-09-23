package com.foodhub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Chào buổi trưa, FoodHub", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(15.dp), tint = AdminMuted)
                Spacer(Modifier.width(6.dp))
                Text("Hôm nay, 21 tháng 9", color = AdminMuted, fontSize = 13.sp)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Doanh thu", "2,48 tr", "+12,4%", Icons.AutoMirrored.Filled.TrendingUp, AdminGreen, Modifier.weight(1f))
                    AdminMetric("Đơn đang xử lý", activeOrders.size.toString(), "${orders.size} tổng đơn", Icons.AutoMirrored.Filled.ReceiptLong, AdminBlue, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric("Chờ xác nhận", orders.count { it.status == AdminOrderStatus.PENDING_CONFIRMATION }.toString(), "Cần xử lý", Icons.Default.AccessTime, AdminAmber, Modifier.weight(1f))
                    AdminMetric("Tin chưa đọc", conversations.sumOf { it.unread }.toString(), "${conversations.count { it.isOnline }} đang online", Icons.Default.ChatBubbleOutline, AdminPrimary, Modifier.weight(1f))
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
    onUpdateOrder: (AdminOrder) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<AdminOrderStatus?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<AdminOrder, Boolean>?>(null) }
    val filtered = orders.filter { order ->
        (selectedStatus == null || order.status == selectedStatus) &&
            (query.isBlank() || order.code.contains(query, true) || order.destination.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        AdminSearchField(query, { query = it }, "Tìm mã đơn, bàn hoặc địa chỉ", Modifier.padding(16.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selectedStatus == null, { selectedStatus = null }, { Text("Tất cả") }) }
            items(listOf(AdminOrderStatus.PENDING_CONFIRMATION, AdminOrderStatus.CONFIRMED, AdminOrderStatus.PREPARING, AdminOrderStatus.READY, AdminOrderStatus.COMPLETED)) { status ->
                FilterChip(selectedStatus == status, { selectedStatus = status }, { Text(status.label) })
            }
        }
        Text("${filtered.size} đơn hàng", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = AdminMuted, fontSize = 13.sp)
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.id }) { order ->
                Column {
                    AdminOrderCard(order, onClick = { onOpenOrder(order.id) })
                    if (order.status == AdminOrderStatus.PENDING_CONFIRMATION) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ pendingAction = order to false }, Modifier.weight(1f)) { Text("Từ chối") }
                            Button({ pendingAction = order to true }, Modifier.weight(1f)) { Text("Xác nhận") }
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
            text = { Text(if (approve) "Đơn sẽ được chuyển sang trạng thái đã xác nhận." else "Khách hàng sẽ thấy đơn đã bị hủy.") },
            confirmButton = {
                Button(onClick = {
                    onUpdateOrder(order.copy(status = if (approve) AdminOrderStatus.CONFIRMED else AdminOrderStatus.CANCELLED))
                    pendingAction = null
                }) { Text(if (approve) "Xác nhận" else "Từ chối") }
            },
            dismissButton = { TextButton({ pendingAction = null }) { Text("Đóng") } },
        )
    }
}

@Composable
fun AdminOrderDetailScreen(order: AdminOrder, onUpdate: (AdminOrder) -> Unit) {
    val next = nextOrderStatus(order.status)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
        items(order.items) { line ->
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
        if (next != null) {
            item {
                Button(
                    onClick = { onUpdate(order.copy(status = next)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Chuyển sang: ${next.label}") }
            }
        }
    }
}

@Composable
fun AdminMenuScreen(
    items: List<AdminMenuItem>,
    onToggleAvailability: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    var tab by remember { mutableStateOf("Món ăn") }
    val categories = listOf("Tất cả") + items.map { it.category }.distinct()
    val filtered = items.filter { (category == "Tất cả" || it.category == category) && (query.isBlank() || it.name.contains(query, true)) }

    Scaffold(
        containerColor = AdminBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = { onMessage("Mở biểu mẫu thêm món") }, containerColor = AdminPrimary) {
                Icon(Icons.Default.Add, "Thêm", tint = Color.White)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AdminSearchField(query, { query = it }, "Tìm món ăn", Modifier.padding(16.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Món ăn", "Danh mục", "Flash sale").forEach { label ->
                    FilterChip(tab == label, { tab = label }, { Text(label) })
                }
            }
            if (tab == "Món ăn") {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { item -> FilterChip(category == item, { category = item }, { Text(item) }) }
                }
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { menuItem ->
                        AdminMenuRow(menuItem, { onToggleAvailability(menuItem.id) }, onMessage)
                    }
                }
            } else {
                AdminPlaceholder(
                    icon = if (tab == "Danh mục") Icons.Default.RestaurantMenu else Icons.AutoMirrored.Filled.TrendingUp,
                    title = tab,
                    message = if (tab == "Danh mục") "Quản lý và sắp xếp nhóm món tại đây." else "Tạo chương trình giảm giá theo khung giờ.",
                    button = if (tab == "Danh mục") "Thêm danh mục" else "Tạo flash sale",
                    onClick = { onMessage("Mở biểu mẫu $tab") },
                )
            }
        }
    }
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
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { conversation ->
                AdminConversationRow(conversation, { onOpenChat(conversation.id) })
            }
        }
    }
}

@Composable
fun AdminChatDetailScreen(conversation: AdminConversation) {
    val messages = remember {
        mutableStateListOf(
            AdminUiMessage("1", "Xin chào, FoodHub có thể hỗ trợ gì cho bạn?", "12:20", true),
            AdminUiMessage("2", conversation.lastMessage, conversation.time, false),
        )
    }
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(AdminSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AdminAvatar(conversation.customer)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(conversation.customer, fontWeight = FontWeight.Bold)
                Text(if (conversation.isOnline) "Đang trực tuyến" else "Không trực tuyến", color = if (conversation.isOnline) AdminGreen else AdminMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            conversation.orderCode?.let { Text(it, color = AdminPrimary, fontWeight = FontWeight.Bold) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message -> AdminMessageBubble(message) }
        }
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                onClick = {
                    if (draft.isNotBlank()) {
                        messages.add(AdminUiMessage(System.nanoTime().toString(), draft.trim(), "Bây giờ", true))
                        draft = ""
                    }
                },
                modifier = Modifier.background(AdminPrimary, CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White) }
        }
    }
}

@Composable
fun AdminProfileScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(84.dp).background(AdminPrimary, CircleShape), contentAlignment = Alignment.Center) {
            Text("AD", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Quản trị viên FoodHub", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("admin@foodhub.vn", color = AdminMuted)
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AdminProfileItem(Icons.Default.Storefront, "Chi nhánh", "FoodHub Keangnam")
                HorizontalDivider()
                AdminProfileItem(Icons.Default.Person, "Vai trò", "Quản trị viên")
                HorizontalDivider()
                AdminProfileItem(Icons.Default.Inventory2, "Ca làm việc", "09:00 - 17:00")
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
private fun AdminMenuRow(item: AdminMenuItem, onToggle: () -> Unit, onMessage: (String) -> Unit) {
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
            Switch(checked = item.isAvailable, onCheckedChange = { onToggle() })
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Tùy chọn") }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Chỉnh sửa") }, onClick = { menuOpen = false; onMessage("Mở chỉnh sửa ${item.name}") })
                    DropdownMenuItem({ Text("Tạo giảm giá") }, onClick = { menuOpen = false; onMessage("Mở giảm giá ${item.name}") })
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
            if (conversation.isOnline) Box(Modifier.align(Alignment.BottomEnd).size(11.dp).background(AdminGreen, CircleShape))
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

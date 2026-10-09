package com.example.foodhubapp.feature.order.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.menu.ui.FoodImage
import com.example.foodhubapp.core.payment.VnPayReturn
import com.example.foodhubapp.core.payment.ZaloPayLaunchDialog
import com.example.foodhubapp.feature.order.data.CheckoutPaymentMethod
import com.example.foodhubapp.feature.order.data.CustomerOrder
import com.example.foodhubapp.feature.order.data.OrderItem
import com.example.foodhubapp.feature.order.data.OrderStatus
import com.example.foodhubapp.feature.order.data.OrderType
import com.example.foodhubapp.feature.order.viewmodel.OrderGroup
import com.example.foodhubapp.feature.order.viewmodel.OrderListUiState
import com.example.foodhubapp.feature.order.viewmodel.OrderListViewModel
import com.example.foodhubapp.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OrderListRoute(
    paymentReturn: VnPayReturn? = null,
    onPaymentReturnConsumed: (VnPayReturn) -> Unit = {},
    onHomeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: OrderListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var cancelTarget by remember { mutableStateOf<CustomerOrder?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(refresh = true) }
    LaunchedEffect(paymentReturn) {
        paymentReturn?.let {
            viewModel.handlePaymentReturn(it.orderCode, it.result, it.provider)
            onPaymentReturnConsumed(it)
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(state.paymentLaunch, state.paymentLaunchMethod) {
        if (state.paymentLaunchMethod == CheckoutPaymentMethod.VNPAY) state.paymentLaunch?.let { launch ->
            runCatching { uriHandler.openUri(launch.url) }
                .onFailure { snackbar.showSnackbar("Không thể mở trang thanh toán VNPay.") }
            viewModel.consumePaymentLaunch()
        }
    }

    if (state.paymentLaunchMethod == CheckoutPaymentMethod.ZALOPAY) {
        state.paymentLaunch?.let { launch ->
            ZaloPayLaunchDialog(
                paymentUrl = launch.url,
                qrContent = launch.qrContent,
                onOpenOnThisDevice = {
                    runCatching { uriHandler.openUri(launch.url) }
                    viewModel.consumePaymentLaunch()
                },
                onDismiss = viewModel::consumePaymentLaunch,
            )
        }
    }

    OrderListScreen(
        state = state,
        onGroupClick = viewModel::selectGroup,
        onTypeClick = viewModel::selectType,
        onRefresh = { viewModel.load(refresh = true) },
        onRetry = { viewModel.load() },
        onLoadMore = viewModel::loadMore,
        onCancel = { cancelTarget = it },
        onPay = viewModel::pay,
        onHomeClick = onHomeClick,
        onNotificationClick = {
            onNotificationClick()
            scope.launch { snackbar.showSnackbar("Thông báo chưa được nối vào màn hình riêng.") }
        },
        onProfileClick = onProfileClick,
        snackbarHost = { SnackbarHost(snackbar) }
    )

    cancelTarget?.let { order ->
        CancelOrderDialog(
            orderCode = order.orderCode,
            busy = state.busyOrderId == order.id,
            onDismiss = { if (state.busyOrderId == null) cancelTarget = null },
            onConfirm = { reason ->
                cancelTarget = null
                viewModel.cancel(order, reason)
            }
        )
    }
    if (state.requiresLogin) AlertDialog(
        onDismissRequest = viewModel::dismissLogin,
        title = { Text("Đăng nhập") },
        text = { Text("Bạn cần đăng nhập để xem đơn hàng của mình.") },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissLogin(); onLoginClick() }) { Text("Đăng nhập") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissLogin) { Text("Để sau") } }
    )
}

@Composable
fun OrderListScreen(
    state: OrderListUiState,
    onGroupClick: (OrderGroup) -> Unit,
    onTypeClick: (OrderType?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onCancel: (CustomerOrder) -> Unit,
    onPay: (CustomerOrder) -> Unit,
    onHomeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    snackbarHost: @Composable () -> Unit = {}
) {
    Scaffold(
        containerColor = AppBackground,
        snackbarHost = snackbarHost,
        topBar = { OrderHeader(state, onRefresh) },
        bottomBar = { OrderBottomBar(onHomeClick, onNotificationClick, onProfileClick) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth()) {
            OrderGroupTabs(state.selectedGroup, onGroupClick)
            OrderTypeFilters(state.selectedType, onTypeClick)
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Brand)
                    state.error != null -> OrderError(state.error, onRetry, Modifier.align(Alignment.Center))
                    state.visibleOrders.isEmpty() -> EmptyOrders(state.selectedGroup, Modifier.align(Alignment.Center))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.visibleOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                busy = state.busyOrderId == order.id ||
                                    state.checkingPaymentOrderCode == order.orderCode,
                                onCancel = { onCancel(order) },
                                onPay = { onPay(order) }
                            )
                        }
                        if (state.hasMore) item {
                            TextButton(
                                onClick = onLoadMore,
                                enabled = !state.isLoadingMore,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (state.isLoadingMore) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Text("Tải thêm đơn hàng", color = Brand)
                            }
                        }
                    }
                }
                if (state.isRefreshing) LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Brand,
                    trackColor = BrandSoft
                )
            }
        }
        }
    }
}

@Composable
private fun OrderHeader(state: OrderListUiState, onRefresh: () -> Unit) {
    Surface(color = AppBackground) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(38.dp).background(Brand, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Đơn hàng", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Neutral)
                val activeCount = state.orders.count { it.status.isActiveOrder() }
                Text(
                    if (activeCount > 0) "$activeCount đơn đang hoạt động" else "Theo dõi các đơn của bạn",
                    color = OnSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = onRefresh,
                enabled = !state.isLoading && !state.isRefreshing,
                modifier = Modifier.semantics { contentDescription = "Làm mới đơn hàng" }
            ) { Icon(Icons.Default.Refresh, null, tint = Brand) }
        }
    }
}

@Composable
private fun OrderGroupTabs(selected: OrderGroup, onClick: (OrderGroup) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .background(InputBackgroundSoft, RoundedCornerShape(8.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            OrderGroup.ACTIVE to "Đang xử lý",
            OrderGroup.COMPLETED to "Hoàn thành",
            OrderGroup.CANCELLED to "Đã hủy"
        ).forEach { (group, label) ->
            val active = group == selected
            Box(
                Modifier.weight(1f).height(38.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) Brand else Color.Transparent)
                    .clickable { onClick(group) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) Color.White else OnSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun OrderTypeFilters(selected: OrderType?, onClick: (OrderType?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { OrderFilterChip("Tất cả", Icons.Default.GridView, selected == null) { onClick(null) } }
        item { OrderFilterChip("Tại bàn", Icons.Default.TableRestaurant, selected == OrderType.DINE_IN) { onClick(OrderType.DINE_IN) } }
        item { OrderFilterChip("Mang về", Icons.Default.ShoppingBag, selected == OrderType.TAKEAWAY) { onClick(OrderType.TAKEAWAY) } }
        item { OrderFilterChip("Giao hàng", Icons.Default.DeliveryDining, selected == OrderType.DELIVERY) { onClick(OrderType.DELIVERY) } }
    }
}

@Composable
private fun OrderFilterChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = OnSurfaceVariant,
            iconColor = OnSurfaceVariant,
            selectedContainerColor = Brand,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CardStroke,
            selectedBorderColor = Brand
        )
    )
}

@Composable
private fun OrderCard(order: CustomerOrder, busy: Boolean, onCancel: () -> Unit, onPay: () -> Unit) {
    var expanded by rememberSaveable(order.id) { mutableStateOf(false) }
    val visibleItems = if (expanded) order.items else order.items.take(2)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = CeramicSurface,
        border = BorderStroke(1.dp, CardStroke),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${order.orderCode.removePrefix("#")}", fontWeight = FontWeight.Bold, color = Neutral)
                Text(" · ${formatOrderTime(order.createdAt)}", color = CaptionBrown, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                StatusBadge(order.status)
            }
            OrderContext(order)
            visibleItems.forEach { OrderItemRow(it) }
            if (order.items.size > 2) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(InputBackgroundSoft)
                        .clickable { expanded = !expanded }.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (expanded) "Thu gọn" else "+ ${order.items.size - 2} món khác",
                        Modifier.weight(1f), color = OnSurfaceVariant, fontSize = 11.sp
                    )
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = Brand)
                }
            }
            if (order.status in setOf(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.SERVED)) {
                OrderProgress(order.status)
            }
            if (order.cancelReason != null && order.status == OrderStatus.CANCELLED) {
                Text("Lý do hủy: ${order.cancelReason}", color = OnSurfaceVariant, fontSize = 11.sp)
            }
            HorizontalDivider(color = CardStroke)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Tổng thanh toán", color = OnSurfaceVariant, fontSize = 11.sp)
                    if (!order.paymentStatus.isNullOrBlank()) Text(
                        paymentLabel(order), color = if (order.paymentStatus == "PAID") Success else CaptionBrown, fontSize = 10.sp
                    )
                }
                Text(money(order.totalAmount), color = Brand, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (order.items.isNotEmpty()) Text(" (${order.items.sumOf { it.quantity }} món)", color = CaptionBrown, fontSize = 10.sp)
            }
            val canCancel = order.status in setOf(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED)
            val canPay = order.status == OrderStatus.PENDING_PAYMENT
            if (canCancel || canPay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canCancel) OutlinedButton(
                    onClick = onCancel,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand),
                    border = BorderStroke(1.dp, BrandSoft)
                ) { Text("Hủy đơn") }
                if (canPay) Button(
                    onClick = onPay,
                    enabled = !busy,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.CreditCard, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thanh toán ngay")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderContext(order: CustomerOrder) {
    val (icon, text) = when (order.type) {
        OrderType.DINE_IN -> Icons.Default.TableRestaurant to buildString {
            append("Tại bàn")
            order.tableName?.let { append(" · $it") }
            order.tableFloor?.let { append(" ($it)") }
        }
        OrderType.TAKEAWAY -> Icons.Default.ShoppingBag to buildString {
            append("Mang về")
            order.pickupCode?.let { append(" · Mã nhận: $it") }
        }
        OrderType.DELIVERY -> Icons.Default.DeliveryDining to buildString {
            append("Giao hàng")
            order.deliveryAddress?.let { append(" · $it") }
        }
    }
    Row(
        Modifier.fillMaxWidth().background(BrandSoft.copy(alpha = .55f), RoundedCornerShape(6.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = Brand)
        Spacer(Modifier.width(5.dp))
        Text(text, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        FoodImage(item.imageUrl, item.name, Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text("${item.quantity}x ${item.name}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.description.isNotBlank()) Text(
                item.description, color = OnSurfaceVariant, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Text(money(if (item.subTotal > 0) item.subTotal else item.unitPrice * item.quantity), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun OrderProgress(status: OrderStatus) {
    val steps = listOf(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.SERVED)
    val current = steps.indexOf(status).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().background(SuccessSoft, RoundedCornerShape(6.dp)).padding(9.dp)) {
        Text(status.label(), color = SuccessDark, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { index, _ ->
                Box(Modifier.size(8.dp).background(if (index <= current) Success else MutedDot, CircleShape))
                if (index < steps.lastIndex) Box(Modifier.weight(1f).height(2.dp).background(if (index < current) Success else MutedDot))
            }
        }
    }
}

@Composable
private fun StatusBadge(status: OrderStatus) {
    val background = when (status) {
        OrderStatus.COMPLETED -> SuccessSoft
        OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED -> Color(0xFFFFE6E3)
        OrderStatus.PENDING_PAYMENT -> Color(0xFFFFE4A8)
        else -> Color(0xFFD9FBEA)
    }
    val foreground = when (status) {
        OrderStatus.COMPLETED -> SuccessDark
        OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED -> Color(0xFF9C1C10)
        OrderStatus.PENDING_PAYMENT -> WarningDark
        else -> SuccessDark
    }
    Text(
        status.label(),
        modifier = Modifier.background(background, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
        color = foreground,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        maxLines = 1
    )
}

@Composable
private fun CancelOrderDialog(orderCode: String, busy: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable(orderCode) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(8.dp),
        title = { Text("Hủy đơn #${orderCode.removePrefix("#")}") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(255) },
                label = { Text("Lý do hủy") },
                supportingText = { Text("${reason.length}/255") },
                minLines = 3,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim()) }, enabled = reason.isNotBlank() && !busy) {
                Text("Xác nhận hủy", color = Brand)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Quay lại") } }
    )
}

@Composable
private fun EmptyOrders(group: OrderGroup, modifier: Modifier = Modifier) {
    val message = when (group) {
        OrderGroup.ACTIVE -> "Bạn chưa có đơn đang xử lý."
        OrderGroup.COMPLETED -> "Bạn chưa có đơn đã hoàn thành."
        OrderGroup.CANCELLED -> "Bạn chưa có đơn đã hủy."
    }
    Column(modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Brand, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Chưa có đơn hàng", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(message, color = OnSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun OrderError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CloudOff, null, tint = Brand, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, color = OnSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Thử lại") }
    }
}

@Composable
private fun OrderBottomBar(onHomeClick: () -> Unit, onNotificationClick: () -> Unit, onProfileClick: () -> Unit) {
    Surface(color = AppBackground.copy(alpha = .97f), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 8.dp)) {
            OrderBottomDestination(Icons.Default.Home, "Trang chủ", false, onHomeClick)
            OrderBottomDestination(Icons.AutoMirrored.Filled.ReceiptLong, "Đơn hàng", true, {})
            OrderBottomDestination(Icons.Default.NotificationsNone, "Thông báo", false, onNotificationClick)
            OrderBottomDestination(Icons.Default.Person, "Cá nhân", false, onProfileClick)
        }
    }
}

@Composable
private fun RowScope.OrderBottomDestination(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = if (selected) Brand else OnSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (selected) Brand else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun OrderStatus.label(): String = when (this) {
    OrderStatus.PENDING_PAYMENT -> "Chờ thanh toán"
    OrderStatus.PENDING_CONFIRMATION -> "Chờ xác nhận"
    OrderStatus.CONFIRMED -> "Đã xác nhận"
    OrderStatus.PREPARING -> "Đang chế biến"
    OrderStatus.READY -> "Sẵn sàng"
    OrderStatus.SERVED -> "Đã phục vụ"
    OrderStatus.COMPLETED -> "Hoàn thành"
    OrderStatus.CANCELLED -> "Đã hủy"
    OrderStatus.PAYMENT_FAILED -> "Thanh toán lỗi"
    OrderStatus.UNKNOWN -> "Đang cập nhật"
}

private fun OrderStatus.isActiveOrder() = this in setOf(
    OrderStatus.PENDING_PAYMENT,
    OrderStatus.PENDING_CONFIRMATION,
    OrderStatus.CONFIRMED,
    OrderStatus.PREPARING,
    OrderStatus.READY,
    OrderStatus.SERVED,
    OrderStatus.UNKNOWN
)

private fun paymentLabel(order: CustomerOrder): String = when (order.paymentStatus) {
    "PAID" -> "Đã thanh toán${order.paymentMethod?.let { " qua $it" }.orEmpty()}"
    "REFUNDED" -> "Đã hoàn tiền"
    "FAILED" -> "Thanh toán thất bại"
    else -> "Chưa thanh toán"
}

private fun formatOrderTime(raw: String): String {
    if (raw.isBlank()) return "Vừa đặt"
    val formatter = DateTimeFormatter.ofPattern("HH:mm · dd/MM", Locale.forLanguageTag("vi-VN"))
    return runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).format(formatter) }
        .recoverCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).format(formatter) }
        .getOrDefault(raw)
}

private fun money(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"

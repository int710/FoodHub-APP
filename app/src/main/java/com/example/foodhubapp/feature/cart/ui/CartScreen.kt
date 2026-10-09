package com.example.foodhubapp.feature.cart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.cart.data.Cart
import com.example.foodhubapp.feature.cart.data.CartItem
import com.example.foodhubapp.feature.cart.data.CartType
import com.example.foodhubapp.feature.cart.viewmodel.CartViewModel
import com.example.foodhubapp.core.payment.VnPayReturn
import com.example.foodhubapp.core.payment.ZaloPayLaunchDialog
import com.example.foodhubapp.feature.menu.ui.FoodDetail
import com.example.foodhubapp.feature.menu.ui.FoodImage
import com.example.foodhubapp.feature.order.data.CheckoutPaymentMethod
import com.example.foodhubapp.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Route có state: kết nối CartViewModel với UI, snackbar, dialog xác nhận và
 * điều hướng đăng nhập. CartScreen phía dưới chỉ nhận dữ liệu/callback.
 */
@Composable
fun CartRoute(
    paymentReturn: VnPayReturn? = null,
    onPaymentReturnConsumed: (VnPayReturn) -> Unit = {},
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onScanQrClick: () -> Unit,
    viewModel: CartViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var deleteTarget by remember { mutableStateOf<CartItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    // Cart có thể đã nằm trong back stack trước khi người dùng thêm món. Luôn
    // đồng bộ lại Redis khi quay về màn hình để item vừa thêm xuất hiện ngay.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(paymentReturn) {
        paymentReturn?.let {
            viewModel.handlePaymentReturn(it.orderCode, it.result, it.provider)
            onPaymentReturnConsumed(it)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.paymentLaunch, state.paymentMethod) {
        if (state.paymentMethod == CheckoutPaymentMethod.VNPAY) state.paymentLaunch?.let { launch ->
            val url = launch.url
            runCatching { uriHandler.openUri(url) }
                .onFailure { snackbar.showSnackbar("Không thể mở trang thanh toán VNPay.") }
            viewModel.consumePaymentLaunch()
        }
    }

    if (state.paymentMethod == CheckoutPaymentMethod.ZALOPAY) {
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

    CartScreen(
        cart = state.cart,
        isLoading = state.isLoading,
        error = state.error,
        busyItemId = state.busyItemId,
        isClearing = state.isClearing,
        isCheckingOut = state.isCheckingOut,
        onBackClick = onBackClick,
        onRetry = viewModel::load,
        onQuantityChange = viewModel::changeQuantity,
        onEdit = viewModel::edit,
        onDelete = { deleteTarget = it },
        onClear = { confirmClear = true },
        onCheckout = viewModel::checkout,
        snackbarHost = { SnackbarHost(snackbar) },
        cartType = state.cartType,
        paymentMethod = state.paymentMethod,
        onPaymentMethodChange = viewModel::selectPaymentMethod
    )

    // Giữ item cần xóa ở Route để chỉ gọi API sau khi người dùng xác nhận.
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa món") },
            text = { Text("Xóa ${item.name} khỏi giỏ hàng?") },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; viewModel.delete(item) }) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Hủy") } }
        )
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Xóa toàn bộ giỏ hàng") },
        text = { Text("Thao tác này sẽ xóa tất cả món trong giỏ.") },
        confirmButton = {
            TextButton(onClick = { confirmClear = false; viewModel.clear() }) { Text("Xóa tất cả") }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Hủy") } }
    )
    // ViewModel tải FoodDetail trước; dialog chỉ mở khi đã có contract option đầy đủ.
    state.editingItem?.let { item ->
        if (state.isLoadingEditor) AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Đang tải tùy chọn") },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        ) else state.editingFood?.let { food ->
            CartEditDialog(item, food, viewModel::dismissEditor, viewModel::saveEdit)
        }
    }
    if (state.requiresLogin) AlertDialog(
        onDismissRequest = viewModel::dismissLogin,
        title = { Text("Đăng nhập") },
        text = { Text("Bạn cần đăng nhập để xem và chỉnh sửa giỏ hàng.") },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissLogin(); onLoginClick() }) { Text("Đăng nhập") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissLogin) { Text("Để sau") } }
    )
    if (state.requiresTableScan) AlertDialog(
        onDismissRequest = viewModel::dismissTableScan,
        title = { Text("Phiên bàn đã hết hạn") },
        text = { Text("Vui lòng quét lại mã QR trên bàn để tiếp tục với giỏ tại bàn.") },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissTableScan(); onScanQrClick() }) { Text("Quét lại QR") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissTableScan) { Text("Để sau") } }
    )
}

/**
 * UI thuần của giỏ hàng. Hàm này không gọi API trực tiếp nên có thể Preview/test
 * bằng Cart giả và callback giả.
 */
@Composable
fun CartScreen(
    cart: Cart,
    isLoading: Boolean,
    error: String?,
    busyItemId: String?,
    isClearing: Boolean,
    isCheckingOut: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onQuantityChange: (CartItem, Int) -> Unit,
    onEdit: (CartItem) -> Unit,
    onDelete: (CartItem) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    cartType: CartType = CartType.TAKEAWAY,
    paymentMethod: CheckoutPaymentMethod = CheckoutPaymentMethod.CASH,
    onPaymentMethodChange: (CheckoutPaymentMethod) -> Unit = {}
) {
    Scaffold(
        containerColor = AppBackground,
        snackbarHost = snackbarHost,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
                Column(Modifier.weight(1f)) {
                    Text("Giỏ hàng", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(
                        when (cartType) {
                            CartType.DINE_IN -> "Ăn tại bàn • DINE_IN"
                            CartType.TAKEAWAY -> "Nhận tại quầy • TAKEAWAY"
                            CartType.DELIVERY -> "Giao tận nơi • DELIVERY"
                        },
                        color = OnSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
                if (cart.items.isNotEmpty()) TextButton(onClick = onClear, enabled = !isClearing && busyItemId == null) {
                    Text("Xóa tất cả", color = Brand)
                }
            }
        },
        bottomBar = {
            if (cart.items.isNotEmpty()) Surface(shadowElevation = 8.dp, color = CeramicSurface) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Phương thức thanh toán", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentChoice(
                            label = if (cartType == CartType.DINE_IN) "Tiền mặt tại bàn" else "Tiền mặt khi nhận",
                            selected = paymentMethod == CheckoutPaymentMethod.CASH,
                            enabled = !isCheckingOut,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onPaymentMethodChange(CheckoutPaymentMethod.CASH) }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentChoice(
                                label = "VNPay",
                                selected = paymentMethod == CheckoutPaymentMethod.VNPAY,
                                enabled = !isCheckingOut,
                                modifier = Modifier.weight(1f),
                                onClick = { onPaymentMethodChange(CheckoutPaymentMethod.VNPAY) }
                            )
                            PaymentChoice(
                                label = "ZaloPay / QR",
                                selected = paymentMethod == CheckoutPaymentMethod.ZALOPAY,
                                enabled = !isCheckingOut,
                                modifier = Modifier.weight(1f),
                                onClick = { onPaymentMethodChange(CheckoutPaymentMethod.ZALOPAY) }
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tổng cộng", color = OnSurfaceVariant)
                        Text(money(cart.totalAmount), color = Brand, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Button(
                        onClick = onCheckout,
                        enabled = !isCheckingOut && !isClearing && busyItemId == null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) {
                        if (isCheckingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Đang xử lý…")
                        } else {
                            Text(
                                when (paymentMethod) {
                                    CheckoutPaymentMethod.VNPAY -> "Thanh toán bằng VNPay"
                                    CheckoutPaymentMethod.ZALOPAY -> "Thanh toán bằng ZaloPay"
                                    CheckoutPaymentMethod.CASH -> "Gửi đơn • Thanh toán tiền mặt"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Brand)
                error != null -> ErrorContent(error, onRetry, Modifier.align(Alignment.Center))
                cart.items.isEmpty() -> EmptyCart(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().align(Alignment.TopCenter),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cart.items, key = { it.id }) { item ->
                        CartItemRow(
                            item,
                            item.id == busyItemId,
                            { onQuantityChange(item, it) },
                            { onEdit(item) },
                            { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Brand.copy(alpha = 0.08f) else CeramicSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Brand else CardStroke
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    busy: Boolean,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = CeramicSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FoodImage(item.imageUrl, item.name, Modifier.size(88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(item.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onEdit, enabled = !busy, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Sửa ${item.name}", Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, enabled = !busy, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Xóa ${item.name}", Modifier.size(19.dp), tint = Brand)
                    }
                }
                if (item.options.isNotEmpty()) Text(
                    item.options.joinToString { it.name },
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (item.note.isNotBlank()) Text(
                    "Ghi chú: ${item.note}",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(money(item.subTotal), Modifier.weight(1f), color = Brand, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onQuantityChange(item.quantity - 1) },
                        enabled = item.quantity > 1 && !busy,
                        modifier = Modifier.size(34.dp).semantics { contentDescription = "Giảm số lượng" }
                    ) { Text("−", fontSize = 22.sp) }
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("${item.quantity}", Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        enabled = item.quantity < 99 && !busy,
                        modifier = Modifier.size(34.dp)
                    ) { Icon(Icons.Default.Add, "Tăng số lượng", Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CartEditDialog(
    item: CartItem,
    food: FoodDetail,
    onDismiss: () -> Unit,
    onSave: (CartItem, Int, List<String>, String) -> Unit
) {
    var quantity by rememberSaveable(item.id) { mutableIntStateOf(item.quantity) }
    var selected by rememberSaveable(item.id) { mutableStateOf(item.options.map { it.id }) }
    var note by rememberSaveable(item.id) { mutableStateOf(item.note) }
    val valid = food.groups.all { group -> !group.required || group.options.any { it.id in selected } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = CeramicSurface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sửa ${item.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    food.groups.forEach { group ->
                        Text(group.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        group.options.forEach { option ->
                            val checked = option.id in selected
                            // SINGLE thay lựa chọn cũ trong cùng nhóm; MULTIPLE bật/tắt độc lập.
                            val choose = {
                                selected = if (group.multiple) {
                                    if (checked) selected - option.id else selected + option.id
                                } else selected.filterNot { id -> group.options.any { it.id == id } } + option.id
                            }
                            Row(
                                Modifier.fillMaxWidth().then(
                                    if (group.multiple) Modifier.toggleable(checked, role = Role.Checkbox) { choose() }
                                    else Modifier.selectable(checked, role = Role.RadioButton) { choose() }
                                ).padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (group.multiple) Checkbox(checked, null) else RadioButton(checked, null)
                                Text(option.name, Modifier.weight(1f))
                                if (option.priceAdd > 0) Text("+${money(option.priceAdd)}", color = Brand)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(255) },
                        label = { Text("Ghi chú") },
                        supportingText = { Text("${note.length}/255") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        enabled = quantity > 1,
                        modifier = Modifier.semantics { contentDescription = "Giảm số lượng" }
                    ) {
                        Text("−", fontSize = 24.sp)
                    }
                    Text("$quantity", Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { if (quantity < 99) quantity++ }, enabled = quantity < 99) {
                        Icon(Icons.Default.Add, "Tăng số lượng")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Hủy") }
                    Button(onClick = { onSave(item, quantity, selected, note) }, enabled = valid) { Text("Lưu") }
                }
            }
        }
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Giỏ hàng đang trống", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("Hãy thêm món từ thực đơn.", color = OnSurfaceVariant)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Thử lại") }
    }
}

private fun money(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"

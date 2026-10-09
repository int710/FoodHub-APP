package com.example.foodhubapp.feature.cart.ui

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.cart.data.Cart
import com.example.foodhubapp.feature.cart.data.CartItem
import com.example.foodhubapp.feature.cart.data.CartType
import com.example.foodhubapp.feature.cart.data.CheckoutRequest
import com.example.foodhubapp.feature.cart.viewmodel.CartViewModel
import com.example.foodhubapp.feature.menu.ui.FoodDetail
import com.example.foodhubapp.feature.menu.ui.FoodImage
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.CardStroke
import com.example.foodhubapp.theme.CeramicSurface
import com.example.foodhubapp.theme.OnSurfaceVariant
import java.text.NumberFormat
import java.util.Locale

/**
 * Route có state: kết nối CartViewModel với UI, snackbar, dialog xác nhận và
 * điều hướng đăng nhập. CartScreen phía dưới chỉ nhận dữ liệu/callback.
 */
@Composable
fun CartRoute(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onOrderCreated: () -> Unit = {},
    viewModel: CartViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<CartItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.checkoutResult) {
        state.checkoutResult?.let { result ->
            result.paymentUrl?.let { runCatching { uriHandler.openUri(it) } }
            viewModel.consumeCheckoutResult()
            onOrderCreated()
        }
    }

    CartScreen(
        cart = state.cart,
        isLoading = state.isLoading,
        error = state.error,
        busyItemId = state.busyItemId,
        isClearing = state.isClearing,
        cartType = state.cartType,
        isCheckingOut = state.isCheckingOut,
        onBackClick = onBackClick,
        onRetry = viewModel::load,
        onQuantityChange = viewModel::changeQuantity,
        onEdit = viewModel::edit,
        onDelete = { deleteTarget = it },
        onClear = { confirmClear = true },
        onTypeChange = viewModel::selectType,
        onCheckout = { showCheckout = true },
        snackbarHost = { SnackbarHost(snackbar) }
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
    if (showCheckout) {
        CheckoutDialog(
            type = state.cartType,
            isSubmitting = state.isCheckingOut,
            onDismiss = { if (!state.isCheckingOut) showCheckout = false },
            onSubmit = {
                showCheckout = false
                viewModel.checkout(it)
            },
        )
    }
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
    cartType: CartType = CartType.TAKEAWAY,
    isCheckingOut: Boolean = false,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onQuantityChange: (CartItem, Int) -> Unit,
    onEdit: (CartItem) -> Unit,
    onDelete: (CartItem) -> Unit,
    onClear: () -> Unit,
    onTypeChange: (CartType) -> Unit = {},
    onCheckout: () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {}
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
                Text("Giỏ hàng", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 21.sp)
                if (cart.items.isNotEmpty()) TextButton(onClick = onClear, enabled = !isClearing && busyItemId == null) {
                    Text("Xóa tất cả", color = Brand)
                }
            }
        },
        bottomBar = {
            if (cart.items.isNotEmpty()) Surface(shadowElevation = 8.dp, color = CeramicSurface) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng cộng", color = OnSurfaceVariant)
                        Text(money(cart.totalAmount), color = Brand, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Button(
                        onClick = onCheckout,
                        enabled = !isCheckingOut && busyItemId == null && !isClearing,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    ) {
                        if (isCheckingOut) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text("Đặt món")
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(CartType.TAKEAWAY, CartType.DELIVERY, CartType.DINE_IN).forEach { type ->
                                FilterChip(
                                    selected = cartType == type,
                                    onClick = { onTypeChange(type) },
                                    label = { Text(type.label()) },
                                )
                            }
                        }
                    }
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
private fun CheckoutDialog(
    type: CartType,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CheckoutRequest) -> Unit,
) {
    var paymentMethod by rememberSaveable { mutableStateOf("CASH") }
    var note by rememberSaveable { mutableStateOf("") }
    var recipientName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    val deliveryValid = type != CartType.DELIVERY ||
        (recipientName.isNotBlank() && phone.isNotBlank() && address.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận ${type.label().lowercase()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Phương thức thanh toán", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(paymentMethod == "CASH", { paymentMethod = "CASH" }, { Text("Tiền mặt") })
                    FilterChip(paymentMethod == "VNPAY", { paymentMethod = "VNPAY" }, { Text("VNPay") })
                }
                if (type == CartType.DELIVERY) {
                    OutlinedTextField(recipientName, { recipientName = it }, label = { Text("Người nhận") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("Số điện thoại") }, singleLine = true)
                    OutlinedTextField(address, { address = it }, label = { Text("Địa chỉ giao hàng") }, minLines = 2)
                }
                OutlinedTextField(note, { note = it.take(255) }, label = { Text("Ghi chú đơn hàng") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(CheckoutRequest(type, paymentMethod, note, recipientName, phone, address))
                },
                enabled = deliveryValid && !isSubmitting,
            ) { Text("Tạo đơn") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Hủy") } },
    )
}

private fun CartType.label(): String = when (this) {
    CartType.DINE_IN -> "Tại bàn"
    CartType.TAKEAWAY -> "Mang về"
    CartType.DELIVERY -> "Giao hàng"
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

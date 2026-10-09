package com.example.foodhubapp.feature.home.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.cart.data.CartType
import com.example.foodhubapp.feature.home.viewmodel.HomeUiState
import com.example.foodhubapp.feature.home.viewmodel.HomeViewModel
import com.example.foodhubapp.feature.menu.data.MenuFood
import com.example.foodhubapp.feature.table.data.RestaurantTable
import com.example.foodhubapp.feature.table.data.RestaurantTableStatus
import com.example.foodhubapp.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeRoute(
    onFoodClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable = { label: String ->
        scope.launch { snackbar.showSnackbar("$label chưa khả dụng với API hiện tại.") }
        Unit
    }

    // Cập nhật lại badge cart và menu khi người dùng quay về từ màn chi tiết/giỏ.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    HomeScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCategoryClick = viewModel::selectCategory,
        onRetry = viewModel::load,
        onFoodClick = onFoodClick,
        onMenuClick = onMenuClick,
        onCartClick = onCartClick,
        onOrdersClick = onOrdersClick,
        onScanQrClick = onScanQrClick,
        onProfileClick = onProfileClick,
        onUnavailable = unavailable,
        onCartTypeChange = viewModel::selectCartType,
        snackbarHost = { SnackbarHost(snackbar) }
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onRetry: () -> Unit,
    onFoodClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUnavailable: (String) -> Unit,
    onScanQrClick: () -> Unit = {},
    onCartTypeChange: (CartType) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {}
) {
    var showContextPicker by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = AppBackground,
        snackbarHost = snackbarHost,
        topBar = { HomeHeader(state, onCartClick, onProfileClick, onScanQrClick) },
        bottomBar = { HomeBottomBar(onOrdersClick, onProfileClick, onUnavailable) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.categories.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Brand)
            } else if (state.error != null && state.categories.isEmpty()) {
                HomeError(state.error, onRetry, Modifier.align(Alignment.Center))
            } else {
                val visible = state.visibleFoods
                LazyColumn(
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().align(Alignment.TopCenter),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { OrderingContext(state, { showContextPicker = true }) }
                    item { SearchRow(state.query, onQueryChange, onUnavailable) }
                    item { DiscoveryBanner(onMenuClick) }
                    item { QuickActions(onMenuClick, onScanQrClick, onCartTypeChange, onUnavailable) }
                    item { TableMapSection(state.tables, state.tableError, state.tableSession?.tableId, onScanQrClick, onRetry) }
                    item {
                        SectionTitle("Danh Mục Món Ăn", "Tất cả (${state.categories.sumOf { it.items.size }})", onMenuClick)
                        Spacer(Modifier.height(10.dp))
                        CategoryRow(state, onCategoryClick)
                    }
                    if (state.query.isBlank() && state.selectedCategoryId == null && state.popularFoods.isNotEmpty()) {
                        item {
                            SectionTitle("Món Bán Chạy Hôm Nay", "Xem thêm", onMenuClick)
                            Spacer(Modifier.height(10.dp))
                            PopularRow(state.popularFoods, onFoodClick)
                        }
                    }
                    item {
                        val title = if (state.query.isNotBlank()) "Kết Quả Tìm Kiếm" else "Gợi Ý Dành Cho Bạn"
                        Text(title, color = Neutral, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    if (visible.isEmpty()) {
                        item { Text("Không tìm thấy món phù hợp.", color = OnSurfaceVariant) }
                    } else {
                        items(visible.take(8), key = { "home:${it.id}" }) { food ->
                            RecommendationCard(food, fallbackFor(food, visible), onFoodClick)
                        }
                    }
                    item { ServiceBar { onUnavailable("Gọi phục vụ") } }
                }
            }
        }
    }
    if (showContextPicker) OrderingContextDialog(
        currentType = state.cartType,
        isAuthenticated = state.isAuthenticated,
        onDismiss = { showContextPicker = false },
        onScanQr = {
            showContextPicker = false
            onScanQrClick()
        },
        onSelect = {
            showContextPicker = false
            onCartTypeChange(it)
        }
    )
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanQrClick: () -> Unit
) {
    Surface(color = AppBackground.copy(alpha = .96f), shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.foodhub_logo),
                    contentDescription = "FoodHub",
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("FoodHub", color = Brand, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val customerLabel = if (state.isAuthenticated) " • CUSTOMER" else ""
                    val sessionLabel = state.tableSession?.let { "${it.tableName} • DINE_IN$customerLabel" }
                        ?: "${state.userName?.ifBlank { null } ?: "Khách"}$customerLabel • ${state.cartType.name}"
                    Text(sessionLabel, color = OnSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HeaderButton("Quét mã QR", onScanQrClick) {
                Icon(Icons.Default.QrCodeScanner, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box {
                HeaderButton("Giỏ hàng", onCartClick) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(20.dp))
                }
                if (state.cartItemCount > 0) Badge(
                    Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp),
                    containerColor = Brand
                ) { Text(state.cartItemCount.coerceAtMost(99).toString()) }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onProfileClick, modifier = Modifier.size(44.dp)) {
                Image(
                    painterResource(R.drawable.home_figma_08),
                    "Tài khoản",
                    Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun HeaderButton(description: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = InputBackground) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp).semantics { contentDescription = description }
        ) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
        }
    }
}

@Composable
private fun OrderingContext(state: HomeUiState, onChangeContext: () -> Unit) {
    val session = state.tableSession
    val isDineIn = session != null
    val color = when (state.cartType) {
        CartType.DINE_IN -> Brand
        CartType.TAKEAWAY -> Color(0xFF282526)
        CartType.DELIVERY -> Color(0xFF008A62)
    }
    val title = when (state.cartType) {
        CartType.DINE_IN -> "Đang dùng món tại: ${session?.tableName ?: "Bàn đã quét"}"
        CartType.TAKEAWAY -> "Nhận tại quầy (Takeaway)"
        CartType.DELIVERY -> "Giao tận nơi (Delivery)"
    }
    val detail = when (state.cartType) {
        CartType.DINE_IN -> listOfNotNull(
            session?.floor,
            session?.capacity?.let { "$it ghế" },
            "Giỏ dùng chung theo bàn"
        ).joinToString(" • ")
        CartType.TAKEAWAY -> "Giỏ TAKEAWAY theo tài khoản • Nhận món tại quầy"
        CartType.DELIVERY -> "Giỏ DELIVERY theo tài khoản • Yêu cầu đăng nhập"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = .055f),
        border = BorderStroke(if (isDineIn) 1.5.dp else 1.dp, color.copy(alpha = if (isDineIn) .8f else .2f)),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(
                    if (isDineIn) Icons.Default.QrCodeScanner else if (state.cartType == CartType.DELIVERY) Icons.Default.DeliveryDining else Icons.Default.TakeoutDining,
                    null,
                    Modifier.size(21.dp),
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f, fill = false), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = CircleShape, color = color.copy(alpha = .12f)) {
                        Text(state.cartType.name, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(detail, color = OnSurfaceVariant, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onChangeContext, contentPadding = PaddingValues(horizontal = 7.dp)) {
                Text("Đổi", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrderingContextDialog(
    currentType: CartType,
    isAuthenticated: Boolean,
    onDismiss: () -> Unit,
    onScanQr: () -> Unit,
    onSelect: (CartType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn hình thức đặt món", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ContextChoice("Ăn tại bàn", "Quét QR để dùng giỏ chung theo bàn", Icons.Default.QrCodeScanner, Brand, currentType == CartType.DINE_IN, onScanQr)
                ContextChoice("Nhận tại quầy", "Giỏ TAKEAWAY theo tài khoản", Icons.Default.TakeoutDining, Color(0xFF282526), currentType == CartType.TAKEAWAY) { onSelect(CartType.TAKEAWAY) }
                ContextChoice(
                    "Giao tận nơi",
                    if (isAuthenticated) "Giỏ DELIVERY theo tài khoản" else "Cần đăng nhập trước khi đặt giao hàng",
                    Icons.Default.DeliveryDining,
                    Color(0xFF008A62),
                    currentType == CartType.DELIVERY
                ) { onSelect(CartType.DELIVERY) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun ContextChoice(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = .055f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, color.copy(alpha = if (selected) .8f else .18f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(color, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = OnSurfaceVariant, fontSize = 10.sp)
            }
            if (selected) Icon(Icons.Default.CheckCircle, "Đang chọn", tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit, onUnavailable: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            placeholder = {
                Text(
                    "Tìm món ngon, đồ uống, tráng miệng...",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, "Xóa tìm kiếm", Modifier.size(18.dp))
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Brand,
                unfocusedBorderColor = Color.Transparent
            )
        )
        FilledIconButton(
            onClick = { onUnavailable("Bộ lọc nâng cao") },
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryContainer)
        ) { Icon(Icons.Default.Tune, "Lọc món ăn", tint = Color.White) }
    }
}

@Composable
private fun DiscoveryBanner(onMenuClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PrimaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(Modifier.heightIn(min = 156.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = Warning) {
                    Text("THỰC ĐƠN HÔM NAY", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = WarningDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("Khám phá món ngon\ntừ FoodHub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp)
                Text("Danh mục và giá được cập nhật trực tiếp từ hệ thống.", color = Color.White.copy(alpha = .9f), fontSize = 11.sp, lineHeight = 15.sp)
                TextButton(onClick = onMenuClick, contentPadding = PaddingValues(0.dp)) {
                    Text("Xem thực đơn", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Image(
                painterResource(R.drawable.home_figma_02),
                "Burger FoodHub",
                Modifier.size(108.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun QuickActions(
    onMenuClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onCartTypeChange: (CartType) -> Unit,
    onUnavailable: (String) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 380.dp
        val actions = listOf<@Composable () -> Unit>(
            { QuickAction("Quét QR\nBàn", BrandSoft, onScanQrClick, Modifier.fillMaxWidth()) { Icon(Icons.Default.QrCodeScanner, null, tint = Brand) } },
            { QuickAction("Đặt Mang\nVề", WarmAccent, { onCartTypeChange(CartType.TAKEAWAY); onMenuClick() }, Modifier.fillMaxWidth()) { Icon(Icons.Default.TakeoutDining, null, tint = WarningDark) } },
            { QuickAction("Giao Tận\nNơi", SoftGreen, { onCartTypeChange(CartType.DELIVERY); onMenuClick() }, Modifier.fillMaxWidth()) { Icon(Icons.Default.DeliveryDining, null, tint = SuccessDark) } },
            { QuickAction("Mã Giảm\nGiá", Color(0xFFFFB59D), { onUnavailable("Mã giảm giá") }, Modifier.fillMaxWidth()) { Icon(Icons.Default.ConfirmationNumber, null, tint = Brand) } }
        )
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.chunked(2).forEach { rowActions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowActions.forEach { action -> Box(Modifier.weight(1f)) { action() } }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { action -> Box(Modifier.weight(1f)) { action() } }
            }
        }
    }
}

@Composable
private fun TableMapSection(
    tables: List<RestaurantTable>,
    error: String?,
    activeTableId: String?,
    onScanQrClick: () -> Unit,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Sơ Đồ Bàn", "Quét QR", onScanQrClick)
        when {
            error != null && tables.isEmpty() -> Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardStroke)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chưa tải được sơ đồ bàn.", Modifier.weight(1f), color = OnSurfaceVariant, fontSize = 12.sp)
                    TextButton(onClick = onRetry) { Text("Thử lại") }
                }
            }
            tables.isEmpty() -> Box(
                Modifier.fillMaxWidth().height(92.dp).background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(Modifier.size(24.dp), color = Brand, strokeWidth = 2.dp) }
            else -> {
                TableLegend(tables)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = when {
                        maxWidth < 600.dp -> 2
                        maxWidth < 780.dp -> 3
                        else -> 4
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tables.groupBy { it.floor ?: "Khu vực chung" }.forEach { (floor, floorTables) ->
                            Text(floor, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                floorTables.chunked(columns).forEach { rowTables ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        rowTables.forEach { table ->
                                            TableMapCard(table, table.id == activeTableId, onScanQrClick, Modifier.weight(1f))
                                        }
                                        repeat(columns - rowTables.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableLegend(tables: List<RestaurantTable>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TableLegendItem(Color(0xFF28A66A), "Trống ${tables.count { it.status == RestaurantTableStatus.AVAILABLE }}")
        TableLegendItem(Brand, "Có khách ${tables.count { it.status == RestaurantTableStatus.OCCUPIED }}")
        TableLegendItem(Color(0xFF8D9098), "Tạm ngưng ${tables.count { it.status == RestaurantTableStatus.INACTIVE }}")
    }
}

@Composable
private fun TableLegendItem(color: Color, label: String) {
    Surface(color = color.copy(alpha = .08f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color.copy(alpha = .18f))) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Text(label, color = OnSurfaceVariant, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun TableMapCard(
    table: RestaurantTable,
    isCurrent: Boolean,
    onScanQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (table.status) {
        RestaurantTableStatus.AVAILABLE -> Color(0xFF168F58) to "Bàn trống"
        RestaurantTableStatus.OCCUPIED -> Brand to "Có khách"
        RestaurantTableStatus.INACTIVE -> Color(0xFF777B84) to "Tạm ngưng"
    }
    var showDetails by remember(table.id) { mutableStateOf(false) }
    val cardColor = if (isCurrent) Brand else color
    val cardLabel = if (isCurrent) "Đang hoạt động" else label
    Box(modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .pointerInput(table.id) {
                    awaitPointerEventScope {
                        while (true) {
                            if (awaitPointerEvent().type == PointerEventType.Enter) showDetails = true
                        }
                    }
                }
                .clickable { showDetails = true }
                .semantics { contentDescription = "${table.name}, $cardLabel, ${table.capacity} chỗ" },
            shape = RoundedCornerShape(12.dp),
            color = cardColor.copy(alpha = if (isCurrent) .065f else .035f),
            border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, cardColor.copy(alpha = if (isCurrent) .9f else .22f))
        ) {
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        table.name,
                        Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Surface(shape = RoundedCornerShape(5.dp), color = Color(0xFFECEBEA)) {
                        Text("${table.capacity} ghế", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = OnSurfaceVariant, fontSize = 9.sp)
                    }
                }
                Text(
                    listOfNotNull(table.floor, table.note).joinToString(" - ").ifBlank { "Khu vực chung" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurfaceVariant,
                    fontSize = 10.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(6.dp).background(cardColor, CircleShape))
                    Text(cardLabel, color = cardColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        DropdownMenu(
            expanded = showDetails,
            onDismissRequest = { showDetails = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.widthIn(min = 176.dp).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableRestaurant, null, Modifier.size(20.dp), tint = color)
                    Spacer(Modifier.width(8.dp))
                    Text(table.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(cardColor, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(cardLabel, color = cardColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Sức chứa: ${table.capacity} chỗ", color = OnSurfaceVariant, fontSize = 11.sp)
                if (table.status == RestaurantTableStatus.AVAILABLE && !isCurrent) {
                    TextButton(
                        onClick = {
                            showDetails = false
                            onScanQrClick()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Quét QR")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryRow(state: HomeUiState, onCategoryClick: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = state.selectedCategoryId == null,
                onClick = { onCategoryClick(null) },
                label = { Text("Tất cả") },
                leadingIcon = { Icon(Icons.Default.Restaurant, null, Modifier.size(16.dp)) },
                colors = homeFilterChipColors()
            )
        }
        items(state.categories, key = { it.id }) { category ->
            FilterChip(
                selected = state.selectedCategoryId == category.id,
                onClick = { onCategoryClick(category.id) },
                label = { Text(category.name, maxLines = 1) },
                colors = homeFilterChipColors()
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(Brand, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(title, Modifier.weight(1f), color = Neutral, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(action, Modifier.clickable(onClick = onAction), color = Brand, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PopularRow(foods: List<MenuFood>, onFoodClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(foods, key = { "popular:${it.id}" }) { food ->
            Surface(
                modifier = Modifier.width(174.dp).clickable { onFoodClick(food.id) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column {
                    HomeFoodImage(food.imageUrl, fallbackFor(food, foods), Modifier.fillMaxWidth().height(116.dp))
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(food.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                        if (food.rating != "0") Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = Warning)
                            Text(food.rating, fontSize = 11.sp, color = OnSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(money(food.price), Modifier.weight(1f), color = Brand, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Surface(shape = CircleShape, color = BrandSoft) {
                                Icon(Icons.Default.Add, "Xem ${food.name}", Modifier.padding(7.dp).size(16.dp), tint = Brand)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(food: MenuFood, @DrawableRes fallback: Int, onFoodClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onFoodClick(food.id) },
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeFoodImage(food.imageUrl, fallback, Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f).height(96.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(food.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (food.description.isNotBlank()) Text(
                        food.description,
                        color = OnSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(money(food.price), Modifier.weight(1f), color = Brand, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Surface(shape = CircleShape, color = BrandSoft) {
                        Icon(Icons.Default.Add, "Xem ${food.name}", Modifier.padding(7.dp).size(16.dp), tint = Brand)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFoodImage(url: String?, @DrawableRes fallback: Int, modifier: Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.background(InputBackground),
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) } },
        error = { Image(painterResource(fallback), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    )
}

@Composable
private fun ServiceBar(onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = InputBackground) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(WarmAccent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.RoomService, null, Modifier.size(18.dp), tint = WarningDark)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Cần trợ giúp tại bàn?", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Tính năng sẽ mở khi có phiên QR bàn", color = OnSurfaceVariant, fontSize = 10.sp)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Warning, contentColor = WarningDark)) {
                Text("Gọi phục vụ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeBottomBar(onOrdersClick: () -> Unit, onProfileClick: () -> Unit, onUnavailable: (String) -> Unit) {
    Surface(color = AppBackground.copy(alpha = .97f), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 8.dp)) {
            BottomDestination(Icons.Default.Home, "Trang chủ", true, {})
            BottomDestination(Icons.AutoMirrored.Filled.ReceiptLong, "Đơn hàng", false, onOrdersClick)
            BottomDestination(Icons.Default.NotificationsNone, "Thông báo", false) { onUnavailable("Thông báo") }
            BottomDestination(Icons.Default.Person, "Cá nhân", false, onProfileClick)
        }
    }
}

@Composable
private fun RowScope.BottomDestination(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun HomeError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Thử lại") }
    }
}

@DrawableRes
private fun fallbackFor(food: MenuFood, foods: List<MenuFood>): Int {
    val index = foods.indexOfFirst { it.id == food.id }.coerceAtLeast(0)
    return listOf(
        R.drawable.home_figma_01,
        R.drawable.home_figma_04,
        R.drawable.home_figma_05,
        R.drawable.home_figma_06,
        R.drawable.home_figma_07
    )[index % 5]
}

private fun money(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"

@Composable
private fun homeFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.White,
    labelColor = Neutral,
    selectedContainerColor = Brand,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White
)

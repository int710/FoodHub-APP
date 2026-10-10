package com.example.foodhubapp.feature.customer.home.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import coil.compose.SubcomposeAsyncImage
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.customer.home.viewmodel.HomeUiState
import com.example.foodhubapp.feature.customer.home.viewmodel.HomeViewModel
import com.example.foodhubapp.feature.customer.menu.data.MenuFood
import com.example.foodhubapp.feature.shared.notification.viewmodel.NotificationViewModel
import com.example.foodhubapp.feature.customer.cart.data.CartType
import com.example.foodhubapp.feature.customer.order.data.OrderingContextStore
import com.example.foodhubapp.feature.customer.table.data.TableSessionStore
import com.example.foodhubapp.feature.customer.table.data.TableSession
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.InputBackground
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.PrimaryContainer
import com.example.foodhubapp.theme.SoftGreen
import com.example.foodhubapp.theme.SuccessDark
import com.example.foodhubapp.theme.SuccessSoft
import com.example.foodhubapp.theme.WarmAccent
import com.example.foodhubapp.theme.Warning
import com.example.foodhubapp.theme.WarningDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeRoute(
    onFoodClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onQrClick: () -> Unit,
    onChatClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { com.example.foodhubapp.core.datastore.TokenStore(context.applicationContext) }
    val tableSessionStore = remember(context) { TableSessionStore(context.applicationContext) }
    val orderingContextStore = remember(context) { OrderingContextStore(context.applicationContext) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    var tableSession by remember { mutableStateOf(tableSessionStore.current()) }
    var selectedOrderType by remember { mutableStateOf(orderingContextStore.currentType()) }
    val scope = rememberCoroutineScope()

    // Cập nhật lại badge cart và menu khi người dùng quay về từ màn chi tiết/giỏ.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
        tableSession = tableSessionStore.current()
        scope.launch {
            orderingContextStore.selectDefault(!tokenStore.getAccessToken().isNullOrBlank())
            selectedOrderType = orderingContextStore.currentType()
            if (!tokenStore.getAccessToken().isNullOrBlank()) {
                notificationViewModel.refresh()
            }
        }
    }

    HomeScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onMaxPriceChange = viewModel::selectMaxPrice,
        onCategoryClick = viewModel::selectCategory,
        onRetry = viewModel::load,
        onFoodClick = onFoodClick,
        onMenuClick = onMenuClick,
        onCartClick = onCartClick,
        onOrdersClick = onOrdersClick,
        onProfileClick = onProfileClick,
        onQrClick = onQrClick,
        onDeliveryClick = {
            if (state.isLoggedIn) {
                orderingContextStore.select(CartType.DELIVERY)
                selectedOrderType = CartType.DELIVERY
                onMenuClick()
            } else {
                onProfileClick()
            }
        },
        tableSession = tableSession,
        selectedOrderType = selectedOrderType,
        onOrderTypeSelected = { type ->
            if (type == CartType.DINE_IN && tableSession == null) {
                onQrClick()
            } else {
                orderingContextStore.select(type)
                selectedOrderType = type
            }
        },
        onChatClick = onChatClick,
        onNotificationsClick = onNotificationsClick,
        notificationUnreadCount = notificationState.unreadCount,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onMaxPriceChange: (Long?) -> Unit = {},
    onCategoryClick: (String?) -> Unit,
    onRetry: () -> Unit,
    onFoodClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onQrClick: () -> Unit,
    onDeliveryClick: () -> Unit = {},
    tableSession: TableSession? = null,
    selectedOrderType: CartType = CartType.TAKEAWAY,
    onOrderTypeSelected: (CartType) -> Unit = {},
    onChatClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    notificationUnreadCount: Int = 0,
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = { HomeHeader(state.cartItemCount, onCartClick, onProfileClick, onQrClick, tableSession, selectedOrderType) },
        bottomBar = {
            HomeBottomBar(
                onOrdersClick = onOrdersClick,
                onProfileClick = onProfileClick,
                hasTableSession = tableSession != null,
                onChatClick = onChatClick,
                onNotificationsClick = onNotificationsClick,
                notificationUnreadCount = notificationUnreadCount,
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.categories.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Brand)
            } else if (state.error != null && state.categories.isEmpty()) {
                HomeError(state.error, onRetry, Modifier.align(Alignment.Center))
            } else {
                val visible = state.visibleFoods
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OrderingContext(
                            selectedType = selectedOrderType,
                            tableSession = tableSession,
                            isLoggedIn = state.isLoggedIn,
                            onSelect = onOrderTypeSelected,
                        )
                    }
                    item { SearchRow(state.query, onQueryChange, state.maxPrice, onMaxPriceChange) }
                    item { DiscoveryBanner(onMenuClick) }
                    item { QuickActions(onMenuClick, onQrClick, onDeliveryClick) }
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
                    item {
                        ServiceBar(tableSession != null, onChatClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    cartCount: Int,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onQrClick: () -> Unit,
    tableSession: TableSession?,
    selectedType: CartType,
) {
    Surface(color = AppBackground.copy(alpha = .96f), shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("FoodHub", color = Brand, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    tableSession?.let { "${it.tableName}${it.floor?.let { floor -> " · $floor" }.orEmpty()}" }
                        ?: when (selectedType) {
                            CartType.DELIVERY -> "Giao tận nơi"
                            CartType.TAKEAWAY -> "Khách mang về"
                            CartType.DINE_IN -> "Dùng tại bàn"
                        },
                    color = OnSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            HeaderButton("Quét mã QR", onQrClick) {
                Icon(Icons.Default.QrCodeScanner, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box {
                HeaderButton("Giỏ hàng", onCartClick) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(20.dp))
                }
                if (cartCount > 0) Badge(
                    Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp),
                    containerColor = Brand
                ) { Text(cartCount.coerceAtMost(99).toString()) }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onProfileClick, modifier = Modifier.size(44.dp)) {
                Image(
                    painterResource(R.drawable.avatar_user),
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
private fun OrderingContext(
    selectedType: CartType,
    tableSession: TableSession?,
    isLoggedIn: Boolean,
    onSelect: (CartType) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val title = when (selectedType) {
        CartType.DINE_IN -> "Ăn tại ${tableSession?.tableName ?: "bàn"}"
        CartType.TAKEAWAY -> "Đặt món mang về"
        CartType.DELIVERY -> "Giao tận nơi"
    }
    val detail = when (selectedType) {
        CartType.DINE_IN -> listOfNotNull(tableSession?.floor, tableSession?.capacity?.let { "$it chỗ" }).joinToString(" · ").ifBlank { "Phiên bàn đang hoạt động" }
        CartType.TAKEAWAY -> if (isLoggedIn) "Nhận tại quầy" else "Khách vãng lai · Không cần đăng nhập"
        CartType.DELIVERY -> "Giao đến địa chỉ của bạn"
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().clickable { showDialog = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(SuccessSoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    when (selectedType) {
                        CartType.DINE_IN -> Icons.Default.TableRestaurant
                        CartType.DELIVERY -> Icons.Default.DeliveryDining
                        CartType.TAKEAWAY -> Icons.Default.TakeoutDining
                    }, null, Modifier.size(18.dp), tint = SuccessDark,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(detail, color = OnSurfaceVariant, fontSize = 11.sp, maxLines = 1)
            }
            Surface(shape = CircleShape, color = SuccessSoft) {
                Text("Thay đổi", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SuccessDark, fontSize = 10.sp)
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Column { Text("KHỞI TẠO ĐẶT MÓN", color = Brand, fontSize = 11.sp); Text("Bạn muốn nhận món theo cách nào?", fontWeight = FontWeight.Bold) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrderModeChoice("Ăn tại bàn (Quét mã QR)", tableSession?.let { "Đang kết nối: ${it.tableName}${it.floor?.let { floor -> " ($floor)" }.orEmpty()}" } ?: "Quét QR trên bàn để bắt đầu", selectedType == CartType.DINE_IN) {
                        showDialog = false; onSelect(CartType.DINE_IN)
                    }
                    OrderModeChoice("Nhận tại quầy (Takeaway)", "Cho phép khách vãng lai, không bắt buộc đăng nhập", selectedType == CartType.TAKEAWAY) {
                        showDialog = false; onSelect(CartType.TAKEAWAY)
                    }
                    OrderModeChoice("Giao tận nơi (Delivery)", if (isLoggedIn) "Giao tới địa chỉ của bạn" else "Yêu cầu đăng nhập", selectedType == CartType.DELIVERY, enabled = isLoggedIn) {
                        showDialog = false; onSelect(CartType.DELIVERY)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Đóng") } },
        )
    }
}

@Composable
private fun OrderModeChoice(
    title: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) BrandSoft.copy(alpha = .45f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Brand else Color(0xFFE4E0DE)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (enabled) Neutral else OnSurfaceVariant)
            Text(detail, fontSize = 11.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    maxPrice: Long?,
    onMaxPriceChange: (Long?) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
        Box {
            FilledIconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (maxPrice == null) PrimaryContainer else Brand
                )
            ) { Icon(Icons.Default.Tune, "Lọc theo giá", tint = Color.White) }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                listOf(
                    "Tất cả mức giá" to null,
                    "Tối đa 50.000đ" to 50_000L,
                    "Tối đa 100.000đ" to 100_000L,
                    "Tối đa 200.000đ" to 200_000L,
                ).forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onMaxPriceChange(value); menuExpanded = false },
                        leadingIcon = {
                            if (maxPrice == value) Icon(Icons.Default.Check, null)
                        },
                    )
                }
            }
        }
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
    onQrClick: () -> Unit,
    onDeliveryClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction("Quét QR\nBàn", BrandSoft, onQrClick, Modifier.weight(1f)) {
            Icon(Icons.Default.QrCodeScanner, null, tint = Brand)
        }
        QuickAction("Đặt Mang\nVề", WarmAccent, onMenuClick, Modifier.weight(1f)) {
            Icon(Icons.Default.TakeoutDining, null, tint = WarningDark)
        }
        QuickAction("Giao Tận\nNơi", SoftGreen, onDeliveryClick, Modifier.weight(1f)) {
            Icon(Icons.Default.DeliveryDining, null, tint = SuccessDark)
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
                modifier = Modifier.width(174.dp).clickable(enabled = food.available) { onFoodClick(food.id) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column {
                    AvailabilityImage(food, fallbackFor(food, foods), Modifier.fillMaxWidth().height(116.dp))
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(food.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                        if (food.rating != "0") Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = Warning)
                            Text(food.rating, fontSize = 11.sp, color = OnSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(money(food.price), Modifier.weight(1f), color = Brand, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (food.available) Surface(shape = CircleShape, color = BrandSoft) {
                                Icon(Icons.Default.Add, "Xem ${food.name}", Modifier.padding(7.dp).size(16.dp), tint = Brand)
                            } else Text("Tạm hết", color = Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        modifier = Modifier.fillMaxWidth().clickable(enabled = food.available) { onFoodClick(food.id) },
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvailabilityImage(food, fallback, Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)))
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
                    if (food.available) Surface(shape = CircleShape, color = BrandSoft) {
                        Icon(Icons.Default.Add, "Xem ${food.name}", Modifier.padding(7.dp).size(16.dp), tint = Brand)
                    } else Text("Tạm hết hàng", color = Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AvailabilityImage(food: MenuFood, @DrawableRes fallback: Int, modifier: Modifier) {
    Box(modifier) {
        HomeFoodImage(food.imageUrl, fallback, Modifier.fillMaxSize().alpha(if (food.available) 1f else .42f))
        if (!food.available) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = .72f),
                shape = RoundedCornerShape(8.dp),
            ) { Text("TẠM HẾT", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
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
private fun ServiceBar(hasTableSession: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = InputBackground) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(WarmAccent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.RoomService, null, Modifier.size(18.dp), tint = WarningDark)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Cần trợ giúp tại bàn?", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    if (hasTableSession) "Nhắn tin trực tiếp với nhân viên" else "Tính năng sẽ mở khi có phiên QR bàn",
                    color = OnSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            Button(
                onClick = onClick,
                enabled = hasTableSession,
                colors = ButtonDefaults.buttonColors(containerColor = Warning, contentColor = WarningDark),
            ) {
                Text("Gọi phục vụ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    hasTableSession: Boolean,
    onChatClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    notificationUnreadCount: Int,
) {
    Surface(color = AppBackground.copy(alpha = .97f), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 8.dp)) {
            BottomDestination(Icons.Default.Home, "Trang chủ", true, {})
            BottomDestination(Icons.AutoMirrored.Filled.ReceiptLong, "Đơn hàng", false, onOrdersClick)
            if (hasTableSession) {
                BottomDestination(Icons.Default.ChatBubbleOutline, "Tin nhắn", false, onChatClick)
            }
            BottomDestination(
                Icons.Default.NotificationsNone,
                "Thông báo",
                false,
                onNotificationsClick,
                notificationUnreadCount,
            )
            BottomDestination(Icons.Default.Person, "Cá nhân", false, onProfileClick)
        }
    }
}

@Composable
private fun RowScope.BottomDestination(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    Column(
        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (badgeCount > 0) {
            BadgedBox(badge = { Badge { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) } }) {
                Icon(icon, label, Modifier.size(20.dp), tint = if (selected) Brand else OnSurfaceVariant)
            }
        } else {
            Icon(icon, label, Modifier.size(20.dp), tint = if (selected) Brand else OnSurfaceVariant)
        }
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

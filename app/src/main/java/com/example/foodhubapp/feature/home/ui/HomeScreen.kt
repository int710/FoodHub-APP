package com.example.foodhubapp.feature.home.ui

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
import androidx.compose.ui.graphics.Color
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
import com.example.foodhubapp.feature.home.viewmodel.HomeUiState
import com.example.foodhubapp.feature.home.viewmodel.HomeViewModel
import com.example.foodhubapp.feature.menu.data.MenuFood
import com.example.foodhubapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeRoute(
    onFoodClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
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
        onProfileClick = onProfileClick,
        onUnavailable = unavailable,
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
    snackbarHost: @Composable () -> Unit = {}
) {
    Scaffold(
        containerColor = AppBackground,
        snackbarHost = snackbarHost,
        topBar = { HomeHeader(state.cartItemCount, onCartClick, onProfileClick, onUnavailable) },
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { OrderingContext() }
                    item { SearchRow(state.query, onQueryChange, onUnavailable) }
                    item { DiscoveryBanner(onMenuClick) }
                    item { QuickActions(onMenuClick, onUnavailable) }
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
}

@Composable
private fun HomeHeader(
    cartCount: Int,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUnavailable: (String) -> Unit
) {
    Surface(color = AppBackground.copy(alpha = .96f), shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("FoodHub", color = Brand, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Trang Chủ", color = OnSurfaceVariant, fontSize = 10.sp)
            }
            HeaderButton("Quét mã QR", { onUnavailable("Quét QR") }) {
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
private fun OrderingContext() {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(SuccessSoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TakeoutDining, null, Modifier.size(18.dp), tint = SuccessDark)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Đặt món mang về", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Giỏ TAKEAWAY • Sẵn sàng chọn món", color = OnSurfaceVariant, fontSize = 11.sp)
            }
            Surface(shape = CircleShape, color = SuccessSoft) {
                Text("Đang hoạt động", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SuccessDark, fontSize = 10.sp)
            }
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
private fun QuickActions(onMenuClick: () -> Unit, onUnavailable: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction("Quét QR\nBàn", BrandSoft, { onUnavailable("Quét QR bàn") }, Modifier.weight(1f)) {
            Icon(Icons.Default.QrCodeScanner, null, tint = Brand)
        }
        QuickAction("Đặt Mang\nVề", WarmAccent, onMenuClick, Modifier.weight(1f)) {
            Icon(Icons.Default.TakeoutDining, null, tint = WarningDark)
        }
        QuickAction("Giao Tận\nNơi", SoftGreen, { onUnavailable("Giao tận nơi") }, Modifier.weight(1f)) {
            Icon(Icons.Default.DeliveryDining, null, tint = SuccessDark)
        }
        QuickAction("Mã Giảm\nGiá", Color(0xFFFFB59D), { onUnavailable("Mã giảm giá") }, Modifier.weight(1f)) {
            Icon(Icons.Default.ConfirmationNumber, null, tint = Brand)
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

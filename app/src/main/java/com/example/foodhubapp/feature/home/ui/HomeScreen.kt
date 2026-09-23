package com.example.foodhubapp.feature.home.ui

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.home.viewmodel.HomeUiState
import com.example.foodhubapp.feature.home.viewmodel.HomeViewModel
import com.example.foodhubapp.feature.menu.model.MenuCategoryUiModel
import com.example.foodhubapp.feature.menu.model.MenuItemUiModel
import com.example.foodhubapp.ui.theme.AppBackground
import com.example.foodhubapp.ui.theme.BodyFont
import com.example.foodhubapp.ui.theme.Brand
import com.example.foodhubapp.ui.theme.BrandSoft
import com.example.foodhubapp.ui.theme.CardStroke
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import com.example.foodhubapp.ui.theme.HeadingFont
import com.example.foodhubapp.ui.theme.InputBackground
import com.example.foodhubapp.ui.theme.Neutral
import com.example.foodhubapp.ui.theme.OnSurfaceVariant
import com.example.foodhubapp.ui.theme.PrimaryContainer
import com.example.foodhubapp.ui.theme.Success
import com.example.foodhubapp.ui.theme.SuccessDark
import com.example.foodhubapp.ui.theme.SuccessSoft
import com.example.foodhubapp.ui.theme.WarmAccent
import com.example.foodhubapp.ui.theme.Warning
import com.example.foodhubapp.ui.theme.WarningDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private data class QuickAction(
    val titleTop: String,
    val titleBottom: String,
    @DrawableRes val icon: Int,
    val background: Color
)

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    // Route là lớp nối ViewModel với UI thuần Compose.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onCategoryClick = viewModel::selectCategory,
        onRetryClick = viewModel::loadHomeMenu,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCategoryClick: (String?) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Nội dung chính scroll được; header và bottom nav được ghim bằng Box alignment.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 76.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TableContextCard()
            SearchFilterRow()
            PromoBanner()
            QuickActionRow()
            uiState.errorMessage?.let {
                HomeErrorCard(message = it, onRetryClick = onRetryClick)
            }
            // Các section dưới đây lấy dữ liệu thật từ /menu/all thông qua HomeViewModel.
            CategorySection(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategoryClick = onCategoryClick
            )
            FeaturedSection(items = uiState.bestSellers)
            SuggestionSection(items = uiState.suggestions)
            TableAssistBar()
        }

        HomeHeader(
            modifier = Modifier.align(Alignment.TopCenter)
        )
        BottomNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppBackground.copy(alpha = 0.92f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(Brand)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FoodHub",
                            color = Brand,
                            fontFamily = HeadingFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Warning.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Bàn 08",
                                color = WarningDark,
                                fontFamily = BodyFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Trang Chủ",
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCircleButton(iconRes = R.drawable.ic_onboarding_qr)
                CartHeaderButton()
                Image(
                    painter = painterResource(id = R.drawable.profile_avatar_thu_ha),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(@DrawableRes iconRes: Int) {
    Surface(
        color = InputBackground,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                colorFilter = ColorFilter.tint(Neutral)
            )
        }
    }
}

@Composable
private fun CartHeaderButton() {
    Box {
        HeaderCircleButton(iconRes = R.drawable.ic_onboarding_card)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 5.dp),
            color = Brand,
            shape = CircleShape
        ) {
            Text(
                text = "3",
                color = Color.White,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun TableContextCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIconBubble()
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bàn số 05",
                            color = Neutral,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        SmallStatusPill(text = "Ăn tại chỗ")
                    }
                    Text(
                        text = "Khu vực tầng 1 • Sẵn sàng phục vụ",
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                color = InputBackground,
                shape = CircleShape
            ) {
                Text(
                    text = "Đổi bàn",
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusIconBubble() {
    Box {
        Surface(
            color = SuccessSoft,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_bowl),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(Success)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(10.dp)
                .background(SuccessDark, CircleShape)
        )
    }
}

@Composable
private fun SmallStatusPill(text: String) {
    Surface(
        color = SuccessSoft,
        shape = CircleShape
    ) {
        Text(
            text = text,
            color = SuccessDark,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SearchFilterRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⌕", color = OnSurfaceVariant, fontSize = 18.sp)
                Text(
                    text = "Tìm món ngon, đồ uống, tráng miệng...",
                    modifier = Modifier.weight(1f),
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = BodyFont,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_auth_camera),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(OnSurfaceVariant)
                )
            }
        }
        Surface(
            color = PrimaryContainer,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {

        }
    }
}

@Composable
private fun PromoBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(Warning.copy(alpha = 0.32f), PrimaryContainer),
                        radius = 520f
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Warning,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "GIỜ VÀNG ƯU ĐÃI",
                            color = WarningDark,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Giảm 30% khi quét\nVNPay-QR",
                        color = Color.White,
                        fontFamily = HeadingFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 25.sp
                    )
                    Text(
                        text = "Áp dụng tối đa 60k cho mọi đơn ăn\ntại bàn từ 150k.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 1.dp
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_qrcode),
                            contentDescription = null
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.home_food_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow() {
    val actions = listOf(
        QuickAction("Quét QR", "Bàn", R.drawable.ic_qrcode, BrandSoft),
        QuickAction("Đặt Mang", "Về", R.drawable.ic_pot, WarmAccent),
        QuickAction("Giao Tận", "Nơi", R.drawable.ic__moto, Color(0xFF6FFBBE)),
        QuickAction("Mã Giảm", "Giá", R.drawable.ic_coupon, Color(0xFFFFB59D))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            Surface(
                modifier = Modifier.weight(1f),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = action.background,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = action.icon),
                                contentDescription = null,
                                tint = Brand
                            )
                        }
                    }
                    Text(
                        text = "${action.titleTop}\n${action.titleBottom}",
                        color = Neutral,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    categories: List<MenuCategoryUiModel>,
    selectedCategoryId: String?,
    onCategoryClick: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val itemsCount = categories.sumOf { it.items.size }

        SectionHeader(title = "Danh Mục Món Ăn", action = "Tất cả ($itemsCount)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                text = "Tất cả",
                selected = selectedCategoryId == null,
                onClick = { onCategoryClick(null) }
            )
            categories.forEach { category ->
                CategoryChip(
                    text = category.name,
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = if (selected) Brand else Color.White,
        shape = CircleShape,
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Neutral,
            fontFamily = BodyFont,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun FeaturedSection(items: List<MenuItemUiModel>) {
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Món Bán Chạy Hôm Nay", action = "Xem thêm")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.take(2).forEachIndexed { index, dish ->
                FeaturedDishCard(
                    dish = dish,
                    label = if (index == 0) "Bán chạy #1" else "Bán chạy",
                    fallbackImageRes = if (index == 0) {
                        R.drawable.home_food_burger
                    } else {
                        R.drawable.home_food_pizza
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FeaturedDishCard(
    dish: MenuItemUiModel,
    label: String,
    @DrawableRes fallbackImageRes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column {
            Box {
                RemoteMenuImage(
                    imageUrl = dish.imageUrl,
                    contentDescription = dish.name,
                    fallbackImageRes = fallbackImageRes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.padding(8.dp),
                    color = Brand,
                    shape = CircleShape
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dish.name,
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${dish.avgRating.formatRating()} • ${dish.totalOrder} lượt đặt",
                    color = OnSurfaceVariant,
                    fontFamily = BodyFont,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dish.displayPrice(),
                        color = Brand,
                        fontFamily = HeadingFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Surface(
                        color = Brand,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "+ Thêm",
                            color = Color.White,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionSection(items: List<MenuItemUiModel>) {
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Gợi Ý Riêng Cho Bàn 05",
                color = Neutral,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Phù hợp nhóm 2-3 người",
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.take(3).forEachIndexed { index, dish ->
                SuggestionDishCard(
                    dish = dish,
                    fallbackImageRes = when (index) {
                        0 -> R.drawable.home_food_salmon
                        1 -> R.drawable.home_food_spring_roll
                        else -> R.drawable.home_food_tiramisu
                    }
                )
            }
        }
    }
}

@Composable
private fun SuggestionDishCard(
    dish: MenuItemUiModel,
    @DrawableRes fallbackImageRes: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteMenuImage(
                imageUrl = dish.imageUrl,
                contentDescription = dish.name,
                fallbackImageRes = fallbackImageRes,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = dish.name,
                        color = Neutral,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dish.description ?: "Được nhiều khách lựa chọn hôm nay",
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = dish.displayPrice(),
                    color = Brand,
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Surface(
                color = BrandSoft,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Brand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TableAssistBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = InputBackground,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = WarmAccent,
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "!", color = WarningDark, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(
                        text = "Cần trợ giúp tại bàn?",
                        color = Neutral,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Nhân viên sẽ có mặt sau 1 phút",
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Surface(
                color = Warning,
                shape = CircleShape
            ) {
                Text(
                    text = "Gọi phục vụ",
                    color = WarningDark,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppBackground.copy(alpha = 0.96f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(R.drawable.ic_onboarding_logo_mark, label = "Trang chủ", selected = true)
            BottomNavItem(R.drawable.ic_list, label = "Đơn hàng", showDot = true)
            BottomNavItem(R.drawable.ic_message, label = "Tin nhắn")
            BottomNavItem(R.drawable.ic_profile, label = "Cá nhân")
        }
    }
}

@Composable
private fun BottomNavItem(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean = false,
    showDot: Boolean = false
) {
    Box {
        Column(
            modifier = Modifier
                .widthIn(min = 56.dp)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                painterResource(id = icon),
                contentDescription = null,
                tint = Brand
            )
            Text(
                text = label,
                color = if (selected) Brand else OnSurfaceVariant,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .background(SuccessDark, CircleShape)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Neutral,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp
        )
        Text(
            text = action,
            color = Brand,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun HomeErrorCard(
    message: String,
    onRetryClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BrandSoft,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB4A3)),
        onClick = onRetryClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Không tải được thực đơn",
                color = Brand,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Text(
                text = message,
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RemoteMenuImage(
    imageUrl: String?,
    contentDescription: String?,
    @DrawableRes fallbackImageRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageBitmap by remember(imageUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    // Không thêm thư viện ảnh mới ở bước này; tải ảnh URL trên IO và fallback về ảnh local nếu lỗi.
    LaunchedEffect(imageUrl) {
        imageBitmap = null
        if (!imageUrl.isNullOrBlank()) {
            imageBitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(imageUrl).openStream().use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Image(
            painter = painterResource(id = fallbackImageRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

private fun MenuItemUiModel.displayPrice(): String {
    val price = salePrice ?: basePrice
    return "${price.toInt().toVietnameseNumber()}đ"
}

private fun Double.formatRating(): String {
    return if (this == 0.0) {
        "Mới"
    } else {
        String.format("%.1f", this)
    }
}

private fun Int.toVietnameseNumber(): String {
    return "%,d".format(this).replace(',', '.')
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    FoodHubAppTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onCategoryClick = {},
            onRetryClick = {}
        )
    }
}

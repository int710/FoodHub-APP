package com.kma.foodhub.feature.customer.menu.ui

import com.kma.foodhub.core.ui.icons.TablerIcons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kma.foodhub.feature.customer.menu.viewmodel.MenuViewModel
import com.kma.foodhub.theme.AppBackground
import com.kma.foodhub.theme.Brand
import com.kma.foodhub.theme.CardStroke
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MenuRoute(
    onFoodClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: MenuViewModel = viewModel(),
    onCartClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(containerColor = AppBackground) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier
            .fillMaxHeight()
            .widthIn(max = 840.dp)
            .fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        TablerIcons.ArrowBack,
                        "Quay lại")
                }
                Text(
                    "Thực đơn",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onCartClick) {
                    Icon(TablerIcons.ShoppingCart, "Mở giỏ hàng")
                }
            }
            Box(Modifier
                .weight(1f)
                .fillMaxWidth()) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Brand)

                    state.error != null -> FoodLoadError(
                        state.error!!,
                        viewModel::load,
                        Modifier.align(Alignment.Center))

                    state.categories.all { it.items.isEmpty() } -> Text(
                        "Chưa có món ăn.",
                        Modifier.align(Alignment.Center))

                    else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        state.categories.forEach { category ->
                            item(key = "category:${category.id}") {
                                Text(
                                    category.name,
                                    Modifier.padding(vertical = 12.dp),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium)
                            }
                            items(category.items, key = { "${category.id}:${it.id}" }) { food ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = food.available) { onFoodClick(food.id) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(80.dp)) {
                                        FoodImage(food.imageUrl, food.name, Modifier.fillMaxSize().alpha(if (food.available) 1f else .38f))
                                        Row(
                                            Modifier.align(Alignment.TopEnd).padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            if (food.isBestSeller) Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFFFFC66D)) {
                                                Icon(TablerIcons.Flame, "Món bán chạy", Modifier.padding(4.dp).size(13.dp), tint = Color(0xFF7A4B00))
                                            }
                                            if (food.salePercent != null) Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Brand) {
                                                Icon(TablerIcons.RosetteDiscount, "Món đang giảm giá", Modifier.padding(4.dp).size(13.dp), tint = Color.White)
                                            }
                                        }
                                        if (!food.available) Box(
                                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)),
                                            contentAlignment = Alignment.Center,
                                        ) { Text("TẠM HẾT", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(food.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
                                                    .format(food.price) + "đ", color = Brand, fontWeight = FontWeight.Bold)
                                            if (food.salePercent != null) Text(
                                                NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(food.basePrice) + "đ",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                            )
                                        }
                                        if (!food.available) Text("Món đang tạm hết hàng", color = Brand, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(color = CardStroke)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

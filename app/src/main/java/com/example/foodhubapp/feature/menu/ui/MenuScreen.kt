package com.example.foodhubapp.feature.menu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.menu.viewmodel.MenuViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.CardStroke
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Quay lại")
                }
                Text(
                    "Thực đơn",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onCartClick) {
                    Icon(Icons.Default.ShoppingCart, "Mở giỏ hàng")
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
                                        .clickable { onFoodClick(food.id) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    FoodImage(food.imageUrl, food.name, Modifier.size(80.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(food.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
                                                .format(food.price) + "đ", color = Brand)
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

package com.example.foodhubapp.feature.customer.menu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.customer.menu.viewmodel.FoodDetailViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.Brand

/**
 * Cầu nối giữa FoodDetailViewModel và FoodDetailScreen: thu StateFlow, hiển thị
 * loading/error/snackbar và chuyển trường hợp 401 sang dialog đăng nhập.
 */
@Composable
fun FoodDetailRoute(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: FoodDetailViewModel = viewModel(),
    onCartClick: () -> Unit = {},
    onScanQrClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    val food = state.food
    if (food != null) {
        FoodDetailScreen(food, onBackClick, viewModel::addToCart, reviews = state.reviews, onCartClick = onCartClick, isAdding = state.isAdding,
            snackbarHost = { SnackbarHost(snackbar) })
    } else {
        Scaffold(containerColor = AppBackground) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                }
                if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Brand)
                else FoodLoadError(state.error ?: "Không tìm thấy món ăn.", viewModel::loadFood,
                    Modifier.align(Alignment.Center))
            }
        }
    }
    if (state.requiresLogin) AlertDialog(
        onDismissRequest = viewModel::dismissLogin,
        title = { Text("Đăng nhập") },
        text = { Text("Bạn cần đăng nhập để thêm món vào giỏ TAKEAWAY hoặc DELIVERY.") },
        confirmButton = { TextButton(onClick = { viewModel.dismissLogin(); onLoginClick() }) { Text("Đăng nhập") } },
        dismissButton = { TextButton(onClick = viewModel::dismissLogin) { Text("Để sau") } }
    )
    if (state.requiresTableScan) AlertDialog(
        onDismissRequest = viewModel::dismissTableScan,
        title = { Text("Phiên bàn đã hết hạn") },
        text = { Text("Vui lòng quét lại mã QR trên bàn để tiếp tục gọi món.") },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissTableScan(); onScanQrClick() }) { Text("Quét lại QR") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissTableScan) { Text("Để sau") } }
    )
}

@Composable
internal fun FoodLoadError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Thử lại") }
    }
}

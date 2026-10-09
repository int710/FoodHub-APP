package com.example.foodhubapp.feature.notification.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.feature.notification.data.FoodHubNotification
import com.example.foodhubapp.feature.notification.viewmodel.NotificationUiState
import com.example.foodhubapp.feature.notification.viewmodel.NotificationViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationRoute(
    onBack: () -> Unit,
    onLoginClick: () -> Unit = {},
    onOpenOrder: (String?) -> Unit = {},
    viewModel: NotificationViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    NotificationScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onUnreadOnlyChange = viewModel::setUnreadOnly,
        onNotificationClick = { notification ->
            if (!notification.isRead) viewModel.markAsRead(notification)
            onOpenOrder(notification.orderId)
        },
        onMarkAllRead = viewModel::markAllAsRead,
    )
    if (state.requiresLogin) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissLogin,
            title = { Text("Đăng nhập") },
            text = { Text("Phiên đăng nhập của bạn đã hết hạn. Vui lòng đăng nhập lại.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissLogin(); onLoginClick() }) { Text("Đăng nhập") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissLogin) { Text("Để sau") } }
        )
    }
}

@Composable
fun NotificationScreen(
    state: NotificationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUnreadOnlyChange: (Boolean) -> Unit,
    onNotificationClick: (FoodHubNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    showHeader: Boolean = true,
) {
    Scaffold(containerColor = AppBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showHeader) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
                    Text("Thông báo", Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, "Tải lại")
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(!state.unreadOnly, { onUnreadOnlyChange(false) }, { Text("Tất cả") })
                FilterChip(state.unreadOnly, { onUnreadOnlyChange(true) }, { Text("Chưa đọc (${state.unreadCount})") })
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onMarkAllRead, enabled = state.unreadCount > 0 && state.busyId == null) {
                    Text("Đọc tất cả")
                }
            }
            when {
                state.isLoading && state.notifications.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand)
                }
                state.errorMessage != null && state.notifications.isEmpty() -> NotificationEmpty(
                    title = "Không tải được thông báo",
                    message = state.errorMessage,
                    action = "Thử lại",
                    onClick = onRefresh,
                )
                state.notifications.isEmpty() -> NotificationEmpty(
                    title = if (state.unreadOnly) "Không có thông báo chưa đọc" else "Chưa có thông báo",
                    message = "Các cập nhật về đơn hàng sẽ xuất hiện tại đây.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.errorMessage?.let { message ->
                        item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    }
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            isBusy = state.busyId == notification.id,
                            onClick = { onNotificationClick(notification) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: FoodHubNotification, isBusy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (notification.isRead) Color.White else BrandSoft, RoundedCornerShape(8.dp))
            .clickable(enabled = !isBusy, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(40.dp).background(if (notification.isRead) AppBackground else Brand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.NotificationsNone, null, tint = if (notification.isRead) OnSurfaceVariant else Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(notification.title, Modifier.weight(1f), color = Neutral, fontWeight = FontWeight.Bold)
                if (!notification.isRead) Box(Modifier.size(8.dp).background(Brand, CircleShape))
            }
            Text(notification.message, color = OnSurfaceVariant, fontSize = 13.sp)
            Text(
                listOfNotNull(notification.orderCode, formatNotificationTime(notification.createdAt)).joinToString(" • "),
                color = OnSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun NotificationEmpty(title: String, message: String, action: String? = null, onClick: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), tint = Brand)
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(message, color = OnSurfaceVariant)
        if (action != null) Button(onClick = onClick, modifier = Modifier.padding(top = 12.dp)) { Text(action) }
    }
}

private fun formatNotificationTime(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd/MM HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.parse(value))
}.getOrDefault(value.take(16))

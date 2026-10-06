package com.example.foodhubapp.feature.admin.order.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foodhubapp.feature.admin.order.data.AdminOrderRepository
import com.example.foodhubapp.feature.admin.order.data.KitchenItem
import com.example.foodhubapp.feature.admin.model.AdminItemStatus
import kotlinx.coroutines.launch

@Composable
internal fun AdminKitchenScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AdminOrderRepository(context) }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<KitchenItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    suspend fun load() {
        loading = true; error = null
        try {
            val all = mutableListOf<KitchenItem>()
            var page = 1
            do { val batch = repository.getKitchenItems(page++); all.addAll(batch) } while (batch.size == 100)
            items = all
        } catch (e: Exception) { error = e.message ?: "Không thể tải danh sách bếp" }
        finally { loading = false }
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row { TextButton(onBack) { Text("Quay lại đơn hàng") }; TextButton({ scope.launch { load() } }, enabled = !loading && !busy) { Text("Tải lại") } }
        Text("Món chờ làm / đang làm", style = MaterialTheme.typography.titleLarge)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!loading && error == null && items.isEmpty()) Text("Không có món đang chờ bếp")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${item.quantity} × ${item.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${item.destination} • ${item.status.label}")
                        if (item.options.isNotEmpty()) Text("Tùy chọn: ${item.options.joinToString(", ")}")
                        item.note?.let { Text("Ghi chú: $it") }
                        Button({ scope.launch {
                            busy = true; error = null
                            try {
                                repository.updateItem(item.id, if (item.status == AdminItemStatus.WAITING) AdminItemStatus.PREPARING else AdminItemStatus.READY)
                                load()
                            } catch (e: Exception) { error = e.message ?: "Không thể cập nhật món" }
                            finally { busy = false }
                        } }, enabled = !busy && !loading) { Text(if (item.status == AdminItemStatus.WAITING) "Bắt đầu làm" else "Đã làm xong") }
                    }
                }
            }
        }
    }
}

@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.menu.ui

import com.example.foodhubapp.feature.admin.components.AdminPlaceholder
import com.example.foodhubapp.feature.admin.components.AdminSearchField
import com.example.foodhubapp.feature.admin.components.vnd

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.WarningAmber
import com.example.foodhubapp.feature.shared.ui.FoodHubAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodhubapp.feature.admin.navigation.AdminBackground
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.model.AdminMenuCategory
import com.example.foodhubapp.feature.admin.model.AdminMenuCategoryInput
import com.example.foodhubapp.feature.admin.model.AdminMenuInput
import com.example.foodhubapp.feature.admin.model.AdminFlashSaleInput
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminRed
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import kotlin.collections.forEach

@Composable
fun AdminMenuScreen(
    items: List<AdminMenuItem>,
    categories: List<AdminMenuCategory>,
    isLoading: Boolean,
    isSaving: Boolean,
    isUploading: Boolean,
    busyItemId: String?,
    busyCategoryId: String?,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onCreate: (AdminMenuInput) -> Unit,
    onUpdate: (String, AdminMenuInput) -> Unit,
    onDelete: (String) -> Unit,
    onToggleAvailability: (String) -> Unit,
    onCreateCategory: (AdminMenuCategoryInput) -> Unit,
    onUpdateCategory: (String, AdminMenuCategoryInput) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onCreateFlashSale: (AdminFlashSaleInput) -> Unit,
    onDeleteFlashSale: (String) -> Unit,
    onUploadImage: (Uri, (String) -> Unit) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    var tab by remember { mutableStateOf("Món ăn") }
    var editingItem by remember { mutableStateOf<AdminMenuItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AdminMenuItem?>(null) }
    var editingCategory by remember { mutableStateOf<AdminMenuCategory?>(null) }
    var showCategoryEditor by remember { mutableStateOf(false) }
    var deleteCategoryTarget by remember { mutableStateOf<AdminMenuCategory?>(null) }
    var showFlashSaleEditor by remember { mutableStateOf(false) }
    var flashSaleTargetId by remember { mutableStateOf<String?>(null) }
    var variantTarget by remember { mutableStateOf<AdminMenuItem?>(null) }
    val categoryFilters = listOf("Tất cả") + items.map { it.category }.distinct()
    val filtered = items.filter { (category == "Tất cả" || it.category == category) && (query.isBlank() || it.name.contains(query, true)) }

    Scaffold(
        containerColor = AdminBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (tab) {
                        "Món ăn" -> {
                            editingItem = null
                            showEditor = true
                        }
                        "Danh mục" -> {
                            editingCategory = null
                            showCategoryEditor = true
                        }
                        else -> showFlashSaleEditor = true
                    }
                },
                containerColor = AdminPrimary,
            ) {
                Icon(Icons.Default.Add, "Thêm", tint = Color.White)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AdminSearchField(
                query,
                { query = it },
                if (tab == "Danh mục") "Tìm danh mục" else "Tìm món ăn",
                Modifier.padding(16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Món ăn", "Danh mục", "Flash sale").forEach { label ->
                    FilterChip(tab == label, { tab = label }, { Text(label) })
                }
            }
            if (tab == "Món ăn") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryFilters) { item -> FilterChip(category == item, { category = item }, { Text(item) }) }
                }
                when {
                    isLoading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPrimary)
                    }
                    errorMessage != null && items.isEmpty() -> AdminPlaceholder(
                        icon = Icons.Default.WarningAmber,
                        title = "Không tải được thực đơn",
                        message = errorMessage,
                        button = "Thử lại",
                        onClick = onRefresh,
                    )
                    filtered.isEmpty() -> AdminPlaceholder(
                        icon = Icons.Default.RestaurantMenu,
                        title = "Chưa có món ăn",
                        message = if (query.isBlank()) "Hãy thêm món đầu tiên vào thực đơn." else "Không tìm thấy món phù hợp.",
                        button = "Tải lại",
                        onClick = onRefresh,
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { menuItem ->
                            AdminMenuRow(
                                item = menuItem,
                                isBusy = busyItemId == menuItem.id,
                                onToggle = { onToggleAvailability(menuItem.id) },
                                onEdit = {
                                    editingItem = menuItem
                                    showEditor = true
                                },
                                onDelete = { deleteTarget = menuItem },
                                onVariant = { variantTarget = menuItem },
                                onFlashSale = {
                                    flashSaleTargetId = menuItem.id
                                    showFlashSaleEditor = true
                                },
                            )
                        }
                    }
                }
            } else if (tab == "Danh mục") {
                val filteredCategories = categories.filter { query.isBlank() || it.name.contains(query, true) }
                when {
                    isLoading && categories.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPrimary)
                    }
                    filteredCategories.isEmpty() -> AdminPlaceholder(
                        icon = Icons.Default.RestaurantMenu,
                        title = "Chưa có danh mục",
                        message = "Tạo danh mục để sắp xếp các món trong thực đơn.",
                        button = "Thêm danh mục",
                        onClick = {
                            editingCategory = null
                            showCategoryEditor = true
                        },
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredCategories, key = { it.id }) { menuCategory ->
                            AdminCategoryRow(
                                category = menuCategory,
                                isBusy = busyCategoryId == menuCategory.id,
                                onEdit = {
                                    editingCategory = menuCategory
                                    showCategoryEditor = true
                                },
                                onDelete = { deleteCategoryTarget = menuCategory },
                            )
                        }
                    }
                }
            } else {
                val saleItems = items.filter { it.salePrice != null }
                if (saleItems.isEmpty()) AdminPlaceholder(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Chưa có flash sale",
                    message = "Tạo chương trình giảm giá theo khung giờ.",
                    button = "Tạo flash sale",
                    onClick = { showFlashSaleEditor = true },
                ) else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(saleItems, key = { it.id }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = AdminSurface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("${item.price.vnd()} → ${item.salePrice?.vnd()}", color = AdminPrimary)
                                }
                                TextButton(onClick = { onDeleteFlashSale(item.id) }) { Text("Kết thúc", color = AdminRed) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryEditor) {
        AdminCategoryEditorDialog(
            category = editingCategory,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showCategoryEditor = false },
            onSubmit = { input ->
                editingCategory?.let { onUpdateCategory(it.id, input) } ?: onCreateCategory(input)
                showCategoryEditor = false
            },
        )
    }

    if (showEditor) {
        AdminMenuEditorDialog(
            item = editingItem,
            categories = categories,
            isSaving = isSaving,
            isUploading = isUploading,
            onUploadImage = onUploadImage,
            onDismiss = { if (!isSaving) showEditor = false },
            onSubmit = { input ->
                editingItem?.let { onUpdate(it.id, input) } ?: onCreate(input)
                showEditor = false
            },
        )
    }

    if (showFlashSaleEditor) {
        AdminFlashSaleDialog(
            items = items,
            initialItemId = flashSaleTargetId,
            isSaving = isSaving,
            onDismiss = {
                showFlashSaleEditor = false
                flashSaleTargetId = null
            },
            onSubmit = {
                onCreateFlashSale(it)
                showFlashSaleEditor = false
                flashSaleTargetId = null
            },
        )
    }

    variantTarget?.let { item ->
        AdminVariantManager(item.id, item.name, onDismiss = { variantTarget = null; onRefresh() })
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa món ăn?") },
            text = { Text("${item.name} sẽ bị xóa hoặc ẩn nếu đang có trong đơn hàng.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(item.id)
                        deleteTarget = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AdminRed),
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Hủy") } },
        )
    }

    deleteCategoryTarget?.let { menuCategory ->
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = { Text("Xóa danh mục?") },
            text = {
                Text(
                    if (menuCategory.itemCount > 0) {
                        "${menuCategory.name} đang có ${menuCategory.itemCount} món. Hãy chuyển hoặc xóa các món trước."
                    } else {
                        "Danh mục ${menuCategory.name} sẽ bị xóa khỏi thực đơn."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(menuCategory.id)
                        deleteCategoryTarget = null
                    },
                    enabled = menuCategory.itemCount == 0,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AdminRed),
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteCategoryTarget = null }) { Text("Hủy") } },
        )
    }
}


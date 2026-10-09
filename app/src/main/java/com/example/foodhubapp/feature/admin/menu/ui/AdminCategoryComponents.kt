@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.menu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.model.AdminMenuCategory
import com.example.foodhubapp.feature.admin.model.AdminMenuCategoryInput
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminPrimarySoft
import com.example.foodhubapp.feature.admin.navigation.AdminRed
import com.example.foodhubapp.feature.admin.navigation.AdminSurface

@Composable
internal fun AdminCategoryRow(
    category: AdminMenuCategory,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(AdminPrimarySoft, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(category.icon, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Bold)
                Text("${category.itemCount} món • Thứ tự ${category.sortOrder}", color = AdminMuted, fontSize = 12.sp)
            }
            if (isBusy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onEdit) { Text("Sửa") }
                TextButton(onClick = onDelete) { Text("Xóa", color = AdminRed) }
            }
        }
    }
}

@Composable
internal fun AdminCategoryEditorDialog(
    category: AdminMenuCategory?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AdminMenuCategoryInput) -> Unit,
) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember(category?.id) { mutableStateOf(category?.icon ?: "🍽") }
    var sortOrder by remember(category?.id) { mutableStateOf(category?.sortOrder?.toString() ?: "0") }
    val parsedSortOrder = sortOrder.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Thêm danh mục" else "Sửa danh mục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên danh mục") }, singleLine = true)
                OutlinedTextField(icon, { icon = it.take(8) }, label = { Text("Biểu tượng") }, singleLine = true)
                OutlinedTextField(
                    sortOrder,
                    { sortOrder = it.filter(Char::isDigit) },
                    label = { Text("Thứ tự hiển thị") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(AdminMenuCategoryInput(name.trim(), icon.trim(), parsedSortOrder ?: 0)) },
                enabled = !isSaving && name.isNotBlank() && icon.isNotBlank() && parsedSortOrder != null,
            ) { Text(if (category == null) "Thêm" else "Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") } },
    )
}


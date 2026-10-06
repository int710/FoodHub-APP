@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.menu.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.model.AdminMenuCategory
import com.example.foodhubapp.feature.admin.model.AdminMenuInput
import kotlinx.coroutines.launch
import kotlin.collections.forEach

@Composable
internal fun AdminMenuEditorDialog(
    item: AdminMenuItem?,
    categories: List<AdminMenuCategory>,
    isSaving: Boolean,
    isUploading: Boolean,
    onUploadImage: (Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (AdminMenuInput) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var description by remember(item?.id) { mutableStateOf(item?.description.orEmpty()) }
    var price by remember(item?.id) { mutableStateOf(item?.price?.toString().orEmpty()) }
    var imageUrl by remember(item?.id) { mutableStateOf(item?.imageUrl.orEmpty()) }
    var sortOrder by remember(item?.id) { mutableStateOf(item?.sortOrder?.toString() ?: "0") }
    var categoryId by remember(item?.id, categories) {
        mutableStateOf(item?.categoryId?.takeIf(String::isNotBlank) ?: categories.firstOrNull()?.id.orEmpty())
    }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var isAvailable by remember(item?.id) { mutableStateOf(item?.isAvailable ?: true) }
    var isFeatured by remember(item?.id) { mutableStateOf(item?.isFeatured ?: true) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val parsedPrice = price.toLongOrNull()
    val parsedSortOrder = sortOrder.toIntOrNull()
    val isValid = name.trim().length >= 2 && categoryId.isNotBlank() && parsedPrice != null && parsedPrice > 0 && parsedSortOrder != null
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadImage(it) { uploadedUrl -> imageUrl = uploadedUrl } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Thêm món ăn" else "Chỉnh sửa món ăn") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên món") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        enabled = categories.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(selectedCategory?.name ?: "Chọn danh mục") }
                    DropdownMenu(categoryMenuOpen, { categoryMenuOpen = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    categoryMenuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    label = { Text("Giá bán") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    label = { Text("Mô tả") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text("URL ảnh") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isUploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(if (isUploading) "Đang tải ảnh..." else "Chọn ảnh từ thiết bị")
                }
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter(Char::isDigit) },
                    label = { Text("Thứ tự hiển thị") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Đang bán", Modifier.weight(1f))
                    Switch(isAvailable, { isAvailable = it })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Món nổi bật", Modifier.weight(1f))
                    Switch(isFeatured, { isFeatured = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        AdminMenuInput(
                            categoryId = categoryId,
                            name = name.trim(),
                            description = description.trim().takeIf(String::isNotBlank),
                            basePrice = parsedPrice ?: 0,
                            imageUrl = imageUrl.trim().takeIf(String::isNotBlank),
                            isAvailable = isAvailable,
                            isFeatured = isFeatured,
                            sortOrder = parsedSortOrder ?: 0,
                        )
                    )
                },
                enabled = isValid && !isSaving && !isUploading,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (item == null) "Thêm món" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") } },
    )
}


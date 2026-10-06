@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.menu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.model.AdminFlashSaleInput
import kotlin.collections.forEach

@Composable
internal fun AdminFlashSaleDialog(
    items: List<AdminMenuItem>,
    initialItemId: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AdminFlashSaleInput) -> Unit,
) {
    var selectedId by remember(initialItemId, items) {
        mutableStateOf(initialItemId?.takeIf { id -> items.any { it.id == id } } ?: items.firstOrNull()?.id.orEmpty())
    }
    var discount by remember { mutableStateOf("10") }
    var hours by remember { mutableStateOf("24") }
    var menuOpen by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.id == selectedId }
    val parsedDiscount = discount.toDoubleOrNull()
    val parsedHours = hours.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo flash sale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected?.name ?: "Chọn món", Modifier.weight(1f))
                    }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        items.forEach { item ->
                            DropdownMenuItem({ Text(item.name) }, onClick = { selectedId = item.id; menuOpen = false })
                        }
                    }
                }
                OutlinedTextField(discount, { discount = it.filter { char -> char.isDigit() || char == '.' } }, label = { Text("Phần trăm giảm") })
                OutlinedTextField(hours, { hours = it.filter(Char::isDigit) }, label = { Text("Thời lượng (giờ)") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(AdminFlashSaleInput(selectedId, parsedDiscount ?: 0.0, parsedHours ?: 0)) },
                enabled = !isSaving && selectedId.isNotBlank() && parsedDiscount != null && parsedDiscount in 0.01..100.0 && parsedHours != null && parsedHours > 0,
            ) { Text("Tạo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}


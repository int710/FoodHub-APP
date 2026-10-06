package com.example.foodhubapp.feature.admin.menu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foodhubapp.feature.admin.menu.data.AdminMenuRepository
import com.example.foodhubapp.feature.admin.model.*
import kotlinx.coroutines.launch

@Composable
internal fun AdminVariantManager(itemId: String, itemName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AdminMenuRepository(context) }
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<AdminVariantGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<AdminVariantGroup?>(null) }
    var adding by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    fun reload() { scope.launch {
        loading = true; error = null
        try { groups = repository.getVariantGroups(itemId) } catch (e: Exception) { error = e.message }
        finally { loading = false }
    } }
    LaunchedEffect(itemId) { reload() }
    if (adding || editing != null) {
        VariantEditor(editing?.input, saving, error,
            onDismiss = { if (!saving) { adding = false; editing = null; error = null } },
            onSubmit = { input -> scope.launch {
                saving = true; error = null
                try {
                    val group = editing
                    if (group == null) repository.createVariant(itemId, input) else repository.updateVariant(group.id, input)
                    adding = false; editing = null; reload()
                } catch (e: Exception) { error = e.message } finally { saving = false }
            } })
    } else AlertDialog(onDismissRequest = onDismiss, title = { Text("Tùy chọn: $itemName") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton({ reload() }) { Text("Thử lại") } }
            if (!loading && error == null && groups.isEmpty()) Text("Món chưa có nhóm tùy chọn")
            groups.forEach { group -> OutlinedButton({ editing = group }) { Text("Sửa ${group.input.name}") } }
            Button({ adding = true }, enabled = !loading && error == null) { Text("Thêm nhóm") }
        }
    }, confirmButton = { TextButton(onDismiss) { Text("Đóng") } })
}

private data class OptionDraft(val id: String?, val name: String, val price: String)

@Composable
private fun VariantEditor(initial: AdminVariantInput?, saving: Boolean, error: String?, onDismiss: () -> Unit, onSubmit: (AdminVariantInput) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var multiple by remember { mutableStateOf(initial?.multiple ?: false) }
    var required by remember { mutableStateOf(initial?.required ?: true) }
    var options by remember { mutableStateOf(initial?.options?.map { OptionDraft(it.id, it.name, it.priceAdd.toString()) } ?: listOf(OptionDraft(null, "", "0"))) }
    val valid = name.isNotBlank() && options.all { it.name.isNotBlank() && (it.price.toLongOrNull() ?: -1) >= 0 }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(if (initial == null) "Thêm nhóm" else "Sửa nhóm tùy chọn") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(name, { name = it }, label = { Text("Tên nhóm") }, enabled = !saving)
            Row { Text("Chọn nhiều", Modifier.weight(1f)); Switch(multiple, { multiple = it }, enabled = !saving) }
            Row { Text("Bắt buộc", Modifier.weight(1f)); Switch(required, { required = it }, enabled = !saving) }
            options.forEachIndexed { index, option ->
                OutlinedTextField(option.name, { value -> options = options.mapIndexed { i, row -> if (i == index) row.copy(name = value) else row } }, label = { Text("Tên tùy chọn ${index + 1}") }, enabled = !saving)
                OutlinedTextField(option.price, { value -> options = options.mapIndexed { i, row -> if (i == index) row.copy(price = value) else row } }, label = { Text("Giá cộng (đ)") }, enabled = !saving)
            }
            TextButton({ options = options + OptionDraft(null, "", "0") }, enabled = !saving) { Text("Thêm tùy chọn") }
        }
    }, confirmButton = { Button({ onSubmit(AdminVariantInput(name.trim(), multiple, required, options.map { AdminVariantOptionInput(it.name.trim(), it.price.toLong(), it.id) }, initial?.sortOrder ?: 0)) }, enabled = valid && !saving) { Text(if (saving) "Đang lưu…" else "Lưu") } },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text("Hủy") } })
}

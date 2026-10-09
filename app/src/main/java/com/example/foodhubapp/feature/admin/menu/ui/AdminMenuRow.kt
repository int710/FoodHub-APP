@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.menu.ui

import com.example.foodhubapp.feature.admin.components.vnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.model.AdminMenuItem
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminPrimarySoft
import com.example.foodhubapp.feature.admin.navigation.AdminRed
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import com.example.foodhubapp.feature.admin.navigation.AdminText

@Composable
internal fun AdminMenuRow(
    item: AdminMenuItem,
    isBusy: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVariant: () -> Unit,
    onFlashSale: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AdminPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AdminPrimary)
                            }
                        },
                        error = {
                            Text(item.icon, color = AdminPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    )
                } else {
                    Text(item.icon, color = AdminPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.category, color = AdminMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.salePrice?.let { Text(it.vnd(), color = AdminPrimary, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(6.dp))
                    Text(item.price.vnd(), color = if (item.salePrice != null) AdminMuted else AdminText, fontSize = 13.sp)
                }
            }
            if (isBusy) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = AdminPrimary)
            } else {
                Switch(checked = item.isAvailable, onCheckedChange = { onToggle() })
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Tùy chọn") }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Chỉnh sửa") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem({ Text("Thêm nhóm tùy chọn") }, onClick = { menuOpen = false; onVariant() })
                    DropdownMenuItem({ Text("Tạo giảm giá") }, onClick = { menuOpen = false; onFlashSale() })
                    DropdownMenuItem({ Text("Xóa", color = AdminRed) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}


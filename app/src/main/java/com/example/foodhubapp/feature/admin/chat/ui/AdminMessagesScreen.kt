@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.chat.ui

import com.example.foodhubapp.feature.admin.components.AdminAvatar
import com.example.foodhubapp.feature.admin.components.AdminSearchField

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.chat.model.AdminConversation
import com.example.foodhubapp.feature.admin.chat.model.isTableSession
import com.example.foodhubapp.feature.admin.navigation.AdminGreen
import com.example.foodhubapp.feature.admin.navigation.AdminGreenSoft
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminPrimarySoft
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import com.example.foodhubapp.feature.admin.navigation.AdminBorder
import com.example.foodhubapp.feature.admin.navigation.AdminText

@Composable
fun AdminMessagesScreen(conversations: List<AdminConversation>, onOpenChat: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var unreadOnly by remember { mutableStateOf(false) }
    var tableOnly by remember { mutableStateOf(false) }
    val filtered = conversations.filter {
        (!unreadOnly || it.unread > 0) && (!tableOnly || it.isTableSession) &&
            (query.isBlank() || it.customer.contains(query, true) || it.lastMessage.contains(query, true))
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("Hộp thư hỗ trợ", color = AdminText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Mỗi tài khoản và mỗi phiên QR là một hội thoại riêng.", color = AdminMuted, fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth().background(AdminGreenSoft).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(AdminGreen, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("Trung tâm hỗ trợ đang trực tuyến", color = AdminGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("${conversations.size} hội thoại", color = AdminGreen, fontSize = 11.sp)
        }
        AdminSearchField(query, { query = it }, "Tìm khách hàng hoặc nội dung", Modifier.padding(16.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!unreadOnly && !tableOnly, { unreadOnly = false; tableOnly = false }, { Text("Tất cả") })
            FilterChip(tableOnly, { tableOnly = !tableOnly; if (tableOnly) unreadOnly = false }, { Text("Phiên bàn") })
            FilterChip(unreadOnly, { unreadOnly = !unreadOnly; if (unreadOnly) tableOnly = false }, { Text("Chưa đọc") })
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(42.dp), tint = AdminMuted)
                        Spacer(Modifier.height(10.dp))
                        Text("Không có hội thoại phù hợp", color = AdminText, fontWeight = FontWeight.SemiBold)
                        Text("Tin nhắn mới sẽ tự động xuất hiện tại đây.", color = AdminMuted, fontSize = 12.sp)
                    }
                }
            }
            items(filtered, key = { it.id }) { conversation ->
                AdminConversationRow(conversation, { onOpenChat(conversation.id) })
            }
        }
    }
}

@Composable
internal fun AdminConversationRow(conversation: AdminConversation, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = AdminSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (conversation.unread > 0) AdminPrimary.copy(alpha = .35f) else AdminBorder),
        shadowElevation = if (conversation.unread > 0) 2.dp else 0.dp,
    ) {
    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AdminAvatar(conversation.customer)
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).size(19.dp),
                color = if (conversation.isTableSession) AdminPrimarySoft else AdminGreenSoft,
                shape = CircleShape,
                border = BorderStroke(2.dp, AdminSurface),
            ) {
                Icon(
                    if (conversation.isTableSession) Icons.Default.TableRestaurant else Icons.Default.PersonOutline,
                    null,
                    Modifier.padding(3.dp),
                    tint = if (conversation.isTableSession) AdminPrimary else AdminGreen,
                )
            }
            if (conversation.isOnline) Box(Modifier.align(Alignment.BottomEnd).size(11.dp).background(
                AdminGreen, CircleShape))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(conversation.customer, fontWeight = if (conversation.unread > 0) FontWeight.Bold else FontWeight.Medium)
                Text(conversation.time, color = AdminMuted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(conversation.contextLabel, color = if (conversation.isTableSession) AdminPrimary else AdminGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                conversation.orderCode?.let { Text("#$it", color = AdminMuted, fontSize = 10.sp) }
            }
            Text(conversation.lastMessage, color = if (conversation.unread > 0) AdminText else AdminMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (conversation.unread > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(22.dp).background(AdminPrimary, CircleShape), contentAlignment = Alignment.Center) {
                Text(conversation.unread.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    }
}


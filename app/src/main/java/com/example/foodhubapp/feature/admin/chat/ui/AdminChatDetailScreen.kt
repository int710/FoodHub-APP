@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.chat.ui

import com.example.foodhubapp.feature.admin.components.AdminAvatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.chat.AdminChatSocketClient
import com.example.foodhubapp.feature.shared.chat.ChatConnectionState
import com.example.foodhubapp.feature.shared.chat.ChatMessage
import com.example.foodhubapp.feature.admin.chat.model.AdminConversation
import com.example.foodhubapp.feature.admin.chat.model.AdminUiMessage
import com.example.foodhubapp.feature.admin.navigation.AdminGreen
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminRed
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import com.example.foodhubapp.feature.admin.navigation.AdminText
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun AdminChatDetailScreen(conversation: AdminConversation, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val client = remember { AdminChatSocketClient(context) }
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(ChatConnectionState.CONNECTING) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.id) {
        client.connect(
            conversationId = conversation.id,
            onState = { connectionState = it; if (it == ChatConnectionState.CONNECTED) error = null },
            onHistory = { history -> messages.clear(); messages.addAll(history) },
            onMessage = { message -> if (messages.none { it.id == message.id }) messages.add(message) },
            onError = { error = it },
        )
    }
    DisposableEffect(client) { onDispose { client.disconnect() } }

    fun send() {
        val content = draft.trim()
        if (content.isBlank() || isSending || connectionState != ChatConnectionState.CONNECTED) return
        isSending = true
        client.send(conversation.id, content) { success, message ->
            isSending = false
            if (success) draft = "" else error = message ?: "Không thể gửi tin nhắn"
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(AdminSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AdminAvatar(conversation.customer)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(conversation.customer, fontWeight = FontWeight.Bold)
                Text(
                    if (connectionState == ChatConnectionState.CONNECTED) "Đã kết nối" else "Đang kết nối...",
                    color = if (connectionState == ChatConnectionState.CONNECTED) AdminGreen else AdminMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            conversation.orderCode?.let { Text(it, color = AdminPrimary, fontWeight = FontWeight.Bold) }
            TextButton(onClick = onClose) { Text("Đóng hội thoại") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            error?.let { item { Text(it, color = AdminRed, fontSize = 12.sp) } }
            items(messages, key = { it.id }) { message ->
                AdminMessageBubble(
                    AdminUiMessage(
                        id = message.id,
                        content = message.content,
                        time = message.createdAt.take(16),
                        fromAdmin = !message.isCustomer,
                    )
                )
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Quán đã nhận yêu cầu", "Món sắp hoàn thành", "Cảm ơn bạn")) { reply ->
                AssistChip(onClick = { draft = reply }, label = { Text(reply) })
            }
        }
        Row(Modifier.fillMaxWidth().background(AdminSurface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = ::send,
                enabled = !isSending && connectionState == ChatConnectionState.CONNECTED,
                modifier = Modifier.background(AdminPrimary, CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White) }
        }
    }
}

@Composable
private fun AdminMessageBubble(message: AdminUiMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromAdmin) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(0.78f).background(if (message.fromAdmin) AdminPrimary else AdminSurface, RoundedCornerShape(12.dp)).padding(11.dp),
            horizontalAlignment = if (message.fromAdmin) Alignment.End else Alignment.Start,
        ) {
            Text(message.content, color = if (message.fromAdmin) Color.White else AdminText)
            Text(message.time, color = if (message.fromAdmin) Color.White.copy(alpha = 0.75f) else AdminMuted, fontSize = 10.sp)
        }
    }
}


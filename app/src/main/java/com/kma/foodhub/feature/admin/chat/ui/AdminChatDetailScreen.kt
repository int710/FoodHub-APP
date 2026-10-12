package com.kma.foodhub.feature.admin.chat.ui

import com.kma.foodhub.core.ui.icons.TablerIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kma.foodhub.feature.admin.chat.AdminChatSocketClient
import com.kma.foodhub.feature.admin.chat.model.AdminConversation
import com.kma.foodhub.feature.admin.chat.model.isTableSession
import com.kma.foodhub.feature.admin.chat.model.isClosed
import com.kma.foodhub.feature.admin.components.AdminAvatar
import com.kma.foodhub.feature.admin.navigation.AdminBackground
import com.kma.foodhub.feature.admin.navigation.AdminBorder
import com.kma.foodhub.feature.admin.navigation.AdminGreen
import com.kma.foodhub.feature.admin.navigation.AdminGreenSoft
import com.kma.foodhub.feature.admin.navigation.AdminMuted
import com.kma.foodhub.feature.admin.navigation.AdminPrimary
import com.kma.foodhub.feature.admin.navigation.AdminPrimarySoft
import com.kma.foodhub.feature.admin.navigation.AdminRed
import com.kma.foodhub.feature.admin.navigation.AdminRedSoft
import com.kma.foodhub.feature.admin.navigation.AdminSurface
import com.kma.foodhub.feature.admin.navigation.AdminText
import com.kma.foodhub.feature.shared.chat.ChatConnectionState
import com.kma.foodhub.feature.shared.chat.ChatMessage
import com.kma.foodhub.feature.shared.ui.FoodHubDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AdminChatDetailScreen(
    conversation: AdminConversation,
    onClose: () -> Unit = {},
    onDeleteConversation: () -> Unit = {},
    canDeleteConversation: Boolean = true,
) {
    val context = LocalContext.current
    val client = remember { AdminChatSocketClient(context) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(ChatConnectionState.CONNECTING) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var confirmDeleteConversation by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.id) {
        client.connect(
            conversationId = conversation.id,
            onState = { connectionState = it; if (it == ChatConnectionState.CONNECTED) error = null },
            onHistory = { history -> messages.clear(); messages.addAll(history.distinctBy(ChatMessage::id)) },
            onMessage = { message -> if (messages.none { it.id == message.id }) messages.add(message) },
            onError = { error = it },
        )
    }
    DisposableEffect(client) { onDispose { client.disconnect() } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun send(text: String = draft) {
        val content = text.trim()
        if (content.isBlank() || isSending || connectionState != ChatConnectionState.CONNECTED) return
        isSending = true
        client.send(conversation.id, content) { success, message ->
            isSending = false
            if (success) draft = "" else error = message ?: "Không thể gửi tin nhắn"
        }
    }

    Column(Modifier.fillMaxSize().background(AdminBackground).imePadding()) {
        Surface(color = AdminSurface, shadowElevation = 2.dp) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        Icon(TablerIcons.ArrowBack, "Quay lại", tint = AdminText)
                    }
                    AdminAvatar(conversation.customer)
                    Column(Modifier.weight(1f).padding(start = 11.dp, end = 12.dp)) {
                        Text(
                            conversation.customer,
                            color = AdminText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(connectionColor(connectionState), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${conversation.contextLabel} · ${connectionText(connectionState)}",
                                color = connectionColor(connectionState),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (canDeleteConversation) {
                        androidx.compose.material3.IconButton(onClick = { confirmDeleteConversation = true }) {
                            Icon(TablerIcons.Delete, "Xóa cuộc trò chuyện", tint = AdminRed)
                        }
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 64.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    item {
                        Surface(color = if (conversation.isTableSession) AdminPrimarySoft else AdminGreenSoft, shape = RoundedCornerShape(9.dp)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (conversation.isTableSession) TablerIcons.Table else TablerIcons.User,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = if (conversation.isTableSession) AdminPrimary else AdminGreen,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (conversation.isTableSession) "Bàn" else "Tài khoản", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    conversation.orderCode?.let { orderCode ->
                        item {
                            Surface(color = AdminPrimarySoft, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "#$orderCode",
                                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    color = AdminPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        error?.let {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                color = AdminRedSoft,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(it, Modifier.padding(12.dp), color = AdminRed, fontSize = 12.sp)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(color = Color(0xFFEDEFF4), shape = RoundedCornerShape(20.dp)) {
                        Text("Hỗ trợ trực tuyến", Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = AdminMuted, fontSize = 10.sp)
                    }
                }
            }
            if (messages.isEmpty() && connectionState == ChatConnectionState.CONNECTED) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(TablerIcons.ChatBubble, null, Modifier.size(38.dp), tint = AdminMuted)
                        Text("Chưa có tin nhắn", Modifier.padding(top = 10.dp), color = AdminText, fontWeight = FontWeight.SemiBold)
                        Text("Hãy bắt đầu hỗ trợ khách hàng.", color = AdminMuted, fontSize = 12.sp)
                    }
                }
            }
            items(messages, key = ChatMessage::id) { message ->
                AdminMessageBubble(message)
            }
        }

        if (conversation.isClosed) {
            Surface(color = AdminPrimarySoft) {
                Text(
                    "Phiên đã kết thúc · Lịch sử tin nhắn được lưu ở chế độ chỉ đọc.",
                    Modifier.fillMaxWidth().padding(16.dp),
                    color = AdminText,
                    fontSize = 12.sp,
                )
            }
        } else Surface(color = AdminSurface, shadowElevation = 6.dp) {
            Column {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(listOf("Quán đã nhận yêu cầu", "Món sắp hoàn thành", "Nhân viên đang tới", "Cảm ơn bạn")) { reply ->
                        AssistChip(
                            onClick = { draft = reply },
                            label = { Text(reply, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = AdminBackground, labelColor = AdminText),
                            border = BorderStroke(1.dp, AdminBorder),
                        )
                    }
                }
                HorizontalDivider(color = AdminBorder)
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(5_000) },
                        enabled = connectionState == ChatConnectionState.CONNECTED,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp, max = 130.dp),
                        placeholder = { Text("Nhập phản hồi cho khách...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(18.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AdminPrimary, unfocusedBorderColor = AdminBorder),
                    )
                    FilledIconButton(
                        onClick = { send() },
                        enabled = !isSending && draft.isNotBlank() && connectionState == ChatConnectionState.CONNECTED,
                        modifier = Modifier.size(50.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = AdminPrimary, disabledContainerColor = AdminBorder),
                    ) { Icon(TablerIcons.Send, "Gửi", tint = Color.White) }
                }
            }
        }
    }

    if (confirmDeleteConversation) {
        FoodHubDialog(
            onDismissRequest = { confirmDeleteConversation = false },
            title = "Xóa toàn bộ cuộc trò chuyện?",
            eyebrow = "Quản trị hội thoại",
            content = {
                Text("Toàn bộ lịch sử tin nhắn với ${conversation.customer} sẽ bị xóa vĩnh viễn. Không thể xóa riêng từng tin nhắn.", color = AdminText)
            },
            actions = {
                TextButton(
                    onClick = { confirmDeleteConversation = false },
                    modifier = Modifier.weight(1f),
                ) { Text("Giữ lại", color = AdminText) }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        confirmDeleteConversation = false
                        onDeleteConversation()
                        onClose()
                    },
                ) { Text("Xóa cuộc trò chuyện", color = AdminRed) }
            },
        )
    }
}

@Composable
private fun AdminMessageBubble(message: ChatMessage) {
    val fromAdmin = !message.isCustomer
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (fromAdmin) Alignment.End else Alignment.Start) {
        if (!fromAdmin) Text("${message.senderRole.replaceFirstChar(Char::uppercase)} · Khách hàng", color = AdminMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(0.78f),
            color = if (fromAdmin) AdminPrimary else AdminSurface,
            shape = if (fromAdmin) RoundedCornerShape(17.dp, 17.dp, 4.dp, 17.dp) else RoundedCornerShape(17.dp, 17.dp, 17.dp, 4.dp),
            border = if (fromAdmin) null else BorderStroke(1.dp, AdminBorder),
            shadowElevation = if (fromAdmin) 0.dp else 1.dp,
        ) {
            Text(message.content, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = if (fromAdmin) Color.White else AdminText, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Text(
            formatChatTime(message.createdAt),
            color = AdminMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
        )
    }
}

private fun connectionText(state: ChatConnectionState) = when (state) {
    ChatConnectionState.CONNECTED -> "Đang kết nối trực tiếp"
    ChatConnectionState.CONNECTING -> "Đang kết nối..."
    ChatConnectionState.DISCONNECTED -> "Đang kết nối lại..."
    ChatConnectionState.ERROR -> "Mất kết nối"
}

private fun connectionColor(state: ChatConnectionState) = when (state) {
    ChatConnectionState.CONNECTED -> AdminGreen
    ChatConnectionState.ERROR -> AdminRed
    else -> AdminMuted
}

private fun formatChatTime(raw: String): String = runCatching {
    CHAT_TIME_FORMAT.format(Instant.parse(raw).atZone(ZoneId.systemDefault()))
}.getOrDefault("Vừa xong")

private val CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm · dd/MM")

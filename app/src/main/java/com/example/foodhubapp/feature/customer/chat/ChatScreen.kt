package com.example.foodhubapp.feature.customer.chat

import com.example.foodhubapp.feature.shared.chat.ChatConnectionState
import com.example.foodhubapp.feature.shared.chat.ChatMessage
import com.example.foodhubapp.feature.shared.chat.ChatSocketListener

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.customer.table.BurntOrange
import com.example.foodhubapp.feature.customer.table.Green
import com.example.foodhubapp.feature.customer.table.Ink
import com.example.foodhubapp.feature.customer.table.Label
import com.example.foodhubapp.feature.customer.table.Muted
import com.example.foodhubapp.feature.customer.table.PaleBlue
import com.example.foodhubapp.feature.customer.table.data.TableSession
import com.example.foodhubapp.feature.customer.table.data.TableSessionStore
import com.example.foodhubapp.feature.customer.table.data.TableScanRepository
import com.example.foodhubapp.feature.shared.ui.FoodHubDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatScreen(onBack: () -> Unit, onSessionEnded: () -> Unit = onBack) {
    val context = LocalContext.current
    val tableSession = remember { TableSessionStore(context).current() }
    val isTableChat = tableSession != null
    val tableRepository = remember { TableScanRepository(context) }
    val scope = rememberCoroutineScope()
    val chatClient = remember { ChatSocketClient(context) }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(ChatConnectionState.CONNECTING) }
    var isStaffTyping by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCallDialog by remember { mutableStateOf(false) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var isEndingSession by remember { mutableStateOf(false) }
    var sessionEnded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var connectionAttempt by remember { mutableStateOf(0) }

    fun addOrUpdateMessage(message: ChatMessage) {
        val index = chatMessages.indexOfFirst { it.id == message.id }
        if (index >= 0) chatMessages[index] = message else chatMessages.add(message)
    }

    DisposableEffect(chatClient, connectionAttempt) {
        chatClient.connect(object : ChatSocketListener {
            override fun onConnectionStateChanged(state: ChatConnectionState) {
                connectionState = state
                if (state == ChatConnectionState.CONNECTED) errorMessage = null
                if (state != ChatConnectionState.CONNECTED) isStaffTyping = false
            }

            override fun onHistory(messages: List<ChatMessage>) {
                if (messages.isEmpty()) { chatMessages.clear(); return }
                val merged = (messages + chatMessages)
                    .associateBy(ChatMessage::id)
                    .values
                    .sortedBy(ChatMessage::createdAt)
                chatMessages.clear()
                chatMessages.addAll(merged)
            }

            override fun onMessage(message: ChatMessage) = addOrUpdateMessage(message)

            override fun onTyping(isTyping: Boolean) {
                isStaffTyping = isTyping
            }

            override fun onError(message: String) {
                errorMessage = message
            }

            override fun onSessionEnded() {
                sessionEnded = true
                chatMessages.clear()
                draft = ""
                isSending = false
                errorMessage = null
            }
        })
        onDispose {
            chatClient.setTyping(false)
            chatClient.disconnect()
        }
    }

    fun send(text: String, onSuccess: () -> Unit = {}) {
        val content = text.trim()
        if (content.isBlank() || isSending) return
        if (connectionState != ChatConnectionState.CONNECTED) {
            errorMessage = "Kênh chat chưa kết nối. Vui lòng chờ và thử lại."
            return
        }

        isSending = true
        chatClient.setTyping(false)
        chatClient.sendMessage(content) { success, error ->
            isSending = false
            if (success) {
                if (draft.trim() == content) draft = ""
                onSuccess()
            } else {
                errorMessage = error ?: "Không thể gửi tin nhắn"
            }
        }
    }

    LaunchedEffect(draft) {
        if (draft.isBlank()) {
            chatClient.setTyping(false)
        } else {
            chatClient.setTyping(true)
            delay(1_200)
            chatClient.setTyping(false)
        }
    }

    LaunchedEffect(chatMessages.size, isStaffTyping) {
        val extraItems = 2 + if (isStaffTyping) 1 else 0
        if (chatMessages.isNotEmpty() || isStaffTyping) {
            listState.animateScrollToItem(chatMessages.size + extraItems - 1)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F5F3)).safeDrawingPadding().imePadding()) {
        ChatHeader(
            tableName = tableSession?.tableName ?: "Hỗ trợ đơn hàng",
            connectionState = connectionState,
            onBack = onBack,
            onCall = {
                send(if (isTableChat) "Tôi cần nhân viên hỗ trợ tại bàn." else "Tôi cần hỗ trợ về đơn hàng.") {
                    showCallDialog = true
                }
            },
        )
        SessionSummary(
            session = tableSession,
            connectionState = connectionState,
            onDetails = { showSessionDialog = true },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { CenterPill("Hỗ trợ trực tiếp") }
            item { SystemMessage(tableSession?.tableName ?: "đơn hàng của bạn", connectionState) }
            items(chatMessages, key = ChatMessage::id) { message ->
                if (message.isCustomer) {
                    SentMessage(message.content, formatMessageTime(message.createdAt))
                } else {
                    StaffMessage(message)
                }
            }
            if (isStaffTyping) {
                item { CenterPill("Nhân viên đang phản hồi  •••") }
            }
            errorMessage?.let { error ->
                item {
                    ConnectionNotice(error) {
                        errorMessage = null
                        connectionAttempt++
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White)) {
            HorizontalDivider(color = Color(0xFFEAE5E1))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (isTableChat) {
                    QuickReply("Xin thêm đá") { send("Xin thêm đá giúp mình nhé!") }
                    QuickReply("Lấy thêm tương ớt/cà") { send("Cho mình xin thêm tương ớt và tương cà nhé!") }
                    QuickReply("Cần thìa/dĩa thêm") { send("Cho mình xin thêm thìa và dĩa nhé!") }
                } else {
                    QuickReply("Trạng thái đơn") { send("Cho mình hỏi trạng thái đơn hàng hiện tại.") }
                    QuickReply("Hỗ trợ thanh toán") { send("Mình cần hỗ trợ về thanh toán đơn hàng.") }
                    QuickReply("Đổi cách nhận món") { send("Mình cần hỗ trợ về phương thức nhận món.") }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(5_000) },
                    enabled = connectionState == ChatConnectionState.CONNECTED,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp, max = 140.dp),
                    placeholder = {
                        Label(
                            if (connectionState == ChatConnectionState.CONNECTED) {
                                "Nhắn tin cho nhân viên..."
                            } else {
                                "Chưa kết nối"
                            },
                            13.sp,
                            Color(0xFF999BA7),
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(28.dp),
                )
                FilledIconButton(
                    onClick = { send(draft) },
                    enabled = connectionState == ChatConnectionState.CONNECTED && draft.isNotBlank() && !isSending,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = BurntOrange,
                        disabledContainerColor = Color(0xFFE7E2DF),
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi tin nhắn", tint = Color.White)
                }
            }
        }
    }

    if (sessionEnded) {
        FoodHubDialog(
            onDismissRequest = onSessionEnded,
            title = "Phiên bàn đã kết thúc",
            eyebrow = "Phiên phục vụ",
            content = { Text("Bạn không còn quyền chat hoặc đặt món trong phiên này. Quét lại QR khi bắt đầu lượt dùng bàn mới.") },
            actions = { TextButton(onClick = onSessionEnded, modifier = Modifier.fillMaxWidth()) { Text("Về trang chủ") } },
        )
    }

    if (showCallDialog) {
        FoodHubDialog(
            onDismissRequest = { showCallDialog = false },
            title = "Đã gửi yêu cầu",
            eyebrow = "Hỗ trợ FoodHub",
            content = { Label(if (isTableChat) "Yêu cầu hỗ trợ đã được gửi tới nhân viên phục vụ ${tableSession.tableName}." else "Yêu cầu hỗ trợ đơn hàng đã được gửi tới FoodHub.") },
            actions = {
                TextButton(onClick = { showCallDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Label("Đóng", color = BurntOrange)
                }
            },
        )
    }
    if (showSessionDialog) {
        FoodHubDialog(
            onDismissRequest = { showSessionDialog = false },
            title = tableSession?.tableName ?: "Hỗ trợ đơn hàng",
            eyebrow = if (tableSession != null) "Phiên tại bàn" else "Kênh hỗ trợ",
            dismissEnabled = !isEndingSession,
            content = {
                Label(
                    listOfNotNull(tableSession?.floor, connectionLabel(connectionState)).joinToString(" • "),
                )
            },
            actions = {
                if (tableSession != null) {
                    TextButton(
                        enabled = !isEndingSession,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isEndingSession = true
                            scope.launch {
                                runCatching { tableRepository.endSession() }
                                    .onSuccess { onSessionEnded() }
                                    .onFailure { errorMessage = it.message }
                                isEndingSession = false
                                showSessionDialog = false
                            }
                        },
                    ) { Label(if (isEndingSession) "Đang kết thúc..." else "Kết thúc phiên bàn", color = Color(0xFFB3261E)) }
                }
                TextButton(onClick = { showSessionDialog = false }, modifier = Modifier.weight(1f)) {
                    Label("Đóng", color = BurntOrange)
                }
            },
        )
    }
}

@Composable
private fun ChatHeader(
    tableName: String,
    connectionState: ChatConnectionState,
    onBack: () -> Unit,
    onCall: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 3.dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink)
        }
        Box(Modifier.size(38.dp).background(Color(0xFFFFE8DF), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.HeadsetMic, null, tint = BurntOrange, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Quản lý FoodHub", fontSize = 16.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Label(
                connectionLabel(connectionState),
                10.sp,
                if (connectionState == ChatConnectionState.CONNECTED) Green else Muted,
                bold = true,
            )
        }
        Box(
            Modifier.background(Color(0xFFE4F3F1), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            Text(tableName, fontSize = 10.sp, color = Color(0xFF0B6E58), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(56.dp))
        }
        Spacer(Modifier.width(7.dp))
        IconButton(onClick = onCall, enabled = connectionState == ChatConnectionState.CONNECTED) {
            Icon(Icons.Default.Call, "Gọi nhân viên", tint = if (connectionState == ChatConnectionState.CONNECTED) BurntOrange else Color.Gray)
        }
    }
    }
}

@Composable
private fun SessionSummary(
    session: TableSession?,
    connectionState: ChatConnectionState,
    onDetails: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(17.dp)).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(Color(0xFFFFEEE9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.TableRestaurant, null, tint = BurntOrange)
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Label(session?.tableName ?: "Hỗ trợ đơn hàng", 15.sp, Ink, bold = true)
            Label(
                connectionLabel(connectionState),
                10.sp,
                if (connectionState == ChatConnectionState.CONNECTED) Green else Muted,
                bold = true,
            )
        }
        Box(
            Modifier.background(Color(0xFFF5F5F7), RoundedCornerShape(20.dp))
                .clickable(onClick = onDetails).padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Modifier.size(14.dp), tint = Color(0xFF765E53))
                Spacer(Modifier.width(4.dp))
                Label("Chi tiết", 10.sp, Color(0xFF765E53), bold = true)
            }
        }
    }
}

@Composable
private fun CenterPill(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.background(Color(0xFFEFF0F5), RoundedCornerShape(18.dp))
                .padding(horizontal = 13.dp, vertical = 5.dp),
        ) {
            Label(text, 10.sp, Color(0xFF67616A), bold = true)
        }
    }
}

@Composable
private fun SystemMessage(tableName: String, state: ChatConnectionState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            .background(Color(0xFFF0F3FF), RoundedCornerShape(17.dp)).padding(12.dp),
    ) {
        Box(
            Modifier.size(28.dp).background(Color(0xFFFFE9AE), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = Color(0xFF896000))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Label("HỆ THỐNG FOODHUB", 10.sp, Color(0xFF9B6514), bold = true)
            Label(if (state == ChatConnectionState.CONNECTED) "Kênh hỗ trợ của $tableName đã kết nối với quản lý FoodHub." else "Đang chờ kết nối với quản lý FoodHub.", 12.sp)
        }
    }
}

@Composable
private fun SentMessage(message: String, time: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(
            Modifier.fillMaxWidth(.83f).background(BurntOrange, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Label(message, 14.sp, Color.White)
        }
        Label("$time  ✓", 10.sp, Color(0xFF647067), modifier = Modifier.padding(top = 3.dp, end = 3.dp))
    }
}

@Composable
private fun StaffMessage(message: ChatMessage) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).background(Color(0xFFD2E7DA), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.HeadsetMic, null, Modifier.size(18.dp), tint = Color(0xFF537257))
            }
            Label(
                if (message.senderRole.equals("admin", true)) "Quản lý FoodHub" else "Nhân viên phục vụ",
                11.sp,
                Ink,
                bold = true,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Surface(
            Modifier.fillMaxWidth(.84f).padding(start = 38.dp)
            , color = Color.White, shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp), shadowElevation = 1.dp,
        ) {
            Label(message.content, 13.sp, modifier = Modifier.padding(12.dp))
        }
        Label(
            formatMessageTime(message.createdAt),
            10.sp,
            Green,
            modifier = Modifier.padding(start = 43.dp, top = 3.dp),
        )
    }
}

@Composable
private fun ConnectionNotice(message: String, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFFFE9E3), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(message, 12.sp, Color(0xFF8B2E14), modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Thử lại", color = BurntOrange) }
    }
}

@Composable
private fun QuickReply(text: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Color(0xFFF5F6FA), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Label(text, 10.sp, Ink, bold = true)
    }
}

private fun connectionLabel(state: ChatConnectionState): String = when (state) {
    ChatConnectionState.CONNECTING -> "Đang kết nối..."
    ChatConnectionState.CONNECTED -> "Đã kết nối trực tiếp"
    ChatConnectionState.DISCONNECTED -> "Mất kết nối, đang thử lại"
    ChatConnectionState.ERROR -> "Không thể kết nối"
}

private fun formatMessageTime(value: String): String {
    if (value.isBlank()) return "Vừa xong"
    return runCatching {
        MESSAGE_TIME_FORMATTER.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrDefault("Vừa xong")
}

private val MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

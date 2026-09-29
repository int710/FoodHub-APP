package com.example.foodhubapp.feature.table

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import com.example.foodhubapp.feature.table.data.TableSession
import com.example.foodhubapp.feature.table.data.TableSessionStore
import com.example.foodhubapp.feature.table.data.TableScanRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatScreen(onBack: () -> Unit, onSessionEnded: () -> Unit = onBack) {
    val context = LocalContext.current
    val tableSession = remember { TableSessionStore(context).current() }
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
    val listState = rememberLazyListState()

    fun addOrUpdateMessage(message: ChatMessage) {
        val index = chatMessages.indexOfFirst { it.id == message.id }
        if (index >= 0) chatMessages[index] = message else chatMessages.add(message)
    }

    DisposableEffect(chatClient) {
        chatClient.connect(object : ChatSocketListener {
            override fun onConnectionStateChanged(state: ChatConnectionState) {
                connectionState = state
                if (state != ChatConnectionState.CONNECTED) isStaffTyping = false
            }

            override fun onHistory(messages: List<ChatMessage>) {
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

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FF))) {
        ChatHeader(
            tableName = tableSession?.tableName ?: "Chưa có bàn",
            connectionState = connectionState,
            onBack = onBack,
            onCall = {
                send("Tôi cần nhân viên hỗ trợ tại bàn.") {
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
            item { SystemMessage(tableSession?.tableName ?: "bàn của bạn") }
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
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                QuickReply("Xin thêm đá") { send("Xin thêm đá giúp mình nhé!") }
                QuickReply("Lấy thêm tương ớt/cà") { send("Cho mình xin thêm tương ớt và tương cà nhé!") }
                QuickReply("Cần thìa/dĩa thêm") { send("Cho mình xin thêm thìa và dĩa nhé!") }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlyphCircle("▧", 42.dp, PaleBlue, Color(0xFF725C56), 20.sp)
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(5_000) },
                    enabled = connectionState == ChatConnectionState.CONNECTED,
                    modifier = Modifier.weight(1f).height(54.dp),
                    placeholder = {
                        Label(
                            if (connectionState == ChatConnectionState.CONNECTED) {
                                "Nhắn tin cho nhân viên..."
                            } else {
                                "Đang kết nối..."
                            },
                            13.sp,
                            Color(0xFF999BA7),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
                GlyphCircle(
                    if (isSending) "…" else "➤",
                    48.dp,
                    if (draft.isBlank() || isSending) Color(0xFFB8B8BC) else Color(0xFFD94400),
                    Color.White,
                    23.sp,
                    onClick = { send(draft) },
                )
            }
        }
    }

    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = { Label("Đã gọi nhân viên", 20.sp, bold = true) },
            text = { Label("Yêu cầu hỗ trợ đã được gửi qua kênh chat của ${tableSession?.tableName ?: "bàn"}.") },
            confirmButton = {
                TextButton(onClick = { showCallDialog = false }) {
                    Label("Đóng", color = BurntOrange)
                }
            },
        )
    }
    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = { Label(tableSession?.tableName ?: "Phiên bàn", 20.sp, bold = true) },
            text = {
                Label(
                    listOfNotNull(tableSession?.floor, connectionLabel(connectionState)).joinToString(" • "),
                )
            },
            confirmButton = {
                TextButton(onClick = { showSessionDialog = false }) {
                    Label("Đóng", color = BurntOrange)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isEndingSession,
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
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label("←", 27.sp, Ink, modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp))
        Box(
            Modifier.size(40.dp).background(Color(0xFFF9ECE9), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Label("♙", 21.sp, BurntOrange)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Label("Nhân viên phục vụ", 16.sp, Ink, bold = true)
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
            Label(tableName, 10.sp, Color(0xFF0B6E58), bold = true)
        }
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier.background(Color(0xFFD84400), RoundedCornerShape(28.dp))
                .clickable(onClick = onCall).padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Label("♧  Gọi", 11.sp, Color.White, bold = true)
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
            Modifier.size(42.dp).background(Color(0xFFFFEEE9), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Label("▤", 21.sp, BurntOrange)
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Label(session?.tableName ?: "Chưa có phiên bàn", 15.sp, Ink, bold = true)
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
            Label("▤ Chi tiết", 10.sp, Color(0xFF765E53), bold = true)
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
private fun SystemMessage(tableName: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            .background(Color(0xFFF0F3FF), RoundedCornerShape(17.dp)).padding(12.dp),
    ) {
        GlyphCircle("▣", 25.dp, Color(0xFFFFE9AE), Color(0xFF896000), 15.sp)
        Column(Modifier.padding(start = 10.dp)) {
            Label("HỆ THỐNG FOODHUB", 10.sp, Color(0xFF9B6514), bold = true)
            Label("Kênh hỗ trợ của $tableName đã sẵn sàng. Tin nhắn được gửi trực tiếp đến nhân viên.", 12.sp)
        }
    }
}

@Composable
private fun SentMessage(message: String, time: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(
            Modifier.fillMaxWidth(.83f).background(BurntOrange, RoundedCornerShape(17.dp))
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
            GlyphCircle("♙", 30.dp, Color(0xFFD2E7DA), Color(0xFF537257), 16.sp)
            Label(
                if (message.senderRole.equals("admin", true)) "Quản lý FoodHub" else "Nhân viên phục vụ",
                11.sp,
                Ink,
                bold = true,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Box(
            Modifier.fillMaxWidth(.84f).padding(start = 38.dp)
                .background(Color.White, RoundedCornerShape(16.dp)).padding(12.dp),
        ) {
            Label(message.content, 13.sp)
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
            .clickable(onClick = onDismiss).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(message, 12.sp, Color(0xFF8B2E14), modifier = Modifier.weight(1f))
        Label("×", 18.sp, Color(0xFF8B2E14), bold = true)
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
    ChatConnectionState.CONNECTING -> "● Đang kết nối..."
    ChatConnectionState.CONNECTED -> "● Đã kết nối trực tiếp"
    ChatConnectionState.DISCONNECTED -> "● Mất kết nối, đang thử lại"
    ChatConnectionState.ERROR -> "● Không thể kết nối"
}

private fun formatMessageTime(value: String): String {
    if (value.isBlank()) return "Vừa xong"
    return runCatching {
        MESSAGE_TIME_FORMATTER.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrDefault("Vừa xong")
}

private val MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

package com.foodhub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Line = Color(0xFFE8EBF4)

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val newMessages = remember { mutableStateListOf<String>() }
    var draft by remember { mutableStateOf("") }
    var showCallDialog by remember { mutableStateOf(false) }
    var showOrderDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun send(text: String) {
        if (text.isNotBlank()) {
            newMessages.add(text.trim())
            draft = ""
        }
    }

    LaunchedEffect(newMessages.size) {
        if (newMessages.isNotEmpty()) listState.animateScrollToItem(5 + newMessages.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FF))) {
        ChatHeader(onBack = onBack, onCall = { showCallDialog = true })
        OrderSummary(onDetails = { showOrderDialog = true })

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { CenterPill("Hôm nay • 12:20 PM") }
            item { SystemMessage() }
            item { SentMessage("Chào bạn, cho mình xin thêm 1 chén sốt nấm Truffle và 1 ly đá nhé!", "12:22 PM") }
            item { StaffMessage() }
            item { UpdateMessage() }
            items(newMessages) { message -> SentMessage(message, "Vừa xong") }
            item { CenterPill("Nhân viên bàn đang phản hồi  •••") }
        }

        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                QuickReply("Xin thêm đá 🧊") { send("Xin thêm đá giúp mình nhé!") }
                QuickReply("Lấy thêm tương ớt/cà 🍅") { send("Cho mình xin thêm tương ớt và tương cà nhé!") }
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
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).height(54.dp),
                    placeholder = { Label("Nhắn tin cho nhân viên...", 13.sp, Color(0xFF999BA7)) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
                GlyphCircle("➤", 48.dp, Color(0xFFD94400), Color.White, 23.sp, onClick = { send(draft) })
            }
        }
    }

    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = { Label("Gọi nhân viên", 20.sp, bold = true) },
            text = { Label("Đã gửi yêu cầu hỗ trợ cho nhân viên tại Bàn 08.") },
            confirmButton = { TextButton(onClick = { showCallDialog = false }) { Label("Đóng", color = BurntOrange) } },
        )
    }
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Label("Đơn #FH-8942", 20.sp, bold = true) },
            text = { Label("Bàn 04 • Bếp trưởng & Phục vụ đang trực tuyến") },
            confirmButton = { TextButton(onClick = { showOrderDialog = false }) { Label("Đóng", color = BurntOrange) } },
        )
    }
}

@Composable
private fun ChatHeader(onBack: () -> Unit, onCall: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label("←", 27.sp, Ink, modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp))
        Box(Modifier.size(40.dp).background(Color(0xFFF9ECE9), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Label("♙", 21.sp, BurntOrange)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Label("Nhân viên phục vụ", 16.sp, Ink, bold = true)
            Label("●  Online & Sẵn sàng hỗ trợ", 10.sp, Green, bold = true)
        }
        Box(Modifier.background(Color(0xFFE4F3F1), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp)) {
            Label("Bàn\n08", 10.sp, Color(0xFF0B6E58), bold = true)
        }
        Spacer(Modifier.width(7.dp))
        Box(Modifier.background(Color(0xFFD84400), RoundedCornerShape(28.dp))
            .clickable(onClick = onCall).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Label("♧  Gọi\nbàn", 11.sp, Color.White, bold = true)
        }
    }
}

@Composable
private fun OrderSummary(onDetails: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(17.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(Color(0xFFFFEEE9), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Label("⚒", 21.sp, BurntOrange)
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Label("Đơn #FH-8942  •  Bàn 04", 15.sp, Ink, bold = true)
            Label("● Bếp trưởng & Phục vụ đang trực tuyến", 10.sp, Green, bold = true)
        }
        Box(Modifier.background(Color(0xFFF5F5F7), RoundedCornerShape(20.dp))
            .clickable(onClick = onDetails).padding(horizontal = 8.dp, vertical = 5.dp)) {
            Label("▤ Chi tiết", 10.sp, Color(0xFF765E53), bold = true)
        }
    }
}

@Composable
private fun CenterPill(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.background(Color(0xFFEFF0F5), RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 5.dp)) {
            Label(text, 10.sp, Color(0xFF67616A), bold = true)
        }
    }
}

@Composable
private fun SystemMessage() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        .background(Color(0xFFF0F3FF), RoundedCornerShape(17.dp)).padding(12.dp)) {
        GlyphCircle("▣", 25.dp, Color(0xFFFFE9AE), Color(0xFF896000), 15.sp)
        Column(Modifier.padding(start = 10.dp)) {
            Label("HỆ THỐNG FOODHUB", 10.sp, Color(0xFF9B6514), bold = true)
            Label("Xin chào quý khách! Bếp và nhân viên Bàn 04 luôn sẵn sàng hỗ trợ bạn qua kênh chat trực tiếp này.", 12.sp)
        }
    }
}

@Composable
private fun SentMessage(message: String, time: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(Modifier.fillMaxWidth(.83f).background(BurntOrange, RoundedCornerShape(17.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)) {
            Label(message, 14.sp, Color.White)
        }
        Label("$time  ✓✓", 10.sp, Color(0xFF647067), modifier = Modifier.padding(top = 3.dp, end = 3.dp))
    }
}

@Composable
private fun StaffMessage() {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphCircle("♙", 30.dp, Color(0xFFD2E7DA), Color(0xFF537257), 16.sp)
            Label("Minh Quân (Phục vụ)", 11.sp, Ink, bold = true, modifier = Modifier.padding(start = 7.dp))
            Spacer(Modifier.width(5.dp))
            Box(Modifier.background(Color(0xFFFFE5DB), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp)) {
                Label("Bếp tiếp nhận", 9.sp, Color(0xFF9D4A27), bold = true)
            }
        }
        Column(Modifier.fillMaxWidth(.84f).padding(start = 38.dp)
            .background(Color.White, RoundedCornerShape(16.dp)).padding(10.dp)) {
            Label("Dạ FoodHub đã nhận yêu cầu! Bếp trưởng đang chuẩn bị thêm sốt nóng hổi cho bàn mình, bạn phục vụ sẽ mang tới ngay sau 2 phút nữa ạ 👨‍🍳", 13.sp)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().background(PaleBlue, RoundedCornerShape(12.dp))
                .padding(horizontal = 9.dp, vertical = 7.dp)) {
                Label("♨   +1 Sốt nấm Truffle & 1 Đá bi\n       Đã chuyển lệnh đến bếp • Miễn phí", 10.sp, Green, bold = true)
            }
        }
        Label("12:23 PM   Đã xem", 10.sp, Green, modifier = Modifier.padding(start = 43.dp, top = 3.dp))
    }
}

@Composable
private fun UpdateMessage() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        .background(Color(0xFFFFF1E1), RoundedCornerShape(16.dp))
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphCircle("◆", 38.dp, Color(0xFFFFB300), Color(0xFF6B4D00), 20.sp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Label("CẬP NHẬT MÓN ĂN •", 10.sp, Color(0xFF966312), bold = true)
            Label("Món Pizza Margherita đã nướng xong và đang được mang ra bàn của bạn!", 12.sp)
        }
        Label("12:25", 9.sp, Color(0xFF6D5F52))
    }
}

@Composable
private fun QuickReply(text: String, onClick: () -> Unit) {
    Box(Modifier.background(Color(0xFFF5F6FA), RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Label(text, 10.sp, Ink, bold = true)
    }
}

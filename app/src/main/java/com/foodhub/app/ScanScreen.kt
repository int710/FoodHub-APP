package com.foodhub.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFFFB600)

@Composable
fun ScanScreen(onContinue: () -> Unit) {
    var flashOn by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var tableNumber by remember { mutableStateOf("05") }
    var tableDraft by remember { mutableStateOf(tableNumber) }
    var selectedGallery by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFF17181B))) {
        CafeBackdrop()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphCircle("×", 44.dp, Color(0xCC121417), Color.White, 29.sp)
                Label("Scan Qr", 18.sp, Color.White, bold = true)
                GlyphCircle(if (flashOn) "ϟ" else "♜", 44.dp, Color(0xCC121417), Color.White,
                    onClick = { flashOn = !flashOn })
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.background(Color(0xD91C1C1D), RoundedCornerShape(30.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) { Label("●  AI CAMERA SẴN SÀNG", 10.sp, Color(0xFFFFDB97), bold = true) }
                Spacer(Modifier.height(6.dp))
                Label("Quét mã QR tại bàn để gọi món", 19.sp, Color.White, bold = true, align = TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                Label(
                    "Đưa camera hướng vào mã QR dán trên mặt bàn để tự động nhận diện số bàn và mở thực đơn",
                    12.sp, Color(0xFFE2E2E4), align = TextAlign.Center,
                )
            }

            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 50.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
                ScannerFrame(Modifier.fillMaxWidth().fillMaxSize())
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                    .background(Color(0xF01B1C1F), RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScanTool("ϟ", if (flashOn) "Tắt flash" else "Đèn flash") { flashOn = !flashOn }
                ScanTool("▣", "Thư viện ảnh") { selectedGallery = !selectedGallery }
                ScanTool("▦", "Nhập số bàn") { tableDraft = tableNumber; showTableDialog = true }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .background(Color.White, RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphCircle("♜", 36.dp, Color(0xFFF9ECE7), BurntOrange, 18.sp)
                    Column(Modifier.weight(1f).padding(start = 9.dp)) {
                        Label("CHI NHÁNH HIỆN TẠI", 10.sp, Color(0xFF8B5E50), bold = true)
                        Label("FoodHub Landmark 81", 16.sp, Ink, bold = true)
                    }
                    Box(Modifier.background(Color(0xFFB2FFD9), RoundedCornerShape(24.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Label("◉ Đã kết nối", 10.sp, Color(0xFF0B6C43), bold = true)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth().background(PaleBlue, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(Gold, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Label("▤", 28.sp, Color(0xFF6C4200), bold = true)
                        }
                        Column(Modifier.padding(start = 12.dp)) {
                            Label("Tự động dò tìm   Beacon 1.2m", 10.sp, Color(0xFF775D55))
                            Label("Bàn số $tableNumber", 19.sp, Ink, bold = true)
                            Label("Tầng 1 • Khu vực máy lạnh thoáng mát", 11.sp, Color(0xFF565A64))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BurntOrange),
                        shape = RoundedCornerShape(13.dp),
                    ) { Label("Xác nhận vào bàn ngay  →", 14.sp, Color.White, bold = true) }
                }
                Spacer(Modifier.height(14.dp))
                Label("♢  Thanh toán bảo mật qua VNPay & MoMo tại bàn", 11.sp,
                    Color(0xFF5D5960), modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
        }
        if (selectedGallery) {
            Box(Modifier.align(Alignment.Center).background(Color(0xEF202329), RoundedCornerShape(14.dp))
                .clickable { selectedGallery = false }.padding(18.dp)) {
                Label("Chọn ảnh QR sẽ được kết nối sau", 13.sp, Color.White)
            }
        }
    }

    if (showTableDialog) {
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Label("Nhập số bàn", 20.sp, bold = true) },
            text = { OutlinedTextField(value = tableDraft, onValueChange = { tableDraft = it.filter(Char::isDigit).take(3) },
                label = { Label("Số bàn") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                if (tableDraft.isNotBlank()) tableNumber = tableDraft.padStart(2, '0')
                showTableDialog = false
            }) { Label("Xác nhận", color = BurntOrange, bold = true) } },
            dismissButton = { TextButton(onClick = { showTableDialog = false }) { Label("Hủy") } },
        )
    }
}

@Composable
private fun ScanTool(icon: String, name: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        GlyphCircle(icon, 38.dp, Color(0xFF303135), Color.White, 18.sp)
        Spacer(Modifier.height(5.dp))
        Label(name, 10.sp, Color.White, bold = true)
    }
}

@Composable
private fun CafeBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2B2C), Color(0xFF4C382D), Color(0xFF1F2022))))
        val w = size.width
        val h = size.height
        // Soft shapes stand in for the restaurant photo until a separate asset is provided.
        drawCircle(Color(0x44FFD3A4), w * .17f, Offset(w * .17f, h * .20f))
        drawCircle(Color(0x33FFE0BD), w * .10f, Offset(w * .84f, h * .17f))
        drawRect(Color(0x66533825), Offset(0f, h * .40f), androidx.compose.ui.geometry.Size(w, h * .21f))
        drawLine(Color(0x66401F15), Offset(0f, h * .57f), Offset(w, h * .57f), 25.dp.toPx())
        drawRect(Color(0xB918191B))
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val x0 = w * .07f
            val x1 = w * .93f
            val y0 = h * .06f
            val y1 = h * .94f
            val len = 22.dp.toPx()
            val stroke = 4.dp.toPx()
            listOf(
                Pair(Offset(x0, y0 + len), Offset(x0, y0)), Pair(Offset(x0, y0), Offset(x0 + len, y0)),
                Pair(Offset(x1 - len, y0), Offset(x1, y0)), Pair(Offset(x1, y0), Offset(x1, y0 + len)),
                Pair(Offset(x0, y1 - len), Offset(x0, y1)), Pair(Offset(x0, y1), Offset(x0 + len, y1)),
                Pair(Offset(x1 - len, y1), Offset(x1, y1)), Pair(Offset(x1, y1), Offset(x1, y1 - len)),
            ).forEach { (a, b) -> drawLine(Gold, a, b, stroke, cap = StrokeCap.Round) }
            drawRect(Color(0x55FFFFFF), topLeft = Offset(w * .25f, h * .24f),
                size = androidx.compose.ui.geometry.Size(w * .5f, h * .52f), style = Stroke(1.dp.toPx()))
            drawLine(Color(0x55FFFFFF), Offset(w * .25f, h * .50f), Offset(w * .75f, h * .50f), 1.dp.toPx())
        }
        Box(Modifier.size(128.dp).background(Color(0xCCDDD8CD), RoundedCornerShape(5.dp))
            .border(5.dp, Color(0xCC604638), RoundedCornerShape(5.dp)).padding(12.dp)) {
            FakeQr()
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
            .background(Color(0xCC171717), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Label("▦  Căn chỉnh mã trong khung", 10.sp, Color.White, bold = true)
        }
    }
}

@Composable
private fun FakeQr() {
    Canvas(Modifier.fillMaxSize()) {
        val count = 21
        val cell = minOf(size.width, size.height) / count
        fun finder(x: Int, y: Int, fx: Int, fy: Int): Boolean {
            val dx = x - fx
            val dy = y - fy
            if (dx !in 0..6 || dy !in 0..6) return false
            return dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4)
        }
        for (y in 0 until count) for (x in 0 until count) {
            val insideFinder = (x in 0..6 && y in 0..6) || (x in 14..20 && y in 0..6) || (x in 0..6 && y in 14..20)
            val filled = if (insideFinder) finder(x, y, 0, 0) || finder(x, y, 14, 0) || finder(x, y, 0, 14)
                else ((x * 13 + y * 7 + x * y * 3) % 11) < 5
            if (filled) drawRect(Color(0xFF27221F), Offset(x * cell, y * cell),
                androidx.compose.ui.geometry.Size(cell, cell))
        }
    }
}

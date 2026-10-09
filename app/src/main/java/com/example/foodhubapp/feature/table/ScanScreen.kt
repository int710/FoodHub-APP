package com.example.foodhubapp.feature.table

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.foodhubapp.feature.table.data.TableScanException
import com.example.foodhubapp.feature.table.data.TableScanRepository
import com.example.foodhubapp.feature.table.data.TableSession
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val Gold = Color(0xFFFFB600)

@Composable
fun ScanScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit = {},
    cameraEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val repository = remember { TableScanRepository(context) }
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var flashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var session by remember { mutableStateOf<TableSession?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanRevision by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            scanQrFromImage(context, it) { rawValue ->
                if (rawValue == null) {
                    errorMessage = "Không tìm thấy mã QR trong ảnh đã chọn"
                } else {
                    submitQr(
                        rawValue,
                        repository,
                        { isSubmitting = it },
                        { session = it },
                        { errorMessage = it },
                        scope,
                    )
                }
            }
        }
    }

    LaunchedEffect(cameraEnabled) {
        if (cameraEnabled && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(flashOn, camera) {
        val activeCamera = camera ?: return@LaunchedEffect
        if (activeCamera.cameraInfo.hasFlashUnit()) {
            activeCamera.cameraControl.enableTorch(flashOn)
        } else {
            flashOn = false
        }
    }

    val onQrDetected: (String) -> Unit = { rawValue ->
        if (!isSubmitting && session == null && errorMessage == null) {
            submitQr(
                rawValue,
                repository,
                { isSubmitting = it },
                { session = it },
                { errorMessage = it },
                scope,
            )
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF17181B))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphCircle("×", 44.dp, Color(0xCC121417), Color.White, 29.sp, onClick = onBack)
                Label("Quét QR", 18.sp, Color.White, bold = true)
                GlyphCircle(
                    if (flashOn) "ϟ" else "♜",
                    44.dp,
                    Color(0xCC121417),
                    Color.White,
                    onClick = { if (hasCameraPermission) flashOn = !flashOn },
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.background(Color(0xD91C1C1D), RoundedCornerShape(30.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    val status = when {
                        isSubmitting -> "●  ĐANG KIỂM TRA MÃ QR"
                        session != null -> "●  ĐÃ NHẬN DIỆN BÀN"
                        hasCameraPermission -> "●  CAMERA SẴN SÀNG"
                        else -> "●  CẦN QUYỀN CAMERA"
                    }
                    Label(status, 10.sp, Color(0xFFFFDB97), bold = true)
                }
                Spacer(Modifier.height(6.dp))
                Label("Quét mã QR tại bàn để gọi món", 19.sp, Color.White, bold = true, align = TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                Label(
                    "Đưa camera hướng vào mã QR dán trên mặt bàn để xác thực bàn và bắt đầu phiên gọi món",
                    12.sp,
                    Color(0xFFE2E2E4),
                    align = TextAlign.Center,
                )
            }

            Box(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 50.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScannerFrame(
                    hasCameraPermission,
                    cameraEnabled,
                    session == null && !isSubmitting && errorMessage == null,
                    scanRevision,
                    { camera = it },
                    onQrDetected,
                    { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
                if (isSubmitting) {
                    Box(
                        Modifier.size(78.dp).background(Color(0xCC17181B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Gold, modifier = Modifier.size(38.dp))
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                    .background(Color(0xF01B1C1F), RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScanTool("ϟ", if (flashOn) "Tắt flash" else "Đèn flash") {
                    if (hasCameraPermission) flashOn = !flashOn
                }
                ScanTool("▣", "Thư viện ảnh") { galleryLauncher.launch("image/*") }
                ScanTool("▦", "Quét lại") {
                    session = null
                    errorMessage = null
                    scanRevision++
                }
            }

            TableSessionCard(session, isSubmitting, onContinue)
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
                scanRevision++
            },
            title = { Label("Không thể vào bàn", 20.sp, bold = true) },
            text = { Label(message) },
            confirmButton = {
                TextButton(onClick = {
                    errorMessage = null
                    scanRevision++
                }) {
                    Label("Quét lại", color = BurntOrange, bold = true)
                }
            },
        )
    }
}

private fun submitQr(
    rawValue: String,
    repository: TableScanRepository,
    setLoading: (Boolean) -> Unit,
    onSuccess: (TableSession) -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope,
) {
    setLoading(true)
    scope.launch {
        runCatching { repository.scan(rawValue) }
            .onSuccess(onSuccess)
            .onFailure { error ->
                onError(
                    when (error) {
                        is TableScanException -> error.message ?: "Mã QR không hợp lệ"
                        is IOException -> "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại."
                        else -> "Không thể đọc mã QR. Vui lòng thử lại."
                    },
                )
            }
        setLoading(false)
    }
}

@Composable
private fun TableSessionCard(
    session: TableSession?,
    isLoading: Boolean,
    onContinue: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphCircle("♜", 36.dp, Color(0xFFF9ECE7), BurntOrange, 18.sp)
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Label("PHIÊN DÙNG BỮA", 10.sp, Color(0xFF8B5E50), bold = true)
                Label(if (session == null) "Chưa kết nối bàn" else "FoodHub", 16.sp, Ink, bold = true)
            }
            Box(
                Modifier.background(
                    if (session == null) Color(0xFFF0F0F2) else Color(0xFFB2FFD9),
                    RoundedCornerShape(24.dp),
                ).padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Label(
                    if (session == null) "Chờ quét" else "● Đã kết nối",
                    10.sp,
                    if (session == null) Muted else Color(0xFF0B6C43),
                    bold = true,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().background(PaleBlue, RoundedCornerShape(18.dp)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(Gold, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Label("▤", 28.sp, Color(0xFF6C4200), bold = true)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Label(
                        if (session == null) "Hướng camera vào mã QR" else "Đã xác thực với máy chủ",
                        10.sp,
                        Color(0xFF775D55),
                    )
                    Label(session?.tableName ?: "Đang chờ nhận diện bàn", 19.sp, Ink, bold = true)
                    Label(session?.floor ?: "Thông tin bàn sẽ xuất hiện sau khi quét", 11.sp, Color(0xFF565A64))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onContinue,
                enabled = session != null && !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrange),
                shape = RoundedCornerShape(13.dp),
            ) {
                Label(
                    if (isLoading) "Đang xác thực..." else "Xác nhận vào bàn ngay  →",
                    14.sp,
                    Color.White,
                    bold = true,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Label(
            "♢  Phiên bàn được bảo vệ bằng mã xác thực riêng",
            11.sp,
            Color(0xFF5D5960),
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center,
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
private fun ScannerFrame(
    hasCameraPermission: Boolean,
    cameraEnabled: Boolean,
    scanEnabled: Boolean,
    scanRevision: Int,
    onCameraReady: (Camera) -> Unit,
    onQrDetected: (String) -> Unit,
    onRequestPermission: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0xFF242529)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !cameraEnabled -> CameraPlaceholder()
            hasCameraPermission -> QrCameraPreview(
                scanEnabled,
                scanRevision,
                onCameraReady,
                onQrDetected,
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Label("Cần quyền camera để quét mã QR", 13.sp, Color.White, align = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRequestPermission) {
                    Label("Cấp quyền camera", color = Gold, bold = true)
                }
            }
        }

        ScannerOverlay(Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                .background(Color(0xCC171717), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Label("▦  Căn chỉnh mã trong khung", 10.sp, Color.White, bold = true)
        }
    }
}

@Composable
@SuppressLint("UnsafeOptInUsageError")
private fun QrCameraPreview(
    scanEnabled: Boolean,
    scanRevision: Int,
    onCameraReady: (Camera) -> Unit,
    onQrDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentScanEnabled by rememberUpdatedState(scanEnabled)
    val currentOnQrDetected by rememberUpdatedState(onQrDetected)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    val processing = remember { AtomicBoolean(false) }
    val delivered = remember { AtomicBoolean(false) }

    LaunchedEffect(scanRevision) { delivered.set(false) }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                if (!currentScanEnabled || delivered.get() || !processing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    processing.set(false)
                    imageProxy.close()
                    return@setAnalyzer
                }
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val value = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                        if (value != null && delivered.compareAndSet(false, true)) {
                            currentOnQrDetected(value)
                        }
                    }
                    .addOnCompleteListener {
                        processing.set(false)
                        imageProxy.close()
                    }
            }

            provider?.unbindAll()
            provider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )?.let(onCameraReady)
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            provider?.unbindAll()
            analyzerExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val x0 = w * .07f
        val x1 = w * .93f
        val y0 = h * .06f
        val y1 = h * .94f
        val len = 22.dp.toPx()
        val stroke = 4.dp.toPx()
        listOf(
            Offset(x0, y0 + len) to Offset(x0, y0),
            Offset(x0, y0) to Offset(x0 + len, y0),
            Offset(x1 - len, y0) to Offset(x1, y0),
            Offset(x1, y0) to Offset(x1, y0 + len),
            Offset(x0, y1 - len) to Offset(x0, y1),
            Offset(x0, y1) to Offset(x0 + len, y1),
            Offset(x1 - len, y1) to Offset(x1, y1),
            Offset(x1, y1) to Offset(x1, y1 - len),
        ).forEach { (start, end) -> drawLine(Gold, start, end, stroke, cap = StrokeCap.Round) }
        drawRect(
            Color(0x66FFFFFF),
            topLeft = Offset(w * .18f, h * .20f),
            size = androidx.compose.ui.geometry.Size(w * .64f, h * .60f),
            style = Stroke(1.dp.toPx()),
        )
        drawLine(Color(0xAAFFB600), Offset(w * .18f, h * .50f), Offset(w * .82f, h * .50f), 2.dp.toPx())
    }
}

@Composable
private fun CameraPlaceholder() {
    Box(Modifier.fillMaxSize().background(Color(0xFF34353A)), contentAlignment = Alignment.Center) {
        Label("Camera preview", 14.sp, Color.White)
    }
}

private fun scanQrFromImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
    )
    runCatching { InputImage.fromFilePath(context, uri) }
        .onSuccess { image ->
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    onResult(barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) })
                }
                .addOnFailureListener { onResult(null) }
                .addOnCompleteListener { scanner.close() }
        }
        .onFailure {
            scanner.close()
            onResult(null)
        }
}

@ComposePreview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScanScreenPreview() {
    ScanScreen(onContinue = {}, cameraEnabled = false)
}

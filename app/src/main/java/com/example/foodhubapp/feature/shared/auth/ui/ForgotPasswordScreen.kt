package com.example.foodhubapp.feature.shared.auth.ui

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.shared.auth.viewmodel.ForgotPasswordUiState
import com.example.foodhubapp.feature.shared.auth.viewmodel.ForgotPasswordViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.BodyFont
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.CaptionBrown
import com.example.foodhubapp.theme.FoodHubAppTheme
import com.example.foodhubapp.theme.HeadingFont
import com.example.foodhubapp.theme.InputBackgroundSoft
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.PrimaryContainer
import com.example.foodhubapp.theme.SuccessSoft
import com.example.foodhubapp.theme.Warning

@Composable
fun ForgotPasswordRoute(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onResetTokenClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        uiState = state,
        onEmailChange = viewModel::updateEmail,
        onBackClick = onBackClick,
        onLoginClick = onLoginClick,
        onRegisterClick = onRegisterClick,
        onSendLinkClick = viewModel::sendResetLink,
        onResetTokenClick = onResetTokenClick,
        modifier = modifier)
}

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onSendLinkClick: () -> Unit,
    onResetTokenClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = AppBackground,
        topBar = {
            Surface(color = AppBackground, shadowElevation = 1.dp) {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconCircleButton(
                        R.drawable.ic_forgot_back,
                        "Quay lại",
                        color = PrimaryContainer,
                        onBackClick)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecoveryText("Reset Password", weight = FontWeight.SemiBold, size = 14)
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Brand, CircleShape),
                            contentAlignment = Alignment.Center) {
                            RecoveryIcon(R.drawable.ic_forgot_person, 12)
                        }
                    }
                }
            }
        }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier
                    .widthIn(max = 448.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier
                        .size(width = 100.dp, height = 100.dp)
                        .drawWithCache {
                            val glowRadius = 120.dp.toPx()
                            val glowCenter = Offset(size.width / 2f, size.height / 2f)
                            val glow = Brush.radialGradient(
                                0f to BrandSoft.copy(alpha = 0.75f),
                                0.4f to BrandSoft.copy(alpha = 0.55f),
                                0.75f to BrandSoft.copy(alpha = 0.18f),
                                1f to Color.Transparent,
                                center = glowCenter,
                                radius = glowRadius)
                            // Draw beyond the compact layout so the logo does not hide the glow.
                            onDrawBehind {
                                drawCircle(
                                    brush = glow, radius = glowRadius, center = glowCenter)
                            }
                        })
                    Box(Modifier.size(84.dp)) {
                        Surface(
                            Modifier.size(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 3.dp) {
                            Image(
                                painterResource(R.drawable.foodhub_logo),
                                "FoodHub",
                                Modifier
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp)))
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .background(Warning, CircleShape),
                            contentAlignment = Alignment.Center) {
                            RecoveryIcon(R.drawable.ic_forgot_badge, 14)
                        }
                    }
                }
                Text(
                    "Quên mật khẩu? 🔐",
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Neutral,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                RecoveryText(
                    "Đừng lo lắng! Nhập email đã đăng ký để nhận đường dẫn đặt lại mật khẩu FoodHub.",
                    align = TextAlign.Center)
            }
            Column(Modifier
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 1.dp) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(InputBackgroundSoft, RoundedCornerShape(16.dp))
                                .padding(4.dp)) {
                            Surface(
                                Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center) {
                                    RecoveryIcon(R.drawable.ic_forgot_email, 16)
                                    Spacer(Modifier.width(6.dp))
                                    RecoveryText(
                                        "Email đăng ký",
                                        color = Brand,
                                        weight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecoveryText("Email tài khoản", weight = FontWeight.SemiBold)
                            BasicTextField(
                                value = uiState.email,
                                onValueChange = onEmailChange,
                                enabled = !uiState.isLoading,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                textStyle = TextStyle(
                                    fontFamily = BodyFont, fontSize = 14.sp, color = Neutral),
                                cursorBrush = SolidColor(Brand),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(InputBackgroundSoft, RoundedCornerShape(12.dp))
                                    .semantics { contentDescription = "Email tài khoản" },
                                decorationBox = { input ->
                                    Row(
                                        Modifier.padding(start = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) {
                                            if (uiState.email.isEmpty()) RecoveryText(
                                                "email@example.com",
                                                color = CaptionBrown,
                                                size = 14)
                                            input()
                                        }
                                        IconButton(
                                            onClick = { onEmailChange("") },
                                            enabled = !uiState.isLoading && uiState.email.isNotEmpty()) {
                                            RecoveryIcon(R.drawable.ic_forgot_clear, 15)
                                        }
                                    }
                                })
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(InputBackgroundSoft, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(SuccessSoft, CircleShape),
                                contentAlignment = Alignment.Center) {
                                RecoveryIcon(R.drawable.ic_forgot_sent, 15)
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                RecoveryText(
                                    if (uiState.isRequestSent) "Yêu cầu đã được gửi" else "Khôi phục tài khoản qua email",
                                    color = Neutral,
                                    weight = FontWeight.Bold)
                                RecoveryText(
                                    if (uiState.isRequestSent) "Nếu email đã được đăng ký, bạn sẽ nhận được đường dẫn đặt lại mật khẩu. Hãy kiểm tra hộp thư và thư rác."
                                    else "Chúng tôi sẽ gửi đường dẫn đặt lại mật khẩu tới email đã đăng ký.",
                                    size = 11)
                            }
                        }
                        if (uiState.errorMessage != null) {
                            RecoveryText(
                                uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = onSendLinkClick,
                            enabled = uiState.canSend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Brand,
                                disabledContainerColor = Brand.copy(alpha = 0.3f))) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            RecoveryText(
                                if (uiState.isLoading) "Đang gửi..."
                                else if (uiState.isRequestSent) "Gửi lại link đặt lại mật khẩu"
                                else "Gửi link đặt lại mật khẩu",
                                color = Color.White,
                                weight = FontWeight.Bold,
                                size = 14)
                            if (!uiState.isLoading) {
                                Spacer(Modifier.width(8.dp))
                                RecoveryIcon(R.drawable.ic_forgot_arrow, 12)
                            }
                        }
                        TextButton(onClick = onResetTokenClick) {
                            Text("Tôi đã có token đặt lại mật khẩu")
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp, Alignment.CenterHorizontally)) {
                            Box(Modifier
                                .size(6.dp)
                                .background(Warning, CircleShape))
                            RecoveryText(
                                "Không chia sẻ đường dẫn đặt lại mật khẩu",
                                size = 11,
                                align = TextAlign.Center,
                                modifier = Modifier.weight(1f))
                            Box(Modifier
                                .size(6.dp)
                                .background(Warning, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                RecoveryLink("Đã nhớ lại mật khẩu?", "Đăng nhập ngay", onLoginClick)
                RecoveryLink("Chưa có tài khoản?", "Tạo tài khoản mới", onRegisterClick)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RecoveryIcon(@DrawableRes resource: Int, size: Int) {
    Image(painterResource(resource), contentDescription = null, modifier = Modifier.size(size.dp))
}

@Composable
private fun RecoveryText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OnSurfaceVariant,
    weight: FontWeight = FontWeight.Normal,
    size: Int = 12,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontFamily = BodyFont,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * 1.6f).sp,
        textAlign = align)
}

@Composable
private fun RecoveryLink(label: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        RecoveryText(label, size = 11)
        TextButton(
            onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
            RecoveryText(
                action, color = Brand, weight = FontWeight.Bold, size = 11)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun ForgotPasswordPreview() {
    FoodHubAppTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(email = "user@example.com"),
            onEmailChange = {},
            onBackClick = {},
            onLoginClick = {},
            onRegisterClick = {},
            onSendLinkClick = {},
            )
    }
}

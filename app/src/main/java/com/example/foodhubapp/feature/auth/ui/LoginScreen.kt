package com.example.foodhubapp.feature.auth.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.auth.model.AuthMode
import com.example.foodhubapp.feature.auth.viewmodel.LoginUiState
import com.example.foodhubapp.feature.auth.viewmodel.LoginViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.BodyFont
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.FoodHubAppTheme
import com.example.foodhubapp.theme.InputBackgroundSoft
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.Warning

@Composable
fun LoginRoute(
    onBackClick: () -> Unit,
    onLoginClick: (String?) -> Unit,
    onRegisterClick: () -> Unit,
    onGuestQrClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onVerifyEmailClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginClick(uiState.userRole)
        }
    }

    LoginScreen(
        uiState = uiState,
        onAccountChange = viewModel::onAccountChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onBackClick = onBackClick,
        onLoginClick = viewModel::login,
        onRegisterClick = onRegisterClick,
        onGuestQrClick = onGuestQrClick,
        onForgotPasswordClick = onForgotPasswordClick,
        onVerifyEmailClick = onVerifyEmailClick,
        modifier = modifier
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onGuestQrClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onVerifyEmailClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .size(288.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Brand.copy(alpha = 0.6f),
                            Warning.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(48.dp)
        )

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            AuthHeaderBar(onBackClick = onBackClick)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginBrandHero()
                        Spacer(modifier = Modifier.height(20.dp))
                        AuthSegmentedTabs(
                            selectedMode = AuthMode.Login,
                            onLoginClick = {},
                            onRegisterClick = onRegisterClick
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                LoginFormCard(
                    uiState = uiState,
                    onAccountChange = onAccountChange,
                    onPasswordChange = onPasswordChange,
                    onTogglePassword = onTogglePassword,
                    onLoginClick = onLoginClick,
                    onForgotPasswordClick = onForgotPasswordClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                GuestQrCard(onGuestQrClick = onGuestQrClick)
                Spacer(modifier = Modifier.height(24.dp))
                AuthSwitchText(
                    normalText = "Chưa có tài khoản?",
                    actionText = "Đăng ký ngay",
                    onClick = onRegisterClick
                )
                Text(
                    "Xác minh email bằng token",
                    modifier = Modifier.clickable(onClick = onVerifyEmailClick).padding(12.dp),
                    color = Brand,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LoginFormCard(
    uiState: LoginUiState,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField(
                label = "Email",
                value = uiState.account,
                onValueChange = onAccountChange,
                iconRes = R.drawable.ic_auth_email,
                backgroundColor = InputBackgroundSoft
            )
            AuthTextField(
                label = "Mật khẩu",
                value = uiState.password,
                onValueChange = onPasswordChange,
                iconRes = R.drawable.ic_auth_lock,
                isPassword = true,
                isPasswordVisible = uiState.isPasswordVisible,
                backgroundColor = InputBackgroundSoft,
                trailing = {
                    PasswordToggleButton(
                        isVisible = uiState.isPasswordVisible,
                        onClick = onTogglePassword
                    )
                }
            )
            Text(
                text = "Quên mật khẩu?",
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onForgotPasswordClick)
                    .padding(vertical = 4.dp),
                color = Brand,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = Brand,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            PrimaryAuthButton(
                text = if (uiState.isLoading) "Đang đăng nhập..." else "Đăng nhập ngay",
                onClick = onLoginClick,
                enabled = !uiState.isLoading
            )
        }
    }
}

@Composable
private fun GuestQrCard(onGuestQrClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            Warning.copy(alpha = 0.5f),
                            InputBackgroundSoft,
                            Color.White
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Warning, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_qr),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gọi món tại bàn không cần tài khoản?",
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                Text(
                    text = "Dành cho thực khách tại nhà hàng. Xem thực đơn và gọi món siêu tốc.",
                    color = OnSurfaceVariant,
                    fontFamily = BodyFont,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGuestQrClick() },
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_auth_camera),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Quét QR Dùng Bữa Ngay (Khách Vãng Lai)",
                            color = Brand,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AuthSwitchText(
    normalText: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = normalText,
            color = OnSurfaceVariant,
            fontFamily = BodyFont,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            modifier = Modifier.clickable { onClick() },
            text = actionText,
            color = Brand,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 948)
@Composable
private fun LoginScreenPreview() {
    FoodHubAppTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onAccountChange = {},
            onPasswordChange = {},
            onTogglePassword = {},
            onBackClick = {},
            onLoginClick = {},
            onRegisterClick = {},
            onGuestQrClick = {}
        )
    }
}

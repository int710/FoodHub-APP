package com.example.foodhubapp.feature.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.example.foodhubapp.ui.theme.CardStroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.auth.model.AuthMode
import com.example.foodhubapp.feature.auth.model.PasswordStrength
import com.example.foodhubapp.feature.auth.viewmodel.RegisterUiState
import com.example.foodhubapp.feature.auth.viewmodel.RegisterViewModel
import com.example.foodhubapp.ui.theme.AppBackground
import com.example.foodhubapp.ui.theme.BodyFont
import com.example.foodhubapp.ui.theme.Brand
import com.example.foodhubapp.ui.theme.CaptionBrown
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import com.example.foodhubapp.ui.theme.InputBackground
import com.example.foodhubapp.ui.theme.Neutral
import com.example.foodhubapp.ui.theme.OnSurfaceVariant
import com.example.foodhubapp.ui.theme.SuccessDark
import com.example.foodhubapp.ui.theme.Warning
import com.example.foodhubapp.ui.theme.WarningDark

@Composable
fun RegisterRoute(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegisterClick()
        }
    }

    RegisterScreen(
        uiState = uiState,
        onFullNameChange = viewModel::onFullNameChange,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onReferralCodeChange = viewModel::onReferralCodeChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onToggleTerms = viewModel::toggleTerms,
        onBackClick = onBackClick,
        onRegisterClick = viewModel::register,
        onLoginClick = onLoginClick,
        modifier = modifier
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleTerms: () -> Unit,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(AppBackground)
    ) {
        AuthHeaderBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            RegisterIntroCard(
                selectedMode = AuthMode.Register,
                onLoginClick = onLoginClick,
                onRegisterClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            RegisterFormCard(
                uiState = uiState,
                onFullNameChange = onFullNameChange,
                onPhoneNumberChange = onPhoneNumberChange,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onReferralCodeChange = onReferralCodeChange,
                onTogglePassword = onTogglePassword,
                onToggleTerms = onToggleTerms,
                onRegisterClick = onRegisterClick
            )
            Spacer(modifier = Modifier.height(16.dp))
            RewardsBanner()
            Spacer(modifier = Modifier.height(16.dp))
            DividerLabel(text = "Hoặc đăng ký bằng")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialButton(
                    text = "Google",
                    iconRes = R.drawable.ic_auth_google,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                SocialButton(
                    text = "Apple ID",
                    iconRes = R.drawable.ic_auth_apple,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            AuthSwitchText(
                normalText = "Đã có tài khoản?",
                actionText = "Đăng nhập ngay",
                onClick = onLoginClick
            )
        }
    }
}

@Composable
private fun RegisterFormCard(
    uiState: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleTerms: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthTextField(
                label = "Họ và tên",
                placeholder = "Nhập họ và tên",
                value = uiState.fullName,
                onValueChange = onFullNameChange,
                iconRes = R.drawable.ic_auth_person,
                backgroundColor = InputBackground
            )
            AuthTextField(
                label = "Số điện thoại",
                placeholder = "Nhập số điện thoại",
                value = uiState.phoneNumber,
                onValueChange = onPhoneNumberChange,
                iconRes = R.drawable.ic_auth_email,
                backgroundColor = InputBackground,
                prefix = {
                    Text(
                        text = "+84  |  ",
                        color = Neutral,
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                },
                trailing = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_auth_check_circle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            AuthTextField(
                label = "Email",
                value = uiState.email,
                onValueChange = onEmailChange,
                iconRes = R.drawable.ic_auth_email,
                placeholder = "Email nhận hóa đơn VAT",
                backgroundColor = InputBackground
            )
            AuthTextField(
                label = "Mật khẩu",
                value = uiState.password,
                onValueChange = onPasswordChange,
                iconRes = R.drawable.ic_auth_lock,
                isPassword = true,
                placeholder = "Tối thiểu 8 ký tự",
                isPasswordVisible = uiState.isPasswordVisible,
                backgroundColor = InputBackground,
                trailing = {
                    PasswordToggleButton(
                        isVisible = uiState.isPasswordVisible,
                        onClick = onTogglePassword
                    )
                },
                helper = {
                    PasswordStrengthMeter(strength = uiState.passwordStrength)
                }
            )
            AuthTextField(
                label = "Mã giới thiệu / Quán quen (Tùy chọn)",
                value = uiState.referralCode,
                onValueChange = onReferralCodeChange,
                iconRes = R.drawable.ic_auth_tag,
                placeholder = "Mã giới thiệu / Số bàn quán quen",
                backgroundColor = InputBackground,
                trailing = {
                    Surface(color = Warning, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "+30 Điểm",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = WarningDark,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTerms() },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (uiState.acceptedTerms) Brand else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (uiState.acceptedTerms) Brand else CardStroke,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.acceptedTerms) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_auth_check_circle),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
                TermsText()
            }
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
                text = if (uiState.isLoading) "Đang đăng ký..." else "Đăng Ký & Nhận Ưu Đãi",
                onClick = onRegisterClick,
                badgeText = "Tặng 50K",
                height = 56,
                enabled = !uiState.isLoading
            )
        }
    }
}

@Composable
private fun PasswordStrengthMeter(
    strength: PasswordStrength
) {
    val activeColor = when (strength) {
        PasswordStrength.EMPTY -> CardStroke
        PasswordStrength.WEAK -> Brand
        PasswordStrength.MEDIUM -> Warning
        PasswordStrength.STRONG -> SuccessDark
    }

    val label = when (strength) {
        PasswordStrength.EMPTY -> "Chưa nhập mật khẩu"
        PasswordStrength.WEAK -> "Yếu"
        PasswordStrength.MEDIUM -> "Trung bình"
        PasswordStrength.STRONG -> "Rất mạnh (An toàn)"
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val isActive = index < strength.level

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (isActive) activeColor else CardStroke,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Độ bảo mật:",
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Text(
                text = label,
                color = activeColor,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 948)
@Composable
private fun RegisterScreenPreview() {
    FoodHubAppTheme {
        RegisterScreen(
            uiState = RegisterUiState(),
            onFullNameChange = {},
            onPhoneNumberChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onReferralCodeChange = {},
            onTogglePassword = {},
            onToggleTerms = {},
            onBackClick = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}

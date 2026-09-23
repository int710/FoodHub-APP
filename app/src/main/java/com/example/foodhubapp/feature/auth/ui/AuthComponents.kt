package com.example.foodhubapp.feature.auth.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.auth.model.AuthMode
import com.example.foodhubapp.feature.auth.viewmodel.LoginUiState
import com.example.foodhubapp.ui.theme.BodyFont
import com.example.foodhubapp.ui.theme.Brand
import com.example.foodhubapp.ui.theme.BrandSoft
import com.example.foodhubapp.ui.theme.CaptionBrown
import com.example.foodhubapp.ui.theme.CardStroke
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import com.example.foodhubapp.ui.theme.HeadingFont
import com.example.foodhubapp.ui.theme.InputBackgroundSoft
import com.example.foodhubapp.ui.theme.Neutral
import com.example.foodhubapp.ui.theme.OnSurfaceVariant
import com.example.foodhubapp.ui.theme.Primary
import com.example.foodhubapp.ui.theme.PrimaryContainer
import com.example.foodhubapp.ui.theme.SoftGreen
import com.example.foodhubapp.ui.theme.Success
import com.example.foodhubapp.ui.theme.SuccessDark
import com.example.foodhubapp.ui.theme.SuccessSoft
import com.example.foodhubapp.ui.theme.SurfaceContainerLow
import com.example.foodhubapp.ui.theme.Warning
import com.example.foodhubapp.ui.theme.WarningDark

@Composable
internal fun AuthHeaderBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(
                iconRes = R.drawable.ic_auth_back,
                contentDescription = "Quay lại",
                color = PrimaryContainer,
                onClick = onBackClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            SmallFoodHubLogo()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FoodHub",
                color = Neutral,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                color = Brand,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
internal fun LoginBrandHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Primary, Color(0xFFFF7A35)))),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "FoodHub",
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp) // Khoảng cách giữa chữ và icon
        ) {
            Text(
                text = "FoodHub",
                color = Neutral,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_onboarding_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Chào mừng bạn trở lại! Đăng nhập để nhận ưu\nđãi bàn và tích điểm Rewards.",
            color = OnSurfaceVariant,
            fontFamily = BodyFont,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun RegisterIntroCard(
    selectedMode: AuthMode,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(128.dp)
                    .background(BrandSoft.copy(alpha = 0.45f), CircleShape)
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = SoftGreen, shape = CircleShape) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_auth_gift),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Quà Tặng Chào Bạn Mới",
                            color = Color(0xFF002113),
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                Text(
                    text = "Tạo Tài Khoản Mới",
                    color = Neutral,
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 34.sp
                )
                Text(
                    text = "Đăng ký thành viên để nhận voucher 50K và tích lũy\nđiểm thưởng ẩm thực mỗi lần gọi món.",
                    color = OnSurfaceVariant,
                    fontFamily = BodyFont,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                AuthSegmentedTabs(
                    selectedMode = selectedMode,
                    onLoginClick = onLoginClick,
                    onRegisterClick = onRegisterClick
                )
            }
        }
    }
}

@Composable
internal fun AuthSegmentedTabs(
    selectedMode: AuthMode,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFE5E8F0),
        shape = CircleShape,
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            AuthTabButton(
                text = "Đăng nhập",
                iconRes = R.drawable.ic_auth_login,
                selected = selectedMode == AuthMode.Login,
                onClick = onLoginClick,
                modifier = Modifier.weight(1f)
            )
            AuthTabButton(
                text = "Đăng ký",
                iconRes = R.drawable.ic_auth_user_plus,
                selected = selectedMode == AuthMode.Register,
                onClick = onRegisterClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthTabButton(
    text: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (selected) Color.White else Color.Transparent,
        shape = CircleShape,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (selected) Brand else OnSurfaceVariant,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
internal fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailing: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = true,
    backgroundColor: Color = InputBackgroundSoft,
    helper: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                prefix?.invoke()
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    cursorBrush = SolidColor(Brand),
                    textStyle = TextStyle(
                        color = if (value.isBlank()) CaptionBrown else Neutral,
                        fontFamily = BodyFont,
                        fontSize = 14.sp
                    ),
                    visualTransformation = if (isPassword && !isPasswordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    decorationBox = { innerTextField ->
                        if (value.isBlank() && placeholder.isNotBlank()) {
                            Text(
                                text = placeholder,
                                color = CaptionBrown,
                                fontFamily = BodyFont,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                )
                trailing?.invoke()
            }
        }
        helper?.invoke()
    }
}

@Composable
internal fun PasswordToggleButton(
    isVisible: Boolean,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = if (isVisible) R.drawable.ic_auth_eye else R.drawable.ic_auth_eye_off),
        contentDescription = if (isVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
        modifier = Modifier
            .size(22.dp)
            .clickable { onClick() }
    )
}

@Composable
internal fun PrimaryAuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    height: Int = 48,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .shadow(8.dp, RoundedCornerShape(if (height > 48) 28.dp else 12.dp))
            .clickable(enabled = enabled) { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(if (height > 48) 28.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Brand, PrimaryContainer)))
                .padding(horizontal = 24.dp),
            horizontalArrangement = if (badgeText == null) Arrangement.Center else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    color = Color.White,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_arrow_right),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (badgeText != null) {
                Surface(color = Warning, shape = CircleShape) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = WarningDark,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun AuthCheckRow(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onTrailingClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (checked) Brand else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (checked) Brand else CardStroke,
                        shape = RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_auth_check_circle),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (trailingText != null) {
            Text(
                modifier = Modifier.clickable { onTrailingClick() },
                text = trailingText,
                color = Brand,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun DividerLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CardStroke)
        )
        Text(
            text = text,
            color = OnSurfaceVariant,
            fontFamily = BodyFont,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CardStroke)
        )
    }
}

@Composable
internal fun SocialButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Neutral
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardStroke),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = textColor,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SmallFoodHubLogo() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Primary, Color(0xFFFF7A35)))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun IconCircleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    color :  Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(color)

        )
    }
}

@Composable
internal fun TermsText() {
    Text(
        text = buildAnnotatedString {
            append("Tôi đồng ý với ")
            withStyle(SpanStyle(color = Brand, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                append("Điều khoản dịch vụ")
            }
            append(" & ")
            withStyle(SpanStyle(color = Brand, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                append("Chính sách bảo mật")
            }
            append(" của FoodHub.")
        },
        color = OnSurfaceVariant,
        fontFamily = BodyFont,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
}

@Composable
internal fun RewardsBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SuccessSoft,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Success),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_auth_gift),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Đặc quyền FoodHub Rewards",
                    color = SuccessDark,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Tặng ngay 100 điểm thưởng và Voucher giảm\n30% cho lần quét mã gọi món đầu tiên tại bàn.",
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
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
            onToggleRemember = {},
            onBackClick = {},
            onLoginClick = {},
            onRegisterClick = {},
            onGuestQrClick = {}
        )
    }
}

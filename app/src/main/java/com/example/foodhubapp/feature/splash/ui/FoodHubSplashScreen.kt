package com.example.foodhubapp.feature.splash.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.splash.viewmodel.FoodHubSplashUiState
import com.example.foodhubapp.feature.splash.viewmodel.FoodHubSplashViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.BodyFont
import com.example.foodhubapp.theme.FoodHubAppTheme
import com.example.foodhubapp.theme.HeadingFont
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.OutlineVariant
import com.example.foodhubapp.theme.Primary
import com.example.foodhubapp.theme.PrimaryContainer
import com.example.foodhubapp.theme.Secondary
import com.example.foodhubapp.theme.SurfaceContainerLow
import com.example.foodhubapp.theme.SurfaceVariant
import com.example.foodhubapp.theme.Tertiary
import kotlinx.coroutines.delay

@Composable
fun FoodHubSplashRoute(
    onSplashFinished: (isAuthenticated: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodHubSplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoading, uiState.isAuthenticated) {
        val isAuthenticated = uiState.isAuthenticated

        if (!uiState.isLoading && isAuthenticated != null) {
            delay(450)
            onSplashFinished(isAuthenticated)
        }
    }

    FoodHubSplashScreen(
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
fun FoodHubSplashScreen(
    uiState: FoodHubSplashUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SoftBackgroundGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 52.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopStatusRow()
            Spacer(modifier = Modifier.height(158.dp))
            FoodHubLogo()
            Spacer(modifier = Modifier.height(26.dp))
            FoodHubTitle()
            Spacer(modifier = Modifier.height(16.dp))
            FoodHubTagline()
            Spacer(modifier = Modifier.height(18.dp))
            FeatureChips()
            Spacer(modifier = Modifier.weight(1f))
            LoadingStatus(uiState = uiState)
            Spacer(modifier = Modifier.height(18.dp))
            InfoCard()
        }
    }
}

@Composable
private fun BoxScope.SoftBackgroundGlow() {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 165.dp)
            .size(280.dp)
            .blur(90.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Secondary.copy(alpha = 0.4f),
                        Secondary.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .size(190.dp)
            .blur(90.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Secondary.copy(alpha = 0.4f),
                        Secondary.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(190.dp)
            .blur(90.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Secondary.copy(alpha = 0.2f),
                        Secondary.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )
}

@Composable
private fun TopStatusRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusPill(
            leading = {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Tertiary, CircleShape)
                )
            },
            text = "HỆ THỐNG BÀN SẴN SÀNG",
            textColor = Color(0xFF006947)
        )

        StatusPill(
            leading = {
                Text(
                    text = "⌗",
                    color = Color(0xFF7E5700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = "Smart QR 2.0",
            textColor = Neutral
        )
    }
}

@Composable
private fun StatusPill(
    leading: @Composable () -> Unit,
    text: String,
    textColor: Color
) {
    Surface(
        modifier = Modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = Neutral.copy(alpha = 0.08f),
            spotColor = Primary.copy(alpha = 0.03f)
        ),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Neutral.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            leading()
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = BodyFont,
                letterSpacing = 0.24.sp
            )
        }
    }
}

@Composable
private fun FoodHubLogo() {
    Image(
        painter = painterResource(id = R.drawable.foodhub_logo),
        contentDescription = "Logo FoodHub",
        modifier = Modifier
            .size(112.dp)
            .shadow(
                elevation = 28.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Primary.copy(alpha = 0.32f),
                spotColor = Primary.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(28.dp))
    )
}

@Composable
private fun FoodHubTitle() {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Neutral)) {
                append("Food")
            }
            withStyle(SpanStyle(color = Primary)) {
                append("Hub •")
            }
        },
        fontSize = 29.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = HeadingFont,
        letterSpacing = 0.sp
    )
}

@Composable
private fun FoodHubTagline() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chạm Để Gọi\nMón",
                color = OnSurfaceVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = HeadingFont,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .size(5.dp)
                    .background(OutlineVariant, CircleShape)
            )
            Text(
                text = "Thưởng Thức Trọn\nVẹn",
                color = Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = HeadingFont,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Ứng dụng gọi món thông minh tại bàn & giao tận nơi\nhàng đầu Việt Nam",
            color = OnSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            fontFamily = BodyFont
        )
    }
}

@Composable
private fun FeatureChips() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeatureChip(icon = "▱", text = "Dine-In QR", color = Primary)
        Spacer(modifier = Modifier.width(10.dp))
        FeatureChip(icon = "▵", text = "Takeaway", color = Color(0xFF7E5700))
        Spacer(modifier = Modifier.width(10.dp))
        FeatureChip(icon = "⌁", text = "Fast Delivery", color = Tertiary)
    }
}

@Composable
private fun FeatureChip(icon: String, text: String, color: Color) {
    Surface(
        color = SurfaceVariant,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Neutral.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = icon,
                color = color,
                fontWeight = FontWeight.Bold,
                fontFamily = BodyFont,
                fontSize = 13.sp
            )
            Text(
                text = text,
                color = Neutral,
                fontWeight = FontWeight.SemiBold,
                fontFamily = BodyFont,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LoadingStatus(uiState: FoodHubSplashUiState) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressFraction,
        animationSpec = tween(durationMillis = 450),
        label = "Splash loading progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(SurfaceContainerLow)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Secondary, PrimaryContainer)))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Tertiary, CircleShape)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = uiState.loadingMessage,
                color = OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = BodyFont
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${uiState.progressPercent.coerceIn(0, 100)}%",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = BodyFont
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Neutral.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "⌁", color = Tertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phiên bản 3.4.2 • Kết nối Realtime Socket.IO",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontFamily = BodyFont
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "♡", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bảo mật PCI-DSS",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = BodyFont
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(OutlineVariant, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "⚒", color = Color(0xFF7E5700), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FoodHub Cloud Tech",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = BodyFont
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 430, heightDp = 932)
@Composable
private fun FoodHubSplashScreenPreview() {
    FoodHubAppTheme {
        FoodHubSplashScreen(
            uiState = FoodHubSplashUiState(
                loadingMessage = "Đang đồng bộ thực đơn bàn...",
                progressPercent = 97
            )
        )
    }
}

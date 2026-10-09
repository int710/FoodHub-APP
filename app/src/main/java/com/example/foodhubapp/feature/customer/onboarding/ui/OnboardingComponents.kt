package com.example.foodhubapp.feature.customer.onboarding.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.R
import com.example.foodhubapp.theme.BodyFont
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandDark
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.CardStroke
import com.example.foodhubapp.theme.HeadingFont
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.SoftGreen
import com.example.foodhubapp.theme.SurfaceContainerLow
import com.example.foodhubapp.theme.WarmAccent

@Composable
internal fun OnboardingTopBar(onSkipClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(1.dp, CircleShape)
                    .background(Brand, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "FoodHub",
                color = Neutral,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp
            )
        }

        Surface(
            modifier = Modifier.clickable { onSkipClick() },
            color = SurfaceContainerLow,
            shape = CircleShape
        ) {
            Text(
                text = "Bỏ qua",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun OnboardingHeadline() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = BrandSoft,
            shape = CircleShape
        ) {
            Text(
                text = "Trải Nghiệm Ẩm Thực Thông Minh",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = BrandDark,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Gọi Món Tại Bàn Trong\nTích Tắc",
            color = Neutral,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Chỉ cần quét mã QR tại bàn, tùy chỉnh topping\ntheo khẩu vị riêng và theo dõi tiến trình đầu bếp\nchế biến trực tiếp theo thời gian thực.",
            color = OnSurfaceVariant,
            fontFamily = BodyFont,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun FeatureList() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureRow(
            iconBackground = BrandSoft,
            title = "Quét QR vào bàn tức thì",
            subtitle = "Nhận diện số bàn tự động, không chờ phục vụ",
            iconRes = R.drawable.ic_onboarding_lightning
        )
        FeatureRow(
            iconBackground = SoftGreen,
            title = "Theo dõi bếp nấu Realtime",
            subtitle = "Cập nhật chính xác từng giai đoạn lên món",
            iconRes = R.drawable.ic_onboarding_timer
        )
        FeatureRow(
            iconBackground = WarmAccent,
            title = "Thanh toán 1 chạm tiện lợi",
            subtitle = "Tích hợp VNPay, MoMo & Thẻ không tiền mặt",
            iconRes = R.drawable.ic_onboarding_card
        )
    }
}

@Composable
private fun FeatureRow(
    iconBackground: Color,
    title: String,
    subtitle: String,
    iconRes: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardStroke),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = OnSurfaceVariant,
                    fontFamily = BodyFont,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_onboarding_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(width = 6.dp, height = 9.dp)
            )
        }
    }
}

@Composable
internal fun BottomActions(
    onStartClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onStartClick() },
            color = Brand,
            shape = CircleShape,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bắt đầu trải nghiệm ngay",
                    color = Color.White,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_arrow_right),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đã có tài khoản?",
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                modifier = Modifier.clickable { onLoginClick() },
                text = "Đăng nhập ngay",
                color = Brand,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

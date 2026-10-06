package com.example.foodhubapp.feature.customer.onboarding.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.customer.onboarding.model.OnboardingHeroPage
import com.example.foodhubapp.theme.BodyFont
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandDark
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.MutedDot
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.SurfaceContainerLow
import com.example.foodhubapp.theme.WarmAccent

@Composable
internal fun OnboardingHeroPager(
    pages: List<OnboardingHeroPage>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val page = pages[pagerState.currentPage.coerceIn(pages.indices)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(304.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandSoft.copy(alpha = 0.4f),
                        SurfaceContainerLow,
                        Color(0xFFEBEEF6)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-56).dp)
                .size(176.dp)
                .blur(20.dp)
                .background(BrandSoft.copy(alpha = 0.6f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-48).dp, y = 52.dp)
                .size(144.dp)
                .blur(12.dp)
                .background(WarmAccent.copy(alpha = 0.5f), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroBadges(page = page)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(224.dp)
            ) { index ->
                HeroImageCard(page = pages[index])
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 2.dp)
                .size(40.dp)
                .rotate(6f)
                .shadow(6.dp, CircleShape)
                .background(Brand, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_onboarding_qr),
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun HeroBadges(page: OnboardingHeroPage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.9f),
            shape = CircleShape,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF006947), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = page.tableLabel,
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = " • ${page.areaLabel}",
                    color = Color(0xFF8F7066),
                    fontFamily = BodyFont,
                    fontSize = 11.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Surface(
            color = Color(0xFF00855B),
            shape = CircleShape,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_onboarding_realtime),
                    contentDescription = null,
                    modifier = Modifier.size(width = 12.dp, height = 9.dp)
                )
                Text(
                    text = page.statusLabel,
                    color = Color.White,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun HeroImageCard(page: OnboardingHeroPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth(),
            color = Color.White.copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BrandSoft, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = page.orderIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(width = 18.dp, height = 15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = page.orderTitle,
                            color = Neutral,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PriceBadge(text = page.progressLabel)
                    }
                    Text(
                        text = page.orderSubtitle,
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0x1A006947), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_onboarding_check_double),
                        contentDescription = null,
                        modifier = Modifier.size(width = 15.dp, height = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceBadge(text: String) {
    Box(
        modifier = Modifier
            .width(67.dp)
            .height(14.dp)
            .clip(CircleShape)
            .background(WarmAccent.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = BrandDark,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = page == currentPage
            Box(
                modifier = Modifier
                    .width(if (isSelected) 28.dp else 8.dp)
                    .height(8.dp)
                    .background(if (isSelected) Brand else MutedDot, CircleShape)
                    .clickable { onPageClick(page) }
            )
            if (page < pageCount - 1) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

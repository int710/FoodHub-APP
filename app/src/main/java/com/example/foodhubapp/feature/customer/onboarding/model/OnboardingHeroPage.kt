package com.example.foodhubapp.feature.customer.onboarding.model

import androidx.annotation.DrawableRes
import com.example.foodhubapp.R

data class OnboardingHeroPage(
    @DrawableRes val imageRes: Int,
    val tableLabel: String,
    val areaLabel: String,
    val statusLabel: String,
    @DrawableRes val orderIconRes: Int,
    val orderTitle: String,
    val orderSubtitle: String,
    val progressLabel: String
)

val onboardingHeroPages = listOf(
    OnboardingHeroPage(
        imageRes = R.drawable.onboarding_hero_foodhub,
        tableLabel = "Bàn #05",
        areaLabel = "Khu Vườn",
        statusLabel = "Bếp kết nối Realtime",
        orderIconRes = R.drawable.ic_onboarding_bowl,
        orderTitle = "Truffle Burger & Pizza",
        orderSubtitle = "Đầu bếp nhận đơn lúc 19:42",
        progressLabel = "Đã đặt"
    ),
    OnboardingHeroPage(
        imageRes = R.drawable.screen,
        tableLabel = "Bàn #12",
        areaLabel = "Tầng 2",
        statusLabel = "Đang chế biến",
        orderIconRes = R.drawable.ic_onboarding_timer,
        orderTitle = "Salmon Bowl & Iced Tea",
        orderSubtitle = "Bếp đang chuẩn bị món chính",
        progressLabel = "Đang làm"
    ),
    OnboardingHeroPage(
        imageRes = R.drawable.screen1,
        tableLabel = "Mang đi",
        areaLabel = "Quầy Pick-up",
        statusLabel = "Sẵn sàng nhận món",
        orderIconRes = R.drawable.ic_onboarding_card,
        orderTitle = "Coffee Combo & Fries",
        orderSubtitle = "Đơn hàng sẵn sàng trong 3 phút",
        progressLabel = "Sắp xong"
    )
)

package com.example.foodhubapp.feature.customer.profile.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.foodhubapp.R
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.InputBackground
import com.example.foodhubapp.theme.WarmAccent

data class ProfileUser(
    val name: String,
    val email: String,
    val role: String,
    val phone: String?,
    val phoneMasked: String,
    val dateOfBirth: String?,
    val isActive: Boolean,
    val isVerified: Boolean,
    val memberSince: String,
    @DrawableRes val avatarRes: Int
)

data class RewardsSummary(
    val points: String,
    val pointLabel: String,
    val equivalentAmount: String,
    val currentTier: String,
    val nextTier: String,
    val progress: Float,
    val remainingPoints: String
)

data class QuickAccessItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val iconBackground: Color,
    val badge: String? = null,
    val count: String? = null
)

data class SettingItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val subtitleHighlight: Boolean = false,
    val hasToggle: Boolean = false
)

data class ProfileUiModel(
    val user: ProfileUser,
    val rewards: RewardsSummary,
    val quickAccessItems: List<QuickAccessItem>,
    val tasteNotes: List<String>,
    val settingItems: List<SettingItem>
)

val previewProfile = ProfileUiModel(
    user = ProfileUser(
        name = "Thu Hà",
        email = "thuha@example.com",
        role = "CUSTOMER",
        phone = "0987123890",
        phoneMasked = "0987.xxx.890",
        dateOfBirth = "1998-04-20",
        isActive = true,
        isVerified = true,
        memberSince = "Khách hàng thân thiết từ 2022",
        avatarRes = R.drawable.avatar_user
    ),
    rewards = RewardsSummary(
        points = "1.250",
        pointLabel = "Điểm",
        equivalentAmount = "Tương đương 125.000đ khi thanh toán đơn món",
        currentTier = "Hạng Vàng (1.000)",
        nextTier = "Bạch Kim (1.500)",
        progress = 0.83f,
        remainingPoints = "Còn 250 điểm"
    ),
    quickAccessItems = listOf(
        QuickAccessItem(
            title = "Ví Voucher",
            subtitle = "Giảm tới 50k & Freeship",
            iconRes = R.drawable.ic_auth_tag,
            iconBackground = BrandSoft,
            badge = "4 khả dụng"
        ),
        QuickAccessItem(
            title = "Món yêu thích",
            subtitle = "Phở Thìn, Bún chả...",
            iconRes = R.drawable.ic_profile_heart,
            iconBackground = Color(0xFFFFDAD6),
            count = "8"
        ),
        QuickAccessItem(
            title = "Thanh toán",
            subtitle = "Liên kết 1 ví & 2 thẻ",
            iconRes = R.drawable.ic_onboarding_card,
            iconBackground = WarmAccent,
            badge = "VNPay"
        ),
        QuickAccessItem(
            title = "Hóa đơn VAT",
            subtitle = "Tải về hóa đơn điện tử",
            iconRes = R.drawable.ic_profile_receipt,
            iconBackground = InputBackground,
            count = "3"
        )
    ),
    tasteNotes = listOf("Không ăn hành lá", "Ăn cay ít (1/3 ớt)", "Nước dùng ít mỡ"),
    settingItems = listOf(
        SettingItem(
            title = "Thông tin cá nhân & Địa chỉ",
            subtitle = "Nhà riêng, Văn phòng Keangnam",
            iconRes = R.drawable.ic_profile_location
        ),
        SettingItem(
            title = "Trạng thái bếp & Ưu đãi",
            subtitle = "Nhận báo chuông khi món sẵn sàng",
            iconRes = R.drawable.ic_profile_bell,
            hasToggle = true
        ),
        SettingItem(
            title = "Hỗ trợ khách hàng & Hotline",
            subtitle = "1900 8888 (Phục vụ 24/7)",
            iconRes = R.drawable.ic_profile_phone,
            subtitleHighlight = true
        ),
        SettingItem(
            title = "Điều khoản & Chính sách bảo mật",
            subtitle = "Bảo vệ dữ liệu & quy định hoàn tiền",
            iconRes = R.drawable.ic_profile_shield
        )
    )
)

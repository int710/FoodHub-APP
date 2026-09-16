package com.example.foodhubapp.feature.profile.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.profile.model.ProfileUiModel
import com.example.foodhubapp.feature.profile.model.QuickAccessItem
import com.example.foodhubapp.feature.profile.model.SettingItem
import com.example.foodhubapp.feature.profile.model.previewProfile
import com.example.foodhubapp.feature.profile.viewmodel.ProfileUiState
import com.example.foodhubapp.feature.profile.viewmodel.ProfileViewModel
import com.example.foodhubapp.ui.theme.AppBackground
import com.example.foodhubapp.ui.theme.BodyFont
import com.example.foodhubapp.ui.theme.Brand
import com.example.foodhubapp.ui.theme.BrandDark
import com.example.foodhubapp.ui.theme.BrandSoft
import com.example.foodhubapp.ui.theme.CardStroke
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import com.example.foodhubapp.ui.theme.HeadingFont
import com.example.foodhubapp.ui.theme.InputBackground
import com.example.foodhubapp.ui.theme.Neutral
import com.example.foodhubapp.ui.theme.OnSurfaceVariant
import com.example.foodhubapp.ui.theme.Primary
import com.example.foodhubapp.ui.theme.PrimaryContainer
import com.example.foodhubapp.ui.theme.Success
import com.example.foodhubapp.ui.theme.SuccessDark
import com.example.foodhubapp.ui.theme.Warning
import com.example.foodhubapp.ui.theme.WarningDark

@Composable
fun ProfileRoute(
    onLogoutFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogoutFinished()
        }
    }

    ProfileScreen(
        uiState = uiState,
        onLogoutClick = viewModel::logout,
        modifier = modifier)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileHeaderCard(profile = uiState.profile)
        uiState.errorMessage?.let { ErrorMessageCard(message = it) }
        AccountInfoCard(profile = uiState.profile)
        LogoutButton(onClick = onLogoutClick)
    }
}

@Composable
private fun ProfileHeaderCard(profile: ProfileUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Image(
                    painter = painterResource(id = profile.user.avatarRes),
                    contentDescription = profile.user.name,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp),
                    color = Brand,
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.White)) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile_edit),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            colorFilter = ColorFilter.tint(Color.White))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.user.name,
                    color = Neutral,
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp)
                Text(
                    text = profile.user.phoneMasked,
                    color = OnSurfaceVariant,
                    fontFamily = BodyFont,
                    fontSize = 13.sp,
                    lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    GoldBadge(text = profile.user.role.toRoleLabel())
                    Text(
                        text = profile.user.memberSince,
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AccountInfoCard(profile: ProfileUiModel) {
    val user = profile.user

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardStroke),
        shadowElevation = 1.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(text = "Thông tin tài khoản")
            ProfileInfoRow(label = "Email", value = user.email)
            ProfileInfoRow(label = "Vai trò", value = user.role.toRoleLabel())
            ProfileInfoRow(label = "Ngày sinh", value = user.dateOfBirth ?: "Chưa cập nhật")
            ProfileInfoRow(
                label = "Trạng thái",
                value = if (user.isActive) "Đang hoạt động" else "Đã khóa")
            ProfileInfoRow(
                label = "Xác thực email",
                value = if (user.isVerified) "Đã xác thực" else "Chưa xác thực")
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = BodyFont,
            fontSize = 13.sp,
            lineHeight = 18.sp)
        Text(
            text = value,
            color = Neutral,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        )
    }
}

@Composable
private fun ErrorMessageCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BrandSoft,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB4A3))) {
        Text(
            text = message,
            color = BrandDark,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun RewardsCard(profile: ProfileUiModel) {
    val rewards = profile.rewards

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Brand, PrimaryContainer, Warning)))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "FoodHub Rewards",
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = rewards.points,
                            color = Color.White,
                            fontFamily = HeadingFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            lineHeight = 42.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = rewards.pointLabel,
                            color = Color.White,
                            fontFamily = BodyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 26.sp)
                    }
                    Text(
                        text = rewards.equivalentAmount,
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = BodyFont,
                        fontSize = 12.sp,
                        lineHeight = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_star),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        colorFilter = ColorFilter.tint(Color.White))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(rewards.progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(Color.White, CircleShape))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    RewardsText(text = rewards.currentTier)
                    RewardsText(text = rewards.nextTier)
                }
                Text(
                    text = rewards.remainingPoints,
                    color = Color.White,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun QuickAccessSection(items: List<QuickAccessItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = "Truy cập nhanh")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { item ->
                        QuickAccessCard(item = item, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: QuickAccessItem, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(128.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardStroke),
        shadowElevation = 1.dp) {
        Column(
            modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                IconBubble(
                    iconRes = item.iconRes, backgroundColor = item.iconBackground, iconTint = Brand)
                item.badge?.let { SmallPill(text = it) }
                item.count?.let { CountBubble(text = it) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.title,
                    color = Neutral,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text = item.subtitle,
                    color = OnSurfaceVariant,
                    fontFamily = BodyFont,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TastePreferencesCard(notes: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF6FFBBE), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_taste_tools),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Ghi chú khẩu vị cố định",
                    color = Neutral,
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f))
                Text(
                    text = "Tùy chỉnh",
                    color = Brand,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 14.sp)
            }
            Text(
                text = "Bếp sẽ tự động ghi nhớ và ưu tiên chế biến đúng yêu cầu\ncủa bạn cho mọi đơn ăn tại bàn hoặc mang về.",
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontSize = 12.sp,
                lineHeight = 16.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TasteNotePill(
                        text = notes.getOrElse(0) { "Không ăn hành lá" },
                        iconRes = R.drawable.ic_profile_no_food)
                    TasteNotePill(
                        text = notes.getOrElse(1) { "Ăn cay ít (1/3 ớt)" },
                        iconRes = R.drawable.ic_profile_spicy)
                }
                Row {
                    TasteNotePill(
                        text = notes.getOrElse(2) { "Nước dùng ít mỡ" },
                        iconRes = R.drawable.ic_profile_broth)
                }
            }
        }
    }

}

@Composable
private fun TasteNotePill(
    text: String,
    @DrawableRes iconRes: Int
) {
    Surface(
        color = Color(0xFFE9ECF2),
        shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp))
            Text(
                text = text,
                color = Neutral,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 1)
        }
    }
}



@Composable
private fun SettingsSection(
    items: List<SettingItem>,
    kitchenNotificationsEnabled: Boolean,
    onToggleKitchenNotifications: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardStroke),
        shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(text = "Cài đặt & Dịch vụ")
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, item ->
                SettingRow(
                    item = item,
                    checked = kitchenNotificationsEnabled,
                    onToggle = onToggleKitchenNotifications)
                if (index != items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardStroke))
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    item: SettingItem, checked: Boolean, onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconBubble(
            iconRes = item.iconRes, backgroundColor = InputBackground, iconTint = Brand)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Neutral,
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = item.subtitle,
                color = if (item.subtitleHighlight) Primary else OnSurfaceVariant,
                fontFamily = BodyFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        if (item.hasToggle) {
            Switch(
                checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Brand,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CardStroke,
                    uncheckedBorderColor = CardStroke))
        } else {
            Text(
                text = ">",
                color = OnSurfaceVariant,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp)
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable { onClick() },
        color = BrandSoft,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB4A3))) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_logout),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(Brand))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Đăng xuất",
                color = Brand,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Neutral,
        fontFamily = HeadingFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp)
}

@Composable
private fun IconBubble(
    @DrawableRes iconRes: Int, backgroundColor: Color, iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            colorFilter = ColorFilter.tint(iconTint))
    }
}

@Composable
private fun GoldBadge(text: String) {
    Surface(color = Warning, shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_star),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                colorFilter = ColorFilter.tint(WarningDark))
            Text(
                text = text,
                color = WarningDark,
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SmallPill(text: String) {
    Surface(color = BrandSoft, shape = CircleShape) {
        Text(
            text = text,
            modifier = Modifier
                .widthIn(max = 84.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Brand,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CountBubble(text: String) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(Brand, CircleShape),
        contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 14.sp)
    }
}

@Composable
private fun RewardsText(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.85f),
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp)
}

private fun String.toRoleLabel(): String {
    return when (uppercase()) {
        "ADMIN" -> "Quản trị viên"
        "STAFF" -> "Nhân viên"
        "CUSTOMER" -> "Khách hàng"
        else -> this
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 948)
@Composable
private fun ProfileScreenPreview() {
    FoodHubAppTheme {
        ProfileScreen(
            uiState = ProfileUiState(profile = previewProfile),
            onLogoutClick = {})
    }
}

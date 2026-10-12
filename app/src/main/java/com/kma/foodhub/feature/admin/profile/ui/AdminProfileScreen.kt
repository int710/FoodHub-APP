@file:Suppress("SameParameterValue")

package com.kma.foodhub.feature.admin.profile.ui

import com.kma.foodhub.core.ui.icons.TablerIcons

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kma.foodhub.feature.admin.navigation.AdminMuted
import com.kma.foodhub.feature.admin.navigation.AdminPrimary
import com.kma.foodhub.feature.admin.navigation.AdminSurface
import com.kma.foodhub.feature.shared.auth.model.UserDto
import com.kma.foodhub.theme.FoodHubAppTheme

@Composable
fun AdminProfileScreen(
    user: UserDto?,
    onReviews: () -> Unit,
    onVouchers: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.kma.foodhub.R.drawable.avatar_user),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            if (!user?.avatarUrl.isNullOrBlank()) {
                coil.compose.SubcomposeAsyncImage(
                    model = com.kma.foodhub.core.network.resolveMediaUrl(user.avatarUrl), contentDescription = "Ảnh đại diện admin",
                    modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                user?.fullName?.takeIf(String::isNotBlank) ?: "Quản trị viên FoodHub",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(user?.email.orEmpty(), color = AdminMuted)
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AdminProfileItem(TablerIcons.User, "Vai trò", user?.role ?: "ADMIN")
                user?.phoneNumber?.takeIf(String::isNotBlank)?.let { phone ->
                    HorizontalDivider()
                    AdminProfileItem(TablerIcons.User, "Số điện thoại", phone)
                }
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(16.dp)) {
            Column {
                AdminActionItem(TablerIcons.Star, "Quản lý đánh giá", "Xem đánh giá toàn hệ thống và theo món", onReviews)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                AdminActionItem(TablerIcons.Receipt, "Quản lý voucher", "Mã giảm giá, điều kiện và lượt sử dụng", onVouchers)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                AdminActionItem(TablerIcons.Adjustments, "Cài đặt hệ thống", "Thông tin ứng dụng, VAT và các loại phí", onSettings)
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Đăng xuất khỏi Admin") }
    }
}

@Composable
private fun AdminProfileItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AdminPrimary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = AdminMuted, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AdminActionItem(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(com.kma.foodhub.feature.admin.navigation.AdminPrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AdminPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = AdminMuted, fontSize = 12.sp)
        }
        Icon(TablerIcons.ChevronRight, null, tint = AdminMuted)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdminProfileScreenPreview() {
    FoodHubAppTheme {
        AdminProfileScreen(user = null, onReviews = {}, onVouchers = {}, onSettings = {}, onLogout = {})
    }
}


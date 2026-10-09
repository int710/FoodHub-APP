@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.profile.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminPrimary
import com.example.foodhubapp.feature.admin.navigation.AdminSurface
import com.example.foodhubapp.feature.shared.auth.model.UserDto
import com.example.foodhubapp.theme.FoodHubAppTheme

@Composable
fun AdminProfileScreen(user: UserDto?, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(84.dp).background(AdminPrimary, CircleShape), contentAlignment = Alignment.Center) {
            Text("AD", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                AdminProfileItem(Icons.Default.Person, "Vai trò", user?.role ?: "ADMIN")
                user?.phoneNumber?.takeIf(String::isNotBlank)?.let { phone ->
                    HorizontalDivider()
                    AdminProfileItem(Icons.Default.Person, "Số điện thoại", phone)
                }
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdminProfileScreenPreview() {
    FoodHubAppTheme {
        AdminProfileScreen(user = null, onLogout = {})
    }
}


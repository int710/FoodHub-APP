package com.example.foodhubapp.feature.customer.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.customer.profile.viewmodel.ProfileUiState
import com.example.foodhubapp.feature.customer.profile.viewmodel.ProfileViewModel
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.BodyFont
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.CardStroke
import com.example.foodhubapp.theme.HeadingFont
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant

@Composable
fun ProfileRoute(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    onMenuClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshProfile() }
    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }
    ProfileScreen(state, viewModel::logout, onMenuClick, viewModel::updateProfile)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogoutClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onUpdateProfile: (String, String) -> Unit = { _, _ -> },
) {
    var showEdit by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier
            .widthIn(max = 448.dp)
            .fillMaxWidth()) {
            Text(
                "Tài khoản",
                fontFamily = HeadingFont,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Neutral)
            Spacer(Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(BrandSoft, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.avatar_user),
                        "Ảnh đại diện mặc định",
                        Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(uiState.user?.fullName?.takeIf { it.isNotBlank() } ?: "Tài khoản FoodHub",
                        color = Neutral,
                        fontFamily = BodyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp)
                    Text(
                        "Thông tin cá nhân",
                        color = OnSurfaceVariant,
                        fontFamily = BodyFont,
                        fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            ProfileDetail("Email", uiState.user?.email)
            HorizontalDivider(color = CardStroke)
            ProfileDetail("Số điện thoại", uiState.user?.phoneNumber)
            OutlinedButton(onClick = { showEdit = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Chỉnh sửa thông tin")
            }
            Spacer(Modifier.height(24.dp))
            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(
                onClick = onMenuClick,
                modifier = Modifier.fillMaxWidth()) { Text("Thực đơn") }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onLogoutClick,
                enabled = !uiState.isLoggingOut && !uiState.isLoggedOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isLoggingOut) "Đang đăng xuất..." else "Đăng xuất",
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showEdit) {
        var name by remember(uiState.user?.fullName) { mutableStateOf(uiState.user?.fullName.orEmpty()) }
        var phone by remember(uiState.user?.phoneNumber) { mutableStateOf(uiState.user?.phoneNumber.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Thông tin cá nhân") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("Số điện thoại") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { onUpdateProfile(name, phone); showEdit = false }, enabled = name.isNotBlank()) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Hủy") } },
        )
    }
}

@Composable
private fun ProfileDetail(label: String, value: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = OnSurfaceVariant, fontFamily = BodyFont, fontSize = 12.sp)
        Text(value?.takeIf { it.isNotBlank() } ?: "Chưa có thông tin",
            color = Neutral,
            fontFamily = BodyFont,
            fontSize = 14.sp)
    }
}



package com.example.foodhubapp.feature.profile.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.profile.viewmodel.ProfileUiState
import com.example.foodhubapp.feature.profile.viewmodel.ProfileViewModel
import com.example.foodhubapp.ui.theme.*

@Composable
fun ProfileRoute(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    onMenuClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }
    ProfileScreen(state, viewModel::logout, onMenuClick)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogoutClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
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
                    Image(painterResource(R.drawable.ic_auth_person), null, Modifier.size(28.dp))
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



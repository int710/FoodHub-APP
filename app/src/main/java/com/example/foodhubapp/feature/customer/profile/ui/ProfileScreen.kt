package com.example.foodhubapp.feature.customer.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.SubcomposeAsyncImage
import com.example.foodhubapp.R
import com.example.foodhubapp.core.network.resolveMediaUrl
import com.example.foodhubapp.feature.shared.ui.FoodHubDialog
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
    ProfileScreen(state, viewModel::logout, onMenuClick, viewModel::updateProfile, viewModel::uploadAvatar)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogoutClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onUpdateProfile: (String, String, String, String, () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onUploadAvatar: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
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
                    ProfileAvatar(uiState.user?.avatarUrl)
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
            HorizontalDivider(color = CardStroke)
            ProfileDetail("Ngày sinh", uiState.user?.dateOfBirth)
            HorizontalDivider(color = CardStroke)
            ProfileDetail("Trạng thái email", if (uiState.user?.isVerified == true) "Đã xác minh" else "Chưa xác minh")
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
        var dateOfBirth by remember(uiState.user?.dateOfBirth) { mutableStateOf(uiState.user?.dateOfBirth.orEmpty()) }
        var avatarUrl by remember(uiState.user?.avatarUrl) { mutableStateOf(uiState.user?.avatarUrl.orEmpty()) }
        val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onUploadAvatar(it) { uploadedUrl -> avatarUrl = uploadedUrl } }
        }
        FoodHubDialog(
            onDismissRequest = { if (!uiState.isUploadingAvatar && !uiState.isUpdatingProfile) showEdit = false },
            title = "Thông tin cá nhân",
            eyebrow = "Hồ sơ tài khoản",
            dismissEnabled = !uiState.isUploadingAvatar && !uiState.isUpdatingProfile,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Box(
                        Modifier.size(76.dp).align(Alignment.CenterHorizontally).clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProfileAvatar(avatarUrl)
                    }
                    OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }, singleLine = true)
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("Số điện thoại") }, singleLine = true)
                    OutlinedTextField(
                        dateOfBirth,
                        { value -> dateOfBirth = value.filter { it.isDigit() || it == '-' }.take(10) },
                        label = { Text("Ngày sinh (YYYY-MM-DD)") },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !uiState.isUploadingAvatar,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isUploadingAvatar) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (uiState.isUploadingAvatar) "Đang tải ảnh..." else "Chọn ảnh từ điện thoại")
                    }
                    OutlinedTextField(
                        avatarUrl,
                        { avatarUrl = it },
                        label = { Text("Link ảnh đại diện") },
                        singleLine = true,
                    )
                }
            },
            actions = {
                OutlinedButton(
                    onClick = { showEdit = false },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isUploadingAvatar && !uiState.isUpdatingProfile,
                ) { Text("Hủy") }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { onUpdateProfile(name, phone, dateOfBirth, avatarUrl) { showEdit = false } },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && !uiState.isUploadingAvatar && !uiState.isUpdatingProfile,
                ) {
                    if (uiState.isUpdatingProfile) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (uiState.isUpdatingProfile) "Đang lưu..." else "Lưu")
                }
            },
        )
    }
}

@Composable
private fun ProfileAvatar(avatarUrl: String?) {
    if (avatarUrl.isNullOrBlank()) {
        Image(
            painterResource(R.drawable.avatar_user),
            "Ảnh đại diện mặc định",
            Modifier.fillMaxSize().clip(CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    } else {
        SubcomposeAsyncImage(
            model = resolveMediaUrl(avatarUrl),
            contentDescription = "Ảnh đại diện",
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            loading = { Image(painterResource(R.drawable.avatar_user), null, Modifier.fillMaxSize()) },
            error = { Image(painterResource(R.drawable.avatar_user), null, Modifier.fillMaxSize()) },
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



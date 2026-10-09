package com.example.foodhubapp.feature.shared.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.shared.auth.data.RemoteAuthRepository
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.FoodHubAppTheme
import kotlinx.coroutines.launch

enum class AccountTokenMode { RESET_PASSWORD, VERIFY_EMAIL }

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun AccountTokenRoute(
    mode: AccountTokenMode,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    initialToken: String = "",
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository =
        remember { RemoteAuthRepository(tokenStore = TokenStore(context.applicationContext)) }
    val scope = rememberCoroutineScope()
    var token by remember(initialToken) { mutableStateOf(initialToken) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val reset = mode == AccountTokenMode.RESET_PASSWORD
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (reset) "Đặt lại mật khẩu" else "Xác minh email") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (reset) "Nhập token trong email khôi phục và mật khẩu mới."
                else "Nhập token xác minh đã được gửi tới email của bạn.",
                fontSize = 16.sp,
            )
            OutlinedTextField(
                token,
                { token = it.trim() },
                Modifier.fillMaxWidth(),
                label = { Text("Token") },
                minLines = 2)
            if (reset) {
                OutlinedTextField(
                    password,
                    { password = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Mật khẩu mới") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    confirmation,
                    { confirmation = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Nhập lại mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
            error?.let {
                Text(
                    it,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        val result =
                            if (reset) repository.resetPassword(token, password, confirmation)
                            else repository.verifyEmail(token)
                        result.onSuccess { onSuccess() }
                            .onFailure { error = it.message ?: "Không thể thực hiện yêu cầu" }
                        busy = false
                    }
                },
                enabled = !busy && token.isNotBlank() && (!reset || (password.length >= 6 && password == confirmation)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (busy) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (reset) "Đổi mật khẩu" else "Xác minh", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, name = "Xác minh Email")
@Composable
private fun AccountTokenVerifyEmailPreview() {
    FoodHubAppTheme {
        AccountTokenRoute(mode = AccountTokenMode.VERIFY_EMAIL, onBack = {}, onSuccess = {})
    }
}

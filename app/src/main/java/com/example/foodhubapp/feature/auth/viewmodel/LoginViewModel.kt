package com.example.foodhubapp.feature.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.auth.data.AuthRepository
import com.example.foodhubapp.feature.auth.data.RemoteAuthRepository
import com.example.foodhubapp.feature.auth.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic nghiệp vụ và trạng thái giao diện (UI State) cho màn hình Đăng nhập (Login).
 * Kế thừa từ [AndroidViewModel] để có quyền truy cập [Application] Context phục vụ khởi tạo DataStore.
 *
 * @property application Ứng dụng Android hiện tại.
 */
class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {
    // Khởi tạo AuthRepository sử dụng RemoteAuthRepository và TokenStore
    private val authRepository: AuthRepository = RemoteAuthRepository(
        tokenStore = TokenStore(application.applicationContext)
    )

    // Trạng thái giao diện nội bộ (mutable)
    private val _uiState = MutableStateFlow(LoginUiState())
    
    /**
     * Luồng trạng thái giao diện công khai (read-only) để UI quan sát và cập nhật.
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Cập nhật email đăng nhập khi người dùng nhập.
     *
     * @param value Giá trị chuỗi tài khoản mới.
     */
    fun onAccountChange(value: String) {
        _uiState.update { it.copy(account = value, errorMessage = null) }
    }

    /**
     * Cập nhật giá trị mật khẩu khi người dùng nhập.
     *
     * @param value Giá trị chuỗi mật khẩu mới.
     */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /**
     * Thay đổi trạng thái hiển thị hoặc ẩn ký tự mật khẩu.
     */
    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * Thực hiện quy trình đăng nhập tài khoản.
     * Kiểm tra tính hợp lệ của đầu vào, gọi API qua [AuthRepository] và cập nhật trạng thái UI tương ứng.
     */
    fun login() {
        val state = _uiState.value

        if (state.isLoading) return

        // Kiểm tra dữ liệu đầu vào không được để trống
        if (state.account.isBlank() || state.password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Vui lòng nhập email và mật khẩu")
            }
            return
        }

        // Bật trạng thái tải và xóa thông báo lỗi cũ trong coroutine scope
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            // Gọi API đăng nhập
            authRepository.login(
                LoginRequest(
                    account = state.account.trim(),
                    password = state.password
                )
            ).onSuccess { response ->
                // Đăng nhập thành công, cập nhật trạng thái đã đăng nhập
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = response.user?.role
                    )
                }
            }.onFailure { error ->
                // Đăng nhập thất bại, hiển thị thông báo lỗi
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Đăng nhập thất bại"
                    )
                }
            }
        }
    }
}

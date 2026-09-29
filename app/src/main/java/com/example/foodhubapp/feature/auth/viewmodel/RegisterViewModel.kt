package com.example.foodhubapp.feature.auth.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.auth.data.AuthRepository
import com.example.foodhubapp.feature.auth.data.RemoteAuthRepository
import com.example.foodhubapp.feature.auth.model.PasswordStrength
import com.example.foodhubapp.feature.auth.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic nghiệp vụ và trạng thái giao diện (UI State) cho màn hình Đăng ký (Register).
 * Kế thừa từ [AndroidViewModel] để khởi tạo Repository và TokenStore.
 *
 * @property application Ứng dụng Android hiện tại.
 */
class RegisterViewModel(
    application: Application
) : AndroidViewModel(application) {
    // Khởi tạo AuthRepository phục vụ gọi API đăng ký
    private val authRepository: AuthRepository = RemoteAuthRepository(
        tokenStore = TokenStore(application.applicationContext)
    )

    // Trạng thái giao diện nội bộ (mutable)
    private val _uiState = MutableStateFlow(RegisterUiState())
    
    /**
     * Luồng trạng thái giao diện công khai (read-only) để UI quan sát.
     */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * Cập nhật họ và tên khi người dùng nhập.
     */
    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    /**
     * Cập nhật số điện thoại khi người dùng nhập.
     */
    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, errorMessage = null) }
    }

    /**
     * Cập nhật địa chỉ email khi người dùng nhập.
     */
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    /**
     * Cập nhật mật khẩu khi người dùng nhập.
     */
    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordStrength = value.calculatePasswordStrength(),
                errorMessage = null
            )
        }
    }

    /**
     * Thay đổi trạng thái hiển thị hoặc ẩn mật khẩu.
     */
    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * Thay đổi trạng thái đồng ý với điều khoản dịch vụ của ứng dụng.
     */
    fun toggleTerms() {
        _uiState.update { it.copy(acceptedTerms = !it.acceptedTerms, errorMessage = null) }
    }

    /**
     * Thực hiện quy trình đăng ký tài khoản mới.
     * Kiểm tra tính hợp lệ của thông tin đầu vào (họ tên, số điện thoại, độ mạnh mật khẩu, điều khoản),
     * sau đó gọi API đăng ký qua [AuthRepository].
     */
    fun register() {
        val state = _uiState.value

        if (state.isLoading) return

        // Kiểm tra tính hợp lệ của dữ liệu trước khi gửi yêu cầu
        val validationMessage = when {
            state.fullName.isBlank() -> "Vui lòng nhập họ và tên"
            state.phoneNumber.isBlank() -> "Vui lòng nhập số điện thoại"
            !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() -> "Vui lòng nhập email hợp lệ"
            state.passwordStrength != PasswordStrength.STRONG -> {
                "Mật khẩu cần ít nhất 8 ký tự, gồm chữ cái, số và ký tự đặc biệt"
            }
            !state.acceptedTerms -> "Bạn cần đồng ý điều khoản của FoodHub"
            else -> null
        }

        // Nếu có lỗi xác thực, cập nhật thông báo lỗi lên UI và dừng lại
        if (validationMessage != null) {
            _uiState.update { it.copy(errorMessage = validationMessage) }
            return
        }

        // Tiến hành gọi API đăng ký trong coroutine scope
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            authRepository.register(
                RegisterRequest(
                    fullName = state.fullName.trim(),
                    phoneNumber = state.phoneNumber.trim(),
                    email = state.email.trim(),
                    password = state.password,
                    confirmPassword = state.password
                )
            ).onSuccess {
                // Đăng ký thành công, cập nhật trạng thái hoàn tất
                _uiState.update {
                    it.copy(isLoading = false, isRegistered = true)
                }
            }.onFailure { error ->
                // Đăng ký thất bại, hiển thị thông báo lỗi từ server hoặc ngoại lệ
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Đăng ký thất bại"
                    )
                }
            }
        }
    }
}

/**
 * Hàm mở rộng kiểm tra độ mạnh của mật khẩu.
 * Yêu cầu:
 * - Độ dài tối thiểu 8 ký tự.
 * - Chứa ít nhất một chữ cái.
 * - Chứa ít nhất một chữ số.
 * - Chứa ít nhất một ký tự đặc biệt.
 *
 * @return `true` nếu mật khẩu đủ mạnh, ngược lại trả về `false`.
 */
private fun String.calculatePasswordStrength(): PasswordStrength {
    if (isBlank()) return PasswordStrength.EMPTY

    var score = 0

    if (length >= 8) score++
    if (any { it.isLetter() }) score++
    if (any { it.isDigit() }) score++
    if (any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 1 -> PasswordStrength.WEAK
        score == 2 || score == 3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }
}

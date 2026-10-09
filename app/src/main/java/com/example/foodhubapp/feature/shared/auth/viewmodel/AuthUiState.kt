package com.example.foodhubapp.feature.shared.auth.viewmodel

import com.example.foodhubapp.feature.shared.auth.model.PasswordStrength

/**
 * Đại diện cho trạng thái giao diện (UI State) của màn hình Đăng nhập (Login).
 *
 * @property account Tên tài khoản (số điện thoại hoặc email) mà người dùng nhập.
 * @property password Mật khẩu người dùng nhập.
 * @property isPasswordVisible Trạng thái hiển thị hay ẩn mật khẩu trên giao diện.
 * @property isLoading Trạng thái đang tải (gọi API đăng nhập).
 * @property errorMessage Thông báo lỗi (nếu có) khi xác thực hoặc gọi API thất bại.
 * @property isLoggedIn Trạng thái xác định xem người dùng đã đăng nhập thành công hay chưa.
 */
data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null
)

/**
 * Đại diện cho trạng thái giao diện (UI State) của màn hình Đăng ký (Register).
 *
 * @property fullName Họ và tên đầy đủ của người dùng.
 * @property phoneNumber Số điện thoại đăng ký.
 * @property email Địa chỉ email của người dùng (tùy chọn).
 * @property password Mật khẩu tài khoản.
 * @property isPasswordVisible Trạng thái hiển thị hoặc ẩn mật khẩu trên giao diện.
 * @property acceptedTerms Trạng thái người dùng đã đồng ý với các điều khoản dịch vụ hay chưa.
 * @property isLoading Trạng thái đang tải (gọi API đăng ký).
 * @property errorMessage Thông báo lỗi (nếu có) khi kiểm tra dữ liệu hoặc gọi API thất bại.
 * @property isRegistered Trạng thái xác định xem người dùng đã đăng ký thành công hay chưa.
 */
data class RegisterUiState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.EMPTY
)

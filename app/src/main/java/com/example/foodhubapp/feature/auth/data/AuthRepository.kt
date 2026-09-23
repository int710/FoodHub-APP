package com.example.foodhubapp.feature.auth.data

import com.example.foodhubapp.feature.auth.model.AuthResponse
import com.example.foodhubapp.feature.auth.model.LoginRequest
import com.example.foodhubapp.feature.auth.model.RegisterRequest

/**
 * Repository định nghĩa các thao tác liên quan đến xác thực người dùng
 * (đăng nhập, đăng ký tài khoản).
 */
interface AuthRepository {
    /**
     * Thực hiện đăng nhập tài khoản vào hệ thống.
     *
     * @param request Thông tin yêu cầu đăng nhập chứa tài khoản và mật khẩu ([LoginRequest]).
     * @return [Result] chứa [AuthResponse] nếu thành công, hoặc ngoại lệ nếu thất bại.
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse>

    /**
     * Thực hiện đăng ký tài khoản mới trên hệ thống.
     *
     * @param request Thông tin yêu cầu đăng ký chi tiết ([RegisterRequest]).
     * @return [Result] chứa [AuthResponse] nếu thành công, hoặc ngoại lệ nếu thất bại.
     */
    suspend fun register(request: RegisterRequest): Result<AuthResponse>
}

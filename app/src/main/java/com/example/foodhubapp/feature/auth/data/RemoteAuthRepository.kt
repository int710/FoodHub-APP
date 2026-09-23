package com.example.foodhubapp.feature.auth.data

import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.auth.model.AuthResponse
import com.example.foodhubapp.feature.auth.model.LoginRequest
import com.example.foodhubapp.feature.auth.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository chịu trách nhiệm thực hiện các tác vụ xác thực (Authentication) từ xa qua Network.
 * Kế thừa từ [AuthRepository] và sử dụng [FoodHubApiClient] để gọi API,
 * đồng thời dùng [TokenStore] để lưu trữ token xác thực (Access Token, Refresh Token).
 */
class RemoteAuthRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
    private val tokenStore: TokenStore
) : AuthRepository {

    /**
     * Thực hiện đăng nhập tài khoản.
     * 
     * @param request Chứa thông tin đăng nhập (tài khoản/mật khẩu).
     * @return [Result] chứa [AuthResponse] nếu thành công, hoặc Exception nếu thất bại.
     */
    override suspend fun login(
        request: LoginRequest
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        // Sử dụng runCatching để bắt và bọc các ngoại lệ mạng/JSON thành Result.failure nếu có lỗi xảy ra
        runCatching {
            // 1. Gửi request POST đến endpoint đăng nhập trên server
            val json = apiClient.post(
                path = "/auth/login",
                body = request.toJson()
            )
            
            // 2. Chuyển đổi chuỗi JSON nhận được thành đối tượng AuthResponse
            val response = AuthResponse.fromJson(json)

            // 3. Lưu lại Access Token và Refresh Token vào Local Storage (TokenStore)
            tokenStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken
            )

            // 4. Trả về thông tin phản hồi thành công
            response
        }
    }

    /**
     * Thực hiện đăng ký tài khoản mới.
     * 
     * @param request Chứa thông tin đăng ký (Họ tên, SĐT, mật khẩu, v.v.).
     * @return [Result] chứa [AuthResponse] nếu thành công, hoặc Exception nếu thất bại.
     */
    override suspend fun register(
        request: RegisterRequest
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        // Sử dụng runCatching để bắt và bọc các ngoại lệ mạng/JSON thành Result.failure
        runCatching {
            // 1. Gửi request POST đến endpoint đăng ký trên server
            val json = apiClient.post(
                path = "/user/register",
                body = request.toJson()
            )
            
            // 2. Chuyển đổi chuỗi JSON nhận được thành đối tượng AuthResponse
            val response = AuthResponse.fromJson(json)

            // 3. Lưu lại Access Token và Refresh Token vào Local Storage (TokenStore)
            tokenStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken
            )

            // 4. Trả về thông tin phản hồi thành công
            response
        }
    }
}

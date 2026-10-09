package com.example.foodhubapp.feature.shared.auth.data

import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.shared.auth.model.AuthResponse
import com.example.foodhubapp.feature.shared.auth.model.LoginRequest
import com.example.foodhubapp.feature.shared.auth.model.RegisterRequest
import com.example.foodhubapp.feature.shared.auth.model.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import org.json.JSONException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

/**
 * Repository chịu trách nhiệm thực hiện các tác vụ xác thực (Authentication) từ xa qua Network.
 * Kế thừa từ [AuthRepository] và sử dụng [FoodHubApiClient] để gọi API,
 * đồng thời dùng [TokenStore] để lưu trữ token xác thực (Access Token, Refresh Token).
 */
class RemoteAuthRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
    private val tokenStore: TokenStore
) : AuthRepository {
    override suspend fun resetPassword(token: String, password: String, confirmation: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                apiClient.post(
                    "/user/reset-password",
                    JSONObject()
                        .put("forgot_password_token", token.trim())
                        .put("new_password", password)
                        .put("confirmNewPassword", confirmation),
                ).optString("message")
            }
        }

    override suspend fun verifyEmail(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            apiClient.post("/user/verify-email", JSONObject().put("verify_email_token", token.trim()))
                .optString("message")
        }
    }
    override suspend fun forgotPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.post(
                path = "/user/forgot-password",
                body = JSONObject().put("email", email.trim())
            )
            Result.success(response.optString("message"))
        } catch (error: CancellationException) {
            throw error
        } catch (error: SocketTimeoutException) {
            Result.failure(IllegalStateException("Máy chủ phản hồi quá lâu. Vui lòng thử lại.", error))
        } catch (error: UnknownHostException) {
            Result.failure(IllegalStateException("Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng.", error))
        } catch (error: JSONException) {
            Result.failure(IllegalStateException("Phản hồi máy chủ không hợp lệ. Vui lòng thử lại sau.", error))
        } catch (error: IOException) {
            Result.failure(IllegalStateException("Kết nối bị gián đoạn. Vui lòng thử lại.", error))
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

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
                path = "/user/login",
                body = request.toJson()
            )
            
            // 2. Chuyển đổi chuỗi JSON nhận được thành đối tượng AuthResponse
            val response = AuthResponse.fromJson(json)

            val user = response.user ?: loadCurrentUser(response.accessToken)

            // 3. Lưu lại Access Token và Refresh Token vào Local Storage (TokenStore)
            tokenStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                user = user
            )

            // 4. Trả về thông tin phản hồi thành công
            response.copy(user = user)
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

            val user = response.user ?: loadCurrentUser(response.accessToken)

            // 3. Lưu lại Access Token và Refresh Token vào Local Storage (TokenStore)
            tokenStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                user = user
            )

            // 4. Trả về thông tin phản hồi thành công
            response.copy(user = user)
        }
    }

    private fun loadCurrentUser(accessToken: String) = UserDto.fromJson(
        apiClient.getJson("/user/me", accessToken).optJSONObject("data")
            ?: throw IllegalStateException("API không trả về thông tin người dùng")
    )
}

package com.example.foodhubapp.feature.customer.profile.data

import com.example.foodhubapp.core.network.*
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.feature.customer.profile.model.ProfileUiModel
import com.example.foodhubapp.feature.customer.profile.model.previewProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject

/**
 * Giao diện  định nghĩa các hàm thao tác dữ liệu liên quan đến Profile.
 */
interface ProfileRepository {
    suspend fun getProfileData(): Result<ProfileUiModel>
    suspend fun logout(): Result<Unit>
    suspend fun updateProfile(name: String, phone: String?): Result<ProfileUiModel>
}

/**
 * Lớp thực thi lấy dữ liệu Profile từ Server thực tế thông qua [FoodHubApiClient].
 * Kết hợp sử dụng [TokenStore] để lấy Access Token xác thực người dùng.
 */
class RemoteProfileRepository(
    private val apiClient: FoodHubApiClient = FoodhubRetrofit.apiClient,
    private val tokenStore: TokenStore
) : ProfileRepository {

    override suspend fun updateProfile(name: String, phone: String?): Result<ProfileUiModel> = withContext(Dispatchers.IO) {
        runCatching {
            val token = tokenStore.getAccessToken() ?: error("Bạn cần đăng nhập lại")
            val response = apiClient.execute(
                apiClient.authApi.updateProfile(
                    headers = mapOf("Authorization" to "Bearer $token"),
                    body = JsonObject().put("name", name.trim()).put("phone", phone?.trim()?.takeIf(String::isNotBlank) ?: com.google.gson.JsonNull.INSTANCE)
                )
            )
            (response.optObject("data") ?: response).toProfileUiModel()
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            tokenStore.getRefreshToken()?.takeIf(String::isNotBlank)?.let { refreshToken ->
                apiClient.execute(
                    apiClient.authApi.logout(
                        body = JsonObject().put("refresh_token", refreshToken)
                    )
                )
            }
            tokenStore.clearTokens()
        }
    }

    /**
     * Tải dữ liệu hồ sơ cá nhân từ API.
     * Trả về [Result.success] chứa [ProfileUiModel] nếu thành công, hoặc [Result.failure] nếu có lỗi.
     */
    override suspend fun getProfileData(): Result<ProfileUiModel> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Kiểm tra xem người dùng đã đăng nhập chưa (có Token chưa)
            val accessToken = tokenStore.getAccessToken()
                ?: throw IllegalStateException("Bạn cần đăng nhập lại")

            // 2. Gọi API lấy dữ liệu JSON kèm theo Token xác thực
            val json = apiClient.execute(
                apiClient.authApi.getProfile(
                    headers = mapOf("Authorization" to "Bearer $accessToken")
                )
            )
            
            // 3. Trích xuất cục "data" từ JSON trả về (nếu có), nếu không dùng chính json gốc
            val data = json.optObject("data") ?: json

            // 4. Chuyển đổi dữ liệu JSON thô thành đối tượng ProfileUiModel của ứng dụng
            data.toProfileUiModel()
        }
    }
}

/**
 * Hàm mở rộng (Extension function) giúp chuyển đổi một [JsonObject] từ server
 * thành đối tượng giao diện [ProfileUiModel].
 */
private fun JsonObject.toProfileUiModel(): ProfileUiModel {
    // API user/me hiện chỉ trả thông tin user, chưa trả rewards/tier/quick access.
    val userJson = optObject("user") ?: this

    return previewProfile.copy(
        user = previewProfile.user.copy(
            name = userJson.optString("name")
                .ifBlank { null }
                ?: previewProfile.user.name,
            email = userJson.optString("email")
                .ifBlank { null }
                ?: previewProfile.user.email,
            role = userJson.optString("role")
                .ifBlank { null }
                ?: previewProfile.user.role,
            phone = userJson.optString("phone").ifBlank { null },
            phoneMasked = userJson.optString("phone").ifBlank { null }
                ?.let(::maskPhone)
                ?: "",
            dateOfBirth = userJson.optString("dateOfBirth")
                .ifBlank { null }
                ?.take(10),
            isActive = userJson.optBoolean("isActive", previewProfile.user.isActive),
            isVerified = userJson.optBoolean("isVerified", previewProfile.user.isVerified),
            memberSince = userJson.optString("createdAt")
                .ifBlank { null }
                ?.let { "Khách hàng thân thiết từ ${it.take(4)}" }
                ?: previewProfile.user.memberSince
        )
    )
}

/**
 * Che bớt số điện thoại để bảo mật (Ví dụ: 0987123456 thành 0987.xxx.456).
 */
private fun maskPhone(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    return if (digits.length >= 7) {
        "${digits.take(4)}.xxx.${digits.takeLast(3)}"
    } else {
        phone
    }
}

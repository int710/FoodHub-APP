package com.example.foodhubapp.feature.profile.data

import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import com.example.foodhubapp.feature.profile.model.ProfileUiModel
import com.example.foodhubapp.feature.profile.model.previewProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Đường dẫn API lấy thông tin profile người dùng trên server
private const val PROFILE_ENDPOINT = "/user/me"


/**
 * Giao diện  định nghĩa các hàm thao tác dữ liệu liên quan đến Profile.
 */
interface ProfileRepository {
    suspend fun getProfileData(): Result<ProfileUiModel>
}

/**
 * Lớp thực thi lấy dữ liệu Profile từ Server thực tế thông qua [FoodHubApiClient].
 * Kết hợp sử dụng [TokenStore] để lấy Access Token xác thực người dùng.
 */
class RemoteProfileRepository(
    private val apiClient: FoodHubApiClient = FoodHubApiClient(),
    private val tokenStore: TokenStore
) : ProfileRepository {

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
            val json = apiClient.getJson(
                path = PROFILE_ENDPOINT,
                accessToken = accessToken
            )
            
            // 3. Trích xuất cục "data" từ JSON trả về (nếu có), nếu không dùng chính json gốc
            val data = json.optJSONObject("data") ?: json

            // 4. Chuyển đổi dữ liệu JSON thô thành đối tượng ProfileUiModel của ứng dụng
            data.toProfileUiModel()
        }
    }
}

/**
 * Hàm mở rộng (Extension function) giúp chuyển đổi một [JSONObject] từ server
 * thành đối tượng giao diện [ProfileUiModel].
 */
private fun JSONObject.toProfileUiModel(): ProfileUiModel {
    // API user/me hiện chỉ trả thông tin user, chưa trả rewards/tier/quick access.
    val userJson = optJSONObject("user") ?: this

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
            phoneMasked = maskPhone(
                userJson.optString("phone")
                    .ifBlank { null }
                    ?: previewProfile.user.phoneMasked
            ),
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

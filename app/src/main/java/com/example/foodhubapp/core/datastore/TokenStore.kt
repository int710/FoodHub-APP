package com.example.foodhubapp.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.example.foodhubapp.feature.shared.auth.model.UserDto
import org.json.JSONObject
import com.example.foodhubapp.core.network.SessionTokens

/**
 * Tên của DataStore Preferences lưu trữ thông tin phiên đăng nhập (token).
 */
private const val AUTH_DATA_STORE_NAME = "auth_session"

/**
 * Thuộc tính mở rộng cho [Context] để khởi tạo và truy cập DataStore Preferences quản lý token.
 */
private val Context.authDataStore by preferencesDataStore(name = AUTH_DATA_STORE_NAME)

/**
 * Quản lý việc lưu trữ, truy xuất và xóa các Token xác thực (Access Token, Refresh Token)
 * sử dụng Jetpack DataStore Preferences một cách an toàn với Coroutines.
 *
 * @property context Context của ứng dụng, dùng để truy cập DataStore.
 */
class TokenStore(
    private val context: Context
) {
    // Định nghĩa các khóa (key) để lưu trữ token trong Preferences DataStore
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userKey = stringPreferencesKey("user")

    val user: Flow<UserDto?> = context.authDataStore.data.map { preferences ->
        preferences[userKey]?.let { value ->
            runCatching { UserDto.fromJson(JSONObject(value)) }.getOrNull()
        }
    }

    /**
     * Luồng dữ liệu (Flow) phát ra Access Token hiện tại, tự động cập nhật khi token thay đổi.
     */
    val accessToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[accessTokenKey]
    }

    /**
     * Luồng dữ liệu (Flow) phát ra Refresh Token hiện tại, tự động cập nhật khi token thay đổi.
     */
    val refreshToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[refreshTokenKey]
    }

    /**
     * Lưu trữ Access Token và Refresh Token vào DataStore.
     * Nếu refresh token trống hoặc null, khóa tương ứng sẽ bị xóa.
     *
     * @param accessToken Token truy cập API.
     * @param refreshToken Token làm mới phiên đăng nhập (có thể null).
     */
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        user: UserDto? = null
    ) {
        context.authDataStore.edit { preferences ->
            // Lưu Access Token
            preferences[accessTokenKey] = accessToken
            if (user == null) {
                preferences.remove(userKey)
            } else {
                preferences[userKey] = JSONObject()
                    .put("id", user.id)
                    .put("fullName", user.fullName)
                    .put("phoneNumber", user.phoneNumber ?: "")
                    .put("email", user.email ?: "")
                    .put("role", user.role)
                    .toString()
            }

            // Kiểm tra và lưu hoặc xóa Refresh Token tùy theo giá trị đầu vào
            if (refreshToken.isNullOrBlank()) {
                preferences.remove(refreshTokenKey)
            } else {
                preferences[refreshTokenKey] = refreshToken
            }
        }
    }

    /**
     * Lấy giá trị Access Token hiện tại (đồng bộ một lần qua Flow).
     *
     * @return Chuỗi Access Token hoặc null nếu chưa đăng nhập.
     */
    suspend fun getAccessToken(): String? = accessToken.first()

    /**
     * Lấy giá trị Refresh Token hiện tại (đồng bộ một lần qua Flow).
     *
     * @return Chuỗi Refresh Token hoặc null nếu không tồn tại.
     */
    suspend fun getRefreshToken(): String? = refreshToken.first()

    suspend fun getUser(): UserDto? = user.first()

    internal suspend fun sessionTokens(): SessionTokens {
        val preferences = context.authDataStore.data.first()
        return SessionTokens(preferences[accessTokenKey], preferences[refreshTokenKey])
    }

    internal suspend fun replaceSessionTokens(expected: SessionTokens, replacement: SessionTokens): Boolean {
        var replaced = false
        context.authDataStore.edit { preferences ->
            if (SessionTokens(preferences[accessTokenKey], preferences[refreshTokenKey]) == expected) {
                replacement.access?.let { preferences[accessTokenKey] = it } ?: preferences.remove(accessTokenKey)
                replacement.refresh?.let { preferences[refreshTokenKey] = it } ?: preferences.remove(refreshTokenKey)
                if (replacement.access == null) preferences.remove(userKey)
                replaced = true
            }
        }
        return replaced
    }

    suspend fun saveUser(user: UserDto) {
        context.authDataStore.edit { preferences ->
            preferences[userKey] = JSONObject()
                .put("id", user.id)
                .put("fullName", user.fullName)
                .put("phoneNumber", user.phoneNumber ?: "")
                .put("email", user.email ?: "")
                .put("role", user.role)
                .toString()
        }
    }

    /**
     * Xóa toàn bộ token khỏi DataStore khi người dùng đăng xuất hoặc hết hạn phiên.
     */
    suspend fun clearTokens() {
        context.authDataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
            preferences.remove(userKey)
        }
    }
}

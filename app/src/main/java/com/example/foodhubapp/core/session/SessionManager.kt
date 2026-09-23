package com.example.foodhubapp.core.session

import com.example.foodhubapp.core.datastore.TokenStore

/**
 * Quản lý trạng thái phiên làm việc của người dùng (kiểm tra đăng nhập, đăng xuất, v.v.)
 * thông qua việc tương tác với [TokenStore].
 *
 * @property tokenStore Nơi lưu trữ và quản lý các token xác thực.
 */
class SessionManager(
    private val tokenStore: TokenStore
) {
    /**
     * Kiểm tra xem người dùng hiện tại đã đăng nhập hay chưa bằng cách kiểm tra
     * sự tồn tại của Access Token trong [TokenStore].
     *
     * @return `true` nếu tồn tại Access Token hợp lệ, ngược lại trả về `false`.
     */
    suspend fun isLoggedIn(): Boolean {
        return !tokenStore.getAccessToken().isNullOrBlank()
    }

    /**
     * Thực hiện đăng xuất người dùng bằng cách xóa toàn bộ token lưu trữ trong [TokenStore].
     */
    suspend fun logout() {
        tokenStore.clearTokens()
    }
}

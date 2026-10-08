package com.example.foodhubapp.feature.cart.data

import android.content.Context
import com.foodhub.app.TableSessionStore

/**
 * Nguồn quyết định duy nhất cho ngữ cảnh giỏ hàng.
 * Phiên QR luôn ưu tiên DINE_IN; khi không có phiên bàn, lựa chọn của tài khoản
 * chỉ có thể là TAKEAWAY hoặc DELIVERY.
 */
class CartContextStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val tableSessionStore = TableSessionStore(appContext)

    fun currentType(): CartType {
        if (tableSessionStore.current() != null) return CartType.DINE_IN
        return runCatching {
            CartType.valueOf(preferences.getString(KEY_ACCOUNT_CART_TYPE, null).orEmpty())
        }.getOrNull()?.takeIf { it != CartType.DINE_IN } ?: CartType.TAKEAWAY
    }

    fun selectAccountType(type: CartType) {
        require(type != CartType.DINE_IN) { "DINE_IN chỉ được kích hoạt bằng phiên QR bàn." }
        preferences.edit().putString(KEY_ACCOUNT_CART_TYPE, type.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "ordering_context"
        const val KEY_ACCOUNT_CART_TYPE = "account_cart_type"
    }
}

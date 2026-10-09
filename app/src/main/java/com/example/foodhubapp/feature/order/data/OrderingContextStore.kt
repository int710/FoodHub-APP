package com.example.foodhubapp.feature.order.data

import android.content.Context
import com.example.foodhubapp.feature.cart.data.CartType
import com.example.foodhubapp.feature.table.data.TableSessionStore

/** Persists the customer's selected ordering mode between menu and cart screens. */
class OrderingContextStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("ordering_context", Context.MODE_PRIVATE)
    private val tableSessionStore = TableSessionStore(appContext)

    fun currentType(): CartType {
        if (tableSessionStore.current() != null) return CartType.DINE_IN
        return runCatching {
            CartType.valueOf(preferences.getString(KEY_TYPE, CartType.TAKEAWAY.name).orEmpty())
        }.getOrDefault(CartType.TAKEAWAY)
    }

    fun select(type: CartType) {
        require(type != CartType.DINE_IN || tableSessionStore.current() != null) {
            "Vui lòng quét QR bàn trước khi chọn dùng tại bàn."
        }
        preferences.edit().putString(KEY_TYPE, type.name).apply()
    }

    private companion object {
        const val KEY_TYPE = "cart_type"
    }
}

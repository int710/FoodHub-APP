package com.example.foodhubapp.feature.customer.order.data

import android.content.Context
import com.example.foodhubapp.feature.customer.cart.data.CartType
import com.example.foodhubapp.feature.customer.table.data.TableSessionStore

/** Persists the customer's selected ordering mode between menu and cart screens. */
class OrderingContextStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("ordering_context", Context.MODE_PRIVATE)
    private val tableSessionStore = TableSessionStore(appContext)

    fun currentType(): CartType {
        val stored = preferences.getString(KEY_TYPE, null)
        if (stored == null) return if (tableSessionStore.current() != null) CartType.DINE_IN else CartType.TAKEAWAY
        val type = runCatching { CartType.valueOf(stored) }.getOrDefault(CartType.TAKEAWAY)
        return if (type == CartType.DINE_IN && tableSessionStore.current() == null) CartType.TAKEAWAY else type
    }

    fun selectDefault(isLoggedIn: Boolean) {
        if (tableSessionStore.current() != null) {
            select(CartType.DINE_IN)
        } else if (!isLoggedIn) {
            if (!preferences.contains(KEY_TYPE) || currentType() == CartType.DELIVERY) select(CartType.TAKEAWAY)
        } else if (!preferences.contains(KEY_TYPE)) {
            select(CartType.DELIVERY)
        }
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

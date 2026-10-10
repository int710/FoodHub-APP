package com.example.foodhubapp.feature.shared.payment

import com.example.foodhubapp.core.network.*
import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject

class PaymentRepository internal constructor(private val apiClient: FoodHubApiClient, private val accessToken: suspend () -> String?) {
    constructor(context: Context) : this(FoodhubRetrofit.apiClient, { TokenStore(context.applicationContext).getAccessToken() })

    suspend fun getDetail(orderId: String): JsonObject? = withContext(Dispatchers.IO) {
        val token = accessToken()?.takeIf(String::isNotBlank) ?: error("Vui lòng đăng nhập lại")
        apiClient.execute(
            apiClient.paymentApi.getDetail(
                id = orderId,
                headers = mapOf("Authorization" to "Bearer $token")
            )
        ).optObject("data")
    }
}

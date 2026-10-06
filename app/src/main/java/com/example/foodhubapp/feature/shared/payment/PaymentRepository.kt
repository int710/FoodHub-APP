package com.example.foodhubapp.feature.shared.payment

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.network.FoodHubApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PaymentRepository internal constructor(private val apiClient: FoodHubApiClient, private val accessToken: suspend () -> String?) {
    constructor(context: Context) : this(FoodHubApiClient(), { TokenStore(context.applicationContext).getAccessToken() })

    suspend fun getDetail(orderId: String): JSONObject? = withContext(Dispatchers.IO) {
        val token = accessToken()?.takeIf(String::isNotBlank) ?: error("Vui lòng đăng nhập lại")
        apiClient.getJson("/payment/$orderId", mapOf("Authorization" to "Bearer $token")).optJSONObject("data")
    }
}

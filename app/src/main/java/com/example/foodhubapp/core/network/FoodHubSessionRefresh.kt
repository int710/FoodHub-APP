package com.example.foodhubapp.core.network

import android.content.Context
import com.example.foodhubapp.core.datastore.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FoodHubSessionRefresh {
    @Volatile private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    @Synchronized
    fun refresh(httpClient: OkHttpClient, baseUrl: String): String? {
        val context = applicationContext ?: return null
        val store = TokenStore(context)
        return runBlocking {
            val refreshToken = store.getRefreshToken()?.takeIf(String::isNotBlank) ?: return@runBlocking null
            val request = Request.Builder()
                .url("$baseUrl/user/refresh-token")
                .post(
                    JSONObject().put("refresh_token", refreshToken).toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    store.clearTokens()
                    return@runBlocking null
                }
                val root = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
                    ?: return@runBlocking null
                val data = root.optJSONObject("data") ?: root
                val newAccess = data.optString("access_token", data.optString("accessToken"))
                    .takeIf(String::isNotBlank) ?: return@runBlocking null
                val newRefresh = data.optString("refresh_token", data.optString("refreshToken"))
                    .takeIf(String::isNotBlank) ?: refreshToken
                store.saveTokens(newAccess, newRefresh, store.getUser())
                newAccess
            }
        }
    }
}

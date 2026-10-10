package com.example.foodhubapp.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import com.google.gson.JsonObject
import com.example.foodhubapp.feature.shared.auth.data.AuthApi

internal data class SessionTokens(val access: String?, val refresh: String?)

internal interface SessionTokenStorage {
    suspend fun read(): SessionTokens
    suspend fun replace(expected: SessionTokens, replacement: SessionTokens): Boolean
}

/** Serializes refreshes and reuses tokens already renewed by another request. */
internal class SessionRefreshCoordinator(private val storage: SessionTokenStorage) {
    @Synchronized
    fun refresh(
        client: OkHttpClient,
        baseUrl: String,
        rejectedAccessToken: String? = null
    ): String? = runBlocking {
        val tokens = storage.read()
        if (rejectedAccessToken != null && !tokens.access.isNullOrBlank() && tokens.access != rejectedAccessToken) {
            return@runBlocking tokens.access
        }
        val refreshToken = tokens.refresh?.takeIf(String::isNotBlank) ?: return@runBlocking null
        // A separate service with no session authenticator prevents recursive refresh requests.
        val api = createRetrofit(baseUrl, client).create(AuthApi::class.java)
        val response = runCatching {
            api.refreshToken(body = JsonObject().put("refresh_token", refreshToken)).execute()
        }.getOrElse { error ->
            if (error is java.io.IOException) throw error
            throw IllegalStateException("Máy chủ không trả dữ liệu làm mới phiên hợp lệ. Vui lòng thử lại.", error)
        }
        run {
            val root = if (response.isSuccessful) response.body() else {
                runCatching { response.errorBody()?.use { parseJsonObject(it.string()) } }.getOrNull()
            }
            if (root == null) throw IllegalStateException("Máy chủ không trả dữ liệu làm mới phiên hợp lệ. Vui lòng thử lại.")
            if (response.code() == 401 || response.code() == 403) {
                return@runBlocking if (storage.replace(
                        tokens,
                        SessionTokens(null, null))) null else storage.read().access
            }
            if (!response.isSuccessful) {
                throw FoodHubApiException(
                    response.code(),
                    "Không thể làm mới phiên lúc này. Vui lòng thử lại.")
            }
            if (root.has("data") && root.isNull("data")) {
                return@runBlocking if (storage.replace(
                        tokens,
                        SessionTokens(null, null))) null else storage.read().access
            }
            val data = root.optObject("data") ?: root
            val payload = data.optObject("auth") ?: data
            val access = payload.optString("access_token", payload.optString("accessToken"))
                .takeIf { it.isNotBlank() && it != "null" }
                ?: throw IllegalStateException("Máy chủ chưa trả token mới. Vui lòng thử lại.")
            val refresh = payload.optString("refresh_token", payload.optString("refreshToken"))
                .takeIf { it.isNotBlank() && it != "null" } ?: refreshToken
            if (!storage.replace(
                    tokens,
                    SessionTokens(access, refresh))) return@runBlocking storage.read().access
            access
        }
    }
}

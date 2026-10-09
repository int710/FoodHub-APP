package com.example.foodhubapp.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal data class SessionTokens(val access: String?, val refresh: String?)

internal interface SessionTokenStorage {
    suspend fun read(): SessionTokens
    suspend fun replace(expected: SessionTokens, replacement: SessionTokens): Boolean
}

/** Serializes refreshes and reuses tokens already renewed by another request. */
internal class SessionRefreshCoordinator(private val storage: SessionTokenStorage) {
    @Synchronized
    fun refresh(client: OkHttpClient, baseUrl: String, rejectedAccessToken: String? = null): String? = runBlocking {
        val tokens = storage.read()
        if (rejectedAccessToken != null && !tokens.access.isNullOrBlank() && tokens.access != rejectedAccessToken) {
            return@runBlocking tokens.access
        }
        val refreshToken = tokens.refresh?.takeIf(String::isNotBlank) ?: return@runBlocking null
        val request = Request.Builder().url("$baseUrl/user/refresh-token")
            .post(JSONObject().put("refresh_token", refreshToken).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val root = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
            if (root == null) throw IllegalStateException("Máy chủ không trả dữ liệu làm mới phiên hợp lệ. Vui lòng thử lại.")
            if (response.code == 401 || response.code == 403) {
                return@runBlocking if (storage.replace(tokens, SessionTokens(null, null))) null else storage.read().access
            }
            if (!response.isSuccessful) {
                throw FoodHubApiException(response.code, "Không thể làm mới phiên lúc này. Vui lòng thử lại.")
            }
            if (root.has("data") && root.isNull("data")) {
                return@runBlocking if (storage.replace(tokens, SessionTokens(null, null))) null else storage.read().access
            }
            val data = root.optJSONObject("data") ?: root
            val payload = data.optJSONObject("auth") ?: data
            val access = payload.optString("access_token", payload.optString("accessToken"))
                .takeIf { it.isNotBlank() && it != "null" }
                ?: throw IllegalStateException("Máy chủ chưa trả token mới. Vui lòng thử lại.")
            val refresh = payload.optString("refresh_token", payload.optString("refreshToken"))
                .takeIf { it.isNotBlank() && it != "null" } ?: refreshToken
            if (!storage.replace(tokens, SessionTokens(access, refresh))) return@runBlocking storage.read().access
            access
        }
    }
}

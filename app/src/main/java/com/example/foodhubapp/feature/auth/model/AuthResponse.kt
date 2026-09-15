package com.example.foodhubapp.feature.auth.model

import org.json.JSONObject

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: UserDto?
) {
    companion object {
        fun fromJson(json: JSONObject): AuthResponse {
            val data = json.optJSONObject("data") ?: json
            val authPayload = data.optJSONObject("auth") ?: data

            return AuthResponse(
                accessToken = authPayload.findString("accessToken", "token", "access_token"),
                refreshToken = authPayload.findOptionalString("refreshToken", "refresh_token"),
                user = data.optJSONObject("user")?.let(UserDto::fromJson)
                    ?: authPayload.optJSONObject("user")?.let(UserDto::fromJson)
            )
        }
    }
}

private fun JSONObject.findString(vararg keys: String): String {
    return keys.firstNotNullOfOrNull { key ->
        optString(key).ifBlank { null }
    } ?: throw IllegalStateException("API không trả về access token")
}

private fun JSONObject.findOptionalString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        optString(key).ifBlank { null }
    }
}

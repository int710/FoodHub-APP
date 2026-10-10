@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.shared.auth.model

import com.example.foodhubapp.core.network.*
import com.google.gson.JsonObject

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: UserDto?
) {
    companion object {
        fun fromJson(json: JsonObject): AuthResponse {
            val data = json.optObject("data") ?: json
            val authPayload = data.optObject("auth") ?: data

            return AuthResponse(
                accessToken = authPayload.findString("accessToken", "token", "access_token"),
                refreshToken = authPayload.findOptionalString("refreshToken", "refresh_token"),
                user = data.optObject("user")?.let(UserDto::fromJson)
                    ?: authPayload.optObject("user")?.let(UserDto::fromJson)
            )
        }
    }
}

private fun JsonObject.findString(vararg keys: String): String {
    return keys.firstNotNullOfOrNull { key ->
        optString(key).ifBlank { null }
    } ?: throw IllegalStateException("API không trả về access token")
}

private fun JsonObject.findOptionalString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        optString(key).ifBlank { null }
    }
}

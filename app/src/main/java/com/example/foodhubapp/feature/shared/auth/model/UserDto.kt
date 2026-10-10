package com.example.foodhubapp.feature.shared.auth.model

import com.example.foodhubapp.core.network.*
import com.google.gson.JsonObject

data class UserDto(
    val id: String,
    val fullName: String,
    val phoneNumber: String?,
    val email: String?,
    val role: String = "CUSTOMER",
    val dateOfBirth: String? = null,
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
) {
    companion object {
        fun fromJson(json: JsonObject): UserDto {
            return UserDto(
                id = json.optString("id", json.optString("_id")),
                fullName = json.optString("fullName", json.optString("name")),
                phoneNumber = json.optString("phoneNumber", json.optString("phone")).ifBlank { null },
                email = json.optString("email").ifBlank { null },
                role = json.optString("role", "CUSTOMER"),
                dateOfBirth = json.optString("dateOfBirth").ifBlank { null }?.take(10),
                avatarUrl = json.optString("avatar").ifBlank { null },
                isActive = json.optBoolean("isActive", true),
                isVerified = json.optBoolean("isVerified", false),
            )
        }
    }
}

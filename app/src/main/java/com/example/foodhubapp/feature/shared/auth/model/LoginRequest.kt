package com.example.foodhubapp.feature.shared.auth.model

import com.example.foodhubapp.core.network.*
import com.google.gson.JsonObject

data class LoginRequest(
    val account: String,
    val password: String
) {
    fun toJson(): JsonObject {
        return JsonObject()
            .put("email", account.trim())
            .put("password", password)
    }
}

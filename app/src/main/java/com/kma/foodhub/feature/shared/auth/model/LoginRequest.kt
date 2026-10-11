package com.kma.foodhub.feature.shared.auth.model

import com.kma.foodhub.core.network.*
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

package com.example.foodhubapp.feature.auth.model

import org.json.JSONObject

data class LoginRequest(
    val account: String,
    val password: String
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("email", account.trim())
            .put("password", password)
    }
}

package com.example.foodhubapp.feature.auth.model

import org.json.JSONObject

data class LoginRequest(
    val account: String,
    val password: String
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("account", account)
            .put("emailOrPhone", account)
            .put("password", password)
    }
}

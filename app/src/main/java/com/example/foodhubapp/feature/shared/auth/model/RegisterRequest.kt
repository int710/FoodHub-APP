package com.example.foodhubapp.feature.shared.auth.model

import org.json.JSONObject

data class RegisterRequest(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val confirmPassword: String
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("name", fullName)
            .put("phone", phoneNumber)
            .put("email", email)
            .put("password", password)
            .put("confirmPassword", confirmPassword)
    }
}

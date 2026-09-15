package com.example.foodhubapp.feature.auth.model

import org.json.JSONObject

data class RegisterRequest(
    val fullName: String,
    val phoneNumber: String,
    val email: String?,
    val password: String,
    val confirmPassword: String,
    val referralCode: String?
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("fullName", fullName)
            .put("name", fullName)
            .put("phoneNumber", phoneNumber)
            .put("phone", phoneNumber)
            .put("email", email)
            .put("password", password)
            .put("confirmPassword", confirmPassword)
            .put("referralCode", referralCode)
    }
}

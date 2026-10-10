package com.example.foodhubapp.feature.shared.auth.model

import com.example.foodhubapp.core.network.*
import com.google.gson.JsonObject

data class RegisterRequest(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val confirmPassword: String
) {
    fun toJson(): JsonObject {
        return JsonObject()
            .put("name", fullName)
            .put("phone", phoneNumber)
            .put("email", email)
            .put("password", password)
            .put("confirmPassword", confirmPassword)
    }
}

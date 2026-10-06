package com.example.foodhubapp.feature.shared.auth.model

/**
 * Enum mức độ của mật khẩu.
 */
enum class PasswordStrength(
    val level: Int,
    val label: String
) {
    EMPTY(0, ""),
    WEAK(1, "Yếu"),
    MEDIUM(2, "Trung bình"),
    STRONG(3, "Mạnh")
}

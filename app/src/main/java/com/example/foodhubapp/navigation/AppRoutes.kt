package com.example.foodhubapp.navigation

/**
 * Định nghĩa tất cả các đường dẫn (routes) điều hướng trong ứng dụng FoodHub.
 * Giúp tránh việc hardcode chuỗi string khi navigate giữa các màn hình.
 */
object AppRoutes {
    const val Splash = "splash"         // Màn hình chờ khởi động
    const val Onboarding = "onboarding" // Màn hình giới thiệu app
    const val Login = "login"           // Màn hình đăng nhập
    const val Register = "register"     // Màn hình đăng ký
    const val Home = "home"             // Màn hình trang chủ chính
    const val Menu = "menu"             // Màn hình thực đơn / quét QR
}

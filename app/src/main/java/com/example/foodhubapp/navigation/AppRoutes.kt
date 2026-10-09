@file:Suppress("ConstPropertyName")

package com.example.foodhubapp.navigation

/**
 * Định nghĩa tất cả các đường dẫn (routes) điều hướng trong ứng dụng FoodHub.
 * Giúp tránh việc hardcode chuỗi string khi navigate giữa các màn hình.
 */
object AppRoutes {
    const val FoodDetail = "food_detail/{foodId}"
    fun foodDetail(foodId: String) = "food_detail/${android.net.Uri.encode(foodId)}"
    const val Profile = "profile"
    const val ForgotPassword = "forgot_password"
    const val ResetPassword = "reset_password"
    const val ResetPasswordPattern = "reset_password?token={token}"
    fun resetPassword(token: String) = "reset_password?token=${android.net.Uri.encode(token)}"
    const val VerifyEmail = "verify_email"
    const val VerifyEmailPattern = "verify_email?token={token}"
    fun verifyEmail(token: String) = "verify_email?token=${android.net.Uri.encode(token)}"
    const val Splash = "splash"         // Màn hình chờ khởi động
    const val Onboarding = "onboarding" // Màn hình giới thiệu app
    const val Login = "login"           // Màn hình đăng nhập
    const val Register = "register"     // Màn hình đăng ký
    const val Home = "home"             // Màn hình trang chủ chính
    const val ScanTable = "scan_table"  // Quét QR để nhận table token
    const val Menu = "menu"             // Màn hình thực đơn / quét QR
    const val Cart = "cart"             // Giỏ hàng mang đi
    const val Orders = "orders"         // Lịch sử và trạng thái đơn của khách hàng
    const val Notifications = "notifications"
    const val QrScan = "qr_scan"        // Quét QR để mở phiên tại bàn
    const val TableChat = "table_chat"  // Chat hỗ trợ theo phiên bàn
    const val Admin = "admin"           // Khu vực quản trị nhà hàng
}

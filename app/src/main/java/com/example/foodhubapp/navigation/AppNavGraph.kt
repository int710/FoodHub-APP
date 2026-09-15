package com.example.foodhubapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.foodhubapp.feature.auth.ui.LoginRoute
import com.example.foodhubapp.feature.auth.ui.RegisterRoute
import com.example.foodhubapp.feature.onboarding.ui.OnboardingScreen
import com.example.foodhubapp.feature.splash.ui.FoodHubSplashRoute
import com.example.foodhubapp.ui.theme.AppBackground
import com.example.foodhubapp.ui.theme.HeadingFont
import com.example.foodhubapp.ui.theme.Neutral

/**
 * Biểu đồ điều hướng trung tâm (Navigation Graph) của ứng dụng.
 * Quản lý tất cả các màn hình (destinations) và quy tắc chuyển hướng (navigation actions).
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoutes.Splash // Màn hình khởi đầu là Splash
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 1. Màn hình Splash: Kiểm tra trạng thái đăng nhập ban đầu
        composable(AppRoutes.Splash) {
            FoodHubSplashRoute(
                onSplashFinished = { isAuthenticated ->
                    // Nếu đã đăng nhập -> vào Home, ngược lại -> vào Onboarding
                    val nextRoute = if (isAuthenticated) {
                        AppRoutes.Home
                    } else {
                        AppRoutes.Onboarding
                    }

                    navController.navigate(nextRoute) {
                        popUpTo(AppRoutes.Splash) {
                            inclusive = true // Xóa Splash khỏi backstack
                        }
                    }
                }
            )
        }

        // 2. Màn hình Onboarding: Giới thiệu tính năng cho người dùng mới
        composable(AppRoutes.Onboarding) {
            OnboardingScreen(
                onStartClick = {
                    navController.navigate(AppRoutes.Register)
                },
                onLoginClick = {
                    navController.navigate(AppRoutes.Login)
                },
                onSkipClick = {
                    navController.navigate(AppRoutes.Home)
                }
            )
        }

        // 3. Màn hình Home: Trang chủ sau khi đăng nhập thành công
        composable(AppRoutes.Home) {
            PlaceholderScreen(title = "Home")
        }

        // 4. Màn hình Đăng nhập (Login)
        composable(AppRoutes.Login) {
            LoginRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Login) {
                            inclusive = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate(AppRoutes.Register)
                },
                onGuestQrClick = {
                    navController.navigate(AppRoutes.Menu)
                }
            )
        }

        // 5. Màn hình Đăng ký (Register)
        composable(AppRoutes.Register) {
            RegisterRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onRegisterClick = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Register) {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate(AppRoutes.Login)
                }
            )
        }

        // 6. Màn hình Menu / Khách vãng lai quét QR
        composable(AppRoutes.Menu) {
            PlaceholderScreen(title = "Menu")
        }
    }
}

/**
 * Màn hình tạm thời (Placeholder) dùng cho các route chưa triển khai đầy đủ UI chi tiết.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Neutral,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
    }
}

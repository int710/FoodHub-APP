package com.example.foodhubapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.foodhubapp.feature.auth.ui.ForgotPasswordRoute
import com.example.foodhubapp.core.payment.VnPayReturn
import com.example.foodhubapp.feature.auth.ui.LoginRoute
import com.example.foodhubapp.feature.auth.ui.RegisterRoute
import com.example.foodhubapp.feature.cart.ui.CartRoute
import com.example.foodhubapp.feature.home.ui.HomeRoute
import com.example.foodhubapp.feature.menu.ui.FoodDetailRoute
import com.example.foodhubapp.feature.menu.ui.MenuRoute
import com.example.foodhubapp.feature.onboarding.ui.OnboardingScreen
import com.example.foodhubapp.feature.order.ui.OrderListRoute
import com.example.foodhubapp.feature.profile.ui.ProfileRoute
import com.example.foodhubapp.feature.splash.ui.FoodHubSplashRoute
import com.foodhub.app.ScanScreen
import com.foodhub.app.TableSessionStore

/**
 * Biểu đồ điều hướng trung tâm (Navigation Graph) của ứng dụng.
 * Quản lý tất cả các màn hình (destinations), quy tắc chuyển hướng (navigation actions)
 * và hiệu ứng chuyển cảnh mượt mà giữa các màn hình (screen transitions).
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoutes.Splash,
    paymentReturn: VnPayReturn? = null,
    onPaymentReturnConsumed: (VnPayReturn) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(paymentReturn) {
        if (paymentReturn != null) {
            val target = if (TableSessionStore(context).current() != null) AppRoutes.Cart else AppRoutes.Orders
            if (navController.currentDestination?.route != target) {
                navController.navigate(target) { launchSingleTop = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        
        // Hiệu ứng mặc định khi mở màn hình MỚI (Trượt từ phải sang + Mờ dần)
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        
        // Hiệu ứng mặc định khi màn hình CŨ bị đẩy ra sau (Trượt nhẹ sang trái + Mờ dần)
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
        
        // Hiệu ứng khi bấm QUAY LẠI (Pop Enter - Màn hình phía trước quay lại từ bên trái)
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        
        // Hiệu ứng khi màn hình hiện tại đóng lại (Pop Exit - Trượt về bên phải + Mờ dần)
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        }
    ) {
        // 1. Màn hình Splash: Kiểm tra trạng thái đăng nhập ban đầu
        composable(AppRoutes.Splash) {
            FoodHubSplashRoute(
                onSplashFinished = { isAuthenticated ->
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

        // 3. Màn hình Home: Trang chủ
        composable(AppRoutes.Home) {
            HomeRoute(
                onFoodClick = { navController.navigate(AppRoutes.foodDetail(it)) },
                onMenuClick = { navController.navigate(AppRoutes.Menu) },
                onCartClick = { navController.navigate(AppRoutes.Cart) },
                onOrdersClick = { navController.navigate(AppRoutes.Orders) { launchSingleTop = true } },
                onScanQrClick = { navController.navigate(AppRoutes.ScanTable) { launchSingleTop = true } },
                onProfileClick = { navController.navigate(AppRoutes.Profile) }
            )
        }

        // 4. Màn hình Đăng nhập (Login)
        composable(AppRoutes.Login) {
            LoginRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = {
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    if (previousRoute == AppRoutes.FoodDetail || previousRoute == AppRoutes.Cart || previousRoute == AppRoutes.Orders) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(AppRoutes.Home) {
                            popUpTo(navController.graph.id)
                            launchSingleTop = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigateToAuth(AppRoutes.Register)
                },
                onGuestQrClick = {
                    navController.navigate(AppRoutes.ScanTable)
                },
                onForgotPasswordClick = {
                    navController.navigate(AppRoutes.ForgotPassword) { launchSingleTop = true }
                }
            )
        }

        composable(AppRoutes.ForgotPassword) {
            ForgotPasswordRoute(
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.navigateToAuth(AppRoutes.Login) },
                onRegisterClick = {
                    navController.navigate(AppRoutes.Register) {
                        popUpTo(AppRoutes.ForgotPassword) { inclusive = true }
                        launchSingleTop = true
                    }
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
                        popUpTo(navController.graph.id)
                        launchSingleTop = true
                    }
                },
                onLoginClick = {
                    navController.navigateToAuth(AppRoutes.Login)
                }
            )
        }

        // 6. Màn hình Hồ sơ Cá nhân (Profile): Sử dụng hiệu ứng trượt trơn tru
        composable(AppRoutes.Profile) {
            ProfileRoute(
                onLoggedOut = {
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(navController.graph.id)
                        launchSingleTop = true
                    }
                }, 
                onMenuClick = { navController.navigate(AppRoutes.Menu) }
            )
        }

        composable(AppRoutes.ScanTable) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    val origin = navController.previousBackStackEntry?.destination?.route
                    val canReturnToOrdering = origin == AppRoutes.Home || origin == AppRoutes.Menu ||
                        origin == AppRoutes.FoodDetail || origin == AppRoutes.Cart
                    if (canReturnToOrdering) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(AppRoutes.Home) {
                            popUpTo(navController.graph.id)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // 7. Màn hình Menu / Khách vãng lai quét QR
        composable(AppRoutes.Menu) {
            MenuRoute(
                onFoodClick = { navController.navigate(AppRoutes.foodDetail(it)) },
                onBackClick = { navController.popBackStack() },
                onCartClick = { navController.navigate(AppRoutes.Cart) }
            )
        }

        // 8. Màn hình Chi tiết Món ăn
        composable(
            AppRoutes.FoodDetail, 
            arguments = listOf(navArgument("foodId") { type = NavType.StringType })
        ) {
            FoodDetailRoute(
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.navigate(AppRoutes.Login) { launchSingleTop = true } },
                onScanQrClick = { navController.navigate(AppRoutes.ScanTable) { launchSingleTop = true } },
                onCartClick = { navController.navigate(AppRoutes.Cart) }
            )
        }

        // 9. Màn hình Giỏ hàng (Cart)
        composable(AppRoutes.Cart) {
            CartRoute(
                paymentReturn = paymentReturn,
                onPaymentReturnConsumed = onPaymentReturnConsumed,
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.navigate(AppRoutes.Login) { launchSingleTop = true } },
                onScanQrClick = { navController.navigate(AppRoutes.ScanTable) { launchSingleTop = true } }
            )
        }

        // 10. Tab Đơn hàng: lịch sử, lọc trạng thái, hủy đơn và thanh toán lại VNPay.
        composable(
            AppRoutes.Orders,
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            OrderListRoute(
                paymentReturn = paymentReturn,
                onPaymentReturnConsumed = onPaymentReturnConsumed,
                onHomeClick = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNotificationClick = { /* Notification API sẽ được nối ở feature riêng. */ },
                onProfileClick = { navController.navigate(AppRoutes.Profile) { launchSingleTop = true } },
                onLoginClick = { navController.navigate(AppRoutes.Login) { launchSingleTop = true } }
            )
        }
    }
}

private fun NavHostController.navigateToAuth(route: String) {
    if (popBackStack(route, inclusive = false)) return
    navigate(route) { launchSingleTop = true }
}

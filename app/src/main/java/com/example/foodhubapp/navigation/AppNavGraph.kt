package com.example.foodhubapp.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.payment.toVnPayReturnOrNull
import com.example.foodhubapp.feature.shared.auth.ui.ForgotPasswordRoute
import com.example.foodhubapp.feature.shared.auth.ui.LoginRoute
import com.example.foodhubapp.feature.shared.auth.ui.RegisterRoute
import com.example.foodhubapp.feature.shared.auth.ui.AccountTokenMode
import com.example.foodhubapp.feature.shared.auth.ui.AccountTokenRoute
import com.example.foodhubapp.feature.customer.cart.ui.CartRoute
import com.example.foodhubapp.feature.customer.home.ui.HomeRoute
import com.example.foodhubapp.feature.customer.menu.ui.FoodDetailRoute
import com.example.foodhubapp.feature.customer.menu.ui.MenuRoute
import com.example.foodhubapp.feature.customer.onboarding.ui.OnboardingScreen
import com.example.foodhubapp.feature.customer.onboarding.data.OnboardingStore
import com.example.foodhubapp.feature.shared.notification.ui.NotificationRoute
import com.example.foodhubapp.feature.customer.order.ui.OrderListRoute
import com.example.foodhubapp.feature.customer.profile.ui.ProfileRoute
import com.example.foodhubapp.feature.customer.splash.ui.FoodHubSplashRoute
import com.example.foodhubapp.feature.admin.navigation.AdminApp
import com.example.foodhubapp.feature.customer.chat.ChatScreen
import com.example.foodhubapp.feature.customer.table.ScanScreen
import kotlinx.coroutines.launch

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
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenStore = remember(context) { TokenStore(context.applicationContext) }
    val onboardingStore = remember(context) { OnboardingStore(context.applicationContext) }
    val paymentReturn = deepLinkUri?.toVnPayReturnOrNull()

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
                    coroutineScope.launch {
                        val nextRoute = when {
                            !isAuthenticated && !onboardingStore.hasCompleted() -> AppRoutes.Onboarding
                            !isAuthenticated -> AppRoutes.Home
                            tokenStore.getUser()?.role.equals("ADMIN", ignoreCase = true) -> AppRoutes.Admin
                            else -> AppRoutes.Home
                        }

                        navController.navigate(nextRoute) {
                            popUpTo(AppRoutes.Splash) {
                                inclusive = true // Xóa Splash khỏi backstack
                            }
                        }
                    }
                }
            )
        }

        // 2. Màn hình Onboarding: Giới thiệu tính năng cho người dùng mới
        composable(AppRoutes.Onboarding) {
            OnboardingScreen(
                onStartClick = {
                    onboardingStore.complete()
                    navController.navigate(AppRoutes.Register)
                },
                onLoginClick = {
                    onboardingStore.complete()
                    navController.navigate(AppRoutes.Login)
                },
                onSkipClick = {
                    onboardingStore.complete()
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Onboarding) { inclusive = true }
                    }
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
                onProfileClick = { navController.navigate(AppRoutes.Profile) },
                onQrClick = { navController.navigate(AppRoutes.QrScan) },
                onChatClick = { navController.navigate(AppRoutes.TableChat) { launchSingleTop = true } },
                onNotificationsClick = { navController.navigate(AppRoutes.Notifications) { launchSingleTop = true } },
            )
        }

        // 4. Màn hình Đăng nhập (Login)
        composable(AppRoutes.Login) {
            LoginRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = { role ->
                    if (role.equals("ADMIN", ignoreCase = true)) {
                        navController.navigate(AppRoutes.Admin) {
                            popUpTo(navController.graph.id)
                            launchSingleTop = true
                        }
                    } else {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute == AppRoutes.FoodDetail || previousRoute == AppRoutes.Cart || previousRoute == AppRoutes.Orders) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(AppRoutes.Home) {
                                popUpTo(navController.graph.id)
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigateToAuth(AppRoutes.Register)
                },
                onGuestQrClick = {
                    navController.navigate(AppRoutes.QrScan)
                },
                onForgotPasswordClick = {
                    navController.navigate(AppRoutes.ForgotPassword) { launchSingleTop = true }
                },
                onVerifyEmailClick = { navController.navigate(AppRoutes.VerifyEmail) { launchSingleTop = true } },
            )
        }
         // Màn hình quên mật khẩu
        composable(AppRoutes.ForgotPassword) {
            ForgotPasswordRoute(
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.navigateToAuth(AppRoutes.Login) },
                onRegisterClick = {
                    navController.navigate(AppRoutes.Register) {
                        popUpTo(AppRoutes.ForgotPassword) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onResetTokenClick = { navController.navigate(AppRoutes.ResetPassword) { launchSingleTop = true } },
            )
        }

        composable(
            AppRoutes.ResetPasswordPattern,
            arguments = listOf(navArgument("token") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entry ->
            AccountTokenRoute(
                mode = AccountTokenMode.RESET_PASSWORD,
                initialToken = entry.arguments?.getString("token").orEmpty(),
                onBack = { navController.popBackStack() },
                onSuccess = { navController.navigateToAuth(AppRoutes.Login) },
            )
        }

        composable(
            AppRoutes.VerifyEmailPattern,
            arguments = listOf(navArgument("token") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entry ->
            AccountTokenRoute(
                mode = AccountTokenMode.VERIFY_EMAIL,
                initialToken = entry.arguments?.getString("token").orEmpty(),
                onBack = { navController.popBackStack() },
                onSuccess = { navController.navigateToAuth(AppRoutes.Login) },
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

        // 6. Màn hình Hồ sơ Cá nhân
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

        // 7. Màn hình Menu / Khách vãng lai quét QR
        composable(AppRoutes.Menu) {
            MenuRoute(
                onFoodClick = { navController.navigate(AppRoutes.foodDetail(it)) },
                onBackClick = { navController.popBackStack() },
                onCartClick = { navController.navigate(AppRoutes.Cart) }
            )
        }

        composable(AppRoutes.QrScan) {
            ScanScreen(
                onContinue = {
                    navController.navigate(AppRoutes.TableChat) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.TableChat) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onSessionEnded = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoutes.Notifications) {
            NotificationRoute(
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.navigate(AppRoutes.Login) { launchSingleTop = true } },
                onOpenOrder = {
                    navController.navigate(AppRoutes.Orders) {
                        popUpTo(AppRoutes.Notifications) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoutes.Admin) {
            AdminApp(
                onLogout = {
                    coroutineScope.launch {
                        tokenStore.clearTokens()
                        navController.navigate(AppRoutes.Login) {
                            popUpTo(navController.graph.id)
                            launchSingleTop = true
                        }
                    }
                }
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
                onCartClick = { navController.navigate(AppRoutes.Cart) }
            )
        }

        // 9. Màn hình Giỏ hàng
        composable(AppRoutes.Cart) {
            CartRoute(
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.navigate(AppRoutes.Login) { launchSingleTop = true } },
                onOrderCreated = {
                    navController.navigate(AppRoutes.Orders) {
                        popUpTo(AppRoutes.Cart) { inclusive = true }
                        launchSingleTop = true
                    }
                },
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
                onHomeClick = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNotificationClick = { navController.navigate(AppRoutes.Notifications) { launchSingleTop = true } },
                onProfileClick = { navController.navigate(AppRoutes.Profile) { launchSingleTop = true } },
                onLoginClick = { navController.navigate(AppRoutes.Login) { launchSingleTop = true } },
                onChatClick = { navController.navigate(AppRoutes.TableChat) { launchSingleTop = true } },
                paymentReturn = paymentReturn,
                onPaymentReturnConsumed = { onDeepLinkConsumed() },
            )
        }
    }

    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        if (uri.scheme == "foodhub") {
            val route = when (uri.host) {
                "verify" -> uri.getQueryParameter("token")?.let(AppRoutes::verifyEmail)
                "reset-password" -> uri.getQueryParameter("token")?.let(AppRoutes::resetPassword)
                "payment" -> AppRoutes.Orders
                else -> null
            }
            route?.let {
                navController.navigate(it) {
                    popUpTo(AppRoutes.Splash) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        if (uri.host != "payment") onDeepLinkConsumed()
    }
}

private fun NavHostController.navigateToAuth(route: String) {
    if (popBackStack(route, inclusive = false)) return
    navigate(route) { launchSingleTop = true }
}

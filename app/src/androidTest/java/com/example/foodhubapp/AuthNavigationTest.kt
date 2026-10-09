package com.example.foodhubapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.foodhubapp.navigation.AppNavGraph
import com.example.foodhubapp.navigation.AppRoutes
import com.example.foodhubapp.theme.FoodHubAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController

    private fun start() {
        compose.setContent {
            FoodHubAppTheme {
                controller = rememberNavController()
                AppNavGraph(controller, startDestination = AppRoutes.Login)
            }
        }
    }

    private fun assertRoute(route: String) {
        compose.runOnIdle { assertEquals(route, controller.currentDestination?.route) }
    }

    @Test
    fun forgotPasswordReturnsToExistingLoginWithoutDuplicate() {
        start()
        compose.onNodeWithText("Quên mật khẩu?").performScrollTo().performClick()
        assertRoute(AppRoutes.ForgotPassword)
        compose.onNodeWithText("Đăng nhập ngay").performScrollTo().performClick()
        assertRoute(AppRoutes.Login)
        compose.runOnIdle { assertEquals(null, controller.previousBackStackEntry) }
    }

    @Test
    fun forgotPasswordOpensRegistrationAndLoginDoesNotDuplicate() {
        start()
        compose.onNodeWithText("Quên mật khẩu?").performScrollTo().performClick()
        compose.onNodeWithText("Tạo tài khoản mới").performScrollTo().performClick()
        assertRoute(AppRoutes.Register)
        compose.onNodeWithText("Đăng nhập").performScrollTo().performClick()
        assertRoute(AppRoutes.Login)
        compose.runOnIdle { assertEquals(null, controller.previousBackStackEntry) }
    }
}

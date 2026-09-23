package com.example.foodhubapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.session.SessionManager
import com.example.foodhubapp.feature.auth.model.UserDto
import com.example.foodhubapp.navigation.AppNavGraph
import com.example.foodhubapp.navigation.AppRoutes
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileLogoutTest {
    @get:Rule val compose = createComposeRule()
    private val store = TokenStore(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
    private var oldAccess: String? = null
    private var oldRefresh: String? = null
    private var oldUser: UserDto? = null
    private lateinit var controller: NavHostController

    @Before fun saveSession() = runBlocking {
        oldAccess = store.getAccessToken()
        oldRefresh = store.getRefreshToken()
        oldUser = store.user.first()
        store.saveTokens("test-access", "test-refresh", UserDto("test-user", "Test User", "0900000000", "test@example.com"))
    }

    @After fun restoreSession() {
        runBlocking {
            store.clearTokens()
            oldAccess?.let { store.saveTokens(it, oldRefresh, oldUser) }
        }
    }

    @Test fun logoutClearsSessionAndProfileBackStack() {
        compose.setContent {
            FoodHubAppTheme {
                controller = rememberNavController()
                AppNavGraph(controller, startDestination = AppRoutes.Profile)
            }
        }
        compose.waitUntil(10000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("test@example.com")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Đăng xuất").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("Quên mật khẩu?")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(AppRoutes.Login, controller.currentDestination?.route)
            assertNull(controller.previousBackStackEntry)
        }
        runBlocking {
            assertNull(store.getAccessToken())
            assertNull(store.getRefreshToken())
            assertNull(store.user.first())
            assertFalse(SessionManager(store).isLoggedIn())
        }
    }
}

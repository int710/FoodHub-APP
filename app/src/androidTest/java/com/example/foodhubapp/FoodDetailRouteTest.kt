package com.example.foodhubapp

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodhubapp.feature.customer.menu.data.*
import com.example.foodhubapp.feature.customer.menu.ui.*
import com.example.foodhubapp.feature.customer.menu.viewmodel.FoodDetailViewModel
import com.example.foodhubapp.theme.FoodHubAppTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FoodDetailRouteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repositoryDataRendersAndGuestCanOpenLogin() {
        val pending = CompletableDeferred<FoodDetail>()
        val repository = object : FoodRepository {
            override suspend fun getMenu(): List<MenuCategory> = error("Not used")
            override suspend fun getFood(id: String) = pending.await()
            override suspend fun addToCart(selection: FoodCartSelection) { throw LoginRequiredException() }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var loginOpened = false
        compose.setContent {
            val model = remember {
                FoodDetailViewModel(context.applicationContext as Application,
                    SavedStateHandle(mapOf("foodId" to "api-burger")), repository)
            }
            FoodHubAppTheme { FoodDetailRoute({}, { loginOpened = true }, model) }
        }
        compose.onNodeWithText("Burger bò phô mai").assertDoesNotExist()
        pending.complete(FoodDetail("api-burger", "Burger bò phô mai",
            "Bánh burger bò nướng với phô mai và rau tươi.", 99000, "4.2",
            groups = listOf(FoodOptionGroup("extras", "Thêm món", true, false,
                listOf(FoodOption("egg", "Trứng", 15000), FoodOption("bacon", "Bacon", 30000)))),
            imageUrl = null))
        compose.waitUntil(5000) { compose.onAllNodesWithText("Burger bò phô mai").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Chọn kích cỡ").assertDoesNotExist()
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "food-detail-api.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Trứng").performScrollTo().performClick()
        compose.onNodeWithText("114.000đ").assertExists()
        compose.onNodeWithText("Thêm vào giỏ").performClick()
        compose.onNodeWithText("Bạn cần đăng nhập để thêm món vào giỏ mang đi.").assertExists()
        compose.onNode(hasText("Đăng nhập") and hasClickAction()).performClick()
        compose.runOnIdle { assertTrue(loginOpened) }
    }
}

package com.example.foodhubapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.foodhubapp.feature.customer.menu.ui.*
import com.example.foodhubapp.theme.FoodHubAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun requiredOptionAndToppingUpdateTotalAndCartPayload() {
        var result: FoodCartSelection? = null
        compose.setContent {
            FoodHubAppTheme { FoodDetailScreen(PreviewBurger, {}, { result = it }) }
        }
        compose.onNodeWithText("Thêm vào giỏ").assertIsNotEnabled()
        compose.onNodeWithText("Cỡ lớn (Double Patty)").performScrollTo().performClick()
        compose.onNodeWithText("Thêm vào giỏ").assertIsEnabled()
        compose.onNodeWithText("190.000đ").assertExists()
        compose.onNodeWithText("Trứng ốp la lòng đào").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Tăng số lượng").performClick()
        compose.onNodeWithText("404.000đ").assertExists()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Không hành")
        compose.onNodeWithText("Thêm vào giỏ").performClick()
        compose.runOnIdle {
            assertEquals(FoodCartSelection("preview-burger", 2, listOf("double", "egg"), "Không hành"), result)
        }
    }

    @Test
    fun itemWithoutGroupsCanBeAddedAndUnavailableItemCannot() {
        compose.setContent {
            FoodHubAppTheme { FoodDetailScreen(PreviewBurger.copy(groups = emptyList(), available = false), {}, {}) }
        }
        compose.onNodeWithText("Tạm hết món").assertIsNotEnabled()
        compose.onNodeWithText("Chọn kích cỡ").assertDoesNotExist()
    }
}

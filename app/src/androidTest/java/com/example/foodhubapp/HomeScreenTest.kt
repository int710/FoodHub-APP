package com.example.foodhubapp

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodhubapp.feature.home.ui.HomeScreen
import com.example.foodhubapp.feature.home.viewmodel.HomeUiState
import com.example.foodhubapp.feature.menu.data.MenuCategory
import com.example.foodhubapp.feature.menu.data.MenuFood
import com.example.foodhubapp.ui.theme.FoodHubAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeMatchesMainFigmaSections() {
        val foods = listOf(
            MenuFood("burger", "Truffle Smash Burger", 145_000, null, "Bò nướng áp chảo, sốt truffle", "4.9", true, 20),
            MenuFood("pizza", "Pizza Hải Sản Lò Củi", 185_000, null, "Hải sản tươi và phô mai", "4.8", true, 18),
            MenuFood("salmon", "Cơm Cá Hồi Áp Chảo Teriyaki", 165_000, null, "Cá hồi Na-uy sốt teriyaki", "4.7", false, 10)
        )
        compose.setContent {
            FoodHubAppTheme(dynamicColor = false) {
                HomeScreen(
                    state = HomeUiState(
                        categories = listOf(
                            MenuCategory("burger", "Burger & Sandwich", foods.take(1)),
                            MenuCategory("pizza", "Pizza Lò Củi", foods.drop(1))
                        ),
                        cartItemCount = 3,
                        userName = "FoodHub User"
                    ),
                    onQueryChange = {},
                    onCategoryClick = {},
                    onRetry = {},
                    onFoodClick = {},
                    onMenuClick = {},
                    onCartClick = {},
                    onOrdersClick = {},
                    onProfileClick = {},
                    onUnavailable = {}
                )
            }
        }

        compose.onNodeWithText("FoodHub").assertExists()
        compose.onNodeWithText("Danh Mục Món Ăn").assertExists()
        compose.onNodeWithText("Món Bán Chạy Hôm Nay").assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "home-figma.png")
        compose.onRoot().captureToImage().asAndroidBitmap().compress(
            Bitmap.CompressFormat.PNG,
            100,
            file.outputStream()
        )
    }
}

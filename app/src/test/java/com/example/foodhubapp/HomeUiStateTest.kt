package com.example.foodhubapp

import com.example.foodhubapp.feature.home.viewmodel.HomeUiState
import com.example.foodhubapp.feature.menu.data.MenuCategory
import com.example.foodhubapp.feature.menu.data.MenuFood
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {
    private val burger = MenuFood("burger", "Burger bò", 99_000, null, isFeatured = true, totalOrder = 12)
    private val pizza = MenuFood("pizza", "Pizza hải sản", 185_000, null, totalOrder = 20)
    private val tea = MenuFood("tea", "Trà đào", 45_000, null, available = false)
    private val state = HomeUiState(
        categories = listOf(
            MenuCategory("food", "Món ăn", listOf(burger, pizza)),
            MenuCategory("drink", "Đồ uống", listOf(tea))
        )
    )

    @Test fun queryAndCategoryFilterUseCurrentMenuData() {
        assertEquals(listOf("burger"), state.copy(query = "burger").visibleFoods.map { it.id })
        assertEquals(listOf("tea"), state.copy(selectedCategoryId = "drink").visibleFoods.map { it.id })
    }

    @Test fun featuredItemsHavePriorityAndUnavailableItemsAreExcluded() {
        assertEquals(listOf("burger"), state.popularFoods.map { it.id })
    }
}

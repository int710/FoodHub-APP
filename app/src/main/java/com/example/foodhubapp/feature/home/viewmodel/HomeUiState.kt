package com.example.foodhubapp.feature.home.viewmodel

import com.example.foodhubapp.feature.menu.model.MenuCategoryUiModel
import com.example.foodhubapp.feature.menu.model.MenuItemUiModel

data class HomeUiState(
    val isLoading: Boolean = false,
    val categories: List<MenuCategoryUiModel> = emptyList(),
    val bestSellers: List<MenuItemUiModel> = emptyList(),
    val suggestions: List<MenuItemUiModel> = emptyList(),
    val selectedCategoryId: String? = null,
    val errorMessage: String? = null
)

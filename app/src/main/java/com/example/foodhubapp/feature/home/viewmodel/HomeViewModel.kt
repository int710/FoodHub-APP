package com.example.foodhubapp.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.feature.menu.data.MenuRepository
import com.example.foodhubapp.feature.menu.data.RemoteMenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val menuRepository: MenuRepository = RemoteMenuRepository()
) : ViewModel() {
    // UI chỉ đọc StateFlow này; mọi thay đổi state đi qua ViewModel.
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeMenu()
    }

    fun loadHomeMenu() {
        if (_uiState.value.isLoading && _uiState.value.categories.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            // ViewModel không gọi HTTP trực tiếp; toàn bộ logic lấy/parse data nằm trong Repository.
            menuRepository.getHomeMenu()
                .onSuccess { homeData ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = homeData.categories,
                            bestSellers = homeData.bestSellers,
                            suggestions = homeData.suggestions,
                            selectedCategoryId = it.selectedCategoryId
                                ?: homeData.categories.firstOrNull()?.id,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không tải được thực đơn"
                        )
                    }
                }
        }
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update {
            it.copy(selectedCategoryId = categoryId)
        }
    }
}

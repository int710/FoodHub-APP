package com.example.foodhubapp.feature.splash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.feature.splash.data.LocalSplashLoadingRepository
import com.example.foodhubapp.feature.splash.data.SplashLoadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodHubSplashUiState(
    val loadingMessage: String = "Đang khởi động FoodHub...",
    val progressPercent: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = progressPercent.coerceIn(0, 100) / 100f
}

class FoodHubSplashViewModel(
    private val repository: SplashLoadingRepository = LocalSplashLoadingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodHubSplashUiState())
    val uiState: StateFlow<FoodHubSplashUiState> = _uiState.asStateFlow()

    init {
        observeLoadingProgress()
    }

    private fun observeLoadingProgress() {
        viewModelScope.launch {
            repository.observeMenuSyncProgress()
                .catch {
                    _uiState.update { current ->
                        current.copy(
                            loadingMessage = "Không thể khởi động ứng dụng",
                            isLoading = false,
                            errorMessage = it.message ?: "Đã có lỗi xảy ra"
                        )
                    }
                }
                .collect { progress ->
                    _uiState.update {
                        FoodHubSplashUiState(
                            loadingMessage = progress.message,
                            progressPercent = progress.progressPercent.coerceIn(0, 100),
                            isLoading = progress.progressPercent < 100
                        )
                    }
                }
        }
    }
}

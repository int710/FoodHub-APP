package com.example.foodhubapp.feature.splash.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.session.SessionManager
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
    val isAuthenticated: Boolean? = null,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = progressPercent.coerceIn(0, 100) / 100f
}

class FoodHubSplashViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: SplashLoadingRepository = LocalSplashLoadingRepository()
    private val sessionManager: SessionManager = SessionManager(
        tokenStore = TokenStore(application.applicationContext)
    )

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
                    val isComplete = progress.progressPercent >= 100
                    val isAuthenticated = if (isComplete) {
                        sessionManager.isLoggedIn()
                    } else {
                        null
                    }

                    _uiState.update {
                        FoodHubSplashUiState(
                            loadingMessage = progress.message,
                            progressPercent = progress.progressPercent.coerceIn(0, 100),
                            isLoading = !isComplete,
                            isAuthenticated = isAuthenticated
                        )
                    }
                }
        }
    }
}

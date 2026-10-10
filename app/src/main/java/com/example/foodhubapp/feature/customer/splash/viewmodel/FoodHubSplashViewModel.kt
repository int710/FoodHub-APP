package com.example.foodhubapp.feature.customer.splash.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhubapp.core.datastore.TokenStore
import com.example.foodhubapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodHubSplashUiState(
    val isAuthenticated: Boolean? = null,
)

class FoodHubSplashViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val sessionManager: SessionManager = SessionManager(
        tokenStore = TokenStore(application.applicationContext)
    )

    private val _uiState = MutableStateFlow(FoodHubSplashUiState())
    val uiState: StateFlow<FoodHubSplashUiState> = _uiState.asStateFlow()

    init {
        resolveSession()
    }

    private fun resolveSession() {
        viewModelScope.launch {
            val isAuthenticated = runCatching { sessionManager.isLoggedIn() }
                .getOrDefault(false)
            _uiState.update { it.copy(isAuthenticated = isAuthenticated) }
        }
    }
}
